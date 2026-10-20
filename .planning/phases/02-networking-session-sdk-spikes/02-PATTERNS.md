# Phase 2: Networking, Session & SDK Spikes - Pattern Map

**Mapped:** 2026-10-20
**Files analyzed:** 48 new or modified (grouped below)
**Analogs found:** 40 / 48. Most networking and session files have only an iOS analog. The Android analogs are Phase 1 shell, DI and test files.

Paths are relative to `/Users/LeonChen/Documents/ClickMe_Android`. `APP` = `app/src/main/java/com/uptrendinvestments/clickmeapp`. `iOS` = `ios-reference/ClickMe2026`, which is `/Users/LeonChen/Documents/ClickMe2026/ClickMe2026`.

Phase 1 has no Hilt modules, networking, DataStore or ViewModel with injected dependencies. Only `@HiltViewModel` classes with an empty constructor exist. So the Android analogs below only supply conventions: package header, KDoc style, `@HiltViewModel`/`@Inject constructor`, `StateFlow` exposure, a stateless Shell composable split from a Hilt-aware wrapper, and Robolectric tests.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `gradle/libs.versions.toml` (modify) | config | n/a | itself (existing entries) | exact |
| `app/build.gradle.kts` (modify: deps, ksp moshi, signing, version) | config | n/a | itself | exact |
| `settings.gradle.kts` (modify: include spikes) | config | n/a | itself | exact |
| `.github/workflows/ci.yml` (modify: spike assembleDebug) | config | batch | itself | exact |
| `scripts/check-design-rules.sh` (modify: scan `spikes/`) | config | batch | itself | exact |
| `APP/network/ApiRequestUrls.kt` | utility | request-response | `iOS/Network/APIRequestURLs.swift` | iOS only |
| `APP/network/ApiException.kt`, `ApiErrorParser.kt` | utility | request-response | `iOS/Network/Error+UserMessage.swift`, `StandardErrorResponse.swift` | iOS only |
| `APP/network/MoshiProvider.kt`, `InstantAdapter.kt` | utility | transform | none on Android (iOS `NetworkService` decoder) | no analog |
| `APP/network/AuthHeaderInterceptor.kt` | middleware | request-response | iOS `NetworkService.applyAuth` | iOS only |
| `APP/network/SuspensionInterceptor.kt` | middleware | request-response | iOS `NetworkService.send` lines 159-202 | iOS only |
| `APP/network/TokenAuthenticator.kt` (+ `TokenRefresher`) | middleware | request-response | iOS `TokenRefresher` actor + `UserManager.performRefreshCycle` | iOS only |
| `APP/network/ClickMeApi.kt` (Retrofit interface) | service | CRUD | iOS `ClickMeAPI+Auth/Me/UserProfile.swift` | iOS only |
| `APP/network/DebugNetwork.kt` (`src/debug` and `src/release`) | config | request-response | none | no analog |
| `APP/model/enums/ErrorCode.kt`, `UserRole.kt`, `AppMode.kt` | model | transform | `iOS/Model/Enums/ErrorCode.swift`; Android `AppRoute.kt` for enum style | role-match |
| `APP/model/responses/*` (LoginData, RefreshTokenData, MeData, AuthUser, PaginationMeta, SuccessDataResponse, ApiErrorEnvelope, UserProfileData, ExpertProfileStub, MyTopicsStub) | model | transform | `iOS/Model/Responses/*.swift` | iOS only |
| `APP/managers/SecureTokenStore.kt` (+ `EncryptedPrefsTokenStore`) | service | file-I/O | none (iOS Keychain in `UserManager`) | no analog |
| `APP/managers/SessionStore.kt` (DataStore) | service | file-I/O | iOS `UserManager` UserDefaults keys | iOS only |
| `APP/managers/UserManager.kt` | service | request-response + event-driven | `iOS/Managers/UserManager.swift` | iOS only |
| `APP/di/NetworkModule.kt`, `SessionModule.kt` | config | n/a | none (Hilt `@HiltViewModel` use only) | no analog |
| `APP/AppViewModel.kt` (modify) | store | event-driven | itself (Phase 1 stub) + `iOS/.../SplashViewModel.swift` | exact |
| `APP/ClickMeApp.kt` (modify: Login, SignupSetup placeholder, suspended dialog) | component | event-driven | itself | exact |
| `APP/MainActivity.kt` (modify: ON_START refresh) | component | event-driven | itself | exact |
| `APP/screens/login/LoginScreen.kt`, `LoginViewModel.kt` | component + store | request-response | `HomeClientScreen.kt` + `HomeClientViewModel.kt`; `ValidatedField`; `AsyncSaveButton` | role-match |
| `APP/screens/signup/SignupSetupPlaceholder.kt` | component | n/a | `components/PlaceholderTabContent.kt` | role-match |
| Profile tab logout button in `HomeClientScreen.kt` and `HomeExpertScreen.kt` | component | event-driven | existing `footer` slot in `ClientTabRoot` | exact |
| `res/values/strings*.xml` (modify) | config | n/a | `strings.xml` | exact |
| `config/AppLinks.kt` (modify: support mailto) | utility | n/a | itself | exact |
| `app/src/test/.../AppViewModelTest.kt` (rewrite) | test | event-driven | itself | exact |
| `app/src/test/.../screens/HomeShellsTest.kt` (extend) | test | request-response | itself | exact |
| `app/src/test/.../network/*Test.kt` (ResponseDecoding, InstantAdapter, ApiErrorParser, ApiRequestUrls, TokenAuthenticator, SuspensionInterceptor) | test | request-response | no Android analog; `AppConfigDevTest.kt` for plain-JUnit style | partial |
| `app/src/test/.../managers/UserManagerTest.kt`, `testing/FakeSecureTokenStore.kt`, `testing/` helpers | test | CRUD | none | no analog |
| `spikes/stripe/**`, `spikes/agora/**` (module build files, manifests, Activities, `CallService`) | module/component | request-response / streaming | `app/build.gradle.kts`, `MainActivity.kt`, `AndroidManifest.xml` | role-match |
| `app/src/main/keepRules/rules.keep` (modify only if R8 breaks) | config | n/a | itself | exact |

## Pattern Assignments

### `gradle/libs.versions.toml` and `app/build.gradle.kts` (config)

**Analog:** the files themselves. Add entries in the same inline-table style.

**Catalog style** (`libs.versions.toml` lines 14-18, 41-44):
```toml
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
moshi = { group = "com.squareup.moshi", name = "moshi", version.ref = "moshi" }
robolectric = { group = "org.robolectric", name = "robolectric", version = "4.17" }
```
Add the RESEARCH "Installation" block: `datastore`, `securityCrypto`, `stripe`, `agora` versions, and the six libraries.

**Dependency block style** (`app/build.gradle.kts` lines 94-103). The `ksp` plugin and `ksp(libs.hilt.compiler)` already exist, so add `ksp(libs.moshi.kotlin.codegen)` next to it:
```kotlin
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.okhttp)
debugImplementation(libs.okhttp.logging)
implementation(libs.retrofit)
implementation(libs.retrofit.converter.moshi)
implementation(libs.moshi)
```

**Signing and version** (modify `defaultConfig` lines 20-21 and `buildTypes` lines 52-58). Currently `versionName = "1.0"`, so change it to `"0.1.0"` (D-17). Use RESEARCH Pattern 7 verbatim. Note the existing `release { optimization { ... } }` block must be kept and the `signingConfig` line added inside it:
```kotlin
buildTypes {
    release {
        optimization {
            enable = true
            packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
        }
    }
}
```
Do not add Stripe or Agora to `:app` (D-10).

### `settings.gradle.kts` and spike modules (config / module)

**Analog:** `settings.gradle.kts` lines 19-23 (`FAIL_ON_PROJECT_REPOS`, so spikes must not declare repositories; `include(":app")` at the end).
```kotlin
rootProject.name = "ClickMe_Android"
include(":app")
// add: include(":spikes:stripe", ":spikes:agora")
```
Spike `build.gradle.kts` copies the `app/build.gradle.kts` header (lines 1-24): `alias(libs.plugins.android.application)`, `alias(libs.plugins.kotlin.compose)`, `compileSdk { version = release(37) }`, minSdk 26, targetSdk 37, `buildFeatures { compose = true; buildConfig = true }`, `compileOptions` JVM 11. Leave out `ksp`, `hilt` and the flavors. Add `buildConfigField` copies of `BASE_URL` and `STRIPE_PUBLISHABLE_KEY`/`AGORA_APP_ID` from the `dev` flavor (`app/build.gradle.kts` lines 29-38), as plain `defaultConfig` fields. Apply `alias(libs.plugins.ktlint)` (header line 7) so CI style holds, or exclude the spikes on purpose.

### `.github/workflows/ci.yml` and `scripts/check-design-rules.sh` (config)

**Analog:** the files themselves.

CI command (ci.yml line 28). Append the spike tasks:
```yaml
run: ./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug --stacktrace
# append: :spikes:stripe:assembleDebug :spikes:agora:assembleDebug
```
Secret scan (check-design-rules.sh line 22). Add `spikes` to the path list:
```bash
hits=$(grep -rnE '(sk_live|sk_test|rk_live|rk_test)_[A-Za-z0-9]+' app gradle ./*.kts 2>/dev/null || true)
```
The hex-color check uses `SRC_DIR` (default `app/src`). Spikes with Compose should avoid `Color(0x...)` or run the script with a spike path too.

### `APP/network/ApiRequestUrls.kt` (utility, request-response)

**Analog:** `iOS/Network/APIRequestURLs.swift` lines 529-543. Port with the encoded-path fix from RESEARCH Pattern 2:
```swift
private static let publicPaths: Set<String> = [
    "/auth/login", "/auth/signup", "/auth/refresh",
    "/auth/password/forgot", "/auth/password/reset",
]
static func needAuthToken(url: String) -> Bool {
    !publicPaths.contains { url.hasSuffix($0) }
}
```
```kotlin
object ApiRequestUrls {
    private val publicPaths = setOf("/auth/login", "/auth/signup", "/auth/refresh", "/auth/password/forgot", "/auth/password/reset")
    fun needAuthToken(encodedPath: String): Boolean = publicPaths.none { encodedPath.endsWith(it) }
}
```
Do not mark `/auth/email/*` as public.

### `APP/network/SuspensionInterceptor.kt` (middleware, request-response)

**Analog:** `iOS/Network/NetworkService.swift` lines 155-202. Only 403 with `code == ACCOUNT_SUSPENDED` fires the handler. The error still propagates. This runs before the 401 path. Parse the code leniently from a raw string:
```swift
if case let NetworkError.httpError(403, data) = error,
   isAccountSuspendedError(data: data),
   let handler = suspensionHandler
{
    await handler()
    throw error
}
...
struct Envelope: Decodable { let code: String? }
return envelope.code == ErrorCode.accountSuspended.rawValue
```
Android shape is RESEARCH Pattern 3 (`response.peekBody(64 * 1024)`). Add the idempotency guard from `UserManager.swift` line 146 (`guard !wasSuspended else { return }`).

### `APP/network/TokenAuthenticator.kt` and `TokenRefresher` (middleware, request-response)

**Analog:** `iOS/Network/NetworkService.swift` lines 172-188 (401 then refresh then retry once) and `iOS/Managers/UserManager.swift` lines 439-467. The decision matrix to port (D-03, plus resolved Q3):
```swift
guard let stored = storedRefreshToken() else { logout(); return false }   // no refresh token -> logout
do {
    let response = try await api.refreshSession(refreshToken: stored)
    applyRefreshed(response.data)          // persist rotated pair immediately
    return true
} catch {
    if case NetworkError.httpError(let status, _) = error, status == 401 { logout() }
    return false                            // network / 429 / 5xx: keep tokens
}
```
The Android implementation is RESEARCH Pattern 1, verbatim (Mutex plus a sent-vs-current token comparison, `priorResponse` count for retry once, a bare client for the refresh call). Resolved Q3 adds this. A 401 on the retry after a successful refresh must clear the session. The `priorResponse > 1` branch that currently returns `null` is the place to call `onSessionRejected()`, but only if a refresh just succeeded. Persist the rotated pair inside the lock and not through a cancellable suspend (Pitfall 3).

### `APP/network/ClickMeApi.kt` (service, CRUD)

**Analog:** `iOS/Network/ClickMeAPI+Auth.swift` lines 11-30, `ClickMeAPI+Me.swift` and `ClickMeAPI+UserProfile.swift` lines 11-13. The Retrofit interface covers these endpoints: `login`, `refreshSession`, `getMe`, `getUserProfile` (resolved Q1), `GET /expert/profile`, `GET /expert/topics`. Payloads are `{email,password}` and `{refresh_token}`.
```swift
func login(email: String, password: String) async throws -> SuccessDataResponse<LoginData>
func refreshSession(refreshToken: String) async throws -> SuccessDataResponse<RefreshTokenData>
func getUserProfile() async throws -> SuccessDataResponse<UserProfileData>
```
Use `suspend fun` with `SuccessDataResponse<T>` returns. Retrofit base URL has a trailing slash appended (`AppConfig.baseUrl + "/"`). Paths in the interface have no leading slash.

### `APP/model/responses/*` (model, transform)

**Analogs:** `iOS/Model/Responses/LoginData.swift`, `RefreshTokenData.swift`, `MeData.swift`, `StandardErrorResponse.swift`, `UserProfileData.swift`. Port each field and add an explicit `@Json(name = "snake_case")` for every multi-word key (Pitfall 4).

Key shapes. `LoginData`: `token`, `user: AuthUser`, `refreshToken`, `expiresAt: Int` (epoch seconds), `expiresIn: Int`. `RefreshTokenData`: the same trio without `user`. `MeData`: `id`, `email`, `roles`, `pricingLimits?` (decode, never persist). `UserProfileData` (full mirror, resolved Q1) is a nested structure:
```swift
struct UserProfileData: Codable {
    struct PersonalDetails: Codable { firstName, lastName, email, avatarUrl, bio }   // all String?
    struct ProfessionalDetails: Codable { jobTitle, company, education }
    struct Location: Codable { city, stateProvince, country /* alpha-3 code */, timezone }
    struct LanguageEntry: Codable { id, label }
    let personalDetails; let professionalDetails; let location; let languages: [LanguageEntry]
}
```
Note: iOS marks the `StandardErrorResponse.code` as lenient (unknown code becomes nil, `StandardErrorResponse.swift` lines 20-32). The Android `ApiErrorEnvelope` keeps `code: String?`. Use the exact DTO from RESEARCH "Code Examples". `UserProfileData` is `Codable` in iOS because it is cached to `profileCacheKey` (`UserManager.logout` line 305). The Android cache stores it as one Moshi JSON string in DataStore (`cached_profile_json`), alongside the login `AuthUser` snapshot. Because Q1 is resolved as "mirror in full", RESEARCH Pattern 6's smaller `CachedUser` is superseded.

### `APP/model/enums/ErrorCode.kt`, `UserRole.kt`, `AppMode.kt` (model, transform)

**Analogs:** `iOS/Model/Enums/ErrorCode.swift` (65 cases, port verbatim with raw values) and `APP/AppRoute.kt` for Android enum/KDoc style:
```kotlin
/**
 * Root route of the app, mirroring iOS `AppRoute` in `ClickMe2026App.swift`.
 * ...
 */
enum class AppRoute { Splash, Login, SignupSetup, ClientHome, ExpertHome }
```
Use the `ErrorCode(val wire: String)` plus `fromWire()` lookup from RESEARCH Pattern 4. Do not use `EnumJsonAdapter`. A unit test must assert 65 unique wire values.

### `APP/managers/UserManager.kt` (service, request-response + event-driven)

**Analog:** `iOS/Managers/UserManager.swift`. Port these functions: `login` (lines 163-175, `apply(loginData:)`), `tryAutoLogin` (225-279), `logout` (288-307), `setMode`/`hasStoredModePreference` (360-374), the suspension flag (146-155) and `performRefreshCycle` (439-467).

Logout clears these on iOS. The Android equivalent must clear the same set (FND-08), tokens first and synchronously:
```swift
authUser = nil; me = nil; profile = nil; expertProfile = nil
api.bearerToken = nil
persist(token: nil); clearRefreshCredentials()
UserDefaults.standard.removeObject(forKey: onboardingShownKey)
UserDefaults.standard.removeObject(forKey: currentModeKey)
UserDefaults.standard.removeObject(forKey: profileCacheKey)
currentMode = .client
```
Android deviation to implement (D-01, Pitfall 2). iOS `isAuthRejection` (lines 265-279) treats any 401 or non-suspension 403 as a rejection and logs out, and treats network and 5xx as transient and keeps the session. RESEARCH says iOS logs out on any `/me` failure, which is imprecise. iOS already keeps the session on transient failures. The real deviation is that on Android a 401 with tokens still stored (soft refresh failure) must also take the cached path.

The restore flow follows `SplashViewModel.swift` lines 53-126. Keep the sequence: `/me` then, for experts, `isExpertProfileIncomplete` (a failure counts as complete) then `resolveLandingMode`:
```swift
guard let roles = userManager.me?.roles else { destination = .dashboard(userManager.currentMode); return }
if isExpert { if await isExpertProfileIncomplete() { destination = .setupIncomplete; return } }
destination = .dashboard(await resolveLandingMode(isExpert: isExpert))
```
Dependency rule: keep `UserManager` out of the OkHttp graph (RESEARCH Pattern 6 Hilt cycle). The authenticator and interceptor depend only on `SecureTokenStore` and a `SessionEvents` object.

### `APP/managers/SecureTokenStore.kt` and `SessionStore.kt` (service, file-I/O)

**Analog:** none on Android. The iOS equivalent is the Keychain plus UserDefaults keys in `UserManager.swift` lines 66-67 (`hasSeenOnboarding`, `currentMode`). Use RESEARCH Pattern 6: an interface with an in-memory `@Volatile` cache, an `EncryptedPrefsTokenStore` with guarded init (delete and retry once), and a `SessionStore` over DataStore Preferences with keys `current_mode`, `has_seen_onboarding` and `cached_profile_json`. Logout is `edit { it.clear() }`. The real ESP store cannot run under Robolectric, so tests use `FakeSecureTokenStore`.

### `APP/di/NetworkModule.kt`, `SessionModule.kt` (config)

**Analog:** none. Phase 1 used `@HiltAndroidApp` only (`APP/ClickMeApplication.kt`):
```kotlin
@HiltAndroidApp
class ClickMeApplication : Application()
```
Use RESEARCH Pattern 10 for the wiring. Install in `SingletonComponent`. A `@Named("bare")` OkHttpClient is the refresher's client. The base URL comes from `AppConfig` (`APP/config/AppConfig.kt` lines 11-16: `AppConfig.baseUrl` returns `BuildConfig.BASE_URL`). Only screen-level and data-layer code may read `AppConfig`, which suits the Hilt modules.

### `APP/network/DebugNetwork.kt` (config)

**Analog:** none. `okhttp.logging` is `debugImplementation` only (`app/build.gradle.kts` line 99), so main-source code can't reference it. Create `app/src/debug/java/.../network/DebugNetwork.kt` and a stub of the same class in `app/src/release/java/...`. Use `Level.BASIC`, or HEADERS with `redactHeader("Authorization")` (Pitfall 7). Never use `BODY`. Existing flavor source sets (`app/src/dev/res`, `testDev`, `testProd`) show source-set dirs are already in use.

### `APP/AppViewModel.kt` (store, event-driven) - modify

**Analog:** itself. Keep the `StateFlow` and `asStateFlow` exposure and the `@HiltViewModel` shape. Replace the empty constructor with injected dependencies.

Current structure to keep (lines 16-28):
```kotlin
@HiltViewModel
class AppViewModel
    @Inject
    constructor() : ViewModel() {
        private val _route = MutableStateFlow(AppRoute.Splash)
        val route: StateFlow<AppRoute> = _route.asStateFlow()

        private val _destinationResolved = MutableStateFlow(false)
        val destinationResolved: StateFlow<Boolean> = _destinationResolved.asStateFlow()

        init { resolveInitialRoute() }
```
Hook to replace (lines 30-40, the KDoc explicitly says Phase 2 must add the splash cap):
```kotlin
private fun resolveInitialRoute() {
    _route.value = AppRoute.ClientHome
    _destinationResolved.value = true
}
```
New body, per RESEARCH Pattern 6: `val job = viewModelScope.async { userManager.restore() }` then `withTimeoutOrNull(5_000) { job.await() }`. A null result means the cached/offline path if tokens and a cached profile exist, else Login. Never wrap the restore body in `withTimeout`. Keep `debugSwitchScaffold()` (lines 46-53) unchanged. Add collectors for session state and `wasSuspended` that flip the route to `Login` (mirror of iOS `onChange(of: isLoggedIn)`).

Warning: the `AppViewModelTest` constructs `AppViewModel()` with no arguments in 2 tests, so it must be rewritten.

### `APP/ClickMeApp.kt` (component, event-driven) - modify

**Analog:** itself. Current `when` (lines 24-36) marks Login and SignupSetup as unreachable and renders `EmptySurface`:
```kotlin
when (route) {
    // The OS splash covers Splash; Login and SignupSetup are unreachable until Phases 3 and 4.
    AppRoute.Splash, AppRoute.Login, AppRoute.SignupSetup, -> EmptySurface(modifier)
    AppRoute.ClientHome -> HomeClientScreen(onDebugSwitchScaffold = appViewModel::debugSwitchScaffold)
    AppRoute.ExpertHome -> HomeExpertScreen(onDebugSwitchScaffold = appViewModel::debugSwitchScaffold)
}
```
Split the branch: `Splash` keeps `EmptySurface`, `Login` renders `LoginScreen`, `SignupSetup` renders `SignupSetupPlaceholder`. Update the stale comment. Add the suspension dialog after the `when`, driven by `collectAsStateWithLifecycle()` (already imported and used on line 23). Use a Material `AlertDialog` (CLAUDE.md Platform swaps). Its `onDismissRequest` and confirm button both call `clearSuspensionFlag()`. Copy per A5: title "Your account has been suspended" plus `support@clickme.app` via `AppLinks`.

### `APP/MainActivity.kt` (component, event-driven) - modify

**Analog:** itself. Keep the `setKeepOnScreenCondition { !appViewModel.destinationResolved.value }` (line 22). The 5s cap lives in the ViewModel, so the activity only needs the `ON_START` stale `/me` retry from RESEARCH Pattern 6, using `repeatOnLifecycle(Lifecycle.State.STARTED)` inside `lifecycleScope.launch`. Keep the `@AndroidEntryPoint`/`by viewModels()` shape (lines 14-16).

### `APP/screens/login/LoginScreen.kt` and `LoginViewModel.kt` (component and store, request-response)

**Analogs:** `APP/screens/clientmode/home/HomeClientScreen.kt` + `HomeClientViewModel.kt` for the Screen/ViewModel/Hilt split; `components/ValidatedField.kt` and `components/buttons/AsyncSaveButton.kt` for the form controls; iOS `Views/Screens/Login Flow/Login/LoginView.swift` and `LoginViewModel.swift` for behavior.

**Hilt-aware wrapper plus stateless content** (`HomeClientScreen.kt` lines 37-50, 56-62). Follow the same split so the content composable renders under Robolectric:
```kotlin
@Composable
fun HomeClientScreen(onDebugSwitchScaffold: () -> Unit, viewModel: HomeClientViewModel = hiltViewModel()) {
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    HomeClientShell(avatarUrl = avatarUrl, ...)
}
```
**ViewModel shape** (`HomeClientViewModel.kt` lines 14-20):
```kotlin
@HiltViewModel
class HomeClientViewModel
    @Inject
    constructor() : ViewModel() {
        private val _avatarUrl = MutableStateFlow<String?>(null)
        val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()
    }
```
**Fields:** `ValidatedField(value, onValueChange, placeholder, leadingIcon, error, isSecure = ...)` with `Icons.Outlined.Email` and `Icons.Outlined.Lock` (the icons used in its own preview, lines 234-255). It is stateless. The caller owns `value`. The error line is its `error` parameter.
**Submit:** `AsyncSaveButton(title, state: SaveState, onClick)` with `SaveState.Idle/Saving/Saved`. Taps are ignored unless `Idle`. D-07 asks for a plain Material 3 form, so a `Button` is acceptable, but `AsyncSaveButton` is available and avoids double submits.
**Post-login routing:** call `UserManager.login`, then choose the mode like iOS (smart-landing, `resolveLandingMode`), `setMode`, and let `AppViewModel` map it to `ClientHome`/`ExpertHome`. The login 401 (bad credentials) must show an inline error and must not trigger refresh (public path). A `403 ACCOUNT_SUSPENDED` on login is also caught by `SuspensionInterceptor`.

### `APP/screens/signup/SignupSetupPlaceholder.kt` (component)

**Analog:** `APP/components/PlaceholderTabContent.kt` (lines 31-78). It is a `Scaffold` with a `TopAppBar`, and an `EmptyState` in a centered scrollable `Column`. Reuse it as-is, passing a title and icon, or copy the layout:
```kotlin
@Composable
fun PlaceholderTabContent(
    title: String, icon: ImageVector, modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
)
```
The `footer` slot can hold a logout button so a user stuck on this route can escape.

### Logout button in `HomeClientScreen.kt` and `HomeExpertScreen.kt` (component, event-driven) - modify

**Analog:** the existing `footer` slot on the Profile tab, `HomeClientScreen.kt` lines 83-98:
```kotlin
entry<ClientProfileRoot> {
    ClientTabRoot(
        key = ClientProfileRoot,
        footer = {
            if (showDebugAffordances) {
                OutlinedButton(onClick = onDebugSwitchScaffold) { Text(stringResource(R.string.debug_switch_to_expert)) }
                Spacer(Modifier.height(Spacing.sm))
                ...
            }
        },
    )
}
```
Add an always-visible (not debug-gated) `OutlinedButton` with `stringResource(R.string.<logout>)` before the debug block. Add a new `onLogout: () -> Unit` parameter to `HomeClientShell`/`HomeExpertShell`. The Shell stays free of Hilt (its KDoc says so, lines 52-55), and the Hilt wrapper supplies `viewModel::logout`. Mirror the same change in `HomeExpertScreen.kt` for `ExpertProfileRoot`. If the debug-only "invalidate access token" action (RESEARCH Manual-Only) is added, put it under `showDebugAffordances` next to the existing debug buttons.

### `APP/config/AppLinks.kt` and strings (config)

**Analogs:** themselves. Read `config/AppLinks.kt` before editing to follow how existing links are exposed, then add the support `mailto:`. New strings go in `res/values/strings.xml`. Existing resource names to follow include `empty_state_placeholder_body`, `debug_switch_to_expert` and `a11y_state_saving`.

### Tests

**`AppViewModelTest.kt` - rewrite.** Current file (lines 7-40) has 3 tests. `freshViewModelResolvesClientHomeSynchronously` and `debugSwitchScaffoldTogglesBetweenClientAndExpertHome` call `AppViewModel()` with no args, so they must be rewritten around injected fakes. `appRouteMirrorsIosRoutesInOrder` stays as is. Plain JUnit4 plus `kotlinx-coroutines-test` (already in the catalog, `testImplementation(libs.kotlinx.coroutines.test)`) for the virtual-time 5s cap:
```kotlin
class AppViewModelTest {
    @Test
    fun appRouteMirrorsIosRoutesInOrder() {
        assertEquals(listOf(AppRoute.Splash, AppRoute.Login, AppRoute.SignupSetup, AppRoute.ClientHome, AppRoute.ExpertHome), AppRoute.entries)
    }
}
```
**`HomeShellsTest.kt` - extend** (Robolectric Compose, `createComposeRule()` from `androidx.compose.ui.test.junit4.v2`). Adding the `onLogout` parameter to the shells means `setClient`/`setExpert` (lines 27-52) must pass it. Add a test that taps the Profile tab and asserts the logout button exists, using the existing helpers:
```kotlin
private fun tab(label: String) = hasText(label) and isSelectable()
private fun tapTab(label: String) = composeRule.onNode(tab(label)).performClick()
```
**Networking tests** (`TokenAuthenticatorTest`, `SuspensionInterceptorTest`, and so on) have no Android analog. Use RESEARCH "Phase Requirements to Test Map" and `mockwebserver3` 5.5.0. For plain unit style, `app/src/testDev/.../AppConfigDevTest.kt` is the nearest example.

**Shared test helpers** in `app/src/test/.../testing/`: `FakeSecureTokenStore`, a temp-file `PreferenceDataStoreFactory` helper and a dispatcher rule (Wave 0 gaps in RESEARCH).

### `spikes/stripe/**` and `spikes/agora/**` (module, request-response and streaming)

**Analogs:** `APP/MainActivity.kt` (single `ComponentActivity` with `setContent`) and `app/src/main/AndroidManifest.xml` (application and activity declaration, launcher intent-filter). The spikes have no Hilt and no `:app` imports (D-10, D-11), so the Activity is a plain `ComponentActivity` with `setContent { MaterialTheme { ... } }`. Do not copy `@AndroidEntryPoint` or `ClickMeTheme`.

**Manifest template** (`AndroidManifest.xml` lines 5-27): a minimal `<application>` with `android:allowBackup="false"` and a launcher `<activity>`. For Agora add the permissions and the `<service ... android:foregroundServiceType="microphone"/>` from RESEARCH Pattern 9.

**Per-spike behavior** has no codebase analog; use RESEARCH Patterns 8 and 9. The iOS reference for Agora is `iOS/Managers/AgoraManager.swift` and `Network/ClickMeAPI+Call.swift` (`JoinCallResponse`, lines 13-31):
```swift
struct AgoraConfig: Decodable { let rtcToken: String; let uid: Int; let channelName: String }
let bookingId: UUID; let connectionType: ConnectionType
let agoraConfig: AgoraConfig?; let externalConfig: ExternalConfig?
```
Spikes use `Map<String, Any?>` Retrofit bodies, so these DTOs are not needed there. Read `rtc_token`, `uid` and `channel_name` from `data.agora_config`.

## Shared Patterns

### Hilt ViewModel shape
**Source:** `APP/screens/clientmode/home/HomeClientViewModel.kt` lines 14-20, `APP/AppViewModel.kt` lines 16-24.
**Apply to:** `LoginViewModel`, the modified `AppViewModel`.
Use `@HiltViewModel class X @Inject constructor(deps) : ViewModel()`, with a private `MutableStateFlow` and a public `asStateFlow()`.

### Stateless Shell plus Hilt wrapper
**Source:** `HomeClientScreen.kt` lines 37-62.
**Apply to:** `LoginScreen` (and `LoginContent`), the logout wiring.
The wrapper reads `hiltViewModel()` and `BuildConfig`. The inner composable takes plain parameters, so Robolectric can render it.

### ktlint and file style
**Source:** every Phase 1 file.
**Apply to:** all new Kotlin, including the spikes if they apply the plugin.
No wildcard imports, imports sorted, `@Inject constructor` on its own line under the class name (see `AppViewModel.kt` lines 17-19), trailing commas in multi-line argument lists, and KDoc on public types. The CI gate also bans `Color(0x...)` outside `ui/theme/Color.kt` and any `*Brand` type. Use `MaterialTheme.colorScheme` and `MaterialTheme.extras` (for example `extras.destructive`, as in `ValidatedField.kt` line 87) rather than literals.

### Spacing and theme
**Source:** `ui/theme/Spacing.kt` via `Spacing.sm`, `Spacing.md`, `Spacing.lg` (used in `PlaceholderTabContent.kt` line 74 and `ValidatedField.kt`).
**Apply to:** `LoginScreen` and the suspension dialog content. Do not hard-code `dp` for layout gaps.

### Per-flavor config access
**Source:** `APP/config/AppConfig.kt` lines 11-16.
**Apply to:** `NetworkModule` only (base URL). Shared components take plain parameters.

### Error and session semantics (iOS source of truth)
**Source:** `iOS/Network/NetworkService.swift` lines 155-190, `iOS/Managers/UserManager.swift` lines 231-307 and 439-467.
**Apply to:** `SuspensionInterceptor`, `TokenAuthenticator`, `UserManager`, `LoginViewModel`.
Suspension is checked before the 401 refresh. Refresh logs out only on a 401 from `/auth/refresh` or on a missing refresh token. Logout clears tokens, profile cache, `currentMode` and the onboarding flag. Network, 429 and 5xx failures keep the session.

### Logging and secrets
**Source:** `app/build.gradle.kts` line 99 (logging is `debugImplementation`) and `scripts/check-design-rules.sh` line 22.
**Apply to:** all network code and spikes. Debug-only logging with `Authorization` redacted. Only `pk_` keys in the repo.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `network/MoshiProvider.kt`, `InstantAdapter.kt` | utility | transform | Phase 1 has no JSON code; use RESEARCH Pattern 5 (`OffsetDateTime.parse`). iOS truncates fractions by regex and is not worth copying. |
| `managers/SecureTokenStore.kt` (ESP) | service | file-I/O | No Android storage code yet; iOS uses the Keychain. Use RESEARCH Pattern 6. |
| `managers/SessionStore.kt` (DataStore) | service | file-I/O | No DataStore yet. |
| `di/NetworkModule.kt`, `di/SessionModule.kt` | config | n/a | No Hilt `@Module` exists; use RESEARCH Pattern 10. |
| `network/DebugNetwork.kt` (debug/release source sets) | config | request-response | No debug/release source set code exists (only `dev` res and `testDev`/`testProd`). |
| MockWebServer and `UserManager` tests, fakes | test | request-response | No such tests exist; use the RESEARCH test map. |
| Stripe spike (PaymentSheet, `rememberPaymentLauncher`) and Agora spike (`CallService`, `RtcEngine`) | component | streaming | No SDK code anywhere; use RESEARCH Patterns 8 and 9. Android 17 and Stripe API drift are called out in Pitfalls 6 and 8. |
| Play signing config | config | n/a | No signing config; use RESEARCH Pattern 7. |

## Notes for the planner

1. RESEARCH says iOS `tryAutoLogin()` logs out on any `/me` failure, but the source (`UserManager.swift` lines 245-257) already keeps the session on non-auth errors. The true Android deviation is the soft-401 case (tokens stored after a soft refresh failure), not offline or 5xx. Plan tests around that.
2. The Q1 resolution (full `UserProfileData` cache) adds `GET /user/profile` to the Phase 2 endpoint list. Fetch it in `restore()` and on login, and persist it in DataStore as JSON (clear on logout).
3. The Q3 resolution (clear on a 401 after a successful refresh) changes the `TokenAuthenticator` "retry once" branch. It must call `onSessionRejected()` when the retry of a request that just received a fresh token also returns 401.
4. `HomeClientShell` and `HomeExpertShell` gain an `onLogout` parameter, so `HomeShellsTest` call sites (lines 27-52 of that test) must be updated in the same plan.
5. `app/build.gradle.kts` still says `versionName = "1.0"`. D-17 requires `"0.1.0"`.

## Metadata

**Analog search scope:** `app/src/main`, `app/src/test`, `app/src/testDev`, `app/src/testProd`, `gradle/`, `scripts/`, `.github/workflows/`, and iOS `Network/`, `Managers/`, `Model/Responses/`, `Views/Screens/Splash/`.
**Files scanned:** about 45 Android files listed, and 12 iOS files read in full or in part.
**Pattern extraction date:** 2026-10-20
