package com.diversitus.service

import com.diversitus.data.CompanyRepository
import com.diversitus.data.JobRepository
import com.diversitus.model.Job
import com.diversitus.model.MatchResult
import com.diversitus.model.NeurodiversityProfile
import com.diversitus.model.Company
import kotlin.math.pow
import kotlin.math.sqrt

class MatchingService(
    private val jobRepository: JobRepository,
    private val companyRepository: CompanyRepository
) {

    /**
     * Original method - finds jobs that match a neurodiversity profile
     */
    suspend fun findMatchingJobs(profile: NeurodiversityProfile): List<MatchResult> {
        val allJobs = jobRepository.getAllJobs()
        val requiredCompanyIds = allJobs.map { it.companyId }.toSet()
        val companiesById = companyRepository.getCompaniesByIds(requiredCompanyIds).associateBy { it.id }

        val allMatches = allJobs.mapNotNull { job ->
            val company = companiesById[job.companyId] ?: return@mapNotNull null
            val effectiveJobTraits = company.traits + job.traits
            val commonTraits = profile.traits.keys.intersect(effectiveJobTraits.keys)
            if (commonTraits.isEmpty()) return@mapNotNull null

            val sumOfSquares = commonTraits.sumOf { trait ->
                (profile.traits[trait]!! - effectiveJobTraits[trait]!!).toDouble().pow(2)
            }
            val score = 1.0 / (1.0 + sqrt(sumOfSquares))
            MatchResult(job, company, score)
        }
        
        return allMatches.filter { it.score > 0.15 }.sortedByDescending { it.score }
    }

    /**
     * Debug version - returns simple string with debug information
     */
    suspend fun findMatchingJobsWithDebug(profile: NeurodiversityProfile): String {
        return try {
            val allJobs = jobRepository.getAllJobs()
            val requiredCompanyIds = allJobs.map { it.companyId }.toSet()
            val companiesById = companyRepository.getCompaniesByIds(requiredCompanyIds).associateBy { it.id }
            
            // Debug company loading
            val firstFewJobCompanyIds = allJobs.take(5).map { "\"${it.title}\": \"${it.companyId}\"" }.joinToString(", ")
            val loadedCompanyIds = companiesById.keys.joinToString(", ") { "\"$it\"" }
            val missingCompanyIds = requiredCompanyIds - companiesById.keys
            
            val allMatches = allJobs.mapNotNull { job ->
                val company = companiesById[job.companyId] ?: return@mapNotNull null
                val effectiveJobTraits = company.traits + job.traits
                val commonTraits = profile.traits.keys.intersect(effectiveJobTraits.keys)
                if (commonTraits.isEmpty()) return@mapNotNull null

                val sumOfSquares = commonTraits.sumOf { trait ->
                    (profile.traits[trait]!! - effectiveJobTraits[trait]!!).toDouble().pow(2)
                }
                val score = 1.0 / (1.0 + sqrt(sumOfSquares))
                MatchResult(job, company, score)
            }
            
            val filteredMatches = allMatches.filter { it.score > 0.15 }.sortedByDescending { it.score }
            
            // Show top 5 scores for analysis
            val topScores = allMatches.sortedByDescending { it.score }.take(5)
            val scoresText = topScores.map { "\"${it.job.title}\": ${String.format("%.4f", it.score)}" }.joinToString(", ")
            
            """
            {
                "totalJobsAnalyzed": ${allJobs.size},
                "requiredCompanyIds": ${requiredCompanyIds.size},
                "totalCompanies": ${companiesById.size},
                "totalMatches": ${allMatches.size},
                "filteredMatches": ${filteredMatches.size},
                "threshold": 0.15,
                "userTraitCount": ${profile.traits.size},
                "topScores": {${scoresText}},
                "maxScore": ${allMatches.maxOfOrNull { it.score } ?: 0.0},
                "sampleJobCompanyIds": {${firstFewJobCompanyIds}},
                "loadedCompanyIds": [${loadedCompanyIds}],
                "missingCompanyCount": ${missingCompanyIds.size},
                "message": "Debug analysis complete"
            }
            """.trimIndent()
        } catch (e: Exception) {
            println("ERROR in findMatchingJobsWithDebug: ${e.message}")
            """
            {
                "error": "${e.message?.replace("\"", "\\\"") ?: "Unknown error"}",
                "totalJobsAnalyzed": 0,
                "message": "Error occurred during analysis"
            }
            """.trimIndent()
        }
    }
}