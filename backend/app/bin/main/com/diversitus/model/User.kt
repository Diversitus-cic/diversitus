package com.diversitus.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val profile: NeurodiversityProfile
)