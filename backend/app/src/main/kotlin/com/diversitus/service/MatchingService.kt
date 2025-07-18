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
            
            """
            {
                "totalJobsAnalyzed": ${allJobs.size},
                "totalCompanies": ${companiesById.size},
                "totalMatches": ${allMatches.size},
                "filteredMatches": ${filteredMatches.size},
                "threshold": 0.15,
                "userTraitCount": ${profile.traits.size},
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