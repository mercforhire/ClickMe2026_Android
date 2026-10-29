---
phase: 14-payouts-account-settings
plan: 15
subsystem: ui
tags: [hand-off, dashboard, expert-tools, push-routing, notifications, tdd]
status: complete

requires:
  - phase: 14-payouts-account-settings
    provides: "14-02 SettingsOrigin, 14-04 AccountSecurityKey, 14-08 PayoutsKey(origin), 14-13 ExpertProfileSettingsKey(origin)"
provides:
  - "ExpertDashboardScreen/ExpertDashboardContent take onOpenPayouts and onOpenProfileSettings; the Dashboard route navigates to PayoutsKey(Dashboard) and ExpertProfileSettingsKey(Dashboard)"
  - "EarningsCard is one Role.Button with click label Open payouts (FinancialSummarySection takes onOpenPayouts)"
  - "PushDestinationResolver routes Payouts/KycOnboarding to the Expert Dashboard Payouts screen and AccountSecurity to the current mode's Profile tab"
  - "landsOnNotificationsList is true for NotificationsList only, so payout and account notification rows are tappable"
affects: [14-14 shell entries for PayoutsKey/ExpertProfileSettingsKey/AccountSecurityKey, 14-16 device gate]

actuals:
  tokens: 9500
  tasks: 2
  commits: 3

plan_head_before: 6f9056809f1f6db65fc6bce1f545963df2a62547
plan_head_after: ba797788940d9cb568446202ce6a359f62eda3b7

tech-stack:
  added: []
  patterns:
    - "Existing allowSwitch applied after the resolver's wanted-route computation keeps client-only accounts and live calls on the current list"

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/FinancialSummarySection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/ExpertToolsGrid.kt
    - app/src/main/res/values/strings_expert_dashboard.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestination.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestinationResolver.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationRows.kt

key-decisions:
  - "Earnings card uses Modifier.clickable(onClickLabel, role = Role.Button) with a shape clip, matching PayoutOnboardingCard, rather than the Surface onClick overload (which sets no role)"
  - "Payout/KYC resolution reuses the final allowSwitch check instead of a second guard, so the refused-switch path stays the single place that falls back to the current list"

patterns-established:
  - "Hand-off markers (PHASE-14, coming-soon strings) are removed when the real screen lands, with tests flipped from inert/coming-soon to routed"

requirements-completed: [PAYOUT-01, PAYOUT-02, PROF-03]

coverage:
  - id: D1
    description: "Dashboard Payouts tool and earnings card open Payouts; Profile Settings tool opens Expert Settings; no coming-soon text"
    requirement: "PAYOUT-01"
    verification:
      - kind: unit
        ref: "ExpertDashboardScreenTest#payoutsToolOpensPayouts, profileSettingsToolOpensSettings, earningsCardOpensPayouts, noComingSoonText"
        status: pass
  - id: D2
    description: "Payout/KYC pushes open Payouts on the Expert Dashboard; client-only accounts and Client-mode calls get the current list"
    requirement: "PAYOUT-02"
    verification:
      - kind: unit
        ref: "PushDestinationResolverTest#payoutsPushOpensPayoutsOnDashboard, payoutsPushForClientOnlyOpensList"
        status: pass
  - id: D3
    description: "Account push opens the current mode's Profile tab with Account Security; payout/account notification rows are tappable"
    requirement: "PROF-03"
    verification:
      - kind: unit
        ref: "PushDestinationResolverTest#accountSecurityOpensProfileTab, NotificationDestinationTest#payoutRowsRouteToPayouts, NotificationsViewModelTest#rowTapPostsDestination, PushRouterTest#landsOnNotificationsList"
        status: pass
---

# Phase 14 Plan 15: Dashboard and Push Hand-offs Summary

The Dashboard's Payouts tool, earnings card and Profile Settings tool now navigate to the real Phase 14 screens, and payout, KYC and account pushes and notification rows open Payouts and Account Security instead of the notifications list.

## Accomplishments

- Dashboard: `onOpenPayouts` / `onOpenProfileSettings` threaded from `ExpertDashboardRoute` through `ExpertDashboardScreen` and `ExpertDashboardContent`. The Payouts cell and the earnings card open `PayoutsKey(SettingsOrigin.Dashboard)`; the Profile Settings cell opens `ExpertProfileSettingsKey(SettingsOrigin.Dashboard)`. The two coming-soon snackbars and the `dashboard_coming_soon` string are gone; `dashboard_open_payouts` ("Open payouts") added.
- Earnings card is a single `Role.Button` with click label "Open payouts"; the merged earnings content description is unchanged.
- Push routing: `Payouts` and `KycOnboarding` resolve to `ShellRoute(Expert, DashboardRoot, [PayoutsKey(Dashboard)])`; the existing `allowSwitch` check turns a client-only account or a call in Client mode into the current mode's list. `AccountSecurity` resolves to `[AccountSecurityKey]` on `ClientProfileRoot` / `ExpertProfileRoot` for the current mode with no switch.
- `landsOnNotificationsList` is true only for `NotificationsList`; both `PHASE-14` markers and the "until Phase 14" KDoc are removed, so payout/account rows post a destination on tap.
- Verified: ExpertDashboardScreenTest, ExpertDashboardViewModelTest, HomeShellsTest, PushRouterTest, PushDestinationResolverTest, NotificationDestinationTest, NotificationsViewModelTest, NotificationsScreenTest, PushRoutingViewModelTest all pass; `ktlintCheck`, `:app:assembleDevDebug` and `scripts/check-design-rules.sh` pass.

## Task Commits

1. RED tests for Task 1 - `d9e9528a` (test)
2. Task 1: Dashboard navigation - `d6dec601` (feat)
3. Task 2: push and notification routing (tests and implementation together) - `ba797788` (feat)

## Deviations from Plan

None - plan executed as written. Task 2's RED and GREEN landed in one commit rather than a separate failing-test commit.

## Known Stubs

None.

## Threat Flags

None. T-14-40/T-14-41 (client-only switch, switch during a call) are covered by `payoutsPushForClientOnlyOpensList`.

## Open Items

- The shell entries for `PayoutsKey`, `ExpertProfileSettingsKey` and `AccountSecurityKey` are registered by 14-14 (parallel plan); the on-device end-to-end tap checks belong to the 14-16 gate.

## Self-Check: PASSED

- Commits d9e9528a, d6dec601, ba797788 exist on the branch.
- Modified files exist; no `PHASE-14`, `until Phase 14` or `dashboard_coming_soon` references remain under `app/src`.
