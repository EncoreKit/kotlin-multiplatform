package com.encorekit.kmp

import com.encorekit.kmp.models.BillingPurchaseResult
import com.encorekit.kmp.models.LogLevel
import com.encorekit.kmp.models.NotGrantedReason
import com.encorekit.kmp.models.PresentationResult
import com.encorekit.kmp.models.PurchaseRequest
import com.encorekit.kmp.models.UserAttributes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ModelsTest {

    // -- PresentationResult --

    @Test
    fun grantedHoldsOfferAndCampaign() {
        val result = PresentationResult.Granted(offerId = "offer_1", campaignId = "campaign_1")
        assertIs<PresentationResult.Granted>(result)
        assertEquals("offer_1", result.offerId)
        assertEquals("campaign_1", result.campaignId)
    }

    @Test
    fun grantedFieldsDefaultToNull() {
        val result = PresentationResult.Granted()
        assertNull(result.offerId)
        assertNull(result.campaignId)
    }

    @Test
    fun notGrantedHoldsReason() {
        val result = PresentationResult.NotGranted(reason = NotGrantedReason.USER_CLOSED)
        assertIs<PresentationResult.NotGranted>(result)
        assertEquals(NotGrantedReason.USER_CLOSED, result.reason)
        assertEquals("user_closed", result.reason.value)
    }

    @Test
    fun notGrantedReasonEnumHasCorrectValues() {
        assertEquals(4, NotGrantedReason.entries.size)
        assertEquals("user_closed", NotGrantedReason.USER_CLOSED.value)
        assertEquals("no_offer_available", NotGrantedReason.NO_OFFERS.value)
        assertEquals("error", NotGrantedReason.ERROR.value)
        assertEquals("unknown", NotGrantedReason.UNKNOWN.value)
    }

    // -- PurchaseRequest --

    @Test
    fun purchaseRequestFieldsPopulated() {
        val req = PurchaseRequest(
            productId = "com.app.annual",
            placementId = "cancel_flow",
            promoOfferId = "promo_2024",
        )
        assertEquals("com.app.annual", req.productId)
        assertEquals("cancel_flow", req.placementId)
        assertEquals("promo_2024", req.promoOfferId)
    }

    @Test
    fun purchaseRequestOptionalFieldsDefaultToNull() {
        val req = PurchaseRequest(productId = "com.app.monthly", placementId = null)
        assertNull(req.placementId)
        assertNull(req.promoOfferId)
    }

    // -- BillingPurchaseResult --

    @Test
    fun billingPurchaseResultAllFields() {
        val result = BillingPurchaseResult(
            productId = "com.app.annual",
            purchaseToken = "token_abc",
            orderId = "GPA.123",
            transactionId = "txn_456",
        )
        assertEquals("com.app.annual", result.productId)
        assertEquals("token_abc", result.purchaseToken)
        assertEquals("GPA.123", result.orderId)
        assertEquals("txn_456", result.transactionId)
    }

    @Test
    fun billingPurchaseResultOptionalFieldsDefaultToNull() {
        val result = BillingPurchaseResult(productId = "com.app.monthly")
        assertNull(result.purchaseToken)
        assertNull(result.orderId)
        assertNull(result.transactionId)
    }

    // -- UserAttributes --

    @Test
    fun userAttributesAllFieldsPopulated() {
        val attrs = UserAttributes(
            email = "user@example.com",
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
        assertEquals("user@example.com", attrs.email)
        assertEquals("Ada", attrs.firstName)
        assertEquals("Austin", attrs.city)
        assertEquals(mapOf("source" to "organic"), attrs.custom)
    }

    @Test
    fun userAttributesDefaultsEmpty() {
        val attrs = UserAttributes()
        assertNull(attrs.email)
        assertNull(attrs.firstName)
        assertEquals(emptyMap(), attrs.custom)
    }

    // -- LogLevel --

    @Test
    fun logLevelHasCorrectValues() {
        assertEquals(5, LogLevel.entries.size)
        assertEquals(LogLevel.NONE, LogLevel.entries[0])
        assertEquals(LogLevel.DEBUG, LogLevel.entries[4])
    }
}
