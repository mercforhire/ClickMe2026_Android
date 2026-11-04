---
phase: 16-cross-platform-qa
plan: 05
subsystem: qa
tags: [qa, bug-fix, fix-batch, versioncode, live-money]
requires:
  - phase: 16-cross-platform-qa
    provides: session A results and the Fix batch A input list
provides:
  - versionCode 4 on the Play internal track with BUG-05 to BUG-12
  - First paid bookings completed on Android, both voided
affects: [16-06, 16-07, 16-08]
key-files:
  created:
    - .planning/phases/16-cross-platform-qa/16-05-SUMMARY.md
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeResultMapping.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeFailureLog.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/ClickMePaymentSheetStyle.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEventMapping.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/splash/SplashScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffold.kt
    - app/build.gradle.kts
    - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md
    - .planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md
    - .planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md
    - .planning/STATE.md
key-decisions:
  - "The user approved all seven fix-if-cheap bugs for this batch, overriding the plan's budget of 3; the 2-file limit still applied"
  - "BUG-22 (controls hidden above End Call) waits for the next batch because scrolling reaches them"
requirements-completed: []
completed: 2026-11-03
status: complete
---

# Phase 16 Plan 05: Fix Batch A Summary

**versionCode 4 is on Play and paid booking now works on Android: BUG-05 is verified with a real hold on Stripe, which was then voided.** No money has been charged.

## Re-test on versionCode 4

Proof: installer `com.android.vending`, versionCode 4, lastUpdateTime 2026-11-03 20:25:37, backend `2ec82af`, crash count 0.

| Bug | Row or check | Result |
|---|---|---|
| BUG-05 paid booking | D1.18 | verified; Stripe read as Uncaptured, ending WIUsEq |
| BUG-06 stuck spinner | D1.23 | verified; dark mode switched with the sheet open, relaunch seen in logcat |
| BUG-07 Stripe Link | sheet screenshot | verified |
| BUG-08 failure logging | none | not exercised: no Stripe failure occurred; JVM tests only |
| BUG-09 stuck on Waiting | D1.10 | verified with a limit below |
| BUG-10 End Call | banner screenshot | verified; side effect is BUG-22 |
| BUG-11 splash glow | D1.SP1 | verified by recording and user |
| BUG-12 tab labels | client tabs | verified on client tabs; expert label not read |
| Hold release | D1.19 | pass; Stripe read as cancelled |
| Smoke | login, free booking, chat, call join, push | pass, reported by the user |

**Limits of this evidence:**
- **BUG-09:** Agora sent offline and joined each time, so the no-offline path seen on versionCode 3 did not recur. That path is proven by JVM tests only.
- **Amounts:** the user read each PaymentIntent's status but not its amount, so CA$5.00 is the app's displayed price, not a Stripe reading.
- **D1.19:** the snackbar was not stated.
- **D1.23:** the challenge and JPY parts were not run (no 3DS card, no JPY topic).
- **BUG-12:** readability at the largest font was not stated.
- **Smoke:** the call join and push steps are the user's report; I did not read those log lines.

## Paid bookings

Both voided by the app's cancel flow: `efd9a2e1` (PaymentIntent ending WIUsEq) and the Nov 6 booking (ending gAi5kj; booking id not read). Section 7's sweep should still confirm both.

## New bugs

BUG-22 (call controls hidden above End Call in the last 5 minutes) and BUG-23 (Past card offers Leave a review after an app restart) are open as fix-if-cheap for the next batch.

## Deviations

- **Run in a worktree.** The plan asks for the main checkout, but the isolation guard requires a worktree for executors. The fixes were built there and merged; lint, the bump and the signed build ran in the main checkout.
- **Lint caught what the worktree gate didn't.** The first release run stopped on 5 lint errors from the BUG-08 and BUG-11 work. They were fixed in `a31bccce` before the bump.
- **Existing tests edited.** `MeetingCallScreenTest` no longer scrolls to End Call, because it is now pinned. `AgoraManagerCreateEngineTest` asserts the added event. The sheet configuration equality test expects the Link setting. No assertion was removed.
- **Backend moved during the session.** Production went from `8d4450f` to `2ec82af` (decline reason optional, validation error messages). Not from this plan.
- **Account question.** The client account opens in expert mode after a fresh login because it has a topic. This is the app's rule, and my reading of iOS is that it is similar. It doesn't fully explain the 17:09 screen in BUG-20, which stays low priority.

## Task Commits

- BUG-05: `f94d6bbf`
- BUG-06 to BUG-12: `0c2a12bc`, `85c1e6da`, `b303e452`, `f15d472c`, `a961760a`, `3300dcdf`, `7c24288e`
- Lint fixes: `a31bccce`
- versionCode 4: `469acd78`
- Ledger: `0c350457`
- Task 3: the commit that adds this file
