package com.resumemaster.android

import android.content.Context
import com.resumemaster.android.data.AuthRepository
import com.resumemaster.android.data.JobRepository
import com.resumemaster.android.data.MockData
import com.resumemaster.android.data.ResumeRepository
import com.resumemaster.android.data.SecureTokenStore
import com.resumemaster.android.data.db.AppDatabase
import com.resumemaster.android.data.net.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The app's singletons, created once and shared.
 *
 * Deliberately a hand-rolled locator rather than a DI framework: this app has three dependencies
 * and adding Hilt would mean another annotation processor on a toolchain where KSP already needed a
 * migration flag to build at all.
 *
 * ── WHY THE BASE URL IS RESOLVED HERE AND ONLY HERE ─────────────────────────────────────────────
 *
 * The old repository hardcoded the production origin and carried the emulator loopback address
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

  lateinit var database: AppDatabase
    private set

  /**
   * SHARED, and that is a fix, not just tidiness.
   *
   * ResumeViewModel used to construct its own `ResumeRepository()`, and `viewModel()` scopes to the
   * NavBackStackEntry — so the builder and the preview each got a separate ViewModel with a separate
   * repository seeded from the same mock. Edits made in the builder were invisible in the preview,
   * and the PDF exported the mock resume rather than the user's. One instance here is what makes
   * them the same resume.
   */
  lateinit var resume: ResumeRepository
    private set

  /**
   * Outlives every ViewModel on purpose. A durable write must not be cancelled because the user
   * navigated away from the screen that started it — viewModelScope would do exactly that, and the
   * edit would be lost in the one case the user is most likely to make one: typing, then leaving.
   */
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
      val t0 = android.os.SystemClock.uptimeMillis()
      tokenStore = SecureTokenStore(context.applicationContext)
      val t1 = android.os.SystemClock.uptimeMillis()
      auth = AuthRepository(tokenStore, baseUrl)
      jobs = JobRepository(auth.authed)
      val t2 = android.os.SystemClock.uptimeMillis()
      // Room does not touch the disk until the first query, so building it here does no main-thread
      // I/O; the hydration that does happens on appScope inside ResumeRepository.
      database = AppDatabase.build(context.applicationContext)
      resume = ResumeRepository(database.resumeDao(), appScope) { MockData.defaultResume }
      val t3 = android.os.SystemClock.uptimeMillis()
      // MEASURED, so the next person does not have to guess whether startup is this object's
      // fault. On a fresh API 36 emulator: tokenStore=605ms repos=10ms room=107ms total=722ms,
      // against a 15.5s cold start (Settings itself takes 6.7s on the same device, so the bulk is
      // Compose and dex on swiftshader, not this).
      //
      // The 605ms is EncryptedSharedPreferences building a Keystore-backed MasterKey, and it is
      // pre-existing. It sits on the main thread because NavGraph reads tokenStore.hasToken()
      // synchronously to choose its start destination; moving it off would make that choice async
      // and is a change to the auth flow rather than to persistence.
      if (BuildConfig.DEBUG) {
        android.util.Log.i(
          "AppGraphTiming",
          "tokenStore=" + (t1 - t0) + "ms repos=" + (t2 - t1) + "ms room=" + (t3 - t2) +
            "ms total=" + (t3 - t0) + "ms",
        )
      }
      initialised = true
    }
  }
}
