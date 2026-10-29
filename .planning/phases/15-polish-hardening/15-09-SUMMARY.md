---
phase: 15-polish-hardening
plan: 09
subsystem: call
tags: [call, process-death, datastore, push-routing, tracer, hilt]
status: complete

requires:
  - phase: 09-voice-calls
    provides: CallCenter.session (memory-only live call), CallService
  - phase: 13-push
    provides: PushLaunchRequest / PushRoutingHost / PushDestinationResolver (BookingDetail with PushAudience.Either)
provides:
  - "CallResumeMarker: PII-free persisted in-call marker (booking id + scheduled end) that routes the next start to the booking detail"
  - "CallResumeNotice: memory one-shot (post / consumeFor) the two booking detail view models consume in 15-18"
  - "CallResumeModule: call_resume DataStore + SessionDataCleaner binding"
affects: [15-18]

commits: 2
plan_head_before: 283f0109be4ba4e6fb5984231ad407ad3bc15e88
plan_head_after: fa08901ef2f93cd1c4978b15763bed904f35299d

actuals:
  tokens: 4500
  tasks: 1
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Persisted marker chooses the landing screen only; routing reuses PushLaunchRequest -> PushRoutingHost"
    - "Qualified Preferences DataStore in its own file, registered as SessionDataCleaner via @Binds @IntoSet (abstract module + companion @Provides)"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/CallResumeModule.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallResumeMarker.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallResumeNotice.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallResumeMarkerTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt

key-decisions:
  - "The session collector does not skip its initial value: after the start-up resume step the first emission either clears an already empty store or re-marks a call that is still live (Activity recreation), so a live call stays protected across a later process kill"
  - "A session whose status is Ended, or whose connection type becomes SkypeZoom after the join response, clears the marker instead of keeping it"
  - "The marker is cleared before routing so a crash while routing cannot replay it"

patterns-established:
  - "DataStore tests: wait on the store / push slot (flow first {}) rather than advanceUntilIdle, because DataStore file work runs off the test scheduler"

requirements-completed: [POL-02]

duration: 40min
completed: 2026-10-29
---

# Phase 15 Plan 09: Call resume marker Summary

**A process kill during a live in-app call now routes the next start to that booking's detail (Join Call primary) through the existing push routing, flags a one-time resume note, and never joins or opens the mic.**

## Accomplishments

- `CallResumeMarker` writes only `call_resume_booking_id` and `call_resume_scheduled_end_ms` (fallback scheduled start + 2 h) while an in-app session is live, and clears them when the session ends, goes external, or on logout (`SessionDataCleaner`).
- At `start()` a stored marker is read once and cleared; with no live session and `now < scheduledEnd + 3600 s` it posts `CallResumeNotice` and `PushDestination.BookingDetail(id, PushAudience.Either)`. Stale or live-session markers route nothing.
- `ClickMeApplication` injects the marker and starts it after `pushRegistrationManager.start()`.
- No reference to the call engine, `startCall`, join or the permission gate exists in the marker (grep count 0).

## TDD

- RED (`c696bb2b`): `CallResumeMarkerTest` (8 tests) plus a compiling skeleton (no-op marker, real notice, DI module). 6 of 8 failed on the planned assertions (for example `resumePostsBookingDetailAndNotice`: expected `BookingDetail(b1, Either)`, was null). The JUnit XML was checked with `gsd-tools check tdd-red-evidence` and returned `RED_EVIDENCE_OK` (target ran, 6 failures, no build/fixture failure). Semantic assessment: the failures are assertion failures on the intended behaviour; `externalMeetingIsNotMarked` and `liveSessionNeverRoutes` passed vacuously against the no-op skeleton, which is expected for "nothing happens" cases.
- GREEN (`fa08901e`): marker implemented. The first GREEN run still failed 5 tests because the tests used `advanceUntilIdle()`, which returns before DataStore's off-scheduler file work lands. The tests (not the implementation) were changed to wait on the store and the push slot; all 8 then passed. No REFACTOR commit.

## Verification

- `testDevDebugUnitTest` for `CallResumeMarkerTest` (8/8), `CallCenterStartTest`, `CallCenterEndTest`, `PushRoutingHostTest`: pass (46 tests, 0 failures).
- `:app:assembleDevDebug` (Hilt graph) succeeds.
- `scripts/check-design-rules.sh`: "Design rules: OK".
- Acceptance greps: `PushAudience.Either` in the marker == 1; `startCall|joinChannel|requestPermission` == 0; `callResumeMarker.start()` in the application == 1.
- `ktlintCheck` exits non-zero, but only because of existing violations in files this plan does not touch (see Deferred Issues). The ktlint reports contain zero entries for the plan's files.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Test synchronization with DataStore**
- **Found during:** Task 1 GREEN run
- **Issue:** Tests that used `advanceUntilIdle()` asserted before the DataStore write or read had finished, giving false failures.
- **Fix:** Tests wait with `dataStore.data.first { ... }` and `push.destination.pending.first { it != null }`; "nothing happened" cases use a short real-time `settle()`.
- **Files modified:** CallResumeMarkerTest.kt
- **Commit:** fa08901e

**2. [Rule 3 - Blocking] Test fixture for the external meeting case**
- **Found during:** Task 1 GREEN run
- **Issue:** `externalJoinResponse(url = null)` makes the join fail and tears the session down, so the test never had a live SkypeZoom session.
- **Fix:** The join handler suspends on a `CompletableDeferred`, so the session stays at its SkypeZoom hint.
- **Commit:** fa08901e

Otherwise the plan was executed as written. The RED commit contains the notice and DI module in addition to the test because the skeleton had to compile.

## Deferred Issues

Existing ktlint violations that make `ktlintCheck` fail, outside this plan's files and not fixed (scope boundary):
- `screens/settings/accountsecurity/AccountSecurityViewModel.kt:208` (main source set)
- `screens/settings/clientprofilesettings/ClientProfileSettingsViewModelTest.kt:98`
- `screens/settings/deleteaccount/DeleteAccountViewModelTest.kt:35`

The plan's verify command (`... ktlintCheck ...`) therefore exits non-zero on this base commit for reasons unrelated to 15-09; the orchestrator or a later plan should clear these.

## Open Items (device checks deferred by the user)

- On a device: start an in-app call, kill the app from the recents screen, relaunch; expect the booking's detail (client Upcoming Booking Details or expert Booking Summary) with Join Call, no auto-rejoin and no microphone indicator. The resume note itself is rendered by 15-18.
- The routing depends on `PushRoutingHost` waiting for the session and shell; this was covered by the existing `PushRoutingHostTest` but not exercised end to end.

## Known Stubs

None.

## Threat Flags

None. The marker file holds the booking id and scheduled end only (T-15-19), is cleared at logout (T-15-20), and has no path to the engine, starter or permission gate (T-15-18).

## Self-Check: PASSED

- FOUND: CallResumeModule.kt, CallResumeMarker.kt, CallResumeNotice.kt, CallResumeMarkerTest.kt, ClickMeApplication.kt change
- FOUND commits: c696bb2b (test), fa08901e (feat)
