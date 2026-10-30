package com.resumemaster.android.ui.portals

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.data.portals.CookieSource
import com.resumemaster.android.data.portals.FamilyCookies
import com.resumemaster.android.data.portals.PortalFamily
import com.resumemaster.android.viewmodel.PortalsViewModel
import kotlinx.coroutines.delay

/**
 * D112 item 7 — the in-app browser on a portal's sign-in. The user signs in themselves; draft never sees the
 * password. When the page shows the user is signed in (a sign-out control on the page, or an account page URL),
 * "Let draft apply here" seals the family's cookies to the worker's verified key and uploads ciphertext only.
 *
 * ⛔ The WebView is CLEARED when this screen leaves — after a capture, on Close and on Back alike: every cookie in
 * the app's WebView jar (this app has no other WebView: LinkedIn uses Custom Tabs), web storage, cache, history.
 * It is also cleared on entry, so a previous attempt's sign-in can never be the one captured.
 *
 * ⚠ The real amazon.jobs flow was not exercised from here (no emulator in this lane): the sign-in page, the
 * signed-in signals and whether Amazon's sign-in accepts an Android WebView are unverified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PortalConnectScreen(family: PortalFamily, onDone: () -> Unit, vm: PortalsViewModel = viewModel()) {
  val state by vm.state.collectAsState()
  var webView by remember { mutableStateOf<WebView?>(null) }
  var signedIn by remember { mutableStateOf(false) }
  var host by remember { mutableStateOf<String?>(null) }

  fun check(view: WebView?) {
    val url = view?.url
    host = runCatching { java.net.URI(url).host }.getOrNull()
    if (!family.owns(url)) { signedIn = false; return }
    if (family.isAccountPage(url)) { signedIn = true; return }
    view?.evaluateJavascript(FamilyCookies.SIGNED_IN_SCRIPT) { result -> signedIn = result == "true" }
  }

  DisposableEffect(Unit) {
    clearWebState(null)
    onDispose { clearWebState(webView); webView = null }
  }
  // Single-page sign-ins change the page without a load; look again every two seconds while open.
  LaunchedEffect(webView) { while (webView != null) { delay(2_000); check(webView) } }
  LaunchedEffect(state.connectedFamily) {
    if (state.connectedFamily == family.ats) { vm.consumeConnected(); onDone() }
  }

  Scaffold(topBar = {
    TopAppBar(
      title = { Column { Text("Sign in to ${family.name}"); host?.let { Text(it, style = MaterialTheme.typography.labelSmall) } } },
      navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, contentDescription = "Close") } },
    )
  }, bottomBar = {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
      state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)) }
      Button(
        onClick = { vm.connect(family, CookieSource { CookieManager.getInstance().getCookie(it) }) },
        enabled = signedIn && !state.busy,
        modifier = Modifier.fillMaxWidth(),
      ) { Text(if (state.busy) "Sealing…" else "Let draft apply here") }
      Text(
        if (signedIn) "Only ${family.name}'s sign-in is read, sealed on this phone, then this browser is cleared."
        else "Sign in above. The button wakes when ${family.name} shows you are signed in.",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 6.dp),
      )
    }
  }) { padding ->
    AndroidView(
      modifier = Modifier.padding(padding).fillMaxSize(),
      factory = { ctx ->
        WebView(ctx).apply {
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          settings.allowFileAccess = false
          settings.allowContentAccess = false
          settings.setSupportMultipleWindows(false)
          CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
          webViewClient = object : WebViewClient() {
            // https only — a portal's sign-in has no business on plain http or a custom scheme.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
              request.url.scheme != "https"
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) { signedIn = false; check(view) }
            override fun onPageFinished(view: WebView, url: String?) { check(view) }
          }
          loadUrl(family.signInUrl)
          webView = this
        }
      },
    )
  }
}

/** Everything the app's WebViews hold — the cookie jar, web storage, and (given a view) its cache and history. */
private fun clearWebState(view: WebView?) {
  CookieManager.getInstance().removeAllCookies(null)
  CookieManager.getInstance().flush()
  WebStorage.getInstance().deleteAllData()
  view?.apply {
    stopLoading()
    clearCache(true)
    clearHistory()
    clearFormData()
    destroy()
  }
}
