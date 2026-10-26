---
phase: 11-expert-requests-bookings
plan: 02
subsystem: navigation
tags: [navigation, nav3, shell, keys, wave-0, hilt, compose]

requires:
  - phase: 10-expert-topics-availability
    provides: HomeExpertShell topicsRoot / detailEntries slots, AvailabilityKey origin trick
  - phase: 08-client-bookings
    provides: BookingsLaunch / BookingsTab / BookingsNotice types, showMyBookings pattern
provides:
  - dashboardRoot and bookingsRoot slots on HomeExpertShell plus ExpertDashboardRoute / ExpertBookingsRoute
  - IncomingRequests, RequestDecision, BookingConfirmed, ExpertBookingSummary and ExpertCancel route keys
  - ExpertBookingOrigin and AvailabilityOrigin Dashboard / Bookings values
  - Navigator.showDashboard and Navigator.showExpertBookings
  - ExpertLaunchRequest singleton with three independent OneShotSlots
  - five per-feature entry registration functions already called from HomeExpertScreen
affects: [11-04, 11-05, 11-06, 11-07, 11-08, 11-09, 11-10, 11-12, 11-13]

actuals:
  tokens: 6700
  tasks: 3
  commits: 3
plan_head_before: f3a31401b9bcde5033bd4b1dc7f224291d2c72ae
plan_head_after: a3a525b95eb303cb0c2baaba3bab012fcd2d5301

tech-stack:
  added: []
  patterns:
    - "Tab-root slots on the shell: HomeExpertScreen supplies Hilt-backed roots, defaults stay Hilt-free for Robolectric"
    - "One registration function per feature package so feature plans never edit HomeExpertScreen.kt"
    - "One consumer per OneShotSlot (consume-on-read StateFlow singleton)"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertNavigation.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertLaunchRequest.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsEntries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionEntries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedEntries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryEntries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancelKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancelEntries.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/ExpertNavigationTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertLaunchRequestTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityKey.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt

key-decisions:
  - "ExpertTabRoot is internal so the route files render the same placeholder the shell defaults use"
  - "Registration functions are empty bodies with @Suppress(UNUSED_PARAMETER) so the shell compiles before each feature exists"
  - "RequestDecisionKey and BookingConfirmedKey carry list/accept data in the key (epoch millis, major-unit earnings) because the detail and accept responses lack it"
  - "OneShotSlot is a generic class reused for all three expert slots; BookingsLaunch from Phase 8 is reused for the bookings slot"

patterns-established:
  - "Feature plans register routes from their own package; HomeExpertScreen is not edited again until the phase gate"
  - "Key round-trip tests use the positional AbstractEncoder/Decoder because kotlinx-serialization-json is not on the unit-test classpath"

requirements-completed: [EXP-05, EXP-06, EXP-07, EXP-08, EXP-09]

coverage:
  - id: D1
    description: "Dashboard and Bookings tab roots render through shell slots supplied by HomeExpertScreen; defaults keep existing shell tests green"
    requirement: "EXP-05"
    verification:
      - kind: unit
        ref: "HomeShellsTest#expertDashboardRootSlotRendersSuppliedContent"
        status: pass
      - kind: unit
        ref: "HomeShellsTest#expertBookingsRootSlotRendersSuppliedContent"
        status: pass
    human_judgment: false
  - id: D2
    description: "All Phase 11 route keys and origins exist and round-trip through kotlinx.serialization; origins keep Nav3 content keys distinct"
    requirement: "EXP-06"
    verification:
      - kind: unit
        ref: "ExpertNavigationTest#keysRoundTripThroughSerialization"
        status: pass
      - kind: unit
        ref: "ExpertNavigationTest#originKeepsKeysDistinct"
        status: pass
    human_judgment: false
  - id: D3
    description: "Accept replaces Request Decision with Confirmed (Back lands on Incoming Requests); Back to Home and post-cancel stack resets"
    requirement: "EXP-07"
    verification:
      - kind: unit
        ref: "ExpertNavigationTest#acceptReplacesDecisionWithConfirmed"
        status: pass
      - kind: unit
        ref: "ExpertNavigationTest#backToHomeResetsDashboardFromAnyTab"
        status: pass
      - kind: unit
        ref: "ExpertNavigationTest#finishedCancelFromDashboardOriginLandsOnBookings"
        status: pass
    human_judgment: false
  - id: D4
    description: "ExpertLaunchRequest holds three independent one-shot slots, each consumed exactly once"
    requirement: "EXP-08"
    verification:
      - kind: unit
        ref: "ExpertLaunchRequestTest#slotsAreIndependent"
        status: pass
      - kind: unit
        ref: "ExpertLaunchRequestTest#consumeReturnsOnceThenNull"
        status: pass
      - kind: unit
        ref: "ExpertLaunchRequestTest#laterPostReplacesUnconsumed"
        status: pass
    human_judgment: false
  - id: D5
    description: "Hilt graph builds with the new singleton and HomeExpertScreen calls the five registration functions"
    requirement: "EXP-09"
    verification:
      - kind: other
        ref: "./gradlew :app:assembleDevDebug"
        status: pass
    human_judgment: false

duration: 18min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 02: Expert navigation skeleton Summary

**Expert shell Dashboard and Bookings tab-root slots, every Phase 11 Nav3 route key with origin enums, cross-tab Navigator resets, a three-slot one-shot hand-off singleton, and five per-feature entry registration functions already wired into HomeExpertScreen**

## Performance

- **Duration:** about 18 min
- **Started:** 2026-10-26T17:05:00Z
- **Completed:** 2026-10-26T17:24:00Z
- **Tasks:** 3
- **Files modified:** 19 (16 created, 3 modified)

## Accomplishments
- `HomeExpertShell` renders the Dashboard and Bookings roots through `dashboardRoot` / `bookingsRoot` slots; `HomeExpertScreen` supplies `ExpertDashboardRoute` / `ExpertBookingsRoute`, and the Hilt-free defaults keep all 44 `HomeShellsTest` tests passing.
- Five route keys plus `ExpertBookingOrigin` exist, are `@Serializable`, and round-trip; `AvailabilityOrigin` gains `Dashboard` and `Bookings`.
- `Navigator.showDashboard()` and `showExpertBookings()` proven on a real `Navigator`, along with the accept `replaceTop` and Back behaviour.
- `ExpertLaunchRequest` provides independent `incomingRequests`, `dashboard` and `bookings` one-shot slots; the Hilt graph builds (`assembleDevDebug`).
- Feature plans 11-05 to 11-09 can register their routes by editing only their own package.

## Task Commits

1. **Task 1: tab-root slots** - `3dd6533` (feat)
2. **Task 2: route keys, origins, Navigator helpers** - `0b17d17` (feat)
3. **Task 3: launch store and entry registration functions** - `a3a525b` (feat)

**Plan metadata:** committed with this SUMMARY (docs: complete plan)

## Files Created/Modified
- `screens/expertmode/home/HomeExpertScreen.kt` - dashboardRoot / bookingsRoot slots, internal `ExpertTabRoot`, five registration calls
- `screens/expertmode/dashboard/ExpertDashboardRoute.kt`, `upcomingbookings/ExpertBookingsRoute.kt` - tab-root routes (placeholder bodies until 11-10 / 11-04)
- `screens/expertmode/{incomingrequests,requestdecision,confirmed,bookingsummary,cancellation}/*Key.kt` - route keys
- `screens/expertmode/*/…Entries.kt` - empty registration functions
- `screens/expertmode/ExpertNavigation.kt` - `showDashboard`, `showExpertBookings`
- `screens/expertmode/ExpertLaunchRequest.kt` - `OneShotSlot`, launch types, singleton store
- `screens/expertmode/availability/AvailabilityKey.kt` - origin values
- Tests: `HomeShellsTest` (+2), `ExpertNavigationTest` (6), `ExpertLaunchRequestTest` (5)

## Decisions Made
- Registration functions carry `@Suppress("UNUSED_PARAMETER")` while their bodies are empty; the feature plans remove it when they use `navigator`.
- `RequestDecisionKey` KDoc says "correct to the cent" rather than an exact phrase from the plan, with the same meaning.

## Deviations from Plan

None - plan executed exactly as written, apart from process notes below.

Process notes (not plan deviations):
- Tasks are marked `tdd="true"` but each introduces new symbols, so a RED run would be a compile failure (INVALID_RED). Tests and implementation were committed together per task; every task's tests were run green before commit.
- ktlint required a multi-line form for the single-entry enum `IncomingRequestsNotice`; fixed before the Task 3 commit.
- The per-plan commit ledger file under the git dir could not be written (the worktree guard blocks it), so `plan_head_before` was taken from the known spawn base.

## Issues Encountered
None.

## User Setup Required

None - no external service configuration required.

## Known Stubs

`ExpertDashboardRoute` and `ExpertBookingsRoute` render the tab placeholder, and the five registration functions have empty bodies. All are intentional: the plan's purpose is the skeleton, and plans 11-10 (Dashboard), 11-04 (Bookings), 11-05 to 11-09 (features) replace them.

## Threat Flags

None - no new network, auth or file surface. T-11-07 mitigation (one slot per consumer, consume-on-read) is covered by `ExpertLaunchRequestTest`.

## Next Phase Readiness
Wave 1 feature plans can proceed in parallel: each edits only its own route or registration file.

## Self-Check: PASSED

All created files exist and commits `3dd6533`, `0b17d17`, `a3a525b` are ancestors of HEAD. Verify chain (tests, ktlintCheck, `assembleDevDebug`) exits 0.

---
*Phase: 11-expert-requests-bookings*
*Completed: 2026-10-26*
