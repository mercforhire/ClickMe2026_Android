---
phase: 09-voice-call
verified: 2026-10-24T18:00:00Z
status: human_needed
score: 5/5 roadmap success criteria verified in code (device behavior pending); 8/8 requirements implemented
overrides_applied: 0
re_verification: false
gaps: []
warnings:
  - id: W-1
    ref: "WR-09 (09-REVIEW.md); CALL-08 / SC-4 'rejoin within the window'"
    severity: warning
    summary: "Join/Rejoin is disabled from start+60 min for any booking longer than 60 min, although the backend accepts /join later and CALL-08 says 'rejoin for the whole window'."
    decision_needed: "Accept the inherited iOS rule (D-02) or widen the window to max(start+3600, end)."
  - id: W-2
    ref: "WR-02"
    severity: warning
    summary: "End tapped while still Connecting (no channel joined) sends a fabricated /end with a 1 s duration and manual_exit. Contradicts D-04 intent; the peer sees 'waiting for peer'."
  - id: W-3
    ref: "WR-03"
    severity: warning
    summary: "/end can overtake the in-flight confirm-connection PATCH. Backend returns 409 for 'completed' on a not-yet-in_progress booking, so only that leg's metadata is lost. Confirm is still sent exactly once, so CALL-05 holds."
  - id: W-4
    ref: "WR-01, WR-04, WR-05, WR-06, WR-07, WR-08, WR-10"
    severity: warning
    summary: "Latent races, stopSelf misuse, notification tap stacking MainActivity over a Custom Tab, rejected meeting link still confirming and minimizing, provider spoofing, uncaught non-ApiException in the start coroutine. None is reachable on the happy path; none breaks a success criterion."
deferred: []
human_verification:
  - test: "SC-1: Join from Upcoming Details (opened from Explore and from My Bookings), the My Bookings card and Today's Sessions, against a live iOS expert. First mic prompt, deny once (rationale), deny permanently (Open settings). A Skype/Zoom booking opens the native app or a Custom Tab and minimizes."
    expected: "Two-way audio with the iOS expert; both mic dialogs appear; the external link opens and the call minimizes to the pill."
    why_human: "Needs a real device, a live Agora channel, an iOS counterpart and OS permission UI."
  - test: "SC-2: 5+ minutes backgrounded with the screen off. Check the notification (title, Starts at / chronometer), notification tap after a task swipe, and the notification End call action. Capture `adb logcat -s ClickMeCall` and `adb shell dumpsys audio`."
    expected: "Audio continues both ways; notification persists; tap expands the call; End call leaves, sends /end and tears down."
    why_human: "Foreground-service and audio-focus survival is OEM/OS behavior (Samsung SM-A075F); it cannot be checked by grep or JVM tests."
  - test: "Android 17 check on the Medium_Phone_API_37.0 emulator with `adb shell cmd audio set-enable-hardening enable`; confirm the targetSdk 37 local-network permission does not block Agora."
    expected: "Background audio and focus still work; Agora joins."
    why_human: "Platform-hardening behavior that exists only on the target OS image."
  - test: "SC-3: Pill across every tab; Client/Expert switch attempt, logout attempt, add-payment-method attempt and Cancel of the live booking each show the guard dialog; predictive back minimizes the call."
    expected: "Pill stays visible; guard text 'End the call to ...'; Back minimizes and never ends the call."
    why_human: "Visual overlay layering and gesture feel."
  - test: "SC-4: iOS expert hangs up; then rejoin; then the final-5-minute amber state and banner; and the 3600 s past scheduled-end cutoff (or rely on the unit test)."
    expected: "'Waiting for other person' without ending; rejoin restores Connected; timer and chip turn amber at 5:00 left."
    why_human: "Needs a real peer and wall-clock timing. Also decide W-1 (>60 min bookings)."
  - test: "SC-5: Full paid session end to end with confirm-connection sent exactly once (backend logs). Paid part is blocked on the test-mode Stripe backend; a free booking covers the rest."
    expected: "One confirm-connection PATCH per call in the backend logs; session completes."
    why_human: "Needs a Stripe test-mode backend and an iOS expert."
  - test: "200% font scale on the Connected pair, banner and external variant; TalkBack order; POST_NOTIFICATIONS denied on API 33+; Bluetooth headset routing; incoming GSM call; Samsung battery/sleeping-apps; cold-start notification tap after a task swipe; pill position over gesture vs 3-button nav."
    expected: "Call still runs and the UI stays usable in each case."
    why_human: "Device, accessibility and OEM behavior. Logged in STATE.md as '[Phase 9] DEFERRED (human)'."
  - test: "Product decision: ongoing-call notification is a plain notification, swipe-dismissible on Android 14+. Choose whether to switch to NotificationCompat.CallStyle.forOngoingCall (one-file change in CallNotificationFactory)."
    expected: "User decision recorded before release."
    why_human: "Product choice, logged as a Phase 9 open item."
---

# Phase 9: Voice Call Verification Report

**Phase Goal:** An Android user can join and hold a full voice session that survives backgrounding and navigation, which completes the client side.
**Verified:** 2026-10-24
**Status:** human_needed
**Re-verification:** No, initial verification

All code-checkable truths hold. What remains is device behavior, which the user's standing rule defers. One design warning (W-1) needs a human decision.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Join from Upcoming Details (incl. from My Bookings) reaches an iOS expert; external join URL opens in a Custom Tab; mic denial shows rationale and recovery dialogs | VERIFIED in code, device pending | `UpcomingBookingViewModel.join()` -> `callStarter.startCall(CallStartRequest(...))`. Screen passes `onJoin = viewModel::join` and `joinEnabled = isWithinJoinWindow(...)`. The same `CallStarter` is wired in `MyBookingsViewModel` and `ExploreViewModel`. No `PHASE-9` markers remain in `app/src`. `CallCenter.runStart` calls `api.joinCall` and branches on `connection_type`. In-app path: `permissionGate.ensureMicrophone()`, foreground service, `engine.join`. External path: `onExternalLinkOpened`, with `openExternalMeeting` trying `ACTION_VIEW` then a Custom Tab. `MicPermissionRequester` shows `MicRationaleDialog` after one denial. `CallDialog.MicPermissionDenied` leads to `MicDeniedDialog`, which opens `ACTION_APPLICATION_DETAILS_SETTINGS` (CallDialogs.kt:64). |
| 2 | Call continues with a persistent notification in background / screen off | VERIFIED in code, device pending | Manifest: `<service .CallService foregroundServiceType="microphone" stopWithTask="false">`, plus RECORD_AUDIO, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS, MODIFY_AUDIO_SETTINGS. `CallService.start` calls `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)` before the engine joins, requests `USAGE_VOICE_COMMUNICATION` audio focus, and posts an ongoing notification with chronometer, tap-to-expand and an End action. `CallCenter` waits on `foreground.startAndAwait` before `engine.join`, and refuses to start if the process is not foreground. Realtime socket is kept up during a call. Agora SDK 4.6.4 is a real dependency. Real background survival is not provable without a device. |
| 3 | Call UI minimizes to a pill visible across tabs and mode switches; restricted actions show the guard | VERIFIED | `ClickMeApp` wraps the whole `AnimatedContent` route switch in `CallOverlayHost`. The pill is an `AnimatedVisibility` at BottomEnd (`MeetingCallPill`), and the expanded layer has a `PredictiveBackHandler` that minimizes. `CallCenter` is a Hilt `@Singleton`, so the call outlives the UI. Guard call sites: `AppViewModel` (SwitchModes, LogOut), `HomeClientViewModel`/`HomeExpertViewModel`/`SignupOverviewViewModel` (LogOut), `PaymentMethodsViewModel` (AddPaymentMethod), `ClientCancellationViewModel` and `UpcomingBookingViewModel.attemptCancel` (`attemptModifyBooking`). The Cancel button on the details screen is gated by `attemptCancel()`. |
| 4 | Peer hang-up shows "waiting for peer" and does not end the call; rejoin works within the window; timer counts from scheduled start, amber in the last 5 min, cutoff at 3600 s | VERIFIED, with W-1 | `onPeerCallEnded` filters by booking id and sets remote = Disconnected, which `deriveCallStatus` maps to `WaitingForPeer`. It never calls `endCall`. Agora `onUserOffline` is the primary signal. `callTimerState` uses `scheduledStart` as anchor; `isInFinalStretch` is 1..300 s remaining; `isWarning` colors the timer with the tertiary (amber) color, and the pill does the same. `overtimeCutoff = scheduledEnd + 3600 s`, and `startCutoff` polls the wall clock and force-ends once. `CallCenterCutoffTest` checks that 5 minutes past the end is not the cutoff. Hang-up always sends `/end` and never branches on the response, so the booking stays open for rejoin. Rejoin is allowed in the UI only while `isWithinJoinWindow` holds, which is the W-1 limit. |
| 5 | Android client + iOS expert finish a full paid session end to end; confirm-connection sent exactly once | VERIFIED in code, end to end pending | `confirmConnectionOnce()` is guarded by `session.confirmSent`, which is set before the request and kept across state updates. The test `activeFlap_doesNotConfirmTwice` covers flapping. The request body matches the backend `.strict()` schema (`connection_status`, `timestamp`, `method`). The end-to-end session and the paid part are deferred to a device and a test-mode Stripe backend. |

**Score:** 5/5 code-verifiable. Every item that needs a device is in `human_verification`.

### Requirements Coverage

Every CALL ID appears in at least one PLAN `requirements:` frontmatter (09-01..09-17). 09-17 claims all eight. REQUIREMENTS.md maps all eight to Phase 9, and there are no orphaned IDs.

| Req | Plans | Status | Evidence |
|-----|-------|--------|----------|
| CALL-01 | 01, 03, 05, 09, 14, 15, 17 | SATISFIED | `ClickMeApi.joinCall` (`POST bookings/{id}/join`); `startInApp` / `startExternal`; Join wired on Upcoming Details, My Bookings and Explore; `ExternalLinkLauncher` is app-first with a Custom Tab fallback. |
| CALL-02 | 04, 05, 10, 13, 17 | SATISFIED | `CallPermissionBroker`, `MicPermissionRequester`, rationale dialog and denied-forever dialog with an Open settings intent; POST_NOTIFICATIONS requested on API 33+ without blocking the call. |
| CALL-03 | 04, 05, 06, 07, 11, 13, 17 | SATISFIED | `CallService` (microphone type), `CallForegroundBridge`, `CallNotificationFactory`, manifest. `CallManifestTest` passes. |
| CALL-04 | 04, 05, 08, 10, 12, 13, 14, 16, 17 | SATISFIED | Root `CallOverlayHost`, `MeetingCallPill`, `CallGuard` and its call sites. |
| CALL-05 | 01, 04, 05, 17 | SATISFIED | `confirmConnectionOnce`; `CallCenterConfirmTest`. |
| CALL-06 | 04, 05, 06, 11, 17 | SATISFIED | `onPeerCallEnded`, `RealtimeEvent.CallEnded`, `CallCenterPeerTest`. |
| CALL-07 | 02, 08, 09, 11, 12, 17 | SATISFIED | `CallTimer.kt` (anchor, 300 s final stretch, `OVERTIME_CUTOFF_SECONDS = 3600`), `startCutoff`. |
| CALL-08 | 01, 09, 11, 12, 14, 17 | SATISFIED with W-1 | `performEnd`: leave -> `/end` -> destroy -> stop. `CallCenterEndTest.rejoin_afterEnd_confirmsAgain` passes. |

### Required Artifacts

All files below exist, are substantive (no stubs) and are wired.

| Artifact | Status |
|----------|--------|
| `managers/CallCenter.kt` (530 lines), `CallSession.kt`, `CallTimer.kt`, `JoinWindow.kt`, `CallGuard.kt`, `CallDialog.kt`, `CallPermissionGate.kt` | VERIFIED |
| `managers/AgoraManager.kt` and `CallEngine.kt`, bound in `di/CallModule.kt` | VERIFIED |
| `services/call/CallService.kt`, `CallForegroundBridge.kt`, `CallNotificationFactory.kt`, AndroidManifest service entry | VERIFIED |
| `screens/call/calling/*` (overlay host, screen, view model, pill, controls, dialogs, external subviews), mounted in `ClickMeApp.kt` | VERIFIED |
| `components/web/ExternalLinkLauncher.kt`, `components/call/JoinCallButton.kt` | VERIFIED |
| `MainActivity` `EXTRA_EXPAND_CALL` handling in `onCreate` and `onNewIntent` | VERIFIED |
| `:spikes:agora` removed from `settings.gradle.kts` and the repo (only `spikes/stripe` remains) | VERIFIED |

### Key Link Verification

| From | To | Status |
|------|----|--------|
| Join sites (3) | `CallStarter` -> `CallCenter.startCall` | WIRED (Hilt `@Binds` in `CallModule`) |
| `CallCenter` | `ClickMeApi` join / confirm / end | WIRED; `apiCall` result drives the flow |
| `CallCenter` | `CallForegroundBridge` -> `CallService` | WIRED; `startAndAwait` suspends on `onServiceStarted` |
| `CallService` End action | `bridge.requestHangUp` -> `CallCenter.endCall` | WIRED |
| `CallCenter` | `AgoraManager` events | WIRED |
| `RealtimeConnection` `call.ended` | `CallCenter.onPeerCallEnded` | WIRED; decoder registered |
| `CallCenter.session` | `MeetingCallViewModel` -> overlay, pill | WIRED (1 s UI ticker, real data) |
| Notification tap | `MainActivity` -> `callCenter.expand()` | WIRED |
| `CallCenter` | `SessionDataCleaner` set (forced logout) | WIRED |

### Data-Flow Trace (Level 4)

The overlay renders `CallSession` (from the live engine and server responses) projected by `toMeetingCallUiState`. No hardcoded empty props. The Join buttons receive `joinEnabled` computed from real booking times and the `JoinClock` flow. FLOWING.

### Behavioral Spot-Checks

| Behavior | Command | Result |
|----------|---------|--------|
| Call state machine, confirm-once, end flow, cutoff, peer, permission, guard, timer, join window, manifest, view model | `:app:testDevDebugUnitTest` filtered to `*CallCenter*`, `CallTimerTest`, `JoinWindowTest`, `CallManifestTest`, `MeetingCallViewModelTest`, `CallSessionTest` | BUILD SUCCESSFUL; 80 tests, 0 failures, 0 skipped (the full suite of 2206 tests per flavor was reported green by 09-17) |

### Probe Execution

No phase-declared probes. Step 7c: SKIPPED.

### Anti-Patterns Found

| Scope | Pattern | Severity |
|-------|---------|----------|
| Phase 9 call files | `TBD`, `FIXME`, `XXX`, `TODO`, `HACK`, `PLACEHOLDER` grep: no matches | None |
| `CallOverlayHost.kt:69` | `rememberExternalMeetingLauncher(onFailure = {})` swallows launch failures, and `onExternalLinkOpened()` runs regardless (WR-07) | Warning |
| `CallCenter.kt:383-402` | Fabricated 1 s `/end` when no leg started (WR-02) | Warning |
| `JoinWindow.kt:36-46` | Window closes at start+60 min (WR-09) | Warning |

The 09-REVIEW.md reports 0 blockers and 10 warnings. I read the code behind WR-01..WR-10 and the findings match it. None makes a success criterion fail.

### Human Verification Required

See the `human_verification` frontmatter. Everything needs a device, and it is already logged in STATE.md as "[Phase 9] DEFERRED (human)". The paid end-to-end session is also blocked on a test-mode Stripe backend.

### Gaps Summary

There are no blocking gaps. Each success criterion has real, wired, tested code behind it.

Decision requested on W-1: `isWithinJoinWindow` (D-02, ported from iOS) disables Join and Rejoin from start+60 min. Backend durations are multiples of 30 min with no maximum seen, so a 90-minute or 120-minute booking cannot be rejoined in its second half. CALL-08 says "rejoin for the whole window". Either accept the rule (and log an iOS parity note), or widen it to `max(start + 3600 s, end)`. If you accept it, add an override:

```yaml
overrides:
  - must_have: "user can rejoin within the window (CALL-08 / SC-4)"
    reason: "D-02 locks the iOS join window of +-3600 s around start; sessions over 60 min lose Rejoin in the second half"
    accepted_by: "{name}"
    accepted_at: "{ISO timestamp}"
```

W-2 and W-3 (the `/end` hang-up edge cases) and W-4 are advisory. They are worth a small follow-up plan (`/gsd-plan-phase --gaps`) but do not block this phase.

---

_Verified: 2026-10-24_
_Verifier: Claude (gsd-verifier)_
