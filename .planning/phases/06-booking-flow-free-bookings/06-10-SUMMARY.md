---
phase: 06-booking-flow-free-bookings
plan: 10
subsystem: booking
tags: [compose, request-sent, animation, empty-state]
requires: [06-03, 06-05]
provides:
  - BookingRequestSentScreen / BookingRequestSentContent
  - subviews BookingRequestSentCheckIcon / Headline / DetailsCard / Actions
  - strings_request_sent.xml (16 strings)
affects: [06-13 navigation wiring]
tech-stack:
  added: []
  patterns: [Animatable per block with rememberSaveable played flag, graphicsLayer fade-slide keeping layout space]
key-files:
  created:
    - app/src/main/res/values/strings_request_sent.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentCheckIcon.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentDetailsCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentActions.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreenTest.kt
  modified: []
key-decisions:
  - "Animation state lives in the Loaded branch composable, so a Failed -> Retry -> Loaded sequence plays it then; `played` is set when the animation ends and is rememberSaveable"
  - "Animatables start at 1f when `played` is already true, so a restored screen never shows a transparent frame"
  - "Expert name is substituted via a marker in the formatted string, so the name is never a format pattern and can be styled as its own span (T-06-31)"
requirements-completed: [BOOK-04]
duration: ~25 min
completed: 2026-10-22
---

# Phase 6 Plan 10: Request Sent screen Summary

The Request Sent screen now renders Loading, Failed (fixed copy plus Retry) and Loaded states, with the check badge, headline, details card (device time format, "Awaiting Approval" pill) and the two actions, plus a 700 ms staggered entry animation that plays once per successful load and keeps the buttons disabled until they are fully visible.

## Tasks

| Task | Commit | Notes |
|------|--------|-------|
| 1. Strings and four subviews | 011eccb | 7 subview tests |
| 2. Screen, states, entry animation | 1f734bb | 6 screen tests |

## Verification

`./gradlew :app:testDevDebugUnitTest --tests '*BookingRequestSentScreenTest' --tests '*BookingRequestSentViewModelTest' ktlintCheck` passes: BookingRequestSentScreenTest 13/13, BookingRequestSentViewModelTest 11/11, ktlint clean. `bash scripts/check-design-rules.sh` passes. 16 `request_sent_` strings; no `.blur(`, Glow or PaymentLifecycle in the package.

## Deviations from Plan

- The worktree HEAD was not on the expected base, so it was reset to cbcae55 as the startup check prescribes.
- RED and GREEN were committed together per task (tests cannot compile before the declarations exist).
- The acceptance grep `grep -c collectAsStateWithLifecycle` prints 2, not 1: the import line plus the single call. There is one usage.
- The loaded-content test scrolls each button into view before clicking, because the Robolectric window is smaller than the column.
- The Scaffold `innerPadding` already carries the navigation-bar inset, so no separate inset modifier was added on top of the `Spacing.xxl` bottom padding.

## Deferred / manual

- Animations-off (system animator scale 0) check on a device (RESEARCH A2) and 200% font scale check remain manual items.

## Known Stubs

None.

## Threat Flags

None. T-06-31 (plain spans, name used as a format argument) and T-06-32 (actions disabled until visible, tested with a paused clock) are mitigated.

## Self-Check: PASSED
