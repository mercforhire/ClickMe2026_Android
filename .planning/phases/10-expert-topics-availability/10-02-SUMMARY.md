---
phase: 10-expert-topics-availability
plan: 02
subsystem: expert-availability
tags: [pure-logic, tdd, availability, timezone]
status: complete

requires:
  - phase: earlier-phase
    provides: "constants/Timezones.kt (TimezoneEntry, Timezones.all, entryFor)"
provides:
  - "TimeSlot / DaySchedule model and AvailabilityScheduleLogic (hydrate, overlap, edits, wire format, picker options, display format, saved-state codec, name join)"
  - "AvailabilityTimezoneLogic (isUtc, autoFillZone, label, pickerEntries)"
affects: [10-08 availability components, 10-12 availability ViewModel]

commits: 4
plan_head_before: bc1f77406ae16cf9279280e323115b90dcd6b967
plan_head_after: ca5a51262270fbc132eb13b0622c8f280fa14b4a
actuals:
  tasks: 2
  commits: 4

tech-stack:
  added: []
  patterns: ["Android-free pure logic objects with JVM tests", "UTC-pinned DateFormat clone for clock-face formatting"]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScheduleLogic.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityTimezoneLogic.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScheduleLogicTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityTimezoneLogicTest.kt
  modified: []

key-decisions:
  - "End picker keeps the iOS 23:30 cap; 1440 (24:00) round-trips through model, hydrate, wire and picker (current value injected), nothing calls LocalTime.of(24, 0)"
  - "Overlap uses a running maximum end with strict start < maxEnd (server rule), stricter than iOS adjacent-only"
  - "Timezone label order: bundled label, then server display_name for the server's own zone, then runtime label"

requirements-completed: [EXP-03, EXP-04]
---

# Phase 10 Plan 02: Availability Pure Logic Summary

Android-free availability schedule and timezone logic with 43 JVM tests, including lossless 24:00 handling and ASCII-digit wire output.

## Accomplishments

- `AvailabilityScheduleLogic`: model, defaults (17:00-21:00), strict `HH:mm` parse (incl. 24:00), `Locale.ROOT` formatting, hydration (drops malformed/inverted slots, enabled iff slots kept), server-matching overlap detection, setEnabled / addHours / removeSlot / replaceSlot, sorted wire ranges, Start/End picker options preserving off-grid and 24:00 values, UTC-pinned `formatMinutes`, validated `v1|tz|...` saved-state codec, `joinNames`.
- `AvailabilityTimezoneLogic`: `isUtc` (UTC / Etc/UTC, case-insensitive), `autoFillZone`, `label`, `pickerEntries` (selected and server zones first when unbundled, no duplicates).
- Tests: `AvailabilityScheduleLogicTest` 34 cases, `AvailabilityTimezoneLogicTest` 9 cases; `ktlintCheck` clean.

## Task Commits

1. Task 1 RED `d60f72a`, GREEN `796429e`
2. Task 2 RED `5f59440`, GREEN `ca5a512`

## Deviations from Plan

None - plan executed as written. (Seven-day decode requires exactly seven `;`-separated entries and a non-blank timezone id; the codec format was left to executor discretion by the plan.)

## Known Stubs

None.

## Threat Flags

None. T-10-05..T-10-08 mitigations are implemented and tested (strict parse, no `LocalTime.of`, validated decode, Arabic-locale digit test).

## Self-Check: PASSED

All four source/test files exist and all four commits are in the branch history.
