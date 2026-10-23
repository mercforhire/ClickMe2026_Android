---
phase: 07-payment-methods-paid-booking
plan: 08
subsystem: ui
tags: [compose, request-sent, paid, lifecycle-card]

requires:
  - phase: 07-payment-methods-paid-booking
    provides: "PaymentLifecycleCard / PaymentLifecycleStage (07-04)"
provides:
  - "formatSessionWhen(start, zone, locale, timeFormat) -> \"Oct 24, 2:00 PM\""
  - "Paid Request Sent shows PaymentLifecycleCard in HoldPlaced stage (REQUEST_SENT_PAYMENT_CARD_TAG)"
affects: [07-payment-methods-paid-booking]

tech-stack:
  added: []
  patterns:
    - "Paid-only block shares an existing entry-animation index instead of adding a stage"

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormatting.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentDetailsCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormattingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreenTest.kt

key-decisions:
  - "Headline unchanged for paid bookings (iOS parity, UI-SPEC A2); the lifecycle card is the only paid difference"
  - "Card reuses CARD_INDEX so it enters with the details card; BLOCK_COUNT stays 4"
  - "Session time is \"MMM d, h:mm a\" per the plan (iOS uses \" · \" separator; plan specified comma)"

patterns-established:
  - "Pure formatters take zone and locale as parameters, never ZoneId.systemDefault()"

requirements-completed: [PAY-07]

duration: 25min
completed: 2026-10-22
---

# Phase 7 Plan 08: Paid Request Sent lifecycle card Summary

**Paid bookings now show the hold-placed payment lifecycle card on Request Sent, with the session time formatted in the viewer's zone and device 12/24h setting; free bookings and the headline are unchanged.**

## Performance

- **Tasks:** 2/2
- **Files modified:** 7

## Accomplishments
- `formatSessionWhen` yields "Oct 24, 2:00 PM" (date and time both in the viewer's zone, time via device format).
- `LoadedBody` renders `PaymentLifecycleCard(HoldPlaced, sessionWhen)` under `if (!details.isFree)`, tagged `REQUEST_SENT_PAYMENT_CARD_TAG`, sharing the details card's animation progress.
- Stale "Reserved for Phase 7 / Phase 7 adds it" KDoc replaced; headline KDoc records the iOS-parity decision.
- Tests: 3 new formatting cases; Request Sent tests cover paid card content ("Done" step, "Your session runs on Oct 22, 4:00 PM"), ordering (details < card < actions), unchanged headline, free = no card and no payment wording, and no animation replay after restoration with a paid booking.

## Task Commits

1. **Task 1: formatSessionWhen** - `d6fb3ac` (feat)
2. **Task 2: Paid lifecycle card on Request Sent** - `60212b0` (feat)

## Deviations from Plan

None - plan executed exactly as written. Tests and implementation for each task were committed together rather than as separate RED/GREEN commits.

Note: the worktree started on an older commit (d1217fb); it was fast-forward reset to the specified base d882eb5 per the startup check (worktree was clean).

## Issues Encountered
None. Gate `testDevDebugUnitTest ktlintCheck assembleDevDebug` passes.

## Known Stubs
None.

## Threat Flags
None. The card shows static copy plus the session time already visible above it (T-07-28 accepted).

## Next Phase Readiness
Request Sent is ready for paid bookings arriving from the paid booking flow in later plans.

## Self-Check: PASSED

- Commits d6fb3ac and 60212b0 exist; all modified files present; verification greps match acceptance criteria.
