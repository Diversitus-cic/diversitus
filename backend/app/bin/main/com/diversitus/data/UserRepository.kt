package com.diversitus.data

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import com.diversitus.model.NeurodiversityProfile
import com.diversitus.model.User

class UserRepository(private val dbClient: DynamoDbClient, private val tableName: String) {

    suspend fun getUserById(id: String): User? {
        val request = GetItemRequest {
            tableName = this@UserRepository.tableName
            key = mapOf("id" to AttributeValue.S(id))
        }

        val response = dbClient.getItem(request)
        return response.item?.toUser()
    }

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

    private fun Map<String, AttributeValue>.toUser(): User {
        val profileAttributes = this["profile"]?.asM() ?: emptyMap()
        val traits = profileAttributes.mapValues { it.value.asN()?.toInt() ?: 0 }

        return User(
            id = this["id"]?.asS() ?: throw IllegalStateException("User item is missing an 'id'."),
            name = this["name"]?.asS() ?: throw IllegalStateException("User item is missing a 'name'."),
            profile = NeurodiversityProfile(traits = traits)
        )
    }
}