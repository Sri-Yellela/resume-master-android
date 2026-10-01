package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumemaster.android.AppGraph
import com.resumemaster.android.data.Interaction
import com.resumemaster.android.data.JobRepository
import com.resumemaster.android.data.contract.Deck
import com.resumemaster.android.data.contract.FeedError
import com.resumemaster.android.data.contract.FeedException
import com.resumemaster.android.models.Job
import com.resumemaster.android.models.SwipeAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Today's draft on the phone — ANDROID.md 2c, revised to the deck (../draft/docs/UI_REDESIGN.md §5.2).
 *
 * The same frozen deal as the web (GET /api/draft/today with the LOCAL date), so a card drafted here
 * reads as decided there. Gestures mean what they mean everywhere:
 *
 *   right (Queue)  DRAFT on the server — POST /api/apply/runs, surface "deck", ⛔ no approvalMode. It
 *                  is previewed and held for review; this app cannot submit an application. (It used
 *                  to record LOCAL intent only: nothing reached a server and it was gone on restart.)
 *   left (Dislike) PASS — saved; the server never deals it again
 *   Star           SAVE — a button only; the diagonal gesture is gone
 *
 * ⛔ A REFUSED CALL IS NEVER A SUCCESS: the card goes back to the front of the stack and the server's
 * sentence is shown. ⛔ A 401 forgets the credential and routes to sign-in.
 */
class DeckViewModel(
  private val repository: JobRepository = AppGraph.jobs,
  private val clearCredential: () -> Unit = { AppGraph.tokenStore.clear() },
  private val today: () -> String = { LocalDate.now().toString() },
) : ViewModel() {

  private val _deck = MutableStateFlow<Deck?>(null)
  val deck: StateFlow<Deck?> = _deck

  /** The undecided cards, in deal order. */
  private val _jobs = MutableStateFlow<List<Job>>(emptyList())
  val jobs: StateFlow<List<Job>> = _jobs

  private val _drafted = MutableStateFlow(0)
  val drafted: StateFlow<Int> = _drafted

  private var swipeSeq = 0L
  private val _lastAction = MutableStateFlow<SwipeEvent?>(null)
  val lastAction: StateFlow<SwipeEvent?> = _lastAction

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading

  private val _sessionExpired = MutableStateFlow(false)
  val sessionExpired: StateFlow<Boolean> = _sessionExpired

  init { load() }

  fun load() {
    if (_loading.value) return
    _loading.value = true
    viewModelScope.launch {
      repository.today(today())
        .onSuccess { d ->
          _deck.value = d
          _jobs.value = d.undecided.map { it.job }
          _drafted.value = d.cards.count { it.decision == "drafted" }
          _error.value = null
        }
        .onFailure(::onFailure)
      _loading.value = false
    }
  }

  private fun onFailure(e: Throwable) {
    if ((e as? FeedException)?.kind == FeedError.UNAUTHORIZED) {
      clearCredential()
      _sessionExpired.value = true
    }
    _error.value = e.message
  }

  /** The screen has shown the current error. */
  fun errorShown() { _error.value = null }

  fun onSwipe(action: SwipeAction, job: Job) {
    // Commit at once — the card leaves the stack now; the call races behind it.
    _jobs.value = _jobs.value.filterNot { it.id == job.id }
    if (action == SwipeAction.Queue) _drafted.value += 1
    _lastAction.value = SwipeEvent(action, ++swipeSeq)
    viewModelScope.launch {
      val result = when (action) {
        SwipeAction.Queue -> repository.draft(job.id)
        SwipeAction.Dislike -> repository.interact(job.id, Interaction(disliked = true)).map { }
        SwipeAction.Star -> repository.interact(job.id, Interaction(starred = true)).map { }
      }
      result.onFailure { e ->
        // Back to the FRONT, undecided — the user is shown the card the server refused.
        _jobs.value = listOf(job) + _jobs.value.filterNot { it.id == job.id }
        if (action == SwipeAction.Queue) _drafted.value -= 1
        onFailure(e)
      }
    }
  }

  fun onButtonAction(action: SwipeAction) {
    _jobs.value.firstOrNull()?.let { onSwipe(action, it) }
  }
}
