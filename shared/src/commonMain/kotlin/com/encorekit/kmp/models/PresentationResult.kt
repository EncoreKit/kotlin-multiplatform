package com.encorekit.kmp.models

sealed class PresentationResult {
    /** User completed an offer flow (tapped CTA, opened link). */
    data class Granted(
        val offerId: String? = null,
        val campaignId: String? = null,
    ) : PresentationResult()

    /** User was not granted an offer. */
    data class NotGranted(
        val reason: String,
    ) : PresentationResult()
}
