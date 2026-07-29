package com.voicetasker.app.data.ai

import com.voicetasker.app.domain.ai.NoteAiResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NoteAiContractTest {
    private val validBody = """
        {
          "title":"Riunione",
          "improvedText":"Riunione domani alle 10.",
          "date":"2026-07-23",
          "time":"10:00",
          "location":"Ufficio",
          "category":"Lavoro"
        }
    """.trimIndent()

    @Test
    fun `request contains only request id and note fields`() {
        val encoded = SupabaseNoteAiProcessor.encodeRequest(
            ProcessNoteAiRequest(
                requestId = "123e4567-e89b-42d3-a456-426614174000",
                text = "Nota da elaborare",
                categoryNames = listOf("Lavoro"),
                currentDate = "2026-07-22"
            )
        )
        val fields = Json.parseToJsonElement(encoded).jsonObject.keys

        assertEquals(setOf("requestId", "text", "categoryNames", "currentDate"), fields)
        listOf("userId", "email", "jwt", "token", "apiKey", "secret", "audio").forEach {
            assertFalse(encoded.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun `success maps every metadata field exactly`() = runBlocking {
        val processor = processorWith(response(200, validBody))

        val result = processor.process("Nota", listOf("Lavoro"), "2026-07-22")

        assertTrue(result is NoteAiResult.Success)
        val metadata = (result as NoteAiResult.Success).metadata
        assertEquals("Riunione", metadata.title)
        assertEquals("Riunione domani alle 10.", metadata.improvedText)
        assertEquals("2026-07-23", metadata.date)
        assertEquals("10:00", metadata.time)
        assertEquals("Ufficio", metadata.location)
        assertEquals("Lavoro", metadata.category)
    }

    @Test
    fun `optional response fields may be null`() = runBlocking {
        val body = """{"title":"Nota","improvedText":"Testo","date":null,"time":null,"location":null,"category":null}"""
        val processor = processorWith(response(200, body))

        val metadata = (processor.process("Testo", emptyList(), "2026-07-22") as NoteAiResult.Success).metadata

        assertNull(metadata.date)
        assertNull(metadata.time)
        assertNull(metadata.location)
        assertNull(metadata.category)
    }

    @Test
    fun `unauthenticated user does not invoke the function`() = runBlocking {
        val remote = FakeRemote(hasSession = false, responses = ArrayDeque())
        val processor = SupabaseNoteAiProcessor(remote, 1_000)

        val result = processor.process("Nota", emptyList(), "2026-07-22")

        assertEquals(NoteAiResult.AuthenticationRequired, result)
        assertEquals(0, remote.invokeCount)
    }

    @Test
    fun `one 401 refreshes once and retries exactly once`() = runBlocking {
        val remote = FakeRemote(
            responses = ArrayDeque(listOf(response(401), response(200, validBody)))
        )
        val processor = SupabaseNoteAiProcessor(remote, 1_000)

        val result = processor.process("Nota", listOf("Lavoro"), "2026-07-22")

        assertTrue(result is NoteAiResult.Success)
        assertEquals(1, remote.refreshCount)
        assertEquals(2, remote.invokeCount)
        assertEquals(1, remote.requests.map { it.requestId }.distinct().size)
    }

    @Test
    fun `second 401 is a session expired result without another retry`() = runBlocking {
        val remote = FakeRemote(
            responses = ArrayDeque(listOf(response(401), response(401)))
        )
        val processor = SupabaseNoteAiProcessor(remote, 1_000)

        val result = processor.process("Nota", emptyList(), "2026-07-22")

        assertEquals(NoteAiResult.SessionExpired, result)
        assertEquals(1, remote.refreshCount)
        assertEquals(2, remote.invokeCount)
    }

    @Test
    fun `timeout maps without a retry`() = runBlocking {
        val remote = FakeRemote(responses = ArrayDeque(), delayForever = true)
        val processor = SupabaseNoteAiProcessor(remote, 10)

        val result = processor.process("Nota", emptyList(), "2026-07-22")

        assertEquals(NoteAiResult.Timeout, result)
        assertEquals(1, remote.invokeCount)
    }

    @Test
    fun `offline failure maps to network error`() = runBlocking {
        val remote = FakeRemote(responses = ArrayDeque(), failure = IOException("offline"))
        val processor = SupabaseNoteAiProcessor(remote, 1_000)

        val result = processor.process("Nota", emptyList(), "2026-07-22")

        assertEquals(NoteAiResult.NetworkError, result)
        assertEquals(1, remote.invokeCount)
    }

    @Test
    fun `rate limit maps retry after and is not retried`() = runBlocking {
        val remote = FakeRemote(
            responses = ArrayDeque(listOf(response(429, retryAfterSeconds = 45)))
        )
        val processor = SupabaseNoteAiProcessor(remote, 1_000)

        val result = processor.process("Nota", emptyList(), "2026-07-22")

        assertEquals(NoteAiResult.RateLimited(45), result)
        assertEquals(1, remote.invokeCount)
        assertEquals(0, remote.refreshCount)
    }

    @Test
    fun `application errors map distinctly without retries`() = runBlocking {
        val cases = listOf(
            Triple(400, "TEXT_TOO_LONG", NoteAiResult.TextTooLong),
            Triple(429, "MONTHLY_QUOTA_EXHAUSTED", NoteAiResult.MonthlyQuotaExhausted(60)),
            Triple(429, "DAILY_QUOTA_EXHAUSTED", NoteAiResult.DailyQuotaExhausted(60)),
            Triple(429, "RATE_LIMITED", NoteAiResult.RateLimited(60)),
            Triple(409, "CONCURRENT_REQUEST", NoteAiResult.ConcurrentRequest(60)),
            Triple(409, "REQUEST_IN_PROGRESS", NoteAiResult.RequestInProgress(60)),
            Triple(409, "IDEMPOTENCY_CONFLICT", NoteAiResult.IdempotencyConflict)
        )

        cases.forEach { (status, code, expected) ->
            val body = """{"error":{"code":"$code","retryAfterSeconds":60}}"""
            val remote = FakeRemote(responses = ArrayDeque(listOf(response(status, body))))
            val processor = SupabaseNoteAiProcessor(remote, 1_000)

            assertEquals(expected, processor.process("Nota", emptyList(), "2026-07-22"))
            assertEquals(1, remote.invokeCount)
            assertEquals(0, remote.refreshCount)
        }
    }

    @Test
    fun `server error is not retried`() = runBlocking {
        val remote = FakeRemote(responses = ArrayDeque(listOf(response(500))))
        val processor = SupabaseNoteAiProcessor(remote, 1_000)

        assertEquals(
            NoteAiResult.ServerError(500),
            processor.process("Nota", emptyList(), "2026-07-22")
        )
        assertEquals(1, remote.invokeCount)
        assertEquals(0, remote.refreshCount)
    }

    @Test
    fun `process creates a valid uuid request id`() = runBlocking {
        val remote = FakeRemote(responses = ArrayDeque(listOf(response(200, validBody))))
        val processor = SupabaseNoteAiProcessor(remote, 1_000)

        processor.process("Nota", listOf("Lavoro"), "2026-07-22")

        assertTrue(
            Regex(
                """[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"""
            ).matches(remote.requests.single().requestId)
        )
    }

    @Test
    fun `invalid or unexpected success body is rejected`() = runBlocking {
        val processor = processorWith(response(200, """{"title":"Nota","improvedText":"Testo","date":null,"time":null,"location":null,"category":null,"token":"forbidden"}"""))

        val result = processor.process("Nota", emptyList(), "2026-07-22")

        assertEquals(NoteAiResult.InvalidResponse, result)
    }

    private fun processorWith(response: NoteAiRemoteResponse): SupabaseNoteAiProcessor =
        SupabaseNoteAiProcessor(FakeRemote(responses = ArrayDeque(listOf(response))), 1_000)

    private fun response(
        statusCode: Int,
        body: String = "{}",
        retryAfterSeconds: Long? = null
    ) = NoteAiRemoteResponse(statusCode, body, retryAfterSeconds)

    private class FakeRemote(
        private val hasSession: Boolean = true,
        private val responses: ArrayDeque<NoteAiRemoteResponse>,
        private val failure: Exception? = null,
        private val delayForever: Boolean = false
    ) : NoteAiRemote {
        var invokeCount = 0
        var refreshCount = 0
        val requests = mutableListOf<ProcessNoteAiRequest>()

        override suspend fun awaitInitialization() = Unit

        override fun hasCurrentSession(): Boolean = hasSession

        override suspend fun invoke(request: ProcessNoteAiRequest): NoteAiRemoteResponse {
            invokeCount++
            requests += request
            failure?.let { throw it }
            if (delayForever) delay(Long.MAX_VALUE)
            return responses.removeFirst()
        }

        override suspend fun refreshCurrentSession(): Boolean {
            refreshCount++
            return true
        }
    }
}
