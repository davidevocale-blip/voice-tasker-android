package com.voicetasker.app.ui.resources

import com.voicetasker.app.R
import com.voicetasker.app.data.billing.BillingState
import com.voicetasker.app.data.recorder.SpeechTranscriberImpl
import com.voicetasker.app.domain.error.BillingFailure
import com.voicetasker.app.domain.error.BillingUserError
import com.voicetasker.app.domain.error.SpeechTranscriptionError
import org.junit.Assert.assertEquals
import org.junit.Test

class UserErrorMappingsTest {

    @Test
    fun `data states expose neutral errors instead of localized strings`() {
        val billingError: BillingUserError? = BillingState(
            purchaseError = BillingFailure(BillingUserError.PURCHASE_FAILED)
        ).purchaseError?.userError
        val speechError: SpeechTranscriptionError =
            SpeechTranscriberImpl.TranscriptionState.Error(
                SpeechTranscriptionError.UNKNOWN
            ).error

        assertEquals(BillingUserError.PURCHASE_FAILED, billingError)
        assertEquals(SpeechTranscriptionError.UNKNOWN, speechError)
    }

    @Test
    fun `billing errors map to stable UI resources`() {
        assertEquals(
            UiText.Resource(R.string.plan_unavailable),
            BillingUserError.PLAN_UNAVAILABLE.toUiText()
        )
        assertEquals(
            UiText.Resource(R.string.purchase_error),
            BillingUserError.PURCHASE_FAILED.toUiText()
        )
        assertEquals(
            UiText.Resource(R.string.purchase_confirmation_error),
            BillingUserError.PURCHASE_CONFIRMATION_FAILED.toUiText()
        )
    }

    @Test
    fun `unknown billing error has a stable fallback resource`() {
        assertEquals(
            UiText.Resource(R.string.purchase_error),
            BillingUserError.UNKNOWN.toUiText()
        )
    }

    @Test
    fun `speech errors map to stable UI resources`() {
        assertEquals(
            UiText.Resource(R.string.speech_recognition_unavailable),
            SpeechTranscriptionError.RECOGNITION_UNAVAILABLE.toUiText()
        )
        assertEquals(
            UiText.Resource(R.string.microphone_permission_denied),
            SpeechTranscriptionError.MICROPHONE_PERMISSION_DENIED.toUiText()
        )
    }

    @Test
    fun `unknown speech error has a stable fallback resource`() {
        assertEquals(
            UiText.Resource(R.string.speech_transcription_error),
            SpeechTranscriptionError.UNKNOWN.toUiText()
        )
    }
}
