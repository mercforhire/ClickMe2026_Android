---
phase: 15-polish-hardening
fixed_at: 2026-10-29T00:00:00Z
review_path: .planning/phases/15-polish-hardening/15-REVIEW.md
iteration: 1
findings_in_scope: 13
fixed: 11
skipped: 2
status: partial
---

# Phase 15: Code Review Fix Report

**Fixed at:** 2026-10-29
**Source review:** .planning/phases/15-polish-hardening/15-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 13 (0 critical, 13 warnings)
- Fixed: 11
- Skipped: 2

**Verification environment:** run in the main checkout on branch `main` (no worktree; direct commits were authorized for this run). Final gate `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` passed (exit 0). `scripts/check-design-rules.sh` and `scripts/check-design-rules-selftest.sh` also pass.

## Fixed Issues

### WR-A01: Call-resume routing can overwrite an explicit push-tap destination

**Files modified:** `managers/CallResumeMarker.kt`, `screens/expertmode/ExpertLaunchRequest.kt`, `managers/CallResumeMarkerTest.kt`
**Commit:** 53e9345d (shared with WR-A02, the two fixes rewrite the same function)
**Applied fix:** Added `OneShotSlot.postIfAbsent` (compare-and-set on null). The resume now posts with it, so a pending push-tap destination wins. The resume notice is posted only after the destination was accepted. When a tap is already pending the stale marker is cleared. New test `pendingPushTapIsNotOverwritten`.
**Status:** fixed: requires human verification (race/ordering logic)

### WR-A02: The marker is consumed by a process start that never reaches the UI

**Files modified:** same as WR-A01
**Commit:** 53e9345d
**Applied fix:** The stored marker is no longer cleared before routing. After posting, the marker waits (`pending.first { it != destination }`) until the screen shell consumes the destination or something replaces it, and only then clears the store. A background process start (FCM wake-up) that dies before the UI consumes the slot therefore keeps the marker for the next real launch. Session tracking starts after the wait, which is safe because a call cannot start without UI. Tests updated (`resumePostsBookingDetailAndNotice`, `neverJoins` consume the slot) and `markerSurvivesUntilTheDestinationIsConsumed` added.
**Status:** fixed: requires human verification (lifecycle logic)

### WR-A04: Time-slot count live region does not announce for a same-count day

**Files modified:** `screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt`
**Commit:** 5d8eedc1
**Applied fix:** The heading description is now `"$title, $dateLabel, $countText"`, so choosing a different day with the same count changes the text. The count is computed at a `now` captured with `remember` keyed on availability, selected date and window, so a slot expiring on the clock tick no longer re-announces unprompted. The IN-A04 string-resource suggestion was not part of scope and is untouched.

### WR-A05: Restored decline sheet / accept dialog shown for a non-actionable proposal

**Files modified:** `components/RescheduleResponseCard.kt`
**Commit:** ee076c36
**Applied fix:** The dialog and sheet render only while `actionable && submitting == None`, and a `LaunchedEffect(actionable)` resets both saveable flags when the proposal stops being actionable.

### WR-A06: The design-rules gate fails open, and rule (a) checks usage file-wide

**Files modified:** `scripts/check-design-rules.sh`, `scripts/check-design-rules-selftest.sh`
**Commit:** 2c043ded
**Applied fix:** The perl pipelines no longer end in `|| true`; a failure runs `internal_error` (exit 2, message on stderr, never "OK"). `PERL_BIN` is overridable so the self-test proves a missing perl exits 2. Perl `die`s on unreadable files instead of skipping. Rule (a) now brace-matches the Scaffold content lambda and checks the padding use only inside it, and it also handles `content = { name -> ... }`. New self-test fixtures: `a_two_scaffolds`, `a_named_content`, `a_named_content_ok`.

### WR-A07: A quiet 404 produces a Retry snackbar that can never succeed

**Files modified:** `screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt`, `UpcomingBookingViewModelTest.kt`
**Commit:** e2443e4d
**Applied fix:** A 404 maps to `Unavailable` in both the loud and quiet branches. `refreshQuietly` returns early while a user Retry is refreshing (`isRefreshing`), so a silent reload cannot cancel it. Two tests added.

### WR-B01: Refresh-failed snackbar not lifted above the IME in Chat Thread and Conversations

**Files modified:** `screens/chat/chatting/ChatThreadScreen.kt`, `screens/chat/conversations/ConversationsScreen.kt`
**Commit:** 633fe822
**Applied fix:** Neither Scaffold has zero insets, so the reviewer's `navigationBarsPadding().imePadding()` would have double-counted the nav bar. Instead the host moved into the content. Chat Thread: `SnackbarHost` sits in the content `Column` directly above the composer, so it clears the keyboard and the composer. Conversations: the content is a `Box` with `padding(innerPadding).consumeWindowInsets(innerPadding)` and a bottom-aligned `SnackbarHost(...imePadding())`. Needs a visual check on a device with the keyboard up.

### WR-B02: PeerArrivalAnnouncer re-announces an old peer message, drops identical back-to-back messages

**Files modified:** `screens/chat/chatting/ChatThreadScreen.kt`, `ChatThreadScreenTest.kt`
**Commit:** 0087213a
**Applied fix:** Replaced `lastNewestKey` with a saved `lastSeenAtMs` (newest `createdAt` seen). Only a newest item strictly newer than it can be an arrival, so deleting a failed send of mine no longer re-speaks the peer message before it. When an announcement is still showing, the node is cleared and two frames (`withFrameNanos`) pass before the new text is set, so identical consecutive texts are a real change. Test `deletingMyFailedSendDoesNotReAnnounceThePeerMessageBeforeIt` added. The identical-text part is not covered by a JVM test.
**Status:** fixed: requires human verification (TalkBack announce timing)

### WR-B03: Change-password error focus jumps again after rotation or back-stack return

**Files modified:** `screens/settings/accountsecurity/subviews/AccountSecurityCards.kt`
**Commit:** b10b1e41
**Applied fix:** `shownErrors` is seeded with the errors already in `state.fieldErrors`, so the first effect run after recreation never treats an existing error as new.

### WR-B04: Tax Profile restore silently reverts an edited payout account to the server value

**Files modified:** `screens/expertmode/taxprofile/ExpertTaxProfileViewModel.kt`, `res/values/strings_tax_profile.xml`, `ExpertTaxProfileViewModelTest.kt`
**Commit:** 70618d46
**Applied fix:** Took the reviewer's first option. When a touched handle is restored over the server data, the existing info banner shows the new string `tax_profile_secrets_not_restored` ("... payout account and TIN are not kept when the app is closed. Check them and enter again any you changed."). The account field keeps showing the server value. Test `restoredTinIsEmptyWithoutError` now expects the banner; tests added for the server-account case and for an untouched load (no banner). Copy is new and may want a product look.

### WR-B05: A quiet 404 on Expert Booking Summary loops "Couldn't refresh / Retry"

**Files modified:** `screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt`, `ExpertBookingSummaryViewModelTest.kt`
**Commit:** 349e5b64
**Applied fix:** A 404 maps to `Unavailable` in both branches (small `withLoad` helper keeps lines under 120 columns). Test added.

## Skipped Issues

### WR-A03: `contentDescription` on the text field can shadow the typed value for TalkBack

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt:168-170`
**Reason:** Needs on-device TalkBack verification and a product/mechanism choice. How TalkBack speaks a Compose `BasicTextField` node that carries both `contentDescription` and `EditableText` cannot be determined on the JVM. The review's own recommendation is conditional ("at minimum treat A6 as a release gate, keep a fallback ready"). It is already a device-checklist row (`15-DEVICE-CHECKLIST.md` L3, with a conditional fix row: "add the name to the field's semantics without hiding the value"). Changing the mechanism blind risks a worse regression across every `ValidatedField`. Action for the owner: run L3 on the Samsung device; if only the label is spoken, apply the fallback.
**Original issue:** The placeholder is set as `contentDescription` on the same node as `EditableText`, which may make TalkBack read the label instead of the typed value.

### WR-B06: Booking Confirmed re-grabs focus on every recreation

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreen.kt:171-173`
**Reason:** Contradicts a deliberate, tested design decision. `BookingConfirmedScreenTest.headlineFocusedAgainOnRecreation` (added in plan 15-14) asserts "A recreated screen is a new entry, so the headline takes focus again". A saved "already focused" flag made that test fail, and it was rolled back (`git checkout`). Activity recreation (rotation, fold, font scale) also resets focus and TalkBack position anyway, so there is no preserved position to protect. If the owner wants once-per-entry semantics across recreation, the test and the UI-SPEC rule must change together.
**Original issue:** `LaunchedEffect(Unit) { requestFocus() }` re-runs on recreation and returns focus to the headline.

---

_Fixed: 2026-10-29_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
