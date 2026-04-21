package com.gentlefit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gentlefit.app.data.local.entity.CoachMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachMessageDao {

    @Query("SELECT * FROM coach_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<CoachMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CoachMessageEntity): Long

    @Query("DELETE FROM coach_messages")
    suspend fun clearMessages()
}
