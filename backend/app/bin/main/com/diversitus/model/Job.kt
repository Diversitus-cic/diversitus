package com.diversitus.model

import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: String = "",
    val title: String,
    val company: String,
    val requirements: Map<String, Int>,
    val benefits: List<String>
)