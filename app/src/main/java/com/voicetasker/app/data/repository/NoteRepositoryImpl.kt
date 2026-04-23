package com.voicetasker.app.data.repository

import com.voicetasker.app.data.local.dao.NoteDao
import com.voicetasker.app.data.local.entity.NoteEntity
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(private val dao: NoteDao) : NoteRepository {
    override fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes().map { list -> list.map { it.toDomain() } }
    override fun getNoteById(noteId: Long): Flow<Note?> = dao.getNoteById(noteId).map { it?.toDomain() }
    override fun getNotesForDate(startOfDay: Long, endOfDay: Long): Flow<List<Note>> = dao.getNotesForDate(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    override suspend fun insertNote(note: Note): Long = dao.insertNote(note.toEntity())
    override suspend fun updateNote(note: Note) = dao.updateNote(note.toEntity())
    override suspend fun deleteNoteById(noteId: Long) = dao.deleteNoteById(noteId)
    override suspend fun countNotesSince(sinceMillis: Long): Int = dao.countNotesSince(sinceMillis)
}

private fun NoteEntity.toDomain() = Note(id, title, transcription, audioFilePath, categoryId, scheduledDate, createdAt, updatedAt, durationMs, isPinned, isCompleted, location, noteTime)
private fun Note.toEntity() = NoteEntity(id, title, transcription, audioFilePath, categoryId, scheduledDate, createdAt, updatedAt, durationMs, isPinned, isCompleted, location, noteTime)
