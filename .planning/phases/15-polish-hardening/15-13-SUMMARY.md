---
phase: 15-polish-hardening
plan: 13
subsystem: ui
tags: [expert, dashboard, lists, refresh, snackbar-insets, large-screen, tracer]
status: complete

requires:
  - phase: 15-polish-hardening
    provides: "15-01 RefreshFailedSnackbarEffect / showRefreshFailed and the LoadMode.Realtime pilot; 15-02 contentMaxWidth and InsetsTestSupport"
provides:
  - ExpertDashboardViewModel.LoadMode.Realtime and ExpertBookingsViewModel.LoadMode.Realtime
  - Retry snackbar (Long, with action) on Expert Dashboard, Expert Bookings, Incoming Requests, Topics and Availability
  - Availability SnackbarHost above the navigation bar and the IME
  - 600 dp content cap on the five expert screens
affects: [15-14, 15-15, 15-16, 15-17, 15-18, 15-19, 15-20]

actuals:
  tokens: 7000
  tasks: 3
  commits: 3
plan_head_before: e808da6b9c3dd62717fc017104c562ebca5b1350
plan_head_after: 8627a58c76f5bce081a57051509434cb885395e4

tech-stack:
  added: []
  patterns:
    - "Flag-and-consume refresh failure on every expert list: RefreshFailedSnackbarEffect(onRetry = the pull-to-refresh action)"
    - "Enum-based snackbar (Topics) calls showRefreshFailed for the RefreshFailed kind only; result kinds keep a plain showSnackbar"
    - "Zero-inset Scaffold: SnackbarHost(state, Modifier.navigationBarsPadding().imePadding())"
    - "List items cap with Modifier.animateItem().contentMaxWidth(); scrolling columns cap with verticalScroll(...).contentMaxWidth()"

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScreenTest.kt

key-decisions:
  - "The Dashboard's payout-open-failed snackbar dropped its explicit SnackbarDuration.Short (the showSnackbar default is Short without an action), so the screen file no longer names Short and the acceptance grep is 0"
  - "Incoming Requests has no realtime collector, so only the pull-to-refresh failure gets the Retry action (as the plan states); no LoadMode.Realtime was added there"
  - "Time Range sheet needed no production change: start/end were already rememberSaveable and the open flag (editing) is a rememberSaveable Pair, so the restore test passed without edits"

patterns-established:
  - "Expert list screens pass message = stringResource(R.string.expert_refresh_failed) (same text as common_refresh_failed) to keep their screen-specific string, as My Bookings does"

requirements-completed: [POL-01, POL-02, POL-03]

coverage:
  - id: D1
    description: "A failed Dashboard or Expert Bookings pull-to-refresh or booking.update refetch keeps the content and flags refreshFailed; a tab-return reload stays silent; a realtime event during the first load stays Loud"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "ExpertDashboardViewModelTest#realtimeFailureFlags, #quietTabReturnFailureIsSilent, #realtimeDuringTheFirstLoadStaysLoud"
        status: pass
      - kind: unit
        ref: "ExpertBookingsViewModelTest#realtimeFailureFlags, #quietTabReturnFailureIsSilent, #realtimeDuringTheFirstLoadStaysLoud"
        status: pass
    human_judgment: false
  - id: D2
    description: "The refresh-failed snackbar shows Retry, stays up past 5 s (Long) and Retry re-runs the pull refresh once, on Dashboard, Expert Bookings, Incoming Requests, Topics and Availability"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "ExpertDashboardScreenTest#refreshFailedShowsRetry, ExpertBookingsScreenTest#refreshFailedShowsRetry, IncomingRequestsScreenTest#refreshFailedShowsRetry, TopicsScreenTest#refreshFailedShowsRetry, AvailabilityScreenTest#refreshFailedShowsRetry"
        status: pass
    human_judgment: false
  - id: D3
    description: "Topics' Added, Updated and Deleted snackbars keep no action"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "TopicsScreenTest#addedSnackbarKeepsNoAction, #updatedSnackbarKeepsNoAction, #deletedSnackbarKeepsNoAction"
        status: pass
    human_judgment: false
  - id: D4
    description: "Each expert list shows its skeleton on first load, its shipped empty heading and a named error with Retry; a failed earnings section degrades alone"
    requirement: POL-02
    verification:
      - kind: unit
        ref: "ExpertBookingsScreenTest#listCells, IncomingRequestsScreenTest#listCells, TopicsScreenTest#listCells, ExpertDashboardScreenTest#sectionFailureIsolated"
        status: pass
    human_judgment: false
  - id: D5
    description: "On the zero-inset Availability Scaffold the snackbar sits above the navigation bar and above the IME"
    requirement: POL-03
    verification:
      - kind: unit
        ref: "AvailabilityScreenTest#snackbarAboveNavBar (fails with the padding removed, passes with it)"
        status: pass
    human_judgment: false
  - id: D6
    description: "The Time Range sheet reopens with its selected times after state restoration"
    requirement: POL-03
    verification:
      - kind: unit
        ref: "AvailabilityScreenTest#timeRangeSheetSurvivesRestore"
        status: pass
    human_judgment: false
  - id: D7
    description: "Dashboard, Expert Bookings, Incoming Requests, Topics and Availability cap their content at 600 dp on a w900dp window"
    requirement: POL-02
    verification:
      - kind: unit
        ref: "ExpertDashboardScreenTest#dashboardCapped, ExpertBookingsScreenTest#capped, IncomingRequestsScreenTest#capped, TopicsScreenTest#capped, AvailabilityScreenTest#capped"
        status: pass
    human_judgment: false
  - id: D8
    description: "On-device look of the Retry snackbar above the real gesture bar and keyboard, TalkBack announcement, and the cap on a real tablet or resized window"
    requirement: POL-03
    verification: []
    human_judgment: true
    rationale: "Robolectric asserts bounds, not rendering; device testing is deferred by the user"

duration: 70 min
completed: 2026-10-29
---

# Phase 15 Plan 13: Expert lists to the refresh standard Summary

**Retry snackbar on all five expert list screens (Dashboard, Expert Bookings, Incoming Requests, Topics, Availability), realtime refetch failures flagged on Dashboard and Expert Bookings, the Availability snackbar lifted above the nav bar and keyboard, and a 600 dp content cap on each.**

## Performance

- **Duration:** about 70 min
- **Tasks:** 3 of 3
- **Files:** 14 modified (7 main, 7 test), 569 insertions, 53 deletions

## Accomplishments

- `ExpertDashboardViewModel` and `ExpertBookingsViewModel`: new `LoadMode.Realtime`. A `booking.update` refetch with content showing keeps it and flags `refreshFailed`; during the first load it stays Loud; `onScreenShown` and launch-request reloads stay Quiet and silent (D-05). Copied from the My Bookings pilot.
- Dashboard, Expert Bookings, Incoming Requests, Availability: the action-less effects (Short on Dashboard and Availability, Long on the other two) are now `RefreshFailedSnackbarEffect(onRetry = onRefresh)`, Long, with "Retry" that re-runs the pull refresh.
- Topics: the `TopicsSnackbar.RefreshFailed` branch calls `showRefreshFailed(message, retryLabel) { onRefresh }`; Added, Updated and Deleted keep a plain Short snackbar with no action.
- Availability: `SnackbarHost(state, Modifier.navigationBarsPadding().imePadding())` because the Scaffold has zero content insets. The test `snackbarAboveNavBar` was checked by mutation: it fails with the padding removed and passes with it.
- `contentMaxWidth()` on the five screens. Dashboard and Availability apply it to the scrolling column after `verticalScroll` (the scroll area and pull-to-refresh stay full width); the three lists apply `Modifier.animateItem().contentMaxWidth()` per card. App bars, tabs and empty/error states are untouched.

## Task Commits

1. **Task 1 (tracer): Dashboard Realtime mode, Retry snackbar, width cap** - `b5fc7f90` (feat)
2. **Task 2: Expert Bookings and Incoming Requests** - `1ad0a52b` (feat)
3. **Task 3: Topics and Availability** - `8627a58c` (feat)

**Tracer gate:** the Task 1 `<verify>` tests (57 tests, 0 failures) and `ktlintCheck` were green before Task 2 started. Tracer verified end to end, expanding.

## State audit (per screen)

| Screen | Archetype | Error block | Cells tested |
|--------|-----------|-------------|--------------|
| Expert Dashboard | composite | compliant (`EmptyState` "Couldn't load dashboard" + body + Retry, polite live region) | skeleton (existing), error + Retry (existing `hardFailureShowsTheErrorStateAndRetryRecovers`), new `sectionFailureIsolated`, `refreshFailedShowsRetry`, `dashboardCapped` |
| Expert Bookings | list | compliant ("Couldn't load sessions" + Retry, polite live region) | `listCells` (skeleton, error + Retry, empty Upcoming and Past headings), `refreshFailedShowsRetry`, `capped` |
| Incoming Requests | list | compliant ("Couldn't load requests" + Retry, polite live region) | `listCells` (skeleton, error + Retry, "No pending requests"), `refreshFailedShowsRetry`, `capped` |
| Topics | list | compliant (`TopicsErrorContent`, "Couldn't load topics" + body + Retry) | `listCells`, `refreshFailedShowsRetry`, result-snackbar no-action tests, `capped` |
| Availability | form | compliant ("Couldn't load availability" + body + Retry) | `refreshFailedShowsRetry`, `snackbarAboveNavBar`, `timeRangeSheetSurvivesRestore`, `capped` |

No screen needed conversion to the shared `ErrorState`: all five already name the object and offer Retry (D-08). The Topics and Availability blocks are not live regions; the shared `ErrorState` is. Left as is under the audit rule; a later pass can switch them if the a11y audit asks for it.

## Verification Results

- Task 1: `ExpertDashboardViewModelTest` + `ExpertDashboardScreenTest` 57 tests, 0 failures.
- Task 2: `ExpertBookingsViewModelTest` 23, `ExpertBookingsScreenTest` 24, `IncomingRequestsScreenTest` 11, `IncomingRequestsViewModelTest` 12, 0 failures.
- Task 3: `TopicsScreenTest`, `TopicsViewModelTest`, `AvailabilityScreenTest` (20 existing + 4 new), `AvailabilityViewModelTest`, 0 failures.
- Acceptance greps: `LoadMode.Realtime` 3 lines in each VM; `RefreshFailedSnackbarEffect(` 1 each in Dashboard, Expert Bookings, Incoming Requests, Availability; `showRefreshFailed(` 1 in Topics; `SnackbarDuration.Short` 0 in Dashboard and Availability screens.
- Whole-project `ktlintCheck` BUILD SUCCESSFUL; `bash scripts/check-design-rules.sh` prints "Design rules: OK".

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking acceptance criterion] Dashboard still had a second `SnackbarDuration.Short`**
- **Found during:** Task 1
- **Issue:** The acceptance criterion needs 0 occurrences in `ExpertDashboardScreen.kt`, but the payout-open-failed snackbar (`rememberCustomTabLauncher` callback) also passed an explicit `SnackbarDuration.Short`, so removing only the refresh effect left one.
- **Fix:** Dropped the explicit argument; `showSnackbar(message)` without an action defaults to Short, so behaviour is identical. Removed the now-unused import.
- **Files modified:** `ExpertDashboardScreen.kt`
- **Commit:** `b5fc7f90`

**2. [Rule 1 - Test adaptation] Snackbar duration is asserted by clock advance, not by reading the duration**
- **Found during:** Task 1
- **Issue:** With `mainClock.autoAdvance = false` set before the failure, no frames ran and the snackbar never appeared.
- **Fix:** In all five `refreshFailedShowsRetry` tests the snackbar is first shown with auto-advance on, then auto-advance is switched off and the clock moved 5 s, so a Short (4 s) snackbar would be gone and the Long one still shows "Retry".
- **Commit:** `b5fc7f90`, `1ad0a52b`, `8627a58c`

**Total deviations:** 2 (one acceptance-criterion fix with identical behaviour, one test-technique adaptation). **Impact:** none on behaviour.

### TDD note

All three tasks are `tdd="true"` inside a `type: execute` plan, so the plan-level gate sequence does not apply. New tests and implementation were committed together per task (one `feat` commit each), as in 15-01. In Task 3 the Availability tests were written and run first: `refreshFailedShowsRetry` and `capped` failed, `timeRangeSheetSurvivesRestore` passed without a code change (the state was already saved), and `snackbarAboveNavBar` was confirmed by mutation after the fix.

## Issues Encountered

None.

## Known Stubs

None.

## Threat Flags

None. No new endpoints, auth paths or trust-boundary surface. T-15-28 (booking.update burst) is covered by the unchanged latest-wins `startLoad` and by `showRefreshFailed` staying silent while one refresh-failed snackbar is visible.

## Open Items (device checks deferred by the user)

- On a device with a gesture or 3-button nav bar, force a failed pull-to-refresh on Availability and open the keyboard (for example a timezone search): the snackbar must sit above the bar and the keyboard.
- With TalkBack: the "Retry" snackbar action is announced and focusable on all five screens.
- On a tablet or resized window (>= 840 dp): Dashboard, Bookings, Requests, Topics and Availability content is capped and centred; app bars and tabs stay full width, and the pull-to-refresh gesture works from the side margins.
- Time Range sheet after rotation and after process death on a real device (Robolectric covers `rememberSaveable` restoration only).

## Self-Check: PASSED

- FOUND commits: `b5fc7f90`, `1ad0a52b`, `8627a58c`
- FOUND: all 14 modified files listed in key-files
