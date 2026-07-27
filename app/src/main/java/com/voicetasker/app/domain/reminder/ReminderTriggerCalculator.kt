package com.voicetasker.app.domain.reminder

import com.voicetasker.app.domain.model.ReminderType
import java.time.Clock
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.util.Locale
import javax.inject.Inject

sealed interface ReminderTriggerResult {
    data class Success(val triggerAt: Instant) : ReminderTriggerResult

    sealed interface Failure : ReminderTriggerResult
    data object MissingDate : Failure
    data object MissingTime : Failure
    data object InvalidTime : Failure
    data object TriggerInPast : Failure
    data object InvalidDate : Failure
    data object NonexistentLocalTime : Failure
}

/**
 * Calculates reminder instants without consulting mutable global time settings.
 *
 * A stored date at exact UTC midnight is the canonical DatePicker representation
 * and identifies that UTC calendar date. For backward compatibility, an instant
 * at exact midnight in the supplied `zoneId` identifies that local calendar date. Other stored
 * values are rejected rather than guessed. During a DST overlap, the offset before
 * the transition (the first offset returned by ZoneRules) is selected; a gap fails.
 */
class ReminderTriggerCalculator @Inject constructor() {
    fun calculate(
        scheduledDateMillis: Long?,
        noteTime: String,
        reminderType: ReminderType,
        zoneId: ZoneId,
        clock: Clock
    ): ReminderTriggerResult {
        val date = selectedDate(scheduledDateMillis, zoneId)
            ?: return if (scheduledDateMillis == null) {
                ReminderTriggerResult.MissingDate
            } else {
                ReminderTriggerResult.InvalidDate
            }
        val time = parseTime(noteTime) ?: return if (noteTime.isEmpty()) {
            ReminderTriggerResult.MissingTime
        } else {
            ReminderTriggerResult.InvalidTime
        }
        val eventInstant = resolve(date, time, zoneId)
            ?: return ReminderTriggerResult.NonexistentLocalTime
        val triggerInstant = when (reminderType) {
            ReminderType.TWO_HOURS -> eventInstant.minus(Duration.ofHours(2))
            ReminderType.TWELVE_HOURS -> eventInstant.minus(Duration.ofHours(12))
            ReminderType.ONE_DAY -> resolve(date.minusDays(1), time, zoneId)
                ?: return ReminderTriggerResult.NonexistentLocalTime
        }

        return if (triggerInstant.isAfter(clock.instant())) {
            ReminderTriggerResult.Success(triggerInstant)
        } else {
            ReminderTriggerResult.TriggerInPast
        }
    }

    private fun selectedDate(scheduledDateMillis: Long?, zoneId: ZoneId): LocalDate? {
        if (scheduledDateMillis == null) return null
        return try {
            val instant = Instant.ofEpochMilli(scheduledDateMillis)
            val utc = instant.atZone(ZoneOffset.UTC)
            if (utc.toLocalTime() == LocalTime.MIDNIGHT) {
                return utc.toLocalDate()
            }

            val local = instant.atZone(zoneId)
            val localDate = local.toLocalDate()
            if (
                local.toLocalTime() == LocalTime.MIDNIGHT &&
                localDate.atStartOfDay(zoneId).toInstant() == instant
            ) {
                localDate
            } else {
                null
            }
        } catch (_: DateTimeException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    private fun parseTime(value: String): LocalTime? = try {
        LocalTime.parse(value, STRICT_HOUR_MINUTE)
    } catch (_: DateTimeException) {
        null
    }

    private fun resolve(date: LocalDate, time: LocalTime, zoneId: ZoneId): Instant? {
        val localDateTime = LocalDateTime.of(date, time)
        val validOffsets = zoneId.rules.getValidOffsets(localDateTime)
        if (validOffsets.isEmpty()) return null

        // ZoneRules returns the offset before the transition first during an overlap.
        return localDateTime.atOffset(validOffsets.first()).toInstant()
    }

    private companion object {
        val STRICT_HOUR_MINUTE: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT)
    }
}

/**
 * Produces the canonical persisted date: UTC midnight representing a LocalDate.
 */
object ReminderDateNormalizer {
    fun fromDatePickerMillis(value: Long): Long? = try {
        Instant.ofEpochMilli(value)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeException) {
        null
    } catch (_: ArithmeticException) {
        null
    }

    fun fromIsoDate(value: String): Long? = try {
        LocalDate.parse(value, STRICT_ISO_DATE)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeException) {
        null
    } catch (_: ArithmeticException) {
        null
    }

    private val STRICT_ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE
        .withLocale(Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)
}
