package com.voicetasker.app.data.auth

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SupabaseProfile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("subscription_type") val subscriptionType: String? = null
)

@Singleton
class SupabaseAuthManager @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseAuth"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    val isLoggedIn: Boolean get() = _currentUser.value != null

    init {
        observeSessionStatus()
    }

    private fun observeSessionStatus() {
        scope.launch {
            supabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        Log.d(TAG, "Session authenticated")
                        loadUserProfile()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        Log.d(TAG, "Session not authenticated")
                        _currentUser.value = null
                    }
                    is SessionStatus.Initializing -> {
                        Log.d(TAG, "Session initializing")
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Sign in with Google ID token obtained from Credential Manager.
     */
    suspend fun signInWithGoogle(idToken: String) {
        try {
            Log.d(TAG, "Signing in with Google ID token")
            supabaseClient.auth.signInWith(IDToken) {
                provider = Google
                this.idToken = idToken
            }
            // Profile will be loaded by the session observer
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            throw e
        }
    }

    /**
     * Sign up with Email and Password.
     */
    suspend fun signUpWithEmail(email: String, password: String) {
        try {
            Log.d(TAG, "Signing up with Email")
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-up failed", e)
            throw e
        }
    }

    /**
     * Sign in with Email and Password.
     */
    suspend fun signInWithEmail(email: String, password: String) {
        try {
            Log.d(TAG, "Signing in with Email")
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in failed", e)
            throw e
        }
    }

    /**
     * Sign out the current user.
     */
    suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
        }
    }

    /**
     * Load user profile from Supabase 'profiles' table.
     */
    private suspend fun loadUserProfile() {
        try {
            val session = supabaseClient.auth.currentSessionOrNull() ?: return
            val user = session.user ?: return

            // Try to load profile from 'profiles' table
            val profile = try {
                supabaseClient.postgrest["profiles"]
                    .select { filter { eq("id", user.id) } }
                    .decodeSingleOrNull<SupabaseProfile>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not load profile", e)
                null
            }

            _currentUser.value = UserInfo(
                id = user.id,
                email = user.email ?: "",
                displayName = profile?.displayName
                    ?: user.userMetadata?.get("full_name")?.toString()?.removeSurrounding("\""),
                avatarUrl = user.userMetadata?.get("avatar_url")?.toString()?.removeSurrounding("\""),
                isPremium = profile?.isPremium ?: false,
                subscriptionType = profile?.subscriptionType
            )
            Log.d(TAG, "User loaded: ${_currentUser.value?.email}")
        } catch (e: Exception) {
            Log.e(TAG, "loadUserProfile failed", e)
        }
    }

    /**
     * Update premium status in Supabase profile.
     */
    suspend fun updatePremiumStatus(isPremium: Boolean, subscriptionType: String?) {
        try {
            val userId = _currentUser.value?.id ?: return
            supabaseClient.postgrest["profiles"]
                .update({
                    set("is_premium", isPremium)
                    set("subscription_type", subscriptionType)
                    set("updated_at", java.time.Instant.now().toString())
                }) {
                    filter { eq("id", userId) }
                }
            // Update local state
            _currentUser.value = _currentUser.value?.copy(
                isPremium = isPremium,
                subscriptionType = subscriptionType
            )
        } catch (e: Exception) {
            Log.e(TAG, "updatePremiumStatus failed", e)
        }
    }

    /**
     * Refresh user profile from Supabase.
     */
    suspend fun refreshProfile() {
        loadUserProfile()
    }
}
