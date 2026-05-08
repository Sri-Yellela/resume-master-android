package com.resumemaster.android.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ProfileScreen(onAdmin:()->Unit){ Scaffold(topBar={CenterAlignedTopAppBar(title={Text("Profile")})}){padding-> Column(Modifier.padding(padding).padding(18.dp)){ Text("Sri Yellela",style=MaterialTheme.typography.titleMedium); Text("Admin • Pro",style=MaterialTheme.typography.bodyMedium); HorizontalDivider(Modifier.padding(vertical=16.dp)); Row(Modifier.fillMaxWidth().clickable(onClick=onAdmin).padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Rounded.AdminPanelSettings,contentDescription=null); Column(Modifier.weight(1f).padding(start=12.dp)){Text("Admin Panel");Text("Users, jobs, queue, flags, analytics",style=MaterialTheme.typography.labelSmall)}; Icon(Icons.Rounded.ChevronRight,contentDescription="Open") } } } }
