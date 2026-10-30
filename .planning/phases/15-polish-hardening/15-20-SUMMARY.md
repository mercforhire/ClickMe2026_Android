---
phase: 15-polish-hardening
plan: 20
subsystem: settings
tags: [settings, focus, large-screen, restoration, tracer]
status: complete

requires: [15-02, 15-15]
provides:
  - "contentMaxWidth() on Delete Account (steps and pinned footer), both profile hubs (shared ProfileHubScaffold) and Mode Switch"
  - "Tests: Farewell headline focus, large-window caps on four screens, log-out confirm never auto-fires after a restore"
affects: []

tech-stack:
  added: []
  patterns:
    - "Cap inside the scroll container: .verticalScroll(...).contentMaxWidth().padding(...) keeps the scroll gesture area full width while the content column is centered at 600 dp (same chain as Account Security)"
    - "Pinned footers cap only their inner button column; the surface background and divider stay full width"

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/subviews/DeleteAccountComponents.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/subviews/ProfileHubComponents.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/modeswitching/ModeSwitchScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/ClientProfileHomeScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/modeswitching/ModeSwitchScreenTest.kt

key-decisions:
  - "Farewell headline focus already existed (Phase 14-05, DeleteAccountFarewell.kt: FocusRequester + focusable + LaunchedEffect). No production change; the plan's must-have is proven by a new test instead."
  - "Hub cap lives once in ProfileHubScaffold, which both hubs share, rather than being duplicated in each hub screen."

patterns-established: []

requirements-completed: [POL-02, POL-03, POL-04]

duration: ~1 h
completed: 2026-10-29
actuals:
  tokens: 6000
  tasks: 1
  commits: 1
plan_head_before: 8829c7c113c0b00eadebb0ad340d5bacebfce98b
plan_head_after: 14d3227e758e0daeb3707b53c98c87555d142aec
---

# Phase 15 Plan 20: Delete Account, hubs and Mode Switch large-window caps Summary

Delete Account (both steps and the pinned footer buttons), the Client and Expert profile hubs and Mode Switch now cap their content at 600 dp centered on large windows with app bars and footer decoration left full width, and tests prove the Farewell headline takes focus and a restored log-out confirm never logs out by itself.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (tracer, tdd) | Farewell focus proof, caps on Delete Account / hubs / Mode Switch, log-out restore proof | 14d3227e |

## What changed

- **DeleteAccountScreen:** `.contentMaxWidth()` on each step's scrolling column (inside the scroll, after `verticalScroll`). **DeleteAccountBottomBar:** the button column is capped; the surface background, hairline and IME/nav-bar padding stay full width.
- **ProfileHubScaffold** (shared by `ClientProfileHomeScreen` and `ExpertProfileHomeScreen`): `.contentMaxWidth()` on the scrolling column; the top app bar and snackbar host stay full width.
- **ModeSwitchScreen:** `.contentMaxWidth()` on the scrolling column.
- **Tests:** `DeleteAccountScreenTest.farewellHeadlineFocused` and `capped` (w900dp: comments field, confirm and keep buttons <= 600 dp and centered, Back arrow still at the leading edge); `ClientProfileHomeScreenTest.logOutNeverAutoConfirmsAfterRestore` (open dialog, `StateRestorationTester.emulateSavedInstanceStateRestore`, log-out count stays 0 until a tap, "Stay logged in" dismisses without logging out) and `hubCapped`; `ModeSwitchScreenTest.capped`.
- **Verification:** `:app:testDevDebugUnitTest` for DeleteAccountScreenTest, ClientProfileHomeScreenTest, ExpertProfileHomeScreenTest, ModeSwitchScreenTest, AccountDeletionFarewellTest plus `ktlintCheck`: BUILD SUCCESSFUL, no failures. `scripts/check-design-rules.sh`: "Design rules: OK".

## Deviations from Plan

**1. [Plan/code mismatch] Farewell is not in DeleteAccountScreen.kt.**
- **Found during:** Task 1 read.
- **Issue:** The plan asks for a `FocusRequester` in `DeleteAccountScreen.kt`, but the Farewell is hosted at the app root in `DeleteAccountFarewell.kt` (14-05), and already has `FocusRequester` + `focusable()` requested once in `LaunchedEffect(Unit)`. `DeleteAccountScreen.kt` has no Farewell headline.
- **Fix:** No production change for focus; added `farewellHeadlineFocused` (asserts the headline `isFocused` on entry) to lock the existing behavior. The acceptance criterion `grep -c FocusRequester DeleteAccountScreen.kt >= 1` therefore stays 0 on purpose; adding a dummy requester only to satisfy the grep would have been wrong.
- **Files modified:** DeleteAccountScreenTest.kt

**2. [Rule 3 - Blocking/structure] Hub cap placed in the shared scaffold.**
- **Issue:** Both hubs pass their content as a `ColumnScope` lambda into `ProfileHubScaffold`, which owns the scrolling column. The cap can only be applied there without wrapping/duplicating it twice.
- **Fix:** `contentMaxWidth()` in `ProfileHubComponents.kt`. Acceptance grep for `contentMaxWidth()` across the four named files therefore matches 2 (DeleteAccountScreen, ModeSwitchScreen) instead of 4; the hubs are covered through the scaffold (`hubCapped` test on the client hub; the expert hub shares the same scaffold).
- **Files modified:** ProfileHubComponents.kt

**3. [Rule 2 - Missing critical] Delete Account footer capped too** (`DeleteAccountComponents.kt`, not in the plan's file list): otherwise the pinned buttons would span a 900 dp window while the form is capped.

## Known Stubs

None.

## Open items (deferred device checks)

- Device/TalkBack: confirm the Farewell headline is the first announced element after deletion.
- Device: Delete Account, hubs and Mode Switch look right on a tablet/foldable-width window (not run; unit tests use Robolectric qualifiers).
- The Farewell's own "Back to Login" button is still `fillMaxWidth()` on a large window (`DeleteAccountFarewell.kt`, outside this plan's scope); candidate for a follow-up cap.

## Threat Flags

None. T-15-34 mitigated by `logOutNeverAutoConfirmsAfterRestore`.

## Self-Check: PASSED

- Modified files present; commit 14d3227e reachable from HEAD; new tests present in the Gradle XML results (`farewellHeadlineFocused`, `capped` x2, `hubCapped`, `logOutNeverAutoConfirmsAfterRestore`).
