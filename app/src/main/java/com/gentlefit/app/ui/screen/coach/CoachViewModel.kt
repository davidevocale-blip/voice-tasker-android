package com.gentlefit.app.ui.screen.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlefit.app.data.preferences.UserPreferences
import com.gentlefit.app.domain.model.CoachMessage
import com.gentlefit.app.domain.repository.CoachRepository
import com.gentlefit.app.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val coachRepository: CoachRepository,
    private val routineRepository: RoutineRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val messages: StateFlow<List<CoachMessage>> = coachRepository.getMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { sendInitialGreeting() }

    private fun sendInitialGreeting() {
        viewModelScope.launch {
            val name = userPreferences.userName.first()
            val streak = routineRepository.getCurrentStreak().first()
            val greeting = coachRepository.getGreeting(name, LocalTime.now().hour, streak)
            coachRepository.addMessage(greeting)
        }
    }

    fun sendQuickReply(reply: String) {
        viewModelScope.launch {
            val userMsg = CoachMessage(text = reply, type = com.gentlefit.app.domain.model.MessageType.USER)
            coachRepository.addMessage(userMsg)
            val response = coachRepository.getQuickReplyResponse(reply)
            coachRepository.addMessage(response)
        }
    }

    fun requestMotivation() {
        viewModelScope.launch {
            val msg = coachRepository.getMotivation()
            coachRepository.addMessage(msg)
        }
    }
}
