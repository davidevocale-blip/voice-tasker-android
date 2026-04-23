package com.voicetasker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "reminders", indices = [Index("noteId"), Index("triggerAt")])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val triggerAt: Long,
    val type: String,
    val isTriggered: Boolean = false,
    val workRequestId: String = ""
)
