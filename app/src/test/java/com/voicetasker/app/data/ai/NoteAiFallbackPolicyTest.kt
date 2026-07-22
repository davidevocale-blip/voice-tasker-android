package com.voicetasker.app.data.ai

import com.voicetasker.app.domain.ai.NoteAiResult
import com.voicetasker.app.domain.ai.toFallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteAiFallbackPolicyTest {
    @Test
    fun `every AI failure keeps original text and local saving available`() {
        val originalText = "Testo originale della nota"
        val failures = listOf(
            NoteAiResult.AuthenticationRequired,
            NoteAiResult.SessionExpired,
            NoteAiResult.Timeout,
            NoteAiResult.RateLimited(retryAfterSeconds = 60),
            NoteAiResult.NetworkError,
            NoteAiResult.InvalidResponse,
            NoteAiResult.ServerError(statusCode = 503)
        )

        failures.forEach { failure ->
            val fallback = failure.toFallback(originalText)
            assertEquals(originalText, fallback.text)
            assertTrue(fallback.canSaveLocally)
            assertTrue(fallback.message?.isNotBlank() == true)
        }
    }

    @Test
    fun `authentication failures request login without preventing local save`() {
        listOf(
            NoteAiResult.AuthenticationRequired,
            NoteAiResult.SessionExpired
        ).forEach { failure ->
            val fallback = failure.toFallback("Nota")
            assertTrue(fallback.authenticationRequired)
            assertTrue(fallback.canSaveLocally)
        }
    }
}
