package com.resumemaster.android.data

import com.resumemaster.android.data.net.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AuthedUser(val id: String, val username: String, val email: String, val name: String?)

/**
 * Sign-in, and the credential exchange that makes a phone's session durable.
 *
 * ── THE SEQUENCE, AND WHY THE MIDDLE STEP EXISTS ────────────────────────────────────────────────
 *
 *   POST /api/auth/login        -> authContext   SESSION-BOUND. Used once, in memory, never stored.
 *   GET  /api/auth/mobile-token -> token         sessionLess, durable. THIS is what gets persisted.
 *
 * The login-issued token stores `session_sid = req.sessionID` server-side, and
 * `revokeBrowserAuthContexts` revokes every token sharing a sid. A phone presents no cookie, so
 * that sid is a throwaway — persisting the login token files the phone's credential under a session
 * nobody can sign out of or audit, and produces sign-outs that are intermittent (they fire when
 * some unrelated browser session is revoked) and untraceable (nothing on the phone caused them).
 *
 * The mobile mint stores session_sid NULL, which that sweep deliberately never touches.
 *
 * This is a two-call sign-in and not one call, so the failure in between is a real state: if the
 * exchange fails after the login succeeded, we are authenticated on the server and hold nothing
 * durable. `signIn` treats that as a FAILED sign-in and stores nothing, rather than reporting
 * success and leaving the app to discover on the next launch that it has no credential.
 */
class AuthRepository(
  private val tokenStore: SecureTokenStore,
  private val baseUrl: String = ApiClient.DEFAULT_BASE_URL,
) {

  /** Unauthenticated client: no bearer, because there is not one yet. */
  private val anonymous = ApiClient(baseUrl) { null }

  /** Authenticated client. Reads the store on EVERY call so a sign-out takes effect immediately. */
  val authed = ApiClient(baseUrl) { tokenStore.token() }

  fun isSignedIn(): Boolean = tokenStore.hasToken()

  /**
   * Sign in with a USERNAME, not an email.
   *
   * The server authenticates through passport-local with its default field names, and the contract
   * types the body as { username, password }. Sending `email` produces a 401 that looks exactly
   * like a wrong password -- the same wrong-key-name defect that made the old job parser read
   * snake_case, except this one fails loudly instead of silently.
   */
  suspend fun signIn(username: String, password: String): Result<AuthedUser> =
    withContext(Dispatchers.IO) {
      try {
        val loginBody = JSONObject()
          .put("username", username)
          .put("password", password)
          .toString()

        val login = anonymous.post("/api/auth/login", loginBody)
        if (!login.isSuccess) {
          return@withContext Result.failure(
            Exception(errorMessageOf(login.body, "Sign-in failed (${login.status})"))
          )
        }

        val loginJson = JSONObject(login.body)
        val sessionBoundToken = loginJson.optString("authContext", "")
        if (sessionBoundToken.isEmpty()) {
          return@withContext Result.failure(Exception("Sign-in returned no authContext."))
        }

        val user = loginJson.optJSONObject("user")

        // The exchange. The session-bound token authorises exactly this one call and is then
        // discarded — it is never handed to tokenStore and never leaves this function.
        val exchangeClient = ApiClient(baseUrl) { sessionBoundToken }
        val exchange = exchangeClient.get("/api/auth/mobile-token")
        if (!exchange.isSuccess) {
          return@withContext Result.failure(
            Exception(
              errorMessageOf(
                exchange.body,
                "Signed in, but could not obtain a durable mobile credential (${exchange.status})."
              )
            )
          )
        }

        val exchangeJson = JSONObject(exchange.body)
        val durableToken = exchangeJson.optString("token", "")
        if (durableToken.isEmpty()) {
          return@withContext Result.failure(Exception("Token exchange returned no token."))
        }

        tokenStore.save(
          token = durableToken,
          // Falls back to the contract's documented 90 days if the server omits it. Zero would
          // make token() treat the credential as instantly expired.
          absoluteSeconds = exchangeJson.optLong("absoluteSeconds", 7_776_000L),
        )

        Result.success(
          AuthedUser(
            id = user?.optString("id", "").orEmpty(),
            username = user?.optString("username", username) ?: username,
            email = user?.optString("email", "").orEmpty(),
            name = user?.optString("name", "")?.ifEmpty { null },
          )
        )
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  /**
   * Sign out.
   *
   * The local token is cleared EVEN IF the server call fails. A user who taps sign out has withdrawn
   * consent for this device to hold their credential, and honouring that must not depend on
   * connectivity. The server-side revoke is best-effort and keyed on
   * user_agent='resume-master-mobile', so it does not disturb the extension's independent token.
   */
  suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
    val outcome = try {
      val response = authed.post("/api/auth/revoke-mobile-token")
      if (response.isSuccess) Result.success(Unit)
      else Result.failure(Exception("Server did not confirm revocation (${response.status})."))
    } catch (e: Exception) {
      Result.failure(e)
    }
    tokenStore.clear()
    outcome
  }

  private fun errorMessageOf(body: String, fallback: String): String = try {
    JSONObject(body).optString("error", "").ifEmpty { fallback }
  } catch (_: Exception) {
    fallback
  }
}
