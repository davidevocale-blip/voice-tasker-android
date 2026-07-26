package com.voicetasker.app.ui.localization

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizedFormattingTest {
    @Test
    fun `first day of week follows the locale`() {
        assertEquals(Calendar.SUNDAY, firstDayOfWeek(Locale.US))
        assertEquals(Calendar.MONDAY, firstDayOfWeek(Locale.ITALY))
    }

    @Test
    fun `short weekday names start from the locale first day`() {
        val usSymbols = DateFormatSymbols.getInstance(Locale.US).shortWeekdays
        val italianSymbols =
            DateFormatSymbols.getInstance(Locale.ITALY).shortWeekdays

        assertEquals(
            listOf(
                usSymbols[Calendar.SUNDAY],
                usSymbols[Calendar.MONDAY],
                usSymbols[Calendar.TUESDAY],
                usSymbols[Calendar.WEDNESDAY],
                usSymbols[Calendar.THURSDAY],
                usSymbols[Calendar.FRIDAY],
                usSymbols[Calendar.SATURDAY]
            ),
            orderedShortWeekdayNames(Locale.US)
        )
        assertEquals(
            listOf(
                italianSymbols[Calendar.MONDAY],
                italianSymbols[Calendar.TUESDAY],
                italianSymbols[Calendar.WEDNESDAY],
                italianSymbols[Calendar.THURSDAY],
                italianSymbols[Calendar.FRIDAY],
                italianSymbols[Calendar.SATURDAY],
                italianSymbols[Calendar.SUNDAY]
            ),
            orderedShortWeekdayNames(Locale.ITALY)
        )
    }
}
