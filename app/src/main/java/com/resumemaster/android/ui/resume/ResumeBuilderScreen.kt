package com.resumemaster.android.ui.resume

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.auth.LinkedInAuthManager
import com.resumemaster.android.models.ResumeSection
import com.resumemaster.android.viewmodel.ResumeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeBuilderScreen(vm: ResumeViewModel = viewModel()) {
    val resume by vm.activeResume.collectAsState()
    val pendingImport by LinkedInAuthManager.pendingImport.collectAsState()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<ResumeSection?>(null) }
    var importNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingImport) {
        pendingImport?.let { fields ->
            vm.onLinkedInImport(fields)
            LinkedInAuthManager.consumeImport()
            importNotice = "Name and email imported from LinkedIn"
        }
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Resume") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    TextButton(onClick = { LinkedInAuthManager.startImport(context) }) {
                        Text("Import from LinkedIn")
                    }
                    Text(
                        "Import your name and email from LinkedIn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    importNotice?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            items(resume.sections.sortedBy { it.order }, key = { it.id }) { section ->
                SectionRow(
                    section,
                    { editing = section },
                    { vm.toggleSectionVisible(section.id) },
                    { vm.deleteSection(section.id) }
                )
            }
        }
        editing?.let { section ->
            SectionEditorSheet(
                section,
                { editing = null },
                {
                    vm.updateSectionTitle(section.id, it)
                    editing = vm.activeResume.value.sections.first { updated -> updated.id == section.id }
                },
                { id, label -> vm.updateFieldLabel(section.id, id, label) },
                { id, value -> vm.updateField(section.id, id, value) },
                { vm.deleteField(section.id, it) },
                { vm.addField(section.id) }
            )
        }
    }
}
