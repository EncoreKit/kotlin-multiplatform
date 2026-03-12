package com.encorekit.kmp

import android.app.Activity
import android.content.Context
import com.encorekit.kmp.models.LogLevel

/**
 * Android-specific configuration. Call in `Application.onCreate()`.
 *
 * ```kotlin
 * Encore.configure(applicationContext, "pk_xxx")
 * ```
 */
fun Encore.configure(context: Context, apiKey: String, logLevel: LogLevel = LogLevel.NONE) {
    platform.configure(context, apiKey, logLevel)
}

/**
 * Sets the current Activity for offer presentation.
 * Call in `Activity.onCreate()` and pass `null` in `onDestroy()`.
 *
 * ```kotlin
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     Encore.setActivity(this)
 * }
 *
 * override fun onDestroy() {
 *     Encore.setActivity(null)
 *     super.onDestroy()
 * }
 * ```
 */
fun Encore.setActivity(activity: Activity?) {
    platform.activity = activity
}
