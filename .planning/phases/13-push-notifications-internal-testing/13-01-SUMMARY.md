---
phase: 13-push-notifications-internal-testing
plan: 01
subsystem: push
tags: [push, router, pure-logic, wave-0, hilt]
requires: []
provides:
  - PushEvent (payload parser with event_type fallback and UUID validation)
  - PushDestination / PushAudience / PushRouter / landsOnNotificationsList
  - PushLaunchRequest (one-shot pending destination + foreground arrivals counter)
  - PushModule (SessionDataCleaner binding)
affects: [13-02, 13-06, 13-07, 13-10]
tech-stack:
  added: []
  patterns: [OneShotSlot singleton hand-off, "@Binds @IntoSet SessionDataCleaner"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushEvent.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestination.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushLaunchRequest.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PushModule.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushEventTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushRouterTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushLaunchRequestTest.kt
  modified: []
key-decisions:
  - "Review sub_categories other than nudge/received route to BookingDetail with audience Either"
  - "Android keeps the full event_type fallback even after 13-02 adds category (lagging deploys must not break routing)"
requirements-completed: [PUSH-03, PUSH-04]
status: complete
duration: ~35 min (most of it a cold Gradle build)
completed: 2026-10-28
actuals:
  tokens: 14000
  tasks: 3
  commits: 3
plan_head_before: 1c9c187606c03a3bdbc76c63f8635978b6b8849d
plan_head_after: a2e9de1e1318af0078ea5a1ce4978a6141ccd972
commits: 3
---

# Phase 13 Plan 01: Push routing core Summary

Pure push routing core: any backend push payload (with or without `category`) parses to a validated `PushEvent`, maps to the same destination iOS would pick plus the recipient audience, and waits once in a session-cleared slot.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Booking payload parses, maps to BookingDetail, waits in the pending slot (tracer) | c7f5152 |
| 2 | event_type fallback, booking-only payload, id aliases, UUID validation, list rows | 316ef17 |
| 3 | Full iOS destination table, audience, notifications-list landings | a2e9de1 |

## Verification

- `:app:testDevDebugUnitTest` for PushEventTest (9 tests), PushRouterTest (6), PushLaunchRequestTest (3): all pass, 0 failures.
- `ktlintCheck` passes; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Tracer verified end-to-end after Task 1 (tests and ktlint) before expanding.

## Deviations from Plan

None of substance. Two small sequencing notes: the booking-audience helper was introduced in Task 1 (the Task 1 test needs `Client` for confirmations), and the Task 1 version of PushRouterTest held only the tracer test; the remaining router tests landed with Task 3.

## Threat model

T-13-01 mitigated (`canonicalUuid`, `nonUuidIdsAreDropped`); T-13-03 mitigated (`PushLaunchRequest` bound as `SessionDataCleaner`, `clearSessionDataDropsPending`). No new surface beyond the plan.

## Known Stubs

None.

## Open items

- Edge probe item PUSH-03 (unclassified) remains flagged for the verifier, as stated in the plan assumptions.
- No device or human steps in this plan.

## Self-Check: PASSED

All seven files exist; commits c7f5152, 316ef17 and a2e9de1 are on HEAD.
