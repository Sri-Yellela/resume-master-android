package com.resumemaster.android.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AdminUsersScreen(onBack:()->Unit,vm:AdminViewModel=viewModel()){ val users by vm.users.collectAsState(); var filter by remember{mutableStateOf("All")}; var search by remember{mutableStateOf("")}; Scaffold(topBar={TopAppBar(title={Text("Users")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,contentDescription="Back")}})}){padding-> LazyColumn(Modifier.padding(padding).padding(12.dp)){ item{OutlinedTextField(search,{search=it},label={Text("Search")},modifier=Modifier.fillMaxWidth()); androidx.compose.foundation.layout.Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(vertical=10.dp)){listOf("All","Active","Suspended","Admin").forEach{FilterChip(selected=filter==it,onClick={filter=it},label={Text(it)})}}}; items(users.filter{it.name.contains(search,true)||it.email.contains(search,true)}.filter{filter=="All"||(filter=="Active"&&!it.suspended)||(filter=="Suspended"&&it.suspended)||(filter=="Admin"&&it.role=="admin")}){u-> ListItem(headlineContent={Text(u.name)},supportingContent={Text("${u.email} • ${u.joinedDate} • ${u.resumeCount} resumes")},trailingContent={AssistChip(onClick={},label={Text(u.plan)})}); androidx.compose.foundation.layout.Row(Modifier.padding(start=16.dp,bottom=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){TextButton(onClick={vm.suspendUser(u.id)}){Text(if(u.suspended)"Unsuspend" else "Suspend")};TextButton(onClick={vm.deleteUser(u.id)}){Text("Delete")};TextButton(onClick={}){Text("Impersonate read-only")}} } } } }
