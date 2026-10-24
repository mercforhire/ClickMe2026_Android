---
phase: 08-client-bookings-cancellation
plan: 06
subsystem: ui
tags: [compose, components, reschedule, dialogs]
requires: [08-02, 08-04]
provides:
  - "RescheduleResponseCard and RescheduleOutcomeDialog shared composables with test tags"
  - "BookingStatusBadge, StatusTone.color(), BookingStatusDisplay.labelText()"
  - "DestructiveOutlinedButton (error-outlined press-scale pill)"
  - "formatProposalSlot time helper"
affects: [08-07, 08-08, 08-09, 08-10, 08-11, 08-12]
tech-stack:
  added: []
  patterns: ["stateless card with local confirm-dialog state in rememberSaveable", "stateDescription on in-flight buttons"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/BookingStatusBadge.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/buttonstyles/DestructiveOutlinedButton.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/BookingStatusBadgeTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCardTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormatting.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormattingTest.kt
key-decisions:
  - "Decline reason counter is a separate Text under the field (not supportingText) so its node is unambiguous"
  - "Accept/Decline stack vertically (Accept first) when fontScale >= 1.5 or width < 320dp"
requirements-completed: [BOOK-05, BOOK-08, BOOK-09]
duration: 40min
completed: 2026-10-23
---

# Phase 8 Plan 06: Shared Reschedule Card, Status Badge and Destructive Button Summary

One reschedule response card (expert-initiated Accept/Decline behind confirm dialogs, client-initiated read-only) plus a word-labelled status badge, an error-outlined destructive button and `formatProposalSlot`, all theme-token only.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (RED) | Failing badge, button and formatProposalSlot tests | cd48b9b |
| 1 (GREEN) | BookingStatusBadge, DestructiveOutlinedButton, formatProposalSlot | 9394056 |
| 2 (RED) | Failing RescheduleResponseCardTest | 6bcb104 |
| 2 (GREEN) | RescheduleResponseCard and RescheduleOutcomeDialog | 8f2ece6 |

## Verification

- `:app:testDevDebugUnitTest --tests '*RescheduleResponseCardTest' '*BookingStatusBadgeTest' '*BookingFormattingTest' ktlintCheck` passes (RescheduleResponseCardTest: 17 tests, 0 failures).
- `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Grep gates: no `fontSize`/`Color(0x`/start-reschedule actions in the card; `rememberSaveable` used 4 times; no `extras.destructive` in the new files.

## Deviations from Plan

None - plan executed exactly as written. One test-only adjustment: `detailLabelsRender` initially called `setContent` repeatedly (not allowed), so it now drives one composition through mutable state. Startup: the worktree base differed from the expected commit and was reset to e682101 per the branch check.

## TDD Gate Compliance

For both tasks, `test(08-06)` commits (cd48b9b, 6bcb104) precede the `feat(08-06)` commits (9394056, 8f2ece6). RED was a compile failure (types did not exist), not failing assertions.

## Known Stubs

None.

## Threat Flags

None. T-08-21 (note is plain Text in SelectionContainer), T-08-22 (reason capped at 500 on every change), T-08-23 (Decline is error-outlined and both actions need a confirm dialog) are mitigated.

## Self-Check: PASSED

All created files exist; commits cd48b9b, 9394056, 6bcb104, 8f2ece6 are in the log.
