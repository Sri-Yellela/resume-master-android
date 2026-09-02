package com.resumemaster.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * The mobile bearer credential, at rest.
 *
 * ── WHICH TOKEN GOES IN HERE ────────────────────────────────────────────────────────────────────
 *
 * NOT the one POST /api/auth/login returns. That one stores `session_sid = req.sessionID`
 * server-side, and `revokeBrowserAuthContexts` sweeps every token sharing a sid. For a cookie-less
 * client that sid is a throwaway it will never present again, so the binding is not merely useless
 * — it files the phone's credential under a session nobody can sign out of or audit, and the phone
 * gets signed out at an unpredictable moment by an unrelated browser action.
 *
 * The credential is the one from GET /api/auth/mobile-token, which mints with `sessionLess: true`
 * and stores session_sid NULL — a row that sweep deliberately never touches. AuthRepository does
 * the exchange and only ever hands THAT token to this class.
 *
 * ── WHY THE FILE NAME IS LOAD-BEARING ───────────────────────────────────────────────────────────
 *
 * "rm_secure_prefs" is named in three places besides this one: backup_rules.xml (API 28-30),
 * and both <cloud-backup> and <device-transfer> in data_extraction_rules.xml. The manifest sets
 * android:allowBackup="true", so an un-excluded SharedPreferences file is copied to Google cloud
 * backup BY DEFAULT. Renaming this constant without editing those two XML files would silently
 * start backing up a live credential, and nothing would fail or warn.
 *
 * EncryptedSharedPreferences is Keystore-backed, so the bytes at rest are ciphertext and the
 * wrapping key is hardware-bound and non-exportable. That is defence in depth, NOT the reason the
 * excludes can be skipped: ciphertext leaving the device is still a credential leaving the device.
 */
class SecureTokenStore(context: Context) {

  companion object {
    /** Must stay in step with backup_rules.xml and data_extraction_rules.xml. */
    const val PREFS_FILE_NAME = "rm_secure_prefs"

    private const val KEY_TOKEN = "mobile_token"
    private const val KEY_ISSUED_AT_MS = "mobile_token_issued_at_ms"
    private const val KEY_ABSOLUTE_SECONDS = "mobile_token_absolute_seconds"
  }

  private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
    context,
    PREFS_FILE_NAME,
    MasterKey.Builder(context)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
  )

  /**
   * The stored token, or null.
   *
   * Returns null once the ABSOLUTE window has passed, without a network round trip. The idle window
   * is NOT enforced here on purpose: it slides on every authenticated request, so the server is the
   * only party that knows where it currently sits, and a client second-guessing it would sign a
   * user out of an app they are actively using. The absolute window is different — it is measured
   * from issue and never moves, so the client can be certain.
   */
  fun token(): String? {
    val token = prefs.getString(KEY_TOKEN, null) ?: return null
    val issuedAtMs = prefs.getLong(KEY_ISSUED_AT_MS, 0L)
    val absoluteSeconds = prefs.getLong(KEY_ABSOLUTE_SECONDS, 0L)
    if (issuedAtMs > 0L && absoluteSeconds > 0L) {
      val ageSeconds = (System.currentTimeMillis() - issuedAtMs) / 1000L
      if (ageSeconds >= absoluteSeconds) {
        clear()
        return null
      }
    }
    return token
  }

  fun hasToken(): Boolean = token() != null

  fun save(token: String, absoluteSeconds: Long) {
    prefs.edit()
      .putString(KEY_TOKEN, token)
      .putLong(KEY_ISSUED_AT_MS, System.currentTimeMillis())
      .putLong(KEY_ABSOLUTE_SECONDS, absoluteSeconds)
      .commit()
  }

  /**
   * Forget the token locally.
   *
   * `commit()` rather than `apply()`, deliberately: sign-out is usually followed by navigation and
   * may be followed by process death, and an asynchronous write that loses the race leaves a live
   * credential on disk after the UI has said it is gone.
   */
  fun clear() {
    prefs.edit()
      .remove(KEY_TOKEN)
      .remove(KEY_ISSUED_AT_MS)
      .remove(KEY_ABSOLUTE_SECONDS)
      .commit()
  }
}
