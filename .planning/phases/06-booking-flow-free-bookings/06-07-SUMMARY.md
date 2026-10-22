---
phase: 06-booking-flow-free-bookings
plan: 07
subsystem: booking
tags: [compose, material3, subviews, topic-picker, bottom-sheet]
requires: [06-04]
provides:
  - MakeABookingExpertCard
  - MakeABookingTopicPicker and TOPIC_PICKER_TAG
  - MakeABookingTopicSheet, makeABookingTopicItems, MakeABookingTopicCard
affects: [06-09]
tech-stack:
  added: []
  patterns: [stateless subviews taking 06-04 UI types, PickerSheet reuse for the topic sheet, merged semantics with Role]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingExpertCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTopicPicker.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTopicSheet.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingTopicCardsTest.kt
  modified: []
key-decisions:
  - "Picker row uses Surface(onClick) with explicit Role.Button and a merged contentDescription so the test tag, click action and semantics sit on one node"
  - "The duration, separator and price in the picker sub-line are separate Text nodes so '60 min' and 'Free' match exactly"
  - "Topic cards use Modifier.selectable inside the Surface so the ripple is clipped to the card shape"
requirements-completed: [BOOK-01]
duration: ~25min
completed: 2026-10-22
---

# Phase 6 Plan 07: Expert card, topic picker and topic sheet Summary

Three stateless Make a Booking subviews built to the UI-SPEC: the expert card, the topic picker row with loading, loaded, empty and failed states, and the "Choose a Topic" sheet with radio cards that lists paid topics too (D-01).

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Expert card and topic picker row with four states | f900ada |
| 2 | Topic sheet with radio cards, paid topics included | 3713151 |

## Verification

`./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingTopicCardsTest' ktlintCheck` passes: 13 tests, 0 failures (3 expert card, 6 picker, 4 sheet). `bash scripts/check-design-rules.sh` prints "Design rules: OK". `isFree` appears 0 times in MakeABookingTopicSheet.kt; there is no `fontSize` or `Color(0x` in the subviews.

## Deviations from Plan

None in scope. Minor implementation notes:
- Plan said `clickable(role = Role.Button)` on the picker card; implemented as `Surface(onClick)` plus `semantics { role = Role.Button }` so the rounded shape clips the ripple and the tag, click action and merged description share one node.
- TDD: tests and implementation were committed together per task (same precedent as 06-04), not as separate RED commits.

## Known Stubs

None.

## Threat Flags

None. T-06-23 is mitigated: topic titles, descriptions and the expert name and title are rendered only as plain `Text`. T-06-24 is accepted as planned (paid topics are listed; Book Now gating belongs to 06-09).

## Self-Check: PASSED

All four created files exist; commits f900ada and 3713151 are present. STATE.md and ROADMAP.md were not modified.
