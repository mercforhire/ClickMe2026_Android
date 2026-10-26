---
phase: 11-expert-requests-bookings
plan: 11
subsystem: ui
tags: [expert-bookings, past, empty-states, snackbar, compose, wave-3]

requires:
  - phase: 11-expert-requests-bookings
    provides: "11-04 ExpertBookingsContent, ExpertBookingsUiState, ExpertBookingLists/ExpertPastRow, ExpertBookingsSkeleton(past), ViewModel consumeNotice/consumeRefreshFailure"
  - phase: 11-expert-requests-bookings
    provides: "11-09 expert_cancel_result_refunded string and the post-cancel Past-tab hand-off"
  - phase: 08-client-booking-management
    provides: "BookingStatusBadge, pastStatusDisplay, PastBookingCard layout, BookingsNotice, my_bookings_date_time"
provides:
  - "ExpertPastCard with client name, Completed / Cancelled / Missed badge, Topic line and date line; merged Role.Button, opens the Summary"
  - "Past tab LazyColumn in the shared PullToRefreshBox with a 150 ms cross-fade between tabs"
  - "Empty states: Upcoming (Update Availability, View Past History) and Past, inside a scrollable column so pull-to-refresh still works"
  - "Snackbars for the post-cancel notice (plain and refund wording, consumed once) and for a failed pull"
affects: [11-13]

actuals:
  tokens: 14000
  tasks: 3
  commits: 3
plan_head_before: 12a38f1c33f95e9eb5883046a694c426121ff444
plan_head_after: 1f38f23f97ce7cdf480c0ebd47f557b29eeddcef
commits: 3

tech-stack:
  added: []
  patterns:
    - "Snackbar shown on a remembered composition scope so consuming the one-shot notice does not cancel it (same as MyBookingsContent)"
    - "Empty and error states share one scrollable, tagged, centered column so a swipe down still refreshes"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertPastCard.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreen.kt
    - app/src/main/res/values/strings_expert_bookings.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreenTest.kt

key-decisions:
  - "The Past card announces the plain topic in its merged content description (client, topic, date, time range, status label); the visible line stays 'Topic: {topic}' with the label muted and the value in onSurface"
  - "The 'Topic: %1$s' label/value split for coloring is derived from the localized string by locating the topic inside it, so translations with a different word order still color correctly"
  - "ExpertBookingsScreenTest runs at w411dp-h1200dp so a three-card Past list is fully composed (the default Robolectric screen only composed two LazyColumn items)"

patterns-established:
  - "Tab body split into TabBody (state routing), List and EmptyState composables so each stays under the detekt-style size limits"

requirements-completed: [EXP-08]

coverage:
  - id: D1
    description: "Past tab lists past sessions newest first with Completed / Cancelled / Missed badges, 'Topic: {topic}' and the date line; no Join control on Past; tapping a card opens the Summary"
    requirement: EXP-08
    verification:
      - kind: automated_ui
        ref: "ExpertBookingsScreenTest#pastTabShowsLabelledCards, pastCardsAreNewestFirst, pastCardOpensSummary"
        status: pass
    human_judgment: false
  - id: D2
    description: "Selecting Past during the first load shows the skeleton"
    requirement: EXP-08
    verification:
      - kind: automated_ui
        ref: "ExpertBookingsScreenTest#pastFirstLoadShowsPastSkeleton"
        status: pass
    human_judgment: false
  - id: D3
    description: "Upcoming empty state with Update Availability (callback) and View Past History (selects Past); Past empty state with no action; both pull-to-refresh"
    requirement: EXP-08
    verification:
      - kind: automated_ui
        ref: "ExpertBookingsScreenTest#emptyUpcomingOffersAvailabilityAndPast, emptyPastShowsItsState, emptyStatesStillPullToRefresh"
        status: pass
    human_judgment: false
  - id: D4
    description: "Post-cancel notice shows 'Booking cancelled' or the refund wording once and is consumed; a failed pull shows the refresh-failed snackbar and is consumed; no state, no snackbar"
    requirement: EXP-08
    verification:
      - kind: automated_ui
        ref: "ExpertBookingsScreenTest#cancelNoticeShowsOnce, refundNoticeShowsAmount, refreshFailureShowsSnackbar, noNoticeNoSnackbar"
        status: pass
    human_judgment: false
  - id: D5
    description: "Visual fidelity of the Past card (badge wrap at large font scales, spacing) and the 150 ms tab cross-fade"
    requirement: EXP-08
    verification: []
    human_judgment: true
    rationale: "Layout at large fonts and the animation are not asserted pixel for pixel; manual testing is deferred per project policy and logged as an open item."

duration: 35min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 11: Expert Bookings Past Tab, Empty States and Snackbars Summary

**Expert Bookings Past tab with Completed / Cancelled / Missed cards that open the Summary, per-tab empty states with Update Availability and View Past History shortcuts, and once-only snackbars for the post-cancel result (with the client refund amount) and a failed pull.**

## Performance

- **Duration:** about 35 min (most of it Gradle runs)
- **Tasks:** 3 (one tracer, two expansion), all `tdd="true"`
- **Files:** 4 (1 created, 3 modified)

## Accomplishments

- Tracer: `ExpertPastCard` mirrors the client `PastBookingCard` with the client in place of the expert, reusing `BookingStatusBadge`, `pastStatusDisplay` (declined maps to Cancelled, expired to Missed) and the device date and time formatting. The Past tab renders in the shared `PullToRefreshBox` with a keyed `LazyColumn`; switching tabs cross-fades in 150 ms. Tracer verify re-run green before expanding.
- Empty states: Upcoming shows the heading, body, a filled "Update Availability" action (wired to `onUpdateAvailability`, which the tab root already maps to Availability with origin Bookings) and a 48 dp "View Past History" text button that selects Past; Past shows its own copy with no action. Both live in the scrollable centered column, so a swipe down still refreshes (tested with a real swipe).
- Snackbars: `LaunchedEffect(state.notice)` shows `cancel_booking_result_cancelled` or `expert_cancel_result_refunded` with the server amount on a remembered scope, then calls `onNoticeConsumed()`; `LaunchedEffect(state.refreshFailed)` shows `expert_refresh_failed` then `onRefreshFailureConsumed()`. A quiet reload failure never sets `refreshFailed`, so it shows nothing (ViewModel behavior from 11-04, covered by its tests).

## Task Commits

1. **Task 1: Past tab lists labelled past sessions that open the Summary (tracer)** - `48eeb0f`
2. **Task 2: Empty states for Upcoming and Past** - `9b4ab17`
3. **Task 3: Post-cancel notice and refresh-failure snackbars** - `1f38f23`

## Files Created/Modified

- `E/upcomingbookings/subviews/ExpertPastCard.kt` - Past card (tag `expert_past_card`)
- `E/upcomingbookings/ExpertBookingsScreen.kt` - Crossfade tab body, Past list, empty states, snackbar host and effects, new `EXPERT_BOOKINGS_STATE_TAG`
- `res/values/strings_expert_bookings.xml` - topic, Past a11y, and the six empty-state strings
- `ExpertBookingsScreenTest` - 11 new tests (18 total)

(`E/` = `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/`.)

## Decisions Made

- The merged content description of the Past card carries the plain topic and the status label ("{client}, {topic}, {date}, {time range}, {label}"), while the on-screen line keeps "Topic: {topic}".
- Page 1 only (limit 50) for Past, as planned (Phase 8 D-09 accepted limit); no paging was added.
- Test screen enlarged to `w411dp-h1200dp` for the whole screen test class so a three-card Past list is fully composed.

## Deviations from Plan

**1. Process note: no separate RED commits**
- Tasks are marked `tdd="true"`, but each task introduces new symbols or strings, so a RED run would be a compile failure (INVALID_RED). Tests and implementation were committed together per task and each task's tests were run green before commit (same approach as 11-03, 11-04 and 11-09).

**2. [Rule 3 - Blocking] Test viewport enlarged**
- **Found during:** Task 1
- **Issue:** with the default Robolectric screen only two of three Past cards were composed by the `LazyColumn`, so the three-card assertion failed.
- **Fix:** `@Config(qualifiers = "w411dp-h1200dp")` on `ExpertBookingsScreenTest`.
- **Files modified:** `ExpertBookingsScreenTest.kt`
- **Commit:** `48eeb0f`

**3. Minor:** removed the `UNUSED_PARAMETER` suppression from `ExpertBookingsContent` once the two consume callbacks became used (Task 3).

**Total deviations:** 1 auto-fix (Rule 3) plus 2 notes. **Impact:** none on behavior.

## Issues Encountered

- ktlint import ordering (java/javax/kotlin last, `kotlinx` in lexicographic position) and a chained-call line in a test; both fixed and re-verified.

## Authentication Gates

None.

## Known Stubs

None. The Past tab's earlier empty list (a 11-04 hand-off) is now real.

## Threat Flags

None. T-11-34 (notice shown once and consumed, tested), T-11-35 (client name rendered as plain `Text`) are mitigated as planned; T-11-SC accepted (no package installs).

## Verification

- `*ExpertBookingsScreenTest` (18) and `*ExpertBookingsViewModelTest` (20, unchanged) plus `ktlintCheck` pass; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `BookingStatusBadge` in ExpertPastCard = 2; `ExpertPastCard(` in the screen = 1; the four empty-state string lines = 4; `Text("[A-Za-z]` literals in the screen = 0; `expert_cancel_result_refunded` = 1; `expert_refresh_failed` = 1.

## Next Phase Readiness

- EXP-08 is complete (Upcoming and Past, labels, empty states, post-cancel notice). Manual on-device checks of Past-card wrapping at large fonts and the cross-fade are deferred and should be logged as open items for phase UAT.

## Self-Check: PASSED

- `ExpertPastCard.kt` exists on disk; the screen, strings and test files are committed (clean `git status` apart from the gitignored `local.properties`).
- Commits `48eeb0f`, `9b4ab17`, `1f38f23` are ancestors of HEAD; `git rev-list --count 12a38f1..HEAD` = 3 before this summary commit.

---
*Phase: 11-expert-requests-bookings*
*Completed: 2026-10-26*
