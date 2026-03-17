# Encore KMP SDK

Kotlin Multiplatform SDK for [Encore](https://encorekit.com) — thin bridge delegating to native iOS and Android SDKs.

## Installation

### Gradle (KMP / Android)

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.encorekit:encore-kmp:0.1.0")
}
```

### iOS (CocoaPods)

The iOS target is distributed via the KMP cocoapods plugin. In your KMP project's `build.gradle.kts`:

```kotlin
cocoapods {
    pod("EncoreKit") { version = "1.4.36" }
}
```

## Quick Start

### Common (Shared Code)

```kotlin
import com.encorekit.kmp.Encore
import com.encorekit.kmp.models.PresentationResult

// Identify user
Encore.identify(userId = "user_123")

// Show placement
val result = Encore.placement("cancel_flow").show()
when (result) {
    is PresentationResult.Granted -> println("Offer accepted: ${result.offerId}")
    is PresentationResult.NotGranted -> println("Declined: ${result.reason.value}")
}
```

### Android Setup

```kotlin
// Application.onCreate()
Encore.configure(context = this, apiKey = "pk_xxx")

// Activity.onCreate()
Encore.setActivity(this)

// Activity.onDestroy()
Encore.setActivity(null)
```

### iOS Setup

```kotlin
// In common or iOS-specific code
Encore.configure(apiKey = "pk_xxx")
```

The iOS target requires CocoaPods. The `EncoreKMPBridge` pod (included in `iosHelper/`) wraps the Swift-only EncoreKit APIs for Kotlin/Native cinterop.

## Platform-Specific Setup

### Android

- **Context**: Pass `applicationContext` in `configure()` — required for the native SDK
- **Activity**: Set via `Encore.setActivity(activity)` — required for presenting offer UI
- **Min SDK**: 26

### iOS

- **CocoaPods**: The KMP cocoapods plugin generates a pod from the `shared` module
- **Deployment Target**: iOS 16.0
- **Bridge**: `iosHelper/EncoreKMPBridge.swift` provides `@objc` wrappers for cinterop

## API Reference

| Method | Description |
|:-------|:------------|
| `Encore.configure(apiKey, logLevel?)` | Initialize the SDK |
| `Encore.identify(userId, attributes?)` | Set current user |
| `Encore.setUserAttributes(attributes)` | Update user attributes |
| `Encore.reset()` | Clear user state |
| `Encore.placement(id?).show()` | Present an offer (suspend) |
| `Encore.placements.setClaimEnabled(bool)` | Enable/disable claim UI |
| `Encore.onPurchaseRequest { request -> }` | Handle purchase requests |
| `Encore.onPurchaseComplete { result, id -> }` | Handle purchase completions |
| `Encore.onPassthrough { placementId -> }` | Handle passthrough events |

## Development

```bash
make test           # Run all tests
make build          # Build all targets
make publish-local  # Publish to ~/.m2 for local testing
make demo-android   # Build Android demo APK
make demo-ios       # Build iOS demo for simulator
make release        # Interactive release flow
```

## Architecture

```
commonMain/          Public API (Encore object, models, expect classes)
androidMain/         Delegates to com.encorekit:encore (Maven Central)
iosMain/             Delegates to EncoreKMPBridge → EncoreKit (CocoaPods)
iosHelper/           Swift @objc bridge for cinterop
```

The SDK is a thin bridge (~600 LOC) with zero business logic — all behavior is delegated to the native SDKs.
