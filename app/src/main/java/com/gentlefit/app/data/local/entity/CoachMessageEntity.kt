package com.gentlefit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coach_messages")
data class CoachMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val quickReplies: String = "" // comma-separated
)
