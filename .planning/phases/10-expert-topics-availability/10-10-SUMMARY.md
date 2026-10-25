---
phase: 10-expert-topics-availability
plan: 10
subsystem: expert-mode-topics
tags: [viewmodel, assisted-inject, saved-state, money, topics]
status: complete
requires: [10-01, 10-03, 10-04, 10-05]
provides:
  - "TopicEditorKey (newTopic, forTopic), TopicEditorSeed, TopicEditorSeedTag"
  - "TopicEditorUiState, PriceFieldError, TopicTagsLoad, EditorField"
  - "Assisted TopicEditorViewModel: form edits, profile tags, live and on-Save price floor, create/patch save"
affects: [10-13 topic editor screen, 10-11 topics list]
tech-stack:
  added: []
  patterns:
    - "assisted Hilt ViewModel with SavedStateHandle mirroring and restore through the same caps and filters"
    - "floor read from the in-memory /me at validation time, one background refresh when limits are missing"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorKeyTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModelTest.kt
  modified: []
key-decisions:
  - "A finished save sets finished=true and isSaving=false; save() also refuses once finished, so a double tap during the pop cannot resubmit"
  - "Restored tag selections are de-duplicated by lower-cased id and capped at 3 before the profile load applies its drop rule"
  - "Add-mode currency and floor are recomputed on every /me emission; Edit-mode currency is fixed at open (D-10)"
requirements-completed: [EXP-01, EXP-02]
duration: 55min
completed: 2026-10-25
actuals:
  tasks: 2
  commits: 2
commits: 2
plan_head_before: 524f6cfa67bbcdd96c63e885d041474a5e6591ba
plan_head_after: c28a20aac3f1a0ba2cce642b970fe4adeb27fa0a
---

# Phase 10 Plan 10: Topic Editor key and ViewModel Summary

Serializable editor key seeded from the list item plus an assisted ViewModel that edits, restores, validates the price floor live and on Save, and saves via POST or PATCH with `PRICE_BELOW_MINIMUM` routed to the price field.

## What was built

- **Task 1** (`1af760a`): `TopicEditorKey` / `TopicEditorSeed` (round-trips through its generated serializer; Edit never fetches), `TopicEditorUiState` with `PriceFieldError`, `TopicTagsLoad`, `EditorField` and derived `visibleTags` / `atCap` / `selectedIds`, and the ViewModel: seeding (price shown as "12.50", free topics blank, duration 60 for Add), SavedStateHandle overlay and write-back, currency rules (D-10), `/me` collection for floor and Add-mode currency, one background `/me` refresh when limits are null, fresh profile-tag load that drops selections not on the profile and caps at 3, `onExpertiseSaved`, duration and icon toggles.
- **Task 2** (`c28a20a`): `save()` single-flight with title-then-price validation and focus, floor read at save time, exact create body and full PATCH body (cleared icon/description are null), free topic wire amount (floor, else 100), `TopicSavedEvent(Added|Updated)` then `finished`, `PRICE_BELOW_MINIMUM` as `PriceFieldError.Server` with price focus, other failures through `toExpertDialogError()` in a dialog with all fields kept. No call guard on topic save (UI-SPEC Surface 7).

## Verification

`:app:testDevDebugUnitTest --tests '*TopicEditorKeyTest' --tests '*TopicEditorViewModelTest' :app:assembleDevDebug ktlintCheck` passes: 4 key tests and 34 ViewModel tests, 0 failures. Acceptance greps: `@AssistedFactory` 1, `refreshMe()` 1, `ErrorCode.PriceBelowMinimum` 1, `topicEvents.emit` 1, `callGuard` 0, no DataStore/SharedPreferences outside comments.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] JSON round-trip not possible in the unit-test source set**
- **Found during:** Task 1 verify
- **Issue:** `kotlinx-serialization-json` is not on the test compile classpath (only `-core` is), so `Json.encodeToString` did not compile.
- **Fix:** `TopicEditorKeyTest` round-trips through the generated `KSerializer` with a small positional `AbstractEncoder`/`AbstractDecoder` defined in the test, instead of adding a build dependency (avoids touching shared Gradle files in a parallel wave). It still proves every field, null and the nested tag list encode and decode.
- **Files modified:** TopicEditorKeyTest.kt
- **Commit:** 1af760a

**Total deviations:** 1 auto-fixed (Rule 3). **Impact:** none on the production code.

## Known Stubs

None.

## Threat Flags

None. T-10-30 (no persistence of pricing_limits, floor read from `userManager.me.value` at validation time), T-10-31 (restored values pass through caps, `filterInput`, `parse`) and T-10-32 (single flight) are mitigated as planned; T-10-29 and T-10-33 accepted.

## Self-Check: PASSED

- All five created files exist; commits `1af760a` and `c28a20a` are in HEAD history; `commits: 2` measured with `git rev-list --count 524f6cf..HEAD`.
