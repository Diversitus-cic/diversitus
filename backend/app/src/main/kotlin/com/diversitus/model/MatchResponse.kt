package com.diversitus.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchResponse(
    val matches: List<MatchResult>,
    val analysis: MatchAnalysis
)

@Serializable
data class MatchAnalysis(
    val totalJobsAnalyzed: Int,
    val matchesFound: Int,
    val feedback: List<String>,
    val traitCoverage: Map<String, TraitCoverage>
)

@Serializable
data class TraitCoverage(
    val userValue: Int,
    val jobsWithTrait: Int,
    val averageJobValue: Double?,
    val minJobValue: Int?,
    val maxJobValue: Int?,
    val suggestion: String?
)