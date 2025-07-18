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
     * Legacy method - finds jobs that match a neurodiversity profile
     */
    suspend fun findMatchingJobs(profile: NeurodiversityProfile): List<MatchResult> {
        val allJobs = jobRepository.getAllJobs()
        println("DEBUG: Found ${allJobs.size} jobs in database")
        
        val requiredCompanyIds = allJobs.map { it.companyId }.toSet()
        val companiesById = companyRepository.getCompaniesByIds(requiredCompanyIds).associateBy { it.id }
        println("DEBUG: Found ${companiesById.size} companies for ${requiredCompanyIds.size} required company IDs")

        val allMatches = allJobs.mapNotNull { job ->
            val company = companiesById[job.companyId]
            if (company == null) {
                println("DEBUG: No company found for job ${job.title} with companyId ${job.companyId}")
                return@mapNotNull null
            }

            // Combine company and job traits. Job-specific traits override company traits.
            val effectiveJobTraits = company.traits + job.traits

            // Find the set of traits common to both the user and the job offering.
            val commonTraits = profile.traits.keys.intersect(effectiveJobTraits.keys)
            if (commonTraits.isEmpty()) {
                println("DEBUG: No common traits between user and job ${job.title}. User traits: ${profile.traits.keys}, Job traits: ${effectiveJobTraits.keys}")
                return@mapNotNull null
            }

            // Calculate the sum of squared differences for common traits.
            val sumOfSquares = commonTraits.sumOf { trait ->
                (profile.traits[trait]!! - effectiveJobTraits[trait]!!).toDouble().pow(2)
            }

            val score = 1.0 / (1.0 + sqrt(sumOfSquares)) // Similarity score between 0 and 1
            println("DEBUG: Job ${job.title} score: $score (common traits: ${commonTraits.size})")
            MatchResult(job, company, score)
        }
        
        println("DEBUG: Total potential matches before threshold filter: ${allMatches.size}")
        val filteredMatches = allMatches.filter { it.score > 0.15 }
        println("DEBUG: Matches above 0.15 threshold: ${filteredMatches.size}")
        
        return filteredMatches.sortedByDescending { it.score }
    }
}