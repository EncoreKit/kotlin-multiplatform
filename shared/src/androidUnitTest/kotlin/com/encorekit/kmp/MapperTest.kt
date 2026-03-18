package com.encorekit.kmp

import com.encorekit.kmp.models.BillingPurchaseResult
import com.encorekit.kmp.models.NotGrantedReason
import com.encorekit.kmp.models.PresentationResult
import com.encorekit.kmp.models.PurchaseRequest
import com.encorekit.kmp.models.UserAttributes
import com.encorekit.encore.features.offers.PresentationResult as NativePresentationResult
import com.encorekit.encore.features.offers.DismissReason as NativeDismissReason
import com.encorekit.encore.core.canonical.iap.PurchaseRequest as NativePurchaseRequest
import com.encorekit.encore.core.infrastructure.billing.BillingPurchaseResult as NativeBillingResult
import com.encorekit.encore.core.canonical.user.UserAttributes as NativeUserAttributes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MapperTest {

    // -- PresentationResult: Granted (Completed) --

    @Test
    fun grantedMapsOfferAndCampaign() {
        val native = NativePresentationResult.Completed(
            offerId = "offer_42",
            campaignId = "camp_7",
        )
        val result = native.toCommon()
        assertIs<PresentationResult.Granted>(result)
        assertEquals("offer_42", result.offerId)
        assertEquals("camp_7", result.campaignId)
    }

    @Test
    fun grantedMapsNullFields() {
        val native = NativePresentationResult.Completed(offerId = "offer_1", campaignId = null)
        val result = native.toCommon()
        assertIs<PresentationResult.Granted>(result)
        assertEquals("offer_1", result.offerId)
        assertNull(result.campaignId)
    }

    // -- PresentationResult: NotGranted (Dismissed) --

    @Test
    fun notGrantedMapsUserClosed() {
        val native = NativePresentationResult.Dismissed(reason = NativeDismissReason.USER_CLOSED)
        val result = native.toCommon()
        assertIs<PresentationResult.NotGranted>(result)
        assertEquals(NotGrantedReason.USER_CLOSED, result.reason)
    }

    @Test
    fun notGrantedMapsNoOffers() {
        val native = NativePresentationResult.Dismissed(reason = NativeDismissReason.NO_OFFERS)
        val result = native.toCommon()
        assertIs<PresentationResult.NotGranted>(result)
        assertEquals(NotGrantedReason.NO_OFFERS, result.reason)
    }

    @Test
    fun notGrantedMapsError() {
        val native = NativePresentationResult.Dismissed(reason = NativeDismissReason.ERROR)
        val result = native.toCommon()
        assertIs<PresentationResult.NotGranted>(result)
        assertEquals(NotGrantedReason.ERROR, result.reason)
    }

    // -- PresentationResult: NoOffers --

    @Test
    fun noOffersMapsToNotGrantedNoOffers() {
        val native = NativePresentationResult.NoOffers
        val result = native.toCommon()
        assertIs<PresentationResult.NotGranted>(result)
        assertEquals(NotGrantedReason.NO_OFFERS, result.reason)
    }

    // -- UserAttributes --

    @Test
    fun userAttributesMapsAllFields() {
        val kmp = UserAttributes(
            email = "ada@example.com",
            firstName = "Ada",
            lastName = "Lovelace",
            phoneNumber = "+1234567890",
            postalCode = "78701",
            city = "Austin",
            state = "TX",
            countryCode = "US",
            latitude = "30.2672",
            longitude = "-97.7431",
            dateOfBirth = "1815-12-10",
            gender = "female",
            language = "en",
            subscriptionTier = "premium",
            monthsSubscribed = "12",
            billingCycle = "annual",
            lastPaymentAmount = "99.99",
            lastActiveDate = "2026-03-11",
            totalSessions = "42",
            custom = mapOf("source" to "organic"),
        )
        val native = kmp.toNative()
        assertEquals("ada@example.com", native.email)
        assertEquals("Ada", native.firstName)
        assertEquals("Lovelace", native.lastName)
        assertEquals("+1234567890", native.phoneNumber)
        assertEquals("78701", native.postalCode)
        assertEquals("Austin", native.city)
        assertEquals("TX", native.state)
        assertEquals("US", native.countryCode)
        assertEquals("30.2672", native.latitude)
        assertEquals("-97.7431", native.longitude)
        assertEquals("1815-12-10", native.dateOfBirth)
        assertEquals("female", native.gender)
        assertEquals("en", native.language)
        assertEquals("premium", native.subscriptionTier)
        assertEquals("12", native.monthsSubscribed)
        assertEquals("annual", native.billingCycle)
        assertEquals("99.99", native.lastPaymentAmount)
        assertEquals("2026-03-11", native.lastActiveDate)
        assertEquals("42", native.totalSessions)
        assertEquals(mapOf("source" to "organic"), native.custom)
    }

    @Test
    fun userAttributesMapsNullFields() {
        val kmp = UserAttributes()
        val native = kmp.toNative()
        assertNull(native.email)
        assertNull(native.firstName)
        assertEquals(emptyMap(), native.custom)
    }

    // -- PurchaseRequest --

    @Test
    fun purchaseRequestMapsAllFields() {
        val native = NativePurchaseRequest(
            productId = "com.app.annual",
            placementId = "cancel_flow",
            promoOfferId = "promo_2024",
        )
        val result = native.toCommon()
        assertEquals("com.app.annual", result.productId)
        assertEquals("cancel_flow", result.placementId)
        assertEquals("promo_2024", result.promoOfferId)
    }

    @Test
    fun purchaseRequestMapsNullOptionals() {
        val native = NativePurchaseRequest(
            productId = "com.app.monthly",
            placementId = null,
            promoOfferId = null,
        )
        val result = native.toCommon()
        assertEquals("com.app.monthly", result.productId)
        assertNull(result.placementId)
        assertNull(result.promoOfferId)
    }

    // -- BillingPurchaseResult --

    @Test
    fun billingResultMapsFields() {
        val native = NativeBillingResult(
            productId = "com.app.annual",
            purchaseToken = "token_abc",
            orderId = "GPA.123",
        )
        val result = native.toCommon()
        assertEquals("com.app.annual", result.productId)
        assertEquals("token_abc", result.purchaseToken)
        assertEquals("GPA.123", result.orderId)
    }
}
