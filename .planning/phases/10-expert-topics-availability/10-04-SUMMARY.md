---
phase: 10-expert-topics-availability
plan: 04
subsystem: expert-mode-foundations
tags: [copy-contract, call-guard, components, errors]
requires: []
provides:
  - "strings_expert_topics.xml and strings_expert_availability.xml (full Phase 10 copy)"
  - "RestrictedAction.EditAvailability with guard copy"
  - "SelectableChip(enabled)"
  - "Motion.AVAILABILITY_SAVED_HOLD_MS"
  - "ApiException.toExpertDialogError()"
  - "topicDurationText(minutes)"
affects: [10-05, 10-06, 10-07, 10-08, 10-09, 10-10, 10-11, 10-12]
key-files:
  created:
    - app/src/main/res/values/strings_expert_topics.xml
    - app/src/main/res/values/strings_expert_availability.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertErrors.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicFormatting.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertErrorsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicFormattingTest.kt
  modified:
    - app/src/main/res/values/strings_call.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallDialog.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogs.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SelectableChip.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Motion.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/PickerComponentsTest.kt
decisions:
  - "toExpertDialogError returns the first non-blank field-error message before the envelope message, so 422 responses never show 'Validation failed'"
  - "SelectableChip.enabled is inserted before modifier; all existing callers use named arguments so none changed"
status: complete
commits: 2
plan_head_before: bc1f77406ae16cf9279280e323115b90dcd6b967
plan_head_after: 79c666d992a160db0e363f423baa7aea3eb848b2
---

# Phase 10 Plan 04: Shared foundations Summary

Phase 10 copy contract (73 topics/editor/expertise strings, 35 availability strings), the `EditAvailability` call-guard entry, a disable-able `SelectableChip`, the 2 s Saved hold constant, a shared expert error mapper and the iOS-style duration text helper.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Copy contract and the EditAvailability guard entry | 0b4f6b7 |
| 2 | Disable-able SelectableChip, Saved hold constant, expert error mapper and duration text | 79c666d |

## Verification

- Task 1: `CallDialogsTest`, `assembleDevDebug`, `ktlintCheck` pass; string counts 73 and 35; guard dialog reads "End the call to edit your availability."
- Task 2: `PickerComponentsTest` (19, incl. new disabled case), `ExpertErrorsTest` (7), `TopicFormattingTest` (1), `SignupTagsScreenTest`, `ktlintCheck` and `scripts/check-design-rules.sh` pass.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None.

## Threat Flags

None. T-10-13 (server text rendered as plain `Text` via `FieldError.Text`), T-10-14 (`EditAvailability` guard entry, wiring in 10-12) and T-10-15 (suspended maps to null) are implemented as planned.

## Self-Check: PASSED

All created files exist and commits 0b4f6b7 and 79c666d are ancestors of HEAD.
