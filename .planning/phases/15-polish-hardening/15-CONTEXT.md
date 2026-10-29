# Phase 15: Polish & Hardening - Context

**Gathered:** 2026-10-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Harden the MVP that already exists. No new features. Phase 15 delivers four things:
- **POL-01:** every MVP screen, and every bottom sheet that loads data, has loading, empty and error-with-retry states.
- **POL-02:** core flows (signup, booking, chat, call, and forms with user input) survive Activity recreation and process death with the user's position and input kept.
- **POL-03:** the app draws edge-to-edge with correct insets for system bars, display cutouts and the IME.
- **POL-04:** a TalkBack user can log in, book a session, join a call and send a chat message.

This phase also consolidates the deferred visual and accessibility device checks from Phases 3–14 into one non-blocking device checklist.

</domain>

<decisions>
## Implementation Decisions

### Orientation and large screens
- **D-01: Lock phones to portrait app-wide**, including the call screen. This matches iOS (`INFOPLIST_KEY_UISupportedInterfaceOrientations = UIInterfaceOrientationPortrait`). There's one policy for the single Activity, with no per-screen exceptions. Today the manifest declares no orientation. Reversibility: reversible.
- **D-02: Large screens (sw600dp+) must be usable, not optimized.** At targetSdk 37 the system ignores the orientation lock on large displays, so tablets and foldables will rotate and resize. Content must never be clipped or unreachable, input must survive resizing, and content that would stretch badly gets a max width and is centered. No tablet-specific or list-detail layouts. Verify on a resizable emulator.
- **D-03: SC-2's "rotating the device" means any Activity recreation.** With phones locked, verify it with config changes (dark/font-scale change, resize on an emulator or large screen) and with "Don't keep activities" / process death. Record this reading in VERIFICATION.md. Release and debug builds behave the same, with no debug-only rotation.

### Loading, empty and error standard (POL-01)
- **D-04: A failed first load (nothing to show) uses the shared `ErrorState(onRetry)`** from `components/EmptyState.kt` on every screen, in place of ad-hoc text or an AlertDialog. Today only 2 real screens use it (ClientProfileSettings, ExpertProfile).
- **D-05: A failed refresh with content already showing keeps the content and shows a snackbar** ("Couldn't refresh") with a Retry action. This covers pull-to-refresh, explicit reloads and realtime-triggered refetches. **Quiet** tab-return reloads fail silently, with no snackbar.
- **D-06: No dedicated offline UI.** No ConnectivityManager monitoring and no offline banner. Network failures go through the D-04 and D-05 paths. The planner may distinguish "no connection" (IOException) from a server error in the copy, but this is optional.
- **D-07: Audit scope = every `*Screen.kt` destination plus every bottom sheet that fetches data** (topic picker, time slots, and similar). For static forms and dialogs, check only their in-flight state and their submit-error state.
- **D-08: Keep the existing loading idioms.** Skeletons are already widespread (~56 files) and pull-to-refresh is on 14 screens. Fill gaps with the same skeleton pattern. Don't rework screens that already comply.

### Process death and recreation (POL-02)
- **D-09: Signup restores the current step plus the unsaved field text** (name, city, etc.) through `SavedStateHandle`. It also re-hydrates the app-scoped `SignupDraft` (an in-memory `@Singleton` in `SignupSetupRepository`) from the server. **Passwords, OTP codes and other secrets are never written to the saved-state Bundle.** After process death those fields come back empty. Reversibility: reversible.
- **D-10: The chat composer's unsent text survives per thread through `SavedStateHandle`** (`ChatThreadUiState.composerText` is ViewModel-only today). It survives recreation and process death while the thread is in the back stack. It is not persisted across app restarts. Failed bubbles still don't persist (Phase 12 D-02).
- **D-11: If the process is killed during a live call,** relaunch lands on the booking/session detail screen with a Join/Rejoin action. **Never auto-rejoin, and never open the mic without a user tap.** Re-fetch the microphone permission and the Agora token on rejoin. The foreground `CallService` makes this rare.
- **D-12: Sheets and dialogs that hold user input reopen with that input** after recreation and process death (examples: report details, decline reason, the time range sheet). Confirm-only dialogs may simply close. Scroll position is best-effort: kept where the data loads synchronously, and not guaranteed otherwise.
- **D-13: Booking already complies** (Phase 6: `SavedStateHandle` with assisted injection, device-verified). Use it as the reference pattern. The 3DS bank step returning an idle form after process death is still accepted (Phase 7 D-17).

### Verification
- **D-14: An automated gate plus one consolidated, non-blocking device checklist.** The automated gate:
  - Robolectric/Compose tests for loading, empty and error states.
  - `StateRestorationTester` and `SavedStateHandle` tests for the core flows.
  - Semantics assertions (content descriptions, roles, state descriptions, traversal order) for the 4 SC-4 TalkBack flows.
  - Compose accessibility checks enabled in the UI tests.

  The device checklist is a human item. Following the standing preference, it is logged as open and does not block phase close.
- **D-15: The device checklist absorbs every deferred visual, accessibility and system item from Phases 3–14 in STATE.md** (200% font scale, TalkBack, rotation/process death, IME and keyboard checks). It is deduplicated and ordered by flow, and the originals are marked superseded by Phase 15. **Stripe-card items** (blocked on the test-mode backend) and **live-call items** (need an iOS peer) stay with Phase 16.
- **D-16: Priority if the work exceeds Week 7:**
  1. The 4 SC-4 core flows get the full treatment first: login, booking (free and paid), call, chat.
  2. Then the remaining client screens.
  3. Then the expert screens.
  4. Then settings.

  Leftovers go on a logged gap list handed to Phase 16.
- **D-17: Automated insets guard (POL-03).** Extend `scripts/check-design-rules.sh` to flag screens and Scaffolds that are missing insets handling. Add a few Robolectric tests with IME and cutout insets on the chat composer and the form screens. `enableEdgeToEdge` (in `MainActivity.kt`) and `adjustResize` are already set.

### Claude's Discretion
- How to apply the orientation lock (manifest `screenOrientation` versus a runtime setting), as long as D-01 and D-02 hold.
- The exact snackbar and ErrorState copy, and whether to tell IOException apart from server errors (D-06).
- Which ViewModels without `SavedStateHandle` (34 of 58) actually need one. Read-only screens that just reload are fine without it.
- The structure of the device checklist file and its plan (it follows the Phase 13 D-21 "one non-blocking checklist plan" pattern).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements and scope
- `.planning/ROADMAP.md` § "Phase 15: Polish & Hardening": goal and success criteria SC-1..SC-4.
- `.planning/REQUIREMENTS.md`: POL-01..POL-04.
- `.planning/PROJECT.md` § Key Decisions: locked DEC-* entries (iOS source of truth, Compose M3, MVVM, Hilt, SDK levels).
- `CLAUDE.md`: platform swaps and file organization.

### Deferred device checks to consolidate (D-15)
- `.planning/STATE.md` § Accumulated Context: every "[Phase N] DEFERRED (human)" entry and its unchecked visual and system sub-items for Phases 3–14, including the Phase 13 and Phase 14 hand-offs naming their screens for the TalkBack and font-scale pass.

### Prior decisions this phase must respect
- `.planning/phases/06-booking-flow-free-bookings/06-CONTEXT.md` and `06-UAT.md`: the booking `SavedStateHandle` pattern, verified on a device (reference for D-13).
- `.planning/phases/07-payment-methods-paid-booking/07-CONTEXT.md`: D-17, the accepted idle form after process death in 3DS.
- `.planning/phases/12-chat-report-block/12-CONTEXT.md`: D-01/D-02, the failed-send state and no persistence across process death.
- `.planning/phases/13-push-notifications-internal-testing/13-CONTEXT.md`: D-21, the non-blocking human checklist plan pattern.

### iOS reference
- `ios-reference/ClickMe.xcodeproj/project.pbxproj`: `UISupportedInterfaceOrientations = Portrait` (D-01).
- `ios-reference/`: the per-screen loading, empty and error behavior and copy, used where it exists.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `components/EmptyState.kt`: `EmptyState(icon, heading, body, action)` and `ErrorState(onRetry)`, the standard for D-04.
- The existing skeleton composables (~56 files) and the `PullToRefresh` usage (14 screens) for D-08.
- `SnackbarHost` is already wired on 23 screens, for D-05.
- `navigation/NavigationState.kt`: Navigation 3 `rememberNavBackStack` plus `rememberSaveableStateHolderNavEntryDecorator`. Back stack keys must stay serializable so the back stack restores after process death.
- `test/.../screens/clientmode/makeabooking/EntrySavedStateHandleTest.kt`: the existing SavedStateHandle test pattern.
- `scripts/check-design-rules.sh`: the place to add the D-17 insets rule.

### Established Patterns
- MVVM with `StateFlow` UiState. Some screens use a sealed `*UiState` (BookingSummary, RequestSent, UpcomingBooking).
- `SignupDraft` is an app-scoped singleton re-hydrated from the server. Each step PATCHes, so the server already holds the saved steps.
- `MainActivity.kt` calls `enableEdgeToEdge(...)`. The manifest sets `windowSoftInputMode="adjustResize"` and declares no orientation.
- `services/call/CallService.kt` is the foreground service that keeps calls alive (Phase 9).

### Integration Points
- `AndroidManifest.xml` (MainActivity) for the orientation lock.
- 34 ViewModels without `SavedStateHandle`, including the signup steps (Review, Overview, ProfilePhoto, SetupHost), `MeetingCallViewModel`, the settings forms (AccountSecurity, DeleteAccount, profile settings) and the expert Request Decision.
- `screens/chat/chatting/ChatThreadViewModel.kt` / `ChatThreadUiState.kt` for D-10.

</code_context>

<specifics>
## Specific Ideas

- SC-4's four TalkBack flows are the acceptance bar: log in, book a session, join a call, send a chat message.
- The test device is the Samsung SM-A075F. Use a resizable emulator for the large-screen checks in D-02.

</specifics>

<deferred>
## Deferred Ideas

- Persisting chat drafts to disk across app restarts (beyond POL-02). Possible post-MVP polish.
- Proper adaptive or tablet layouts (list-detail panes). Post-MVP.
- An app-wide offline banner driven by a NetworkCallback. Not required by iOS parity or by any requirement.

</deferred>

---

*Phase: 15-polish-hardening*
*Context gathered: 2026-10-29*
