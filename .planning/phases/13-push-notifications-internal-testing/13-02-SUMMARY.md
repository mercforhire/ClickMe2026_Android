---
phase: 13-push-notifications-internal-testing
plan: 02
subsystem: backend-push
tags: [backend, cross-repo, fcm, notification-channels, push-payload, jest]

requires:
  - phase: 11-expert-requests-bookings
    provides: "backend STATE row format (11-18) and the existing sendPushToUser fan-out"
provides:
  - "Backend push.js sends android.notification.channelId = channelIdFor(category) to Android tokens"
  - "Android FCM data map carries category and sub_category (empty string when absent)"
  - "CHANNEL_BY_CATEGORY and channelIdFor exported, with jest tests pinning the five channel literals"
  - "iOS and platform-less token rows keep the pre-change message (deep-equal regression test)"
affects: [13-05 NotificationChannels, 13-01 event_type fallback, 13-14 release checklist, 13-17 gate plan]

actuals:
  tokens: 3600
  tasks: 2
  commits: 3
plan_head_before: bed4758fd4936b5251dc7a2ee650da7bfca92023
plan_head_after: 92b4827a9dad2ad2a33ae4dcefeff2e3b1fdc402
# commits are in the BACKEND repo (/Users/LeonChen/Documents/ClickMe2026 Backend, branch master), measured with
# `git -C <backend> rev-list --count bed4758..HEAD` = 3. The Android repo gets only this SUMMARY.

tech-stack:
  added: []
  patterns:
    - "Per-platform FCM message built in one place in the token loop, branching on device_tokens.platform"
    - "Cross-repo wire contract pinned by literal-string tests on both sides"

key-files:
  created: []
  modified:
    - "/Users/LeonChen/Documents/ClickMe2026 Backend/src/services/push.js"
    - "/Users/LeonChen/Documents/ClickMe2026 Backend/tests/push.test.js"
    - "/Users/LeonChen/Documents/ClickMe2026 Backend/.planning/STATE.md"

key-decisions:
  - "category and sub_category are added to FCM data for Android tokens only; adding them for iOS would change iOS behavior, which D-04 forbids (iOS follow-up is logged by 13-17)"
  - "Token rows with no platform are treated as iOS-shaped (the unchanged payload)"
  - "review maps to the sessions channel (user-confirmed 2026-10-27)"

requirements-completed: [PUSH-02, PUSH-03]

coverage:
  - id: D1
    description: "Android tokens receive android.notification.channelId from the push category, with the full map booking->bookings, session/review->sessions, message->messages, payout/account->account_payouts, else general"
    requirement: "PUSH-02"
    verification:
      - kind: unit
        ref: "tests/push.test.js#android token gets channelId and category data"
        status: pass
      - kind: unit
        ref: "tests/push.test.js#channelIdFor maps every category"
        status: pass
      - kind: unit
        ref: "tests/push.test.js#channel ids are the Android contract"
        status: pass
    human_judgment: false
  - id: D2
    description: "Android FCM data carries category and sub_category as strings, empty when absent"
    requirement: "PUSH-03"
    verification:
      - kind: unit
        ref: "tests/push.test.js#android push without category"
        status: pass
      - kind: unit
        ref: "tests/push.test.js#review nudge uses sessions"
        status: pass
    human_judgment: false
  - id: D3
    description: "iOS tokens and platform-less legacy rows receive the byte-identical pre-change message (notification, data, apns, android {priority:'high'})"
    requirement: "PUSH-02"
    verification:
      - kind: unit
        ref: "tests/push.test.js#ios token message is unchanged"
        status: pass
      - kind: unit
        ref: "tests/push.test.js#legacy row without platform keeps the old shape"
        status: pass
      - kind: unit
        ref: "tests/push.test.js#mixed tokens fan out per platform"
        status: pass
    human_judgment: false
  - id: D4
    description: "The change is live on the deployed backend so a background Android push lands on its channel on a real device"
    requirement: "PUSH-02"
    verification: []
    human_judgment: true
    rationale: "The executor never pushes or deploys the backend. Deploy and the /health check are a human step (item 1 of the 13-14 release checklist); on-device channel behavior is verified in the Phase 13 device run."

duration: 4min
completed: 2026-10-28
status: complete
---

# Phase 13 Plan 02: Backend Android FCM Channel and Category Data Summary

**Backend `sendPushToUser` now branches on `device_tokens.platform`: Android tokens get `android.notification.channelId` from a frozen category-to-channel map plus `category`/`sub_category` in the FCM data, while iOS and legacy rows stay deep-equal to the pre-change message. Three unpushed commits sit on backend `master`.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-10-28T15:30:48Z
- **Completed:** 2026-10-28T15:35Z
- **Tasks:** 2
- **Files modified:** 3 (all in the backend repo)

## Accomplishments

- `CHANNEL_BY_CATEGORY` (frozen) and `channelIdFor(category)` in `src/services/push.js`, defaulting to `general`; both exported. The comment names the Android `NotificationChannels.kt` contract.
- The device token read selects `platform`; the per-token message is built in one place. Android gets `android: { priority: 'high', notification: { channelId } }` and `data` plus `category` / `sub_category`. iOS and rows with no platform get exactly the old shape. Stale-token reaping, the preference gate and the in-app notifications row are untouched.
- Nine new jest tests in `describe('android channel and data (Phase 13 D-04)')`, including a `toEqual` pin of the iOS message and a Set-equality pin of the five channel literals (the same literals the Android 13-05 test asserts).
- Backend `.planning/STATE.md` has the `261028-a13` Quick Tasks Completed row.

## Task Commits

All in the backend repo `/Users/LeonChen/Documents/ClickMe2026 Backend`, branch `master`:

1. **Task 1 (tracer, TDD): Android token gets the category's channel; iOS unchanged** - `6daf5c0` (feat)
2. **Task 2: full map, general fallback, legacy rows, full suite** - `3ba94b9` (test)
3. **Task 2: backend STATE row** - `92b4827` (docs)

**Android-repo plan metadata:** this SUMMARY (see "Issues Encountered": the commit was blocked by the protected-branch guard).

### TDD evidence

RED (before any `push.js` change): `npm test -- --silent tests/push.test.js` gave `Tests: 3 failed, 30 passed, 33 total`. The failing tests were `android token gets channelId and category data`, `mixed tokens fan out per platform` and `token query selects platform`. `ios token message is unchanged` already passed, which is intended: it pins behavior that must not change. GREEN: 33 passed after the `push.js` change. The five Task 2 tests were characterization tests on the already-green implementation, so they had no separate RED; they pass and pin the contract. Final: 38 passed.

## Verification

| Check | Result |
|-------|--------|
| `npm test -- --silent tests/push.test.js` | 38 passed (baseline 29; required at least 38) |
| Full suite `npm test -- --silent` | `Test Suites: 1 failed, 120 passed, 121 total`; `Tests: 6 failed, 2436 passed, 2442 total`. Only `tests/services/redis.test.js` fails (REDIS_URL unset), same as baseline (2427 passed + 9 new) |
| `grep -c "select('token, device_id, platform')" push.js` | 1 (baseline 0) |
| `grep -c "account_payouts" push.js` | 2 (required at least 1) |
| `grep -c "apns: { payload: { aps: { sound: 'default' } } }" push.js` | 1 (invariant held) |
| Task 2 verify chain (push tests, `261028-a13` count == 1, clean tracked tree) | exit 0 |
| `git log --format=%s -3` | `docs(...)`, `test(...)`, `feat(quick-261028-a13)` subjects in that order |
| `git diff --name-only bed4758..HEAD` | exactly `.planning/STATE.md`, `src/services/push.js`, `tests/push.test.js` |
| `origin/main` unchanged | `3274175ce514fcab7341a1e3e8033903586acf26` before and after (nothing pushed) |
| No `app/` change in the Android repo | `git status --short app` empty |
| Post-commit deletion check | no deletions in any of the three commits |

## Unpushed backend commits (for the human deploy)

`git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" log --oneline origin/main..master`:

```
92b4827 docs(quick-261028-a13): STATE row
3ba94b9 test(quick-261028-a13): channel map, general fallback and legacy token rows
6daf5c0 feat(quick-261028-a13): android channel id and category data in FCM messages
bed4758 docs(quick-261027-pf4): plan, summary, STATE row
```

The code commit to deploy is `6daf5c0`; the deployed head after `git push origin master:main` will be `92b4827`, which is the short sha to check on `/health` after the deploy.

## Open Items (human)

1. **Deploy the backend** (`git push origin master:main`, then confirm `/health` reports `92b4827`). Not done by the executor by design; this is item 1 of the 13-14 release checklist. Until deployed, Android relies on its event_type fallback (13-01).
2. iOS has the same routing gap (it parse-fails every push except chat and booking requests). `category` / `sub_category` were deliberately not added for iOS (D-04); 13-17 logs this as an iOS follow-up for the user to decide.

## Decisions Made

- Followed the plan's Android-only data keys and the "no platform means iOS-shaped" rule (see key-decisions).
- Backend `CLAUDE.md` asks that file changes start from a GSD command. This work ran inside `/gsd-execute-phase` for a locked user decision (D-04), which satisfies that rule.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- **Protected-branch guard blocked the Android SUMMARY commit.** The Android main checkout is on `main`, `gsd_run query git.base-branch --is-protected main` returns `true`, and `git.allow_default_branch_commits` is not set in `.planning/config.json`. Per the executor's pre-commit assertion (#2924, #3819) the SUMMARY was written but not committed, and no config was changed. The orchestrator can commit it with an explicit path:
  `git add .planning/phases/13-push-notifications-internal-testing/13-02-SUMMARY.md && git commit -m "docs(13-02): complete backend android channel and category data plan"`, or set the override and re-dispatch.
- The orchestrator note that the first dispatch was blocked by the worktree path guard held true; this run, outside worktree isolation, edited the backend directly with no problem.

## Known Stubs

None. No stub patterns in the changed files.

## Threat Flags

None. Threat register dispositions: T-13-04 (iOS shape) mitigated by the deep-equal tests and the `apns` literal grep; T-13-06 (unknown category) mitigated by the `general` default; T-13-05 and T-13-SC accepted as planned (only taxonomy strings added; no package installs).

## User Setup Required

None - no external service configuration required. The backend deploy is the human step listed under Open Items.

## Next Phase Readiness

- 13-05 (`NotificationChannels`) can assert the same five literals: `bookings`, `sessions`, `messages`, `account_payouts`, `general`.
- 13-01's event_type fallback stays in place until the backend is deployed.

## Self-Check: PASSED

- FOUND: `src/services/push.js`, `tests/push.test.js`, `.planning/STATE.md` (backend)
- FOUND commits in backend history: `6daf5c0`, `3ba94b9`, `92b4827`

---
*Phase: 13-push-notifications-internal-testing*
*Completed: 2026-10-28*
