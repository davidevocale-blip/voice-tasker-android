package com.voicetasker.app.ui.screen.notedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.model.Reminder
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.reminder.ReminderDateNormalizer
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository
import com.voicetasker.app.domain.usecase.deleteNoteWithReminders
import com.voicetasker.app.domain.usecase.updateNoteWithReminders
import com.voicetasker.app.util.FeedbackManager
import com.voicetasker.app.ui.resources.failureMessageRes
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
    val isDeleted: Boolean = false,
    val isPremium: Boolean = false,
    val reminderFailureRes: Int? = null
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val feedbackManager: FeedbackManager,
    private val billingManager: com.voicetasker.app.data.billing.BillingManager
) : ViewModel() {
    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { noteRepository.getNoteById(noteId).collect { n -> n?.let { _uiState.update { s -> s.copy(note = n) } } } }
        viewModelScope.launch { categoryRepository.getAllCategories().collect { cats -> _uiState.update { it.copy(categories = cats) } } }
        viewModelScope.launch { reminderRepository.getRemindersForNote(noteId).collect { rems -> _uiState.update { it.copy(reminders = rems) } } }
        viewModelScope.launch { billingManager.state.collect { billing -> _uiState.update { it.copy(isPremium = billing.isPremium) } } }
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
    fun onEditDateChanged(d: Long) {
        ReminderDateNormalizer.fromDatePickerMillis(d)?.let { canonicalDate ->
            _uiState.update { it.copy(editScheduledDate = canonicalDate) }
        }
    }
    fun cancelEditing() { _uiState.update { it.copy(isEditing = false) } }

    fun saveEdits() {
        val n = _uiState.value.note ?: return; val s = _uiState.value
        viewModelScope.launch {
            val updatedNote = n.copy(
                title = s.editTitle, transcription = s.editTranscription,
                categoryId = s.editCategoryId ?: n.categoryId,
                location = s.editLocation, noteTime = s.editNoteTime,
                scheduledDate = s.editScheduledDate,
                updatedAt = System.currentTimeMillis()
            )
            val reminderResult = updateNoteWithReminders(
                original = n,
                updated = updatedNote,
                noteRepository = noteRepository,
                reminderRepository = reminderRepository
            )
            feedbackManager.play(FeedbackManager.FeedbackType.EDIT)
            _uiState.update {
                it.copy(
                    isEditing = false,
                    reminderFailureRes = reminderResult?.failureMessageRes()
                )
            }
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            deleteNoteWithReminders(
                noteId,
                reminderRepository,
                noteRepository
            )
            feedbackManager.play(FeedbackManager.FeedbackType.DELETE)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun addReminder(type: ReminderType) {
        val note = _uiState.value.note ?: return
        viewModelScope.launch {
            val result = reminderRepository.scheduleReminder(
                noteId = noteId,
                scheduledDate = note.scheduledDate,
                noteTime = note.noteTime,
                type = type
            )
            _uiState.update {
                it.copy(reminderFailureRes = result.failureMessageRes())
            }
        }
    }
    fun removeReminder(id: Long) { viewModelScope.launch { reminderRepository.cancelReminder(id) } }

    fun getCategory(catId: Long): Category? =
        _uiState.value.categories.find { it.id == catId }
    fun getCategoryColor(catId: Long): String = _uiState.value.categories.find { it.id == catId }?.colorHex ?: "#6C63FF"
}
