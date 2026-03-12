package com.encorekit.kmp

import com.encorekit.encore.Encore as NativeEncore

actual class EncorePlacements internal constructor() {
    actual fun setClaimEnabled(enabled: Boolean) {
        NativeEncore.shared.placements.isClaimEnabled = enabled
    }
}
