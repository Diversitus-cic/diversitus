package com.diversitus.model

import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: String,
    val companyId: String,
    val title: String,
    val description: String,
    val traits: Map<String, Int>
)