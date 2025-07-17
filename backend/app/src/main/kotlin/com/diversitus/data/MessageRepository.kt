package com.diversitus.data

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.QueryRequest
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import aws.sdk.kotlin.services.dynamodb.model.UpdateItemRequest
import com.diversitus.model.Message
import com.diversitus.model.MessageStatus
import com.diversitus.model.NeurodiversityProfile

class MessageRepository(private val dbClient: DynamoDbClient, private val tableName: String) {

    suspend fun saveMessage(message: Message) {
        val item = mutableMapOf(
            "id" to AttributeValue.S(message.id),
            // New field names
            "fromId" to AttributeValue.S(message.fromId),
            "toId" to AttributeValue.S(message.toId),
            // Old field names for backward compatibility
            "fromUserId" to AttributeValue.S(message.fromId),
            "toCompanyId" to AttributeValue.S(message.toId),
            "content" to AttributeValue.S(message.content),
            "isAnonymous" to AttributeValue.Bool(message.isAnonymous),
            "isFromCompany" to AttributeValue.Bool(message.isFromCompany),
            "createdAt" to AttributeValue.S(message.createdAt),
            "status" to AttributeValue.S(message.status.name)
        )

        // Add optional fields if present
        message.jobId?.let { item["jobId"] = AttributeValue.S(it) }
        message.threadId?.let { item["threadId"] = AttributeValue.S(it) }
        message.senderName?.let { item["senderName"] = AttributeValue.S(it) }
        message.senderProfile?.let { profile ->
            item["senderProfile"] = AttributeValue.M(
                profile.traits.mapValues { AttributeValue.N(it.value.toString()) }
            )
        }

        val request = PutItemRequest {
            tableName = this@MessageRepository.tableName
            this.item = item
        }
        dbClient.putItem(request)
    }

    suspend fun getMessagesForCompany(companyId: String): List<Message> {
        val request = QueryRequest {
            tableName = this@MessageRepository.tableName
            indexName = "CompanyIndex" // Use existing old index
            keyConditionExpression = "toCompanyId = :companyId"
            expressionAttributeValues = mapOf(
                ":companyId" to AttributeValue.S(companyId)
            )
        }
        val response = dbClient.query(request)
        return response.items?.map { it.toMessage() } ?: emptyList()
    }

    suspend fun getMessagesForUser(userId: String): List<Message> {
        // Get messages sent by user (using old index)
        val sentRequest = QueryRequest {
            tableName = this@MessageRepository.tableName
            indexName = "UserIndex" // Use existing old index
            keyConditionExpression = "fromUserId = :userId"
            expressionAttributeValues = mapOf(
                ":userId" to AttributeValue.S(userId)
            )
        }
        val sentResponse = dbClient.query(sentRequest)
        val sentMessages = sentResponse.items?.map { it.toMessage() } ?: emptyList()

        // For received messages, we need to scan since there's no toUserId index
        // This is a limitation of the current schema - we'll add proper indexes later
        val receivedMessages = emptyList<Message>() // TODO: Add when new indexes are available

        // Combine and sort by creation time
        return (sentMessages + receivedMessages).sortedBy { it.createdAt }
    }

    suspend fun getMessagesByThread(threadId: String): List<Message> {
        val request = QueryRequest {
            tableName = this@MessageRepository.tableName
            indexName = "ThreadIndex"
            keyConditionExpression = "threadId = :threadId"
            expressionAttributeValues = mapOf(
                ":threadId" to AttributeValue.S(threadId)
            )
        }
        val response = dbClient.query(request)
        return response.items?.map { it.toMessage() } ?: emptyList()
    }

    suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        val request = UpdateItemRequest {
            tableName = this@MessageRepository.tableName
            key = mapOf("id" to AttributeValue.S(messageId))
            updateExpression = "SET #status = :status"
            expressionAttributeNames = mapOf("#status" to "status")
            expressionAttributeValues = mapOf(":status" to AttributeValue.S(status.name))
        }
        dbClient.updateItem(request)
    }

    suspend fun getMessageById(messageId: String): Message? {
        val request = GetItemRequest {
            tableName = this@MessageRepository.tableName
            key = mapOf("id" to AttributeValue.S(messageId))
        }
        val response = dbClient.getItem(request)
        return response.item?.toMessage()
    }

    suspend fun findExistingThread(fromId: String, toId: String, jobId: String?): String? {
        // Check messages from sender using old index structure
        val sentRequest = QueryRequest {
            tableName = this@MessageRepository.tableName
            indexName = "UserIndex" // Use existing old index
            keyConditionExpression = "fromUserId = :fromId"
            expressionAttributeValues = mapOf(
                ":fromId" to AttributeValue.S(fromId)
            )
        }
        val sentResponse = dbClient.query(sentRequest)
        val sentMessages = sentResponse.items?.map { it.toMessage() } ?: emptyList()
        
        // Since we're using old schema, check toCompanyId field
        val relevantSentMessages = sentMessages.filter { message ->
            message.toId == toId &&  // This will use the fallback logic in toMessage()
            message.jobId == jobId &&
            message.threadId != null
        }
        
        if (relevantSentMessages.isNotEmpty()) {
            return relevantSentMessages.first().threadId
        }
        
        // For now, return null since we can't efficiently query received messages
        // without the new indexes
        return null
    }

    private fun Map<String, AttributeValue>.toMessage(): Message {
        val senderProfile = this["senderProfile"]?.asM()?.let { profileMap ->
            NeurodiversityProfile(
                traits = profileMap.mapValues { it.value.asN()?.toInt() ?: 0 }
            )
        }

        return Message(
            id = this["id"]?.asS() ?: throw IllegalStateException("Message missing id"),
            // Try new field names first, fallback to old field names
            fromId = this["fromId"]?.asS() ?: this["fromUserId"]?.asS() ?: "",
            toId = this["toId"]?.asS() ?: this["toCompanyId"]?.asS() ?: "",
            jobId = this["jobId"]?.asS(),
            content = this["content"]?.asS() ?: "",
            isAnonymous = this["isAnonymous"]?.asBool() ?: false,
            senderName = this["senderName"]?.asS(),
            senderProfile = senderProfile,
            isFromCompany = this["isFromCompany"]?.asBool() ?: false,
            threadId = this["threadId"]?.asS(),
            createdAt = this["createdAt"]?.asS() ?: "",
            status = this["status"]?.asS()?.let { MessageStatus.valueOf(it) } ?: MessageStatus.SENT
        )
    }
}