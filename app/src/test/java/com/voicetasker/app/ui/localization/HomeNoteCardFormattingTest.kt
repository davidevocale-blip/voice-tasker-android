package com.voicetasker.app.ui.localization

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNoteCardFormattingTest {
    @Test
    fun `summer UTC midnight in Rome does not display a derived time in US locale`() {
        val result = formatHomeNoteCardDateTime(
            scheduledDate = Instant.parse("2026-07-27T00:00:00Z").toEpochMilli(),
            noteTime = "",
            locale = Locale.US,
            zoneId = ZoneId.of("Europe/Rome"),
            is24Hour = false
        )

        assertEquals(localizedDate("2026-07-27", Locale.US), result)
        assertFalse(result.contains("2:00 AM"))
        assertFalse(result.contains(":"))
    }

    @Test
    fun `summer UTC midnight in Rome does not display a derived time in Italian locale`() {
        val result = format(
            instant = "2026-07-27T00:00:00Z",
            locale = Locale.ITALY,
            zoneId = ROME
        )

        assertEquals(localizedDate("2026-07-27", Locale.ITALY), result)
        assertFalse(result.contains("02:00"))
    }

    @Test
    fun `winter UTC midnight in Rome does not display a derived time`() {
        val result = format(
            instant = "2026-01-27T00:00:00Z",
            locale = Locale.US,
            zoneId = ROME
        )

        assertEquals(localizedDate("2026-01-27", Locale.US), result)
        assertFalse(result.contains("1:00 AM"))
    }

    @Test
    fun `Rome UTC plus two does not shift morning note time in 24 hour format`() {
        val result = format(
            instant = "2026-07-27T00:00:00Z",
            noteTime = "09:30",
            locale = Locale.US,
            zoneId = ROME,
            is24Hour = true
        )

        assertEquals("${localizedDate("2026-07-27", Locale.US)}, 09:30", result)
    }

    @Test
    fun `Rome UTC plus two does not shift evening note time in 24 hour format`() {
        val result = format(
            instant = "2026-07-27T00:00:00Z",
            noteTime = "21:20",
            locale = Locale.US,
            zoneId = ROME,
            is24Hour = true
        )

        assertEquals("${localizedDate("2026-07-27", Locale.US)}, 21:20", result)
        assertFalse(result.contains("23:20"))
    }

    @Test
    fun `evening note time uses localized 12 hour format without a timezone shift`() {
        val result = format(
            instant = "2026-07-27T00:00:00Z",
            noteTime = "21:20",
            locale = Locale.US,
            zoneId = ROME,
            is24Hour = false
        )

        assertEquals("${localizedDate("2026-07-27", Locale.US)}, 9:20 PM", result)
        assertFalse(result.contains("11:20 PM"))
    }

    @Test
    fun `morning note time uses localized 12 hour format without a timezone shift`() {
        val result = format(
            instant = "2026-07-27T00:00:00Z",
            noteTime = "09:30",
            locale = Locale.US,
            zoneId = ROME,
            is24Hour = false
        )

        assertEquals("${localizedDate("2026-07-27", Locale.US)}, 9:30 AM", result)
        assertFalse(result.contains("11:30 AM"))
    }

    @Test
    fun `blank note time displays only the date`() {
        val result = format(
            instant = "2026-07-27T00:00:00Z",
            noteTime = "",
            locale = Locale.US,
            zoneId = ROME
        )

        assertEquals(localizedDate("2026-07-27", Locale.US), result)
    }

    @Test
    fun `invalid note time displays only the date`() {
        val result = format(
            instant = "2026-07-27T00:00:00Z",
            noteTime = "9:30",
            locale = Locale.US,
            zoneId = ROME
        )

        assertEquals(localizedDate("2026-07-27", Locale.US), result)
    }

    @Test
    fun `canonical UTC date does not move to previous day in New York`() {
        val result = format(
            instant = "2026-07-27T00:00:00Z",
            locale = Locale.US,
            zoneId = ZoneId.of("America/New_York")
        )

        assertEquals(localizedDate("2026-07-27", Locale.US), result)
        assertFalse(result.contains("Jul 26"))
    }

    @Test
    fun `legacy local midnight keeps its local calendar day`() {
        val legacyLocalMidnight = LocalDate.parse("2026-07-27")
            .atStartOfDay(ROME)
            .toInstant()

        val result = formatHomeNoteCardDateTime(
            scheduledDate = legacyLocalMidnight.toEpochMilli(),
            noteTime = "",
            locale = Locale.US,
            zoneId = ROME,
            is24Hour = true
        )

        assertEquals(localizedDate("2026-07-27", Locale.US), result)
    }

    @Test
    fun `Home card text uses note time instead of UTC offset`() {
        val result = format(
            instant = "2026-07-27T00:00:00Z",
            noteTime = "21:20",
            locale = Locale.US,
            zoneId = ROME,
            is24Hour = true
        )

        assertTrue(result.contains("21:20"))
        assertFalse(result.contains("23:20"))
        assertFalse(result.contains("02:00"))
        assertFalse(result.contains("2:00 AM"))
    }

    @Test
    fun `non midnight timestamp falls back to its local calendar day`() {
        val result = format(
            instant = "2026-07-27T23:30:00Z",
            locale = Locale.US,
            zoneId = ROME
        )

        assertEquals(localizedDate("2026-07-28", Locale.US), result)
    }

    private fun format(
        instant: String,
        noteTime: String = "",
        locale: Locale,
        zoneId: ZoneId,
        is24Hour: Boolean = true
    ): String = formatHomeNoteCardDateTime(
        scheduledDate = Instant.parse(instant).toEpochMilli(),
        noteTime = noteTime,
        locale = locale,
        zoneId = zoneId,
        is24Hour = is24Hour
    )

    private fun localizedDate(value: String, locale: Locale): String =
        LocalDate.parse(value)
            .format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(locale)
            )

    private companion object {
        val ROME: ZoneId = ZoneId.of("Europe/Rome")
    }
}
