package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumemaster.android.AppGraph
import com.resumemaster.android.data.Interaction
import com.resumemaster.android.data.JobFilters
import com.resumemaster.android.data.JobRepository
import com.resumemaster.android.data.contract.FeedError
import com.resumemaster.android.data.contract.FeedException
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
/** One swipe. `seq` exists only to make two identical swipes unequal; see `lastAction`. */
data class SwipeEvent(val action: SwipeAction, val seq: Long)

class JobsViewModel(
  private val repository: JobRepository = AppGraph.jobs,
  /** Forget this device's credential. Called when the server says it is no longer valid. */
  private val clearCredential: () -> Unit = { AppGraph.tokenStore.clear() },
) : ViewModel() {

  /**
   * The server has said this device's credential is no longer valid (401: expired, revoked, or
   * signed out elsewhere). The screen routes to sign-in on it.
   *
   * A 401 used to render as "Could not load your board" and nothing else. The token stayed on disk,
   * hasToken() stayed true, and every launch started on a board that could not load, with no way
   * back to sign-in (Shape 3 #3). The contract says a mobile client must treat a 401 as "sign in
   * again".
   */
  private val _sessionExpired = MutableStateFlow(false)
  val sessionExpired: StateFlow<Boolean> = _sessionExpired

  private fun onFailure(e: Throwable) {
    if ((e as? FeedException)?.kind == FeedError.UNAUTHORIZED) {
      // Forget the dead credential BEFORE routing, so a relaunch lands on sign-in too.
      clearCredential()
      _sessionExpired.value = true
    }
    _error.value = e.message
  }

  /** The visible stack. Swiped cards are removed locally; the server list is the repository's. */
  private val _jobs = MutableStateFlow<List<Job>>(emptyList())
  val jobs: StateFlow<List<Job>> = _jobs

  /**
   * The most recent swipe, as an EVENT rather than a value.
   *
   * It was the bare SwipeAction. A StateFlow drops an assignment equal to what it holds, so a second
   * Queue after a first was Queue == Queue: no emission, ActionBadge's LaunchedEffect key unchanged,
   * no confirmation for the second swipe. ActionBadge's `onDismiss` — the callback that could have
   * reset it — was never passed by anyone (Shape 2: a handler wired to nothing). `seq` makes every
   * swipe a distinct value, so every swipe confirms and each one restarts the badge's timer.
   */
  private var swipeSeq = 0L
  private val _lastAction = MutableStateFlow<SwipeEvent?>(null)
  val lastAction: StateFlow<SwipeEvent?> = _lastAction

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading

  /** Queued locally, pending the review queue wiring in Phase 2b. */
  val queue = mutableListOf<Job>()
  val starred = mutableListOf<Job>()

  /**
   * Every job the user has already decided on this session, whatever the action.
   *
   * THE VISIBLE STACK IS DERIVED, not copied. The repository accumulates every page it has fetched;
   * the stack is that list MINUS what has been swiped. This used to be two lists that did not meet:
   * onSwipe removed the card from `_jobs` locally, and the next successful page replaced `_jobs`
   * wholesale with the repository's list -- so every card swiped since launch came back to the top
   * of the stack the moment page 2 arrived. JobsViewModelTest pins it.
   *
   * Deliberately NOT cleared by setFilters: a card already decided on stays decided under a new
   * filter set for the rest of the session.
   */
  private val decided = mutableSetOf<String>()

  private var filters = JobFilters()

  init {
    // A NEW BOARD STARTS FROM PAGE 1. The repository is AppGraph's process-wide singleton and
    // outlives a sign-out; this ViewModel is scoped to the jobs destination, which a sign-out pops.
    // Without the restart the next sign-in continued the previous session's feed -- its cursor,
    // appended to its accumulated rows -- so whoever signed in next was served the last user's
    // board. A retained ViewModel (tab switch, rotation) does not re-run this.
    repository.restart()
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
        .onSuccess { _jobs.value = repository.uiJobs.value.filterNot { it.id in decided }; _error.value = null }
        .onFailure(::onFailure)
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
      // A left swipe (and the X button) is a SAVED pass — the owner's call, 09-29. It was a local
      // Skip, so a passed card came back on the next launch; the server now excludes it from every
      // feed query (user_jobs.disliked). Nothing else produced Dislike, which made it unreachable.
      SwipeAction.Dislike -> write(job, Interaction(disliked = true))
    }
    _lastAction.value = SwipeEvent(action, ++swipeSeq)

    // Advance the stack. The card leaves the screen immediately; the server write, if any, races
    // behind it and reconciles in the repository.
    decided += job.id
    _jobs.value = _jobs.value.filterNot { it.id == job.id }

    // Fetch the next page BEFORE the stack empties, so the user never sees an empty board while a
    // page is in flight.
    if (_jobs.value.size <= 3) loadMore()
  }

  /** The screen has shown the current error (as a snackbar over a non-empty stack). */
  fun errorShown() { _error.value = null }

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
        .onFailure(::onFailure)
    }
  }
}
