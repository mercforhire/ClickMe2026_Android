# Phase 2: Networking, Session & SDK Spikes - Research

**Researched:** 2026-10-20
**Domain:** Android networking core (OkHttp/Retrofit/Moshi), token session management, Stripe + Agora SDK spikes, Play signing and first upload
**Confidence:** HIGH for networking, session and signing (prototyped and run in a scratch copy of the repo). MEDIUM for the Stripe and Agora spikes (APIs read from source and compiled, but never run on a device). MEDIUM for the Play Console steps (partly cited, partly assumed).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Launch, auto-login & session
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

#### Temporary login screen
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

#### SDK spikes
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

#### Release signing & first upload
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

### Deferred Ideas (OUT OF SCOPE)
- A global offline banner or indicator. Polish phase (POL).
- CI-signed release bundles with the keystore in GitHub secrets. Release phase (Phase 17), if wanted.
- Proactive token refresh near `expiresAt`. Not planned; revisit only if 401 round trips become a problem.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FND-03 | Decode snake_case JSON, microsecond ISO-8601 dates, `{data}` wrappers, `PaginationMeta` | Moshi codegen with explicit `@Json(name=)` (verified by a passing scratch test), `OffsetDateTime.parse` adapter (verified for micro, milli, no fraction, offset and Z), generic `SuccessDataResponse<T>` (verified) |
| FND-04 | Parse the three error shapes into a ~65-code `ErrorCode`, unknown codes map to null | Raw-string envelope DTO plus a reflection-free `ErrorCode.fromWire()` lookup. The iOS enum has exactly 65 cases. |
| FND-05 | `needAuthToken`, single-flight refresh via `Authenticator` + Mutex, retry once, failed refresh logs out | Authenticator prototype run against MockWebServer: 12 concurrent 401s give exactly 1 refresh and 12 successes. D-03 refines the logout rule. |
| FND-06 | Any `403 ACCOUNT_SUSPENDED` logs out and shows a message | Application interceptor that peeks the 403 body. iOS has NO suspension UI, so the copy comes from USER-REPORTING.md §4. |
| FND-07 | Tokens in EncryptedSharedPreferences, profile/mode/onboarding in DataStore, auto-login on launch | security-crypto 1.1.0 and datastore-preferences 1.2.1 (both compile with the current toolchain). ESP does not run under Robolectric, so hide it behind an interface. |
| FND-08 | Logout clears tokens, cached profile, onboarding flag (and `currentMode`) | Clear order and the logout hook are in "Architecture Patterns". |
| FND-13 | Stripe spike: PaymentSheet SetupIntent save plus `PaymentLauncher` 3DS on a connected account | `com.stripe:paymentsheet:23.24.0`; `rememberPaymentLauncher(pk, stripeAccountId, cb)` signature confirmed from source. The Compose PaymentSheet entry point changed (see Pitfall 6). |
| FND-14 | Agora spike: audio in a `microphone` FGS, survives backgrounding | `io.agora.rtc:voice-sdk:4.6.4`, FGS type rules from the Android docs, Android 17 background-audio rule |
| REL-03 | First signed internal-track upload registers the package | Conditional signingConfig verified: a signed `bundleProdRelease` AAB built and passed `jarsigner -verify`. Play steps are in "Architecture Patterns". |
</phase_requirements>

## Summary

The networking, session and signing parts of this phase are low-risk, because I prototyped each one in a scratch copy of the repo under `/tmp` and the real repo is untouched. In that copy, Moshi codegen (KSP) builds with Kotlin 2.4.21, KSP 2.3.12 and AGP 9.4.1. A Moshi test passed for snake_case, the generic `{status,data}` envelope, the error envelope with `detail`, and microsecond dates. An `Authenticator` + `Mutex` test against MockWebServer produced exactly one refresh for 12 concurrent 401s. A signed `bundleProdRelease` built using a conditional signing config. The R8 build passes with Stripe, Agora, DataStore and security-crypto on the classpath.

Three iOS behaviors differ from what CONTEXT.md assumes. First, iOS has no suspension UI: `wasSuspended` is set in `UserManager` and nothing reads it. D-08's "matches iOS" has nothing to copy, so the dialog copy comes from `USER-REPORTING.md` §4 and iOS `HelpView` (`support@clickme.app`). Second, iOS's single-flight actor clears its in-flight task after completion, so a late 401 from an already-superseded token triggers a second refresh. The Android version must compare the request's `Authorization` token with the current token inside the mutex to hit "exactly one refresh". Third, iOS `tryAutoLogin()` logs out whenever `/me` returns a 401, even when the 401 came from a soft refresh failure. D-01 and D-03 say not to, so the Android auto-login must check whether the session still holds tokens after the failure.

The spikes carry the real uncertainty because they need a physical device and the iOS app as the peer. The test device is a Samsung SM-A075F on Android 16 (API 36), so it cannot exercise the Android 17 target-37 background-audio rule. The `Medium_Phone_API_37.0` AVD can partly cover that. Stripe's `rememberPaymentSheet` is deprecated in 23.24.0 in favor of `PaymentSheet.Builder(cb).build()` (composable). `rememberPaymentLauncher` takes `stripeAccountId` as a `remember` key, so the `acct_` value (known only after the 402) must be set in state and `handleNextActionForPaymentIntent` called from a `LaunchedEffect` after recomposition.

**Primary recommendation:** Build the networking core as plain-Kotlin classes (`TokenAuthenticator`, interceptors, `SecureTokenStore` interface) with the prototyped single-flight design. Use Moshi codegen with explicit `@Json(name=)`, a raw-string `ErrorCode` lookup, and `OffsetDateTime` for dates. Make the spikes independent modules that copy the pk/App ID as `buildConfigField`s. Gate the Stripe, Agora and Play work behind explicit `checkpoint:human-action` tasks.

## Project Constraints (from CLAUDE.md)

- Jetpack Compose + Material 3, MVVM with Compose State, Kotlin coroutines (not RxJava), Hilt for DI, Retrofit + Moshi, EncryptedSharedPreferences for JWT storage, Firebase Messaging for push, Agora Android SDK for voice, Stripe Android SDK with PaymentSheet. minSdk 26, targetSdk 37, compileSdk 37.
- iOS code in `ios-reference/` is the source of truth for behavior. Deviate only for Android conventions (Platform swaps: `.alert` becomes `AlertDialog`, `.task` becomes `LaunchedEffect(Unit)`, `.sheet` becomes `ModalBottomSheet`).
- Mirror the iOS folder tree, with the Swift view/viewmodel pair becoming `XScreen.kt` + `XViewModel.kt`, all under `app/src/main/java/com/uptrendinvestments/clickmeapp/`.
- Backend: same REST contract as iOS. Authoritative specs are in `backend-reference/docs/ios/`.
- Not a CLAUDE.md rule but enforced by CI (`scripts/check-design-rules.sh`, `ktlintCheck`): no `Color(0x…)` literals outside `ui/theme/Color.kt`, no `*Brand` types, no `sk_`/`rk_` Stripe secret patterns.
- PROJECT.md "Do not port": iOS debug logging (use OkHttp logging in debug builds only), `Debug/PreviewSecrets.swift`.
- Worktree executors: copy `local.properties` and read iOS/backend via absolute paths (`/Users/LeonChen/Documents/ClickMe2026/`, `/Users/LeonChen/Documents/ClickMe2026 Backend/`). Build with `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` (system `java` is JDK 18).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Auth header injection, `needAuthToken` | Data/network (OkHttp interceptor) | — | Must be uniform for every request, with no per-call code |
| 401 single-flight refresh + retry | Data/network (`Authenticator`) | Session store (token read/write) | OkHttp invokes it on the call thread, so it is the one place that sees all 401s |
| `403 ACCOUNT_SUSPENDED` trap | Data/network (application interceptor) | Session layer (clear + flag) | Must fire for any request, including `/auth/login` |
| Token persistence | Android Keystore-backed ESP | — | Locked decision. Must never be backed up. |
| Cached profile, mode, onboarding flag | DataStore (device storage) | — | Non-secret state (PROJECT.md "allowed additions") |
| Auto-login / logout orchestration | Session layer (`UserManager`-style singleton) | ViewModel (`AppViewModel`) | Mirrors iOS `UserManager`. `AppViewModel` only maps session state to `AppRoute`. |
| Splash hold and 5s cap | ViewModel (`AppViewModel`) | Activity (`setKeepOnScreenCondition`) | Already wired in Phase 1 |
| Suspension dialog | UI (root composable) | ViewModel (one-shot flag) | Must survive the route change to Login |
| Stripe card collection and 3DS UI | Stripe SDK UI (spike module) | Backend (intent creation, server-side confirm) | The app never confirms the PI (Path A) |
| Voice audio capture/playback | Foreground Service (spike module) | Agora native SDK | Mic access in the background needs a `microphone` FGS started while the app is visible |
| Package registration | Play Console (human) | Gradle signing config | Needs a signed AAB uploaded by the user |

## Standard Stack

### Core (`:app`, additions to the Phase 1 catalog)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.squareup.moshi:moshi-kotlin-codegen` (KSP) | 1.15.2 | Generated JSON adapters, no kotlin-reflect, R8-friendly | Latest on Maven Central. Built and tested against the project's Kotlin 2.4.21 / KSP 2.3.12 [VERIFIED: scratch build + test, 2026-10-20]. |
| `androidx.security:security-crypto` | 1.1.0 | `EncryptedSharedPreferences` for tokens | Locked decision. 1.1.0 is the last stable release; it is deprecated upstream and still works. [CITED: .planning/PROJECT.md Open items] |
| `androidx.datastore:datastore-preferences` | 1.2.1 | Cached profile JSON, `currentMode`, `hasSeenOnboarding` | Latest stable. `1.3.0-rc01` exists; don't use an RC. [VERIFIED: dl.google.com maven-metadata, 2026-10-20] |
| `com.squareup.okhttp3:okhttp` | 5.5.0 (already in catalog) | HTTP client, `Authenticator`, interceptors | Already present and current |
| `com.squareup.retrofit2:retrofit` + `converter-moshi` | 3.0.0 (already in catalog) | Typed API | Already present. 3.0.0 is the latest. |
| `com.squareup.okhttp3:logging-interceptor` | 5.5.0 (already `debugImplementation`) | Debug logging | Debug-only; see Pitfall 7 |

### Supporting (test)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `com.squareup.okhttp3:mockwebserver3` | 5.5.0 | Authenticator / interceptor / API tests | Add as `testImplementation`. Used in the passing scratch prototype. |
| JUnit 4.13.2, Robolectric 4.17, kotlinx-coroutines-test 1.11.0 | already in catalog | Unit and Compose shell tests | Existing infrastructure |

### Spike modules (`:spikes:stripe`, `:spikes:agora`)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.stripe:paymentsheet` | 23.24.0 (released 2026-10-19) | PaymentSheet and `PaymentLauncher` | `paymentsheet` alone pulls in `payments-core`, so both `PaymentSheet` and `PaymentLauncher` resolved and compiled in the scratch spike. [VERIFIED: Maven Central metadata + scratch build] |
| `io.agora.rtc:voice-sdk` | 4.6.4 | Audio-only RTC | Latest stable, on Maven Central. The audio-only artifact is smaller than `full-sdk`. iOS ships 4.6.2, same 4.6 family. [VERIFIED: Maven Central metadata; iOS `Package.resolved`] |
| Retrofit + Moshi (existing catalog) | — | Spike login and booking calls | Per D-11. Use `Map<String, Any?>` bodies and responses, since Moshi handles maps without codegen (verified in the scratch test). |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Moshi codegen | `moshi-kotlin` reflection | Needs kotlin-reflect (about 2.5 MB), is slower, and has more R8 risk. Codegen is the documented preference. |
| Per-DTO `@Json(name=)` | A custom snake_case factory | Moshi has no naming strategy. Explicit names are verbose but exact, and tests catch typos. |
| `OffsetDateTime.parse` | Port the iOS regex truncation | Java's ISO parser accepts 0-9 fractional digits natively, so no truncation is needed. |
| `Preferences` DataStore + a Moshi JSON string for the profile | Proto DataStore | Proto needs a schema plugin. A single JSON string per snapshot is enough for one cached object. |
| `kotlinx.serialization` | — | The plugin is already applied (for Navigation 3 keys), but CLAUDE.md locks Moshi for networking. |

**Installation (version catalog additions):**
```toml
# [versions]
datastore = "1.2.1"
securityCrypto = "1.1.0"
stripe = "23.24.0"
agora = "4.6.4"
# [libraries]
moshi-kotlin-codegen = { group = "com.squareup.moshi", name = "moshi-kotlin-codegen", version.ref = "moshi" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
okhttp-mockwebserver3 = { group = "com.squareup.okhttp3", name = "mockwebserver3", version.ref = "okhttp" }
stripe-paymentsheet = { group = "com.stripe", name = "paymentsheet", version.ref = "stripe" }
agora-voice-sdk = { group = "io.agora.rtc", name = "voice-sdk", version.ref = "agora" }
```
```kotlin
// app/build.gradle.kts additions
ksp(libs.moshi.kotlin.codegen)
implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.security.crypto)
testImplementation(libs.okhttp.mockwebserver3)
```
Do NOT add Stripe or Agora to `:app` in this phase (D-10). Adding both made the scratch AAB about 80 MB.

**Version verification (2026-10-20):** `stripe-android`/`paymentsheet` 23.24.0 (lastUpdated 2026-10-19), `voice-sdk` 4.6.4 (a later `12-15-ci-test` tag is not a release), `moshi-kotlin-codegen` 1.15.2, `okhttp` 5.5.0, `retrofit` 3.0.0, `security-crypto` 1.1.0, `datastore-preferences` 1.2.1 stable (1.3.0-rc01 exists). The Agora doc also lists a custom repo (`download.agora.io/maven`), but 4.6.4 resolved from Maven Central, which the project already uses (`FAIL_ON_PROJECT_REPOS` keeps working).

## Package Legitimacy Audit

> slopcheck could not be installed in this session (`pip` is not available on this machine), so the protocol's fallback applies: every package below is tagged `[ASSUMED]` and the planner should gate installs behind ONE consolidated `checkpoint:human-verify` task. The risk is low and the evidence below supports that. All coordinates come from the official Stripe, Agora, Android and Square documentation or from the existing Phase 1 catalog. All resolved from Maven Central or Google Maven, and all compiled in the scratch build.

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `com.stripe:paymentsheet` | Maven Central | years (Stripe first-party) | n/a | github.com/stripe/stripe-android | unavailable | [ASSUMED] Approved; the human-verify checkpoint confirms the coordinates |
| `io.agora.rtc:voice-sdk` | Maven Central | years (Agora first-party) | n/a | closed source, documented at docs.agora.io | unavailable | [ASSUMED] Approved; same checkpoint |
| `com.squareup.moshi:moshi-kotlin-codegen` | Maven Central | years (Square) | n/a | github.com/square/moshi | unavailable | [ASSUMED] Approved |
| `com.squareup.okhttp3:mockwebserver3` | Maven Central | years (Square) | n/a | github.com/square/okhttp | unavailable | [ASSUMED] Approved |
| `androidx.security:security-crypto` | Google Maven | years (Google, deprecated) | n/a | android.googlesource.com | unavailable | [ASSUMED] Approved (locked decision) |
| `androidx.datastore:datastore-preferences` | Google Maven | years (Google) | n/a | android.googlesource.com | unavailable | [ASSUMED] Approved |

**Packages removed due to slopcheck [SLOP] verdict:** none (slopcheck unavailable).
**Packages flagged as suspicious [SUS]:** none. `npm view` / `postinstall` checks don't apply (Maven ecosystem).

## Architecture Patterns

### System Architecture Diagram

```
App launch
   |
   v
MainActivity (splash held by setKeepOnScreenCondition) --> AppViewModel.resolveInitialRoute()
                                                              |
                          viewModelScope.async { session.restore() }   withTimeoutOrNull(5s) { await() }
                                                              |
        +--------------------- no tokens ------------------> AppRoute.Login
        |
        +-- tokens --> GET /me --+-- 200 --> expert? --> GET /expert/profile --> setup_completed == false --> AppRoute.SignupSetup
        |                        |                       \--> else landing mode (stored pref, else expert+topics?) --> Client/ExpertHome
        |                        +-- IO / timeout / 5xx / 5s cap --> tokens still present? --> cached DataStore mode --> Home (retry /me on next foreground)
        |                        \-- 401 after refresh --> tokens cleared by Authenticator --> AppRoute.Login
        v
Every API call:  Retrofit -> OkHttpClient
   app interceptors:  [AuthHeaderInterceptor: Bearer from in-memory token if needAuthToken(path)]
                      -> [SuspensionInterceptor: 403 body code == ACCOUNT_SUSPENDED? -> SessionEvents.suspended()]
                      -> [debug-only HttpLoggingInterceptor, Authorization redacted]
   network:           ... -> server
   on 401 -> TokenAuthenticator.authenticate():
        public path? -> null (no refresh)        priorResponse chain > 1? -> null (retry once)
        runBlocking { mutex.withLock {
            sentToken != currentToken ?  -> retry with currentToken          (someone already refreshed)
            no refresh token ?           -> clearSession(); null
            POST /auth/refresh (bare client, no authenticator)
               200 -> persist rotated pair (ESP) -> retry with new token
               401 -> clearSession() + emit LoggedOut -> null
               IO/429/5xx -> keep tokens -> null (original 401 propagates)
        } }

SessionEvents (StateFlow<SessionState>) --> UserManager (clears DataStore) --> AppViewModel --> AppRoute.Login + suspended dialog flag
```

### Recommended Project Structure

```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── network/                       # mirrors iOS Network/
│   ├── ApiRequestUrls.kt          # path constants + needAuthToken(path)
│   ├── ApiException.kt            # sealed: Http(status, code, message, fieldErrors, detail) | Network | Decoding
│   ├── ApiErrorParser.kt          # body -> ApiException; user-message mapping (Error+UserMessage.swift)
│   ├── MoshiProvider.kt           # Moshi.Builder().add(InstantAdapter)
│   ├── InstantAdapter.kt
│   ├── AuthHeaderInterceptor.kt
│   ├── SuspensionInterceptor.kt
│   ├── TokenAuthenticator.kt
│   ├── ClickMeApi.kt              # Retrofit interface: login, refresh, getMe, getExpertProfile, getMyTopics
│   └── DebugNetwork.kt            # in src/debug and src/release source sets (see Pitfall 7)
├── model/
│   ├── enums/ErrorCode.kt         # 65 cases + fromWire(); UserRole.kt; AppMode.kt
│   └── responses/                 # LoginData, RefreshTokenData, MeData, AuthUser, PaginationMeta,
│                                  # SuccessDataResponse<T>, ApiErrorEnvelope, ExpertProfileStub, MyTopicsStub
├── managers/
│   ├── UserManager.kt             # login, restore (tryAutoLogin), logout, setMode, refreshMe, suspension flag
│   ├── SecureTokenStore.kt        # interface + EncryptedPrefsTokenStore (ESP)
│   └── SessionStore.kt            # DataStore wrapper: cached user, currentMode, hasSeenOnboarding
├── di/NetworkModule.kt, SessionModule.kt
├── AppViewModel.kt (changed), ClickMeApp.kt (changed: Login + SignupSetup placeholders, suspended dialog)
└── screens/login/LoginScreen.kt, LoginViewModel.kt, screens/signup/SignupSetupPlaceholder.kt
spikes/stripe/  spikes/agora/      # separate application modules
```

### Pattern 1: Single-flight refresh `Authenticator` (prototyped and tested)

**What:** Serialize refreshes with a `Mutex`. Inside the lock, compare the token the failed request carried with the current stored token, and refresh only if they are the same.
**When to use:** The one 401 handler in the app.
**Why the comparison matters:** The iOS actor releases its in-flight task after completion. A request that was already in flight with the old token and 401s *after* the refresh would trigger a second refresh. That works (the rotated token is valid) but violates "exactly one refresh". The token comparison avoids it.

```kotlin
// Verified: 12 parallel 401s -> refreshCount == 1, all 12 responses 200 (MockWebServer, scratch repo, 2026-10-20)
class TokenAuthenticator(
    private val tokens: SecureTokenStore,
    private val refresher: TokenRefresher,          // uses its own bare OkHttpClient (no authenticator, no auth interceptor)
    private val onSessionRejected: () -> Unit,      // clear tokens + emit LoggedOut
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (!ApiRequestUrls.needAuthToken(response.request.url.encodedPath)) return null   // login 401 = bad credentials
        if (generateSequence(response) { it.priorResponse }.count() > 1) return null       // retry once
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
        return runBlocking {                                                               // OkHttp calls this on a blocking thread
            mutex.withLock {
                val current = tokens.accessToken
                if (current != null && current != sent) return@withLock response.retryWith(current)
                val refresh = tokens.refreshToken
                if (refresh == null) { onSessionRejected(); return@withLock null }         // D-03: missing refresh token -> logout
                when (val r = refresher.refresh(refresh)) {
                    is RefreshResult.Success -> { tokens.save(r.pair); response.retryWith(r.pair.accessToken) }
                    RefreshResult.Rejected -> { onSessionRejected(); null }                // HTTP 401 only
                    RefreshResult.Transient -> null                                        // IO / 429 / 5xx: keep tokens
                }
            }
        }
    }
    private fun Response.retryWith(token: String) =
        request.newBuilder().header("Authorization", "Bearer $token").build()
}
```
Notes for the plan:
- Persist the rotated pair before releasing the lock. Make the persist step independent of caller cancellation, because a cancelled caller would otherwise lose the new refresh token (see Pitfall 3).
- After `onSessionRejected()` clears the tokens, requests queued on the mutex see `current == null` and `refresh == null`, return null, and make no extra network calls.
- `/auth/refresh` is on the public-path list, so it never gets a bearer header and can't recurse.

### Pattern 2: `needAuthToken` (port, with a small fix)

iOS tests `url.hasSuffix(path)` on the full URL string, so a query string would make a public path look authed. Test the encoded path instead.

```kotlin
object ApiRequestUrls {
    // Source: ios-reference/ClickMe2026/Network/APIRequestURLs.swift (publicPaths). /auth/email/* are NOT public.
    private val publicPaths = setOf("/auth/login", "/auth/signup", "/auth/refresh", "/auth/password/forgot", "/auth/password/reset")
    fun needAuthToken(encodedPath: String): Boolean = publicPaths.none { encodedPath.endsWith(it) }
}
```

### Pattern 3: Suspension trap (application interceptor)

`403` never reaches the `Authenticator` (which only handles 401), so "checked before the refresh path" holds automatically. Peek the body without consuming it:

```kotlin
class SuspensionInterceptor(private val onSuspended: () -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 403) {
            val code = runCatching { ApiErrorParser.codeOf(response.peekBody(64 * 1024).string()) }.getOrNull()
            if (code == ErrorCode.AccountSuspended) onSuspended()   // idempotent: ignore if already suspended (iOS guard !wasSuspended)
        }
        return response   // the original error still propagates to the caller, as in iOS
    }
}
```
It must also fire for `POST /auth/login`, since the backend returns `403 ACCOUNT_SUSPENDED` there [CITED: backend-reference/docs/ios/USER-REPORTING.md §4].

### Pattern 4: Error envelope, `ErrorCode` and moshi models (verified in scratch)

- Parse error bodies leniently: one DTO with every field optional (`status`, `code`, `message`, `errors: List<FieldErrorDto>?`, `detail: Map<String, Any?>?`). This covered all three shapes, an unknown code, and unknown extra keys in the scratch test.
- Keep `code` as a raw `String?` in the DTO and resolve it with a reflection-free lookup. This makes "unknown codes map to null" trivial and is safe under R8:
```kotlin
enum class ErrorCode(val wire: String) {
    MissingToken("MISSING_TOKEN"), InvalidToken("INVALID_TOKEN"), /* ... port all 65 cases verbatim ... */ AccountSuspended("ACCOUNT_SUSPENDED"), InternalError("INTERNAL_ERROR");
    companion object { private val byWire = entries.associateBy { it.wire }; fun fromWire(raw: String?): ErrorCode? = raw?.let(byWire::get) }
}
```
- Do not use Moshi's `EnumJsonAdapter` (needs the `moshi-adapters` artifact and annotation reflection on enum constants, which is a bigger R8 risk).
- Same approach for `UserRole` (unknown role strings are ignored, not fatal).
- Use `String` for ids (`MeData.id`, etc.). Moshi has no built-in UUID adapter and Phase 2 never needs UUID math. When sending a UUID in a body, lowercase it (iOS lowercases; `java.util.UUID.toString()` is already lowercase).
- Generic wrappers work with codegen: `@JsonClass(generateAdapter = true) data class SuccessDataResponse<T>(val status: String, val data: T)` decoded as `Types.newParameterizedType(...)`, and Retrofit's converter passes the full generic type.
- `PaginationMeta(currentPage, totalPages, totalCount?, hasMore?)` with `hasMoreOrDerived` as a plain property.
- `MeData.pricingLimits` is optional, and iOS says never to cache it across launches (PRICING-LIMITS.md). In Phase 2, decode it as nullable and don't persist it.

### Pattern 5: Dates

```kotlin
class InstantAdapter {
    @FromJson fun fromJson(raw: String): Instant = OffsetDateTime.parse(raw).toInstant()   // verified on JDK 25
    @ToJson fun toJson(value: Instant): String = value.toString()
}
```
Verified inputs: `2026-10-20T16:17:09.983286+00:00`, `…983Z`, `…09Z`, `…9832Z`, and nine fractional digits with `-04:00`. These fail to parse, exactly as in iOS: `2026-10-20 16:17:09…` (space separator) and no-offset strings. `java.time` is native from API 26, so no desugaring is needed (minSdk 26). Phase 2's own DTOs carry no dates (`expires_at` is an epoch-seconds Int). Ship the adapter plus unit tests so FND-03 is demonstrably met and later phases reuse it.

### Pattern 6: Session layer (port of iOS `UserManager`)

- **`SecureTokenStore` interface** (access, refresh, `expiresAt`) with an in-memory `@Volatile` cache. `EncryptedPrefsTokenStore` implements it with `MasterKey.Builder(ctx).setKeyScheme(AES256_GCM)` and `EncryptedSharedPreferences.create(ctx, "clickme_secure", masterKey, AES256_SIV, AES256_GCM)`. Create it lazily, off the main thread. Wrap creation in `try/catch (GeneralSecurityException | IOException)`: on failure, call `context.deleteSharedPreferences(name)`, delete the Keystore alias, retry once, and otherwise treat the user as logged out. Phase 1 already excludes all app data from backup and device transfer, which is the main source of Keystore mismatches.
- **ESP cannot be unit-tested under Robolectric.** A scratch Robolectric test failed with `KeyStoreException ← NoSuchAlgorithmException` in `MasterKey.build()`. Test everything against a `FakeSecureTokenStore` and verify the real class on the Samsung device, via the manual checklist and the relaunch step in the login success criteria.
- **`SessionStore` (DataStore Preferences):** keys `current_mode` ("client"/"expert", like iOS), `has_seen_onboarding`, `cached_user_json`. `hasStoredModePreference` means the `current_mode` key is present. Logout does `edit { it.clear() }`, which clears all three at once.
- **Cached profile in Phase 2:** the `/user/profile` group is out of scope, so cache the login `AuthUser` (id, first/last name, roles, avatarUrl), merged with `/me` (email, roles) when available. See Open Question 1.
- **Logout (FND-08):** clear the tokens first and synchronously, then DataStore, then emit `LoggedOut`. Call a no-op `PushTokenRegistrar.unregister()` hook before clearing (D-09). Realtime disconnect is Phase 3.
- **Auto-login (`restore()`), per D-01/D-03/D-05:**
  1. No access or refresh token: `Login`.
  2. `GET /me` succeeds: if the user is an expert, `GET /expert/profile`; a failure there counts as "complete", as in iOS. `setup_completed == false` gives `SignupSetup`. Otherwise compute the landing mode: stored preference if present, else expert only when the user is an expert with at least one topic (`GET /expert/topics`, any failure means client). Persist it with `setMode`.
  3. `/me` fails with IO, timeout or 5xx: **if tokens are still stored**, take the cached path (`currentMode` from DataStore, default client) and mark `meStale = true`.
  4. `/me` fails with 401 or a non-suspension 403: if the session was cleared (Authenticator rejection, no refresh token), `Login`. If tokens remain (soft refresh failure), treat it as transient (cached path). iOS would log out here, but D-03 says tokens must survive soft failures.
  5. `ACCOUNT_SUSPENDED`: handled globally, so `Login` plus the dialog flag.
- **Retry `/me` after an offline launch:** run `userManager.refreshIfStale()` from a lifecycle `ON_START` observer (`repeatOnLifecycle(STARTED)` in `MainActivity`). It is cheap, deterministic and testable. A "retry on the next successful request" scheme needs hooks in every call and is not worth it.
- **Splash cap (D-02):** launch the restore as `val job = viewModelScope.async { ... }` and wait with `withTimeoutOrNull(5_000) { job.await() }`. A timeout cancels only the *waiter*, not the `async`, so an in-flight refresh and `/me` finish and update state later. Never wrap the restore body itself in `withTimeout`.
- **Suspension flow:** `UserManager` exposes `StateFlow<SessionState>` and `StateFlow<Boolean> wasSuspended`. `AppViewModel` collects them: when the session flips to logged-out while on a home or SignupSetup route, set the route to `Login` (this mirrors iOS's `onChange(of: isLoggedIn)`). `ClickMeApp` shows an `AlertDialog` while `wasSuspended` is true, and dismissing it calls `clearSuspensionFlag()`.
- **Dialog copy:** iOS has no suspension view (`grep -ri suspend` over `Views/` finds nothing; only `UserManager` sets the flag). Use the backend wording from USER-REPORTING.md §4: title "Your account has been suspended", plus a support contact. iOS `HelpView` uses `support@clickme.app`. Expose it as a `mailto:` via `AppLinks`.
- **Hilt cycle:** `OkHttpClient` needs the authenticator, which needs the refresher (bare client) and the token store, and `UserManager` needs the Retrofit API. Keep `UserManager` out of the OkHttp graph. The authenticator and interceptor take only `SecureTokenStore` and a small `SessionEvents` object. `UserManager` observes `SessionEvents` to clear DataStore. Use `Provider<>` or `Lazy<>` only if a cycle appears.

### Pattern 7: Release signing (verified: signed AAB built and `jarsigner -verify` passed)

```kotlin
// app/build.gradle.kts (top)
import java.util.Properties
val keystorePropsText = providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties")).asText
val keystoreProps = Properties().apply { keystorePropsText.orNull?.let { load(it.reader()) } }
// android { ... }
signingConfigs {
    if (!keystoreProps.isEmpty) {
        create("release") {
            storeFile = file(keystoreProps.getProperty("storeFile"))   // absolute path to the .jks outside the repo
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }
}
buildTypes { release { if (!keystoreProps.isEmpty) signingConfig = signingConfigs.getByName("release"); optimization { /* existing */ } } }
// defaultConfig: versionCode = 1, versionName = "0.1.0"
```
- Use `providers.fileContents` so the configuration cache (enabled in `gradle.properties`) tracks the file. A plain `File.readText()` would leave a stale cache entry when the file appears or changes.
- `.gitignore` already ignores `*.jks`, `*.keystore` and `keystore.properties`.
- Build the bundle: `./gradlew :app:bundleProdRelease` produces `app/build/outputs/bundle/prodRelease/app-prod-release.aab`. Verify with `jarsigner -verify <aab>`.
- Generate the upload key (human step): `keytool -genkeypair -v -keystore ~/keys/clickme-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`. Store the `.jks` and the passwords in the password manager (D-16).
- Play Console flow (user steps; partly [CITED], see Assumptions A1): create the app entry, go to Test and release > Testing > **Internal testing** (not "Internal app sharing", which doesn't register the app), create a release, enroll in Play App Signing with the Google-generated key, upload the AAB, then save or roll out. Per Google: an internal test can start before app setup is complete, and "once you upload an artifact, the package name for that app is fixed" [CITED: support.google.com/googleplay/android-developer/answer/9845334]. Success means the Play Console shows the package `com.uptrendinvestments.clickmeapp` after the upload.
- Also run the signed release as an R8 smoke test (see Pitfall 5) before uploading.

### Pattern 8: Stripe spike (`:spikes:stripe`)

Module setup that built successfully with AGP 9.4.1 in the scratch copy: plugins `android.application` + `kotlin.compose` only (AGP 9 has built-in Kotlin, so there is no `kotlin-android` plugin), `compileSdk { version = release(37) }`, minSdk 26, targetSdk 37, `buildFeatures { compose = true; buildConfig = true }`, dependencies `compose-bom`, `activity-compose`, `material3`, `okhttp`, `retrofit`, `converter-moshi`, `moshi`, `coroutines-android`, `com.stripe:paymentsheet`. Add `buildConfigField` copies of `STRIPE_PUBLISHABLE_KEY` (the dev `pk_test_…`) and `BASE_URL` from `app/build.gradle.kts`. A publishable key is allowed in the repo (D-04). The CI secret-key scan only matches `sk_`/`rk_`.

Flow (backend contract from STRIPE-INTEGRATION.md §2.1, §2.4, §4):
1. Login: `POST /auth/login {email,password}` returns `data.token`. Keep the token in memory only.
2. `POST /me/payment-methods/setup-intent {}` returns `data.client_secret`, `data.customer_id`, `data.ephemeral_key`.
3. `PaymentConfiguration.init(applicationContext, pk)`; present PaymentSheet in setup mode:
```kotlin
// Compose entry. rememberPaymentSheet is DEPRECATED in 23.24.0; its own ReplaceWith is this form.
val sheet = remember { PaymentSheet.Builder(::onSheetResult) }.build()          // @Composable build()
val cfg = PaymentSheet.Configuration.Builder("ClickMe")
    .customer(PaymentSheet.CustomerConfiguration(id = customerId, ephemeralKeySecret = ephemeralKey))
    .build()
sheet.presentWithSetupIntent(clientSecret, cfg)
// onSheetResult(PaymentSheetResult): Completed | Canceled | Failed(error)
```
4. `GET /me/payment-methods` returns `data.payment_methods[].id` (`pm_…`). PaymentSheet doesn't hand back the `pm_`, so the spike must list the methods and pick the card.
5. Book: `POST /bookings/payment-intent` with `{expert_id, topic_id, selected_date (UTC yyyy-MM-dd), time_slot (UTC HH:mm), duration_minutes, booking_attempt_id (fresh UUID)}`. The response is a **bare** body with no `{status,data}` envelope [CITED: STRIPE-INTEGRATION.md §2.1]. Keep `payment_intent_id`.
6. `POST /bookings/confirm {payment_intent_id, expert_id, topic_id, start_time (ISO-8601 with offset), meeting_type, stripe_payment_method_id}`. Expect `402 PAYMENT_REQUIRES_ACTION` with `detail.client_secret` and `detail.stripe_account`.
7. Run 3DS on the connected account:
```kotlin
var challenge by remember { mutableStateOf<Sca?>(null) }                          // Sca(clientSecret, stripeAccount)
val launcher = rememberPaymentLauncher(BuildConfig.STRIPE_PUBLISHABLE_KEY, challenge?.stripeAccount, ::onPaymentResult)
LaunchedEffect(challenge) { challenge?.let { launcher.handleNextActionForPaymentIntent(it.clientSecret) } }
// onPaymentResult(PaymentResult): Completed -> retry /bookings/confirm (same booking_attempt_id + pm_); Failed/Canceled -> show
```
   `rememberPaymentLauncher` re-creates the launcher when `stripeAccountId` changes (it is a `remember` key). Calling `handleNextAction…` in the same click handler that sets the state would still use the old, account-less launcher, which fails on a connected-account PI. Do not persist `stripe_account`.
8. Retry `/bookings/confirm` with the same attempt id and `pm_` and expect `201` with `status: pending_approval`. The PaymentIntent is never confirmed client-side (Path A).

Data the human must supply (D-14), via a `checkpoint:human-action`:
- Client and expert credentials, expert id, topic id.
- A **paid** topic, with a total of at least 50 cents, whose slot is available within 6 days.
- `selected_date`, `time_slot` (UTC) and `start_time` of that slot.
- Read them from the iOS app or from `GET /experts/:id/details`.
- The expert must be `charges_enabled`, otherwise `422 EXPERT_NOT_BOOKABLE`.

Test card `4000 0027 6000 3184` is "Always authenticate: requires authentication on all transactions, regardless of how the card is set up" [CITED: docs.stripe.com/testing]. So PaymentSheet may show a 3DS challenge once during the SetupIntent, and the server's off-session confirm will fail with `requires_action`. That is the 402 the spike needs. `4000 0025 0000 3155` would not do (after setup, off-session payments succeed without a challenge).

### Pattern 9: Agora spike (`:spikes:agora`)

Module setup built in the scratch copy: same plugin set as Stripe, plus `io.agora.rtc:voice-sdk:4.6.4`.

Manifest (mirrors the Android FGS docs [CITED: developer.android.com/develop/background-work/services/fgs/service-types]):
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<service android:name=".CallService" android:exported="false" android:foregroundServiceType="microphone" />
```
Agora also lists optional `MODIFY_AUDIO_SETTINGS`, `ACCESS_NETWORK_STATE`, and (Android 12+) `BLUETOOTH_CONNECT` and `READ_PHONE_STATE` runtime permissions in its quickstart [CITED: docs.agora.io Android voice quickstart]. For the spike, `RECORD_AUDIO` is the one that gates success.

Rules:
- Request `RECORD_AUDIO` first. Starting a `microphone` FGS without it throws `SecurityException` on Android 14+.
- Start the service **while the activity is in the foreground**: `ContextCompat.startForegroundService(...)`. `RECORD_AUDIO` is a while-in-use permission, and a microphone FGS can't be started from the background.
- Create the notification channel first (API 26+), then `ServiceCompat.startForeground(this, id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)` in `onStartCommand`. Denied `POST_NOTIFICATIONS` (API 33+) doesn't stop the FGS.
- Create the `RtcEngine` in the service (`RtcEngineConfig` with `mContext`, `mAppId`, `mEventHandler`; `ChannelMediaOptions` with `CHANNEL_PROFILE_COMMUNICATION`, `CLIENT_ROLE_BROADCASTER`, `publishMicrophoneTrack = true`, `autoSubscribeAudio = true`; all of these compiled in the scratch spike). `joinChannel(rtcToken, channelName, uid, options)` takes values from `POST /bookings/:id/join` -> `data.agora_config {rtc_token, uid (Int), channel_name}` (iOS `JoinCallResponse`).
- Port the iOS engine defaults (speech-standard audio profile, speakerphone on) and check the exact Android method names against the 4.6.4 Javadoc before relying on them (Assumption A8).
- Stop path: `leaveChannel()`, `RtcEngine.destroy()`, `stopForeground`, `stopSelf`.

Pass criteria for FND-14 (device evidence):
- Two-way audio with the iOS expert.
- Press Home, lock the screen for at least 5 minutes, and keep hearing the peer and being heard.
- `adb shell dumpsys activity services <pkg>` shows the FGS with `foregroundServiceType=0x80` (microphone).
- The Samsung battery setting stays at "Optimized", and a repeat run with "Restricted" is noted.

The attached test device is Android 16, so the Android 17 (target 37) background-audio rule ("a background app that interacts with audio must have a foreground service with while-in-use capability") can't be exercised there. A microphone FGS started from the foreground has while-in-use capability, so it should comply [CITED: developer.android.com/about/versions/17/behavior-changes-17]. Repeat once on the `Medium_Phone_API_37.0` AVD as a secondary check.

Join eligibility: the booking must be `confirmed` (accepted by the expert) and inside the join window. The size of the pre-start window isn't in the docs I have; the iOS Join button becoming active is the reference. A `503 CALL_UNAVAILABLE` means Agora isn't configured server-side. Use a **free** topic for this booking (`POST /bookings/request`, BOOK-03), so the Agora spike doesn't depend on the Stripe flow.

### Pattern 10: Hilt + OkHttp wiring

```kotlin
@Provides @Singleton @Named("bare") fun bareClient(): OkHttpClient = OkHttpClient.Builder().connectTimeout(15, SECONDS).readTimeout(60, SECONDS).build()
@Provides @Singleton fun apiClient(auth: AuthHeaderInterceptor, susp: SuspensionInterceptor, authenticator: TokenAuthenticator, debug: DebugNetwork): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(15, SECONDS).readTimeout(60, SECONDS).writeTimeout(60, SECONDS)   // iOS timeoutIntervalForRequest = 60
        .addInterceptor(auth).addInterceptor(susp)
        .apply { debug.interceptors().forEach(::addInterceptor) }                           // logging last so it sees final headers
        .authenticator(authenticator).build()
```
The refresher uses the bare client against the same base URL. Retrofit: `Retrofit.Builder().baseUrl(AppConfig.baseUrl + "/")` (the base URL has no trailing slash and Retrofit requires one), `.addConverterFactory(MoshiConverterFactory.create(moshi))`.

### Anti-Patterns to Avoid
- **Proactive or timer-based refresh.** D-04 says reactive only.
- **Putting the refresh call on the main OkHttp client.** The refresh would recurse into its own authenticator. Use a bare client.
- **Wrapping the restore in `withTimeout`.** It cancels the in-flight refresh and risks losing a rotated refresh token. Use `async` + `withTimeoutOrNull(await)`.
- **Reading ESP on the main thread or per request.** Cache the tokens in memory.
- **`HttpLoggingInterceptor.Level.BODY`.** It prints passwords and tokens (see Pitfall 7).
- **Confirming the booking PaymentIntent on the device.** Path A is server-side only.
- **Persisting `detail.stripe_account`.** Use it once and discard it.
- **Importing `:app` classes into the spikes.** The spikes must stay deletable and parallelizable.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Card collection and 3DS UI | Custom card form, WebView 3DS | Stripe `PaymentSheet` + `PaymentLauncher` | PCI scope, SCA, browser/native challenge handling |
| Audio capture, transport and echo cancellation | `AudioRecord` + WebRTC glue | Agora `voice-sdk` | Locked decision, and the iOS peer uses the same service |
| Token encryption | Own Keystore/AES code | `EncryptedSharedPreferences` (locked) | Key management mistakes. Keep it, deprecated or not. |
| Refresh serialization | Ad-hoc flags/`@Synchronized` scattered across callers | One `Authenticator` + `Mutex` | Refresh-token reuse revokes the whole session. One chokepoint. |
| ISO-8601 parsing | Regex truncation like the iOS code | `OffsetDateTime.parse` | Handles 0-9 fractional digits and offsets natively |
| JSON mapping | Manual `JSONObject` walking | Moshi codegen adapters | Type safety, R8-safe |
| Test HTTP server | Hand-written fakes of OkHttp | `mockwebserver3` | Needed for real concurrency/retry behavior (used in the prototype) |
| Preferences persistence | Raw `SharedPreferences` for non-secrets | DataStore | Locked by PROJECT.md "allowed additions" |
| Release signing plumbing | Env-var scripts | Gradle `signingConfigs` + `providers.fileContents` | Verified with config cache on |

**Key insight:** The risky parts of this phase (refresh-token reuse, backgrounded microphone, 3DS on a connected account) are all places where a hand-rolled variant looks fine in the happy path. Use the platform and SDK primitives, and prove each one with a test or a device run.

## Common Pitfalls

### Pitfall 1: Late 401s cause a second refresh
**What goes wrong:** A request sent with the old token returns 401 after the first refresh completed, and a naive authenticator refreshes again.
**Why it happens:** The lock only protects the refresh itself; it doesn't know the token was already replaced. The iOS actor has the same gap.
**How to avoid:** Compare the request's bearer with the stored token inside the lock (Pattern 1) and have a MockWebServer test cover it.
**Warning signs:** More than one `/auth/refresh` call in the test's request log.

### Pitfall 2: Soft refresh failure turns into a logout at launch
**What goes wrong:** iOS's `tryAutoLogin` logs out when `/me` finally returns 401, even after a network failure during refresh. A direct port logs the user out while offline.
**How to avoid:** After a `/me` failure, decide by session state: if the Authenticator cleared the tokens, go to Login; if tokens remain, go to the cached path (D-01).
**Warning signs:** A user in airplane mode with an expired access token lands on Login.

### Pitfall 3: Cancelled caller loses the rotated refresh token
**What goes wrong:** The server rotates the refresh token, the response is read, but the caller's coroutine is cancelled (the splash cap, a screen leaving) before the new pair is saved. The old token is now invalid, and the next refresh trips reuse detection and revokes the session.
**How to avoid:** Persist the rotated pair inside the authenticator's critical section on the OkHttp thread, not through a cancellable suspend call. The splash cap must not cancel the restore work (Pattern 6). Accepted residual risk: if the connection drops *after* the server rotated but *before* the response arrives, that case is unrecoverable client-side, as on iOS.

### Pitfall 4: Moshi misses on the wire names or Kotlin nullability
**What goes wrong:** A property without `@Json(name = "snake_case")` silently stays null (if it has a default) or throws `JsonDataException` (if non-null).
**How to avoid:** Name every multi-word field explicitly. Make iOS-optional fields nullable with defaults. A DTO unit test per response type (decode a literal JSON sample) is cheap and catches typos.

### Pitfall 5: R8 breaks release only
**What goes wrong:** Debug works, `prodRelease` fails at runtime (adapter not found, Retrofit generic type erased). Release `optimization { enable = true }` is on.
**Evidence:** In the scratch build `minifyProdReleaseWithR8` succeeded with the new dependencies, but that proves little because the sample DTO was unreferenced and was stripped.
**How to avoid:** Install the **signed `prodRelease` APK** on the Samsung device, then log in, relaunch and log out. This is a required plan task. Moshi and Retrofit ship consumer keep rules. If something breaks, add narrow rules to `app/src/main/keepRules/rules.keep`; don't add blanket `-keep class **`.

### Pitfall 6: Stripe API drift
**What goes wrong:** Snippets from older tutorials use APIs that are deprecated or gone. `rememberPaymentSheet(cb)` and `PaymentLauncher.rememberLauncher/createForCompose` are deprecated in 23.24.0 (a compile warning, not an error).
**How to avoid:** Use `PaymentSheet.Builder(cb).build()` (composable) and `rememberPaymentLauncher(...)`. Confirm the `build()` signature against the 23.24.0 source (I read it from the repository's master branch, not from a device build).

### Pitfall 7: Logging leaks credentials
**What goes wrong:** `/auth/login` request bodies hold the password and responses hold both tokens. `Level.BODY` prints them to logcat. Also, `logging-interceptor` is `debugImplementation`, so main-source code can't reference it and release would fail to compile.
**How to avoid:** Put the interceptor in `src/debug/java/.../network/DebugNetwork.kt` and an empty stub of the same class in `src/release/...`. Use `Level.BASIC` (or HEADERS with `redactHeader("Authorization")`).

### Pitfall 8: Foreground service start rules
**What goes wrong:** `ForegroundServiceStartNotAllowedException`, `SecurityException` (no `RECORD_AUDIO`) or "Bad notification for startForeground" (no channel).
**How to avoid:** Follow the order in Pattern 9: permission, then activity visible, then channel, then `startForegroundService`, then `startForeground` with the type.

### Pitfall 9: ESP corruption or Robolectric
**What goes wrong:** After an OS update or a restore, ESP init throws (`AEADBadTagException`, `InvalidProtocolBufferException`), and an unguarded init crashes at launch. And any Robolectric test that touches the real ESP fails.
**How to avoid:** Guarded init with delete-and-retry, and the interface-plus-fake pattern (Pattern 6).

### Pitfall 10: CI and module side effects
**What goes wrong:** Spike modules appear in CI. `lintDevDebug` and `testDevDebugUnitTest` are matched by name in modules that have them, so spikes (which have no flavors) are skipped. `ktlintCheck` runs in every module that applies the plugin.
**How to avoid:** Apply the ktlint plugin to the spikes (cheap, keeps style), or exclude them on purpose and say so. Add `:spikes:stripe:assembleDebug :spikes:agora:assembleDebug` to the CI command so the spikes can't rot unnoticed. `scripts/check-design-rules.sh` scans `app/src` only (the `sk_` pattern scan covers `app gradle *.kts`), so add `spikes` to the secret scan.

### Pitfall 11: Deprecation warnings that look like errors
`warning: Kapt support in Moshi Kotlin Code Gen is deprecated` appears during `hiltJavaCompile` even though codegen runs through `ksp(...)`. It is benign, and I saw it on both debug and release in the scratch build.

## Runtime State Inventory

Not a rename/refactor/migration phase: omitted.

## Code Examples

### Moshi DTOs and tests that passed (scratch repo)
```kotlin
@JsonClass(generateAdapter = true)
data class ApiErrorEnvelope(
    val status: String? = null, val code: String? = null, val message: String? = null,
    val errors: List<FieldErrorDto>? = null, val detail: Map<String, Any?>? = null,
)
@JsonClass(generateAdapter = true)
data class RefreshTokenData(
    val token: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_at") val expiresAt: Long,
    @Json(name = "expires_in") val expiresIn: Long,
)
// Verified: SuccessDataResponse<RefreshTokenData> decodes via Types.newParameterizedType; unknown keys are ignored;
// detail["stripe_account"] reads back; "2026-10-20T16:17:09.983286+00:00" -> Instant with nano == 983286000.
```

### Phase 2 endpoint list (the only groups to port)
| Method | Path | Auth | Response |
|--------|------|------|----------|
| POST | `/auth/login` `{email,password}` | public | `{status,data:{token,user:AuthUser,refresh_token,expires_at,expires_in}}` |
| POST | `/auth/refresh` `{refresh_token}` | public | `{status,data:{token,refresh_token,expires_at,expires_in}}` (rotating) |
| GET | `/me` | bearer | `{status,data:{id,email,roles[],pricing_limits?}}` |
| GET | `/expert/profile` | bearer | only `data.setup_completed` is needed (D-05) |
| GET | `/expert/topics` | bearer | only "is `data.topics` non-empty" is needed (landing mode) |

[CITED: ios-reference `ClickMeAPI+Auth.swift`, `ClickMeAPI+Me.swift`, `ClickMeAPI+ExpertProfile.swift`, `APIRequestURLs.swift`.]

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `rememberPaymentSheet(cb)` | `remember { PaymentSheet.Builder(cb) }.build()` | deprecated in the 23.x line | Use the Builder form in the spike |
| `PaymentLauncher.createForCompose` / `rememberLauncher` | `rememberPaymentLauncher(pk, stripeAccountId, cb)` | deprecated in 23.x | Account id is a `remember` key |
| `androidx.security:crypto` as the recommended store | Deprecated; still works at 1.1.0 | 2025 | Locked decision stays (STATE.md). Isolate behind `SecureTokenStore` so a later swap is a one-class change. |
| Kotlin reflection Moshi | KSP codegen | long-standing | Works on Kotlin 2.4.21 / KSP 2.3.12 here |
| Android 16 and below FGS rules | Android 17 target-37 background-audio rule (FGS with while-in-use capability) | API 37 | Microphone FGS from the foreground complies |
| Certificate Transparency opt-in | CT on by default for target-37 apps | API 37 | Low risk for the Railway host. Watch for unexpected TLS failures on the first device run. [CITED: developer.android.com/about/versions/17/behavior-changes-17] |

**Deprecated/outdated:**
- `rememberPaymentSheet` (Stripe 23.24.0): deprecated, still works. Avoid it in new code.
- `EncryptedSharedPreferences`: deprecated upstream, retained deliberately.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The Play Console steps (create app entry, Internal testing > create release, Play App Signing choice on first release, then upload) are as described. Only "internal test can start before setup is complete" and "package name is fixed on first upload" are cited. | Pattern 7 | The user's checklist needs adjusting. They will see the actual UI. |
| A2 | Package legitimacy: all Maven coordinates are real and first-party. slopcheck couldn't run. | Package Legitimacy Audit | Low. Coordinates came from official docs and resolved and compiled. One consolidated human-verify checkpoint covers it. |
| A3 | A `/me` 401 with tokens still present means a soft refresh failure, so the cached path applies. The rarer case (refresh succeeded but the retry also 401s) would loop on the cached path. | Pattern 6 | A user with a server-rejected-but-not-refresh-rejected token stays "logged in" with failing screens. Mitigation: a second consecutive 401 after a *successful* refresh also clears the session (needs the user's confirmation, Open Question 3). |
| A4 | "Cached profile" for Phase 2 means the login `AuthUser` merged with `/me`, not iOS `UserProfileData` (`/user/profile` is out of scope). | Pattern 6 | If the user expects the full profile cached now, a `/user/profile` call must be added. |
| A5 | Suspension dialog copy and the support contact come from USER-REPORTING.md and iOS `HelpView` (`support@clickme.app`), since iOS has no suspension UI. | Pattern 6 | Wording differs from what the user pictured in D-08. Cheap to change. |
| A6 | Agora Android audio-config method names (for example, speakerphone default route, audio profile) mirror the iOS manager. Not verified against the 4.6.4 Javadoc. | Pattern 9 | Compile error in the spike. Fix by reading the Javadoc. |
| A7 | Samsung (One UI) battery management won't kill a `microphone` FGS with a notification during the 5-minute background test. | Pattern 9 | Spike fails on Samsung only. Record it as a finding for Phase 9. |
| A8 | `PaymentSheet.Builder(cb).build()` composable behaves as in the Stripe source (read from `master`, compiled only for `rememberPaymentLauncher`/`presentWithSetupIntent`/`CustomerConfiguration`). | Pattern 8 | Minor API adjustment in the spike. |
| A9 | The join window size before `start_time` isn't documented in the files I read. | Pattern 9 | The spike's booking must be timed using the iOS Join button as the signal. |
| A10 | The 3DS challenge UI for test card 3184 appears inside PaymentSheet during SetupIntent as well as at the 402 step. | Pattern 8 | Extra tap in the manual run. No impact on correctness. |

## Open Questions

1. **What does the Phase 2 "cached profile" contain?**
   - Known: D-01 routes offline launches using a cached DataStore profile and `currentMode`. iOS caches `UserProfileData` from `/user/profile`, which is a different API group (not in Phase 2 scope).
   - Unclear: whether `AuthUser` + `/me` fields are enough.
   - Recommendation: cache a small `CachedUser` (id, first/last name, roles, avatarUrl, email) now. Extend it when the `/user/profile` group is ported.
2. **Who supplies the spike inputs, and when?**
   - Known: D-14 says the accounts exist and the plan asks at a checkpoint.
   - Unclear: the exact list of values.
   - Recommendation: one checkpoint listing client email/password, expert email/password (for the iOS peer), `expert_id`, a paid `topic_id` plus `selected_date`/`time_slot`/`start_time`/`duration_minutes`, and a free-topic booking id for Agora.
3. **Should a 401 after a *successful* refresh also clear the session?**
   - Known: D-01 lists the only clearing conditions, and a retry-401 isn't among them. iOS does clear in that case.
   - Recommendation: treat a second consecutive 401 (after a successful refresh) as auth rejection and clear. Confirm with the user before planning it.
4. **Should CI build the spike modules?**
   - Recommendation: yes, `assembleDebug` only, so API drift is caught while the spikes are still referenced.
5. **Play Console internal-testing prerequisites for this account.**
   - Known: the org account is verified and an internal test may start before setup completes.
   - Unclear: whether the Dashboard blocks rollout on missing declarations. The upload itself registers the package either way.
   - Recommendation: the human checklist should save the release as a draft if rollout is blocked, and note the blocker.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 (Android Studio JBR) | Gradle 9.6 / AGP 9.4.1 | ✓ | 25.0.3 at `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | — (system `java` is 18, so set `JAVA_HOME`) |
| Gradle wrapper | all builds | ✓ | 9.6.0 | — |
| Android SDK | builds | ✓ | `android-37.0` and `android-34` platforms, build-tools 34.0.0 and 36.0.0 | — |
| `adb` | spike device runs, `dumpsys` | ✓ | 1.0.41 at `~/Library/Android/sdk/platform-tools` | — |
| Physical test device | FND-13, FND-14, R8 smoke | ✓ | Samsung SM-A075F, Android 16 (API 36), wireless adb. May appear twice (USB + wireless), so use `-s`. | — |
| Emulator AVD | secondary Android 17 background-audio check | ✓ | `Medium_Phone_API_37.0` (from the memory notes; not re-checked this session) | — |
| `keytool`, `jarsigner` | upload key, bundle verification | ✓ | JBR JDK 25 | — |
| `pip` / slopcheck | package legitimacy gate | ✗ | — | All packages `[ASSUMED]` with one human-verify checkpoint |
| `ctx7` CLI | docs lookup | ✗ | — | Used WebFetch and primary source repos instead |
| Production backend | login, spikes | ✓ (assumed up; not probed) | Railway | — |
| iOS app + a second device or simulator | Agora two-way audio (D-13) | user-provided | — | Blocks FND-14 sign-off if unavailable |
| Test accounts, Connect-onboarded expert | spikes (D-14) | user-provided | — | Blocks FND-13 sign-off |
| Play Console org account (verified) | REL-03 | ✓ per STATE.md | — | If it regresses, log the blocker in STATE.md per the success criterion |

**Missing dependencies with no fallback:** the iOS peer and the test accounts are the user's, so they are `checkpoint:human-action` items (they block FND-13 and FND-14 sign-off, not the code).
**Missing dependencies with fallback:** slopcheck (consolidated human verification), ctx7 (primary sources).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 (existing) + `mockwebserver3` 5.5.0 (new) |
| Config file | none beyond `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` is already set in `app/build.gradle.kts` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` (about 20-35 s warm in the scratch copy) |
| Full suite command | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug` (the CI command; append the two spike `assembleDebug` tasks) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FND-03 | snake_case, `{data}` wrapper, `PaginationMeta`, unknown keys ignored | unit | `…testDevDebugUnitTest --tests '*ResponseDecodingTest'` | ❌ Wave 0 |
| FND-03 | Instant decode: micro, milli, no fraction, Z and offset; space/no-offset rejected | unit | `--tests '*InstantAdapterTest'` | ❌ Wave 0 |
| FND-04 | three error shapes, unknown code → null, non-JSON body, `ErrorCode` has 65 unique wire values | unit | `--tests '*ApiErrorParserTest'` | ❌ Wave 0 |
| FND-05 | `needAuthToken` table (public vs authed, query strings) | unit | `--tests '*ApiRequestUrlsTest'` | ❌ Wave 0 |
| FND-05 | N concurrent 401s → exactly 1 refresh, all succeed; late 401 → no second refresh; public-path 401 → no refresh; retry once only | MockWebServer | `--tests '*TokenAuthenticatorTest'` | ❌ Wave 0 |
| FND-05 | refresh 401 → tokens cleared and LoggedOut; refresh 5xx/IO/429 → tokens kept and original 401 propagates; missing refresh token → logout | MockWebServer + fake store | `--tests '*TokenAuthenticatorTest'` | ❌ Wave 0 |
| FND-06 | 403 with `ACCOUNT_SUSPENDED` fires the handler once, including on `/auth/login`; other 403 doesn't | MockWebServer | `--tests '*SuspensionInterceptorTest'` | ❌ Wave 0 |
| FND-07 | login persists tokens + cached user; restore() with /me 200, offline, 5xx, soft-401, hard-401, no tokens, expert incomplete, landing-mode rules | unit (fake store, temp-file DataStore) | `--tests '*UserManagerTest'` | ❌ Wave 0 |
| FND-07 | `AppViewModel`: route resolution, 5s cap leaves the restore running, SignupSetup routing, suspended → Login + dialog flag, dismiss clears flag | unit (virtual time) | `--tests '*AppViewModelTest'` | ✅ exists, must be rewritten (currently asserts the synchronous ClientHome stub) |
| FND-07 | Real `EncryptedPrefsTokenStore` round-trip and relaunch auto-login | manual on device | see Manual-Only | n/a |
| FND-08 | logout clears tokens, cached user, `currentMode`, onboarding flag; logout button present in both profile tabs | unit + Robolectric Compose | `--tests '*UserManagerTest' --tests '*HomeShellsTest'` | ❌ new / ✅ `HomeShellsTest` exists, extend |
| FND-13 | SetupIntent card save and 3DS on the connected account | manual on device | see Manual-Only | n/a |
| FND-14 | Agora audio in a `microphone` FGS while backgrounded | manual on device | see Manual-Only | n/a |
| REL-03 | signed `prodRelease` AAB builds; package appears in Play Console | command + manual | `./gradlew :app:bundleProdRelease && jarsigner -verify app/build/outputs/bundle/prodRelease/app-prod-release.aab` | n/a |

**Manual-Only (with justification):** FND-13, FND-14, the ESP round-trip and the R8 release smoke test all need a real device, real SDK native code or Keystore, and the iOS peer. Each gets a written checklist with evidence (screenshots or `adb logcat`/`dumpsys` excerpts) in the plan's summary. Checklist for the release smoke test: install the signed `assembleProdRelease` APK, log in, kill and relaunch (still logged in), force an expired access token with a debug-only "invalidate access token" action in the Profile footer (`BuildConfig.DEBUG`, same pattern as the existing debug buttons) and confirm a refresh, log out.

### Sampling Rate
- **Per task commit:** the quick command for the touched test class
- **Per wave merge:** `./gradlew testDevDebugUnitTest ktlintCheck`
- **Phase gate:** the full CI command green, plus the manual checklists signed off, before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `libs.versions.toml` + `app/build.gradle.kts`: add `mockwebserver3`, `datastore-preferences`, `security-crypto`, `moshi-kotlin-codegen` (`ksp`)
- [ ] `FakeSecureTokenStore`, a temp-file `PreferenceDataStoreFactory` helper, and a `MainDispatcherRule` (or `runTest` with `StandardTestDispatcher`) in `app/src/test/.../testing/`
- [ ] The test classes listed above (all ❌)
- [ ] Rewrite `AppViewModelTest.kt`: it constructs `AppViewModel()` with no arguments and asserts the Phase 1 stub
- [ ] `settings.gradle.kts`: `include(":spikes:stripe", ":spikes:agora")`; spike `build.gradle.kts` files

## Security Domain

`security_enforcement` is not set to `false` (the config file doesn't exist), so it is treated as enabled.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Email/password login against the backend. Refresh-token rotation with single-flight. Rejected refresh logs out. |
| V3 Session Management | yes | Tokens in `EncryptedSharedPreferences`, cleared fully on logout and suspension. Reuse detection respected via the single-flight authenticator. |
| V4 Access Control | yes | The `needAuthToken` allowlist means only authed paths carry the bearer. No bearer is sent to the public endpoints. |
| V5 Input Validation | yes | Moshi typed decoding. Lenient error parsing that never throws on unknown codes. |
| V6 Cryptography | yes | Android Keystore through security-crypto. Never hand-roll crypto. |
| V8 Data Protection | yes | Phase 1 already excludes all app data from cloud backup and device transfer. Debug logs redact `Authorization` and avoid bodies. |
| V9 Communication | yes | HTTPS only (cleartext is off by default at targetSdk ≥ 28). Certificate Transparency is on by default at target 37. |

### Known Threat Patterns for this stack

| Pattern | STRIPE category | Standard Mitigation |
|---------|-----------------|---------------------|
| Token leakage through logcat / `Level.BODY` | Information disclosure | `Level.BASIC` (or HEADERS with `redactHeader("Authorization")`) in a debug-only source set |
| Refresh-token replay (concurrent refresh) | Spoofing / repudiation (session revocation) | Single-flight `Authenticator` + `Mutex` + token comparison |
| Tokens restored from backup without the Keystore key | Information disclosure / DoS | Backup and extraction rules exclude everything (Phase 1); guarded ESP init |
| Suspended user keeps a valid local session | Elevation of privilege | Global 403 trap clears tokens and DataStore |
| Stripe secret key committed | Information disclosure | Design-rules `sk_`/`rk_` scan (extend to `spikes/`). Only `pk_` keys in the repo. |
| Upload keystore or passwords committed | Information disclosure | `*.jks`, `*.keystore`, `keystore.properties` are gitignored. Back up the key to the password manager (D-16). |
| Client-side PaymentIntent confirmation | Tampering | Path A: the app never confirms. Don't persist `stripe_account`. |
| Hard-coded JWT (`Debug/PreviewSecrets.swift`) | Information disclosure | Not ported (PROJECT.md) |

## Sources

### Primary (HIGH confidence)
- `ios-reference/ClickMe2026/Network/NetworkService.swift`, `APIRequestURLs.swift`, `ClickMeAPI+Auth.swift`, `ClickMeAPI+Me.swift`, `ClickMeAPI+ExpertProfile.swift`, `Error+UserMessage.swift`: request pipeline, single-flight actor, suspension trap, public paths (read in full).
- `ios-reference/ClickMe2026/Managers/UserManager.swift`, `ClickMe2026App.swift`, `Views/Screens/Splash/SplashViewModel.swift`, `Views/Screens/Login Flow/Login/LoginView.swift` and `LoginViewModel.swift`: session, routing and landing-mode logic.
- `ios-reference/ClickMe2026/Model/Enums/ErrorCode.swift` (65 cases counted), `Model/Responses/*`: DTO shapes.
- `backend-reference/docs/ios/STRIPE-INTEGRATION.md` §1, §2.1, §2.2, §2.4, §4, §6, `USER-REPORTING.md` §4, `CALL-END-SEMANTICS.md`.
- Stripe Android source (`stripe-android` master): `PaymentLauncher.kt`, `PaymentSheet.kt`, `PaymentSheetResult.kt`, `PaymentSheetCompose.kt`, `CHANGELOG.md` (23.24.0 dated 2026-10-19).
- Maven Central and Google Maven `maven-metadata.xml` for every version claim (2026-10-20).
- developer.android.com: FGS service types (microphone, while-in-use rule, Play declaration) and Android 17 behavior changes (background audio, Certificate Transparency).
- docs.stripe.com/testing (3DS test cards).
- docs.agora.io Android voice quickstart (artifact, permissions, engine and join snippets).
- support.google.com/googleplay/android-developer/answer/9845334 (internal test before setup is complete, package name fixed on upload).
- **Scratch-repo experiments (2026-10-20, in `/tmp/scratch-clickme`, real repo untouched):** Moshi codegen build and tests; `minifyProdReleaseWithR8`; both spike modules `assembleDebug` on AGP 9.4.1; `bundleProdRelease` signed with a conditional config and `jarsigner -verify`; MockWebServer single-flight test; the Robolectric ESP failure.

### Secondary (MEDIUM confidence)
- Android 17 `bg-audio` section read through a fetch summary of the behavior-changes page.
- Play Console steps beyond the cited lines (A1).

### Tertiary (LOW confidence)
- Samsung/One UI background behavior (A7), Agora Android method names (A6), join-window size (A9).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. Versions verified on Maven metadata, and everything compiled in a scratch build with the project's toolchain.
- Architecture (networking, session, signing): HIGH. Each key mechanism was prototyped and tested.
- Spikes (Stripe, Agora): MEDIUM. Source-read and compiled but not run on a device. This is exactly what the spikes exist to prove.
- Play Console flow: MEDIUM. Partly cited, partly assumed.
- Pitfalls: HIGH for the ones reproduced (Robolectric ESP, deprecations, logging source sets), MEDIUM for device behavior.

**Research date:** 2026-10-20
**Valid until:** 2026-11-03 for the Stripe and Agora versions (fast-moving, with Stripe releasing weekly). About 30 days for the rest.

## Resolved Open Questions (user, 2026-10-20)

- **Q1, cached profile:** Mirror iOS `UserProfileData` in full. Port the full model and the `/user/profile` fetch it needs now. This overrides the small-`CachedUser` recommendation.
- **Q2, spike inputs:** Use the recommendation, a single human checkpoint that collects all spike inputs.
- **Q3, 401 after a successful refresh:** Clear the session. A second 401 in a row after a successful refresh counts as auth rejection, matching iOS. This adds to the D-01 clearing conditions.
- **Q4, spike CI:** Yes. CI runs `assembleDebug` for both spike modules.
- **Q5, Play Console:** Use the recommendation. If rollout is blocked, save the release as a draft and log the blocker in STATE.md.
