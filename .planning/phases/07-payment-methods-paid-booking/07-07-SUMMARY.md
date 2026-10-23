---
phase: 07-payment-methods-paid-booking
plan: 07
subsystem: make-a-booking
tags: [viewmodel, ui-state, cta, gate, payment-methods]
requires:
  - phase: 07-02
    provides: PaymentMethodsRepository.list(), PaymentMethodItem, FakeClickMeApi payment-methods handler
  - phase: 07-03
    provides: PaymentMethodsAvailability (Unknown, None, Available)
provides:
  - MakeABookingUiState.paymentMethods, BookDisabledReason.CheckingPaymentMethods, BookCtaAction, bookCtaAction
  - MakeABookingViewModel.refreshPaymentMethods() (silent, single-flight, never books)
  - MakeABookingBookButton with a cross-fading Book Now / Add a Payment Method label
  - First-topic default (D-10)
affects: [07-10 paid book(), 07-12 CTA routing and resume hook]
tech-stack:
  added: []
  patterns:
    - "Availability value is never reset to Unknown on refresh, so the CTA does not flicker"
key-files:
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingBookButton.kt
    - app/src/main/res/values/strings_make_a_booking.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTestFixtures.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiStateTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingFormCardsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt
key-decisions:
  - "A failed card load maps to None (iOS parity); Payment Methods has its own error state and Retry"
  - "A paid topic with None is enabled without a slot, since the CTA routes to Payment Methods (iOS canBook)"
requirements-completed: [PAY-03, PAY-04, PAY-07]
duration: ~35min
completed: 2026-10-22
---

# Phase 7 Plan 07: Saved-card gate on Make a Booking Summary

Make a Booking now loads the client's saved cards in parallel with topics and availability and derives the CTA from them (Checking / Add a Payment Method / Book Now); the first topic is preselected again and the Phase 6 "coming soon" note is gone.

## Tasks

| Task | Commit | Result |
| ---- | ------ | ------ |
| 1. Card availability load, silent refresh, CTA derivation, first-topic default | 1f4e376 | `paymentMethods` state, `loadPaymentMethods()`, `refreshPaymentMethods()`, `bookCtaAction`, `topics.first` default |
| 2. CTA label switch, strings, paid-note removal | e2e6317 | Crossfade label, `stateDescription` per reason, string add/remove/reword, JPY Summary price test |

## Verification

- `./gradlew testDevDebugUnitTest ktlintCheck lintDevDebug assembleDevDebug` all pass.
- 162 `*MakeABooking*` tests pass, including the new behaviors (card load Available/None/IOException/500, Unknown while suspended, refresh single-flight, refresh during `isSubmitting`, refresh keeps selections and `requestBookingCalls == 0`, UI-state table, CTA labels, real-ViewModel screen tests, `¥1,500` with no `¥15.00`).
- Acceptance greps: no `firstOrNull { it.isFree }` and no `PaidTopic` in main sources; no references to the removed string names.

## Deviations from Plan

**1. [Rule 3 - Blocking] Task 1 commit does not compile on its own**
- **Found during:** Task 1
- **Issue:** Removing `BookDisabledReason.PaidTopic` breaks `MakeABookingBookButton`, which Task 2 rewrites, so the Task 1 commit alone does not build. Both commits together build and pass.
- **Fix:** Kept the plan's two-commit split (state/ViewModel, then UI) rather than merging them.

**2. [Rule 1 - Test expectation] `unknownSavedTopicFallsBackToTheDefault`**
- **Issue:** The fallback is now the first topic (D-10), not the first free topic.
- **Fix:** Updated the expected id from `t2` to `t1` in `MakeABookingViewModelTest`.

**3. Button wrapper:** The `Column` wrapper around the button was removed because it only existed to host the paid note; the button now applies `modifier.fillMaxWidth()` directly. The screen passes no modifier, so layout is unchanged.

## Known Stubs

None. `onClick = onBook` is kept for the `AddPaymentMethod` action by plan design; 07-12 routes it to Payment Methods and wires the resume call to `refreshPaymentMethods()`, and 07-10 adds the paid `book()` path (`savedPaymentMethods` is stored for it).

## Threat Flags

None. T-07-25 (Unknown disables the CTA) and T-07-27 (refresh ignored while submitting or loading) are covered by tests.

## Self-Check: PASSED

- Commits 1f4e376 and e2e6317 exist on the branch.
- All ten modified files are present and committed.
