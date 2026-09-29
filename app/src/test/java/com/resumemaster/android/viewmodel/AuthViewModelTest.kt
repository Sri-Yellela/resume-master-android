package com.resumemaster.android.viewmodel

import com.resumemaster.android.data.AuthRepository
import com.resumemaster.android.data.FakePrefs
import com.resumemaster.android.data.SecureTokenStore
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Sign-in and sign-out, through the REAL AuthRepository and ApiClient against a local HTTP server,
 * over a SecureTokenStore whose backing store can be told to fail a commit.
 *
 * What is pinned is the Shape 3 property: the UI state says what actually happened to the
 * credential on THIS device. A token that was not stored is not a sign-in; a token that was not
 * removed is not a sign-out.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

  private lateinit var server: HttpServer
  private val prefs = FakePrefs()
  private val store = SecureTokenStore(prefs)
  @Volatile private var revokeStatus = 200
  private val revokes = mutableListOf<String?>()

  private fun HttpServer.json(path: String, status: () -> Int = { 200 }, body: () -> String) {
    createContext(path) { ex ->
      if (path == "/api/auth/revoke-mobile-token") {
        synchronized(revokes) { revokes += ex.requestHeaders.getFirst("Authorization") }
      }
      val bytes = body().toByteArray(Charsets.UTF_8)
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(status(), bytes.size.toLong())
      ex.responseBody.use { it.write(bytes) }
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.json("/api/auth/login") {
      JSONObject().put("authContext", "session-bound")
        .put("user", JSONObject().put("id", "u1").put("username", "sri")).toString()
    }
    server.json("/api/auth/mobile-token") {
      JSONObject().put("token", "durable").put("absoluteSeconds", 7_776_000L).toString()
    }
    server.json("/api/auth/revoke-mobile-token", status = { revokeStatus }) {
      JSONObject().put("success", revokeStatus == 200).toString()
    }
    server.start()
  }

  @After
  fun tearDown() {
    server.stop(0)
    Dispatchers.resetMain()
  }

  private fun viewModel() =
    AuthViewModel(AuthRepository(store, "http://127.0.0.1:${server.address.port}"))

  private fun await(vm: AuthViewModel, until: (AuthUiState) -> Boolean) {
    val deadline = System.currentTimeMillis() + 10_000
    while (System.currentTimeMillis() < deadline) {
      if (until(vm.state.value)) return
      Thread.sleep(10)
    }
    error("auth did not settle: ${vm.state.value}")
  }

  private fun signedInViewModel(): AuthViewModel {
    val vm = viewModel()
    vm.signIn("sri", "pw")
    await(vm) { !it.busy }
    assertTrue("precondition: signed in", vm.state.value.signedIn)
    assertEquals("durable", store.token())
    return vm
  }

  @Test
  fun `sign-out removes the credential and says so`() {
    val vm = signedInViewModel()
    vm.signOut()
    await(vm) { !it.signedIn && !it.busy }
    assertNull("the token is gone from this device", store.token())
    assertEquals("the server was asked to revoke THIS token", listOf("Bearer durable"), revokes)
    assertNull(vm.state.value.error)
  }

  @Test
  fun `sign-out still signs out locally when the server cannot confirm the revoke`() {
    val vm = signedInViewModel()
    revokeStatus = 500
    vm.signOut()
    await(vm) { !it.signedIn && !it.busy }
    // Withdrawing consent for this device to hold the credential must not depend on the network.
    assertNull(store.token())
  }

  @Test
  fun `a clear that did not reach disk is not reported as signed out`() {
    val vm = signedInViewModel()
    prefs.failCommits = true
    vm.signOut()
    await(vm) { (it.error != null || !it.signedIn) && !it.busy }
    // SecureTokenStore.clear() discarded commit()'s result and AuthViewModel set signedIn=false
    // unconditionally, so the UI went to "signed out" over a credential still on disk.
    assertEquals("the token is still on this device", "durable", store.token())
    assertTrue("so the UI must not claim a sign-out that did not happen", vm.state.value.signedIn)
    assertNotNull("and it must say why", vm.state.value.error)
  }

  @Test
  fun `a token that could not be stored is a failed sign-in, not a successful one`() {
    prefs.failCommits = true
    val vm = viewModel()
    vm.signIn("sri", "pw")
    await(vm) { !it.busy }
    assertNull(store.token())
    assertFalse("nothing durable is held, so this is not signed in", vm.state.value.signedIn)
    assertNotNull(vm.state.value.error)
  }
}
