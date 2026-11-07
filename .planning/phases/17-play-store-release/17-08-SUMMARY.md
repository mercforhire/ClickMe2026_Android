---
phase: 17-play-store-release
plan: 08
subsystem: release
tags: [play-console, app-content, data-safety, store-listing, internal-testing, human-gate]
requires:
  - phase: 17-03
    provides: deletion page, privacy edits, 17-PAYMENTS-DECISION.md
  - phase: 17-04
    provides: 17-CONSOLE-PASTE.md
  - phase: 17-05
    provides: screenshot script, Play icon, asset manifest
  - phase: 17-06
    provides: 17-RELEASE-CHECKLIST.md
  - phase: 17-07
    provides: signed vc 6 / 1.0.0 bundle
provides:
  - D-05 and the 13 pre-submission answers confirmed by the user
  - deletion page and privacy-policy edits live on uptrendinvestments.net
  - Play Console App content complete (Needs attention empty)
  - main store listing saved with icon, feature graphic and 7 approved screenshots
  - vc 6 available on Internal testing and installed on the Samsung from Play
affects: [17-09, 17-10]
key-files:
  created:
    - .planning/phases/17-play-store-release/17-08-SUMMARY.md
    - /Users/LeonChen/ClickMe-release/store-assets/feature-graphic.jpg (outside repo)
    - /Users/LeonChen/ClickMe-release/store-assets/fgs-video.mp4, fgs-video-trimmed.mp4 (outside repo)
    - /Users/LeonChen/ClickMe-release/store-assets/01..05, 07-search, 08-reviews .jpg (outside repo, retaken)
  modified:
    - .planning/phases/17-play-store-release/17-RELEASE-CHECKLIST.md
    - .planning/phases/17-play-store-release/17-PAYMENTS-DECISION.md
    - .planning/phases/17-play-store-release/17-CONSOLE-PASTE.md
    - .planning/phases/17-play-store-release/17-STORE-ASSETS.md
    - .planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md
    - .planning/STATE.md
key-decisions:
  - "D-05: keep Stripe under Google Payments FAQ 10281818 (user, 2026-11-06); rows 2-11 as recommended"
  - "D-01: production-access gate not shown (Production offers Create new release), so vc 6 goes to Internal testing for smoke run 1, then production; no closed test, no tester service"
  - "Contact email is clickmeapp@uptrendinvestments.net everywhere, including the app (b80f99e9, in vc 6)"
  - "delete_account_body over-promise ships in vc 6 unchanged; post-release fix (STATE todo)"
  - "Store screenshots retaken as the demo account Ada Kensington; first set rejected and archived"
requirements-completed: []
duration: ~6h (human Console session)
completed: 2026-11-06
status: complete
---

# Plan 17-08 Summary: first human release gate

## What happened

**Task 1 (preflight, executor):** every artifact the Console session uses was ready. The vc 6 AAB sha256 matches ledger row 6, the paste file has 11 sections, the deletion page passes xmllint, and the decision table has 13 rows. Open at the time: the feature graphic, the video, and the unpublished web pages.

**Task 2 (decision, user):** D-05 is option-stripe-faq, and rows 2-11 were accepted as recommended. On row 12, the user checked Test and release > Production, which offers "Create new release", so the production-access gate is **not shown** (2026-11-06). Row 13: Testers Community Pro was chosen but is not needed or purchased. The delete-text fix is post-release, and the screenshots were sent for retakes. The executor recorded all of this in commit 3600fd3f, and the orchestrator recorded the STATE lines in 04e7aec0.

**Task 3 (human action, user with orchestrator support):**

| Box | Item | Evidence |
|-----|------|----------|
| 4 | Foreground-service video | Recorded over ADB on the Samsung (vc 6 Play install) after the mic permission was reset. Trimmed to 59.8 s. YouTube unlisted: https://www.youtube.com/watch?v=So1nFYW5aj0 (oEmbed 200) |
| 5 | Feature graphic | Rendered by Claude with CoreGraphics from the Play icon mark, Nunito and the app palette. 1024x500 JPEG, no alpha. Approved by the user |
| 7 | Web pages | delete-account.html 200 (clickmeapp@ x2, support@clickme.app x0); privacy.html 200 ("Firebase Cloud Messaging" x2, link to the deletion page, "Last updated 6 November 2026"). Published by the user's website agent from a change request |
| 8 | App content | The user reports Needs attention is empty; the last item was the Advertising ID declaration (No, since the manifest has no AD_ID) |
| 9 | Store listing | Text, icon, feature graphic, 7 screenshots (demo account), category Business, tags Business/Education/Communication/Self-help, contact email and website |
| 10 | Internal testing | vc 6 rolled out. Samsung: versionCode=6, versionName=1.0.0, installer=com.android.vending (H1/H2 pass). Ledger row 6: "Available on Internal testing" |

Task 3 automated verify printed `CONSOLE-LIVE` (2026-11-06).

## Answers added to the paste file during the session

- Content rating: block users → Yes; report users or content → Yes; chat moderation → Yes (human moderation through reports and the admin console, with no automated filter); interactions limited to invited friends → No.
- Data safety: no Location row. Auto-detect time zone reads `ZoneId.systemDefault()`, with no location permission.
- Tags chosen from Console's list: Business, Education, Communication, Self-help.
- AI asset declaration: the app has no generative AI. The feature graphic was drawn by code from the user's own logo, not by an image model. AI helped with the layout and copy, so answer honestly if Console asks broadly.

## Deviations

- The plan expected the user to make the feature graphic. At the user's request, Claude rendered it instead.
- The plan expected the user to record the video. The user did the steps and Claude captured over ADB (`screenrecord`) and trimmed it.
- The first screenshot set was rejected (third-party photo, gibberish chat, torch chip). It was retaken as the demo account, and two extra frames were added (07-search, 08-reviews). Play's 8th slot is reserved for 06-in-call (17-09).
- `npm run seed:app-review` before App access is a user step. It was not observed by Claude.
- The phone reports minSdk=32 for Play's device-specific split. The bundle declares minSdkVersion 26.

## Open items carried forward

- 17-09: smoke run 1 on the Internal-track install, the BUG-22/BUG-34 device re-tests and the 06-in-call screenshot.
- Test bookings created during this session (the 8:30 p.m. "Free Intro Chat", and any bookings made as Ada Kensington) need to be cancelled by the user if they are not wanted.
- Post-release: reword `delete_account_body`, and make the iOS HelpView support-email fix.
- Whether Google accepts the declarations is known only after review (17-10).
