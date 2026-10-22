---
phase: 6
slug: booking-flow-free-bookings
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-10-22
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 + Compose UI test (`createComposeRule` under `RobolectricTestRunner`) |
| **Config file** | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| **Full suite command** | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease && bash scripts/check-design-rules.sh` |
| **Estimated runtime** | ~30 s targeted; several minutes full |

---

## Sampling Rate

- **After every task commit:** Run the targeted `--tests` pattern for the files touched
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck && bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite must be green on both flavors, including `assembleProdRelease`
- **Max feedback latency:** ~60 seconds

---

## Per-Task Verification Map

Filled at planning time: one row per plan task, with tests written first inside each `tdd="true"` task. The requirement-to-test map is in `06-RESEARCH.md` § Validation Architecture.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `FakeClickMeApi`: add `expertAvailabilityHandler`, `requestBookingHandler`, `clientBookingDetailHandler` and call counters
- [ ] `BookingSlotsTest`, `BookingCalendarTest`, `BookingTimezoneNoteTest`: pure-logic golden tests, written before the UI
- [ ] `MakeABookingViewModelTest`, `BookingRequestSentViewModelTest`: fake API plus `MutableClock`
- [ ] Compose tests for the booking screen, subviews and Request Sent
- [ ] Delete `MakeABookingStubScreenTest`; rewrite the booking test in `HomeShellsTest`
- [ ] Compile check for `SavedStateHandle` + `@AssistedInject`
- Framework install: none (existing infrastructure)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Android client books a free topic; iOS expert receives the `booking_request` push | BOOK-03, BOOK-04 | Needs a real iOS expert device and a backend push | Set the iOS expert's timezone different from the Samsung device. Book a free topic on Android, confirm Request Sent, and confirm the request arrives on iOS. Deferred per project preference. |
| Cross-timezone slot placement on a device | BOOK-02 | Needs a live expert in another timezone | Compare the slot days and times on Android against the iOS expert's own availability view |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
