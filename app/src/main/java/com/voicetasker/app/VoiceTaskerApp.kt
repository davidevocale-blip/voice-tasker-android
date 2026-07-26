package com.voicetasker.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.voicetasker.app.notification.ReminderNotificationChannel
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class VoiceTaskerApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var reminderNotificationChannel: ReminderNotificationChannel
    override val workManagerConfiguration: Configuration get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
    
    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        reminderNotificationChannel.createOrUpdate()
    }
    
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashFile = File(filesDir, "crash_log.txt")
                val stackTrace = Log.getStackTraceString(throwable)
                crashFile.writeText("${java.util.Date()}\n${throwable.message}\n$stackTrace")
            } catch (_: Exception) { }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    companion object {
        fun getCrashLog(context: Context): String? {
            val file = File(context.filesDir, "crash_log.txt")
            return if (file.exists()) file.readText() else null
        }
        fun clearCrashLog(context: Context) {
            File(context.filesDir, "crash_log.txt").delete()
        }
    }
}
