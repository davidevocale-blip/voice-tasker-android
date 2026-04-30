package com.voicetasker.app.ui.screen.record

import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.Locale
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
    val location: String = "",
    val noteTime: String = "",
    val noteDate: String = "",
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

    companion object {
        private const val TAG = "RecordViewModel"
    }

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
    private var timerJob: Job? = null
    private var rmsJob: Job? = null
    private var stateCollectorJob: Job? = null

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun startRecording() {
        Log.d(TAG, "startRecording")
        _uiState.update {
            it.copy(
                isRecording = true, amplitudes = emptyList(), recordingDurationMs = 0,
                transcription = "", errorMessage = null, title = "", aiTitleSuggestion = null,
                location = "", noteTime = "", noteDate = "", isAiProcessing = false
            )
        }

        speechTranscriber.startListening()

        // Collect transcription state
        stateCollectorJob?.cancel()
        stateCollectorJob = viewModelScope.launch {
            speechTranscriber.state.collect { state ->
                when (state) {
                    is SpeechTranscriberImpl.TranscriptionState.Result -> {
                        Log.d(TAG, "Got Result: ${state.text.take(50)}")
                        _uiState.update { it.copy(transcription = state.text) }
                    }
                    is SpeechTranscriberImpl.TranscriptionState.PartialResult -> {
                        _uiState.update { it.copy(transcription = state.text) }
                    }
                    is SpeechTranscriberImpl.TranscriptionState.SilenceTimeout -> {
                        Log.d(TAG, "SilenceTimeout received, isRecording=${_uiState.value.isRecording}")
                        if (_uiState.value.isRecording) {
                            performStop()
                        }
                    }
                    is SpeechTranscriberImpl.TranscriptionState.Error -> {
                        Log.e(TAG, "Error: ${state.message}")
                        _uiState.update { it.copy(errorMessage = state.message) }
                    }
                    else -> {}
                }
            }
        }

        // Collect RMS for waveform
        rmsJob?.cancel()
        rmsJob = viewModelScope.launch {
            speechTranscriber.rmsLevel.collect { rms ->
                if (_uiState.value.isRecording) {
                    _uiState.update { it.copy(amplitudes = it.amplitudes + rms) }
                }
            }
        }

        // Timer
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                if (!_uiState.value.isRecording) break
                _uiState.update { it.copy(recordingDurationMs = it.recordingDurationMs + 100) }
            }
        }
    }

    fun stopRecording() {
        Log.d(TAG, "stopRecording (manual)")
        speechTranscriber.stopListening()
        performStop()
    }

    private fun performStop() {
        if (!_uiState.value.isRecording && !_uiState.value.isAiProcessing) return
        Log.d(TAG, "performStop, transcription length=${_uiState.value.transcription.length}")

        timerJob?.cancel()
        rmsJob?.cancel()
        _uiState.update { it.copy(isRecording = false) }

        val transcription = _uiState.value.transcription
        Log.d(TAG, "Gemini available=${geminiService.isAvailable}, text='${transcription.take(80)}'")

        if (geminiService.isAvailable && transcription.isNotBlank()) {
            viewModelScope.launch {
                processWithAi(transcription)
            }
        }
    }

    private suspend fun processWithAi(rawTranscription: String) {
        Log.d(TAG, "processWithAi START")
        _uiState.update { it.copy(isAiProcessing = true) }

        try {
            val catNames = _uiState.value.categories.map { it.name }
            Log.d(TAG, "Calling extractNoteMetadata with categories=$catNames")
            val metadata = geminiService.extractNoteMetadata(rawTranscription, catNames)
            Log.d(TAG, "AI result: title='${metadata.title}', date=${metadata.date}, time=${metadata.time}, location=${metadata.location}, category=${metadata.category}")

            _uiState.update { s ->
                var updated = s.copy(
                    title = metadata.title.ifBlank { s.title },
                    transcription = metadata.improvedText.ifBlank { rawTranscription },
                    aiTitleSuggestion = metadata.title.takeIf { it.isNotBlank() },
                    location = metadata.location ?: "",
                    noteTime = metadata.time ?: "",
                    noteDate = metadata.date ?: "",
                    isAiProcessing = false
                )
                // Set scheduled date
                if (metadata.date != null) {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsed = sdf.parse(metadata.date)
                        if (parsed != null) updated = updated.copy(scheduledDate = parsed.time)
                    } catch (_: Exception) {}
                }
                // Set category
                if (metadata.category != null) {
                    val catId = s.categories.find { it.name.equals(metadata.category, ignoreCase = true) }?.id
                    if (catId != null) {
                        Log.d(TAG, "Setting category to ${metadata.category} (id=$catId)")
                        updated = updated.copy(selectedCategoryId = catId)
                    }
                }
                updated
            }
            Log.d(TAG, "processWithAi DONE")
        } catch (e: Exception) {
            Log.e(TAG, "processWithAi FAILED", e)
            _uiState.update { it.copy(isAiProcessing = false) }
        }
    }

    fun onTitleChanged(t: String) { _uiState.update { it.copy(title = t) } }
    fun onTranscriptionChanged(t: String) { _uiState.update { it.copy(transcription = t) } }
    fun onCategorySelected(id: Long) { _uiState.update { it.copy(selectedCategoryId = id) } }
    fun onScheduledDateChanged(d: Long) { _uiState.update { it.copy(scheduledDate = d) } }
    fun onLocationChanged(l: String) { _uiState.update { it.copy(location = l) } }
    fun onTimeChanged(t: String) { _uiState.update { it.copy(noteTime = t) } }
    fun onReminderToggled(type: ReminderType) {
        _uiState.update { s ->
            val u = s.selectedReminders.toMutableSet()
            if (type in u) u.remove(type) else u.add(type)
            s.copy(selectedReminders = u)
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            val s = _uiState.value
            val now = System.currentTimeMillis()
            val noteId = noteRepository.insertNote(
                Note(
                    title = s.title.ifBlank { "Nota vocale" },
                    transcription = s.transcription,
                    audioFilePath = "",
                    categoryId = s.selectedCategoryId ?: 1,
                    scheduledDate = s.scheduledDate,
                    createdAt = now, updatedAt = now,
                    durationMs = s.recordingDurationMs,
                    location = s.location,
                    noteTime = s.noteTime
                )
            )
            s.selectedReminders.forEach { type ->
                reminderRepository.scheduleReminder(noteId, s.scheduledDate, type)
            }
            feedbackManager.play(FeedbackManager.FeedbackType.SAVE)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechTranscriber.destroy()
    }
}
