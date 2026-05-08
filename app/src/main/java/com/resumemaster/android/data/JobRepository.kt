package com.resumemaster.android.data

import com.resumemaster.android.models.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class JobRepository { private val _jobs=MutableStateFlow(MockData.jobs); val jobs:StateFlow<List<Job>> = _jobs; fun rotate(job:Job){_jobs.value=_jobs.value.filterNot{it.id==job.id}+job.copy(id=job.id+"-next", postedDate=System.currentTimeMillis())} }

