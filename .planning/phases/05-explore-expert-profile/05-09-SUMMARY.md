---
phase: 05-explore-expert-profile
plan: 09
subsystem: expert-profile
tags: [compose, expert-profile, hero, skeleton]
requires: [05-02, 05-03, 05-06]
provides:
  - "ExpertProfileScreen (ViewModel wrapper) and stateless ExpertProfileContent"
  - "Subviews: ExpertProfileHero, StatsBar, ActionButtons (BOOK_SESSION_TAG), AboutCard, ExpertiseSection, TopicsCard, ReviewsCard plus skeleton variants"
  - "strings_expert_profile.xml (15 resources)"
affects: [05-10]
tech-stack:
  added: []
  patterns:
    - "Single details lazy item with a 150 ms Crossfade between skeleton, content and error"
    - "One merged Loading node via clearAndSetSemantics over several skeleton groups"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileHero.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileStatsBar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileAboutCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileExpertiseSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileTopicsCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileReviewsCard.kt
    - app/src/main/res/values/strings_expert_profile.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreenTest.kt
  modified: []
key-decisions:
  - "Topics and Reviews share an internal ExpertProfileSectionCard shell (header strip, divider) defined in the Topics file"
  - "Details sections sit in one lazy item so a single Crossfade covers loading, loaded and error; reviews are a second item"
requirements-completed: [DISC-03]
duration: ~45 min
completed: 2026-10-21
---

# Phase 5 Plan 09: Expert Profile screen Summary

Expert Profile UI per the UI-SPEC: static-glow hero with a 160 dp ringed avatar, stats bar, Book Session (or a disabled "Not accepting bookings" pill), About, Expertise, Topics of Discussion (Free in green, formatted price or a dash), up to 3 recent reviews, and skeleton, 404 unavailable and details-error states, wired to the 05-06 ViewModel's `bookingKey()`.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Hero, stats bar, action buttons, About, Expertise subviews and strings | e0acc77 |
| 2 | Topics and Reviews cards, ExpertProfileContent, ExpertProfileScreen | c776f70 |

## Verification

- `ExpertProfileScreenTest` (22 tests) and `ExpertProfileViewModelTest` pass; `ktlintCheck` passes; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: strings 15, `enabled = false` 1, no `infiniteTransition`/`online`/`message`/`favorite` in code lines, `ErrorState(` 1, `bookingKey()` 1, `take(MAX_REVIEWS)` 1, no `LaunchedEffect`, no `fontSize`, no favorite/MoreVert/see_all.

## Deviations from Plan

- **Hero glow call is fully qualified.** The acceptance grep expects `GreenGlowBlobLayer` on exactly one line, which an import would break, so the call is written with its package path and no import.
- **`PersonOff` appears on 2 lines in `ExpertProfileScreen.kt`** (import plus usage) rather than the 1 the acceptance grep expects; an extension icon property cannot be used without its import.
- Tests and implementation were committed together per task (the test source set cannot compile without the new composables), so there is no separate RED commit. The plan is `type: execute`, so no plan-level TDD gate applies.
- At startup the worktree base (d1217fb) was behind the expected base (c95d981) and was reset to it per the worktree check.
- The Loading test asserts one merged "Loading" node: the reviews skeleton is below the fold on the Robolectric screen and is not composed. The Retry test scrolls to the button before clicking for the same reason.
- ktlint required splitting a chained call in the test and an unused import removed in the screen.

## Known Stubs

None. Book Session reaches the Phase 6 stub through `onBook(BookingKey)`; wiring that route happens in 05-10.

## Threat Flags

None. T-05-29 (plain `Text` only, no links or markdown), T-05-30 (Unavailable replaces the body including the hero) and T-05-31 (disabled pill when not bookable, skeleton pill while loading or failed, no Book button on error) are implemented and covered by tests.

## Notes for downstream plans

- 05-10 creates the ViewModel in the Nav3 entry and passes `onBook = { backStack.add(it) }` to `ExpertProfileScreen`. Hiding the bottom bar for `BookingKey` is route metadata there.
- The profile uses the Scaffold default insets with the `TopAppBar` (transparent, pinned) and lets the list scroll beneath it.
- Manual checks deferred: skeleton-to-content cross-fade feel, glow behind the transparent bar, 200% font scale.

## Self-Check: PASSED

All ten files exist; commits e0acc77 and c776f70 are on the worktree branch.
