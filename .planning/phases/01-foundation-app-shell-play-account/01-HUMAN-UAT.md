---
status: partial
phase: 01-foundation-app-shell-play-account
source: [01-VERIFICATION.md]
started: 2026-10-20T14:00:00Z
updated: 2026-10-20T14:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Cold launch on API 26 and on API 31+
expected: The splash shows the ClickMe mark on #131313 with no white frame and no fixed delay. The mark is neither cropped nor oversized, and the app lands on the Explore tab. On API 26, Nunito 600 is visibly heavier than 400, and the system bar icons are light.
result: [pending]

### 2. Per-tab back stack and scroll retention on device
expected: Open the gallery from Profile, scroll halfway, switch to Explore and back. The gallery and its scroll position are still there. Re-tapping Profile pops to the root. Back on Bookings selects Explore, and Back on Explore exits the app. Tab switches cross-fade, and gallery push and pop slide.
result: [pending]

### 3. Expert scaffold via the debug switch
expected: The expert shell renders Dashboard, Bookings, Topics, Chats and Profile, and switching back restores the client shell.
result: [pending]

### 4. 200% font scale
expected: No clipping or overlap across the tabs and the gallery.
result: [pending]

### 5. Launcher icon with themed icons on and off (API 33+, dev flavor)
expected: The label is "ClickMe Dev", the mark sits inside the adaptive mask, and the themed icon is legible. See IN-04 in 01-REVIEW.md.
result: [pending]

### 6. Shared components in the debug gallery
expected: Press feel, AsyncSaveButton states, field focus and error borders, the password eye toggle, opening a Custom Tab and coming back, and the blocked-link message all behave as described in 01-UI-SPEC.md.
result: [pending]

## Summary

total: 6
passed: 0
issues: 0
pending: 6
skipped: 0
blocked: 0

## Gaps
