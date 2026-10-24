---
phase: 9
slug: voice-call
status: planned
nyquist_compliant: false
wave_0_complete: false
created: 2026-10-24
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + Compose UI test (BOM 2026.09.00) + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 |
| **Config file** | `app/build.gradle.kts` (`unitTests.isIncludeAndroidResources = true`); no `androidTest` source set |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDevDebugUnitTest --tests "com.uptrendinvestments.clickmeapp.managers.CallCenter*" --tests "*CallTimerTest" --tests "*JoinWindowTest"` |
| **Full suite command** | `JAVA_HOME=... ./gradlew testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug assembleProdDebug && bash scripts/check-design-rules.sh` (lint + R8 in a separate invocation after `./gradlew --stop`) |
| **Estimated runtime** | ~180 seconds (quick ~60s) |

---

## Sampling Rate

- **After every task commit:** Run the task's `<automated>` command (tests for touched files) + `ktlintCheck`
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` + `bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite must be green (plan 09-17)
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

`--tests` filters below run under `JAVA_HOME=... ./gradlew :app:testDevDebugUnitTest`.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 09-01-T1 | 01 | 1 | CALL-01, CALL-05, CALL-08 | T-09-02, T-09-04 | `AgoraConfig.toString` redacts token; null `join_url` decodes | unit | `--tests '*ResponseDecodingTest'` | ✅ extend | ⬜ pending |
| 09-01-T2 | 01 | 1 | CALL-01, CALL-05, CALL-08 | T-09-01 | Exact strict bodies for confirm / end | unit (MockWebServer) | `--tests '*ClickMeApiTest' --tests '*ApiRequestUrlsTest'` | ✅ extend | ⬜ pending |
| 09-02-T1 | 02 | 1 | CALL-07 (copy for all) | — | — | resource merge | `:app:mergeDevDebugResources` | ❌ W0 | ⬜ pending |
| 09-02-T2 | 02 | 1 | CALL-07 | T-09-06 | — | unit + compose | `--tests '*CallTimerTest' --tests '*CallTimerLabelsTest'` | ❌ W0 | ⬜ pending |
| 09-03-T1 | 03 | 1 | CALL-01 | T-09-10 | Client window is UX only (no `canJoin`) | unit + compose | `--tests '*JoinWindowTest' --tests '*JoinClockTest' --tests '*JoinCallButtonTest'` | ❌ W0 | ⬜ pending |
| 09-03-T2 | 03 | 1 | CALL-01 | T-09-07, T-09-08, T-09-09 | Scheme allow-list, no `parseUri`, REQUIRE_NON_BROWSER | unit (Robolectric) | `--tests '*ExternalMeetingProviderTest' --tests '*ExternalLinkLauncherTest'` | ❌ W0 | ⬜ pending |
| 09-04-T1 | 04 | 1 | CALL-02..CALL-06 | T-09-11, T-09-13 | No credentials representable in service info; 60 s permission timeout | unit | `--tests '*CallSessionTest' --tests '*CallPermissionBrokerTest'` | ❌ W0 | ⬜ pending |
| 09-04-T2 | 04 | 1 | (fakes) | — | — | compile | `:app:compileDevDebugUnitTestKotlin` | ❌ W0 | ⬜ pending |
| 09-05-T1 | 05 | 2 | CALL-01, CALL-02, CALL-03, CALL-04 | T-09-15, T-09-16, T-09-18 | FGS only when visible and after the grant; no `/end` on failures | unit | `--tests '*CallCenterStartTest' --tests '*CallCenterPermissionTest' --tests '*CallCenterGuardTest'` | ❌ W0 | ⬜ pending |
| 09-05-T2 | 05 | 2 | CALL-05, CALL-06 | T-09-14, T-09-17 | `call.ended` filtered by booking; confirm once | unit | `--tests 'com.uptrendinvestments.clickmeapp.managers.CallCenter*'` | ❌ W0 | ⬜ pending |
| 09-06-T1 | 06 | 2 | CALL-03, CALL-06 | — | — | unit | `--tests '*AgoraEventMappingTest'` | ❌ W0 | ⬜ pending |
| 09-06-T2 | 06 | 2 | CALL-03 | T-09-19, T-09-21 | No credential in log lines; destroy off main | build + grep | `:app:assembleDevDebug` + log grep | n/a | ⬜ pending |
| 09-07-T1 | 07 | 2 | CALL-03 | T-09-22, T-09-23 | Immutable PendingIntents, `exported=false`, private lock-screen version | unit (Robolectric) | `--tests '*CallNotificationFactoryTest' --tests '*CallManifestTest'` | ❌ W0 | ⬜ pending |
| 09-07-T2 | 07 | 2 | CALL-03 | T-09-24, T-09-25 | No credentials in extras; orphan service stops | unit + build | `--tests 'com.uptrendinvestments.clickmeapp.services.call.*'` + `:app:assembleDevDebug` | ❌ W0 | ⬜ pending |
| 09-08-T1 | 08 | 2 | CALL-07 | — | — | compose | `--tests '*MeetingCallHeroTest'` | ❌ W0 | ⬜ pending |
| 09-08-T2 | 08 | 2 | CALL-04 | — | — | compose | `--tests '*MeetingCallPillTest'` | ❌ W0 | ⬜ pending |
| 09-09-T1 | 09 | 2 | CALL-07, CALL-08 | — | — | compose | `--tests '*MeetingCallActionsTest'` | ❌ W0 | ⬜ pending |
| 09-09-T2 | 09 | 2 | CALL-01 | T-09-31, T-09-32 | Copy never counts as Open | compose | `--tests '*ExternalLinkActionsTest'` | ❌ W0 | ⬜ pending |
| 09-10-T1 | 10 | 2 | CALL-02, CALL-04 | T-09-33 | Settings intent targets own package only | compose | `--tests '*CallDialogsTest'` | ❌ W0 | ⬜ pending |
| 09-10-T2 | 10 | 2 | CALL-02 | T-09-34 | RECORD_AUDIO decisive; notification denial never blocks | unit + compose | `--tests '*MicPermissionLogicTest' --tests '*MicPermissionRequesterTest'` | ❌ W0 | ⬜ pending |
| 09-11-T1 | 11 | 3 | CALL-08 | T-09-36, T-09-37, T-09-38, T-09-39 | Logout teardown without `/end`; teardown in `finally` | unit | `--tests 'com.uptrendinvestments.clickmeapp.managers.CallCenter*'` (CallCenterEndTest) | ❌ W0 | ⬜ pending |
| 09-11-T2 | 11 | 3 | CALL-07 | — | — | unit | `--tests 'com.uptrendinvestments.clickmeapp.managers.CallCenter*'` (CallCenterCutoffTest) | ❌ W0 | ⬜ pending |
| 09-11-T3 | 11 | 3 | CALL-03, CALL-06 | T-09-40 | Hilt graph resolves, no cycle | unit + build | `--tests '*RealtimeConnectionManagerTest'` + `:app:assembleDevDebug` | ✅ extend | ⬜ pending |
| 09-12-T1 | 12 | 3 | CALL-07 | — | — | unit | `--tests '*MeetingCallUiStateTest'` | ❌ W0 | ⬜ pending |
| 09-12-T2 | 12 | 3 | CALL-04, CALL-08 | — | — | compose | `--tests '*MeetingCallScreenTest'` | ❌ W0 | ⬜ pending |
| 09-13-T1 | 13 | 4 | CALL-04 | — | — | unit | `--tests '*MeetingCallViewModelTest'` | ❌ W0 | ⬜ pending |
| 09-13-T2 | 13 | 4 | CALL-02, CALL-04 | T-09-44, T-09-45 | Back never ends; content hidden while expanded | compose | `--tests '*CallOverlayHostTest'` | ❌ W0 | ⬜ pending |
| 09-13-T3 | 13 | 4 | CALL-03 (D-10) | T-09-43 | Expand extra no-ops without a live call | unit + build | `--tests '*MainActivityIntentTest'` + `:app:assembleDevDebug` | ❌ W0 | ⬜ pending |
| 09-14-T1 | 14 | 4 | CALL-01, CALL-04, CALL-08 | T-09-47, T-09-48 | Cancel of the live booking blocked | unit + compose | `--tests 'com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.*'` | ✅ extend | ⬜ pending |
| 09-14-T2 | 14 | 4 | CALL-01 | T-09-49 | — | unit + compose | `--tests 'com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.*'` | ✅ extend | ⬜ pending |
| 09-15-T1 | 15 | 4 | CALL-01 | — | — | unit | `--tests '*ExploreViewModelTest'` | ✅ extend | ⬜ pending |
| 09-15-T2 | 15 | 4 | CALL-01 | T-09-51 | Join click does not navigate | compose | `--tests 'com.uptrendinvestments.clickmeapp.screens.clientmode.explore.*'` | ✅ extend | ⬜ pending |
| 09-16-T1 | 16 | 4 | CALL-04 | T-09-53 | Forced logout still works (UserManager unguarded) | unit | `--tests '*AppViewModelTest' --tests '*HomeClientViewModelTest' --tests '*HomeExpertViewModelTest' --tests '*SignupOverviewViewModelTest'` | ✅ extend / ❌ W0 (HomeExpert) | ⬜ pending |
| 09-16-T2 | 16 | 4 | CALL-04 | T-09-52, T-09-54 | No SetupIntent / cancel during the call | unit | `--tests '*PaymentMethodsViewModelTest' --tests '*PaymentMethodsScreenTest' --tests '*ClientCancellationViewModelTest'` | ✅ extend | ⬜ pending |
| 09-17-T1 | 17 | 5 | CALL-01..CALL-08 | T-09-55, T-09-56, T-09-58 | R8 keeps Agora; no credential logs; spike removed | full gate | full suite, then `lintDevDebug lintProdDebug`, then `assembleProdRelease` | n/a | ⬜ pending |
| 09-17-T2 | 17 | 5 | CALL-01..CALL-08 | T-09-57 | D-16 UAT tracked | docs grep | `grep '[Phase 9] DEFERRED (human)' .planning/STATE.md` | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

Sampling continuity: every task has an `<automated>` command; no three consecutive tasks lack automated verification.

---

## Wave 0 Requirements

Wave 0 is folded into the first task that needs each file (TDD order inside each plan); no separate scaffold plan.

- [ ] `T/testing/FakeCallEngine.kt`, `FakeCallForegroundController.kt`, `FakeCallPermissionGate.kt`, `FakeCallGuard.kt`, `FakeCallStarter.kt` — plan 09-04
- [ ] `FakeClickMeApi` handlers for `joinCall`, `confirmCallConnection`, `endCall` — plan 09-01
- [ ] `T/managers/CallTimerTest.kt` (09-02), `JoinWindowTest.kt` + `JoinClockTest.kt` (09-03), `CallSessionTest.kt` + `CallPermissionBrokerTest.kt` (09-04), `CallCenterHarness.kt` + `CallCenter{Start,Permission,Guard,Confirm,Peer}Test.kt` (09-05), `AgoraEventMappingTest.kt` (09-06), `CallCenter{End,Cutoff}Test.kt` (09-11)
- [ ] `T/screens/call/calling/CallTimerLabelsTest.kt` (09-02), `ExternalMeetingProviderTest.kt` (09-03), `subviews/MeetingCallHeroTest.kt` + `MeetingCallPillTest.kt` (09-08), `subviews/MeetingCallActionsTest.kt` + `ExternalLinkActionsTest.kt` (09-09), `CallDialogsTest.kt` + `MicPermissionLogicTest.kt` + `MicPermissionRequesterTest.kt` (09-10), `MeetingCallUiStateTest.kt` + `MeetingCallScreenTest.kt` (09-12), `MeetingCallViewModelTest.kt` + `CallOverlayHostTest.kt` (09-13)
- [ ] `T/services/call/CallNotificationFactoryTest.kt`, `CallManifestTest.kt`, `CallForegroundBridgeTest.kt` — plan 09-07
- [ ] `T/components/web/ExternalLinkLauncherTest.kt`, `T/components/call/JoinCallButtonTest.kt` — plan 09-03
- [ ] `T/MainActivityIntentTest.kt` — plan 09-13; `T/screens/expertmode/home/HomeExpertViewModelTest.kt` — plan 09-16
- [ ] No framework install needed

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Audio keeps flowing in background / screen off for 5+ min with persistent notification | CALL-03 | Needs real device, two parties, OS FGS policy | Join a call from Android with an iOS expert, background app, lock screen 5+ min, confirm two-way audio and notification (D-16 list in STATE, plan 09-17) |
| Full paid session end to end with iOS expert; `confirm-connection` exactly once server-side | CALL-05, CALL-08 | Needs iOS app + real paid booking | Run D-16 UAT; inspect backend logs for a single confirm-connection (paid part blocked on the test-mode Stripe backend) |

*Per memory: if no device available, log as open items rather than blocking.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter (plan 09-17 after the gate run)

**Approval:** pending (plan 09-17)
