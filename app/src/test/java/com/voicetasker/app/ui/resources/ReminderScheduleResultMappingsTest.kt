package com.voicetasker.app.ui.resources

import com.voicetasker.app.domain.reminder.ReminderTriggerResult
import com.voicetasker.app.domain.repository.ReminderScheduleResult
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReminderScheduleResultMappingsTest {
    @Test
    fun `every scheduling failure has a user-visible message`() {
        val results = listOf(
            ReminderScheduleResult.CalculationFailure(ReminderTriggerResult.MissingDate),
            ReminderScheduleResult.CalculationFailure(ReminderTriggerResult.MissingTime),
            ReminderScheduleResult.CalculationFailure(ReminderTriggerResult.InvalidDate),
            ReminderScheduleResult.CalculationFailure(ReminderTriggerResult.InvalidTime),
            ReminderScheduleResult.CalculationFailure(ReminderTriggerResult.TriggerInPast),
            ReminderScheduleResult.CalculationFailure(
                ReminderTriggerResult.NonexistentLocalTime
            ),
            ReminderScheduleResult.PersistenceFailure,
            ReminderScheduleResult.SchedulingFailure
        )

        results.forEach { result ->
            assertNotNull(result.failureMessageRes())
        }
    }

    @Test
    fun `successful scheduling has no failure message`() {
        val result = ReminderScheduleResult.Success(
            reminderId = 1,
            triggerAt = Instant.parse("2026-08-10T11:00:00Z")
        )

        assertNull(result.failureMessageRes())
    }

    @Test
    fun `failed reminder still completes note save and exposes warning`() {
        val completion = listOf(
            ReminderScheduleResult.CalculationFailure(
                ReminderTriggerResult.TriggerInPast
            )
        ).toCompletedNoteSaveUiResult()

        assertTrue(completion.isSaved)
        assertNotNull(completion.reminderFailureRes)
    }
}
