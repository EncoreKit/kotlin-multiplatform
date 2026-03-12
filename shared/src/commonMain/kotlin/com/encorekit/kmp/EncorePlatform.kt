package com.encorekit.kmp

import com.encorekit.kmp.models.BillingPurchaseResult
import com.encorekit.kmp.models.LogLevel
import com.encorekit.kmp.models.PresentationResult
import com.encorekit.kmp.models.PurchaseRequest
import com.encorekit.kmp.models.UserAttributes

internal expect class EncorePlatform() {
    fun configure(apiKey: String, logLevel: LogLevel)
    fun identify(userId: String, attributes: UserAttributes?)
    fun setUserAttributes(attributes: UserAttributes)
    fun reset()
    suspend fun show(placementId: String?): PresentationResult
    val placements: EncorePlacements
    fun onPurchaseRequest(handler: suspend (PurchaseRequest) -> Unit)
    fun onPurchaseComplete(handler: suspend (BillingPurchaseResult, String) -> Unit)
    fun onPassthrough(handler: (placementId: String?) -> Unit)
}
