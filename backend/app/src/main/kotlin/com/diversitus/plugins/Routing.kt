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
            messageRepository.saveMessage(message)
            call.respond(HttpStatusCode.Created, message)
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
            val statusUpdate = call.receive<MessageStatusUpdate>()
            
            // Verify message exists before updating
            val existingMessage = messageRepository.getMessageById(messageId)
            if (existingMessage == null) {
                return@patch call.respond(HttpStatusCode.NotFound, "Message not found")
            }
            
            messageRepository.updateMessageStatus(messageId, statusUpdate.status)
            call.respond(HttpStatusCode.OK, mapOf("status" to "updated"))
        }
    }
}