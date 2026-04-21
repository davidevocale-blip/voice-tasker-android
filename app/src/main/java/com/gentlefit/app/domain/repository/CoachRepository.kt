package com.gentlefit.app.domain.repository

import com.gentlefit.app.domain.model.CoachMessage
import kotlinx.coroutines.flow.Flow

interface CoachRepository {
    fun getMessages(): Flow<List<CoachMessage>>
    suspend fun addMessage(message: CoachMessage)
    fun getGreeting(userName: String, hour: Int, streakDays: Int): CoachMessage
    fun getCelebration(achievement: String): CoachMessage
    fun getMotivation(): CoachMessage
    fun getQuickReplyResponse(reply: String): CoachMessage
}
