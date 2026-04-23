package com.voicetasker.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    // Custom-generated tones via raw resource IDs would be ideal,
    // but we use system defaults for reliability
    private var soundSaveId: Int = 0
    private var soundDeleteId: Int = 0
    private var soundEditId: Int = 0
    private var loaded = false

    init {
        try {
            // Load system notification sounds as fallback
            soundSaveId = soundPool.load(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.hashCode(), 1)
        } catch (_: Exception) {}
        loaded = true
    }

    enum class FeedbackType { SAVE, DELETE, EDIT }

    fun play(type: FeedbackType) {
        haptic(type)
        playTone(type)
    }

    private fun haptic(type: FeedbackType) {
        val effect = when (type) {
            FeedbackType.SAVE -> VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
            FeedbackType.DELETE -> VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1)
            FeedbackType.EDIT -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }

    private fun playTone(type: FeedbackType) {
        try {
            val toneGenerator = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION, 60
            )
            when (type) {
                FeedbackType.SAVE -> toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 150)
                FeedbackType.DELETE -> toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_NACK, 150)
                FeedbackType.EDIT -> toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100)
            }
            // Release after tone plays
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ toneGenerator.release() }, 300)
        } catch (_: Exception) {}
    }

    fun release() {
        try { soundPool.release() } catch (_: Exception) {}
    }
}
