---
phase: 07-payment-methods-paid-booking
plan: 09
subsystem: ui
tags: [compose, stripe, paymentsheet, payment-methods, accessibility]

requires:
  - phase: 07-payment-methods-paid-booking
    provides: "clickMeSheetConfiguration + rememberClickMePaymentSheetAppearance (07-01), SheetOutcome / ChallengeOutcome (07-03), PaymentMethodsViewModel + UI state (07-06)"
provides:
  - "toSheetOutcome(), toChallengeOutcome(), shouldConfirmSelection() in one SDK-mapping file (stripe/StripeResultMapping.kt)"
  - "brandDisplayNameRes(raw) and expiryParts(expMonth, expYear) top-level functions"
  - "PaymentMethodRow, PaymentMethodRemoveDialog, PaymentMethodsScreen / PaymentMethodsContent / AddCardSheetHost, PAYMENT_METHODS_ADD_TAG"
affects: [07-12, 07-13]

tech-stack:
  added: []
  patterns:
    - "Stripe host injected into the screen as a composable parameter so Robolectric never instantiates PaymentSheet"
    - "One-shot request consumed (onLaunched) before the SDK is presented"
    - "Semantics merged on the description half of a row only, so the trailing button stays its own focus stop"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeResultMapping.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/subviews/PaymentMethodBrandDisplay.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/subviews/PaymentMethodRow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/subviews/PaymentMethodRemoveDialog.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/stripe/StripeResultMappingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodBrandDisplayTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodRowTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsScreenTest.kt
  modified: []

key-decisions:
  - "D-18 confirm rule is a pure function: dismissing the FlowController sheet never confirms, even with a preselected card (deliberate difference from iOS, RESEARCH Pitfall 6)"
  - "Row a11y label uses mergeDescendants plus contentDescription on the card-info half only; overflow button and spinner stay separate nodes"
  - "Remove target kept as an id in rememberSaveable and resolved against the live list, so the dialog disappears if the card vanishes"
  - "Spacing lg between last row, Add button and trust copy built from the md list gap plus an sm spacer"

patterns-established:
  - "Stripe SDK result classes with internal constructors are built in tests through reflection on the public bytecode constructor"

requirements-completed: [PAY-01, PAY-02]

duration: 40min
completed: 2026-10-22
---

# Phase 7 Plan 09: Payment Methods screen and add-card PaymentSheet host Summary

**Payment Methods now renders loading, failed, empty and loaded states with a per-row overflow menu, a confirm-first remove dialog, non-optimistic busy rows, and an add-card host that presents Stripe PaymentSheet in setup mode exactly once per request.**

## Performance

- **Tasks:** 2/2
- **Files:** 5 production, 4 test (all new)
- **Tests added:** 7 mapping cases + 4 brand/expiry cases, 13 row cases, 13 screen cases; `PaymentMethodsViewModelTest` (15) still passes

## Accomplishments
- `StripeResultMapping.kt` maps `PaymentSheetResult` and `PaymentResult` to the SDK-free outcomes, and holds `shouldConfirmSelection(didCancel, hasPaymentOption) = !didCancel && hasPaymentOption` with the D-18 rationale in KDoc.
- Brand display is top-level functions only (no `*Brand` type, CI rule passes); unknown, empty and null brands fall back to "Card".
- `PaymentMethodRow`: brand tile, "{Brand} •••• {last4}" with `FlowRow` Default badge, "Expires MM/YY", merged label ("Visa ending 4242, expires 09/28, default card"), 48dp ⋮ button with `DropdownMenu` ("Set as default" hidden on the default card, "Remove" in `colorScheme.error`), cross-faded spinner with "Updating" / "Removing" state description while busy.
- `PaymentMethodsContent` handles every UI-SPEC state, locks all other rows' menus while one operation runs, asks "Remove this card?" before removing, and shows `AuthErrorDialog` with the ViewModel's title and message.
- `AddCardSheetHost` calls `onLaunched()` before `presentWithSetupIntent` and routes results through `rememberUpdatedState`; the screen takes it as a replaceable `addCardHost` parameter, and a test proves the request reaches the host once, is cleared, and a Canceled report leaves the list untouched.

## Task Commits

1. **Task 1 RED:** `dcde49b` test(07-09): failing tests for Stripe result mapping and brand display
2. **Task 1 GREEN:** `a0276c9` feat(07-09): Stripe result mapping and brand display functions
3. **Task 2 RED:** `3c6b14d` test(07-09): failing tests for Payment Methods row and screen
4. **Task 2 GREEN:** `808c66d` feat(07-09): Payment Methods screen, rows, remove dialog, add-card host

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] SDK result constructors are internal**
- **Found during:** Task 1 (GREEN, test compile)
- **Issue:** In Stripe 23.24.0, `PaymentSheetResult.Completed`, `Canceled` and `Failed` are classes with internal constructors, so `PaymentSheetResult.Completed.toSheetOutcome()` and `Failed(...)` do not compile in tests. `PaymentResult.*` is public.
- **Fix:** Tests build real `PaymentSheetResult` instances through the public bytecode constructors via reflection, so the production mapping is still exercised end to end (no primitive-taking helper needed).
- **Files modified:** `StripeResultMappingTest.kt`
- **Commit:** `a0276c9`

**2. [Rule 1 - Bug] Overflow button label built with the wrong arguments**
- **Found during:** Task 2 (before first run)
- **Issue:** Initial draft passed the combined card name plus an empty string to `payment_methods_more_options` (which takes brand and last4).
- **Fix:** The menu computes brand and last4 itself.
- **Commit:** `808c66d`

No architectural changes.

## Deferred / Open Items
- **Device check (D-03, needs the 07-05 Stripe key):** Profile -> Payment Methods -> Add Payment Method -> PaymentSheet in setup mode renders dark on a light-mode phone with Nunito and a "Save card" button; add 4242 4242 4242 4242; the card appears; set it default; remove it; rotate while the sheet is open (no second sheet). Not run here per the deferred-manual-testing policy; the real PaymentSheet is intentionally not covered by Robolectric.
- Entry points to this screen are wired in 07-12.

## Known Stubs
None.

## Threat Flags
None. T-07-30 to T-07-33 are mitigated as planned: only Stripe's sheet collects card data, the request is consumed before presenting (tested with a fake host), only the remove-target id and menu flags are saved, and removal requires the confirmation dialog while other rows are locked.

## Verification
- `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` passes; `lintDevDebug` passes; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: one `presentWithSetupIntent`, `onLaunched()` at line 128 before it at line 129, zero `extras.destructive` in the row, zero `fontSize` / `Color(0x` in the package, zero `*Brand` types.

## Self-Check: PASSED
- All 9 created files exist; commits `dcde49b`, `a0276c9`, `3c6b14d`, `808c66d` present in `git log`.
