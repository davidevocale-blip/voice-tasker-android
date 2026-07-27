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
import kotlinx.coroutines.runBlocking
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
            prepareWork = { _, initialDelayMillis ->
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
            prepareWork = { _, _ -> PreparedReminderWork("work-id") {} }
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
            prepareWork = { _, _ -> PreparedReminderWork("work-id") {} }
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
            prepareWork = { _, _ ->
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
            prepareWork = { _, _ ->
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
            prepareWork = { _, _ -> PreparedReminderWork("work-id") { enqueued += 1 } }
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
        assertEquals("work-id", dao.inserted.single().workRequestId)
        assertEquals(1, enqueued)
    }

    @Test
    fun `persistence error does not enqueue prepared work`() = runBlocking {
        val dao = FakeReminderDao(insertFailure = true)
        var enqueued = 0
        val repository = repository(
            dao = dao,
            prepareWork = { _, _ -> PreparedReminderWork("work-id") { enqueued += 1 } }
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
    fun `enqueue error removes the newly persisted reminder`() = runBlocking {
        val dao = FakeReminderDao(nextId = 41)
        val repository = repository(
            dao = dao,
            prepareWork = { _, _ ->
                PreparedReminderWork("work-id") { error("WorkManager unavailable") }
            }
        )

        val result = repository.scheduleReminder(
            noteId = 7,
            scheduledDate = date,
            noteTime = "10:30",
            type = ReminderType.TWO_HOURS
        )

        assertEquals(ReminderScheduleResult.SchedulingFailure, result)
        assertEquals(listOf(41L), dao.deletedIds)
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
            prepareWork = { _, _ ->
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
            prepareWork = { _, _ -> PreparedReminderWork("work-id") {} }
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

    private fun repository(
        dao: FakeReminderDao,
        clock: Clock = Clock.fixed(now, ZoneOffset.UTC),
        prepareWork: (Long, Long) -> PreparedReminderWork
    ) = ReminderRepositoryImpl(
        dao = dao,
        calculator = ReminderTriggerCalculator(),
        clock = clock,
        zoneIdProvider = { ZoneId.of("Europe/Rome") },
        prepareWork = prepareWork,
        cancelWork = {}
    )

    private class FakeReminderDao(
        private val nextId: Long = 1,
        private val insertFailure: Boolean = false,
        private val insertCancellation: Boolean = false
    ) : ReminderDao {
        val inserted = mutableListOf<ReminderEntity>()
        val deletedIds = mutableListOf<Long>()

        override fun getRemindersForNote(noteId: Long): Flow<List<ReminderEntity>> = flowOf(emptyList())
        override suspend fun getReminderById(reminderId: Long): ReminderEntity? = null
        override suspend fun markAsTriggered(reminderId: Long) = Unit
        override suspend fun insertReminder(reminder: ReminderEntity): Long {
            if (insertCancellation) throw CancellationException("cancelled")
            if (insertFailure) error("database unavailable")
            inserted += reminder
            return nextId
        }
        override suspend fun deleteRemindersForNote(noteId: Long) = Unit
        override suspend fun deleteReminderById(reminderId: Long) {
            deletedIds += reminderId
        }
    }

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
