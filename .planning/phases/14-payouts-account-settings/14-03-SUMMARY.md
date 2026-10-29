---
phase: 14-payouts-account-settings
plan: 03
subsystem: ui
tags: [profile-hub, compose, navigation-callbacks, custom-tab, self-view]
requires: []
provides:
  - ClientProfileHomeScreen / ClientProfileHomeContent / ClientProfileHubActions
  - ExpertProfileHomeScreen / ExpertProfileHomeContent / ExpertProfileHubActions
  - ProfileHubComponents (user card, mode card, section header, footer, log-out button and dialog, scaffold, privacy opener)
  - ExpertProfileKey.isSelfView and ExpertProfileScreen(isSelfView, showBookSession)
affects: [14-14 shell wiring]
tech-stack:
  added: []
  patterns:
    - "Hubs take navigation callbacks instead of keys; the shell maps them to destinations"
    - "Hub card derived from UserManager.profile / authUser / me via a pure profileHubCard() function"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/ClientProfileHomeViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/ClientProfileHomeScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/subviews/ProfileHubComponents.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/profile/ExpertProfileHomeViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/profile/ExpertProfileHomeScreen.kt
    - app/src/main/res/values/strings_profile_hub.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/ClientProfileHomeScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/ClientProfileHomeViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/profile/ExpertProfileHomeScreenTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreenTest.kt
decisions:
  - "ProfileHubSection (caption + rows group) is shared by both hubs as an internal composable in ClientProfileHomeScreen.kt"
  - "profileHubVersionLabel() helper added so the shell can pass the formatted BuildConfig version label"
  - "Hiding Book Session also hides the disabled 'not bookable' pill in self view"
metrics:
  tasks: 3
  tests_added: 21
status: complete
completed: 2026-10-29
actuals:
  tokens: 36000
  tasks: 3
  commits: 6
plan_head_before: 466e755dd0e79f915ed6bf75f28b935266bf41b6
plan_head_after: cbe625b0c9c15295f3c04cffc832be45ba6d33d2
requirements-completed: [PROF-01]
---

# Phase 14 Plan 03: Profile Hubs and Self-View Summary

**Stand-alone client and expert Profile hubs that take navigation callbacks and show only working rows, plus an `isSelfView` flag that hides Book Session on an expert's own public profile.**

## What was built

- **Client hub** (`ClientProfileHomeContent`): `TopAppBar` "Profile", user card, "Switch to Expert Mode" card, Account (Security & Password, Payment), Support (FAQ & Help, Privacy), Log out, version footer, then a `debugContent` slot.
- **Expert hub** (`ExpertProfileHomeContent`): "Expert Settings", user card, "Switch to Client Mode" card, Professional (View My Public Profile, Expertise & Topics, Availability with tag `profile_availability_row`, Payouts & Earnings, Tax & Payout Info), Account (Security & Password), Support, Log out, footer, debug slot.
- **View models** render from the cached profile with `authUser` and `/me` as fallbacks (`profileHubCard`); no network call. The expert VM adds `publicProfileKey()` returning `ExpertProfileKey(..., isSelfView = true)`, or null with no user id.
- **Shared components**: user card (one `Role.Button`, merged label "{name}, {email}, Edit profile", no online dot), mode-switch card, section header, version footer, `DestructiveOutlinedButton` Log out, `LogOutConfirmDialog` ("Log Out" / "Stay logged in"). Privacy opens `AppLinks.PRIVACY_POLICY` in the Custom Tab, with the "Couldn't open the link" snackbar on failure.
- **Self-view**: `ExpertProfileKey.isSelfView` (defaulted, so old saved stacks decode); `ExpertProfileScreen(isSelfView, showBookSession)` computes `showBookSession && !isSelfView && !chatEntry.isSelf(expertId)`; `ExpertProfileContent` and the action buttons take `showBookSession`.

## Verification

- Task 3 verify command (unit tests for the five test classes, `ktlintCheck`, `:app:assembleDevDebug`) exits 0; `scripts/check-design-rules.sh` prints "Design rules: OK".
- ClientProfileHomeScreenTest 8, ClientProfileHomeViewModelTest 4, ExpertProfileHomeScreenTest 6, ExpertProfileScreenTest 35 (3 new), ExpertProfileViewModelTest 17 (unchanged), all green.
- Tracer gate (Task 1): `<verify>` re-run end-to-end after the commit and passed; expansion continued.

## Deviations from Plan

None. The plan was executed as written. Notes:

- RED commits are compile-failing test files (the production types did not exist yet), which counts as RED for the TDD gate.
- The plan's per-plan commit ledger file could not be written from the worktree sandbox; `commits:` was measured with `git rev-list --count 466e755..HEAD` instead (base recorded in `plan_head_before`).
- Added a second string `profile_hub_user_card_a11y_no_email` so the merged label reads "Your Profile, Edit profile" when there is no email, as the behavior list requires.

## Known Stubs

None. The hubs take callbacks, which 14-14 wires to real destinations; nothing renders placeholder data.

## Open items for later plans

- 14-14 must pass `ClientProfileHubActions` / `ExpertProfileHubActions`, `profileHubVersionLabel()` and the debug affordances into the hubs, register `ExpertProfileKey` in the expert shell with `showBookSession = false` and `isSelfView` from the key, and remove the throwaway Profile footers.
- Device checks (not done, per project policy): hub look at 200% font scale, TalkBack pass, Custom Tab Privacy on the Samsung device.
- PROF-01 is unclassified; flagged for the verifier.

## Threat Flags

None. T-14-08 (Privacy link) is mitigated by the constant URL through the http(s)-only launcher and the `privacyOpensCustomTab` test. T-14-09 (self-booking) is mitigated by `isSelfView` / `showBookSession` and the `selfViewHidesBookSession` test.

## Self-Check: PASSED

All created files exist; commits 3d448ae, f35d0f2, 8a7ed65, a6ab97e, 2b2ee48 and cbe625b are in the branch history.
