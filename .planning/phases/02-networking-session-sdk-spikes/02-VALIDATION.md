---
phase: 2
slug: networking-session-sdk-spikes
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-10-20
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
| **Full suite command** | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug :spikes:stripe:assembleDebug :spikes:agora:assembleDebug` |
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
| FND-03 | snake_case, `{data}` wrapper, PaginationMeta, unknown keys ignored | unit | `--tests '*ResponseDecodingTest'` | ❌ W0 | ⬜ pending |
| FND-03 | Instant decode (micro/milli/none, Z/offset) | unit | `--tests '*InstantAdapterTest'` | ❌ W0 | ⬜ pending |
| FND-04 | Error shapes, unknown code → null, non-JSON body | unit | `--tests '*ApiErrorParserTest'` | ❌ W0 | ⬜ pending |
| FND-05 | needAuthToken table | unit | `--tests '*ApiRequestUrlsTest'` | ❌ W0 | ⬜ pending |
| FND-05 | Single-flight refresh, late 401, retry-once, refresh failure modes, 401 after successful refresh clears session | MockWebServer | `--tests '*TokenAuthenticatorTest'` | ❌ W0 | ⬜ pending |
| FND-06 | 403 ACCOUNT_SUSPENDED → handler once | MockWebServer | `--tests '*SuspensionInterceptorTest'` | ❌ W0 | ⬜ pending |
| FND-07 | Login persist, restore() matrix, cached UserProfileData | unit | `--tests '*UserManagerTest'` | ❌ W0 | ⬜ pending |
| FND-07 | AppViewModel routing / 5s cap / suspension dialog | unit | `--tests '*AppViewModelTest'` | ✅ rewrite | ⬜ pending |
| FND-08 | Logout clears everything; logout buttons | unit + Compose | `--tests '*UserManagerTest' --tests '*HomeShellsTest'` | ❌/✅ | ⬜ pending |
| REL-03 | Signed prodRelease AAB | command | `./gradlew :app:bundleProdRelease && jarsigner -verify …aab` | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Version catalog + `app/build.gradle.kts`: mockwebserver3, datastore-preferences, security-crypto, moshi-kotlin-codegen (ksp)
- [ ] `FakeSecureTokenStore`, temp-file DataStore helper, `MainDispatcherRule` in `app/src/test/.../testing/`
- [ ] Test classes listed above
- [ ] Rewrite `AppViewModelTest.kt` (it asserts the Phase 1 stub)
- [ ] `settings.gradle.kts` includes `:spikes:stripe`, `:spikes:agora`

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| EncryptedSharedPreferences round-trip + relaunch auto-login | FND-07 | Robolectric can't run Keystore-backed ESP | Log in on device, kill app, relaunch → lands in tabs |
| Release R8 smoke (login, relaunch, forced refresh, logout) | FND-05/07/08 | R8/Keystore on real build | Install signed prodRelease APK; use debug "invalidate access token" action in a debug build to force refresh |
| PaymentSheet SetupIntent card save + PaymentLauncher 3DS on connected account | FND-13 | Real Stripe SDK + 3DS UI | Run `:spikes:stripe` on device with checkpoint inputs; screenshot evidence |
| Agora audio in microphone FGS while backgrounded | FND-14 | Real native SDK + iOS peer | Run `:spikes:agora`, join with iOS peer, background app ≥60 s; `dumpsys activity services` + logcat evidence; also check on API 37 AVD |
| Play Console internal-track upload | REL-03 | External console | Upload AAB; if rollout blocked, save draft and log blocker in STATE.md |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
