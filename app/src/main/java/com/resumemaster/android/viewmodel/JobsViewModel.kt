package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumemaster.android.AppGraph
import com.resumemaster.android.data.Interaction
import com.resumemaster.android.data.JobFilters
import com.resumemaster.android.data.JobRepository
import com.resumemaster.android.models.Job
import com.resumemaster.android.models.SwipeAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The swipe stack.
 *
 * ── A SWIPE QUEUES, IT NEVER SUBMITS ────────────────────────────────────────────────────────────
 *
 * Nothing in here reaches an employer, and nothing in here triggers generation. Queue and
 * QueuePriority record local intent; Star and Dislike are the only actions that write to the server,
 * and they write PREFERENCE (via the idempotent /api/jobs/interact), not an application.
 *
 * Generation stays deferred to approval. Swiping is about a second per job, so five idle minutes is
 * roughly sixty jobs; at about four cents a generation that is a bill of a couple of dollars from a
 * gesture that cost the user no thought. A right-swipe has to be free.
 */
class JobsViewModel(
  private val repository: JobRepository = AppGraph.jobs,
) : ViewModel() {

  /** The visible stack. Swiped cards are removed locally; the server list is the repository's. */
  private val _jobs = MutableStateFlow<List<Job>>(emptyList())
  val jobs: StateFlow<List<Job>> = _jobs

  private val _lastAction = MutableStateFlow<SwipeAction?>(null)
  val lastAction: StateFlow<SwipeAction?> = _lastAction

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading

  /** Queued locally, pending the review queue wiring in Phase 2b. */
  val queue = mutableListOf<Job>()
  val starred = mutableListOf<Job>()

  private var filters = JobFilters()

  init {
    loadMore()
  }

  fun setFilters(next: JobFilters) {
    if (next == filters) return
    filters = next
    // A cursor is only valid for the filter set that produced it, so the feed restarts rather than
    // sending one the server would reject with cursor_sort_mismatch.
    repository.restart()
    _jobs.value = emptyList()
    loadMore()
  }

  fun loadMore() {
    if (_loading.value || repository.isExhausted) return
    _loading.value = true
    viewModelScope.launch {
      val result = repository.feed(filters)
      result
        .onSuccess { _jobs.value = repository.uiJobs.value; _error.value = null }
        .onFailure { _error.value = it.message }
      _loading.value = false
    }
  }

  fun onSwipe(action: SwipeAction, job: Job) {
    when (action) {
      SwipeAction.Queue -> if (queue.none { it.id == job.id }) queue.add(job)
      SwipeAction.QueuePriority -> if (queue.none { it.id == job.id }) queue.add(0, job)
      SwipeAction.Star -> {
        if (starred.none { it.id == job.id }) starred.add(job)
        write(job, Interaction(starred = true))
      }
      SwipeAction.Dislike -> write(job, Interaction(disliked = true))
      SwipeAction.Skip -> Unit
    }
    _lastAction.value = action

    // Advance the stack. The card leaves the screen immediately; the server write, if any, races
    // behind it and reconciles in the repository.
    _jobs.value = _jobs.value.filterNot { it.id == job.id }

    // Fetch the next page BEFORE the stack empties, so the user never sees an empty board while a
    // page is in flight.
    if (_jobs.value.size <= 3) loadMore()
  }

  fun onButtonAction(action: SwipeAction) {
    _jobs.value.firstOrNull()?.let { onSwipe(action, it) }
  }

  /**
   * Send a preference.
   *
   * A failure is surfaced, not swallowed. The old code could not fail here because it never made a
   * request; now that it does, a silent failure would leave the user believing a dislike stuck and
   * then seeing the job again on the next launch with no explanation.
   */
  private fun write(job: Job, interaction: Interaction) {
    viewModelScope.launch {
      repository.interact(job.id, interaction)
        .onFailure { _error.value = it.message }
    }
  }
}
