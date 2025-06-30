package com.diversitus.model

import kotlinx.serialization.Serializable
import com.diversitus.model.Company

@Serializable
data class MatchResult(
    val job: Job,
    val company: Company,
    val score: Double,
)