# Encore KMP SDK — Implementation Plan

## Executive Summary

### Motivation

A key client uses Kotlin Multiplatform (KMP) as their cross-platform framework. KMP allows developers to write shared Kotlin business logic once and compile it to native Android and iOS targets. Since our existing cross-platform SDKs (Flutter, React Native) are thin bridges (~500-600 LOC each) that delegate 100% of business logic to our native SDKs, we can follow the same proven pattern to ship a KMP SDK with minimal effort and risk.

### Solution

Build `encore-kmp` as a thin bridge layer — no business logic, no error handling beyond boundary safety. The KMP shared module defines the public API in `commonMain`, with `actual` implementations that delegate to our existing native SDKs:

- **Android**: Direct Kotlin → Kotlin delegation to `com.encorekit:encore` (trivial)
- **iOS**: Kotlin/Native → Swift helper → `EncoreKit` CocoaPod (same pattern as Flutter iOS plugin)

The async purchase request flow uses the **implicit suspend/resume pattern** (like Flutter), not the explicit `completePurchaseRequest` pattern (like React Native). Developers write a normal `suspend` lambda — when it returns, the purchase succeeded; when it throws, it failed. No postback ceremony.

### Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Repo location** | `kotlin-multiplatform/` (monorepo root) | Cross-platform bridge, same tier as Flutter/RN — not an Android feature |
| **Maven artifact** | `com.encorekit:encore-kmp` | Consistent with `com.encorekit:encore` naming |
| **iOS distribution** | CocoaPods | Already used by all SDKs; SPM consumers would just use the native Swift SDK directly |
| **Kotlin version** | 2.0+ | K2 adoption is 95-99% among IntelliJ users; K2 compiler is required for stable KMP multiplatform support |
| **Async pattern** | Suspend/resume (implicit) | Flutter-proven; cleaner DX than explicit completion callbacks |
| **Error handling** | Boundary-only (don't crash) | Native SDKs own all error handling, analytics, and retry logic |

### Scope

Full API parity with Flutter and React Native SDKs. No features omitted from V1.

### Effort Estimate

| Phase | Effort | Notes |
|-------|--------|-------|
| Project scaffold + Gradle KMP | 1-2 days | Build config, targets, version catalog |
| commonMain API surface + models | 2-3 days | Public types, expect declarations |
| androidMain implementation | 1-2 days | Trivial Kotlin → Kotlin delegation |
| iosMain + Swift helper | 4-6 days | Continuation bridging, cinterop |
| Tests (unit + async flow) | 2-3 days | Including pause/resume tests |
| Example app (Android + iOS) | 2-3 days | Both platforms, full demo |
| CI/CD + distribution | 1-2 days | GitHub Actions, Maven publishing |
| Documentation | 1 day | README, CLAUDE.md, integration guide |
| **Total** | **~2-3 weeks** | |

---

## Architecture

### Dependency Graph

```
┌─────────────────────────────────────────────────────┐
│                    KMP Consumer App                  │
│         (commonMain Kotlin shared code)              │
└──────────────────────┬──────────────────────────────┘
                       │ depends on
┌──────────────────────▼──────────────────────────────┐
│               encore-kmp (shared module)             │
│                                                      │
│  commonMain:   Public API + models (expect)          │
│  androidMain:  actual → com.encorekit:encore         │
│  iosMain:      actual → EncoreKMPBridge.swift        │
└──────────┬───────────────────────────┬──────────────┘
           │                           │
┌──────────▼──────────┐   ┌───────────▼────────────┐
│  encore-android      │   │  EncoreKMPBridge.swift   │
│  (Maven: encore)     │   │  (Swift helper, ~150 LOC)│
│  Kotlin → Kotlin     │   │         │                 │
└──────────────────────┘   │  ┌──────▼───────────┐    │
                           │  │ encore-swift-sdk  │    │
                           │  │ (CocoaPods)       │    │
                           │  └──────────────────┘    │
                           └──────────────────────────┘
```

### Bridge Pattern

The KMP SDK follows the identical "thin bridge / dumb pipe" architecture established by Flutter and React Native:

- **Zero business logic** in the bridge layer
- **Zero error handling** beyond boundary safety (don't crash the host app)
- **All analytics, error reporting, retry logic, UI presentation** handled by native SDKs
- **Bridge code ≈ 500-700 LOC total** (comparable to Flutter at ~540 and RN at ~660)

### Async Purchase Request Flow

Uses the **implicit suspend pattern** (same as Flutter, superior to RN's explicit completion):

```
Developer writes:
  Encore.onPurchaseRequest { request ->     ← suspend lambda
      RevenueCat.purchase(request.productId) ← their async logic
  }                                          ← return = success, throw = failure

Android flow:
  Native SDK fires onPurchaseRequest
  → androidMain calls developer's suspend lambda directly
  → lambda returns/throws
  → native SDK resumes
  (No bridging needed — Kotlin → Kotlin)

iOS flow:
  Native SDK fires onPurchaseRequest
  → Swift helper receives it, creates CheckedContinuation, calls Kotlin/Native callback
  → iosMain wraps callback in suspendCancellableCoroutine
  → calls developer's suspend lambda
  → lambda returns/throws
  → iosMain calls Swift helper's success/failure method
  → Swift helper resumes CheckedContinuation
  → native SDK resumes
```

---

## Directory Structure

```
kotlin-multiplatform/
├── shared/                                 # KMP shared module
│   ├── build.gradle.kts                    # KMP plugin, targets, dependencies
│   └── src/
│       ├── commonMain/kotlin/com/encorekit/kmp/
│       │   ├── Encore.kt                   # Public facade (expect declarations)
│       │   ├── EncorePlacements.kt         # Placement-level config
│       │   ├── PlacementBuilder.kt         # Fluent builder → show()
│       │   └── models/
│       │       ├── LogLevel.kt
│       │       ├── PresentationResult.kt
│       │       ├── PurchaseRequest.kt
│       │       ├── BillingPurchaseResult.kt
│       │       └── UserAttributes.kt
│       ├── androidMain/kotlin/com/encorekit/kmp/
│       │   ├── EncoreAndroid.kt            # actual implementations
│       │   └── Mappers.kt                  # native ↔ common type mappers
│       ├── iosMain/kotlin/com/encorekit/kmp/
│       │   ├── EncoreIos.kt               # actual implementations
│       │   └── Mappers.kt                  # native ↔ common type mappers
│       ├── commonTest/kotlin/com/encorekit/kmp/
│       │   ├── ModelsTest.kt              # Model serialization + equality
│       │   └── ApiSurfaceTest.kt          # Verify public API completeness
│       └── androidTest/kotlin/com/encorekit/kmp/
│           ├── DelegationTest.kt          # Handler registration, replacement, reset survival
│           └── PurchaseFlowTest.kt        # Suspend/resume async flow tests
├── iosHelper/                              # Swift bridge for Kotlin/Native ↔ EncoreKit
│   ├── EncoreKMPBridge.swift              # ~150 LOC (mirrors EncoreFlutterPlugin.swift)
│   └── EncoreKMPBridge.podspec            # or included via cinterop def file
├── example/
│   ├── androidApp/                         # Android demo app
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/encorekit/kmpexample/
│   │       └── MainActivity.kt
│   ├── iosApp/                             # iOS demo app
│   │   ├── iosApp.xcodeproj/
│   │   └── iosApp/
│   │       ├── ContentView.swift
│   │       └── KMPExampleApp.swift
│   └── shared/                             # Shared demo logic (optional)
│       └── src/commonMain/kotlin/com/encorekit/kmpexample/
│           └── DemoEncoreSetup.kt
├── scripts/
│   ├── demo/
│   │   ├── setup-example.sh
│   │   ├── demo-android.sh
│   │   ├── demo-ios.sh
│   │   └── clean-example.sh
│   └── release/
│       └── publish-release.sh
├── .github/workflows/
│   ├── ci.yml                              # Test, build Android, build iOS
│   ├── release.yml                         # Tag → Maven Central + GitHub release
│   └── bump-native-sdk.yml                 # Workflow dispatch for version bumps
├── build.gradle.kts                        # Root build config
├── settings.gradle.kts                     # Module includes
├── gradle.properties                       # KMP settings
├── gradle/libs.versions.toml              # Version catalog
├── Makefile                                # Dev commands
├── CLAUDE.md                               # Agent guidelines
└── README.md                               # Integration guide
```

---

## Public API Surface (commonMain)

Full parity with Flutter and React Native SDKs.

### Encore (Facade)

```kotlin
object Encore {
    // -- Configuration --
    fun configure(apiKey: String, logLevel: LogLevel = LogLevel.NONE)
    fun identify(userId: String, attributes: UserAttributes? = null)
    fun setUserAttributes(attributes: UserAttributes)
    fun reset()

    // -- Placements --
    fun placement(id: String? = null): PlacementBuilder
    val placements: EncorePlacements

    // -- Delegation handlers --
    fun onPurchaseRequest(handler: suspend (PurchaseRequest) -> Unit)
    fun onPurchaseComplete(handler: (BillingPurchaseResult, String) -> Unit)
    fun onPassthrough(handler: (placementId: String?) -> Unit)
}
```

### PlacementBuilder

```kotlin
class PlacementBuilder {
    suspend fun show(): PresentationResult
}
```

### PresentationResult

```kotlin
sealed class PresentationResult {
    data class Granted(
        val offerId: String? = null,
        val campaignId: String? = null,
    ) : PresentationResult()

    data class NotGranted(
        val reason: String,
    ) : PresentationResult()
}
```

### PurchaseRequest

```kotlin
data class PurchaseRequest(
    val productId: String,
    val placementId: String,
    val promoOfferId: String? = null,
)
```

### BillingPurchaseResult

```kotlin
data class BillingPurchaseResult(
    val productId: String,
    val purchaseToken: String? = null,
    val orderId: String? = null,
    val transactionId: String? = null,
)
```

### UserAttributes

```kotlin
data class UserAttributes(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val state: String? = null,
    val countryCode: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val language: String? = null,
    val subscriptionTier: String? = null,
    val monthsSubscribed: String? = null,
    val billingCycle: String? = null,
    val lastPaymentAmount: String? = null,
    val lastActiveDate: String? = null,
    val totalSessions: String? = null,
    val custom: Map<String, String> = emptyMap(),
)
```

### LogLevel

```kotlin
enum class LogLevel {
    NONE, ERROR, WARN, INFO, DEBUG;
}
```

### EncorePlacements

```kotlin
class EncorePlacements {
    fun setClaimEnabled(enabled: Boolean)
}
```

---

## Platform Implementations

### androidMain (trivial — Kotlin → Kotlin)

Direct delegation to `com.encorekit.encore`. No bridging, no continuations, no ceremony:

```kotlin
// androidMain — EncoreAndroid.kt (sketch)
actual object Encore {
    private val native = com.encorekit.encore.Encore.shared

    actual fun configure(apiKey: String, logLevel: LogLevel) {
        native.configure(context, apiKey, logLevel.toNative())
    }

    actual fun onPurchaseRequest(handler: suspend (PurchaseRequest) -> Unit) {
        native.onPurchaseRequest { nativeRequest ->
            handler(nativeRequest.toCommon())
        }
    }

    actual suspend fun show(placementId: String?): PresentationResult {
        val nativeResult = native.placement(placementId).show(activity)
        return nativeResult.toCommon()
    }
    // ... remaining methods follow the same pattern
}
```

**Android context**: The Android SDK requires `Context` for `configure()` and `Activity` for `show()`. The KMP facade will need to accept these via an `init(context)` or platform-specific extension. This mirrors how the Flutter and RN plugins obtain context from their framework lifecycle.

### iosMain + Swift Helper

Two files working together:

**EncoreKMPBridge.swift** (~150 LOC) — Obj-C-visible Swift wrapper around `Encore.shared`:

```swift
// iosHelper/EncoreKMPBridge.swift (sketch)
@objc public class EncoreKMPBridge: NSObject {
    @objc public static let shared = EncoreKMPBridge()

    @objc public func configure(apiKey: String, logLevel: String) {
        Encore.shared.configure(apiKey: apiKey, logLevel: parseLogLevel(logLevel))
    }

    @objc public func show(placementId: String?,
                           onResult: @escaping (NSDictionary) -> Void,
                           onError: @escaping (NSError) -> Void) {
        Task { @MainActor in
            do {
                let result = try await Encore.shared.placement(placementId).show()
                onResult(serializeResult(result))
            } catch {
                onError(error as NSError)
            }
        }
    }

    @objc public func registerOnPurchaseRequest(
        callback: @escaping (NSDictionary) -> Void,
        onSuccess: @escaping () -> Void,
        onFailure: @escaping (NSError) -> Void
    ) {
        Encore.shared.onPurchaseRequest { purchaseRequest in
            try await withCheckedThrowingContinuation { continuation in
                // Store continuation, call Kotlin callback, resume on success/failure
            }
        }
    }
    // ... remaining methods
}
```

**EncoreIos.kt** (~150 LOC) — `actual` implementations using cinterop:

```kotlin
// iosMain — EncoreIos.kt (sketch)
actual object Encore {
    private val bridge = EncoreKMPBridge.shared()

    actual fun onPurchaseRequest(handler: suspend (PurchaseRequest) -> Unit) {
        bridge.registerOnPurchaseRequest(
            callback = { nativeDict ->
                // This fires when native SDK triggers a purchase
                // We need to run the handler and signal back
            },
            onSuccess = { /* resume continuation */ },
            onFailure = { error -> /* resume with exception */ }
        )
    }
}
```

---

## Test Plan

### Unit Tests (commonTest)

| Test | Description |
|------|-------------|
| `ModelsTest` | `PresentationResult`, `PurchaseRequest`, `UserAttributes`, `BillingPurchaseResult`, `LogLevel` construction and equality |
| `ApiSurfaceTest` | Verify all public API methods are callable (compile-time parity check) |

### Integration Tests (androidTest)

| Test | Description |
|------|-------------|
| `DelegationTest.handler_registration` | `onPurchaseRequest` stores handler on native SDK |
| `DelegationTest.handler_replacement` | Second `onPurchaseRequest` replaces the first |
| `DelegationTest.handlers_survive_reset` | Handlers persist after `reset()` |
| `DelegationTest.onPassthrough_stores_handler` | `onPassthrough` stores handler on native SDK |
| `DelegationTest.onPurchaseComplete_stores_handler` | `onPurchaseComplete` stores handler on native SDK |

### Async Purchase Flow Tests (androidTest) — **NEW, does not exist in any SDK**

| Test | Description |
|------|-------------|
| `PurchaseFlowTest.suspends_until_handler_completes` | Native fires purchase request → handler invoked → flow still suspended → handler returns → flow resumes |
| `PurchaseFlowTest.propagates_handler_exception` | Handler throws → native SDK receives failure signal |
| `PurchaseFlowTest.handler_receives_correct_data` | `productId`, `placementId`, `promoOfferId` passed through correctly |
| `PurchaseFlowTest.concurrent_requests_handled` | Stale request cancelled when new one arrives |
| `PurchaseFlowTest.cancellation_propagates` | Coroutine cancellation triggers cleanup, no leak |

### Backfill: Continuation Tests for Flutter and RN

These tests are **missing from both existing SDKs** and should be backfilled:

**Flutter** (`flutter-sdk/test/`):
- `purchase_flow_test.dart` — Mock `MethodChannel` to simulate: native fires `onPurchaseRequest` → Dart handler delays → handler returns → verify native received success response
- Test handler exception propagates as `FlutterError`
- Test handler timeout behavior

**React Native** (`encore-react-native/src/__tests__/`):
- `purchase_flow.test.ts` — Mock native module to simulate: `onPurchaseRequest` event fires → JS handler runs → `completePurchaseRequest` called → verify native module received success
- Test `completePurchaseRequest(false)` on handler failure
- Test stale deferred cleanup

---

## Distribution

### Maven Central

- **Coordinates**: `com.encorekit:encore-kmp:<version>`
- **Publishing**: `maven-publish` + `signing` plugins in `shared/build.gradle.kts`
- **Same pattern as `encore-android`**: PGP signing, Sonatype staging

### CocoaPods (iOS framework)

- The KMP shared module produces an iOS framework via `cocoapods` Gradle plugin
- Published as a CocoaPod alongside the Swift helper
- Consumer adds: `pod 'EncoreKMP'` (which transitively depends on `EncoreKit`)

### Native SDK Version Pinning

Single source of truth in `gradle/libs.versions.toml`:

```toml
[versions]
encore-android = "1.4.25"
encore-ios = "1.4.36"
kotlin = "2.0.21"

[libraries]
encore-android = { group = "com.encorekit", name = "encore", version.ref = "encore-android" }
```

iOS version pinned in the podspec or cinterop def file.

---

## CI/CD

### ci.yml (PRs to main)

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - run: make test

  build-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - run: ./gradlew :shared:assembleRelease

  build-ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew :shared:podPublishReleaseXCFramework
```

### release.yml (tag push)

- Build + sign shared module
- Publish to Maven Central (Sonatype)
- Publish CocoaPod for iOS framework
- Create GitHub Release with changelog

### bump-native-sdk.yml (workflow dispatch)

- Update `gradle/libs.versions.toml` with new native SDK versions
- Update podspec iOS dependency
- Open PR

---

## Makefile

```makefile
.PHONY: test build demo-android demo-ios clean release setup-example

test:
	./gradlew :shared:allTests

test-android:
	./gradlew :shared:testDebugUnitTest

build:
	./gradlew :shared:assemble

setup-example:
	scripts/demo/setup-example.sh

demo-android:
	scripts/demo/demo-android.sh

demo-ios:
	scripts/demo/demo-ios.sh

demo-all: demo-android demo-ios

clean:
	./gradlew clean

clean-example:
	scripts/demo/clean-example.sh

nuke: clean clean-example

release:
	scripts/release/publish-release.sh

dry-run:
	./gradlew :shared:publishToMavenLocal
```

---

## Example App

### Android (`example/androidApp/`)

Minimal Compose app demonstrating the full SDK flow:
- Configure with API key
- Identify user with attributes
- Show placement (with result display)
- Event log showing `onPurchaseRequest`, `onPurchaseComplete`, `onPassthrough`
- Reset button
- `onPurchaseRequest` handler with auto-complete (for demo) or RevenueCat integration

### iOS (`example/iosApp/`)

SwiftUI app consuming the KMP shared module:
- Same feature set as Android example
- Demonstrates the KMP → iOS bridge working end-to-end

### Shared (`example/shared/`)

Optional shared demo logic (configure, identify, attribute setup) to demonstrate the KMP value proposition — write once, run on both platforms.

---

## Implementation Phases

### Phase 1: Scaffold (1-2 days)
- [ ] Initialize Gradle KMP project with `build.gradle.kts`, `settings.gradle.kts`
- [ ] Set up `gradle/libs.versions.toml` with Kotlin 2.0+, native SDK versions
- [ ] Configure KMP targets: `androidTarget`, `iosArm64`, `iosSimulatorArm64`, `iosX64`
- [ ] Add `Makefile` with core targets
- [ ] Add `CLAUDE.md` with repo-specific guidelines
- [ ] Verify `./gradlew build` succeeds with empty sources

### Phase 2: commonMain API + Models (2-3 days)
- [ ] Define all model data classes: `PresentationResult`, `PurchaseRequest`, `UserAttributes`, `BillingPurchaseResult`, `LogLevel`
- [ ] Define `Encore` facade with `expect` declarations for platform-specific implementations
- [ ] Define `PlacementBuilder` and `EncorePlacements`
- [ ] Write `commonTest` model tests

### Phase 3: androidMain (1-2 days)
- [ ] Add `com.encorekit:encore` dependency to androidMain
- [ ] Implement all `actual` declarations as direct delegation
- [ ] Implement type mappers (native ↔ common)
- [ ] Handle Android `Context`/`Activity` lifecycle
- [ ] Write `androidTest` delegation tests

### Phase 4: iosMain + Swift Helper (4-6 days)
- [ ] Write `EncoreKMPBridge.swift` wrapping `Encore.shared` with Obj-C-visible API
- [ ] Set up cinterop def file pointing to the Swift helper's generated Obj-C headers
- [ ] Implement all `actual` declarations using cinterop calls to Swift helper
- [ ] Bridge `onPurchaseRequest` using `suspendCancellableCoroutine` + `CheckedContinuation`
- [ ] Bridge `show()` using callback → suspend conversion
- [ ] Add CocoaPods configuration for iOS framework output
- [ ] Verify end-to-end on iOS simulator

### Phase 5: Tests (2-3 days)
- [ ] Write `PurchaseFlowTest` — suspend/resume, exception propagation, cancellation
- [ ] Write `DelegationTest` — handler registration, replacement, reset survival
- [ ] Write `ApiSurfaceTest` — compile-time parity verification
- [ ] **Backfill**: Add `purchase_flow_test.dart` to Flutter SDK
- [ ] **Backfill**: Add `purchase_flow.test.ts` to React Native SDK
- [ ] Verify all tests pass: `make test`

### Phase 6: Example App (2-3 days)
- [ ] Set up `example/androidApp` with Compose UI
- [ ] Set up `example/iosApp` with SwiftUI
- [ ] Implement full demo flow: configure → identify → show → handlers → reset
- [ ] Add event log display for all callbacks
- [ ] Verify `make demo-android` and `make demo-ios` work

### Phase 7: Distribution + CI (1-2 days)
- [ ] Configure `maven-publish` + `signing` in `shared/build.gradle.kts`
- [ ] Configure CocoaPods plugin for iOS framework
- [ ] Write `.github/workflows/ci.yml`
- [ ] Write `.github/workflows/release.yml`
- [ ] Write `.github/workflows/bump-native-sdk.yml`
- [ ] Write `scripts/release/publish-release.sh`
- [ ] Test `make dry-run` (publishToMavenLocal)

### Phase 8: Documentation (1 day)
- [ ] Write `README.md` — installation, quick start, API reference, troubleshooting
- [ ] Write `CLAUDE.md` — repo structure, principles, key commands
- [ ] Write `docs/runbooks/distribution.md` and `docs/runbooks/release.md`

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Kotlin/Native ↔ Swift interop complexity | iOS bridge takes longer than estimated | Model closely on Flutter iOS plugin (`EncoreFlutterPlugin.swift`); same continuation pattern, same ~150 LOC |
| Android SDK Context/Activity lifecycle | KMP can't access Android framework types in commonMain | Use `expect`/`actual` for platform-specific initialization; document clearly in README |
| Swift SDK lacks `@objc` annotations | Swift helper needs Obj-C-visible API surface | Write thin Swift wrapper (same approach as Flutter plugin); no changes to core Swift SDK needed |
| Kotlin 2.0 requirement excludes some projects | Potential adoption blocker | K2 adoption is 95-99%; Kotlin 2.0 is required for stable KMP multiplatform support regardless |
| CocoaPods deprecation trend | Future maintenance burden | Monitor SPM adoption; CocoaPods still widely used and matches existing SDK infrastructure |

---

## Cross-SDK Parity Matrix

| API | Swift | Android | Flutter | RN | KMP (planned) |
|-----|-------|---------|---------|-----|---------------|
| `configure(apiKey, logLevel)` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `identify(userId, attributes)` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `setUserAttributes(attributes)` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `reset()` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `placement(id).show()` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `placements.setClaimEnabled(bool)` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `onPurchaseRequest(handler)` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `onPurchaseComplete(handler)` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `onPassthrough(handler)` | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Async purchase flow pattern** | native | native | implicit (suspend) | explicit (complete) | implicit (suspend) |

---

## Appendix: Native SDK Versions (at time of writing)

| SDK | Version | Distribution |
|-----|---------|-------------|
| `encore-swift-sdk` | 1.4.36 | CocoaPods (`EncoreKit`) |
| `encore-android` | 1.4.25 | Maven Central (`com.encorekit:encore`) |
| `encore-react-native` | 1.1.22 | npm (`@encorekit/react-native`) |
| `flutter-sdk` | 1.0.20 | pub.dev (`encore_flutter`) |
