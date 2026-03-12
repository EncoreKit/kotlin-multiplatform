package com.encorekit.kmp

import cocoapods.EncoreKMPBridge.EncoreKMPBridge
import com.encorekit.kmp.models.BillingPurchaseResult
import com.encorekit.kmp.models.LogLevel
import com.encorekit.kmp.models.PresentationResult
import com.encorekit.kmp.models.PurchaseRequest
import com.encorekit.kmp.models.UserAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal actual class EncorePlatform actual constructor() {

    private val bridge = EncoreKMPBridge.shared()

    actual val placements: EncorePlacements = EncorePlacements()

    // -- Configuration --

    actual fun configure(apiKey: String, logLevel: LogLevel) {
        bridge.configureWithApiKey(apiKey, logLevel = logLevel.name)
    }

    // -- User Identity --

    actual fun identify(userId: String, attributes: UserAttributes?) {
        bridge.identifyWithUserId(userId, attributes = attributes?.toMap())
    }

    actual fun setUserAttributes(attributes: UserAttributes) {
        bridge.setUserAttributes(attributes.toMap())
    }

    actual fun reset() {
        bridge.reset()
    }

    // -- Placements --

    actual suspend fun show(placementId: String?): PresentationResult =
        suspendCancellableCoroutine { continuation ->
            bridge.showPlacement(placementId) { status, reason, error ->
                val result = when (status) {
                    "granted" -> PresentationResult.Granted()
                    "notGranted" -> PresentationResult.NotGranted(
                        reason = reason ?: error ?: "unknown"
                    )
                    else -> PresentationResult.NotGranted(reason = "unknown")
                }
                continuation.resume(result)
            }
        }

    // -- Delegate Handlers --

    actual fun onPurchaseRequest(handler: suspend (PurchaseRequest) -> Unit) {
        bridge.registerOnPurchaseRequestOnRequest { productId, placementId, promoOfferId ->
            val request = PurchaseRequest(
                productId = productId ?: "",
                placementId = placementId,
                promoOfferId = promoOfferId,
            )
            // Launch the suspend handler on the main dispatcher
            CoroutineScope(Dispatchers.Main).launch {
                handler(request)
                bridge.completePurchaseRequestWithSuccess(true)
            }
        }
    }

    actual fun onPurchaseComplete(handler: suspend (BillingPurchaseResult, String) -> Unit) {
        bridge.registerOnPurchaseCompleteOnComplete { transactionId, productId ->
            val result = BillingPurchaseResult(
                productId = productId ?: "",
                transactionId = transactionId,
            )
            CoroutineScope(Dispatchers.Main).launch {
                handler(result, productId ?: "")
            }
        }
    }

    actual fun onPassthrough(handler: (placementId: String?) -> Unit) {
        bridge.registerOnPassthroughOnPassthrough { placementId ->
            handler(placementId)
        }
    }
}

// -- Mappers --

private fun UserAttributes.toMap(): Map<Any?, Any?> = buildMap<Any?, Any?> {
    email?.let { put("email", it) }
    firstName?.let { put("firstName", it) }
    lastName?.let { put("lastName", it) }
    phoneNumber?.let { put("phoneNumber", it) }
    postalCode?.let { put("postalCode", it) }
    city?.let { put("city", it) }
    state?.let { put("state", it) }
    countryCode?.let { put("countryCode", it) }
    latitude?.let { put("latitude", it) }
    longitude?.let { put("longitude", it) }
    dateOfBirth?.let { put("dateOfBirth", it) }
    gender?.let { put("gender", it) }
    language?.let { put("language", it) }
    subscriptionTier?.let { put("subscriptionTier", it) }
    monthsSubscribed?.let { put("monthsSubscribed", it) }
    billingCycle?.let { put("billingCycle", it) }
    lastPaymentAmount?.let { put("lastPaymentAmount", it) }
    lastActiveDate?.let { put("lastActiveDate", it) }
    totalSessions?.let { put("totalSessions", it) }
    if (custom.isNotEmpty()) put("custom", custom)
}
