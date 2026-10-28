---
phase: 13-push-notifications-internal-testing
plan: 11
subsystem: push-routing
tags: [push, routing, root-host, mode-switch, cold-start]
requires:
  - phase: 13-01
    provides: PushLaunchRequest.destination slot (SessionDataCleaner)
  - phase: 13-04
    provides: ShellRouteRequest, PushRoutingModule
  - phase: 13-07
    provides: PushDestinationResolver, FakeClientBookingProbe, FakePendingRequestLookup
provides:
  - PushRoutingViewModel (route watcher, resolver driver, mode-switch coordinator) and PushRoleSource seam
  - PushRoutingHost root composable mounted in ClickMeApp
  - PushRoutingModule.pushRoleSource (roles from /me, else login user)
affects: [13-17]
tech-stack:
  added: []
  patterns: [root ViewModel combining route and one-shot slot, switch request flow consumed by a non-visual host]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushRoutingViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushRoutingHost.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushRoutingViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushRoutingHostTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PushRoutingModule.kt
key-decisions:
  - "Cancelling an older resolution is followed by join() so its switch-request cleanup cannot clear the newer tap's request"
  - "The route is re-checked after the resolver returns and again before posting, so a tap never lands in another session or mode"
  - "A tap arriving on SignupSetup is dropped like Login (RESEARCH A3)"
requirements-completed: [PUSH-04]
status: complete
commits: 3
plan_head_before: 86fbe6fb30a99c7498c4d9e01449e270f329f86c
plan_head_after: f863e3a82740336be4349c4ec81e1e89d5c1fde3
actuals:
  tokens: 16000
  tasks: 3
  commits: 3
duration: ~35min
completed: 2026-10-28
---

# Phase 13 Plan 11: Root push router Summary

A root `PushRoutingViewModel` holds a tapped push through Splash, resolves it once a home route exists, switches mode through `AppViewModel.switchMode` (via `PushRoutingHost`) when the destination belongs to the other mode, and posts the result to `ShellRouteRequest` or `ChatLaunchRequest.thread`; Login/SignupSetup, a switch timeout, a newer tap or a session end drop it.

## Tasks

1. Tracer: wait on Splash, deliver on ClientHome/ExpertHome, host mounted in `ClickMeApp`, `PushRoleSource` provider (commit 5563c9d). `:app:assembleDevDebug` and `AppViewModelTest` stayed green; the tracer `<verify>` was the same command, so it was re-verified end to end before expanding.
2. Mode switch then deliver: `switchRequest` flow, 5 s `SWITCH_TIMEOUT_MS`, request cleared in `finally`, host calls `onSwitchMode` once (commit dcdaa9e).
3. Drop on Login/SignupSetup, newest tap wins, cancel on session end, route re-check before posting (commit f863e3a).

## Verification

- `PushRoutingViewModelTest` 11 tests, `PushRoutingHostTest` 1 test, `PushDestinationResolverTest` and `HomeShellsTest` unchanged and green; `ktlintCheck` clean; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Privacy prohibition (no tap held across login or delivered into another session): `loginDropsPendingTap`, `signupSetupDropsPendingTap` and `logoutDuringResolutionDrops`.
- Acceptance greps: `PushRoutingHost(` in ClickMeApp = 1, `fun interface PushRoleSource` = 1, `fun pushRoleSource` = 1, `SWITCH_TIMEOUT_MS = 5_000L` = 1, `routingJob?.cancel()` = 2, `onSwitchMode(` = 1.

## Deviations from Plan

None - plan executed exactly as written. Minor: each task is one commit containing its tests (no separate RED commits), because the tests need the class under test to compile. The `join()` after cancel is an addition to keep the switch-request cleanup race-free.

## Open items for human/device

- Real-device cold-start check (killed app, tap push, lands in the right shell) is logged by 13-17; PUSH-04 stays flagged for the verifier as the plan states.
- UAT note (RESEARCH A3): a tap while on SignupSetup is dropped, not held.
- Known limitation: a tap that arrives while another resolution is mid-switch and the user manually switches mode in that window is dropped by the route re-check (rare race).

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

All four created files exist; commits 5563c9d, dcdaa9e, f863e3a are on the branch.
