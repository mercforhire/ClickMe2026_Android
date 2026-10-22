---
phase: 05-explore-expert-profile
plan: 05
subsystem: explore-viewmodel
tags: [viewmodel, repository, explore, filter, coroutines]
requires:
  - "05-01: getClientHome / getClientBookings, DTOs, FakeClickMeApi"
  - "05-02: PriceDisplay, java.time.Clock binding, MutableClock"
provides:
  - screens.clientmode.mybookings.ClientBookingsRepository (list, TYPE_UPCOMING, TYPE_PAST)
  - screens.clientmode.mybookings.todaysSessions, JOINABLE_BOOKING_STATUSES, formatSessionTimeRange
  - screens.clientmode.explore.ExploreRepository.home(selection)
  - ExploreUiState.kt (SectionState, CategoryChipUi, FeaturedTopicUi, ExpertCardUi, TodaysSessionUi, ExploreUiState)
  - ExploreViewModel (@HiltViewModel) with KEY_SELECTION = "explore_selection"
affects: [05-07, 05-10, phase-08]
tech-stack:
  added: []
  patterns:
    - "Latest-wins: each loader cancels its previous Job; results are also guarded by selection == requested"
    - "Per-section SectionState with FieldError messages (server message, else error_state_body)"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/ClientBookingsRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/TodaysSessions.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/TodaysSessionsFilterTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModelTest.kt
  modified: []
key-decisions:
  - "A refresh failure keeps Loaded sections and raises refreshFailed, but any section not Loaded at that moment still goes to Error so nothing stays stuck in Loading"
  - "A home failure leaves the experts section to its own in-flight request when the selection changed meanwhile"
requirements-completed: [DISC-01]
duration: ~30 min
completed: 2026-10-21
---

# Phase 5 Plan 05: Explore data layer and ExploreViewModel Summary

Explore state machine with per-section Loading/Loaded/Error, single-select chip toggle, sorted comma-joined sheet selection, latest-wins loading, pull-to-refresh with a one-shot failure flag, and the Today's Sessions filter in the bookings package Phase 8 extends.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Client bookings repository and Today's Sessions filter (D-01) | c42b83d |
| 2 | ExploreRepository, UI state and ExploreViewModel | a148a4c |

## Verification

- `:app:testDevDebugUnitTest --tests '*ExploreViewModelTest' --tests '*TodaysSessionsFilterTest' ktlintCheck` passes (ExploreViewModelTest 19 tests, 0 failures).
- `:app:assembleDevDebug` passes (Hilt resolves the ViewModel's Clock and repositories).
- Acceptance greps: one `@HiltViewModel`, zero non-comment `runCatching`, three `catch ... ApiException`, four `.cancel()`, one `sorted()` in ExploreRepository, one `sealed interface SectionState`, one `"pending_reschedule"`, one `ofLocalizedTime`, no `h:mm a`, no `LocalDate.now()` without a clock.

## Deviations from Plan

- Worktree base at startup was an ancestor of the expected base (d1217fb vs bd2d7c6); reset to bd2d7c6 per the worktree check.
- Tests and implementation were committed together per task (one commit per task): the test classes cannot compile without the production types. Plan is `type: execute`, so no plan-level TDD gate applies.
- Minor: ktlint `chain-method-continuation` rejected two test assertions; rewritten.
- `FakeClickMeApi` does not record page/limit, so the repository test asserts the type and call count only; page 1 / limit 20 are the declared defaults in `ClientBookingsRepository.list`.

## Known Stubs

None.

## Threat Flags

None. T-05-15 (slugs only from server lists, Retrofit-encoded), T-05-16 (no logging) and T-05-17 (latest-wins cancellation, covered by the stale-response test) are implemented as planned.

## Self-Check: PASSED

All seven files exist; commits c42b83d and a148a4c exist on the worktree branch.
