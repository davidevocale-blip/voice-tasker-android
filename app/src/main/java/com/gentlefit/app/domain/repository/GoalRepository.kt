package com.gentlefit.app.domain.repository

import com.gentlefit.app.domain.model.MicroGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getActiveGoals(): Flow<List<MicroGoal>>
    fun getCompletedGoals(): Flow<List<MicroGoal>>
    fun getSuggestedGoals(): Flow<List<MicroGoal>>
    suspend fun addGoal(goal: MicroGoal)
    suspend fun completeGoal(goalId: Long, completedDate: String)
    suspend fun updateStreak(goalId: Long, streakDays: Int)
    suspend fun deleteGoal(goalId: Long)
}
