---
phase: 10-expert-topics-availability
plan: 17
subsystem: expert-mode-settings
tags: [gap-closure, cr-01, viewmodel, bottom-sheet, tags]
requires: []
provides:
  - "ExpertiseSeedState (Loading / Failed / Ready) gating Edit Expertise selection edits and Save"
affects: [ExpertiseEditorViewModel, ExpertiseEditorSheet]
tech-stack:
  added: []
  patterns: ["fail-closed seed state gates a full-replace PATCH"]
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorSheet.kt
    - app/src/main/res/values/strings_expert_topics.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorSheetTest.kt
decisions:
  - "Block selection edits until the seed is Ready rather than merging early picks with the late profile (one rule, matches iOS synchronous seeding)"
requirements: [EXP-01]
status: complete
commits: 2
plan_head_before: 72baa1e3504f1989f96bede47f10e7523aafb11c
plan_head_after: 6e97ed0a955d2109cdce6ef79c473f5f939f4d2c
actuals:
  tokens: 9000
  tasks: 2
  commits: 2
completed: 2026-10-25
---

# Phase 10 Plan 17: Edit Expertise seed gate (CR-01) Summary

Edit Expertise can no longer wipe a profile's tags: Save and selection edits are gated on an explicit seed state, and a failed profile load shows Retry instead of being swallowed.

## What changed

- `ExpertiseSeedState` (Loading / Failed(error) / Ready) added; `ExpertiseEditorUiState.seed` defaults to Loading (fails closed).
- `canSave` and `save()` both require `seed == Ready`.
- `toggle` / `remove` / `updateSelection` are ignored until Ready; the private `touched` flag that discarded a late profile response was deleted, so the late seed always lands in full (primary first).
- `loadProfileTags()` sets Loading, then Ready on success or Failed(`toSectionMessage()`) on `ApiException`. `retry()` reloads whichever of catalog / seed is Failed.
- A restored `SavedStateHandle` selection starts Ready (the mirror is only written after Ready).
- Sheet: `TagList` shows a single `PickerLoading("Loading your tags…")` item while Loading, or an `EmptyState` (CloudOff, "Couldn't load your current tags", server message, Retry) while Failed. Preview passes `seed = Ready`.
- Strings: `expertise_editor_seed_loading`, `expertise_editor_seed_error`.

## Commits

- 41da29f: Task 1 (tracer) failed seed disables Save and offers Retry
- 6e97ed0: Task 2 slow seed blocks edits until Ready; late seed never discarded

## Tests

`ExpertiseEditorViewModelTest`: `aProfileLoadFailureFailsTheSeedAndDisablesSave` (replaces the test codifying the bug), `retryAfterASeedFailureSeedsTheSelectionAndEnablesSave`, `aTagTappedBeforeTheSeedLandsIsIgnoredAndTheLateSeedKeepsEveryExistingTag`; restore test asserts Ready and canSave. `ExpertiseEditorSheetTest`: `seedFailureOffersRetryAndDisablesSave`, `seedLoadingShowsTheLoaderAndDisablesSave`; existing states set `seed = Ready`.

Verification: `:app:testDevDebugUnitTest` for `*ExpertiseEditorViewModelTest`, `*ExpertiseEditorSheetTest`, `*SignupTagsScreenTest` plus `ktlintCheck` all pass.

## Deviations from Plan

- Task 1's failed-seed ViewModel test initially asserted that `toggle` is ignored; that behavior belongs to Task 2 (edit blocking), so the assertion was added in Task 2 as the plan's behavior list for Task 2 specifies. No functional deviation.
- `ktlintFormat` was run to fix formatting of new test/source lines.
- The per-plan commit ledger file could not be written (tool refused writes into the shared git dir); `plan_head_before` is the known worktree base SHA, so `commits: 2` is still measured via `git rev-list --count`.

## Known Stubs

None.

## Threat Flags

None. T-10-52 / T-10-53 / T-10-54 mitigated as planned.

## Self-Check: PASSED

Both commits (41da29f, 6e97ed0) are on the branch; all five modified files exist; scoped tests and ktlint green.
