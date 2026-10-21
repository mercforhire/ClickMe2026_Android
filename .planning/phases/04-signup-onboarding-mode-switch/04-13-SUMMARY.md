---
phase: 04-signup-onboarding-mode-switch
plan: 13
subsystem: signup
tags: [compose, timezone, picker, viewmodel]
requires:
  - phase: 04-02
    provides: Timezones (all, detected, pickerEntries, entryFor)
  - phase: 04-03
    provides: AuthTopAppBar, AuthErrorDialog, signup_* common strings
  - phase: 04-04
    provides: PickerField, PickerSheet, PickerSheetContent, PickerRow
  - phase: 04-07
    provides: SignupSetupRepository.saveTimezone, SignupTestHarness
provides:
  - SignupTimezoneViewModel and SignupTimezoneUiState
  - SignupTimezoneScreen, SignupTimezoneContent, TimezoneSheetContent, timezoneSheetRows
  - strings_signup_timezone.xml
affects: [04-19]
tech-stack:
  added: []
  patterns:
    - "Step ViewModel follows the draft until the user picks, then pins the choice in SavedStateHandle"
    - "Saved hold: Saving -> Saved -> delay(600) -> saved flag consumed by the screen"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneScreen.kt
    - app/src/main/res/values/strings_signup_timezone.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneScreenTest.kt
  modified: []
key-decisions:
  - "The extra (non-bundled) zone is derived from the seed draft zone, so it stays in the list after the user picks another zone"
  - "The ViewModel collects repository.draft so a hydration that finishes after the screen opens still preselects the server zone, until the user chooses"
requirements-completed: [SIGN-02]
duration: long (shared machine, slow Gradle)
completed: 2026-10-21
---

# Phase 4 Plan 13: Signup Timezone Step Summary

**Timezone step with the 10 curated zones, the device or server zone listed first when not bundled, auto-detect, and a `{timezone}`-only PATCH with a 600 ms Saved! hold.**

## Accomplishments
- ViewModel: selection seeded from the shared draft (or `timezone_selected` in SavedStateHandle), entries from `Timezones.pickerEntries()` plus the seed/selected zone first when unlisted, `autoDetect()` via `Timezones.detected()`, `save()` through `repository.saveTimezone`, 422 on `timezone` shown inline, other failures in the dialog.
- Screen per UI-SPEC screen 4: `AuthTopAppBar`, heading, two-line body, `PickerField` (Schedule icon), underlined auto-detect `TextButton` (MyLocation icon), explainer, pinned `AsyncSaveButton`, `PickerSheet` without search, error dialog titled "Couldn't save".
- 13 new tests pass (8 ViewModel, 5 screen).

## Task Commits
1. Task 1: SignupTimezoneViewModel and tests - `bd50770`
2. Task 2: SignupTimezoneScreen, strings, screen tests - `ecc86cd`

## Verification
- `:app:testDevDebugUnitTest --tests "*SignupTimezone*"`, `ktlintCheck`, `lintDevDebug` all succeed; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `repository.saveTimezone(`, `Timezones.pickerEntries`, `Timezones.detected`, `"timezone_selected"`, `Icons.Outlined.MyLocation`, `minHeight = 56.dp` and the auto-detect string all present.

## Deviations from Plan
- **Sheet rows helper.** The plan lists `TimezoneSheetContent(entries, selectedId, onSelect)` for use inside `PickerSheet`, but `PickerSheet` already wraps `PickerSheetContent` and takes a `LazyListScope` lambda. I added `LazyListScope.timezoneSheetRows(...)` (used by `PickerSheet` in the screen) and kept `TimezoneSheetContent` as a composable wrapper over `PickerSheetContent` for tests and previews.
- Tests and implementation for each task were committed together, not as separate RED and GREEN commits (same as plan 04-07). Task 1 tests ran against the final ViewModel.
- ktlint reformatted one chained call in the ViewModel test; the format change is in commit `ecc86cd`.

## Known Stubs
None.

## Threat Flags
None. T-04-35: only bundled ids, the device's valid IANA id, or the server's own saved id are offered; a strict-validator 422 is shown inline.

## Manual checks (deferred by project policy)
- On device with a non-bundled zone (e.g. America/Toronto): confirm it is listed first and preselected.
- Tap Auto-detect after choosing another zone and confirm the selection returns to the device zone.
- Save and confirm the "Saved!" state holds briefly before returning to the hub (needs plan 04-19 navigation wiring).

## Self-Check: PASSED
All five created files exist; commits `bd50770` and `ecc86cd` exist.
