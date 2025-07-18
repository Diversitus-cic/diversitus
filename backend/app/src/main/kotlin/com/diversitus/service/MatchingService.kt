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
        val debugResult = findMatchingJobsWithDebug(profile)
        @Suppress("UNCHECKED_CAST")
        return debugResult["matches"] as List<MatchResult>
    }

    /**
     * Debug version - finds jobs with comprehensive debug information
     */
    suspend fun findMatchingJobsWithDebug(profile: NeurodiversityProfile): Map<String, Any> {
        val allJobs = jobRepository.getAllJobs()
        println("DEBUG: Found ${allJobs.size} jobs in database")
        
        val requiredCompanyIds = allJobs.map { it.companyId }.toSet()
        val companiesById = companyRepository.getCompaniesByIds(requiredCompanyIds).associateBy { it.id }
        println("DEBUG: Found ${companiesById.size} companies for ${requiredCompanyIds.size} required company IDs")

        val debugInfo = mutableListOf<String>()
        val jobAnalysis = mutableListOf<Map<String, Any>>()
        
        debugInfo.add("Found ${allJobs.size} jobs in database")
        debugInfo.add("Found ${companiesById.size} companies for ${requiredCompanyIds.size} required company IDs")

        val allMatches = allJobs.mapNotNull { job ->
            val company = companiesById[job.companyId]
            if (company == null) {
                val message = "No company found for job ${job.title} with companyId ${job.companyId}"
                println("DEBUG: $message")
                debugInfo.add(message)
                return@mapNotNull null
            }

            // Combine company and job traits. Job-specific traits override company traits.
            val effectiveJobTraits = company.traits + job.traits

            // Find the set of traits common to both the user and the job offering.
            val commonTraits = profile.traits.keys.intersect(effectiveJobTraits.keys)
            
            val jobInfo = mutableMapOf<String, Any>(
                "jobTitle" to job.title,
                "companyName" to company.name,
                "userTraits" to profile.traits.keys.toList(),
                "jobTraits" to effectiveJobTraits.keys.toList(),
                "commonTraits" to commonTraits.toList()
            )
            
            if (commonTraits.isEmpty()) {
                val message = "No common traits between user and job ${job.title}"
                println("DEBUG: $message. User traits: ${profile.traits.keys}, Job traits: ${effectiveJobTraits.keys}")
                jobInfo["status"] = "NO_COMMON_TRAITS"
                jobInfo["score"] = 0.0
                jobAnalysis.add(jobInfo)
                return@mapNotNull null
            }

            // Calculate the sum of squared differences for common traits.
            val traitDifferences = commonTraits.map { trait ->
                val userVal = profile.traits[trait]!!
                val jobVal = effectiveJobTraits[trait]!!
                val diff = userVal - jobVal
                mapOf("trait" to trait, "userValue" to userVal, "jobValue" to jobVal, "difference" to diff)
            }
            
            val sumOfSquares = commonTraits.sumOf { trait ->
                (profile.traits[trait]!! - effectiveJobTraits[trait]!!).toDouble().pow(2)
            }

            val score = 1.0 / (1.0 + sqrt(sumOfSquares)) // Similarity score between 0 and 1
            println("DEBUG: Job ${job.title} score: $score (common traits: ${commonTraits.size})")
            
            jobInfo["status"] = "CALCULATED"
            jobInfo["score"] = score
            jobInfo["commonTraitCount"] = commonTraits.size
            jobInfo["traitDifferences"] = traitDifferences
            jobInfo["sumOfSquares"] = sumOfSquares
            jobAnalysis.add(jobInfo)
            
            MatchResult(job, company, score)
        }
        
        println("DEBUG: Total potential matches before threshold filter: ${allMatches.size}")
        val filteredMatches = allMatches.filter { it.score > 0.15 }
        println("DEBUG: Matches above 0.15 threshold: ${filteredMatches.size}")
        
        debugInfo.add("Total potential matches before threshold filter: ${allMatches.size}")
        debugInfo.add("Matches above 0.15 threshold: ${filteredMatches.size}")
        
        // Get top 5 jobs by score for analysis
        val topJobs = allMatches.sortedByDescending { it.score }.take(5)
        val topJobsInfo = topJobs.map { match ->
            mapOf(
                "jobTitle" to match.job.title,
                "companyName" to match.company.name,
                "score" to match.score,
                "aboveThreshold" to (match.score > 0.15)
            )
        }
        
        return mapOf(
            "matches" to filteredMatches.sortedByDescending { it.score },
            "debug" to mapOf(
                "summary" to debugInfo,
                "userProfile" to profile.traits,
                "topJobsByScore" to topJobsInfo,
                "allJobAnalysis" to jobAnalysis.take(10), // Limit to first 10 for readability
                "threshold" to 0.15,
                "totalJobsAnalyzed" to allJobs.size,
                "totalMatches" to allMatches.size,
                "filteredMatches" to filteredMatches.size
            )
        )
    }
}