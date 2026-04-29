package com.personalassistant.jarvis.data

import java.util.UUID

enum class MessageRole { User, Assistant }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pending: Boolean = false,
    val imageUri: String? = null,
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val preview: String
        get() = messages.lastOrNull { it.role == MessageRole.User }?.body
            ?: messages.firstOrNull()?.body
            ?: ""
}
