---
phase: 06-booking-flow-free-bookings
verified: 2026-10-22T20:05:00Z
status: gaps_found
score: 2/3 must-haves verified
overrides_applied: 0
gaps:
  - truth: "From Expert Profile, the client picks a topic, a day on the custom month calendar, a slot, a meeting type and optional notes. (SC-1 / BOOK-01, notes part)"
    status: partial
    reason: "CR-01 is confirmed by a throwaway Robolectric test. MakeABookingScreen clears focus whenever scrollState.isScrollInProgress is true, which includes programmatic scrolls. When the viewport shrinks (the same thing imePadding does when the keyboard opens) the notes field loses focus. With the clearFocus effect disabled the same test passes, so the effect is the cause. On a device the keyboard should close as soon as it opens, so the client can't reliably type the message."
    artifacts:
      - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt"
        issue: "Lines 122-124: LaunchedEffect(scrollState.isScrollInProgress) { if (scrollState.isScrollInProgress) focusManager.clearFocus() } fires on bring-into-view and IME-resize scrolls."
    missing:
      - "Clear focus only on a user drag, e.g. scrollState.interactionSource.collectIsDraggedAsState(), or a NestedScrollConnection that checks source == UserInput."
      - "Add a Robolectric regression test: focus NOTES_FIELD_TAG, shrink the viewport, assertIsFocused()."
human_verification:
  - test: "SC-1 Booking form on the Samsung SM-A075F (devDebug, production backend, client login): Expert Profile -> Book Session, then topic sheet (paid topics listed with prices; a paid topic disables Book Now with the coming-soon note), day (past days and earlier months blocked; dots on open days), slot, meeting type, notes (500 cap, counter, keyboard does not cover Book Now)."
    expected: "All selections work. The notes field keeps focus and the keyboard stays open (re-check after the CR-01 fix)."
    why_human: "Real IME and layout; the user deferred device testing (STATE.md [Phase 6] DEFERRED)."
  - test: "SC-2 Cross-timezone: set the iOS expert's timezone to something different from the device (e.g. Pacific/Auckland) with slots near midnight at month edges."
    expected: "Each slot is under the correct local date with no duplicates across neighbouring months. The timezone note names the expert city and abbreviation. A slot held from iOS is dimmed and not selectable."
    why_human: "Needs the live backend and an iOS expert; the unit tests only use golden data."
  - test: "SC-3 Free booking end to end: book a free topic."
    expected: "Request Sent loads the details (Awaiting Approval). The iOS expert gets the booking_request push and sees the request. Back lands on Expert Profile. View My Bookings selects Bookings. Back to Home returns to Explore. A Skype/Zoom booking with an expert who has no link shows the D-08 alert and snaps back to In-app Voice. A slot taken from another account shows the D-09 alert and the slot turns dimmed."
    why_human: "Cross-platform delivery (iOS receives the request) and the push can't be checked from the codebase."
  - test: "Visual and system: 200% font scale on both screens; rotation and 'Don't keep activities'; animations off; system Back during an in-flight booking; topic sheet state across rotation; prodRelease R8 smoke."
    expected: "No clipping. State survives. The Request Sent animation doesn't replay. Back is ignored while submitting."
    why_human: "BackHandler and rememberSaveable aren't JVM-tested, and the R8 smoke needs the deferred Phase 2 keystore."
---

# Phase 6: Booking Flow & Free Bookings Verification Report

**Phase Goal:** A client can pick a topic and a correct local-time slot, then send a free booking request that reaches the expert.
**Verified:** 2026-10-22
**Status:** gaps_found (one confirmed blocker, CR-01; device checks deferred by the user and listed separately)
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | From Expert Profile, the client picks a topic, a day on the custom month calendar, a slot, a meeting type and optional notes. | PARTIAL / FAILED (notes) | Everything except typing the notes is wired and substantive. See "Wiring" below. CR-01 is confirmed with a test (see Behavioral Spot-Checks), so the optional notes can't be typed reliably. |
| 2 | Slots for the previous, current and next month appear under the client's correct local date with no duplicates. The expert's timezone is shown, and `held` slots can't be selected. | VERIFIED in code (device check deferred) | `groupSlots` in `BookingSlots.kt` ignores the expert-local `day.date`. It keys each slot by `startUtc` and converts to the viewer zone, dedups by start instant with AND of availability, and treats `held == true` as unavailable. `MakeABookingViewModel.loadAvailability` fetches month-1, month and month+1 in parallel and calls `groupSlots` once over the flattened days. The latest load wins. `TimeSlotsCard` shows the timezone note (`timezoneNote`) and uses `enabled = slot.isSelectable(now)` on chips, and `selectSlot` re-checks selectability. |
| 3 | An Android client books a free topic and sees Request Sent, and the iOS expert receives the request. | VERIFIED in code (iOS delivery is human-only) | `book()` guards single flight, free topics only, and a slot that is still in the future, then builds `BookingRequestBody` and posts to `bookings/request`. The body matches the backend strict `requestSchema` exactly: expert_id, topic_id, scheduled_start (ISO UTC), duration_minutes (multiple of 30, default 30), meeting_type, timezone, optional client_notes (null is omitted, never `""`). On success `completedBookingId` is consumed once and `navigator.replaceTop(RequestSentKey(id))` runs. `BookingRequestSentViewModel` loads `GET client/bookings/{id}`. Whether the iOS expert receives it needs a device. |

**Score:** 2/3 truths verified. SC-1 fails on the notes part only.

### Wiring and artifacts (levels 1-4)

| Link | Status | Details |
|------|--------|---------|
| Expert Profile Book Session -> `BookingKey` | WIRED | `ExpertProfileViewModel.bookingKey()` -> `onBook` -> `navigator.navigate`. |
| `BookingKey` entry -> `MakeABookingViewModel` (assisted Hilt) -> `MakeABookingScreen` | WIRED | `HomeClientScreen.kt:94-115` via `clientBookingFlowEntries`, bottom bar hidden. |
| `onBooked` -> `replaceTop(RequestSentKey)` | WIRED | `ClientBookingFlowEntries.kt`; View My Bookings uses `resetAndSelect`, Back to Home uses `reselect(ExploreRoot)`. |
| `BookingRepository` -> `ClickMeApi` -> real endpoints | WIRED | `experts/{id}/availability`, `bookings/request`, `client/bookings/{id}` in `ApiRequestUrls`. |
| Data flow | FLOWING | The topics come from `profiles.details(expertId)`, availability from the API, and Request Sent from the detail endpoint. No static or hardcoded fallbacks in the data paths. |
| Stubs | NONE | No TBD/FIXME/XXX/TODO in the phase's source files. The Make a Booking stub was removed (commit 865a37e). |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Notes field keeps focus when the viewport shrinks (as `imePadding` does with the keyboard) | Throwaway Robolectric test `TmpVerifyFocusTest`: click the notes field, shrink the host Box, `assertIsFocused()` | Focused after the click, NOT focused after the shrink. Test FAILED. | FAIL (CR-01 confirmed) |
| Same test with the `clearFocus` effect disabled | Temporary edit of `MakeABookingScreen.kt` (`if (false)`) | BUILD SUCCESSFUL, test passed | Confirms the effect is the cause |
| Phase 6 test suites (makeabooking, requestsent, BookingFlowNavigationTest, HomeShellsTest) | `./gradlew testDevDebugUnitTest --tests ...` | BUILD SUCCESSFUL | PASS |

The throwaway test and the temporary edit were both reverted (`git status` shows no change under `app/`).

### Probe Execution

No probes are declared in the plans. Step 7c is skipped.

### Requirements Coverage

| Requirement | Source plans | Status | Evidence |
|-------------|--------------|--------|----------|
| BOOK-01 (topic, calendar day, slot, meeting type, optional notes) | 06-03, 06-04, 06-06 to 06-09, 06-12 | BLOCKED (partial) | Everything is implemented, but the notes can't be reliably entered because of CR-01. |
| BOOK-02 (3-month load, regroup by `startUtc`, dedup, timezone shown, `held` not selectable) | 06-01, 06-03, 06-04, 06-06, 06-08, 06-12 | SATISFIED | `groupSlots`, `loadAvailability`, `timezoneNote`, `isSelectable`; golden tests pass. |
| BOOK-03 (free `POST /bookings/request`) | 06-01, 06-04, 06-09, 06-11, 06-12, 06-13 | SATISFIED | `book()` and the body match the backend schema. |
| BOOK-04 (Request Sent confirmation) | 06-01, 06-02, 06-05, 06-10, 06-13 | SATISFIED | `BookingRequestSentScreen` and `ViewModel` with Retry; the route replaces the booking entry. |

All four IDs from the plan frontmatter are mapped to Phase 6 in REQUIREMENTS.md. No orphaned requirements. (REQUIREMENTS.md currently marks BOOK-01 as Complete. It should stay open until CR-01 is fixed.)

### Review findings assessment

| ID | Blocks a success criterion? | Verdict |
|----|----------------------------|---------|
| CR-01 notes focus cleared by programmatic scroll | Yes, SC-1 / BOOK-01 notes | BLOCKER. Reproduced and root-caused above. Small fix (use drag detection). |
| WR-01 Book Now re-enabled between success and navigation | Not a normal-use blocker. A second request needs a tap inside the transition window, and it only creates a second booking if the user also picks a different slot there. The same slot gets a 409. | WARNING. Fix with CR-01: keep `isSubmitting` true, or add a completed flag, after success. |
| WR-02 id lost if the process dies while backgrounded | No. Edge case. | WARNING. Persist `completedBookingId` in `SavedStateHandle`. |
| WR-03 `today` and the zone are frozen in the ViewModel | No. Needs midnight or a timezone change while the screen is open. | WARNING |
| WR-04 form editable while a request is in flight | No. The body is built from a snapshot, so the request itself is correct. The UI and the 409/422 side effects can drift. | WARNING |
| IN-01..IN-11 | No | Info. IN-01 is also visible in `BookingRepository.requestBooking` KDoc ("POST /bookings/confirm", but the call is `bookings/request`). |

### Anti-Patterns Found

| File | Line | Pattern | Severity |
|------|------|---------|----------|
| `MakeABookingScreen.kt` | 122-124 | `clearFocus()` on any scroll | BLOCKER (CR-01) |
| `MakeABookingViewModel.kt` | 175 | Success resets `isSubmitting` before navigation | WARNING (WR-01) |
| `BookingRepository.kt` | 31 | KDoc names the wrong endpoint | Info |

### Gaps Summary

One gap, with one root cause. The booking, regrouping, free-submit and Request Sent paths are real, wired to the backend contract and covered by tests. The notes field can't be used reliably because the screen's "clear focus when a scroll starts" effect also fires on programmatic scrolls. I reproduced this: focus was lost when the viewport shrank, and the same test passed with the effect disabled. Replace it with drag-only detection, add a regression test, and fix WR-01 while in the same file's ViewModel. After that, the phase should pass the automated checks and only the deferred device checks above remain.

---

_Verified: 2026-10-22_
_Verifier: Claude (gsd-verifier)_
