---
phase: 15-polish-hardening
reviewed: 2026-10-29T00:00:00Z
depth: standard
files_reviewed: 200
lanes: [A, B]
findings:
  critical: 0
  warning: 13
  info: 11
  total: 24
status: issues_found
---

# Phase 15: Code Review Report (combined)

Two lanes reviewed the 200 files changed since `283f0109`. Lane A covered app root, components, managers, DI, theme, login, signup, onboarding, client mode, call and scripts (118 files). Lane B covered chat, expert mode, settings and notifications (82 files). Finding IDs carry the lane letter.

# Lane A

# Phase 15 (lane A): Code Review Report

**Reviewed:** 2026-10-29
**Depth:** standard (diff vs 283f0109 plus full read of the new/changed non-test sources)
**Files Reviewed:** 118 (listed in `/tmp/p15-review-A.txt`; non-test sources read in full or by diff, tests and scripts spot-checked)
**Status:** issues_found

## Summary

Lane A covers the shared components, call overlay, call-resume marker, login/signup/onboarding and client-mode screens.

The headline Phase 15 invariants hold in the reviewed code:
- `SetNewPasswordViewModel` and `SignupVerifyEmailViewModel` no longer write the reset or verification code to `SavedStateHandle`.
- `DeclineReasonSaver` caps restored text.
- `CallResumeMarker` stores only the booking id and scheduled end, and never touches the call starter or the mic gate.
- Restored sheets and dialogs need a user tap to confirm.
- The silent versus loud `LoadMode` split is correct in `MyBookingsViewModel`.

No blockers. The main problems are:
- Two correctness gaps in the call-resume routing: it can hijack an explicit push-tap destination, and it can be consumed by a background process start that never reaches the UI.
- A TalkBack-correctness risk in `ValidatedField` and a live-region that does not fire for repeated identical counts.
- A design-rules gate that fails open.

## Warnings

### WR-A01: Call-resume routing can overwrite an explicit push-tap destination (race)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallResumeMarker.kt:62-74` (interacts with `MainActivity.kt:60-62` and `PushLaunchRequest.destination`)
**Issue:** `start()` runs from `Application.onCreate`. `resumeStoredMarker()` reads DataStore asynchronously, then calls `pushLaunchRequest.destination.post(BookingDetail)`. `OneShotSlot.post` is "newest wins". On a cold start caused by a push tap, `MainActivity.onCreate` posts the user's tapped destination synchronously. If the marker coroutine finishes after that, the user's explicit tap is silently replaced by the call booking. If it finishes before, the tap wins, so the outcome depends on thread timing. The marker is also already cleared by then, so the loser is lost either way. A user who killed the app mid-call and then taps a chat push lands on the wrong screen intermittently.
**Fix:** Do not overwrite a pending destination.
```kotlin
// OneShotSlot: add postIfAbsent
fun postIfAbsent(value: T): Boolean = _pending.compareAndSet(null, value)
// CallResumeMarker.resumeStoredMarker():
if (!pushLaunchRequest.destination.postIfAbsent(PushDestination.BookingDetail(bookingId, PushAudience.Either))) return
notice.post(bookingId)   // only flag the note when the resume actually routed
```
Post the notice only after the destination was accepted, so a hijacked resume does not leave a stale pending notice either.

### WR-A02: The marker is consumed by a process start that never reaches the UI

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallResumeMarker.kt:62-75`, `ClickMeApplication.kt:46`
**Issue:** `callResumeMarker.start()` runs in `Application.onCreate`, which also runs when the process is spawned by an FCM message, the realtime service or another background component. `resumeStoredMarker()` clears the store before routing, then posts to an in-memory slot and notice. If the process is killed again before an Activity consumes the slot, the marker is gone and the user's next real launch does not resume the call. This is a common shape: after a mid-call kill, the next process start is often an FCM wake-up rather than the user. The code's own comment, "cleared before routing, so a crash cannot replay it", chooses loss over replay.
**Fix:** Defer consumption to when the UI consumes the destination. Either start the resume from the `MainActivity`/`ClickMeApp` first-composition path, or keep the marker in the store until the `BookingDetail` destination is actually consumed, and clear it there. Alternatively, only clear the store after the destination has been taken by the screen shell.

### WR-A03: `contentDescription` on the text field can shadow the typed value for TalkBack

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt:168-170`
**Issue:** Once the field has text, the placeholder is set as the node's `contentDescription` on the same node that carries `EditableText`. For nodes with an explicit content description, TalkBack generally speaks that description in preference to the node text. The user can end up hearing the label ("Email") instead of what they typed, which regresses reading back email or code values. The KDoc defers this to device-checklist row A6, but the change ships to every `ValidatedField` (login, signup, set password, verify code), so the risk is wide. The JVM `assertActionablesNamed` helper counts `ContentDescription + Text` as a name, so it cannot catch this.
**Fix:** Prefer a mechanism that does not replace the value. For example give the label a separate semantics path (`Modifier.semantics { hideFromAccessibility() }` on the decoration plus a `paneTitle`/`stateDescription` that carries the label, or `Modifier.semantics(mergeDescendants = true)` with the label `Text` kept composed but visually hidden when filled). At minimum, treat A6 as a release gate for this change rather than a deferred item, and keep a fallback ready.

### WR-A04: Time-slot count live region does not announce when the new day has the same count

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt:108-135, 211-226`
**Issue:** The announcement is a polite live region on the heading whose `contentDescription` is `"$title, $countText"`. TalkBack announces on a change of that text. Within a loaded month, tapping day A (5 times) then day B (5 times) produces an identical description, so nothing is announced. The comment promises "Choosing a day announces the result once". Conversely, `now` is a ticking input to `isSelectable`, so a slot expiring changes the count and re-announces unprompted mid-interaction.
**Fix:** Key the announcement on the selection event rather than the text. For example include the selected date in the description (`"$title, $dateLabel, $countText"`), or announce through `LocalView.current.announceForAccessibility` / a one-shot `LaunchedEffect(selectedDate)`. Freeze `now` for the count text, or only recompute it when `selectedDate` or the availability changes.

### WR-A05: Restored decline sheet and accept dialog are shown regardless of whether the proposal is still actionable

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCard.kt:111-172`
**Issue:** `showAccept` and `showDecline` are `rememberSaveable` and are rendered from `if (showAccept)` and `if (showDecline)`, not gated on `actionable` or on `submitting`. This phase makes the decline sheet and its typed reason survive process death (D-12), which widens the window. After a restore, the screen reloads. If the proposal was withdrawn, accepted elsewhere or became non-actionable in the meantime (a realtime `booking.update`), the sheet still shows. The user can then confirm a Decline or Accept against a stale proposal. The server rejects it, but the user sees a confusing error. No auto-submit occurs, so this is a robustness defect rather than a trigger bug.
**Fix:**
```kotlin
if (showAccept && actionable && submitting == RescheduleSubmitting.None) { AcceptDialog(...) }
if (showDecline && actionable && submitting == RescheduleSubmitting.None) { DeclineSheet(...) }
```
Reset the flags in a `LaunchedEffect(actionable) { if (!actionable) { showAccept = false; showDecline = false } }`.

### WR-A06: The design-rules gate fails open, and rule (a) checks usage file-wide

**File:** `scripts/check-design-rules.sh:96-99, 129-132, 136-139 (the perl pipelines), 74-92`
**Issue:**
1. The perl invocations are written `hits=$(... | xargs -0 perl -e "$PERL_SCAFFOLD" -- || true)`. If perl is missing, the heredoc script has a syntax error, or xargs fails, the failure is swallowed and `hits` is empty, so the gate prints "Design rules: OK". CI would pass with the rules effectively disabled. The self-test only proves the happy path.
2. Rule (a) accepts a Scaffold as long as `padding(<name>`, `consumeWindowInsets(<name>` etc. appears anywhere in the file (`$src =~ /padding\(\s*$n\b.../`). A file with two Scaffolds that both name the parameter `innerPadding`, where only one applies it, passes. A Scaffold written as `Scaffold(content = { ... })` is not matched at all.
**Fix:** Capture the perl exit status separately and fail on a non-zero status other than the intended hit output. For example, have perl print hits to stdout and `exit 2` on internal error, run it without `|| true`, and gate on `$?`. Restrict the "used" check to the Scaffold's own lambda body by matching braces from `$+[0]`.

### WR-A07: A 404 on a quiet realtime/resume refetch with content showing produces a Retry snackbar that can never succeed

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt:194-212`
**Issue:** `isNotFound()` is only consulted in the non-quiet branch. If the booking is removed while the details are showing, a realtime `BookingUpdate` triggers a quiet reload. It fails with 404 and raises `refreshFailed`, so the user gets "Couldn't refresh" with Retry. Retry hits the same permanent 404 forever, which the code's own comment calls the loop to avoid ("a retry would loop on a permanent condition"). A second gap is that `startLoad(quiet = true, flagFailure = false)` from `refreshQuietly` cancels an in-flight user Retry. If that silent reload then fails, nothing is flagged, so a user who tapped Retry sees the progress bar vanish with no feedback.
**Fix:** Apply the 404 mapping in both branches:
```kotlin
catch (e: ApiException) {
    if (e.isNotFound()) { _uiState.value = UpcomingBookingUiState.Unavailable; return@launch }
    ...
}
```
In `refreshQuietly`, do not cancel a running Retry: return early when `(_uiState.value as? Loaded)?.isRefreshing == true`.

## Info

### IN-A01: Unused `SavedStateHandle` constructor parameter kept only for a test

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModel.kt` (constructor, `@Suppress("unused") private val savedStateHandle`)
**Issue:** The dependency is injected and never read or written, solely so tests can assert nothing is saved. This is dead code that needs a suppression, and a future edit can quietly start using it again.
**Fix:** Drop the parameter and have the test construct the VM without a handle. Keep the "secrets not saved" assertion by verifying the handle created by the Hilt/`SavedStateHandle()` test factory stays empty, or enforce it with a lint or grep gate.

### IN-A02: The one-time call-resume note persists through every reload and after rejoining

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt:83-86, 194`
**Issue:** `showCallResumeNote` is a constant captured at construction and copied into every `Loaded` state. The note "You were on a call" stays visible above Join Call for the whole VM lifetime, including after the user rejoins and returns to this screen. Behaviour is "once per screen instance", not "once".
**Fix:** Make it a `MutableStateFlow<Boolean>` and clear it when `join()` is called, or after the first screen-shown announcement.

### IN-A03: Focus request in `BookingRequestSentHeadline` runs while the heading is still alpha 0 and is not guarded

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt` (the `LaunchedEffect(Unit)` block), with `BookingRequestSentScreen.kt:232`
**Issue:** The headline's `fadeSlideUp` progress starts at 0, so it is transparent when the request runs. Transparent nodes are excluded from the accessibility tree, so TalkBack may never land on it, which is the opposite of the intent. Unlike `MeetingCallInfo`, the call is not wrapped in `runCatching`, and `focusRequested` is set to true before the request, so a failure is never retried.
**Fix:** Request focus after the headline's progress reaches 1 (or `snapTo`), or wrap in `runCatching` and set the saved flag after success. Track this in the device checklist.

### IN-A04: Non-localizable string concatenation in a content description

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt:128-133`
**Issue:** `contentDescription = "$title, $countText"` hardcodes the separator and the word order.
**Fix:** Use a format string resource, for example `stringResource(R.string.make_a_booking_slots_a11y, title, countText)`.

### IN-A05: Status-line focus is re-requested on every composition of `MeetingCallInfo`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallInfo.kt:150-155`
**Issue:** The effect is keyed on `hasStatus`. It also re-fires when the `AnimatedContent` variant changes, because that composes a fresh `MeetingCallInfo` (for example SkypeZoom to InApp after the join response). TalkBack focus then jumps back to the status line mid-call. The KDoc says focus moves once.
**Fix:** Hoist the "already focused" flag into `MeetingCallScreen` (`rememberSaveable`) or key it on the call/session id.

### IN-A06: A debug gallery screen keeps a password in saveable state (outside this lane's changed files)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/GalleryControlsSection.kt:118`
**Issue:** `var password by rememberSaveable { mutableStateOf("") }` writes a typed value to the saved-state bundle. It is a debug-only gallery and the value is not a real credential, but it contradicts the D-09 "no secrets in saved state" rule and a future grep gate would flag it.
**Fix:** Use `remember`, or confirm the gallery is stripped from release builds.

---

_Reviewed: 2026-10-29_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

# Lane B

# Phase 15 (lane B): Code Review Report

**Reviewed:** 2026-10-29
**Depth:** standard (diff-focused against 283f0109)
**Files Reviewed:** 82
**Status:** issues_found

## Summary

Lane B covers chat, expert mode, notifications and settings. The main Phase 15 mechanisms hold up:

- **Secrets and SavedStateHandle.** The TIN and the financial account identifier are never written to the handle: `TaxProfileField.savedStateKey()` returns null for both, and `tinNeverInSavedState` checks this. The delete-account password is not mirrored either. Only Strings, Booleans and string lists are stored.
- **Server hydration.** Restored values are overlaid on the fetched profile inside `seed` or `load`, so they win over a late fetch.
- **Restoration and destructive actions.** Restoring does not trigger cancel, delete or logout. Delete Account restores only the step, the reason and the comments.
- **LoadMode.** `Realtime` is handled consistently across the Chat, Conversations, Dashboard, Bookings and Notifications view models. Silent tab-return reloads stay `Quiet`, and a first load stays `Loud`.
- **Snackbar deduplication.** `showRefreshFailed` deduplicates by message, so repeated failures do not stack.

I found no critical defects. There are six warnings and five info items. They are mostly TalkBack and focus behaviour that fires in cases the plans did not consider, a snackbar that can end up under the keyboard, and one silent data revert in the tax form restore.

## Warnings

### WR-B01: Refresh-failed snackbar (with the new Retry action) is not lifted above the IME in Chat Thread and Conversations

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt:240` (also `chat/conversations/ConversationsScreen.kt`, the `snackbarHost = { SnackbarHost(snackbarHostState) }` slot)
**Issue:**
- Phase 15 deliberately added `Modifier.navigationBarsPadding().imePadding()` to the snackbar host on Availability and Tax Profile ("R9"). Chat Thread and Conversations did not get it.
- Material3 `Scaffold` offsets the snackbar by `contentWindowInsets.bottom`, which is system bars only and excludes the IME.
- A Chat Thread realtime reload (`LoadMode.Realtime`, a reconnect or a booking event) now raises the snackbar while the user is typing, so the keyboard is up. The snackbar and its Retry action then render under the keyboard and overlap the composer.
- Conversations has the same problem while the search field has focus.
- This defeats the point of the Retry standard (D-05) in the places where realtime failures are most likely.

**Fix:**
```kotlin
snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding().imePadding()) },
```
In ChatThread the composer already sits above the keyboard, so lift the host by the composer height. Alternatively, put the `SnackbarHost` inside the content `Column` above `ChatComposer` instead of in the Scaffold slot.

### WR-B02: PeerArrivalAnnouncer re-announces an old peer message when a trailing item of mine disappears, and drops back-to-back identical messages

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt:327-348`
**Issue:**
- The announcer detects an arrival as "newest key differs from `lastNewestKey` and newest is a peer message".
- The newest key can revert to an already-heard peer message without any new arrival. The sequence is: peer message P is newest, then I send M (newest = M, recorded, silent), then M fails and I Delete the failed bubble. Newest becomes P again, with key P != M, so P is spoken a second time.
- The same applies after a deletion or reconcile that removes the trailing item.
- Returning from a pushed screen can also replay the newest peer message. `lastNewestKey` is `rememberSaveable`, but `items` may have moved on while away.
- Separately, two peer messages with identical text arriving within `ANNOUNCEMENT_MS` set `announcement = ""` and then the same string in the same synchronous block. No state change reaches composition, so the second message is not announced.

**Fix:** Track the set or highest `createdAt` of peer message keys already announced, instead of only the last newest key. For example, announce only when `newest` is a peer message whose `createdAt` is strictly greater than a remembered `lastAnnouncedAt`.
```kotlin
var lastAnnouncedAt by rememberSaveable { mutableStateOf<Long?>(null) }
// announce only if newest is a peer Message and newest.createdAt.toEpochMilli() > (lastAnnouncedAt ?: Long.MAX_VALUE)
```
For identical consecutive texts, force a recomposition between the clear and the set, for example with `yield()` or a tick counter in the node key.

### WR-B03: Change-password error focus jumps again after rotation or back-stack return

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/subviews/AccountSecurityCards.kt:68-77`
**Issue:**
- `shownErrors` is a plain `remember`, but `state.fieldErrors` lives in the ViewModel and survives configuration change.
- After a rotation, a fold or a return from the back stack with an error still displayed, the `LaunchedEffect(state.fieldErrors)` runs on a fresh composition with `shownErrors = emptySet()`. It treats the existing error as new and calls `requestFocus()`.
- Focus is stolen from wherever the user was, and the IME opens. For TalkBack users this also moves the accessibility focus unexpectedly.
- This contradicts the stated rule that focus must only move for a newly failing field.

**Fix:** Make `shownErrors` survive recreation, or seed it with the current errors on first run, so initial composition never requests focus.
```kotlin
var shownErrors by rememberSaveable { mutableStateOf(state.fieldErrors.keys.map { it.name }) }
// or: skip the request on the first effect run by seeding shownErrors = state.fieldErrors.keys in remember
```

### WR-B04: Tax Profile restore silently reverts an edited payout account to the server value while keeping the other restored edits

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileViewModel.kt:169-189`
**Issue:**
- `restoredOver` overlays only the eight non-secret fields. `financialAccount` comes back as the server's value (`data.toForm()`), not empty, and there is no indication that it was reset.
- Scenario: an expert changes their payout account and the legal name, the process dies, and the form returns. The name is the edited one, but the account field shows the old server account.
- Saving then submits the old account together with the edited identity data. Nothing tells the expert the bank change was discarded.
- The test `restoredTinIsEmptyWithoutError` only checks `financialAccount == ""` against a fixture where the server has none, so it does not cover this case.
- The class doc and the tests say secrets come back "empty with no error text". That holds for the TIN but not for the account identifier when the server already holds one.

**Fix:** When the handle is touched and restored, give the user a cue that the secret fields were not restored. Options:
- Set a one-shot hint or banner such as "For your security, please re-enter your account and TIN".
- Or, when `touched` is true, blank `financialAccount` so the field validation forces re-entry.

Add a test with a server-side account plus a touched handle.

### WR-B05: A quiet refetch that returns 404 on Expert Booking Summary produces a permanent "Couldn't refresh / Retry" loop over stale content

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt:222-231`
**Issue:**
- The new `Unavailable` mapping for 404 is applied only in the non-quiet branch.
- If the booking is removed while the screen shows it, the realtime `booking.update` triggers `startLoad(quiet = true, flagFailure = true)`. A 404 then goes down the quiet branch, which keeps the stale `Loaded` content, raises `refreshFailed`, and shows a snackbar with Retry.
- Retry runs `refresh()`, which gets another 404, flags again, and so on. The user stays on a booking that no longer exists, with Join and Cancel still enabled.

**Fix:** In the quiet branch, map a not-found to `Unavailable` as well.
```kotlin
} catch (e: ApiException) {
    if (e.isNotFound()) {
        _uiState.value = ExpertBookingSummaryUiState(ExpertSummaryLoad.Unavailable, showCallResumeNote)
    } else if (quiet) { /* existing */ } else { /* existing Failed */ }
}
```

### WR-B06: Booking Confirmed re-grabs focus on every recreation, not "once, on entry"

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreen.kt:171-173`
**Issue:**
- `LaunchedEffect(Unit) { runCatching { headlineFocus.requestFocus() } }` re-runs whenever the composable re-enters composition: rotation, fold, locale or font-scale change, or a return from the back stack.
- Each time it moves input and TalkBack focus back to the headline. A user who had moved to "Done" or the calendar action loses their position.
- The comment says "once, on entry".

**Fix:** Gate it with saved state.
```kotlin
var focused by rememberSaveable { mutableStateOf(false) }
LaunchedEffect(Unit) {
    if (!focused) { focused = true; runCatching { headlineFocus.requestFocus() } }
}
```

## Info

### IN-B01: Side effects inside `MutableStateFlow.update` lambdas

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileViewModel.kt:212` and `:266`
**Issue:**
- `mirror(field, next)` and `savedStateHandle[KEY_TOUCHED] = false` run inside `_uiState.update { }`. That lambda is re-executed on CAS contention.
- The writes are idempotent today, so there is no bug. The pattern is fragile, though: any non-idempotent edit would double-apply.

**Fix:** Compute `next` outside the lambda, or call `mirror` after `update` returns.

### IN-B02: Profile Settings restore auto-PATCHes a restored draft with no user action, and the touched flag is never cleared

**File:** `ClientProfileSettingsViewModel.kt:195-210` and `ExpertProfileSettingsViewModel.kt:241-258`
**Issue:**
- `if (snapshot != server) saver.onEdited(snapshot)` sends the restored draft after the debounce, with no keystroke. This is documented and matches pre-death intent.
- `KEY_TOUCHED` and the per-field values are never reset after a successful save, unlike Tax Profile. If the server data changed elsewhere before a later recreation of the same back-stack entry, the stale edited fields are re-applied and PATCHed over it.
- A restored cleared required field such as first name is also sent, exactly as a live clear would be. There is no blank guard on either path.

**Fix:** Reset the mirrored keys (or `KEY_TOUCHED`) in the save-success callback once `saver.status` reaches `Saved` and `latest == saved`, as Tax Profile does.

### IN-B03: Five added lines exceed the 120-column `.editorconfig` limit

**File:**
- `ChatThreadViewModel.kt` (companion KDoc, around line 586)
- `ConversationsViewModel.kt:64` and `:156`
- `AvailabilityScreen.kt:190`
- `ExpertDashboardViewModel.kt` (class KDoc, the "stay silent, while the pull and the realtime reload flag the failure so the snackbar offers Retry" line, 165 characters)

**Issue:** They violate `max_line_length = 120` and will fail ktlint if the style check covers comments.
**Fix:** Re-wrap the comments.

### IN-B04: `contentMaxWidth()` is applied before `verticalScroll` in TopicEditor, unlike every other screen

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorScreen.kt:236-238`
**Issue:**
- Here the order is `.imePadding().contentMaxWidth().verticalScroll(...)`. On a large window the scrollable surface is narrowed to 600 dp, so touches in the side gutters do not scroll.
- The other screens put the cap after `verticalScroll`, so the scroller spans the window.

**Fix:** Move `.contentMaxWidth()` after `.verticalScroll(rememberScrollState())`.

### IN-B05: Bubble semantics may read the message text twice (verify on device)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/MessageBubble.kt:122-123`
**Issue:**
- The merged bubble node now sets `contentDescription = "{speaker}: {text}"`, while the descendant `Text` still contributes its own text to the merged node.
- Compose concatenates content descriptions, and how TalkBack treats a node carrying both a content description and text varies by TalkBack version.
- The unit tests assert only that the content description contains the expected strings, so they do not prove single speech.

**Fix:** If device testing shows duplicated speech, wrap the body `Text` in `clearAndSetSemantics {}` (or `Modifier.semantics { invisibleToUser() }`) so the spoken string comes only from the container.

---

_Reviewed: 2026-10-29_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
