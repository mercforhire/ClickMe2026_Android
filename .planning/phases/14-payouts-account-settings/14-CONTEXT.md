# Phase 14: Payouts & Account Settings - Context

**Gathered:** 2026-10-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Experts get paid through Stripe Connect, and any user can manage or delete their account. Requirements: PAYOUT-01, PAYOUT-02, PAYOUT-03, PROF-01, PROF-02, PROF-03, PROF-04, PROF-05.

The phase delivers:
- **Profile hubs** for both modes. They replace the throwaway Profile footers in `HomeClientScreen.kt:315` and `HomeExpertScreen.kt:215`.
- **Settings:** edit profile and avatar, Security & Password (change password and Delete Account), and FAQ & Help (with a `mailto:` support link).
- **Expert Payouts:** Connect onboarding in a Custom Tab with the `clickme://connect/return|refresh` deep links, balance and status, recent activity plus the full transactions list, and the tax profile.
- **Phase 9/10/11/13 hand-offs** (see Integration Points).

The ROADMAP cut order still applies if the schedule slips: payouts shrink to the onboarding link only (PAYOUT-02). Account deletion (PROF-04) is never cut.

</domain>

<decisions>
## Implementation Decisions

### Profile hub rows
- **D-01:** The hubs show **only rows that work**. Mirror the iOS section structure (`ClientProfileHomeScreen.swift`, `ExpertProfileHomeScreen.swift`), but **hide** these iOS rows, which no requirement covers: Saved Experts, Payment History, Notifications (settings), Send Feedback. Do not ship "coming soon" placeholders.
- **D-02:** Add two cheap links:
  - **Privacy** opens `AppLinks.PRIVACY_POLICY` through `CustomTabLauncher`, on both hubs.
  - **View My Public Profile** (expert hub) reuses the existing Expert Profile screen in self-view.
- **D-03:** Rows that stay:
  - **Client hub:** user card, Mode Switch, Profile settings, Security & Password, Payment (the existing Payment Methods), Privacy, FAQ & Help, Log Out.
  - **Expert hub:** user card, Mode Switch, View My Public Profile, Expertise & Topics, Availability (replaces the Phase 10 placeholder row), Payouts & Earnings, Tax & Payout Info, Security & Password, Privacy, FAQ & Help, Log Out.
  - The planner may adjust the order to match iOS.

### Delete account
- **D-04:** Delete Account lives **inside Security & Password** (bottom section), as on iOS. There is no separate hub row.
- **D-05:** Port the **full iOS flow**:
  1. Warning.
  2. Password, sent to `POST` request-deletion, which returns a `deletion_token`.
  3. Reason, one of 4 categories: `too_expensive`, `found_alternative`, `privacy_concerns`, `other`. Plus optional feedback.
  4. Final confirm: `DELETE` with `deletion_token`, `reason_category`, `feedback_details` and `confirm_permanent_action: true`.
  5. Farewell screen. "Back to Login" runs `UserManager.logout()`.

  — **Reversibility:** one-way — the account is deleted server-side. A wrong or double submit can't be undone, so guard against double taps.
- **D-06:** **Pause Account.** iOS shows a "Pause Account" card that does nothing. The user asked for a backend request for a real pause feature: `.planning/phases/14-payouts-account-settings/14-BACKEND-REQUEST.md`.
  - If the backend ships the endpoint before Phase 14 is planned, build the Pause card against it.
  - Otherwise **hide** the card, and the feature goes to the backlog.
  - Never ship an inert card.

### Payouts depth
- **D-07:** Port iOS Payouts (`PayoutsView.swift`):
  - Available balance, pending, the payout schedule, "Eligible for next payout".
  - The states: setup required ("Complete payout setup"), on hold ("Payment on hold"), ready with no earnings yet.
  - "Recent activity", **plus "See all activity"**, which opens the full paged transactions list (`TransactionsView.swift`, `EXPERT-EARNINGS-TRANSACTIONS.md`). Both share one row component.
  - Amounts follow PAYOUT-03: transactions are in cents, `earnings_breakdown` is in dollars, and `net_amount` can be null, so check `fee_source` first.
- **D-08:** **Include Tax & Payout Info** (`ExpertTaxProfileView.swift`, `ClickMeAPI+TaxProfile.swift`). It's a row on the expert hub, also reachable from Payouts. Country defaults to CA (the only Connect country in v1). Follow the iOS TIN handling: when the server responds `saved: false, tin_required`, ask for the TIN. This goes past the listed requirements by user decision. Add a requirement ID for it (suggested: PAYOUT-04) when planning.
- **D-09:** Connect onboarding is fixed by PAYOUT-02 and the ROADMAP: Custom Tab on the hosted Account Link. `clickme://connect/return` re-fetches the status. `clickme://connect/refresh` calls refresh-link and reopens it. The return goes into a Dashboard reload through `ExpertLaunchRequest.dashboard`.

### Edit profile
- **D-10:** **Auto-save like iOS.** A debounced PATCH runs on each change, with a saving/saved/error status icon in the TopAppBar actions. There is no Save button and no discard dialog. A failed avatar upload shows its own error dialog, as on iOS.
- **D-11:** **Two screens, mirroring iOS:**
  - `ClientProfileSettings`: avatar, read-only email, first and last name, bio, professional card (job title and company), city, state/province, country, languages.
  - `ExpertProfileSettings`: the same fields plus the "Skype / Zoom Meeting Link" field and an Expertise section that reuses `ExpertiseEditorSheet` / `ExpertiseTagsRepository` from Phase 10.
  - Shared field composables are fine. They are separate screens and ViewModels, matching the iOS file tree.
  - Skip the iOS "reset onboarding" debug affordance unless it is debug-only.
- **D-12:** **Reuse the Phase 4 signup editors and sheets** (professional info, languages, location, profile photo / `PhotoProcessor`) wherever they fit, so validation and copy match signup. The avatar uses `PickVisualMedia` (CLAUDE.md platform swap).

### Carried forward (already decided, not re-asked)
- **Help/FAQ:** port iOS `HelpView` (`GET /help/faqs`, categories sorted by `displayOrder`, expandable). The `mailto:` link uses `AppLinks.SUPPORT_EMAIL` through `ACTION_SENDTO` (`supportEmailIntent()` in `SuspendedAccountDialog.kt`).
- **CallGuard (Phase 9, D-13):** change password, delete account and payout actions go through `CallGuard.attempt(RestrictedAction.*)`, with new entries and `call_action_*` strings.
- **Platform swaps** per CLAUDE.md: `TopAppBar` back, `AlertDialog`, `ModalBottomSheet`, Material `Switch`.
- **Change password** follows iOS `AccountSecurityView` / `UpdatePasswordRequest`, and reuses `PasswordStrength` / `PasswordStrengthMeter`.

### Claude's Discretion
- Exact row order and icons within each hub section (follow iOS where it's clear).
- How shared field composables are split between the two settings screens.
- The auto-save debounce interval (match iOS if it's stated).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and hand-offs
- `.planning/ROADMAP.md` — Phase 14 section: success criteria, cut order and the Phase 9/10/11/13 hand-off notes.
- `.planning/REQUIREMENTS.md` — PAYOUT-01..03 and PROF-01..05. Add the tax-profile requirement (D-08).
- `.planning/phases/14-payouts-account-settings/14-BACKEND-REQUEST.md` — Pause Account and the tax-profile questions (D-06, D-08).

### iOS source of truth (ios-reference/ClickMe2026/)
- `Views/Screens/Client Mode/Profile/ClientProfileHomeScreen.swift` and its `Subviews/` — client hub.
- `Views/Screens/Expert Mode/Profile/ExpertProfileHomeScreen.swift` — expert hub, including the public-profile self-view.
- `Views/Screens/Settings/Client Profile Settings/ClientProfileSettingsView.swift` — client settings (auto-save).
- `Views/Screens/Expert Mode/Settings/ExpertProfileSettingsView.swift` and `ExpertProfileSettingsViewModel.swift` — expert settings.
- `Views/Screens/Settings/Account Security/AccountSecurityView.swift` and its ViewModel — change password and the Delete entry.
- `Views/Screens/Settings/Delete account/` — the full delete flow, `DeleteAccountReason.swift` and its subviews.
- `Views/Screens/Settings/Help/HelpView.swift` and `HelpViewModel.swift` — FAQ and `mailto:`.
- `Views/Screens/Expert Mode/Payouts/PayoutsView.swift`, `PayoutsViewModel.swift`, `TransactionsView.swift`, `TransactionsViewModel.swift`, `Subviews/ConnectOnboardingSession.swift`, `Subviews/EarningsTransactionRow.swift`.
- `Views/Screens/Expert Mode/Tax Profile/ExpertTaxProfileView.swift` and `ExpertTaxProfileViewModel.swift`.
- `Network/ClickMeAPI+Account.swift`, `+Payouts.swift`, `+TaxProfile.swift`, `+UserProfile.swift`, `+Me.swift`, `+Media.swift`, `+Support.swift`.
- `Model/Responses/PayoutSummaryData.swift`, `ConnectStatusData.swift`, `ConnectOnboardData.swift`, `EarningsTransactionsData.swift`, `TaxProfileData.swift`, `SaveTaxProfileData.swift`, `FaqArticle.swift`, `Children/Earnings*.swift`, `Children/Faq*.swift`.

### Backend contract (backend-reference/docs/ios/)
- `EXPERT-EARNINGS-BREAKDOWN.md` — breakdown in dollars; `net_amount` and `fee_source` rules.
- `EXPERT-EARNINGS-TRANSACTIONS.md` — transactions in cents and paging.
- `STRIPE-INTEGRATION.md` §3 (Connect onboarding, status, refresh-link, `CONNECT_NOT_RATIFIED`) and §5 (payouts).
- `backend-reference/openapi/clickme-api.yaml` — account deletion, password, profile, tax-profile and FAQ endpoints.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `components/ProfileMenuRow.kt` — the hub row, already used by the throwaway Profile footers.
- `components/web/CustomTabLauncher.kt` — Privacy link and Connect onboarding (http(s) only; `mailto:` goes through `ACTION_SENDTO`).
- `config/AppLinks.kt` — `PRIVACY_POLICY` and `SUPPORT_EMAIL`. `supportEmailIntent()` is in `screens/login/SuspendedAccountDialog.kt`.
- `components/PasswordStrength.kt` and `PasswordStrengthMeter.kt` — change password.
- `screens/signup/profilephoto/PhotoProcessor.kt`, `PhotoWorkingFilesCleaner.kt` and `di/PhotoModule.kt` — avatar picking and processing.
- The Phase 4 signup step editors under `screens/signup/` (basicinfo, tags, timezone, profilephoto) — D-12.
- `ExpertiseEditorSheet` / `ExpertiseTagsRepository` (Phase 10, `screens/expertmode/...`) — the expert Expertise section.
- `ClickMeApi` already has `expert/payouts/summary`, `expert/connect/status`, `expert/connect/onboard` and `expert/connect/refresh-link` (`ApiRequestUrls.kt:89-93`), plus the Dashboard `startPayoutSetup` path from Phase 11.
- `UserManager.logout()` — the end of the delete flow, with the full teardown and the push-token unregister.

### Established Patterns
- MVVM with Compose State, Hilt, and `apiCall {}` / `ApiException` error mapping (`toXFailure()` mappers per screen).
- Per-tab back stacks with `Navigator` and the per-shell `entry<>` registration (`HomeClientScreen.kt`, `HomeExpertScreen.kt`).
- `CallGuard.attempt(RestrictedAction.X)` with a `call_action_*` string for restricted actions.

### Integration Points
- Replace the Profile footers at `HomeClientScreen.kt:315-320` and `HomeExpertScreen.kt:215-220` with the hubs.
- `ExpertToolsGrid.kt:55` — wire the Profile Settings and Payouts handlers, plus the inert earnings card tap.
- `screens/push/PushDestination.kt:57` and `PushDestinationResolver.kt:104` — retarget the `PHASE-14` markers. Payouts and KycOnboarding pushes go to Payouts; AccountSecurity goes to Security & Password.
- `managers/CallDialog.kt:7` — the payout-setup `CallDialog` action.
- `clickme://connect/return|refresh` deep links feed `ExpertLaunchRequest.dashboard` and a Payouts reload.

</code_context>

<specifics>
## Specific Ideas

- The user wants Android to match iOS closely: two settings screens, auto-save, the full delete flow, Payouts with "See all activity".
- No dead UI in the store build: no "coming soon" rows and no inert Pause card.

</specifics>

<deferred>
## Deferred Ideas

- **Saved Experts (Favorites)**, **Payment History** (client transactions), **Notification settings**, **Send Feedback**. The iOS hub rows are hidden for v1 (D-01). They are candidates for the roadmap backlog: the endpoints exist (`ClickMeAPI+Favorites.swift`, `docs/ios/CLIENT-TRANSACTIONS.md`, `GET/PATCH /notifications/preferences`, `ClickMeAPI+Feedback.swift`).
- **Pause Account**, only if the backend doesn't ship it before planning (D-06).
- **iOS follow-up:** the iOS Pause card is inert; remove it or wire it once the backend endpoint exists.

</deferred>

---

*Phase: 14-payouts-account-settings*
*Context gathered: 2026-10-28*
