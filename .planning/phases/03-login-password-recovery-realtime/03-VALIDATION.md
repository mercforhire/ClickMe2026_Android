---
phase: 3
slug: login-password-recovery-realtime
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-10-20
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 (sdk=35) + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 + Compose UI test under Robolectric |
| **Config file** | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| **Full suite command** | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug && bash scripts/check-design-rules.sh` |
| **Estimated runtime** | ~60 seconds quick / ~4 minutes full |

---

## Sampling Rate

- **After every task commit:** Run the quick command scoped to the touched test class
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck` + `bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite must be green, plus one `assembleProdRelease` R8 smoke
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

Final statuses after the Phase 3 gate (plan 03-12): 421 tests per flavor, 0 failures. Source map: RESEARCH.md §Validation Architecture.

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| AUTH-01 | Validators + exact messages; silent until first submit | unit | `--tests '*AuthValidatorsTest' --tests '*LoginViewModelTest'` | ✅ created | ✅ green |
| AUTH-01 | 401 inline-only; 422 email inline; others dialog; routing | unit | `--tests '*LoginViewModelTest'` | ✅ rewritten | ✅ green |
| AUTH-01 | Login screen render + dialog | Robolectric Compose | `--tests '*LoginScreenTest'` | ✅ rewritten | ✅ green |
| AUTH-01/02 | Auth back stack (Forgot w/ email, Signup placeholder, Password Changed → Login); AuthNavigationTest was split into these two classes | unit/Compose | `--tests '*AuthNavigatorTest' --tests '*AuthNavDisplayTest'` | ✅ created | ✅ green |
| AUTH-02 | forgot/reset bodies, no Authorization header | MockWebServer | `--tests '*ClickMeApiTest' --tests '*ApiRequestUrlsTest'` | ✅ extended | ✅ green |
| AUTH-02 | Forgot VM / Set New Password VM | unit | `--tests '*ForgotPasswordViewModelTest' --tests '*SetNewPasswordViewModelTest'` | ✅ created | ✅ green |
| AUTH-02 | PasswordStrength.evaluate | unit | `--tests '*PasswordStrengthTest'` | ✅ created | ✅ green |
| RT-01 | URL derivation (wss, no /api) | unit | `--tests '*RealtimeUrlTest'` | ✅ created | ✅ green |
| RT-01 | Handshake auth, websocket-only, state, rejection mapping, reconnect | Robolectric + MockWebServer | `--tests '*RealtimeServiceProtocolTest'` | ✅ created | ✅ green |
| RT-02 | Manager lifecycle, Refreshed reconnect, INVALID_TOKEN single probe | unit | `--tests '*RealtimeConnectionManagerTest'` | ✅ created | ✅ green |
| RT-03 | Event decoding, redaction, flow semantics | unit | `--tests '*RealtimeEventDecoderTest'` | ✅ created | ✅ green |
| RT-03 (D-19) | Emit payloads | unit | `--tests '*RealtimeEmitsTest'` | ✅ created | ✅ green |
| AUTH-01/02 | Shared auth components (fields, strength meter, buttons) | Robolectric Compose | `--tests '*AuthComponentsTest'` | ✅ created | ✅ green |
| AUTH-02 | Forgot Password screen render + error states | Robolectric Compose | `--tests '*ForgotPasswordScreenTest'` | ✅ created | ✅ green |
| AUTH-02 | Set New Password screen render + validation | Robolectric Compose | `--tests '*SetNewPasswordScreenTest'` | ✅ created | ✅ green |
| AUTH-02 | Password Updated / Password Changed static screens | Robolectric Compose | `--tests '*AuthStaticScreensTest'` | ✅ created | ✅ green |
| AUTH-01/02 | Auth navigator state and NavDisplay back handling | unit/Compose | `--tests '*AuthNavigatorTest' --tests '*AuthNavDisplayTest'` | ✅ created | ✅ green |
| RT-01/02 | Connection contracts (state, rejection mapping) | unit | `--tests '*RealtimeConnectionContractsTest'` | ✅ created | ✅ green |
| RT-03 | Debug Logcat logger redaction and release twin | Robolectric | `--tests '*RealtimeDebugLoggerTest'` | ✅ created | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `FakeClickMeApi.kt` — add `forgotPassword`/`resetPassword` handlers + counters (compile blocker)
- [x] `FakeRealtimeConnection.kt`, `FakeProcessForeground.kt` — seam doubles
- [x] Rewrite `LoginViewModelTest`, `LoginScreenTest` (assert temporary screen today)
- [x] New test files listed in RESEARCH.md §Wave 0 Gaps
- [x] Extend `ClickMeApiTest`, `ApiRequestUrlsTest`

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions | Status |
|----------|-------------|------------|-------------------|--------|
| iOS-triggered message / typing / booking update appear in Logcat `Realtime` | RT-01, RT-03 | Needs real backend + iOS device | Log in on Android (debug), trigger events from iOS, `adb logcat -s Realtime` | deferred (non-blocking) |
| Reconnect within ~2 s after network drop and after forced token refresh | RT-01, RT-02 | Needs device networking | Toggle airplane mode; expire access token; watch connection-state logs | deferred (non-blocking) |
| End-to-end password reset with real OTP email | AUTH-02 | Needs inbox | Forgot → enter emailed code → new password → log in | deferred (non-blocking) |

*Deferred human checkpoints per D-20 and project manual-testing preference — logged as open items, non-blocking.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] Nyquist compliance flag set in frontmatter

**Approval:** automated gate green 2026-10-20 (plan 03-12); device checks deferred (non-blocking)
