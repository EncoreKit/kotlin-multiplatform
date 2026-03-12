package com.encorekit.kmp

import android.app.Activity
import android.content.Context
import com.encorekit.kmp.models.BillingPurchaseResult
import com.encorekit.kmp.models.LogLevel
import com.encorekit.kmp.models.PresentationResult
import com.encorekit.kmp.models.PurchaseRequest
import com.encorekit.kmp.models.UserAttributes
import com.encorekit.encore.Encore as NativeEncore
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

internal actual class EncorePlatform actual constructor() {

    private val native = NativeEncore.shared
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    internal var activity: Activity? = null

    @Volatile
    internal var context: Context? = null

    actual val placements: EncorePlacements = EncorePlacements()

    // -- Configuration --

    actual fun configure(apiKey: String, logLevel: LogLevel) {
        val ctx = context
        if (ctx == null) {
            println("[Encore KMP] configure(apiKey) called without Context. Use Encore.configure(context, apiKey) on Android.")
            return
        }
        native.configure(ctx, apiKey, logLevel.toNative())
    }

    fun configure(context: Context, apiKey: String, logLevel: LogLevel) {
        this.context = context
        native.configure(context, apiKey, logLevel.toNative())
    }

    // -- User Identity --

    actual fun identify(userId: String, attributes: UserAttributes?) {
        native.identify(userId, attributes?.toNative())
    }

    actual fun setUserAttributes(attributes: UserAttributes) {
        native.setUserAttributes(attributes.toNative())
    }

    actual fun reset() {
        native.reset()
    }

    // -- Placements --

    actual suspend fun show(placementId: String?): PresentationResult {
        val act = activity
        if (act == null) {
            return PresentationResult.NotGranted(reason = "no_activity")
        }
        val id = placementId ?: "placement_${UUID.randomUUID().toString().take(8)}"
        val nativeResult = native.placement(id).show(act)
        return nativeResult.toCommon()
    }

    // -- Delegate Handlers --

    actual fun onPurchaseRequest(handler: suspend (PurchaseRequest) -> Unit) {
        native.onPurchaseRequest { _, nativeRequest ->
            handler(nativeRequest.toCommon())
        }
    }

    actual fun onPurchaseComplete(handler: suspend (BillingPurchaseResult, String) -> Unit) {
        native.onPurchaseComplete { nativeResult, productId ->
            handler(nativeResult.toCommon(), productId)
        }
    }

    actual fun onPassthrough(handler: (placementId: String?) -> Unit) {
        native.onPassthrough { placementId ->
            handler(placementId)
        }
    }
}
