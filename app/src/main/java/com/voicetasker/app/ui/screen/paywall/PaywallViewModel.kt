package com.voicetasker.app.ui.screen.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.voicetasker.app.R
import com.voicetasker.app.data.auth.SupabaseAuthManager
import com.voicetasker.app.data.billing.BillingManager
import com.voicetasker.app.data.billing.BillingState
import com.voicetasker.app.ui.resources.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    val purchaseError: UiText? = null,
    val purchaseSuccess: Boolean = false,
    val monthlyPrice: String? = null,
    val yearlyPrice: String? = null,
    val lifetimePrice: String? = null,
    val isMonthlyAvailable: Boolean = false,
    val isYearlyAvailable: Boolean = false,
    val isLifetimeAvailable: Boolean = false
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val authManager: SupabaseAuthManager
) : ViewModel() {

    private val localPurchaseError = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<PaywallUiState> = combine(
        authManager.currentUser,
        billingManager.state,
        localPurchaseError
    ) { user, billing, localError ->
        val monthlyPrice = billing.monthlyDetails
            ?.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == "monthly-base" }
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull()
            ?.formattedPrice
        val yearlyPrice = billing.yearlyDetails
            ?.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == "yearly-base" }
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull()
            ?.formattedPrice
        val lifetimePrice = billing.lifetimeDetails
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice

        PaywallUiState(
            isLoggedIn = user != null,
            isPremium = billing.isPremium,
            purchaseInProgress = billing.purchaseInProgress,
            purchaseError = localError ?: billing.purchaseError?.let { UiText.Dynamic(it) },
            purchaseSuccess = billing.purchaseSuccess,
            monthlyPrice = monthlyPrice,
            yearlyPrice = yearlyPrice,
            lifetimePrice = lifetimePrice,
            isMonthlyAvailable = billing.monthlyDetails != null && monthlyPrice != null,
            isYearlyAvailable = billing.yearlyDetails != null && yearlyPrice != null,
            isLifetimeAvailable = billing.lifetimeDetails != null && lifetimePrice != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaywallUiState())

    fun launchMonthlyPurchase(activity: Activity) {
        val details = billingManager.state.value.monthlyDetails
        if (details == null) {
            localPurchaseError.value = UiText.Resource(R.string.monthly_product_unavailable)
            return
        }
        billingManager.launchSubscriptionPurchase(activity, details, "monthly-base")
    }

    fun launchYearlyPurchase(activity: Activity) {
        val details = billingManager.state.value.yearlyDetails
        if (details == null) {
            localPurchaseError.value = UiText.Resource(R.string.yearly_product_unavailable)
            return
        }
        billingManager.launchSubscriptionPurchase(activity, details, "yearly-base")
    }

    fun launchLifetimePurchase(activity: Activity) {
        val details = billingManager.state.value.lifetimeDetails
        if (details == null) {
            localPurchaseError.value = UiText.Resource(R.string.lifetime_product_unavailable)
            return
        }
        billingManager.launchLifetimePurchase(activity, details)
    }

    fun clearPurchaseState() {
        localPurchaseError.value = null
        billingManager.clearPurchaseState()
    }
}
