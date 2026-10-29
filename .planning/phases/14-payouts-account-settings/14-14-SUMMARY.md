---
phase: 14-payouts-account-settings
plan: 14
subsystem: ui
tags: [navigation, shells, profile-hub, entries, wiring]
requires: [14-03, 14-04, 14-05, 14-06, 14-07, 14-08, 14-09, 14-10, 14-11, 14-12, 14-13]
provides:
  - clientSettingsEntries / expertSettingsEntries (the one place mapping Phase 14 callbacks to keys)
  - HomeClientShell / HomeExpertShell profileRoot slot with the hubs as the Profile tab roots
  - clientProfileHubActions / expertProfileHubActions
  - expertProfileEntry (ExpertProfileKey in the expert shell, bar hidden, Book Session never shown)
affects: [14-15 push routing, 14-16]
tech-stack:
  added: []
  patterns:
    - "Shells stay Hilt/BuildConfig-free: a profileRoot slot with a stateless default, Hilt hub passed in by HomeClientScreen/HomeExpertScreen"
    - "Entry metadata asserted in plain JVM tests by reading NavEntry.metadata from entryProvider without composing"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/SettingsEntries.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/SettingsEntriesTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpScreenTest.kt
decisions:
  - "Shell debugContent is a plain @Composable () -> Unit (matches the hubs' debugContent), not ColumnScope.() as the plan sketched"
  - "ExpertProfileKey registration extracted to internal expertProfileEntry(navigator) in HomeExpertScreen.kt so its metadata is unit-testable without Hilt"
  - "Shell default hubs use an empty card and empty version label; production passes the Hilt hubs with profileHubVersionLabel()"
metrics:
  tasks: 2
status: complete
completed: 2026-10-29
actuals:
  tokens: 30000
  tasks: 2
  commits: 4
plan_head_before: 6f9056809f1f6db65fc6bce1f545963df2a62547
plan_head_after: 5cce2e63d6e9b85f00f764abb468526e0faf454d
requirements-completed: [PROF-01, PROF-02, PROF-03, PROF-04, PROF-05, PAYOUT-01, PAYOUT-04]
---

# Phase 14 Plan 14: Shell Wiring Summary

**Both Profile tabs are now the Phase 14 hubs, and one registration function per mode (`clientSettingsEntries`, `expertSettingsEntries`) makes every settings, account, help, payouts, activity and tax screen reachable with the bottom bar hidden.**

## What was built

- **`SettingsEntries.kt`** (new): `clientSettingsEntries` registers `ClientProfileSettingsKey` and the shared set; `expertSettingsEntries` registers `ExpertProfileSettingsKey`, `PayoutsKey`, `TransactionsKey`, `TaxProfileKey` and the shared set. Shared (private `accountAndHelpEntries`): `ProfilePhotoCropKey` (`PhotoCropScreen`, done/cancel pop), `AccountSecurityKey` (Delete Account row pushes `DeleteAccountKey`), `DeleteAccountKey`, `HelpKey`. All use `BottomBar.hidden()`. Origins travel to the keys each screen pushes (crop, activity, tax).
- **Client shell**: `profileRoot` slot (stateless hub default); the throwaway footer and its imports are gone. `clientProfileHubActions` maps card -> `ClientProfileSettingsKey`, Security -> `AccountSecurityKey`, Payment -> `PaymentMethodsKey`, Help -> `HelpKey`, Switch -> `onSwitchMode(Expert)`, Log out -> `onLogout` (after the hub's dialog). `HomeClientScreen` passes `ClientProfileHomeScreen` with `profileHubVersionLabel()`; `detailEntries` adds `clientSettingsEntries(navigator)`.
- **Expert shell**: same slot; `expertProfileHubActions` maps card -> `ExpertProfileSettingsKey(Profile)`, Expertise -> `navigate(TopicsRoot)`, Availability -> `AvailabilityKey(Profile)` (row tag `profile_availability_row` kept), Payouts -> `PayoutsKey(Profile)`, Tax -> `TaxProfileKey(Profile)`, Security, Help, Switch -> `onSwitchMode(Client)`. `HomeExpertScreen` passes `ExpertProfileHomeScreen` with `onOpenPublicProfile = navigate(it)` (self-view key from the VM). `expertProfileEntry` registers `ExpertProfileKey` hidden-bar with `showBookSession = false` and `isSelfView = key.isSelfView`; `expertSettingsEntries(navigator)` added to `detailEntries`.
- **Debug affordances**: private `ClientProfileDebugAffordances` / `ExpertProfileDebugAffordances` (gallery, force refresh, status) rendered via the hub's `debugContent` below the version footer, only when `showDebugAffordances` (BuildConfig.DEBUG in production). Absent otherwise (T-14-38).

## Verification

- Task 1 verify (SettingsEntriesTest, HomeShellsTest, BookingFlowNavigationTest, EntrySavedStateHandleTest, ktlintCheck, assembleDevDebug) and Task 2 verify (SettingsEntriesTest, HomeShellsTest, ExpertNavigationTest, HelpScreenTest, ktlintCheck, assembleDevDebug, lintDevDebug with 0 errors) exit 0. `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: Throwaway footers 0 in both shells; `clientSettingsEntries(navigator)` 1; `expertSettingsEntries(navigator)` 1; `entry<ExpertProfileKey>` 1; `showBookSession = false` 1; `BottomBar.hidden()` in SettingsEntries.kt 9.
- Tracer gate (Task 1): `<verify>` re-run end to end after the commit and passed before expanding to Task 2.
- New/updated tests: SettingsEntriesTest (3: client keys hidden, expert keys hidden for both origins, expert profile entry hidden); HomeShellsTest: `clientProfileTabIsTheHub`, `clientHubRowsNavigate`, `clientLogOutGoesThroughDialog`, `clientModeCardSwitches`, `clientDebugAffordancesBelowFooter`, `expertProfileTabIsTheHub`, `expertHubRowsNavigate`, `expertiseAndTopicsSelectsTopicsTab`, `availabilityRowStillOpensAvailability`, `expertLogOutGoesThroughDialog`, `expertModeCardSwitches`, plus updated payment-row and debug tests.

## Deviations from Plan

**1. [Rule 3 - Blocking] HelpScreenTest tripped lint `ViewModelConstructorInComposable`**
- **Found during:** Task 2 (`lintDevDebug` gate)
- **Issue:** Two tests from plan 14-06 constructed `HelpViewModel` inside `setContent`, a lint error that failed the plan's required `lintDevDebug` gate (not caught by unit tests).
- **Fix:** Hoisted the view model construction above `setContent` (no behavior change).
- **Files modified:** `app/src/test/.../screens/settings/help/HelpScreenTest.kt`
- **Commit:** 5cce2e63

**2. [Plan wording] `debugContent` type** is `@Composable () -> Unit` (what the 14-03 hubs accept), not `ColumnScope.() -> Unit`.

**3. [Plan wording] `ExpertProfileKey` test seam**: the entry is registered through `internal fun EntryProviderScope<NavKey>.expertProfileEntry(navigator)` in `HomeExpertScreen.kt` so the hidden-bar metadata is asserted in `SettingsEntriesTest` (the plan allowed "a provider built with the same block").

Notes: RED commits (bb624186, e8c9cd09) are compile-failing or runtime-failing test files committed before the implementation. The per-plan commit ledger file was not used (worktree sandbox); `commits:` is measured with `git rev-list --count` from the recorded base.

## Known Stubs

None. The shell defaults (empty card, empty version label, no-op View My Public Profile) exist only so Robolectric shell tests run without Hilt; production always passes the Hilt hubs.

## Open items

- Device checks not done (project policy): hub navigation on the Samsung device, bottom bar hiding on pushed screens, Expert self-view without Book Session.
- Pre-existing out-of-scope: none logged.

## Threat Flags

None. T-14-38 mitigated (debug rows gated by `showDebugAffordances`, both values tested); T-14-39 mitigated (`showBookSession = false` in the expert shell entry, grep gate).

## Self-Check: PASSED

SettingsEntries.kt and SettingsEntriesTest.kt exist; commits bb624186, acba8edc, e8c9cd09 and 5cce2e63 are in the branch history.
