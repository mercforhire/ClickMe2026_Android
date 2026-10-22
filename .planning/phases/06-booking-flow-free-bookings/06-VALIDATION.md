---
phase: 6
slug: booking-flow-free-bookings
status: complete
nyquist_compliant: true
wave_0_complete: true
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

Filled at planning time: one row per plan task, with tests written first inside each `tdd="true"` task. The requirement-to-test map is in `06-RESEARCH.md` § Validation Architecture. All commands run with `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`. Targeted runs take about 30-60 s; rows that include `:app:assembleDevDebug` or the full suite take a few minutes. 06-14 sets the final statuses.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 06-01-T1 | 06-01 | 1 | BOOK-02, BOOK-03, BOOK-04 | T-06-01, T-06-03 | Lenient DTOs; exact strict-schema body; null client_notes omitted | unit (decoding) | `./gradlew :app:testDevDebugUnitTest --tests '*ResponseDecodingTest'` | extend | ✅ green |
| 06-01-T2 | 06-01 | 1 | BOOK-02, BOOK-03, BOOK-04 | T-06-02, T-06-04 | New paths carry the bearer; topic_id omitted when null; 409/422 map to ErrorCode | MockWebServer | `./gradlew :app:testDevDebugUnitTest --tests '*ClickMeApiTest' --tests '*ApiRequestUrlsTest'` | extend | ✅ green |
| 06-02-T1 | 06-02 | 1 | BOOK-04 | T-06-06 | replaceTop refuses a tab root; unknown tabs rejected | unit | `./gradlew :app:testDevDebugUnitTest --tests '*NavigatorTest'` | extend | ✅ green |
| 06-02-T2 | 06-02 | 1 | BOOK-04 | T-06-07, T-06-08 | Back from Request Sent never reaches the booking form (D-12) | unit | `./gradlew :app:testDevDebugUnitTest --tests '*BookingFlowNavigationTest'` | new | ✅ green |
| 06-03-T1 | 06-03 | 1 | BOOK-01 | — | D-06 bounds (no earlier month, no past day) | unit (pure) | `./gradlew :app:testDevDebugUnitTest --tests '*BookingCalendarTest'` | new | ✅ green |
| 06-03-T2 | 06-03 | 1 | BOOK-02 | T-06-09, T-06-11 | Invalid expert zone does not throw; raw IANA id never shown | unit (pure) | `./gradlew :app:testDevDebugUnitTest --tests '*BookingTimezoneNoteTest'` | new | ✅ green |
| 06-03-T3 | 06-03 | 1 | BOOK-01, BOOK-02 | T-06-10 | Notes cap never splits a surrogate pair | unit (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*BookingFormattingTest'` | new | ✅ green |
| 06-04-T1 | 06-04 | 2 | BOOK-02 | T-06-12 | Duplicate slots merged conservatively; held/unavailable/past unselectable | unit (pure, golden zones) | `./gradlew :app:testDevDebugUnitTest --tests '*BookingSlotsTest'` | new | ✅ green |
| 06-04-T2 | 06-04 | 2 | BOOK-03 | T-06-13, T-06-14, T-06-15 | Server text only as plain Text; ACCOUNT_SUSPENDED silent | unit | `./gradlew :app:testDevDebugUnitTest --tests '*BookingErrorsTest'` | new | ✅ green |
| 06-04-T3 | 06-04 | 2 | BOOK-01, BOOK-03 | T-06-14 | Paid topic disables Book Now (display gate) | unit | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingUiStateTest'` | new | ✅ green |
| 06-05-T1 | 06-05 | 2 | BOOK-04 | T-06-16 | Detail mapping; end-time fallback | unit | `./gradlew :app:testDevDebugUnitTest --tests '*BookingRequestSentViewModelTest'` | new | ✅ green |
| 06-05-T2 | 06-05 | 2 | BOOK-04 | T-06-16, T-06-17 | 404 (not owned) ends in Failed; latest-wins Retry | unit | `./gradlew :app:testDevDebugUnitTest --tests '*BookingRequestSentViewModelTest'` | new | ✅ green |
| 06-06-T1 | 06-06 | 3 | BOOK-01 | T-06-21 | SavedStateHandle with assisted injection compiles and works in a Nav3 entry; only primitives saved | unit + Robolectric + assemble | `./gradlew :app:testDevDebugUnitTest --tests '*EntrySavedStateHandleTest' --tests '*MakeABookingViewModelTest' :app:assembleDevDebug` | new | ✅ green |
| 06-06-T2 | 06-06 | 3 | BOOK-01, BOOK-02 | T-06-19, T-06-20, T-06-22 | Stale month cannot overwrite; restored slot re-validated; past slots rejected | unit | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingViewModelTest'` | new | ✅ green |
| 06-07-T1 | 06-07 | 3 | BOOK-01 | T-06-23 | Topic and expert text rendered as plain Text | Compose (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingTopicCardsTest'` | new | ✅ green |
| 06-07-T2 | 06-07 | 3 | BOOK-01 | T-06-23, T-06-24 | Paid topics listed (D-01), radio semantics | Compose (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingTopicCardsTest'` | new | ✅ green |
| 06-08-T1 | 06-08 | 3 | BOOK-01 | T-06-25 | Past days not clickable; previous month disabled on current month | Compose (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingCalendarSlotsTest'` | new | ✅ green |
| 06-08-T2 | 06-08 | 3 | BOOK-02 | T-06-25, T-06-26, T-06-27 | Unavailable/past chips disabled; note never shows raw zone id | Compose (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingCalendarSlotsTest'` | new | ✅ green |
| 06-09-T1 | 06-09 | 3 | BOOK-01 | T-06-30 | Both meeting types offered; notes counter | Compose (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingFormCardsTest'` | new | ✅ green |
| 06-09-T2 | 06-09 | 3 | BOOK-03 | T-06-28, T-06-29 | In-flight taps ignored; paid disabled with note | Compose (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingFormCardsTest'` | new | ✅ green |
| 06-10-T1 | 06-10 | 3 | BOOK-04 | T-06-31 | Expert name and topic as plain text; no payment wording | Compose (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*BookingRequestSentScreenTest'` | new | ✅ green |
| 06-10-T2 | 06-10 | 3 | BOOK-04 | T-06-32, T-06-33 | Buttons disabled while transparent; animation once | Compose (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*BookingRequestSentScreenTest'` | new | ✅ green |
| 06-11-T1 | 06-11 | 4 | BOOK-03 | T-06-34, T-06-35, T-06-37 | Single flight; past-slot guard; paid topic never calls the API; exact body | unit | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingViewModelBookTest'` | new | ✅ green |
| 06-11-T2 | 06-11 | 4 | BOOK-03 | T-06-38 | 409 clears and reloads; 422 resets meeting type; suspended silent | unit | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingViewModelBookTest'` | new | ✅ green |
| 06-12-T1 | 06-12 | 5 | BOOK-01, BOOK-03 | T-06-39, T-06-40 | Back blocked while submitting; dialog text plain | Compose (Robolectric) | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingScreenTest'` | new | ✅ green |
| 06-12-T2 | 06-12 | 5 | BOOK-02, BOOK-03 | T-06-41 | Real VM: one request, onBooked once; 409 recovery; paid gate | Compose integration | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingScreenTest'` | new | ✅ green |
| 06-13-T1 | 06-13 | 6 | BOOK-03, BOOK-04 | T-06-42, T-06-43 | Booking replaced by Request Sent; single route registration | Compose + assemble | `./gradlew :app:testDevDebugUnitTest --tests '*HomeShellsTest' --tests '*BookingFlowNavigationTest' :app:assembleDevDebug` | extend | ✅ green |
| 06-13-T2 | 06-13 | 6 | BOOK-03 | T-06-43 | No dangling stub references | full dev suite | full: `:app:testDevDebugUnitTest ktlintCheck :app:assembleDevDebug && bash scripts/check-design-rules.sh` | n/a (deletions) | ✅ green |
| 06-14-T1 | 06-14 | 7 | BOOK-01..04 | T-06-45, T-06-46, T-06-47 | R8 release builds; no secrets; no Phase 7 leak | full CI suite + sweeps | full suite command (Test Infrastructure) | n/a | ✅ green |
| 06-14-T2 | 06-14 | 7 | BOOK-01..04 | — | Deferred device checks logged | doc checks | `grep -c 'nyquist_compliant: true' 06-VALIDATION.md` etc. (plan 06-14) | n/a | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Wave 0 work is folded into the tasks that need it (tests first inside each `tdd="true"` task), so no separate scaffold plan exists:

- [x] `FakeClickMeApi`: add `expertAvailabilityHandler`, `requestBookingHandler`, `clientBookingDetailHandler` and call counters (06-01-T2, Wave 1)
- [x] `BookingSlotsTest`, `BookingCalendarTest`, `BookingTimezoneNoteTest`: pure-logic golden tests, written before the UI (06-04-T1, 06-03-T1, 06-03-T2)
- [x] `MakeABookingViewModelTest`, `BookingRequestSentViewModelTest`: fake API plus `MutableClock` (06-06, 06-05; plus `MakeABookingViewModelBookTest` in 06-11)
- [x] Compose tests for the booking screen, subviews and Request Sent (06-07, 06-08, 06-09, 06-10, 06-12)
- [x] Delete `MakeABookingStubScreenTest`; rewrite the booking test in `HomeShellsTest` (06-13)
- [x] Compile check for `SavedStateHandle` + `@AssistedInject` (06-06-T1: `EntrySavedStateHandleTest` + `assembleDevDebug`, with the `rememberSaveable` fallback)
- Framework install: none (existing infrastructure)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Android client books a free topic; iOS expert receives the `booking_request` push | BOOK-03, BOOK-04 | Needs a real iOS expert device and a backend push | Set the iOS expert's timezone different from the Samsung device. Book a free topic on Android, confirm Request Sent, and confirm the request arrives on iOS. Deferred per project preference. |
| Cross-timezone slot placement on a device | BOOK-02 | Needs a live expert in another timezone | Compare the slot days and times on Android against the iOS expert's own availability view |
| Visual and system behavior (200% font scale, keyboard over notes, rotation and "Don't keep activities" restore, Request Sent animation feel and animations off, Back ignored mid-request) | BOOK-01, BOOK-04 | Needs a device; SavedStateHandle with assisted injection is only proven in the JVM (research A1), animator scale only on a device (A2) | Run the checklist that 06-14 writes to STATE.md under `[Phase 6] DEFERRED (human)`. Deferred per project preference. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** 2026-10-22 - automated gate green; manual checks deferred
