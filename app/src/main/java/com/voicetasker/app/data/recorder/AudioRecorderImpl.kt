package com.voicetasker.app.data.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderImpl @Inject constructor(@ApplicationContext private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null
    private var startTime: Long = 0

    fun startRecording(): String? {
        val file = File(context.filesDir, "recording_${System.currentTimeMillis()}.m4a")
        outputFile = file.absolutePath
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            startTime = System.currentTimeMillis()
            outputFile
        } catch (e: Exception) { recorder = null; null }
    }

    fun stopRecording(): Pair<String?, Long> {
        val duration = System.currentTimeMillis() - startTime
        try { recorder?.apply { stop(); release() } } catch (_: Exception) {}
        recorder = null
        return Pair(outputFile, duration)
    }

    fun getMaxAmplitude(): Int = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
}
