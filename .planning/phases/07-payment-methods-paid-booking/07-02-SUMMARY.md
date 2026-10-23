---
phase: 07-payment-methods-paid-booking
plan: 02
subsystem: api
tags: [retrofit, moshi, stripe, payments, repository]

requires:
  - phase: 06-booking
    provides: BookingRepository, ClickMeApi booking group, ApiException 402/422 mapping
provides:
  - Payment request/response DTOs matching the backend strict schemas
  - Six payment Retrofit endpoints and URL constants
  - BookingRepository.paymentIntent() and confirm()
  - Singleton PaymentMethodsRepository (list, createSetupIntent, delete, setDefault)
  - FakeClickMeApi handlers, counters and recorders for all six endpoints
affects: [07-payment-methods-screen, 07-booking-gate, 07-paid-flow, 07-3ds]

tech-stack:
  added: []
  patterns:
    - "Strict-schema bodies: nulls omitted, explicit {} body via Map<String, String> = emptyMap()"
    - "Secret-bearing DTOs override toString() to print only non-secret ids"
    - "Bare (non-enveloped) response decoded without declaring decimal amount fields"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/PaymentRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/PaymentIntentResponse.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ConfirmBookingData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/PaymentMethodsData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsRepository.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentRepositoriesTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingRepository.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ResponseDecodingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt

key-decisions:
  - "PaymentIntentResponse declares only payment_intent_id, client_secret and currency; total_amount and summary are intentionally undeclared (D-12)"
  - "ConfirmBookingRequest has no booking_attempt_id field at all, so it cannot be sent by construction"
  - "setup-intent and set-default send an explicit {} body to satisfy z.object({}).strict()"

patterns-established:
  - "Payment repositories throw ApiException only; the 402 detail map reaches callers via ApiException.Http.detail"

requirements-completed: [PAY-01, PAY-02, PAY-03, PAY-04, PAY-07]

duration: 20min
completed: 2026-10-22
---

# Phase 7 Plan 02: Payment Network Contract Summary

**Six Retrofit payment endpoints with strict-schema DTOs (omitted nulls, explicit `{}` bodies, bare payment-intent decoding without amounts) plus BookingRepository paid calls and a shared PaymentMethodsRepository.**

## Performance

- **Tasks:** 3 of 3
- **Files modified:** 13 (6 created, 7 modified)

## Accomplishments
- DTOs encode exactly what the backend's strict schemas accept; secrets (`client_secret`, `ephemeral_key`) are redacted from `toString()`.
- `GET/POST/DELETE/PATCH me/payment-methods...`, `POST bookings/payment-intent` (bare) and `POST bookings/confirm` (201 envelope) with MockWebServer tests asserting verb, path and exact body.
- 402 PAYMENT_REQUIRES_ACTION surfaces as `ApiException.Http` with `detail.client_secret` and `detail.stripe_account` preserved, verified both through MockWebServer and the repository.
- Full gate green: `testDevDebugUnitTest ktlintCheck assembleDevDebug`.

## Task Commits

1. Task 1 RED: `b7d2238` (test), GREEN: `050a648` (feat) - payment DTOs
2. Task 2: URL constants, six endpoints, fake and tests - see `git log` (feat(07-02): add six payment endpoints...)
3. Task 3: repositories and PaymentRepositoriesTest - see `git log` (feat(07-02): add paid booking repository calls...)

Tasks 2 and 3 were committed as single test+implementation commits (no separate RED commit).

## Decisions Made
See key-decisions above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint chain-method-continuation in new tests**
- **Found during:** Task 3 gate
- **Issue:** Two new test assertions used long call chains that ktlint rejects.
- **Fix:** Extracted local variables.
- **Files modified:** ClickMeApiTest.kt, ResponseDecodingTest.kt
- **Commit:** included in the Task 3 commit

**2. Worktree base correction**
- The worktree HEAD (d1217fb) was behind the required base fb0e73c (a descendant), so `git reset --hard fb0e73c` was applied per the startup protocol on a clean tree.

## Known Stubs
None.

## Threat Flags
None. All new surface is covered by the plan's threat model (T-07-05..09).

## Issues Encountered
None.

## Self-Check: PASSED
