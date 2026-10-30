package com.resumemaster.android.ui.portals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.data.portals.PortalFamily
import com.resumemaster.android.viewmodel.PortalsViewModel

/**
 * D112 item 7 — Portals: the accounts draft's apply worker may apply on for the user, each sealed so draft's
 * servers store it and cannot open it. Reached from Profile, and only when a worker identity is pinned
 * (the Profile row is not drawn otherwise; this screen also says so if reached anyway).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalsScreen(onBack: () -> Unit, onConnect: (PortalFamily) -> Unit, vm: PortalsViewModel = viewModel()) {
  val state by vm.state.collectAsState()
  LaunchedEffect(Unit) { vm.refresh() }
  var asking by remember { mutableStateOf<PortalFamily?>(null) }

  Scaffold(topBar = {
    CenterAlignedTopAppBar(
      title = { Text("Portals") },
      navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
    )
  }) { padding ->
    Column(Modifier.padding(padding).padding(18.dp).verticalScroll(rememberScrollState())) {
      if (!vm.isOffered) {
        Text("This version of draft cannot connect a portal yet.", style = MaterialTheme.typography.bodyMedium)
        return@Column
      }
      Text(
        "Sign in to a job portal here and draft's apply worker can fill your applications there while you're away. " +
          "Your sign-in is sealed on this phone; draft's servers store it but cannot open it.",
        style = MaterialTheme.typography.bodyMedium,
      )
      Spacer(Modifier.height(16.dp))
      val snapshot = state.snapshot
      for (family in PortalFamily.ALL) {
        val live = snapshot?.liveFor(family)
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
          Text(family.name, style = MaterialTheme.typography.titleMedium)
          when {
            state.loading && snapshot == null -> Text("Checking…", style = MaterialTheme.typography.labelSmall)
            live != null -> {
              Text(
                if (live.status == "expiring") "Connected — the sign-in expires soon; connect again to renew it."
                else "Connected — draft can apply on ${family.name} for you.",
                style = MaterialTheme.typography.bodyMedium,
              )
              Spacer(Modifier.height(8.dp))
              OutlinedButton(onClick = { vm.disconnect(family) }, enabled = !state.busy) { Text("Disconnect") }
              Text(
                "The apply worker destroys the key, and the stored sign-in can never be opened again.",
                style = MaterialTheme.typography.labelSmall,
              )
            }
            snapshot != null && !snapshot.worker -> Text(
              "The apply worker is not running yet, so a portal cannot be connected.",
              style = MaterialTheme.typography.bodyMedium,
            )
            else -> {
              Spacer(Modifier.height(8.dp))
              Button(onClick = { asking = family }, enabled = !state.busy && snapshot != null) { Text("Connect ${family.name}") }
            }
          }
        }
      }
      HorizontalDivider()
      if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
      state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp)) }
    }
  }

  asking?.let { family ->
    ConsentDialog(family, onAllow = { asking = null; onConnect(family) }, onLater = { asking = null })
  }
}

/** Shown BEFORE the in-app browser opens. Said in full: what is read, where it goes, who can open it, how to undo it. */
@Composable
fun ConsentDialog(family: PortalFamily, onAllow: () -> Unit, onLater: () -> Unit) {
  AlertDialog(
    onDismissRequest = onLater,
    title = { Text("Let draft apply on ${family.name} while you're away?") },
    text = {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        for (line in consentPoints(family)) {
          Text("• $line", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
      }
    },
    confirmButton = { TextButton(onClick = onAllow) { Text("Continue to sign in") } },
    dismissButton = { TextButton(onClick = onLater) { Text("Not now") } },
  )
}

fun consentPoints(family: PortalFamily): List<String> {
  val hosts = family.hosts
  val list = hosts.dropLast(1).joinToString(", ") + " and " + hosts.last()
  return listOf(
    "You sign in to ${family.name} yourself, in a browser inside draft. draft never sees your password.",
    "When you tap \"Let draft apply here\", draft reads that browser's sign-in cookies for $list only — never another site's.",
    "They are sealed on this phone to a key only draft's apply worker holds. draft's servers store them but cannot open them.",
    "The in-app browser is then cleared: nothing of ${family.name} stays on this phone.",
    "Your Draft House mode decides the rest: in SEMI each application waits for your approval; FULL AUTO sends by your standing instruction.",
    "Disconnect here at any time: the key is destroyed and the stored sign-in can never be opened again.",
  )
}
