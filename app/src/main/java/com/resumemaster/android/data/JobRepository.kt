package com.resumemaster.android.data

import com.resumemaster.android.data.contract.AutomationTier
import com.resumemaster.android.data.contract.ContractJob
import com.resumemaster.android.data.contract.FeedError
import com.resumemaster.android.data.contract.FeedException
import com.resumemaster.android.data.contract.JobFeedPage
import com.resumemaster.android.data.net.ApiClient
import com.resumemaster.android.models.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The filter set a cursor belongs to.
 *
 * A cursor is valid ONLY for the filters that produced it. Bundling them into one object means a
 * filter change is a value change, so `feed()` can detect it and restart paging instead of sending a
 * cursor the server will reject.
 */
data class JobFilters(
  val query: String = "",
  val location: String = "",
  val starred: Boolean? = null,
  val visited: Boolean? = null,
  /**
   * Defaults to the tiers a phone can actually complete.
   *
   * SERVER-SIDE, never client-side. The server pages before a client could filter, so hiding rows
   * after the fact yields short pages, a `total` that disagrees with the list, and — on a cursor
   * feed — a cursor that has advanced past rows the user never saw.
   */
  val tiersInclude: List<String> = AutomationTier.completableWireValues(),
) {
  fun asQueryParams(pageSize: Int, cursor: String?): List<Pair<String, String>> = buildList {
    if (query.isNotBlank()) add("q" to query)
    if (location.isNotBlank()) add("location" to location)
    starred?.let { add("starred" to it.toString()) }
    visited?.let { add("visited" to it.toString()) }
    if (tiersInclude.isNotEmpty()) add("tiers_include" to tiersInclude.joinToString(","))
    add("pageSize" to pageSize.toString())
    cursor?.let { add("cursor" to it) }
  }
}

/** What a swipe asserts about a job. Maps to PATCH /api/jobs/interact. */
data class Interaction(val starred: Boolean? = null, val disliked: Boolean? = null)

class JobRepository(
  private val client: ApiClient,
) {

  private val _jobs = MutableStateFlow<List<ContractJob>>(emptyList())
  val jobs: StateFlow<List<ContractJob>> = _jobs

  private val _uiJobs = MutableStateFlow<List<Job>>(emptyList())
  val uiJobs: StateFlow<List<Job>> = _uiJobs

  /** Cursor for the NEXT page, and the filters it belongs to. */
  private var nextCursor: String? = null
  private var cursorFilters: JobFilters? = null
  private var exhausted = false

  /** Reset paging. Call on any filter change, and on a cursor rejection. */
  fun restart() {
    nextCursor = null
    cursorFilters = null
    exhausted = false
    _jobs.value = emptyList()
    _uiJobs.value = emptyList()
  }

  val isExhausted: Boolean get() = exhausted

  /**
   * Fetch the next page.
   *
   * Sends a cursor ONLY when it belongs to the same filter set. A filter change silently restarts
   * paging rather than sending a cursor that would come back `cursor_sort_mismatch`.
   */
  suspend fun feed(filters: JobFilters, pageSize: Int = 20): Result<JobFeedPage> =
    withContext(Dispatchers.IO) {
      if (cursorFilters != null && cursorFilters != filters) restart()
      if (exhausted) {
        return@withContext Result.success(
          JobFeedPage(emptyList(), _jobs.value.size, null, com.resumemaster.android.data.contract.PagingMode.CURSOR,
            fromCache = true, reason = "exhausted", page = null, totalPages = null, droppedRows = 0)
        )
      }

      try {
        val response = client.get("/api/jobs", filters.asQueryParams(pageSize, nextCursor))

        if (!response.isSuccess) {
          val kind = when {
            response.isUnauthorized -> FeedError.UNAUTHORIZED
            response.status == 400 -> when (codeOf(response.body)) {
              "cursor_sort_mismatch" -> FeedError.CURSOR_SORT_MISMATCH
              "cursor_malformed" -> FeedError.CURSOR_MALFORMED
              else -> FeedError.BAD_FILTER
            }
            else -> FeedError.SERVER
          }
          // A rejected cursor is not retryable. Drop it here so the caller's retry starts a valid
          // feed instead of replaying the same failure.
          if (kind.restartsFeed) restart()
          return@withContext Result.failure(
            FeedException(kind, errorOf(response.body, "Feed request failed (${response.status})"))
          )
        }

        val page = JobFeedPage.fromJson(JSONObject(response.body))

        nextCursor = page.nextCursor
        cursorFilters = filters
        exhausted = page.isLastPage

        _jobs.value = _jobs.value + page.jobs
        _uiJobs.value = _jobs.value.map { it.toUiJob() }

        Result.success(page)
      } catch (e: Exception) {
        Result.failure(FeedException(FeedError.NETWORK, e.message ?: "Network error"))
      }
    }

  /**
   * Record a swipe.
   *
   * ── WHY NOT PATCH /api/jobs/{id}/starred ────────────────────────────────────────────────────────
   *
   * That route TOGGLES. On a flaky phone network a retried request undoes the first one and returns
   * 200, so the user's swipe silently reverses and nothing reports it. /api/jobs/interact takes the
   * DESIRED VALUE, making it idempotent, and echoes the resolved id and the values now stored — so
   * this reconciles against what the server holds rather than what the UI optimistically rendered.
   * The contract excludes the toggle routes for this reason.
   */
  suspend fun interact(jobId: String, interaction: Interaction): Result<Interaction> =
    withContext(Dispatchers.IO) {
      try {
        val body = JSONObject().put("jobId", jobId)
        interaction.starred?.let { body.put("starred", it) }
        interaction.disliked?.let { body.put("disliked", it) }

        val response = client.patch("/api/jobs/interact", body.toString())
        if (!response.isSuccess) {
          return@withContext Result.failure(
            Exception(errorOf(response.body, "Could not save that (${response.status})"))
          )
        }

        val json = JSONObject(response.body)
        val resolved = Interaction(
          starred = if (json.has("starred")) json.optBoolean("starred") else null,
          disliked = if (json.has("disliked")) json.optBoolean("disliked") else null,
        )

        // Reconcile against the server's answer, keyed on the id IT resolved.
        val resolvedId = json.optString("jobId", jobId)
        _jobs.value = _jobs.value.map { job ->
          if (job.id != resolvedId) job
          else job.copy(
            starred = resolved.starred ?: job.starred,
            disliked = resolved.disliked ?: job.disliked,
          )
        }
        _uiJobs.value = _jobs.value.map { it.toUiJob() }

        Result.success(resolved)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun codeOf(body: String): String? = try {
    JSONObject(body).optString("code", "").ifEmpty { null }
  } catch (_: Exception) { null }

  private fun errorOf(body: String, fallback: String): String = try {
    JSONObject(body).optString("error", "").ifEmpty { fallback }
  } catch (_: Exception) { fallback }
}

/**
 * Contract row -> the existing UI model.
 *
 * ── WHAT THIS FIXES ─────────────────────────────────────────────────────────────────────────────
 *
 * `matchScore` and `logoColor` were HARDCODED to 0 and "#888888" in the parser this replaces, while
 * the server was sending real values. Both are now read.
 *
 * The score is carried through as a NULLABLE Int and is not rendered as a number anywhere — the
 * contract marks it internal ("DO NOT DISPLAY THIS NUMBER") and the UI shows the band. Null stays
 * null: it means the scorer declined, which is its own band and must never render as a low score.
 */
/** Avatar colours. Chosen by a stable hash of the company name, never at random. */
private val LOGO_PALETTE = listOf(
  "#4f46e5", "#0891b2", "#059669", "#b45309", "#be123c", "#7c3aed", "#0369a1", "#4d7c0f",
)

fun ContractJob.toUiJob(): Job {
  val tier = AutomationTier.from(automationTier)

  val salaryText = when {
    salaryMin != null && salaryMax != null ->
      "$" + (salaryMin.toInt() / 1000) + "k–$" + (salaryMax.toInt() / 1000) + "k"
    salaryMin != null -> "$" + (salaryMin.toInt() / 1000) + "k+"
    salaryMax != null -> "up to $" + (salaryMax.toInt() / 1000) + "k"
    else -> null
  }

  return Job(
    id = id,
    company = company,
    role = title,
    location = if (remote && !location.contains("remote", ignoreCase = true))
      (if (location.isBlank()) "Remote" else "$location (Remote)") else location,
    salary = salaryText,
    // workplaceType and contractType are real fields the old parser never read. `sourcePlatform` is
    // deliberately NOT used: it looks like the ATS name and is not — it resolves to where the job
    // was found, and the contract lists it as a trap. automationTier is the trustworthy field.
    tags = listOfNotNull(
      workplaceType,
      contractType?.replace("_", " "),
      experienceLevel,
      if (!tier.completableOnMobile) "Desktop only" else null,
    ),
    matchScore = matchScore,
    // NOT companyIconUrl. `logoColor` is fed to android.graphics.Color.parseColor in JobCard, so a
    // URL here is a crash, not a wrong colour — and the server has no colour field at all. The old
    // parser's hardcoded "#888888" made every avatar identical grey; this derives a STABLE colour
    // from the company name, so the same employer always looks the same without inventing data.
    // Loading the real companyIconUrl image is a UI change and is not made here.
    logoColor = LOGO_PALETTE[
      ((company.ifEmpty { id }.hashCode() % LOGO_PALETTE.size) + LOGO_PALETTE.size) % LOGO_PALETTE.size
    ],
    description = description,
    postedDate = (discoveredAt ?: 0L) * 1000L,
  )
}
