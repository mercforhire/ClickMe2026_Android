---
phase: 3
slug: login-password-recovery-realtime
status: draft
nyquist_compliant: false
wave_0_complete: false
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

Filled in by the planner/executor per task. The requirement → test map from RESEARCH.md §Validation Architecture is the source:

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| AUTH-01 | Validators + exact messages; silent until first submit | unit | `--tests '*AuthValidatorsTest' --tests '*LoginViewModelTest'` | ❌ W0 | ⬜ pending |
| AUTH-01 | 401 inline-only; 422 email inline; others dialog; routing | unit | `--tests '*LoginViewModelTest'` | ❌ rewrite | ⬜ pending |
| AUTH-01 | Login screen render + dialog | Robolectric Compose | `--tests '*LoginScreenTest'` | ❌ rewrite | ⬜ pending |
| AUTH-01/02 | Auth back stack (Forgot w/ email, Signup placeholder, Password Changed → Login) | unit/Compose | `--tests '*AuthNavigationTest'` | ❌ W0 | ⬜ pending |
| AUTH-02 | forgot/reset bodies, no Authorization header | MockWebServer | `--tests '*ClickMeApiTest' --tests '*ApiRequestUrlsTest'` | ✅ extend | ⬜ pending |
| AUTH-02 | Forgot VM / Set New Password VM | unit | `--tests '*ForgotPasswordViewModelTest' --tests '*SetNewPasswordViewModelTest'` | ❌ W0 | ⬜ pending |
| AUTH-02 | PasswordStrength.evaluate | unit | `--tests '*PasswordStrengthTest'` | ❌ W0 | ⬜ pending |
| RT-01 | URL derivation (wss, no /api) | unit | `--tests '*RealtimeUrlTest'` | ❌ W0 | ⬜ pending |
| RT-01 | Handshake auth, websocket-only, state, rejection mapping, reconnect | Robolectric + MockWebServer | `--tests '*RealtimeServiceProtocolTest'` | ❌ W0 | ⬜ pending |
| RT-02 | Manager lifecycle, Refreshed reconnect, INVALID_TOKEN single probe | unit | `--tests '*RealtimeConnectionManagerTest'` | ❌ W0 | ⬜ pending |
| RT-03 | Event decoding, redaction, flow semantics | unit | `--tests '*RealtimeEventDecoderTest'` | ❌ W0 | ⬜ pending |
| RT-03 (D-19) | Emit payloads | unit | `--tests '*RealtimeEmitsTest'` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `FakeClickMeApi.kt` — add `forgotPassword`/`resetPassword` handlers + counters (compile blocker)
- [ ] `FakeRealtimeConnection.kt`, `FakeProcessForeground.kt` — seam doubles
- [ ] Rewrite `LoginViewModelTest`, `LoginScreenTest` (assert temporary screen today)
- [ ] New test files listed in RESEARCH.md §Wave 0 Gaps
- [ ] Extend `ClickMeApiTest`, `ApiRequestUrlsTest`

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| iOS-triggered message / typing / booking update appear in Logcat `Realtime` | RT-01, RT-03 | Needs real backend + iOS device | Log in on Android (debug), trigger events from iOS, `adb logcat -s Realtime` |
| Reconnect within ~2 s after network drop and after forced token refresh | RT-01, RT-02 | Needs device networking | Toggle airplane mode; expire access token; watch connection-state logs |
| End-to-end password reset with real OTP email | AUTH-02 | Needs inbox | Forgot → enter emailed code → new password → log in |

*Deferred human checkpoints per D-20 and project manual-testing preference — logged as open items, non-blocking.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
