package com.diversitus.plugins

import com.diversitus.data.CompanyRepository
import com.diversitus.data.JobRepository
import com.diversitus.data.UserRepository
import com.diversitus.data.MessageRepository
import com.diversitus.model.*
import com.diversitus.service.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.util.UUID

fun generateThreadId(fromId: String, toId: String, jobId: String?): String {
    val jobSuffix = if (jobId != null) "_job_${jobId}" else "_general"
    return "thread_${fromId}_${toId}${jobSuffix}_${UUID.randomUUID().toString().take(8)}"
}

@Serializable
data class HealthStatus(val status: String)

@Serializable
data class CompanyLoginRequest(val email: String)

@Serializable
data class UserLoginRequest(val email: String)

@Serializable
data class CompanyLoginResponse(val success: Boolean, val company: Company? = null, val message: String? = null)

@Serializable
data class UserLoginResponse(val success: Boolean, val user: User? = null, val message: String? = null)

fun Application.configureRouting(
    jobRepository: JobRepository,
    companyRepository: CompanyRepository,
    userRepository: UserRepository,
    messageRepository: MessageRepository,
    matchingService: MatchingService
) {
    val traitLibraryService = TraitLibraryService()
    routing {
        get("/") {
            call.respondText("Welcome to the Diversitus API!")
        }

        get("/health") {
            call.respond(HealthStatus(status = "UP"))
        }
        
        head("/health") {
            call.respond(HttpStatusCode.OK)
        }

        get("/jobs") {
            val companyId = call.request.queryParameters["companyId"]
            val jobs = if (companyId != null) {
                jobRepository.getJobsByCompanyId(companyId)
            } else {
                jobRepository.getAllJobs()
            }
            call.respond(jobs)
        }

        post("/jobs") {
            val job = call.receive<Job>()
            jobRepository.saveJob(job)
            call.respond(HttpStatusCode.Created, job)
        }

        get("/companies") {
            call.respond(companyRepository.getAllCompanies())
        }

        post("/companies") {
            val company = call.receive<Company>()
            companyRepository.saveCompany(company)
            call.respond(HttpStatusCode.Created, company)
        }

        get("/companies/{id}") {
            val identifier = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Company ID or Email required")

            val company = if ("@" in identifier) {
                companyRepository.getCompanyByEmail(identifier)
            } else {
                companyRepository.getCompaniesByIds(setOf(identifier)).firstOrNull()
            }

            if (company != null) {
                call.respond(company)
            } else {
                call.respond(HttpStatusCode.NotFound, "Company not found")
            }
        }

        post("/users") {
            val user = call.receive<User>()
            userRepository.saveUser(user)
            call.respond(user)
        }

        get("/users/{id}") {
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

        post("/match") {
            val profile = call.receive<NeurodiversityProfile>()
            val matches = matchingService.findMatchingJobs(profile)
            call.respond(matches)
        }

        post("/match/debug") {
            try {
                val profile = call.receive<NeurodiversityProfile>()
                call.respondText("""
                    {
                        "status": "received_profile",
                        "message": "Debug endpoint is working",
                        "userTraitCount": ${profile.traits.size},
                        "firstTrait": "${profile.traits.keys.firstOrNull() ?: "none"}"
                    }
                """.trimIndent(), ContentType.Application.Json)
            } catch (e: Exception) {
                call.respondText("""
                    {
                        "error": "${e.message?.replace("\"", "\\\"") ?: "Unknown error"}",
                        "message": "Error in debug endpoint"
                    }
                """.trimIndent(), ContentType.Application.Json)
            }
        }

        post("/match/debug2") {
            try {
                val profile = call.receive<NeurodiversityProfile>()
                val response = matchingService.findMatchingJobsWithDebug(profile)
                call.respondText(response, ContentType.Application.Json)
            } catch (e: Exception) {
                call.respondText("""
                    {
                        "error": "${e.message?.replace("\"", "\\\"") ?: "Unknown error"}",
                        "message": "Error in matching service"
                    }
                """.trimIndent(), ContentType.Application.Json)
            }
        }

        post("/auth/company/login") {
            val loginRequest = call.receive<CompanyLoginRequest>()
            val company = companyRepository.getCompanyByEmail(loginRequest.email)
            
            if (company != null) {
                call.respond(CompanyLoginResponse(success = true, company = company))
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    CompanyLoginResponse(success = false, message = "Company not found with email: ${loginRequest.email}")
                )
            }
        }

        post("/auth/user/login") {
            val loginRequest = call.receive<UserLoginRequest>()
            val user = userRepository.getUserByEmail(loginRequest.email)
            
            if (user != null) {
                call.respond(UserLoginResponse(success = true, user = user))
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    UserLoginResponse(success = false, message = "User not found with email: ${loginRequest.email}")
                )
            }
        }

        post("/messages") {
            val message = call.receive<Message>()
            
            // Automatic thread management
            val messageWithThread = if (message.threadId == null) {
                // Look for existing thread between these participants for this job
                val existingThreadId = messageRepository.findExistingThread(
                    message.fromId, 
                    message.toId, 
                    message.jobId
                )
                
                if (existingThreadId != null) {
                    // Use existing thread
                    message.copy(threadId = existingThreadId)
                } else {
                    // Create new thread
                    val newThreadId = generateThreadId(message.fromId, message.toId, message.jobId)
                    message.copy(threadId = newThreadId)
                }
            } else {
                // Thread ID explicitly provided
                message
            }
            
            messageRepository.saveMessage(messageWithThread)
            call.respond(HttpStatusCode.Created, messageWithThread)
        }

        get("/messages/company/{companyId}") {
            val companyId = call.parameters["companyId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Company ID required")
            val messages = messageRepository.getMessagesForCompany(companyId)
            call.respond(messages)
        }

        get("/messages/user/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")
            val messages = messageRepository.getMessagesForUser(userId)
            call.respond(messages)
        }

        get("/messages/thread/{threadId}") {
            val threadId = call.parameters["threadId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Thread ID required")
            val messages = messageRepository.getMessagesByThread(threadId)
            call.respond(messages)
        }

        patch("/messages/{id}/status") {
            val messageId = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest, "Message ID required")
            
            try {
                val statusUpdate = call.receive<MessageStatusUpdate>()
                
                // Verify message exists before updating
                val existingMessage = messageRepository.getMessageById(messageId)
                if (existingMessage == null) {
                    return@patch call.respond(HttpStatusCode.NotFound, "Message not found")
                }
                
                messageRepository.updateMessageStatus(messageId, statusUpdate.status)
                call.respond(HttpStatusCode.OK, mapOf("status" to "updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to update message status: ${e.message}"))
            }
        }

        // Trait Library endpoints
        get("/traits") {
            val library = traitLibraryService.getTraitLibrary()
            call.respond(library)
        }

        get("/traits/{id}") {
            val traitId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Trait ID required")
            val trait = traitLibraryService.getTraitById(traitId)
            
            if (trait != null) {
                call.respond(trait)
            } else {
                call.respond(HttpStatusCode.NotFound, "Trait not found")
            }
        }

        get("/traits/category/{category}") {
            val categoryName = call.parameters["category"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Category required")
            
            try {
                val category = TraitCategory.valueOf(categoryName.uppercase())
                val traits = traitLibraryService.getTraitsByCategory(category)
                call.respond(traits)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid category. Valid categories: ${TraitCategory.values().joinToString()}")
            }
        }

        post("/traits/validate") {
            val traitsMap = call.receive<Map<String, Int>>()
            val errors = traitLibraryService.validateTraitMap(traitsMap)
            
            if (errors.isEmpty()) {
                call.respond(mapOf("valid" to true))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("valid" to false, "errors" to errors))
            }
        }
    }
}