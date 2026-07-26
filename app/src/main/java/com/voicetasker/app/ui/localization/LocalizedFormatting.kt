package com.voicetasker.app.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

private const val DAYS_IN_WEEK = 7

@Composable
fun resourceLocale(): Locale = LocalConfiguration.current.locales[0]

fun firstDayOfWeek(locale: Locale): Int =
    Calendar.getInstance(locale).firstDayOfWeek

fun localizedDateFormatter(locale: Locale): DateFormat =
    DateFormat.getDateInstance(DateFormat.LONG, locale)

fun localizedDateTimeFormatter(locale: Locale): DateFormat =
    DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
        locale
    )

fun orderedShortWeekdayNames(locale: Locale): List<String> {
    val shortWeekdays = DateFormatSymbols.getInstance(locale).shortWeekdays
    val firstDay = firstDayOfWeek(locale)

    return List(DAYS_IN_WEEK) { offset ->
        val dayOfWeek = (firstDay - Calendar.SUNDAY + offset) %
            DAYS_IN_WEEK + Calendar.SUNDAY
        shortWeekdays[dayOfWeek]
    }
}
