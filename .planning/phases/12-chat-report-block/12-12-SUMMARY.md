---
phase: 12-chat-report-block
plan: 12
subsystem: chat
tags: [chat-entry, expert-bookings, request-decision, message-button, tdd]
status: complete

requires:
  - phase: 12-chat-report-block
    provides: ChatEntryViewModel, ChatPeer, MessageButton, ChatEntryErrorDialog, strings_chat_entry.xml, testChatEntryViewModel (12-04)
provides:
  - Message Client on the expert Booking Summary (MESSAGE_CLIENT_TAG)
  - Message icon in the Request Decision footer, stacked text form, and expired-state text form (REQUEST_MESSAGE_TAG)
  - RequestDecisionDetails.clientId
  - Icon-form Message on expert Upcoming booking cards (EXPERT_BOOKINGS_MESSAGE_TAG)
affects: [12-13, 12-14, 12-15]

tech-stack:
  added: []
  patterns:
    - "Hilt-aware screens take chatEntry after modifier; stateless Content composables take messageBusy and onMessage with defaults"
    - "Per-card busy: messageBusyPeerId == item.client.id, so only the tapped card shows the spinner"

key-files:
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestActionFooter.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreenTest.kt

key-decisions:
  - "Footer Message is disabled together with Decline and Accept while a decision is in flight (one action at a time, T-12-36)"
  - "Expired requests show the text-form Message at the end of the scroll content with Spacing.lg above it; pending requests get it only through the footer"
  - "Footer side-by-side row uses Spacing.sm between Message, Decline and Accept (was Spacing.md between the two buttons), as the plan specified"

patterns-established:
  - "Expert Message controls always build ChatPeer(clientId, name, avatar, isExpert = false) and never read block state"

requirements-completed: [CHAT-05]

duration: ~75min
completed: 2026-10-27
commits: 3
plan_head_before: daa12dd1d9ff48e83c44a9ae369cb3bb3b9ce2c5
plan_head_after: 0a4fbadf91dc7f61220237f3850586de43564375
actuals:
  tokens: 14000
  tasks: 3
  commits: 3
---

# Phase 12 Plan 12: Expert Message entry points Summary

**Experts can message a client from the Booking Summary ("Message Client"), the Request Decision footer (56 dp icon, stacked text form, expired-state text form) and Upcoming booking cards (icon beside Join), all through the shared 12-04 ChatEntryViewModel with `isExpert = false`.**

## Accomplishments

- Booking Summary: full-width text-form Message Client after Join Call (shown or not) and before Cancel Booking; the screen shows `ChatEntryErrorDialog` and passes busy state down. Content gains `messageBusy` and `onMessage` with defaults, so existing callers compile.
- Request Decision: `RequestDecisionDetails.clientId` mapped from the request detail's client id. `RequestActionFooter` leads with the icon Message ("Message {client}", tag `request_message`) before Decline and Accept; below 320 dp or at font scale 1.5 it stacks Accept, Decline, then the text form. An expired request (no footer) shows the text form at the bottom of its content.
- Upcoming cards: Join (`weight(1f)`) and a 56 dp icon Message share a row; Past cards are unchanged. Tapping Message does not open the booking.
- No new strings (reuses `chat_entry_message_client` and `chat_entry_message_name`). No Report or Block control on any of these screens (D-11); no block-state check (D-12).

## Task Commits

1. Task 1 (tracer): `7a535dc` feat(12-12): Message Client on the expert Booking Summary
2. Task 2: `1a950a0` feat(12-12): Message in the Request Decision footer, stacked layout and expired state
3. Task 3: `0a4fbad` feat(12-12): icon-form Message on expert Upcoming booking cards

## Verification

- `ExpertBookingSummaryScreenTest` 32, `RequestDecisionScreenTest` 28, `RequestDecisionViewModelTest` 26, `ExpertBookingsScreenTest` 21: all green. `ktlintCheck` and `lintDevDebug` pass. `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Tracer feedback gate: Task 1's verify (summary tests plus ktlint) passed end to end before Tasks 2 and 3; the task had no `blocking-human` gate.
- Acceptance greps: `message_client`, `isExpert = false` (summary, request decision), `request_message`, `clientId` in the UI state, `MessageButtonVariant.Icon` in the upcoming card, `chatEntry.open` in the bookings screen.

## TDD Gate Compliance

Tests and implementation are committed together per task. The new tests reference symbols that do not exist before the implementation (`MESSAGE_CLIENT_TAG`, `REQUEST_MESSAGE_TAG`, `clientId`, `chatEntry` parameters), so a separate RED run would be a compile failure (INVALID_RED). The plan is `type: execute`, so the plan-level gate sequence does not apply.

## Deviations from Plan

**1. [Rule 1 - Test] Replaced `noMessageRescheduleOrNoteControls`**
- The existing summary test asserted no node containing "Message", which the new control correctly breaks. It is now `noRescheduleNoteReportOrBlockControls` (Reschedule, notes, meeting link, Report, Block).

**2. [Process] No `Spacer(Spacing.md)` before Message Client**
- The loaded column already uses `spacedBy(Spacing.md)`, so the plan's explicit spacer would have doubled the gap.

**3. [Process] `ExpertUpcomingCard` Message parameters have no defaults**
- `messageBusy` and `onMessage` are required (after `onJoin`); the only caller and the preview were updated.

**4. [Environment] Kotlin daemon OOM on the first Gradle run**
- Re-ran with `-Pkotlin.daemon.jvmargs=-Xmx3g` as documented; no code change.

Otherwise the plan executed as written.

## Known Stubs

None.

## Threat Flags

None. T-12-36 mitigated: the shared busy guard and ChatLauncher single flight (12-04) apply, and the footer disables Message together with Decline and Accept during a decision (`footerMessageIsDisabledWhileADecisionIsInFlight`). T-12-35 accepted.

## Deferred / Open Items

- On-device check that Message lands on the Chats tab thread needs the real thread screen (12-09); manual testing is deferred per project policy.

## Self-Check: PASSED

- Modified files found in the worktree (11 listed in key-files).
- Commits 7a535dc, 1a950a0, 0a4fbad are ancestors of HEAD.
