package com.voicetasker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes", indices = [Index("categoryId"), Index("scheduledDate")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val transcription: String,
    val audioFilePath: String,
    val categoryId: Long = 1,
    val scheduledDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val durationMs: Long,
    val isPinned: Boolean = false,
    val isCompleted: Boolean = false
)
