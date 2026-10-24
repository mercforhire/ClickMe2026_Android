---
phase: 09-voice-call
plan: 10
subsystem: call
tags: [compose, dialogs, permissions, accessibility]
requires:
  - phase: 09-02
    provides: strings_call.xml copy contract
  - phase: 09-04
    provides: CallDialog, MicPermissionResult, PendingMicPermission
provides:
  - CallDialogHost, MicRationaleDialog, joinErrorMessage, guardMessageText, restrictedActionText, appDetailsSettingsIntent
  - MicPreRequestStep, micPreRequestStep, micResultAfterRequest, permissionsToRequest, MicPermissionRequester
affects: [09-13]
tech-stack:
  added: []
  patterns: [pure decision table plus thin launcher composable, fake ActivityResultRegistry in Compose tests]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogs.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MicPermissionRequester.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MicPermissionLogicTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MicPermissionRequesterTest.kt
  modified: []
key-decisions:
  - "Join error, guard and time-limit dialogs reuse AuthErrorDialog; only the two-button mic dialogs are new, sharing one private AlertDialog"
  - "When the mic is already granted but POST_NOTIFICATIONS is not (API 33+), a separate result-ignoring RequestPermission launcher asks for notifications after reporting Granted"
requirements-completed: [CALL-02, CALL-04]
duration: ~50 min
completed: 2026-10-24
---

# Phase 9 Plan 10: Call dialogs and mic permission requester Summary

Typed `CallDialog` values render with the approved copy via `CallDialogHost`, and a pure mic decision table (granted / rationale / launch, then granted / denied once / denied permanently) drives a `MicPermissionRequester` composable that fulfils the broker's pending request.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Root call dialogs (join error, mic denied, guard, time limit, rationale) | 942b984 |
| 2 | Mic permission decision table and launcher-hosting requester | f49dd66 |

## Verification

- `CallDialogsTest` 16 tests, `MicPermissionLogicTest` 9 tests, `MicPermissionRequesterTest` 10 tests: all pass; `ktlintCheck` clean.
- `scripts/check-design-rules.sh`: OK.
- Acceptance greps: `AuthErrorDialog(` count 3, `ACTION_APPLICATION_DETAILS_SETTINGS` count 1, `RequestMultiplePermissions` 1, `rememberSaveable` 3.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Launcher callback uses a labelled return**
- **Found during:** Task 2
- **Issue:** The plan's bare `return` does not compile in the launcher lambda (also flagged in the executor notes).
- **Fix:** `return@rememberLauncherForActivityResult`.
- **Files modified:** `MicPermissionRequester.kt`
- **Commit:** f49dd66

**2. [Rule 1 - Bug] Test-side ktlint/compile slips in first drafts**
- Import ordering, chained-call wrapping and a wrong `assertDoesNotExist` import (it is a member of `SemanticsNodeInteraction`) were fixed before commit.

### Notes

- The worktree HEAD was behind the stated base (`d1217fb` vs `c22e86e`); reset to the base per the startup check before any work.
- The tests run on the default Robolectric SDK 35, so the API 32 path is covered only by the pure `permissionsToRequest(32, ...)` test, not by a Compose test.
- Notifications-only launch on the already-granted path is implemented per the plan text but not asserted in a Compose test (the fake registry only records it).

## Known Stubs

None.

## Threat Flags

None. T-09-33: the Settings intent is the fixed app-details action with our own package only. T-09-34: only `RECORD_AUDIO` decides the result. T-09-35: server text rendered as plain `Text`.

## Self-Check: PASSED

All five created files exist; commits 942b984 and f49dd66 present.
