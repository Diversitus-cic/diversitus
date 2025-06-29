package com.diversitus.model

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: String,
    val name: String,
    val traits: Map<String, Int>
)