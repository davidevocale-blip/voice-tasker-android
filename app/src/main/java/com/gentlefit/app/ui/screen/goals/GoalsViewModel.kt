package com.gentlefit.app.ui.screen.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlefit.app.domain.model.MicroGoal
import com.gentlefit.app.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    val activeGoals: StateFlow<List<MicroGoal>> = goalRepository.getActiveGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedGoals: StateFlow<List<MicroGoal>> = goalRepository.getCompletedGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestedGoals: StateFlow<List<MicroGoal>> = goalRepository.getSuggestedGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun completeGoal(goalId: Long) {
        viewModelScope.launch {
            goalRepository.completeGoal(goalId, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
    }

    fun addGoal(goal: MicroGoal) {
        viewModelScope.launch { goalRepository.addGoal(goal) }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch { goalRepository.deleteGoal(goalId) }
    }
}
