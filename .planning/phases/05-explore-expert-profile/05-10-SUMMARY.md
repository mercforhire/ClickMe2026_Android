---
phase: 05-explore-expert-profile
plan: 10
subsystem: explore-shell-wiring
tags: [compose, explore, navigation3, shell, hilt]
requires:
  - "05-05: ExploreViewModel and Explore UI models"
  - "05-06: ExpertProfileKey, BookingKey, MakeABookingStubScreen"
  - "05-07: Explore section composables"
  - "05-08: AllCategoriesSheet and CategoriesSheetViewModel"
  - "05-09: ExpertProfileScreen"
provides:
  - "ExploreScreen (Hilt wrapper) and stateless ExploreContent, EXPLORE_LIST_TAG"
  - "ExpertCardUi.toProfileKey / FeaturedTopicUi.toProfileKey"
  - "HomeClientShell exploreRoot and detailEntries slots; BookingKey entry with bottom bar hidden"
affects: [phase-06-booking, phase-08-bookings]
tech-stack:
  added: []
  patterns:
    - "Hilt-free shell with injected slots; Hilt wrapper supplies real screens"
    - "Assisted Hilt ViewModel created per Nav3 entry via creationCallback"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreNavigation.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreenTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
key-decisions:
  - "Refresh-failure Snackbar is launched on a remember-ed coroutine scope, then the one-shot flag is consumed, so clearing the flag does not cancel the visible Snackbar"
  - "Scroll-to-experts on selection change skips the first composition by tracking the last seen selection in remember"
  - "Optional sections (Today's Sessions, Featured Topics) are emitted conditionally and fade/expand in once via a MutableTransitionState, avoiding zero-height items that would add spacedBy gaps"
requirements-completed: [DISC-01, DISC-02, DISC-03]
duration: ~40 min
completed: 2026-10-21
---

# Phase 5 Plan 10: Explore screen and shell wiring Summary

The client Explore tab now shows the real Explore home (hero, Today's Sessions, Trending Categories, Featured Topics, Recommended Experts) with pull-to-refresh, a shared-selection All Categories sheet and card-to-profile navigation. The path Explore, Expert Profile, Book Session, booking stub works inside the Hilt-free client shell.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | ExploreContent, ExploreScreen and the profile-key mappers | 3e30cfa |
| 2 | Shell slots and route registration for Explore, Expert Profile and the booking stub | 1fe2fea |

## What was built

- `ExploreContent`: Scaffold with an "Explore" TopAppBar (same colors as the placeholder) and the avatar button, `PullToRefreshBox` around a keyed `LazyColumn`, a Snackbar host for "Couldn't refresh. Pull down to try again.", and animated scroll to Recommended Experts when the selection changes while its header is off screen.
- `ExploreScreen`: collects `ExploreViewModel`, opens `AllCategoriesSheet` from a `rememberSaveable` flag after `sheetViewModel.open(state.selection)`; Apply calls `applySelection` and closes, any other dismissal discards the draft.
- `toProfileKey` mappers: expert card passes every field; featured topic passes only id, name and image.
- `HomeClientShell`: new `exploreRoot` (default is the old placeholder with avatar to Mode Switch) and `detailEntries` slots, plus `entry<BookingKey>(metadata = BottomBar.hidden())`. `HomeClientScreen` passes `ExploreScreen` and `entry<ExpertProfileKey>` using `hiltViewModel<ExpertProfileViewModel, ExpertProfileViewModel.Factory>(creationCallback = ...)`.

## Verification

- Full `testDevDebugUnitTest` green (no failing or erroring suites). `ExploreScreenTest` 12 tests, `HomeShellsTest` 22 tests (19 existing unchanged plus 3 new).
- `ktlintCheck`, `scripts/check-design-rules.sh` ("Design rules: OK"), `assembleDevDebug` and `assembleProdRelease` all succeed. No R8 keep rule was needed for the new serializable keys or the assisted ViewModel (T-05-33).
- Acceptance greps: BookingKey entry 1, `entry<ExpertProfileKey>` 1, `creationCallback` 1, `ExploreScreen(` 1 in HomeClientScreen; no `PlaceholderTabContent` reference in ExploreScreen; no `hiltViewModel` call inside `HomeClientShell`.

## Deviations from Plan

- Tests and implementation were committed together per task (the test class cannot compile without the production types); the plan is `type: execute`, so no plan-level TDD gate applies.
- `ExploreScreenTest` uses `@Config(qualifiers = "w411dp-h3000dp")` so every lazy item composes at once, which lets the section-order test compare vertical positions directly.
- Worktree base at startup was an ancestor of the expected base (d1217fb vs 8fd9b85); reset to 8fd9b85 per the worktree check.

## Known Stubs

None added. The booking destination is the intentional Phase 6 stub (`MakeABookingStubScreen`), and Today's Sessions cards stay inert with a disabled Join Call (D-02, from 05-07).

## Threat Flags

None. T-05-32 (keys are display seed only, profile re-fetches), T-05-33 (prod release build passes without keep rules) and T-05-34 (avatar entry to Mode Switch preserved, existing avatar test green) are implemented as planned.

## Open items

- Manual on-device check of the Explore, Profile and Book Session flow (pull-to-refresh gesture, sheet Apply, scroll-to-experts animation) is not covered by Robolectric; log with the other deferred manual checks.

## Self-Check: PASSED

ExploreNavigation.kt, ExploreScreen.kt, ExploreScreenTest.kt exist; HomeClientScreen.kt and HomeShellsTest.kt modified; commits 3e30cfa and 1fe2fea are on the worktree branch.
