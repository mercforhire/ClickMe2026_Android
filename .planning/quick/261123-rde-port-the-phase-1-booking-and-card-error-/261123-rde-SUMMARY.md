---
phase: quick-261123-rde
plan: 01
subsystem: booking-errors, expert-cancel
tags: [ios-parity, BACK-15, BACK-19, BACK-07, BACK-12]
requirements: [PAY-06, EXP-09, PAY-01, BOOK-07]
status: complete
commits: 3
plan_head_before: d8e72dad
plan_head_after: 7de35062943ad1a9cdd8e5fb8928bd810476b1e4
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
    - app/src/main/res/values/strings_make_a_booking.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreview.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/subviews/ExpertCancelConfirmStep.kt
    - app/src/main/res/values/strings_expert_cancel.xml
decisions:
  - "Expert cancel copy follows shipped iOS v1.1 (up to wording, WR-03 f649900), not the iOS 03-08 text in the original follow-up"
  - "Duplicate-booking fallback dialog keeps Android's single button (no Check My Bookings button)"
  - "The 3-5 business days footnote is kept as iOS v1.1 has it, shown only with the full-refund line"
actuals:
  tasks: 3
  commits: 3
---

# Quick 261123-rde: Port the iOS Phase 1 booking and card error fixes, and the Phase 3 expert cancel rule

Android now routes a paid-flow `DUPLICATE_BOOKING` to the server's own booking the way iOS v1.1 does, and the expert cancel sheet states a client-money line only when the payload proves money is held or taken. `PAYMENT_METHOD_IN_USE` and `SESSION_ALREADY_ENDED` were already at iOS parity and are now pinned by tests.

## Commits (local on main, not pushed)

| Task | Commit | What |
|------|--------|------|
| 1 | 32016068 | `PaidFailure.OpenBooking`, DUPLICATE_BOOKING rule, `make_a_booking_error_may_already_booked`, tests |
| 2 | 87530fff | `payment_action_required` / `payment_not_charged` decoding, `ExpertRefundPreview` rewrite, iOS v1.1 strings, screen/ViewModel/decode tests |
| 3 | 7de35062 | Pinning tests for BACK-07 and BACK-12 (no production change) |

## What changed

1. **BACK-15 (paid DUPLICATE_BOOKING).** When this attempt's `/bookings/confirm` ended with an unknown outcome and the 409 carries `detail.booking_id` as a canonical UUID (either case), `applyPaidFailure` ends the flow like a successful confirm (`completedBookingId` set, `isSubmitting` stays true, no dialog) and the existing screen effect opens Request Sent for that id. Every other paid DUPLICATE_BOOKING (no unknown outcome, or id missing, blank, non-string, non-UUID, non-canonical, in either phase) clears the slot, mints a new attempt id, reloads availability and shows "Couldn't book" / "You may already have this booking. Check My Bookings.". The free path keeps "A booking for this session already exists." (unchanged).
2. **BACK-19 / D-17 (expert cancel).** `ExpertBookingDetail` gained nullable `paymentActionRequired` and `paymentNotCharged`. `expertRefundPreview` is a port of iOS `ExpertCancelRefundLine`: a line appears only when the fee is above 0 and both keys are exactly false; a non-null `authorizationExpiresAt` gives the hold line, otherwise the full-refund line. Any other combination shows nothing. The within-24-hours and policy rows and the `clock` constructor parameter of `ExpertCancellationViewModel` were removed (an expert cancel always releases or refunds in full).
3. **BACK-07 / BACK-12.** Tests only: the server message is shown, the card stays in the list, and the client cancel stays on its confirm step.

## Recorded finding: 08-12 footnote (flag to the user)

The earlier Android port of iOS 08-12 (Android commit 2ffb5d23) left the expert footnote "Refunds typically appear in the client's account within 3–5 business days." alone for two reasons: iOS `ExpertCancellationMainCard` says the same thing, and the iOS todo marked expert-cancel wording out of scope (the iOS Phase 6 Q2 wording question). Sources: iOS `.planning/phases/08-release/08-12-SUMMARY.md` line 54 and iOS `.planning/todos/completed/2026-11-13-android-cancellation-policy-link.md` lines 30 and 40. The first reason still holds: iOS v1.1 still shows that footnote, for the full-refund line only. This task keeps the text, shows it only with the full-refund line, and does not copy the client "Your bank decides..." wording onto the expert sheet.

Android's full-refund row was dormant before this change, so the 3–5 business days footnote now appears on Android for the first time, for a paid booking without a live hold (both keys false, `authorization_expires_at` null).

## Copy note

The expert copy follows iOS v1.1 ("up to", WR-03 f649900), not the iOS 03-08 text in the original follow-up: "The client's hold of up to {amount} will be released." and "The client will get a full refund of up to {amount}" (no trailing period, exactly as iOS). Without a usable currency or a finite amount: "The client's hold will be released." and "The client will get a full refund." (footnote still shown for the latter, as iOS does).

## Known deviations from iOS

- The fallback dialog has a single button; iOS adds a "Check My Bookings" button (orchestrator decision).
- Android's routing gate is the per-attempt unknown-outcome record (D-16/D-19); iOS's `UnconfirmedBookingLedger` is keyed by expert and slot. Android routes in a narrower set of cases and shows the message otherwise.
- The fallback reloads availability; iOS only clears the slot.
- The card-delete error title is "Couldn't remove" on Android against "Something went wrong" on iOS. This predates the task and was left unchanged.

## Deviations from Plan

- **[Rule 1 - test fix]** The existing table test `only_conflict_pi_and_slot_rows_ask_for_a_new_attempt_id` failed because DUPLICATE_BOOKING now correctly asks for a new attempt id; added it to the expected set and renamed the test `only_conflict_pi_slot_and_duplicate_rows_ask_for_a_new_attempt_id`. The plan did not mention this test.
- `ExpertCancellationScreenTest.stub` now supplies a live hold and both keys false so the end-to-end hold-line test still reaches the money line.

Otherwise the plan was executed as written. No auth gates, no production backend calls.

## Verification

- Full `:app:testDevDebugUnitTest`: 5474 tests, 0 failures, 0 errors, 0 skipped.
- `ktlintCheck`: pass.
- `:app:lintDevDebug`: pass (BUILD SUCCESSFUL).
- Preconditions for all three tasks verified against backend 1d18aac and iOS v1.1 via `git show` before editing.
- All three commits are local on main; nothing pushed. Working tree under `app/src` is clean.

## Self-Check: PASSED

- 32016068, 87530fff, 7de35062 are ancestors of HEAD.
- Modified files listed above exist; `OpenBooking`, `paymentNotCharged = detail.paymentNotCharged` and the new strings are present.
