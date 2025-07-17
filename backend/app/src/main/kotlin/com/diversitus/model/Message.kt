package com.diversitus.model

import kotlinx.serialization.Serializable
import java.util.UUID
import java.time.Instant

@Serializable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val fromUserId: String,
    val toCompanyId: String,
    val jobId: String? = null, // Optional: if message is about a specific job
    val content: String,
    val isAnonymous: Boolean = false, // User choice: true = anonymous, false = disclosed
    val senderName: String? = null, // Included only if not anonymous
    val senderProfile: NeurodiversityProfile? = null, // Included only if not anonymous
    val isFromCompany: Boolean = false, // true if company is replying to user
    val threadId: String? = null, // For grouping related messages
    val createdAt: String = Instant.now().toString(),
    val status: MessageStatus = MessageStatus.SENT
)

@Serializable
enum class MessageStatus {
    SENT,
    READ,
    REPLIED
}

@Serializable
data class MessageStatusUpdate(
    val status: MessageStatus
)