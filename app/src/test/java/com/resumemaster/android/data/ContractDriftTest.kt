package com.resumemaster.android.data

import com.resumemaster.android.data.contract.ContractJob
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The vendored contract, and the model generated from it.
 *
 * These are the guards that make "generated from the contract" mean something after the generator
 * has finished running. Without them the generated file is just source that happens to look right
 * today: a contract bump would leave the model stale, every new field would decode as its default,
 * and NOTHING WOULD FAIL -- which is the exact failure mode the snake_case parser had.
 */
class ContractDriftTest {

  private fun repoRoot(): File {
    // Gradle runs unit tests with the module directory (app/) as the working directory.
    var dir = File("").absoluteFile
    while (!File(dir, "contract/mobile-api.v1.json").exists()) {
      dir = dir.parentFile ?: error("could not locate contract/ above ${File("").absoluteFile}")
    }
    return dir
  }

  private val contractFile get() = File(repoRoot(), "contract/mobile-api.v1.json")

  private fun contract(): JSONObject = JSONObject(contractFile.readText(Charsets.UTF_8))

  @Test
  fun `the vendored contract matches CHECKSUMS, hashed LF-normalised as it instructs`() {
    val sums = JSONObject(File(repoRoot(), "contract/CHECKSUMS.json").readText(Charsets.UTF_8))
    val expected = sums.getJSONObject("files").getString("mobile-api.v1.json")

    // LF-normalised ON PURPOSE. This repo is CRLF while the desktop repo runs core.autocrlf=true,
    // so a raw-byte verify fails spuriously here -- and a check that cries wolf gets deleted.
    val normalised = contractFile.readBytes()
      .toString(Charsets.UTF_8)
      .replace("\r\n", "\n")
      .toByteArray(Charsets.UTF_8)

    val digest = MessageDigest.getInstance("SHA-256").digest(normalised)
      .joinToString("") { "%02x".format(it) }

    assertEquals("vendored contract does not match CHECKSUMS.json", expected, digest)
  }

  @Test
  fun `ContractJob carries every field in the contract's Job schema`() {
    val schemaFields = contract()
      .getJSONObject("components").getJSONObject("schemas")
      .getJSONObject("Job").getJSONObject("properties")
      .keys().asSequence().toSortedSet()

    // Instance fields only. The Kotlin/Compose compilers add STATIC members that are not properties
    // -- `Companion`, and `$stable` from the Compose plugin -- and isSynthetic does not cover them.
    val modelFields = ContractJob::class.java.declaredFields
      .filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }
      .map { it.name }
      .toSortedSet()

    val missing = schemaFields - modelFields
    val extra = modelFields - schemaFields

    assertTrue("ContractJob is missing contract fields: $missing", missing.isEmpty())
    assertTrue("ContractJob has fields the contract does not define: $extra", extra.isEmpty())
    assertEquals(37, schemaFields.size)
  }

  @Test
  fun `the contract still marks matchScore internal - the reason the band exists`() {
    val description = contract()
      .getJSONObject("components").getJSONObject("schemas")
      .getJSONObject("Job").getJSONObject("properties")
      .getJSONObject("matchScore").getString("description")

    // If the desktop repo ever un-marks this, the band-only rendering here becomes a choice nobody
    // is defending rather than a contract obligation, and this test says so out loud.
    assertTrue(
      "matchScore is no longer marked internal in the contract: $description",
      description.contains("DO NOT DISPLAY", ignoreCase = true)
    )
  }

  @Test
  fun `the contract still forbids reading a null automationTier as direct`() {
    val description = contract()
      .getJSONObject("components").getJSONObject("schemas")
      .getJSONObject("Job").getJSONObject("properties")
      .getJSONObject("automationTier").getString("description")

    assertTrue(description.contains("never as 'direct'", ignoreCase = true))
  }

  @Test
  fun `the cursor rejection codes this client branches on are the ones the contract defines`() {
    val description = contract()
      .getJSONObject("components").getJSONObject("schemas")
      .getJSONObject("ErrorFeedBadRequest").getString("description")

    // JobRepository maps exactly these two to FeedError values that restart the feed. A rename
    // upstream would otherwise silently demote them to BAD_FILTER, which does not restart -- and
    // the feed would retry a dead cursor forever.
    assertTrue(description.contains("cursor_sort_mismatch"))
    assertTrue(description.contains("cursor_malformed"))
  }

  @Test
  fun `the mobile credential is still the sessionLess mint, not the login token`() {
    val flow = contract().getJSONObject("x-auth-model").getJSONArray("flow")
    val joined = (0 until flow.length()).joinToString(" ") { flow.getString(it) }

    assertTrue(joined.contains("/api/auth/mobile-token"))
    assertTrue(joined.contains("do NOT persist", true) || joined.contains("SESSION-BOUND", true))
  }

  @Test
  fun `the token windows this client stores match the contract`() {
    val auth = contract().getJSONObject("x-auth-model")
    assertEquals(604800L, auth.getLong("idleSeconds"))
    // AuthRepository falls back to this exact value when the server omits absoluteSeconds.
    assertEquals(7776000L, auth.getLong("absoluteSeconds"))
  }
}
