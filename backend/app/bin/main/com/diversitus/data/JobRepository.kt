package com.diversitus.data

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import com.diversitus.model.Job

class JobRepository(private val dbClient: DynamoDbClient, private val tableName: String) {

    suspend fun getAllJobs(): List<Job> {
        val request = ScanRequest {
            tableName = this@JobRepository.tableName
        }

        val response = dbClient.scan(request)
        return response.items?.map { it.toJob() } ?: emptyList()
    }

    suspend fun saveJob(job: Job) {
        val item = mapOf(
            "id" to AttributeValue.S(job.id),
            "companyId" to AttributeValue.S(job.companyId),
            "title" to AttributeValue.S(job.title),
            "description" to AttributeValue.S(job.description),
            "traits" to AttributeValue.M(
                job.traits.mapValues {
                    AttributeValue.N(it.value.toString())
                }
            )
        )
        val request = PutItemRequest {
            tableName = this@JobRepository.tableName
            this.item = item
        }
        dbClient.putItem(request)
    }

    private fun Map<String, AttributeValue>.toJob(): Job {
        return Job(
            id = this["id"]?.asS()
                ?: throw IllegalStateException("Job item in DynamoDB is missing an 'id' attribute."),
            companyId = this["companyId"]?.asS() ?: "",
            title = this["title"]?.asS() ?: "",
            description = this["description"]?.asS() ?: "",
            traits = this["traits"]?.asM()?.mapValues {
                it.value.asN()?.toInt() ?: 0
            } ?: emptyMap()
        )
    }
}