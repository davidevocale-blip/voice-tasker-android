package com.gentlefit.app.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlefit.app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage = _currentPage.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    private val _userGoal = MutableStateFlow("")
    val userGoal = _userGoal.asStateFlow()

    fun nextPage() { _currentPage.value = (_currentPage.value + 1).coerceAtMost(2) }
    fun previousPage() { _currentPage.value = (_currentPage.value - 1).coerceAtLeast(0) }
    fun updateName(name: String) { _userName.value = name }
    fun updateGoal(goal: String) { _userGoal.value = goal }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferences.setUserName(_userName.value)
            userPreferences.setUserGoal(_userGoal.value)
            userPreferences.setOnboardingCompleted()
            onComplete()
        }
    }
}
