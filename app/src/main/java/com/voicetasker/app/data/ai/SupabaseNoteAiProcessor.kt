package com.voicetasker.app.data.ai

import com.voicetasker.app.domain.ai.NoteAiProcessor
import com.voicetasker.app.domain.ai.NoteAiResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

internal data class NoteAiRemoteResponse(
    val statusCode: Int,
    val body: String,
    val retryAfterSeconds: Long?
)

internal interface NoteAiRemote {
    suspend fun awaitInitialization()
    fun hasCurrentSession(): Boolean
    suspend fun invoke(request: ProcessNoteAiRequest): NoteAiRemoteResponse
    suspend fun refreshCurrentSession(): Boolean
}

private class SupabaseNoteAiRemote(
    private val client: SupabaseClient
) : NoteAiRemote {
    override suspend fun awaitInitialization() {
        client.auth.awaitInitialization()
    }

    override fun hasCurrentSession(): Boolean =
        client.auth.currentSessionOrNull() != null &&
            !client.auth.currentAccessTokenOrNull().isNullOrBlank()

    override suspend fun invoke(request: ProcessNoteAiRequest): NoteAiRemoteResponse {
        return try {
            val response = client.functions.invoke(
                SupabaseNoteAiProcessor.PROCESS_NOTE_AI_FUNCTION,
                request
            )
            NoteAiRemoteResponse(
                statusCode = response.status.value,
                body = response.bodyAsText(),
                retryAfterSeconds = response.headers["Retry-After"]?.toLongOrNull()
            )
        } catch (error: RestException) {
            NoteAiRemoteResponse(
                statusCode = error.statusCode,
                body = "",
                retryAfterSeconds = null
            )
        }
    }

    override suspend fun refreshCurrentSession(): Boolean {
        return try {
            client.auth.refreshCurrentSession()
            hasCurrentSession()
        } catch (error: RestException) {
            false
        }
    }
}

@Singleton
class SupabaseNoteAiProcessor internal constructor(
    private val remote: NoteAiRemote,
    private val timeoutMs: Long
) : NoteAiProcessor {
    @Inject
    constructor(client: SupabaseClient) : this(
        remote = SupabaseNoteAiRemote(client),
        timeoutMs = DEFAULT_TIMEOUT_MS
    )

    override suspend fun process(
        text: String,
        categoryNames: List<String>,
        currentDate: String
    ): NoteAiResult {
        val request = ProcessNoteAiRequest(text, categoryNames, currentDate)
        return try {
            remote.awaitInitialization()
            if (!remote.hasCurrentSession()) {
                NoteAiResult.AuthenticationRequired
            } else {
                invoke(request, categoryNames, allowRefresh = true)
            }
        } catch (error: TimeoutCancellationException) {
            NoteAiResult.Timeout
        } catch (error: HttpRequestTimeoutException) {
            NoteAiResult.Timeout
        } catch (error: SocketTimeoutException) {
            NoteAiResult.Timeout
        } catch (error: IOException) {
            NoteAiResult.NetworkError
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            NoteAiResult.ServerError(statusCode = null)
        }
    }

    private suspend fun invoke(
        request: ProcessNoteAiRequest,
        categoryNames: List<String>,
        allowRefresh: Boolean
    ): NoteAiResult {
        val response = withTimeout(timeoutMs) { remote.invoke(request) }
        if (response.statusCode == HttpStatusCode.Unauthorized.value) {
            if (!allowRefresh) return NoteAiResult.SessionExpired
            val refreshed = remote.refreshCurrentSession()
            if (!refreshed) return NoteAiResult.SessionExpired
            return invoke(request, categoryNames, allowRefresh = false)
        }
        return mapResponse(response, categoryNames)
    }

    internal companion object {
        const val PROCESS_NOTE_AI_FUNCTION = "process-note-ai"
        const val DEFAULT_TIMEOUT_MS = 30_000L

        private val json = Json {
            ignoreUnknownKeys = false
            explicitNulls = true
        }

        internal fun encodeRequest(request: ProcessNoteAiRequest): String =
            json.encodeToString(request)

        internal fun mapResponse(
            response: NoteAiRemoteResponse,
            categoryNames: List<String>
        ): NoteAiResult = when (response.statusCode) {
            HttpStatusCode.OK.value -> decodeSuccess(response.body, categoryNames)
            HttpStatusCode.Unauthorized.value -> NoteAiResult.SessionExpired
            HttpStatusCode.RequestTimeout.value,
            HttpStatusCode.GatewayTimeout.value -> NoteAiResult.Timeout
            HttpStatusCode.TooManyRequests.value ->
                NoteAiResult.RateLimited(response.retryAfterSeconds)
            in 400..599 -> NoteAiResult.ServerError(response.statusCode)
            else -> NoteAiResult.InvalidResponse
        }

        private fun decodeSuccess(
            body: String,
            categoryNames: List<String>
        ): NoteAiResult {
            val response = try {
                json.decodeFromString<ProcessNoteAiResponse>(body)
            } catch (error: SerializationException) {
                return NoteAiResult.InvalidResponse
            } catch (error: IllegalArgumentException) {
                return NoteAiResult.InvalidResponse
            }

            if (!isValid(response, categoryNames)) return NoteAiResult.InvalidResponse
            return NoteAiResult.Success(response.toDomain())
        }

        private fun isValid(
            response: ProcessNoteAiResponse,
            categoryNames: List<String>
        ): Boolean {
            if (response.title.isBlank() || response.title.length > 120) return false
            if (response.improvedText.isBlank() || response.improvedText.length > 20_000) return false
            if (response.date != null && !DATE_PATTERN.matches(response.date)) return false
            if (response.time != null && !TIME_PATTERN.matches(response.time)) return false
            if (response.location != null && (response.location.isBlank() || response.location.length > 200)) return false
            if (response.category != null && categoryNames.none {
                    it.equals(response.category, ignoreCase = true)
                }
            ) return false
            return true
        }

        private val DATE_PATTERN = Regex("""\d{4}-(0[1-9]|1[0-2])-([0-2]\d|3[01])""")
        private val TIME_PATTERN = Regex("""([01]\d|2[0-3]):[0-5]\d""")
    }
}
