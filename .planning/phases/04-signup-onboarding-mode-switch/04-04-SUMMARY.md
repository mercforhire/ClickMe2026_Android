---
phase: 04-signup-onboarding-mode-switch
plan: 04
subsystem: ui-components
tags: [compose, material3, modal-bottom-sheet, components, accessibility]
requires: []
provides:
  - components.PickerField
  - components.SelectableChip
  - components.PickerSheet / PickerSheetContent / PickerRow / PickerLoading
  - res/values/strings_pickers.xml
affects: [personal-details, timezone, expertise-tags]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PickerField.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SelectableChip.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PickerSheet.kt
    - app/src/main/res/values/strings_pickers.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/PickerComponentsTest.kt
  modified: []
key-decisions:
  - "PickerSheet delegates to stateless PickerSheetContent so semantics are testable under Robolectric without a window"
  - "Selected-row check icon carries an internal test tag (PICKER_ROW_CHECK_TAG) since it has no content description"
requirements-completed: [SIGN-02]
duration: ~25 min
completed: 2026-10-21
---

# Phase 4 Plan 04: Shared Picker Components Summary

PickerField, SelectableChip and PickerSheet (with PickerRow and PickerLoading) built as the shared Wave 1 components for the Personal Details, Timezone and Expertise Tags screens, with 15 Robolectric tests.

## Tasks

1. **PickerField and SelectableChip** - `980d5e1` (test, RED: compile failure), `2c6f179` (feat). PickerField is a pill, 56 dp row with outline or 2 dp destructive border, `Role.Button`, and a merged description "$placeholder, $value". SelectableChip is at least 48 dp tall, with a close icon and the "Remove {label}" description when selected.
2. **PickerSheet, PickerSheetContent, PickerRow, PickerLoading** - `2692646`. ModalBottomSheet uses `skipPartiallyExpanded = true` and an IME-padded body: heading title, optional ValidatedField search with a clear button, LazyColumn, optional footer. PickerRow uses `selectable` semantics with an accent check and an outlineVariant divider. PickerLoading shows a 48 dp primary spinner and muted text.

## Verification

- `:app:testDevDebugUnitTest --tests '*PickerComponentsTest'`: 15 tests, 0 failures.
- `ktlintCheck` and `lintDevDebug` pass; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps pass: `skipPartiallyExpanded = true`, `imePadding()`, `PickerSheetContent(` (3 occurrences), `heightIn(min = 48.dp)`, `Role.Button`, no `fontSize`.

## Deviations from Plan

None - plan executed as written. Test count is 15, above the plan's minimum of 11. The RED step of task 2 was verified as a compile failure but not committed separately; the tests and implementation went into one commit.

## Manual checks deferred (project policy)

- Open PickerSheet on a device and confirm it opens fully expanded and rides above the keyboard when the search field is focused (Pitfall 13).
- Confirm TalkBack reads "Country, Canada" for PickerField and "Remove English" for a selected chip.

## Known Stubs

None.

## Threat Flags

None. Search text is used only for local filtering by callers (T-04-09 accepted).

## Self-Check: PASSED

All five files exist; commits 980d5e1, 2c6f179 and 2692646 exist.
