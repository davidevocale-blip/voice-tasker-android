package com.voicetasker.app.domain.repository

import com.voicetasker.app.domain.model.Reminder
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.reminder.ReminderTriggerResult
import kotlinx.coroutines.flow.Flow
import java.time.Instant

sealed interface ReminderScheduleResult {
    data class Success(
        val reminderId: Long,
        val triggerAt: Instant
    ) : ReminderScheduleResult

    data class CalculationFailure(
        val reason: ReminderTriggerResult.Failure
    ) : ReminderScheduleResult

    data object PersistenceFailure : ReminderScheduleResult
    data object SchedulingFailure : ReminderScheduleResult
}

interface ReminderRepository {
    fun getRemindersForNote(noteId: Long): Flow<List<Reminder>>
    suspend fun scheduleReminder(
        noteId: Long,
        scheduledDate: Long?,
        noteTime: String,
        type: ReminderType
    ): ReminderScheduleResult
    suspend fun cancelReminder(reminderId: Long)
    suspend fun markAsTriggered(reminderId: Long)
    suspend fun getReminderById(reminderId: Long): Reminder?
}
