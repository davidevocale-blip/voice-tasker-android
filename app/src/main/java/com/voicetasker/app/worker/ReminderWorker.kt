package com.voicetasker.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voicetasker.app.R
import com.voicetasker.app.data.local.dao.NoteDao
import com.voicetasker.app.domain.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val noteDao: NoteDao,
    private val reminderRepository: ReminderRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong("noteId", -1L)
        val reminderId = inputData.getLong("reminderId", -1L)
        if (noteId == -1L) return Result.failure()
        val note = noteDao.getNoteByIdOnce(noteId) ?: return Result.failure()
        if (reminderId > 0) reminderRepository.markAsTriggered(reminderId)
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(note.title.ifBlank { "Promemoria VoiceTasker" })
            .setContentText(note.transcription.take(100).ifBlank { "Hai un impegno in programma!" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(noteId.toInt(), notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "voicetasker_reminders"
        const val CHANNEL_NAME = "Promemoria"
    }
}
