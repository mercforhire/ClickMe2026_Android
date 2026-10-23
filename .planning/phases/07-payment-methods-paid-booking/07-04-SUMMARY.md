---
phase: 07-payment-methods-paid-booking
plan: 04
subsystem: ui
tags: [compose, components, copy, accessibility]
requires: []
provides:
  - PaymentLifecycleCard and PaymentLifecycleStage (shared by info sheet and paid Request Sent)
  - PaymentInfoSheet / PaymentInfoSheetBody ("How does payment work?" sheet)
  - MakeABookingPaymentInfoLink (neutral text link, PAYMENT_INFO_LINK_TAG)
  - ProfileMenuRow (generic Profile list row)
  - strings_payment_lifecycle.xml (iOS copy verbatim)
affects: [07-08, 07-13, 14-profile]
tech-stack:
  added: []
  patterns: [stateless sheet body for Robolectric tests, merged-semantics step rows with stateDescription]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PaymentLifecycleCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ProfileMenuRow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/PaymentInfoSheet.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingPaymentInfoLink.kt
    - app/src/main/res/values/strings_payment_lifecycle.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/PaymentLifecycleCardTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/ProfileMenuRowTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaymentInfoSheetTest.kt
  modified: []
key-decisions:
  - "Test tags PAYMENT_LIFECYCLE_CARD_TAG and paymentLifecycleStepTag(index) are internal to the module; PAYMENT_INFO_LINK_TAG is public"
  - "Connector between steps is a 2x16dp box centered under the 32dp badge column"
requirements-completed: [PAY-01, PAY-07]
duration: ~25 min
completed: 2026-10-22
---

# Phase 7 Plan 04: Payment Lifecycle Card, Info Sheet and Profile Row Summary

Shared three-step PaymentLifecycleCard (Upcoming / HoldPlaced with accessible "Done" state), the "How does payment work?" link plus ModalBottomSheet built on PickerSheet, and a reusable 56dp ProfileMenuRow, all with iOS copy verbatim and Robolectric coverage.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Shared PaymentLifecycleCard and its copy | f8cb934 |
| 2 | Payment info link and sheet, ProfileMenuRow | 8e39df0 |

## Verification

- `testDevDebugUnitTest` for PaymentLifecycleCardTest (4), PaymentInfoSheetTest (3), ProfileMenuRowTest (1): pass
- `ktlintCheck`: pass
- `bash scripts/check-design-rules.sh`: "Design rules: OK"
- Acceptance greps: all as specified (no fontSize/Color literals, link has no `colorScheme.primary`, single `PickerSheet(` call)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Worktree base reset**
- **Found during:** startup branch check
- **Issue:** the worktree HEAD (d1217fb) was not based on the expected fb0e73c, so Phase 7 plan files were absent.
- **Fix:** `git reset --hard fb0e73c` as prescribed by the branch-check step (tree was clean).

**2. [Rule 1 - Bug] Test adjustments**
- **Issue:** the "Got it" button sits below the default Robolectric viewport inside the lazy list, so the first click attempt failed to find the node.
- **Fix:** the test scrolls to the node (`performScrollToNode`) before clicking. No production change.

TDD note: RED and GREEN were developed in one pass per task (tests written before the implementation, but committed together with it, to avoid committing a non-compiling test tree).

## Known Stubs

None. The link and sheet are not yet wired into the booking form (07-13), and ProfileMenuRow is not yet placed on the Profile tab; both are called out in the plan as later work.

## Threat Flags

None.

## Self-Check: PASSED

All eight created files exist; commits f8cb934 and 8e39df0 exist on the worktree branch.
