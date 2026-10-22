---
phase: 06-booking-flow-free-bookings
plan: 04
subsystem: booking
tags: [timezone, grouping, golden-tests, error-mapping, ui-state, strings]
requires: [06-01, 06-03]
provides:
  - groupSlots / isSelectable / openDays (BookingSlots.kt)
  - BookingFailure / toBookingFailure / toSectionMessage (BookingErrors.kt)
  - MakeABookingUiState, BookingTopicUi, TopicsState, AvailabilityState, BookDisabledReason
  - MeetingType enum with fromWire
  - Make a Booking copy in strings_make_a_booking.xml
affects: [06-06, 06-07, 06-08, 06-09, 06-11, 06-12, 06-13]
tech-stack:
  added: []
  patterns: [pure grouping function with zone as parameter, derived-getter UI state, sealed failure with side-effect flags]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/MeetingType.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingSlots.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrors.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingSlotsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrorsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiStateTest.kt
  modified:
    - app/src/main/res/values/strings_make_a_booking.xml
key-decisions:
  - "Duplicate slot start instants keep one entry whose availability is the AND of all copies (T-06-12)"
  - "day.date from the server is never read; slots are filed by startUtc.atZone(zone).toLocalDate()"
  - "bookDisabledReason order is NoTopic, PaidTopic, NoSlot; a stale (past) selected slot reports NoSlot"
requirements-completed: [BOOK-01, BOOK-02, BOOK-03]
duration: ~20min
completed: 2026-10-22
---

# Phase 6 Plan 04: Slot grouping, error mapping and UI-state contract Summary

Pure, tested contracts for the booking screen: client-local slot regrouping proven across five time zones, the D-10 booking error mapping with D-08/D-09 side effects, the `MakeABookingUiState` shape with derived rules, `MeetingType`, and all Make a Booking copy.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | groupSlots with cross-timezone golden tests | b192e5f |
| 2 | MeetingType, strings and booking error mapping | 9220589 |
| 3 | MakeABookingUiState contract | 67f5f59 |

## Verification

`./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingUiStateTest' --tests '*BookingSlotsTest' --tests '*BookingErrorsTest' ktlintCheck` passes: BookingSlotsTest 20, BookingErrorsTest 16, MakeABookingUiStateTest 13, no failures. Golden cases cover America/Toronto, UTC, Asia/Kolkata and Pacific/Kiritimati, the month boundary, the 2026-11-01 US fall-back, duplicates and held/unavailable/past slots. 49 `make_a_booking_*` string names are present.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint filename rule on BookingSlots.kt**
- **Found during:** Task 3 (ktlintCheck)
- **Issue:** `BookingSlots.kt` contains one class plus extension functions, so ktlint demanded the name `BookingTimeSlot.kt`; the plan fixes the file name.
- **Fix:** Added `@file:Suppress("ktlint:standard:filename")`, the same precedent as `Color.kt` and `BookingRequests.kt`.
- **Files modified:** BookingSlots.kt
- **Commit:** 67f5f59

TDD note: tests and implementation were committed together per task (tests were written first and failed to compile until the implementation existed), not as separate RED commits.

## Known Stubs

None.

## Threat Flags

None. T-06-12, T-06-13 and T-06-15 mitigations are implemented as planned (AND of duplicate availability, server text only in `FieldError.Text`, ACCOUNT_SUSPENDED returns `Silent`).

## Self-Check: PASSED

All seven created files and the modified strings file exist; commits b192e5f, 9220589 and 67f5f59 are present. STATE.md and ROADMAP.md were not modified.
