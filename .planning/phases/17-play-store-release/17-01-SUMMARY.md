---
phase: 17-play-store-release
plan: 01
subsystem: viewmodel-state
tags: [pol-02, savedstatehandle, process-death, viewmodel, d-09, tracer]

requires:
  - phase: 16.x
    provides: BookingNoteViewModel, FeedbackViewModel, SearchExpertViewModel (memory-only state)
provides:
  - BookingNoteViewModel, FeedbackViewModel and SearchExpertViewModel restore user input after process death via an injected SavedStateHandle
  - JVM tests for restore, mirror, clear and validation on all three screens
affects: [17-07 release build (vc 6), POL-02 requirement closure]

actuals:
  tokens: 7600
  tasks: 3
  commits: 6
plan_head_before: 25942db1b3ae502aa5036bd56faeed737fa75bec
plan_head_after: b6ff259f75d566dbfebcbc8270a7b47fc188597e
commits: 6

tech-stack:
  added: []
  patterns:
    - "SavedStateHandle mirror-on-edit, validate-on-restore, clear-on-success (WriteReviewViewModel shape)"

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/bookingnote/BookingNoteViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/feedback/FeedbackViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/search/SearchExpertViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/search/SearchExpertUiState.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/bookingnote/BookingNoteViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/bookingnote/BookingNoteScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/feedback/FeedbackViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/search/SearchExpertViewModelTest.kt

key-decisions:
  - "Phase 17 D-09 supersedes Phase 16.1 D-02 (Search), Phase 16.5 RESEARCH V8 (Feedback) and Phase 16.4 A4 (note): the three screens now restore after process death"
  - "Feedback email prefill is never stored; an explicit feedback_email_edited flag keeps a user-cleared email empty after restore"

requirements-completed: [POL-02]

coverage:
  - id: D1
    description: "Private booking note draft survives process death, capped at NOTE_MAX_LENGTH, hasChanges true over the server note, key cleared after save"
    requirement: POL-02
    verification:
      - kind: unit
        ref: "BookingNoteViewModelTest#restoresTheNoteDraftAfterProcessDeath, editsAreMirroredToSavedState, savedDraftIsClearedAfterASuccessfulSave, restoredDraftIsCappedAtTheNoteMaximum"
        status: pass
    human_judgment: false
  - id: D2
    description: "Send Feedback type, details and user-edited email survive process death; prefill never stored; stale type falls back; cleared after send; toString redacted"
    requirement: POL-02
    verification:
      - kind: unit
        ref: "FeedbackViewModelTest#restoresTheFormAfterProcessDeath, signedInPrefillIsNotWrittenToSavedState, aClearedEmailStaysClearedAfterProcessDeath, staleRestoredTypeFallsBackToTheFirstType, savedFormIsClearedAfterASuccessfulSend, toStringStaysRedactedWithRestoredText"
        status: pass
    human_judgment: false
  - id: D3
    description: "Search query, category chip and sort survive process death and the first search runs at once without debounce"
    requirement: POL-02
    verification:
      - kind: unit
        ref: "SearchExpertViewModelTest#restoresQueryCategoryAndSortAfterProcessDeath, restoredQueryIsSearchedAtOnceWithoutDebounce, editsAreMirroredToSavedState, unknownSavedSortFallsBackToMostPopular"
        status: pass
    human_judgment: false
  - id: D4
    description: "Real process-death restore on a device (SavedStateHandle round trip through the system Bundle)"
    verification: []
    human_judgment: true
    rationale: "JVM tests prove the ViewModel logic with a SavedStateHandle; the system Bundle round trip after a real kill (adb shell am kill) is a device check, deferred per the manual-testing policy"

duration: 14min
completed: 2026-11-06
status: complete
---

# Phase 17 Plan 01: POL-02 process-death restore Summary

**The private booking note, Send Feedback and Search tab now restore the user's typed input after process death through an injected `SavedStateHandle`, following the `WriteReviewViewModel` pattern, with 14 new JVM tests.**

## Performance

- **Duration:** about 14 min
- **Tasks:** 3 of 3
- **Files modified:** 8 (4 main, 4 test)
- **Commits:** 6 (3 RED test commits, 3 GREEN fix commits)

## Accomplishments

- **Booking note (tracer, Task 1):** the draft is mirrored under `note` on every edit (already capped), restored over the loaded server note in `load()` with a second `capText` cap, and removed after a successful save. `lastPersisted` stays the server note, so a differing draft keeps `hasChanges` true and the discard prompt and Save still work. The `Factory` interface is unchanged.
- **Send Feedback (Task 2):** type, details and an email the user edited are mirrored; `_uiState` is seeded from the handle. On load a restored type is kept only if the loaded list contains it, otherwise the first type. The signed-in prefill is never written; `feedback_email_edited` makes a user-cleared email stay empty after restore. All four keys are removed after a successful send. `toString()` is unchanged and still redacted.
- **Search (Task 3):** query, chip slug and sort are mirrored and seed `_uiState` before `init`, so the existing `runSearch(Fresh)` searches the restored values at once with no debounce and no second search. An unknown saved sort falls back to Most Popular.
- The three KDocs (and the `SearchExpertUiState` KDoc) that claimed the opposite are rewritten to describe the restore.

## Task Commits

| Task | Phase | Commit | Message |
| ---- | ----- | ------ | ------- |
| 1 | RED | 8646936b | test(17-01): add failing tests for the booking note draft restore |
| 1 | GREEN | e3ff2712 | fix(17-01): POL-02 restore the booking note draft after process death |
| 2 | RED | 5b22c94c | test(17-01): add failing tests for the feedback form restore |
| 2 | GREEN | 14541ada | fix(17-01): POL-02 restore the feedback form after process death |
| 3 | RED | 90577754 | test(17-01): add failing tests for the search query, chip and sort restore |
| 3 | GREEN | b6ff259f | fix(17-01): POL-02 restore the search query, chip and sort after process death |

## TDD Gate Compliance

RED commit precedes GREEN for each task. RED evidence in each case is a compile failure on the new `SavedStateHandle` constructor parameter, which the plan explicitly accepts as RED ("compile failure on the constructor counts as RED"). Under the stricter #3770 rule this is a `build_failure` rather than an assertion failure, so the new assertions were not individually seen failing before GREEN; they all pass after GREEN (BookingNoteViewModelTest 30, FeedbackViewModelTest 36, SearchExpertViewModelTest 27 tests, 0 failures). No REFACTOR commits were needed.

## Verification

- `:app:testDevDebugUnitTest` for the `bookingnote.*`, `settings.feedback.*` and `clientmode.search.*` packages together plus `ktlintCheck`: BUILD SUCCESSFUL.
- `./scripts/check-design-rules.sh`: `Design rules: OK`.
- Acceptance greps: SavedStateHandle import present once in each of the three ViewModels; the three old "no saved state" KDoc phrases are gone (0); `Log.` count is 0 in all three ViewModels; feedback `toString()` unchanged (1); test-name greps print 4, 6 and 4.
- `git log --grep='(17-01): POL-02'` lists three commits.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Draft key removal after save is conditional**
- **Found during:** Task 1
- **Issue:** The plan removes the draft key unconditionally after a successful save. Text typed while the save was in flight (`typingDuringASaveStaysDirty` scenario) is still an unsaved draft, so removing the key would lose it if the process died right after.
- **Fix:** Remove the key only when `normalizeNote(current noteText) == body`. `savedDraftIsClearedAfterASuccessfulSave` still passes.
- **Files modified:** BookingNoteViewModel.kt
- **Commit:** e3ff2712

**2. [Process] Separate RED and GREEN commits instead of one combined commit per task**
- The plan says to commit test and code together; the executor instructions require a failing-test commit first (tdd.md). Followed the executor instructions. The `fix(17-01): POL-02` commits still number three, as the plan's verification expects.

**3. [Additional test] `FeedbackViewModelTest.editsAreMirroredToSavedState`** added to cover type and details mirroring on edit (the plan's list covered only the email key and the clear/restore paths).

**Total deviations:** 1 auto-fixed (Rule 2), 2 process notes. **Impact:** none on scope.

## Flagged Assumption

- **RESEARCH A15:** `SavedStateHandle` restores only after process death while the task is still in recents, never on a cold start, so a draft never outlives the task. Not verifiable on the JVM; it is documented Android behavior and was not exercised on a device here.

## Known Stubs

None.

## Threat Flags

None. No new endpoints, auth paths or file access. Threat register: T-17-01 (no `Log.` calls, `toString` redacted and tested), T-17-02 (restore capped, tested), T-17-03 (prefill not stored, tested) are mitigated.

## Issues Encountered

- The first Task 1 ktlint run failed on a line-length wrap in `BookingNoteScreenTest` (my edit); fixed before the GREEN commit. A chain-continuation lint in the new Feedback test was likewise fixed before its commit.
- Not run: the full ~5000-test suite and any on-device process-death check (`adb shell am kill`); only the three targeted packages were run, as the plan's verify commands specify.

## Self-Check: PASSED

- Modified files exist on disk (4 main, 4 test).
- Commits 8646936b, e3ff2712, 5b22c94c, 14541ada, 90577754, b6ff259f are ancestors of HEAD (`git rev-list --count 25942db1..HEAD` = 6).
