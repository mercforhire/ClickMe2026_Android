---
phase: 10-expert-topics-availability
plan: 15
subsystem: expert-shell-navigation
tags: [navigation, shell, wiring]
status: complete

requires:
  - phase: 10-09
    provides: "TopicsScreen"
  - phase: 10-13
    provides: "TopicEditorScreen, TopicEditorKey, TopicEditorViewModel.Factory"
  - phase: 10-14
    provides: "AvailabilityScreen, AvailabilityKey / AvailabilityOrigin"
provides:
  - "HomeExpertShell topicsRoot and detailEntries slots (Hilt-free shell)"
  - "TopicEditorKey and AvailabilityKey routes registered with the bottom bar hidden"
  - "Placeholder Availability row on the expert Profile tab (profile_availability_row)"
affects: [phase 11 dashboard entry, phase 14 profile hub]

commits: 2
plan_head_before: 64b5b4981cfb12b224366a7e4cab9e3c1dc7f9bc
plan_head_after: 349b11bac5eadfac56de967ead762d7c337f6ff0
actuals:
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns: ["Slot-injected Hilt screens in the expert shell, mirroring HomeClientShell"]

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt

key-decisions:
  - "Followed the plan as written; Availability origin keys keep the Topics and Profile stack entries distinct (T-10-46)"

requirements-completed: [EXP-01, EXP-03, EXP-04]
---

# Phase 10 Plan 15: Expert shell wiring Summary

The expert Topics tab now shows the real Topics screen, and the Topic Editor and Availability routes are reachable (bottom bar hidden) from the Topics stack and from a throwaway Availability row on the Profile tab.

## What was built

- `HomeExpertShell` gained `topicsRoot` (default: old placeholder) and `detailEntries` (default empty) slots; the shell contains no `hiltViewModel` call.
- `HomeExpertScreen` supplies `TopicsScreen` (Add/Edit push `TopicEditorKey`, app-bar action pushes `AvailabilityKey(Topics)`) and registers `entry<TopicEditorKey>` (assisted `TopicEditorViewModel.Factory`) and `entry<AvailabilityKey>`, both with `BottomBar.hidden()`.
- `ExpertProfileRoot` footer: "Availability" `ProfileMenuRow` (tag `profile_availability_row`) above Log out, pushing `AvailabilityKey(Profile)`; marked as replaced by the Phase 14 hub (D-01).
- `HomeShellsTest`: four new cases (default Topics placeholder; Profile row ordering, navigation and hidden bar; Topics slot to Availability with system Back; Topic Editor route with system Back). 42/42 tests in the class pass.

## Verification

- `assembleDevDebug`, `assembleProdDebug`, `ktlintCheck` pass; `scripts/check-design-rules.sh` OK.
- Task 1 acceptance greps: 1 / 2 / 1 / 0 as required.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None. (The Profile Availability row is an intentional throwaway entry point replaced by Phase 14.)

## Threat Flags

None.

## Self-Check: PASSED

- HomeExpertScreen.kt and HomeShellsTest.kt modified and committed (9d0671d, 349b11b).
