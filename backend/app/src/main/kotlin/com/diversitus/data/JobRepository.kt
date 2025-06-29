package com.diversitus.data

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.*
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import com.diversitus.model.*

class JobRepository(private val dbClient: DynamoDbClient) {
    private val tableName = System.getenv("JOBS_TABLE_NAME")
        ?: throw IllegalStateException("Environment variable JOBS_TABLE_NAME not set")

    /**
     * Scans and returns all jobs from the DynamoDB table.
     * Note: For production with many items, a simple scan can be inefficient and costly.
     * Consider using more targeted Query operations based on access patterns.
     */
    suspend fun getAllJobs(): List<Job> {
        val request = ScanRequest {
            tableName = this@JobRepository.tableName
        }

        val response = dbClient.scan(request)
        return response.items?.mapNotNull { it.toJob() } ?: emptyList()
    }

    /**
     * Maps a DynamoDB item (Map<String, AttributeValue>) to our Job data class.
     */
    private fun Map<String, AttributeValue>.toJob(): Job? {
        return try {
            Job(
                id = this["id"]?.asS() ?: return null,
                title = this["title"]?.asS() ?: "",
                company = this["company"]?.asS() ?: "",
                requirements = this["requirements"]?.asM()?.mapValues { it.value.asN().toInt() } ?: emptyMap(),
                benefits = this["benefits"]?.asL()?.mapNotNull { it.asS() } ?: emptyList()
            )
        } catch (e: Exception) {
            // A proper logger would be better here, but this is fine for now.
            println("Error mapping DynamoDB item to Job: $e. Item: $this")
            null
        }
    }
}