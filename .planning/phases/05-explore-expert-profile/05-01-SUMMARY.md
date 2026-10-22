---
phase: 05-explore-expert-profile
plan: 01
subsystem: network
tags: [retrofit, moshi, dto, network, discovery]
requires: []
provides:
  - "ClickMeApi: getClientHome, getClientBookings, getCategories, getExpertDetails, getExpertReviews"
  - "Discovery DTOs (ClientHomeData, CategoriesPayload, PublicProfileDetailsData, ExpertReviewsPayload, ClientBookingsPayload)"
  - "FakeClickMeApi handlers/counters for the five endpoints, plus categoriesCalls and clientHomeCategories"
affects: [05-02, 05-03, 05-04, 05-05, 05-06]
tech-stack:
  added: []
  patterns:
    - "Lenient DTOs: optional server fields nullable or defaulted (RESEARCH Pitfall 2)"
    - "Free flag modeled where the server sends it: top-level is_free on featured topics, price.is_free on details topics"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientHomeData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/CategoriesPayload.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/PublicProfileDetailsData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertReviewsPayload.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientBookingsPayload.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ResponseDecodingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
key-decisions:
  - "No new public paths: /categories is public on the backend, but the harmless bearer is still attached (publicPaths stays at 5)"
  - "getClientHome never sends page or limit; category is a single comma-joined parameter, omitted when null"
  - "isBookable is Boolean? with null meaning bookable (iOS parity); gating is display-only, server enforces EXPERT_NOT_BOOKABLE"
requirements-completed: [DISC-01, DISC-02, DISC-03]
duration: ~25 min
completed: 2026-10-21
---

# Phase 5 Plan 01: Discovery network contract Summary

Five discovery GET endpoints (client home, client bookings, categories, expert details, expert reviews) with lenient Moshi DTOs, URL constants, scriptable fakes, and decoding/wire-format tests pinned to the backend controller shapes.

## Tasks

| Task | Name | Commits |
| ---- | ---- | ------- |
| 1 | Discovery DTOs and decoding tests | 6b9b7fd (test, RED), 8310da8 (feat, GREEN) |
| 2 | Endpoints, URL constants, FakeClickMeApi, wire tests | 9826f30 |

## Verification

- `:app:testDevDebugUnitTest` (full dev suite) and `ktlintCheck` pass.
- ResponseDecodingTest 34 tests, ClickMeApiTest 22, ApiRequestUrlsTest 9, all green.
- Acceptance greps: `is_free` once in each of ClientHomeData.kt and PublicProfileDetailsData.kt, `featured_topics` once, 6 `@JsonClass` in ClientHomeData.kt, 0 non-comment `is_favorite`, 21 `suspend fun` in ClickMeApi and 21 overrides in FakeClickMeApi, `@Query("page")` twice, `categoriesCalls` twice.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint import order in ClickMeApiTest**
- **Found during:** Task 2 verification
- **Issue:** `assertNull` import was inserted before `assertNotNull`, failing ktlintTestSourceSetCheck.
- **Fix:** Reordered the two imports.
- **Files modified:** ClickMeApiTest.kt
- **Commit:** 9826f30

### Process notes

- Task 2 tests and implementation were committed together (no separate RED commit) because the test source set cannot compile without the new interface methods and fake overrides. The plan is `type: execute`, so no plan-level TDD gate applies. Task 1 has a separate RED commit.
- At startup the worktree base was an ancestor of the expected base (d1217fb vs 29ea10b); it was reset to 29ea10b per the worktree check.

## Known Stubs

None.

## Threat Flags

None. The threat register mitigations (nullable/defaulted DTO fields pinned by tests, Retrofit-encoded `@Path`/`@Query`) are implemented; T-05-04 and T-05-05 are accepted as planned.

## Self-Check: PASSED

All five DTO files exist; commits 6b9b7fd, 8310da8, 9826f30 exist on the worktree branch.
