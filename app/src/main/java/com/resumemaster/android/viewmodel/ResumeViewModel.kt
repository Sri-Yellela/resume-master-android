package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import com.resumemaster.android.auth.LinkedInResumeFields
import com.resumemaster.android.data.ResumeRepository
import com.resumemaster.android.models.Resume
import com.resumemaster.android.models.ResumeField
import com.resumemaster.android.models.ResumeSection
import kotlinx.coroutines.flow.StateFlow

class ResumeViewModel : ViewModel() {
    private val repository = ResumeRepository()
    val activeResume: StateFlow<Resume> = repository.resume

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

    fun save() { repository.save(activeResume.value) }
    private fun mutate(block: (Resume) -> Resume) { repository.save(block(activeResume.value)) }
}
