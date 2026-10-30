package com.resumemaster.android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.resumemaster.android.AppGraph
import com.resumemaster.android.ui.admin.*
import com.resumemaster.android.ui.auth.LoginScreen
import com.resumemaster.android.ui.components.BottomTabBar
import com.resumemaster.android.ui.components.bottomTabs
import com.resumemaster.android.ui.jobs.JobsScreen
import com.resumemaster.android.data.portals.PortalFamily
import com.resumemaster.android.ui.portals.PortalConnectScreen
import com.resumemaster.android.ui.portals.PortalsScreen
import com.resumemaster.android.ui.preview.ResumePreviewScreen
import com.resumemaster.android.ui.profile.ProfileScreen
import com.resumemaster.android.ui.resume.ResumeBuilderScreen
import com.resumemaster.android.ui.templates.TemplatesScreen

@Composable fun NavGraph(){ val nav=rememberNavController(); val start=remember{startDestination()}; val back by nav.currentBackStackEntryAsState(); val route=back?.destination?.route?:"jobs"; val showTabs=bottomTabs.any{it.route==route}; Scaffold(bottomBar={if(showTabs)BottomTabBar(route){target->nav.navigate(target){popUpTo(nav.graph.findStartDestination().id){saveState=true};launchSingleTop=true;restoreState=true}}}){padding-> NavHost(navController=nav,startDestination=start,modifier=Modifier.padding(padding)){ composable("login"){LoginScreen(onSignedIn={nav.navigate("jobs"){popUpTo("login"){inclusive=true}}})}; composable(LOGIN_EXPIRED){LoginScreen(notice="Your session has ended. Sign in again.",onSignedIn={nav.navigate("jobs"){popUpTo(LOGIN_EXPIRED){inclusive=true}}})}; composable("jobs"){JobsScreen(onSessionExpired={toLogin(nav,LOGIN_EXPIRED)})}; composable("resume"){ResumeBuilderScreen()}; composable("templates"){TemplatesScreen()}; composable("profile"){ProfileScreen(onAdmin={nav.navigate("admin")},onSignedOut={toLogin(nav,"login")},onPortals={nav.navigate("portals")})}; composable("portals"){PortalsScreen(onBack={nav.popBackStack()},onConnect={f->nav.navigate("portals/connect/${f.ats}")})}; composable("portals/connect/{ats}"){entry->val family=PortalFamily.byAts(entry.arguments?.getString("ats")); if(family==null||!AppGraph.portals.isOffered){LaunchedEffect(Unit){nav.popBackStack()}}else{PortalConnectScreen(family,onDone={nav.popBackStack()})}}; composable("preview"){ResumePreviewScreen()}; composable("admin"){AdminDashboardScreen(onBack={nav.popBackStack()},onNavigate={nav.navigate(it)})}; composable("admin/users"){AdminUsersScreen(onBack={nav.popBackStack()})}; composable("admin/jobs"){AdminJobsScreen(onBack={nav.popBackStack()})}; composable("admin/queue"){AdminQueueScreen(onBack={nav.popBackStack()})}; composable("admin/flags"){AdminFlagsScreen(onBack={nav.popBackStack()})}; composable("admin/analytics"){AdminAnalyticsScreen(onBack={nav.popBackStack()})} } } }

/**
 * Leave every signed-in screen for sign-in (sign-out, or a session the server ended), with nothing to go Back to.
 *
 * Popping the WHOLE graph, not just the current destination: Back from sign-in must not return to a
 * board that has no credential behind it. The jobs destination is popped with it, so the next
 * sign-in builds a new JobsViewModel, which restarts the shared feed (see JobsViewModel.init).
 */
private fun toLogin(nav: NavHostController, route: String) {
  nav.navigate(route) { popUpTo(nav.graph.id) { inclusive = true } }
}

/**
 * Sign-in, arrived at because the SERVER ended the session (a 401), not because the user signed
 * out. A separate route rather than an argument so the start destination stays the plain "login".
 */
private const val LOGIN_EXPIRED = "login/expired"

/**
 * Read ONCE per NavGraph, not on every recomposition.
 *
 * It was an expression in the body, so it re-ran on every back-stack change: after a sign-in it
 * flipped from "login" to "jobs", NavHost was handed a new graph, and the jobs destination was built
 * a second time -- a second JobsViewModel and a second page-1 request (seen on the emulator as two
 * identical GET /api/jobs per sign-in). It also meant a keystore read on the main thread per
 * recomposition.
 */
private fun startDestination(): String = if (AppGraph.tokenStore.hasToken()) "jobs" else "login"
