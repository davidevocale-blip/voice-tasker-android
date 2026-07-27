package com.voicetasker.app.ui.screen.notedetail

import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.model.Reminder
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository
import com.voicetasker.app.domain.repository.ReminderScheduleResult
import com.voicetasker.app.domain.usecase.deleteNoteWithReminders
import com.voicetasker.app.domain.usecase.reminderScheduleDiffersFrom
import com.voicetasker.app.domain.usecase.updateNoteWithReminders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class NoteReminderLifecycleTest {
    @Test
    fun `note deletion cancels reminders before deleting note`() = runBlocking {
        val events = mutableListOf<String>()

        deleteNoteWithReminders(
            noteId = 42,
            reminderRepository = FakeReminderRepository(events),
            noteRepository = FakeNoteRepository(events)
        )

        assertEquals(listOf("cancel:42", "delete:42"), events)
    }

    @Test
    fun `successful date edit keeps updated note after rescheduling`() = runBlocking {
        val events = mutableListOf<String>()
        val original = Note(id = 42, scheduledDate = 100, noteTime = "09:00")

        assertTrue(original.reminderScheduleDiffersFrom(original.copy(scheduledDate = 200)))
        assertTrue(original.reminderScheduleDiffersFrom(original.copy(noteTime = "10:00")))
        updateNoteWithReminders(
            original = original,
            updated = original.copy(scheduledDate = 200),
            noteRepository = FakeNoteRepository(events),
            reminderRepository = FakeReminderRepository(events)
        )
        assertEquals(listOf("update:200", "reschedule:42"), events)
    }

    @Test
    fun `rescheduling failure restores original note and returns reminder failure`() = runBlocking {
        val events = mutableListOf<String>()
        val original = Note(id = 42, scheduledDate = 100, noteTime = "09:00")
        val failure = ReminderScheduleResult.SchedulingFailure

        val result = updateNoteWithReminders(
            original = original,
            updated = original.copy(scheduledDate = 200),
            noteRepository = FakeNoteRepository(events),
            reminderRepository = FakeReminderRepository(events, result = failure)
        )

        assertEquals(failure, result)
        assertEquals(listOf("update:200", "reschedule:42", "update:100"), events)
    }

    @Test
    fun `restoration failure does not hide reminder failure`() = runBlocking {
        val events = mutableListOf<String>()
        val original = Note(id = 42, scheduledDate = 100, noteTime = "09:00")
        val failure = ReminderScheduleResult.PersistenceFailure

        val result = updateNoteWithReminders(
            original = original,
            updated = original.copy(scheduledDate = 200),
            noteRepository = FakeNoteRepository(events, failOnUpdateNumber = 2),
            reminderRepository = FakeReminderRepository(events, result = failure)
        )

        assertEquals(failure, result)
        assertEquals(listOf("update:200", "reschedule:42", "update:100"), events)
    }

    @Test
    fun `no active reminders keeps updated note`() = runBlocking {
        val events = mutableListOf<String>()
        val original = Note(id = 42, scheduledDate = 100, noteTime = "09:00")

        val result = updateNoteWithReminders(
            original = original,
            updated = original.copy(noteTime = "10:00"),
            noteRepository = FakeNoteRepository(events),
            reminderRepository = FakeReminderRepository(events, result = null)
        )

        assertEquals(null, result)
        assertEquals(listOf("update:100", "reschedule:42"), events)
    }

    @Test
    fun `cancellation during rescheduling restores original note then propagates`() {
        val events = mutableListOf<String>()
        val original = Note(id = 42, scheduledDate = 100, noteTime = "09:00")

        assertThrows(CancellationException::class.java) {
            runBlocking {
                updateNoteWithReminders(
                    original = original,
                    updated = original.copy(scheduledDate = 200),
                    noteRepository = FakeNoteRepository(events, yieldOnUpdate = true),
                    reminderRepository = FakeReminderRepository(
                        events,
                        cancelCoroutine = true
                    )
                )
            }
        }

        events += "propagated"
        assertEquals(
            listOf("update:200", "reschedule:42", "update:100", "propagated"),
            events
        )
    }

    @Test
    fun `title content category and location edits update note without rescheduling`() = runBlocking {
        val events = mutableListOf<String>()
        val original = Note(
            id = 42,
            title = "old",
            transcription = "old content",
            categoryId = 1,
            location = "Rome",
            scheduledDate = 100,
            noteTime = "09:00"
        )
        val edited = original.copy(
            title = "new",
            transcription = "new content",
            categoryId = 2,
            location = "Milan"
        )

        assertFalse(original.reminderScheduleDiffersFrom(edited))
        updateNoteWithReminders(
            original = original,
            updated = edited,
            noteRepository = FakeNoteRepository(events),
            reminderRepository = FakeReminderRepository(events)
        )
        assertEquals(listOf("update:100"), events)
    }

    private class FakeNoteRepository(
        private val events: MutableList<String>,
        private val failOnUpdateNumber: Int? = null,
        private val yieldOnUpdate: Boolean = false
    ) : NoteRepository {
        private var updateCount = 0
        override fun getAllNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun getNoteById(noteId: Long): Flow<Note?> = flowOf(null)
        override fun getNotesForDate(startOfDay: Long, endOfDay: Long): Flow<List<Note>> =
            flowOf(emptyList())
        override fun getDaysWithNotesInMonth(monthStart: Long, monthEnd: Long): Flow<List<Int>> =
            flowOf(emptyList())
        override suspend fun insertNote(note: Note): Long = 0
        override suspend fun updateNote(note: Note) {
            if (yieldOnUpdate) yield()
            updateCount += 1
            events += "update:${note.scheduledDate}"
            if (updateCount == failOnUpdateNumber) error("note restoration failed")
        }
        override suspend fun deleteNoteById(noteId: Long) {
            events += "delete:$noteId"
        }
        override suspend fun countNotesSince(sinceMillis: Long): Int = 0
    }

    private class FakeReminderRepository(
        private val events: MutableList<String>,
        private val result: ReminderScheduleResult? =
            ReminderScheduleResult.Success(42, java.time.Instant.EPOCH),
        private val cancelCoroutine: Boolean = false
    ) : ReminderRepository {
        override fun getRemindersForNote(noteId: Long): Flow<List<Reminder>> = flowOf(emptyList())
        override suspend fun scheduleReminder(
            noteId: Long,
            scheduledDate: Long?,
            noteTime: String,
            type: ReminderType
        ): ReminderScheduleResult = error("unused")
        override suspend fun cancelReminder(reminderId: Long) = Unit
        override suspend fun cancelRemindersForNote(noteId: Long) {
            events += "cancel:$noteId"
        }
        override suspend fun rescheduleActiveReminders(
            noteId: Long,
            scheduledDate: Long?,
            noteTime: String
        ): ReminderScheduleResult? {
            events += "reschedule:$noteId"
            if (cancelCoroutine) {
                currentCoroutineContext().cancel(CancellationException("cancelled"))
                yield()
            }
            return result
        }
        override suspend fun markAsTriggered(reminderId: Long) = Unit
        override suspend fun getReminderById(reminderId: Long): Reminder? = null
    }
}
