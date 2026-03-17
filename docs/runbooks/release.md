# Release Runbook

## Overview

The KMP SDK publishes to Maven Central as `com.encorekit:encore-kmp`. The release script (`scripts/release/publish-release.sh`) handles version bumping, testing, tagging, and bundle creation.

## Native Dependency Management

Native SDK versions are centralized in `gradle/libs.versions.toml`:

```toml
[versions]
encore-android = "1.4.25"
encore-ios = "1.4.36"
```

Two readers, one source:
- **Gradle**: `libs.versions.encore.ios.get()` in `shared/build.gradle.kts`
- **CocoaPods**: Ruby regex in `iosHelper/EncoreKMPBridge.podspec`

To bump a native SDK version, either:
- Edit `libs.versions.toml` directly, or
- Trigger the `bump-native-sdk.yml` workflow (creates a PR)

## Release Steps

### Pre-Release Checklist

- [ ] On `main`, clean working tree, synced with remote
- [ ] All tests pass: `make test`
- [ ] Android demo builds: `make demo-android`
- [ ] iOS demo builds: `make demo-ios`
- [ ] Native SDK versions in `libs.versions.toml` are correct
- [ ] CHANGELOG or commit log reviewed

### Interactive Release

```bash
make release
```

The script walks through:

1. **Validate** — main branch, clean, synced
2. **Version** — reads current from `gradle.properties`, prompts for new (supports `patch`/`minor`/`major` shortcuts)
3. **Confirm** — shows changelog, asks for confirmation
4. **Test** — runs `./gradlew :shared:allTests`
5. **Update** — writes `VERSION_NAME` to `gradle.properties`
6. **Commit & Tag** — pushes to origin with `v{version}` tag
7. **Bundle** — builds Maven Central upload ZIP via `publishToMavenLocal`
8. **Upload** — pauses for manual Sonatype upload

### Manual Sonatype Upload

1. Go to https://central.sonatype.com
2. Click "Publish Component"
3. Deployment name: `encore-kmp-{version}`
4. Upload the bundle ZIP from `.build/`
5. Click "Publish"

## Bumping Native SDKs

### Via GitHub Actions (Preferred)

```bash
gh workflow run bump-native-sdk.yml -f sdk=android -f version=1.4.26
gh workflow run bump-native-sdk.yml -f sdk=ios -f version=1.4.37
```

Creates a PR updating `libs.versions.toml`. Both Gradle and podspec pick up the change automatically.

### Manually

Edit `gradle/libs.versions.toml` and commit. No other files need updating.

## Required Secrets (CI)

| Secret | Purpose |
|:-------|:--------|
| `GPG_SIGNING_KEY` | ASCII-armored GPG private key for Maven signing |
| `GPG_SIGNING_PASSWORD` | Passphrase for the GPG key |

Signing is optional for local development (guarded by `isRequired = signingKey.isNotBlank()`).

## Workflow Reference

| Workflow | Trigger | Purpose |
|:---------|:--------|:--------|
| `ci.yml` | PR + push to main | Tests, iOS build, demo build |
| `release.yml` | Tag `v*` | Validate, build, GitHub Release |
| `bump-native-sdk.yml` | Manual dispatch | Update native SDK version, create PR |
