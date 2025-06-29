package com.diversitus.model

import kotlinx.serialization.Serializable

@Serializable
data class NeurodiversityProfile(
    val preferences: List<String>
)