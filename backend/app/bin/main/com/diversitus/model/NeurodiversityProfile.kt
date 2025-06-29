package com.diversitus.model

import kotlinx.serialization.Serializable

@Serializable
data class NeurodiversityProfile(
    val userId: String,
    val conditions: List<String>,
    val accommodations: String,
    val preferences: List<String> // Added to match the matching logic in Routing.kt
)