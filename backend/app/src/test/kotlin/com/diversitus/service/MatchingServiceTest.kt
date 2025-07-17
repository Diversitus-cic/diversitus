package com.diversitus.service

import com.diversitus.data.CompanyRepository
import com.diversitus.data.JobRepository
import com.diversitus.model.Company
import com.diversitus.model.Job
import com.diversitus.model.NeurodiversityProfile
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class MatchingServiceTest {

    private val jobRepository = mockk<JobRepository>()
    private val companyRepository = mockk<CompanyRepository>()
    private val matchingService = MatchingService(jobRepository, companyRepository)

    @Test
    fun `should calculate perfect match score of 1_0 for identical traits`() = runTest {
        val company = Company(
            id = "company1",
            name = "Perfect Match Co",
            email = "contact@perfect.com",
            traits = mapOf("focus" to 8, "collaboration" to 6)
        )
        
        val job = Job(
            id = "job1",
            companyId = "company1",
            title = "Software Engineer",
            description = "Perfect role",
            traits = emptyMap() // No job-specific traits, inherits from company
        )
        
        val userProfile = NeurodiversityProfile(
            traits = mapOf("focus" to 8, "collaboration" to 6)
        )

        coEvery { jobRepository.getAllJobs() } returns listOf(job)
        coEvery { companyRepository.getCompaniesByIds(setOf("company1")) } returns listOf(company)

        val results = matchingService.findMatchingJobs(userProfile)

        assertEquals(1, results.size)
        assertEquals(1.0, results[0].score, 0.001)
        assertEquals(job, results[0].job)
        assertEquals(company, results[0].company)
    }

    @Test
    fun `should calculate correct euclidean distance score for different traits`() = runTest {
        val company = Company(
            id = "company1",
            name = "Test Co",
            email = "test@company.com",
            traits = mapOf("focus" to 5, "creativity" to 8)
        )
        
        val job = Job(
            id = "job1",
            companyId = "company1",
            title = "Designer",
            description = "Creative role",
            traits = emptyMap()
        )
        
        val userProfile = NeurodiversityProfile(
            traits = mapOf("focus" to 7, "creativity" to 6) // Differences: 2, 2
        )

        coEvery { jobRepository.getAllJobs() } returns listOf(job)
        coEvery { companyRepository.getCompaniesByIds(setOf("company1")) } returns listOf(company)

        val results = matchingService.findMatchingJobs(userProfile)

        // Expected: sqrt(2^2 + 2^2) = sqrt(8) = 2.828...
        // Score: 1 / (1 + 2.828...) = 1 / 3.828... ≈ 0.261
        assertEquals(1, results.size)
        assertTrue(abs(results[0].score - 0.261) < 0.01, "Expected score ~0.261, got ${results[0].score}")
    }

    @Test
    fun `should prioritize job traits over company traits when conflicts exist`() = runTest {
        val company = Company(
            id = "company1",
            name = "Hybrid Co",
            email = "hybrid@company.com",
            traits = mapOf("remote_work" to 3, "collaboration" to 8)
        )
        
        val job = Job(
            id = "job1",
            companyId = "company1",
            title = "Remote Developer",
            description = "Fully remote role",
            traits = mapOf("remote_work" to 10) // Overrides company's remote_work: 3
        )
        
        val userProfile = NeurodiversityProfile(
            traits = mapOf("remote_work" to 10, "collaboration" to 8)
        )

        coEvery { jobRepository.getAllJobs() } returns listOf(job)
        coEvery { companyRepository.getCompaniesByIds(setOf("company1")) } returns listOf(company)

        val results = matchingService.findMatchingJobs(userProfile)

        // Effective traits: remote_work=10 (from job), collaboration=8 (from company)
        // Perfect match should give score = 1.0
        assertEquals(1, results.size)
        assertEquals(1.0, results[0].score, 0.001)
    }

    @Test
    fun `should filter out jobs with no common traits`() = runTest {
        val company = Company(
            id = "company1",
            name = "Different Co",
            email = "different@company.com",
            traits = mapOf("leadership" to 9, "public_speaking" to 7)
        )
        
        val job = Job(
            id = "job1",
            companyId = "company1",
            title = "Manager",
            description = "Leadership role",
            traits = emptyMap()
        )
        
        val userProfile = NeurodiversityProfile(
            traits = mapOf("focus" to 8, "detail_oriented" to 9) // No overlap
        )

        coEvery { jobRepository.getAllJobs() } returns listOf(job)
        coEvery { companyRepository.getCompaniesByIds(setOf("company1")) } returns listOf(company)

        val results = matchingService.findMatchingJobs(userProfile)

        assertEquals(0, results.size, "Should filter out jobs with no common traits")
    }

    @Test
    fun `should filter out jobs with score below 0_15 threshold`() = runTest {
        val company = Company(
            id = "company1",
            name = "Poor Match Co",
            email = "poor@company.com",
            traits = mapOf("focus" to 1) // Very different from user
        )
        
        val job = Job(
            id = "job1",
            companyId = "company1",
            title = "Mismatched Role",
            description = "Poor fit",
            traits = emptyMap()
        )
        
        val userProfile = NeurodiversityProfile(
            traits = mapOf("focus" to 10) // Large difference: 9
        )

        coEvery { jobRepository.getAllJobs() } returns listOf(job)
        coEvery { companyRepository.getCompaniesByIds(setOf("company1")) } returns listOf(company)

        val results = matchingService.findMatchingJobs(userProfile)

        // Score: 1 / (1 + 9) = 0.1, which is below 0.15 threshold
        assertEquals(0, results.size, "Should filter out jobs with score < 0.15")
    }

    @Test
    fun `should sort results by score in descending order`() = runTest {
        val company1 = Company(
            id = "company1", name = "Good Match", email = "good@company.com",
            traits = mapOf("focus" to 8, "creativity" to 7)
        )
        val company2 = Company(
            id = "company2", name = "Better Match", email = "better@company.com", 
            traits = mapOf("focus" to 9, "creativity" to 8)
        )
        val company3 = Company(
            id = "company3", name = "Perfect Match", email = "perfect@company.com",
            traits = mapOf("focus" to 10, "creativity" to 9)
        )
        
        val job1 = Job(id = "job1", companyId = "company1", title = "Job 1", description = "Desc 1", traits = emptyMap())
        val job2 = Job(id = "job2", companyId = "company2", title = "Job 2", description = "Desc 2", traits = emptyMap())
        val job3 = Job(id = "job3", companyId = "company3", title = "Job 3", description = "Desc 3", traits = emptyMap())
        
        val userProfile = NeurodiversityProfile(
            traits = mapOf("focus" to 10, "creativity" to 9)
        )

        coEvery { jobRepository.getAllJobs() } returns listOf(job1, job2, job3)
        coEvery { companyRepository.getCompaniesByIds(setOf("company1", "company2", "company3")) } returns 
            listOf(company1, company2, company3)

        val results = matchingService.findMatchingJobs(userProfile)

        assertEquals(3, results.size)
        assertTrue(results[0].score >= results[1].score, "Results should be sorted by score descending")
        assertTrue(results[1].score >= results[2].score, "Results should be sorted by score descending")
        assertEquals("company3", results[0].company.id, "Perfect match should be first")
    }

    @Test
    fun `should handle empty job list gracefully`() = runTest {
        val userProfile = NeurodiversityProfile(
            traits = mapOf("focus" to 8, "creativity" to 7)
        )

        coEvery { jobRepository.getAllJobs() } returns emptyList()
        coEvery { companyRepository.getCompaniesByIds(emptySet()) } returns emptyList()

        val results = matchingService.findMatchingJobs(userProfile)

        assertEquals(0, results.size)
    }

    @Test
    fun `should handle empty user profile traits gracefully`() = runTest {
        val company = Company(
            id = "company1", name = "Test Co", email = "test@company.com",
            traits = mapOf("focus" to 8, "creativity" to 7)
        )
        val job = Job(
            id = "job1", companyId = "company1", title = "Test Job", 
            description = "Test", traits = emptyMap()
        )
        
        val userProfile = NeurodiversityProfile(traits = emptyMap())

        coEvery { jobRepository.getAllJobs() } returns listOf(job)
        coEvery { companyRepository.getCompaniesByIds(setOf("company1")) } returns listOf(company)

        val results = matchingService.findMatchingJobs(userProfile)

        assertEquals(0, results.size, "Should return no matches for empty user profile")
    }

    @Test
    fun `should handle missing company for job gracefully`() = runTest {
        val job = Job(
            id = "job1", companyId = "missing-company", title = "Orphaned Job",
            description = "Job with no company", traits = mapOf("focus" to 8)
        )
        
        val userProfile = NeurodiversityProfile(
            traits = mapOf("focus" to 8)
        )

        coEvery { jobRepository.getAllJobs() } returns listOf(job)
        coEvery { companyRepository.getCompaniesByIds(setOf("missing-company")) } returns emptyList()

        val results = matchingService.findMatchingJobs(userProfile)

        assertEquals(0, results.size, "Should filter out jobs with missing companies")
    }

    @Test
    fun `should handle job with only job-specific traits correctly`() = runTest {
        val company = Company(
            id = "company1", name = "Generic Co", email = "generic@company.com",
            traits = mapOf("culture" to 5) // Different trait from job
        )
        
        val job = Job(
            id = "job1", companyId = "company1", title = "Specialist Role",
            description = "Specialized position", 
            traits = mapOf("technical_skills" to 9, "problem_solving" to 8)
        )
        
        val userProfile = NeurodiversityProfile(
            traits = mapOf("technical_skills" to 9, "problem_solving" to 8)
        )

        coEvery { jobRepository.getAllJobs() } returns listOf(job)
        coEvery { companyRepository.getCompaniesByIds(setOf("company1")) } returns listOf(company)

        val results = matchingService.findMatchingJobs(userProfile)

        assertEquals(1, results.size)
        assertEquals(1.0, results[0].score, 0.001, "Should match perfectly on job-specific traits")
    }

    @Test
    fun `should calculate score correctly for single trait match`() = runTest {
        val company = Company(
            id = "company1", name = "Focus Co", email = "focus@company.com",
            traits = mapOf("focus" to 6)
        )
        
        val job = Job(
            id = "job1", companyId = "company1", title = "Focused Role",
            description = "Requires focus", traits = emptyMap()
        )
        
        val userProfile = NeurodiversityProfile(
            traits = mapOf("focus" to 9) // Difference of 3
        )

        coEvery { jobRepository.getAllJobs() } returns listOf(job)
        coEvery { companyRepository.getCompaniesByIds(setOf("company1")) } returns listOf(company)

        val results = matchingService.findMatchingJobs(userProfile)

        // Score: 1 / (1 + 3) = 0.25
        assertEquals(1, results.size)
        assertEquals(0.25, results[0].score, 0.001)
    }
}