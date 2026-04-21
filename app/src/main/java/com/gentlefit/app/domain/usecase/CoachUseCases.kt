package com.gentlefit.app.domain.usecase

import com.gentlefit.app.domain.model.CoachMessage
import com.gentlefit.app.domain.repository.CoachRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCoachMessagesUseCase @Inject constructor(
    private val coachRepository: CoachRepository
) {
    operator fun invoke(): Flow<List<CoachMessage>> = coachRepository.getMessages()
}

class SendCoachReplyUseCase @Inject constructor(
    private val coachRepository: CoachRepository
) {
    suspend operator fun invoke(reply: String) {
        val userMessage = CoachMessage(text = reply, type = com.gentlefit.app.domain.model.MessageType.USER)
        coachRepository.addMessage(userMessage)
        val response = coachRepository.getQuickReplyResponse(reply)
        coachRepository.addMessage(response)
    }
}
