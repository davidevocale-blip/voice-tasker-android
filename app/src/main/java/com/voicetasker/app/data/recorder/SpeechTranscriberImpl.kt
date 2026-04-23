package com.voicetasker.app.data.recorder

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = TranscriptionState.Error("Riconoscimento vocale non disponibile")
            return
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _state.value = TranscriptionState.Listening }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { _state.value = TranscriptionState.Error("Errore trascrizione: $error") }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                _state.value = TranscriptionState.Result(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (text.isNotBlank()) _state.value = TranscriptionState.PartialResult(text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer?.startListening(intent)
    }

    fun stopListening() { try { recognizer?.stopListening() } catch (_: Exception) {} }
    fun destroy() { try { recognizer?.destroy() } catch (_: Exception) {}; recognizer = null }
}
