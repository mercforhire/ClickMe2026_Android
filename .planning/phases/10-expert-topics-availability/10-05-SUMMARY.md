---
phase: 10-expert-topics-availability
plan: 05
subsystem: data
tags: [repository, hilt, events]
status: complete
requires: [10-01]
provides:
  - ExpertTopicsRepository (list, create, update, delete)
  - ExpertiseTagsRepository (profileTags, catalog, saveProfileTags)
  - AvailabilityRepository (get, put)
  - TopicEvents one-shot channel (TopicSavedEvent, TopicSavedKind) bound as a SessionDataCleaner
affects: [10-06, 10-07, 10-08, 10-09]
tech-stack:
  added: []
  patterns:
    - stateless @Singleton repositories wrapping apiCall (no per-user cache)
    - Channel.BUFFERED + receiveAsFlow one-shot event holder drained via SessionDataCleaner
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/ExpertTopicsRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicEvents.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertiseTagsRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ExpertModule.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertRepositoriesTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicEventsTest.kt
  modified: []
decisions:
  - Repositories hold no state; TopicEvents is the only singleton with data and is a SessionDataCleaner (T-10-16)
  - saveProfileTags lower-cases ids with Locale.ROOT and marks index 0 primary (T-10-18)
requirements: [EXP-01, EXP-03, EXP-04]
commits: 2
plan_head_before: 367841f5d56091f10977b8ed9db6871cf03397fc
plan_head_after: 5827531755cfc02c465f9b9416794560b62fd2e7
completed: 2026-10-25
---

# Phase 10 Plan 05: Expert data layer Summary

Three stateless repositories over the Phase 10 endpoints plus a session-safe one-shot `TopicEvents` channel, so Wave 3 ViewModels depend only on these classes.

## What was built

- **Task 1** (`d991edb`): `ExpertTopicsRepository`, `ExpertiseTagsRepository` (catalog sorted case-insensitively; profile tags empty when null; save lower-cases ids, first tag primary) and `AvailabilityRepository`, each a one-line-per-method `apiCall {}` wrapper. `ExpertRepositoriesTest` (9 tests) runs against `FakeClickMeApi`, including the 500 and 404 `ApiException.Http` paths.
- **Task 2** (`5827531`): `TopicEvents` (`Channel.BUFFERED`, `trySend` emit, `receiveAsFlow` events, `clearSessionData` drains via `tryReceive`), `ExpertModule` binding it `@IntoSet` as a `SessionDataCleaner`, and `TopicEventsTest` (consume-once, buffered order, drained on clear).

## Verification

`:app:testDevDebugUnitTest --tests '*TopicEventsTest' --tests '*ExpertRepositoriesTest'`, `:app:assembleDevDebug` (Hilt graph accepts the binding) and `ktlintCheck` all pass. Acceptance greps: 0 state markers in each repository, 4 `apiCall` uses in `ExpertTopicsRepository`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint formatting**
- **Found during:** Task 1 verify
- **Issue:** A one-line `create` signature and an inline assertion exceeded ktlint_official wrapping rules.
- **Fix:** Wrapped the expression body and extracted the expected request in the test.
- **Commit:** d991edb

The per-plan commit ledger file under the git dir could not be written (the sandbox refused the compound git command), so `plan_head_before` is the known spawn base `367841f` and `commits` was measured with `git rev-list --count 367841f..HEAD` (2).

## Known Stubs

None.

## Threat Flags

None. Threats T-10-16 and T-10-18 are mitigated as planned; T-10-17 accepted.

## Self-Check: PASSED

- All seven created files exist; commits `d991edb` and `5827531` are in HEAD history.
