package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import com.resumemaster.android.AppGraph
import com.resumemaster.android.auth.LinkedInResumeFields
import com.resumemaster.android.data.ResumeRepository
import com.resumemaster.android.models.Resume
import com.resumemaster.android.models.ResumeField
import com.resumemaster.android.models.ResumeSection
import kotlinx.coroutines.flow.StateFlow

/**
 * The resume builder.
 *
 * `activeResume` is NULL until the stored resume has been read off disk. It used to be non-null and
 * seeded from mock data, which meant the first frame showed a resume that was not the user's — and
 * any edit made in that window was written over the real one. Screens render a loading state
 * instead; see ResumeRepository for why that window exists at all.
 *
 * The repository is SHARED via AppGraph rather than constructed here. `viewModel()` scopes to the
 * NavBackStackEntry, so a repository owned by this class gave the builder and the preview a
 * different resume each.
 */
class ResumeViewModel(
    private val repository: ResumeRepository = AppGraph.resume,
) : ViewModel() {
    val activeResume: StateFlow<Resume?> = repository.resume
    val ready: StateFlow<Boolean> = repository.ready

    fun updateField(sectionId: String, fieldId: String, value: String) = mutate { r ->
        r.copy(sections = r.sections.map { s ->
            if (s.id == sectionId) s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(value = value) else it }) else s
        })
    }

    fun updateFieldLabel(sectionId: String, fieldId: String, label: String) = mutate { r ->
        r.copy(sections = r.sections.map { s ->
            if (s.id == sectionId) s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(label = label) else it }) else s
        })
    }

    fun updateSectionTitle(sectionId: String, title: String) = mutate { it.copy(sections = it.sections.map { s -> if (s.id == sectionId) s.copy(title = title) else s }) }
    fun toggleSectionVisible(sectionId: String) = mutate { it.copy(sections = it.sections.map { s -> if (s.id == sectionId) s.copy(isVisible = !s.isVisible) else s }) }
    fun deleteSection(sectionId: String) = mutate { it.copy(sections = it.sections.filterNot { s -> s.id == sectionId }.mapIndexed { i, s -> s.copy(order = i) }) }
    fun addField(sectionId: String) = mutate { it.copy(sections = it.sections.map { s -> if (s.id == sectionId) s.copy(fields = s.fields + ResumeField(label = "New Field", value = "")) else s }) }
    fun deleteField(sectionId: String, fieldId: String) = mutate { it.copy(sections = it.sections.map { s -> if (s.id == sectionId) s.copy(fields = s.fields.filterNot { f -> f.id == fieldId }) else s }) }
    fun reorderSections(fromIndex: Int, toIndex: Int) = mutate { r ->
        val list = r.sections.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        r.copy(sections = list.mapIndexed { i, s -> s.copy(order = i) })
    }

    fun onLinkedInImport(fields: LinkedInResumeFields) = mutate { resume ->
        val updatedSections = resume.sections.toMutableList()
        val contactIndex = updatedSections.indexOfFirst { it.title.contains("contact", ignoreCase = true) || it.title.contains("summary", ignoreCase = true) }
        if (contactIndex >= 0) {
            val section = updatedSections[contactIndex]
            val emailIndex = section.fields.indexOfFirst { it.label.contains("email", ignoreCase = true) }
            val fieldsList = section.fields.toMutableList()
            if (emailIndex >= 0) fieldsList[emailIndex] = fieldsList[emailIndex].copy(value = fields.email)
            else if (fields.email.isNotBlank()) fieldsList.add(0, ResumeField(label = "Email", value = fields.email))
            updatedSections[contactIndex] = section.copy(fields = fieldsList)
        } else if (fields.email.isNotBlank()) {
            updatedSections.add(0, ResumeSection(title = "Contact", fields = listOf(ResumeField(label = "Email", value = fields.email)), order = 0))
        }
        resume.copy(name = fields.name.ifBlank { resume.name }, sections = updatedSections.mapIndexed { i, s -> s.copy(order = i) })
    }

    fun save() { activeResume.value?.let(repository::save) }

    /**
     * Apply an edit to the loaded resume.
     *
     * A no-op before hydration finishes. Dropping the edit is the correct branch of a choice
     * between two bad options: the alternative is to apply it to a resume we have not read yet,
     * which persists a mutation of the SEED over the user's stored work. Dropping loses a keystroke
     * in a window the user cannot realistically type in, because the screens do not render an
     * editable field until `ready`.
     */
    private fun mutate(block: (Resume) -> Resume) {
        val current = activeResume.value ?: return
        repository.save(block(current))
    }
}
