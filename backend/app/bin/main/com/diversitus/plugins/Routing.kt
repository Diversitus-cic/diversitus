package com.diversitus.plugins

import com.diversitus.model.HealthStatus
import com.diversitus.data.JobRepository
import com.diversitus.model.*
import com.diversitus.service.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(jobRepository: JobRepository, matchingService: MatchingService) {
    routing {
        get("/") {
            call.respondText("Hello from Diversitus Matching Service!")
        }

        /**
         * A standard health check endpoint for monitoring services.
         */
        get("/health") {
            call.respond(HealthStatus(status = "UP"))
        }

        /**
         * Retrieves a list of all available jobs.
         */
        get("/jobs") {
            val jobs = jobRepository.getAllJobs()
            call.respond(jobs)
        }

        /**
         * Finds and returns jobs that match a given neurodiversity profile.
         * The request body should be a NeurodiversityProfile object, e.g.:
         * { "traits": { "attention_to_detail": 9, "problem_solving": 7 } }
         */
        post("/match") {
            val profile = call.receive<NeurodiversityProfile>()
            // Delegate the matching logic to the MatchingService
            val matchedJobs = matchingService.findMatchingJobs(profile)
            call.respond(matchedJobs)
        }
    }
}