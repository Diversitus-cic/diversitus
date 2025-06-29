package com.diversitus

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import com.diversitus.data.JobRepository
import com.diversitus.data.CompanyRepository
import com.diversitus.plugins.configureHTTP
import com.diversitus.plugins.configureRouting
import com.diversitus.plugins.configureSerialization
import com.diversitus.service.MatchingService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() = runBlocking {
    // 1. Create the AWS SDK client for DynamoDB.
    val dbClient = DynamoDbClient.fromEnvironment() 
    val jobsTableName = System.getenv("JOBS_TABLE_NAME")
        ?: throw IllegalStateException("JOBS_TABLE_NAME not set.")
    val companiesTableName = System.getenv("COMPANIES_TABLE_NAME")
        ?: throw IllegalStateException("COMPANIES_TABLE_NAME not set.")

    // 2. Instantiate repositories.
    val jobRepository = JobRepository(dbClient, jobsTableName)
    val companyRepository = CompanyRepository(dbClient, companiesTableName)

    // 3. Instantiate services.
    val matchingService = MatchingService(jobRepository, companyRepository)

    // 4. Configure the application plugins.
    configureHTTP()
    configureSerialization()
    configureRouting(jobRepository, matchingService)
}