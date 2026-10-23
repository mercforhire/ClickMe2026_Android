---
phase: 08-client-bookings-cancellation
plan: 01
subsystem: network
tags: [network, dto, retrofit, moshi, repository, test-fakes]
requires: []
provides:
  - RescheduleDto on ClientBookingItem (slim) and ClientBookingDetail (full)
  - CancelBookingData / RescheduleResponseData response DTOs
  - CancelBookingRequest / AcceptRescheduleRequest / DeclineRescheduleRequest strict bodies
  - ClickMeApi cancelClientBooking / acceptClientReschedule / declineClientReschedule
  - ClientBookingsRepository cancel / acceptReschedule / declineReschedule
  - FakeClickMeApi handlers, FakeRealtimeConnection.emit, BookingManagementFixtures
affects: [08-02 through 08-15]
tech-stack:
  added: []
  patterns: [strict-body DTOs with null omitted, one-liner apiCall repository methods]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/BookingActionResponses.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/BookingActionRequests.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingManagementFixtures.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/ClientBookingsRepositoryTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientBookingsPayload.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientBookingDetail.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/ClientBookingsRepository.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeRealtimeConnection.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ResponseDecodingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
key-decisions:
  - "One RescheduleDto serves both the slim list block and the full detail block; every field nullable so a malformed row cannot fail the list decode"
  - "private_note and private_note_preview are not declared (D-16)"
  - "refund_amount is Double in major units, formatted only via Money.formatMajor (plan 08-03)"
requirements-completed: [BOOK-05, BOOK-06, BOOK-07, BOOK-09]
duration: ~35 min
completed: 2026-10-23
---

# Phase 8 Plan 01: Client booking network contract Summary

Ports the client booking-management network layer: reschedule/join/authorization DTO fields, three strict-body action endpoints (cancel, accept-reschedule, decline-reschedule), repository methods, and the Wave 0 test fakes and fixtures later plans depend on.

## Tasks

| Task | Name | Commits |
| ---- | ---- | ------- |
| 1 | DTO extensions, action DTOs, fixtures | 91e54fa (RED), e687f94 (GREEN) |
| 2 | URL constants, Retrofit endpoints, fakes | e471e51 |
| 3 | Repository cancel/accept/decline | a3eae76 |

## Verification

`./gradlew :app:testDevDebugUnitTest --tests '*ResponseDecodingTest' --tests '*ClickMeApiTest' --tests '*ApiRequestUrlsTest' --tests '*ClientBookingsRepositoryTest' ktlintCheck` exits 0. The four test classes ran 117 tests in total with 0 failures. The whole test source set compiles, so every `ClickMeApi` implementer is covered. The `private_note` / `private_note_preview` greps (comments excluded) print 0.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Worktree base reset**
- **Found during:** startup HEAD check
- **Issue:** Worktree HEAD (d1217fb) was an ancestor of the expected base 3edeb7e, not equal to it.
- **Fix:** `git reset --hard 3edeb7e` as the startup check prescribes (nothing had been committed).

**2. [Rule 1 - Style] ktlint violations in new tests**
- **Found during:** Task 3 verification
- **Issue:** Chained-call and long-line violations in two new tests.
- **Fix:** Split into local variables. Commit e687f94 and a3eae76 carry the fixes.

### TDD note

Task 1 has a separate RED commit (91e54fa, tests plus fixtures that did not compile before the DTOs existed). For Tasks 2 and 3 the tests and implementation were written together and committed together (e471e51, a3eae76), so there is no separate failing-test commit for those two tasks.

### Not covered

`FakeRealtimeConnection.emit` has no dedicated test; it is a three-line wrapper over `tryEmit` and will be exercised by the ViewModel tests in later plans.

## Known Stubs

None.

## Threat Flags

None. T-08-01 to T-08-04 mitigations are implemented as planned (exact wire bodies asserted in ClickMeApiTest, all new DTO fields nullable/defaulted, repository never logs).

## Self-Check: PASSED

All created files exist and commits 91e54fa, e687f94, e471e51 and a3eae76 are in `git log`.
