package com.diversitus.model

import kotlinx.serialization.Serializable
import java.util.UUID
import java.time.Instant

@Serializable
data class Job(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val title: String,
    val description: String,
    val traits: Map<String, Int>,
    val createdAt: String = Instant.now().toString()
)