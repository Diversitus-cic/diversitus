package com.diversitus.service

import com.diversitus.model.Trait
import com.diversitus.model.TraitCategory
import com.diversitus.model.TraitLibrary
import java.time.Instant

class TraitLibraryService {
    
    companion object {
        const val LIBRARY_VERSION = "1.0.0"
        
        // Official Diversitus trait definitions
        private val OFFICIAL_TRAITS = listOf(
            Trait(
                id = "attention_to_detail",
                name = "Attention to Detail",
                description = "Noticing fine details and accuracy in work",
                category = TraitCategory.COGNITIVE,
                examples = listOf(
                    "Thorough code reviews and testing",
                    "Precise documentation and specifications", 
                    "Quality assurance and error detection"
                )
            ),
            Trait(
                id = "autonomy",
                name = "Autonomy",
                description = "Working independently and making decisions",
                category = TraitCategory.WORK_STYLE,
                examples = listOf(
                    "Self-directed project management",
                    "Independent problem-solving",
                    "Minimal supervision required"
                )
            ),
            Trait(
                id = "collaboration",
                name = "Collaboration", 
                description = "Working effectively with others in teams",
                category = TraitCategory.SOCIAL,
                examples = listOf(
                    "Team-based projects and brainstorming",
                    "Cross-functional communication",
                    "Pair programming and code reviews"
                )
            ),
            Trait(
                id = "deep_focus",
                name = "Deep Focus",
                description = "Maintaining concentration for extended periods",
                category = TraitCategory.COGNITIVE,
                examples = listOf(
                    "Complex algorithm development",
                    "Long coding sessions",
                    "Research and analysis tasks"
                )
            ),
            Trait(
                id = "empathy",
                name = "Empathy",
                description = "Understanding and relating to others' emotions",
                category = TraitCategory.EMOTIONAL,
                examples = listOf(
                    "User experience design",
                    "Customer support and relations",
                    "Team leadership and mentoring"
                )
            ),
            Trait(
                id = "pattern_recognition",
                name = "Pattern Recognition",
                description = "Identifying patterns and connections",
                category = TraitCategory.COGNITIVE,
                examples = listOf(
                    "Data analysis and insights",
                    "System architecture design",
                    "Debugging and troubleshooting"
                )
            ),
            Trait(
                id = "problem_solving",
                name = "Problem Solving",
                description = "Finding solutions to complex challenges",
                category = TraitCategory.COGNITIVE,
                examples = listOf(
                    "Technical troubleshooting",
                    "Algorithm optimization",
                    "Creative solution development"
                )
            ),
            Trait(
                id = "quiet_office",
                name = "Quiet Office",
                description = "Low noise, minimal distractions workspace",
                category = TraitCategory.ENVIRONMENTAL,
                examples = listOf(
                    "Noise-cancelling headphones provided",
                    "Private or semi-private workspace",
                    "Minimal interruptions policy"
                )
            ),
            Trait(
                id = "systematic_thinking",
                name = "Systematic Thinking",
                description = "Approaching problems methodically",
                category = TraitCategory.COGNITIVE,
                examples = listOf(
                    "Structured development processes",
                    "Step-by-step problem breakdown",
                    "Methodical testing approaches"
                )
            ),
            Trait(
                id = "visual_thinking",
                name = "Visual Thinking",
                description = "Processing and understanding visual information",
                category = TraitCategory.COGNITIVE,
                examples = listOf(
                    "UI/UX design and prototyping",
                    "Data visualization",
                    "Diagram-based communication"
                )
            ),
            Trait(
                id = "work_life_balance",
                name = "Work Life Balance",
                description = "Maintaining healthy balance between work and personal life",
                category = TraitCategory.WORK_STYLE,
                examples = listOf(
                    "Flexible working hours",
                    "Respect for personal time",
                    "Mental health support"
                )
            ),
            Trait(
                id = "working_from_home",
                name = "Working from Home",
                description = "Remote work capabilities and preferences",
                category = TraitCategory.ENVIRONMENTAL,
                examples = listOf(
                    "Full remote work options",
                    "Flexible location policies",
                    "Digital collaboration tools"
                )
            )
        )
    }
    
    fun getTraitLibrary(): TraitLibrary {
        return TraitLibrary(
            traits = OFFICIAL_TRAITS,
            version = LIBRARY_VERSION,
            lastUpdated = Instant.now().toString()
        )
    }
    
    fun getTraitById(id: String): Trait? {
        return OFFICIAL_TRAITS.find { it.id == id }
    }
    
    fun getTraitsByCategory(category: TraitCategory): List<Trait> {
        return OFFICIAL_TRAITS.filter { it.category == category }
    }
    
    fun getAllTraitIds(): List<String> {
        return OFFICIAL_TRAITS.map { it.id }
    }
    
    fun isValidTraitId(id: String): Boolean {
        return OFFICIAL_TRAITS.any { it.id == id }
    }
    
    fun validateTraitMap(traits: Map<String, Int>): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        traits.forEach { (traitId, value) ->
            val trait = getTraitById(traitId)
            if (trait == null) {
                errors[traitId] = "Unknown trait ID"
            } else {
                if (value < trait.minValue || value > trait.maxValue) {
                    errors[traitId] = "Value must be between ${trait.minValue} and ${trait.maxValue}"
                }
            }
        }
        
        return errors
    }
}