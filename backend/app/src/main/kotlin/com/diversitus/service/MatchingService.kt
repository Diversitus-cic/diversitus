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
     * Debug version - finds jobs with comprehensive debug information
     */
    suspend fun findMatchingJobsWithDebug(profile: NeurodiversityProfile): Map<String, Any> {
        return try {
            val allJobs = jobRepository.getAllJobs()
            val requiredCompanyIds = allJobs.map { it.companyId }.toSet()
            val companiesById = companyRepository.getCompaniesByIds(requiredCompanyIds).associateBy { it.id }
            
            val debugInfo = mutableListOf<String>()
            debugInfo.add("Found ${allJobs.size} jobs in database")
            debugInfo.add("Found ${companiesById.size} companies")
            
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
            
            mapOf(
                "matches" to filteredMatches,
                "debug" to mapOf(
                    "summary" to debugInfo,
                    "userProfile" to profile.traits,
                    "totalJobsAnalyzed" to allJobs.size,
                    "totalMatches" to allMatches.size,
                    "filteredMatches" to filteredMatches.size,
                    "threshold" to 0.15
                )
            )
        } catch (e: Exception) {
            println("ERROR in findMatchingJobsWithDebug: ${e.message}")
            mapOf(
                "matches" to emptyList<MatchResult>(),
                "debug" to mapOf(
                    "error" to e.message,
                    "userProfile" to profile.traits
                )
            )
        }
    }
}