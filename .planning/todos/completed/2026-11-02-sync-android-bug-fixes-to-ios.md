---
created: 2026-11-03T04:49:43.118Z
title: Sync Android bug fixes to iOS
area: planning
severity: major
status: closed-out-of-scope
closed: 2026-11-10
files:
  - .planning/STATE.md
  - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md
  - /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Call/Calling/MeetingCallViewModel.swift:518
---

## Problem

The Android port fixed or avoided many bugs that are still in the iOS app. They are recorded as iOS follow-up notes in `STATE.md` (Phases 6 to 14), in phase files and in the Phase 16 bug log. Nothing plans their fix on iOS. The user asked on 2026-11-02 to sync the fixes to iOS after Android is finalized.

Most notes were written while porting and have not been confirmed on an iPhone. The first item below was.

These affect live iOS users and should not wait for Android to be final, if the notes are right:

- Calls drop 5 minutes after the scheduled end. `overtimeCutoffSeconds` is `5 * 60 // TEMP: was 3600`, and its comment says to revert for production. Confirmed on the production iPhone app on 2026-11-02 (Phase 16 session A). Android ends at end + 60 minutes.
- Decline sends OUTSIDE_EXPERTISE, SCHEDULE_CONFLICT or PRICING, which the backend rejects with 422 (Phase 11 note).
- `PushEvent` parses only chat and booking-request pushes (Phase 13 note).
- Profile language edits never save, because auto-save does not send languages (Phase 14 note).

## Solution

1. First, have the user commit or review the iOS repo's 54 uncommitted changes (counted 2026-11-02), which include the call file above. Do not touch them without the user's decision.
2. Build one list from `STATE.md`, the phase files and `16-BUG-LOG.md`, with duplicates removed.
3. Check each item against the current iOS code.
4. Sort into real iOS bugs, by-design differences and items that need a backend change.
5. Run it as its own GSD milestone in the iOS repo, with fix, test and device-check steps, then ship a new iOS build.

Phase 16 and the Android release come first, except for any urgent item above that the user chooses to fix sooner.

**Added 2026-11-06 (Phase 17):** iOS `Views/Screens/Settings/Help/HelpView.swift` line 20 has `supportEmail = "support@clickme.app"`, which the user says is wrong. The correct address is `clickmeapp@uptrendinvestments.net`, as in the published privacy policy. Android fixed this in b80f99e9 (`AppLinks.SUPPORT_EMAIL`).

**Closed 2026-11-10:** out of scope. The user said this project is only concerned with the Android build. The iOS list above is kept for reference only; no Android work follows from it.
