---
phase: 06-booking-flow-free-bookings
plan: 03
subsystem: booking
tags: [java-time, calendar, timezone, formatting, pure-functions, tdd]
requires: []
provides:
  - monthCells / weekdayOrder / firstDayOfWeek / isPastDay / canGoToPreviousMonth
  - TimezoneNote / timezoneNote / zoneAbbreviation / zoneCity
  - NOTES_MAX_CHARS / capNotes / formatSlotTime / formatSlotDateLine / formatShortDate / formatMonthYear / formatDayLabel / rememberDeviceTimeFormat
affects: [06-04, 06-05, 06-06]
tech-stack:
  added: []
  patterns: [pure top-level functions taking zone/locale/today as parameters, cloned DateFormat per call]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingCalendar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTimezoneNote.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormatting.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingCalendarTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTimezoneNoteTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormattingTest.kt
  modified: []
key-decisions:
  - "Week start follows the locale (WeekFields), Sunday for en-US and Monday for en-GB"
  - "Timezone abbreviations computed at noon of the selected date so EST/EDT switch by date; raw IANA id never returned"
  - "capNotes drops a trailing lone high surrogate rather than splitting an emoji"
requirements-completed: [BOOK-01, BOOK-02]
duration: ~25min
completed: 2026-10-22
---

# Phase 6 Plan 03: Booking calendar, timezone note and formatting Summary

Pure, JVM-tested java.time functions for the booking month grid and D-06 bounds, the D-05 friendly timezone note (DST-correct by selected date, safe on bad zone ids), and device-format time/locale date formatting with a surrogate-safe 500-unit notes cap.

## Tasks

| Task | Name | Commits |
| ---- | ---- | ------- |
| 1 | Month grid and calendar bounds | RED ee2689b, GREEN e189099 |
| 2 | Friendly timezone note | RED 57744f2, GREEN 7340984 |
| 3 | Formatting and notes cap | RED b8bf162, GREEN c82064c |

## Verification

`./gradlew :app:testDevDebugUnitTest --tests '*BookingFormattingTest' --tests '*BookingCalendarTest' --tests '*BookingTimezoneNoteTest' ktlintCheck` exits 0. BookingCalendarTest 10/10, BookingTimezoneNoteTest 7/7, BookingFormattingTest 11/11 (Robolectric).

## Deviations from Plan

None - plan executed as written. The worktree base was behind the expected base commit (6c2fd02) and was fast-forwarded with `git reset --hard` per the startup check (working tree was clean).

## Known Stubs

None.

## Threat Flags

None. T-06-09 (invalid zone id caught, tested with "" and "Not/A_Zone"), T-06-10 (surrogate-safe cap, tested) and T-06-11 (only city and abbreviation returned, tested) are mitigated.

## Self-Check: PASSED
