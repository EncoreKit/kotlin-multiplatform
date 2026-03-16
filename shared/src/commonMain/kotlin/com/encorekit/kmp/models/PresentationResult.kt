package com.encorekit.kmp.models

sealed class PresentationResult {
    /** User completed an offer flow (tapped CTA, opened link). */
    data class Granted(
        val offerId: String? = null,
        val campaignId: String? = null,
    ) : PresentationResult()

    /** User was not granted an offer. */
    data class NotGranted(
        val reason: NotGrantedReason,
    ) : PresentationResult()
}

enum class NotGrantedReason(val value: String) {
    USER_CLOSED("user_closed"),
    NO_OFFERS("no_offer_available"),
    ERROR("error"),
    UNKNOWN("unknown"),
}
