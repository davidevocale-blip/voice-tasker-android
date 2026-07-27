package com.voicetasker.app.domain.usecase

import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository

internal suspend fun deleteNoteWithReminders(
    noteId: Long,
    reminderRepository: ReminderRepository,
    noteRepository: NoteRepository
) {
    reminderRepository.cancelRemindersForNote(noteId)
    noteRepository.deleteNoteById(noteId)
}
