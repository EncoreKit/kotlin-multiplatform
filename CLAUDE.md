# Encore KMP SDK

Kotlin Multiplatform SDK: thin bridge delegating to native Android and iOS SDKs.

## Architecture

| Layer | Purpose |
|:------|:--------|
| **commonMain** | Public API facade (`Encore` object), models, expect classes |
| **androidMain** | Delegates to `com.encorekit:encore` (Maven Central) |
| **iosMain** | Delegates to `EncoreKMPBridge` → `EncoreKit` (CocoaPods) |
| **iosHelper/** | Swift `@objc` bridge for cinterop with EncoreKit |

### Data Flow

```
KMP commonMain API → expect/actual → Native SDK
                                   ← (callbacks via handlers)
```

### Key Patterns

- **Singleton facade**: `Encore` object in commonMain
- **Expect/actual**: `EncorePlatform` (internal), `EncorePlacements` (public)
- **PlacementBuilder**: commonMain class (NOT expect) wrapping `EncorePlatform.show()`
- **Android 2-step init**: `Encore.configure(context, apiKey)` + `Encore.setActivity(activity)`
- **iOS @objc bridge**: `EncoreKMPBridge.swift` wraps Swift-only APIs for cinterop
- **Stale continuation**: NSLock-guarded on iOS, cancelled on new show()/reset()

## Commands

```bash
make build          # Build all targets
make test           # Run all tests
make publish-local  # Publish to Maven Local
make clean          # Clean build
```

## Key Principles

- **Never crash host apps**: All boundary guards return safe fallbacks
- **Thin bridge only**: Zero business logic in KMP layer
- **Platform parity**: Same `Encore.placement(id).show()` API across all SDKs
