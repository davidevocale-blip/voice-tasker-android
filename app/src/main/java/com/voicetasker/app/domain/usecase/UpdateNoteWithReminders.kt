package com.voicetasker.app.domain.usecase

import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository
import com.voicetasker.app.domain.repository.ReminderScheduleResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal suspend fun updateNoteWithReminders(
    original: Note,
    updated: Note,
    noteRepository: NoteRepository,
    reminderRepository: ReminderRepository
): ReminderScheduleResult? {
    noteRepository.updateNote(updated)
    if (!original.reminderScheduleDiffersFrom(updated)) return null

    return try {
        val result = reminderRepository.rescheduleActiveReminders(
            noteId = updated.id,
            scheduledDate = updated.scheduledDate,
            noteTime = updated.noteTime
        )
        if (result != null && result !is ReminderScheduleResult.Success) {
            restoreOriginalNoteBestEffort(original, noteRepository)
        }
        result
    } catch (error: CancellationException) {
        restoreOriginalNoteBestEffort(original, noteRepository)
        throw error
    } catch (error: Exception) {
        restoreOriginalNoteBestEffort(original, noteRepository)
        throw error
    }
}

private suspend fun restoreOriginalNoteBestEffort(
    original: Note,
    noteRepository: NoteRepository
) {
    withContext(NonCancellable) {
        try {
            noteRepository.updateNote(original)
        } catch (_: Exception) {
            // Reminder failure/cancellation remains primary even if restoration fails.
        }
    }
}

internal fun Note.reminderScheduleDiffersFrom(other: Note): Boolean =
    scheduledDate != other.scheduledDate || noteTime != other.noteTime
