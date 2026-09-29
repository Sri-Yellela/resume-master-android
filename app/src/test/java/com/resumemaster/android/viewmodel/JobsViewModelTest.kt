package com.resumemaster.android.viewmodel

import com.resumemaster.android.data.JobRepository
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The swipe stack, driven through the REAL JobRepository and ApiClient against a local HTTP server.
 *
 * Not a fake repository, on purpose: the defect pinned first below lived in the seam between the
 * ViewModel's visible stack and the repository's accumulated list, and a fake repository is exactly
 * the thing that would have agreed with whichever side the test author had in mind.
 *
 * Only GET is exercised (Skip and Queue make no request). The desktop JDK's HttpURLConnection
 * rejects PATCH, so Star/Dislike are deliberately not swiped here -- see ApiClient.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JobsViewModelTest {

  private lateinit var server: HttpServer
  private val requests = mutableListOf<String>()

  private fun job(id: String) = JSONObject().put("id", id).put("title", "Role $id").put("company", "Co $id")

  private fun page(ids: List<String>, nextCursor: String?) = JSONObject()
    .put("success", true)
    .put("jobs", JSONArray().apply { ids.forEach { put(job(it)) } })
    .put("total", ids.size)
    .put("paging", "cursor")
    .put("nextCursor", nextCursor ?: JSONObject.NULL)
    .toString()

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/api/jobs") { ex ->
      val query = ex.requestURI.rawQuery.orEmpty()
      synchronized(requests) { requests += query }
      val body = if ("cursor=c1" in query) page(listOf("b1", "b2"), null)
                 else page(listOf("a1", "a2", "a3", "a4", "a5"), "c1")
      val bytes = body.toByteArray(Charsets.UTF_8)
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(200, bytes.size.toLong())
      ex.responseBody.use { it.write(bytes) }
    }
    server.start()
  }

  @After
  fun tearDown() {
    server.stop(0)
    Dispatchers.resetMain()
  }

  private fun viewModel(): JobsViewModel {
    val client = ApiClient("http://127.0.0.1:${server.address.port}") { "test-token" }
    return JobsViewModel(JobRepository(client))
  }

  /** The feed runs on Dispatchers.IO, so wait for it the way the UI does: by observing state. */
  private fun awaitIdle(vm: JobsViewModel, pagesRequested: Int) {
    val deadline = System.currentTimeMillis() + 10_000
    while (System.currentTimeMillis() < deadline) {
      val served = synchronized(requests) { requests.size }
      if (served >= pagesRequested && !vm.loading.value) return
      Thread.sleep(10)
    }
    error("feed did not settle: ${synchronized(requests) { requests.toList() }}, loading=${vm.loading.value}")
  }

  @Test
  fun `a card swiped away does not come back when the next page arrives`() {
    val vm = viewModel()
    awaitIdle(vm, pagesRequested = 1)
    assertEquals(listOf("a1", "a2", "a3", "a4", "a5"), vm.jobs.value.map { it.id })

    // Two swipes leave three cards, which is the prefetch threshold: the second swipe fetches page 2.
    vm.onSwipe(SwipeAction.Skip, vm.jobs.value.first())
    vm.onSwipe(SwipeAction.Skip, vm.jobs.value.first())
    awaitIdle(vm, pagesRequested = 2)
    assertTrue("page 2 was requested with the cursor", requests.any { "cursor=c1" in it })

    // The stack the user sees must be what they have NOT yet decided on, followed by the new page.
    // It used to be replaced wholesale by the repository's accumulated list, so every card swiped
    // since launch reappeared at the top of the stack the moment page 2 landed.
    assertEquals(listOf("a3", "a4", "a5", "b1", "b2"), vm.jobs.value.map { it.id })
  }

  @Test
  fun `two identical swipes in a row are two distinct confirmations`() {
    val vm = viewModel()
    awaitIdle(vm, pagesRequested = 1)

    vm.onSwipe(SwipeAction.Queue, vm.jobs.value.first())
    val first = vm.lastAction.value
    vm.onSwipe(SwipeAction.Queue, vm.jobs.value.first())
    val second = vm.lastAction.value

    assertEquals(SwipeAction.Queue, first?.action)
    assertEquals(SwipeAction.Queue, second?.action)
    // A StateFlow does not emit a value equal to the one it holds. When lastAction was the bare
    // SwipeAction, the second Queue was Queue == Queue: no emission, ActionBadge's LaunchedEffect
    // key did not change, and the second swipe got no "Queued for review" at all.
    assertNotEquals("the second swipe must be a new value, or the badge never sees it", first, second)
  }
}
