package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import com.resumemaster.android.AppGraph
import com.resumemaster.android.data.MockData
import com.resumemaster.android.data.ResumeRepository
import com.resumemaster.android.models.Resume
import kotlinx.coroutines.flow.StateFlow

/**
 * The template picker.
 *
 * THE SELECTION IS THE RESUME'S templateID, not a flow of its own. It used to be
 * `MutableStateFlow("modern")` owned by this class and read by nothing else: the picker highlighted
 * the card you tapped, and the preview and PDF export -- which read `resume.templateID` -- carried on
 * in whatever template the resume already had. A picker wired to nothing. It also forgot the choice
 * on every navigation, because `viewModel()` scopes to the back-stack entry. TemplatesViewModelTest
 * pins it.
 */
class TemplatesViewModel(
    private val repository: ResumeRepository = AppGraph.resume,
) : ViewModel() {
    val templates = MockData.templates
    val resume: StateFlow<Resume?> = repository.resume

    /** A no-op before hydration, for the same reason every ResumeViewModel edit is. */
    fun select(id: String) {
        val current = repository.resume.value ?: return
        if (current.templateID != id) repository.save(current.copy(templateID = id))
    }
}

