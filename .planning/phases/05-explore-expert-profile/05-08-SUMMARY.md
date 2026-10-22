---
phase: 05-explore-expert-profile
plan: 08
subsystem: ui
tags: [bottom-sheet, categories, multi-select, viewmodel, compose]
requires:
  - phase: 05-explore-expert-profile
    provides: CategoryStore (05-04), CategoryIconMap and PickerRow Checkbox role (05-03), section_retry string
provides:
  - CategoriesSheetViewModel with store-backed catalog, local search and recreatable draft
  - AllCategoriesSheet (PickerSheet) and stateless AllCategoriesSheetContent
  - strings_all_categories.xml (9 UI-SPEC strings)
affects: [05-10 explore screen wiring]
tech-stack:
  added: []
  patterns: [PickerSheet multi-select with Role.Checkbox rows, SavedStateHandle-backed draft]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/categoriesmodal/CategoriesSheetViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/categoriesmodal/AllCategoriesSheet.kt
    - app/src/main/res/values/strings_all_categories.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/categoriesmodal/CategoriesSheetViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/categoriesmodal/AllCategoriesSheetTest.kt
  modified: []
key-decisions:
  - "Explicit Apply: only Apply hands the draft to the caller; drag, scrim and Back call onDismiss and discard it"
  - "Retry uses CategoryStore.refresh(); first load uses fetch() so a warm cache shows with no network call"
  - "Concurrent ensureLoaded() calls are coalesced by an in-flight Job guard"
requirements-completed: [DISC-02]
duration: 25min
completed: 2026-10-21
---

# Phase 5 Plan 08: All Categories Sheet Summary

**Searchable multi-select All Categories bottom sheet over the 7-day-cached CategoryStore, with checkbox rows, icons, Clear/Apply footer and loading/error/no-match states.**

## Accomplishments
- `CategoriesSheetViewModel` (`@HiltViewModel`): `CategoryCatalog` Loading/Loaded/Failed, `visible` (case-insensitive substring, server order) and `noMatch`, `open/ensureLoaded/onQueryChange/toggle/clear/retry`. Draft (sorted ArrayList) and query persist in `SavedStateHandle` under `categories_sheet_draft` / `categories_sheet_query`.
- `AllCategoriesSheet` / `AllCategoriesSheetContent`: shared `allCategoriesItems` list scope; `PickerRow(role = Role.Checkbox, leadingIcon = CategoryIconMap.icon(id), minHeight 56dp)`; tonal Retry, tonal Clear, filled Apply (always enabled).
- Tests: 9 ViewModel tests (real `CategoryStore` over `FakeClickMeApi`, asserting `categoriesCalls` for the cache claims) and 5 Robolectric UI tests; all pass. `ktlintCheck` and `check-design-rules.sh` pass.

## Task Commits
1. Task 1 RED: `2d19b87` test(05-08) failing ViewModel test
2. Task 1 GREEN: `26fdde9` feat(05-08) CategoriesSheetViewModel
3. Task 2 RED: `2db8855` test(05-08) failing sheet test and strings
4. Task 2 GREEN: `757b72d` feat(05-08) AllCategoriesSheet

## Deviations from Plan

None in scope. Notes:
- Worktree base was behind the expected base (`d1217fb` vs `c95d981`); reset to `c95d981` per the startup check, before any work.
- The error and no-match states use a private `StateColumn` rather than the shared `EmptyState`, because `EmptyState`'s action is a filled button and the plan requires a tonal Retry (Apply is the sheet's only filled control). The no-match state uses `Icons.Outlined.SearchOff`.
- Added a draft-persistence test (`mutationsPersistToSavedStateHandle`) and a `retryBypassesWarmCache` test beyond the listed behaviors.

## Known Stubs
None.

## Threat Flags
None. Search is local only (T-05-26); names render as plain Text (T-05-27).

## Next Phase Readiness
05-10 can call `viewModel.open(selection)` before showing `AllCategoriesSheet(viewModel, onApply, onDismiss)` and apply the returned slug set.

## Self-Check: PASSED
