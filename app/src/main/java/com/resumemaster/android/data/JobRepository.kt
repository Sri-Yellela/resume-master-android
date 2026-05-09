package com.resumemaster.android.data

import com.resumemaster.android.models.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// ─── API response types (internal — not the same as the UI Job model) ────────

private data class ApiJob(
  val id: String,
  val title: String,
  val company: String,
  val location: String,
  val url: String,
  val description: String?,
  val salaryMin: Double?,
  val salaryMax: Double?,
  val salaryCurrency: String?,
  val postedAt: String?,
  val contractType: String?,
  val remote: Boolean?,
)

data class JobAttribution(val name: String, val url: String)

data class JobSearchResult(
  val jobs: List<Job>,
  val total: Int,
  val page: Int,
  val pageSize: Int,
  val sources: List<String>,
  val attribution: List<JobAttribution>,
)

data class JobSearchParams(
  val query: String    = "",
  val location: String = "",
  val country: String  = "us",
  val page: Int        = 1,
  val pageSize: Int    = 10,
)

// ─── Maps API response → existing UI Job model ───────────────────────────────

private fun ApiJob.toUiJob(): Job {
  val salary = when {
    salaryMin != null && salaryMax != null ->
      "\$${salaryMin.toInt() / 1000}k–\$${salaryMax.toInt() / 1000}k"
    salaryMin != null -> "\$${salaryMin.toInt() / 1000}k+"
    salaryMax != null -> "up to \$${salaryMax.toInt() / 1000}k"
    else -> null
  }
  return Job(
    id          = id,
    company     = company,
    role        = title,
    location    = if (remote == true && !location.contains("remote", ignoreCase = true))
                    "$location (Remote)" else location,
    salary      = salary,
    tags        = listOfNotNull(contractType?.replace("_", " ")),
    matchScore  = 0,
    logoColor   = "#888888",
    description = description ?: "",
    postedDate  = System.currentTimeMillis(),
  )
}

// ─── Repository ───────────────────────────────────────────────────────────────

class JobRepository {

  private val BASE_URL = "https://resumemaster.one"
  // For local dev: "http://10.0.2.2:3000"

  private val _jobs = MutableStateFlow<List<Job>>(MockData.jobs)
  val jobs: StateFlow<List<Job>> = _jobs

  private val _attribution = MutableStateFlow<List<JobAttribution>>(emptyList())
  val attribution: StateFlow<List<JobAttribution>> = _attribution

  /** Legacy rotate used by swipe UI. */
  fun rotate(job: Job) {
    _jobs.value = _jobs.value.filterNot { it.id == job.id } +
      job.copy(id = job.id + "-next", postedDate = System.currentTimeMillis())
  }

  suspend fun search(params: JobSearchParams): Result<JobSearchResult> =
    withContext(Dispatchers.IO) {
      try {
        val queryParts = buildList {
          if (params.query.isNotBlank())    add("q=${encode(params.query)}")
          if (params.location.isNotBlank()) add("location=${encode(params.location)}")
          add("country=${params.country}")
          add("page=${params.page}")
          add("pageSize=${params.pageSize}")
        }
        val urlString  = "$BASE_URL/api/jobs?${queryParts.joinToString("&")}"
        val connection = URL(urlString).openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout    = 8000

        val responseText = connection.inputStream.bufferedReader().readText()
        val json         = JSONObject(responseText)

        if (!json.optBoolean("success", false)) {
          return@withContext Result.failure(Exception("API returned success: false"))
        }

        val jobsArray = json.getJSONArray("jobs")
        val apiJobs = (0 until jobsArray.length()).map { i ->
          val j = jobsArray.getJSONObject(i)
          ApiJob(
            id           = j.getString("id"),
            title        = j.getString("title"),
            company      = j.getString("company"),
            location     = j.getString("location"),
            url          = j.getString("url"),
            description  = j.optString("description").ifEmpty { null },
            salaryMin    = if (j.isNull("salary_min")) null else j.getDouble("salary_min"),
            salaryMax    = if (j.isNull("salary_max")) null else j.getDouble("salary_max"),
            salaryCurrency = j.optString("salary_currency").ifEmpty { null },
            postedAt     = j.optString("posted_at").ifEmpty { null },
            contractType = j.optString("contract_type").ifEmpty { null },
            remote       = if (j.isNull("remote")) null else j.getBoolean("remote"),
          )
        }

        val attrArray = json.getJSONArray("attribution")
        val attribution = (0 until attrArray.length()).map { i ->
          val a = attrArray.getJSONObject(i)
          JobAttribution(name = a.getString("name"), url = a.getString("url"))
        }

        val uiJobs = apiJobs.map { it.toUiJob() }
        _jobs.value = uiJobs
        _attribution.value = attribution

        Result.success(JobSearchResult(
          jobs        = uiJobs,
          total       = json.getInt("total"),
          page        = json.getInt("page"),
          pageSize    = json.getInt("pageSize"),
          sources     = json.getJSONArray("sources").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
          },
          attribution = attribution,
        ))
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun encode(s: String) =
    java.net.URLEncoder.encode(s, "UTF-8")
}
