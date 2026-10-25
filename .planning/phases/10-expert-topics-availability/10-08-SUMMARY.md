---
phase: 10-expert-topics-availability
plan: 08
subsystem: expert-availability-ui
tags: [compose, components, availability, bottom-sheet]
status: complete

requires:
  - phase: 10-02
    provides: "AvailabilityScheduleLogic (startOptions, endOptions, formatMinutes), AvailabilityTimezoneLogic.pickerEntries"
  - phase: 10-04
    provides: "availability_* strings, Motion.AVAILABILITY_SAVED_HOLD_MS"
provides:
  - "AvailabilityDayCard, AvailabilityTimeButton, AvailabilityOverlapBanner, AvailabilitySaveAction, AvailabilitySkeleton"
  - "TimeRangePickerSheet / TimeRangePickerContent (D-04)"
  - "TimezonePickerSheet / TimezonePickerContent (D-13, D-14)"
affects: [10-12 availability ViewModel, 10-14 availability screen]

commits: 2
plan_head_before: 367841f5d56091f10977b8ed9db6871cf03397fc
plan_head_after: b7f54e9b29a4018b24b0c98876f5bdd496079432
actuals:
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns: ["Stateless *Content composables tested under Robolectric; ModalBottomSheet wrappers kept thin", "LazyListState seeded once with an initial index for centre-on-open"]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilityDayCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilityTimeButton.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilityOverlapBanner.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilitySaveAction.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilitySkeleton.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/TimeRangePickerSheet.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/TimezonePickerSheet.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilityComponentsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/TimeRangePickerSheetTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/TimezonePickerSheetTest.kt
  modified: []

key-decisions:
  - "Time button uses a clickable Box inside a Surface (not Surface(onClick)) so onClickLabel 'Change time range' and the 'Start/End time ...' contentDescription can both be set"
  - "Public test-tag helpers availabilityDayTag(day) / availabilityAddHoursTag(day) so the screen (10-14) and tests share tag names"

requirements-completed: [EXP-03, EXP-04]
---

# Phase 10 Plan 08: Availability Components Summary

Stateless Availability UI: day cards with toggleable Switch header, overlap banner, three-state Save action, skeleton, and the Select Time Range and Select Timezone bottom sheets, covered by 19 Robolectric tests.

## Accomplishments

- `AvailabilityDayCard`: header row is the single toggle target (`Role.Switch`, stateDescription Available/Unavailable, tag `availability_day_{DAY}`); enabled days animate in slot rows (FlowRow: start button, separator, end button, 48dp remove) and a centered "Add hours"; overlap gives a 50% `error` border. Times use the device `DateFormat` via `formatMinutes`, so 1440 renders as midnight without `LocalTime`.
- `AvailabilityOverlapBanner`: title plus "Time slots on {days} overlap..." as a polite live region.
- `AvailabilitySaveAction`: cross-faded Idle (disabled with the explanation as stateDescription), Saving (spinner, stateDescription) and Saved (check, polite live region).
- `AvailabilitySkeleton`: 56dp pill and four day-card blocks under one `SkeletonGroup`.
- `TimeRangePickerContent`: Start/End `LazyColumn`s (240dp) fed by `startOptions` / `endOptions`, selected row with primary text and 2dp border (150 ms color animation), lists centered once on open via the initial index, Cancel and Done `PressScaleButton`s. `TimeRangePickerSheet` keeps a `rememberSaveable` draft; Done calls `onDone(TimeSlot)`, Cancel/scrim/Back call `onDismiss` and discard (A8).
- `TimezonePickerSheet` / `TimezonePickerContent` reuse `timezoneSheetRows`.

## Task Commits

1. Task 1: `d8df27e` day card, time button, banner, Save action, skeleton, `AvailabilityComponentsTest` (10 tests)
2. Task 2: `b7f54e9` time range and timezone sheets, `TimeRangePickerSheetTest` (8), `TimezonePickerSheetTest` (1)

## Verification

- `testDevDebugUnitTest` for the three new classes: 19 tests, 0 failures.
- `ktlintCheck` and `scripts/check-design-rules.sh` pass; acceptance greps (Role.Switch 1, LocalTime 0, Polite 1, skipPartiallyExpanded 1, timezoneSheetRows 3, start/endOptions 2) match.

## Deviations from Plan

None - plan executed as written. (Test detail: lazy rows only compose when visible, so absence assertions scroll to the neighbouring boundary row first so the check is meaningful; the sheet's `ModalBottomSheet` wrapper itself is not driven under Robolectric, per plan.)

## Known Stubs

None.

## Threat Flags

None. T-10-23 (current values always listed, 1440 tested), T-10-24 (`formatMinutes`, no `LocalTime`) and T-10-25 (server display name rendered through `PickerRow` plain `Text`) are implemented as planned.

## Self-Check: PASSED

All ten files exist and commits d8df27e and b7f54e9 are in the branch history.
