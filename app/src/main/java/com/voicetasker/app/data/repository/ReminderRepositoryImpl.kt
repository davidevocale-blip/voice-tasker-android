package com.voicetasker.app.data.repository

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.voicetasker.app.data.local.dao.ReminderDao
import com.voicetasker.app.data.local.entity.ReminderEntity
import com.voicetasker.app.domain.model.Reminder
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.repository.ReminderRepository
import com.voicetasker.app.worker.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
    @ApplicationContext private val context: Context
) : ReminderRepository {

    override fun getRemindersForNote(noteId: Long): Flow<List<Reminder>> =
        dao.getRemindersForNote(noteId).map { list -> list.map { it.toDomain() } }

    override suspend fun scheduleReminder(noteId: Long, scheduledDate: Long, type: ReminderType): Long {
        val triggerAt = scheduledDate - type.offsetMs
        val delay = triggerAt - System.currentTimeMillis()
        if (delay <= 0) return -1
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("noteId" to noteId, "reminderId" to 0L))
            .build()
        val entity = ReminderEntity(noteId = noteId, triggerAt = triggerAt, type = type.name, workRequestId = request.id.toString())
        val id = dao.insertReminder(entity)
        WorkManager.getInstance(context).enqueue(request)
        return id
    }

    override suspend fun cancelReminder(reminderId: Long) {
        val reminder = dao.getReminderById(reminderId)
        if (reminder != null && reminder.workRequestId.isNotBlank()) {
            try { WorkManager.getInstance(context).cancelWorkById(java.util.UUID.fromString(reminder.workRequestId)) } catch (_: Exception) {}
        }
        dao.deleteReminderById(reminderId)
    }

    override suspend fun markAsTriggered(reminderId: Long) = dao.markAsTriggered(reminderId)

    override suspend fun getReminderById(reminderId: Long): Reminder? = dao.getReminderById(reminderId)?.toDomain()
}

private fun ReminderEntity.toDomain() = Reminder(id, noteId, triggerAt, try { ReminderType.valueOf(type) } catch (_: Exception) { ReminderType.TWO_HOURS }, isTriggered, workRequestId)
