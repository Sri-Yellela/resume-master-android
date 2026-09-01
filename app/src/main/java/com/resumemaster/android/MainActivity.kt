package com.resumemaster.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.resumemaster.android.auth.LinkedInAuthManager
import com.resumemaster.android.ui.navigation.NavGraph
import com.resumemaster.android.ui.theme.ResumeMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        LinkedInAuthManager.pendingImport.value = LinkedInAuthManager.handleAuthCallback(intent)
        setContent { ResumeMasterTheme { NavGraph() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        LinkedInAuthManager.pendingImport.value = LinkedInAuthManager.handleAuthCallback(intent)
    }
}
