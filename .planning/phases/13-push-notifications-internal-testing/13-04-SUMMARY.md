---
phase: 13-push-notifications-internal-testing
plan: 04
subsystem: navigation
tags: [navigation, nav3, shells, push-routing, hilt]

requires:
  - phase: 12-chat-report-block
    provides: ChatLaunchViewModel hand-off precedent, chatEntries, StackOrigin, Navigator.openChatThread
provides:
  - ShellRoute and the per-mode ShellRouteRequest slot (a SessionDataCleaner) with ShellRouteViewModel and PushRoutingModule
  - Navigator.openRouted(tab, keys) with cross-stack dedupe
  - pendingShellRoute / onConsumeShellRoute on HomeClientShell and HomeExpertShell
  - NotificationsKey(origin) and WriteReviewKey with placeholder routes and entries registered in the shells
affects: [13-07 push resolver, 13-12 notifications screen, 13-13 write review screen, 13-15, 13-18]

plan_head_before: 1c9c187606c03a3bdbc76c63f8635978b6b8849d
plan_head_after: 8ed1d05d12c010f2202b38dc33464fcf26c1065f

actuals:
  tokens: 12400
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Per-mode one-slot hand-off: consumeFor(mode) uses getAndUpdate so the outgoing shell never takes the other mode's route"
    - "Navigator.openRouted: dedupe the destination across the start stack and the target stack before inserting"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/ShellRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/ShellRouteViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PushRoutingModule.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsEntries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewEntries.kt
    - app/src/main/res/values/strings_notifications.xml
    - app/src/main/res/values/strings_write_review.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/NavigatorOpenRoutedTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/ShellRouteRequestTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt

key-decisions:
  - "openRouted drops an earlier chain key that already sits in the other stack in use (it was only a Back stop), and keeps the last key"
  - "The shell LaunchedEffect also skips a route with an empty key chain, so a bad post cannot crash the shell through openRouted's require"
  - "Notifications and Write Review routes ship as app-bar-only placeholders; 13-12 and 13-13 replace the body and keep the signatures"

patterns-established:
  - "A route that a later plan fills is registered now with a placeholder body, so the shells are edited once (as 12-03 did)"

requirements-completed: [PUSH-04, PUSH-05, REV-01]

coverage:
  - id: D1
    description: "A ShellRoute posted to ShellRouteRequest is consumed only by the shell of its own mode and left pending for the other"
    requirement: PUSH-04
    verification:
      - kind: unit
        ref: "ShellRouteRequestTest#consumeForOtherModeLeavesPending"
        status: pass
      - kind: integration
        ref: "HomeShellsTest#expertRouteIsLeftForTheExpertShell"
        status: pass
    human_judgment: false
  - id: D2
    description: "Navigator.openRouted validates first, dedupes the destination across the start and target stacks, cuts repeated chain keys and keeps the chain's Back order"
    requirement: PUSH-04
    verification:
      - kind: unit
        ref: "NavigatorOpenRoutedTest (11 tests)"
        status: pass
      - kind: integration
        ref: "HomeShellsTest#expertRequestChainBacksToListThenDashboard"
        status: pass
    human_judgment: false
  - id: D3
    description: "A client route lands on Booking Summary on the Bookings tab, and an expert route on its own shell"
    requirement: PUSH-04
    verification:
      - kind: integration
        ref: "HomeShellsTest#postedClientRouteOpensSummaryOnBookingsTab, #expertShellConsumesItsRoute"
        status: pass
    human_judgment: false
  - id: D4
    description: "NotificationsKey (both shells) and WriteReviewKey (client shell only) are registered with the bottom bar hidden"
    requirement: PUSH-05
    verification:
      - kind: integration
        ref: "HomeShellsTest#notificationsRouteOnExploreHidesBottomBar, #writeReviewRouteHidesBottomBarOnTheBookingsStack, #notificationsKeysDifferPerStack"
        status: pass
    human_judgment: false
  - id: D5
    description: "ShellRouteRequest is a SessionDataCleaner bound into the cleaner set, so logout drops an unconsumed route"
    verification:
      - kind: unit
        ref: "ShellRouteRequestTest#clearSessionDataDrops"
        status: pass
    human_judgment: true
    rationale: "The Hilt multibinding in PushRoutingModule is compiled by assembleDevDebug but no test asserts that UserManager runs this cleaner on logout"

duration: 45min
completed: 2026-10-28
status: complete
---

# Phase 13 Plan 04: Push routing into the shells Summary

**Per-mode ShellRoute hand-off, a single Navigator.openRouted with cross-stack dedupe, and Notifications and Write Review routes registered in both Nav3 shells**

## Performance

- **Duration:** about 45 min (wall clock, including Gradle waits while other wave-1 executors shared the machine)
- **Completed:** 2026-10-28T15:53Z
- **Tasks:** 3
- **Files modified:** 17 (13 created, 4 modified)

## Accomplishments

- `ShellRouteRequest.consumeFor(mode)` takes the pending route atomically only when the mode matches, so the outgoing shell during the Client/Expert cross-fade cannot swallow it (RESEARCH F-7, Pitfall 3).
- `Navigator.openRouted(tab, keys)` validates before mutating, then: dedupes the destination across the start and target stacks (Pitfall 2), pops and switches for another tab, or cuts a repeated chain key and pushes on the selected tab. The expert request chain `[IncomingRequestsKey, RequestDecisionKey]` Backs to the list, then the Dashboard root.
- Both shells take `pendingShellRoute` / `onConsumeShellRoute`; the Hilt wrappers feed them through `ShellRouteViewModel`.
- `NotificationsKey(origin)` is registered in both shells and `WriteReviewKey` in the client shell only, both with the bottom bar hidden, so 13-12, 13-13, 13-15 and 13-18 never edit the shells for these routes.

## Task Commits

1. **Task 1: Hand a resolved push route to the shell of its own mode (tracer)** - `c503e3f` (feat)
2. **Task 2: openRouted dedupe across the stacks in use** - `d78354b` (feat)
3. **Task 3: Notifications and Write Review keys, routes and entries** - `8ed1d05` (feat)

**Plan metadata:** the docs commit that adds this file.

## Files Created/Modified

See `key-files` above. Tests: `NavigatorOpenRoutedTest` (11), `ShellRouteRequestTest` (4), `HomeShellsTest` (51 before, 55 after: 3 from Task 1, 4 from Task 3, all green).

## Decisions Made

- `openRouted` drops an earlier chain key that is already in the other stack in use and keeps the last key. Without this a chain whose intermediate key sits in the start stack would put an equal key in two stacks in use.
- The shell effect additionally skips a route with an empty key chain (plan only checked the tab), so a malformed post is ignored instead of throwing from `openRouted`.
- Notifications and Write Review ship as app-bar-only placeholder routes; signatures are fixed for the later plans.

## TDD Notes

The plan is `type: execute`, so no plan-level gate applies. Task 1 (tracer): the three new `HomeShellsTest` tests were written first against symbols that did not exist (a compile-level RED; not committed separately because it would break the build in history), then implemented to green. Task 2: RED run recorded before the dedupe code, with 5 of 15 target tests failing on their assertions (`intermediateKeyInStartStackIsDropped`, `noKeyIsEverInTwoStacksInUse`, `destinationInStartStackIsReused`, `destinationDeeperInTargetIsTruncated`, `repeatedChainKeyIsCutFirst`); after the implementation all 15 pass. The RED and GREEN for Task 2 share one commit by design (one atomic commit per task).

Tracer gate: the tracer's `<verify>` is automated-only and `human_verify_mode` is `end-of-phase`, so it was re-run end to end (HomeShellsTest 51 tests plus ktlintCheck, green) and expansion continued.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Import order rejected by ktlint in both Hilt-aware home screens**
- **Found during:** Task 1 verify (`ktlintMainSourceSetCheck` failed)
- **Issue:** the `screens.push.*` imports were placed after `screens.settings.*`
- **Fix:** moved them before `screens.settings`
- **Files modified:** HomeClientScreen.kt, HomeExpertScreen.kt
- **Committed in:** `c503e3f`

**2. [Rule 2 - Missing critical] Empty key chain guard in the shell effect**
- **Found during:** Task 1
- **Issue:** `openRouted` requires a non-empty chain; an empty posted chain would crash the shell from a `LaunchedEffect`
- **Fix:** the effect skips a route whose `keys` is empty (it is still consumed)
- **Committed in:** `c503e3f`

---

**Total deviations:** 2 auto-fixed (1 bug, 1 missing critical). **Impact on plan:** none on scope.

## Issues Encountered

- Two background Gradle runs wrote to the same log file, so one log was misleading; re-run with a unique log per run confirmed `BUILD SUCCESSFUL`.
- Tool restrictions on compound git/gradle commands in the worktree required splitting commands; no effect on the result.

## Known Stubs

| File | Line | Reason |
|------|------|--------|
| `screens/notifications/NotificationsRoute.kt` | body | App bar over an empty `Box`; 13-12 fills the list body |
| `screens/clientmode/writereview/WriteReviewRoute.kt` | body | App bar over an empty `Box`; 13-13 fills the review form |

Both are intentional and named in the plan; no entry point reaches them until the bell (13-12) and the review callbacks (13-15, 13-18) land.

## Threat Flags

None. T-13-10 (duplicate keys across stacks) is mitigated by `openRouted` Rule 1 and Rule 3 with the `destinationInStartStackIsReused` and `repeatedChainKeyIsCutFirst` tests; T-13-11 (route surviving logout) by `ShellRouteRequest` being a `SessionDataCleaner` with `clearSessionDataDrops`.

## Open Items (human)

- None required for this plan. Optional on a device once 13-07 and 13-12 land: confirm a mode-switching push opens the destination in the freshly composed shell and that logout clears a pending route.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 13-07 can post `ShellRoute(mode, tab, keys)` to `ShellRouteRequest` and add its lookup bindings to `PushRoutingModule`.
- 13-12 and 13-13 replace the bodies of `NotificationsRoute` and `WriteReviewRoute` without touching the shells.
- Verification run: `:app:testDevDebugUnitTest` for `HomeShellsTest`, `NavigatorOpenRoutedTest`, `ShellRouteRequestTest`, `NavigatorTest`, `NavigatorOpenChatThreadTest`, plus `ktlintCheck` and `:app:assembleDevDebug` all green; `scripts/check-design-rules.sh` prints "Design rules: OK".

## Self-Check: PASSED

- All created files exist on disk; `c503e3f`, `d78354b`, `8ed1d05` are ancestors of HEAD.
- Acceptance greps: `fun openRouted` 1, `onConsumeShellRoute` 4 in each shell file, `bindShellRouteCleaner` 1, `inUseAfter` 3, `fun consumeFor` 1, `notificationsEntries(navigator, AppMode.Client)` 1, `...Expert)` 1, `writeReviewEntries(navigator)` 1 in the client shell and 0 in the expert shell, `notifications_title` 1.

---
*Phase: 13-push-notifications-internal-testing*
*Completed: 2026-10-28*
