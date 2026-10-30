---
phase: 15-polish-hardening
plan: 12
subsystem: ui
tags: [client, notifications, explore, state-audit, refresh, large-screen, tracer]
status: complete

requires:
  - phase: 15-01
    provides: RefreshFailedSnackbarEffect, ErrorState extension, MyBookings LoadMode.Realtime pilot
  - phase: 15-02
    provides: Modifier.contentMaxWidth()
provides:
  - NotificationsViewModel.LoadMode.Realtime (foreground push arrival reload flags a failure)
  - RefreshFailedSnackbarEffect wiring on Notifications and Explore (Retry re-runs the refresh)
  - contentMaxWidth() on Explore, My Bookings, Notifications, Booking Summary, Client Cancellation, Write Review
  - All Categories sheet empty-catalog state (no action)
  - State, restoration and large-window tests on seven test classes
affects: [15-13, 15-14, 15-15, 15-16, 15-17, 15-18, 15-19, 15-20]

actuals:
  tokens: 13800
  tasks: 3
  commits: 3
plan_head_before: e808da6b9c3dd62717fc017104c562ebca5b1350
plan_head_after: b7b88c66c3de8015f6ffa7574d0c59915d9849ab

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/categoriesmodal/AllCategoriesSheet.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/subviews/CancellationReasonStep.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/subviews/CancellationConfirmStep.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/WriteReviewFooter.kt
    - app/src/main/res/values/strings_all_categories.xml
    - app/src/main/res/values/strings_explore.xml

key-decisions:
  - "Explore's snackbar now uses the shared common_refresh_failed copy; the old explore_refresh_failed string said 'Pull down to try again', which contradicts a Retry action, and was removed"
  - "Scroll areas stay full width; contentMaxWidth() is applied after verticalScroll (forms) or per item (lists) so gestures, pull-to-refresh and app bars span the window"
  - "A restored cancel confirm step is covered at two levels: the stateless screen (StateRestorationTester, zero onConfirm) and the ViewModel (SavedStateHandle with step=Confirm, zero cancel API calls)"

patterns-established:
  - "Large-window test: @Config(qualifiers = \"w900dp-h1200dp\"), assert the content width <= 600 dp and left >= 150 dp, and that the list/scroll area stays > 600 dp"

requirements-completed: [POL-01, POL-02, POL-03]

coverage:
  - id: D1
    description: "A failed Notifications reload after a foreground push keeps rows and flags refreshFailed; a quiet tab-return failure is silent; a first-load arrival failure stays loud"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "NotificationsViewModelTest#arrivalReloadFailureFlags, #quietTabReturnFailureIsSilent, #arrivalDuringFirstLoadFailureIsLoud"
        status: pass
    human_judgment: false
  - id: D2
    description: "Notifications and Explore show the refresh-failed snackbar with Retry; Retry re-runs the refresh and keeps rows/sections"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "NotificationsScreenTest#refreshFailedShowsRetry; ExploreScreenTest#refreshFailedShowsRetryAndKeepsSections"
        status: pass
    human_judgment: false
  - id: D3
    description: "Explore sections fail independently: a failed section shows SectionError with Retry while loaded sections keep their content"
    requirement: POL-02
    verification:
      - kind: unit
        ref: "ExploreScreenTest#sectionFailureIsolated"
        status: pass
    human_judgment: false
  - id: D4
    description: "All Categories sheet: loading, error with Retry that keeps typed search text, empty catalog with no action"
    requirement: POL-02
    verification:
      - kind: unit
        ref: "AllCategoriesSheetTest#sheetCells, #failedCatalogKeepsTheTypedSearchTextAndRetries, #emptyCatalogShowsEmptyCopyWithNoAction"
        status: pass
    human_judgment: false
  - id: D5
    description: "The cancel confirmation never confirms by itself after restore (screen and ViewModel)"
    requirement: POL-02
    verification:
      - kind: unit
        ref: "ClientCancellationScreenTest#confirmNeverAutoConfirmsAfterRestore; ClientCancellationViewModelTest#aRestoredConfirmStepNeverCancelsOnItsOwn"
        status: pass
    human_judgment: false
  - id: D6
    description: "The six screens cap their content at 600 dp on a 900 dp window while scroll areas stay full width"
    requirement: POL-03
    verification:
      - kind: unit
        ref: "NotificationsScreenTest#listCapped; ExploreScreenTest#exploreCapped; MyBookingsScreenTest#capped; BookingSummaryScreenTest#capped; ClientCancellationScreenTest#cappedReasonStep, #cappedConfirmStep; WriteReviewScreenTest#capped"
        status: pass
    human_judgment: false
  - id: D7
    description: "On-device look of the capped layouts, TalkBack announcement of the snackbar Retry, real push-arrival failure"
    requirement: POL-03
    verification: []
    human_judgment: true
    rationale: "Device and TalkBack behaviour is not asserted by Robolectric tests; device testing is deferred by the user"

completed: 2026-10-29
---

# Phase 15 Plan 12: Remaining client screens to the state standard Summary

**Notifications and Explore now offer a Retry snackbar on a failed refresh (including a push-triggered reload), the categories sheet handles an empty catalog, and six client screens are capped at 600 dp with the cancel confirm proven never to fire by itself after restore.**

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (tracer) | Notifications Realtime load mode, Retry snackbar, 600 dp cap | `da9a43f5` |
| 2 | Explore Retry snackbar, categories sheet empty state, Explore and My Bookings caps | `a2ee2849` |
| 3 | Booking Summary, Cancel, Write Review caps and state/restore tests | `b7b88c66` |

**Tracer gate:** Task 1 `<verify>` tests (15 + 26) and ktlint were green before Task 2 started. Tracer verified end-to-end, expanding.

## Per-screen audit (D-08)

| Screen | Archetype | Result | Cells that gained a test |
| ------ | --------- | ------ | ------------------------ |
| Notifications | A (list) | compliant (named "Couldn't load notifications" + Retry already present); snackbar converted to Retry | arrival failure flags, tab-return silent, first-load arrival loud, Retry snackbar, listCells (skeleton, empty, named error, one row), capped |
| Explore | C (composite) | snackbar converted to Retry; per-section `SectionError` already compliant | refreshFailedShowsRetryAndKeepsSections, sectionFailureIsolated, exploreCapped |
| All Categories sheet | E (sheet) | skeleton and error with Retry compliant; empty catalog was blank, converted to an empty state with no action | sheetCells, retry keeps search text, empty catalog |
| My Bookings | A (list) | compliant (15-01 snackbar wiring); width cap only | one item no empty state, capped |
| Booking Summary | B (detail) | compliant ("Couldn't load session" + Retry, Loading skeleton) | capped (loading and error cells already covered) |
| Client Cancellation | B/D (form) | compliant ("Couldn't load booking" + Retry, skeleton) | cappedReasonStep, cappedConfirmStep, confirmNeverAutoConfirmsAfterRestore (+ ViewModel twin) |
| Write Review | B/D (form) | compliant ("Couldn't load review" + Retry, skeleton) | submitInFlightAndError, submitErrorKeepsTheRatingAndText, capped |

No screen maps a 404 to an "unavailable" state, so `notFoundIsUnavailable` does not apply. The cancel ViewModel treats 404 or 409 INVALID_STATUS as "return to My Bookings" (existing, tested), and the other two surface the server message with Retry.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Empty-catalog state in the All Categories sheet**
- **Found during:** Task 2 (`sheetCells` expectation "empty catalog shows the empty copy with no action")
- **Issue:** `CategoryCatalog.Loaded(emptyList())` with a blank query rendered nothing (blank sheet body).
- **Fix:** Added `CategoriesEmpty()` (reusing the sheet's `StateColumn`) and strings `all_categories_empty_heading` "No categories yet" / `all_categories_empty_body` "Check back soon." in `strings_all_categories.xml`. No action, per UI-SPEC archetype E. The copy is new (the UI-SPEC does not name it); the user may reword.
- **Files modified:** `AllCategoriesSheet.kt`, `strings_all_categories.xml`
- **Commit:** `a2ee2849`

**2. [Rule 1 - Bug] Explore snackbar copy contradicted its Retry action**
- **Issue:** `explore_refresh_failed` said "Pull down to try again." while the new snackbar has a Retry button.
- **Fix:** Explore now uses the shared `common_refresh_failed` default; the unused string was removed and the existing Explore test updated to the shared copy.
- **Files modified:** `ExploreScreen.kt`, `strings_explore.xml`, `ExploreScreenTest.kt`
- **Commit:** `a2ee2849`

**3. [Rule 2 - extra test file] ViewModel restore test**
- **Issue:** The screen content is stateless, so a `StateRestorationTester` test alone cannot prove the guard (T-15-27). Added `ClientCancellationViewModelTest#aRestoredConfirmStepNeverCancelsOnItsOwn` (SavedStateHandle with step=Confirm, zero cancel API calls). This file was not in the plan's `files_modified` list; the change is additive.
- **Commit:** `b7b88c66`

**Total deviations:** 3, none affecting behaviour outside the plan's intent.

### TDD note

The tasks are `tdd="true"` in a `type: execute` plan, so the plan-level gate sequence does not apply. Tests and implementation were committed together per task (as in 15-01); a separate failing RED commit was not produced. Most new cases for already-compliant screens pass against existing behaviour by design (the audit rule says compliant blocks only gain tests).

## Verification Results

- Task 1: `NotificationsViewModelTest` 15/15, `NotificationsScreenTest` 26/26.
- Task 2: `ExploreScreenTest` 18/18, `ExploreViewModelTest` 24/24, `AllCategoriesSheetTest` 8/8, `MyBookingsScreenTest` 46/46, `NotificationsScreenTest` 26/26 (re-run after the ktlint fix).
- Task 3: `BookingSummaryScreenTest` 26/26, `ClientCancellationScreenTest` 33/33, `ClientCancellationViewModelTest` 25/25, `WriteReviewScreenTest` 17/17.
- Whole-project `ktlintCheck` BUILD SUCCESSFUL after each task. `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `LoadMode.Realtime` in NotificationsViewModel >= 2, `RefreshFailedSnackbarEffect(` 1 in each of NotificationsScreen and ExploreScreen, `contentMaxWidth()` present in all six screen files (Cancel via ClientCancellationScreen plus its two step subviews, Write Review via WriteReviewScreen plus its footer).

## Known Stubs

None.

## Threat Flags

None. T-15-26 mitigated: the arrival collector uses latest-wins `startLoad` and the 15-01 helper allows one refresh-failed snackbar at a time. T-15-27 mitigated: the cancel runs only from the confirm button's `onClick` and is proven by the two restore tests.

## Open Items (device checks deferred by the user)

- On a device, with airplane mode on, receive a foreground push while on Notifications: confirm the rows stay and one snackbar "Couldn't refresh. Check your connection and try again." with "Retry" appears, and Retry reloads.
- Pull to refresh on Explore with the network off: all loaded sections stay, the snackbar shows Retry.
- On a tablet or resizable window at 900 dp and wider: the six screens show a centered 600 dp column; app bars and pull-to-refresh span the full width; the Write Review footer background spans the window while its button is capped.
- Kill the process on the cancel confirm step and reopen: the confirm step is shown and nothing cancels until Confirm is tapped.
- Review the new "No categories yet / Check back soon." copy.

## Self-Check: PASSED

- FOUND commits: `da9a43f5`, `a2ee2849`, `b7b88c66`
- FOUND files: all 13 main files and 11 test files listed above modified in the commits.
