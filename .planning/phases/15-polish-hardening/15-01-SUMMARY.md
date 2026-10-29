---
phase: 15-polish-hardening
plan: 01
subsystem: ui
tags: [state-standard, snackbar, error-state, realtime, tracer]
status: complete

requires:
  - phase: 08-client-booking
    provides: MyBookingsViewModel load modes and realtime booking.update collector
provides:
  - RefreshFailedSnackbarEffect and SnackbarHostState.showRefreshFailed (shared Retry snackbar helper)
  - ErrorState(onRetry, modifier, heading, body, retryLabel) with defaults unchanged
  - EmptyState heading semantics and ErrorState polite live region
  - Shared strings common_refresh_failed and a11y_in_progress
  - MyBookingsViewModel.LoadMode.Realtime (failed realtime refetch flags refreshFailed)
affects: [15-02, 15-03, 15-04, 15-05, 15-06, 15-07, 15-08, 15-09, 15-10, 15-11, 15-12, 15-13, 15-14, 15-15, 15-16, 15-17, 15-18, 15-19, 15-20]

actuals:
  tokens: 6900
  tasks: 2
  commits: 2
plan_head_before: 283f0109be4ba4e6fb5984231ad407ad3bc15e88
plan_head_after: 14d8c1ad31c25409024e502c8e5e83a6b0405445

tech-stack:
  added: []
  patterns:
    - "Flag-and-consume refresh failure: ViewModel sets refreshFailed, screen calls RefreshFailedSnackbarEffect with onRetry = the pull-to-refresh action"
    - "LoadMode.Realtime = Quiet that flags a failure; tab-return reloads stay Quiet and silent"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/RefreshFailedSnackbar.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/RefreshFailedSnackbarTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/EmptyState.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/ControlsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreenTest.kt

key-decisions:
  - "Snackbar de-duplication compares the visible snackbar message with the new one: equal means silent, different means dismiss first, so at most one refresh-failed snackbar is ever visible"
  - "The snackbar is shown on a separate rememberCoroutineScope scope and onShown() runs immediately, so consuming the flag never cancels it (RESEARCH Pitfall 5)"
  - "onRetry is read through rememberUpdatedState so a recomposed lambda is used when Retry is tapped"
  - "My Bookings keeps its screen-specific refresh string (my_bookings_refresh_failed) and passes it as message (D-08)"

patterns-established:
  - "Rollout plans call RefreshFailedSnackbarEffect(refreshFailed, snackbarHostState, onRetry = onRefresh, onShown = consume) and never edit strings.xml"
  - "Converted first-load errors call ErrorState(heading = names the object, body = failure.toSectionMessage(), retryLabel = stringResource(R.string.section_retry))"

requirements-completed: [POL-01]

coverage:
  - id: D1
    description: "A failed My Bookings pull-to-refresh or realtime refetch keeps the rows and shows one Retry snackbar that re-runs the refresh"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "MyBookingsViewModelTest#realtimeFailureWithRowsFlagsRefreshFailed"
        status: pass
      - kind: unit
        ref: "MyBookingsScreenTest#refreshFailureShowsRetryThatRefreshes"
        status: pass
    human_judgment: false
  - id: D2
    description: "A quiet tab-return reload failure is silent; a realtime event during the first load stays Loud; concurrent triggers end latest-wins"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "MyBookingsViewModelTest#quietTabReturnFailureIsSilent, #realtimeDuringLoadingStaysLoud, #retryDuringRealtimeIsLatestWins"
        status: pass
    human_judgment: false
  - id: D3
    description: "At most one refresh-failed snackbar is visible, other snackbars are dismissed first, dismiss never retries"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "RefreshFailedSnackbarTest (5 tests)"
        status: pass
    human_judgment: false
  - id: D4
    description: "ErrorState accepts heading, body and retryLabel with unchanged defaults; EmptyState heading is a heading; ErrorState is a polite live region; text is not clamped"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "ControlsTest#errorStateDefaultsUnchanged, #errorStateNamedHeading, #emptyStateHeadingIsHeading, #errorStateIsPoliteLiveRegion, #longHeadingWraps"
        status: pass
    human_judgment: false
  - id: D5
    description: "TalkBack announcement and on-device look of the snackbar Retry and the live-region error state"
    requirement: POL-01
    verification: []
    human_judgment: true
    rationale: "Device and TalkBack behaviour is not asserted by Robolectric tests; device testing is deferred by the user"

duration: 38 min
completed: 2026-10-29
---

# Phase 15 Plan 01: State standard tracer on My Bookings Summary

**Shared "Couldn't refresh" Retry snackbar helper plus an extended ErrorState (named heading, body, "Retry"), proven end to end on My Bookings, with realtime refetch failures now flagged while tab-return reloads stay silent.**

## Performance

- **Duration:** 38 min
- **Tasks:** 2 of 2
- **Files:** 9 (2 created, 7 modified)

## Accomplishments

- `RefreshFailedSnackbar.kt`: `SnackbarHostState.showRefreshFailed` (silent when the same message is visible, dismisses any other snackbar first, `Long` duration, no dismiss action, calls `onRetry` only on `ActionPerformed`) and `RefreshFailedSnackbarEffect` (flag-and-consume, shows on a separate scope then calls `onShown()` at once).
- `MyBookingsViewModel`: new `LoadMode.Realtime`. A `booking.update` refetch with rows showing keeps them and flags `refreshFailed` on failure; during the first load it stays Loud. `onScreenShown` and launch-request reloads stay Quiet and silent (D-05).
- `MyBookingsScreen` uses `RefreshFailedSnackbarEffect(onRetry = onRefresh)`; Retry runs the same refresh as pull-to-refresh without resetting the tab or scroll.
- `ErrorState(onRetry, modifier, heading, body, retryLabel)` with the old copy as defaults (ExpertProfile and ClientProfileSettings callers and tests untouched); EmptyState heading carries `heading()`, ErrorState is a polite live region.
- `strings.xml`: `common_refresh_failed` and `a11y_in_progress` added (the phase's only edit of that file).
- No `ConnectivityManager` or `NetworkCallback` anywhere in `app/src/main` (D-06 guard: 0 files).

## Task Commits

1. **Task 1 (tracer): My Bookings Retry snackbar and Realtime load mode** - `e36ae274` (feat)
2. **Task 2: ErrorState named heading/body/retry label and semantics** - `14d8c1ad` (feat)

**Tracer gate:** the Task 1 `<verify>` tests (5 + 32 + 44 tests, 0 failures) were green before Task 2 started. Logged: Tracer verified end-to-end, expanding.

## Verification Results

- `RefreshFailedSnackbarTest` 5/5, `MyBookingsViewModelTest` 32/32 (4 new), `MyBookingsScreenTest` 44/44 (1 new), `ControlsTest` 19/19 (5 new), `ExpertProfileScreenTest` and `ClientProfileSettingsScreenTest` green unchanged.
- All acceptance-criteria greps pass; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- ktlint: none of the files this plan touched are reported (checked via `ktlintMainSourceSetCheck ktlintTestSourceSetCheck`).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Test infeasible as written] `longHeadingWraps` cannot reach a wrap under Robolectric**
- **Found during:** Task 2
- **Issue:** Robolectric lays text out with a 1 px per character stand-in font, so even a 227 character heading measured 227 px wide on one line at the 328 px constraint; `lineCount > 1` from length alone never holds.
- **Fix:** The test heading contains two explicit line breaks (3 lines proves nothing clamps it) and additionally asserts `layoutInput.maxLines == Int.MAX_VALUE`, `overflow == Clip` and `softWrap` on both EmptyState and ErrorState headings. This covers the requirement (no max lines, no ellipsis) but not true width-based wrapping, which stays an on-device check.
- **Files modified:** `ControlsTest.kt`
- **Commit:** `14d8c1ad`

**2. [Plan gate - ktlint] The verify command's `ktlintCheck` fails on code outside this plan**
- **Found during:** Task 1 and 2 verification
- **Issue:** `ktlintCheck` is red on pre-existing violations unrelated to this plan: `AccountSecurityViewModel.kt:208`, `ClientProfileSettingsViewModelTest.kt:98`, `DeleteAccountViewModelTest.kt:35`. Gradle aborts before the tests would run when the two are combined in one invocation.
- **Fix:** Out of scope (scope boundary), not fixed. Ran the unit tests and the ktlint source-set checks separately and confirmed that no file touched here is flagged. Orchestrator or a later plan should clean these three files.
- **Files modified:** none

**Total deviations:** 2 (1 test adaptation, 1 pre-existing out-of-scope lint failure). **Impact:** none on behaviour; one coverage item moves to the device checklist.

### TDD note

Both tasks are `tdd="true"` inside a `type: execute` plan, so the plan-level gate sequence does not apply. Tests and implementation were written together and committed together per task (one `feat` commit each); a separate compile-failing `test(...)` RED commit was not produced because the new tests reference symbols that do not exist yet (build-failure RED is INVALID_RED). The new tests were run green before each commit.

## Issues Encountered

None beyond the deviations above.

## Known Stubs

None.

## Threat Flags

None. No new endpoints or trust-boundary surface. T-15-01 (event burst) mitigated: `showRefreshFailed` is silent while one is visible and `startLoad` cancels the in-flight load (covered by `secondFailureWhileVisibleIsSilent` and `retryDuringRealtimeIsLatestWins`).

## Open Items (device checks deferred by the user)

- On a device, force a failed pull-to-refresh and a failed realtime refetch (airplane mode plus a booking change) on My Bookings: confirm one snackbar "Couldn't refresh. Check your connection and try again." with "Retry", rows and tab unchanged, and Retry refreshes.
- With TalkBack: the ErrorState heading and body announce once; the EmptyState and ErrorState headings navigate as headings.
- At a narrow width and 200% font scale, a long ErrorState heading wraps without clipping.

## Next Phase Readiness

Ready for the rollout plans: the helper, the ErrorState extension and both shared strings exist, so later plans never touch `strings.xml` for these.

## Self-Check: PASSED

- FOUND: `RefreshFailedSnackbar.kt`, `RefreshFailedSnackbarTest.kt`
- FOUND commits: `e36ae274`, `14d8c1ad`
