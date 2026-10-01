package com.resumemaster.android.viewmodel

import com.resumemaster.android.data.JobRepository
import com.resumemaster.android.data.contract.AtsBand
import com.resumemaster.android.data.net.ApiClient
import com.resumemaster.android.models.SwipeAction
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The deck on the phone, against a real local HTTP server speaking contract 1.2.0's field names.
 * POST works on the desktop JDK (PATCH does not — see JobsViewModelTest), so the Draft call itself
 * is exercised here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModelTest {
  private lateinit var server: HttpServer
  @Volatile private var deckStatus = 200
  @Volatile private var runsStatus = 202
  private val runBodies = mutableListOf<JSONObject>()
  private val deckQueries = mutableListOf<String>()

  private fun card(id: String, position: Int, band: String, decision: String?) = JSONObject()
    .put("position", position).put("band", band).put("decision", decision ?: JSONObject.NULL)
    .put("job", JSONObject().put("id", id).put("title", "Role $id").put("company", "Co $id").put("matchScore", JSONObject.NULL))
    .put("why", JSONObject().put("matched", JSONArray().put("Go")).put("missing", JSONArray()).put("matchedTotal", 1))

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/api/draft/today") { ex ->
      synchronized(deckQueries) { deckQueries += ex.requestURI.rawQuery.orEmpty() }
      val body = if (deckStatus != 200) """{"error":"Not authenticated"}""" else JSONObject()
        .put("date", "2026-09-30").put("profileId", 1).put("size", 4).put("dealtNow", true).put("thin", true).put("reason", "thin")
        .put("cards", JSONArray()
          .put(card("a", 0, "strong", null)).put(card("b", 1, "strong", "drafted"))
          .put(card("c", 2, "moderate", null)).put(card("d", 3, "moderate", "passed")))
        .toString()
      val bytes = body.toByteArray(Charsets.UTF_8)
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(deckStatus, bytes.size.toLong()); ex.responseBody.use { it.write(bytes) }
    }
    server.createContext("/api/apply/runs") { ex ->
      synchronized(runBodies) { runBodies += JSONObject(ex.requestBody.readBytes().toString(Charsets.UTF_8)) }
      val body = if (runsStatus == 202) """{"ok":true,"runId":9,"queued":["a"]}"""
                 else """{"error":"missing_prerequisites","message":"Add your email address first."}"""
      val bytes = body.toByteArray(Charsets.UTF_8)
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(runsStatus, bytes.size.toLong()); ex.responseBody.use { it.write(bytes) }
    }
    server.start()
  }

  @After
  fun tearDown() { server.stop(0); Dispatchers.resetMain() }

  private fun vm(onClear: () -> Unit = {}) =
    DeckViewModel(JobRepository(ApiClient("http://127.0.0.1:${server.address.port}") { "t" }), onClear) { "2026-09-30" }

  private fun await(cond: () -> Boolean) {
    val deadline = System.currentTimeMillis() + 10_000
    while (System.currentTimeMillis() < deadline) { if (cond()) return; Thread.sleep(10) }
    error("timed out")
  }

  @Test
  fun `the deck shows only what is undecided, in deal order, under the band it was dealt`() {
    val v = vm()
    await { v.deck.value != null }
    assertEquals(listOf("a", "c"), v.jobs.value.map { it.id })
    assertEquals(listOf(AtsBand.STRONG, AtsBand.MODERATE), v.jobs.value.map { it.band })
    // The score is null by design; the chip must read the dealt band, not derive "Not enough signal".
    assertEquals(null, v.jobs.value.first().matchScore)
    assertEquals(1, v.drafted.value)
    assertTrue("the local date is sent", deckQueries.single().contains("date=2026-09-30"))
  }

  @Test
  fun `right drafts on the server - surface deck, and NEVER an approvalMode`() {
    val v = vm()
    await { v.jobs.value.isNotEmpty() }
    v.onSwipe(SwipeAction.Queue, v.jobs.value.first())
    await { runBodies.isNotEmpty() }
    val body = runBodies.single()
    assertEquals("deck", body.getString("surface"))
    assertEquals("a", body.getJSONArray("jobIds").getString(0))
    assertFalse("the deck must not ask for no review", body.has("approvalMode"))
    assertEquals(listOf("c"), v.jobs.value.map { it.id })
    assertEquals(2, v.drafted.value)
  }

  @Test
  fun `a refused draft is never a success - the card returns to the front with the server's words`() {
    runsStatus = 409
    val v = vm()
    await { v.jobs.value.isNotEmpty() }
    v.onSwipe(SwipeAction.Queue, v.jobs.value.first())
    await { v.error.value != null }
    assertEquals(listOf("a", "c"), v.jobs.value.map { it.id })
    assertEquals("Add your email address first.", v.error.value)
    assertEquals(1, v.drafted.value)
  }

  @Test
  fun `a 401 forgets the credential and says the session ended`() {
    deckStatus = 401
    var cleared = 0
    val v = vm { cleared++ }
    await { v.sessionExpired.value }
    assertEquals(1, cleared)
  }
}
