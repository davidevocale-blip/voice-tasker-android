package com.voicetasker.app.ui.screen.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.voicetasker.app.data.auth.SupabaseAuthManager
import com.voicetasker.app.data.billing.BillingManager
import com.voicetasker.app.data.billing.BillingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.lifecycle.viewModelScope

data class PaywallUiState(
    val isLoggedIn: Boolean = false,
    val isPremium: Boolean = false,
    val purchaseInProgress: Boolean = false,
    val purchaseError: String? = null,
    val purchaseSuccess: Boolean = false
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val authManager: SupabaseAuthManager
) : ViewModel() {

    val uiState: StateFlow<PaywallUiState> = combine(
        authManager.currentUser,
        billingManager.state
    ) { user, billing ->
        PaywallUiState(
            isLoggedIn = user != null,
            isPremium = billing.isPremium,
            purchaseInProgress = billing.purchaseInProgress,
            purchaseError = billing.purchaseError,
            purchaseSuccess = billing.purchaseSuccess
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaywallUiState())

    fun launchMonthlyPurchase(activity: Activity) {
        val details = billingManager.state.value.monthlyDetails
        if (details == null) {
            billingManager.setPurchaseError("Prodotto mensile non disponibile su Google Play (Play Console in configurazione).")
            return
        }
        billingManager.launchSubscriptionPurchase(activity, details, "monthly-base")
    }

    fun launchYearlyPurchase(activity: Activity) {
        val details = billingManager.state.value.yearlyDetails
        if (details == null) {
            billingManager.setPurchaseError("Prodotto annuale non disponibile su Google Play (Play Console in configurazione).")
            return
        }
        billingManager.launchSubscriptionPurchase(activity, details, "yearly-base")
    }

    fun launchLifetimePurchase(activity: Activity) {
        val details = billingManager.state.value.lifetimeDetails
        if (details == null) {
            billingManager.setPurchaseError("Prodotto Lifetime non disponibile su Google Play (Play Console in configurazione).")
            return
        }
        billingManager.launchLifetimePurchase(activity, details)
    }

    fun clearPurchaseState() {
        billingManager.clearPurchaseState()
    }
}
