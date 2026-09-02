package com.resumemaster.android.data

import com.resumemaster.android.data.contract.AtsBand
import com.resumemaster.android.data.contract.AtsBands
import com.resumemaster.android.data.contract.AutomationTier
import com.resumemaster.android.data.contract.ContractJob
import com.resumemaster.android.data.contract.JobFeedPage
import com.resumemaster.android.data.contract.PagingMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The decoder, against the payloads the server actually sends.
 *
 * These run on the JVM with a REAL org.json on the test classpath (see app/build.gradle.kts). The
 * org.json in android.jar is a stub that returns null and throws, so without that dependency every
 * assertion here would pass or fail for reasons unrelated to the decoder.
 */
class ContractJobDecodeTest {

  /** A minimal row carrying only what the contract marks non-optional in practice. */
  private fun row(vararg pairs: Pair<String, Any?>): JSONObject {
        val o = JSONObject().put("id", "job-1")
        pairs.forEach { (k, v) -> if (v == null) o.put(k, JSONObject.NULL) else o.put(k, v) }
        return o
    }

  // ── THE DEFECT THAT MOTIVATED THE REWRITE ─────────────────────────────────────────────────────

  @Test
  fun `camelCase salary fields decode - the five the old parser read as snake_case`() {
    val job = ContractJob.fromJson(
      row(
        "salaryMin" to 120000,
        "salaryMax" to 180000,
        "salaryCurrency" to "USD",
        "postedAt" to "2026-08-01",
        "contractType" to "full_time",
      )
    )!!

    assertEquals(120000.0, job.salaryMin!!, 0.001)
    assertEquals(180000.0, job.salaryMax!!, 0.001)
    assertEquals("USD", job.salaryCurrency)
    assertEquals("2026-08-01", job.postedAt)
    assertEquals("full_time", job.contractType)
  }

  @Test
  fun `snake_case keys are NOT read - this is what silently produced null salaries`() {
    // Exactly the payload shape the old parser expected. The server never sent this, which is why
    // salary never rendered and nothing failed. If someone reintroduces snake_case reads, the
    // assertions above start passing for the wrong payload and this one fails.
    val job = ContractJob.fromJson(
      row(
        "salary_min" to 120000,
        "salary_max" to 180000,
        "salary_currency" to "USD",
        "posted_at" to "2026-08-01",
        "contract_type" to "full_time",
      )
    )!!

    assertNull(job.salaryMin)
    assertNull(job.salaryMax)
    assertNull(job.salaryCurrency)
    assertNull(job.postedAt)
    assertNull(job.contractType)
  }

  // ── ABSENT IS NOT NULL IS NOT EMPTY ───────────────────────────────────────────────────────────

  @Test
  fun `the eight non-required strings may be absent entirely without throwing`() {
    // applyUrl, company, description, id, location, source, title, url have no null coalescing
    // server-side, so JSON.stringify DELETES them: they arrive ABSENT, not null.
    val job = ContractJob.fromJson(JSONObject().put("id", "job-1"))!!

    assertEquals("", job.applyUrl)
    assertEquals("", job.company)
    assertEquals("", job.description)
    assertEquals("", job.location)
    assertEquals("", job.title)
    assertEquals("", job.url)
    // sourcePlatform is required and its documented empty-row value is "direct", not "".
    assertEquals("direct", job.sourcePlatform)
  }

  @Test
  fun `an explicit empty string is preserved and is not laundered into null`() {
    // optString().ifEmpty { null } collapsed absent, JSON null and "" into one answer. "" is a
    // legitimate value for these fields.
    val job = ContractJob.fromJson(row("summary" to ""))!!
    assertEquals("", job.summary)

    val nulled = ContractJob.fromJson(row("summary" to null))!!
    assertNull(nulled.summary)

    val absent = ContractJob.fromJson(row())!!
    assertNull(absent.summary)
  }

  @Test
  fun `booleans use the contract's empty-row value, not a blanket false`() {
    val job = ContractJob.fromJson(JSONObject().put("id", "job-1"))!!
    // isActive's documented empty-row value is TRUE. Defaulting it to false would hide every job
    // on a payload that simply omitted the field.
    assertTrue(job.isActive)
    assertFalse(job.starred)
    assertFalse(job.disliked)
    assertFalse(job.visited)
    assertFalse(job.remote)
  }

  @Test
  fun `nullable booleans keep the tri-state`() {
    assertNull(ContractJob.fromJson(row())!!.isH1bSponsor)
    assertNull(ContractJob.fromJson(row("isH1bSponsor" to null))!!.isH1bSponsor)
    assertEquals(true, ContractJob.fromJson(row("isH1bSponsor" to true))!!.isH1bSponsor)
    assertEquals(false, ContractJob.fromJson(row("isH1bSponsor" to false))!!.isH1bSponsor)
  }

  // ── A NULL SCORE IS NOT A ZERO ────────────────────────────────────────────────────────────────

  @Test
  fun `a null matchScore stays null and never becomes zero`() {
    assertNull(ContractJob.fromJson(row("matchScore" to null))!!.matchScore)
    assertNull(ContractJob.fromJson(row())!!.matchScore)
    assertEquals(43, ContractJob.fromJson(row("matchScore" to 43))!!.matchScore)
  }

  @Test
  fun `a null score bands as NOT_ENOUGH_SIGNAL, never as Weak and never as zero`() {
    assertEquals(AtsBand.NOT_ENOUGH_SIGNAL, AtsBands.bandFor(null))
    assertNotEquals(AtsBands.bandFor(null), AtsBands.bandFor(0))
    assertEquals(AtsBand.WEAK, AtsBands.bandFor(0))
  }

  @Test
  fun `band cutpoints sit exactly where the graded 30 put them`() {
    assertEquals(AtsBand.STRONG, AtsBands.bandFor(44))
    assertEquals(AtsBand.MODERATE, AtsBands.bandFor(43))
    assertEquals(AtsBand.MODERATE, AtsBands.bandFor(26))
    assertEquals(AtsBand.WEAK, AtsBands.bandFor(25))
  }

  @Test
  fun `the auto-apply gate is not coupled to the Strong band, in either direction`() {
    // 44 and 30 being different is not a near-miss to be tidied up. Gating on Strong would cut
    // auto-apply volume from ~36% of the board to ~6% as a side effect of a copy decision.
    assertNotEquals(AtsBands.STRONG_CUTPOINT, AtsBands.AUTO_APPLY_GATE_THRESHOLD)
    assertEquals(44, AtsBands.STRONG_CUTPOINT)
    assertEquals(30, AtsBands.AUTO_APPLY_GATE_THRESHOLD)
    // A score that clears the gate need not be Strong -- that is the whole point.
    assertEquals(AtsBand.MODERATE, AtsBands.bandFor(AtsBands.AUTO_APPLY_GATE_THRESHOLD))
  }

  @Test
  fun `no band label renders the number`() {
    listOf(44, 43, 26, 25, 0).forEach { score ->
      val band = AtsBands.bandFor(score)
      val rendered = AtsBands.shortLabel(band) + AtsBands.label(band) + AtsBands.blurb(band)
      assertFalse("band copy must not contain the score $score", rendered.contains(score.toString()))
      assertFalse("band copy must not claim a percentage", rendered.contains("%"))
    }
  }

  @Test
  fun `no signal is grey and off the green-amber-red axis`() {
    val (bg, fg) = AtsBands.colors(AtsBand.NOT_ENOUGH_SIGNAL)
    assertEquals("#e5e7eb", bg)
    assertEquals("#4b5563", fg)
    listOf(AtsBand.STRONG, AtsBand.MODERATE, AtsBand.WEAK).forEach {
      assertNotEquals(AtsBands.colors(it), AtsBands.colors(AtsBand.NOT_ENOUGH_SIGNAL))
    }
  }

  // ── TIER GATING ───────────────────────────────────────────────────────────────────────────────

  @Test
  fun `a null automationTier reads as UNKNOWN and never as direct`() {
    assertEquals(AutomationTier.UNKNOWN, AutomationTier.from(null))
    assertEquals(AutomationTier.UNKNOWN, AutomationTier.from("something-new"))
    assertNotEquals(AutomationTier.DIRECT, AutomationTier.from(null))
  }

  @Test
  fun `unknown is not completable on mobile - broader than gated`() {
    assertFalse(AutomationTier.UNKNOWN.completableOnMobile)
    assertFalse(AutomationTier.GATED.completableOnMobile)
    assertFalse(AutomationTier.ACCOUNT.completableOnMobile)
    assertTrue(AutomationTier.DIRECT.completableOnMobile)
    assertTrue(AutomationTier.GUEST.completableOnMobile)

    assertEquals(listOf("direct", "guest"), AutomationTier.completableWireValues())
  }

  @Test
  fun `tiers are filtered server-side, in the query the client sends`() {
    val params = JobFilters().asQueryParams(pageSize = 20, cursor = null).toMap()
    assertEquals("direct,guest", params["tiers_include"])
  }

  // ── THE FEED PAGE ─────────────────────────────────────────────────────────────────────────────

  @Test
  fun `a successful page with no attribution key does not throw`() {
    // GET /api/jobs never emits `attribution` -- not on the populated path and not on the
    // cache-empty path. The old parser called json.getJSONArray("attribution"), which throws, so
    // EVERY successful board load was reported as a network failure.
    val body = JSONObject()
      .put("success", true)
      .put("jobs", org.json.JSONArray().put(JSONObject().put("id", "a").put("title", "SWE")))
      .put("total", 1)
      .put("page", 1)
      .put("pageSize", 20)
      .put("totalPages", 1)
      .put("nextCursor", JSONObject.NULL)
      .put("paging", "offset")

    val page = JobFeedPage.fromJson(body)
    assertEquals(1, page.jobs.size)
    assertEquals("SWE", page.jobs[0].title)
  }

  @Test
  fun `a null nextCursor means last page - a fact, not an inference`() {
    val page = JobFeedPage.fromJson(
      JSONObject().put("jobs", org.json.JSONArray()).put("nextCursor", JSONObject.NULL)
        .put("paging", "cursor").put("total", 0)
    )
    assertTrue(page.isLastPage)
    assertNull(page.nextCursor)
  }

  @Test
  fun `in cursor mode page and totalPages are dropped rather than rendered`() {
    val cursorPage = JobFeedPage.fromJson(
      JSONObject().put("jobs", org.json.JSONArray()).put("paging", "cursor")
        .put("page", 1).put("totalPages", 34).put("total", 680)
        .put("nextCursor", "abc")
    )
    assertEquals(PagingMode.CURSOR, cursorPage.paging)
    // "page 1 of 34" on every cursor page is the quietly-wrong surface `paging` exists to prevent.
    assertNull(cursorPage.page)
    assertNull(cursorPage.totalPages)

    val offsetPage = JobFeedPage.fromJson(
      JSONObject().put("jobs", org.json.JSONArray()).put("paging", "offset")
        .put("page", 2).put("totalPages", 34).put("total", 680)
        .put("nextCursor", "abc")
    )
    assertEquals(2, offsetPage.page)
    assertEquals(34, offsetPage.totalPages)
  }

  @Test
  fun `a row with no id is dropped and counted, not rendered as a dead card`() {
    val body = JSONObject()
      .put(
        "jobs",
        org.json.JSONArray()
          .put(JSONObject().put("id", "good").put("title", "Kept"))
          .put(JSONObject().put("title", "No id, cannot be starred or applied to"))
      )
      .put("total", 2)
      .put("paging", "cursor")
      .put("nextCursor", JSONObject.NULL)

    val page = JobFeedPage.fromJson(body)
    assertEquals(1, page.jobs.size)
    assertEquals(1, page.droppedRows)
  }

  @Test
  fun `skills is always a list`() {
    assertEquals(emptyList<String>(), ContractJob.fromJson(row())!!.skills)
    assertEquals(emptyList<String>(), ContractJob.fromJson(row("skills" to null))!!.skills)
    assertEquals(
      listOf("Kotlin", "Compose"),
      ContractJob.fromJson(row("skills" to org.json.JSONArray().put("Kotlin").put("Compose")))!!.skills
    )
  }

  // ── THE UI MAPPING ────────────────────────────────────────────────────────────────────────────

  @Test
  fun `matchScore and logoColor are read, not hardcoded to 0 and grey`() {
    val ui = ContractJob.fromJson(row("matchScore" to 51, "company" to "Stripe", "title" to "SWE"))!!.toUiJob()
    assertEquals(51, ui.matchScore)
    assertNotEquals("#888888", ui.logoColor)
  }

  @Test
  fun `logoColor is a parseable hex colour and is stable per company`() {
    fun colorFor(company: String) =
      ContractJob.fromJson(row("company" to company))!!.toUiJob().logoColor

    val first = colorFor("Stripe")
    assertEquals(first, colorFor("Stripe"))
    assertTrue("must be #rrggbb, it is fed to Color.parseColor", Regex("^#[0-9a-fA-F]{6}$").matches(first))
  }

  @Test
  fun `a job the phone cannot complete is labelled desktop only`() {
    val gated = ContractJob.fromJson(row("automationTier" to "gated", "company" to "X"))!!.toUiJob()
    assertTrue(gated.tags.contains("Desktop only"))

    val direct = ContractJob.fromJson(row("automationTier" to "direct", "company" to "X"))!!.toUiJob()
    assertFalse(direct.tags.contains("Desktop only"))

    // null tier is unknown, and unknown is not completable.
    val untyped = ContractJob.fromJson(row("company" to "X"))!!.toUiJob()
    assertTrue(untyped.tags.contains("Desktop only"))
  }

  @Test
  fun `sourcePlatform is never used as a user-facing label`() {
    // It looks like the ATS name and is not: it resolves to where the job was FOUND. The contract
    // lists it as a trap and names automationTier as the trustworthy field.
    val ui = ContractJob.fromJson(
      row("sourcePlatform" to "greenhouse", "company" to "X", "automationTier" to "direct")
    )!!.toUiJob()
    assertFalse(ui.tags.any { it.contains("greenhouse", ignoreCase = true) })
  }
}
