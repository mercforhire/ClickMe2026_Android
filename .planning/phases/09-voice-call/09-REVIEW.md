---
phase: 09-voice-call
reviewed: 2026-10-24T17:28:40Z
depth: standard
files_reviewed: 74
files_reviewed_list:
  - .github/workflows/ci.yml
  - app/build.gradle.kts
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/MainActivity.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/call/JoinCallButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/web/CustomTabLauncher.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/web/ExternalLinkLauncher.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/CallModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEventMapping.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallEngine.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallForegroundController.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallGuard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallPermissionGate.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallSession.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallTimer.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/EncryptedPrefsTokenStore.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/ConnectionType.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/EndReason.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/CallRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ConfirmConnectionData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/EndCallData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/JoinCallResponse.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/ProcessForeground.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogs.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHost.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallTimerLabels.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/ExternalMeetingProvider.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MicPermissionRequester.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/EndingSoonBanner.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/ExternalLinkActions.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/ExternalSessionHero.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallAvatar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallBackground.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallControls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallEndButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallInfo.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallPill.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/StartingNowBadge.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/UpcomingBookingCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingJoinCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallForegroundBridge.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallNotificationFactory.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallService.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Motion.kt
  - app/src/main/res/drawable/ic_stat_call.xml
  - app/src/main/res/values/strings_call.xml
  - settings.gradle.kts
findings:
  critical: 0
  warning: 10
  info: 6
  total: 16
status: issues_found
---

# Phase 9: Code Review Report

**Reviewed:** 2026-10-24T17:28:40Z
**Depth:** standard
**Files Reviewed:** 74
**Status:** issues_found

## Summary

I reviewed the Phase 9 voice-call stack at standard depth, starting with the call core: `CallCenter`, `AgoraManager`, `CallService`, `CallForegroundBridge`, `CallNotificationFactory`, `CallModule`, the overlay and permission UI, the link launchers and the manifest. I then reviewed the guard and Join call sites. I checked the code against the iOS reference (`MeetingCallViewModel.swift`, `UpcomingBooking.swift`) and the backend contract (`call.controller.js`, `call.schema.js`, `expirePastSessions.js`, `CALL-END-SEMANTICS.md`).

The core state machine mostly holds up:
- Every transition is confined to Main.
- `startJob.cancelAndJoin()` correctly lets an in-flight engine join settle before leave.
- `/end` is always sent after leave and before destroy, and its body matches the `.strict()` schema.
- confirm-connection fires at most once per session.
- RTC credentials never reach the service intent or a `toString`.

No finding reaches BLOCKER level. The defects found are:
- Latent races in the start/cutoff ordering.
- A `/end` that reports a 1-second call leg that never happened.
- An unsynchronised engine reference reachable from UI controls during teardown.
- `stopSelf()` misuse in the foreground service.
- The notification tap stacking a second `MainActivity` instance.
- External links that are rejected by the allow-list still confirming the connection.
- A join-window rule (carried over from iOS) that locks users out of rejoining long sessions.

## Narrative Findings (AI reviewer)

## Warnings

### WR-01: Cutoff starts before `startJob` is assigned, and the start coroutine guards only on session id, not on `isEndingCall`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt:134-135, 149, 187, 208, 215`
**Issue:** `startCall` launches the cutoff with `Dispatchers.Main.immediate` (line 134) before `startJob` is assigned (line 135). If the cutoff is already due, the launch body runs synchronously and calls `endCall(forced = true)`. `performEnd` then runs at once:
- `cutoffJob?.cancel()` and `startJob?.cancelAndJoin()` act on the previous session's jobs, which are already finished.
- `performEnd` then suspends on `/end`.

Back in `startCall`, `startJob` is launched against a session that is already ending. `runStart` checks only `_session.value?.id != id`, so it keeps going while `isEndingCall == true`: permission prompt, FGS start, engine join.

The dangerous interleaving: `finishEnd` reaches `engine.destroy()` while `runStart` is suspended between the line-208 check and `engine.join`'s `withContext(io)`.
1. `destroy` wins the mutex and finds no engine.
2. `join` then creates a new engine and publishes the microphone.
3. Line 215 sees the id mismatch and returns without `leave`/`destroy`.

The result is a live, publishing Agora engine with no session, no FGS and no UI.

With today's Join gating this is hard to reach: it needs `scheduledEnd + 1h <= now` at tap time. But the invariant "start never outlives end" currently holds only because of call ordering, and nothing enforces it. Line 215 has the same latent leak if any future path clears the session without cancelling `startJob`.
**Fix:**
```kotlin
setSession(session)
_isMinimized.value = false
startJob = scope.launch(Dispatchers.Main.immediate) { runStart(id, request) }
startCutoff(session) // after startJob exists, so performEnd can cancel it

// in runStart/startInApp/startExternal, use one predicate everywhere:
private fun isLive(id: Long) = _session.value?.let { it.id == id && !it.isEndingCall } == true

// and after engine.join:
if (!isLive(id)) { engine.leave(); engine.destroy(); return }
```

### WR-02: Hanging up before any channel/link was joined sends a fabricated `/end` with a 1-second duration

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt:383-402`
**Issue:** If the user taps End while still Connecting (`/join` in flight, permission prompt open, FGS starting), `joinStartedAt` is null:
- `seconds` becomes 0 and is clamped to 1 (line 389).
- `/end {actual_duration: 1, end_reason: "manual_exit"}` is sent anyway (line 398).

The class doc and D-04 both say "no `/end` because no channel was joined", but that rule only applies to `abortStart`. iOS sends `max(0, d)` = 0 here, which the backend rejects with a 400, so on iOS nothing is written. Android instead persists a 1-second `manual_exit` leg over the booking row's `actual_duration_secs`/`end_reason`. That overwrites the metadata from a real earlier leg, and it also broadcasts `call.ended` to the peer, who then sees their call drop to "waiting for peer".
**Fix:** Skip the request when no leg started. Keep the clamp only for a real sub-second leg:
```kotlin
val joinedAt = current.joinStartedAt
if (joinedAt != null) {
    val secs = Duration.between(joinedAt, clock.instant()).seconds.coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
    withTimeoutOrNull(END_REQUEST_TIMEOUT_MS) { /* POST /end */ }
}
```

### WR-03: `/end` can overtake the in-flight confirm-connection PATCH, so a `completed` hang-up gets a 409 and is lost

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt:286-303, 385, 396-402`
**Issue:** `confirmConnectionOnce` sets `confirmSent = true` and fires the PATCH in a separate, untracked coroutine. If the user hangs up shortly after Active, `performEnd` reads `confirmSent == true` and sends `end_reason: "completed"`, possibly before the PATCH has moved the booking `confirmed -> in_progress`. The backend then rejects with 409 (`ENDABLE_STATUSES = ['in_progress']`, call.controller.js:227-231). The leg metadata is dropped, and `call.ended` is never emitted to the peer. The same happens when the PATCH itself fails, because the flag is set optimistically and never retried.
**Fix:** Keep the confirm `Job` and await it (bounded) before choosing the reason and sending `/end`. Record whether it succeeded:
```kotlin
private var confirmJob: Job? = null
// in confirmConnectionOnce: confirmJob = scope.launch { ... confirmOk = true ... }
// in performEnd, before computing reason:
withTimeoutOrNull(CONFIRM_SETTLE_MS) { confirmJob?.join() }
val reason = if (confirmOk) EndReason.Completed else EndReason.ManualExit
```

### WR-04: Mute/speaker controls stay live during teardown, and `AgoraManager.engine` is read across threads without synchronisation

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt:310-323`; `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt:45, 146-159, 161-172`
**Issue:** `toggleMute`/`toggleSpeaker` don't check `isEndingCall`, and `MeetingCallControls` keeps both buttons enabled while ending. So the user can call `engine.setMuted` on Main while `performEnd`/`finishEnd` run `leave()`/`destroy()` on the IO dispatcher. `engine` is a plain `var` (not `@Volatile`). `setMuted`, `setSpeakerOn` and `leave()` read it without the mutex, while `destroy()` calls `RtcEngine.destroy()` and nulls it under the mutex. A toggle can therefore call into an engine that is being or has been destroyed. The session flag can also flip after `status = Ended`.
**Fix:** In `CallCenter`, return early when `current.isEndingCall`, and disable the controls in the UI when `state.isEndingCall`. In `AgoraManager`, mark `engine` `@Volatile` and capture it once per call (`val rtc = engine ?: return false`). Also take the mutex in `leave()` so it cannot interleave with `destroy()`.

### WR-05: `CallService.teardown()` uses `stopSelf()` (no start id), and the failure path stops a service that never reached foreground

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallService.kt:76-79, 88-98, 161-166`
**Issue:** `teardown()` calls `stopSelf()`, which stops the service even if a newer `startForegroundService` command is already queued, for example after a retry following the 3 s `START_TIMEOUT_MS` in `CallForegroundBridge`. That newer command's `startForeground` is then never called. On API 26+ that is the "Context.startForegroundService() did not then call Service.startForeground()" crash path. The same risk applies to:
- `infoFrom(intent) == null` at line 76, which calls `teardown()` without ever calling `startForeground`.
- The `SecurityException`/`IllegalStateException` catch at lines 88-98. Whether stopping after a refused `startForeground` avoids the platform's FGS-did-not-start crash varies by OS version and needs device verification on API 34+.

A stale late `onServiceStarted(false)` from an earlier attempt can also complete the current attempt's `pendingStart` and tear down the new start.
**Fix:** Keep `startId` and use `stopSelfResult(startId)` so only the command that failed is stopped. On any path that received `ACTION_START`, post a minimal notification through `startForeground` before validating, then stop. Tag `pendingStart` with an attempt id and include that id in the start intent, so `onServiceStarted` only completes the matching deferred.

### WR-06: Notification tap can create a second `MainActivity` on top of a Custom Tab

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallNotificationFactory.kt:95-106`; `app/src/main/AndroidManifest.xml:25-30`
**Issue:** `MainActivity` is `singleTop`, and the content intent uses `NEW_TASK | SINGLE_TOP`. `singleTop` only reuses the instance when it is at the top of the task. Custom Tabs launched from the activity (`openCustomTab`, external-meeting fallback, any web link) run in the same task, on top of `MainActivity`. Tapping the ongoing-call notification then brings the task forward and starts a second `MainActivity`. The second instance has its own `AppViewModel` (splash/route resolution runs again), its own `CallOverlayHost`/`MicPermissionRequester`, and a confusing back stack.
**Fix:** Add `Intent.FLAG_ACTIVITY_CLEAR_TOP` to the open intent (`NEW_TASK | CLEAR_TOP | SINGLE_TOP`) so the existing instance receives `onNewIntent`. Alternatively, switch the activity to `launchMode="singleTask"`.

### WR-07: A rejected or unlaunchable meeting link still confirms the connection and minimizes

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHost.kt:69, 80-84`; `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt:167-173, 326-333`
**Issue:** `startExternal` accepts any non-blank `join_url`, so a `javascript:`, `intent:` or `file:` link (expert-controlled data, T-09-07) still enables the Open button. On tap:
1. `openExternalMeeting` rejects the link through the allow-list and calls `onFailure = {}`, so nothing happens and the user gets no feedback.
2. `viewModel.onExternalLinkOpened()` runs anyway.
3. confirm-connection (`skype_zoom`) is sent, the booking moves to `in_progress`, the call minimizes, and the later `/end` reports `completed`.

iOS fires regardless only because `UIApplication.open` always hands off. The Android allow-list is new behaviour, and its rejection should not count as "opened".
**Fix:** In `startExternal`, validate the URL with `isAllowedMeetingUri(Uri.parse(url))` and abort with `JoinErrorReason.MissingMeetingLink` (or a new `InvalidMeetingLink`) when it is invalid. Have the launcher report success, and call `onExternalLinkOpened()` only on success. Show a Snackbar or dialog from `onFailure`.

### WR-08: Provider detection can be spoofed, so expert-supplied links get a trusted "Open in Zoom" label

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/ExternalMeetingProvider.kt:49-65`
**Issue:** `host.contains("zoom.us")` matches `zoom.us.attacker.example` and `notzoom.us`. The raw-substring fallback labels `https://evil.example/?q=skype` as Skype. The KDoc claims the code "never mislabels a link (T-09-09)", but an expert-controlled URL can get a provider-branded button and icon. That lends credibility to a phishing destination.
**Fix:** Match on the registrable domain, not a substring, e.g. `host == d || host.endsWith(".$d")` for each of `zoom.us`, `zoom.com`, `skype.com`, `meet.google.com`, `teams.microsoft.com`, `teams.live.com`. Drop the raw-substring fallback for http(s) URLs and keep it only for unparseable strings, if at all.

### WR-09: Join window (carried over from iOS) disables Rejoin for the second half of any session longer than 60 minutes

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt:36-46`
**Issue:** For `confirmed`/`in_progress` bookings the button is enabled only within ±3600 s of `start`. Under CALL-END-SEMANTICS (which is also the reason CALL-08 exists), a hang-up leaves the booking `in_progress` and the user "can rejoin for the whole window". For any booking that ends more than 60 minutes after it starts, Join is greyed out from `start + 60 min` until the end, even though the backend accepts `/join` until `end + 120 min`. The `completed -> end > now` branch is now effectively dead, because `completed` is only set by the cron at `end + 15 min`, so it cannot cover this case. iOS has the same rule, so this is an inherited defect. It still contradicts the phase requirement.
**Fix:** Enable through the scheduled end for live statuses, e.g. `now in [start - 3600 s, max(start + 3600 s, end)]`. Log an iOS follow-up for parity.

### WR-10: Non-`ApiException` failures in the start coroutine crash the process with the FGS possibly running

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt:138-161`; `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt:126-144`
**Issue:** `runStart` catches only `ApiException`. `@ApplicationScope` is `SupervisorJob() + Dispatchers.Default` with no `CoroutineExceptionHandler` (SessionModule.kt). Anything else thrown is uncaught and kills the process, leaving a stale ongoing notification state for the user. Possible sources:
- `createEngine` catches only `Exception`, so native load failures such as `UnsatisfiedLinkError` escape.
- A `RuntimeException` from `RtcEngine.joinChannel`.
- `IllegalArgumentException` from `startForegroundService`.
- A bad `Uri`.
**Fix:** Wrap the body of `runStart` in a catch-all that rethrows `CancellationException` and otherwise calls `abortStart(id, CallDialog.JoinError(JoinErrorReason.Unknown), engineTouched = engineEngaged)`. In `createEngine`, catch `Throwable` (minus `CancellationException`).

## Info

### IN-01: Tap-outside dismissal of the first mic prompt is classified as a permanent denial

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MicPermissionRequester.kt:44-52, 80-85`
**Issue:** On Android 11+, dismissing the very first system prompt by tapping outside returns `denied` with `shouldShowRequestPermissionRationale == false`. `micResultAfterRequest` maps that to `DeniedPermanently`, so the user sees the "Open settings" dialog even though the app can still ask again. This is cosmetic, because the next Join re-prompts.
**Fix:** Record whether rationale was `true` before the request. Treat "false before and false after, with no grant" as permanent only if a previous request was already denied (for example, track a persisted "asked once" flag).

### IN-02: `MainActivity` is exported and acts on `EXTRA_EXPAND_CALL` from any caller

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/MainActivity.kt:51, 58, 66-70`
**Issue:** Any app can send an explicit intent with the extra and expand the call overlay. The impact is limited to UI state (no data read, T-09-43). A safer marker is a non-exported trampoline or a check on the `PendingIntent` creator.
**Fix:** Optional. Route the notification through a non-exported `activity-alias` or a `Intent.setIdentifier` token that only `CallNotificationFactory` produces.

### IN-03: The realtime keep-alive also applies to external (Skype/Zoom) calls that have no foreground service

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManager.kt:30-33, 72-76`
**Issue:** `isCallActive` is true for `skype_zoom` sessions too, but D-07 gives those no FGS. The KDoc says "the call's foreground service keeps the process eligible", which is false for this variant. The socket is requested in the background while the process can be frozen or cached, so expect churn and reconnect loops.
**Fix:** Either key the keep-alive on "in-app voice session active", or correct the comment and accept the behaviour.

### IN-04: `TodaysSessionUi.status` defaults to `"confirmed"`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreUiState.kt:56`
**Issue:** If a construction site omits the status, a `pending_approval` session would silently get an enabled Join button. Only one site builds this type today, and it passes the real status.
**Fix:** Remove the default so the compiler forces every caller to supply it.

### IN-05: Meeting allow-list accepts cleartext `http`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/web/ExternalLinkLauncher.kt:20-26`
**Issue:** `http://` meeting links open as-is in a browser or Custom Tab, so an expert-supplied link can send the client to a cleartext page.
**Fix:** Upgrade `http` to `https` before launching, or reject `http` for meeting links.

### IN-06: `CallPermissionBroker` always goes through the UI even when the permission is already granted

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallPermissionGate.kt:52-71`
**Issue:** A granted permission still waits for the overlay's `LaunchedEffect` to compose and answer. If the activity stops right after Join, no frame runs, and the start stalls for up to `MIC_PERMISSION_TIMEOUT_MS` (60 s) before cancelling silently.
**Fix:** Inject `@ApplicationContext` and return `Granted` immediately when `checkSelfPermission(RECORD_AUDIO)` already passes. Use the UI only to prompt.

---

_Reviewed: 2026-10-24T17:28:40Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
