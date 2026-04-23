package com.voicetasker.app.domain.repository

import com.voicetasker.app.domain.model.Reminder
import com.voicetasker.app.domain.model.ReminderType
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getRemindersForNote(noteId: Long): Flow<List<Reminder>>
    suspend fun scheduleReminder(noteId: Long, scheduledDate: Long, type: ReminderType): Long
    suspend fun cancelReminder(reminderId: Long)
    suspend fun markAsTriggered(reminderId: Long)
    suspend fun getReminderById(reminderId: Long): Reminder?
}
