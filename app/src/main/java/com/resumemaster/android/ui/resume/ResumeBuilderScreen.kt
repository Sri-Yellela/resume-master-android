package com.resumemaster.android.ui.resume

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.models.ResumeSection
import com.resumemaster.android.viewmodel.ResumeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ResumeBuilderScreen(vm:ResumeViewModel=viewModel()){ val resume by vm.activeResume.collectAsState(); var editing by remember{ mutableStateOf<ResumeSection?>(null) }; Scaffold(topBar={CenterAlignedTopAppBar(title={Text("Resume")})}){padding-> LazyColumn(Modifier.padding(padding).padding(12.dp)){ items(resume.sections.sortedBy{it.order},key={it.id}){s->SectionRow(s,{editing=s},{vm.toggleSectionVisible(s.id)},{vm.deleteSection(s.id)})} }; editing?.let{s-> SectionEditorSheet(s,{editing=null},{vm.updateSectionTitle(s.id,it); editing=vm.activeResume.value.sections.first{x->x.id==s.id}},{id,label->vm.updateFieldLabel(s.id,id,label)},{id,value->vm.updateField(s.id,id,value)},{vm.deleteField(s.id,it)},{vm.addField(s.id)}) } } }
