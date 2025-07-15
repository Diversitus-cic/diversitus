package com.diversitus

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import com.diversitus.data.JobRepository
import com.diversitus.data.UserRepository
import com.diversitus.data.CompanyRepository
import com.diversitus.plugins.configureHTTP
import com.diversitus.plugins.configureOpenAPI
import com.diversitus.plugins.configureRouting
import com.diversitus.plugins.configureSerialization
import com.diversitus.service.MatchingService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
//import io.github.smiley4.ktoropenapicore.OpenApiRoute
//import io.github.smiley4.ktoropenapi.OpenAPI 

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() = runBlocking {
    val dbClient = DynamoDbClient.fromEnvironment()
    val jobsTableName = System.getenv("JOBS_TABLE_NAME") ?: throw IllegalStateException("JOBS_TABLE_NAME not set.")
    val companiesTableName = System.getenv("COMPANIES_TABLE_NAME") ?: throw IllegalStateException("COMPANIES_TABLE_NAME not set.")
    val usersTableName = System.getenv("USERS_TABLE_NAME") ?: throw IllegalStateException("USERS_TABLE_NAME not set.")

    val jobRepository = JobRepository(dbClient, jobsTableName)
    val companyRepository = CompanyRepository(dbClient, companiesTableName)
    val userRepository = UserRepository(dbClient, usersTableName)

    val matchingService = MatchingService(jobRepository, companyRepository)

    // Install OpenAPI plugin here
    /* install(OpenAPI) {
        info {
            title = "Diversitus API"
            version = "1.0.0"
            description = "Job matching and company management API"
        }
        server("http://localhost:8080") {
            description = "Development server"
        }
    } */

    configureHTTP()
    configureSerialization()
    configureOpenAPI() // ✅ Install OpenAPI plugin
    configureRouting(jobRepository, companyRepository, userRepository, matchingService)
}
