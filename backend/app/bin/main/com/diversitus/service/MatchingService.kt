package com.diversitus.service

import com.diversitus.data.CompanyRepository
import com.diversitus.data.JobRepository
import com.diversitus.model.Job
import com.diversitus.model.MatchResult
import com.diversitus.model.NeurodiversityProfile
import kotlin.math.pow
import kotlin.math.sqrt

class MatchingService(
    private val jobRepository: JobRepository,
    private val companyRepository: CompanyRepository
) {

    /**
     * Finds jobs that are a good match for a given neurodiversity profile by calculating a
     * similarity score. The score is based on the Euclidean distance between the user's trait
     * scores and the combined traits of a job and its parent company.
     *
     * @param profile The neurodiversity profile of the user.
     * @return A list of [MatchResult] objects, sorted from best match (highest score) to worst.
     */
    suspend fun findMatchingJobs(profile: NeurodiversityProfile): List<MatchResult> {
        val allJobs = jobRepository.getAllJobs()
        val requiredCompanyIds = allJobs.map { it.companyId }.toSet()
        val companiesById = companyRepository.getCompaniesByIds(requiredCompanyIds).associateBy { it.id }

        return allJobs.mapNotNull { job ->
            val company = companiesById[job.companyId] ?: return@mapNotNull null

            // Combine company and job traits. Job-specific traits override company traits.
            val effectiveJobTraits = company.traits + job.traits

            // Find the set of traits common to both the user and the job offering.
            val commonTraits = profile.traits.keys.intersect(effectiveJobTraits.keys)
            if (commonTraits.isEmpty()) return@mapNotNull null

            // Calculate the sum of squared differences for common traits.
            val sumOfSquares = commonTraits.sumOf { trait ->
                (profile.traits[trait]!! - effectiveJobTraits[trait]!!).toDouble().pow(2)
            }

            val score = 1.0 / (1.0 + sqrt(sumOfSquares)) // Similarity score between 0 and 1
            MatchResult(job, company, score)
        }.filter { it.score > 0.15 } // Only include jobs with a score above the threshold.
         .sortedByDescending { it.score }
    }
}