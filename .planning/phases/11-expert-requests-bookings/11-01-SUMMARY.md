---
phase: 11-expert-requests-bookings
plan: 01
subsystem: api
tags: [network, dto, moshi, retrofit, repository, wave-0]

requires:
  - phase: 08-client-booking-management
    provides: RescheduleDto, CancelBookingRequest, AcceptRescheduleRequest, DeclineRescheduleRequest, CancelBookingData, RescheduleResponseData
  - phase: 10-expert-topics-availability
    provides: expert repository shape, ExpertApiTest MockWebServer setup, FakeClickMeApi expert handlers
provides:
  - 14 expert endpoint constants and Retrofit methods (booking requests, expert bookings, weekly earnings, payout summary, Connect)
  - DTOs for requests, bookings, earnings breakdown and financial data
  - BookingRequestsRepository, ExpertBookingsRepository, ExpertFinancialRepository (stateless)
  - FakeClickMeApi handlers for all 14 methods and shared ExpertBookingFixtures
  - Exact-wire MockWebServer tests for every request body and the corrected detail path
affects: [11-02, 11-03, 11-04, 11-05, 11-06, 11-07, 11-08, 11-09, 11-10, 11-11, 11-12, 11-13, 11-14, 11-15]

actuals:
  tokens: 18400
  tasks: 3
  commits: 3
plan_head_before: f3a31401b9bcde5033bd4b1dc7f224291d2c72ae
plan_head_after: da761128906882c7cf98336aa198cb7a06404e0b
commits: 3

tech-stack:
  added: []
  patterns:
    - "Stateless @Singleton expert repositories over apiCall, nothing cached"
    - "Strict-schema bodies always passed explicitly; null-omitting converter yields {}"
    - "Minor-unit Long for weekly/payout amounts, major-unit Double for earnings_breakdown, fee and refund"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/EarningsBreakdownDto.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/BookingRequestsPayload.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertFinancialData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ExpertBookingRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/BookingRequestsRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertFinancialRepository.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ExpertBookingFixtures.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/BookingRequestsApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertBookingsApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertFinancialApiTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt

key-decisions:
  - "EXPERT_BOOKING_DETAILS ends in /details; no code path calls a bare expert/bookings/{id} GET (C1)"
  - "The legacy request-detail earnings figure (server always /100) is not declared on any DTO (C4)"
  - "WeeklyEarningsData amounts, currency and session count are required so a malformed body fails decoding instead of showing a fake zero"
  - "ExpertStatusMeta declares label and code only; the server accent color is left to the theme (C6)"

patterns-established:
  - "Expert DTO sections: private-note, join/reschedule/message flags are never declared and KDoc refers to them by description"
  - "ExpertBookingFixtures: defaulted-parameter builders (breakdown, expertBookingItem, expertBookingDetail, bookingRequestItem, bookingRequestDetail)"

requirements-completed: [EXP-05, EXP-06, EXP-07, EXP-08, EXP-09]

coverage:
  - id: D1
    description: "Booking requests list/detail/accept/decline decode the real server shapes and send exact strict bodies ({} / {message} / both decline keys); no page or limit sent"
    requirement: "EXP-05"
    verification:
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/network/BookingRequestsApiTest.kt (5 tests)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Expert bookings feed, /details detail, cancel, accept-reschedule and decline-reschedule are typed with object status and exact bodies"
    requirement: "EXP-06"
    verification:
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertBookingsApiTest.kt (6 tests)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Weekly earnings, payout summary and Connect status/onboard/refresh-link decode with minor-unit Longs and body-less POSTs"
    requirement: "EXP-08"
    verification:
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertFinancialApiTest.kt (5 tests)"
        status: pass
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt#phaseElevenConstants_haveExactRelativeValues"
        status: pass
    human_judgment: false
  - id: D4
    description: "FakeClickMeApi implements every ClickMeApi method (56 of 56) and shared fixtures exist for later plans"
    verification:
      - kind: other
        ref: "./gradlew :app:testDevDebugUnitTest ktlintCheck :app:assembleDevDebug exits 0"
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 01: Expert Network Contract Summary

**14 expert endpoints (booking requests, bookings, weekly earnings, payouts, Stripe Connect) with corrected URLs, object-shaped status, strict request bodies and minor/major money units, pinned by MockWebServer wire tests and a fully faked ClickMeApi.**

## Performance

- **Duration:** about 45 min
- **Completed:** 2026-10-26
- **Tasks:** 3 (one tracer, two expansion)
- **Files modified:** 16 (12 created, 4 modified)

## Accomplishments

- Tracer: booking requests proven end to end (URL constants, four Retrofit methods, DTOs, strict `{}` / `{message}` / both-key decline bodies, repository, fake handlers, 5 wire tests including `ALREADY_PROCESSED` mapping). Tracer verify re-run and green before expanding.
- Expert bookings group: feed with `type/page/limit=50`, detail on `/details` (asserted in a test), `ExpertStatusMeta{label, code}`, `ConsultationFeeDto.isPaid`, cancel/accept/decline-reschedule with exact bodies and typed `SlotUnavailable` / `AlreadyCancelled` errors.
- Financial group: weekly earnings and payout summary as minor-unit Longs (required fields, so a missing `net_amount` throws `ApiException.Decoding`), Connect status, and body-less onboard / refresh-link POSTs (asserted `bodySize == 0`).
- `FakeClickMeApi` now overrides all 56 `ClickMeApi` methods with handlers, call counters and recorded-request lists; `ExpertBookingFixtures` gives later plans five builders.

## Task Commits

1. **Task 1: Booking requests end to end (tracer)** - `22d4fdf` (feat)
2. **Task 2: Expert bookings group and shared fixtures** - `35f28e8` (feat)
3. **Task 3: Financial group and URL constants test** - `da76112` (feat)

## Verification

- Task 1: `*BookingRequestsApiTest` + ktlintCheck, 5/5 pass.
- Task 2: `*ExpertBookingsApiTest` + `*BookingRequestsApiTest` + ktlintCheck, 6/6 + 5/5 pass.
- Task 3 / plan level: `*ExpertFinancialApiTest` (5), `*ExpertBookingsApiTest`, `*BookingRequestsApiTest`, `*ApiRequestUrlsTest` (21), `*ExpertApiTest`, `*ClickMeApiTest`, `ktlintCheck`, `:app:assembleDevDebug` all exit 0.
- Acceptance greps: `expert/bookings/{id}/details` in ApiRequestUrls = 1; `const val EXPERT_` = 22; `potential_earnings` and `pagination` in BookingRequestsPayload = 0; `EXPERT_BOOKING_DETAILS` in ClickMeApi = 1; forbidden keys in ExpertBookingsPayload = 0; `fun expertBookingDetail` = 1; FakeClickMeApi overrides (56) equals ClickMeApi suspend funs (56); `can_withdraw|min_withdrawable` = 0; `phaseElevenConstants_haveExactRelativeValues` = 1.

## Deviations from Plan

None - plan executed exactly as written. The plan's TDD instruction was followed as "tests written first": Task 1's RED was confirmed as a compile failure (unresolved repository and DTO symbols) before implementing; one commit per task per the plan (not separate test/feat commits).

## Authentication Gates

None.

## Backend follow-up notes (for 11-15)

- `POST expert/booking-requests/{id}/accept` validates the optional `message` (up to 1000 chars) but the controller never reads it, so the message is discarded (RESEARCH C10). The client sends it anyway (user decision, 2026-10-25).
- Weekly `net_amount` may equal gross (EXPERT-EARNINGS-BREAKDOWN 6.1); displayed as received.

## Known Stubs

None. All new code is wired to real endpoints; nothing renders placeholder data.

## Threat Flags

None. No new network surface beyond the 14 planned endpoints; repositories do not log client messages, reason text or onboarding URLs (T-11-02), and the `/details` path is asserted (T-11-03).

## Self-Check: PASSED

- All 12 created files and 4 modified files exist on disk (verified via `git status` clean after commits).
- Commits `22d4fdf`, `35f28e8`, `da76112` are ancestors of HEAD; `git rev-list --count f3a3140..HEAD` = 3 before this summary commit.
