# Phase 14: Payouts & Account Settings - Research

**Researched:** 2026-10-28
**Domain:** Android Compose port of iOS profile hubs, account settings (profile edit, password, delete, FAQ) and Stripe Connect payouts, on the existing ClickMe backend
**Confidence:** MEDIUM-HIGH (backend contracts and in-repo facts are HIGH; the Custom Tab return deep link on a real device is MEDIUM and must be device-verified)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** The hubs show **only rows that work**. Mirror the iOS section structure (`ClientProfileHomeScreen.swift`, `ExpertProfileHomeScreen.swift`), but **hide** these iOS rows, which no requirement covers: Saved Experts, Payment History, Notifications (settings), Send Feedback. Do not ship "coming soon" placeholders.
- **D-02:** Add two cheap links: **Privacy** opens `AppLinks.PRIVACY_POLICY` through `CustomTabLauncher`, on both hubs. **View My Public Profile** (expert hub) reuses the existing Expert Profile screen in self-view.
- **D-03:** Rows that stay. **Client hub:** user card, Mode Switch, Profile settings, Security & Password, Payment (the existing Payment Methods), Privacy, FAQ & Help, Log Out. **Expert hub:** user card, Mode Switch, View My Public Profile, Expertise & Topics, Availability (replaces the Phase 10 placeholder row), Payouts & Earnings, Tax & Payout Info, Security & Password, Privacy, FAQ & Help, Log Out. The planner may adjust the order to match iOS.
- **D-04:** Delete Account lives **inside Security & Password** (bottom section), as on iOS. There is no separate hub row.
- **D-05:** Port the **full iOS flow**: (1) Warning. (2) Password, sent to `POST` request-deletion, which returns a `deletion_token`. (3) Reason, one of 4 categories: `too_expensive`, `found_alternative`, `privacy_concerns`, `other`. Plus optional feedback. (4) Final confirm: `DELETE` with `deletion_token`, `reason_category`, `feedback_details` and `confirm_permanent_action: true`. (5) Farewell screen. "Back to Login" runs `UserManager.logout()`. **Reversibility:** one-way — the account is deleted server-side. A wrong or double submit can't be undone, so guard against double taps.
- **D-06:** **Pause Account.** iOS shows a "Pause Account" card that does nothing. The user asked for a backend request for a real pause feature: `.planning/phases/14-payouts-account-settings/14-BACKEND-REQUEST.md`. If the backend ships the endpoint before Phase 14 is planned, build the Pause card against it. Otherwise **hide** the card, and the feature goes to the backlog. Never ship an inert card.
- **D-07:** Port iOS Payouts (`PayoutsView.swift`): available balance, pending, the payout schedule, "Eligible for next payout". The states: setup required ("Complete payout setup"), on hold ("Payment on hold"), ready with no earnings yet. "Recent activity", **plus "See all activity"**, which opens the full paged transactions list (`TransactionsView.swift`, `EXPERT-EARNINGS-TRANSACTIONS.md`). Both share one row component. Amounts follow PAYOUT-03: transactions are in cents, `earnings_breakdown` is in dollars, and `net_amount` can be null, so check `fee_source` first.
- **D-08:** **Include Tax & Payout Info** (`ExpertTaxProfileView.swift`, `ClickMeAPI+TaxProfile.swift`). It's a row on the expert hub, also reachable from Payouts. Country defaults to CA (the only Connect country in v1). Follow the iOS TIN handling: when the server responds `saved: false, tin_required`, ask for the TIN. This goes past the listed requirements by user decision. Add a requirement ID for it (suggested: PAYOUT-04) when planning.
- **D-09:** Connect onboarding is fixed by PAYOUT-02 and the ROADMAP: Custom Tab on the hosted Account Link. `clickme://connect/return` re-fetches the status. `clickme://connect/refresh` calls refresh-link and reopens it. The return goes into a Dashboard reload through `ExpertLaunchRequest.dashboard`.
- **D-10:** **Auto-save like iOS.** A debounced PATCH runs on each change, with a saving/saved/error status icon in the TopAppBar actions. There is no Save button and no discard dialog. A failed avatar upload shows its own error dialog, as on iOS.
- **D-11:** **Two screens, mirroring iOS:** `ClientProfileSettings` (avatar, read-only email, first and last name, bio, professional card (job title and company), city, state/province, country, languages) and `ExpertProfileSettings` (the same fields plus the "Skype / Zoom Meeting Link" field and an Expertise section that reuses `ExpertiseEditorSheet` / `ExpertiseTagsRepository` from Phase 10). Shared field composables are fine. They are separate screens and ViewModels, matching the iOS file tree. Skip the iOS "reset onboarding" debug affordance unless it is debug-only.
- **D-12:** **Reuse the Phase 4 signup editors and sheets** (professional info, languages, location, profile photo / `PhotoProcessor`) wherever they fit, so validation and copy match signup. The avatar uses `PickVisualMedia` (CLAUDE.md platform swap).
- **Carried forward:** Help/FAQ = port iOS `HelpView` (`GET /help/faqs`, categories sorted by `displayOrder`, expandable); the `mailto:` link uses `AppLinks.SUPPORT_EMAIL` through `ACTION_SENDTO` (`supportEmailIntent()`). CallGuard (Phase 9 D-13): change password, delete account and payout actions go through `CallGuard.attempt(RestrictedAction.*)`, new entries plus `call_action_*` strings. Platform swaps per CLAUDE.md. Change password follows iOS `AccountSecurityView` / `UpdatePasswordRequest` and reuses `PasswordStrength` / `PasswordStrengthMeter`.

### Claude's Discretion
- Exact row order and icons within each hub section (follow iOS where it's clear).
- How shared field composables are split between the two settings screens.
- The auto-save debounce interval (match iOS if it's stated).

### Deferred Ideas (OUT OF SCOPE)
- **Saved Experts (Favorites)**, **Payment History** (client transactions), **Notification settings**, **Send Feedback**. Hidden for v1 (D-01); backlog candidates.
- **Pause Account**, only if the backend doesn't ship it before planning (D-06).
- **iOS follow-up:** the iOS Pause card is inert; remove it or wire it once the backend endpoint exists.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PAYOUT-01 | Expert sees payout balance and Stripe Connect status | `ExpertFinancialRepository` already has `payoutSummary()` / `connectStatus()`; add `payout_method`; iOS state machine in `PayoutsViewModel.forceLoad` (Findings 3, 4) |
| PAYOUT-02 | Connect onboarding in a Custom Tab; `clickme://connect/return` re-fetches status; `/refresh` calls refresh-link | Backend 302 bridge confirmed; Android has NO intent filter yet; trampoline activity + `ExpertLaunchRequest` slot + ON_RESUME backstop (Finding 1) |
| PAYOUT-03 | Short recent-earnings list; cents for transactions, dollars for breakdown; null `net_amount` -> check `fee_source` | New `GET expert/earnings/transactions` DTO (minor units, `net_amount` non-null Int); the null-`net_amount`/`fee_source` rule is the Phase 11 `requestEarnings()` and must be reused, not rebuilt (Finding 4) |
| PROF-01 | Open client and expert profile hubs | Replace the two throwaway Profile footers; shell/`entry<>` registration pattern (Finding 8) |
| PROF-02 | Edit profile and avatar with the system photo picker | `PATCH user/profile`, `PATCH expert/profile` (`meeting_url`), avatar endpoints already in `ClickMeApi`; iOS never saves languages, Android must (Finding 5) |
| PROF-03 | Change password from Account Security | `PATCH user/password`; 403 = wrong current password; 422 field envelope (Finding 6) |
| PROF-04 | Delete account in-app (never cut) | Two-step `delete-request` + `DELETE user/account`; error map, Retrofit DELETE-with-body, farewell vs. logout race (Finding 7) |
| PROF-05 | Help/FAQ with `mailto:` support link | `GET help/faqs`; `supportEmailIntent()` exists (Finding 9) |
| PAYOUT-04 (proposed, D-08) | Expert can view and save the Tax & Payout Info profile | `GET/POST expert/tax-profile` (Finding 10) |
</phase_requirements>

## Summary

Phase 14 is a large but well-bounded port. Most of the plumbing already exists: `ExpertFinancialRepository` (summary, status, onboarding link), `isAllowedOnboardingUrl`, `resolvePayoutOnboarding`, `Money.format` / `formatMajor`, `requestEarnings()` (the `fee_source` rule), `PhotoProcessor`, `PasswordStrength*`, `ExpertiseEditorSheet`, `supportEmailIntent()`, `AppLinks`, `CustomTabLauncher`, `CallGuard`, and the `OneShotSlot` hand-off pattern in `ExpertLaunchRequest`. What is genuinely new is: eight Retrofit endpoints and their DTOs, the deep-link plumbing for `clickme://connect/*`, two settings ViewModels with debounced auto-save, a delete-account state machine that must survive the session teardown it triggers, and the hub screens that replace two throwaway footers.

Three findings change how the work should be planned. (1) **The app has no deep-link intent filter at all today**, and the backend bridges Stripe's https `return_url` to the app with a bare 302 to `clickme://connect/return`; whether Chrome follows that 302 into the app from a Custom Tab on a real device is unverified and must be treated as a device-test risk, with an ON_RESUME status reload as the always-on backstop. (2) **iOS has two bugs the port must not copy**: auto-save never sends languages (so editing languages on iOS silently does nothing), and a pending debounce is dropped when the user leaves the screen. (3) **Deleting the account kills the session server-side** (global revoke and ban), so any authenticated request after the DELETE (a quiet refresh, the realtime socket, the push-token unregister inside `logout()`) can emit `SessionEvent.Rejected`, which logs out and swaps the route to Login, removing the farewell screen before the user sees it. The farewell must be hosted at the root, outside the logged-in route.

**Primary recommendation:** Build in this order: (a) API/DTO/`RestrictedAction` foundation plus the deep-link trampoline, (b) Account Security + Delete + Farewell (PROF-03/04, never cut), (c) hubs + both settings screens + Help, (d) Payouts + Transactions + Tax profile, (e) Dashboard/push/ExpertTools hand-offs. If the schedule slips, cut (d) down to the onboarding link only, as the ROADMAP says.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Stripe Connect onboarding page | External hosted page (Custom Tab) | API / Backend (mints single-use Account Link) | Stripe hosts KYC; the app only opens the https URL (`isAllowedOnboardingUrl`) |
| Return/refresh bridge (https -> `clickme://`) | API / Backend (302 in `app.js`) | Client (intent filter + trampoline) | Stripe only accepts https return URLs; backend redirects to the scheme |
| Balance / status / transactions | API / Backend (live Stripe + ledger) | Client (render, format) | Client never computes money; formats minor units via `Money.format` |
| Connect-complete truth | API (`GET connect/status`) | — | Return redirect is not proof of completion (doc 3.1) |
| Profile edit, password, avatar | API / Backend | Client (debounce, validation mirror) | Server is the validator; client mirrors rules to save a round trip |
| Account deletion cascade | API / Backend | Client (2-step orchestration, farewell) | Server deletes and revokes sessions; client must show the farewell after |
| Session teardown after delete | Client (`UserManager.logout`) | — | Token store, push unregister, caches |
| FAQ content | API / Backend | Client (render) | Server-ordered categories |
| Support contact | Client (`ACTION_SENDTO` mailto:) | — | No backend; static address in `AppLinks` |
| Tax profile (TIN) | API / Backend (encrypts, never returns TIN) | Client (form, no TIN persistence) | TIN write-only |

## Standard Stack

### Core (all already in the project; **no new dependencies**)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Retrofit + Moshi (codegen) | existing | New endpoints/DTOs | Project standard (`ClickMeApi`) [VERIFIED: app/src/main/.../network/ClickMeApi.kt read this session] |
| `androidx.browser` | 1.10.0 | Custom Tab for Connect + Privacy | [VERIFIED: gradle/libs.versions.toml `androidx-browser = { group = "androidx.browser", name = "browser", version = "1.10.0" }`] |
| Navigation3 | 1.2.0 | New `NavKey`s + `entry<>` | [VERIFIED: libs.versions.toml `navigation3 = "1.2.0"`] |
| Hilt | 2.60.1 | ViewModels / singletons | [VERIFIED: libs.versions.toml `hilt = "2.60.1"`] |
| Compose BOM / Material 3 | 2026.09.00 | UI | [VERIFIED: libs.versions.toml `composeBom = "2026.09.00"`] |
| Robolectric / JUnit4 / compose ui-test | 4.17 / 4.13.2 / BOM | Tests | [VERIFIED: libs.versions.toml lines 39, 63, 64] |

### Reusable in-repo assets (do not rebuild)
| Asset | Path | Use |
|-------|------|-----|
| `ExpertFinancialRepository` | `screens/expertmode/dashboard/ExpertFinancialRepository.kt` | `payoutSummary()`, `connectStatus()`, `onboardingLink(refresh)` for Payouts |
| `resolvePayoutOnboarding`, `isAllowedOnboardingUrl` | `screens/expertmode/dashboard/DashboardFinancials.kt` | Same Connect-status rule as iOS `hasIncompleteConnectAccount` |
| `requestEarnings()` | `screens/expertmode/requestdecision/RequestEarnings.kt` | The `fee_source` / null-`net_amount` rule (already implemented in Phase 11) |
| `Money.format(minor, ccy)` / `formatMajor` | `util/Money.kt` | Cents for transactions/summary; dollars only for `earnings_breakdown` |
| `OneShotSlot`, `ExpertLaunchRequest` | `screens/expertmode/ExpertLaunchRequest.kt` | Add a Payouts hand-off slot beside `dashboard` |
| `LanguageSelectionSheet(Content)`, `CountryPickerSheet`, `PickerSheet`, `Countries` | `screens/signup/basicinfo/...`, `constants/Countries.kt` | D-12 editors (stateless composables are reusable; their ViewModels are signup-coupled) |
| `PhotoProcessor`, `PhotoCropScreen`, `PickVisualMedia` use | `screens/signup/profilephoto/` | Avatar picking/cropping |
| `ExpertiseEditorSheet` + `ExpertiseTagsRepository` | `screens/expertmode/settings/` | Expert Expertise section |
| `PasswordStrength`, `PasswordStrengthMeter`, `ValidatedField`, `AsyncSaveButton` | `components/` | Change password / forms |
| `supportEmailIntent()` | `screens/login/SuspendedAccountDialog.kt` | `mailto:` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Backend 302 + custom scheme | Verified Android App Links on the backend https URL | More robust on Chrome (no custom-scheme user-gesture rule) but needs backend `assetlinks.json` and a backend change; keep as the fallback if the device test fails |
| Trampoline activity | Intent filter directly on `MainActivity` | Simpler, but risks a second `MainActivity` instance stacked over the Custom Tab (Finding 1) |

**Installation:** none. **Package Legitimacy:** no external packages are added by this phase.

## Package Legitimacy Audit

No new external packages are installed in this phase; `androidx.browser`, Retrofit, Moshi, Hilt, Compose and Navigation3 are already pinned in `gradle/libs.versions.toml`. Audit not applicable. **Packages removed due to [SLOP]:** none. **Packages flagged [SUS]:** none.

## Architecture Patterns

### System Architecture Diagram

```
Expert taps Payouts (Dashboard tool / earnings card / Profile hub / push)
        |
        v
 PayoutsScreen --load--> ExpertFinancialRepository
        |                   |-- GET expert/connect/status   (404 => no account)
        |                   |-- GET expert/payouts/summary  (400 NO_PAYOUT_METHOD|KYC_INCOMPLETE => "not yet")
        |                   |-- GET expert/earnings/transactions?page=1&limit=20  (minor units)
        |                   `-- GET expert/bookings?type=upcoming (best effort: held count)
        |
   onboardingRequired? --yes--> "Complete payout setup" card
        |                          | Continue -> CallGuard.attempt(SetUpPayouts)
        |                          v
        |                    POST connect/onboard | connect/refresh-link  -> https URL (single use, ~5 min)
        |                          v
        |                    Custom Tab (Stripe hosted) ----> https backend /connect/return|refresh
        |                                                          | 302
        |                                                          v
        |                                              clickme://connect/return|refresh
        |                                                          v
        |                                  ConnectReturnActivity (trampoline: validate host+path only)
        |                                     | post ConnectReturn slot + dashboard slot
        |                                     v   startActivity(MainActivity, CLEAR_TOP|SINGLE_TOP)
        |                                  MainActivity.onNewIntent -> (Custom Tab closed by CLEAR_TOP)
        |                                     v
        |             /return => reload status+summary      /refresh => mint refresh-link, reopen tab
        |
   ALSO: Payouts ON_RESUME => reload (covers: redirect blocked, user closes tab by hand)

Profile hub --> ClientProfileSettings / ExpertProfileSettings --(debounced 1.2 s)--> PATCH user/profile
                                                       `--> PATCH expert/profile (meeting_url, only if changed)
            --> Security: PATCH user/password | Delete: POST delete-request -> DELETE user/account
                                                  `--> AccountDeletionState(flag) -> root Farewell -> logout()
            --> Help: GET help/faqs  |  mailto: ACTION_SENDTO
```

### Recommended Project Structure (mirror iOS tree, per CLAUDE.md)
```
screens/
├── settings/
│   ├── profilehub/            # ClientProfileHome*, ExpertProfileHome* (+ shared user card, section header)
│   ├── clientprofile/         # ClientProfileSettingsScreen/ViewModel
│   ├── accountsecurity/       # AccountSecurityScreen/ViewModel + AccountSecurityKey
│   ├── deleteaccount/         # DeleteAccountScreen/ViewModel/Reason, Farewell, AccountDeletionState
│   ├── help/                  # HelpScreen/ViewModel/Repository
│   └── profile/               # shared field composables (labeled field, bio, professional card)
├── expertmode/
│   ├── settings/              # existing; add ExpertProfileSettingsScreen/ViewModel
│   ├── payouts/               # PayoutsScreen/ViewModel/Logic, TransactionsScreen/ViewModel, EarningsTransactionRow, ConnectReturn*
│   └── taxprofile/            # ExpertTaxProfileScreen/ViewModel/Repository
app/src/main/AndroidManifest.xml   # + ConnectReturnActivity
```

### Pattern 1: Retrofit DELETE with a body
`@DELETE` cannot carry `@Body` in Retrofit; use `@HTTP(method = "DELETE", path = ..., hasBody = true)`. [ASSUMED: Retrofit API; verify at first compile/run, a violation is a runtime `IllegalArgumentException` when the service method is first used.]
```kotlin
// New ApiRequestUrls constants (paths relative, no leading slash like the existing ones):
//   USER_PROFILE already = "user/profile"; add:
//   USER_PASSWORD = "user/password"; USER_ACCOUNT_DELETE_REQUEST = "user/account/delete-request"
//   USER_ACCOUNT = "user/account"; HELP_FAQS = "help/faqs"
//   EXPERT_EARNINGS_TRANSACTIONS = "expert/earnings/transactions"; EXPERT_TAX_PROFILE = "expert/tax-profile"
@HTTP(method = "DELETE", path = ApiRequestUrls.USER_ACCOUNT, hasBody = true)
suspend fun deleteAccount(@Body body: DeleteAccountRequest): SuccessMessageResponse
```
Paths: [VERIFIED: ios-reference Network/APIRequestURLs.swift lines 216-220 `.getUserProfile: return "/user/profile"` / `.updatePassword: return "/user/password"`, 289 `"/expert/earnings/transactions"`, 295-296 `"/expert/tax-profile"`, 335 `"/help/faqs"`, 350-351 `"/user/account/delete-request"` / `"/user/account"`; backend openapi headers `/user/password:` (822), `/user/account:` (923), `/user/account/delete-request:` (1076), `/help/faqs:` (1522), `/user/profile:` (5092)].

### Pattern 2: One-shot hand-off slot (existing)
Add `val connectReturn = OneShotSlot<ConnectReturn>()` to `ExpertLaunchRequest` next to `val dashboard = OneShotSlot<DashboardLaunch>()` [VERIFIED: ExpertLaunchRequest.kt read this session]. The deep-link receiver posts BOTH `dashboard` (Dashboard reloads quietly while it is still alive under Payouts) and `connectReturn` (Payouts reload / refresh). A Singleton is required because the target ViewModel may not exist yet (existing KDoc rationale).

### Pattern 3: Auto-save (port of iOS, with fixes)
1.2 s debounce [VERIFIED: ClientProfileSettingsViewModel.swift `private let autoSaveDebounce: UInt64 = 1_200_000_000 // 1.2 s`]; skip when the current snapshot equals `lastAppliedSnapshot`; show Saving/Saved/Error in the TopAppBar actions; re-baseline the snapshot only after a successful PATCH.

### Anti-Patterns to Avoid
- **Copying the iOS language skip.** iOS `performAutoSave` omits `languages` ("wire once a picker sources IDs"), so an iOS language edit never persists. Android already has IDs (`LanguageEntry.id`, `LanguageItem`): send `languages.language_ids` + `languages.custom_languages` (group present = full replace).
- **Dropping a pending debounce on back.** iOS cancels nothing on leave but the task is tied to the VM; on Android, `viewModelScope` is cancelled when the screen pops, losing the last edit. Flush (save immediately, `NonCancellable`/app scope) on back/`onCleared`.
- **Treating the return redirect as proof of completion** (backend doc 3.1). Always re-fetch `connect/status`.
- **Putting the farewell inside the logged-in shell** (Finding 7).
- **Logging, caching or persisting** the Account Link URL, the `deletion_token`, or the TIN. Do not put them in `SavedStateHandle`.
- **Showing a fake "online" dot** on the hub user card: iOS hard-codes `isOnline = true` (`self.isOnline = true`, never updated). D-01 says no dead UI; omit it.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Currency formatting | A new cents/dollars formatter | `Money.format` (minor) / `Money.formatMajor` (major) | Zero-decimal currencies, locale; already shared by Phases 5-11 |
| Null `net_amount` rule | New breakdown logic | `requestEarnings()` | Done in Phase 11; PAYOUT-03's null-net sentence is about `earnings_breakdown` |
| Connect-needed decision | New status logic | `resolvePayoutOnboarding` + `isAllowedOnboardingUrl` | Matches iOS `hasIncompleteConnectAccount` |
| Custom Tab launch | Raw `CustomTabsIntent` | `rememberCustomTabLauncher` / `Context.openCustomTab` | http(s)-only guard, dark theme, fallback |
| Photo pick/crop/JPEG | New pipeline | `PickVisualMedia` + `PhotoProcessor` + `PhotoCropScreen` | Phase 4 tested path |
| Password strength | New meter | `PasswordStrength.evaluate` + `PasswordStrengthMeter` | D-locked reuse |
| Language / country pickers | New sheets | `LanguageSelectionSheet(Content)`, `CountryPickerSheet`, `Countries.normalize` | Alpha-3 handling already solved |
| Pagination flag | Own derivation | `PaginationMeta.hasMoreOrDerived` | [VERIFIED: PaginationMeta.kt] |
| Mail compose | Custom mail UI | `supportEmailIntent()` + try/catch `ActivityNotFoundException` | `startActivity` needs no `<queries>`; only `resolveActivity` would |

**Key insight:** every money fact is server-owned and already has a formatter; every editor already exists. New code is orchestration (state machines, error mapping, navigation) and the deep-link bridge.

## Runtime State Inventory

Not a rename/refactor phase. Omitted. (Related state to be aware of: `UserManager.profile` cache must be refreshed after profile/avatar edits so the top-bar avatar and hub user card update; account deletion must leave nothing cached for the next login, which `UserManager.logout()` already handles via `SessionDataCleaner`s.)

## Common Pitfalls

### Pitfall 1: No deep-link wiring exists, and the launch mode can duplicate the activity
**What goes wrong:** `AndroidManifest.xml` has one `MAIN/LAUNCHER` intent filter on `MainActivity` (`android:launchMode="singleTop"`) and no `VIEW/BROWSABLE` filter [VERIFIED: AndroidManifest.xml read this session]. Adding `clickme` data to `MainActivity` directly means Chrome's intent arrives while the Custom Tab (not `MainActivity`) is on top of the task, so `singleTop` does not reuse the instance and a second `MainActivity` can stack over the tab.
**How to avoid:** Add a tiny exported `ConnectReturnActivity` (`Theme.NoDisplay`, `noHistory`, `excludeFromRecents`) with the filter below. It validates `scheme == "clickme" && host == "connect" && path in {"/return","/refresh"}`, posts the slots, then starts `MainActivity` with `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` and `finish()`es. CLEAR_TOP closes the Custom Tab above `MainActivity` and delivers `onNewIntent`. This is the AppAuth-Android "redirect receiver activity" pattern. [ASSUMED: design from AppAuth practice, the launch-mode behavior must be confirmed on the Samsung device]
```xml
<activity android:name=".screens.expertmode.payouts.ConnectReturnActivity"
    android:exported="true" android:noHistory="true" android:excludeFromRecents="true"
    android:theme="@android:style/Theme.Translucent.NoTitleBar">
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="clickme" android:host="connect" />
  </intent-filter>
</activity>
```
**Security:** the activity is exported and any app can fire `clickme://connect/return`. Treat it only as a "go re-check" signal: it posts a reload, never marks anything complete, and ignores every query parameter and extra (ASVS V5). Mirror the existing `consumePushDestination` discipline.

### Pitfall 2: Chrome may not follow the backend 302 into the app
**What goes wrong:** The backend serves `res.redirect(302, 'clickme://connect/return')` [VERIFIED: backend-reference/src/app.js:223-224 `app.get('/connect/return', (_req, res) => res.redirect(302, 'clickme://connect/return'));` and `'/connect/refresh'` likewise]. Chrome applies a user-gesture rule to external-protocol navigations; a redirect at the end of Stripe's own JS-driven page flow may not count as user-initiated, in which case the tab shows an error page and the app is never called. Search results show historical Chromium reports of Custom Tabs not following an automatic custom-scheme 302 (fixed in some versions) and AppAuth reports of redirects failing after backgrounding. [CITED: groups.google.com/a/chromium.org/g/chromium-bugs/c/LkjAfpkacXA; github.com/openid/AppAuth-Android/issues/135] Behavior on current Chrome with Stripe's flow is unverified. [ASSUMED]
**How to avoid:** (1) Always reload on `ON_RESUME` of Payouts and Dashboard (Dashboard already re-fetches Connect status on resume, A5). Then a user who closes the tab by hand or sees a blocked redirect still lands on an up-to-date screen. (2) Make the device test of PAYOUT-02 an explicit gate (manual testing is deferred by project policy, so log it as an open item with the exact steps). (3) Fallback if blocked: ask the backend for an HTML bridge page with a user-tappable "Return to ClickMe" button, or move to Android App Links on the backend https URL.
**Warning signs:** Custom Tab shows `ERR_UNKNOWN_URL_SCHEME`; status never refreshes after a successful Stripe flow.

### Pitfall 3: Account deletion logs the user out before the farewell can show
**What goes wrong:** After `DELETE user/account` the server revokes all sessions and bans the user (openapi 923: "global session revoke and ban with email release"). Any later authenticated request returns 401 -> `TokenAuthenticator` refresh fails -> `rejectSession()` -> `SessionEvent.Rejected` -> `UserManager.logout()` -> `isLoggedIn=false` -> `AppViewModel` sets route Login [VERIFIED: TokenAuthenticator.kt and UserManager.onSessionEvent `SessionEvent.Rejected -> logout()`; AppViewModel collector `userManager.isLoggedIn.collect`]. The quiet reloads (Dashboard resume, realtime reconnect) and `logout()`'s own `pushTokenRegistrar.unregister()` all make such requests. iOS avoided this only by deferring logout to the button.
**How to avoid:** Hold the farewell in a root-level singleton (`AccountDeletionState`, a `StateFlow<Boolean>`), set it the instant the DELETE returns 200, and have `ClickMeApp` render the Farewell above the route switch while it is true. "Back to Login" clears the flag and calls `logout()` if still logged in (idempotent). Do not make any other authenticated call between the DELETE success and the button. A forced Rejected-driven logout in the meantime is harmless because the farewell does not depend on `isLoggedIn`.

### Pitfall 4: Wrong password on delete-request is a code-less 401 (and costs two rate-limit hits)
**What goes wrong:** `POST /user/account/delete-request` answers a wrong password with `401 { status:error, message:"Invalid password." }` and no `code` (openapi 1076+ "wrong_password ... no code ... enumeration-safe"). `TokenAuthenticator` treats any 401 on an authed path as a possibly-expired token: it refreshes, retries the request once, and only then returns the 401 [VERIFIED: TokenAuthenticator.kt `resolve`]. The retry does not log the user out (its KDoc says the code check exists for exactly this), but each wrong attempt hits the 5 req / 15 min per-IP `deleteLimiter` twice [ASSUMED: limiter counts the retry], so three wrong passwords can produce a 429.
**How to avoid:** Map `Http.status in {401,403}` on the request step to an inline password error (server message or "Password is incorrect."), `429` to "Too many attempts" (`ErrorCode.TooManyRequests`), and never retry automatically. Disable the button while in flight (D-05 double-tap guard) and keep it disabled until the response arrives.

### Pitfall 5: The deletion token is burned even when the guard blocks
**What goes wrong:** `DELETE user/account` can return `409` (`EXPERT_HAS_UPCOMING_BOOKINGS`, `SESSION_IN_PROGRESS`, `CLIENT_HAS_UPCOMING_BOOKINGS`); the token is already consumed ("the token is already burned; request a new one after resolving", openapi delete-request 409). `CLIENT_HAS_UPCOMING_BOOKINGS` is not in the Android `ErrorCode` enum (it has `SessionInProgress("SESSION_IN_PROGRESS")` and `ExpertHasUpcomingBookings("EXPERT_HAS_UPCOMING_BOOKINGS")` only) [VERIFIED: ErrorCode.kt read this session].
**How to avoid:** Show the server `message` verbatim (it is user-ready) in an error dialog; keep the user on the form; on retry run both calls again (mint a new token). Add `ClientHasUpcomingBookings` to the Android-only block of `ErrorCode` (keep after the iOS 65), or just rely on `userMessage`. A `400 INVALID_TOKEN` at step 2 means re-enter the password (iOS copy: "That took too long. Please enter your password and try again.").

### Pitfall 6: Transactions are cents, but the summary note says nothing about null
**What goes wrong:** Conflating PAYOUT-03's two sentences. On `GET expert/earnings/transactions`, `gross_amount`, `commission_amount` and `net_amount` are non-null integers in MINOR units; only `booking_id`, `session_date`, `topic_title`, `client_name`, `processed_at` are nullable, and a null renders as the placeholder `—` (spec sec. 2). The null-`net_amount`/`fee_source` rule belongs to `earnings_breakdown` (major units) on request/booking payloads, already handled by `requestEarnings()`. [VERIFIED: backend-reference/docs/ios/EXPERT-EARNINGS-TRANSACTIONS.md field table and sec. 3]
**How to avoid:** Row = `Money.format(net_amount, currency)` (never `/100`, never `formatMajor`). Add a regression test pinning both conventions side by side. `payout_status` includes `settled_direct` (normal terminal state): label "Settled", never "Pending"/"In your bank"; unknown values -> "Unknown" pill, not a decode failure (use a nullable String + mapper, not a strict enum).

### Pitfall 7: Expert `meeting_url` cannot be sent empty
**What goes wrong:** iOS comment: the server URL-validates `meeting_url` and 422s on `""`; iOS therefore omits the key when empty (so an expert can never clear the link). [CITED: ExpertProfileSettingsViewModel.swift comment on `meetingUrlForPatch`; contract claim unverified against the backend schema, ASSUMED] Android's `ExpertProfileData` does not model `professional_details` at all (its KDoc: "`professional_details` ... not modelled"), so the field cannot be pre-filled today.
**How to avoid:** Add `professional_details.meeting_url` to the GET model, send it via `PATCH expert/profile` only when non-blank and changed, validate as http(s) client-side with `isAllowedMeetingUrl`, and note the "cannot clear" limitation (or ask the backend, see Open Questions).

### Pitfall 8: Self-view of the public profile
**What goes wrong:** `entry<ExpertProfileKey>` is registered only in the client shell's `detailEntries` [VERIFIED: HomeClientScreen.kt line 129], so the expert shell cannot open it; and `ExpertProfileKey` has no self flag, so Book Session would show and a tap ends in `422 CANNOT_BOOK_SELF`.
**How to avoid:** Register `entry<ExpertProfileKey>` in `HomeExpertScreen.detailEntries`, add a defaulted `isSelfView: Boolean = false` to the key (keeps old serialized keys valid), build the key from `UserManager.me` / `profile`, and hide Book (the screen already hides Message/safety through `chatEntry.isSelf(state.expertId)`; reuse the same check for the Book CTA).

### Pitfall 9: Existing tests assert the "coming soon" placeholders
`ExpertDashboardScreen.kt` currently shows `dashboard_coming_soon` snackbars for `ExpertTool.ProfileSettings` and `ExpertTool.Payouts` [VERIFIED: ExpertDashboardScreen.kt lines 151-162], and `PushRouterTest.landsOnNotificationsList` asserts `PushDestination.Payouts / KycOnboarding / AccountSecurity` all land on the list [VERIFIED: PushRouterTest.kt lines 103-111]. Both tests, plus `ExpertDashboardScreenTest`, must be updated in the same plan that changes the behavior.

### Pitfall 10: Sensitive input and process death
TIN, passwords and the deletion token must live only in ViewModel memory, never in `SavedStateHandle`/`rememberSaveable`, and must be cleared after a successful save (iOS clears `tin` after persist). Use `KeyboardOptions(autoCorrectEnabled = false)` and a masked field for TIN and passwords. `android:allowBackup="false"` is already set [VERIFIED: AndroidManifest.xml].

## Code Examples

### Hub Privacy row (existing launcher)
```kotlin
// Source: components/web/CustomTabLauncher.kt (read this session)
val openTab = rememberCustomTabLauncher { /* show snackbar */ }
ProfileMenuRow(title = ..., icon = ..., onClick = { openTab(Uri.parse(AppLinks.PRIVACY_POLICY)) })
```

### Connect return handling (reload vs. refresh)
```kotlin
// Receiver posts; PayoutsViewModel consumes (OneShotSlot semantics).
sealed interface ConnectReturn { data object Done : ConnectReturn; data object LinkExpired : ConnectReturn }
// path "/return"  -> Done        => reload status + summary (never assume success)
// path "/refresh" -> LinkExpired => CallGuard.attempt(SetUpPayouts), financial.onboardingLink(refresh = true), open via launcher
```
Cadence mirrors iOS `PayoutsView.onChange(of: connectOnboardingURL)` [VERIFIED: PayoutsView.swift: `if callback.isConnectRefresh { await viewModel.startOnboarding() } else { await viewModel.reload() }`].

### Payouts screen state selection (port of iOS)
```text
loadState Failed (status == null AND summary failed hard)  -> error + Retry
onboardingRequired (= !chargesEnabled || currentlyDue.isNotEmpty(); else summary 400)  -> "Complete payout setup" card
isReadyWithNoEarnings (!onboardingRequired && (summary == null || available==0 && pending==0)):
        heldCount > 0 -> "Payment on hold" card   else -> "You're ready to receive payouts" card; plus payout meta card
else  -> balance hero + pending line + "Eligible for next payout" (available > 0 && !hasIncompleteConnectAccount) + meta card + schedule note
recent activity: first 5 rows; "See all activity" if hasMore || list > 5
```
Source: PayoutsView.swift `loadedContent`, `isReadyWithNoEarnings`, `eligibilityAffordance`, `previewRows`, `shouldShowSeeAll`. Amounts may be **negative** (dispute residue): never assume >= 0 [VERIFIED: PayoutSummaryData.swift doc]. `next_payout_date` null -> "Manual schedule"; format `yyyy-MM-dd` as `MMMM d, yyyy`.

### Tax-profile save shape
```text
POST expert/tax-profile body (.strict): legal_name, address{line1,line2?,city,region,postal_code,country}, dob "YYYY-MM-DD", tin?, financial_account_identifier?
200 {saved:true, tin_required:false}                      => clear TIN field, show "Tax profile saved."
200 {saved:false, tin_required:true, message}             => NOT an error: keep fields, show message, ask for TIN (nothing was written)
422 VALIDATION_ERROR ("TIN format is invalid", bad dob)   => inline field error
GET: {tin_on_file, legal_name, address|null, dob, financial_account_identifier}  (all-null defaults, not 404, when no row)
```
[VERIFIED: expertTaxProfile.controller.js and expertTaxProfile.schema.js read this session; `address: z.object({}).passthrough()`, `.strict()`.]

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Self-service withdraw | Automatic Stripe payouts; `POST payouts/withdraw` -> 410 `ENDPOINT_RETIRED` | quick 260820-lh4 | No withdraw UI, no minimum/threshold gating; do not decode `can_withdraw`/`min_withdrawable_amount` |
| Ledger `paid` status | `settled_direct` is the normal terminal status | quick 260816-wyh | Label "Settled"; decode unknown statuses leniently |
| Fixed-amount `payout_method` enum | Only `bank_transfer` today | v1 | Render raw value prettified if it ever changes |
| `KYC_INCOMPLETE` terminal | Self-heals; retry is meaningful | 260811-pbx | Never cache a 400 summary as permanent (reload on resume) |

**Deprecated/outdated:** iOS `ASWebAuthenticationSession` auto-close on `clickme://` (no Android equivalent; Custom Tab + trampoline replaces it).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Chrome Custom Tab will follow the backend 302 to `clickme://connect/*` into the app on a current device | Pitfall 2 | Return/refresh deep links never fire; mitigated by ON_RESUME reload, but "expired link" recovery (`/refresh`) would not work without a backend change |
| A2 | A trampoline activity with CLEAR_TOP|SINGLE_TOP is needed to avoid a duplicate `MainActivity` over the Custom Tab | Pitfall 1 | Unneeded extra activity (low cost), or if wrong the other way a duplicate shell |
| A3 | Retrofit `@DELETE` rejects `@Body`; `@HTTP(hasBody=true)` is required | Pattern 1 | Runtime crash on first delete call (caught by a unit test using MockWebServer-style request check) |
| A4 | Wrong-password retry consumes two `deleteLimiter` hits | Pitfall 4 | Rate-limit lockout slightly earlier than expected; UX copy only |
| A5 | Backend answers to `14-BACKEND-REQUEST.md` are not in yet (the Answers section is empty), so Pause Account is not available | D-06 | If the endpoint shipped, the Pause card should be built; plan must re-check the file before execution |
| A6 | Tax profile does not gate payouts or Connect onboarding (no gating code seen in `connect.controller` / tax controller) | Finding 10 | If gated, Payouts needs an extra "complete tax info" state; ask the backend (still open) |
| A7 | Server 422s `meeting_url: ""` on `PATCH expert/profile` | Pitfall 7 | Auto-save error on first edit if the key is sent empty; omit-when-blank avoids it either way |
| A8 | "Payment on hold" held-count uses expert upcoming bookings with status pending_approval/confirmed/pending_reschedule/in_progress (iOS `fetchUpcomingHeldCount`); Android status strings must be mapped from `ExpertBookingsPayload` | Payouts states | Wrong card (hold vs ready); low impact, best-effort by design |
| A9 | Tax address read-back: iOS writes `region`; backend docs example uses `province`. Android should write `region` + `postal_code` (iOS parity) and read `region` falling back to `province` | Finding 10 | Cross-platform prefill mismatch only |

## Open Questions (RESOLVED)

1. **Did the backend answer `14-BACKEND-REQUEST.md`?**
   - Known: the file is marked "not sent yet" and the Answers section is empty.
   - Unclear: Pause Account endpoint, tax-profile gating, whether non-CA experts exist.
   - Recommendation: planner re-reads the file at plan time. Default per D-06: hide the Pause card, add a backlog item; implement tax profile ungated.
   - RESOLVED (planning, 2026-10-29): re-read at plan time, still no answers. No Pause card is rendered anywhere (14-03 hubs, 14-05 delete flow; D-06), and 14-16 adds backlog item "Phase 999.2: Pause Account" and logs the unsent request in STATE. The tax profile is built ungated (14-11 Assumptions; 14-08 adds no tax state), with country defaulting to CA (14-11). The gating and non-CA questions stay open as 14-16 backend notes.
2. **Can an expert clear the Skype/Zoom link?** iOS cannot (empty is omitted). Recommendation: ship parity; log a backend question.
   - RESOLVED (planning, 2026-10-29): iOS parity. A blank link is never sent and a non-http(s) link is rejected inline (14-13 Task 1-2; prohibition and T-14-37), so an expert cannot clear it; 14-16 logs the backend question in STATE "[Backend notes, Phase 14]".
3. **Does the hub need an "Availability" or "Expertise & Topics" distinct destination?** `AvailabilityKey(AvailabilityOrigin.Profile)` already exists and is used by the throwaway row; "Expertise & Topics" on iOS opens `TopicsSetupView`; Android's Topics tab is `TopicsRoot`. Recommendation: the row selects the Topics tab (`navigator.navigate(TopicsRoot)`, same as the Dashboard tool does).
   - RESOLVED (planning, 2026-10-29): no new destinations. Expertise & Topics selects the Topics tab via `navigator.navigate(TopicsRoot)` and Availability opens `AvailabilityKey(AvailabilityOrigin.Profile)` (14-03 hub actions and Assumptions; 14-14 `expertProfileHubActions` and its `expertiseAndTopicsSelectsTopicsTab` / `availabilityRowStillOpensAvailability` tests).
4. **Client hub "Profile settings" entry:** on iOS it is the user-card edit tap, not a list row. Recommendation: keep it as the card's edit action plus make the whole card tappable.
   - RESOLVED (planning, 2026-10-29): the whole user card is one `Role.Button` with the decorative Edit icon and opens settings (14-03 `ProfileHubUserCard`, Assumptions); 14-14 maps `onEditProfile` to `ClientProfileSettingsKey` (client) and `ExpertProfileSettingsKey(SettingsOrigin.Profile)` (expert). There is no separate "Profile settings" list row.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK (Android Studio JBR) | Gradle unit tests, ktlint | yes | `/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java` exists | none needed |
| `adb` on PATH | Device deep-link test | no (not on PATH in this shell) | — | Use Android Studio's SDK `platform-tools/adb` by absolute path; manual device checks are deferred by project policy |
| Samsung test device + Chrome | PAYOUT-02 Custom Tab return | not probed (device state unknown) | — | Log as an open manual item (project policy "Manual testing deferred") |
| Test-mode backend with Connect Express (CA) | Real onboarding run | unknown | live mode returns `403 CONNECT_NOT_RATIFIED` | Unit/Robolectric coverage only until a test backend is confirmed |
| Network (web research) | — | yes | — | — |

**Missing dependencies with no fallback:** none block code work.
**Missing dependencies with fallback:** device/backend checks become logged open items.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + Compose ui-test (junit4) [VERIFIED: libs.versions.toml] |
| Config file | Gradle module `app`, tests in `app/src/test/java/com/uptrendinvestments/clickmeapp/...` (no `androidTest`) |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*SomeTest' ktlintCheck` |
| Full suite command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest ktlintCheck` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command (filter) | File Exists? |
|--------|----------|-----------|----------------------------|-------------|
| PAYOUT-01 | Payouts state selection (onboarding / hold / ready / hero), negative balance, null date -> "Manual schedule" | unit (pure logic + VM) | `--tests '*PayoutsLogicTest' --tests '*PayoutsViewModelTest'` | Wave 0 |
| PAYOUT-02 | Deep-link validator accepts only `clickme://connect/return|refresh`; ignores extras; `/refresh` mints refresh link behind `SetUpPayouts`; slot posts dashboard + connectReturn | unit (Robolectric intent) | `--tests '*ConnectReturnIntentTest'` | Wave 0 |
| PAYOUT-02 | Manifest declares exactly one `clickme`/`connect` BROWSABLE filter | unit (Robolectric PackageManager) | `--tests '*ConnectReturnManifestTest'` | Wave 0 |
| PAYOUT-03 | Row shows `Money.format(net_amount)` from cents; null fields render `—`; `settled_direct` -> "Settled"; unknown status -> "Unknown"; breakdown null-net regression via `requestEarnings` | unit | `--tests '*EarningsTransactionRowTest' --tests '*RequestEarningsTest'` | Row test Wave 0; `RequestEarnings` test exists (verify) |
| PAYOUT-03 | Transactions paging (page 2 on tail, error keeps rows, retry) | unit (VM) | `--tests '*TransactionsViewModelTest'` | Wave 0 |
| PAYOUT-04 | Tax: canSave gate, `saved:false` is not an error, TIN cleared after save, region/province read fallback | unit (VM) | `--tests '*ExpertTaxProfileViewModelTest'` | Wave 0 |
| PROF-01 | Hub rows exactly per D-03 (no hidden rows present); Pause card absent | Robolectric compose | `--tests '*ClientProfileHomeScreenTest' --tests '*ExpertProfileHomeScreenTest'` | Wave 0 |
| PROF-02 | Auto-save: debounce, snapshot skip, languages sent as ids+custom, meeting_url omitted when blank, flush on leave, avatar error dialog | unit (VM, test dispatcher) | `--tests '*ClientProfileSettingsViewModelTest' --tests '*ExpertProfileSettingsViewModelTest'` | Wave 0 |
| PROF-03 | Validation mirror (>=8, digit, differs, confirm match); 403 -> current-password error; 422 field map; guarded by `ChangePassword` | unit (VM) | `--tests '*AccountSecurityViewModelTest'` | Wave 0 |
| PROF-04 | Delete flow: 401/403 -> password error, 429, 409 dialog, 400 INVALID_TOKEN, double-tap guard, success sets `AccountDeletionState` before any other call; farewell survives `isLoggedIn=false`; `DELETE` sends body | unit + Robolectric | `--tests '*DeleteAccountViewModelTest' --tests '*DeleteAccountRequestTest' --tests '*FarewellHostTest'` | Wave 0 |
| PROF-05 | FAQ sort by `display_order`, empty-featured categories hidden, mailto intent is `ACTION_SENDTO mailto:support@clickme.app`, no-handler failure shows message | unit | `--tests '*HelpViewModelTest'` | Wave 0 |
| Hand-offs | `RestrictedAction.ChangePassword/DeleteAccount` have `call_action_*` strings (every enum entry mapped) | unit | `--tests '*CallDialogsTest'` | Exists, extend |
| Hand-offs | Push Payouts/Kyc/AccountSecurity resolve to real routes; `landsOnNotificationsList` only true for `NotificationsList`; client-only Payouts push stays on list | unit | `--tests '*PushRouterTest' --tests '*PushDestinationResolverTest'` | Exists, update |
| Hand-offs | Dashboard tool taps navigate (no snackbar) | Robolectric compose | `--tests '*ExpertDashboardScreenTest'` | Exists, update |

### Sampling Rate
- **Per task commit:** the quick command filtered to the touched test class + `ktlintCheck`.
- **Per wave merge:** full `:app:testDevDebugUnitTest ktlintCheck`.
- **Phase gate:** full suite green before `/gsd-verify-work`; device items (Custom Tab return, delete on a throwaway account, avatar pick) logged as open manual items.

### Wave 0 Gaps
- [ ] DTO decode tests for each new response (fixtures from the openapi examples) in `app/src/test/.../model/`
- [ ] A request-shape test for `deleteAccount` (DELETE with JSON body) using the project's existing HTTP test setup (find the `network/` test pattern first)
- [ ] `FakeUserManager`/fake repositories follow existing `testing/` helpers (inspect `app/src/test/.../testing` before writing)
- Framework install: none.

## Security Domain

`security_enforcement` is not set to false in `.planning/config.json` (the file contains only `workflow._auto_chain_active`), so it is enabled.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Change password and delete re-verify the current password server-side; client never stores either; clear fields after use |
| V3 Session Management | yes | Delete ends the session: `UserManager.logout()` full teardown; farewell outside the logged-in route |
| V4 Access Control | yes | `ensureExpert` endpoints; hide expert-only rows in client mode; server derives ids from JWT |
| V5 Input Validation | yes | Exported deep-link activity validates scheme/host/path and ignores params/extras; Account Link must be https with host (`isAllowedOnboardingUrl`); `meeting_url` http(s)-only (`isAllowedMeetingUrl`); DOB `YYYY-MM-DD`, TIN format left to server |
| V6 Cryptography | yes | Never hand-roll: TIN is encrypted server-side; client only transmits it over TLS and never persists/logs it |
| V8/V13 Data protection | yes | No logging of URL/token/TIN; no `SavedStateHandle` for secrets; `allowBackup=false` already set |

### Known Threat Patterns
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Another app fires `clickme://connect/return` to fake completion | Spoofing | Treat as a re-check trigger only; completion comes from `connect/status` |
| Malicious/foreign URL from `url` field opened in Custom Tab | Tampering | `isAllowedOnboardingUrl` (https + host) before launch; single-use, never stored |
| Double submit on irreversible delete | Tampering/Repudiation | In-flight flag disables the button; two-step token; reason enum fixed |
| Account Link / token / TIN leaked via logs or saved state | Information disclosure | No logging, no saved-state, clear after use, `ApiException` message carries status+code only (T-02-08) |
| Wrong-password brute force through delete-request | Elevation | Server `deleteLimiter`; client maps 429, no auto retry |

## Project Constraints (from CLAUDE.md)

- iOS Swift code is the source of truth for behavior; deviate only for Android platform conventions.
- Stack: Jetpack Compose + Material 3, MVVM with Compose State, Kotlin coroutines (no RxJava), Hilt, Retrofit + Moshi, EncryptedSharedPreferences, Firebase Messaging, Agora, Stripe PaymentSheet; minSdk 26 / targetSdk 37 / compileSdk 37.
- Platform swaps: `.sheet` -> `ModalBottomSheet`; back chevron -> `TopAppBar` with back arrow; `.alert` -> Material `AlertDialog`; iOS switch -> Material `Switch`; `PhotosPicker` -> `rememberLauncherForActivityResult(PickVisualMedia)`; `.toolbar` title -> `TopAppBar` title slot; `.task` -> `LaunchedEffect(Unit)`.
- File organization mirrors the iOS tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/...` with `XScreen.kt` + `XViewModel.kt`.
- Backend contract: same REST contract as iOS; specs in `backend-reference/docs/ios/`.
- Project memory: dark theme only, Nunito, Material styling OK; manual/device testing is deferred (log as open items, do not block); never `gradlew --stop` in parallel executors; worktree executors need `local.properties` and absolute iOS paths; JDK via Android Studio JBR.

## Findings by Area (planner quick reference)

1. **Deep links:** see Pitfalls 1-2. `MainActivity` is `singleTop`, already handles `onNewIntent` for push/call extras [VERIFIED: MainActivity.kt]; the new path should not route through `PushEvent`. The receiver posts to `ExpertLaunchRequest` (`dashboard` + new `connectReturn`). Cold start after process death: the Custom Tab outlives the process; the trampoline posts into a fresh process and `MainActivity` boots through Splash -> route restore; the slot is consumed by whichever ViewModel exists once Payouts/Dashboard load (Dashboard first load already consumes a pre-posted `dashboard` value, then loads).
2. **CallGuard:** add `ChangePassword` and `DeleteAccount` to `RestrictedAction` (current entries: `SwitchModes, LogOut, AddPaymentMethod, CancelBooking, EditAvailability, SetUpPayouts, RespondToReschedule` [VERIFIED: CallDialog.kt]); reuse `SetUpPayouts` for onboarding and the refresh path. The exhaustive `when` in `CallDialogs.kt` (lines 45-51) needs two new `call_action_*` strings; `CallDialogsTest` covers it. Update the stale KDoc line "Phase 14 still adds change password and delete account".
3. **Payouts data layer:** reuse `ExpertFinancialRepository`; add `payout_method` (nullable) to `PayoutSummaryData`, `transactions(page, limit)` and `ClickMeApi.getEarningsTransactions(@Query page, @Query limit)`; reuse `ClickMeApi.getExpertBookings(type, page, limit)` for the best-effort held count (page 1, limit 100 as iOS). `ConnectStatusData` already has only `charges_enabled` + `currently_due` which is all the backend exposes (STRIPE-INTEGRATION 3.2).
4. **Transactions UI:** one shared `EarningsTransactionRow` for the 5-row preview and the full list; paging trigger = last 3 rows visible (iOS) via `LazyColumn` + `snapshotFlow`; limit 20; `has_more` via `hasMoreOrDerived`; pull-to-refresh; empty state; per-page error with Retry that keeps loaded rows. Date label: `session_date ?: created_at`, `MMM d, yyyy`.
5. **Profile edit:** `PATCH user/profile` body groups `basic{first_name,last_name,bio}`, `location{city,state_province,country_code(alpha-3),timezone_id?}`, `professional{job_title,company}`, `languages{language_ids,custom_languages}`; `basic.email` is 403 so omit; bio max 500, names max 100, job/company max 200 [VERIFIED: openapi 5092 PATCH schema]. Use Moshi `@JsonClass(generateAdapter=true)` classes with null-omitting defaults (Moshi omits nulls by default) so absent groups stay untouched; send an explicit empty `languages` group only when the user removed them all. Country goes through `Countries.normalize`. After a successful save call `userManager.refreshProfile()` (it is session-generation guarded). Expert settings also needs a second `PATCH expert/profile` for `professional_details.meeting_url` only; the existing `updateExpertProfileTags` uses the same path with a tags-only body, so add a separate request type rather than extending that class.
6. **Change password:** `PATCH user/password {current_password,new_password}`; server rules >= 8 chars, one digit, differs from current; 403 = wrong current password (message only); 422 = `errors[]` with `field` in `current_password|new_password`; 429 generic. Success keeps the session valid [VERIFIED: ClickMeAPI+UserProfile.swift doc comment and AccountSecurityViewModel `handle(submitError:)`]. Wipe all three fields after success.
7. **Delete account:** state machine `Confirmation -> Feedback -> Deleted`; reasons `too_expensive|found_alternative|privacy_concerns|other` [VERIFIED: DeleteAccountReason.swift]; request `{deletion_token, reason_category, feedback_details?, confirm_permanent_action:true}`; success body `{status, message, data:{status:"account_deleted"}}` decodes into the existing `SuccessMessageResponse` (extra keys ignored). `deletion_token` field in `DeletionRequestData` is `deletion_token` (iOS `deletionToken`, snake-case encoder). Guard both "Delete Account" taps with `CallGuard.attempt(RestrictedAction.DeleteAccount)` (iOS guards both step buttons). Farewell host: Pitfall 3.
8. **Hubs and navigation:** replace the `footer` lambdas at `HomeClientScreen.kt:315-345` and `HomeExpertScreen.kt:215-249` (keep the debug affordances behind `showDebugAffordances`). New keys (all `@Serializable data object/class : NavKey`, registered in the shell `detailEntries` like `PaymentMethodsKey`): `ClientProfileSettingsKey`, `ExpertProfileSettingsKey`, `AccountSecurityKey`, `DeleteAccountKey`, `HelpKey`, `PayoutsKey(origin)`, `TransactionsKey`, `TaxProfileKey`. Use an origin field (as `AvailabilityOrigin`) wherever the same key can be pushed from two tab stacks (Payouts from Dashboard and from Profile). Logout keeps its existing `callGuard.attempt(LogOut)` in `HomeExpertViewModel.logout()`; iOS shows a "Log out of ClickMe?" confirmation first, add an `AlertDialog`. Add the iOS "Version x (build)" footer using `BuildConfig` (`versionName = "0.1.0"` [VERIFIED: app/build.gradle.kts:77]).
9. **Help/FAQ:** `GET help/faqs` -> `{categories[{id,title,icon_url,display_order,featured_articles[{id,category_id,question,snippet,view_count}]}], trending_articles}`; filter categories with empty `featured_articles`, sort by `display_order`, all collapsed by default [VERIFIED: HelpViewModel.swift `forceLoad`]; support row launches `supportEmailIntent()` in try/catch and shows a snackbar on `ActivityNotFoundException`.
10. **Tax profile:** see Code Examples. Country defaults to `CA`, kept as a plain field. Required to enable Save: legal name, dob, line1, city, region, postal code, country (iOS `canSave`). No backend answer yet on gating (A6); build ungated. Implement a date input that produces `YYYY-MM-DD` (a Material `DatePicker` or a masked text field; the iOS field is a free-text string).
11. **Push retarget:** in `PushDestinationResolver.resolve`, replace the `Payouts/KycOnboarding/AccountSecurity` branch (currently returning `notificationsList(context)` under the `// PHASE-14` marker [VERIFIED: PushDestinationResolver.kt line 104]): `Payouts` and `KycOnboarding` -> `shell(AppMode.Expert, <tab>, PayoutsKey(...))` subject to `allowSwitch(..., AppMode.Expert, context)` (falls back to the list for client-only accounts, same as `IncomingRequests`); `AccountSecurity` -> current mode's Profile root + `AccountSecurityKey`. Then change `landsOnNotificationsList` (PushDestination.kt:54-61) to `this is PushDestination.NotificationsList`, which also makes the notification-list rows tappable (`NotificationRows.toRowModel` uses `takeUnless { it.landsOnNotificationsList }`) and fixes its KDoc ("until Phase 14"). The Profile tab roots are `ClientProfileRoot` / `ExpertProfileRoot`; `ShellRoute` accepts a tab plus a key chain.

## Sources

### Primary (HIGH confidence)
- In-repo, read this session: `14-CONTEXT.md`, `14-BACKEND-REQUEST.md`, `STATE.md`, `AndroidManifest.xml`, `MainActivity.kt`, `ExpertLaunchRequest.kt`, `ExpertFinancialRepository.kt`, `DashboardFinancials.kt`, `ExpertDashboardViewModel.kt`, `ExpertDashboardScreen.kt`, `ExpertToolsGrid.kt`, `HomeExpertScreen.kt`, `HomeClientScreen.kt`, `PushDestination.kt`, `PushDestinationResolver.kt`, `CallGuard.kt`, `CallDialog.kt`, `TokenAuthenticator.kt`, `ApiException.kt`, `ErrorCode.kt`, `ApiRequestUrls.kt`, `ClickMeApi.kt`, `UserManager.kt`, `RequestEarnings.kt`, `Money.kt`, `PaginationMeta.kt`, `CustomTabLauncher.kt`, `AppLinks.kt`, `UserProfileData.kt`, `ExpertProfileData.kt`, `gradle/libs.versions.toml`.
- iOS source: `PayoutsView/ViewModel`, `TransactionsView/ViewModel`, `EarningsTransactionRow`, `ConnectOnboardingSession`, `ClickMeAPI+Payouts/Account/Support/UserProfile/TaxProfile`, `ExpertTaxProfileView/ViewModel`, `DeleteAccountView/ViewModel/Reason`, `AccountSecurityViewModel`, `ClientProfileSettingsViewModel`, `ExpertProfileSettingsViewModel`, `ExpertProfileHomeScreen/ViewModel`, `HelpViewModel`, `UpdateUserProfileRequest`, `UpdateExpertProfileRequest`.
- Backend: `backend-reference/src/app.js:223-224`, `src/controllers/expertTaxProfile.controller.js`, `src/schemas/expertTaxProfile.schema.js`, `openapi/clickme-api.yaml` (password 822, account 923, delete-request 1076, faqs 1522, user/profile 5092, expert/profile 4676, tax-profile 9428), `docs/ios/STRIPE-INTEGRATION.md` sec. 3 and 5-6, `docs/ios/EXPERT-EARNINGS-TRANSACTIONS.md`.

### Secondary (MEDIUM confidence)
- [Chromium bug: Custom Tab not triggering URL scheme intent](https://groups.google.com/a/chromium.org/g/chromium-bugs/c/LkjAfpkacXA) (2016, may be outdated)
- [AppAuth-Android issue 135](https://github.com/openid/AppAuth-Android/issues/135) (custom-scheme redirect after backgrounding)
- [AppAuth Android integration notes (RedirectUriReceiverActivity manifest)](https://gluu.org/docs/gluu-server/3.1.1/integration/native-apps/appauth-android/)
- [Stripe Account Links API](https://docs.stripe.com/api/account_links) (single-use links, return/refresh URL semantics)

### Tertiary (LOW confidence)
- Trampoline-activity launch-mode behavior and Chrome's current treatment of a server 302 to a custom scheme from Stripe's page: not verifiable here, tagged `[ASSUMED]` (A1, A2).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, no new dependencies; every reusable asset opened and read.
- Architecture: HIGH for data layer, state machines and navigation; MEDIUM for the deep-link return path (device-dependent).
- Pitfalls: HIGH for items traced to code (Pitfalls 3, 5, 6, 8, 9); MEDIUM for 1, 2, 4, 7.

**Research date:** 2026-10-28
**Valid until:** 2026-11-27 for contracts; re-check `14-BACKEND-REQUEST.md` answers and Chrome behavior at execution time.
