package com.encorekit.kmp

import com.encorekit.kmp.models.BillingPurchaseResult
import com.encorekit.kmp.models.LogLevel
import com.encorekit.kmp.models.PurchaseRequest
import com.encorekit.kmp.models.UserAttributes

/**
 * Main entry point for the Encore KMP SDK.
 *
 * ```kotlin
 * // Configure
 * Encore.configure(apiKey = "key_xxx")
 *
 * // Identify user
 * Encore.identify(userId = "user_123")
 *
 * // Present offers
 * val result = Encore.placement("cancel_flow").show()
 * ```
 */
object Encore {
    internal val platform = EncorePlatform()

    fun configure(apiKey: String, logLevel: LogLevel = LogLevel.NONE) {
        platform.configure(apiKey, logLevel)
    }

    fun identify(userId: String, attributes: UserAttributes? = null) {
        platform.identify(userId, attributes)
    }

    fun setUserAttributes(attributes: UserAttributes) {
        platform.setUserAttributes(attributes)
    }

    fun reset() {
        platform.reset()
    }

    fun placement(id: String? = null): PlacementBuilder {
        return PlacementBuilder(id, platform)
    }

    val placements: EncorePlacements get() = platform.placements

    fun onPurchaseRequest(handler: suspend (PurchaseRequest) -> Unit) {
        platform.onPurchaseRequest(handler)
    }

    fun onPurchaseComplete(handler: suspend (BillingPurchaseResult, String) -> Unit) {
        platform.onPurchaseComplete(handler)
    }

    fun onPassthrough(handler: (placementId: String?) -> Unit) {
        platform.onPassthrough(handler)
    }
}
