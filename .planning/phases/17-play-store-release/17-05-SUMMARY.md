---
phase: 17-play-store-release
plan: 05
subsystem: release
tags: [store-assets, screenshots, adb, play-icon, robolectric, d-16, tracer]
status: complete

requires:
  - phase: 16-cross-platform-qa
    provides: Samsung SM-A075F harness (H1 transport id), Play install signed in
provides:
  - scripts/play-store-screenshot.sh (prep, capture, check, restore, session)
  - five 1080x1920 store screenshots and the 512x512 Play icon in /Users/LeonChen/ClickMe-release/store-assets/
  - 17-STORE-ASSETS.md manifest with spec checks, owners, status and third-party flags
affects: [17-08 Console session (user approves each frame), 17-09 smoke run 1 (in-call screenshot)]

actuals:
  tokens: 9000
  tasks: 3
  commits: 3
plan_head_before: 25942db1b3ae502aa5036bd56faeed737fa75bec
plan_head_after: b808d9ff63b76c66b428f8a905c2c2c37976635e

tech-stack:
  added: []
  patterns: ["adb -t <transport_id> only; always-restore wm override and demo mode", "one-off Robolectric run (never committed) to export a launcher icon"]

key-files:
  created:
    - scripts/play-store-screenshot.sh
    - .planning/phases/17-play-store-release/17-STORE-ASSETS.md
  modified: []

key-decisions:
  - "wm size 1080x1920 / density 420 is honoured on One UI, so the native-crop fallback was not needed (RESEARCH A6, display half confirmed)"
  - "SystemUI demo mode is ignored on One UI; frames keep the live status bar and are flagged instead of cropped"

requirements-completed: [REL-04]

duration: 45 min
completed: 2026-11-06
---

# Phase 17 Plan 05: Store assets Summary

Screenshot capture script for the Samsung (1080x1920 wm override, JPEG with no alpha, 2:1 ratio check, always-restore), five validated store screenshots, and a 512x512 Play icon rendered from the shipped adaptive launcher icon, all listed in a manifest for the user's approval at 17-08.

## Accomplishments

- `scripts/play-store-screenshot.sh`: `prep`, `capture <name> [--native]`, `check <file>`, `restore`, `session <cmd...>` (trap-restore). `check` was exercised against a native 720x1600 frame (FAIL: long side more than twice the short side) and a 720x1440 JPEG (OK), then against the real captures.
- Five phone screenshots in `/Users/LeonChen/ClickMe-release/store-assets/` (outside the repo), each `OK 1080x1920 alpha=no`: `01-explore.jpg`, `02-expert-profile.jpg`, `03-book-a-session.jpg`, `04-bookings.jpg`, `05-chat.jpg`.
- `play-icon-512.png`: 512x512, `hasAlpha: yes`, 64 KB, rendered by a one-off Robolectric test (`@GraphicsMode(NATIVE)`, xxxhdpi) from `R.mipmap.ic_launcher` (surface background plus foreground, no mask). Viewed: ClickMe mark centred and unclipped on the dark surface colour. The temporary test was deleted before any commit and never entered history; no launcher resource changed.
- `17-STORE-ASSETS.md` lists all nine assets (screenshots 1-5 captured, 06-in-call open for 17-09, icon captured, feature graphic and foreground-service video open and owned by the user).

## RESEARCH A6 outcome (flagged assumption)

- **Display override: honoured.** `wm size 1080x1920` and `wm density 420` took effect on the Samsung; `uiautomator` bounds and screencap came back at 1080x1920. The native-crop fallback (2:1) was not used, so the frames meet the 9:16 recommendation.
- **SystemUI demo mode: not honoured.** The status bar kept the live clock, notification bell, a blue "Flashlight" chip (the torch was on) and the real battery level. Frames were not cropped or retouched; the manifest flags this for the user.

## Device state

- Samsung SM-A075F (transport 8048, wireless). It was locked behind a credential at first (the credential screen blocks screencap; the executor never enters credentials), so I waited and re-checked; the user unlocked it and left the app open on an expert profile screen.
- Navigation only: tabs, an Explore expert card, Book Session, one calendar day (Nov 10, no time slot and no booking button tapped), Bookings tab, Chats tab and the user's own thread, Back. No send, book, pay, save, accept, decline, cancel, join, delete or report.
- Left in: the app in Client Mode (Explore tab), as found; `wm size` shows only `Physical size: 720x1600` (no Override), `wm density` shows only `Physical density: 300`, `sysui_demo_allowed` is 0 (its starting value). `/sdcard/clickme-ui.xml` removed.
- No screen was skipped. Bookings used the client tab (it had data), so the expert Bookings fallback was not needed.

## Open human items

- User approves or replaces every frame at 17-08 (T-17-13). Third-party content flags: 01, 04 and 05 show the signed-in account's profile photo, which contains two people; 02 shows another account's name and photo (looks like seeded demo data); 03 is clean.
- 05-chat message bodies are keyboard gibberish ("Bdhdhd"); consider a retake after a nicer demo exchange.
- Status bar on all five frames shows the live clock, bell and a "Flashlight" chip; retake with the torch off if preferred (`prep`, `capture`, `restore`).
- Feature graphic (1024x500, no alpha) and the 30-60 s foreground-service video are the user's.
- `06-in-call.jpg` is captured in smoke run 1 (17-09).

## Verification

- `bash -n scripts/play-store-screenshot.sh`: OK; `test -x`: OK; `sysui_demo_allowed` and `wm size reset` present in the script.
- `check` on 01-05: `OK ... 1080x1920 alpha=no` for all five (SCREENSHOTS-OK, 5 of 5; manifest has 6 `| captured |` rows including the icon).
- Icon: `sips` reports 512, 512, `hasAlpha: yes`; 65579 bytes (limit 1048576).
- `git status --porcelain` shows no image in the repo and nothing under `app/src`.
- Task verify strings PIPELINE-OK / SCREENSHOTS-OK / ICON-OK were checked in their component parts (the compound one-liners are refused by the worktree command guard), each part passing.

## Deviations from Plan

None - plan executed exactly as written, with two ordering notes: the Task 1 commit was split into the script commit (`feat`) and the manifest commit, and the first device check found the Samsung locked, so screenshots were taken after the user unlocked it.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- scripts/play-store-screenshot.sh, 17-STORE-ASSETS.md, 01-05 JPEGs and play-icon-512.png exist.
- Commits 064f1068, 451e6072, b808d9ff are ancestors of HEAD.
