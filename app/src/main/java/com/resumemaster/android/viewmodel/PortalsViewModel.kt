package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumemaster.android.AppGraph
import com.resumemaster.android.data.portals.ConnectOutcome
import com.resumemaster.android.data.portals.CookieSource
import com.resumemaster.android.data.portals.PortalFamily
import com.resumemaster.android.data.portals.PortalRepository
import com.resumemaster.android.data.portals.PortalsSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PortalsUiState(
  val loading: Boolean = true,
  val snapshot: PortalsSnapshot? = null,
  val busy: Boolean = false,
  /** What the last action said, in the user's words (a refusal always says nothing was sent). */
  val message: String? = null,
  /** Set when a connect finished — the connect screen closes and clears its WebView on it. */
  val connectedFamily: String? = null,
)

class PortalsViewModel(
  private val repository: PortalRepository = AppGraph.portals,
  private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

  private val _state = MutableStateFlow(PortalsUiState())
  val state: StateFlow<PortalsUiState> = _state

  val isOffered: Boolean get() = repository.isOffered

  /** The screen calls [refresh] when it is shown (and again on coming back from the connect screen). */
  fun refresh() {
    if (!repository.isOffered) { _state.value = _state.value.copy(loading = false); return }
    viewModelScope.launch {
      _state.value = _state.value.copy(loading = true)
      val r = withContext(io) { repository.list() }
      _state.value = r.fold(
        { _state.value.copy(loading = false, snapshot = it) },
        { _state.value.copy(loading = false, message = it.message) },
      )
    }
  }

  /** Seal and upload the family's sign-in. `cookies` is read only after the worker's key is verified. */
  fun connect(family: PortalFamily, cookies: CookieSource) {
    if (_state.value.busy) return
    _state.value = _state.value.copy(busy = true, message = null, connectedFamily = null)
    viewModelScope.launch {
      when (val out = withContext(io) { repository.connect(family, cookies) }) {
        is ConnectOutcome.Connected -> {
          _state.value = _state.value.copy(busy = false, connectedFamily = family.ats,
            message = "Connected — draft can apply on ${family.name} for you.")
          refresh()
        }
        is ConnectOutcome.Refused -> _state.value = _state.value.copy(busy = false, message = out.say)
      }
    }
  }

  fun disconnect(family: PortalFamily) {
    val live = _state.value.snapshot?.liveFor(family) ?: return
    if (_state.value.busy) return
    _state.value = _state.value.copy(busy = true, message = null)
    viewModelScope.launch {
      val r = withContext(io) { repository.disconnect(live.id) }
      _state.value = r.fold(
        {
          _state.value.copy(busy = false, message = if (it.keyDestroyed)
            "Disconnected — the key is destroyed; your ${family.name} sign-in can no longer be opened."
          else "Disconnected here — draft will not use it again. The apply worker could not be reached to destroy the key yet.")
        },
        { _state.value.copy(busy = false, message = it.message) },
      )
      refresh()
    }
  }

  fun consumeConnected() { _state.value = _state.value.copy(connectedFamily = null) }
}
