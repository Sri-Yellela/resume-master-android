package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumemaster.android.AppGraph
import com.resumemaster.android.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
  val busy: Boolean = false,
  val signedIn: Boolean = false,
  val error: String? = null,
  val username: String? = null,
)

class AuthViewModel(
  private val repository: AuthRepository = AppGraph.auth,
) : ViewModel() {

  private val _state = MutableStateFlow(AuthUiState(signedIn = repository.isSignedIn()))
  val state: StateFlow<AuthUiState> = _state

  fun signIn(username: String, password: String) {
    if (_state.value.busy) return
    _state.value = _state.value.copy(busy = true, error = null)
    viewModelScope.launch {
      repository.signIn(username, password)
        .onSuccess { user ->
          _state.value = AuthUiState(busy = false, signedIn = true, username = user.username)
        }
        .onFailure { e ->
          // Stays signed OUT. The exchange failing after a successful login is a failed sign-in,
          // not a partial one -- reporting success here would send the user to a board that cannot
          // authenticate and has no way to explain why.
          _state.value = AuthUiState(busy = false, signedIn = false, error = e.message)
        }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      repository.signOut()
      // Signed out locally regardless of what the server said -- SecureTokenStore.clear() has
      // already run inside signOut(). Reflecting anything else here would show a signed-in UI with
      // no credential behind it.
      _state.value = AuthUiState(signedIn = false)
    }
  }
}
