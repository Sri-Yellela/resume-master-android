package com.resumemaster.android.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AdminDashboardScreen(onBack:()->Unit,onNavigate:(String)->Unit){ Scaffold(topBar={TopAppBar(title={Text("Admin")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,contentDescription="Back")}})}){padding-> Column(Modifier.padding(padding).padding(12.dp)){ Row(Icons.Rounded.People,"Users","4 users, 1 suspended"){onNavigate("admin/users")}; Row(Icons.Rounded.Work,"Jobs","5 sources, reported listings"){onNavigate("admin/jobs")}; Row(Icons.Rounded.Queue,"Queue","Global auto-apply status"){onNavigate("admin/queue")}; Row(Icons.Rounded.Flag,"Feature Flags","8 launch flags"){onNavigate("admin/flags")}; Row(Icons.Rounded.Analytics,"Analytics","DAU, resumes, applications"){onNavigate("admin/analytics")} } } }
@Composable private fun Row(icon:ImageVector,title:String,subtitle:String,onClick:()->Unit){ Surface(Modifier.fillMaxWidth().padding(vertical=6.dp).clickable(onClick=onClick),tonalElevation=1.dp){ androidx.compose.foundation.layout.Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){ Icon(icon,contentDescription=null); Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(title);Text(subtitle,style=MaterialTheme.typography.labelSmall)}; Icon(Icons.Rounded.ChevronRight,contentDescription="Open") } } }
