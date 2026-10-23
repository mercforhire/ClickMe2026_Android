---
phase: 07-payment-methods-paid-booking
plan: 12
subsystem: navigation
tags: [navigation, nav3, entry-points, profile, gate]
requires: [07-04, 07-06, 07-07, 07-09]
provides:
  - "MakeABookingScreen/Content onOpenPaymentMethods routing for the Add a Payment Method CTA"
  - "clientPaymentMethodsEntry: single PaymentMethodsKey registration (bottom bar hidden)"
  - "Client Profile 'Payment Methods' row above Log out"
  - "Silent card refresh when the booking form returns (ON_RESUME + re-entry)"
affects: [07-13, 07-14]
tech-stack:
  added: []
  patterns: ["Hilt-free entry registration helper driven by fake content in HomeShellsTest"]
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt
key-decisions:
  - "PaymentMethodsKey is registered exactly once (clientPaymentMethodsEntry) and pushed on whichever tab is selected"
  - "Return refresh is driven by both ON_RESUME and LaunchedEffect(Unit); refreshPaymentMethods() de-duplicates via its in-flight guard"
requirements-completed: [PAY-01, PAY-04]
duration: ~25 min
completed: 2026-10-22
---

# Phase 7 Plan 12: Payment Methods entry points and return refresh Summary

Payment Methods is now one Nav3 destination reachable from the Profile tab row and from the booking form's "Add a Payment Method" gate, and the form reloads its saved cards on return without touching selections or booking.

## Tasks

| Task | Name | Commits |
| ---- | ---- | ------- |
| 1 | Booking CTA routing and the return refresh (D-08, D-09) | fd2972a (RED), 4975a6e (GREEN) |
| 2 | Shell registration, Profile row and booking-flow lambda (D-04) | 1eacb3c (RED), 49ad321 (GREEN) |

## What was built

- `MakeABookingScreen` / `MakeABookingContent` take `onOpenPaymentMethods`; the CTA calls it when `bookCtaAction == AddPaymentMethod`, else `onBook`.
- `MakeABookingScreen` calls `refreshPaymentMethods()` from `LifecycleEventEffect(ON_RESUME)` (alongside `refreshClock()`) and from `LaunchedEffect(Unit)`.
- `clientBookingFlowEntries` booking lambda gains a fourth `onOpenPaymentMethods` parameter wired to `navigator.navigate(PaymentMethodsKey)`.
- `clientPaymentMethodsEntry(navigator, paymentMethods)` registers `entry<PaymentMethodsKey>(metadata = BottomBar.hidden())`; `HomeClientScreen` supplies the Hilt-backed `PaymentMethodsScreen`.
- Client Profile footer shows a `ProfileMenuRow` "Payment Methods" (CreditCard icon) above Log out.

## A10 result (ON_RESUME under Nav3 1.2.0)

Observed in the Robolectric nav test (booking fake -> Payment Methods -> Back): the covered booking entry re-entered composition, `LaunchedEffect(Unit)` ran +1, and `LifecycleEventEffect(ON_RESUME)` also fired +1. This is the re-entered observer replaying the already-RESUMED lifecycle on re-composition, which Robolectric cannot distinguish from a true resume delivery to a still-composed entry; either way the refresh is covered on a device. On return both effects call `refreshPaymentMethods()`; the second call is dropped by the in-flight guard in the ViewModel (Pitfall 9), and a no-op duplicate is harmless otherwise.

## Verification

- `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` passes; `lintDevDebug` passes.
- Return test with the real ViewModel: cards reloaded, CTA flips to "Book Now", topic, notes and slot unchanged, `paymentIntentCalls == 0`, `requestBookingCalls == 0` (T-07-45 mitigated).

## Deviations from Plan

**1. [Rule 3 - Blocking] Task 1 GREEN commit includes a temporary call-site fix in HomeClientScreen.kt**
- The new required `onOpenPaymentMethods` parameter broke compilation of `HomeClientScreen`; commit 4975a6e passes `{}` there so that commit builds. Task 2 (49ad321) replaces it with the real wiring.

**2. [Rule 1 - Bug] Existing gallery tests scrolled off-screen**
- The new Profile row pushed "Design system gallery" below the fold of the Robolectric window, failing four existing HomeShellsTest cases. `openGalleryFromProfile()` now uses `performScrollTo()`. Test-only change.

Beyond that, the plan was executed as written. The worktree was at a stale base and was reset to 1e2b770 at startup per the branch check.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED
