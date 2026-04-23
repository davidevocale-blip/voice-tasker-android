package com.voicetasker.app.ui.screen.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.data.ai.GeminiService
import com.voicetasker.app.data.recorder.SpeechTranscriberImpl
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository
import com.voicetasker.app.util.FeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val scheduledDate: Long = System.currentTimeMillis(),
    val selectedReminders: Set<ReminderType> = emptySet(),
    val amplitudes: List<Float> = emptyList(),
    val isSaved: Boolean = false,
    val isAiProcessing: Boolean = false,
    val aiTitleSuggestion: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val speechTranscriber: SpeechTranscriberImpl,
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val feedbackManager: FeedbackManager,
    private val geminiService: GeminiService
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
    private var timerJob: Job? = null
    private var rmsJob: Job? = null

    init { viewModelScope.launch { categoryRepository.getAllCategories().collect { cats -> _uiState.update { it.copy(categories = cats) } } } }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true, amplitudes = emptyList(), recordingDurationMs = 0, transcription = "", errorMessage = null, title = "", aiTitleSuggestion = null) }

        // Start speech recognition (uses mic exclusively - no MediaRecorder conflict)
        speechTranscriber.startListening()

        // Collect transcription results
        viewModelScope.launch {
            speechTranscriber.state.collect { state ->
                when (state) {
                    is SpeechTranscriberImpl.TranscriptionState.Result -> _uiState.update { it.copy(transcription = state.text) }
                    is SpeechTranscriberImpl.TranscriptionState.PartialResult -> _uiState.update { it.copy(transcription = state.text) }
                    is SpeechTranscriberImpl.TranscriptionState.Error -> {
                        if (state.message.contains("Permessi") || state.message.contains("disponibile")) {
                            _uiState.update { it.copy(errorMessage = state.message) }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Collect RMS levels for waveform animation
        rmsJob = viewModelScope.launch {
            speechTranscriber.rmsLevel.collect { rms ->
                if (_uiState.value.isRecording) {
                    _uiState.update { it.copy(amplitudes = it.amplitudes + rms) }
                }
            }
        }

        // Timer
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                delay(100)
                _uiState.update { it.copy(recordingDurationMs = it.recordingDurationMs + 100) }
            }
        }
    }

    fun stopRecording() {
        speechTranscriber.stopListening()
        timerJob?.cancel()
        rmsJob?.cancel()
        _uiState.update { it.copy(isRecording = false) }
        // Launch AI processing
        if (geminiService.isAvailable && _uiState.value.transcription.isNotBlank()) {
            viewModelScope.launch { processWithAi() }
        }
    }

    private suspend fun processWithAi() {
        _uiState.update { it.copy(isAiProcessing = true) }
        val rawText = _uiState.value.transcription
        // 1. Improve transcription
        val improved = geminiService.improveTranscription(rawText)
        _uiState.update { it.copy(transcription = improved) }
        // 2. Generate title suggestion
        val title = geminiService.generateTitle(improved)
        _uiState.update { it.copy(aiTitleSuggestion = title, title = title) }
        // 3. Suggest category
        val catNames = _uiState.value.categories.map { it.name }
        val suggested = geminiService.suggestCategory(improved, catNames)
        if (suggested != null) {
            val catId = _uiState.value.categories.find { it.name.equals(suggested, ignoreCase = true) }?.id
            if (catId != null) _uiState.update { it.copy(selectedCategoryId = catId) }
        }
        _uiState.update { it.copy(isAiProcessing = false) }
    }

    fun onTitleChanged(t: String) { _uiState.update { it.copy(title = t) } }
    fun onTranscriptionChanged(t: String) { _uiState.update { it.copy(transcription = t) } }
    fun onCategorySelected(id: Long) { _uiState.update { it.copy(selectedCategoryId = id) } }
    fun onScheduledDateChanged(d: Long) { _uiState.update { it.copy(scheduledDate = d) } }
    fun onReminderToggled(type: ReminderType) { _uiState.update { s -> val u = s.selectedReminders.toMutableSet(); if (type in u) u.remove(type) else u.add(type); s.copy(selectedReminders = u) } }

    fun saveNote() {
        viewModelScope.launch {
            val s = _uiState.value; val now = System.currentTimeMillis()
            val noteId = noteRepository.insertNote(Note(title = s.title.ifBlank { "Nota vocale" }, transcription = s.transcription, audioFilePath = "", categoryId = s.selectedCategoryId ?: 1, scheduledDate = s.scheduledDate, createdAt = now, updatedAt = now, durationMs = s.recordingDurationMs))
            s.selectedReminders.forEach { type -> reminderRepository.scheduleReminder(noteId, s.scheduledDate, type) }
            feedbackManager.play(FeedbackManager.FeedbackType.SAVE)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    override fun onCleared() { super.onCleared(); speechTranscriber.destroy() }
}
