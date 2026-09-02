package com.resumemaster.android.data.net

import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A response that has not yet been interpreted: status plus raw body, including on an error. */
data class ApiResponse(val status: Int, val body: String) {
  val isSuccess: Boolean get() = status in 200..299
  val isUnauthorized: Boolean get() = status == 401
}

class ApiException(val status: Int, message: String) : IOException(message)

/**
 * The one place this app talks HTTP.
 *
 * ── THREE THINGS THE PREVIOUS PARSER GOT WRONG THAT LIVE HERE, NOT IN THE REPOSITORY ────────────
 *
 * 1. IT CAST TO HttpsURLConnection. `openConnection() as HttpsURLConnection` throws
 *    ClassCastException against http://10.0.2.2:3001 — the loopback address an emulator uses to
 *    reach the developer's own machine — so the local-dev base URL written in a comment beside it
 *    could never have worked. HttpURLConnection is the supertype of both and is what is used here.
 *
 * 2. IT SENT NO Authorization HEADER to a requireAuth endpoint. GET /api/jobs is behind
 *    requireAuth; the only public feed is GET /api/jobs/generic. Every board request was therefore
 *    a 401 that the caller reported as a generic failure. The header is injected HERE for every
 *    request, so no call site can forget it.
 *
 * 3. IT READ ONLY inputStream. On any 4xx/5xx HttpURLConnection throws from inputStream and puts
 *    the body on errorStream, so the server's own explanation — the thing that says WHICH of the
 *    three auth failure modes happened — was discarded and replaced by a stack trace. Both streams
 *    are read here and the status is returned rather than thrown, so a caller can distinguish 401
 *    (sign in again) from 500 (do not sign the user out).
 */
class ApiClient(
  private val baseUrl: String = DEFAULT_BASE_URL,
  private val tokenProvider: () -> String? = { null },
) {

  companion object {
    const val DEFAULT_BASE_URL = "https://resumemaster.one"

    /**
     * 10.0.2.2 is the host loopback as seen from inside the emulator. NOT 127.0.0.1, which is the
     * emulated device itself.
     */
    const val EMULATOR_HOST_BASE_URL = "http://10.0.2.2:3001"

    /**
     * Identifies this client to the server. The mobile token is revoked by a statement keyed on
     * user_agent='resume-master-mobile', and the extension's revoke is keyed the same way on its
     * own value — the two credentials are independently revocable ONLY while these strings differ.
     */
    const val USER_AGENT = "resume-master-mobile"

    private const val TIMEOUT_MS = 15_000

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
  }

  fun get(path: String, query: List<Pair<String, String>> = emptyList()): ApiResponse =
    request("GET", path, query, null)

  fun post(path: String, jsonBody: String? = null): ApiResponse =
    request("POST", path, emptyList(), jsonBody)

  fun patch(path: String, jsonBody: String? = null): ApiResponse =
    request("PATCH", path, emptyList(), jsonBody)

  private fun request(
    method: String,
    path: String,
    query: List<Pair<String, String>>,
    jsonBody: String?,
  ): ApiResponse {
    val queryString =
      if (query.isEmpty()) ""
      else "?" + query.joinToString("&") { (k, v) -> "$k=${encode(v)}" }

    val connection = URL("$baseUrl$path$queryString").openConnection() as HttpURLConnection
    try {
      // PATCH is set directly. Android's HttpURLConnection is OkHttp-backed and accepts it; the
      // desktop JDK's does not, which is why no unit test here performs real I/O.
      //
      // The obvious portable workaround -- POST plus X-HTTP-Method-Override -- was CHECKED AND
      // REJECTED: this server mounts no method-override middleware (nothing in server.js reads
      // that header), so such a request would arrive as a POST to a PATCH-only route and 404.
      // A fallback that quietly does the wrong verb is worse than one that does not exist.
      connection.requestMethod = method
      connection.connectTimeout = TIMEOUT_MS
      connection.readTimeout = TIMEOUT_MS
      connection.setRequestProperty("Accept", "application/json")
      connection.setRequestProperty("User-Agent", USER_AGENT)

      tokenProvider()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }

      if (jsonBody != null) {
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
      }

      val status = connection.responseCode
      val stream = if (status in 200..299) connection.inputStream else connection.errorStream
      val body = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
      return ApiResponse(status, body)
    } finally {
      connection.disconnect()
    }
  }
}
