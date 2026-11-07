---
phase: 17-play-store-release
plan: 09
subsystem: release
tags: [human-gate, smoke-test, production-access, bug-22, bug-34, store-assets]
requires:
  - phase: 17-08
    provides: vc 6 on Internal testing, App content clear, store listing saved, gate not shown
provides:
  - smoke run 1 passed on the Play install of vc 6 (S1.00-S1.10)
  - BUG-22 and BUG-34 verified on the Samsung
  - 06-in-call.jpg captured and approved (8th phone screenshot)
  - production access recorded as not required (gate not shown)
affects: [17-10]
key-files:
  created:
    - .planning/phases/17-play-store-release/17-09-SUMMARY.md
    - logs/smoke-1/S1.05-BUG-34-waiting.png
    - logs/smoke-1/S1.05-BUG-34-waiting-scrolled.png
    - logs/smoke-1/S1.07-BUG-22-last5.png
    - /Users/LeonChen/ClickMe-release/store-assets/06-in-call.jpg (outside repo)
  modified:
    - .planning/phases/17-play-store-release/17-RELEASE-CHECKLIST.md
    - .planning/phases/17-play-store-release/17-STORE-ASSETS.md
    - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md
    - .planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md
    - .planning/STATE.md
key-decisions:
  - "vc 6 is the binary to promote: smoke run 1 passed on a fresh Play install with no fix build needed"
  - "First in-call frame rejected (real profile photos, one of another person); retaken with the demo accounts Ada Kensington (client) and Marcus Webb (expert) and approved"
  - "Gate not shown, so Task 3 recorded no closed test and no production-access application"
requirements-completed: [REL-07]
completed: 2026-11-06
---

# Phase 17 Plan 09: Smoke run 1 and production access Summary

**Smoke run 1 passed on the Play install of versionCode 6. Both call-screen fixes (BUG-22, BUG-34) were verified on the Samsung, the in-call store screenshot was approved, and no production-access application is needed.**

## Results

| Row | Result |
|-----|--------|
| S1.00 | pass: preflight (Task 1, c9cba293) plus the iPhone expert state confirmed by the user |
| S1.01 | pass: fresh Play install, versionCode 6 / 1.0.0, installer com.android.vending, firstInstallTime 2026-11-06 21:50:42 |
| S1.02 | pass: new client signed up, landed on client home |
| S1.03 | pass: free topic booked, Awaiting Approval shown, iPhone got the request push. Booked as the user's own client account (see Deviations) |
| S1.04 | pass: accepted on the iPhone, Samsung push and Confirmed |
| S1.05 | pass (BUG-34): font 2.0 and density 340 set over ADB; after a swipe up all four controls are reachable above End Call. Cosmetic: "Minimize" and "Speaker" wrap mid-word at 200% (not filed) |
| S1.06 | pass: two-way audio, Mute, Speaker, Home and the ongoing notification back to the call |
| S1.08 | pass: 06-in-call.jpg 1080x1920, check OK, approved; display restored to 720x1600 / 300 |
| S1.07 | pass (BUG-22): 22:56 capture at font 1.0 with the "Session ending soon" banner and "3:50 left" chip; all four controls and labels visible above End Call without scrolling |
| S1.09 | pass: 0 crash-buffer lines, 0 FATAL EXCEPTION or ANR; CallService stopped and the ongoing notification removed |
| S1.10 | pass: smoke client deleted in the app, old email no longer signs in |

Task 3: gate not shown, so boxes (11) and (13) are ticked as not applicable and STATE says "[Phase 17] production access: no gate, not required".

## Deviations from Plan

1. **S1.03 to S1.06 ran on the user's own client account, not the S1.02 client.** The user chose this. Signup was proven separately at S1.02, and deleting the account was proven at S1.10 on the S1.02 client.
2. **S1.07 to S1.09 ran on a second call between the demo accounts** (Ada Kensington on the Samsung, Marcus Webb on the iPhone, "Free PM Intro Chat" 10:30-11:00 p.m.). The user switched accounts so the in-call screenshot would have no real faces. Both calls were on the same vc 6 Play install.
3. **At the S1.07 capture the iPhone had already left the call** (the screen showed "Waiting for other person..."). The BUG-22 layout condition was still met: banner and chip were showing at font 1.0.
4. **The Task 3 automated check has a regex typo.** `production access (approved|: no gate)` needs a space before the colon, while the plan's text says `production access: no gate`. The STATE line follows the text. It passes with `production access ?(approved|: no gate)`. 17-10 line 103 has the same typo.
5. In-call status bar: SystemUI demo mode is ignored on One UI (already known), so the frame shows the live clock and battery.

## User to-dos

- Upload `/Users/LeonChen/ClickMe-release/store-assets/06-in-call.jpg` to the 8th phone-screenshot slot of the main store listing.
- Cancel leftover test bookings: tonight's 10:30 p.m. "Free 1-hour chat" (own client account with the expert) if still open, and Ada's Nov 9 Marcus Webb ($180) and Jonas Berg bookings unless they are seeded demo data.

## Next

17-10: production release (100%, CA/US), smoke run 2, close-out.
