---
phase: 10-expert-topics-availability
plan: 09
subsystem: expert-mode-topics
tags: [viewmodel, compose, topics, pull-to-refresh]
status: complete
requires:
  - "10-04 topics_* strings, toExpertDialogError"
  - "10-05 ExpertTopicsRepository, TopicEvents"
  - "10-06 TopicCard, TopicDeleteDialog, TopicsSkeleton, TopicsEmptyContent, TopicsErrorContent"
provides:
  - "TopicsViewModel (load, retry, refresh, onScreenShown, delete, dismissDialog, onSnackbarShown)"
  - "TopicsUiState, TopicsListState, TopicsDialog, TopicsSnackbar"
  - "TopicsScreen (Hilt) and stateless TopicsContent, TOPICS_ADD_FAB_TAG, TOPICS_AVAILABILITY_ACTION_TAG"
affects: [10-15]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsScreenTest.kt
  modified: []
decisions:
  - "A saved-topic event that arrives while the list still shows the skeleton restarts the load as Initial, because a quiet reload that failed would leave the skeleton forever"
  - "A saved-topic event for an id already in the list replaces it, so a duplicate Added event cannot show the same card twice"
requirements-completed: [EXP-01]
commits: 2
plan_head_before: 524f6cfa67bbcdd96c63e885d041474a5e6591ba
plan_head_after: 97435abe2f1dc415651d881c2fac1e1c78fee41b
completed: 2026-10-25
---

# Phase 10 Plan 09: Topics tab Summary

Topics tab for the expert: a ViewModel that loads, reloads quietly, pulls to refresh, deletes one topic at a time and merges editor saves, plus the screen that assembles the 10-06 components with a FAB, snackbars, the delete confirmation and the Availability app-bar action.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | TopicsUiState and TopicsViewModel | a3c309e |
| 2 | TopicsScreen assembly | 97435ab |

## What was built

- **TopicsViewModel**: three load modes (Initial, Pull, Quiet) guarded by a generation counter. A successful delete cancels the load job and bumps the generation, so a reload in flight cannot put the deleted topic back. `onScreenShown()` reloads quietly only when the list is Loaded and no load is active, which absorbs the `LaunchedEffect` + `ON_RESUME` double fire. `TopicEvents` is collected once; Updated replaces in place, Added appends, then a quiet reload confirms. Delete failures map through `toExpertDialogError()` to a "Couldn't delete topic" dialog; no 409 branch.
- **TopicsScreen / TopicsContent**: skeleton, error with Retry, empty with "Add Topic", or a `PullToRefreshBox` list (88dp bottom padding, `animateItem()`, key = id). FAB only when topics exist. Delete target held in `rememberSaveable`; confirm closes the dialog then calls `onDelete`. Snackbars for added, updated, deleted and refresh failed; server messages render through plain `Text`.

## Verification

- `:app:testDevDebugUnitTest --tests '*TopicsViewModelTest' --tests '*TopicsScreenTest'` (15 + 11 tests, 0 failures), `:app:assembleDevDebug`, `ktlintCheck` and `scripts/check-design-rules.sh` pass.
- Acceptance greps: `topicEvents.events` 1, `409` outside comments 0, `onScreenShown` 2, `rememberSaveable` 2, `topics_add_fab` 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Quiet reload could strand the skeleton**
- **Found during:** Task 1 design
- **Issue:** Plan said a saved-topic event with a non-Loaded list starts a Quiet load. If the list is still Loading and that quiet load fails, the screen would stay on the skeleton with no error and no Retry.
- **Fix:** When the list is Loading, the event restarts an Initial load; when Failed, a Quiet load is used as planned.
- **Files modified:** TopicsViewModel.kt
- **Commit:** a3c309e

TDD note: tests were written alongside the implementation in the same commit per task rather than as separate RED commits. The per-plan ledger file was not written (the sandbox rejects compound git commands), so `plan_head_before` is the known spawn base and `commits` was measured with `git rev-list --count 524f6cf..HEAD` (2 at the time of the second task commit).

## Known Stubs

None.

## Threat Flags

None. T-10-26: state lives in the entry-scoped ViewModel; T-10-27: one delete at a time plus generation counter and job cancel on delete success; T-10-28: server text only in Compose `Text`.

## Self-Check: PASSED

All five created files exist; commits a3c309e and 97435ab are ancestors of HEAD.
