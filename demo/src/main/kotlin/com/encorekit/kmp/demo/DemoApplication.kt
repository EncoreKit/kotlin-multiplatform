package com.encorekit.kmp.demo

import android.app.Application
import android.util.Log
import com.encorekit.kmp.Encore
import com.encorekit.kmp.configure
import com.encorekit.kmp.models.LogLevel
import com.encorekit.kmp.models.UserAttributes

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Encore.configure(
            context = this,
            apiKey = "pk_test_yr93u3skt5fgy99ame1pq3wx",
            logLevel = LogLevel.DEBUG,
        )

        Encore.onPurchaseRequest { request ->
            Log.d("EncoreKMPDemo", "Purchase requested: ${request.productId}")
        }

        Encore.onPurchaseComplete { result, placementId ->
            Log.d("EncoreKMPDemo", "Purchase complete: ${result.productId} for $placementId")
        }

        Encore.onPassthrough { placementId ->
            Log.d("EncoreKMPDemo", "Passthrough for placement: $placementId")
        }

        Encore.identify(
            userId = "demo_user_kmp_001",
            attributes = UserAttributes(
                email = "demo@example.com",
                firstName = "Demo",
                lastName = "User",
                countryCode = "US",
                subscriptionTier = "free",
                custom = mapOf("app_version" to "1.0.0", "platform" to "kmp"),
            ),
        )
    }
}
