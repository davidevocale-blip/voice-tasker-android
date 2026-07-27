package com.voicetasker.app.data.repository

import com.voicetasker.app.data.local.dao.ReminderDao
import com.voicetasker.app.data.local.entity.ReminderEntity
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.reminder.ReminderTriggerCalculator
import com.voicetasker.app.domain.reminder.ReminderTriggerResult
import com.voicetasker.app.domain.repository.ReminderScheduleResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class ReminderRepositoryImplTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val date = Instant.parse("2026-07-28T00:00:00Z").toEpochMilli()

    @Test
    fun `Pixel 8 case persists trigger and schedules the same instant`() = runBlocking {
        val dao = FakeReminderDao(nextId = 5)
        var preparedDelayMillis: Long? = null
        val repository = repository(
            dao = dao,
            clock = Clock.fixed(
                Instant.ofEpochMilli(1_785_101_571_260L),
                ZoneOffset.UTC
            ),
            prepareWork = { _, reminderId, initialDelayMillis ->
                assertEquals(5L, reminderId)
                preparedDelayMillis = initialDelayMillis
                PreparedReminderWork(
                    id = "3ec9cb14-d35f-4544-88b9-db6dc3fd21f4",
                    enqueue = {}
                )
            }
        )

        val result = repository.scheduleReminder(
            noteId = 4,
            scheduledDate = 1_785_110_400_000L,
            noteTime = "01:35",
            type = ReminderType.TWO_HOURS
        )

        assertEquals(
            ReminderScheduleResult.Success(
                reminderId = 5,
                triggerAt = Instant.ofEpochMilli(1_785_101_700_000L)
            ),
            result
        )
        assertEquals(1_785_101_700_000L, dao.inserted.single().triggerAt)
        assertEquals(128_740L, preparedDelayMillis)
        assertTrue(dao.inserted.single().triggerAt != 1_785_108_900_000L)
    }

    @Test
    fun `repository schedules twelve hours before the event`() = runBlocking {
        val dao = FakeReminderDao()
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ -> PreparedReminderWork("work-id") {} }
        )

        val result = repository.scheduleReminder(
            noteId = 7,
            scheduledDate = Instant.parse("2026-08-10T00:00:00Z").toEpochMilli(),
            noteTime = "09:00",
            type = ReminderType.TWELVE_HOURS
        )

        val expectedTrigger = Instant.parse("2026-08-09T19:00:00Z")
        assertEquals(
            ReminderScheduleResult.Success(1, expectedTrigger),
            result
        )
        assertEquals(expectedTrigger.toEpochMilli(), dao.inserted.single().triggerAt)
    }

    @Test
    fun `repository keeps local wall time on the previous day`() = runBlocking {
        val dao = FakeReminderDao()
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ -> PreparedReminderWork("work-id") {} }
        )

        val result = repository.scheduleReminder(
            noteId = 7,
            scheduledDate = Instant.parse("2026-08-10T00:00:00Z").toEpochMilli(),
            noteTime = "08:00",
            type = ReminderType.ONE_DAY
        )

        val expectedTrigger = Instant.parse("2026-08-09T06:00:00Z")
        assertEquals(
            ReminderScheduleResult.Success(1, expectedTrigger),
            result
        )
        assertEquals(expectedTrigger.toEpochMilli(), dao.inserted.single().triggerAt)
    }

    @Test
    fun `failed calculation neither prepares work nor inserts entity`() = runBlocking {
        val dao = FakeReminderDao()
        var preparedWork = 0
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ ->
                preparedWork += 1
                PreparedReminderWork("work-id") {}
            }
        )

        val result = repository.scheduleReminder(
            noteId = 7,
            scheduledDate = null,
            noteTime = "10:30",
            type = ReminderType.TWO_HOURS
        )

        assertEquals(
            ReminderScheduleResult.CalculationFailure(ReminderTriggerResult.MissingDate),
            result
        )
        assertEquals(0, preparedWork)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun `past trigger neither prepares work nor inserts entity`() = runBlocking {
        val dao = FakeReminderDao()
        var preparedWork = 0
        val repository = repository(
            dao = dao,
            clock = Clock.fixed(Instant.parse("2026-07-28T06:30:00Z"), ZoneOffset.UTC),
            prepareWork = { _, _, _ ->
                preparedWork += 1
                PreparedReminderWork("work-id") {}
            }
        )

        val result = repository.scheduleReminder(
            noteId = 7,
            scheduledDate = date,
            noteTime = "10:30",
            type = ReminderType.TWO_HOURS
        )

        assertEquals(
            ReminderScheduleResult.CalculationFailure(ReminderTriggerResult.TriggerInPast),
            result
        )
        assertEquals(0, preparedWork)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun `successful calculation persists exact trigger and enqueues work`() = runBlocking {
        val dao = FakeReminderDao(nextId = 41)
        var enqueued = 0
        val repository = repository(
            dao = dao,
            prepareWork = { _, reminderId, _ ->
                assertEquals(41L, reminderId)
                PreparedReminderWork("work-id") {
                    assertEquals("work-id", dao.rows.single().workRequestId)
                    enqueued += 1
                }
            }
        )

        val result = repository.scheduleReminder(
            noteId = 7,
            scheduledDate = date,
            noteTime = "10:30",
            type = ReminderType.TWO_HOURS
        )

        assertEquals(
            ReminderScheduleResult.Success(
                reminderId = 41,
                triggerAt = Instant.parse("2026-07-28T06:30:00Z")
            ),
            result
        )
        assertEquals(Instant.parse("2026-07-28T06:30:00Z").toEpochMilli(), dao.inserted.single().triggerAt)
        assertEquals("", dao.inserted.single().workRequestId)
        assertEquals("work-id", dao.rows.single().workRequestId)
        assertEquals(1, enqueued)
    }

    @Test
    fun `persistence error does not enqueue prepared work`() = runBlocking {
        val dao = FakeReminderDao(insertFailure = true)
        var enqueued = 0
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ -> PreparedReminderWork("work-id") { enqueued += 1 } }
        )

        val result = repository.scheduleReminder(
            noteId = 7,
            scheduledDate = date,
            noteTime = "10:30",
            type = ReminderType.TWO_HOURS
        )

        assertEquals(ReminderScheduleResult.PersistenceFailure, result)
        assertEquals(0, enqueued)
    }

    @Test
    fun `enqueue error cancels prepared work and removes the newly persisted reminder`() = runBlocking {
        val dao = FakeReminderDao(nextId = 41)
        val cancelled = mutableListOf<String>()
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ ->
                PreparedReminderWork("work-id") { error("WorkManager unavailable") }
            },
            cancelWork = cancelled::add
        )

        val result = repository.scheduleReminder(
            noteId = 7,
            scheduledDate = date,
            noteTime = "10:30",
            type = ReminderType.TWO_HOURS
        )

        assertEquals(ReminderScheduleResult.SchedulingFailure, result)
        assertEquals(listOf("work-id"), cancelled)
        assertEquals(listOf(41L), dao.deletedIds)
    }

    @Test
    fun `cancellation during enqueue cancels prepared work and removes row before propagating`() {
        val dao = FakeReminderDao(nextId = 42, yieldBeforeDelete = true)
        val events = mutableListOf<String>()
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ ->
                PreparedReminderWork("cancelled-work") {
                    currentCoroutineContext().cancel(
                        CancellationException("enqueue cancelled")
                    )
                    yield()
                }
            },
            cancelWork = {
                yield()
                events += "cancel:$it"
            }
        )

        assertThrows(CancellationException::class.java) {
            runBlocking {
                repository.scheduleReminder(7, date, "10:30", ReminderType.TWO_HOURS)
            }
        }

        events += "propagated"
        assertEquals(listOf("cancel:cancelled-work", "propagated"), events)
        assertEquals(listOf(42L), dao.deletedIds)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `trigger that expires after calculation is rejected before work preparation`() = runBlocking {
        val dao = FakeReminderDao()
        var preparedWork = 0
        val repository = repository(
            dao = dao,
            clock = AdvancingClock(
                first = Instant.parse("2026-07-28T06:29:59Z"),
                later = Instant.parse("2026-07-28T06:30:00Z")
            ),
            prepareWork = { _, _, _ ->
                preparedWork += 1
                PreparedReminderWork("work-id") {}
            }
        )

        val result = repository.scheduleReminder(
            noteId = 7,
            scheduledDate = date,
            noteTime = "10:30",
            type = ReminderType.TWO_HOURS
        )

        assertEquals(
            ReminderScheduleResult.CalculationFailure(ReminderTriggerResult.TriggerInPast),
            result
        )
        assertEquals(0, preparedWork)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun `database cancellation is propagated`() {
        val repository = repository(
            dao = FakeReminderDao(insertCancellation = true),
            prepareWork = { _, _, _ -> PreparedReminderWork("work-id") {} }
        )

        assertThrows(CancellationException::class.java) {
            runBlocking {
                repository.scheduleReminder(
                    noteId = 7,
                    scheduledDate = date,
                    noteTime = "10:30",
                    type = ReminderType.TWO_HOURS
                )
            }
        }
    }

    @Test
    fun `work preparation failure removes inserted row`() = runBlocking {
        val dao = FakeReminderDao(nextId = 12)
        val repository = repository(
            dao = dao,
            prepareWork = { _, reminderId, _ ->
                assertEquals(12L, reminderId)
                error("cannot prepare work")
            }
        )

        val result = repository.scheduleReminder(7, date, "10:30", ReminderType.TWO_HOURS)

        assertEquals(ReminderScheduleResult.SchedulingFailure, result)
        assertTrue(dao.rows.isEmpty())
        assertEquals(listOf(12L), dao.deletedIds)
    }

    @Test
    fun `work id update failure removes inserted row and does not enqueue`() = runBlocking {
        val dao = FakeReminderDao(nextId = 13, updateFailure = true)
        var enqueued = false
        val cancelled = mutableListOf<String>()
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ ->
                PreparedReminderWork("work-id") { enqueued = true }
            },
            cancelWork = cancelled::add
        )

        val result = repository.scheduleReminder(7, date, "10:30", ReminderType.TWO_HOURS)

        assertEquals(ReminderScheduleResult.PersistenceFailure, result)
        assertTrue(dao.rows.isEmpty())
        assertEquals(false, enqueued)
        assertTrue(cancelled.isEmpty())
    }

    @Test
    fun `cancel reminders for note cancels every valid stored work and deletes rows`() = runBlocking {
        val dao = FakeReminderDao(
            initialRows = listOf(
                reminder(id = 1, workRequestId = "first"),
                reminder(id = 2, workRequestId = "invalid"),
                reminder(id = 3, workRequestId = ""),
                reminder(id = 4, noteId = 99, workRequestId = "other")
            )
        )
        val cancelled = mutableListOf<String>()
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ -> error("unused") },
            cancelWork = {
                if (it == "invalid") error("bad UUID")
                cancelled += it
            }
        )

        repository.cancelRemindersForNote(7)

        assertEquals(listOf("first"), cancelled)
        assertEquals(listOf(4L), dao.rows.map { it.id })
    }

    @Test
    fun `adding an active type replaces its row and worker but keeps triggered history`() = runBlocking {
        val dao = FakeReminderDao(
            nextId = 20,
            initialRows = listOf(
                reminder(id = 1, type = ReminderType.TWO_HOURS, workRequestId = "old"),
                reminder(
                    id = 2,
                    type = ReminderType.TWO_HOURS,
                    isTriggered = true,
                    workRequestId = "history"
                )
            )
        )
        val cancelled = mutableListOf<String>()
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ -> PreparedReminderWork("new") {} },
            cancelWork = cancelled::add
        )

        val result = repository.scheduleReminder(7, date, "10:30", ReminderType.TWO_HOURS)

        assertTrue(result is ReminderScheduleResult.Success)
        assertEquals(listOf("old"), cancelled)
        assertEquals(
            listOf(2L, 20L),
            dao.rows.filter { it.type == ReminderType.TWO_HOURS.name }.map { it.id }.sorted()
        )
        assertEquals(1, dao.rows.count { !it.isTriggered && it.type == ReminderType.TWO_HOURS.name })
    }

    @Test
    fun `reschedule replaces unique active types and ignores triggered reminders`() = runBlocking {
        val dao = FakeReminderDao(
            nextId = 20,
            initialRows = listOf(
                reminder(id = 1, type = ReminderType.TWO_HOURS, workRequestId = "two-a"),
                reminder(id = 2, type = ReminderType.TWO_HOURS, workRequestId = "two-b"),
                reminder(id = 3, type = ReminderType.ONE_DAY, workRequestId = "day"),
                reminder(
                    id = 4,
                    type = ReminderType.TWELVE_HOURS,
                    isTriggered = true,
                    workRequestId = "history"
                )
            )
        )
        val preparedTypes = mutableListOf<Long>()
        val cancelled = mutableListOf<String>()
        val repository = repository(
            dao = dao,
            prepareWork = { _, reminderId, _ ->
                preparedTypes += reminderId
                PreparedReminderWork("new-$reminderId") {}
            },
            cancelWork = cancelled::add
        )

        val result = repository.rescheduleActiveReminders(7, date, "11:30")

        assertTrue(result is ReminderScheduleResult.Success)
        assertEquals(listOf(20L, 21L), preparedTypes)
        assertEquals(listOf("two-a", "two-b", "day"), cancelled)
        assertEquals(1, dao.rows.count { !it.isTriggered && it.type == ReminderType.TWO_HOURS.name })
        assertEquals(1, dao.rows.count { !it.isTriggered && it.type == ReminderType.ONE_DAY.name })
        assertEquals(listOf(4L), dao.rows.filter { it.isTriggered }.map { it.id })
    }

    @Test
    fun `failed replacement removes new reminders and preserves old active rows`() = runBlocking {
        val dao = FakeReminderDao(
            nextId = 20,
            initialRows = listOf(
                reminder(id = 1, type = ReminderType.TWO_HOURS, workRequestId = "two"),
                reminder(id = 2, type = ReminderType.ONE_DAY, workRequestId = "day")
            )
        )
        var preparations = 0
        val cancelled = mutableListOf<String>()
        val repository = repository(
            dao = dao,
            prepareWork = { _, reminderId, _ ->
                preparations += 1
                if (preparations == 2) error("second work fails")
                PreparedReminderWork("new-$reminderId") {}
            },
            cancelWork = cancelled::add
        )

        val result = repository.rescheduleActiveReminders(7, date, "11:30")

        assertEquals(ReminderScheduleResult.SchedulingFailure, result)
        assertEquals(listOf(1L, 2L), dao.rows.map { it.id }.sorted())
        assertEquals(listOf("new-20"), cancelled)
    }

    @Test
    fun `old row deletion failure rolls back newly enqueued replacement`() = runBlocking {
        val dao = FakeReminderDao(
            nextId = 20,
            initialRows = listOf(
                reminder(id = 1, type = ReminderType.TWO_HOURS, workRequestId = "old")
            ),
            deleteFailureIds = setOf(1)
        )
        val cancelled = mutableListOf<String>()
        val repository = repository(
            dao = dao,
            prepareWork = { _, _, _ -> PreparedReminderWork("new") {} },
            cancelWork = cancelled::add
        )

        val result = repository.scheduleReminder(7, date, "10:30", ReminderType.TWO_HOURS)

        assertEquals(ReminderScheduleResult.PersistenceFailure, result)
        assertEquals(listOf(1L), dao.rows.map { it.id })
        assertEquals(listOf("old", "new"), cancelled)
    }

    private fun repository(
        dao: FakeReminderDao,
        clock: Clock = Clock.fixed(now, ZoneOffset.UTC),
        prepareWork: (Long, Long, Long) -> PreparedReminderWork,
        cancelWork: suspend (String) -> Unit = {}
    ) = ReminderRepositoryImpl(
        dao = dao,
        calculator = ReminderTriggerCalculator(),
        clock = clock,
        zoneIdProvider = { ZoneId.of("Europe/Rome") },
        prepareWork = prepareWork,
        cancelWork = cancelWork
    )

    private class FakeReminderDao(
        nextId: Long = 1,
        private val insertFailure: Boolean = false,
        private val insertCancellation: Boolean = false,
        private val updateFailure: Boolean = false,
        initialRows: List<ReminderEntity> = emptyList(),
        private val deleteFailureIds: Set<Long> = emptySet(),
        private val yieldBeforeDelete: Boolean = false
    ) : ReminderDao {
        private var generatedId = nextId
        val inserted = mutableListOf<ReminderEntity>()
        val rows = initialRows.toMutableList()
        val deletedIds = mutableListOf<Long>()

        override fun getRemindersForNote(noteId: Long): Flow<List<ReminderEntity>> = flowOf(emptyList())
        override suspend fun getRemindersForNoteOnce(noteId: Long): List<ReminderEntity> =
            rows.filter { it.noteId == noteId }
        override suspend fun getReminderById(reminderId: Long): ReminderEntity? =
            rows.find { it.id == reminderId }
        override suspend fun markAsTriggered(reminderId: Long) = Unit
        override suspend fun insertReminder(reminder: ReminderEntity): Long {
            if (insertCancellation) throw CancellationException("cancelled")
            if (insertFailure) error("database unavailable")
            inserted += reminder
            val id = generatedId++
            rows += reminder.copy(id = id)
            return id
        }
        override suspend fun updateWorkRequestId(reminderId: Long, workRequestId: String): Int {
            if (updateFailure) error("database unavailable")
            val index = rows.indexOfFirst { it.id == reminderId }
            if (index == -1) return 0
            rows[index] = rows[index].copy(workRequestId = workRequestId)
            return 1
        }
        override suspend fun deleteRemindersForNote(noteId: Long) {
            rows.removeAll { it.noteId == noteId }
        }
        override suspend fun deleteReminderById(reminderId: Long) {
            if (yieldBeforeDelete) yield()
            if (reminderId in deleteFailureIds) error("database unavailable")
            deletedIds += reminderId
            rows.removeAll { it.id == reminderId }
        }
    }

    private fun reminder(
        id: Long,
        noteId: Long = 7,
        type: ReminderType = ReminderType.TWO_HOURS,
        isTriggered: Boolean = false,
        workRequestId: String
    ) = ReminderEntity(
        id = id,
        noteId = noteId,
        triggerAt = Instant.parse("2026-07-28T06:30:00Z").toEpochMilli(),
        type = type.name,
        isTriggered = isTriggered,
        workRequestId = workRequestId
    )

    private class AdvancingClock(
        private val first: Instant,
        private val later: Instant
    ) : Clock() {
        private var calls = 0

        override fun instant(): Instant =
            if (calls++ == 0) first else later

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this
    }
}
