---
phase: 10-expert-topics-availability
plan: 13
subsystem: expert-mode-topic-editor
tags: [compose, screen, topic-editor]
status: complete
requires: [10-07, 10-10, 10-11]
provides:
  - "TopicEditorScreen (ViewModel wiring) and stateless TopicEditorContent"
  - "TopicEditorActions callback bundle"
affects: [10-15]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorScreenTest.kt
  modified: []
key-decisions:
  - "The sheet-open flag lives in TopicEditorContent (rememberSaveable) so the stateless content owns the add-expertise hand-off and the test can drive it with a stub slot"
  - "Each sheet opening gets its own ViewModel key via a rememberSaveable UUID created inside the slot, which is only composed while the sheet is open"
requirements-completed: [EXP-01, EXP-02]
duration: 25min
completed: 2026-10-25
actuals:
  tasks: 2
  commits: 2
commits: 2
plan_head_before: db9cdc1fd2a40dda2a25e4078b163140b404e2a1
plan_head_after: 8d6a8882cde64e040951ddc387841bdae69f47e7
---

# Phase 10 Plan 13: Topic Editor screen Summary

Full-screen Topic Editor assembled from the 10-07 sections over the 10-10 ViewModel: app-bar Save that becomes a spinner, Back and field locking while saving, floor helper and error routing on the price field, focus on the first failing field, a "Couldn't save topic" dialog, pop on success and the Edit Expertise sheet hand-off.

## What was built

- **Task 1** (`3942618`): `TopicEditorScreen(viewModel, onBack)` collects state, pops through `LaunchedEffect(state.finished)` and supplies the `expertiseSheet` slot (`hiltViewModel(key = "expertise_editor_$session")`). `TopicEditorContent` has `BackHandler(enabled = state.isSaving)`, `AuthTopAppBar` with `backEnabled = !state.isSaving`, the Save action (text button with a11y label and click label, or a 24 dp spinner announcing "Saving topic"), the UI-SPEC section order, `enabled = !isSaving` on every field, `FocusRequester`s for title and price driven by `focusTarget` then `onFocusHandled`, price helper/error mapping (BelowFloor, Invalid, Server message or generic) and the save-error dialog.
- **Task 2** (`8d6a888`): `TopicEditorScreenTest`, 16 Robolectric cases on `TopicEditorContent` with a recording `TopicEditorActions` and a stub sheet slot; money strings computed with `Money.format`.

## Verification

`:app:assembleDevDebug ktlintCheck`, `scripts/check-design-rules.sh` (OK) and `:app:testDevDebugUnitTest --tests '*TopicEditorScreenTest' '*TopicEditorViewModelTest' '*TopicEditorSubviewsTest'` pass (74 tests, 0 failures). Acceptance greps: `BackHandler(enabled = state.isSaving)` 1, `backEnabled = !state.isSaving` 1, `expertise_editor_` 1, `fontSize` 0.

## Deviations from Plan

**1. Sheet flag location.** The plan listed `showExpertiseSheet` / `expertiseSession` in `TopicEditorScreen`, but the behavior spec requires clicking Add inside `TopicEditorContent` to invoke the slot. The flag therefore lives in `TopicEditorContent`; the per-opening session id is created inside the slot lambda (a `rememberSaveable` UUID), which gives the same one-ViewModel-per-opening behavior and survives rotation. No Rule 1-4 fixes were needed.

**2. Test order.** Task 2 is marked `tdd="true"` but tests were written after the screen (the screen is a UI assembly with no behavior to RED against); both tasks are committed separately.

**3. Disabled-field assertion.** A disabled Compose text field exposes no SetText action, so the "fields locked while saving" test asserts the title input node is absent plus the Add Expertise button not enabled.

**Not covered by automated tests:** the actual focus movement into a field (only that `onFocusHandled` is called once) and scroll-into-view; both rely on Compose's focused-field bring-into-view behavior and are left for manual verification on device.

## Known Stubs

None.

## Threat Flags

None. T-10-42 (server text rendered as plain `Text` via `ValidatedField` / `AuthErrorDialog`) and T-10-43 (Back, arrow and fields locked while saving) are implemented as planned.

## Self-Check: PASSED

Both created files exist; commits `3942618` and `8d6a888` are in HEAD history; `commits: 2` measured with `git rev-list --count db9cdc1..HEAD`.
