---
status: testing
phase: 15-polish-hardening
source: [15-VERIFICATION.md]
started: 2026-10-30T02:23:37Z
updated: 2026-10-30T02:23:37Z
---

## Current Test

number: 1
name: L1/B1/C1/K1 - complete each SC-4 flow with real TalkBack (log in, book a free session, join a call, send a chat message)
expected: |
  Reading order, names, roles, states, headings and focus match the semantics the Robolectric tests assert; nothing is spoken twice
awaiting: user response

## Tests

### 1. L1/B1/C1/K1 - complete each SC-4 flow with real TalkBack (log in, book a free session, join a call, send a chat message)
expected: Reading order, names, roles, states, headings and focus match the semantics the Robolectric tests assert; nothing is spoken twice
result: [pending]

### 2. Y1-Y7, Y11 - rotate the Samsung, toggle dark mode and font scale, use 'Don't keep activities', force-stop during a call, and install prodRelease (R8) then restore from a deep screen
expected: Same screen and typed text kept, secrets empty, call never auto-rejoins, back stack restores under R8 (add a NavKey keep rule if it lands on the start screen)
result: [pending]

### 3. Y2, Y8, Y9, Y16, C4, C5 - resizable emulator (900x400, 900 dp square) plus cutout, gesture and 3-button navigation
expected: No content clipped or hidden behind bars, cutouts or the IME; 600 dp column centered; call controls reachable on short windows
result: [pending]

### 4. Y12 - run Accessibility Scanner over the four SC-4 flows
expected: No touch-target, contrast or label findings (or findings logged)
result: [pending]

### 5. SC-1 Week 7 checkpoint - user confirmation that loading, empty and error-with-retry states look right on the screens (rows E3, M2, M3, N3, T5, T7, Y13)
expected: Skeleton, empty and error with Retry on each screen; failed refresh keeps content and shows one Retry snackbar
result: [pending]

### 6. M8 / decision D6 - after a successful Retry on Upcoming Booking Details or the expert Booking Summary, where does TalkBack focus land?
expected: Plan 15-18 truth (verification: backstop) says the app bar title; it was NOT implemented. Decide: implement, accept, or move to Phase 16
result: [pending]

### 7. B4 / decision D7 - tap reliability of the 45.7 dp calendar day cells on a 360 dp Samsung
expected: No missed taps; otherwise reduce card padding
result: [pending]

## Summary

total: 7
passed: 0
issues: 0
pending: 7
skipped: 0
blocked: 0

## Gaps

## Phase 16 device results (16-MATRIX-RUNBOOK.md)

Results from the Phase 16 device matrix on the Play internal-track build (versionCode 5, Samsung SM-A075F). The tests above and the frontmatter are unchanged; verify-work owns them. TalkBack was not run in Phase 16: the user skipped all 23 TalkBack rows on 2026-11-05, so POL-04 and Phase 15 SC-4 are not confirmed on a device (semantics are covered by Robolectric tests only).

- Test 1 (L1, B1, C1, K1 with TalkBack): TB.L1, TB.B1, TB.C1, TB.K1 not run, skipped by user decision 2026-11-05. Not confirmed.
- Test 2 (Y1-Y7, Y11): PD.Y1 pass, PD.Y4 pass, PD.Y5 pass, PD.Y6 pass, PD.Y7 and FS.Y7 pass, PD.Y11 pass (R8 under Play, deep screen restored), all vc 5, 2026-11-05. PD.Y3 (signup walk with Don't keep activities) not run, skipped by user decision. The push half of the force-stop case (PD.C7) is not testable on Android: a force-stopped app receives no FCM. 6 of 7 rows pass.
- Test 3 (Y2, Y8, Y9, Y16, C4, C5): LW.Y2 and LW.C4 not run in Phase 16, post-release (resizable emulator, D-06); IM.Y8, IM.Y9, IM.Y16 and IM.C5 not run, skipped by user decision 2026-11-05. Not confirmed.
- Test 4 (Y12 Accessibility Scanner): TB.Y12 and FS.Y12 not run, skipped by user decision 2026-11-05. Not confirmed.
- Test 5 (SC-1 Week 7: E3, M2, M3, N3, T5, T7, Y13): T2.E3, T2.M2, T2.M3, T2.N3, T2.T5, T2.T7, T2.Y13 not run in Phase 16, post-release (D-06).
- Test 6 (M8 focus after Retry): T2.M8 not run in Phase 16, post-release (D-06). Decision D6 stays open.
- Test 7 (B4 day-cell taps): TB.B4 not run, skipped by user decision 2026-11-05. Decision D7 stays open.
