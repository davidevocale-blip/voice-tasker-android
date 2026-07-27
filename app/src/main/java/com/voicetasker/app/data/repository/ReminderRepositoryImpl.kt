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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal data class PreparedReminderWork(
    val id: String,
    val enqueue: () -> Unit
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
    private val prepareWork: (noteId: Long, initialDelayMillis: Long) -> PreparedReminderWork,
    private val cancelWork: (workRequestId: String) -> Unit
) : ReminderRepository {

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
        prepareWork = { noteId, initialDelayMillis ->
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("noteId" to noteId, "reminderId" to 0L))
                .build()
            PreparedReminderWork(request.id.toString()) {
                WorkManager.getInstance(context).enqueue(request)
            }
        },
        cancelWork = { workRequestId ->
            WorkManager.getInstance(context).cancelWorkById(UUID.fromString(workRequestId))
        }
    )

    override fun getRemindersForNote(noteId: Long): Flow<List<Reminder>> =
        dao.getRemindersForNote(noteId).map { list -> list.map { it.toDomain() } }

    override suspend fun scheduleReminder(
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
        val work = try {
            prepareWork(noteId, schedulePlan.initialDelayMillis)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return ReminderScheduleResult.SchedulingFailure
        }
        val entity = ReminderEntity(
            noteId = noteId,
            triggerAt = schedulePlan.triggerAtMillis,
            type = type.name,
            workRequestId = work.id
        )
        val reminderId = try {
            dao.insertReminder(entity)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return ReminderScheduleResult.PersistenceFailure
        }
        try {
            work.enqueue()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            try {
                dao.deleteReminderById(reminderId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // The scheduling failure remains the primary error.
            }
            return ReminderScheduleResult.SchedulingFailure
        }
        return ReminderScheduleResult.Success(
            reminderId = reminderId,
            triggerAt = Instant.ofEpochMilli(schedulePlan.triggerAtMillis)
        )
    }

    override suspend fun cancelReminder(reminderId: Long) {
        val reminder = dao.getReminderById(reminderId)
        if (reminder != null && reminder.workRequestId.isNotBlank()) {
            try { cancelWork(reminder.workRequestId) } catch (_: Exception) {}
        }
        dao.deleteReminderById(reminderId)
    }

    override suspend fun markAsTriggered(reminderId: Long) = dao.markAsTriggered(reminderId)

    override suspend fun getReminderById(reminderId: Long): Reminder? = dao.getReminderById(reminderId)?.toDomain()
}

private fun ReminderEntity.toDomain() = Reminder(id, noteId, triggerAt, try { ReminderType.valueOf(type) } catch (_: Exception) { ReminderType.TWO_HOURS }, isTriggered, workRequestId)
