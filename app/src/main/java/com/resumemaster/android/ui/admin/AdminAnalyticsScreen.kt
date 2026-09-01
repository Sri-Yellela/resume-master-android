package com.resumemaster.android.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.ui.theme.Primary
import com.resumemaster.android.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AdminAnalyticsScreen(onBack:()->Unit,vm:AdminViewModel=viewModel()){ val a by vm.analytics.collectAsState(); Scaffold(topBar={TopAppBar(title={Text("Analytics")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,contentDescription="Back")}})}){padding-> LazyColumn(Modifier.padding(padding).padding(12.dp)){ item{androidx.compose.foundation.layout.Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Kpi("${a.dailyActiveUsers}","Daily Active Users",Modifier.weight(1f));Kpi("${a.resumesToday}","Resumes Today",Modifier.weight(1f));Kpi("${a.applicationsToday}","Applications Today",Modifier.weight(1f))}}; item{Text("Weekly DAU",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=18.dp));BarChart(a.weeklyDau.map{it.toFloat()})}; item{Text("Template popularity",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=18.dp));BarChart(a.templateUse.values.map{it.toFloat()})}; item{Text("Top swipe action ratios",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=18.dp));Segmented(a.swipeRatios)} } } }
@Composable private fun Kpi(value:String,label:String,modifier:Modifier){Surface(modifier,tonalElevation=1.dp){Column(Modifier.padding(12.dp)){Text(value,style=MaterialTheme.typography.displayLarge);Text("↗ $label",style=MaterialTheme.typography.labelSmall)}}}
@Composable private fun BarChart(values:List<Float>){Canvas(Modifier.fillMaxWidth().height(160.dp).padding(10.dp)){val max=values.maxOrNull()?:1f; val w=size.width/values.size; values.forEachIndexed{i,v->drawRect(Primary,Offset(i*w+6f,size.height-(v/max*size.height)),Size(w-12f,v/max*size.height))}}}
@Composable private fun Segmented(values:Map<String,Float>){Canvas(Modifier.fillMaxWidth().height(42.dp).padding(8.dp)){var x=0f; val colors=listOf(Primary,Color(0xFFD19900),Color(0xFF7A7974)); values.values.forEachIndexed{i,f->val w=size.width*f; drawRect(colors[i%colors.size],Offset(x,0f),Size(w,size.height)); x+=w}}}
