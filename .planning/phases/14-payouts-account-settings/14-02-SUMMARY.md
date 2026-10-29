---
phase: 14-payouts-account-settings
plan: 02
subsystem: deep-link, call-guard, navigation
tags: [deep-link, stripe-connect, manifest, call-guard, navigation-origin]
requires: []
provides:
  - "clickme://connect/return|refresh trampoline (ConnectReturnActivity, ConnectReturnReceiver, connectReturnFrom)"
  - "ExpertLaunchRequest.connectReturn slot and ConnectReturn { Done, LinkExpired }"
  - "RestrictedAction.ChangePassword / DeleteAccount with guard copy"
  - "SettingsOrigin { Profile, Dashboard }"
affects: [14-04, 14-05, 14-08, 14-10]
tech-stack:
  added: []
  patterns: ["AppAuth redirect-receiver trampoline", "OneShotSlot hand-off", "origin enum for two-stack keys"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/ConnectReturnActivity.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SettingsOrigin.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/ConnectReturnIntentTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/ConnectReturnManifestTest.kt
  modified:
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertLaunchRequest.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallDialog.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogs.kt
    - app/src/main/res/values/strings_call.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogsTest.kt
key-decisions:
  - "Only the exact URIs clickme://connect/return and /refresh act; query, fragment and extras are never read"
  - "An invalid URI finishes the trampoline without relaunching MainActivity"
requirements-completed: [PAYOUT-02, PROF-03, PROF-04]
status: complete
duration: ~25min
completed: 2026-10-29
actuals:
  tokens: 14000
  tasks: 3
  commits: 4
plan_head_before: 466e755dd0e79f915ed6bf75f28b935266bf41b6
plan_head_after: fd2a76a3a44e850eb24887a9b69f8f684a972684
---

# Phase 14 Plan 02: Connect return deep link and guard hand-offs Summary

The app's first deep link: `clickme://connect/return|refresh` reaches an exported trampoline that posts the Dashboard and Payouts reload hand-offs and brings `MainActivity` back on top, plus the two new call-guarded actions and the shared `SettingsOrigin` enum.

## What was built

- `ConnectReturnActivity.kt`: `connectReturnFrom(uri)` (scheme/host case-insensitive, exact path, nothing else read), `ConnectReturnReceiver.receive(uri)` (posts `DashboardLaunch` and the `ConnectReturn` or nothing), `mainActivityRelaunchIntent` (CLEAR_TOP or SINGLE_TOP, no data, no extras), and the Hilt activity.
- Manifest: one VIEW/DEFAULT/BROWSABLE filter for `clickme://connect` on the trampoline (exported, noHistory, excludeFromRecents, translucent theme). `MainActivity` is unchanged.
- `ExpertLaunchRequest.connectReturn` slot with `ConnectReturn { Done, LinkExpired }`.
- `RestrictedAction.ChangePassword` / `DeleteAccount` with strings; copy renders "End the call to change your password." and "End the call to delete your account."
- `SettingsOrigin { Profile, Dashboard }` serializable enum.

## Verification

- ConnectReturnIntentTest (9) and ConnectReturnManifestTest (5) pass; CallDialogsTest (24, three new) passes; PushManifestTest, CallManifestTest, MainActivity intent tests, CallCenterTest green; ktlintCheck, `assembleDevDebug` and `scripts/check-design-rules.sh` pass.
- No `getQueryParameter` in the trampoline file.

## Task commits

| Task | Commit |
|------|--------|
| 1 RED tests (Tasks 1 and 2 behaviors) | e35094a |
| 1 trampoline implementation | a6c59f8 |
| 3 RED tests | b31c4e0 |
| 3 guard entries and SettingsOrigin | fd2a76a |

## Deviations from Plan

**1. [Rule 1 - Bug] KDoc `/*` opened a nested comment.** A KDoc mentioning `clickme://connect/*` made ktlint and KSP fail to parse `ExpertLaunchRequest.kt` (Kotlin comments nest). Reworded the sentence. Commit a6c59f8.

**2. Task 2 folded into Task 1.** The hardening (exact-path match, no query/fragment access, mapping-table KDoc) was written into `connectReturnFrom` from the start, and all Task 2 behaviors (forged URIs, query/fragment, casing, exported/noHistory flags, single clickme component) were authored as tests alongside Task 1's. Task 2 therefore has no separate commit; its verify set passed in the Task 1 run.

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat register (T-14-04 to T-14-07 mitigated and tested).

## Open items

- RESEARCH A1/A2 (Chrome Custom Tab follows the backend 302 into the app; CLEAR_TOP closes the tab) remain unverified on a device, for the 14-16 open list.

## Self-Check: PASSED

Created files exist; commits e35094a, a6c59f8, b31c4e0, fd2a76a are ancestors of HEAD.
