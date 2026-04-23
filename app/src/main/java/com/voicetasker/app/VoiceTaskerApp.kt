package com.voicetasker.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.voicetasker.app.worker.ReminderWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VoiceTaskerApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
    override fun onCreate() { super.onCreate(); createNotificationChannel() }
    private fun createNotificationChannel() {
        val ch = NotificationChannel(ReminderWorker.CHANNEL_ID, ReminderWorker.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) }
        (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
    }
}
