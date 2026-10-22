---
phase: 05-explore-expert-profile
plan: 06
subsystem: expert-profile
tags: [viewmodel, assisted-inject, nav3, expert-profile, booking-route]
requires: [05-01, 05-02]
provides:
  - "ExpertProfileKey and BookingKey Nav3 keys (@Serializable)"
  - "ExpertProfileRepository (details, reviewsPreview) and REVIEWS_PREVIEW_LIMIT"
  - "ExpertProfileUiState.kt types (HeroRating, DetailsState, ReviewsState, ProfileDetailsUi, TopicRowUi, ReviewRowUi)"
  - "ExpertProfileViewModel (assisted Hilt, first in the repo) with retry() and bookingKey()"
  - "MakeABookingStubScreen under BookingKey and strings_make_a_booking.xml"
affects: [05-09, 05-10, phase-06]
tech-stack:
  added: []
  patterns:
    - "Assisted-injection HiltViewModel keyed by a Nav3 route key"
    - "Two independent viewModelScope jobs with typed ApiException catches (one failure never cancels the other)"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingStubScreen.kt
    - app/src/main/res/values/strings_make_a_booking.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingStubScreenTest.kt
  modified: []
key-decisions:
  - "Seed rating is computed once in the ViewModel and reused by retry() and the reviews-failure fallback"
  - "Stub screen reuses AuthTopAppBar (TopAppBar with AutoMirrored back arrow and a11y_back) rather than a new bar"
requirements-completed: [DISC-03]
duration: ~30 min
completed: 2026-10-21
---

# Phase 5 Plan 06: Expert Profile data side and booking stub Summary

Assisted-injection ExpertProfileViewModel that shows a preview from the route key at once, then loads details and a 3-review preview as two independent jobs (404 gives Unavailable, other failures Error, reviews summary replaces the hero rating), plus the real BookingKey route with a coming-soon stub screen.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Route keys, repository, UI state and assisted ExpertProfileViewModel | 757aed5 |
| 2 | Stub booking screen under BookingKey | 480624e |

## Verification

- ExpertProfileViewModelTest: 17 tests, 0 failures. MakeABookingStubScreenTest: 4 tests, 0 failures.
- `ktlintCheck`, `:app:assembleDevDebug` (Hilt generates the assisted factory) and `scripts/check-design-rules.sh` pass.
- Acceptance greps: `assistedFactory = ExpertProfileViewModel.Factory::class` 1, `@AssistedFactory` 1, `404` 2, code-line `favorite` in the repository 0, `runCatching` in the ViewModel 0, `@Serializable` in BookingKey 1, no `fontSize` or `Color(0x` in makeabooking.

## Deviations from Plan

None to the behavior. Process notes:

- Tests were written before the implementation but each task was committed as a single commit (no separate RED commit); the test source set could not compile without the new types. The plan is `type: execute`, so no plan-level TDD gate applies.
- At startup the worktree base (d1217fb) was an ancestor of the expected base (bd2d7c6) and was fast-forwarded with `git reset --hard` per the worktree check.
- ktlint rejected a wrapped one-expression `bookingKey()` body; collapsed to a single line.
- The stub screen relies on Scaffold's default content insets plus the TopAppBar, as the other pushed screens do, instead of an explicit `WindowInsets.safeDrawing` modifier.

## Known Stubs

`MakeABookingStubScreen` is an intentional stub (D-04). Phase 6 replaces its body and keeps `BookingKey`.

## Threat Flags

None. T-05-19 (404 gives Unavailable and no details data), T-05-20 (keys hold display seed only), T-05-21 (independent jobs with typed catches) are implemented and covered by tests. T-05-22 is handled by passing null reviewer names through for the 05-09 "Anonymous" fallback.

## Notes for downstream plans

- 05-09 renders `ExpertProfileUiState`; reviewer `author == null` must show "Anonymous".
- 05-10 registers `entry<ExpertProfileKey>` with `hiltViewModel<ExpertProfileViewModel, ExpertProfileViewModel.Factory>(creationCallback = { it.create(key) })` and `entry<BookingKey>` with `MakeABookingStubScreen`; the bottom bar is hidden through route metadata there.

## Self-Check: PASSED

All nine files exist; commits 757aed5 and 480624e exist on the worktree branch.
