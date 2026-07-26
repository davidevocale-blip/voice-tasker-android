package com.voicetasker.app.domain.error

enum class BillingUserError {
    PLAN_UNAVAILABLE,
    PURCHASE_FAILED,
    PURCHASE_CONFIRMATION_FAILED,
    UNKNOWN
}

data class BillingFailure(
    val userError: BillingUserError,
    val responseCode: Int? = null
)
