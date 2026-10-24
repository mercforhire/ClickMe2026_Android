# Phase 9: Voice Call - Context

**Gathered:** 2026-10-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Port the iOS call stack (`CallCenter`, `MeetingCallViewModel`, `AgoraManager`, `MeetingCallView`, `MeetingCallPill`, `CallOverlayHost`) so an Android **client** can:

1. Tap Join and start the call through `POST /bookings/:id/join`.
   - An `agora_config` response starts an in-app Agora voice call.
   - An `external_config.join_url` response starts the external-meeting variant (D-05).
2. Hold the in-app call in a `microphone`-type foreground service. The service has audio focus and a persistent notification, and the call keeps going in the background, with the screen off and after a task swipe.
3. Minimize the call to a pill that stays visible across tabs and the Client/Expert mode switch, and expand it again.
4. Get the "you're in a call" guard on restricted actions.
5. Have `confirm-connection` sent once per call when both sides are in the channel.
6. See "waiting for peer" when the peer leaves (`call.ended` or an Agora remote drop) without the call ending.
7. Hang up: leave the channel, then `POST /bookings/:id/end`. The user can rejoin for the whole window (CALL-END-SEMANTICS.md).
8. See a timer that counts from the scheduled start, turns amber in the last 5 minutes, and force-ends the call 3600s after the scheduled end.

Requirements: CALL-01 to CALL-08.

Out of scope:
- Expert-side Join entry points (Expert Dashboard, expert bookings). Phase 11 wires them to the same `CallCenter.startCall`. The call stack itself is role-agnostic.
- The in-call Chat action. The button is shown disabled (D-11), and Phase 12 makes it work.
- Guard call sites on screens that don't exist yet (D-13).
- `Call/Ready` (`ReadyToStartCallView`). Don't port it.
- Restoring a call after process death (D-15).

</domain>

<decisions>
## Implementation Decisions

### Join entry points & window
- **D-01: Wire all three `PHASE-9` Join placeholders to one `CallCenter.startCall(...)`.**
  - Upcoming Booking Details (`UpcomingBookingJoinCard`), whether it was opened from Explore or from My Bookings.
  - The My Bookings upcoming card (`mybookings/subviews/UpcomingBookingCard.kt`).
  - The Explore Today's Sessions card (`ExploreTodaysSessionsSection`).
  - This closes the iOS gap at `MyBookingsView.swift:273`, where the card passes an empty `onJoinCall`. Remove every `PHASE-9` marker.
- **D-02: Join is enabled by the iOS client-side rule** (`UpcomingBooking.isWithinJoinWindow`).
  - Never for `pending_approval`.
  - For `completed`, enabled while `endTime > now` (rejoin).
  - Otherwise enabled within ±3600s of `startTime`.
  - Don't use the server's `actions.canJoin`. iOS dropped it because its window (~5 min) is too tight. The server still has the final say on `POST /join`.
- **D-03: The enabled state updates live.** A lightweight clock flow (about every 30s, Claude picks the exact cadence) re-evaluates the window while a screen with a Join button is visible, so the button turns on without leaving the screen.
- **D-04: Join failures follow iOS `CallCenter.joinError`.**
  - Covers server refusal, network errors, and a missing `agora_config` or `join_url`.
  - The call container tears down immediately, with no `/end`.
  - One root-level Material `AlertDialog` shows the user message.
  - A mic-permission denial is a separate signal with its own dialog (CALL-02, Claude's discretion).

### Skype/Zoom external path
- **D-05: Port the iOS external-meeting surface. Don't just launch the link.**
  - A `skype_zoom` join uses the same call container and pill, in an external variant: peer info, the schedule-anchored timer, **Open in Skype/Zoom**, **Copy link** with a short "Link copied" confirmation, and Hang up.
  - The initial variant comes from the booking's `meeting_type` hint. The `POST /join` response overrides it.
  - `confirm-connection` (`method: "skype_zoom"`) fires once, only when the user taps Open. Copy doesn't count.
  - Open also minimizes the call to the pill.
  - Hang up sends `/end`, as on the Agora path.
- **D-06: Open the link with an app intent first and a Custom Tab as fallback.**
  - Try `ACTION_VIEW` first, so an installed Zoom, Skype or Teams app can claim the link.
  - If nothing resolves it, use the existing `components/web/CustomTabLauncher.kt`.
  - This refines CALL-01's "Custom Tab or intent".
- **D-07: The external variant gets no foreground service and no notification.**
  - There's no mic and no Agora, and a `microphone`-type foreground service without mic use would be a Play policy problem.
  - The 3600s cutoff is enforced only while the process is alive.

### Call surface, pill & notification
- **D-08: The call UI is a root composable overlay, not a separate Activity.**
  - A `CallOverlayHost` sits in `ClickMeApp` above the `AppRoute` switch, so it survives tab changes and the Client/Expert mode switch.
  - Expanded call: a full-screen layer that handles edge-to-edge insets itself. iOS used `fullScreenCover` because a ZStack overlay lost the safe area.
  - Minimized: a floating pill near the bottom trailing corner, above the bottom navigation, as on iOS.
  - Call state lives in an app-scoped singleton (`CallCenter`, provided by Hilt), not in a screen ViewModel. The view can then mount and unmount without tearing down Agora.
- **D-09: System Back and predictive back on the expanded call minimize it to the pill.** Only Hang up ends the call. This matches iOS `expandedBinding`.
- **D-10: The ongoing-call notification opens the call on tap and has a Hang up action.**
  - Tap opens `MainActivity` with the call expanded.
  - The Hang up action runs the same end flow as the in-app button: leave the channel, `POST /end`, tear down.
  - The notification shows the peer name and a timer.
  - No Mute action.
- **D-11: The in-call Chat button is shown disabled** until Phase 12. This keeps the iOS control-row layout (Minimize, Chat, Mute, Speaker). Phase 12 enables it (`initiateChat`, minimize, switch to Chats, push the thread).

### Guards & lifecycle
- **D-12: Build the guard API now.**
  - Port `attempt(action)` ("End the call to {action}.") and `attemptModifyBooking(bookingId, action)` ("This session is in progress. End the call to {action}.").
  - One root `AlertDialog` titled "Can't do that during a call", with an OK button.
  - A second `startCall` during an active call shows "You're already on a call. End it before starting another."
- **D-13: Wire the guard at the call sites that exist today.**
  - Switch modes (`screens/settings/modeswitching/ModeSwitchScreen.kt`).
  - Log out (wherever the logout action lives now).
  - Add a payment method (Phase 7 Payment Methods).
  - Client Cancel on the booking in the call (`attemptModifyBooking`), plus client Reschedule if Android has that entry point.
  - Later phases wire their own call sites: Phase 10 (availability), Phase 11 (expert reschedule) and Phase 14 (change password, delete account, payouts). Add a note to those roadmap entries.
- **D-14: The call keeps going after a task swipe.** The foreground service outlives the task, and its notification reopens the app with the call expanded. Don't end the call in `onTaskRemoved`.
- **D-15: Don't restore a call after process death.**
  - If the process is killed anyway (an OEM battery killer, a crash), the call is gone, and the app persists no call state.
  - The booking stays live on the server, so the user just taps Join again (rejoin is allowed for the whole window).
- **D-16: Verify on a device only at phase end.**
  - The Agora spike's background-audio test (Phase 2 UAT #9) was never run.
  - Don't add a mid-phase device gate. Do one end-to-end check in UAT: Samsung client plus iOS expert, 5+ minutes in the background with the screen off, notification Hang up, rejoin, and a full paid session with `confirm-connection` sent once.
  - Following the project's deferred-manual-testing rule, log it as an open item if it can't run right away.
  - Delete `:spikes:agora` once its `CallService` findings are absorbed into `:app` (Phase 2 D-10).

### Behavior ported as-is from iOS (no discussion needed)
- `callStatus` is derived from (my connection state, remote connection state):
  - local disconnected → `ended`
  - remote disconnected, or local ready while remote is waiting → `waitingForPeer`
  - both ready → `active`, and run `confirmConnectionOnce()`
  - local waiting → `connecting`
- A `call.ended` event for the same booking id moves the call to `waitingForPeer` and never ends it. Events for other bookings are ignored.
- `confirm-connection` uses a local once-per-call guard. It also fires on rejoin (it's idempotent on the server now). A 409 is ignored, and other errors are logged without interrupting the call.
- End reason: `completed` if confirm-connection was sent, otherwise `manual_exit`. `actual_duration` is seconds since the local join. Always send `/end`. Never branch on the `/end` response status (CALL-END-SEMANTICS).
- Timer: `elapsed = now - scheduledStart`. Before the start it shows "Starts in M:SS". Amber with "M:SS left" when 0 < remaining ≤ 300s. Past `scheduledEnd` it shows the "scheduled time complete" state and the call continues. At `scheduledEnd + 3600s` the call is force-ended, once (CALL-07; not the iOS `5 * 60` TEMP value).
- Audio defaults: as in the Phase 2 spike `CallService` (speech-standard profile, speakerphone on by default, mic unmuted).

### Claude's Discretion
- Mic permission UX (CALL-02): the rationale dialog before the request, the permanently-denied dialog with an "Open settings" app-details intent, and POST_NOTIFICATIONS on API 33+ (asked together or separately).
- Audio focus, audio routing (earpiece, speaker, Bluetooth) and interruptions such as an incoming phone call. Follow Agora and Android defaults sensibly.
- What happens on logout or session expiry during a call. Logout is guarded (D-13). If a forced logout (refresh 401) happens mid-call, end the call before clearing the session.
- Ticker cadence, pill visuals and animation, the notification channel's name and importance, and the exact copy of the external variant's status lines (port the iOS strings).
- How the service and `CallCenter` share state: bound service vs. a singleton the service observes.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Call lifecycle contract
- `backend-reference/docs/ios/CALL-END-SEMANTICS.md`: hanging up doesn't end the session, `/end` always returns the unchanged status, rejoin works for the whole window, `call.ended` is only a teardown signal, and confirm-connection is idempotent.
- `backend-reference/docs/SOCKET-CONTRACT.md`: the `call.ended` payload (it now also carries `completed`).

### iOS source of truth
- `ios-reference/ClickMe2026/Managers/CallCenter.swift`: the singleton call owner, startCall/endCall, the join and mic error bridges, minimize/expand, the guard API and its copy.
- `ios-reference/ClickMe2026/Views/Screens/Call/Calling/MeetingCallViewModel.swift`: the state machine, timer, cutoff (port 3600, not 5*60), confirmConnectionOnce, the end-reason rule, the external-meeting actions.
- `ios-reference/ClickMe2026/Views/Screens/Call/Calling/MeetingCallView.swift` and `Subviews/` (`MeetingCallTopBar`, `MeetingCallAvatar`, `MeetingCallInfo`, `MeetingCallControls`, `MeetingCallEndButton`, `MeetingCallPill`, `MeetingCallBrand`): the layout of the expanded UI, the pill and the Skype/Zoom variant.
- `ios-reference/ClickMe2026/Views/Screens/Call/Calling/CallOverlayHost.swift`: root overlay, pill placement, the restriction alert, and "dismiss means minimize".
- `ios-reference/ClickMe2026/Views/Screens/Call/Calling/ExternalMeetingProvider.swift`: the Skype/Zoom provider label.
- `ios-reference/ClickMe2026/Managers/AgoraManager.swift`: engine config, the connection-state publishers, mute and speaker.
- `ios-reference/ClickMe2026/Network/ClickMeAPI+Call.swift`, `Model/Responses/JoinCallResponse.swift`, `Model/Responses/EndCallData.swift`, `Model/Enums/MeetingType.swift`: endpoints and wire models.
- `ios-reference/ClickMe2026/Model/UIModels/UpcomingBooking.swift`: `isWithinJoinWindow` (D-02).
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Explore/ExploreClientView.swift`: how Today's Sessions calls onJoinCall.
- Guard call sites (D-12/D-13): `Client Mode/Profile/ClientProfileHomeScreen.swift`, `Expert Mode/Profile/ExpertProfileHomeScreen.swift`, `Settings/Payment Methods/PaymentMethodsView.swift`, `Client Mode/Cancellation/ClientCancellationView.swift`, `Client Mode/Reschedule/RescheduleView.swift`.

### Prior Android work
- `.planning/phases/02-networking-session-sdk-spikes/02-CONTEXT.md` D-10, D-13: the Agora spike scope, and the rule to delete spikes after use.
- `.planning/phases/02-networking-session-sdk-spikes/02-08-SUMMARY.md`: the spike `CallService` design (manifest permissions, `startForeground` before engine creation, idempotent teardown, never logging credentials).
- `spikes/agora/src/main/java/com/uptrendinvestments/clickmeapp/spike/agora/CallService.kt`: the code to absorb into `:app`.
- `.planning/phases/08-client-bookings-cancellation/08-CONTEXT.md` D-12: the Join placeholder hand-off.

### Project-level
- `.planning/REQUIREMENTS.md`: CALL-01 to CALL-08.
- `.planning/PROJECT.md`: DEC-agora, DEC-platform-swaps (`.alert` becomes `AlertDialog`), and the release note on the foreground-service declaration.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- The spike `CallService`: a microphone foreground service with `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)`, a notification channel, and an Agora join with `ChannelMediaOptions`.
- `components/web/CustomTabLauncher.kt`: the fallback for D-06.
- `realtime/RealtimeEvents.kt`: `CALL_ENDED = "call.ended"` and `RealtimeEvent.CallEnded` already decode. `CallCenter` subscribes and filters by booking id.
- `BuildConfig.AGORA_APP_ID` / `config/AppConfig.kt`: the Agora app id for each flavor.
- `UpcomingBookingUiState` already carries `meetingType` and `joinLink`, which feed the initial variant hint.

### Established Patterns
- Hilt singletons for app-scoped managers (`managers/UserManager.kt`). `CallCenter` goes in the same place, under `managers/` (iOS `Managers/CallCenter.swift`).
- MVVM with Compose State. Coroutines, no Rx. Root-level dialogs are hosted in `ClickMeApp.kt`, as the suspension dialog is.
- The design-rules script (no hex literals) and ktlint run in CI.
- File organization mirrors iOS: `screens/call/calling/MeetingCallScreen.kt`, `MeetingCallViewModel.kt`, `subviews/...`.

### Integration Points
- `ClickMeApp.kt`: mount the `CallOverlayHost` above the `AppRoute` content.
- `network/ClickMeApi.kt`: add `POST /bookings/{id}/join`, `PATCH /bookings/{id}/confirm-connection` and `POST /bookings/{id}/end`. No call endpoints exist yet.
- `AndroidManifest.xml` (`:app`): RECORD_AUDIO, MODIFY_AUDIO_SETTINGS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS, and the service declaration with `foregroundServiceType="microphone"`.
- `MainActivity`: handle the notification's tap intent to expand the call.
- The three Join placeholders (D-01), and the guard call sites (D-13).
- `settings.gradle.kts`: remove `:spikes:agora` at the end of the phase (D-16).

</code_context>

<specifics>
## Specific Ideas

- Android deliberately goes beyond iOS by enabling Join on the My Bookings card (D-01). Log it as an iOS follow-up, as Phase 8 did with its iOS deviations.
- End-to-end proof uses the finished iOS app as the expert on the other side, as the Phase 2 spike plan did.

</specifics>

<deferred>
## Deferred Ideas

- Enabling the in-call Chat button: Phase 12.
- Expert-side Join entry points: Phase 11.
- Guard call sites for availability (Phase 10), expert reschedule (Phase 11), and change password, delete account and payouts (Phase 14).
- A "Rejoin your call?" prompt after process death. Not planned (D-15).
- The Play Console foreground-service declaration and video: Phase 17.

</deferred>

---

*Phase: 09-voice-call*
*Context gathered: 2026-10-23*
