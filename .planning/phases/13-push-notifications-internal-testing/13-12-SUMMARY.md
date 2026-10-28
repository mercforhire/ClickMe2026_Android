---
phase: 13-push-notifications-internal-testing
plan: 12
subsystem: notifications-ui
tags: [compose, notifications, ui, bell]
requires:
  - phase: 13-04
    provides: NotificationsKey, notificationsKeyFor, NotificationsRoute placeholder, shell registration
  - phase: 13-08
    provides: NotificationsViewModel, NotificationsUiState, NotificationRowModel, parseNotificationBody
provides:
  - NotificationsScreen / NotificationsContent (skeleton, error, empty, grouped list, end note, pull-to-refresh)
  - NotificationRow, NotificationIconTile, NotificationBodyText, notificationTimeLabel
  - NotificationsOffRow, NotificationsEndNote, NotificationsSkeleton, openNotificationSettings
  - NotificationBellButton on the Explore and Dashboard top bars
affects: [13-15, 13-18]
tech-stack:
  added: []
  patterns:
    - "MutableTransitionState keeps the permission-off LazyColumn item alive for its 200 ms exit"
    - "AnimatedContent keyed by a load-kind enum so quiet reloads never replay the cross-fade"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/NotificationBellButton.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/subviews/NotificationRow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/subviews/NotificationsListParts.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsScreenTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardRoute.kt
    - app/src/main/res/values/strings_notifications.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreenTest.kt
key-decisions:
  - "NotificationsScreen takes (onBack, modifier, viewModel = hiltViewModel()) following the project's Hilt-screen convention, not the plan's (viewModel, onBack) order"
  - "Test tag notifications_retry sits on the EmptyState container; the Retry button itself is reached by its text, because EmptyState does not expose a tag for its action and the shared component was left untouched"
requirements-completed: [PUSH-05]
status: complete
commits: 3
plan_head_before: 86fbe6fb30a99c7498c4d9e01449e270f329f86c
plan_head_after: 59bbd4b0d860251a8b763c56062cb91b8ae5ba53
actuals:
  tokens: 42000
  tasks: 3
  commits: 3
duration: ~1h 15m (mostly Gradle builds)
completed: 2026-10-28
---

# Phase 13 Plan 12: Notifications list UI and bells Summary

Both entry points (the bell on the client Explore bar and the expert Dashboard bar) now open the notifications list, rendered per UI-SPEC Surface 1: date sections, 48dp category tiles, semibold bold spans, relative timestamps, inert versus tappable rows, the permission-off row, the end note, the skeleton, empty and error states, and pull-to-refresh with its snackbar.

## Tasks

| Task | Commit | What |
| ---- | ------ | ---- |
| 1 (tracer) | ad9b1f9 | Bell component, Explore bell wiring through HomeClientScreen, route body, screen with grouped rows, time labels, strings |
| 2 | 6fbb2c9 | Category tiles, bold spans, merged Role.Button rows (inert rows have no click modifier), end note, permission-off row with settings fallback |
| 3 | 59bbd4b | Skeleton, empty, error with Retry, PullToRefreshBox and snackbar, cross-fade, Dashboard bell |

Tracer gate: the tracer `<verify>` is automated-only (`HomeShellsTest`, `ExploreScreenTest`, `NotificationsScreenTest`, ktlint, `assembleDevDebug`); it passed end to end before expansion.

## Verification

- `NotificationsScreenTest` 15, `ExpertDashboardScreenTest` 25, `ExploreScreenTest` 20, `HomeShellsTest` all pass; `ktlintCheck` and `:app:assembleDevDebug` pass; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: bell tags 1 each, `notificationsKeyFor(AppMode.Client)` 1, `notificationsKeyFor(AppMode.Expert)` 1, `ACTION_APP_NOTIFICATION_SETTINGS` 1, `ACTION_APPLICATION_DETAILS_SETTINGS` 1, `notifications_off_row` 1, `Role.Button` in NotificationRow 1; no `fontSize`, `Color(0x` or hard-coded `Text("...")` in the notifications package.

## Deviations from Plan

None of Rules 1-4. Implementation choices:

- `NotificationsScreen(onBack, modifier, viewModel)` argument order (see key-decisions).
- `notifications_retry` tag placement (see key-decisions); the Retry action is exercised by its text in the test.
- The `largeFontWraps` test asserts the configuration (no `maxLines`, no ellipsis, full text present, "Turn on" visible at fontScale 2) rather than measured line counts: Robolectric's text metrics are not real (a 90-character title measured 80 px wide on one line), so a layout-based wrap check would be meaningless there.
- In the Loaded-with-rows state the permission-off row is the first LazyColumn item (scrolls with the list); in the empty state it is the first item of the scrolling column above the EmptyState, which is top-aligned instead of vertically centred so the row can sit above it.
- Rows keep `maxLines` unset; `heightIn(min = 48.dp)` per plan.

## Open items (human / device, deferred)

- UI-SPEC E3 populated: the permission-off row appearing with notifications disabled and clearing with the 200 ms fade and shrink after enabling them in system settings and returning (ON_RESUME), on a real Android 13+ device (Samsung test device).
- UI-SPEC E1 populated: a foreground push while the list is open reloads silently and the new row appears in Today (the ViewModel tick-to-reload is unit-tested in 13-08; the on-device check is still open).
- UI-SPEC E2 populated: bold-span look (semibold, onSurface) and a real 200% font scale pass on a device; Robolectric cannot verify wrapping.
- TalkBack: the tappable row sets a merged `contentDescription` while its child texts remain; confirm TalkBack does not read the row twice (the full TalkBack pass is Phase 15 POL-04).
- Tapping a row on a device to confirm it routes like a push tap (the router and slot are unit-tested in 13-04/13-08).

## Known Stubs

None.

## Threat Flags

None. T-13-32 (markup injection) is mitigated by the 13-08 parser, with only `**` pairs becoming bold and no HTML or link rendering; T-13-33 (missing settings activity) by `openNotificationSettings` falling back to the app details screen and then doing nothing.

## Self-Check: PASSED

All created files exist; commits ad9b1f9, 6fbb2c9, 59bbd4b are on the branch; STATE.md and ROADMAP.md were not modified.
