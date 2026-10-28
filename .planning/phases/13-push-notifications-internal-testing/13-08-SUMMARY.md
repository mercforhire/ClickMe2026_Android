---
phase: 13-push-notifications-internal-testing
plan: 08
subsystem: notifications
tags: [notifications, viewmodel, grouping, pure-logic]
requires:
  - phase: 13-01
    provides: PushEvent.fromRow, PushRouter, PushLaunchRequest
  - phase: 13-03
    provides: NotificationsRepository.recent, NotificationItem, test fixtures
provides:
  - NotificationsViewModel (load, quiet reload, pull, row tap)
  - groupNotifications / notificationTime (typed relative time)
  - parseNotificationBody / stripBoldMarkers
  - NotificationRowModel with push-router destination
affects: [13-12]
tech-stack:
  added: []
  patterns: [Loud/Quiet/Pull load modes, latest-wins loadJob, typed time labels formatted by the screen]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationRows.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsGrouping.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationBodyParser.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsGroupingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationBodyParserTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationDestinationTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsViewModelTest.kt
  modified: []
key-decisions:
  - "Arrival-tick collector compares against the value seen at construction instead of drop(1), so a tick racing the collector start is never swallowed"
  - "A Quiet load before the first success is promoted to Loud (nothing to keep on screen)"
requirements-completed: [PUSH-05]
status: complete
commits: 3
plan_head_before: 6fa01f099efa3257b758f859ff491cd82e307b64
plan_head_after: f287bc7687859bd3dfb1fad0ce37533fbd6e4007
actuals:
  tokens: 22000
  tasks: 3
  commits: 3
duration: ~1h (mostly Gradle builds)
completed: 2026-10-28
---

# Phase 13 Plan 08: Notifications list logic Summary

Everything behind the notifications list except the pixels: local-day grouping (Today/Yesterday/Earlier), typed relative-time labels, a `**bold**` body parser, row-to-destination mapping through the push router, and a ViewModel with first load, quiet reloads (resume and foreground-push tick), pull-to-refresh and row taps feeding `PushLaunchRequest.destination`.

## Tasks

| Task | Commit | What |
| ---- | ------ | ---- |
| 1 (tracer) | 009224d | Row model, grouping, UI state, ViewModel first load of 50 rows |
| 2 | c1788a4 | notificationTime labels, DST/midnight edges, bold parser, row destinations (payout/account/unknown inert) |
| 3 | f287bc7 | Loud/Quiet/Pull loads, arrival-tick reload, screen shown/hidden, row tap posts destination |

Tests: NotificationsGroupingTest 9, NotificationBodyParserTest 7, NotificationDestinationTest 12, NotificationsViewModelTest 12 (all pass); `ktlintCheck` and `scripts/check-design-rules.sh` pass.

## Deviations from Plan

- Arrival collector uses `tick != seenAtConstruction` rather than `arrivals.drop(1)` (same behavior, no race with collector start). Not a rule deviation, an implementation choice.
- Task 1 and Task 2 tests were written together with their code in each task's turn rather than as separate red commits; the red/green gate was not committed separately.
- Test fix during Task 3: the payout fixture row falls in Yesterday, so the tap test flattens sections instead of using `single()` (test bug, not a code bug).

## Open items (human / device, deferred)

- UI-SPEC E1 populated: foreground push while the list is open reloads silently and the new row shows in Today (tick-to-quiet-reload is unit-tested; the device check belongs to 13-12 screen).
- UI-SPEC E2 populated: bold span rendering (weight 600, onSurface) and literal unmatched markers on a device (parser unit-tested).

## Known Stubs

None.

## Threat Flags

None. T-13-21 (UUID-validated ids, non-UUID gives inert row) and T-13-22 (`toString` prints id and category only, no logging) are mitigated and tested.

## Self-Check: PASSED

All nine files exist; commits 009224d, c1788a4, f287bc7 are on the branch.
