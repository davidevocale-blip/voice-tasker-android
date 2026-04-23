package com.voicetasker.app.ui.screen.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.data.recorder.AudioRecorderImpl
import com.voicetasker.app.data.recorder.SpeechTranscriberImpl
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository
import com.voicetasker.app.util.FeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordUiState(
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0,
    val transcription: String = "",
    val title: String = "",
    val audioFilePath: String? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val scheduledDate: Long = System.currentTimeMillis(),
    val selectedReminders: Set<ReminderType> = emptySet(),
    val amplitudes: List<Int> = emptyList(),
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val audioRecorder: AudioRecorderImpl,
    private val speechTranscriber: SpeechTranscriberImpl,
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val feedbackManager: FeedbackManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    init { viewModelScope.launch { categoryRepository.getAllCategories().collect { cats -> _uiState.update { it.copy(categories = cats) } } } }

    fun startRecording() {
        val path = audioRecorder.startRecording()
        if (path != null) {
            _uiState.update { it.copy(isRecording = true, audioFilePath = path, amplitudes = emptyList(), recordingDurationMs = 0) }
            viewModelScope.launch { while (_uiState.value.isRecording) { val amp = audioRecorder.getMaxAmplitude(); _uiState.update { it.copy(amplitudes = it.amplitudes + amp, recordingDurationMs = it.recordingDurationMs + 100) }; delay(100) } }
        }
        speechTranscriber.startListening()
        viewModelScope.launch {
            speechTranscriber.state.collect { state ->
                when (state) {
                    is SpeechTranscriberImpl.TranscriptionState.Result -> _uiState.update { it.copy(transcription = state.text) }
                    is SpeechTranscriberImpl.TranscriptionState.PartialResult -> _uiState.update { it.copy(transcription = state.text) }
                    is SpeechTranscriberImpl.TranscriptionState.Error -> _uiState.update { it.copy(errorMessage = state.message) }
                    else -> {}
                }
            }
        }
    }

    fun stopRecording() {
        val (path, duration) = audioRecorder.stopRecording()
        speechTranscriber.stopListening()
        _uiState.update { it.copy(isRecording = false, recordingDurationMs = duration, audioFilePath = path) }
    }

    fun onTitleChanged(t: String) { _uiState.update { it.copy(title = t) } }
    fun onTranscriptionChanged(t: String) { _uiState.update { it.copy(transcription = t) } }
    fun onCategorySelected(id: Long) { _uiState.update { it.copy(selectedCategoryId = id) } }
    fun onScheduledDateChanged(d: Long) { _uiState.update { it.copy(scheduledDate = d) } }
    fun onReminderToggled(type: ReminderType) { _uiState.update { s -> val u = s.selectedReminders.toMutableSet(); if (type in u) u.remove(type) else u.add(type); s.copy(selectedReminders = u) } }

    fun saveNote() {
        viewModelScope.launch {
            val s = _uiState.value; val now = System.currentTimeMillis()
            val noteId = noteRepository.insertNote(Note(title = s.title.ifBlank { "Nota vocale" }, transcription = s.transcription, audioFilePath = s.audioFilePath ?: "", categoryId = s.selectedCategoryId ?: 1, scheduledDate = s.scheduledDate, createdAt = now, updatedAt = now, durationMs = s.recordingDurationMs))
            s.selectedReminders.forEach { type -> reminderRepository.scheduleReminder(noteId, s.scheduledDate, type) }
            feedbackManager.play(FeedbackManager.FeedbackType.SAVE)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    override fun onCleared() { super.onCleared(); speechTranscriber.destroy() }
}
