package com.voicetasker.app.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.voicetasker.app.data.auth.SupabaseAuthManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class BillingState(
    val isConnected: Boolean = false,
    val isPremium: Boolean = false,
    val subscriptionType: String? = null,
    val monthlyDetails: ProductDetails? = null,
    val yearlyDetails: ProductDetails? = null,
    val lifetimeDetails: ProductDetails? = null,
    val purchaseInProgress: Boolean = false,
    val purchaseError: String? = null,
    val purchaseSuccess: Boolean = false
)

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: SupabaseAuthManager
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_MONTHLY = "premium_monthly"
        const val PRODUCT_YEARLY = "premium_yearly"
        const val PRODUCT_LIFETIME = "premium_lifetime"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    /**
     * Debug-only: toggle premium status without a real purchase.
     * Only works in debug builds.
     */
    fun debugTogglePremium() {
        val current = _state.value.isPremium
        _state.update { it.copy(
            isPremium = !current,
            subscriptionType = if (!current) "debug" else null
        ) }
        scope.launch { authManager.updatePremiumStatus(!current, if (!current) "debug" else null) }
    }

    private val billingClient: BillingClient = try {
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()
    } catch (e: Exception) {
        Log.e(TAG, "BillingClient creation failed", e)
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    init {
        try {
            connect()
        } catch (e: Exception) {
            Log.e(TAG, "Billing connect failed", e)
        }
    }

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    _state.update { it.copy(isConnected = true) }
                    scope.launch {
                        queryProducts()
                        checkExistingPurchases()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing disconnected")
                _state.update { it.copy(isConnected = false) }
            }
        })
    }

    private suspend fun queryProducts() {
        // Query subscriptions
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_YEARLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val monthly = detailsList.find { it.productId == PRODUCT_MONTHLY }
                val yearly = detailsList.find { it.productId == PRODUCT_YEARLY }
                Log.d(TAG, "Subs loaded: monthly=${monthly != null}, yearly=${yearly != null}")
                _state.update { it.copy(monthlyDetails = monthly, yearlyDetails = yearly) }
            } else {
                Log.e(TAG, "Query subs failed: ${result.debugMessage}")
            }
        }

        // Query in-app (lifetime)
        val iapParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(iapParams) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val lifetime = detailsList.find { it.productId == PRODUCT_LIFETIME }
                Log.d(TAG, "IAP loaded: lifetime=${lifetime != null}")
                _state.update { it.copy(lifetimeDetails = lifetime) }
            } else {
                Log.e(TAG, "Query IAP failed: ${result.debugMessage}")
            }
        }
    }

    /**
     * Check for existing active purchases.
     */
    private suspend fun checkExistingPurchases() {
        // Check subscriptions
        val subsResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val activeSub = subsResult.purchasesList.firstOrNull {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (activeSub != null) {
                val subType = when {
                    PRODUCT_MONTHLY in activeSub.products -> "monthly"
                    PRODUCT_YEARLY in activeSub.products -> "yearly"
                    else -> "subscription"
                }
                Log.d(TAG, "Active subscription found: $subType")
                _state.update { it.copy(isPremium = true, subscriptionType = subType) }
                scope.launch { authManager.updatePremiumStatus(true, subType) }
                return
            }
        }

        // Check in-app purchases (lifetime)
        val iapResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        if (iapResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val lifetimePurchase = iapResult.purchasesList.firstOrNull {
                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        PRODUCT_LIFETIME in it.products
            }
            if (lifetimePurchase != null) {
                Log.d(TAG, "Lifetime purchase found")
                _state.update { it.copy(isPremium = true, subscriptionType = "lifetime") }
                scope.launch { authManager.updatePremiumStatus(true, "lifetime") }
                return
            }
        }

        // No active purchases
        _state.update { it.copy(isPremium = false, subscriptionType = null) }
    }

    /**
     * Launch the purchase flow for a subscription.
     */
    fun launchSubscriptionPurchase(activity: Activity, productDetails: ProductDetails, basePlanId: String) {
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == basePlanId }
            ?.offerToken
            ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: run {
                _state.update { it.copy(purchaseError = "Piano non disponibile") }
                return
            }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        _state.update { it.copy(purchaseInProgress = true, purchaseError = null) }
        billingClient.launchBillingFlow(activity, flowParams)
    }

    /**
     * Launch the purchase flow for the lifetime in-app product.
     */
    fun launchLifetimePurchase(activity: Activity, productDetails: ProductDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()

        _state.update { it.copy(purchaseInProgress = true, purchaseError = null) }
        billingClient.launchBillingFlow(activity, flowParams)
    }

    /**
     * Called when a purchase is completed or cancelled.
     */
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch { handlePurchase(purchase) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Purchase cancelled by user")
                _state.update { it.copy(purchaseInProgress = false) }
            }
            else -> {
                Log.e(TAG, "Purchase error: ${result.debugMessage}")
                _state.update {
                    it.copy(
                        purchaseInProgress = false,
                        purchaseError = "Errore durante l'acquisto: ${result.debugMessage}"
                    )
                }
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        // Acknowledge the purchase if not already
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val ackResult = billingClient.acknowledgePurchase(ackParams)
            if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Acknowledge failed: ${ackResult.debugMessage}")
                _state.update { it.copy(purchaseInProgress = false, purchaseError = "Errore nella conferma dell'acquisto") }
                return
            }
        }

        // Determine subscription type
        val subType = when {
            PRODUCT_MONTHLY in purchase.products -> "monthly"
            PRODUCT_YEARLY in purchase.products -> "yearly"
            PRODUCT_LIFETIME in purchase.products -> "lifetime"
            else -> "unknown"
        }

        Log.d(TAG, "Purchase successful: $subType")

        // Update premium status on Supabase
        authManager.updatePremiumStatus(true, subType)

        _state.update {
            it.copy(
                isPremium = true,
                subscriptionType = subType,
                purchaseInProgress = false,
                purchaseSuccess = true
            )
        }
    }

    fun setPurchaseError(error: String) {
        _state.update { it.copy(purchaseError = error) }
    }

    fun clearPurchaseState() {
        _state.update { it.copy(purchaseError = null, purchaseSuccess = false) }
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
