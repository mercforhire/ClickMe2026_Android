---
phase: 10-expert-topics-availability
plan: 18
subsystem: expert-topics
tags: [gap-closure, wr-02, wr-05, topic-editor, wire-format]
status: complete
requires:
  - phase: 10-expert-topics-availability
    provides: Topic Editor, PATCH adapter and tag picker (10-01..10-16)
provides:
  - "UpdateExpertTopicRequest.durationMins nullable; duration_mins omitted from the PATCH when unchanged"
  - "seedDuration keeps the stored duration; stepDuration snaps onto the 30..480 grid"
  - "TopicTagPicker shows selected tags as removable chips when the profile tags fail to load"
affects: [10-20]
tech-stack:
  added: []
  patterns:
    - "Nullable field in a hand-written Moshi adapter means key omitted (not explicit null)"
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ExpertRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/NullPreservingAdapters.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRules.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicTagPicker.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertTopicRequestJsonTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRulesTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicEditorSubviewsTest.kt
key-decisions:
  - "Edit save() omits duration_mins when the current value equals TopicEditorRules.seedDuration(seed.durationMins), so a topic with a null stored duration (shown as the 60 default) is also left untouched"
  - "Failed-tag state shows removable chips instead of disabling Save; the server profile-subset 422 stays authoritative"
requirements-completed: [EXP-01, EXP-02]
duration: 25min
completed: 2026-10-25
commits: 3
plan_head_before: 72baa1e3504f1989f96bede47f10e7523aafb11c
plan_head_after: 97da110
actuals:
  tokens: 9000
  tasks: 3
  commits: 3
---

# Phase 10 Plan 18: Topic duration preserve and failed-state tag chips Summary

An unrelated topic edit no longer rewrites the stored session length (WR-02), and the tag picker shows removable chips for the selected tags when the profile tags fail to load (WR-05).

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Unchanged duration left out of the topic PATCH (wire path, tracer) | 60ebfc2 |
| 2 | Stored durations display as stored; stepper snaps onto the grid | 190d203 |
| 3 | Selected tags stay visible and removable when profile tags fail to load | 97da110 |

## What changed

- `UpdateExpertTopicRequest.durationMins` is `Int?`. `UpdateExpertTopicRequestAdapter` writes `duration_mins` only when non-null (not via `writeNullable`, because the server's zod `.optional()` rejects null). `description` and `icon_slug` still go out as explicit nulls. `CreateExpertTopicRequest` is unchanged.
- `TopicEditorViewModel.save()` (edit branch) sends `durationMins` only when it differs from the seeded duration.
- `TopicEditorRules.seedDuration` returns a positive stored value unchanged (null or non-positive gives 60). `stepDuration` returns the current value when that direction is disabled, otherwise moves to the next 30-minute grid value above or below and clamps to 30..480 (45 up gives 60, 45 down gives 30, 600 down gives 480, 600 up stays 600).
- `TopicTagPicker` `loadFailed` branch renders the selected tags (matched with `Locale.ROOT` lower-casing) as removable `SelectableChip`s above the Retry row.

## Verification

`./gradlew :app:testDevDebugUnitTest --tests '*TopicEditorSubviewsTest' '*TopicEditorViewModelTest' '*TopicEditorScreenTest' '*TopicEditorRulesTest' '*ExpertTopicRequestJsonTest' '*ExpertApiTest' '*ExpertRepositoriesTest' ktlintCheck` is green, with 0 failures in the topic-editor result XMLs.

New or changed tests: `patchBodyOmitsAnUnchangedDuration`, `anUnchangedDurationIsLeftOutOfThePatch`, `aChangedDurationIsSentInThePatch`, `aStoredDurationAboveTheCapIsShownAndKeptOnAnUnrelatedEdit`, `anOffGridStoredDurationIsKeptUntilTheUserStepsIt`, `aTagRemovedWhileTheProfileTagsFailedIsNotSent`, `seedDuration_keepsTheStoredValue`, `stepDuration_snapsOffGridAndOutOfRangeValuesOntoTheGrid`, `picker_loadFailed_stillShowsSelectedTagsAsRemovableChips`. `editSendsAFullBodyWithClearedFieldsAndKeepsItsCurrency` expectation now `durationMins = null`.

## Deviations from Plan

**1. [Rule 1 - Bug avoidance] Duration compared with the displayed seed, not the raw one**
- **Issue:** The plan compared `current.durationMins == seed.durationMins`. A stored null duration is displayed as the 60 default, so 60 != null would have sent 60 on an unrelated edit.
- **Fix:** Compare against `TopicEditorRules.seedDuration(seed.durationMins)`.
- **Commit:** 60ebfc2

**2. [Style] ktlint chained-call rule** forced two test assertions to use a local variable or helper (`selectedIds(vm)`) instead of `.single().second.durationMins` chains. No behavior change.

Also, RED-first was not run as a separate failing build for each task. Tests and implementation were written together and verified green in one pass.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

All nine files above exist and commits 60ebfc2, 190d203 and 97da110 are on the branch.
