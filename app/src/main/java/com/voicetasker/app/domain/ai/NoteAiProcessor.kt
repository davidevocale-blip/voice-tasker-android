package com.voicetasker.app.domain.ai

import java.util.UUID

data class NoteMetadata(
    val title: String,
    val improvedText: String,
    val date: String?,
    val time: String?,
    val location: String?,
    val category: String?
)

enum class NoteAiRequestIdDisposition {
    RETRY_SAME,
    NEW_REQUEST
}

sealed interface NoteAiResult {
    data class Success(val metadata: NoteMetadata) : NoteAiResult
    data object AuthenticationRequired : NoteAiResult
    data object SessionExpired : NoteAiResult
    data class Timeout(
        val requestIdDisposition: NoteAiRequestIdDisposition? = null
    ) : NoteAiResult
    data object TextTooLong : NoteAiResult
    data class MonthlyQuotaExhausted(val retryAfterSeconds: Long?) : NoteAiResult
    data class DailyQuotaExhausted(val retryAfterSeconds: Long?) : NoteAiResult
    data class RateLimited(val retryAfterSeconds: Long?) : NoteAiResult
    data class ConcurrentRequest(val retryAfterSeconds: Long?) : NoteAiResult
    data class RequestInProgress(val retryAfterSeconds: Long?) : NoteAiResult
    data object IdempotencyConflict : NoteAiResult
    data object NetworkError : NoteAiResult
    data object InvalidResponse : NoteAiResult
    data class ServerError(
        val statusCode: Int?,
        val requestIdDisposition: NoteAiRequestIdDisposition? = null
    ) : NoteAiResult
}

class NoteAiOperation private constructor(
    val requestId: String,
    val text: String,
    val categoryNames: List<String>,
    val currentDate: String
) {
    companion object {
        fun create(
            text: String,
            categoryNames: List<String>,
            currentDate: String
        ): NoteAiOperation = NoteAiOperation(
            requestId = UUID.randomUUID().toString(),
            text = text,
            categoryNames = categoryNames.toList(),
            currentDate = currentDate
        )
    }
}

data class NoteAiOperationIntent(
    val text: String,
    val selectedCategoryId: Long?,
    val scheduledDate: Long
)

data class NoteAiOperationPayload(
    val categoryNames: List<String>,
    val currentDate: String
)

class NoteAiOperationExecution internal constructor(
    val operation: NoteAiOperation,
    internal val generation: Long
)

data class NoteAiOperationCompletion internal constructor(
    val isCurrent: Boolean,
    val deferredExecution: NoteAiOperationExecution?
)

class NoteAiOperationSession {
    private data class Pending(
        val intent: NoteAiOperationIntent,
        val operation: NoteAiOperation
    )

    private data class Deferred(
        val intent: NoteAiOperationIntent,
        val payload: NoteAiOperationPayload
    )

    private var pending: Pending? = null
    private var activeExecution: NoteAiOperationExecution? = null
    private var deferred: Deferred? = null
    private var generation: Long = 0

    fun begin(
        intent: NoteAiOperationIntent,
        payload: NoteAiOperationPayload
    ): NoteAiOperationExecution? {
        if (activeExecution != null) return null

        val operation = pending
            ?.takeIf { it.intent == intent }
            ?.operation
            ?: NoteAiOperation.create(
                text = intent.text,
                categoryNames = payload.categoryNames,
                currentDate = payload.currentDate
            ).also { pending = Pending(intent, it) }
        return NoteAiOperationExecution(operation, generation)
            .also { activeExecution = it }
    }

    fun deferLatest(
        intent: NoteAiOperationIntent,
        payload: NoteAiOperationPayload
    ) {
        deferred = Deferred(
            intent = intent,
            payload = payload.copy(categoryNames = payload.categoryNames.toList())
        )
    }

    fun deferLatestIfActive(
        intent: NoteAiOperationIntent,
        payload: NoteAiOperationPayload
    ): Boolean {
        if (activeExecution == null) return false
        deferLatest(intent, payload)
        return true
    }

    fun updateDeferredIfPresent(
        intent: NoteAiOperationIntent,
        payload: NoteAiOperationPayload
    ): Boolean {
        if (deferred == null) return false
        deferLatest(intent, payload)
        return true
    }

    fun beginDeferred(): NoteAiOperationExecution? {
        val deferredRequest = deferred ?: return null
        val execution = begin(
            intent = deferredRequest.intent,
            payload = deferredRequest.payload
        ) ?: return null
        deferred = null
        return execution
    }

    fun complete(
        execution: NoteAiOperationExecution,
        result: NoteAiResult
    ): Boolean {
        if (activeExecution !== execution) return false
        activeExecution = null

        val isCurrent =
            execution.generation == generation &&
                pending?.operation === execution.operation
        if (isCurrent && !result.keepsAiOperationForRetry()) {
            pending = null
        }
        return isCurrent
    }

    fun completeAndBeginDeferred(
        execution: NoteAiOperationExecution,
        result: NoteAiResult
    ): NoteAiOperationCompletion {
        val isCurrent = complete(execution, result)
        return NoteAiOperationCompletion(
            isCurrent = isCurrent,
            deferredExecution = if (isCurrent) null else beginDeferred()
        )
    }

    fun invalidateIfSelectedCategoryChanged(
        previousSelectedCategoryId: Long?,
        selectedCategoryId: Long?
    ): Boolean {
        if (previousSelectedCategoryId == selectedCategoryId) return false
        invalidate()
        return true
    }

    fun invalidate(clearDeferred: Boolean = true) {
        generation++
        pending = null
        if (clearDeferred) deferred = null
    }
}

interface NoteAiProcessor {
    suspend fun process(operation: NoteAiOperation): NoteAiResult

    /**
     * Starts a new logical operation on every invocation.
     * Do not use this overload for idempotent retries; retain and reuse a
     * [NoteAiOperation] with [process] instead.
     */
    suspend fun process(
        text: String,
        categoryNames: List<String>,
        currentDate: String
    ): NoteAiResult = process(
        NoteAiOperation.create(
            text = text,
            categoryNames = categoryNames,
            currentDate = currentDate
        )
    )
}

fun NoteAiResult.keepsAiOperationForRetry(): Boolean = when (this) {
    is NoteAiResult.Success,
    NoteAiResult.TextTooLong,
    is NoteAiResult.RateLimited,
    NoteAiResult.IdempotencyConflict,
    NoteAiResult.InvalidResponse -> false
    is NoteAiResult.Timeout ->
        requestIdDisposition != NoteAiRequestIdDisposition.NEW_REQUEST
    is NoteAiResult.ServerError ->
        statusCode == null ||
            (
                statusCode in 500..599 &&
                    requestIdDisposition != NoteAiRequestIdDisposition.NEW_REQUEST
            )
    else -> true
}

data class NoteAiFallback(
    val text: String,
    val canSaveLocally: Boolean,
    val failureReason: NoteAiFailureReason?,
    val authenticationRequired: Boolean
)

enum class NoteAiFailureReason {
    AUTHENTICATION_REQUIRED,
    TIMEOUT,
    TEXT_TOO_LONG,
    MONTHLY_QUOTA_EXHAUSTED,
    DAILY_QUOTA_EXHAUSTED,
    RATE_LIMITED,
    CONCURRENT_REQUEST,
    REQUEST_IN_PROGRESS,
    IDEMPOTENCY_CONFLICT,
    NETWORK_ERROR,
    INVALID_RESPONSE,
    SERVER_ERROR
}

fun NoteAiResult.toFallback(originalText: String): NoteAiFallback = when (this) {
    is NoteAiResult.Success -> NoteAiFallback(
        text = metadata.improvedText,
        canSaveLocally = true,
        failureReason = null,
        authenticationRequired = false
    )
    NoteAiResult.AuthenticationRequired,
    NoteAiResult.SessionExpired -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.AUTHENTICATION_REQUIRED,
        authenticationRequired = true
    )
    is NoteAiResult.Timeout -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.TIMEOUT,
        authenticationRequired = false
    )
    NoteAiResult.TextTooLong -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.TEXT_TOO_LONG,
        authenticationRequired = false
    )
    is NoteAiResult.MonthlyQuotaExhausted -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.MONTHLY_QUOTA_EXHAUSTED,
        authenticationRequired = false
    )
    is NoteAiResult.DailyQuotaExhausted -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.DAILY_QUOTA_EXHAUSTED,
        authenticationRequired = false
    )
    is NoteAiResult.RateLimited -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.RATE_LIMITED,
        authenticationRequired = false
    )
    is NoteAiResult.ConcurrentRequest -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.CONCURRENT_REQUEST,
        authenticationRequired = false
    )
    is NoteAiResult.RequestInProgress -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.REQUEST_IN_PROGRESS,
        authenticationRequired = false
    )
    NoteAiResult.IdempotencyConflict -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.IDEMPOTENCY_CONFLICT,
        authenticationRequired = false
    )
    NoteAiResult.NetworkError -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.NETWORK_ERROR,
        authenticationRequired = false
    )
    NoteAiResult.InvalidResponse -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.INVALID_RESPONSE,
        authenticationRequired = false
    )
    is NoteAiResult.ServerError -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.SERVER_ERROR,
        authenticationRequired = false
    )
}
