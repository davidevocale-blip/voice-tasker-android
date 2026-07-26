package com.voicetasker.app.ui.resources

import com.voicetasker.app.R
import com.voicetasker.app.domain.error.BillingUserError
import com.voicetasker.app.domain.error.SpeechTranscriptionError

fun BillingUserError.toUiText(): UiText.Resource = when (this) {
    BillingUserError.PLAN_UNAVAILABLE ->
        UiText.Resource(R.string.plan_unavailable)

    BillingUserError.PURCHASE_FAILED,
    BillingUserError.UNKNOWN ->
        UiText.Resource(R.string.purchase_error)

    BillingUserError.PURCHASE_CONFIRMATION_FAILED ->
        UiText.Resource(R.string.purchase_confirmation_error)
}

fun SpeechTranscriptionError.toUiText(): UiText.Resource = when (this) {
    SpeechTranscriptionError.RECOGNITION_UNAVAILABLE ->
        UiText.Resource(R.string.speech_recognition_unavailable)

    SpeechTranscriptionError.MICROPHONE_PERMISSION_DENIED ->
        UiText.Resource(R.string.microphone_permission_denied)

    SpeechTranscriptionError.UNKNOWN ->
        UiText.Resource(R.string.speech_transcription_error)
}
