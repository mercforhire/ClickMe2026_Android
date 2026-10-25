---
phase: 10-expert-topics-availability
plan: 01
subsystem: network
tags: [network, dto, moshi, retrofit, wave-0]
status: complete
requires: []
provides:
  - ExpertTopicItem, TopicHourlyRateDto, MyExpertTopicPayload (typed topic DTOs)
  - ExpertAvailabilityScheduleData and its timezone/schedule/range DTOs
  - CreateExpertTopicRequest, UpdateExpertTopicRequest, UpdateMyAvailabilityRequest, UpdateExpertTagsRequest bodies
  - UpdateExpertTopicRequestAdapter (explicit-null PATCH body), registered in MoshiProvider
  - ClickMeApi createTopic, updateTopic, deleteTopic, getMyAvailability, updateMyAvailability, updateExpertProfileTags
  - FakeClickMeApi handlers/counters/recorders and testing/ExpertFixtures.kt (topicItem, availabilityData)
affects: [10-02, 10-03, 10-04, 10-05]
tech-stack:
  added: []
  patterns:
    - hand-written Moshi JsonAdapter registered by class for a request body that needs explicit nulls
    - computed DTO properties (amountMinor, storedCurrency) normalising the server price shape
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertTopicItem.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertAvailabilityScheduleData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ExpertRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/NullPreservingAdapters.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ExpertFixtures.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertTopicDtoTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertTopicRequestJsonTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertApiTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/MyExpertTopicsPayload.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/MoshiProvider.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/AppViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/UserManagerTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginViewModelTest.kt
decisions:
  - UpdateExpertTopicRequest has no @JsonClass; only the hand-written adapter serializes it so cleared fields go out as explicit null
  - NullPreservingAdapters.kt keeps the plan-specified filename with a file-level ktlint filename suppression (repo precedent)
requirements: [EXP-01, EXP-02, EXP-03, EXP-04]
commits: 2
plan_head_before: bc1f77406ae16cf9279280e323115b90dcd6b967
plan_head_after: 81b4c0100925fa77dceae3711697bb69f874d50c
completed: 2026-10-25
---

# Phase 10 Plan 01: Expert topics and availability network contract Summary

Typed topic/availability DTOs, an explicit-null PATCH adapter and six new authed Retrofit endpoints, pinned by exact-JSON and MockWebServer wire tests against the real backend shapes.

## What was built

- **Task 1** (`917ab20`): `ExpertTopicItem` decodes the real server shape (price only, `price.currency` may be `""`) with computed `isFree`, `amountMinor` (hourly_rate wins over price) and `storedCurrency` (first non-blank trimmed). `MyExpertTopicsPayload.topics` is now `List<ExpertTopicItem>`. Availability DTOs are all-nullable. Request bodies are in `ExpertRequests.kt`; `UpdateExpertTopicRequestAdapter` writes `description` and `icon_slug` with `serializeNulls` scoped to just those two names (restored in `finally`) and is registered in `MoshiProvider`.
- **Task 2** (`81b4c01`): `EXPERT_TOPIC` and `EXPERT_MY_AVAILABILITY` constants (the latter distinct from the public `experts/{id}/availability`), six `ClickMeApi` methods, `FakeClickMeApi` handlers + counters + request recorders, and `ExpertApiTest` covering paths, verbs, bodies and the 422 `PRICE_BELOW_MINIMUM` mapping through `apiCall`.

## Verification

Scoped run passed: `ExpertTopicDtoTest` (3), `ExpertTopicRequestJsonTest` (5), `ExpertApiTest` (8), `ApiRequestUrlsTest` (19), `ClickMeApiTest` (45), `AppViewModelTest` (43), `UserManagerTest` (68), `LoginViewModelTest` (19), plus `ktlintCheck` and `:app:assembleDevDebug`. All acceptance greps met (0 remaining `mapOf("id" to "t1")`, 6 API methods, 6 fake overrides).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixture migration and ExpertFixtures moved into the Task 1 commit**
- **Found during:** Task 1 verify
- **Issue:** Typing `MyExpertTopicsPayload.topics` broke compilation of the whole unit-test source set (the old `mapOf("id" to "t1")` fixtures), so Task 1's tests could not run without the migration and `topicItem()`.
- **Fix:** Created `testing/ExpertFixtures.kt` and migrated the three test files in the Task 1 commit instead of Task 2.
- **Commit:** 917ab20

**2. [Rule 3 - Blocking] ClickMeApiTest updated for the typed element**
- **Found during:** Task 1 verify
- **Issue:** `getMyTopicsDecodesTwoItemList` indexed the topic as a map (`topics[1]["id"]`); no longer compiles. Not listed in the plan's files.
- **Fix:** Changed to `topics[1].id`; the minimal JSON still decodes because DTO fields default.
- **Commit:** 917ab20

**3. [Rule 3 - Blocking] ktlint filename rule on NullPreservingAdapters.kt**
- **Found during:** Task 1 verify
- **Issue:** ktlint wants a single-class file named after the class; the plan fixes the filename `NullPreservingAdapters.kt` (room for future null-preserving adapters).
- **Fix:** `@file:Suppress("ktlint:standard:filename")`, same as `Color.kt`, `BookingSlots.kt` and others in the repo.
- **Commit:** 917ab20

## Known Stubs

None.

## Threat Flags

None. All new surface is covered by the plan's threat model (T-10-01 typed bodies with exact-JSON tests; T-10-02 explicit-null adapter, asserted in tests).

## Self-Check: PASSED

- All created files exist; commits `917ab20` and `81b4c01` are in HEAD history.
