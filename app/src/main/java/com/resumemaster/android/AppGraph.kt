package com.resumemaster.android

import android.content.Context
import com.resumemaster.android.data.AuthRepository
import com.resumemaster.android.data.JobRepository
import com.resumemaster.android.data.SecureTokenStore
import com.resumemaster.android.data.net.ApiClient

/**
 * The app's singletons, created once and shared.
 *
 * Deliberately a hand-rolled locator rather than a DI framework: this app has three dependencies
 * and adding Hilt would mean another annotation processor on a toolchain where KSP already needed a
 * migration flag to build at all.
 *
 * ── WHY THE BASE URL IS RESOLVED HERE AND ONLY HERE ─────────────────────────────────────────────
 *
 * The old repository hardcoded `https://resumemaster.one` and carried the emulator loopback address
 * in a comment beside it — which could not have worked anyway, because it cast the connection to
 * HttpsURLConnection. One resolution point means a debug build can reach the developer's machine
 * without any call site knowing.
 */
object AppGraph {

  @Volatile private var initialised = false

  lateinit var tokenStore: SecureTokenStore
    private set

  lateinit var auth: AuthRepository
    private set

  lateinit var jobs: JobRepository
    private set

  /**
   * The server this build talks to.
   *
   * 10.0.2.2 is the HOST loopback as seen from inside the emulator; 127.0.0.1 there is the emulated
   * device itself. Debug builds point at the local server so a real request can be verified without
   * touching production data.
   */
  val baseUrl: String
    get() = if (BuildConfig.DEBUG) ApiClient.EMULATOR_HOST_BASE_URL else ApiClient.DEFAULT_BASE_URL

  fun init(context: Context) {
    if (initialised) return
    synchronized(this) {
      if (initialised) return
      tokenStore = SecureTokenStore(context.applicationContext)
      auth = AuthRepository(tokenStore, baseUrl)
      jobs = JobRepository(auth.authed)
      initialised = true
    }
  }
}
