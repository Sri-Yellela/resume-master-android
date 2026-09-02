package com.resumemaster.android.models

import java.util.UUID

// matchScore is NULLABLE and is INTERNAL.
//
// Nullable because null is a real answer, not a missing one: the scorer declines when it has no
// basis for an opinion, and that decline is its own band ("Not enough signal"). It was a non-null
// Int here, so the only way to represent a decline was 0 — which would render the engine's most
// honest answer as the worst grade it has.
//
// Internal because the contract says so, verbatim: "DO NOT DISPLAY THIS NUMBER". Render the band
// from AtsBands.bandFor(score). See AtsBands for why, and for the one sanctioned numeric use.
data class Job(val id: String = UUID.randomUUID().toString(), val company: String, val role: String, val location: String, val salary: String? = null, val tags: List<String> = emptyList(), val matchScore: Int? = null, val logoColor: String, val description: String, val postedDate: Long = System.currentTimeMillis())

