package com.diversitus.plugins

import com.diversitus.data.CompanyRepository
import com.diversitus.data.JobRepository
import com.diversitus.data.UserRepository
import com.diversitus.model.*
import com.diversitus.service.*
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    jobRepository: JobRepository,
    companyRepository: CompanyRepository,
    userRepository: UserRepository,
    matchingService: MatchingService
) {
    routing {
        get("/") {
            call.respondText("Welcome to the Diversitus API!")
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
        route("/jobs") {
            get {
                call.respond(jobRepository.getAllJobs())
            }
            post {
                val job = call.receive<Job>()
                jobRepository.saveJob(job)
                call.respond(HttpStatusCode.Created, job)
            }
        }

        route("/companies") {
            get { call.respond(companyRepository.getAllCompanies()) }
            post {
                val company = call.receive<Company>()
                companyRepository.saveCompany(company)
                call.respond(HttpStatusCode.Created, company)
            }
        }

        route("/users") {
            post {
                val user = call.receive<User>()
                userRepository.saveUser(user)
                call.respond(user)
            }

            get("/{id}") {
                val identifier = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID or Email required")

                val user = if ("@" in identifier) {
                    userRepository.getUserByEmail(identifier)
                } else {
                    userRepository.getUserById(identifier)
                }

                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }
        }

        /**
         * Finds and returns jobs that match a given neurodiversity profile.
         * The request body should be a NeurodiversityProfile object, e.g.:
         * { "traits": { "attention_to_detail": 9, "problem_solving": 7 } }
         */
        post("/match") {
            val profile = call.receive<NeurodiversityProfile>()
            val matches = matchingService.findMatchingJobs(profile)
            call.respond(matches)
        }
    }
}