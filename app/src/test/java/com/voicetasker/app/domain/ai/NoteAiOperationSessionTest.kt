package com.voicetasker.app.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteAiOperationSessionTest {
    @Test
    fun `timeout followed by an unchanged retry reuses the request id`() {
        val session = NoteAiOperationSession()
        val first = begin(session)

        assertTrue(session.complete(first, NoteAiResult.Timeout()))
        val retry = begin(session)

        assertEquals(first.operation.requestId, retry.operation.requestId)
    }

    @Test
    fun `passive category changes keep the pending operation snapshot`() {
        val session = NoteAiOperationSession()
        val first = begin(session)
        session.complete(first, NoteAiResult.Timeout())

        val retry = begin(
            session,
            payload = payload(
                categoryNames = listOf("Personale", "Lavoro", "Nuova")
            )
        )

        assertEquals(first.operation.requestId, retry.operation.requestId)
        assertEquals(
            listOf("Lavoro", "Personale"),
            retry.operation.categoryNames
        )
    }

    @Test
    fun `current date rollover keeps the pending operation snapshot`() {
        val session = NoteAiOperationSession()
        val first = begin(session)
        session.complete(first, NoteAiResult.Timeout())

        val retry = begin(
            session,
            payload = payload(currentDate = "2026-07-31")
        )

        assertEquals(first.operation.requestId, retry.operation.requestId)
        assertEquals("2026-07-30", retry.operation.currentDate)
    }

    @Test
    fun `changing text after timeout starts a new operation`() {
        val session = NoteAiOperationSession()
        val first = begin(session)
        session.complete(first, NoteAiResult.Timeout())

        val changed = begin(session, intent = intent(text = "Nota modificata"))

        assertNotEquals(first.operation.requestId, changed.operation.requestId)
    }

    @Test
    fun `changing selected category after timeout starts a new operation`() {
        val session = NoteAiOperationSession()
        val first = begin(session)
        session.complete(first, NoteAiResult.Timeout())

        val changed = begin(session, intent = intent(selectedCategoryId = 2L))

        assertNotEquals(first.operation.requestId, changed.operation.requestId)
    }

    @Test
    fun `changing scheduled date after timeout starts a new operation`() {
        val session = NoteAiOperationSession()
        val first = begin(session)
        session.complete(first, NoteAiResult.Timeout())

        val changed = begin(
            session,
            intent = intent(scheduledDate = 1_785_369_600_000L)
        )

        assertNotEquals(first.operation.requestId, changed.operation.requestId)
    }

    @Test
    fun `success followed by an identical request starts a new operation`() {
        val session = NoteAiOperationSession()
        val first = begin(session)
        session.complete(first, success())

        val next = begin(session)

        assertNotEquals(first.operation.requestId, next.operation.requestId)
    }

    @Test
    fun `invalidating text while active makes its result stale`() {
        val session = NoteAiOperationSession()
        val active = begin(session)

        session.invalidate()

        assertFalse(session.complete(active, success()))
    }

    @Test
    fun `invalidating category or date while active makes the result stale`() {
        listOf(
            intent(selectedCategoryId = 2L),
            intent(scheduledDate = 1_785_369_600_000L)
        ).forEach { changedIntent ->
            val session = NoteAiOperationSession()
            val active = begin(session)

            session.invalidate()
            assertFalse(session.complete(active, success()))

            val next = begin(session, intent = changedIntent)
            assertNotEquals(active.operation.requestId, next.operation.requestId)
        }
    }

    @Test
    fun `starting a new recording makes the active result stale`() {
        val session = NoteAiOperationSession()
        val active = begin(session)

        session.invalidate()

        assertFalse(session.complete(active, success()))
    }

    @Test
    fun `stale completion releases the active request gate`() {
        val session = NoteAiOperationSession()
        val active = begin(session)
        session.invalidate()

        assertNull(session.begin(intent(), payload()))
        assertFalse(session.complete(active, NoteAiResult.Timeout()))
        assertNotNull(session.begin(intent(), payload()))
    }

    @Test
    fun `stale completion starts the latest deferred operation once`() {
        val session = NoteAiOperationSession()
        val active = begin(session)
        session.invalidate()
        val firstDeferredIntent = intent(text = "Prima nuova registrazione")
        val latestDeferredIntent = intent(text = "Ultima nuova registrazione")

        assertNull(session.begin(firstDeferredIntent, payload()))
        session.deferLatest(firstDeferredIntent, payload())
        session.deferLatest(latestDeferredIntent, payload())

        val completion = session.completeAndBeginDeferred(
            active,
            NoteAiResult.Timeout()
        )
        val deferred = requireNotNull(completion.deferredExecution)

        assertFalse(completion.isCurrent)
        assertEquals(latestDeferredIntent.text, deferred.operation.text)
        assertNotEquals(active.operation.requestId, deferred.operation.requestId)
        assertNull(session.beginDeferred())
    }

    @Test
    fun `category list refresh does not invalidate but selected category change does`() {
        val unchangedSession = NoteAiOperationSession()
        val unchanged = begin(
            unchangedSession,
            payload = payload(categoryNames = listOf("Lavoro"))
        )
        val previousSelectedCategoryId = 1L
        val unchangedSelectedCategoryId = 1L
        assertFalse(
            unchangedSession.invalidateIfSelectedCategoryChanged(
                previousSelectedCategoryId,
                unchangedSelectedCategoryId
            )
        )

        assertTrue(unchangedSession.complete(unchanged, success()))

        val changedSession = NoteAiOperationSession()
        val changed = begin(changedSession)
        val selectedCategoryId = 2L
        assertTrue(
            changedSession.invalidateIfSelectedCategoryChanged(
                previousSelectedCategoryId,
                selectedCategoryId
            )
        )

        assertFalse(changedSession.complete(changed, success()))
    }

    @Test
    fun `transcription change creates one deferred operation while active`() {
        val session = NoteAiOperationSession()
        val active = begin(session, intent = intent(text = "partial"))
        session.invalidate(clearDeferred = false)

        assertTrue(
            session.deferLatestIfActive(
                intent(text = "final"),
                payload()
            )
        )
        val completion = session.completeAndBeginDeferred(
            active,
            NoteAiResult.Timeout()
        )
        val deferred = requireNotNull(completion.deferredExecution)

        assertFalse(completion.isCurrent)
        assertEquals("final", deferred.operation.text)
        assertNull(session.beginDeferred())
    }

    @Test
    fun `latest transcription replaces earlier deferred operation`() {
        val session = NoteAiOperationSession()
        val active = begin(session, intent = intent(text = "A"))

        session.invalidate(clearDeferred = false)
        assertTrue(session.deferLatestIfActive(intent(text = "B"), payload()))
        session.invalidate(clearDeferred = false)
        assertTrue(session.deferLatestIfActive(intent(text = "C"), payload()))

        val completion = session.completeAndBeginDeferred(
            active,
            NoteAiResult.Timeout()
        )
        val deferred = requireNotNull(completion.deferredExecution)

        assertEquals("C", deferred.operation.text)
        assertNull(session.beginDeferred())
    }

    @Test
    fun `transcription change does not create deferred operation without active execution`() {
        val session = NoteAiOperationSession()

        assertFalse(
            session.deferLatestIfActive(
                intent(text = "final"),
                payload()
            )
        )
        assertNull(session.beginDeferred())
    }

    @Test
    fun `rate limited completes the current request id`() {
        val session = NoteAiOperationSession()
        val first = begin(session)

        assertTrue(
            session.complete(
                first,
                NoteAiResult.RateLimited(retryAfterSeconds = 30)
            )
        )
        val retry = begin(session)

        assertNotEquals(first.operation.requestId, retry.operation.requestId)
    }

    @Test
    fun `finalized service unavailable completes the current request id`() {
        val session = NoteAiOperationSession()
        val first = begin(session)

        assertTrue(
            session.complete(
                first,
                NoteAiResult.ServerError(
                    statusCode = 503,
                    requestIdDisposition = NoteAiRequestIdDisposition.NEW_REQUEST
                )
            )
        )

        assertNotEquals(first.operation.requestId, begin(session).operation.requestId)
    }

    @Test
    fun `all 5xx without disposition retain the current request id`() {
        listOf(500, 502, 503, 504).forEach { status ->
            val session = NoteAiOperationSession()
            val first = begin(session)
            val result = if (status == 504) {
                NoteAiResult.Timeout()
            } else {
                NoteAiResult.ServerError(statusCode = status)
            }

            assertTrue(session.complete(first, result))
            assertEquals(first.operation.requestId, begin(session).operation.requestId)
        }
    }

    @Test
    fun `all 5xx with new request disposition close the current request id`() {
        listOf(500, 502, 503, 504).forEach { status ->
            val session = NoteAiOperationSession()
            val first = begin(session)
            val result = if (status == 504) {
                NoteAiResult.Timeout(NoteAiRequestIdDisposition.NEW_REQUEST)
            } else {
                NoteAiResult.ServerError(
                    statusCode = status,
                    requestIdDisposition = NoteAiRequestIdDisposition.NEW_REQUEST
                )
            }

            assertTrue(session.complete(first, result))
            assertNotEquals(first.operation.requestId, begin(session).operation.requestId)
        }
    }

    @Test
    fun `retry same service unavailable retains the current request id`() {
        val session = NoteAiOperationSession()
        val first = begin(session)

        assertTrue(
            session.complete(
                first,
                NoteAiResult.ServerError(
                    statusCode = 503,
                    requestIdDisposition = NoteAiRequestIdDisposition.RETRY_SAME
                )
            )
        )

        assertEquals(first.operation.requestId, begin(session).operation.requestId)
    }

    @Test
    fun `service unavailable without disposition conservatively retains request id`() {
        val session = NoteAiOperationSession()
        val first = begin(session)

        assertTrue(session.complete(first, NoteAiResult.ServerError(statusCode = 503)))

        assertEquals(first.operation.requestId, begin(session).operation.requestId)
    }

    @Test
    fun `ambiguous results retain the operation for an unchanged retry`() {
        val retryableResults = listOf(
            NoteAiResult.Timeout(),
            NoteAiResult.NetworkError,
            NoteAiResult.ServerError(503),
            NoteAiResult.RequestInProgress(retryAfterSeconds = 10)
        )

        retryableResults.forEach { result ->
            val session = NoteAiOperationSession()
            val first = begin(session)
            assertTrue(session.complete(first, result))

            assertEquals(
                first.operation.requestId,
                begin(session).operation.requestId
            )
        }
    }

    @Test
    fun `a second immediate begin is rejected while processing`() {
        val session = NoteAiOperationSession()

        assertNotNull(session.begin(intent(), payload()))
        assertNull(session.begin(intent(), payload()))
    }

    private fun begin(
        session: NoteAiOperationSession,
        intent: NoteAiOperationIntent = intent(),
        payload: NoteAiOperationPayload = payload()
    ): NoteAiOperationExecution =
        requireNotNull(session.begin(intent, payload))

    private fun intent(
        text: String = "Nota da elaborare",
        selectedCategoryId: Long? = 1L,
        scheduledDate: Long = 1_785_283_200_000L
    ) = NoteAiOperationIntent(
        text = text,
        selectedCategoryId = selectedCategoryId,
        scheduledDate = scheduledDate
    )

    private fun payload(
        categoryNames: List<String> = listOf("Lavoro", "Personale"),
        currentDate: String = "2026-07-30"
    ) = NoteAiOperationPayload(
        categoryNames = categoryNames,
        currentDate = currentDate
    )

    private fun success() = NoteAiResult.Success(
        NoteMetadata(
            title = "Nota",
            improvedText = "Nota da elaborare",
            date = null,
            time = null,
            location = null,
            category = null
        )
    )
}
