# Phase 3: Login, Password Recovery & Realtime - Pattern Map

**Mapped:** 2026-10-20
**Files analyzed:** 36 (new or modified)
**Analogs found:** 33 / 36

Base path for all main files: `app/src/main/java/com/uptrendinvestments/clickmeapp/` (written `$M/`). Tests: `app/src/test/java/com/uptrendinvestments/clickmeapp/` (`$T/`).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `$M/screens/login/LoginViewModel.kt` (rewrite) | viewmodel | request-response | itself (current temp version) | exact |
| `$M/screens/login/LoginScreen.kt` (rewrite) | screen | request-response | itself + `SignupSetupPlaceholder.kt` | exact |
| `$M/screens/login/AuthValidators.kt`, `FieldError.kt` | utility | transform | none in repo (iOS `LoginViewModel`) | no analog |
| `$M/screens/login/forgetpassword/ForgotPasswordScreen.kt` + `ViewModel` | screen+viewmodel | request-response | `LoginScreen.kt` / `LoginViewModel.kt` | role-match |
| `$M/screens/login/setnewpassword/SetNewPasswordScreen.kt` + `ViewModel` | screen+viewmodel | request-response | `LoginScreen.kt` / `LoginViewModel.kt` | role-match |
| `$M/screens/login/passwordchanged/PasswordChangedScreen.kt` | screen | static | `SignupSetupPlaceholder.kt` | role-match |
| `$M/screens/signup/SignUpPlaceholderScreen.kt` | screen | static | `SignupSetupPlaceholder.kt` | exact |
| `$M/navigation/AuthNavHost.kt`, `AuthKeys.kt` | navigation | event-driven | `HomeClientScreen.kt` + `HomeTabScaffold.kt` | exact |
| `$M/navigation/HomeTabScaffold.kt` (extract `slide()`) | navigation | transform | itself | exact |
| `$M/ClickMeApp.kt` (route `Login` -> AuthNavHost) | root | switch | itself | exact |
| `$M/components/ValidatedField.kt` (+trailingBadge, contentType) | component | transform | itself | exact |
| `$M/components/PasswordStrength.kt`, `PasswordStrengthMeter.kt` | component+utility | transform | `ValidatedField.kt` (styling) | partial |
| `$M/network/ApiRequestUrls.kt` (+2 consts) | config | n/a | itself | exact |
| `$M/network/ClickMeApi.kt` (+2 endpoints) | service | request-response | itself | exact |
| `$M/model/requests/AuthRequests.kt` (+2 DTOs) | model | n/a | itself | exact |
| `$M/model/responses/SuccessMessageResponse.kt` | model | n/a | `SuccessDataResponse.kt` | role-match |
| `$M/realtime/RealtimeEvents.kt`, `RealtimeEventDecoder.kt` | model+utility | event-driven/transform | `network/MoshiProvider.kt`, `InstantAdapter.kt`, `model/requests/AuthRequests.kt` (DTO style) | partial |
| `$M/realtime/RealtimeEmits.kt`, `RealtimeUrl.kt` | utility | transform | `network/ApiRequestUrls.kt` (pure object) | partial |
| `$M/realtime/RealtimeConnection.kt` | interface | n/a | `managers/SecureTokenStore.kt` (seam interface) | role-match |
| `$M/realtime/RealtimeService.kt` | service | streaming | `managers/SessionEvents.kt` (flows) | partial |
| `$M/realtime/RealtimeConnectionManager.kt` | manager | event-driven | `managers/UserManager.kt` (`onSessionEvent`, `@ApplicationScope`) | role-match |
| `$M/realtime/ProcessForeground.kt` | provider | event-driven | none | no analog |
| `$M/di/RealtimeModule.kt` | config | n/a | `di/SessionModule.kt`, `di/NetworkModule.kt` | exact |
| `$M/ClickMeApplication.kt` (start manager) | config | n/a | itself | exact |
| `app/src/debug/.../realtime/RealtimeDebugLogger.kt` + release stub | utility | streaming | `src/debug/.../network/DebugNetwork.kt` + release twin | exact |
| `gradle/libs.versions.toml`, `app/build.gradle.kts`, keep rules | config | n/a | existing entries | exact |
| `$T/testing/FakeClickMeApi.kt` (+handlers) | test-fake | n/a | itself | exact |
| `$T/testing/FakeRealtimeConnection.kt`, `FakeProcessForeground.kt` | test-fake | n/a | `FakeSecureTokenStore.kt` | role-match |
| `$T/screens/login/LoginViewModelTest.kt` (rewrite), `LoginScreenTest.kt` (rewrite) | test | n/a | themselves | exact |
| `$T/screens/login/*ViewModelTest`, `AuthValidatorsTest`, `PasswordStrengthTest`, `AuthNavigationTest` | test | n/a | `LoginViewModelTest.kt`, `navigation/NavigatorTest.kt` | role-match |
| `$T/network/ClickMeApiTest.kt`, `ApiRequestUrlsTest.kt` (extend) | test | n/a | themselves | exact |
| `$T/realtime/*Test` (Url, Decoder, Emits, Manager, ServiceProtocol) | test | n/a | `TokenAuthenticatorTest.kt` (MockWebServer), `UserManagerTest.kt` (virtual time) | role-match |

## Pattern Assignments

### `$M/screens/login/LoginViewModel.kt` (rewrite in place)

**Analog:** current file. Keep: `@HiltViewModel` + `@Inject constructor(userManager)`, `MutableStateFlow` uiState, one-shot `destination: StateFlow<LaunchDestination?>` with `onDestinationConsumed()`, `try/catch ApiException/finally isSubmitting=false`.

Core submit shape (lines 62-82):
```kotlin
fun submit() {
    val current = _uiState.value
    if (current.isSubmitting) return
    val email = current.email.trim()
    ...
    _uiState.update { it.copy(isSubmitting = true, error = null) }
    viewModelScope.launch {
        try {
            val user = userManager.login(email, current.password)
            _destination.value = userManager.resolvePostAuthDestination(user.userRoles)
        } catch (e: ApiException) {
            _uiState.update { it.copy(error = map(e)) }
        } finally {
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }
}
```
Error mapping (lines 85-97), extend per D-04: `ApiException.Http` has `status`, `code: ErrorCode?`, `serverMessage`, `fieldErrors`, `userMessage`. Keep `e.code == ErrorCode.AccountSuspended -> null` (global dialog). Add: `status == 401` -> password inline; `fieldErrors` with `field == "email"` -> email inline; else dialog state (nullable, cleared on dismiss). `ApiException.Network`/`Decoding` -> dialog. Replace `LoginError` with the new `FieldError` (Res/Text) per research Pattern 6. Add `didAttemptSubmit` for D-02 and `prefill(email)`. Constructor stays `(userManager)` so the Harness in `LoginViewModelTest` keeps compiling.

### `$M/screens/login/LoginScreen.kt` (rewrite)

**Analog:** current file. Keep the two-layer split (Hilt `LoginScreen` + stateless `LoginContent`), `LaunchedEffect(destination)` consumption, `collectAsStateWithLifecycle`, the Column modifier chain, IME handling.

Hilt wrapper (lines 39-60):
```kotlin
@Composable
fun LoginScreen(onLoggedIn: (LaunchDestination) -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    LaunchedEffect(destination) {
        destination?.let { onLoggedIn(it); viewModel.onDestinationConsumed() }
    }
```
Container + fields (lines 71-107): `Column(Modifier.fillMaxSize().background(colorScheme.surface).windowInsetsPadding(WindowInsets.safeDrawing).imePadding().verticalScroll(...).padding(horizontal = Spacing.lg, vertical = Spacing.xl))`. Wire `error = state.emailError?.asString()` into `ValidatedField` (currently `error = null`). Submit button (lines 116-121): `AsyncSaveButton(title, state = if (isSubmitting) SaveState.Saving else SaveState.Idle, onClick = onSubmit)`. Dialog: Material `AlertDialog`; see `screens/login/SuspendedAccountDialog.kt` for the project dialog style. Add `onForgotPassword(email)`, `onSignUp`, `prefillEmail` params. Imports use `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` (not `androidx.hilt.navigation.compose`).

### Recovery screens: `ForgotPasswordScreen/ViewModel`, `SetNewPasswordScreen/ViewModel`

**Analog:** `LoginScreen.kt` / `LoginViewModel.kt` (same Hilt-wrapper + stateless-content split, same state-flow shape, `ValidatedField` + `AsyncSaveButton`). Differences: inject `ClickMeApi` and wrap with `apiCall { api.forgotPassword(ForgotPasswordRequest(email)) }` (no UserManager involvement; public endpoints). Use `ApiException.Http.status` (400 -> code inline on reset, per research Pitfall 10) and `fieldErrors` for 422 mapping. "Saved" hold: `AsyncSaveButton` with `SaveState.Saved`, `delay(600)`, then emit one-shot navigation event. TopAppBar with back arrow per CLAUDE.md platform swaps; see `screens/debug/DesignSystemGalleryScreen.kt` (`DesignSystemGalleryScreen(onBack = ...)`) for a pushed screen with back arrow. Email and code come from the `NavKey` args, not a shared VM; `SavedStateHandle` is not needed unless keys are read in the VM (entry-scoped `hiltViewModel()` with `creationCallback` or pass through screen params).

### `$M/screens/signup/SignUpPlaceholderScreen.kt` and `passwordchanged/PasswordChangedScreen.kt`

**Analog:** `$M/screens/signup/SignupSetupPlaceholder.kt` (lines 28-52). Copy the Column/safeDrawing/`EmptyState` layout.
```kotlin
Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
        .windowInsetsPadding(WindowInsets.safeDrawing).padding(vertical = Spacing.xl),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Spacing.xl, Alignment.CenterVertically),
) {
    EmptyState(icon = Icons.Outlined.AssignmentInd, heading = stringResource(...), body = stringResource(...))
    OutlinedButton(onClick = onLogout) { Text(stringResource(R.string.profile_log_out)) }
}
```
Add all copy to `strings.xml` (UI-SPEC: nothing hard-coded).

### `$M/navigation/AuthNavHost.kt` + `AuthKeys.kt`

**Analog:** `screens/clientmode/home/HomeClientScreen.kt` lines 71-121 plus `navigation/HomeTabScaffold.kt` lines 106-128, `Navigator.kt`, `NavigationState.kt`.

State + navigator + entry provider:
```kotlin
val state = rememberNavigationState(LoginKey, setOf(LoginKey))
val navigator = remember(state) { Navigator(state) }
val entries = state.toDecoratedEntries(
    entryProvider<NavKey> {
        entry<LoginKey> { LoginScreen(...) }
        entry<ForgotPasswordKey> { ... }
    },
)
NavDisplay(entries = entries, onBack = { navigator.goBack() },
    transitionSpec = { slide(forward = true) }, popTransitionSpec = { slide(forward = false) },
    predictivePopTransitionSpec = { slide(forward = false) })
```
Key declaration pattern (`screens/debug/DesignSystemGalleryScreen.kt:28`): `data object DesignSystemGalleryKey : NavKey`; for args use `@Serializable data class ForgotPasswordKey(val email: String) : NavKey`. `navigator.navigate(key)` pushes any key not in `backStacks.keys`; `navigator.reselect(LoginKey)` pops to root (Navigator.kt lines 65-70). For D-11 wrap `onBack`: if top key is `PasswordChangedKey`, run the go-to-login function. Extract private `slide(forward)` (HomeTabScaffold.kt lines 40-46) to a shared `internal` file; update `HomeTabScaffold` to import it.

### `$M/ClickMeApp.kt`

Replace line 36: `AppRoute.Login -> LoginScreen(onLoggedIn = appViewModel::onLoggedIn)` with `AuthNavHost(onLoggedIn = appViewModel::onLoggedIn)`; drop the "Temporary login" comment.

### `$M/components/ValidatedField.kt` (modify)

Add optional params at the end of the signature (after `enabled`), default null, per Pitfall 12: `trailingBadge: (@Composable () -> Unit)? = null`, `contentType: ContentType? = null`. Existing hook points:
- semantics (lines 142-145): `if (isSecure) contentType = ContentType.Password` becomes `contentType ?: ...`. Note the local param name shadows the semantics property `contentType`; import-alias or rename the parameter (`autofillType`).
- trailing slot (lines 172-190): currently `if (isSecure) { IconButton... } else null`; wrap in a `Row` with the badge before the toggle.

### Public endpoints: `ApiRequestUrls.kt`, `ClickMeApi.kt`, `AuthRequests.kt`, `SuccessMessageResponse.kt`

`ApiRequestUrls.kt` lines 9-14: add `const val FORGOT_PASSWORD = "auth/password/forgot"` and `RESET_PASSWORD = "auth/password/reset"`. `publicPaths` (lines 21-28) already lists both, so no bearer.

`ClickMeApi.kt` lines 23-26 pattern:
```kotlin
@POST(ApiRequestUrls.LOGIN)
suspend fun login(@Body body: LoginRequest): SuccessDataResponse<LoginData>
```
`AuthRequests.kt` lines 6-17 pattern (`@JsonClass(generateAdapter = true)`, `@Json(name = "refresh_token")` on every multi-word field):
```kotlin
@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(@Json(name = "refresh_token") val refreshToken: String)
```
`SuccessMessageResponse`: copy the `SuccessDataResponse.kt` style but with nullable `status`/`message` (read that file before writing). Update the `ClickMeApi` kdoc ("Phase 2 endpoints").

### `$M/realtime/RealtimeEvents.kt`, `RealtimeEventDecoder.kt`

**Analog:** DTO conventions from `AuthRequests.kt` (above); Moshi from `network/MoshiProvider.kt` (`MoshiProvider.moshi`, includes `InstantAdapter`). Use the research "Event decoder" code (RESEARCH.md lines 501-535) as the main template; no existing sealed-event decoder in the repo. Sealed interface style reference: `managers/SessionEvents.kt` lines 11-20 (`sealed interface` + `data object`).

### `$M/realtime/RealtimeService.kt`

**Analog:** `managers/SessionEvents.kt` for flow construction (lines 32-37):
```kotlin
private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
val events: SharedFlow<SessionEvent> = _events.asSharedFlow()
fun emit(event: SessionEvent) { _events.tryEmit(event) }
```
Class style: `@Singleton class X @Inject constructor(...)`. Use `@Named("bare") OkHttpClient` (see `NetworkModule.kt` lines 39-42). Socket body: RESEARCH.md "RealtimeService" (lines 422-499), verified by experiments.

### `$M/realtime/RealtimeConnectionManager.kt`

**Analog:** `managers/UserManager.kt` (`@ApplicationScope` scope, `@IoDispatcher` dispatcher injection, `onSessionEvent` collection at line 89, `withContext(io)` for token store reads at line 108). Qualifiers in `di/Qualifiers.kt`: `@IoDispatcher`, `@ApplicationScope`. Use `userManager.isLoggedIn` (StateFlow, line 54), `userManager.refreshMe()` (line 265) for the D-15 probe, `SessionEvents.events`. Body: RESEARCH.md lines 548-592. Constructor delays as parameters for virtual-time tests.

### `$M/di/RealtimeModule.kt`

**Analog:** `di/SessionModule.kt` and `di/NetworkModule.kt`: `@Module @InstallIn(SingletonComponent::class) object`, `@Provides @Singleton`. Provide `RealtimeConfig(socketUrl = socketUrl(AppConfig.baseUrl))` (NetworkModule line 48/60 use `AppConfig.baseUrl`); `@Binds` (abstract module) or `@Provides` for `RealtimeConnection` -> `RealtimeService` and `ProcessForeground`.

### `$M/ClickMeApplication.kt`

Currently `@HiltAndroidApp class ClickMeApplication : Application()`. Add `@Inject lateinit var realtimeManager: RealtimeConnectionManager` and `override fun onCreate() { super.onCreate(); realtimeManager.start(); RealtimeDebugLogger.install(...) }`. Hilt injects in `super.onCreate()`.

### `app/src/debug|release/.../realtime/RealtimeDebugLogger.kt`

**Analog:** `app/src/debug/java/.../network/DebugNetwork.kt` (lines 12-20) and its release twin in `app/src/release/...network/DebugNetwork.kt`: same `object`, same signature, release returns nothing. Log tag `"Realtime"`; never print payloads or tokens (DebugNetwork comment on redaction).

### Tests

- `LoginViewModelTest.kt` (lines 33-79): keep `MainDispatcherRule`, `TemporaryFolder`, the `Harness` (real `UserManager` with `FakeClickMeApi`, `FakeSecureTokenStore`, `SessionEvents`, `testPreferencesDataStore`), `api.loginHandler`, `api.httpError(status, json)`. Rewrite assertions for new copy and error placement.
- `FakeClickMeApi.kt` lines 22-66: add `var forgotPasswordHandler: suspend (ForgotPasswordRequest) -> SuccessMessageResponse = { notStubbed() }`, `@Volatile var forgotPasswordCalls`, overrides incrementing counters. Return the response directly (the new endpoints return `SuccessMessageResponse`, not `ok(...)`). Required for compilation.
- `ClickMeApiTest.kt` lines 19-70: MockWebServer + plain `OkHttpClient()` + `server.takeRequest()`; extend with body assertions and `assertNull(request.getHeader("Authorization"))`.
- Realtime manager tests: virtual time as in `UserManagerTest.kt`; fakes modeled on `FakeSecureTokenStore.kt`.
- Protocol test: MockWebServer `webSocketUpgrade`, RESEARCH.md lines 623-640; also look at `network/TokenAuthenticatorTest.kt` for MockWebServer setup/teardown.
- Navigation tests: `navigation/NavigatorTest.kt` and `HomeTabScaffoldTest.kt` for Robolectric Compose style.

## Shared Patterns

### Error handling and messages
**Source:** `$M/network/ApiCall.kt` (`apiCall { }` maps all failures to `ApiException`, rethrows `CancellationException`) and `network/ApiException.kt` (`Http.status/code/serverMessage/fieldErrors/userMessage`, `Network`, `Decoding`).
**Apply to:** Login, Forgot, SetNewPassword ViewModels. Catch `ApiException` only.

### Hilt ViewModel screen wrapper
**Source:** `LoginScreen.kt` lines 39-60 / `HomeClientScreen.kt` lines 37-54 (`hiltViewModel()` default param, `collectAsStateWithLifecycle`, stateless inner composable that renders under Robolectric).
**Apply to:** all new screens.

### Session scope and dispatchers
**Source:** `$M/di/Qualifiers.kt`, `SessionModule.kt` lines 38-45. **Apply to:** realtime manager and module.

### Moshi DTOs
**Source:** `model/requests/AuthRequests.kt`. `@JsonClass(generateAdapter = true)` and `@Json(name=...)` on every multi-word field (reflection-free; R8-safe).

### Debug/release source-set split
**Source:** `DebugNetwork.kt` in `src/debug` and `src/release`.

### Guard rails
No `Color(0x...)` literals outside `ui/theme/Color.kt` (use `MaterialTheme.colorScheme` / `MaterialTheme.extras`, e.g. `extras.destructive`, `extras.onSurfaceMuted` as in `ValidatedField`); spacing from `Spacing`, motion from `Motion`; ktlint max line 120; all strings in `strings.xml`.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `AuthValidators.kt`, `FieldError.kt` | utility | transform | No validators exist yet; port iOS `LoginViewModel` rules (RESEARCH.md lines 610-621) |
| `ProcessForeground.kt` | provider | event-driven | No lifecycle observers in repo; use RESEARCH.md Pitfall 8 (`ProcessLifecycleOwner.get().lifecycle.currentStateFlow`, created on Main) |
| `PasswordStrengthMeter.kt` | component | transform | No meter component; port iOS `PasswordStrength.evaluate`, style like `ValidatedField` with `MaterialTheme.extras` |
| `RealtimeService.kt` socket internals | service | streaming | No socket code exists; use verified RESEARCH.md code |

## Metadata

**Analog search scope:** `app/src/main`, `app/src/debug`, `app/src/release`, `app/src/test`
**Files scanned:** about 110 Kotlin files listed, 20 read
**Pattern extraction date:** 2026-10-20
