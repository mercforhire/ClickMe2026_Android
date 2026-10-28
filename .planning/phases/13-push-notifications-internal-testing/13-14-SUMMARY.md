---
phase: 13-push-notifications-internal-testing
plan: 14
subsystem: release
tags: [release, play-console, firebase, keystore, human-checklist, docs]

requires:
  - phase: 13-02
    provides: unpushed backend commits (92b4827 is the deploy sha)
  - phase: 13-05
    provides: guarded google-services plugin (app/google-services.json gitignored)
  - phase: 13-06
    provides: FCM token registration (backend device_tokens row to check)
provides:
  - "13-RELEASE-CHECKLIST.md: ten ordered human-only release steps with commands, Console paths and checks"
  - "13-PLAY-CONSOLE-DRAFTS.md: store listing, data safety table, foreground-service (microphone) declaration"
  - "STATE.md: one [Phase 13] OPEN (human, release) entry with ten checkboxes"
affects: [13-17, 16, 17]

actuals:
  tokens: 9000
  tasks: 2
  commits: 2
plan_head_before: 86fbe6fb30a99c7498c4d9e01449e270f329f86c
plan_head_after: 3b7fec6f5dae97f1fee6373b68a127456e9f280f

key-files:
  created:
    - .planning/phases/13-push-notifications-internal-testing/13-RELEASE-CHECKLIST.md
    - .planning/phases/13-push-notifications-internal-testing/13-PLAY-CONSOLE-DRAFTS.md
  modified:
    - .planning/STATE.md

key-decisions:
  - "Step order is documented with dependencies: keystore (3) -> Firebase (2) -> signed build (4) -> upload (5) -> add the Play App Signing SHA-1 back in Firebase; steps 6-9 are order independent"
  - "Card data is declared as collected (conservative default) with a Decisions-for-the-user list rather than silently omitted"
  - "Payments policy stays a human decision for Phase 17 (step 9), not resolved here"

requirements-completed: [REL-04, REL-05]

status: complete
duration: 15min
completed: 2026-10-28
---

# Phase 13 Plan 14: Release checklist and Play Console drafts Summary

**Ten ordered human-only release steps (backend deploy, Firebase, keystore, signed build with the `google_app_id` check, internal-track upload, testers, Console drafts, closed-test and Payments policy checks, device push smoke) plus paste-ready store listing, data safety and microphone foreground-service text, each step logged as an open STATE item.**

## Accomplishments

- `13-RELEASE-CHECKLIST.md`: ten `##` sections, each with exact commands or Console paths and a "Check:" result; includes the backend `master:main` push and `/health` sha check (expected `92b4827`), the `androiddebugkey` debug SHA-1 command, the upload and Play App Signing (App integrity) SHA-1s, the 02-05 keytool command, the signed `bundleProdRelease`/`assembleProdRelease` build with `jarsigner -verify` and `aapt2 ... | grep google_app_id` (Pitfall 10), the internal-track upload (versionCode 1, next is 2), tester list and opt-in link, the closed-test re-check, the Payments policy page (`answer/9858738`) and the device push smoke including the backend token row check from 13-06.
- `13-PLAY-CONSOLE-DRAFTS.md`: store listing (short description 68 characters, full description about 1900 characters, category, support email, privacy URL, graphics checklist), data safety table covering name, email, user ids, profile fields, photo, messages, Agora voice (ephemeral, not stored), Stripe card details, reviews/booking notes, booking details, and the FCM token plus app-generated device id; TLS and deletion answers; FGS declaration for `CallService` with description, user impact, use case and video steps.
- STATE.md: exactly one `- [Phase 13] OPEN (human, release)` entry with ten checkboxes and the D-22 line; the two Phase 2 deferred entries and the Payments todo now point at it.

## Task Commits

1. Task 1 (tracer): checklist and drafts - `8e9539d`
2. Task 2: STATE.md open items - `3b7fec6`

## Verification

- Task 1 verify chain (section count >= 10, `google_app_id`, `FOREGROUND_SERVICE_MICROPHONE`, `Device or other IDs`): exit 0, run against the worktree files. All acceptance phrases present. `grep -ciE 'storePassword=|keyPassword='` on the checklist: 0.
- Task 2 verify chain: `[Phase 13] OPEN (human, release)` present, exactly one line starting with `- [Phase 13] OPEN (human, release)`, `Phase 13 release checklist step 9` present, `Now tracked as ...` count 2, D-22 line count 1. Only the authorized STATE.md lines changed; ROADMAP.md untouched.
- Tracer gate (Task 1, docs only): verify passed end-to-end before Task 2.

## Deviations from Plan

None - plan executed as written. Two notes:
- The plan says "ten checkbox lines, one per checklist section"; the STATE entry also has the D-22 note as a plain bullet below them, as specified.
- The drafts file adds a "Decisions for the user" list (card data, NFC, push content, privacy policy provider names) beyond the two the plan names, because each could make the declaration inaccurate.

## Open Items (human)

All ten checklist steps are open and are the user's to do (see STATE.md). REL-04 and REL-05 are backstop truths: the Play Console outcomes (drafts saved, build on the internal track with a real tester) are user-confirmed later, not verifiable from the repo. REL-05 was flagged as unresolved in the plan's Assumptions and stays so for the verifier. The Payments policy question stays a human decision for Phase 17 (RESEARCH A9).

## Known Stubs

None. Graphics, the FGS video link and the Payments decision are explicitly marked user-supplied/open, not stubs.

## Threat Flags

None. No secrets in either document (T-13-36); data safety derived from the manifest and what the app sends (T-13-37); step 4 requires the `google_app_id` check before upload (T-13-38).

## Self-Check: PASSED

- FOUND: 13-RELEASE-CHECKLIST.md, 13-PLAY-CONSOLE-DRAFTS.md
- FOUND commits `8e9539d`, `3b7fec6` on HEAD
