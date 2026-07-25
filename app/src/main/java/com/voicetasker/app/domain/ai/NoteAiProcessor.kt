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
    data class RateLimited(val retryAfterSeconds: Long?) : NoteAiResult
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
    RATE_LIMITED,
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
    is NoteAiResult.RateLimited -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        failureReason = NoteAiFailureReason.RATE_LIMITED,
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
