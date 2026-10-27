---
phase: 12-chat-report-block
plan: 11
subsystem: chat
tags: [chat-entry, client-bookings, message-button, tdd]
status: complete

requires:
  - phase: 12-chat-report-block
    provides: ChatEntryViewModel, ChatLauncher, MessageButton, ChatEntryErrorDialog, strings_chat_entry.xml, testChatEntryViewModel (12-04)
provides:
  - Message Expert on client Upcoming Booking Details
  - Message Expert on client Booking Summary (BookingSummaryDetails.expertId)
  - Icon-form and text-form Message on My Bookings Upcoming cards
affects: [12-12, 12-13]

tech-stack:
  added: []
  patterns:
    - "Hilt-aware client screens take chatEntry: ChatEntryViewModel = hiltViewModel() after modifier; stateless content gets messageBusy and onMessage with defaults"
    - "Per-card busy: messageBusyPeerId compared case-insensitively with the card's expert id"

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/UpcomingBookingCard.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingManagementFixtures.kt

key-decisions:
  - "BookingSummary expertId is mapped in toSummaryDetails (BookingSummaryUiState.kt), so BookingSummaryViewModel.kt needed no edit"
  - "On Booking Summary, Message Expert sits below Book Again (iOS BookingSummaryActions order), both below the summary card, and shows even when Book Again is absent"
  - "Pending-approval cards show the text-form Message under the pending notice; confirmed cards show Join (weight 1) plus a 56 dp icon button"

patterns-established:
  - "MESSAGE_EXPERT_TAG (Upcoming Booking) and BOOKING_SUMMARY_MESSAGE_TAG (Booking Summary) both equal \"message_expert\"; BOOKING_CARD_MESSAGE_TAG = \"booking_card_message\" covers both card forms"

requirements-completed: [CHAT-05]

duration: ~75min
completed: 2026-10-27
commits: 3
plan_head_before: daa12dd1d9ff48e83c44a9ae369cb3bb3b9ce2c5
plan_head_after: dcff5fd
actuals:
  tokens: 14000
  tasks: 3
  commits: 3
---

# Phase 12 Plan 11: Client Message entry points Summary

**Clients can open a chat with a booking's expert from Upcoming Booking Details, Booking Summary and the My Bookings Upcoming cards (icon beside Join, or a text form on pending-approval cards), all through the shared 12-04 ChatEntryViewModel with no new strings.**

## Accomplishments

- Upcoming Booking Details: full-width "Message Expert" after the Join card and client note, before Cancel Booking. Disabled when the expert id is null; spinner and ignored taps while initiate runs; "Couldn't open chat" dialog on failure.
- Booking Summary: `BookingSummaryDetails.expertId` (blank becomes null) drives the control; "Message Expert" sits below Book Again.
- My Bookings: `UpcomingBookingCard` gains `messageBusy` and `onMessage`. Confirmed cards show Join (weight 1) beside a 56 dp icon button described "Message {name}"; pending-approval cards show the full-width "Message" text form. Taps on the Message control do not open the details. `MyBookingsContent` passes `messageBusyPeerId` and `onMessage(item)`; Past cards are unchanged.
- Block state is never checked (D-12); all copy comes from `strings_chat_entry.xml`.

## Task Commits

1. Task 1 (tracer): `c50fd7b` feat(12-11): Message Expert on Upcoming Booking Details opens the chat with the booking's expert
2. Task 2: `277dbac` feat(12-11): Message Expert on the client Booking Summary
3. Task 3: `dcff5fd` feat(12-11): icon-form Message on My Bookings Upcoming cards

## Verification

- `UpcomingBookingScreenTest`, `BookingSummaryScreenTest`, `BookingSummaryViewModelTest`, `MyBookingsScreenTest`: all green (128 tests across the Task 3 run). `ktlintCheck` passes. `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Tracer gate: Task 1 verify passed end to end (including the real `UpcomingBookingScreen` + `testChatEntryViewModel` test asserting one initiate for "e1" and a `ChatThreadKey` with `peerId == "e1"`, `peerIsExpert == true`) before Tasks 2 and 3. No `blocking-human` gate.
- Acceptance greps all satisfied: `message_expert`, `isExpert = true`, `chat_entry_message_expert`, `expertId` (UiState), `booking_card_message`, `MessageButtonVariant.Icon`, `chatEntry.open`.

## TDD Gate Compliance

Tests and implementation are committed together per task (the tests reference new parameters and tags, so a separate RED run would be a compile failure, INVALID_RED). The plan is `type: execute`, so the plan-level gate sequence does not apply.

## Deviations from Plan

**1. [Rule 3 - Blocking] Fixture edit outside the listed files**
- `BookingManagementFixtures.kt` `bookingItem(...)` gained an `expertId` parameter (default "e1", unchanged behavior) so the per-card busy test can use two experts. Existing callers are unaffected.

**2. [Scope] BookingSummaryViewModel.kt not modified**
- The plan listed it for the expertId mapping, but the mapping lives in `toSummaryDetails` in `BookingSummaryUiState.kt`. The ViewModel test (`detailsCarryExpertId`) covers the mapping.

**3. [Process] Existing assertions updated**
- `noRescheduleMessageOrPrivateNoteActionsExist` and `phaseTwelveAndThirteenElementsDoNotRender` (Booking Summary) and `content_neverOffersExcludedActions` (My Bookings) forbade "Message Expert"/"Message"; they now forbid only what is still excluded (Past cards must have no Message).

Otherwise the plan executed as written.

## Known Stubs

None.

## Threat Flags

None. T-12-33 accepted (peer ids come from the user's own booking payloads). T-12-34 mitigated (shared ChatEntryViewModel busy guard plus ChatLauncher single flight; per-card busy and ignored repeat taps tested).

## Deferred / Open Items

- On-device check that tapping Message lands on the Chats tab thread needs the real thread screen (12-09); manual testing is deferred per project policy.
- Build note: with six parallel Gradle runs the Kotlin daemon crashed once; re-running with `-Pkotlin.daemon.jvmargs=-Xmx3g` passed.

## Self-Check: PASSED

- Modified files found on disk (all ten listed above).
- Commits c50fd7b, 277dbac, dcff5fd are ancestors of HEAD.
