---
phase: 09-voice-call
plan: 16
subsystem: calls
tags: [guard, restricted-actions, viewmodels]

requires:
  - phase: 09-voice-call
    provides: CallGuard / RestrictedAction / FakeCallGuard (09-04), CallGuard bound to CallCenter in Hilt (09-11)
provides:
  - Mode switch, every user-initiated logout, Add Payment Method and the client cancel confirm blocked during a call
affects: [09-13 overlay host (renders the guard dialog), 09-14 Upcoming Details cancel entry, later phases adding their own guard sites]

key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertViewModelTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/AppViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModelTest.kt

key-decisions:
  - "UserManager.logout and the forced-logout path stay unguarded; a test proves a Rejected session event still routes to Login while the guard is blocking"
  - "The addCard guard runs before the in-flight checks so no state changes and no SetupIntent is minted while blocked"
  - "ClientCancellationViewModel.confirm guards after the not-loaded/in-flight early return and before isSubmitting"

requirements-completed: [CALL-04]

duration: 40min
completed: 2026-10-24
---

# Phase 9 Plan 16: Restricted-action guard sites Summary

**Six ViewModels now ask `CallGuard` before switching modes, logging out, adding a card or confirming a cancel, and abort when a call is live (D-13).**

## Accomplishments
- `AppViewModel.switchMode` (SwitchModes) and `logout` (LogOut) guarded; `HomeClientViewModel.logout`, `HomeExpertViewModel.logout`, `SignupOverviewViewModel.requestLogout` guarded with LogOut. `UserManager` has no guard references (grep count 0).
- `PaymentMethodsViewModel.addCard` guarded with AddPaymentMethod as the first line.
- `ClientCancellationViewModel.confirm` uses `attemptModifyBooking(bookingId, RestrictedAction.CancelBooking)`; the id match against the live call lives in `CallCenter` (09-05).
- Tests: new `HomeExpertViewModelTest` (2), plus blocked cases in AppViewModel (logout, forced logout unguarded, switch mode), HomeClient (2), SignupOverview, PaymentMethods (blocked, then unblocked mints one SetupIntent) and ClientCancellation. AppViewModelTest 43, PaymentMethodsViewModelTest 24, ClientCancellationViewModelTest 20, all green; `assembleDevDebug` and `ktlintCheck` pass (Hilt injects CallGuard into all six).

## Task Commits
1. Task 1: mode switch and user-initiated logout guarded - `3bd06e9`
2. Task 2: add payment method and cancel confirm guarded - `4800814`

## Deviations from Plan
None to the code. Operational note: the first Gradle run died with a Kotlin compiler `OutOfMemoryError` (default 2 GB shared daemon heap with parallel agents); re-ran with `-Pkotlin.daemon.jvmargs=-Xmx3g` on the command line, `gradle.properties` untouched. The worktree also started from an older base and was reset to 3150a62 per the branch check.

## Known Stubs
None.

## Threat Flags
None. T-09-52, T-09-53 and T-09-54 are mitigated and covered by tests.

## Self-Check: PASSED
- Commits 3bd06e9 and 4800814 exist; all listed files exist.
