package com.resumemaster.android.ui.jobs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.models.SwipeAction
import com.resumemaster.android.ui.theme.*
import com.resumemaster.android.util.HapticUtil
import com.resumemaster.android.viewmodel.JobsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun JobsScreen(vm:JobsViewModel=viewModel()){ val jobs by vm.jobs.collectAsState(); val last by vm.lastAction.collectAsState(); val context=LocalContext.current; Scaffold(topBar={CenterAlignedTopAppBar(title={Text("Jobs",style=TitleMedium)},actions={IconButton(onClick={}){Icon(Icons.Rounded.FilterList,contentDescription="Filter")}},colors=TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor=MaterialTheme.colorScheme.background))}){padding-> Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),horizontalAlignment=Alignment.CenterHorizontally){ Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal=22.dp),contentAlignment=Alignment.Center){ CardStackLayout(jobs,Modifier.fillMaxWidth().fillMaxHeight(.85f)){a,j->vm.onSwipe(a,j)}; Box(Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp)){ActionBadge(last)} }; Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.SpaceEvenly){ Act(Icons.Rounded.Close,TextMuted){HapticUtil.dismiss(context);vm.onButtonAction(SwipeAction.Skip)}; Act(Icons.Rounded.Star,Gold){HapticUtil.star(context);vm.onButtonAction(SwipeAction.Star)}; Act(Icons.Rounded.Check,Primary){HapticUtil.trigger(context);vm.onButtonAction(SwipeAction.Queue)} } } } }
@Composable private fun Act(icon:androidx.compose.ui.graphics.vector.ImageVector,tint:androidx.compose.ui.graphics.Color,onClick:()->Unit){ IconButton(onClick=onClick,modifier=Modifier.size(52.dp).background(SurfaceOffset,CircleShape)){Icon(icon,contentDescription=null,tint=tint)} }
