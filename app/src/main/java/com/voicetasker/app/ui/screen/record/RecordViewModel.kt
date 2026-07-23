package com.voicetasker.app.ui.screen.record

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.data.billing.BillingManager
import com.voicetasker.app.data.recorder.SpeechTranscriberImpl
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.ai.NoteAiProcessor
import com.voicetasker.app.domain.ai.NoteAiResult
import com.voicetasker.app.domain.ai.toFallback
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
import java.time.LocalDate
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
    val errorMessage: String? = null,
    val authenticationRequired: Boolean = false,
    val isPremium: Boolean = false,
    val maxDurationMs: Long = 60_000L // 1 min free, 10 min premium
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val speechTranscriber: SpeechTranscriberImpl,
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val feedbackManager: FeedbackManager,
    private val noteAiProcessor: NoteAiProcessor,
    private val billingManager: BillingManager
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
        viewModelScope.launch {
            billingManager.state.collect { billing ->
                val premium = billing.isPremium
                _uiState.update { it.copy(
                    isPremium = premium,
                    maxDurationMs = if (premium) 600_000L else 60_000L
                ) }
            }
        }
    }

    fun startRecording() {
        Log.d(TAG, "startRecording")
        _uiState.update {
            it.copy(
                isRecording = true, amplitudes = emptyList(), recordingDurationMs = 0,
                transcription = "", errorMessage = null, title = "", aiTitleSuggestion = null,
                location = "", noteTime = "", noteDate = "", isAiProcessing = false,
                authenticationRequired = false
            )
        }

        speechTranscriber.startListening()

        // Collect transcription state
        stateCollectorJob?.cancel()
        stateCollectorJob = viewModelScope.launch {
            speechTranscriber.state.collect { state ->
                when (state) {
                    is SpeechTranscriberImpl.TranscriptionState.Result -> {
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
        timerJob?.cancel()
        rmsJob?.cancel()
        _uiState.update { it.copy(isRecording = false) }

        val transcription = _uiState.value.transcription
        if (transcription.isNotBlank()) {
            viewModelScope.launch {
                processWithAi(transcription)
            }
        }
    }

    fun requestAiProcessing() {
        val transcription = _uiState.value.transcription
        if (transcription.isBlank() || _uiState.value.isAiProcessing) return
        viewModelScope.launch { processWithAi(transcription) }
    }

    private suspend fun processWithAi(rawTranscription: String) {
        if (_uiState.value.isAiProcessing) return
        _uiState.update {
            it.copy(
                isAiProcessing = true,
                errorMessage = null,
                authenticationRequired = false
            )
        }

        val categoryNames = _uiState.value.categories.map { it.name }
        val result = noteAiProcessor.process(
            text = rawTranscription,
            categoryNames = categoryNames,
            currentDate = LocalDate.now().toString()
        )
        val fallback = result.toFallback(rawTranscription)

        _uiState.update { state ->
            if (result !is NoteAiResult.Success) {
                return@update state.copy(
                    isAiProcessing = false,
                    errorMessage = fallback.message,
                    authenticationRequired = fallback.authenticationRequired
                )
            }

            val metadata = result.metadata
            var updated = state.copy(
                title = metadata.title.ifBlank { state.title },
                transcription = fallback.text.ifBlank { rawTranscription },
                aiTitleSuggestion = metadata.title.takeIf { it.isNotBlank() },
                location = metadata.location ?: "",
                noteTime = metadata.time ?: "",
                noteDate = metadata.date ?: "",
                isAiProcessing = false,
                errorMessage = null,
                authenticationRequired = false
            )
            if (metadata.date != null) {
                try {
                    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(metadata.date)
                    if (parsed != null) updated = updated.copy(scheduledDate = parsed.time)
                } catch (_: Exception) {}
            }
            if (metadata.category != null) {
                val categoryId = state.categories.find {
                    it.name.equals(metadata.category, ignoreCase = true)
                }?.id
                if (categoryId != null) updated = updated.copy(selectedCategoryId = categoryId)
            }
            updated
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
