# Phase 2: Networking, Session & SDK Spikes - Context

**Gathered:** 2026-10-20
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase delivers:
- The networking core: snake_case and microsecond ISO-8601 decoding, `{data}` wrappers, `PaginationMeta`, the `ErrorCode` enum, `needAuthToken`, single-flight refresh and the `403 ACCOUNT_SUSPENDED` trap.
- Session storage: EncryptedSharedPreferences for tokens, DataStore for the profile, mode and onboarding flag.
- Auto-login on launch and logout.
- A temporary login screen against the production backend.
- Two throwaway SDK spikes: Stripe (a SetupIntent card save and a 3DS `PaymentLauncher` on a connected account) and Agora (audio in a `microphone` foreground service).
- A first signed `prodRelease` bundle uploaded to the internal track to register the package (REL-03).

Only the auth and `/me` API groups are ported. Real login UX, password recovery and realtime are Phase 3. Signup and onboarding are Phase 4.

Requirements: FND-03, FND-04, FND-05, FND-06, FND-07, FND-08, FND-13, FND-14, REL-03.

</domain>

<decisions>
## Implementation Decisions

### Launch, auto-login & session
- **D-01:** **Offline or server-error launch keeps the session (deliberate deviation from iOS).**
  - iOS's `tryAutoLogin()` logs out on *any* `/me` failure. Android keeps the session instead.
  - If stored tokens exist and `/me` fails with a network, timeout or 5xx error, route to home using the cached DataStore profile and `currentMode`, and retry `/me` later (for example, on next foreground or next request).
  - Only these clear the session: a 401 after a failed refresh, a missing refresh token, or `ACCOUNT_SUSPENDED`.
- **D-02:** **Splash hold cap is about 5 seconds.**
  - Auto-login is asynchronous, so `AppViewModel.resolveInitialRoute()` must stop holding `setKeepOnScreenCondition` after about 5s.
  - On timeout: with stored tokens and a cached profile, take the cached/offline path from D-01. With no session, go to Login.
- **D-03:** **Refresh failures match iOS `performRefreshCycle`.**
  - Log out only on a **401 from `/auth/refresh`**, or when there's no stored refresh token.
  - Network, 429 or 5xx errors during refresh fail soft: keep the tokens, pass the original error up to the caller, and the next request tries again.
  - This refines the roadmap's "a failed refresh logs the user out" to mean an auth-rejected refresh.
  - A refresh token is still never reused concurrently: single-flight with an OkHttp `Authenticator` and a Mutex, retry once.
- **D-04:** **Refresh is reactive only, like iOS.** It's triggered by a 401 through the `Authenticator`. No proactive refresh near `expiresAt` (store `expiresAt` anyway, as iOS does).
- **D-05:** **Port the iOS `setupIncomplete` detection now** (`SplashViewModel.swift`).
  - An auto-logged-in expert with unfinished profile setup routes to `AppRoute.SignupSetup`.
  - That route shows a placeholder until Phase 4 builds the signup flow.
- **D-06:** **No offline indicator in Phase 2.** Screens show their own load errors.

### Temporary login screen
- **D-07:** **The login screen is minimal and lives where the real one will.**
  - `screens/login/LoginScreen.kt` + `LoginViewModel.kt`: a plain Material 3 email/password form with a submit button and an inline error line.
  - It ships in release builds; the first uploaded bundle needs a login.
  - Phase 3 (AUTH-01) restyles it to match iOS `LoginView` and adds validation, forgot-password and the signup entry.
  - After login, route by mode the way iOS `LoginView` does: to client or expert home.
- **D-08:** **The `ACCOUNT_SUSPENDED` message matches iOS.**
  - A global trap (any 403 whose body code is `ACCOUNT_SUSPENDED`, checked before the 401-refresh path) clears the session and switches the route to Login.
  - A Material `AlertDialog` explains the account is suspended.
  - The suspension flag clears when the dialog is dismissed, as iOS `clearSuspensionFlag()` does.
  - It works both at launch and mid-session.
- **D-09:** **The logout button goes in the placeholder profile/settings tab of both shells.** The real settings screen replaces it in a later phase.
  - Logout clears local state only. iOS has no logout endpoint.
  - It clears the tokens, cached profile, `currentMode` and the onboarding flag (FND-08).
  - Push unregistration is a no-op hook until Phase 12.

### SDK spikes
- **D-10:** **Spikes are separate application modules in this repo: `:spikes:stripe` and `:spikes:agora`.**
  - Each has its own applicationId (for example, `com.uptrendinvestments.clickmeapp.spike.stripe`).
  - They share the version catalog and aren't part of the `:app` release.
  - They're throwaway, so delete them after Phases 7 and 9 absorb their findings.
  - This doesn't violate Phase 1 D-02 (single *app* module).
- **D-11:** **Spikes authenticate with a tiny email/password form** that calls `/auth/login` through its own bare Retrofit call.
  - They have no dependency on `:app` networking, so they can start in parallel with the networking core.
- **D-12:** **The Stripe spike uses the real backend in Stripe test mode with the dev publishable key (`pk_test`).**
  - SetupIntent from `POST /me/payment-methods/setup-intent`, card saved through PaymentSheet in SetupIntent mode.
  - 3DS: save the 3DS-required test card (`4000 0027 6000 3184`), then book the test expert, whose Connect account is already onboarded:
    1. `POST /bookings/payment-intent` with a fresh `booking_attempt_id`.
    2. `POST /bookings/confirm` with the saved `pm_…`, which returns `402 PAYMENT_REQUIRES_ACTION` with `detail.stripe_account`.
    3. Run `PaymentLauncher` / `handleNextAction` with `stripeAccountId = detail.stripe_account`. Don't persist that id.
    4. Retry `/bookings/confirm` with the same `booking_attempt_id` and `pm_…`.
  - The app must **never** confirm the PaymentIntent itself (server-side confirm, Path A).
  - Pick the booking time so the spike follows the short-lead path, not `LONG_LEAD_REQUIRES_SAVED_CARD`. A booking more than 6 days out is rejected.
- **D-13:** **The Agora spike gets its token from the real backend: `POST /bookings/:id/join`.**
  - Setup: book a session on the iOS app a few minutes ahead and accept it as the expert.
  - The spike's client joins with the booking id while the iOS expert joins the same call, which proves two-way audio.
  - Audio must keep playing from a `microphone`-type foreground service while the spike app is in the background.
  - Watch for `confirm-connection` and `end` semantics (CALL-END-SEMANTICS.md), but the spike only needs join plus background audio.
- **D-14:** **The test accounts already exist:** a client and an expert with completed Stripe Connect test onboarding, a topic and availability. The plan asks for the credentials at a `checkpoint:human-action` and doesn't create the accounts.

### Release signing & first upload
- **D-15:** **Signing is local only.**
  - The upload keystore stays on the user's Mac.
  - `keystore.properties` is gitignored and points at it. Gradle applies the `release` signingConfig only when that file exists, so CI still builds without it.
  - CI keeps doing unsigned debug builds. CI signing is a release-phase concern.
- **D-16:** **Play App Signing uses a Google-generated app signing key.** The user holds a separate upload key.
  - The plan includes a human step: back up the upload `.jks` and its passwords to the password manager.
- **D-17:** **Versions are set by hand** in `app/build.gradle.kts`: `versionCode = 1`, `versionName = "0.1.0"`, bumped by hand for each upload.
- **D-18:** **Upload the `prodRelease` bundle to the internal track** to register the package.
  - `pk_live` won't work until the backend goes live (Phase 1 D-05). That's acceptable because this upload is for registration.
  - The Play Console steps are the user's `checkpoint:human-action` tasks, as in Phase 1 D-13.
  - Org verification has already cleared (STATE.md), so the upload isn't blocked.

### Claude's Discretion
- Package layout for the networking and session code: for example, `network/` mirroring iOS `Network/`, `managers/UserManager`-style session repository, `model/enums/ErrorCode.kt`.
- Moshi codegen vs reflection. Pick what survives R8, since release `optimization` is enabled.
- How the `/me` retry after an offline launch is triggered (on foreground, on the next successful request, etc.).
- How the suspended event reaches `AppViewModel`: for example, a session-state `StateFlow` or `SharedFlow` from the session repository.
- Debug-only OkHttp logging level and header redaction for `Authorization`.
- The `ErrorCode` → user message mapping (port `Error+UserMessage.swift` for the codes used here).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Networking core (iOS source of truth)
- `ios-reference/ClickMe2026/Network/NetworkService.swift`: request pipeline, 401 refresh-then-retry-once, single-flight refresher actor, global `ACCOUNT_SUSPENDED` trap (checked before the refresh path), `{data}` decoding.
- `ios-reference/ClickMe2026/Network/APIRequestURLs.swift`: endpoint paths and `needAuthToken` (public paths list near line 527).
- `ios-reference/ClickMe2026/Network/ClickMeAPI.swift`, `ClickMeAPI+Auth.swift`, `ClickMeAPI+Me.swift`: the only API groups to port in this phase.
- `ios-reference/ClickMe2026/Network/Error+UserMessage.swift`: error to user message mapping.
- `ios-reference/ClickMe2026/Model/Enums/ErrorCode.swift`: the ~65 error codes. Unknown codes map to null.

### Session & launch
- `ios-reference/ClickMe2026/Managers/UserManager.swift`:
  - `login`, `tryAutoLogin` (Android deviates per D-01), `logout`, `performRefreshCycle` (D-03), `applyRefreshed`.
  - The suspension flag, `currentMode` and `hasSeenOnboarding`.
- `ios-reference/ClickMe2026/ClickMe2026App.swift`: root `AppRoute` switching, suspended → login, mode → home route.
- `ios-reference/ClickMe2026/Views/Screens/Splash/SplashViewModel.swift`: splash destination resolution, including `setupIncomplete` (D-05).
- `app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt`: `resolveInitialRoute()`, the Phase 2 hook (it must add the ~5s cap).

### Stripe spike
- `backend-reference/docs/ios/STRIPE-INTEGRATION.md`:
  - §1: server-side confirm, Path A. The app never confirms the PaymentIntent.
  - §2.1: `/bookings/payment-intent` and `booking_attempt_id`.
  - §2.4: the saved-card 3DS flow with `detail.stripe_account`.
  - §6: the payment error codes.
- `backend-reference/docs/ios/RESCHEDULE-PAYMENT-CONVERSION.md` §3: the long-lead error taxonomy, to avoid in the spike.

### Agora spike
- `ios-reference/ClickMe2026/Managers/AgoraManager.swift`, `ios-reference/ClickMe2026/Managers/CallCenter.swift`, `ios-reference/ClickMe2026/Network/ClickMeAPI+Call.swift`: join, confirm-connection and end calls.
- `backend-reference/docs/ios/CALL-END-SEMANTICS.md`: call lifecycle contract.

### Project-level
- `.planning/PROJECT.md`: API contract constraints (refresh-token reuse revocation, formats, money) and locked DEC-* decisions.
- `.planning/REQUIREMENTS.md`: FND-03..08, FND-13, FND-14, REL-03.
- `.planning/phases/01-foundation-app-shell-play-account/01-CONTEXT.md`:
  - D-03..D-06: flavors and keys, dev uses `pk_test`, shared applicationId.
  - D-13: the Play steps are done by the user.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AppRoute` (Splash/Login/SignupSetup/ClientHome/ExpertHome) and `AppViewModel` already exist. Phase 2 replaces the synchronous stub resolution with session-based auto-login.
- `config/AppConfig.kt` holds the base URL and keys, and `BuildConfig` has `STRIPE_PUBLISHABLE_KEY` and `AGORA_APP_ID` per flavor. Spikes can read the same values or copy them.
- `components/ValidatedField.kt` and `components/buttons/AsyncSaveButton.kt` can be reused for the minimal login form.
- `components/PlaceholderTabContent.kt` and the home tab subviews are where the logout button goes (D-09).

### Established Patterns
- Hilt is set up (`ClickMeApplication`, `@HiltViewModel`). Networking and session singletons go in Hilt modules.
- Single `:app` module (Phase 1 D-02). Spikes are separate *application* modules (D-10).
- ktlint and the hex-color-literal check run in CI and must pass in spike modules too, or the spikes are excluded from those checks on purpose.
- Release `optimization { enable = true }` is on. Moshi adapters and Retrofit interfaces must survive R8.

### Integration Points
- `MainActivity` uses `setKeepOnScreenCondition` with `AppViewModel.destinationResolved`.
- `settings.gradle.kts` will `include(":spikes:stripe", ":spikes:agora")`.
- `app/build.gradle.kts`: add the `signingConfigs.release` (conditional on `keystore.properties`), `versionCode` and `versionName`.

</code_context>

<specifics>
## Specific Ideas

- Use the finished iOS app as the "other side" for the Agora spike: the iOS expert joins the same booking.
- 3DS test card: `4000 0027 6000 3184`.
- The upload is `prodRelease`, signed locally, and lands on the internal track.

</specifics>

<deferred>
## Deferred Ideas

- A global offline banner or indicator. Polish phase (POL).
- CI-signed release bundles with the keystore in GitHub secrets. Release phase (Phase 17), if wanted.
- Proactive token refresh near `expiresAt`. Not planned; revisit only if 401 round trips become a problem.

</deferred>

---

*Phase: 02-networking-session-sdk-spikes*
*Context gathered: 2026-10-20*
