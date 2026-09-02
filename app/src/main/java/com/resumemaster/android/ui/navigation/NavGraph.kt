package com.resumemaster.android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.resumemaster.android.AppGraph
import com.resumemaster.android.ui.admin.*
import com.resumemaster.android.ui.auth.LoginScreen
import com.resumemaster.android.ui.components.BottomTabBar
import com.resumemaster.android.ui.jobs.JobsScreen
import com.resumemaster.android.ui.preview.ResumePreviewScreen
import com.resumemaster.android.ui.profile.ProfileScreen
import com.resumemaster.android.ui.resume.ResumeBuilderScreen
import com.resumemaster.android.ui.templates.TemplatesScreen

@Composable fun NavGraph(){ val nav=rememberNavController(); val start=if(AppGraph.tokenStore.hasToken()) "jobs" else "login"; val back by nav.currentBackStackEntryAsState(); val route=back?.destination?.route?:"jobs"; val showTabs=route in setOf("jobs","resume","templates","profile"); Scaffold(bottomBar={if(showTabs)BottomTabBar(route){target->nav.navigate(target){popUpTo(nav.graph.findStartDestination().id){saveState=true};launchSingleTop=true;restoreState=true}}}){padding-> NavHost(navController=nav,startDestination=start,modifier=Modifier.padding(padding)){ composable("login"){LoginScreen(onSignedIn={nav.navigate("jobs"){popUpTo("login"){inclusive=true}}})}; composable("jobs"){JobsScreen()}; composable("resume"){ResumeBuilderScreen()}; composable("templates"){TemplatesScreen()}; composable("profile"){ProfileScreen{nav.navigate("admin")}}; composable("preview"){ResumePreviewScreen()}; composable("admin"){AdminDashboardScreen(onBack={nav.popBackStack()},onNavigate={nav.navigate(it)})}; composable("admin/users"){AdminUsersScreen(onBack={nav.popBackStack()})}; composable("admin/jobs"){AdminJobsScreen(onBack={nav.popBackStack()})}; composable("admin/queue"){AdminQueueScreen(onBack={nav.popBackStack()})}; composable("admin/flags"){AdminFlagsScreen(onBack={nav.popBackStack()})}; composable("admin/analytics"){AdminAnalyticsScreen(onBack={nav.popBackStack()})} } } }
