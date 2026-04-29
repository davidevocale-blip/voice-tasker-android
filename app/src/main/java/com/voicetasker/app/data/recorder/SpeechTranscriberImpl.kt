package com.voicetasker.app.data.recorder

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechTranscriberImpl @Inject constructor(@ApplicationContext private val context: Context) {
    sealed class TranscriptionState {
        data object Idle : TranscriptionState()
        data object Listening : TranscriptionState()
        data class PartialResult(val text: String) : TranscriptionState()
        data class Result(val text: String) : TranscriptionState()
        data class Error(val message: String) : TranscriptionState()
        data object SilenceTimeout : TranscriptionState()
    }

    private val _state = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val state: StateFlow<TranscriptionState> = _state

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var accumulatedText = StringBuilder()

    // Silence detection — 3 seconds
    private val SILENCE_TIMEOUT_MS = 3000L
    private val SILENCE_RMS_THRESHOLD = 1.5f
    @Volatile private var lastVoiceActivityTime = 0L
    @Volatile private var hasReceivedAnyText = false
    private var silenceWatchdog: Runnable? = null

    // Mute beeps
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var savedVolume = -1

    private fun createIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        // Tell recognizer to wait longer before auto-stopping
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 15000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 15000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60000L)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = TranscriptionState.Error("Riconoscimento vocale non disponibile")
            return
        }
        isListening = true
        hasReceivedAnyText = false
        accumulatedText.clear()
        lastVoiceActivityTime = System.currentTimeMillis()
        _state.value = TranscriptionState.Idle
        muteBeep()
        mainHandler.post { startRecognizerInternal() }
        startSilenceWatchdog()
    }

    private fun startRecognizerInternal() {
        if (!isListening) return
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = TranscriptionState.Listening
                    mainHandler.postDelayed({ unmuteBeep() }, 200)
                }

                override fun onBeginningOfSpeech() {
                    lastVoiceActivityTime = System.currentTimeMillis()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val level = rmsdB.coerceIn(0f, 10f)
                    _rmsLevel.value = level
                    if (level > SILENCE_RMS_THRESHOLD) {
                        lastVoiceActivityTime = System.currentTimeMillis()
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    _rmsLevel.value = 0f
                    // Check silence timeout on every error
                    if (shouldStopForSilence()) {
                        triggerSilenceStop()
                        return
                    }
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        SpeechRecognizer.ERROR_CLIENT -> {
                            if (isListening) { muteBeep(); mainHandler.postDelayed({ startRecognizerInternal() }, 150) }
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            _state.value = TranscriptionState.Error("Permessi microfono non concessi")
                        }
                        else -> {
                            if (isListening) { muteBeep(); mainHandler.postDelayed({ startRecognizerInternal() }, 500) }
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        if (accumulatedText.isNotEmpty()) accumulatedText.append(". ")
                        accumulatedText.append(text)
                        hasReceivedAnyText = true
                        lastVoiceActivityTime = System.currentTimeMillis()
                        _state.value = TranscriptionState.Result(accumulatedText.toString())
                    }
                    // Continue listening
                    if (isListening) { muteBeep(); mainHandler.postDelayed({ startRecognizerInternal() }, 150) }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        hasReceivedAnyText = true
                        lastVoiceActivityTime = System.currentTimeMillis()
                        val fullText = if (accumulatedText.isNotEmpty()) "${accumulatedText}. $text" else text
                        _state.value = TranscriptionState.PartialResult(fullText)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer?.startListening(createIntent())
        } catch (e: Exception) {
            if (isListening) mainHandler.postDelayed({ startRecognizerInternal() }, 500)
        }
    }

    private fun shouldStopForSilence(): Boolean {
        return hasReceivedAnyText && (System.currentTimeMillis() - lastVoiceActivityTime > SILENCE_TIMEOUT_MS)
    }

    private fun triggerSilenceStop() {
        isListening = false
        _rmsLevel.value = 0f
        stopSilenceWatchdog()
        unmuteBeep()
        mainHandler.post { try { recognizer?.stopListening() } catch (_: Exception) {} }
        _state.value = TranscriptionState.SilenceTimeout
    }

    /**
     * Independent watchdog that fires every 500ms to check for silence,
     * regardless of SpeechRecognizer state.
     */
    private fun startSilenceWatchdog() {
        stopSilenceWatchdog()
        silenceWatchdog = object : Runnable {
            override fun run() {
                if (!isListening) return
                if (shouldStopForSilence()) {
                    triggerSilenceStop()
                    return
                }
                mainHandler.postDelayed(this, 400)
            }
        }
        mainHandler.postDelayed(silenceWatchdog!!, 1500) // Give 1.5s grace period before first check
    }

    private fun stopSilenceWatchdog() {
        silenceWatchdog?.let { mainHandler.removeCallbacks(it) }
        silenceWatchdog = null
    }

    private fun muteBeep() {
        try {
            if (savedVolume < 0) savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } catch (_: Exception) {}
    }

    private fun unmuteBeep() {
        try {
            if (savedVolume >= 0) { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0); savedVolume = -1 }
        } catch (_: Exception) {}
    }

    fun stopListening() {
        isListening = false
        _rmsLevel.value = 0f
        stopSilenceWatchdog()
        unmuteBeep()
        mainHandler.post { try { recognizer?.stopListening() } catch (_: Exception) {} }
    }

    fun destroy() {
        isListening = false
        _rmsLevel.value = 0f
        stopSilenceWatchdog()
        unmuteBeep()
        mainHandler.post { try { recognizer?.destroy() } catch (_: Exception) {}; recognizer = null }
    }
}
