package com.voicetasker.app.ui.screen.login

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.voicetasker.app.BuildConfig
import com.voicetasker.app.data.auth.SupabaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val isLoginMode: Boolean = true,
    val emailInput: String = "",
    val passwordInput: String = "",
    val showSuccessMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: SupabaseAuthManager
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _uiState.update { it.copy(isLoggedIn = user != null) }
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val credentialManager = CredentialManager.create(activity)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                Log.d(TAG, "Got Google ID token, signing in with Supabase")
                authManager.signInWithGoogle(idToken)

                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "User cancelled sign-in")
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore durante l'accesso: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(emailInput = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(passwordInput = password) }
    }

    fun toggleLoginMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode, errorMessage = null) }
    }

    fun submitEmailAuth() {
        val email = _uiState.value.emailInput.trim()
        val password = _uiState.value.passwordInput

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Compila tutti i campi.") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "La password deve avere almeno 6 caratteri.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                if (_uiState.value.isLoginMode) {
                    authManager.signInWithEmail(email, password)
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                } else {
                    authManager.signUpWithEmail(email, password)
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            showSuccessMessage = "Registrazione completata! Controlla la tua email per il link di attivazione.",
                            isLoginMode = true // Switch back to login mode after successful signup
                        ) 
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Email Auth failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore: ${e.message}"
                    )
                }
            }
        }
    }
}
