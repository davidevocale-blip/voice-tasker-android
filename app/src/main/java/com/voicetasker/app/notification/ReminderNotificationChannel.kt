package com.voicetasker.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import com.voicetasker.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationChannel @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @SuppressLint("AppBundleLocaleChanges")
    fun createOrUpdate(
        applicationLocales: LocaleListCompat =
            LocaleManagerCompat.getApplicationLocales(context)
    ) {
        val systemLocale = Resources.getSystem().configuration.locales[0]
            ?: Locale.getDefault()
        val locale = notificationLocale(applicationLocales, systemLocale)
        val localizedConfiguration = Configuration(context.resources.configuration)
            .apply { setLocale(locale) }
        val localizedContext =
            context.createConfigurationContext(localizedConfiguration)
        val channel = NotificationChannel(
            CHANNEL_ID,
            localizedContext.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = localizedContext.getString(
                R.string.notification_channel_reminders_description
            )
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "voicetasker_reminders"
    }
}
