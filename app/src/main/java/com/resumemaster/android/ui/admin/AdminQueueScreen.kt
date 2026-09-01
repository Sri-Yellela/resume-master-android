package com.resumemaster.android.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AdminQueueScreen(onBack:()->Unit){ var paused by remember{mutableStateOf(false)}; Scaffold(topBar={TopAppBar(title={Text("Queue")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,contentDescription="Back")}},actions={androidx.compose.foundation.layout.Row{Text(if(paused)"Paused" else "Live");Switch(!paused,{paused=!it})}})}){padding-> LazyColumn(Modifier.padding(padding).padding(12.dp)){ listOf("Pending","Submitted","Failed").forEach{status-> item{Text(status,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=12.dp)); repeat(3){ListItem(headlineContent={Text("Android Engineer at ${listOf("Google","Stripe","Figma")[it]}")},supportingContent={Text("Sri Yellela • queued ${it+1}h ago${if(status=="Failed")" • Browser timeout" else ""}")},trailingContent={AssistChip(onClick={},label={Text(status)})})}} } } } }
