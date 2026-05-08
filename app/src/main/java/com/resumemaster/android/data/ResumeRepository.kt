package com.resumemaster.android.data

import com.resumemaster.android.models.Resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ResumeRepository { private val _resume=MutableStateFlow(MockData.defaultResume); val resume:StateFlow<Resume> = _resume; fun save(resume:Resume){_resume.value=resume.copy(lastModified=System.currentTimeMillis())} }

