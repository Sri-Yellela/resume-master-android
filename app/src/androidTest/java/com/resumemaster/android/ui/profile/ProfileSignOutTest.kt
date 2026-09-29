package com.resumemaster.android.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sign-out control exists, is reachable, and calls sign-out.
 *
 * AuthViewModel.signOut had zero UI callers, so a user could not remove this device's credential
 * short of clearing app data. Run against af98492 (as `ProfileScreen {}`), the "Sign out" lookup
 * failed: no such node. What sign-out then DOES to the credential is AuthViewModelTest's job, on the
 * JVM, where a failing commit() can be injected.
 */
@RunWith(AndroidJUnit4::class)
class ProfileSignOutTest {
  @get:Rule val rule = createComposeRule()

  @Test
  fun signOutIsVisibleAndCallsSignOut() {
    var calls = 0
    rule.setContent { ProfileContent(onAdmin = {}, onSignOut = { calls++ }, signingOut = false, signOutError = null) }
    rule.onNodeWithText("Sign out").assertIsDisplayed().performClick()
    assertEquals(1, calls)
  }

  @Test
  fun aFailedSignOutSaysWhy() {
    rule.setContent {
      ProfileContent(onAdmin = {}, onSignOut = {}, signingOut = false, signOutError = "Could not remove your sign-in from this device. Try again.")
    }
    rule.onNodeWithText("Could not remove your sign-in from this device. Try again.").assertIsDisplayed()
  }
}
