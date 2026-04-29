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

    // Silence detection
    private val SILENCE_THRESHOLD = 1.5f
    private val SILENCE_TIMEOUT_MS = 4000L
    private var lastSpeechTime = 0L
    private var hasSpeechStarted = false
    private var silenceCheckRunnable: Runnable? = null

    // Audio manager for muting beeps
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalStreamVolume = 0

    private fun createIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        // Extend silence timeouts to prevent premature stops
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000L)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = TranscriptionState.Error("Riconoscimento vocale non disponibile su questo dispositivo")
            return
        }
        isListening = true
        hasSpeechStarted = false
        accumulatedText.clear()
        lastSpeechTime = System.currentTimeMillis()
        _state.value = TranscriptionState.Idle
        muteBeep()
        mainHandler.post { startRecognizerInternal() }
    }

    private fun startRecognizerInternal() {
        if (!isListening) return
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = TranscriptionState.Listening
                    // Unmute after recognizer starts (beep already played or suppressed)
                    mainHandler.postDelayed({ unmuteBeep() }, 300)
                }
                override fun onBeginningOfSpeech() {
                    hasSpeechStarted = true
                    lastSpeechTime = System.currentTimeMillis()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val level = rmsdB.coerceIn(0f, 10f)
                    _rmsLevel.value = level
                    if (level > SILENCE_THRESHOLD) {
                        lastSpeechTime = System.currentTimeMillis()
                        hasSpeechStarted = true
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    _rmsLevel.value = 0f
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            // Check if we should stop due to silence
                            if (hasSpeechStarted && accumulatedText.isNotEmpty() &&
                                System.currentTimeMillis() - lastSpeechTime > SILENCE_TIMEOUT_MS) {
                                _state.value = TranscriptionState.SilenceTimeout
                                return
                            }
                            if (isListening) {
                                muteBeep()
                                mainHandler.postDelayed({ startRecognizerInternal() }, 200)
                            }
                        }
                        SpeechRecognizer.ERROR_CLIENT -> {
                            if (isListening) {
                                muteBeep()
                                mainHandler.postDelayed({ startRecognizerInternal() }, 300)
                            }
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            _state.value = TranscriptionState.Error("Permessi microfono non concessi")
                        }
                        SpeechRecognizer.ERROR_AUDIO -> {
                            if (isListening) {
                                muteBeep()
                                mainHandler.postDelayed({ startRecognizerInternal() }, 1000)
                            }
                        }
                        else -> {
                            if (isListening) {
                                muteBeep()
                                mainHandler.postDelayed({ startRecognizerInternal() }, 500)
                            }
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        if (accumulatedText.isNotEmpty()) accumulatedText.append(". ")
                        accumulatedText.append(text)
                        _state.value = TranscriptionState.Result(accumulatedText.toString())
                        lastSpeechTime = System.currentTimeMillis()
                    }
                    if (isListening) {
                        muteBeep()
                        mainHandler.postDelayed({ startRecognizerInternal() }, 200)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        val fullText = if (accumulatedText.isNotEmpty()) "${accumulatedText}. $text" else text
                        _state.value = TranscriptionState.PartialResult(fullText)
                        lastSpeechTime = System.currentTimeMillis()
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer?.startListening(createIntent())
            startSilenceCheck()
        } catch (e: Exception) {
            if (isListening) {
                muteBeep()
                mainHandler.postDelayed({ startRecognizerInternal() }, 500)
            }
        }
    }

    private fun startSilenceCheck() {
        silenceCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        silenceCheckRunnable = object : Runnable {
            override fun run() {
                if (!isListening) return
                if (hasSpeechStarted && accumulatedText.isNotEmpty() &&
                    System.currentTimeMillis() - lastSpeechTime > SILENCE_TIMEOUT_MS) {
                    _state.value = TranscriptionState.SilenceTimeout
                    return
                }
                mainHandler.postDelayed(this, 500)
            }
        }
        mainHandler.postDelayed(silenceCheckRunnable!!, 1000)
    }

    private fun muteBeep() {
        try {
            originalStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } catch (_: Exception) {}
    }

    private fun unmuteBeep() {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalStreamVolume, 0)
        } catch (_: Exception) {}
    }

    fun stopListening() {
        isListening = false
        _rmsLevel.value = 0f
        silenceCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        unmuteBeep()
        mainHandler.post {
            try { recognizer?.stopListening() } catch (_: Exception) {}
        }
    }

    fun destroy() {
        isListening = false
        _rmsLevel.value = 0f
        silenceCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        unmuteBeep()
        mainHandler.post {
            try { recognizer?.destroy() } catch (_: Exception) {}
            recognizer = null
        }
    }
}
