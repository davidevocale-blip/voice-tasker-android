package com.voicetasker.app.ui.screen.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val freeNotesRemaining: Int = 5
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        noteRepository.getAllNotes(), categoryRepository.getAllCategories(), _searchQuery, _selectedCategoryId
    ) { notes, categories, query, categoryId ->
        val filtered = notes
            .filter { if (query.isBlank()) true else it.title.contains(query, true) || it.transcription.contains(query, true) }
            .filter { if (categoryId == null) true else it.categoryId == categoryId }
        HomeUiState(filtered, categories, query, categoryId, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onSearchQueryChanged(q: String) { _searchQuery.value = q }
    fun onCategoryFilterChanged(id: Long?) { _selectedCategoryId.value = if (_selectedCategoryId.value == id) null else id }
    fun deleteNote(id: Long) { viewModelScope.launch { noteRepository.deleteNoteById(id) } }

    fun getCategoryColor(catId: Long): Color {
        val hex = uiState.value.categories.find { it.id == catId }?.colorHex ?: "#6C63FF"
        return try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(0xFF6C63FF) }
    }
    fun getCategoryName(catId: Long): String = uiState.value.categories.find { it.id == catId }?.name ?: ""
}
