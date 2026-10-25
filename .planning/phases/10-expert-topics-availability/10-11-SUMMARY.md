---
phase: 10-expert-topics-availability
plan: 11
subsystem: expert-mode
tags: [bottom-sheet, viewmodel, tags, refactor]
status: complete
requires: [10-04, 10-05]
provides:
  - components/ExpertiseTagPickerParts.kt (ExpertiseTagSearchField, SelectedTagChips, ExpertiseTagNoMatch)
  - ExpertiseEditorViewModel (catalog + profile seed, toggle, primary-first order, save)
  - ExpertiseEditorSheet and stateless ExpertiseEditorContent
affects: [10-13]
tech-stack:
  added: []
  patterns:
    - stateless content composable behind a Hilt-aware ModalBottomSheet wrapper
    - SavedStateHandle mirror of selection and query, profile seed skipped when restored
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ExpertiseTagPickerParts.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorSheet.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorSheetTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsScreen.kt
decisions:
  - Empty selection cannot be saved (VM guard plus disabled button and the iOS message), resolving research open question 2 with the research recommendation
  - Tag ids are compared case-insensitively (lower-cased, Locale.ROOT) so profile ids and catalog ids of different case do not duplicate
  - Files mirror iOS Expert Mode/Settings/Subviews (CLAUDE.md folder rule) instead of UI-SPEC's topics/editor/subviews
requirements: [EXP-01]
commits: 2
plan_head_before: 524f6cfa67bbcdd96c63e885d041474a5e6591ba
plan_head_after: 5390290d88af093bee79cc4346e6b95026ceccc5
completed: 2026-10-25
---

# Phase 10 Plan 11: Edit Expertise sheet Summary

The inline "Edit Expertise" bottom sheet: an expert can search the full tag catalog, pick tags (first is primary) and save them with PATCH `/expert/profile`, reusing the signup tag picker parts.

## What was built

- **Task 1** (`449c927`): moved the signup search field, selected-chips block and no-match block into `components/ExpertiseTagPickerParts.kt` as public components (signup defaults unchanged) and switched `SignupTagsScreen` to them (private `SearchField` / `NoMatch` removed, unused imports dropped). Added `ExpertiseEditorViewModel`: parallel catalog and profile load, profile seeded primary-first (not overwriting an edit made before the profile arrives), `rows` filter, order-preserving `toggle` / `remove`, single-flight `save()` that ignores an empty selection, `ApiException` mapped through `toExpertDialogError()`, selection and query mirrored to `SavedStateHandle`. 10 ViewModel tests.
- **Task 2** (`5390290`): `ExpertiseEditorSheet` (`ModalBottomSheet`, `skipPartiallyExpanded`, `confirmValueChange` and `onDismissRequest` ignore dismissal while saving, `BackHandler` while saving, `LaunchedEffect(saved)` hands tags to the caller then consumes) and stateless `ExpertiseEditorContent` with title, search, chips or the pick-one line, catalog states, pinned Cancel / Save Expertise row (test tags `expertise_sheet_save`, `expertise_sheet_cancel`) and the "Couldn't save" dialog. 9 sheet tests.

## Verification

`:app:testDevDebugUnitTest --tests '*ExpertiseEditorSheetTest' '*ExpertiseEditorViewModelTest' '*SignupTagsScreenTest' '*SignupTagsViewModelTest'`, `:app:assembleDevDebug`, `ktlintCheck` and `scripts/check-design-rules.sh` all pass. The existing signup tags test files were not edited. Acceptance greps: `private fun SearchField` 0, `ExpertiseTagSearchField` 2 in SignupTagsScreen, `skipPartiallyExpanded = true` 1, `expertise_editor_pick_one` 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint chain-wrapping in tests**
- **Found during:** Task 1 verify
- **Issue:** three `assertTrue(x.y.isEmpty())` lines violated ktlint_official chain wrapping.
- **Fix:** replaced with `assertEquals(emptyList<ExpertiseTagItem>(), ...)`.
- **Commit:** 449c927

**2. [Rule 2 - Correctness] Case-insensitive tag id matching**
- **Found during:** Task 1 design
- **Issue:** the save path lower-cases ids (T-10-18) so profile and catalog ids can differ in case; comparing exactly would show a profile tag unchecked and allow duplicates.
- **Fix:** `selectedIds` and `toggle` / `remove` compare lower-cased ids (Locale.ROOT); tested.
- **Commit:** 449c927

**3. Save button look while saving.** The button is disabled while saving (`canSave` false) but keeps the filled primary colors behind the spinner so it does not grey out.

Nothing is wired yet: the caller (10-13) opens the sheet with a per-opening `hiltViewModel(key = ...)`. EXP-01 is therefore not marked complete by this plan.

## Known Stubs

None.

## Threat Flags

None. T-10-34 (empty-save guard, tested), T-10-35 (typed `UpdateExpertTagsRequest`, only `expertise_tags`) and T-10-36 (single flight, not dismissible while saving) are mitigated as planned.

## Self-Check: PASSED

- All five created files and the modified `SignupTagsScreen.kt` exist; commits `449c927` and `5390290` are in HEAD history; `git rev-list --count` from the spawn base is 2.
