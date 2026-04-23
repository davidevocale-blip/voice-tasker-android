package com.voicetasker.app.ui.screen.notedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.model.Reminder
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository
import com.voicetasker.app.util.FeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailUiState(
    val note: Note? = null,
    val categories: List<Category> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editTranscription: String = "",
    val editCategoryId: Long? = null,
    val editLocation: String = "",
    val editNoteTime: String = "",
    val editScheduledDate: Long = 0,
    val isDeleted: Boolean = false
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val feedbackManager: FeedbackManager
) : ViewModel() {
    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { noteRepository.getNoteById(noteId).collect { n -> n?.let { _uiState.update { s -> s.copy(note = n) } } } }
        viewModelScope.launch { categoryRepository.getAllCategories().collect { cats -> _uiState.update { it.copy(categories = cats) } } }
        viewModelScope.launch { reminderRepository.getRemindersForNote(noteId).collect { rems -> _uiState.update { it.copy(reminders = rems) } } }
    }

    fun startEditing() {
        val n = _uiState.value.note ?: return
        _uiState.update { it.copy(
            isEditing = true, editTitle = n.title, editTranscription = n.transcription,
            editCategoryId = n.categoryId, editLocation = n.location, editNoteTime = n.noteTime,
            editScheduledDate = n.scheduledDate
        ) }
    }

    fun onEditTitleChanged(t: String) { _uiState.update { it.copy(editTitle = t) } }
    fun onEditTranscriptionChanged(t: String) { _uiState.update { it.copy(editTranscription = t) } }
    fun onEditCategoryChanged(id: Long) { _uiState.update { it.copy(editCategoryId = id) } }
    fun onEditLocationChanged(l: String) { _uiState.update { it.copy(editLocation = l) } }
    fun onEditTimeChanged(t: String) { _uiState.update { it.copy(editNoteTime = t) } }
    fun onEditDateChanged(d: Long) { _uiState.update { it.copy(editScheduledDate = d) } }
    fun cancelEditing() { _uiState.update { it.copy(isEditing = false) } }

    fun saveEdits() {
        val n = _uiState.value.note ?: return; val s = _uiState.value
        viewModelScope.launch {
            noteRepository.updateNote(n.copy(
                title = s.editTitle, transcription = s.editTranscription,
                categoryId = s.editCategoryId ?: n.categoryId,
                location = s.editLocation, noteTime = s.editNoteTime,
                scheduledDate = s.editScheduledDate,
                updatedAt = System.currentTimeMillis()
            ))
            feedbackManager.play(FeedbackManager.FeedbackType.EDIT)
            _uiState.update { it.copy(isEditing = false) }
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            noteRepository.deleteNoteById(noteId)
            feedbackManager.play(FeedbackManager.FeedbackType.DELETE)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun addReminder(type: ReminderType) { val n = _uiState.value.note ?: return; viewModelScope.launch { reminderRepository.scheduleReminder(noteId, n.scheduledDate, type) } }
    fun removeReminder(id: Long) { viewModelScope.launch { reminderRepository.cancelReminder(id) } }

    fun getCategoryName(catId: Long): String = _uiState.value.categories.find { it.id == catId }?.name ?: ""
    fun getCategoryColor(catId: Long): String = _uiState.value.categories.find { it.id == catId }?.colorHex ?: "#6C63FF"
}
