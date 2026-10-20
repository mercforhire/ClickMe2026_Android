---
phase: 02-networking-session-sdk-spikes
plan: 05
subsystem: release-signing
tags: [release, signing, keystore, play-app-signing, gradle]
requires: [02-01]
provides:
  - "Conditional release signingConfig in app/build.gradle.kts (active only when keystore.properties exists)"
affects: [02-15, 02-16]
tech-stack:
  added: []
  patterns: ["providers.fileContents for config-cache-tracked optional secrets file"]
key-files:
  created: []
  modified: [app/build.gradle.kts]
key-decisions:
  - "Release signing is local only; CI (no keystore.properties) builds unsigned (D-15)"
requirements: [REL-03]
status: partial (human keystore step deferred)
completed: 2026-10-20
---

# Phase 2 Plan 05: Release Signing Summary

Conditional local-only release signing in `app/build.gradle.kts` via `providers.fileContents`; the no-keystore (CI) path is proven. Signed-AAB verification is deferred pending the user-created upload keystore.

## Tasks

| Task | Status | Commit |
|------|--------|--------|
| 1. Conditional release signing config | Done | 1932157 |
| 2. Generate upload key (checkpoint:human-action) | DEFERRED by user decision (2026-10-20: "defer manual testing for now, continue") | - |
| 3. Build and verify signed AAB | Partially done (non-keystore parts); signed verification DEFERRED | - |

## What was done

- Added `import java.util.Properties`, `keystorePropsText` (`providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties")).asText`) and `keystoreProps`.
- `android.signingConfigs.create("release")` guarded by `!keystoreProps.isEmpty`; `buildTypes.release` assigns `signingConfig` under the same guard. `optimization { }` block unchanged. `versionCode = 1`, `versionName = "0.1.0"` confirmed (D-17). Comment documents D-15/D-16. No property values are printed.

## Verification performed (no keystore present)

- `./gradlew ktlintCheck assembleDevDebug :app:bundleProdRelease --stacktrace` with no `keystore.properties`: BUILD SUCCESSFUL (108 tasks); configuration cache entry stored. This proves the CI path.
- Produced `app/build/outputs/bundle/prodRelease/app-prod-release.aab` is unsigned (`jarsigner -verify` reports "no manifest.") - as designed without a keystore.
- Acceptance greps: `providers.fileContents` = 1 (non-comment), `keystoreProps.isEmpty` = 2, `optimization {` = 1, `readText` (non-comment) = 0.
- `git ls-files | grep -cE '\.jks$|keystore\.properties'` = 0; `git check-ignore` confirms `keystore.properties`, `*.jks`, `*.keystore` are ignored (.gitignore lines 19-21).

## Deferred (human)

Required to finish REL-03's build half (T-02-14, T-02-15, T-02-16 mitigations depend on these):

1. **Task 2 - create the upload keystore (user only; passwords must never enter Claude's transcript):**
   - `mkdir -p ~/keys`, then run `"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -genkeypair -v -keystore ~/keys/clickme-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000` (DN: Uptrend Investments Inc. + country).
   - Create `/Users/LeonChen/Documents/ClickMe_Android/keystore.properties` with `storeFile=/Users/LeonChen/keys/clickme-upload.jks` (absolute), `storePassword=...`, `keyAlias=upload`, `keyPassword=...`.
   - Back up the `.jks` and both passwords in a password manager NOW (D-16). In Play Console later choose "Use a Google-generated key" for Play App Signing.
2. **Task 3 signed verification (after step 1):** copy `keystore.properties` into the build root (worktree: delete the copy afterward), run `./gradlew :app:bundleProdRelease`, then `jarsigner -verify app/build/outputs/bundle/prodRelease/app-prod-release.aab` must print "jar verified."; `keytool -printcert -jarfile <aab>` must show the upload certificate owner (record only owner line and SHA-256 fingerprint). Re-confirm `git ls-files | grep -cE '\.jks$|keystore\.properties'` prints 0.
3. Plan 02-15 rebuilds the uploaded AAB from finished Phase 2 code; this check is a pipeline check only. Plan 02-16 (first internal-track upload) needs the keystore to exist.

## Deviations from Plan

- Worktree base was behind the expected base commit (464746e); ran `git reset --hard 464746e` per the startup branch check (working tree was clean).
- Tasks 2 and 3 (signed part) deferred per explicit user decision; not a code deviation.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- app/build.gradle.kts modified and committed (1932157).
- No keystore.properties or .jks tracked; local.properties (gitignored) not committed.
