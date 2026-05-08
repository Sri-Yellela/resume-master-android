package com.resumemaster.android.ui.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.ui.theme.Primary
import com.resumemaster.android.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AdminFlagsScreen(onBack:()->Unit,vm:AdminViewModel=viewModel()){ val flags by vm.flags.collectAsState(); val snackbar=remember{SnackbarHostState()}; val scope=rememberCoroutineScope(); Scaffold(snackbarHost={SnackbarHost(snackbar)},topBar={TopAppBar(title={Text("Feature Flags")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,contentDescription="Back")}})}){padding-> LazyColumn(Modifier.padding(padding).padding(12.dp)){ items(flags,key={it.key}){f-> val bg by animateColorAsState(if(f.enabled)Primary.copy(alpha=.08f) else MaterialTheme.colorScheme.surface,label="flagBg"); Surface(Modifier.fillMaxWidth().padding(vertical=6.dp),color=bg,tonalElevation=1.dp){ androidx.compose.foundation.layout.Row(Modifier.padding(14.dp)){ Column(Modifier.weight(1f)){Text(f.key,fontFamily=FontFamily.Monospace,style=MaterialTheme.typography.labelSmall);Text(f.description,style=MaterialTheme.typography.bodyMedium); androidx.compose.foundation.layout.Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(top=8.dp)){f.platforms.forEach{AssistChip(onClick={},label={Text(it)})}}}; Switch(checked=f.enabled,onCheckedChange={enabled->vm.updateFlag(f.key,enabled);scope.launch{snackbar.showSnackbar("${f.key} updated — changes apply on next app open across all platforms")}}) } } } } } }
