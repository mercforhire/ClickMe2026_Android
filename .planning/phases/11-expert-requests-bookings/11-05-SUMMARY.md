---
phase: 11-expert-requests-bookings
plan: 05
subsystem: expert-requests
tags: [incoming-requests, viewmodel, compose, nav3, pull-to-refresh]

requires:
  - phase: 11-expert-requests-bookings (11-01)
    provides: BookingRequestsRepository, BookingRequestItem / EarningsBreakdownDto, FakeClickMeApi bookingRequestsHandler, bookingRequestItem and breakdown fixtures
  - phase: 11-expert-requests-bookings (11-02)
    provides: IncomingRequestsKey, RequestDecisionKey, ExpertLaunchRequest.incomingRequests slot, incomingRequestsEntries stub
  - phase: 11-expert-requests-bookings (11-03)
    provides: clientDisplayName
provides:
  - IncomingRequestsLogic (isExpiredAt, buildIncomingRequestRows, requestCardEarnings, toDecisionKey)
  - IncomingRequestsViewModel with loud, quiet and pull loads and the decline hand-off
  - IncomingRequestsScreen / IncomingRequestsContent with skeleton, empty, error, list and snackbars
  - IncomingRequestCard, IncomingRequestsSkeleton, INCOMING_REQUEST_CARD_TAG
  - IncomingRequestsKey registered with the bottom bar hidden
affects: [11-06, 11-10, 11-12]

actuals:
  tokens: 17200
  tasks: 3
  commits: 6
plan_head_before: 2d62a572d451277a2d07d2d2715b4a38af23cb66
plan_head_after: 3bc586238269f0d7fc05d7f714232b1b2fd31eb2
commits: 6

tech-stack:
  added: []
  patterns:
    - "Load modes Loud / Quiet / Pull with cancel-on-restart and a loadIsCurrent flag, copied from MyBookingsViewModel"
    - "One-shot launch slot consumed in init (posted before the ViewModel existed) and collected while alive"
    - "Snackbar launched on the composition scope so consuming the notice does not cancel it"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsLogic.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/subviews/IncomingRequestCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/subviews/IncomingRequestsSkeleton.kt
    - app/src/main/res/values/strings_incoming_requests.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsLogicTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsViewModelTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsEntries.kt

key-decisions:
  - "A row is expired when status is \"expired\" or expiresAt <= now; a pending row with no expiresAt stays pending and sorts after dated pending rows (the server's 409 ALREADY_PROCESSED is the backstop)"
  - "Card earnings come only from earnings_breakdown.net_amount in major units; gross is read only to recognise a free session; a paid row with no net shows Payout pending"
  - "The skeleton reuses SkeletonGroup's content_loading description, so no separate incoming_requests_loading string was added"

patterns-established:
  - "Incoming Requests hands the tapped row's expiry, expired flag and list earnings forward inside RequestDecisionKey"

requirements-completed: [EXP-06]

coverage:
  - id: D1
    description: "Incoming Requests lists GET expert/booking-requests rows with client, topic, date and time range and the net earnings; the list sends no page or limit"
    requirement: "EXP-06"
    verification:
      - kind: unit
        ref: "IncomingRequestsScreenTest#requestCardsRenderFromTheApi"
        status: pass
      - kind: unit
        ref: "IncomingRequestsViewModelTest#theFirstLoadIsLoudAndSendsNoPagination"
        status: pass
    human_judgment: false
  - id: D2
    description: "Pending requests sort soonest-expiry first, a missing expiry after dated rows, expired rows last with an Expired badge and amber ring; expired cards are not dimmed and still open"
    requirement: "EXP-06"
    verification:
      - kind: unit
        ref: "IncomingRequestsLogicTest#pendingRowsSortBySoonestExpiryThenNullExpiryThenExpiredLast"
        status: pass
      - kind: unit
        ref: "IncomingRequestsScreenTest#expiredRowShowsBadgeAndOpensReadOnly"
        status: pass
    human_judgment: false
  - id: D3
    description: "Tapping a card opens Request Decision with a key carrying the row's isExpired, expiry millis, list earnings and currency"
    requirement: "EXP-06"
    verification:
      - kind: unit
        ref: "IncomingRequestsScreenTest#tapOpensDecisionWithRowData"
        status: pass
      - kind: unit
        ref: "IncomingRequestsLogicTest#theDecisionKeyCarriesTheRowFields"
        status: pass
    human_judgment: false
  - id: D4
    description: "Earnings column shows net in major units (no decimals for JPY), Free for a free request, Payout pending when net is null, never gross"
    requirement: "EXP-06"
    verification:
      - kind: unit
        ref: "IncomingRequestsLogicTest (netAmountIsFormattedInMajorUnits, aZeroDecimalCurrencyHasNoDecimals, aFreeRequestIsFree, aPaidRequestWithoutANetAmountIsPayoutPendingAndNeverShowsGross)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Skeleton on first load, empty state with no action, error state with working Retry"
    requirement: "EXP-06"
    verification:
      - kind: unit
        ref: "IncomingRequestsScreenTest (firstLoadShowsSkeleton, emptyListShowsEmptyState, failureShowsErrorWithRetry)"
        status: pass
    human_judgment: false
  - id: D6
    description: "Quiet reload when the screen reappears and on pull-to-refresh; failed pull flags a snackbar, failed quiet reload shows nothing"
    requirement: "EXP-06"
    verification:
      - kind: unit
        ref: "IncomingRequestsViewModelTest (screenShownReloadsQuietly, pullFailureFlagsRefreshFailed, quietFailureKeepsRowsAndFlagsNothing)"
        status: pass
      - kind: unit
        ref: "IncomingRequestsScreenTest#aFailedPullShowsTheRefreshSnackbarAndIsConsumed"
        status: pass
    human_judgment: false
  - id: D7
    description: "After a decline the list shows Request declined once and reloads; an accept or ALREADY_PROCESSED launch reloads quietly"
    requirement: "EXP-06"
    verification:
      - kind: unit
        ref: "IncomingRequestsViewModelTest (declinedLaunchPostedBeforeInitShowsNoticeOnce, launchWhileAliveForcesOneQuietReload, aLaunchWithoutANoticeStillReloads)"
        status: pass
      - kind: unit
        ref: "IncomingRequestsScreenTest#theDeclinedNoticeShowsASnackbarAndIsConsumed"
        status: pass
    human_judgment: false
  - id: D8
    description: "Visual match to the UI-SPEC card (amber ring, badge, spacing) on a device"
    requirement: "EXP-06"
    verification: []
    human_judgment: true
    rationale: "Pixel-level layout and contrast on a real display are not asserted by Robolectric tests; deferred to the phase's manual pass (manual testing is deferred per project memory)"

duration: 35min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 05: Incoming Requests Summary

**Incoming Requests end to end: the expiry/sort/net-earnings rules, a ViewModel with loud, quiet and pull loads plus the decline hand-off, request cards with an Expired badge and amber ring, skeleton/empty/error states, and the route registered on the expert shell.**

## Performance

- **Duration:** about 35 min (13:30 to 14:05 local)
- **Tasks:** 3 (one tracer, two expansion)
- **Files:** 11 (10 created, 1 modified)
- **Commits:** 6 (three RED test commits, three feature commits)

## Accomplishments

- `IncomingRequestsLogic.kt` pins the iOS rules with the research corrections: expiry from the row (C3), net earnings from `earnings_breakdown` in major units (C4), no pagination (C2). `toDecisionKey()` carries the expiry, expired flag, list earnings and currency to Request Decision.
- `IncomingRequestsViewModel` copies the MyBookings load-mode machine (Loud / Quiet / Pull, latest-wins, `onScreenShown` coalescing). It consumes `ExpertLaunchRequest.incomingRequests` in `init` and while alive, so a decline shows "Request declined" exactly once and every launch (decline, accept, ALREADY_PROCESSED) forces a reload.
- `IncomingRequestCard` follows UI-SPEC surface 2: one `Role.Button` node, 56dp avatar with a 2dp ring (`tertiary` when expired), `FlowRow` header, divider, date and time range, "Expires in {n}h" / "Expires in under 1h" or the "Expired" badge, merged accessibility description. Expired cards are not dimmed.
- `IncomingRequestsScreen` has the skeleton, the empty and error states inside scrolling columns (so pull-to-refresh works), snackbars for the decline notice and a failed pull, and the resume/pause/dispose lifecycle effects.
- `incomingRequestsEntries` registers `entry<IncomingRequestsKey>(metadata = BottomBar.hidden())`; a card tap calls `navigator.navigate(key)`.

## Task Commits

1. **Task 1 (tracer):** RED `f411bd6`, GREEN `4a1d35e` (logic, UI state, ViewModel, card, screen, entries, strings)
2. **Task 2:** RED `868ae96`, GREEN `580b4c0` (skeleton, empty and error states)
3. **Task 3:** RED `7439548`, GREEN `3bc5862` (quiet reloads, pull-to-refresh, decline hand-off, snackbars)

Tracer feedback gate: the tracer's `<verify>` is automated-only and `HUMAN_VERIFY_MODE` is `end-of-phase`, so it was re-run end to end (3 of 3 passing plus ktlint) and expansion continued.

## Verification

- `./gradlew :app:testDevDebugUnitTest --tests '*IncomingRequestsViewModelTest' --tests '*IncomingRequestsScreenTest' --tests '*IncomingRequestsLogicTest' ktlintCheck`: BUILD SUCCESSFUL (12 + 8 + 14 tests, 0 failures)
- `HomeShellsTest`, `ExpertNavigationTest`, `ExpertLaunchRequestTest` and `lintDevDebug`: BUILD SUCCESSFUL
- `bash scripts/check-design-rules.sh`: "Design rules: OK"
- Acceptance greps: `entry<IncomingRequestsKey>` = 1; `grossAmount` in the card file = 0; `incomingRequests.pending` = 1; "Request declined" in strings = 1; `Log.x(` in the ViewModel = 0; `@Test` count in the logic test = 14 (needed 8)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] No separate `incoming_requests_loading` string**
- **Found during:** Task 2
- **Issue:** The plan listed `incoming_requests_loading` "Loading", but `SkeletonGroup` already announces the shared `content_loading` ("Loading"), so a second string would be unused (lint noise) and duplicate copy.
- **Fix:** The skeleton uses `SkeletonGroup` and the existing resource; the screen test asserts the "Loading" description.
- **Files modified:** `subviews/IncomingRequestsSkeleton.kt`, `strings_incoming_requests.xml`

**2. [Rule 2 - Missing critical] Extra accessibility strings for rows without an expiry or under an hour**
- **Found during:** Task 1
- **Issue:** The plan's two a11y strings (pending plural, expired) cannot describe a pending row with no `expires_at` or one with under an hour left ("expires in 0 hours" would be wrong).
- **Fix:** Added `incoming_requests_card_a11y_under_hour` and `incoming_requests_card_a11y_no_expiry`; the pending plural is used only for 1 hour or more.
- **Files modified:** `strings_incoming_requests.xml`, `subviews/IncomingRequestCard.kt`

**3. [Rule 2 - Missing critical] Snackbar screen tests**
- **Found during:** Task 3
- **Issue:** The plan's tests covered the ViewModel side of the decline notice and failed pull but not the screen showing and consuming them.
- **Fix:** Added `theDeclinedNoticeShowsASnackbarAndIsConsumed` and `aFailedPullShowsTheRefreshSnackbarAndIsConsumed` to `IncomingRequestsScreenTest`.

**Total deviations:** 3 auto-fixed (1 blocking, 2 missing critical). **Impact:** none on scope or behavior.

## Issues Encountered

- ktlint chain-wrapping and import-order findings on first runs of each task were fixed inline.
- The failure-state screen test counted API calls exactly; with the unconfined test dispatcher the initial load finishes before `onScreenShown` runs, so a second load is legitimate. The test now compares against the call count before pressing Retry.

## Known Stubs

None.

## Threat Flags

None. T-11-14 (plain `Text` with `maxLines` and ellipsis, no links or HTML), T-11-15 (`isExpired` carried in the key) and T-11-16 (gross never read for the earnings text; grep gate and logic test) are mitigated as planned.

## Next Phase Readiness

11-06 (Request Decision) can read `RequestDecisionKey` as built by `toDecisionKey()`. 11-10 (Dashboard) only needs to `navigator.navigate(IncomingRequestsKey)`. 11-12 must post `IncomingRequestsLaunch(IncomingRequestsNotice.Declined)` after a decline and `IncomingRequestsLaunch()` after an accept or an ALREADY_PROCESSED dialog; this screen consumes both.

## Self-Check: PASSED

- Created files exist on disk (10 of 10 found).
- Commits `f411bd6`, `4a1d35e`, `868ae96`, `580b4c0`, `7439548`, `3bc5862` are ancestors of HEAD.
- STATE.md and ROADMAP.md untouched (the orchestrator owns them).
