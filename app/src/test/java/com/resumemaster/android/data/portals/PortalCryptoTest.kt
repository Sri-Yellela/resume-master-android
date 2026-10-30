package com.resumemaster.android.data.portals

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.X25519
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import javax.crypto.AEADBadTagException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The sealing scheme, held to draft's apply-worker/lib/keys.js — the worker's `unseal()` is the only reader.
 */
class PortalCryptoTest {

  private fun sessionKeyPair(): Pair<ByteArray, String> {
    val priv = X25519.generatePrivateKey()
    return priv to PortalCrypto.b64u(PortalCrypto.x25519Spki(X25519.publicFromPrivate(priv)))
  }

  @Test
  fun `a sealed session round-trips in the worker's exact format`() {
    val (priv, pub) = sessionKeyPair()
    val keyId = "sk_0123456789abcdef0123456789abcdef"
    val aad = PortalCrypto.aadFor(42, "amazon")
    assertEquals("42|amazon|v1", aad)
    val plaintext = """{"cookies":[{"name":"session-token","value":"s3cr3t","url":"https://www.amazon.jobs/"}]}"""

    val sealed = PortalCrypto.seal(pub, keyId, plaintext, aad)

    val parts = sealed.split(".")
    assertEquals(5, parts.size)
    assertEquals("v1", parts[0])
    val epk = PortalCrypto.fromB64u(parts[1])
    assertEquals("the ephemeral key is an X25519 SPKI DER", 44, epk.size)
    assertTrue(PortalCrypto.rawFromSpki(epk, PortalCrypto.x25519Spki(ByteArray(32)).copyOfRange(0, 12)) != null)
    assertEquals("a 12-byte GCM nonce", 12, PortalCrypto.fromB64u(parts[2]).size)
    assertEquals("a 16-byte GCM tag", 16, PortalCrypto.fromB64u(parts[3]).size)
    assertTrue("every part is unpadded base64url", parts.all { Regex("^[A-Za-z0-9_-]+$").matches(it) })
    assertFalse("nothing of the plaintext shows", sealed.contains("s3cr3t"))

    assertEquals(plaintext, PortalCrypto.openForTest(sealed, priv, keyId, aad))
  }

  @Test
  fun `the binding holds — another user, portal or key id cannot open it`() {
    val (priv, pub) = sessionKeyPair()
    val keyId = "sk_aaaaaaaaaaaaaaaa"
    val sealed = PortalCrypto.seal(pub, keyId, "{}", PortalCrypto.aadFor(1, "amazon"))
    for ((k, aad) in listOf(keyId to "2|amazon|v1", keyId to "1|workday|v1", "sk_bbbbbbbbbbbbbbbb" to "1|amazon|v1")) {
      try { PortalCrypto.openForTest(sealed, priv, k, aad); fail("opened with $k / $aad") } catch (_: AEADBadTagException) {}
    }
    val (otherPriv, _) = sessionKeyPair()
    try { PortalCrypto.openForTest(sealed, otherPriv, keyId, "1|amazon|v1"); fail("opened with another key") } catch (_: AEADBadTagException) {}
  }

  @Test
  fun `each seal uses a fresh ephemeral key and nonce`() {
    val (_, pub) = sessionKeyPair()
    val a = PortalCrypto.seal(pub, "sk_aaaaaaaaaaaaaaaa", "{}", "1|amazon|v1").split(".")
    val b = PortalCrypto.seal(pub, "sk_aaaaaaaaaaaaaaaa", "{}", "1|amazon|v1").split(".")
    assertNotEquals(a[1], b[1])
    assertNotEquals(a[2], b[2])
  }

  @Test
  fun `a minted key signed by the pinned identity verifies and anything else is refused`() {
    val id = Ed25519Sign.KeyPair.newKeyPair()
    val identity = PortalCrypto.b64u(PortalCrypto.ed25519Spki(id.publicKey))
    val (_, pub) = sessionKeyPair()
    val keyId = "sk_cccccccccccccccc"
    val sig = PortalCrypto.b64u(Ed25519Sign(id.privateKey).sign(PortalCrypto.signedMessage(keyId, pub, "amazon").toByteArray()))
    assertTrue(PortalCrypto.isIdentityWellFormed(identity))
    assertTrue(PortalCrypto.verifyMinted(identity, keyId, pub, "amazon", sig))

    val (_, otherPub) = sessionKeyPair()
    assertFalse("a substituted session key", PortalCrypto.verifyMinted(identity, keyId, otherPub, "amazon", sig))
    assertFalse("another key id", PortalCrypto.verifyMinted(identity, "sk_dddddddddddddddd", pub, "amazon", sig))
    assertFalse("another family", PortalCrypto.verifyMinted(identity, keyId, pub, "workday", sig))
    val impostor = Ed25519Sign.KeyPair.newKeyPair()
    val impostorSig = PortalCrypto.b64u(Ed25519Sign(impostor.privateKey).sign(PortalCrypto.signedMessage(keyId, pub, "amazon").toByteArray()))
    assertFalse("signed by a key that is not the pinned one", PortalCrypto.verifyMinted(identity, keyId, pub, "amazon", impostorSig))
    assertFalse("malformed signature", PortalCrypto.verifyMinted(identity, keyId, pub, "amazon", "not*base64"))
    assertFalse("short signature", PortalCrypto.verifyMinted(identity, keyId, pub, "amazon", PortalCrypto.b64u(ByteArray(10))))
    assertFalse("empty pin", PortalCrypto.verifyMinted("", keyId, pub, "amazon", sig))
    assertFalse("an X25519 key is not an identity", PortalCrypto.verifyMinted(pub, keyId, pub, "amazon", sig))
    assertFalse(PortalCrypto.isIdentityWellFormed(""))
    assertFalse(PortalCrypto.isIdentityWellFormed(pub))
  }

  /**
   * INTEROP WITH THE WORKER ITSELF: draft's apply-worker/lib/keys.js (read-only) mints a session key with its
   * Ed25519 identity; this JVM code verifies that signature, seals, and the worker's own `unseal()` opens it.
   * Skipped — with the reason — when node or the draft checkout is not on this machine.
   */
  @Test
  fun `the worker's own keys_js verifies and opens what the app seals`() {
    val keysJs = File(System.getenv("DRAFT_KEYS_JS") ?: "C:/Users/duggi/WebstormProjects/draft/apply-worker/lib/keys.js")
    assumeTrue("draft's apply-worker/lib/keys.js is not on this machine", keysJs.isFile)
    val node = runCatching { ProcessBuilder("node", "--version").start().waitFor(20, TimeUnit.SECONDS) }.getOrDefault(false)
    assumeTrue("node is not on PATH", node)

    val tmp = Files.createTempDirectory("portal-interop").toFile()
    try {
      val store = File(tmp, "store").absolutePath.replace('\\', '/')
      val master = java.util.Base64.getEncoder().encodeToString(ByteArray(32) { (it * 7 + 3).toByte() })
      val keysUrl = keysJs.toURI().toString()
      val script = File(tmp, "interop.mjs")
      script.writeText(
        """
        import { createKeyStore, verifyMinted } from ${JSONObject.quote(keysUrl)};
        const store = createKeyStore({ dir: ${JSONObject.quote(store)}, master: ${JSONObject.quote(master)} });
        const [cmd, a, b, c] = process.argv.slice(2);
        if (cmd === "mint") process.stdout.write(JSON.stringify(store.mint("amazon")));
        else process.stdout.write(JSON.stringify({ plaintext: store.unseal({ keyId: a, sealed: b, aad: c }) }));
        """.trimIndent()
      )
      fun run(vararg args: String): JSONObject {
        val p = ProcessBuilder(listOf("node", script.absolutePath) + args).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        assertTrue("node exited: $out", p.waitFor(60, TimeUnit.SECONDS) && p.exitValue() == 0)
        return JSONObject(out)
      }

      val minted = run("mint")
      val identity = minted.getString("identity") // the test plays the owner pinning THIS worker's identity
      assertTrue(PortalCrypto.isIdentityWellFormed(identity))
      assertTrue("the JVM verifies the worker's Ed25519 signature",
        PortalCrypto.verifyMinted(identity, minted.getString("keyId"), minted.getString("publicKey"), "amazon", minted.getString("signature")))

      val plaintext = FamilyCookies.plaintext(listOf(EngineCookie("at-main", "Atza|synthetic", "https://www.amazon.jobs/")))
      val aad = PortalCrypto.aadFor(42, "amazon")
      val sealed = PortalCrypto.seal(minted.getString("publicKey"), minted.getString("keyId"), plaintext, aad)

      val opened = run("unseal", minted.getString("keyId"), sealed, aad)
      assertEquals("the worker's unseal() opens the app's ciphertext", plaintext, opened.getString("plaintext"))
      // and the worker reads it as its fill engine expects: storageState.cookies
      assertEquals("at-main", JSONObject(opened.getString("plaintext")).getJSONArray("cookies").getJSONObject(0).getString("name"))
    } finally {
      tmp.deleteRecursively()
    }
  }
}
