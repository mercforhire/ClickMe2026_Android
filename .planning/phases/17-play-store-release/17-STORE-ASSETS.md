# Phase 17 Store Assets Manifest (D-16)

All store assets live outside the repo in `/Users/LeonChen/ClickMe-release/store-assets/` (images of app data never enter git, as with the Phase 16 evidence). The user approves or replaces every frame at the 17-08 Console session before anything is uploaded; specs come from section 10 of `17-CONSOLE-PASTE.md` (written by plan 17-08) and RESEARCH "Release Mechanics". Phone screenshots: JPEG or 24-bit PNG with no alpha, each side 320-3840 px, long side at most twice the short side (the Samsung's native 720 x 1600 is 2.22 and fails; `scripts/play-store-screenshot.sh prep` overrides the display to 1080 x 1920). The 512 x 512 icon is the one asset that is a 32-bit PNG with alpha (RESEARCH Pitfall 9). The feature graphic is JPEG or 24-bit PNG with no alpha.

Capture tool: `scripts/play-store-screenshot.sh` (`prep`, `capture <name> [--native]`, `check <file>`, `restore`, `session <cmd...>`).

| # | File | Content | Spec check | Owner | Status | Third-party content | Approved at 17-08 |
|---|------|---------|------------|-------|--------|---------------------|-------------------|
| 1 | 01-explore.jpg | Explore tab with expert cards | not run | executor | open: Samsung locked behind a screen credential at execution time (see Open items) | not assessed | [ ] |
| 2 | 02-expert-profile.jpg | Expert profile | not run | executor | open: same blocker as row 1 | not assessed | [ ] |
| 3 | 03-book-a-session.jpg | Book a Session (topic and day, no booking tap) | not run | executor | open: same blocker as row 1 | not assessed | [ ] |
| 4 | 04-bookings.jpg | Bookings tab (client or expert, whichever has data) | not run | executor | open: same blocker as row 1 | not assessed | [ ] |
| 5 | 05-chat.jpg | A chat thread between the user's own accounts | not run | executor | open: same blocker as row 1 | not assessed | [ ] |
| 6 | 06-in-call.jpg | In-call screen, captured in smoke run 1 (plan 17-09) | not run | executor | open: planned for 17-09 | not assessed | [ ] |
| 7 | play-icon-512.png | 512 x 512 Play icon rendered from the existing adaptive launcher icon (surface background plus `ic_launcher_foreground`, no mask; Google applies its own) | 512x512, alpha yes, 64 KB (limit 1024 KB) | executor | captured | none | [ ] |
| 8 | feature-graphic.jpg | 1024 x 500 feature graphic | not made | user | open: user makes it (JPEG or 24-bit PNG, no alpha) | none | [ ] |
| 9 | foreground-service-video | 30-60 s screen recording of the microphone foreground service, unlisted link | not made | user | open: user records and links it | none | [ ] |

## Open items

- **Rows 1-5 (phone screenshots):** the Samsung SM-A075F (transport 8048) was on ADB wirelessly with the Play install present (`installer=com.android.vending`), but its lock screen needs a PIN or pattern (`deviceLocked=1`, the credential screen blocks screencap and the executor never enters credentials). The executor did not touch the display, density or demo settings: `wm size` stayed 720x1600 physical with no override, density 300, `sysui_demo_allowed` 0. The pipeline script is committed and its `check` subcommand was exercised against stand-in images (a native 720 x 1600 frame fails with "long side is more than twice the short side", a 720 x 1440 JPEG passes). To finish: unlock the Samsung, then run `bash scripts/play-store-screenshot.sh prep`, navigate (Client Mode, Explore, an expert profile, Book a Session without tapping the booking button, Bookings, a chat thread between your own accounts), `capture 01-explore` and so on, and end with `restore`. If the display override is not honoured on One UI (RESEARCH A6), use `capture <name> --native` for a 2:1 frame, which meets Google's minimum but not the 9:16 featuring recommendation.
- **Rows 8-9:** the user's own assets, needed before closed testing can start.
