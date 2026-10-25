---
phase: 10-expert-topics-availability
plan: 06
subsystem: expert-mode-topics-ui
tags: [compose, components, topics]
requires:
  - "10-01 ExpertTopicItem and topicItem fixture"
  - "10-03 TopicPrice.resolveCurrency"
  - "10-04 topics_* strings, topicDurationText"
provides:
  - "TopicCard (+ topicCardTag, topicOverflowTag)"
  - "TopicDeleteDialog"
  - "TopicsSkeleton (+ TOPICS_SKELETON_TAG)"
  - "TopicsEmptyContent, TopicsErrorContent"
affects: [10-09]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/subviews/TopicCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/subviews/TopicDeleteDialog.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/subviews/TopicsSkeleton.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/subviews/TopicsEmptyState.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/subviews/TopicComponentsTest.kt
  modified: []
decisions:
  - "Overflow control is an overlaid sibling of the merged clickable card row, so it stays its own focus stop while the whole card (minus the overflow) opens the editor"
  - "A topic with no duration uses 'Per session' as the third part of the card description"
requirements-completed: [EXP-01]
status: complete
commits: 2
plan_head_before: 367841f5d56091f10977b8ed9db6871cf03397fc
plan_head_after: 0048036127803b421e4ac922c745e69975e01e45
actuals:
  tokens: 9000
  tasks: 2
  commits: 2
---

# Phase 10 Plan 06: Topics list components Summary

Stateless Topics list pieces from UI-SPEC Surface 1: the topic card with overflow menu and delete spinner, the "Delete this topic?" confirmation, the loading skeleton, and the empty and error bodies. 16 Robolectric cases in `TopicComponentsTest`.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | TopicCard with overflow menu and delete spinner | bd8676b |
| 2 | Delete confirmation dialog, skeleton, empty and error bodies | 0048036 |

## Verification

- `:app:testDevDebugUnitTest --tests '*TopicComponentsTest'` (16 tests, 0 failures), `ktlintCheck` and `scripts/check-design-rules.sh` pass.
- `fontSize` count in TopicCard.kt is 0; `PriceDisplay.of` appears once.

## Deviations from Plan

None - plan executed exactly as written. TDD note: the card and component tests were written alongside the implementation in the same commit per task rather than as a separate RED commit.

## Known Stubs

None.

## Threat Flags

None. T-10-19: titles and messages render through plain `Text` only. T-10-20: `TopicDeleteDialog` gates every delete, and `menuEnabled` / `isDeleting` enforce one operation at a time.

## Self-Check: PASSED

All five created files exist and commits bd8676b and 0048036 are ancestors of HEAD.
