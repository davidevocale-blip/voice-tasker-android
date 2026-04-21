package com.gentlefit.app.domain.repository

import com.gentlefit.app.domain.model.DailyRoutine
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun getTodayRoutine(): Flow<DailyRoutine?>
    fun getRoutineByDate(date: String): Flow<DailyRoutine?>
    suspend fun completeExercise(routineId: Long)
    suspend fun completeFoodTip(routineId: Long)
    suspend fun completeGoal(routineId: Long)
    suspend fun insertRoutine(routine: DailyRoutine)
    fun getCompletedDaysCount(): Flow<Int>
    fun getCurrentStreak(): Flow<Int>
}
