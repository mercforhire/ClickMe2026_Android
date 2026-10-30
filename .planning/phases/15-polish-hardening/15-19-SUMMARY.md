---
phase: 15-polish-hardening
plan: 19
subsystem: accessibility
tags: [booking, ime, focus, large-screen, tracer]
status: complete
requires: [15-02, 15-07]
provides:
  - "Request Sent headline takes focus once on entry (FocusRequester + focusable, saved flag)"
  - "Make a Booking form column and Request Sent content column capped at 600 dp (scroll area and full-bleed parts stay full width)"
  - "MakeABookingInsetsTest (Book Now above a 300 px IME, 600 dp cap) and three Request Sent cases"
affects: [15-17]
key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingInsetsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreenTest.kt
key-decisions:
  - "Book Now is NOT pinned: the plan's premise of an existing pinned footer is wrong (Book Now is the last item of the scrolling form, as on iOS). Kept the structure and the Scaffold/imePadding handling unchanged; Book Now is reachable above the keyboard by scrolling."
  - "Headline focus is requested once per saved instance (rememberSaveable), so a restore does not steal focus again"
requirements-completed: [POL-03, POL-04]
plan_head_before: 756875107864d20c272bd919c8a7a5cb94f3ba1b
plan_head_after: 0e9fb483e3b136993ed437c228e3c896dedbcb87
commits: 1
actuals:
  tokens: 6000
  tasks: 1
  commits: 1
duration: ~1h wall clock (one 4 min build)
completed: 2026-10-29
---

# Phase 15 Plan 19: Booking footer, Request Sent focus and large-window caps Summary

Request Sent now leads TalkBack to its "Request Sent!" heading, and Make a Booking and Request Sent cap their content at 600 dp on large windows; the booking restoration code is untouched.

## What was built

- **BookingRequestSentHeadline:** the heading Text gets `focusRequester` + `focusable()` (keeps `heading()`). A `LaunchedEffect(Unit)` requests focus once; a `rememberSaveable` flag stops it requesting again after recreation or restore.
- **BookingRequestSentScreen:** the scroll Column stays full width; an inner column (`contentMaxWidth()`, tagged `REQUEST_SENT_CONTENT_TAG`) holds the icon, headline, details card, lifecycle card and actions.
- **MakeABookingScreen:** the scroll Column (with `imePadding()`, tap-to-clear-focus, drag detection and `MAKE_A_BOOKING_SCROLL_TAG`) stays full width; the form column inside it uses `contentMaxWidth()`. The zero-inset Scaffold and its `imePadding()` are unchanged (R1 exception, 15-03 rule b).
- No change to `MakeABookingContent`'s parameters, so `BookingTalkBackFlowTest` needed no edit.

## Tests

- `MakeABookingInsetsTest` 2/2 (new): `bookNowAboveImeWhileTypingNotes` (w360dp-h800dp, IME 300 px, Notes focused, Book Now displayed with bottom <= 500 dp after scrolling into the IME-shrunk viewport), `formCappedOnLargeWindow` (w900dp-h600dp: topic row <= 600 dp and centered, scroll area still 900 dp).
- `BookingRequestSentScreenTest` 22/22 (3 new): `headlineFocusedOnEntry`, `headlineFocusIsNotRequestedAgainAfterRestoration`, `requestSentCapped` (content column and lifecycle card <= 600 dp, centered).
- Stay green: `EntrySavedStateHandleTest` 2, `MakeABookingScreenTest` 36, `BookingTalkBackFlowTest` 5.
- `ktlintCheck` clean; `bash scripts/check-design-rules.sh` prints "Design rules: OK".

## Deviations from Plan

**1. [Premise mismatch, no Rule 4 change made] There is no pinned Book Now footer**
- **Found during:** reading `MakeABookingScreen.kt` before editing.
- **Issue:** the plan, truth 1, key link and acceptance criteria assume a pinned footer ("pinned Book Now footer", "footer background stays full width", `contentMaxWidth()` count >= 2 in `MakeABookingScreen.kt`). Book Now (and the payment info link) is the last item of the scrolling form, matching iOS `MakeABookingView` (button inside the `ScrollView`). UI-SPEC R4 and the booking-footer inset test line assume a pinned footer.
- **What was done:** did not re-layout the screen. Pinning would move Book Now out of the scroll column, break about 25 existing `performScrollTo()` calls on `BOOK_NOW_TAG` / payment-link nodes in `MakeABookingScreenTest` and `BookingTalkBackFlowTest`, and diverge from iOS without a user decision. `contentMaxWidth()` is therefore used once in `MakeABookingScreen.kt` (acceptance expects >= 2; the second spot, the footer inner row, does not exist). The IME test follows the Login precedent (`LoginInsetsTest`): Notes focused with a 300 px IME, then scroll Book Now into the shrunk viewport and assert it is displayed above the IME top. It proves the form still scrolls to Book Now under the keyboard, not that Book Now stays visible without scrolling.
- **Decision for the user / planner:** if "Book Now visible above the keyboard without scrolling" is really wanted, add a follow-up that moves the button into the Scaffold `bottomBar` (`navigationBarsPadding()` then `imePadding()`, R3; `contentMaxWidth()` on the inner row, full-width background) and removes `performScrollTo()` on the pinned nodes in the two test classes.

**2. [Process] No separate RED commit.** Tests and implementation were written together and run once (builds are 4 to 40 min under parallel load), so there is no failing-first run for this `tdd="true"` task. All 5 new cases pass; the focus and cap assertions would fail on the base (headline unfocused, content column 900 dp wide).

**Total deviations:** 1 premise mismatch (documented, no code change beyond the plan scope), 1 process note. **Impact:** POL-04 (large-window cap) and the Request Sent focus are fully delivered; POL-03 for the booking form is met by reachability under the IME, not by a pinned bar.

## Hand-offs / open items (device checklist, nothing blocks on a person)

- TalkBack on entry to Request Sent: confirm it speaks "Request Sent!" first (input focus is set on the heading; the JVM proves focus state, not speech). The heading is alpha 0 for the first 150 ms of the entry animation.
- Make a Booking and Request Sent at 600 dp cap on a resizable emulator; resize during a booking recreates the activity (restoration covered by `EntrySavedStateHandleTest`).
- Notes field on the Samsung SM-A075F with the real keyboard: caret stays visible while typing, tap outside clears focus, drag dismisses the keyboard.
- Landscape phone (< 480 dp tall) with the keyboard open: the form viewport is small; the plan did not add the "scroll footer with content" variant because there is no footer.

## Known Stubs

None.

## Threat Flags

None. T-15-14 (restored notes) unchanged: the notes path was not touched and `EntrySavedStateHandleTest` stays green.

## Commits

- 0e9fb483 feat(15-19): cap booking screens on large windows and focus the Request Sent headline

## Self-Check: PASSED

`MakeABookingInsetsTest.kt` exists; commit 0e9fb483 is an ancestor of HEAD; the five test classes pass with 0 failures and `ktlintCheck` plus `check-design-rules.sh` are clean.
