package com.resumemaster.android.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.viewmodel.AuthViewModel

/**
 * Profile, and the ONLY way off this device's credential short of clearing app data.
 *
 * AuthViewModel.signOut existed and nothing in the UI called it (Shape 2: a handler wired to
 * nothing), so a user could not withdraw the phone's token. The row below calls it; navigation to
 * sign-in happens only once the ViewModel reports the local credential gone. A clear that failed
 * keeps the user here with the reason, rather than showing a sign-in screen over a live token.
 *
 * The name and plan lines are still hardcoded -- blocked on 2b1, see CORRUPTION_SWEEP_ANDROID.md.
 */
@Composable
fun ProfileScreen(onAdmin: () -> Unit, onSignedOut: () -> Unit, vm: AuthViewModel = viewModel()) {
  val state by vm.state.collectAsState()
  LaunchedEffect(state.signedIn) { if (!state.signedIn) onSignedOut() }
  ProfileContent(onAdmin = onAdmin, onSignOut = vm::signOut, signingOut = state.busy, signOutError = state.error)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(onAdmin: () -> Unit, onSignOut: () -> Unit, signingOut: Boolean, signOutError: String?) {
  Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Profile") }) }) { padding ->
    Column(Modifier.padding(padding).padding(18.dp)) {
      Text("Sri Yellela", style = MaterialTheme.typography.titleMedium)
      Text("Admin • Pro", style = MaterialTheme.typography.bodyMedium)
      HorizontalDivider(Modifier.padding(vertical = 16.dp))
      Row(
        Modifier.fillMaxWidth().clickable(onClick = onAdmin).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
          Text("Admin Panel")
          Text("Users, jobs, queue, flags, analytics", style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = "Open")
      }
      HorizontalDivider()
      Row(
        Modifier.fillMaxWidth().clickable(enabled = !signingOut, onClick = onSignOut).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
          Text("Sign out", color = MaterialTheme.colorScheme.error)
          Text("Removes your sign-in from this device", style = MaterialTheme.typography.labelSmall)
        }
        if (signingOut) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
      }
      signOutError?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
      }
    }
  }
}
