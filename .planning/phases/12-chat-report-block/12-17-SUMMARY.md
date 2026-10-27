---
phase: 12-chat-report-block
plan: 17
subsystem: expert-profile-safety
tags: [gap-closure, expert-profile, snackbar, gate, review-disposition]
requires:
  - phase: 12-chat-report-block (12-16)
    provides: the other three gap fixes (WR-01, WR-03, IN-02) merged into the base
provides:
  - Expert Profile report and block confirmations that stay on screen (CR-01)
  - Gap-pass gate result, review dispositions, VALIDATION rows and STATE carry-forward
affects: [phase-12-verification, phase-13]
tech-stack:
  added: []
  patterns: ["snackbar shown with scope.launch from rememberCoroutineScope so consuming a state flag cannot cancel it"]
key-files:
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreenTest.kt
    - .planning/phases/12-chat-report-block/12-REVIEW-DISPOSITION.md
    - .planning/phases/12-chat-report-block/12-VALIDATION.md
    - .planning/STATE.md
decisions:
  - "Show the profile snackbar on the composition scope, same shape as ChatThreadScreen, instead of changing the LaunchedEffect key"
metrics:
  completed: 2026-10-27
status: complete
actuals:
  tokens: 14000
  tasks: 2
  commits: 2
plan_head_before: a4651afbf68997681e2d854684aa446f79735b0e
plan_head_after: 30205f7e03de97fe600d92845c5a887afac06400
---

# Phase 12 Plan 17: Expert Profile confirmations and gap-pass gate Summary

The Expert Profile report ("Thanks, we'll review this.") and block ("You blocked {name}" or "You blocked this user") snackbars now stay up for their normal Short duration, and the Phase 12 gap pass (12-16 + 12-17) is proven by one green scoped gate and recorded in the ledger, validation map and STATE.

## What was done

**Task 1 (commit 236a45a, fix):** `ExpertProfileScreen` called `safety.consumeSnackbar()` and then `snackbarHostState.showSnackbar(message)` inside `LaunchedEffect(safetyState.snackbar)`. Consuming changed the effect key, so recomposition cancelled the effect and `showSnackbar` dismissed the snackbar within a frame (CR-01). The effect now does `scope.launch { snackbarHostState.showSnackbar(message) }` on a `rememberCoroutineScope()` and then consumes the flag, the same shape as `ChatThreadScreen`. `ProfileSafetyViewModel`, the strings and the requests are untouched.

Three Robolectric tests on the real `ExpertProfileScreen` (ViewModels built in the test body, never-completing details/reviews handlers): `reportSuccessShowsThanksSnackbar`, `blockSuccessShowsYouBlockedName`, `blockSuccessWithoutNameShowsFallback`. Each also asserts `safety.state.value.snackbar` is null (consumed once).

**RED evidence:** the tests were written first and run against the unfixed screen. All three failed at HEAD at `assertIsDisplayed` with the default variant (clock autoAdvance on); the `autoAdvance = false` fallback was not needed. After the fix they pass together with `ProfileSafetyViewModelTest`, `ExpertProfileViewModelTest` and ktlintCheck. Test and fix are in one commit (the plan marks the task tracer and the project convention commits tests with the implementation).

**Task 2 (commit 30205f7, docs):**
- Gate on the merged tree: `:app:testDevDebugUnitTest` for `*screens.chat.*`, `*ProfileSafetyViewModelTest`, `*ExpertProfile*` = 343 tests, 0 failures (baseline 267 before the gap pass); `ktlintCheck`; `:app:assembleDevDebug`; `:app:lintDevDebug` (run separately with `-Dorg.gradle.jvmargs=-Xmx4g`, no errors); `scripts/check-design-rules.sh` prints "Design rules: OK".
- `12-REVIEW-DISPOSITION.md`: CR-01, WR-01, WR-03, IN-02 `fixed` with plan/task notes; the other eight stay `open` ("Not in the 12-16/12-17 gap pass; carried in STATE"); front matter `open: 8`, `fixed: 4`; gap-pass sentence added. (`updated` was already 2026-10-27.)
- `12-VALIDATION.md`: rows 12-16-01..03 and 12-17-01..02 (green), gate result appended under Approval; `nyquist_compliant: true` and `status: complete` unchanged.
- `STATE.md` body only: new pending todo "[Phase 12] Deferred review findings" with the eight open findings, and re-check notes appended to DEFERRED (human) items (b), (c), (d). Front matter and Current Position untouched; ROADMAP.md untouched.

## Deviations from Plan

None - plan executed as written. The row format in the VALIDATION map follows the file's existing 10-column layout (Task ID, Plan, Wave, Requirement, Threat Ref, Secure Behavior, Test Type, Automated Command, File Exists, Status) rather than the 6-column sketch in the plan.

## Known Stubs

None.

## Threat Flags

None. T-12-48 is mitigated (confirmation visible, consumed once, tested); no new surface.

## Open items (deferred by the user, not blocking)

Device re-checks recorded in STATE under "[Phase 12] DEFERRED (human)": (b) Send then Back at once, (c) profile confirmations on a Samsung device, (d) Unblock from Report & Block returns the composer.

## Self-Check: PASSED

- Files exist: ExpertProfileScreen.kt, ExpertProfileScreenTest.kt, 12-REVIEW-DISPOSITION.md, 12-VALIDATION.md, STATE.md.
- Commits 236a45a and 30205f7 are on the branch; acceptance greps print 4, 8, 5, 1, 1, 1.
