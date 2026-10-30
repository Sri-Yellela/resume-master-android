package com.resumemaster.android.data.portals

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.X25519
import com.resumemaster.android.data.net.ApiClient
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The connect flow against a local stand-in for draft's routes/applyWorker.js — real ApiClient, real sealing.
 */
class PortalRepositoryTest {

  private lateinit var server: HttpServer
  private val calls = mutableListOf<String>()
  private var uploaded: String? = null

  private val worker = Ed25519Sign.KeyPair.newKeyPair()
  private val pinned = PortalCrypto.b64u(PortalCrypto.ed25519Spki(worker.publicKey))
  private val sessionPriv = X25519.generatePrivateKey()
  private val sessionPub = PortalCrypto.b64u(PortalCrypto.x25519Spki(X25519.publicFromPrivate(sessionPriv)))
  private val keyId = "sk_0011223344556677"

  /** What /connect/start answers; tests replace it to play a compromised draft. */
  private var minted: () -> JSONObject = { mintedBy(worker.privateKey, PortalCrypto.b64u(PortalCrypto.ed25519Spki(worker.publicKey))) }

  private fun mintedBy(signingKey: ByteArray, claimedIdentity: String, family: String = "amazon", pub: String = sessionPub) =
    JSONObject().put("keyId", keyId).put("publicKey", pub).put("portalFamily", family)
      .put("signature", PortalCrypto.b64u(Ed25519Sign(signingKey).sign(PortalCrypto.signedMessage(keyId, pub, family).toByteArray())))
      .put("identity", claimedIdentity)

  /** Answers for ANY url — including other sites — so a test can prove only the family's are asked for. */
  private val asked = mutableListOf<String>()
  private val cookies = CookieSource { url ->
    asked += url
    when {
      url.contains("amazon.jobs") -> "session-id=AMZ-SESSION-VALUE-1; at-main=Atza|AMZ-TOKEN-VALUE-2"
      else -> "bank=SOMEONE-ELSES-SECRET"
    }
  }

  @Before
  fun setUp() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { ex ->
      val body = ex.requestBody.bufferedReader().readText()
      val path = ex.requestURI.path
      synchronized(calls) { calls += "${ex.requestMethod} $path" }
      val (status, reply) = when ("${ex.requestMethod} $path") {
        "GET /api/auth/me" -> 200 to """{"authenticated":true,"user":{"id":42,"username":"synthetic"}}"""
        "POST /api/portals/connect/start" -> 200 to minted().toString()
        "POST /api/portals/upload" -> { uploaded = body; 201 to """{"id":7,"portals":[]}""" }
        "GET /api/portals" -> 200 to """{"worker":true,"portals":[{"id":7,"portalFamily":"amazon","originHost":"amazon.jobs","status":"active","connectedAt":1,"lastUsedAt":null,"uses":[]}],"accounts":[]}"""
        "DELETE /api/portals/7" -> 200 to """{"removed":true,"keyDestroyed":true,"portals":[]}"""
        else -> 404 to """{"error":"not_found"}"""
      }
      val bytes = reply.toByteArray()
      ex.responseHeaders.add("Content-Type", "application/json")
      ex.sendResponseHeaders(status, bytes.size.toLong())
      ex.responseBody.use { it.write(bytes) }
    }
    server.start()
  }

  @After
  fun tearDown() { server.stop(0) }

  private fun repo(identity: String = pinned) =
    PortalRepository(ApiClient("http://127.0.0.1:${server.address.port}") { "test-token" }, identity)

  @Test
  fun `connect seals the family's cookies and the upload body carries ciphertext only`() {
    val out = repo().connect(PortalFamily.AMAZON, cookies)
    assertEquals(ConnectOutcome.Connected(7), out)

    val body = JSONObject(uploaded!!)
    assertEquals(setOf("portalFamily", "originHost", "keyId", "ciphertext", "consent"), body.keys().asSequence().toSet())
    assertEquals("amazon", body.getString("portalFamily"))
    assertEquals("amazon.jobs", body.getString("originHost"))
    assertEquals(keyId, body.getString("keyId"))
    assertEquals(true, body.getBoolean("consent"))
    for (secret in listOf("AMZ-SESSION-VALUE-1", "AMZ-TOKEN-VALUE-2", "session-id", "at-main", "Atza")) {
      assertFalse("the upload carries no cookie name or value ($secret)", uploaded!!.contains(secret))
    }

    // Only the worker's session key opens it, and only for user 42 on amazon.
    val opened = JSONObject(PortalCrypto.openForTest(body.getString("ciphertext"), sessionPriv, keyId, "42|amazon|v1"))
    val list = opened.getJSONArray("cookies")
    val names = (0 until list.length()).map { list.getJSONObject(it).getString("name") + "@" + list.getJSONObject(it).getString("url") }
    assertEquals(6, names.size) // 2 cookies × the family's 3 hosts
    assertTrue(names.contains("at-main@https://account.amazon.jobs/"))
    assertFalse(opened.toString().contains("SOMEONE-ELSES-SECRET"))
    assertEquals("Atza|AMZ-TOKEN-VALUE-2", list.getJSONObject(1).getString("value"))

    assertEquals(listOf("https://www.amazon.jobs/", "https://amazon.jobs/", "https://account.amazon.jobs/"), asked)
    assertEquals(listOf("GET /api/auth/me", "POST /api/portals/connect/start", "POST /api/portals/upload"), calls)
  }

  @Test
  fun `a key signed by anyone but the pinned identity is refused before a cookie is read`() {
    // A compromised draft: signs with a key of its own and names that key as `identity` in the reply.
    val rogue = Ed25519Sign.KeyPair.newKeyPair()
    minted = { mintedBy(rogue.privateKey, PortalCrypto.b64u(PortalCrypto.ed25519Spki(rogue.publicKey))) }
    val out = repo().connect(PortalFamily.AMAZON, cookies)
    assertEquals("bad_signature", (out as ConnectOutcome.Refused).reason)
    assertTrue("no cookie was read", asked.isEmpty())
    assertNull("nothing was uploaded", uploaded)
    assertFalse(calls.contains("POST /api/portals/upload"))
  }

  @Test
  fun `a validly signed key minted for another family is refused`() {
    minted = { mintedBy(worker.privateKey, pinned, family = "workday") }
    val out = repo().connect(PortalFamily.AMAZON, cookies)
    assertEquals("bad_signature", (out as ConnectOutcome.Refused).reason)
    assertTrue(asked.isEmpty())
    assertNull(uploaded)
  }

  @Test
  fun `with no pinned identity the feature is not offered and nothing is called`() {
    val r = repo(identity = "")
    assertFalse(r.isOffered)
    assertEquals("not_pinned", (r.connect(PortalFamily.AMAZON, cookies) as ConnectOutcome.Refused).reason)
    assertTrue(calls.isEmpty())
    assertTrue(asked.isEmpty())
    assertFalse("a malformed pin is no pin", repo(identity = "not-a-key").isOffered)
    assertTrue(repo().isOffered)
  }

  @Test
  fun `signed in to the portal with no cookies sends nothing`() {
    val out = repo().connect(PortalFamily.AMAZON) { null }
    assertEquals("not_signed_in", (out as ConnectOutcome.Refused).reason)
    assertNull(uploaded)
  }

  @Test
  fun `list and disconnect`() {
    val snap = repo().list().getOrThrow()
    assertTrue(snap.worker)
    assertEquals(7L, snap.liveFor(PortalFamily.AMAZON)?.id)
    assertTrue(repo().disconnect(7).getOrThrow().keyDestroyed)
    assertTrue(calls.contains("DELETE /api/portals/7"))
  }
}
