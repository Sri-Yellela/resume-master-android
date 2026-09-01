package com.resumemaster.android.models

import java.util.UUID

data class Job(val id: String = UUID.randomUUID().toString(), val company: String, val role: String, val location: String, val salary: String? = null, val tags: List<String> = emptyList(), val matchScore: Int, val logoColor: String, val description: String, val postedDate: Long = System.currentTimeMillis())

