package com.voicetasker.app.domain.model

data class Note(
    val id: Long = 0,
    val title: String = "",
    val transcription: String = "",
    val audioFilePath: String = "",
    val categoryId: Long = 1,
    val scheduledDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val isPinned: Boolean = false,
    val isCompleted: Boolean = false,
    val location: String = "",
    val noteTime: String = ""
)
