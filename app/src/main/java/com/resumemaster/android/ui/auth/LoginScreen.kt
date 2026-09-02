package com.resumemaster.android.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.ui.theme.*
import com.resumemaster.android.viewmodel.AuthViewModel

/**
 * Sign in.
 *
 * There was no login screen, no credential entry and no token storage of any kind before this.
 * `LinkedInAuthManager` is profile-IMPORT only and is explicitly not a session, so the app had no
 * way to authenticate at all — which is why the board request went out with no Authorization header
 * to a requireAuth endpoint.
 *
 * The two-step credential exchange behind this screen is in AuthRepository. Nothing about it is
 * visible here on purpose: the user signs in once, and which of the two tokens gets persisted is not
 * a decision a UI should be able to influence.
 */
@Composable
fun LoginScreen(vm: AuthViewModel = viewModel(), onSignedIn: () -> Unit) {
  val state by vm.state.collectAsState()
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

  Column(
    Modifier.fillMaxSize().padding(28.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Resume Master", style = DisplayLarge, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))
    Text("Sign in to load your board.", style = BodyMedium, color = TextMuted)
    Spacer(Modifier.height(28.dp))

    OutlinedTextField(
      value = username,
      onValueChange = { username = it },
      label = { Text("Username") },
      singleLine = true,
      enabled = !state.busy,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next,
      ),
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
      value = password,
      onValueChange = { password = it },
      label = { Text("Password") },
      singleLine = true,
      enabled = !state.busy,
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
      ),
      modifier = Modifier.fillMaxWidth(),
    )

    state.error?.let {
      Spacer(Modifier.height(14.dp))
      // The server's own message, not a generic one. It is the only thing that distinguishes wrong
      // credentials from "signed in, but the token exchange failed" — two states that need
      // different actions from the user.
      Text(it, style = BodyMedium, color = MaterialTheme.colorScheme.error)
    }

    Spacer(Modifier.height(22.dp))
    Button(
      onClick = { vm.signIn(username.trim(), password) },
      enabled = !state.busy && username.isNotBlank() && password.isNotBlank(),
      modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
      if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
      else Text("Sign in")
    }
  }
}
