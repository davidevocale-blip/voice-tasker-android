package com.voicetasker.app.domain.repository

import com.voicetasker.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getNoteById(noteId: Long): Flow<Note?>
    fun getNotesForDate(startOfDay: Long, endOfDay: Long): Flow<List<Note>>
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNoteById(noteId: Long)
    suspend fun countNotesSince(sinceMillis: Long): Int
}
