---
phase: 09-voice-call
fixed_at: 2026-10-24T20:33:28Z
review_path: .planning/phases/09-voice-call/09-REVIEW.md
iteration: 1
findings_in_scope: 10
fixed: 10
skipped: 0
status: all_fixed
---

# Phase 9: Code Review Fix Report

**Fixed at:** 2026-10-24T20:33:28Z
**Source review:** .planning/phases/09-voice-call/09-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 10 (WR-01 to WR-10; Info findings out of scope for `critical_warning`)
- Fixed: 10
- Skipped: 0

**Gate (run in the isolated worktree after the last fix):** `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` passed (2237 unit tests, 0 failures). `bash scripts/check-design-rules.sh` passed.

## Fixed Issues

### WR-01: Cutoff starts before `startJob` is assigned, and the start coroutine guards only on session id

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterCutoffTest.kt`
**Commit:** d4c1576
**Status:** fixed: requires human verification (concurrency ordering)
**Applied fix:**
- `startJob` is now launched before `startCutoff`, so an already-due cutoff's end flow finds and cancels the start.
- A single `isLive(id)` check (same id and not `isEndingCall`) replaces the id-only checks after each suspension in the start flow. `abortStart` uses it too, so it defers to a running end flow.
- After `engine.join`, a session that was cleared without an end flow (defensive; not reachable today) releases the engine with leave + destroy. An ending session leaves the teardown to `performEnd`, which waits for the start.
- New test: `cutoffAlreadyDueAtStart_cancelsTheStartBeforeAnyEngineWork`. Caveat: the test dispatcher queues `Main.immediate` launches, so the test pins the invariant but cannot reproduce the production inline-dispatch race itself.

### WR-02: Hanging up before any channel/link was joined sends a fabricated `/end` with a 1-second duration

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterEndTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterCutoffTest.kt`
**Commit:** d8ae622
**Applied fix:** `performEnd` now sends `POST /end` only when `joinStartedAt != null`. The 1 s floor stays only for a real sub-second leg. Reasoning, from the backend schema: `endCallSchema` is `.strict()` with `actual_duration: z.number().int().positive()`. iOS sends `max(0, d)` = 0 when no leg started, the backend rejects that with a 400, and nothing is recorded. Skipping the request gives the same server outcome. A clamped 1 s, by contrast, would overwrite `actual_duration_secs`/`end_reason` from a real earlier leg, and `call.controller.js` emits `call.ended` to the peer for every reason. The leave, destroy, service stop and clear still run. The existing "end while join in flight" test now expects no `/end`. Two new tests cover hang-up during the mic prompt and during the service start, and the WR-01 cutoff test asserts no `/end`.

### WR-03: `/end` can overtake the in-flight confirm-connection PATCH

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallSession.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterConfirmTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterEndTest.kt`
**Commit:** a90ae70
**Status:** fixed: requires human verification (concurrency and end-reason logic)
**Applied fix:**
- The confirm PATCH is tracked as `confirmJob`. `performEnd` leaves the channel first, then waits for the job (bounded by the new `CONFIRM_SETTLE_TIMEOUT_MS` = 5 s), and only then reads `confirmSent` to choose `completed` or `manual_exit`.
- `confirmSent` is now set only when the PATCH succeeds. At most one PATCH is in flight. A failed PATCH is retried on the next Active transition or link open, so it still succeeds exactly once.
- A confirm still in flight from the previous session is cancelled on a new start, so it cannot block the rejoin's confirm.
- This deviates from iOS, which sets its flag before the request (RESEARCH Pitfall 9).
- Six new tests cover:
  - `/end` waits for an in-flight confirm, then sends `completed`.
  - A failed confirm leads to `manual_exit`.
  - A hung confirm is bounded at 5 s.
  - A failed confirm is retried and then succeeds exactly once.
  - No duplicate PATCH is sent while one is in flight.
  - A stale confirm does not block a rejoin.

### WR-04: Mute/speaker controls stay live during teardown; `AgoraManager.engine` is unsynchronised

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallControls.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterConfirmTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallActionsTest.kt`
**Commit:** e078667
**Applied fix:**
- `toggleMute`/`toggleSpeaker` return early while `isEndingCall`.
- `MeetingCallControls` has a new `audioControlsEnabled` parameter, set to `!state.isEndingCall` for in-app calls.
- In `AgoraManager`, `engine` is `@Volatile` and `leave()` runs under the lifecycle mutex.
- `setMuted`/`setSpeakerOn` go through a non-blocking `tryLock` helper, so they never touch an engine that a join, leave or destroy is working on. A busy engine returns false and the UI flag stays unchanged.
- The `AgoraManager` change is covered by compilation only, because the native SDK has no unit-test harness.

### WR-05: `CallService.teardown()` uses `stopSelf()`, and failure paths can skip `startForeground`

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallService.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallForegroundBridge.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/services/call/CallForegroundBridgeTest.kt`
**Commit:** fbbd9ad
**Status:** fixed: requires human verification (device check on API 34+)
**Applied fix:**
- The service records the newest start id it has received and tears down with `stopSelfResult(lastStartId)`, so a queued newer start command keeps the service alive.
- Every `ACTION_START` calls `startForeground` before it validates, including a malformed intent (placeholder notification) or a stale or orphaned attempt. It then stops by start id.
- The bridge tags each start with an attempt id, sent as the new `EXTRA_ATTEMPT_ID` (not a credential). `onServiceStarted(attempt, success)` only completes the matching attempt, and `isCurrentAttempt` lets the service drop stale commands.
- Two new bridge tests: a late callback from an earlier attempt is ignored, and a stopped attempt is no longer current.
- Still unverified: whether `stopSelfResult` after a refused `startForeground` avoids the platform's FGS-did-not-start crash on API 34+. This needs a device check (Samsung SM-A075F / API 36). There is no Hilt service test harness, so `CallService` is covered by compilation only.

### WR-06: Notification tap can create a second `MainActivity` on top of a Custom Tab

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallNotificationFactory.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/services/call/CallNotificationFactoryTest.kt`
**Commit:** c753974
**Applied fix:** The content intent now uses `NEW_TASK | CLEAR_TOP | SINGLE_TOP`, so the existing instance receives `onNewIntent` (already handled by `MainActivity`). A new test asserts the three flags.

### WR-07: A rejected or unlaunchable meeting link still confirms the connection and minimizes

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/web/ExternalLinkLauncher.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallDialog.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogs.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHost.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallViewModel.kt`, `app/src/main/res/values/strings_call.xml`, `app/src/test/java/com/uptrendinvestments/clickmeapp/components/web/ExternalLinkLauncherTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterConfirmTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterStartTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogsTest.kt`
**Commit:** 4593e33
**Applied fix:**
- `startExternal` trims `join_url` and checks it with a new pure-JVM `isAllowedMeetingUrl`. A test pins it to the same rules as the existing `isAllowedMeetingUri`. A disallowed link aborts the start with a new `JoinErrorReason.InvalidMeetingLink`, which has its own copy.
- `openExternalMeeting` now returns whether something opened the link. `CallOverlayHost` calls `onExternalLinkOpened()` (confirm and minimize) only on success.
- On failure, a new `CallDialog.ExternalLinkFailed` ("Couldn't open meeting", pointing at Copy Link) is shown. It is queued through `CallCenter.onExternalLinkFailed()`, which never confirms or minimizes.
- New string resources: `call_join_error_invalid_link`, `call_external_open_failed_title`, `call_external_open_failed_message`.

### WR-08: Provider detection can be spoofed

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/ExternalMeetingProvider.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/ExternalMeetingProviderTest.kt`
**Commit:** 85e3bb8
**Applied fix:**
- Hosts now match only the provider's domain or a subdomain of it (`host == d || host.endsWith(".$d")`) for `skype.com`, `zoom.us`, `zoom.com`, `meet.google.com`, `teams.microsoft.com` and `teams.live.com`.
- The raw-substring fallback is removed. After WR-07, a link without a valid scheme and host can no longer reach the UI.
- URLs that `java.net.URI` cannot parse are read leniently (scheme, then authority host) and go through the same domain rule.
- The old substring-fallback test is replaced with look-alike and path/query spoof tests.
- **iOS deviation:** iOS still uses `contains` and the raw fallback (iOS follow-up).

### WR-09: Join window disables Rejoin for the second half of any session longer than 60 minutes

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt`
**Commit:** 107f900
**Applied fix (user decision, overriding the locked D-02 iOS-parity rule):**
- For live statuses, Join is enabled for `now` in `[start - 3600 s, max(start + 3600 s, end)]`, with both edges inclusive.
- `pending_approval` is still never joinable, and `completed` keeps its `end > now` rule.
- `isStartingNow` (the "Starting now" badge) is unchanged.
- The window is still only a UX gate. The backend still authorizes `/join` and the KDoc says so.
- Four new tests: a long session stays joinable until its end, a short session keeps the full hour after start, the opening edge is unchanged, and `pending_approval` is never joinable for a long session.
- **Android now deviates from iOS** (`UpcomingBooking.isWithinJoinWindow` still closes at start + 3600 s). iOS follow-up: apply the same `max(start + 3600 s, end)` closing edge.

### WR-10: Non-`ApiException` failures in the start coroutine crash the process

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterStartTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCallEngine.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCallForegroundController.kt`
**Commit:** 5e32c4a
**Applied fix:**
- `runStart` wraps the start steps in a catch-all. It rethrows `CancellationException`; any other `Exception` aborts with `JoinError(Unknown)` and `engineTouched = engineEngaged`.
- `AgoraManager.createEngine` also catches `LinkageError`, which covers `UnsatisfiedLinkError` from a native load failure.
- The test fakes gained `joinError` / `startError` hooks. Three new tests cover an engine join that throws (the engine is released), a service start that throws (the engine is never touched), and a non-API exception from the join request.

## Follow-ups (not code changes in this run)

- **iOS parity follow-ups:**
  - WR-09: join window closing edge `max(start + 3600 s, end)`.
  - WR-08: domain-based provider detection with no raw-substring fallback.
  - WR-02: iOS posts `/end` with 0 s before a leg starts, which the backend rejects with a 400. It should skip the request.
  - WR-03: iOS sets `hasConfirmedConnection` before the PATCH and does not wait for it before `/end`.
- **Device verification:**
  - WR-05: FGS start-failure and stale-start paths on API 34+.
  - WR-06: tap the notification while a Custom Tab is open; expect a single `MainActivity` and the call expanded.
  - WR-07: a link that no app can open shows the dialog and does not confirm.
- **Info findings IN-01 to IN-06:** out of scope for this run (`fix_scope: critical_warning`).

---

_Fixed: 2026-10-24T20:33:28Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
