package com.diversitus.model

import kotlinx.serialization.Serializable

@Serializable
data class Trait(
    val id: String,
    val name: String,
    val description: String,
    val category: TraitCategory,
    val isActive: Boolean = true,
    val minValue: Int = 1,
    val maxValue: Int = 10,
    val examples: List<String> = emptyList()
)

@Serializable
enum class TraitCategory {
    COGNITIVE,      // Mental processing and thinking styles
    SOCIAL,         // Interpersonal and communication
    ENVIRONMENTAL,  // Workspace and physical environment  
    WORK_STYLE,     // Work preferences and approaches
    EMOTIONAL       // Emotional processing and regulation
}

@Serializable
data class TraitLibrary(
    val traits: List<Trait>,
    val version: String,
    val lastUpdated: String
)