package com.voicetasker.app.ui.screen.record

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.data.billing.BillingManager
import com.voicetasker.app.R
import com.voicetasker.app.data.recorder.SpeechTranscriberImpl
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.reminder.ReminderDateNormalizer
import com.voicetasker.app.domain.ai.NoteAiProcessor
import com.voicetasker.app.domain.ai.NoteAiOperationExecution
import com.voicetasker.app.domain.ai.NoteAiOperationIntent
import com.voicetasker.app.domain.ai.NoteAiOperationPayload
import com.voicetasker.app.domain.ai.NoteAiOperationSession
import com.voicetasker.app.domain.ai.NoteAiResult
import com.voicetasker.app.domain.ai.toFallback
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository
import com.voicetasker.app.domain.repository.ReminderScheduleResult
import com.voicetasker.app.util.FeedbackManager
import com.voicetasker.app.ui.resources.StringResolver
import com.voicetasker.app.ui.resources.UiText
import com.voicetasker.app.ui.resources.messageRes
import com.voicetasker.app.ui.resources.toCompletedNoteSaveUiResult
import com.voicetasker.app.ui.resources.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class RecordUiState(
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0,
    val transcription: String = "",
    val title: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val scheduledDate: Long = System.currentTimeMillis(),
    val isDateSelected: Boolean = false,
    val selectedReminders: Set<ReminderType> = emptySet(),
    val amplitudes: List<Float> = emptyList(),
    val isSaved: Boolean = false,
    val isAiProcessing: Boolean = false,
    val aiTitleSuggestion: String? = null,
    val location: String = "",
    val noteTime: String = "",
    val noteDate: String = "",
    val errorMessage: UiText? = null,
    val reminderFailureRes: Int? = null,
    val isSaving: Boolean = false,
    val authenticationRequired: Boolean = false,
    val isPremium: Boolean = false,
    val maxDurationMs: Long = 60_000L // 1 min free, 10 min premium
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val stringResolver: StringResolver,
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
    private val aiOperationSession = NoteAiOperationSession()

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
        aiOperationSession.invalidate()
        _uiState.update {
            it.copy(
                isRecording = true, amplitudes = emptyList(), recordingDurationMs = 0,
                transcription = "", errorMessage = null, title = "", aiTitleSuggestion = null,
                location = "", noteTime = "", noteDate = "", isAiProcessing = false,
                authenticationRequired = false, isDateSelected = false,
                scheduledDate = System.currentTimeMillis(), reminderFailureRes = null
            )
        }

        speechTranscriber.startListening()

        // Collect transcription state
        stateCollectorJob?.cancel()
        stateCollectorJob = viewModelScope.launch {
            speechTranscriber.state.collect { state ->
                when (state) {
                    is SpeechTranscriberImpl.TranscriptionState.Result -> {
                        onTranscriptionChanged(state.text)
                    }
                    is SpeechTranscriberImpl.TranscriptionState.PartialResult -> {
                        onTranscriptionChanged(state.text)
                    }
                    is SpeechTranscriberImpl.TranscriptionState.SilenceTimeout -> {
                        Log.d(TAG, "SilenceTimeout received, isRecording=${_uiState.value.isRecording}")
                        if (_uiState.value.isRecording) {
                            performStop()
                        }
                    }
                    is SpeechTranscriberImpl.TranscriptionState.Error -> {
                        Log.e(TAG, "Error: ${state.error}")
                        _uiState.update {
                            it.copy(errorMessage = state.error.toUiText())
                        }
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
                processWithAi(transcription, deferIfBusy = true)
            }
        }
    }

    fun requestAiProcessing() {
        val transcription = _uiState.value.transcription
        if (transcription.isBlank() || _uiState.value.isAiProcessing) return
        viewModelScope.launch { processWithAi(transcription) }
    }

    private suspend fun processWithAi(
        rawTranscription: String,
        deferIfBusy: Boolean = false
    ) {
        val state = _uiState.value
        val intent = aiIntent(state, rawTranscription)
        val payload = aiPayload(state)
        val execution = aiOperationSession.begin(
            intent = intent,
            payload = payload
        )
        if (execution == null) {
            if (deferIfBusy) {
                aiOperationSession.deferLatest(intent, payload)
                _uiState.update {
                    it.copy(
                        isAiProcessing = true,
                        errorMessage = null,
                        authenticationRequired = false
                    )
                }
            }
            return
        }
        _uiState.update {
            it.copy(
                isAiProcessing = true,
                errorMessage = null,
                authenticationRequired = false
            )
        }
        processAiExecution(execution, rawTranscription)
    }

    private suspend fun processAiExecution(
        execution: NoteAiOperationExecution,
        originalText: String
    ) {
        val result = noteAiProcessor.process(execution.operation)
        val completion = aiOperationSession.completeAndBeginDeferred(execution, result)
        if (!completion.isCurrent) {
            val deferredExecution = completion.deferredExecution
            if (deferredExecution != null) {
                processAiExecution(
                    execution = deferredExecution,
                    originalText = deferredExecution.operation.text
                )
            } else {
                _uiState.update { it.copy(isAiProcessing = false) }
            }
            return
        }
        val fallback = result.toFallback(originalText)

        _uiState.update { state ->
            if (result !is NoteAiResult.Success) {
                return@update state.copy(
                    isAiProcessing = false,
                    errorMessage = fallback.failureReason?.let { UiText.Resource(it.messageRes()) },
                    authenticationRequired = fallback.authenticationRequired
                )
            }

            val metadata = result.metadata
            var updated = state.copy(
                title = metadata.title.ifBlank { state.title },
                transcription = fallback.text.ifBlank { originalText },
                aiTitleSuggestion = metadata.title.takeIf { it.isNotBlank() },
                location = metadata.location ?: "",
                noteTime = metadata.time ?: "",
                noteDate = metadata.date ?: "",
                isAiProcessing = false,
                errorMessage = null,
                authenticationRequired = false
            )
            if (metadata.date != null) {
                ReminderDateNormalizer.fromIsoDate(metadata.date)?.let { canonicalDate ->
                    updated = updated.copy(
                        scheduledDate = canonicalDate,
                        isDateSelected = true
                    )
                }
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

    private fun aiIntent(
        state: RecordUiState,
        text: String = state.transcription
    ) = NoteAiOperationIntent(
        text = text,
        selectedCategoryId = state.selectedCategoryId,
        scheduledDate = state.scheduledDate
    )

    private fun aiPayload(state: RecordUiState) = NoteAiOperationPayload(
        categoryNames = state.categories.map { it.name },
        currentDate = LocalDate.now().toString()
    )

    private fun updateDeferredAiOperationIfPresent() {
        val state = _uiState.value
        aiOperationSession.updateDeferredIfPresent(
            intent = aiIntent(state),
            payload = aiPayload(state)
        )
    }

    private fun deferLatestAiOperationIfActive() {
        val state = _uiState.value
        aiOperationSession.deferLatestIfActive(
            intent = aiIntent(state),
            payload = aiPayload(state)
        )
    }

    fun onTitleChanged(t: String) { _uiState.update { it.copy(title = t) } }
    fun onTranscriptionChanged(t: String) {
        val changed = _uiState.value.transcription != t
        if (changed) {
            aiOperationSession.invalidate(clearDeferred = false)
        }
        _uiState.update { it.copy(transcription = t) }
        if (changed) deferLatestAiOperationIfActive()
    }
    fun onCategorySelected(id: Long) {
        if (_uiState.value.selectedCategoryId != id) {
            aiOperationSession.invalidate(clearDeferred = false)
        }
        _uiState.update { it.copy(selectedCategoryId = id) }
        updateDeferredAiOperationIfPresent()
    }
    fun onScheduledDateChanged(d: Long) {
        ReminderDateNormalizer.fromDatePickerMillis(d)?.let { canonicalDate ->
            if (_uiState.value.scheduledDate != canonicalDate) {
                aiOperationSession.invalidate(clearDeferred = false)
            }
            _uiState.update {
                it.copy(scheduledDate = canonicalDate, isDateSelected = true)
            }
            updateDeferredAiOperationIfPresent()
        }
    }
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
        if (_uiState.value.isSaving || _uiState.value.isSaved) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val s = _uiState.value
            val now = System.currentTimeMillis()
            val noteId = noteRepository.insertNote(
                Note(
                    title = s.title.ifBlank { stringResolver.resolve(R.string.voice_note) },
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
            val reminderResults = mutableListOf<ReminderScheduleResult>()
            s.selectedReminders.forEach { type ->
                reminderResults += reminderRepository.scheduleReminder(
                    noteId = noteId,
                    scheduledDate = s.scheduledDate.takeIf { s.isDateSelected },
                    noteTime = s.noteTime,
                    type = type
                )
            }
            val completion = reminderResults.toCompletedNoteSaveUiResult()
            feedbackManager.play(FeedbackManager.FeedbackType.SAVE)
            _uiState.update {
                it.copy(
                    isSaved = completion.isSaved,
                    isSaving = false,
                    reminderFailureRes = completion.reminderFailureRes
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechTranscriber.destroy()
    }
}
