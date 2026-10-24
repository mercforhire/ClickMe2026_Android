---
phase: 09-voice-call
plan: 13
subsystem: call
tags: [compose, overlay, back-handling, hilt-viewmodel, notification-intent]
requires:
  - phase: 09-voice-call
    provides: "CallCenter and CallModule (09-05, 09-11), CallNotificationFactory.EXTRA_EXPAND_CALL (09-07), MeetingCallPill (09-08), CallDialogHost and MicPermissionRequester (09-10), MeetingCallScreen and MeetingCallUiState (09-12), rememberExternalMeetingLauncher (09-03)"
provides:
  - MeetingCallViewModel (logic-free adapter over CallCenter with a 1 s display ticker), UI_TICK_MS
  - CallOverlayHost (Hilt) and CallOverlayContent (stateless) with CALL_OVERLAY_LAYER_TAG, CALL_OVERLAY_CONTENT_TAG, PILL_BOTTOM_OFFSET
  - ClickMeApp route switch wrapped in CallOverlayHost
  - MainActivity onNewIntent plus internal Intent?.consumeExpandCallExtra()
affects: [09-14..09-17 (Join and guard sites now have a live overlay to hand off to)]
tech-stack:
  added: []
  patterns:
    - "Back handler composed only while the layer is expanded, so it registers after content handlers"
    - "Overlay as a sibling above the route switch; content hidden with clearAndSetSemantics while expanded"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHost.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHostTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/MainActivityIntentTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/MainActivity.kt
key-decisions:
  - "PredictiveBackHandler (activity-compose 1.13.0) is available and used while expanded; the layer follows the gesture by 20% of its height, and the plain BackHandler fallback was not needed"
  - "Pill and layer keep the last non-null MeetingCallUiState in a remembered holder so their exit animations can finish after the session clears"
requirements-completed: [CALL-02, CALL-03, CALL-04]
duration: ~45 min (mostly Gradle wall time under parallel load)
completed: 2026-10-24
---

# Phase 9 Plan 13: Root call overlay and notification tap Summary

The call now lives at the root of the app: a logic-free `MeetingCallViewModel` projects `CallCenter` at a 1 s tick, `CallOverlayHost` draws the expanded layer or the pill above the whole route switch (surviving tabs and the Client/Expert swap), Back only minimizes, and tapping the ongoing-call notification expands the call cold or warm.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | MeetingCallViewModel (view-state adapter with a 1 s UI ticker) | 5baad5d |
| 2 | CallOverlayHost (layer, pill, back priority, semantics, dialogs, mic requester, external open) | 8a624b3 |
| 3 | Mount in ClickMeApp and handle the notification tap in MainActivity | 8baf821 |

## Verification

- `:app:testDevDebugUnitTest --tests '*MainActivityIntentTest' --tests '*CallOverlayHostTest' --tests '*MeetingCallViewModelTest' :app:assembleDevDebug ktlintCheck`: BUILD SUCCESSFUL (9 + 10 + 4 tests, 0 failures); Hilt resolves `MeetingCallViewModel` and the `MainActivity` field.
- `bash scripts/check-design-rules.sh`: OK.
- Acceptance greps: `UI_TICK_MS = 1_000L` 1; `overtimeCutoff|api.` in the VM 0; `clearAndSetSemantics` 2; `PILL_BOTTOM_OFFSET = 96.dp` 1; `if (expanded)` next to the back handler 1; `CallOverlayHost` in ClickMeApp 3; `override fun onNewIntent` 1.
- Back-priority (Pitfall 2) is covered by a Robolectric test: the content composes its own `BackHandler` first, the call expands afterwards, `onBackPressed()` minimizes once with the content handler untouched and no end call. A minimized case passes Back to the content handler.

## Behavior notes

- Expanded layer: `AnimatedVisibility` slide + fade (300 ms push/pop easing), pointer-swallowing box, content below gets `clearAndSetSemantics { }`. Dialogs and the mic requester are composed last, so dialog windows sit above everything (the pill cannot sit above an open dialog or sheet, accepted in RESEARCH Pattern 7).
- Pill: bottom end, `navigationBars` inset + 96 dp, 16 dp end padding, 200 ms scale (0.6) + fade; tap expands.
- External Open: `rememberExternalMeetingLauncher(onFailure = {})` (app first, Custom Tab fallback), then `onExternalLinkOpened()` (confirm once + minimize). A launch that fails (non-http scheme, no app installed) is silent; logged as a UAT observation.
- `MainActivity` reads only the boolean extra (T-09-43) and removes it so recreation does not re-expand. With no live session `expand()` is a no-op, so the tap just opens the app (process death, D-15).

## Deviations from Plan

### Process notes

- Worktree HEAD (`d1217fb`) was an ancestor of the required base; ran the sanctioned `git reset --hard 3150a62` at startup (clean tree, nothing lost).
- All three tasks are `tdd="true"`; tests and implementation were committed together per task, not as separate RED/GREEN commits (same as 09-09 and 09-12).

### Auto-fixed Issues

**1. [Rule 1 - Bug] Test expectation for the speaker default**
- **Found during:** Task 1
- **Issue:** First draft of `toggles_delegate...` asserted the speaker turns on after one toggle, but `CallSession.isSpeakerOn` defaults to true.
- **Fix:** Assert the toggle turns it off.
- **Files modified:** `MeetingCallViewModelTest.kt`
- **Commit:** 5baad5d

No changes were needed outside the plan's `files_modified`.

## Open items (not blocking)

- Not verified on a device: predictive-back animation feel, real notification tap on cold start, pill position over a gesture-nav versus 3-button device (manual testing deferred).
- Cold start after a task swipe shows Splash / auth first and the layer appears over it once the route resolves (D-14); only exercised through the pure intent test.

## Known Stubs

None.

## Threat Flags

None. T-09-43 accepted (extra only expands a live call; no data read from the intent). T-09-44 mitigated (Back only minimizes, tested). T-09-45 mitigated (`clearAndSetSemantics` plus the pointer-swallowing layer, tested). T-09-46 mitigated (launch only through `rememberExternalMeetingLauncher`).

## Self-Check: PASSED

- Created files exist: MeetingCallViewModel.kt, CallOverlayHost.kt, MeetingCallViewModelTest.kt, CallOverlayHostTest.kt, MainActivityIntentTest.kt.
- Commits 5baad5d, 8a624b3, 8baf821 exist on the branch.
