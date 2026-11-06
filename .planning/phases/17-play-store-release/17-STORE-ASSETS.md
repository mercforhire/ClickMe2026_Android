# Phase 17 Store Assets Manifest (D-16)

All store assets live outside the repo in `/Users/LeonChen/ClickMe-release/store-assets/` (images of app data never enter git, as with the Phase 16 evidence). The user approves or replaces every frame at the 17-08 Console session before anything is uploaded; specs come from section 10 of `17-CONSOLE-PASTE.md` (written by plan 17-08) and RESEARCH "Release Mechanics". Phone screenshots: JPEG or 24-bit PNG with no alpha, each side 320-3840 px, long side at most twice the short side (the Samsung's native 720 x 1600 is 2.22 and fails; `scripts/play-store-screenshot.sh prep` overrides the display to 1080 x 1920). The 512 x 512 icon is the one asset that is a 32-bit PNG with alpha (RESEARCH Pitfall 9). The feature graphic is JPEG or 24-bit PNG with no alpha.

Capture tool: `scripts/play-store-screenshot.sh` (`prep`, `capture <name> [--native]`, `check <file>`, `restore`, `session <cmd...>`). Captured on the Samsung SM-A075F with the `wm size 1080x1920` / `wm density 420` override (honoured on One UI, so the `--native` crop fallback was not needed). SystemUI demo mode was NOT honoured on One UI: the status bar shows the live clock, the notification bell and a blue "Flashlight" chip on every frame (RESEARCH A6, demo half). Frames were captured as they are; see "Status bar" under Open items.

| # | File | Content | Spec check | Owner | Status | Third-party content | Approved at 17-08 |
|---|------|---------|------------|-------|--------|---------------------|-------------------|
| 1 | 01-explore.jpg | Explore tab: Today's Sessions card, Trending Categories | OK 1080x1920 alpha=no | executor | retake requested 2026-11-06 (not approved) | yes: a profile photo showing two people (the user's own account photo, a second person may be in it) on the session card and top-right avatar | no (retake requested 2026-11-06) |
| 2 | 02-expert-profile.jpg | Expert profile (a Recommended Expert from Explore) with Book Session | OK 1080x1920 alpha=no | executor | retake requested 2026-11-06 (not approved) | yes: another account's name and profile photo (looks like seeded demo data) | no (retake requested 2026-11-06) |
| 3 | 03-book-a-session.jpg | Book a Session: calendar with available days and time slots (no slot or booking button tapped) | OK 1080x1920 alpha=no | executor | retake requested 2026-11-06 (not approved) | none | no (retake requested 2026-11-06) |
| 4 | 04-bookings.jpg | My Bookings, Upcoming tab (client Bookings had data, so the expert tab was not needed) | OK 1080x1920 alpha=no | executor | retake requested 2026-11-06 (not approved) | yes: a profile photo showing two people (the user's own account photo, a second person may be in it) | no (retake requested 2026-11-06) |
| 5 | 05-chat.jpg | A chat thread between the user's own accounts, with booking and session cards | OK 1080x1920 alpha=no | executor | retake requested 2026-11-06 (not approved) | yes: same two-person profile photo; message texts are keyboard gibberish ("Bdhdhd", "Hdhdd"), consider replacing the frame | no (retake requested 2026-11-06) |
| 6 | 06-in-call.jpg | In-call screen, captured in smoke run 1 (plan 17-09) | not run | executor | open: planned for 17-09 | not assessed | [ ] |
| 7 | play-icon-512.png | 512 x 512 Play icon rendered from the existing adaptive launcher icon (surface background plus `ic_launcher_foreground`, no mask; Google applies its own) | 512x512, alpha yes, 64 KB (limit 1024 KB) | executor | captured | none | [ ] |
| 8 | feature-graphic.jpg | 1024 x 500 feature graphic | not made | user | open: user makes it (JPEG or 24-bit PNG, no alpha) | none | [ ] |
| 9 | foreground-service-video | 30-60 s screen recording of the microphone foreground service, unlisted link | not made | user | open: user records and links it | none | [ ] |

## Open items

- **Decision 2026-11-06 (rows 1-5):** none of 01 to 05 is approved for upload; the user asked for retakes. Retakes need the Samsung on ADB (wireless debugging), the torch off, a profile photo with no other people, readable chat text and no other account's real data. The user approves each retake before upload; the phone-screenshot upload in the listing waits for approved retakes.
- **Status bar (rows 1-5):** SystemUI demo mode is ignored on this One UI build, so the frames carry the live status bar (12:3x clock, bell, a "Flashlight" chip because the torch was on, 70% battery). Meets Google's spec, but the user may prefer a retake with the torch off, or crops. Retake: `bash scripts/play-store-screenshot.sh prep`, navigate, `capture <name>`, `restore`.
- **Third-party content (rows 1, 2, 4, 5):** the account profile photo shows two people and row 2 shows another account's name and photo. The user decides at 17-08 whether each frame is acceptable or must be replaced; none has been uploaded.
- **Row 5:** the thread's message bodies are gibberish; a retake after a nicer demo exchange between the user's own accounts would read better in the listing.
- **Row 6:** captured during smoke run 1 (17-09).
- **Rows 8-9:** the user's own assets, needed before closed testing can start.
