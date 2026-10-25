---
phase: 10-expert-topics-availability
plan: 14
subsystem: expert-availability-ui
tags: [compose, screen, availability]
status: complete

requires:
  - phase: 10-08
    provides: "AvailabilityDayCard, AvailabilityOverlapBanner, AvailabilitySaveAction, AvailabilitySkeleton, TimeRangePickerSheet, TimezonePickerSheet"
  - phase: 10-12
    provides: "AvailabilityViewModel, AvailabilityUiState / AvailabilityLoad"
provides:
  - "AvailabilityScreen (Hilt wiring) and stateless AvailabilityContent with sheet slots"
  - "AvailabilityActions callback bundle"
affects: [10-15 navigation registration]

commits: 2
plan_head_before: db9cdc1fd2a40dda2a25e4078b163140b404e2a1
plan_head_after: c0e296811eb0c385e3d9bb1fea94f33eca5a0447
actuals:
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns: ["Sheet composables passed as slots so Robolectric tests substitute stubs for ModalBottomSheet windows", "Edit target held as rememberSaveable Pair<Int, Int> (DayOfWeek value, slot index)"]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScreenTest.kt
  modified: []

key-decisions:
  - "Scaffold uses contentWindowInsets = 0 and the scrolling body ends in a navigationBarsPadding spacer (same pattern as MakeABookingScreen), so the xl + navigation-inset bottom padding is applied once and content scrolls under the nav bar"
  - "Banner keeps the last overlapping-day list while it animates out, so the text does not blank during the fade"

requirements-completed: [EXP-03, EXP-04]
---

# Phase 10 Plan 14: Availability Screen Summary

Full-screen Availability route that assembles the 10-08 components over the 10-12 ViewModel: app bar with a guarded Save action, skeleton / error-with-Retry / loaded states with pull-to-refresh, timezone row with Auto-detect, an overlap banner naming the days, seven day cards, and the time-range and timezone sheets.

## Accomplishments

- `AvailabilityScreen(onBack, modifier, viewModel = hiltViewModel())` collects `uiState` and passes it, with an `AvailabilityActions` bundle, to the stateless `AvailabilityContent`.
- `AvailabilityContent`: `AuthTopAppBar` with back arrow, "Availability" title and `AvailabilitySaveAction(enabled = state.canSave)`; no `BackHandler`, so Back pops during a save and unsaved edits are discarded without a prompt (D-16). The screen stays editable in a call; only Save is guarded in the ViewModel (D-03).
- Loaded body: `PickerField` (tag `availability_timezone_row`), the Auto-detect `TextButton` (local state only, D-14), an `AnimatedVisibility` overlap banner using `joinNames` with the localized separators, and the seven day cards in week order.
- Sheets: the edit target is a `rememberSaveable` `Pair<Int, Int>`; Done calls `updateSlot` and closes, dismiss discards, and a stale index after a refresh closes the sheet. The timezone sheet shows `state.timezoneEntries` with the current zone and closes on pick.
- Refresh failure shows `expert_refresh_failed` as a snackbar then calls `onRefreshFailedShown`; a save failure shows `AuthErrorDialog` titled "Couldn't save availability".
- `AvailabilityScreenTest`: 16 Robolectric cases (skeleton, error + Retry, loaded layout and day order, back, overlap banner and disabled Save, Save click, Saving / Saved a11y, slot editor Done and dismiss, timezone sheet select and dismiss, header toggle, Add hours, error dialog and OK, refresh snackbar).

## Task Commits

1. Task 1: `df14f26` AvailabilityScreen, AvailabilityContent, AvailabilityActions
2. Task 2: `c0e2968` AvailabilityScreenTest (16 tests)

## Verification

- `:app:testDevDebugUnitTest` for `*AvailabilityScreenTest` (16), `*AvailabilityViewModelTest` (20) and `*AvailabilityComponentsTest` (10): 0 failures.
- `:app:assembleDevDebug`, `ktlintCheck` and `scripts/check-design-rules.sh` pass.
- Acceptance greps: non-comment `BackHandler` count 0, `availability_timezone_row` 1, `joinNames` 1.

## Deviations from Plan

None - plan executed as written. Interpretation note: the plan's "xl + navigation inset bottom" padding is applied as a trailing `navigationBarsPadding()` spacer with `Spacing.xl` inside the scroll column (Scaffold insets zeroed), matching `MakeABookingScreen`, rather than adding the inset on top of Scaffold's own bottom padding.

## Known Stubs

None.

## Threat Flags

None. T-10-44 (server messages rendered through plain `Text` / `AuthErrorDialog`) and T-10-45 (the only Save path is `actions.save` to the call-guarded `AvailabilityViewModel.save()`) are implemented as planned.

## Self-Check: PASSED

- FOUND: AvailabilityScreen.kt, AvailabilityScreenTest.kt
- FOUND: commits df14f26 and c0e2968 in branch history
