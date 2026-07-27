package com.voicetasker.app.domain.reminder

import com.voicetasker.app.domain.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone

class ReminderTriggerCalculatorTest {
    private val calculator = ReminderTriggerCalculator()
    private val rome = ZoneId.of("Europe/Rome")

    @Test
    fun `Pixel 8 case two hours uses trigger instead of event instant`() {
        val result = calculator.calculate(
            scheduledDateMillis = 1_785_110_400_000L,
            noteTime = "01:35",
            reminderType = ReminderType.TWO_HOURS,
            zoneId = rome,
            clock = Clock.fixed(
                Instant.ofEpochMilli(1_785_101_571_260L),
                ZoneOffset.UTC
            )
        )

        assertEquals(
            ReminderTriggerResult.Success(
                triggerAt = Instant.ofEpochMilli(1_785_101_700_000L)
            ),
            result
        )
    }

    @Test
    fun `Rome 10 August at 15 two hours triggers at 13 local`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-08-10"),
            time = "15:00",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertSuccess("2026-08-10T11:00:00Z", result)
    }

    @Test
    fun `Rome 09 twelve hours crosses midnight to previous day 21 local`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-08-10"),
            time = "09:00",
            type = ReminderType.TWELVE_HOURS,
            zoneId = rome
        )

        assertSuccess("2026-08-09T19:00:00Z", result)
    }

    @Test
    fun `Rome 08 one day triggers previous day at 08 local`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-08-10"),
            time = "08:00",
            type = ReminderType.ONE_DAY,
            zoneId = rome
        )

        assertSuccess("2026-08-09T06:00:00Z", result)
    }

    @Test
    fun `canonical UTC midnight supplies the selected calendar date`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-07-28"),
            time = "10:30",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertSuccess("2026-07-28T06:30:00Z", result)
    }

    @Test
    fun `legacy exact local midnight supplies the local calendar date`() {
        val localMidnight = LocalDate.parse("2026-07-28")
            .atStartOfDay(rome)
            .toInstant()
            .toEpochMilli()

        val result = calculate(
            dateMillis = localMidnight,
            time = "10:30",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertSuccess("2026-07-28T06:30:00Z", result)
    }

    @Test
    fun `non-midnight historical value is rejected conservatively`() {
        val result = calculate(
            dateMillis = Instant.parse("2026-07-28T10:17:00Z").toEpochMilli(),
            time = "10:30",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertEquals(ReminderTriggerResult.InvalidDate, result)
    }

    @Test
    fun `missing selection is reported as missing date`() {
        val result = calculate(
            dateMillis = null,
            time = "10:30",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertEquals(ReminderTriggerResult.MissingDate, result)
    }

    @Test
    fun `blank time is reported as missing`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-07-28"),
            time = "",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertEquals(ReminderTriggerResult.MissingTime, result)
    }

    @Test
    fun `whitespace-only time is invalid rather than missing`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-07-28"),
            time = " ",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertEquals(ReminderTriggerResult.InvalidTime, result)
    }

    @Test
    fun `time must have exactly two hour and minute digits`() {
        listOf("9:30", "09:3", "09:30:00", " 09:30", "09:30 ").forEach { time ->
            val result = calculate(
                dateMillis = utcMidnight("2026-07-28"),
                time = time,
                type = ReminderType.TWO_HOURS,
                zoneId = rome
            )

            assertEquals("time=$time", ReminderTriggerResult.InvalidTime, result)
        }
    }

    @Test
    fun `out of range time is invalid`() {
        listOf("24:00", "23:60", "-1:00").forEach { time ->
            val result = calculate(
                dateMillis = utcMidnight("2026-07-28"),
                time = time,
                type = ReminderType.TWO_HOURS,
                zoneId = rome
            )

            assertEquals("time=$time", ReminderTriggerResult.InvalidTime, result)
        }
    }

    @Test
    fun `two hours means two elapsed hours`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-03-29"),
            time = "04:30",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertSuccess("2026-03-29T00:30:00Z", result)
    }

    @Test
    fun `twelve hours means twelve elapsed hours`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-10-25"),
            time = "10:00",
            type = ReminderType.TWELVE_HOURS,
            zoneId = rome
        )

        assertSuccess("2026-10-24T21:00:00Z", result)
    }

    @Test
    fun `one day means previous local calendar day at the same wall time`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-03-30"),
            time = "10:00",
            type = ReminderType.ONE_DAY,
            zoneId = rome
        )

        assertSuccess("2026-03-29T08:00:00Z", result)
    }

    @Test
    fun `one day preserves wall time across autumn offset change`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-10-26"),
            time = "10:00",
            type = ReminderType.ONE_DAY,
            zoneId = rome
        )

        assertSuccess("2026-10-25T09:00:00Z", result)
    }

    @Test
    fun `nonexistent event local time is rejected`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-03-29"),
            time = "02:30",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertEquals(ReminderTriggerResult.NonexistentLocalTime, result)
    }

    @Test
    fun `nonexistent previous-day reminder wall time is rejected`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-03-30"),
            time = "02:30",
            type = ReminderType.ONE_DAY,
            zoneId = rome
        )

        assertEquals(ReminderTriggerResult.NonexistentLocalTime, result)
    }

    @Test
    fun `ambiguous local time deterministically uses the earlier offset`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-10-25"),
            time = "02:30",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertSuccess("2026-10-24T22:30:00Z", result)
    }

    @Test
    fun `trigger equal to current instant is rejected`() {
        val result = calculator.calculate(
            scheduledDateMillis = utcMidnight("2026-07-28"),
            noteTime = "10:30",
            reminderType = ReminderType.TWO_HOURS,
            zoneId = rome,
            clock = Clock.fixed(Instant.parse("2026-07-28T06:30:00Z"), ZoneOffset.UTC)
        )

        assertEquals(ReminderTriggerResult.TriggerInPast, result)
    }

    @Test
    fun `trigger before current instant is rejected`() {
        val result = calculator.calculate(
            scheduledDateMillis = utcMidnight("2026-07-28"),
            noteTime = "10:30",
            reminderType = ReminderType.TWO_HOURS,
            zoneId = rome,
            clock = Clock.fixed(Instant.parse("2026-07-28T06:30:01Z"), ZoneOffset.UTC)
        )

        assertEquals(ReminderTriggerResult.TriggerInPast, result)
    }

    @Test
    fun `canonical date is stable in a timezone behind UTC`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-07-28"),
            time = "10:30",
            type = ReminderType.TWO_HOURS,
            zoneId = ZoneId.of("America/Los_Angeles")
        )

        assertSuccess("2026-07-28T15:30:00Z", result)
    }

    @Test
    fun `New York timezone is applied explicitly`() {
        val result = calculate(
            dateMillis = utcMidnight("2026-08-10"),
            time = "15:00",
            type = ReminderType.TWO_HOURS,
            zoneId = ZoneId.of("America/New_York")
        )

        assertSuccess("2026-08-10T17:00:00Z", result)
    }

    @Test
    fun `calculation does not mutate global locale or timezone`() {
        val originalLocale = Locale.getDefault()
        val originalTimeZone = TimeZone.getDefault()

        calculate(
            dateMillis = utcMidnight("2026-08-10"),
            time = "15:00",
            type = ReminderType.TWO_HOURS,
            zoneId = rome
        )

        assertEquals(originalLocale, Locale.getDefault())
        assertEquals(originalTimeZone, TimeZone.getDefault())
    }

    @Test
    fun `date picker value is normalized to canonical UTC midnight`() {
        val input = Instant.parse("2026-07-28T15:45:00Z").toEpochMilli()

        val result = ReminderDateNormalizer.fromDatePickerMillis(input)

        assertEquals(utcMidnight("2026-07-28"), result)
    }

    @Test
    fun `strict ISO AI date is normalized to canonical UTC midnight`() {
        assertEquals(
            utcMidnight("2026-07-28"),
            ReminderDateNormalizer.fromIsoDate("2026-07-28")
        )
        assertEquals(null, ReminderDateNormalizer.fromIsoDate("28/07/2026"))
        assertEquals(null, ReminderDateNormalizer.fromIsoDate("2026-7-28"))
        assertEquals(null, ReminderDateNormalizer.fromIsoDate(" 2026-07-28"))
    }

    private fun calculate(
        dateMillis: Long?,
        time: String,
        type: ReminderType,
        zoneId: ZoneId
    ): ReminderTriggerResult = calculator.calculate(
        scheduledDateMillis = dateMillis,
        noteTime = time,
        reminderType = type,
        zoneId = zoneId,
        clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    )

    private fun assertSuccess(expectedInstant: String, actual: ReminderTriggerResult) {
        assertEquals(
            ReminderTriggerResult.Success(Instant.parse(expectedInstant)),
            actual
        )
    }

    private fun utcMidnight(date: String): Long =
        LocalDateTime.of(LocalDate.parse(date), LocalTime.MIDNIGHT)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
}
