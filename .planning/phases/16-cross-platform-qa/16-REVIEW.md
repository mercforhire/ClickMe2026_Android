---
phase: 16-cross-platform-qa
reviewed: 2026-11-05T00:00:00Z
depth: standard
files_reviewed: 15
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEventMapping.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffold.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/payments/ClientTransactionRow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertPastCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/splash/SplashScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/ClickMePaymentSheetStyle.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeFailureLog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeResultMapping.kt
findings:
  critical: 0
  warning: 3
  info: 5
  total: 8
status: issues_found
---

# Phase 16: Code Review Report

**Reviewed:** 2026-11-05
**Depth:** standard
**Files Reviewed:** 15
**Status:** issues_found

## Summary

Reviewed the Phase 16 fix commits (BUG-05..BUG-11 batch A, BUG-12, BUG-29/30/31 batch B) at the changed hunks and their surroundings. I did not find a blocking defect. The two money-path fixes (BUG-05 and BUG-06) hold up. I checked them against the Stripe 23.24.0 sources in the Gradle cache (`DefaultFlowController.kt`):

- `PaymentOptionResult(null, false)` is sent only from `onPaymentResult` on `Completed`, just before the result callback. Every other option callback uses `didCancel = canceled`.
- The FlowController registers its launcher in `init`, so a pending option result is replayed during `build()`. That is the BUG-06 mechanism, and `ConfirmGate` handles it correctly.
- `createPaymentOption` is non-null, so a real selection always carries an option.

The warnings are an accessibility regression from the BUG-29 rows, an inert min-height on the BUG-30 button, and a test gap that explains how BUG-22 and BUG-34 got through. The Info items are lower-risk robustness notes, plus the BUG-34 evidence you asked for.

## Warnings

### WR-01: BUG-29 booking ID is invisible to TalkBack on two of the three rows

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/payments/ClientTransactionRow.kt:79-90` and `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertPastCard.kt:100-101`
**Issue:** Both rows set an explicit `contentDescription` on the clickable node. For `ClientTransactionRow` it is `semantics(mergeDescendants = true) { contentDescription = a11y }`. For `ExpertPastCard` it is the `description` set in the card's `semantics {}`. That description replaces the text of the merged children. The new `ID: #XXXXXXXX` Text (`ClientTransactionRow.kt:115`, `ExpertPastCard.kt:152`) is therefore visible on screen but never spoken. The row KDoc still promises a single node reading "{topic}, {expert}, {date}, {status}, {amount}". The point of BUG-29 is quoting or finding a booking by ID, which a TalkBack user cannot do from the row. `ExpertUpcomingCard` sets no description override, so it is fine.
**Fix:** Append the ID to the a11y string for both rows. For example, add a `%6$s` argument to `payment_history_row_a11y` and the refund variant, and an `expert_bookings_past_a11y` argument. Pass `shortBookingId(item.bookingId)` or `expertShortBookingId(...)`. Update the KDoc order. Extend the existing a11y assertions to cover it.

### WR-02: Overview "Next" button never reaches its 56 dp minimum (modifier order)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreen.kt:168-172`
**Issue:** The chain is `.contentMaxWidth().heightIn(min = 56.dp).navigationBarsPadding().padding(start, end, bottom = Spacing.lg)`, applied to the Button. `heightIn` comes first, so it constrains the outer box, including the nav-bar inset and the 24 dp bottom padding. The button itself only gets `56 - lg - inset`. `PressScaleButton` then applies its own `heightIn(min = 48.dp)` last, so the button is 48 dp tall, not the 56 dp the code says. This predates BUG-30 (the 56 already lost 24 dp to padding). BUG-30 makes it fully inert, because on a 3-button bar (about 48 dp) the whole 56 dp budget is consumed by insets and padding.
**Fix:** Put the min height after the insets and padding so it applies to the button:
```kotlin
Modifier
    .contentMaxWidth()
    .navigationBarsPadding()
    .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg)
    .heightIn(min = 56.dp)
```
If 48 dp is the intended size, delete the dead `heightIn`.

### WR-03: The call-screen tests no longer prove the controls can be reached, which is how BUG-22 and BUG-34 got through

**File:** `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreenTest.kt:234-257` (exercises `MeetingCallScreen.kt:107-170`)
**Issue:** The BUG-10 commit changed the 200% font test from `performScrollTo` on End Call to `assertIsDisplayed()` on End Call alone. Nothing asserts that Mute, Speaker, Minimize and Chat can be scrolled into view above the pinned End Call, or that the scroll column scrolls at all. The pinned End Call can pass while the controls are unreachable, which is exactly BUG-22 and BUG-34. Fixing those two bugs without such a test would repeat the gap.
**Fix:** Add a test at about 340x750 dp at fontScale 2 and at 1.0 with the banner showing. It should run `performTouchInput { swipeUp() }` on `MEETING_CALL_SCREEN_TAG` and assert that Mute, Speaker, Minimize and Chat each become displayed and sit above the End Call top. Also assert that a swipe changes the controls' position, so a non-scrolling column fails.

## Info

### IN-01: BUG-34 root cause evidence (the column should scroll, but does not)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt:107-155`, `.../subviews/MeetingCallAvatar.kt:114-123`, `.../CallOverlayHost.kt:215-223`
**Issue:** I could not find the cause by reading the code alone, but the layout shows what it is not. From `logs/batch-B/FS.C3-200pct.png`:
- The scroll viewport is the weighted `BoxWithConstraints` (about 0 to 1328 px, ending exactly at the top of End Call). The clip line at "Waiting for other" matches that.
- Content is much taller than the viewport. `MeetingCallAvatar` reserves a 360 dp tall hero because its glow Box uses `requiredSize(glowSize)`. The photo shows about 250 px of empty space between the avatar and the name. At 200% font the name, topic, time, countdown and status wrap to several lines, and the four controls follow.
- By Compose's measure rules (`verticalScroll` measures its child with infinite height; `heightIn(min = maxHeight)` only sets a minimum), `maxValue = contentHeight - viewportHeight` should be positive. The same structure scrolled at font 1.0 (BUG-22). So the structure is not a self-evident cause.
- The overlay's catch-all `pointerInput` (`CallOverlayHost.kt:220-224`) consumes in the Main pass. That runs after the child, so it should not block the drag. I cannot rule out an interaction with the `AnimatedVisibility` slide layer without a device.

Candidates to check next, in order:
1. Log or test `scrollState.maxValue` at 340 dp, fontScale 2. If it is 0, the content is being measured at the viewport height. Look at `AnimatedContent` and its `SizeTransform` clip.
2. Drive a real `swipeUp()` through `CallOverlayHost`, not `MeetingCallScreen` alone. The existing tests never do.
3. Whatever the cause, the 360 dp hero is why the controls sit so far down. BUG-22's suggested fix (shrink the avatar and glow while the banner shows) also fixes this, and `glowSize` should not use `requiredSize` in layout.

**Fix:** See WR-03 for the test that would have caught this.

### IN-02: The BUG-05 reset-ignore rule depends on an SDK behavior pinned only by a comment

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeResultMapping.kt:51-63`, `.../makeabooking/PaidFlowStripeHost.kt:56-69`
**Issue:** `isPostCompletionReset` treats every `didCancel = false` with no option as the post-completion reset, and takes no action. The code has no fallback if `Completed` does not follow, so the flow would sit on `CollectingPaymentMethod` with the spinner running. I verified this holds in 23.24.0 (see Summary). The only guard on an SDK upgrade is the KDoc line "recheck on an SDK upgrade".
**Fix:** Add a unit test that reads the Stripe version from `libs.versions.toml` or the resolved dependency and fails when it differs from 23.24.0, with a message pointing at this function. Optionally, make the ignore time-boxed: if no sheet result arrives within a few seconds, report `Canceled`.

### IN-03: `ConfirmGate.runPending` confirms without re-checking the flow step, and `bind` runs in the composition body

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt:43-47, 130-132`
**Issue:** `routePaymentOptionResult` checks `paidStep == CollectingPaymentMethod` when the replayed callback fires inside `build()`. `runPending` later runs `confirm` from a `LaunchedEffect` without checking again. If the step changed in between (for example a reset), a SetupIntent confirm would still run and save the card, and `onSheetOutcome` would then ignore `Completed`. The window is about one frame, so the risk is low. Separately, `gate.bind(flowController::confirm)` is a side effect on a plain field during composition, not in `SideEffect`. It is idempotent, so it is harmless today.
**Fix:** Give `runPending` a guard, for example `runPending { latestStep == PaidStep.CollectingPaymentMethod }`, and wrap `bind` in `SideEffect`, taking care to run it before the effect that calls `runPending`.

### IN-04: The BUG-09 KDoc overstates "changes nothing" for repeated decoding events

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEventMapping.kt:37-47`, `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt:310-340`
**Issue:** The doc says a repeat for a peer who is already Ready changes nothing. The session state is indeed unchanged, and `StateFlow` conflates equal values. But `onEngineEvent` still calls `applyConnStates`, which calls `confirmConnectionOnce()` whenever the status is Active. If `PATCH confirm-connection` keeps failing (409 or a network error), every `DECODING` transition, such as a peer muting and unmuting, now triggers another PATCH attempt. Before the change this only happened on real Active transitions. At most one request is in flight, so it is bounded but is new churn. The other residual (audio decoding just after `call.ended` while the peer is still in the channel) is already acknowledged in the BUG-09 log row.
**Fix:** Correct the KDoc. Optionally, in `onEngineEvent`, skip `applyConnStates` when `RemoteJoined` arrives and `remote` is already Ready.

### IN-05: Tab labels shrink below the user's font choice and clip silently if they still do not fit

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffold.kt:80-96`
**Issue:** With `softWrap = false`, `maxLines = 1` and the default `TextOverflow.Clip`, a label that still does not fit at the 10 dp floor (a longer translation or a sixth tab) is cut off with no visual cue. The floor is also an accessibility trade-off: at 200% font the label renders at 10 dp, smaller than the user asked for. That is probably acceptable for a five-tab bar, but the Direction 2 re-test the bug log mentions should cover the expert Dashboard label.
**Fix:** Add `overflow = TextOverflow.Ellipsis` so truncation is visible, and keep the floor documented as a deliberate trade-off.

---

_Reviewed: 2026-11-05_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
