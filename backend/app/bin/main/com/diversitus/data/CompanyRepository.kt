package com.diversitus.data

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.BatchGetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import com.diversitus.model.Company

class CompanyRepository(private val dbClient: DynamoDbClient, private val tableName: String) {

    suspend fun getAllCompanies(): List<Company> {
        val request = ScanRequest {
            tableName = this@CompanyRepository.tableName
        }

        val response = dbClient.scan(request)
        return response.items?.map { it.toCompany() } ?: emptyList()
    }

    suspend fun getCompaniesByIds(companyIds: Set<String>): List<Company> {
        if (companyIds.isEmpty()) {
            return emptyList()
        }

        val keysToGet = companyIds.map { mapOf("id" to AttributeValue.S(it)) }

        val request = BatchGetItemRequest {
            requestItems = mapOf(tableName to aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes { keys = keysToGet })
        }

        val response = dbClient.batchGetItem(request)
        return response.responses?.get(tableName)?.map { it.toCompany() } ?: emptyList()
    }

    suspend fun saveCompany(company: Company) {
        val item = mapOf(
            "id" to AttributeValue.S(company.id),
            "name" to AttributeValue.S(company.name),
            "traits" to AttributeValue.M(
                company.traits.mapValues {
                    AttributeValue.N(it.value.toString())
                }
            )
        )
        val request = PutItemRequest {
            tableName = this@CompanyRepository.tableName
            this.item = item
        }
        dbClient.putItem(request)
    }

    private fun Map<String, AttributeValue>.toCompany(): Company {
        return Company(
            id = this["id"]?.asS()
                ?: throw IllegalStateException("Company item in DynamoDB is missing an 'id' attribute."),
            name = this["name"]?.asS() ?: "",
            traits = this["traits"]?.asM()?.mapValues {
                it.value.asN()?.toInt() ?: 0
            } ?: emptyMap()
        )
    }
}