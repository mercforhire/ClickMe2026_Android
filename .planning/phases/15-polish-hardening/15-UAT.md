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
