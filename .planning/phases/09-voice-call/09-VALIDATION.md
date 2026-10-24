---
phase: 9
slug: voice-call
status: draft
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

- **After every task commit:** Run the quick run command (tests for touched files) + `ktlintCheck`
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` + `bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| (filled by planner per task) | | | CALL-01..08 | | | | see RESEARCH.md § Validation Architecture → Phase Requirements → Test Map | | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/.../testing/FakeCallEngine.kt`, `FakeCallForegroundController.kt`, `FakeCallPermissionGate.kt` — scriptable ports
- [ ] `FakeClickMeApi` handlers for `joinCall`, `confirmCallConnection`, `endCall`
- [ ] `managers/CallTimerTest.kt`, `JoinWindowTest.kt`, `CallCenter*Test.kt` (Start, Confirm, Peer, End, Cutoff, Guard, Permission)
- [ ] `screens/call/calling/CallOverlayHostTest.kt`, `CallDialogsTest.kt`, `ExternalMeetingProviderTest.kt`
- [ ] `services/call/CallNotificationFactoryTest.kt`, `CallManifestTest.kt`
- [ ] `ExternalLinkLauncherTest.kt`
- [ ] No framework install needed

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Audio keeps flowing in background / screen off for 5+ min with persistent notification | CALL-03 | Needs real device, two parties, OS FGS policy | Join a call from Android with an iOS expert, background app, lock screen 5+ min, confirm two-way audio and notification |
| Full paid session end to end with iOS expert; `confirm-connection` exactly once server-side | CALL-05, CALL-08 | Needs iOS app + real paid booking | Run D-16 UAT; inspect backend logs for a single confirm-connection |

*Per memory: if no device available, log as open items rather than blocking.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
