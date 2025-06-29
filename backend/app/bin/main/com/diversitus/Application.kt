package com.diversitus

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import com.diversitus.data.JobRepository
import com.diversitus.plugins.configureRouting
import com.diversitus.plugins.configureSerialization
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
    val tableName = System.getenv("JOBS_TABLE_NAME")
        ?: throw IllegalStateException("Required environment variable JOBS_TABLE_NAME is not set.")
    
    // 2. Instantiate the repository, passing the client to it.
    val jobRepository = JobRepository(dbClient, tableName)
    
    // 3. Configure the application plugins.
    configureSerialization()
    // Note: The `configureRouting` function only needs the repository,
    // as the repository now contains the dbClient.
    configureRouting(jobRepository) 
}