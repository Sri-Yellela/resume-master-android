package com.resumemaster.android.data.portals

import com.google.crypto.tink.subtle.Ed25519Verify
import com.google.crypto.tink.subtle.Hkdf
import com.google.crypto.tink.subtle.X25519
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * D100 P3 / D112 item 7 — the phone's half of the apply worker's sealed sessions.
 *
 * This is draft's `apply-worker/lib/keys.js` `seal()` and `verifyMinted()`, step for step (the extension's
 * `extension/applyAway.js` is the other client of the same scheme). ONLY the worker's `unseal()` can open what
 * this produces; draft's server stores it and cannot:
 *
 *   "v1." + epk + "." + iv + "." + tag + "." + ct       (each part base64url, unpadded)
 *   epk  the ephemeral X25519 public key, SPKI DER (44 bytes)
 *   key  HKDF-SHA256(X25519(eph, session), salt = keyId, info = "draft-apply-worker/session/v1") → 32 bytes
 *   aad  "user|portal|v1" — so a sealed session cannot be replayed for another user or another portal
 *
 * ── WHY TINK'S `subtle` PRIMITIVES AND NOT THE PLATFORM'S ───────────────────────────────────────────────
 * minSdk is 28. The platform provider gains X25519 key agreement and Ed25519 signatures only in later API
 * levels, so `KeyAgreement.getInstance("XDH")` would work on the JVM test runner and throw on a real Android 9
 * phone — a test that passes for the wrong reason. Tink is ALREADY on this app's runtime classpath (security-
 * crypto, which SecureTokenStore uses, depends on tink-android); its X25519 / Ed25519 / HKDF are pure Java and
 * behave identically on both. It is now declared directly so it cannot vanish with a security-crypto bump.
 * AES-256-GCM is the platform's (available since API 19) and the JVM's alike.
 */
object PortalCrypto {

  private const val INFO = "draft-apply-worker/session/v1"
  private const val GCM_TAG_BITS = 128

  /** SPKI DER prefixes: SEQUENCE { SEQUENCE { OID 1.3.101.110 | 1.3.101.112 }, BIT STRING (32 bytes) }. */
  private val X25519_SPKI_PREFIX = hex("302a300506032b656e032100")
  private val ED25519_SPKI_PREFIX = hex("302a300506032b6570032100")

  private val random = SecureRandom()

  /** keys.js signedMessage — the exact bytes the worker's identity signs. */
  fun signedMessage(keyId: String, publicKey: String, portalFamily: String): String =
    "draft-apply-worker/v1|$keyId|$publicKey|$portalFamily"

  /** keys.js aadFor — user · portal · version. The user id is draft's integer id. */
  fun aadFor(userId: Long, portalFamily: String): String = "$userId|$portalFamily|v1"

  fun b64u(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

  /** Strict: anything not base64url throws (the caller turns that into "refused", never a crash). */
  fun fromB64u(s: String): ByteArray = Base64.getUrlDecoder().decode(s.trimEnd('='))

  /** The raw 32-byte key inside an SPKI DER, or null when it is not exactly that shape for that algorithm. */
  fun rawFromSpki(spki: ByteArray, prefix: ByteArray): ByteArray? {
    if (spki.size != prefix.size + 32) return null
    for (i in prefix.indices) if (spki[i] != prefix[i]) return null
    return spki.copyOfRange(prefix.size, spki.size)
  }

  fun x25519Spki(raw: ByteArray): ByteArray = X25519_SPKI_PREFIX + raw
  fun ed25519Spki(raw: ByteArray): ByteArray = ED25519_SPKI_PREFIX + raw

  /** Is `identity` a well-formed Ed25519 public key (SPKI DER, base64url)? An empty pin is not. */
  fun isIdentityWellFormed(identity: String?): Boolean {
    if (identity.isNullOrBlank()) return false
    return try { rawFromSpki(fromB64u(identity), ED25519_SPKI_PREFIX) != null } catch (_: IllegalArgumentException) { false }
  }

  /**
   * ⛔ Is the minted session key signed by `identity` — which MUST be the PINNED identity, never the
   * `identity` field draft's reply carries (a compromised draft would simply sign with a key of its own).
   * Never throws: anything malformed is false.
   */
  fun verifyMinted(identity: String, keyId: String, publicKey: String, portalFamily: String, signature: String): Boolean {
    return try {
      if (identity.isBlank() || keyId.isBlank() || publicKey.isBlank() || signature.isBlank()) return false
      val raw = rawFromSpki(fromB64u(identity), ED25519_SPKI_PREFIX) ?: return false
      val sig = fromB64u(signature)
      if (sig.size != 64) return false
      Ed25519Verify(raw).verify(sig, signedMessage(keyId, publicKey, portalFamily).toByteArray(Charsets.UTF_8))
      true
    } catch (_: GeneralSecurityException) {
      false
    } catch (_: IllegalArgumentException) {
      false
    }
  }

  /**
   * Seal `plaintext` to the worker's per-session X25519 key (`publicKey`, SPKI DER base64url).
   * `ephemeralPrivate` and `iv` are parameters only so a test can pin them; production passes neither.
   */
  fun seal(
    publicKey: String,
    keyId: String,
    plaintext: String,
    aad: String,
    ephemeralPrivate: ByteArray = X25519.generatePrivateKey(),
    iv: ByteArray = ByteArray(12).also(random::nextBytes),
  ): String {
    val peer = rawFromSpki(fromB64u(publicKey), X25519_SPKI_PREFIX)
      ?: throw GeneralSecurityException("the minted key is not an X25519 public key")
    require(iv.size == 12) { "a GCM nonce is 12 bytes" }
    val shared = X25519.computeSharedSecret(ephemeralPrivate, peer)
    val key = Hkdf.computeHkdf("HMACSHA256", shared, keyId.toByteArray(Charsets.UTF_8), INFO.toByteArray(Charsets.UTF_8), 32)
    shared.fill(0)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
    cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
    val out = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    key.fill(0)
    // The JCA appends the 16-byte tag to the ciphertext; the worker's format carries them apart.
    val ct = out.copyOfRange(0, out.size - 16)
    val tag = out.copyOfRange(out.size - 16, out.size)
    val epk = x25519Spki(X25519.publicFromPrivate(ephemeralPrivate))
    return listOf("v1", b64u(epk), b64u(iv), b64u(tag), b64u(ct)).joinToString(".")
  }

  /**
   * The worker's `unseal()`, for TESTS ONLY — the app never holds a session private key, so nothing in the app
   * calls this. It lives here so the round-trip test exercises the same parsing the worker does.
   */
  internal fun openForTest(sealed: String, sessionPrivate: ByteArray, keyId: String, aad: String): String {
    val parts = sealed.split(".")
    if (parts.size != 5 || parts[0] != "v1") throw GeneralSecurityException("unknown sealed format")
    val epk = rawFromSpki(fromB64u(parts[1]), X25519_SPKI_PREFIX) ?: throw GeneralSecurityException("bad epk")
    val shared = X25519.computeSharedSecret(sessionPrivate, epk)
    val key = Hkdf.computeHkdf("HMACSHA256", shared, keyId.toByteArray(Charsets.UTF_8), INFO.toByteArray(Charsets.UTF_8), 32)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, fromB64u(parts[2])))
    cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
    return String(cipher.doFinal(fromB64u(parts[4]) + fromB64u(parts[3])), Charsets.UTF_8)
  }

  private fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
