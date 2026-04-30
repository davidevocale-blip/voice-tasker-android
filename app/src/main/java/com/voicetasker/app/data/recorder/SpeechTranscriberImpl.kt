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
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechTranscriberImpl @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val TAG = "SpeechTranscriber"
        private const val SILENCE_TIMEOUT_MS = 3000L
    }

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

    @Volatile private var isListening = false
    private val accumulatedText = StringBuilder()

    // Silence: track last time we received ANY text (partial or final)
    @Volatile private var lastTextReceivedTime = 0L
    @Volatile private var hasReceivedAnyText = false
    private var silenceWatchdog: Runnable? = null

    // Track restart count to limit beeps
    private var restartCount = 0

    // Audio manager for muting
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun createIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        // Try to extend the recognizer's own silence timeout as much as possible
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 30000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 30000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60000L)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = TranscriptionState.Error("Riconoscimento vocale non disponibile")
            return
        }
        isListening = true
        hasReceivedAnyText = false
        restartCount = 0
        accumulatedText.clear()
        lastTextReceivedTime = System.currentTimeMillis()
        _state.value = TranscriptionState.Idle

        // Mute ALL streams to suppress the initial beep
        muteAllStreams()

        mainHandler.post { launchRecognizer() }
    }

    private fun launchRecognizer() {
        if (!isListening) return
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(createListener())
            recognizer?.startListening(createIntent())

            // Start silence watchdog (only on first launch)
            if (silenceWatchdog == null) {
                startSilenceWatchdog()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognizer", e)
            if (isListening) mainHandler.postDelayed({ launchRecognizer() }, 500)
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
            _state.value = TranscriptionState.Listening
            // Unmute after a short delay (beep has already been suppressed)
            mainHandler.postDelayed({ restoreAllStreams() }, 400)
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            _rmsLevel.value = rmsdB.coerceIn(0f, 10f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
        }

        override fun onError(error: Int) {
            Log.d(TAG, "onError: $error, hasText=$hasReceivedAnyText, accum=${accumulatedText.length}")
            _rmsLevel.value = 0f

            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    _state.value = TranscriptionState.Error("Permessi microfono non concessi")
                    return
                }
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // The recognizer stopped because it didn't hear anything
                    // If we already have text and enough silence has passed, stop
                    if (hasReceivedAnyText && System.currentTimeMillis() - lastTextReceivedTime >= SILENCE_TIMEOUT_MS) {
                        Log.d(TAG, "Silence timeout triggered from onError")
                        emitSilenceTimeout()
                        return
                    }
                }
            }

            // Restart if still listening
            if (isListening) {
                restartCount++
                // Mute before restart to suppress beep
                muteAllStreams()
                mainHandler.postDelayed({ launchRecognizer() }, 200)
            }
        }

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            Log.d(TAG, "onResults: '$text'")
            if (text.isNotBlank()) {
                if (accumulatedText.isNotEmpty()) accumulatedText.append(". ")
                accumulatedText.append(text)
                hasReceivedAnyText = true
                lastTextReceivedTime = System.currentTimeMillis()
                _state.value = TranscriptionState.Result(accumulatedText.toString())
            }
            // Restart to continue listening (mute beep)
            if (isListening) {
                restartCount++
                muteAllStreams()
                mainHandler.postDelayed({ launchRecognizer() }, 200)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                hasReceivedAnyText = true
                lastTextReceivedTime = System.currentTimeMillis()
                val fullText = if (accumulatedText.isNotEmpty()) "${accumulatedText}. $text" else text
                _state.value = TranscriptionState.PartialResult(fullText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // ── Silence Watchdog ──
    // Runs independently every 500ms. If text was received and 3s of silence passed, auto-stop.
    private fun startSilenceWatchdog() {
        stopSilenceWatchdog()
        silenceWatchdog = object : Runnable {
            override fun run() {
                if (!isListening) return
                if (hasReceivedAnyText && System.currentTimeMillis() - lastTextReceivedTime >= SILENCE_TIMEOUT_MS) {
                    Log.d(TAG, "Silence watchdog triggered")
                    emitSilenceTimeout()
                    return
                }
                mainHandler.postDelayed(this, 500)
            }
        }
        // First check after 2s (give time for first words)
        mainHandler.postDelayed(silenceWatchdog!!, 2000)
    }

    private fun stopSilenceWatchdog() {
        silenceWatchdog?.let { mainHandler.removeCallbacks(it) }
        silenceWatchdog = null
    }

    private fun emitSilenceTimeout() {
        Log.d(TAG, "emitSilenceTimeout - accumulated: ${accumulatedText}")
        isListening = false
        _rmsLevel.value = 0f
        stopSilenceWatchdog()
        restoreAllStreams()
        mainHandler.post {
            try { recognizer?.stopListening() } catch (_: Exception) {}
            try { recognizer?.destroy() } catch (_: Exception) {}
            recognizer = null
        }
        _state.value = TranscriptionState.SilenceTimeout
    }

    // ── Audio Muting ──
    // Mute notification and system streams to suppress recognizer beeps.
    // These are the streams Android uses for the beep.
    private var savedNotifVol = -1
    private var savedSystemVol = -1
    private var savedMusicVol = -1

    private fun muteAllStreams() {
        try {
            if (savedNotifVol < 0) savedNotifVol = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            if (savedSystemVol < 0) savedSystemVol = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
            if (savedMusicVol < 0) savedMusicVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } catch (_: Exception) {}
    }

    private fun restoreAllStreams() {
        try {
            if (savedNotifVol >= 0) { audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotifVol, 0); savedNotifVol = -1 }
            if (savedSystemVol >= 0) { audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, savedSystemVol, 0); savedSystemVol = -1 }
            if (savedMusicVol >= 0) { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedMusicVol, 0); savedMusicVol = -1 }
        } catch (_: Exception) {}
    }

    fun stopListening() {
        Log.d(TAG, "stopListening called, isListening=$isListening")
        isListening = false
        _rmsLevel.value = 0f
        stopSilenceWatchdog()
        restoreAllStreams()
        mainHandler.post {
            try { recognizer?.stopListening() } catch (_: Exception) {}
        }
    }

    fun destroy() {
        isListening = false
        _rmsLevel.value = 0f
        stopSilenceWatchdog()
        restoreAllStreams()
        mainHandler.post {
            try { recognizer?.destroy() } catch (_: Exception) {}
            recognizer = null
        }
    }
}
