package com.gentlefit.app.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlefit.app.data.preferences.UserPreferences
import com.gentlefit.app.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "",
    val userGoal: String = "",
    val showWeight: Boolean = false,
    val darkMode: Boolean = false,
    val notifications: Boolean = true,
    val completedDays: Int = 0,
    val streakDays: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch { userPreferences.userName.collect { n -> _uiState.update { it.copy(userName = n) } } }
        viewModelScope.launch { userPreferences.userGoal.collect { g -> _uiState.update { it.copy(userGoal = g) } } }
        viewModelScope.launch { userPreferences.showWeight.collect { s -> _uiState.update { it.copy(showWeight = s) } } }
        viewModelScope.launch { userPreferences.darkMode.collect { d -> _uiState.update { it.copy(darkMode = d) } } }
        viewModelScope.launch { userPreferences.notificationsEnabled.collect { n -> _uiState.update { it.copy(notifications = n) } } }
        viewModelScope.launch { routineRepository.getCompletedDaysCount().collect { c -> _uiState.update { it.copy(completedDays = c) } } }
        viewModelScope.launch { routineRepository.getCurrentStreak().collect { s -> _uiState.update { it.copy(streakDays = s) } } }
    }

    fun toggleWeight(show: Boolean) { viewModelScope.launch { userPreferences.setShowWeight(show) } }
    fun toggleDarkMode(enabled: Boolean) { viewModelScope.launch { userPreferences.setDarkMode(enabled) } }
    fun toggleNotifications(enabled: Boolean) { viewModelScope.launch { userPreferences.setNotificationsEnabled(enabled) } }
    fun updateName(name: String) { viewModelScope.launch { userPreferences.setUserName(name) } }
}
