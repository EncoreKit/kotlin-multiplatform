package com.encorekit.kmp

import com.encorekit.kmp.models.PresentationResult

class PlacementBuilder internal constructor(
    private val placementId: String?,
    private val platform: EncorePlatform,
) {
    suspend fun show(): PresentationResult = platform.show(placementId)
}
