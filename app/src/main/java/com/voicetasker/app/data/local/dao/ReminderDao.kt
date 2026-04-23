package com.voicetasker.app.data.local.dao

import androidx.room.*
import com.voicetasker.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE noteId = :noteId ORDER BY triggerAt ASC")
    fun getRemindersForNote(noteId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Long): ReminderEntity?

    @Query("UPDATE reminders SET isTriggered = 1 WHERE id = :reminderId")
    suspend fun markAsTriggered(reminderId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE noteId = :noteId")
    suspend fun deleteRemindersForNote(noteId: Long)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)
}
