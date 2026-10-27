---
phase: 11-expert-requests-bookings
plan: 18
subsystem: voice-call
tags: [gap-closure, agora, call, backend, cross-repo, channel-name, ios-parity, deploy-gate]
status: complete
gap_closure: true
gap_ids: [G-11-6]
gap_status: open
requirements: [EXP-08]
requires:
  - phase: 11-expert-requests-bookings
    provides: "11-17 COMMUNICATION profile and remote-presence logging (unchanged)"
provides:
  - "Backend join signs and returns the canonical lowercase booking.id as the Agora channel"
  - "generateToken lowercases its channel input"
  - "confirm-connection and end echo the canonical booking_id"
affects: [voice-call, expert-bookings, backend]
tech-stack:
  added: []
  patterns:
    - "Server-bound identifiers (Agora channel, echoed ids) come from the loaded row, never from request path text"
key-files:
  created: []
  modified:
    - /Users/LeonChen/Documents/ClickMe2026 Backend/src/controllers/call.controller.js
    - /Users/LeonChen/Documents/ClickMe2026 Backend/src/services/agora.js
    - /Users/LeonChen/Documents/ClickMe2026 Backend/tests/call.test.js
    - /Users/LeonChen/Documents/ClickMe2026 Backend/openapi/clickme-api.yaml
    - /Users/LeonChen/Documents/ClickMe2026 Backend/.planning/clickme-api-reference.md
    - /Users/LeonChen/Documents/ClickMe2026 Backend/.planning/STATE.md
    - .planning/phases/11-expert-requests-bookings/11-VALIDATION.md
key-decisions:
  - "Fix G-11-6 on the server (locked user decision): the channel is booking.id, so iOS builds already in the field are covered too"
  - "No Android or iOS app change; 11-17's COMMUNICATION profile stays"
backend_head_before: f686d27148d173dedb095345ffc8e4d15fa9d4ef
backend_head_after: 4c694e7
android_head_before: a6da4cbf77253b9c9a51eef2812cc65e1d301373
closeout: manual
---

# 11-18 SUMMARY: server-side canonical Agora channel name (G-11-6)

## How this plan was executed (manual close-out)

The backend work (Tasks 1-3) was done and pushed before `/gsd-execute-phase 11 --gaps-only` ran. When the orchestrator started, backend `master` was already at `4c694e7`, not the planned `f686d27`, so Task 1's precondition no longer held. The user chose a **manual close-out**. The orchestrator checked the existing backend commits against the plan's acceptance criteria, re-ran the backend call tests, and did the Android-repo part of Task 5 inline. No executor was dispatched and nothing in the backend was changed during this run.

Backend CLAUDE.md asks for file changes to start from a GSD command. The backend changes were requested explicitly by the user (locked decision) and planned in this repo's 11-18 plan. The push was the user's action (the backend STATE row says "pushed at user request"). The orchestrator did not push or deploy anything.

## Backend commits (repo `ClickMe-Backend`, branch `master`, all on `origin/main`)

| sha | message |
|-----|---------|
| `93d8b15` | test(quick-261026-g16): add failing tests for canonical lowercase Agora channel (G-11-6) |
| `a2c4fee` | fix(quick-261026-g16): derive the Agora channel and echoed booking_id from booking.id, not the URL param (G-11-6) |
| `92ac0e4` | docs(quick-261026-g16): document the canonical lowercase Agora channel_name |
| `4c694e7` | docs(quick-261026-g16): STATE row |

- `backend_head_before`: `f686d27148d173dedb095345ffc8e4d15fa9d4ef`
- `android_head_before`: `a6da4cbf77253b9c9a51eef2812cc65e1d301373`
- Files changed `f686d27..4c694e7`: `src/controllers/call.controller.js`, `src/services/agora.js`, `tests/call.test.js`, `openapi/clickme-api.yaml`, `.planning/clickme-api-reference.md`, `.planning/STATE.md`. This is the plan's expected list.

## Acceptance checks (re-run by the orchestrator)

- `join` uses `bookingId` twice (the declaration and the lookup); `confirmConnection` 3; `end` 3. These are the planned values.
- `generateToken(booking.id` appears once; `toLowerCase` is in `agora.js`; every `booking_id` echo and the `booking.update` socket payload use `booking.id`.
- `G-11-6` appears 9 times in `tests/call.test.js`; "lowercase booking UUID" appears twice in each doc file.
- `npm test -- --silent tests/call.test.js`: **48 passed, 48 total** (41 at baseline).
- Full suite, as recorded in the backend STATE row: 2417 passed; the only failing suite is the REDIS_URL-less `tests/services/redis.test.js`, as at baseline.

## Deviations from Plan

- **Test count is 7 new tests, not 8.** Behaviours (a) and (b) are one test (`G-11-6 (a)+(b)`), so call.test.js went 41 to 48 (the plan said at least 49) and the full suite reached 2417 passed (the plan said at least 2418). All eight planned behaviours, (a) to (h), are asserted. Accepted as-is.
- **Commit shape.** The planned four code/test commits arrived as one test commit and one combined fix commit. The planned separate RED commit for (f) to (h) was folded into `93d8b15`. The RED output was not captured in this repo.
- **Backend STATE row** was written with "Deployed (pushed at user request); device re-test: pending". The orchestrator left it unchanged.
- **Task 3 / deploy:** done by the user. `curl -s https://clickme-backend-production.up.railway.app/health` returned `{"status":"success","commit":"4c694e7"}`, which equals backend HEAD.

## Device re-test (Task 4): DEFERRED

The user deferred the two-sided device re-test, so **G-11-6 is still OPEN**. Nothing on a device has yet shown that the casing hypothesis (about 85-90%) is the whole cause. Before testing:
- check that the installed Android build is current: `dumpsys package ... | grep lastUpdateTime` must be later than the last `app` commit, 2026-10-26 20:49:51 -0400. This plan changes no Android code, so no reinstall is otherwise needed.
- The deciding signals are a `remote joined` ClickMeCall line and Active on both phones.
- 09-HUMAN-UAT items 1, 2 and 5 have not run.

## Known Stubs

None.

## Threat Flags

None new. T-11-65 (executor push/deploy): the orchestrator did not push or deploy; the deploy was a user action, confirmed through the public `/health` commit only.

## STATE hand-off (orchestrator writes STATE.md)

```markdown
NEW ENTRY (insert after the "[Phase 11] G-11-5" entry and its checkboxes):

- [Phase 11] G-11-6 fix applied in the backend repo by 11-18; deployed `4c694e7` and /health confirmed; DEFERRED (human), non-blocking for the build, but G-11-6 stays OPEN until the device evidence exists. Symptom: Android expert and iOS client both CONNECT and both stay at "waiting for the other person". Cause (about 85-90%, `.planning/debug/android-ios-peers-never-meet-after-profile-fix.md`): the join endpoint echoed the `:id` URL casing as the Agora channel, so iOS (UPPERCASE) and Android (lowercase) were in different channels. Fix is on the server (backend commits `a2c4fee`..`4c694e7`, pushed per above); no Android change. Re-test on the Samsung SM-A075F (serial R8YYC1QFA6H; devDebug, production backend) as the expert with the iOS app as the client on ONE confirmed booking. adb: `~/Library/Android/sdk/platform-tools/adb -t <transport>`. Deciding evidence, in order:
  - [x] (1) Backend live: `curl -s https://clickme-backend-production.up.railway.app/health` returns `commit` equal to the deployed sha (`4c694e7`, checked 2026-10-26).
  - [ ] (2) Installed Android build is current: `adb -t <transport> shell dumpsys package com.uptrendinvestments.clickmeapp | grep lastUpdateTime` is later than `git log -1 --format=%ci -- app` (no reinstall is needed otherwise; this plan changes no Android code).
  - [ ] (3) Same booking on both phones, inside the join window. `adb -t <transport> logcat -c` first.
  - [ ] (4) `adb -t <transport> logcat -d -s ClickMeCall CallService` shows `remote joined elapsed=` and `remote audio state=2`; both UIs leave "waiting for the other person"; two-way audio, mute, speaker, leave and rejoin work (11-UAT test 6). Optional: the iOS Xcode console join response now shows a lowercase `channel_name`.
  - [ ] (5) Then 09-HUMAN-UAT items 1, 2 and 5.
  - Reading the result: `remote joined` and Active on both -> G-11-6 fixed (`/gsd-verify-work 11` resolves it, and G-11-5 with it). Backend live, same booking, build current, and still no `remote joined` -> the casing hypothesis alone is not the whole story; capture the iOS Agora info log for the join and `didJoinedOfUid` (iOS DEBUG enables `setLogFilter(.info)`), compare channel strings byte for byte, and reopen the debug session.

AMENDMENT (on the existing "[Phase 11] G-11-5 fix applied in code by 11-17" lead line, append to the end of the line):
 Still unresolved after 11-17; the actual cause is the channel-name case split (G-11-6), fixed server-side by 11-18 (deployed `4c694e7`) and awaiting the two-sided re-test.
```

## Self-Check: PASSED

Backend commits `93d8b15`, `a2c4fee`, `92ac0e4`, `4c694e7` exist and are on `origin/main`; `/health` reports `4c694e7`; call tests 48/48 green. In this repo, `11-VALIDATION.md` has the two G-11-6 rows and the approval line. No commit of this plan touches `app/`.
