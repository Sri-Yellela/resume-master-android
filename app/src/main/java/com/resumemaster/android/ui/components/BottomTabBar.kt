package com.resumemaster.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.resumemaster.android.ui.theme.TextFaint

data class BottomTab(val route:String,val icon:ImageVector)
val bottomTabs=listOf(BottomTab("jobs",Icons.Rounded.Work),BottomTab("resume",Icons.Rounded.Description),BottomTab("templates",Icons.Rounded.Style),BottomTab("profile",Icons.Rounded.Person))
@Composable fun BottomTabBar(currentRoute:String,onNavigate:(String)->Unit){ NavigationBar(containerColor=MaterialTheme.colorScheme.surface){ bottomTabs.forEach{tab-> NavigationBarItem(selected=currentRoute==tab.route,onClick={onNavigate(tab.route)},icon={Icon(tab.icon,contentDescription=tab.route)},alwaysShowLabel=false,colors=NavigationBarItemDefaults.colors(selectedIconColor=MaterialTheme.colorScheme.primary,indicatorColor=MaterialTheme.colorScheme.primary.copy(alpha=.12f),unselectedIconColor=TextFaint)) } } }

