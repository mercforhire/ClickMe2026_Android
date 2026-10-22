---
status: partial
phase: 05-explore-expert-profile
source: [05-VERIFICATION.md]
started: 2026-10-22T03:00:00Z
updated: 2026-10-22T03:00:00Z
---

## Current Test

[awaiting human testing — deferred by user preference; non-blocking]

## Tests

### 1. SC-1 Explore on device (Samsung SM-A075F, devDebug against production, logged in as a client)
expected: The hero, Trending Categories (6 icon chips), Featured Topics and Recommended Experts load from /client/home. A free topic reads "Free" in green. With a confirmed booking today, Today's Sessions shows it with a disabled Join Call. A chip tap filters only the experts list, and a second tap clears it. View all opens the sheet, where search, multi-select and Apply filter the list and the chip row reflects the selection. When a filter matches nothing, "No experts in this category yet" appears with a Clear filter action. Pull-to-refresh works. Airplane mode on first load shows a Retry in each section.
result: [pending]

### 2. SC-2 Category cache on device
expected: Open View all while online, force-stop the app, relaunch in airplane mode and open View all again. The list loads from the cache with no error. Optional: move the device date 8 days ahead and confirm the app refetches.
result: [pending]

### 3. SC-3 Expert Profile on device
expected: An expert card opens the profile with a hero (static glow, ring, no online dot), stats, Book Session, About, Expertise, Topics (Free in green, prices to full decimals) and up to 3 reviews. Empty sections are hidden. Opening from a Featured Topic shows the hero filled in. Book Session opens the "Book a Session" stub with no bottom bar, and Back returns. A suspended or deleted expert shows "This profile isn't available".
result: [pending]

### 4. Visual and accessibility pass
expected: At 200% font scale on Explore, the sheet and the profile, cards grow and nothing clips. Sheet search works with the keyboard open. Pull-to-refresh feels right. Skeletons transition to content cleanly on a slow network. Smoke-test the release build once the Phase 2 keystore exists.
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
