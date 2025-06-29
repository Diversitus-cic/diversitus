package com.diversitus.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchResult(
    val job: Job,
    val company: Company,
    val score: Double,
)