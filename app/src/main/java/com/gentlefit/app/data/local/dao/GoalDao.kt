package com.gentlefit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gentlefit.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE isCompleted = 0 ORDER BY createdDate DESC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isCompleted = 1 ORDER BY completedDate DESC")
    fun getCompletedGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Query("UPDATE goals SET isCompleted = 1, completedDate = :completedDate WHERE id = :id")
    suspend fun completeGoal(id: Long, completedDate: String)

    @Query("UPDATE goals SET streakDays = :streakDays WHERE id = :id")
    suspend fun updateStreak(id: Long, streakDays: Int)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM goals ORDER BY createdDate DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>
}
