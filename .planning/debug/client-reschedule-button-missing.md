---
status: diagnosed
trigger: "UAT 16.2 Test 1: Upcoming booking card and Booking Details have no Reschedule button (client)"
created: 2026-11-01
goal: find_root_cause_only
---

## Current Focus
hypothesis: D-04 gate canClientStartReschedule is false for the tester's booking (start <24h, pending_approval or pending_reschedule); everything downstream is correct
next_action: confirm the tester's booking status pill and start time (falsification: Confirmed and >=24h away yet no button)

## Symptoms
expected: client Reschedule button on My Bookings upcoming card and Upcoming Booking Details
actual: "Upcoming booking card has no reschedule button. The booking details page also has no reschedule button."
errors: none
reproduction: 16.2-UAT.md Test 1, Samsung device, live/test backend, sideloaded build of main (includes all 16.2 commits)

## Eliminated
- Rendering/wiring: RescheduleActionButton/ActionButtonPair fine; UpcomingBookingCard.kt:201-222 renders it when onReschedule != null; MyBookingsScreen.kt:361 and UpcomingBookingScreen.kt:449 call the gate; nav keys wired (HomeClientScreen.kt:131, ClientBookingManagementEntries.kt:99).
- Status-string mismatch: backend UPCOMING_STATUSES = pending_approval|confirmed|pending_reschedule|in_progress; gate is a denylist of two real strings; detailStatusDisplay().raw is unmodified server string.
- Time parsing / clock: InstantAdapter uses OffsetDateTime.parse (failure would break the whole list); JoinClock uses Clock.systemUTC(), no fixed clock in main.
- Moshi field names/nulls: status and start_time are non-null required fields on ClientBookingItem; the gate does not read optional fields.
- Stale build: expert 16.2 UI present on device (Tests 2/3); client entries landed in the same wave merges.
- Unrealistic tests: tests use "confirmed" and far-out starts; they encode D-04 exactly.

## Evidence
- RescheduleRules.kt:38-45: false if pending_reschedule|pending_approval or start-now < 24h.
- Backend src/utils/reschedule.js:88 canReschedule: confirmed|pending_approval|pending_reschedule allowed at any time; assertConversionAllowed refuses only a conversion whose PROPOSED start is <24h. So the 24h and pending_approval hides are UX policy (D-04), not server mirrors; CONTEXT D-04's "server refuses those moves" is inaccurate.
- iOS UpcomingBookingCard.swift:270-274 shows Reschedule unconditionally.
- Runbook D1.RS1-RS8 unticked, so UAT Test 4 "pass" is not evidence a client button was ever seen.
- Both surfaces share one gate, matching "both missing at once".

## Resolution
root_cause: D-04 gate hides the button for bookings that are <24h away, pending_approval or pending_reschedule; the UAT truth/test omitted that precondition. No defect in rendering, wiring or data.
fix: (not applied) retest with confirmed >=24h booking, or relax D-04 (drop the 24h and/or pending_approval clause), or show a hint instead of hiding.
files_changed: []
