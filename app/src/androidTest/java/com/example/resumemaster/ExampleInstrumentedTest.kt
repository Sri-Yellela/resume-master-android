package com.example.resumemaster

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // Was "com.example.resumemaster" — the Studio template's placeholder, which this app has
        // never used. The assertion had simply never run: the build itself was unverified until
        // 2026-09-01, so no instrumented test had ever executed.
        //
        // ⛔ packageName IS THE applicationId, NOT THE KOTLIN PACKAGE. Those diverged in the
        // 2026-09-23 rebrand: applicationId became com.draft.android while `namespace` — and so
        // every `package com.resumemaster.android` line in this repo, including this file's own
        // directory — deliberately did not move. Asserting the Kotlin package here would fail on
        // a correct build.
        //
        // The literal is deliberate rather than read from BuildConfig. In androidTest,
        // `BuildConfig` resolves to the TEST APK's, whose APPLICATION_ID carries a `.test` suffix
        // and would never equal the target context's. More to the point, an applicationId is
        // IMMUTABLE AFTER FIRST PUBLISH, so pinning it in a test is the thing worth doing: this
        // assertion is what fails if anyone changes it again after the store listing exists.
        assertEquals("com.draft.android", appContext.packageName)
    }
}