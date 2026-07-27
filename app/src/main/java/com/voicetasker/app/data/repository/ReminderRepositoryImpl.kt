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
import com.voicetasker.app.domain.reminder.ReminderTriggerCalculator
import com.voicetasker.app.domain.reminder.ReminderTriggerResult
import com.voicetasker.app.domain.repository.ReminderScheduleResult
import com.voicetasker.app.worker.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal data class PreparedReminderWork(
    val id: String,
    val enqueue: suspend () -> Unit
)

internal data class ReminderSchedulePlan(
    val triggerAtMillis: Long,
    val initialDelayMillis: Long
)

internal fun ReminderTriggerResult.Success.toSchedulePlan(
    currentInstant: Instant
): ReminderSchedulePlan? {
    val triggerAtMillis = triggerAt.toEpochMilli()
    val initialDelayMillis = triggerAtMillis - currentInstant.toEpochMilli()
    return if (initialDelayMillis > 0) {
        ReminderSchedulePlan(
            triggerAtMillis = triggerAtMillis,
            initialDelayMillis = initialDelayMillis
        )
    } else {
        null
    }
}

class ReminderRepositoryImpl internal constructor(
    private val dao: ReminderDao,
    private val calculator: ReminderTriggerCalculator,
    private val clock: Clock,
    private val zoneIdProvider: () -> ZoneId,
    private val prepareWork: (
        noteId: Long,
        reminderId: Long,
        initialDelayMillis: Long
    ) -> PreparedReminderWork,
    private val cancelWork: suspend (workRequestId: String) -> Unit
) : ReminderRepository {
    private val lifecycleMutex = Mutex()

    @Inject
    constructor(
        dao: ReminderDao,
        @ApplicationContext context: Context,
        calculator: ReminderTriggerCalculator
    ) : this(
        dao = dao,
        calculator = calculator,
        clock = Clock.systemUTC(),
        zoneIdProvider = { ZoneId.systemDefault() },
        prepareWork = { noteId, reminderId, initialDelayMillis ->
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("noteId" to noteId, "reminderId" to reminderId))
                .build()
            PreparedReminderWork(request.id.toString()) {
                runInterruptible(Dispatchers.IO) {
                    WorkManager.getInstance(context).enqueue(request).result.get()
                }
            }
        },
        cancelWork = { workRequestId ->
            runInterruptible(Dispatchers.IO) {
                WorkManager.getInstance(context)
                    .cancelWorkById(UUID.fromString(workRequestId))
                    .result
                    .get()
            }
        }
    )

    override fun getRemindersForNote(noteId: Long): Flow<List<Reminder>> =
        dao.getRemindersForNote(noteId).map { list -> list.map { it.toDomain() } }

    override suspend fun scheduleReminder(
        noteId: Long,
        scheduledDate: Long?,
        noteTime: String,
        type: ReminderType
    ): ReminderScheduleResult = lifecycleMutex.withLock {
        val previousActive = try {
            dao.getRemindersForNoteOnce(noteId)
                .filter { !it.isTriggered && it.type == type.name }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return@withLock ReminderScheduleResult.PersistenceFailure
        }
        val result = createReminder(noteId, scheduledDate, noteTime, type)
        if (result is ReminderScheduleResult.Success) {
            if (!replaceOldReminders(previousActive, listOf(result.reminderId))) {
                return@withLock ReminderScheduleResult.PersistenceFailure
            }
        }
        result
    }

    private suspend fun createReminder(
        noteId: Long,
        scheduledDate: Long?,
        noteTime: String,
        type: ReminderType
    ): ReminderScheduleResult {
        val calculation = calculator.calculate(
            scheduledDateMillis = scheduledDate,
            noteTime = noteTime,
            reminderType = type,
            zoneId = zoneIdProvider(),
            clock = clock
        )
        if (calculation is ReminderTriggerResult.Failure) {
            return ReminderScheduleResult.CalculationFailure(calculation)
        }
        val successfulCalculation = calculation as ReminderTriggerResult.Success
        val schedulePlan = successfulCalculation.toSchedulePlan(clock.instant())
        if (schedulePlan == null) {
            return ReminderScheduleResult.CalculationFailure(
                ReminderTriggerResult.TriggerInPast
            )
        }
        val entity = ReminderEntity(
            noteId = noteId,
            triggerAt = schedulePlan.triggerAtMillis,
            type = type.name
        )
        val reminderId = try {
            dao.insertReminder(entity)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return ReminderScheduleResult.PersistenceFailure
        }
        val work = try {
            prepareWork(noteId, reminderId, schedulePlan.initialDelayMillis)
        } catch (error: CancellationException) {
            cleanupNewReminderAfterCancellation(reminderId)
            throw error
        } catch (_: Exception) {
            return cleanupNewReminder(
                reminderId,
                ReminderScheduleResult.SchedulingFailure
            )
        }
        try {
            if (dao.updateWorkRequestId(reminderId, work.id) != 1) {
                return cleanupNewReminder(
                    reminderId,
                    ReminderScheduleResult.PersistenceFailure
                )
            }
        } catch (error: CancellationException) {
            cleanupNewReminderAfterCancellation(reminderId)
            throw error
        } catch (_: Exception) {
            return cleanupNewReminder(
                reminderId,
                ReminderScheduleResult.PersistenceFailure
            )
        }
        try {
            work.enqueue()
        } catch (error: CancellationException) {
            cleanupPreparedWorkAfterEnqueue(
                reminderId = reminderId,
                workRequestId = work.id
            )
            throw error
        } catch (_: Exception) {
            cleanupPreparedWorkAfterEnqueue(
                reminderId,
                work.id
            )
            return ReminderScheduleResult.SchedulingFailure
        }
        return ReminderScheduleResult.Success(
            reminderId = reminderId,
            triggerAt = Instant.ofEpochMilli(schedulePlan.triggerAtMillis)
        )
    }

    private suspend fun cleanupNewReminder(
        reminderId: Long,
        result: ReminderScheduleResult
    ): ReminderScheduleResult = withContext(NonCancellable) {
        try {
            dao.deleteReminderById(reminderId)
            result
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ReminderScheduleResult.PersistenceFailure
        }
    }

    private suspend fun cleanupNewReminderAfterCancellation(reminderId: Long) {
        withContext(NonCancellable) {
            try {
                dao.deleteReminderById(reminderId)
            } catch (_: Exception) {
                // Preserve the original coroutine cancellation.
            }
        }
    }

    private suspend fun cleanupPreparedWorkAfterEnqueue(
        reminderId: Long,
        workRequestId: String
    ) {
        withContext(NonCancellable) {
            try {
                cancelWork(workRequestId)
            } catch (_: Exception) {
                // The original enqueue failure or cancellation remains primary.
            }
            try {
                dao.deleteReminderById(reminderId)
            } catch (_: Exception) {
                // Cleanup is best effort; do not hide the original outcome.
            }
        }
    }

    override suspend fun cancelReminder(reminderId: Long) = lifecycleMutex.withLock {
        val reminder = dao.getReminderById(reminderId)
        if (reminder != null) {
            cancelStoredWork(reminder.workRequestId)
        }
        dao.deleteReminderById(reminderId)
    }

    override suspend fun cancelRemindersForNote(noteId: Long) = lifecycleMutex.withLock {
        val reminders = dao.getRemindersForNoteOnce(noteId)
        for (reminder in reminders) {
            cancelStoredWork(reminder.workRequestId)
        }
        dao.deleteRemindersForNote(noteId)
    }

    override suspend fun rescheduleActiveReminders(
        noteId: Long,
        scheduledDate: Long?,
        noteTime: String
    ): ReminderScheduleResult? = lifecycleMutex.withLock {
        val previousActive = try {
            dao.getRemindersForNoteOnce(noteId).filterNot(ReminderEntity::isTriggered)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return@withLock ReminderScheduleResult.PersistenceFailure
        }
        val types = previousActive.mapNotNull { it.type.toReminderTypeOrNull() }.distinct()
        if (types.isEmpty()) return@withLock null

        val created = mutableListOf<ReminderScheduleResult.Success>()
        for (type in types) {
            when (val result = createReminder(noteId, scheduledDate, noteTime, type)) {
                is ReminderScheduleResult.Success -> created += result
                else -> {
                    withContext(NonCancellable) {
                        rollbackNewReminders(created.map { it.reminderId })
                    }
                    return@withLock result
                }
            }
        }
        if (!replaceOldReminders(previousActive, created.map { it.reminderId })) {
            return@withLock ReminderScheduleResult.PersistenceFailure
        }
        created.last()
    }

    private suspend fun replaceOldReminders(
        previous: List<ReminderEntity>,
        createdIds: List<Long>
    ): Boolean = withContext(NonCancellable) {
        try {
            removeReminders(previous)
            true
        } catch (error: CancellationException) {
            rollbackNewReminders(createdIds)
            throw error
        } catch (_: Exception) {
            rollbackNewReminders(createdIds)
            false
        }
    }

    private suspend fun rollbackNewReminders(reminderIds: List<Long>) {
        reminderIds.forEach { reminderId ->
            try {
                cancelAndDeleteReminder(reminderId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Continue best-effort rollback of the remaining newly-created reminders.
            }
        }
    }

    private suspend fun removeReminders(reminders: List<ReminderEntity>) {
        reminders.forEach { reminder ->
            cancelStoredWork(reminder.workRequestId)
            dao.deleteReminderById(reminder.id)
        }
    }

    private suspend fun cancelAndDeleteReminder(reminderId: Long) {
        val reminder = dao.getReminderById(reminderId)
        if (reminder != null) cancelStoredWork(reminder.workRequestId)
        dao.deleteReminderById(reminderId)
    }

    private suspend fun cancelStoredWork(workRequestId: String) {
        if (workRequestId.isBlank()) return
        try {
            cancelWork(workRequestId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Invalid or already-finished WorkRequest IDs do not block row cleanup.
        }
    }

    override suspend fun markAsTriggered(reminderId: Long) = dao.markAsTriggered(reminderId)

    override suspend fun getReminderById(reminderId: Long): Reminder? = dao.getReminderById(reminderId)?.toDomain()
}

private fun ReminderEntity.toDomain() = Reminder(id, noteId, triggerAt, try { ReminderType.valueOf(type) } catch (_: Exception) { ReminderType.TWO_HOURS }, isTriggered, workRequestId)

private fun String.toReminderTypeOrNull(): ReminderType? =
    try {
        ReminderType.valueOf(this)
    } catch (_: IllegalArgumentException) {
        null
    }
