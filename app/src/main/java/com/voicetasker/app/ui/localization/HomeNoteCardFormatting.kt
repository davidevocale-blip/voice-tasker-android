package com.voicetasker.app.ui.localization

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * Formats the civil date and optional real event time shown by a Home note card.
 *
 * A canonical UTC-midnight value represents its UTC calendar date. Legacy values
 * stored at local midnight represent their local calendar date. Other timestamps
 * fall back to the calendar date in [zoneId]. No time is derived from
 * [scheduledDate].
 */
fun formatHomeNoteCardDateTime(
    scheduledDate: Long,
    noteTime: String,
    locale: Locale,
    zoneId: ZoneId,
    is24Hour: Boolean
): String {
    val date = resolveCivilDate(scheduledDate, zoneId)
    val formattedDate = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(date)
    val formattedTime = formatLocalNoteTime(
        noteTime = noteTime,
        locale = locale,
        is24Hour = is24Hour
    ) ?: return formattedDate

    return "$formattedDate, $formattedTime"
}

private fun resolveCivilDate(scheduledDate: Long, zoneId: ZoneId): LocalDate {
    val instant = Instant.ofEpochMilli(scheduledDate)
    val utcDateTime = instant.atZone(ZoneOffset.UTC)
    if (utcDateTime.toLocalTime() == LocalTime.MIDNIGHT) {
        return utcDateTime.toLocalDate()
    }

    // A legacy local-midnight value and any non-canonical timestamp both keep
    // their calendar day in the supplied zone.
    return instant.atZone(zoneId).toLocalDate()
}

private fun formatLocalNoteTime(
    noteTime: String,
    locale: Locale,
    is24Hour: Boolean
): String? {
    val localTime = try {
        LocalTime.parse(noteTime, STRICT_HOUR_MINUTE)
    } catch (_: DateTimeException) {
        return null
    }

    // HH:mm is already the canonical local civil time. Returning it unchanged
    // makes a timezone conversion structurally impossible in 24-hour mode.
    if (is24Hour) return noteTime

    return DateTimeFormatter
        .ofPattern("h:mm a", locale)
        .format(localTime)
}

private val STRICT_HOUR_MINUTE: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendLiteral(':')
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    .toFormatter(Locale.ROOT)
    .withResolverStyle(ResolverStyle.STRICT)
