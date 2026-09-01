package com.resumemaster.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
  private const val RESUME_MASTER_URL = "https://YOUR_DOMAIN.com"
  // For local dev: "http://10.0.2.2:3000"

  val pendingImport = MutableStateFlow<LinkedInResumeFields?>(null)

  fun startImport(context: Context) {
    val authUrl = Uri.parse("$RESUME_MASTER_URL/auth/linkedin?source=android")
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
