package com.voicetasker.app.data.local.dao

import androidx.room.*
import com.voicetasker.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, scheduledDate ASC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteByIdOnce(noteId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE scheduledDate >= :startOfDay AND scheduledDate < :endOfDay ORDER BY scheduledDate ASC")
    fun getNotesForDate(startOfDay: Long, endOfDay: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE categoryId = :categoryId ORDER BY scheduledDate ASC")
    fun getNotesByCategory(categoryId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR transcription LIKE '%' || :query || '%' ORDER BY scheduledDate DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE createdAt >= :sinceMillis")
    suspend fun countNotesSince(sinceMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)
}
