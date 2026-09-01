package com.resumemaster.android.models

import java.util.UUID

data class Resume(val id: String = UUID.randomUUID().toString(), val name: String, val sections: List<ResumeSection>, val templateID: String? = null, val lastModified: Long = System.currentTimeMillis())
data class ResumeSection(val id: String = UUID.randomUUID().toString(), val title: String, val fields: List<ResumeField>, val isVisible: Boolean = true, val order: Int)
data class ResumeField(val id: String = UUID.randomUUID().toString(), val label: String, val value: String, val isBold: Boolean = false)
data class AdminUser(val id: String, val name: String, val email: String, val role: String, val plan: String, val suspended: Boolean, val joinedDate: String, val resumeCount: Int)
data class FeatureFlag(val key: String, val description: String, val enabled: Boolean, val platforms: List<String>)
data class AdminAnalytics(val dailyActiveUsers: Int, val resumesToday: Int, val resumesWeek: Int, val resumesTotal: Int, val applicationsToday: Int, val applicationsWeek: Int, val applicationsTotal: Int, val weeklyDau: List<Int>, val templateUse: Map<String, Int>, val swipeRatios: Map<String, Float>)

