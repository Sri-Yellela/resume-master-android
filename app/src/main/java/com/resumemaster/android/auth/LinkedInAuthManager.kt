package com.resumemaster.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.resumemaster.android.AppGraph
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject

/*
 * LinkedIn OIDC Profile Import - Android
 * Uses Chrome Custom Tabs (secure, no WebView)
 * Scopes: openid profile email
 *
 * The server handles LinkedIn OIDC and redirects back with mapped name/email.
 * The app never stores a LinkedIn access token.
 */

data class LinkedInResumeFields(
  val name: String,
  val email: String,
  val photoUrl: String?,
  val given_name: String = "",
  val family_name: String = ""
)

object LinkedInAuthManager {

  val pendingImport = MutableStateFlow<LinkedInResumeFields?>(null)

  /**
   * Open the server's OIDC start endpoint in a Custom Tab.
   *
   * THE BASE URL COMES FROM AppGraph AND NOWHERE ELSE. It used to be a private const here reading
   * `https://YOUR_DOMAIN.com` — a template placeholder, so this entire import path launched a
   * Custom Tab at a domain the project does not own and never could have worked. AppGraph's own
   * doc comment already claimed the base URL was "resolved here and only here"; this was the
   * counter-example, and it is the project's most-repeated defect shape (one value, two homes, one
   * of them wrong) sitting inside the file that documents the rule.
   *
   * Reading AppGraph also picks up the debug/release split for free: a debug build now points at
   * the emulator's host loopback like every other request instead of at production, which is what
   * the comment `// For local dev: "http://10.0.2.2:3000"` was gesturing at without wiring.
   */
  fun startImport(context: Context) {
    val authUrl = Uri.parse("${AppGraph.baseUrl}/auth/linkedin?source=android")
    val customTabsIntent = CustomTabsIntent.Builder()
      .setShowTitle(true)
      .build()
    customTabsIntent.launchUrl(context, authUrl)
  }

  fun handleAuthCallback(intent: Intent?): LinkedInResumeFields? {
    val data = intent?.data ?: return null
    val importStatus = data.getQueryParameter("linkedin_import") ?: return null
    if (importStatus != "success") return null

    val dataParam = data.getQueryParameter("data") ?: return null
    return try {
      val json = android.util.Base64.decode(dataParam, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
      val jsonString = String(json, Charsets.UTF_8)
      val jsonObj = JSONObject(jsonString)
      LinkedInResumeFields(
        name = jsonObj.optString("name", ""),
        email = jsonObj.optString("email", ""),
        photoUrl = jsonObj.optString("photoUrl").ifEmpty { null },
        given_name = jsonObj.optString("given_name", ""),
        family_name = jsonObj.optString("family_name", "")
      )
    } catch (_: Exception) {
      null
    }
  }

  fun consumeImport() {
    pendingImport.value = null
  }
}
