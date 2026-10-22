---
phase: 05-explore-expert-profile
plan: 03
subsystem: ui-components
tags: [compose, components, skeleton, rating, picker]
requires: []
provides:
  - RatingStars, ExpertTagChip, SkeletonBlock/SkeletonGroup, SectionHeader, SectionError
  - PickerRow leadingIcon + role (Role.Checkbox toggleable)
  - strings_discovery_shared.xml (section_retry, content_loading, rating_* strings and plurals)
affects: [05-07, 05-08, 05-09, 05-10]
tech-stack:
  added: []
  patterns: [infinite-transition skeleton pulse, merged-semantics skeleton group, toggleable checkbox rows]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/RatingStars.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ExpertTagChip.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SkeletonBlock.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SectionHeader.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SectionError.kt
    - app/src/main/res/values/strings_discovery_shared.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/Phase5ComponentsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PickerSheet.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/PickerComponentsTest.kt
decisions:
  - "Role.Checkbox rows use Modifier.toggleable (not selectable) so TalkBack announces checked state"
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 5 Plan 03: Shared discovery components Summary

Shared Compose pieces for Explore and Expert Profile: star rating, tag chip, pulsing skeleton with a single "Loading" announcement, section header and per-section error with Retry, plus a backward-compatible PickerRow extension (leading icon, checkbox role) for the All Categories sheet.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | RatingStars, ExpertTagChip, SkeletonBlock/SkeletonGroup, SectionHeader, SectionError | 0a29b20 |
| 2 | PickerRow leading icon and checkbox role | 6b00ad8 |

## Verification

- Phase5ComponentsTest: 11 tests, 0 failures. PickerComponentsTest: 18 tests (including all pre-existing), 0 failures.
- `*Signup*ScreenTest` (Phase 4 picker callers) passes; `ktlintCheck` and `scripts/check-design-rules.sh` pass.
- Acceptance greps satisfied (no fontSize, roundToInt, rememberInfiniteTransition, heading(), tonalColors, plural present).

## Deviations from Plan

None in behavior. Process note: the RED step for each task was a compile failure (tests referencing not-yet-existing components or parameters) rather than a separately committed failing-test commit, because test-only commits would not compile; tests and implementation were committed together per task. One compile fix was needed in SkeletonBlock (missing `androidx.compose.runtime.getValue` import for the delegated `by` state).

## Threat Model

T-05-09 mitigated: server text in ExpertTagChip/SectionError is plain `Text`, chip is single-line with ellipsis. T-05-10 accepted: test proves `waitForIdle()` returns with the infinite pulse running.

## Known Stubs

None.

## Self-Check: PASSED

All created files exist; commits 0a29b20 and 6b00ad8 are on the branch.
