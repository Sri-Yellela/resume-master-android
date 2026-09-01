package com.resumemaster.android.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AdminJobsScreen(onBack:()->Unit){ val sources=remember{mutableStateListOf("Harvest API" to true,"LinkedIn Import" to true,"Manual Listings" to true)}; Scaffold(topBar={TopAppBar(title={Text("Jobs")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,contentDescription="Back")}})},floatingActionButton={FloatingActionButton(onClick={}){Icon(Icons.Rounded.Add,contentDescription="Add listing")}}){padding-> LazyColumn(Modifier.padding(padding).padding(12.dp)){ items(sources.size){i->val item=sources[i]; ListItem(headlineContent={Text(item.first)},supportingContent={Text("Status: ${if(item.second)"on" else "off"} • last sync today • 128 listings")},trailingContent={Switch(item.second,{sources[i]=item.first to it})})}; item{Text("Flagged listings",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=18.dp)); listOf("Duplicate posting at Stripe","Expired Google listing").forEach{ListItem(headlineContent={Text(it)},supportingContent={Text("Swipe or tap remove listing")},trailingContent={TextButton(onClick={}){Text("Remove")}})}} } } }
