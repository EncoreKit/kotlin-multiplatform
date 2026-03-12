package com.encorekit.kmp

import cocoapods.EncoreKMPBridge.EncoreKMPBridge

actual class EncorePlacements internal constructor() {
    actual fun setClaimEnabled(enabled: Boolean) {
        EncoreKMPBridge.shared().setClaimEnabled(enabled)
    }
}
