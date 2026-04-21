package com.gentlefit.app.ui.screen.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlefit.app.data.preferences.UserPreferences
import com.gentlefit.app.domain.model.Mood
import com.gentlefit.app.domain.model.ProgressEntry
import com.gentlefit.app.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ProgressUiState(
    val entries: List<ProgressEntry> = emptyList(),
    val todayEntry: ProgressEntry? = null,
    val selectedMood: Mood? = null,
    val energyLevel: Int = 3,
    val sleepQuality: Int = 3,
    val showWeight: Boolean = false,
    val weight: String = "",
    val isSaved: Boolean = false
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            progressRepository.getRecentProgress(7).collect { entries ->
                _uiState.update { it.copy(entries = entries) }
            }
        }
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            progressRepository.getProgressByDate(today).collect { entry ->
                _uiState.update { it.copy(todayEntry = entry, isSaved = entry != null) }
            }
        }
        viewModelScope.launch {
            userPreferences.showWeight.collect { show ->
                _uiState.update { it.copy(showWeight = show) }
            }
        }
    }

    fun selectMood(mood: Mood) { _uiState.update { it.copy(selectedMood = mood) } }
    fun setEnergy(level: Int) { _uiState.update { it.copy(energyLevel = level) } }
    fun setSleep(quality: Int) { _uiState.update { it.copy(sleepQuality = quality) } }
    fun setWeight(w: String) { _uiState.update { it.copy(weight = w) } }

    fun saveProgress() {
        val state = _uiState.value
        val mood = state.selectedMood ?: return
        viewModelScope.launch {
            val entry = ProgressEntry(
                date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                weight = state.weight.toFloatOrNull(),
                energyLevel = state.energyLevel,
                sleepQuality = state.sleepQuality,
                mood = mood
            )
            progressRepository.logProgress(entry)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
