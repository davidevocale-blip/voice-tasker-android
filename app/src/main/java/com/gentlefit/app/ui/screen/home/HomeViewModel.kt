package com.gentlefit.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlefit.app.data.ContentSeeder
import com.gentlefit.app.data.preferences.UserPreferences
import com.gentlefit.app.domain.model.DailyRoutine
import com.gentlefit.app.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val routine: DailyRoutine? = null,
    val streakDays: Int = 0,
    val completedDays: Int = 0,
    val quote: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            userPreferences.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        viewModelScope.launch {
            routineRepository.getTodayRoutine().collect { routine ->
                val dayOfYear = LocalDate.now().dayOfYear
                _uiState.update {
                    it.copy(
                        routine = routine,
                        quote = ContentSeeder.getMotivationalQuote(dayOfYear),
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            routineRepository.getCurrentStreak().collect { streak ->
                _uiState.update { it.copy(streakDays = streak) }
            }
        }
        viewModelScope.launch {
            routineRepository.getCompletedDaysCount().collect { count ->
                _uiState.update { it.copy(completedDays = count) }
            }
        }
    }

    fun completeExercise() {
        val id = _uiState.value.routine?.id ?: return
        viewModelScope.launch { routineRepository.completeExercise(id) }
    }

    fun completeFoodTip() {
        val id = _uiState.value.routine?.id ?: return
        viewModelScope.launch { routineRepository.completeFoodTip(id) }
    }

    fun completeGoal() {
        val id = _uiState.value.routine?.id ?: return
        viewModelScope.launch { routineRepository.completeGoal(id) }
    }
}
