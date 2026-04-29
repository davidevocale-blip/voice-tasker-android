package com.voicetasker.app.ui.screen.calendar

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CalendarUiState(
    val selectedDate: Long = System.currentTimeMillis(),
    val currentMonth: Calendar = Calendar.getInstance(),
    val notesForDate: List<Note> = emptyList(),
    val daysWithNotes: Set<Int> = emptySet(),
    val categories: List<Category> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    private var daysJob: Job? = null

    init {
        viewModelScope.launch { categoryRepository.getAllCategories().collect { cats -> _uiState.update { it.copy(categories = cats) } } }
        loadNotesForDate(System.currentTimeMillis())
        loadDaysWithNotes()
    }

    fun onDateSelected(millis: Long) { _uiState.update { it.copy(selectedDate = millis) }; loadNotesForDate(millis) }

    fun onMonthChanged(offset: Int) {
        val cal = _uiState.value.currentMonth.clone() as Calendar; cal.add(Calendar.MONTH, offset)
        _uiState.update { it.copy(currentMonth = cal) }
        loadDaysWithNotes()
    }

    private fun loadNotesForDate(millis: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val start = cal.timeInMillis; cal.add(Calendar.DAY_OF_MONTH, 1); val end = cal.timeInMillis
        viewModelScope.launch { noteRepository.getNotesForDate(start, end).collect { notes -> _uiState.update { it.copy(notesForDate = notes) } } }
    }

    private fun loadDaysWithNotes() {
        daysJob?.cancel()
        val cm = _uiState.value.currentMonth
        val start = Calendar.getInstance().apply { set(cm.get(Calendar.YEAR), cm.get(Calendar.MONTH), 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
        val end = start.clone() as Calendar; end.add(Calendar.MONTH, 1)
        daysJob = viewModelScope.launch {
            noteRepository.getDaysWithNotesInMonth(start.timeInMillis, end.timeInMillis).collect { days ->
                _uiState.update { it.copy(daysWithNotes = days.toSet()) }
            }
        }
    }

    fun getCategoryColor(catId: Long): Color {
        val hex = uiState.value.categories.find { it.id == catId }?.colorHex ?: "#6C63FF"
        return try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(0xFF6C63FF) }
    }
    fun getCategoryName(catId: Long): String = uiState.value.categories.find { it.id == catId }?.name ?: ""
}
