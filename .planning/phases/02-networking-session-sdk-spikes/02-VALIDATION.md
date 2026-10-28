---
phase: 2
slug: networking-session-sdk-spikes
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-10-20
validated: 2026-10-28
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 (new) |
| **Config file** | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| **Full suite command** | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug :spikes:stripe:assembleDebug` (`:spikes:agora` was deleted in 09-17, D-16) |
| **Estimated runtime** | ~35 seconds quick / ~4 minutes full |

---

## Sampling Rate

- **After every task commit:** Run the quick command scoped to the touched test class
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck`
- **Before `/gsd-verify-work`:** Full suite must be green and the manual checklists signed off
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

Filled in from the plans (task IDs are assigned by the planner). Requirement → test map from RESEARCH.md:

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| FND-03 | snake_case, `{data}` wrapper, PaginationMeta, unknown keys ignored | unit | `--tests '*ResponseDecodingTest'` | ✅ (62 tests) | ✅ green |
| FND-03 | Instant decode (micro/milli/none, Z/offset) | unit | `--tests '*InstantAdapterTest'` | ✅ (9) | ✅ green |
| FND-04 | Error shapes, unknown code → null, non-JSON body | unit | `--tests '*ApiErrorParserTest'` | ✅ (24) | ✅ green |
| FND-05 | needAuthToken table | unit | `--tests '*ApiRequestUrlsTest' --tests '*AuthHeaderInterceptorTest'` | ✅ (21 + 9) | ✅ green |
| FND-05 | Single-flight refresh, late 401, retry-once, refresh failure modes, 401 after successful refresh clears session | MockWebServer | `--tests '*TokenAuthenticatorTest' --tests '*NetworkStackTest'` | ✅ (13 + 6) | ✅ green |
| FND-06 | 403 ACCOUNT_SUSPENDED → handler once | MockWebServer | `--tests '*SuspensionInterceptorTest'` | ✅ (5) | ✅ green |
| FND-07 | Login persist, restore() matrix, cached UserProfileData | unit | `--tests '*UserManagerTest' --tests '*SessionStoreTest'` | ✅ (69 + 8) | ✅ green |
| FND-07 | AppViewModel routing / 5s cap / suspension dialog | unit | `--tests '*AppViewModelTest'` | ✅ (43) | ✅ green |
| FND-08 | Logout clears everything; logout buttons | unit + Compose | `--tests '*UserManagerTest' --tests '*HomeShellsTest'` | ✅ (69 + 56) | ✅ green |
| REL-03 | Signed prodRelease AAB | command | `./gradlew :app:bundleProdRelease && jarsigner -verify …aab` | n/a | manual-only: needs the upload keystore (02-05 Task 2, not created yet) |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] Version catalog + `app/build.gradle.kts`: mockwebserver3, datastore-preferences, security-crypto, moshi-kotlin-codegen (ksp)
- [x] `FakeSecureTokenStore`, temp-file DataStore helper, `MainDispatcherRule` in `app/src/test/.../testing/`
- [x] Test classes listed above
- [x] Rewrite `AppViewModelTest.kt` (it asserts the Phase 1 stub)
- [x] `settings.gradle.kts` includes `:spikes:stripe` (`:spikes:agora` included until 09-17 deleted it)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| EncryptedSharedPreferences round-trip + relaunch auto-login | FND-07 | Robolectric can't run Keystore-backed ESP | Log in on device, kill app, relaunch → lands in tabs |
| Release R8 smoke (login, relaunch, forced refresh, logout) | FND-05/07/08 | R8/Keystore on real build | Install signed prodRelease APK; use debug "invalidate access token" action in a debug build to force refresh |
| PaymentSheet SetupIntent card save + PaymentLauncher 3DS on connected account | FND-13 | Real Stripe SDK + 3DS UI | Run `:spikes:stripe` on device with checkpoint inputs; screenshot evidence |
| Agora audio in microphone FGS while backgrounded | FND-14 | Real native SDK + iOS peer | Spike superseded by Phase 9 `CallService` (09-17, D-16): run the Phase 9 background-audio device check instead. Original: run `:spikes:agora`, join with iOS peer, background app ≥60 s; `dumpsys activity services` + logcat evidence; also check on API 37 AVD |
| Signed prodRelease AAB + `jarsigner -verify` | REL-03 | Needs the upload keystore (02-05 Task 2) | Same as Phase 13 release checklist steps 3-4 |
| Play Console internal-track upload | REL-03 | External console | Upload AAB; if rollout blocked, save draft and log blocker in STATE.md |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** validated 2026-10-28 (`/gsd-validate-phase 2`). All automatable FND-03..FND-08 behaviors are covered by 12 test classes (325 tests, all green on `testDevDebugUnitTest`). FND-13, FND-14 and REL-03 are device/console-only and stay in Manual-Only; they remain open in the v1.0 milestone audit.

## Validation Audit 2026-10-28

| Metric | Count |
|---|---|
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |
| Tests run | 325 |
| Manual-only | 3 |
