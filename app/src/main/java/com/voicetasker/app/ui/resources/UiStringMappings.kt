package com.voicetasker.app.ui.resources

import androidx.annotation.StringRes
import com.voicetasker.app.R
import com.voicetasker.app.domain.ai.NoteAiFailureReason
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.domain.reminder.ReminderTriggerResult
import com.voicetasker.app.domain.repository.ReminderScheduleResult

@StringRes
fun ReminderType.labelRes(): Int = when (this) {
    ReminderType.ONE_DAY -> R.string.reminder_one_day_before
    ReminderType.TWELVE_HOURS -> R.string.reminder_twelve_hours_before
    ReminderType.TWO_HOURS -> R.string.reminder_two_hours_before
}

@StringRes
fun NoteAiFailureReason.messageRes(): Int = when (this) {
    NoteAiFailureReason.AUTHENTICATION_REQUIRED -> R.string.ai_authentication_required
    NoteAiFailureReason.TIMEOUT -> R.string.ai_timeout
    NoteAiFailureReason.RATE_LIMITED -> R.string.ai_rate_limited
    NoteAiFailureReason.NETWORK_ERROR -> R.string.ai_network_error
    NoteAiFailureReason.INVALID_RESPONSE -> R.string.ai_invalid_response
    NoteAiFailureReason.SERVER_ERROR -> R.string.ai_server_error
}

@StringRes
fun ReminderScheduleResult.failureMessageRes(): Int? = when (this) {
    is ReminderScheduleResult.Success -> null
    is ReminderScheduleResult.CalculationFailure -> when (reason) {
        ReminderTriggerResult.MissingDate -> R.string.reminder_failure_missing_date
        ReminderTriggerResult.MissingTime -> R.string.reminder_failure_missing_time
        ReminderTriggerResult.InvalidDate -> R.string.reminder_failure_invalid_date
        ReminderTriggerResult.InvalidTime -> R.string.reminder_failure_invalid_time
        ReminderTriggerResult.TriggerInPast -> R.string.reminder_failure_trigger_in_past
        ReminderTriggerResult.NonexistentLocalTime ->
            R.string.reminder_failure_nonexistent_local_time
    }
    ReminderScheduleResult.PersistenceFailure -> R.string.reminder_failure_persistence
    ReminderScheduleResult.SchedulingFailure -> R.string.reminder_failure_scheduling
}

data class CompletedNoteSaveUiResult(
    val isSaved: Boolean,
    @StringRes val reminderFailureRes: Int?
)

fun Iterable<ReminderScheduleResult>.toCompletedNoteSaveUiResult() =
    CompletedNoteSaveUiResult(
        isSaved = true,
        reminderFailureRes = firstNotNullOfOrNull {
            it.failureMessageRes()
        }
    )
