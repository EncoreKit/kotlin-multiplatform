package com.encorekit.kmp.models

data class PurchaseRequest(
    val productId: String,
    val placementId: String?,
    val promoOfferId: String? = null,
)
