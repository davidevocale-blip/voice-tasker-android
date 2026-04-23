package com.voicetasker.app.data.recorder

import android.content.Context
import android.content.Intent
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
    }

    private val _state = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val state: StateFlow<TranscriptionState> = _state
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var accumulatedText = StringBuilder()

    private fun createIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = TranscriptionState.Error("Riconoscimento vocale non disponibile su questo dispositivo")
            return
        }
        isListening = true
        accumulatedText.clear()
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
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    // Common non-fatal errors: restart listening
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        SpeechRecognizer.ERROR_CLIENT -> {
                            // Restart after a brief delay
                            if (isListening) {
                                mainHandler.postDelayed({ startRecognizerInternal() }, 500)
                            }
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            _state.value = TranscriptionState.Error("Permessi microfono non concessi")
                        }
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            // Try offline, or just restart
                            if (isListening) {
                                mainHandler.postDelayed({ startRecognizerInternal() }, 1000)
                            }
                        }
                        SpeechRecognizer.ERROR_AUDIO -> {
                            // Microphone busy (e.g., MediaRecorder using it)
                            // Retry after longer delay
                            if (isListening) {
                                mainHandler.postDelayed({ startRecognizerInternal() }, 2000)
                            }
                        }
                        else -> {
                            if (isListening) {
                                mainHandler.postDelayed({ startRecognizerInternal() }, 1000)
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
                    }
                    // Continue listening for more speech
                    if (isListening) {
                        mainHandler.postDelayed({ startRecognizerInternal() }, 300)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        val fullText = if (accumulatedText.isNotEmpty()) {
                            "${accumulatedText}. $text"
                        } else {
                            text
                        }
                        _state.value = TranscriptionState.PartialResult(fullText)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer?.startListening(createIntent())
        } catch (e: Exception) {
            _state.value = TranscriptionState.Error("Errore avvio trascrizione: ${e.message}")
        }
    }

    fun stopListening() {
        isListening = false
        mainHandler.post {
            try { recognizer?.stopListening() } catch (_: Exception) {}
        }
    }

    fun destroy() {
        isListening = false
        mainHandler.post {
            try { recognizer?.destroy() } catch (_: Exception) {}
            recognizer = null
        }
    }
}
