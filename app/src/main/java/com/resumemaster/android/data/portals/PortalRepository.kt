package com.resumemaster.android.data.portals

import com.resumemaster.android.data.net.ApiClient
import com.resumemaster.android.data.net.ApiResponse
import org.json.JSONObject

/** One row of GET /api/portals → `portals[]` (services/applyWorker.js listPortals). Never the ciphertext. */
data class ConnectedPortal(
  val id: Long,
  val portalFamily: String,
  val originHost: String,
  /** active · expiring · expired · removed */
  val status: String,
  val connectedAt: Long?,
  val lastUsedAt: Long?,
) {
  val isLive: Boolean get() = status == "active" || status == "expiring"
}

data class PortalsSnapshot(val worker: Boolean, val portals: List<ConnectedPortal>) {
  /** The family's live session, if any — one per family per user on the server. */
  fun liveFor(family: PortalFamily): ConnectedPortal? = portals.firstOrNull { it.portalFamily == family.ats && it.isLive }
}

sealed class ConnectOutcome {
  data class Connected(val id: Long?) : ConnectOutcome()
  /** `reason` is for code and tests; `say` is what the user reads. Every refusal sent NOTHING about the portal. */
  data class Refused(val reason: String, val say: String) : ConnectOutcome()
}

data class DisconnectOutcome(val keyDestroyed: Boolean)

/**
 * D100 P3 / D112 item 7 — the user's portals, through draft's own API with the app's bearer token.
 *
 * ⚠ NOT IN THE VENDORED MOBILE CONTRACT (contract/mobile-api.v1.json, 1.2.0). The four routes are draft's
 * `routes/applyWorker.js`; they are called here through ApiClient and parsed leniently. The contract must add
 * GET /api/portals, POST /api/portals/connect/start, POST /api/portals/upload and DELETE /api/portals/{id}
 * before this ships — see the commit that introduced this file.
 *
 * The order of [connect] is the security argument, and the same as the extension's connectAway:
 *   who am I → mint → ⛔ VERIFY against the PINNED identity → read the family's cookies → seal → upload.
 * Nothing about the portal is read until the key is verified, and nothing leaves unsealed.
 *
 * Blocking: call from Dispatchers.IO.
 */
class PortalRepository(
  private val api: ApiClient,
  private val pinnedIdentity: String,
) {

  /** ⛔ The feature exists only with a well-formed pinned identity. Empty pin → no row, no screen, no button. */
  val isOffered: Boolean = PortalCrypto.isIdentityWellFormed(pinnedIdentity)

  fun list(): Result<PortalsSnapshot> = runCatching {
    val r = api.get("/api/portals")
    if (!r.isSuccess) throw PortalApiException(r.status, messageOf(r, "Could not load your portals."))
    parseSnapshot(r.body)
  }

  fun connect(family: PortalFamily, cookies: CookieSource): ConnectOutcome {
    if (!isOffered) return ConnectOutcome.Refused("not_pinned", "This version of draft cannot connect a portal yet.")
    if (PortalFamily.byAts(family.ats) != family) return ConnectOutcome.Refused("not_a_portal", "draft cannot connect this site.")

    val me = runCatching { api.get("/api/auth/me") }.getOrNull()
    val user = me?.takeIf { it.isSuccess }?.let { runCatching { JSONObject(it.body) }.getOrNull() }
    val userId = user?.takeIf { it.optBoolean("authenticated") }?.optJSONObject("user")?.let { idOf(it.opt("id")) }
      ?: return ConnectOutcome.Refused("signed_out", "Sign in to draft first — nothing was sent.")

    val start = runCatching {
      api.post("/api/portals/connect/start", JSONObject().put("portalFamily", family.ats).toString())
    }.getOrNull()
    val minted = start?.let { runCatching { JSONObject(it.body) }.getOrNull() }
    if (start == null || !start.isSuccess || minted == null) {
      return ConnectOutcome.Refused("worker_unavailable",
        minted?.optString("message")?.ifBlank { null } ?: "draft's apply worker could not be reached — nothing was read.")
    }
    val keyId = minted.optString("keyId")
    val publicKey = minted.optString("publicKey")
    // ⛔ The PINNED identity — never minted.identity — and the key must be minted for THIS family.
    val verified = minted.optString("portalFamily") == family.ats &&
      PortalCrypto.verifyMinted(pinnedIdentity, keyId, publicKey, family.ats, minted.optString("signature"))
    if (!verified) {
      return ConnectOutcome.Refused("bad_signature", "The apply worker's key could not be verified — nothing was read or sent.")
    }

    val read = FamilyCookies.read(family, cookies)
    if (read.isEmpty()) {
      return ConnectOutcome.Refused("not_signed_in", "Sign in to ${family.name} first — nothing was sent.")
    }
    val ciphertext = try {
      PortalCrypto.seal(publicKey, keyId, FamilyCookies.plaintext(read), PortalCrypto.aadFor(userId, family.ats))
    } catch (_: Exception) {
      return ConnectOutcome.Refused("seal_failed", "Your sign-in could not be sealed — nothing was sent.")
    }

    val up = runCatching { api.post("/api/portals/upload", uploadBody(family, keyId, ciphertext)) }.getOrNull()
    if (up == null || !up.isSuccess) {
      return ConnectOutcome.Refused("upload_failed", up?.let { messageOf(it, "") }?.ifBlank { null }
        ?: "Could not save the sealed sign-in — try again.")
    }
    val id = runCatching { JSONObject(up.body).opt("id") }.getOrNull()?.let(::idOf)
    return ConnectOutcome.Connected(id)
  }

  fun disconnect(portalId: Long): Result<DisconnectOutcome> = runCatching {
    val r = api.delete("/api/portals/$portalId")
    if (!r.isSuccess) throw PortalApiException(r.status, messageOf(r, "Could not disconnect — try again."))
    DisconnectOutcome(keyDestroyed = runCatching { JSONObject(r.body).optBoolean("keyDestroyed") }.getOrDefault(false))
  }

  companion object {
    /**
     * The upload body: the family, its host, the minted key id, the CIPHERTEXT, and consent. Nothing else — no
     * cookie name or value, no URL the user visited. `expiresAt` is omitted: CookieManager does not expose expiry.
     */
    fun uploadBody(family: PortalFamily, keyId: String, ciphertext: String): String = JSONObject()
      .put("portalFamily", family.ats)
      .put("originHost", family.originHost)
      .put("keyId", keyId)
      .put("ciphertext", ciphertext)
      .put("consent", true)
      .toString()

    fun parseSnapshot(body: String): PortalsSnapshot {
      val o = JSONObject(body)
      val arr = o.optJSONArray("portals")
      val rows = (0 until (arr?.length() ?: 0)).mapNotNull { i ->
        val p = arr!!.optJSONObject(i) ?: return@mapNotNull null
        val id = idOf(p.opt("id")) ?: return@mapNotNull null
        ConnectedPortal(
          id = id,
          portalFamily = p.optString("portalFamily"),
          originHost = p.optString("originHost"),
          status = p.optString("status"),
          connectedAt = idOf(p.opt("connectedAt")),
          lastUsedAt = idOf(p.opt("lastUsedAt")),
        )
      }
      return PortalsSnapshot(worker = o.optBoolean("worker"), portals = rows)
    }

    private fun idOf(v: Any?): Long? = when (v) {
      is Number -> v.toLong()
      is String -> v.toLongOrNull()
      else -> null
    }

    private fun messageOf(r: ApiResponse, fallback: String): String =
      runCatching { JSONObject(r.body).optString("message") }.getOrNull()?.ifBlank { null } ?: fallback
  }
}

class PortalApiException(val status: Int, message: String) : Exception(message)
