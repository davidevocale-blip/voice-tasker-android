package com.voicetasker.app.notification

import androidx.core.os.LocaleListCompat
import java.util.Locale

fun notificationLocale(
    applicationLocales: LocaleListCompat,
    systemLocale: Locale
): Locale = applicationLocales[0] ?: systemLocale
