---
phase: 06-booking-flow-free-bookings
plan: 01
subsystem: network
tags: [retrofit, moshi, dto, network, booking]
requires:
  - phase: 05-discovery
    provides: ClickMeApi, TopicPriceDto, ApiRequestUrls, FakeClickMeApi patterns
provides:
  - getExpertAvailability / requestBooking / getClientBookingDetail Retrofit calls
  - ExpertAvailabilityData, BookingRequestData, ClientBookingDetail DTOs and BookingRequestBody
  - FakeClickMeApi handlers, counters and recordings for the booking endpoints
affects: [06-02, 06-03, 06-04, phase-08-my-bookings]
tech-stack:
  added: []
  patterns: [lenient nullable DTOs, null-omitting strict request body, recorded-before-handler fake]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertAvailabilityData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/BookingRequestData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientBookingDetail.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/BookingRequests.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ResponseDecodingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
key-decisions:
  - "Availability topic_id parameter exists but Phase 6 callers pass null (iOS parity, Open Question 2)"
  - "ClientBookingDetail lives in model/responses (Open Question 5)"
  - "start_local/end_local not modelled; UI formats start_utc itself"
patterns-established:
  - "Strict-schema request bodies rely on the converter omitting nulls (no withNullSerialization)"
requirements-completed: [BOOK-02, BOOK-03, BOOK-04]
duration: 25min
completed: 2026-10-22
---

# Phase 6 Plan 1: Booking network contract Summary

Three booking Retrofit calls (availability, free booking request, client booking detail) with lenient Moshi DTOs, a strict null-omitting request body, URL constants, a scriptable FakeClickMeApi and wire-format tests.

## Accomplishments
- Availability DTOs decode the backend generator shape (`.000Z` Instants, null day slots, optional `held`).
- `BookingRequestBody` serializes exactly the seven D-11 keys in order; `client_notes` is omitted when null.
- `ClientBookingDetail` decodes `+00:00` times, missing `end_time`, and topic-level `is_free`.
- 409 SLOT_UNAVAILABLE and 422 MEETING_LINK_UNAVAILABLE reach `ApiException.Http` with their `ErrorCode`.
- The three paths carry the bearer; `publicPaths` is still 5 entries.
- FakeClickMeApi scripts, counts and records the new endpoints (months, topic ids, request bodies recorded before the handler runs).

## Task Commits
1. Task 1: DTOs, request body, decoding tests - `472aefb`
2. Task 2: endpoints, URL constants, fake, wire tests - `3a92323`

## Verification
- `:app:testDevDebugUnitTest` (full dev suite) and `ktlintCheck` pass.
- Acceptance greps: 24 `suspend fun` in ClickMeApi, 24 `override suspend fun` in the fake, one `@Query("topic_id")`, zero null-serialization usages in main.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint filename rule on BookingRequests.kt**
- **Found during:** Task 2 (ktlintCheck)
- **Issue:** The plan-mandated file name `BookingRequests.kt` holds a single class, so `ktlint:standard:filename` fails.
- **Fix:** Added `@file:Suppress("ktlint:standard:filename")` (same precedent as `ui/theme/Color.kt`), keeping the planned file path.
- **Files modified:** model/requests/BookingRequests.kt
- **Commit:** `3a92323`

**2. [TDD note]** The RED step could not be committed separately: the new tests reference types that do not exist, so the test source set would not compile. Tests and implementation were written together per task and committed once each.

## Known Stubs
None.

## Threat Flags
None. No new endpoints beyond the planned three; none added to `publicPaths`.

## Self-Check: PASSED
All created files exist; commits `472aefb` and `3a92323` are on branch `worktree-agent-ad71417a9d3fe800e`.
