package com.encorekit.kmp.models

data class BillingPurchaseResult(
    val productId: String,
    val purchaseToken: String? = null,
    val orderId: String? = null,
    val transactionId: String? = null,
)
