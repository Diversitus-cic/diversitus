package com.diversitus.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    var id: String = UUID.randomUUID().toString(),
    val name: String,
    val profile: NeurodiversityProfile
)