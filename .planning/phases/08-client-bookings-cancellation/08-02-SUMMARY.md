---
phase: 08-client-bookings-cancellation
plan: 02
subsystem: client-bookings
tags: [pure-logic, bucketing, status, navigation-handoff]
requires: []
provides:
  - buildBookingLists / isRejoinableCompleted / BookingLists / PastRow
  - BookingStatusDisplay tables (past, detail, summary), PaymentPill, CANCELLABLE_BOOKING_STATUSES
  - BookingsLaunchRequest singleton one-shot (BookingsTab, BookingsNotice, BookingsLaunch)
affects: [08-03, 08-06, 08-07, 08-08]
tech-stack:
  added: []
  patterns: [pure top-level functions with no Android imports, consume-on-read StateFlow holder]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBuckets.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingStatusDisplay.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingsLaunchRequest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBucketsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingStatusDisplayTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingsLaunchRequestTest.kt
  modified: []
key-decisions:
  - "Details status pill shows the real status (D-14); iOS hardcoded Confirmed is an iOS follow-up for plan 08-15"
  - "Payment pill only for paid bookings with held, captured or refunded (D-15)"
  - "Cross-stack post-cancel hand-off is a @Singleton StateFlow holder consumed on read (D-07)"
requirements-completed: [BOOK-05, BOOK-06, BOOK-08]
duration: ~25 min
completed: 2026-10-23
---

# Phase 8 Plan 02: Booking buckets, status display and launch holder Summary

Pure, Android-free decision units for My Bookings: the rejoin-rule list builder, status and payment-pill tables with the cancel allow-list, and a one-shot `@Singleton` holder that lets a finished cancel hand My Bookings a tab and notice.

## Tasks

| Task | Commit | Result |
|------|--------|--------|
| 1. Status, payment-pill, cancellable tables | ed92db6 | 8 JVM tests |
| 2. buildBookingLists with rejoin rule | b2e248f | 7 JVM tests |
| 3. BookingsLaunchRequest one-shot holder | b86515f | 4 JVM tests |

Verification: `:app:testDevDebugUnitTest` for the three test classes and `ktlintCheck` both exit 0. All acceptance greps hold (distinctBy x2, no `android` imports, no `Instant.now()`, one `@Singleton`, one `getAndUpdate { null }`).

## Deviations from Plan

**1. [Rule 3 - Blocking] Worktree base correction.** The worktree started at `d1217fb`, behind the required base `3edeb7e`; reset to the base per the startup check (no work lost).

**2. [Rule 3 - Style] ktlint line wrapping.** Two lines in `BookingStatusDisplay.kt` and two test expressions in `BookingBucketsTest.kt` were re-wrapped to satisfy ktlint (max line 120). No behavior change.

Otherwise the plan was executed as written. TDD note: RED was confirmed for Task 1 (compile failure on missing symbols); Tasks 2 and 3 tests were written together with their implementations and verified in one run, then committed per task (test and implementation share a commit).

## Threat model

T-08-06 (unknown statuses) mitigated: Past drops unlabeled statuses, details pill falls back to a neutral humanized label, Cancel gated on an explicit allow-list. T-08-07 (duplicate ids) mitigated with tested `distinctBy` on both lists. T-08-08 and T-08-09 accepted as planned.

## Known Stubs

None.

## Self-Check: PASSED

All six created files exist and commits ed92db6, b2e248f, b86515f are in the branch log.
