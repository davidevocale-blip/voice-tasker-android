package com.voicetasker.app.domain.ai

data class NoteMetadata(
    val title: String,
    val improvedText: String,
    val date: String?,
    val time: String?,
    val location: String?,
    val category: String?
)

sealed interface NoteAiResult {
    data class Success(val metadata: NoteMetadata) : NoteAiResult
    data object AuthenticationRequired : NoteAiResult
    data object SessionExpired : NoteAiResult
    data object Timeout : NoteAiResult
    data object TextTooLong : NoteAiResult
    data class MonthlyQuotaExhausted(val retryAfterSeconds: Long?) : NoteAiResult
    data class DailyQuotaExhausted(val retryAfterSeconds: Long?) : NoteAiResult
    data class RateLimited(val retryAfterSeconds: Long?) : NoteAiResult
    data class ConcurrentRequest(val retryAfterSeconds: Long?) : NoteAiResult
    data class RequestInProgress(val retryAfterSeconds: Long?) : NoteAiResult
    data object IdempotencyConflict : NoteAiResult
    data object NetworkError : NoteAiResult
    data object InvalidResponse : NoteAiResult
    data class ServerError(val statusCode: Int?) : NoteAiResult
}

interface NoteAiProcessor {
    suspend fun process(
        text: String,
        categoryNames: List<String>,
        currentDate: String
    ): NoteAiResult
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
    NoteAiResult.Timeout -> NoteAiFallback(
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
