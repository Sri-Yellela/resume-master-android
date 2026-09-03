package com.resumemaster.android.ui.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.data.MockData
import com.resumemaster.android.ui.components.MinimalButton
import com.resumemaster.android.viewmodel.ResumeViewModel

// The preview now shows the USER'S resume, not the mock one.
//
// It never did before: `viewModel()` scopes to the NavBackStackEntry, so this screen's
// ResumeViewModel built its own ResumeRepository seeded from MockData — independent of the one the
// builder was editing. Every edit was invisible here, and "Export as PDF" wrote the mock resume to
// a file the user then shared. The repository is shared through AppGraph now.
//
// The null branch is the pre-hydration window: exporting a PDF of a resume not yet read from disk
// would produce a plausible-looking document with the wrong contents.
@Composable fun ResumePreviewScreen(vm:ResumeViewModel=viewModel()){ val loaded by vm.activeResume.collectAsState(); val resume=loaded; if(resume==null){ Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}; return }; val context=LocalContext.current; var exporting by remember{mutableStateOf(false)}; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){ Surface(Modifier.fillMaxWidth().aspectRatio(1f/1.414f),tonalElevation=2.dp){ Column(Modifier.padding(22.dp)){ Text(resume.name,style=MaterialTheme.typography.displayLarge); resume.sections.filter{it.isVisible}.forEach{s->Text(s.title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=12.dp)); s.fields.forEach{Text("${it.label}: ${it.value}",style=MaterialTheme.typography.bodyMedium)} } } }; MinimalButton(text=if(exporting)"Exporting..." else "Export as PDF",enabled=!exporting,onClick={ exporting=true; val uri=PdfExporter.exportToPdf(resume,MockData.templates.first{it.id==(resume.templateID?:"modern")},context); exporting=false; PdfExporter.share(uri,context) }); if(exporting)CircularProgressIndicator(Modifier.padding(16.dp)) } }
