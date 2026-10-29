---
phase: 14-payouts-account-settings
plan: 10
subsystem: payouts
tags: [payouts, deep-link, stripe-connect, recent-activity, tax-profile-link]

requires:
  - phase: 14-payouts-account-settings
    provides: "14-02 ConnectReturn slot and SettingsOrigin; 14-08 PayoutsViewModel and PayoutsScreen; 14-09 EarningsTransactionRow and transactions()"
provides:
  - "PayoutsViewModel consumes ConnectReturn: Done reloads quietly, LinkExpired re-mints only after this screen opened a link"
  - "PayoutsUiState.recentRows and hasMoreActivity from a soft first-page ledger fetch"
  - "previewRows, shouldShowSeeAll, PREVIEW_ROW_COUNT in PayoutsLogic.kt"
  - "PayoutsScreen(origin, onBack, onOpenActivity, onOpenTaxProfile, modifier, viewModel) with Recent activity and Tax & Payout Info row"
affects: [14-14, payouts, navigation-wiring]

tech-stack:
  added: []
  patterns:
    - "Stale one-shot dropped in init, then collected while alive (same as ExpertDashboardViewModel)"
    - "Shared guarded mintLink(refresh) for the setup tap and the refresh deep link"

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsLogic.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SectionHeader.kt
    - app/src/main/res/values/strings_payouts.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsLogicTest.kt

key-decisions:
  - "A refresh deep link re-mints only when openedOnboarding is true; otherwise it reloads (T-14-28)"
  - "Done only triggers a status reload; the content always comes from the fetched status (T-14-29)"
  - "A Quiet or Pull reload whose ledger fetch failed keeps the rows already shown; a first (Loud) load shows none"
  - "SectionHeader gained an optional actionModifier so the See all action can carry a test tag"

patterns-established:
  - "PayoutsScreenContent takes origin plus onOpenActivity and onOpenTaxProfile callbacks that receive the origin"

requirements-completed: [PAYOUT-02, PAYOUT-03, PAYOUT-04]

duration: 45min
completed: 2026-10-29
status: complete
plan_head_before: b65fd67281052f0dcf51cc3ce74a098e71090717
plan_head_after: bd9a8042fa196fd9e7425a797afbacb89e1950e9
commits: 4
actuals:
  tokens: 14000
  tasks: 2
  commits: 4
---

# Phase 14 Plan 10: Payouts Return Handling, Recent Activity and Tax Row Summary

**Payouts now refreshes on the clickme://connect/return deep link, reopens a freshly minted link only after its own expired one, previews the latest five ledger rows with See all activity, and links to Tax & Payout Info.**

## Accomplishments

- `PayoutsViewModel` takes `ExpertLaunchRequest`. `init` drops a stale `ConnectReturn` before the first Loud load, then collects the slot while alive. `Done` starts a Quiet reload (Loud if not Loaded) and never sets state itself; `LinkExpired` re-mints a refresh link through the call guard (`SetUpPayouts`), the https check and the in-flight check only when this screen already handed a link to the browser, otherwise it only reloads.
- Setup tap and refresh deep link share one private `mintLink(refresh)` so error handling, the guard and the URL check are identical.
- Every load also fetches `transactions(1, 20)` as a soft call: `recentRows` and `hasMoreActivity` are filled on success; a failure leaves the balance and status untouched.
- `PayoutsLogic.kt`: `PREVIEW_ROW_COUNT = 5`, `previewRows`, `shouldShowSeeAll` (`hasMore || rows.size > 5`).
- `PayoutsScreen`: Recent activity (`SectionHeader` plus up to five `EarningsTransactionRow`s) renders in OnHold, ReadyNoEarnings and Loaded when rows exist and never in SetupRequired; "See all activity" (`payouts_see_all`) calls `onOpenActivity(origin)`. The Tax & Payout Info `ProfileMenuRow` (`payouts_tax_row`) is the last block in SetupRequired, OnHold, ReadyNoEarnings and Loaded and calls `onOpenTaxProfile(origin)`; it is absent in Loading and Failed.

## Task Commits

1. Task 1 (tracer): RED `40d263f`, GREEN `576e035`
2. Task 2: RED `013c24e`, GREEN `bd9a804`

Verification: `PayoutsLogicTest` 4, `PayoutsViewModelTest` 20, `PayoutsScreenTest` 21, `EarningsTransactionRowTest` 9, all green; `ktlintCheck`, `:app:assembleDevDebug` and `scripts/check-design-rules.sh` pass. Acceptance greps: `PREVIEW_ROW_COUNT = 5` 1, `EarningsTransactionRow(` in the screen 1, `payouts_tax_row` string 1, `connectReturn.consume()` 2, `ConnectReturn.LinkExpired` 1.

## Tracer gate

The Task 1 slice was re-verified by the automated verify command (`PayoutsViewModelTest`, `ConnectReturnIntentTest`, `ktlintCheck` green) before Task 2 expansion.

## TDD Gate Compliance

RED commits `40d263f` and `013c24e` precede their GREEN commits. Both REDs are compile-level (the tests reference `launchRequest`, `recentRows`, `previewRows`, `PAYOUTS_SEE_ALL_TAG` and the new screen parameters that did not exist), not captured assertion failures; Gradle was not run on the RED commits (shared machine, long builds) and no `tdd-red-evidence` record exists. This does not meet the strict INVALID_RED rule for build-failure REDs.

## Deviations from Plan

**1. [Rule 3 - Blocking] SectionHeader got an optional `actionModifier`**
- **Issue:** the See all action sits inside the shared `SectionHeader`, so the plan's `payouts_see_all` test tag could not be attached to the button (tagging the header row would click the title).
- **Fix:** added `actionModifier: Modifier = Modifier` (default keeps every other caller unchanged).
- **Files:** `components/SectionHeader.kt` (outside `files_modified`).

**2. Screen signature:** `PayoutsScreen(origin, onBack, onOpenActivity, onOpenTaxProfile, modifier, viewModel = hiltViewModel())` with `viewModel` last and defaulted, matching 14-09; the stateless `PayoutsScreenContent` also takes `origin` and both callbacks. 14-14 should call `PayoutsScreen(origin = key.origin, onBack = ..., onOpenActivity = ..., onOpenTaxProfile = ...)`.

**3. Test fixtures:** existing 14-08 tests now stub `earningsTransactionsHandler` (an unstubbed handler throws `IllegalStateException`, not `ApiException`), and the view model constructor takes a fourth `ExpertLaunchRequest` argument.

**4. Reopen skipped while a URL is pending:** `mintLink` ignores a `LinkExpired` while a minted URL has not been opened yet (same busy check as the setup tap), so a burst of forged signals mints at most one link.

## Auth Gates

None.

## Known Stubs

None.

## Threat Flags

None beyond the plan register: T-14-28 (`foreignRefreshOnlyReloads`, `stalePendingIsDropped`, `blockedGuardOnRefreshDoesNothing`), T-14-29 (`returnNeverAssumesSuccess`, `returnReloadsQuietly`), T-14-30 (the refresh path uses the same `isAllowedOnboardingUrl` check).

## Open Items (manual, not blocking)

- Real Custom Tab return via `clickme://connect/return` and `/refresh` on the Samsung device (backstop truth; RESEARCH A1), per the Phase 14 manual-testing policy.
- Tax & Payout Info row placement is not drawn in UI-SPEC Screen 6; kept per CONTEXT D-08, flag at UAT.
- Navigation wiring of the new callbacks (`PayoutsKey` entry, `TransactionsKey`, `TaxProfileKey`) is outside this plan.

## Self-Check: PASSED

- All eight modified files exist on disk.
- Commits 40d263f, 576e035, 013c24e, bd9a804 are ancestors of HEAD.
