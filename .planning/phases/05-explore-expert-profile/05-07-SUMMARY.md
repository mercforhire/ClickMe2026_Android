---
phase: 05-explore-expert-profile
plan: 07
subsystem: explore-ui
tags: [compose, explore, cards, sections]
requires:
  - "05-02: PriceDisplay, CategoryIconMap"
  - "05-03: RatingStars, ExpertTagChip, SkeletonBlock/Group, SectionHeader, SectionError, PriceText"
  - "05-05: SectionState and the Explore *Ui models"
provides:
  - ExploreHeroHeader, ExploreTodaysSessionsSection (+ TODAYS_SESSION_JOIN_TAG), ExploreCategoriesSection, ExploreCategoryCard
  - ExploreFeaturedTopicsSection, ExploreExpertsSection, ExpertCard
  - strings_explore.xml (all Explore copy)
affects: [05-10]
tech-stack:
  added: []
  patterns:
    - "Section composables own all SectionState rendering (loading, error, empty, content)"
    - "Cards: pressScale + clip + background + border + clickable/toggleable with shared InteractionSource, merged semantics"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreHeroHeader.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreCategoriesSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreCategoryCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreFeaturedTopicsSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreExpertsSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExpertCard.kt
    - app/src/main/res/values/strings_explore.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreSectionsTest.kt
  modified: []
key-decisions:
  - "Join Call is a PressScaleButton(enabled = false) tagged todays_session_join, with the exact '// PHASE-9: join window + click (D-02)' marker line"
  - "Featured card accent bar drawn with drawBehind so the card keeps heightIn(min) and SpaceBetween layout"
  - "Expert card TalkBack description adds one string (expert_card_tags_semantics) outside the explore_ prefix, keeping the explore_ string count at 18"
requirements-completed: [DISC-01]
duration: ~35 min
completed: 2026-10-21
---

# Phase 5 Plan 07: Explore section composables Summary

Seven self-contained Explore subviews rendering every state from the UI-SPEC: inert Today's Sessions with a permanently disabled Join Call, Trending Categories with toggleable icon cards, Featured Topics (hidden while loading or empty), and Recommended Experts with skeleton, error and D-17 empty states, plus all Explore strings.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Hero, Today's Sessions, Trending Categories, strings (D-01, D-02, D-09..D-11, D-16) | 9b878a1 |
| 2 | Featured Topics, ExpertCard, Experts section with D-17 empty state | 419dc31 |

## Verification

- `ExploreSectionsTest`: 18 tests, 0 failures (8 after task 1, 10 added in task 2).
- `ktlintCheck` and `bash scripts/check-design-rules.sh` pass ("Design rules: OK").
- Acceptance greps: PHASE-9 marker 1, `enabled = false` 1, no `clickable` in the sessions section, `CategoryIconMap.icon` 1, `Role.Checkbox` 1, 18 `explore_` strings, 1 `take(MAX_TAGS)`, no favorite/See all in code lines, no `fontSize`/`Color(0x`, no fixed 1xx dp heights on text cards.

## Deviations from Plan

- Tests and implementation were committed together per task (test classes cannot compile without the production types); plan is `type: execute`, so no plan-level TDD gate applies.
- [Rule 2 - Accessibility] Added one string, `expert_card_tags_semantics` ("Expertise: %1$s"), for the ExpertCard merged TalkBack description required by the UI-SPEC. It uses the `expert_card_` prefix so the `explore_` string count stays at 18.
- Worktree base at startup was an ancestor of the expected base (d1217fb vs c95d981); reset to c95d981 per the worktree check.

## Known Stubs

None. The disabled Join Call and inert session card are intentional (D-02); Phase 8 adds the card tap and Phase 9 the join window, found via the `PHASE-9` marker and `todays_session_join` tag.

## Threat Flags

None. T-05-23 (plain `Text` only, `maxLines` caps), T-05-24 (avatars only through `Avatar`) and T-05-25 (Join Call always disabled, asserted by test and grep) are implemented as planned.

## Self-Check: PASSED

All nine files exist; commits 9b878a1 and 419dc31 are on the worktree branch.
