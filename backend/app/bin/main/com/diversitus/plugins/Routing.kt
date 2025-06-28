package com.diversitus.plugins

import com.diversitus.data.JobRepository
import com.diversitus.model.NeurodiversityProfile
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(jobRepository: JobRepository) {
    routing {
        get("/") {
            call.respondText("Hello from Diversitus Matching Service!")
        }

        post("/match") {
            val profile = call.receive<NeurodiversityProfile>()

            // Fetch all jobs from the database via the repository
            val allJobs = jobRepository.getAllJobs()

            // The matching logic remains the same, but now operates on live data
            val matchedJobs = allJobs.filter { job ->
                job.benefits.any { it in profile.preferences }
            }

            call.respond(matchedJobs)
        }
    }
}