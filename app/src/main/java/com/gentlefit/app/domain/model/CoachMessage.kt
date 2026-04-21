package com.gentlefit.app.domain.model

data class CoachMessage(
    val id: Long = 0,
    val text: String,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val quickReplies: List<String> = emptyList()
)

enum class MessageType {
    COACH,
    USER,
    CELEBRATION,
    REMINDER
}
