package com.diversitus.model

import kotlinx.serialization.Serializable

@Serializable
data class NeurodiversityProfile(
    val traits: Map<String, Int>
)