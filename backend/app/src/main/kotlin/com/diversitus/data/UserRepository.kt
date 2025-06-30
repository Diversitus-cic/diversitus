package com.diversitus.data

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import com.diversitus.model.User

class UserRepository(private val dbClient: DynamoDbClient, private val tableName: String) {

    suspend fun saveUser(user: User) {
        val item = mapOf(
            "id" to AttributeValue.S(user.id),
            "name" to AttributeValue.S(user.name),
            "profile" to AttributeValue.M(
                user.profile.traits.mapValues {
                    AttributeValue.N(it.value.toString())
                }
            )
        )

        val request = PutItemRequest {
            tableName = this@UserRepository.tableName
            this.item = item
        }

        dbClient.putItem(request)
    }
}