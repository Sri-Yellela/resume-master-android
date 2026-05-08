package com.resumemaster.android.ui.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.data.MockData
import com.resumemaster.android.ui.components.MinimalButton
import com.resumemaster.android.viewmodel.ResumeViewModel

@Composable fun ResumePreviewScreen(vm:ResumeViewModel=viewModel()){ val resume by vm.activeResume.collectAsState(); val context=LocalContext.current; var exporting by remember{mutableStateOf(false)}; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){ Surface(Modifier.fillMaxWidth().aspectRatio(1f/1.414f),tonalElevation=2.dp){ Column(Modifier.padding(22.dp)){ Text(resume.name,style=MaterialTheme.typography.displayLarge); resume.sections.filter{it.isVisible}.forEach{s->Text(s.title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=12.dp)); s.fields.forEach{Text("${it.label}: ${it.value}",style=MaterialTheme.typography.bodyMedium)} } } }; MinimalButton(text=if(exporting)"Exporting..." else "Export as PDF",enabled=!exporting,onClick={ exporting=true; val uri=PdfExporter.exportToPdf(resume,MockData.templates.first{it.id==(resume.templateID?:"modern")},context); exporting=false; PdfExporter.share(uri,context) }); if(exporting)CircularProgressIndicator(Modifier.padding(16.dp)) } }
