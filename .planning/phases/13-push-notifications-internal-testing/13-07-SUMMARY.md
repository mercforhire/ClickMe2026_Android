---
phase: 13-push-notifications-internal-testing
plan: 07
subsystem: push
tags: [push, routing, resolver, mode-switch, pure-logic, hilt]

requires:
  - phase: 13-push-notifications-internal-testing
    provides: "PushDestination / PushAudience (13-01); ShellRoute, notificationsKeyFor/RootFor, PushRoutingModule (13-04)"
provides:
  - "PushDestinationResolver.resolve(destination, PushRoutingContext): ResolvedPush(targetMode, PushRoute.Shell | PushRoute.Chat)"
  - "ClientBookingProbe and PendingRequestLookup seams with repository implementations, bound in PushRoutingModule"
  - "allowSwitch rule: no push-driven mode switch during a call; never into Expert without the expert role"
affects: [13-11 root host push orchestration, 13-12 ShellRoute consumption]

actuals:
  tokens: 8000
  tasks: 3
  commits: 6

tech-stack:
  added: []
  patterns:
    - "Pure suspend resolver behind fun-interface lookup seams, bounded by withTimeoutOrNull(5 s)"
    - "Probe-by-endpoint role detection: GET client/bookings/{id} 404/403 means the caller is the expert"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestinationResolver.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushRouteLookups.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestinationResolverTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushRouteLookupsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakePushRouteLookups.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PushRoutingModule.kt

key-decisions:
  - "A push whose target mode differs is honoured only when allowSwitch holds; otherwise the current mode's notifications list opens (D-05 fallback)"
  - "The expert-request list call is skipped when the switch to Expert is already refused, saving a network round trip"
  - "Client-audience push with a NotClient probe routes to the expert summary like Either (defensive, per plan Assumptions)"

patterns-established:
  - "Resolver returns routes only; the caller (13-11) performs the mode switch and posts the ShellRoute"

requirements-completed: [PUSH-04]

duration: ~45min
completed: 2026-10-28
status: complete
---

# Phase 13 Plan 07: Push Destination Resolver Summary

**Pure, JVM-tested PushDestinationResolver that turns a tapped push into a mode, tab and key chain using a client-booking status probe, a pending-request expiry lookup and a guarded mode-switch rule.**

## Performance

- **Tasks:** 3 (1 tracer, 2 auto, all TDD)
- **Files:** 5 created, 1 modified
- **Tests added:** PushDestinationResolverTest 20, PushRouteLookupsTest 4 (all green)

## Accomplishments

- Client booking push probes `GET client/bookings/{id}` (5 s cap): `confirmed`, `pending_approval`, `pending_reschedule` and `in_progress` open `UpcomingBookingKey`, every other status opens `BookingSummaryKey`, both on the client Bookings tab (D-06).
- 404/403 from the probe, and any Expert-audience push (no probe), route to `ExpertBookingSummaryKey(id, Bookings)` on the expert Bookings tab. An unreadable probe keeps a Client push on the client summary and an Either push in the current mode (D-05/D-06 fallbacks).
- Review landings always open the summary, never a review screen (D-07). Chat stays in the current mode and ignores an active call (D-09). Payouts, KYC and Account Security open the current mode's notifications list behind a `PHASE-14` marker (D-10/D-11).
- Expert request push resolves to `[IncomingRequestsKey, RequestDecisionKey]` with the list row's expiry, or to `[IncomingRequestsKey]` when the id is absent, the list fails or there is no id (D-08, A10).
- `allowSwitch` blocks a switch during a call and a switch to Expert without the expert role, closing the client-only-account conversion hazard (T-13-18, T-13-19).

## Task Commits

1. **Task 1 (tracer): client booking push by live status**
   - RED `ae890d3` test: failing status-routing tests, probe seam, fakes, skeleton resolver
   - GREEN `df952a2` feat: BookingDetail probe routing, NotificationsList, `bindClientBookingProbe`
2. **Task 2: audience, expert summary, landings, mode-switch rules**
   - RED `af9520e` test
   - GREEN `888e078` feat
3. **Task 3: expert request decision with list expiry**
   - RED `026284c` test
   - GREEN `e0b1fdf` feat (also `bindPendingRequestLookup`)

**Plan metadata:** committed with this SUMMARY (docs).

## TDD Gate Compliance

Each task has a `test(13-07)` commit followed by a `feat(13-07)` commit. RED evidence was taken from the Gradle run: the named target tests failed on their planned assertions (route mismatch from a skeleton that always returned the notifications list or null), not on compile or fixture faults. The RED commits carry a minimal skeleton in main sources so the test source set compiles. No REFACTOR commits were needed.

## Decisions Made

- The IncomingRequests branch checks `allowSwitch(current, Expert)` before the list lookup, so a refused switch costs no network call. Same outcome as the plan's post-check, one fewer request.
- `PushRoute.Chat` carries only `ChatThreadKey(threadId)`; Phase 12 fills peer details when it opens the thread.

## Deviations from Plan

None - plan executed exactly as written. Minor additions: two extra tests beyond the listed behaviors (`leaveReviewNeverOpensUpcomingBooking`, `expertPushSwitchesClientToExpertWithTheRole`, `noExpertRoleNotClientProbeOpensClientList`) and one extra lookup test for an elapsed expiry.

## Issues Encountered

- ktlint line-length and argument-wrapping violations in the first drafts of two main-source lines; fixed by extracting locals.

## Verification

- `:app:testDevDebugUnitTest --tests '*PushDestinationResolverTest' --tests '*PushRouteLookupsTest' --tests '*IncomingRequests*' ktlintCheck :app:assembleDevDebug`: BUILD SUCCESSFUL (Hilt graph valid, existing Incoming Requests tests green)
- `bash scripts/check-design-rules.sh`: "Design rules: OK"
- Acceptance greps: `class PushDestinationResolver` 1, `LOOKUP_TIMEOUT_MS = 5_000L` 1, `bindClientBookingProbe` 1, `bindPendingRequestLookup` 1, `PHASE-14` 1, `fun allowSwitch` 1, `ExpertBookingOrigin.Bookings` 1, `toDecisionKey` present in PushRouteLookups.kt

## Known Stubs

None.

## Open Items (human / later plans)

- None for a human. 13-11 must call `resolve`, perform the mode switch implied by `ResolvedPush.targetMode`, and post `PushRoute.Shell` to `ShellRouteRequest` or open `PushRoute.Chat` through Phase 12.

## Threat Flags

None. The only new surface is two authenticated GETs the app already makes elsewhere.

## Self-Check: PASSED

- Created files exist (resolver, lookups, two tests, fakes) and commits `ae890d3`, `df952a2`, `af9520e`, `888e078`, `026284c`, `e0b1fdf` are ancestors of HEAD.

---
*Phase: 13-push-notifications-internal-testing*
*Completed: 2026-10-28*
