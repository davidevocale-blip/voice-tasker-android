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
    val message: String?,
    val authenticationRequired: Boolean
)

fun NoteAiResult.toFallback(originalText: String): NoteAiFallback = when (this) {
    is NoteAiResult.Success -> NoteAiFallback(
        text = metadata.improvedText,
        canSaveLocally = true,
        message = null,
        authenticationRequired = false
    )
    NoteAiResult.AuthenticationRequired,
    NoteAiResult.SessionExpired -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        message = "Accedi per elaborare con AI",
        authenticationRequired = true
    )
    NoteAiResult.Timeout -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        message = "Elaborazione AI scaduta. Puoi salvare la nota senza elaborazione.",
        authenticationRequired = false
    )
    is NoteAiResult.RateLimited -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        message = "Troppe richieste AI. Riprova più tardi o salva la nota.",
        authenticationRequired = false
    )
    NoteAiResult.NetworkError -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        message = "Rete non disponibile. Puoi salvare la nota senza elaborazione AI.",
        authenticationRequired = false
    )
    NoteAiResult.InvalidResponse -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        message = "Risposta AI non valida. Puoi salvare la nota originale.",
        authenticationRequired = false
    )
    is NoteAiResult.ServerError -> NoteAiFallback(
        text = originalText,
        canSaveLocally = true,
        message = "Servizio AI non disponibile. Puoi salvare la nota originale.",
        authenticationRequired = false
    )
}
