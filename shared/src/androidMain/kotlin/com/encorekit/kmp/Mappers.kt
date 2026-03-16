package com.encorekit.kmp

import com.encorekit.kmp.models.BillingPurchaseResult
import com.encorekit.kmp.models.LogLevel
import com.encorekit.kmp.models.NotGrantedReason
import com.encorekit.kmp.models.PresentationResult
import com.encorekit.kmp.models.PurchaseRequest
import com.encorekit.kmp.models.UserAttributes
import com.encorekit.encore.features.offers.PresentationResult as NativePresentationResult
import com.encorekit.encore.features.offers.NotGrantedReason as NativeNotGrantedReason
import com.encorekit.encore.core.canonical.iap.PurchaseRequest as NativePurchaseRequest
import com.encorekit.encore.core.infrastructure.billing.BillingPurchaseResult as NativeBillingResult
import com.encorekit.encore.core.canonical.user.UserAttributes as NativeUserAttributes
import com.encorekit.encore.core.infrastructure.logging.LogLevel as NativeLogLevel

// -- LogLevel --

internal fun LogLevel.toNative(): NativeLogLevel = when (this) {
    LogLevel.NONE -> NativeLogLevel.NONE
    LogLevel.ERROR -> NativeLogLevel.ERROR
    LogLevel.WARN -> NativeLogLevel.WARN
    LogLevel.INFO -> NativeLogLevel.INFO
    LogLevel.DEBUG -> NativeLogLevel.DEBUG
}

// -- UserAttributes --

internal fun UserAttributes.toNative(): NativeUserAttributes = NativeUserAttributes(
    email = email,
    firstName = firstName,
    lastName = lastName,
    phoneNumber = phoneNumber,
    postalCode = postalCode,
    city = city,
    state = state,
    countryCode = countryCode,
    latitude = latitude,
    longitude = longitude,
    dateOfBirth = dateOfBirth,
    gender = gender,
    language = language,
    subscriptionTier = subscriptionTier,
    monthsSubscribed = monthsSubscribed,
    billingCycle = billingCycle,
    lastPaymentAmount = lastPaymentAmount,
    lastActiveDate = lastActiveDate,
    totalSessions = totalSessions,
    custom = custom,
)

// -- PresentationResult --

internal fun NativePresentationResult.toCommon(): PresentationResult = when (this) {
    is NativePresentationResult.Granted -> PresentationResult.Granted(
        offerId = offerId,
        campaignId = campaignId,
    )
    is NativePresentationResult.NotGranted -> PresentationResult.NotGranted(
        reason = reason.toCommon(),
    )
}

// -- PurchaseRequest --

internal fun NativePurchaseRequest.toCommon(): PurchaseRequest = PurchaseRequest(
    productId = productId,
    placementId = placementId,
    promoOfferId = promoOfferId,
)

// -- BillingPurchaseResult --

internal fun NativeBillingResult.toCommon(): BillingPurchaseResult = BillingPurchaseResult(
    productId = productId,
    purchaseToken = purchaseToken,
    orderId = orderId,
)

// -- NotGrantedReason --

internal fun NativeNotGrantedReason.toCommon(): NotGrantedReason = when (this) {
    NativeNotGrantedReason.USER_CLOSED -> NotGrantedReason.USER_CLOSED
    NativeNotGrantedReason.NO_OFFERS -> NotGrantedReason.NO_OFFERS
    NativeNotGrantedReason.ERROR -> NotGrantedReason.ERROR
}
