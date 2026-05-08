package com.resumemaster.android.ui.templates

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.viewmodel.TemplatesViewModel

@OptIn(ExperimentalMaterial3Api::class,ExperimentalFoundationApi::class)
@Composable fun TemplatesScreen(vm:TemplatesViewModel=viewModel()){ val selected by vm.selectedTemplateId.collectAsState(); val state=rememberLazyListState(); Scaffold(topBar={CenterAlignedTopAppBar(title={Text("Templates")})}){padding-> LazyRow(Modifier.padding(padding).padding(vertical=24.dp),state=state,flingBehavior=rememberSnapFlingBehavior(state),horizontalArrangement=Arrangement.spacedBy(18.dp)){ items(vm.templates.size){i->TemplateCard(vm.templates[i],selected==vm.templates[i].id){vm.select(vm.templates[i].id)}} } } }
