package com.voicetasker.app.data.remote

import android.util.Log
import com.voicetasker.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class ApiKeyResponse(val key: String)

/**
 * Fetches the Gemini API key. Strategy:
 * 1. Try Supabase Edge Function (most secure, for production)
 * 2. Fallback to BuildConfig.GEMINI_API_KEY (for development/debug)
 */
@Singleton
class ApiKeyProvider @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "ApiKeyProvider"
        private const val FUNCTION_NAME = "get-gemini-key"
    }

    @Volatile
    private var cachedKey: String? = null

    /**
     * Get the Gemini API key. Returns cached value if available.
     * Tries Supabase Edge Function first, then falls back to BuildConfig.
     */
    suspend fun getGeminiApiKey(): String? {
        cachedKey?.let { return it }

        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            null
        }

        if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "NOT_SET") {
            cachedKey = buildConfigKey
            Log.d(TAG, "Using Gemini API key from BuildConfig (forced fallback)")
            return buildConfigKey
        }

        Log.e(TAG, "No Gemini API key available")
        return null
    }

    /**
     * Clear cached key (e.g., on logout).
     */
    fun clearCache() {
        cachedKey = null
    }
}
