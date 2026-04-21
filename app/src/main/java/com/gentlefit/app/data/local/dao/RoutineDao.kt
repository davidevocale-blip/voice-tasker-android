package com.gentlefit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gentlefit.app.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines WHERE date = :date LIMIT 1")
    fun getRoutineByDate(date: String): Flow<RoutineEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Query("UPDATE routines SET isExerciseCompleted = 1 WHERE id = :id")
    suspend fun completeExercise(id: Long)

    @Query("UPDATE routines SET isFoodTipFollowed = 1 WHERE id = :id")
    suspend fun completeFoodTip(id: Long)

    @Query("UPDATE routines SET isGoalCompleted = 1 WHERE id = :id")
    suspend fun completeGoal(id: Long)

    @Query("SELECT COUNT(*) FROM routines WHERE isExerciseCompleted = 1 AND isFoodTipFollowed = 1 AND isGoalCompleted = 1")
    fun getCompletedDaysCount(): Flow<Int>

    @Query("SELECT * FROM routines WHERE isExerciseCompleted = 1 OR isFoodTipFollowed = 1 OR isGoalCompleted = 1 ORDER BY date DESC")
    fun getActiveRoutines(): Flow<List<RoutineEntity>>
}
