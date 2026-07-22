package com.voicetasker.app.ui.screen.settings

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.data.auth.SupabaseAuthManager
import com.voicetasker.app.data.auth.UserInfo
import com.voicetasker.app.data.billing.BillingManager
import com.voicetasker.app.data.billing.BillingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userInfo: UserInfo? = null,
    val isLoggedIn: Boolean = false,
    val billingState: BillingState = BillingState(),
    val isLoggingOut: Boolean = false,
    val showLogoutConfirmation: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authManager: SupabaseAuthManager,
    val billingManager: BillingManager
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        authManager.currentUser,
        billingManager.state
    ) { user, billing ->
        SettingsUiState(
            userInfo = user,
            isLoggedIn = user != null,
            billingState = billing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun signOut() {
        viewModelScope.launch {
            try {
                authManager.signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Sign out failed", e)
            }
        }
    }

    fun launchMonthlyPurchase(activity: Activity) {
        val details = uiState.value.billingState.monthlyDetails ?: return
        billingManager.launchSubscriptionPurchase(activity, details, "monthly-base")
    }

    fun launchYearlyPurchase(activity: Activity) {
        val details = uiState.value.billingState.yearlyDetails ?: return
        billingManager.launchSubscriptionPurchase(activity, details, "yearly-base")
    }

    fun launchLifetimePurchase(activity: Activity) {
        val details = uiState.value.billingState.lifetimeDetails ?: return
        billingManager.launchLifetimePurchase(activity, details)
    }

    fun clearPurchaseState() {
        billingManager.clearPurchaseState()
    }
}
