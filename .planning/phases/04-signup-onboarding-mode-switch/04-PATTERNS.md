# Phase 4: Signup, Onboarding & Mode Switch - Pattern Map

**Mapped:** 2026-10-20
**Files analyzed:** 36 (new/modified, grouped)
**Analogs found:** 33 / 36

Base path: `app/src/main/java/com/uptrendinvestments/clickmeapp/` (written `M/`); tests `app/src/test/java/com/uptrendinvestments/clickmeapp/` (written `T/`).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `M/model/requests/SignupRequests.kt` (SignupRequest, EmailVerifyRequest, ExpertSetupRequest) | model | request-response | `M/model/requests/AuthRequests.kt` | exact |
| `M/model/responses/ExpertProfileData.kt` (MODIFY, expand) | model | request-response | itself + `M/model/responses/LoginData.kt` | exact |
| `M/model/responses/` SignupData, EmailStatusData, LanguagesData, ExpertiseTagsData, AvatarUploadData | model | request-response | `M/model/responses/LoginData.kt` | exact |
| `M/network/ApiRequestUrls.kt` (MODIFY) | config | request-response | itself | exact |
| `M/network/ClickMeApi.kt` (MODIFY: 10 endpoints, one multipart) | service | request-response / file-I/O | itself (`login`, `getExpertProfile`) | exact (multipart has none) |
| `T/testing/FakeClickMeApi.kt` (MODIFY) | test util | request-response | itself | exact |
| `M/managers/UserManager.kt` (MODIFY: `signup`, `setMode` follow, setup-status check, expert-gate) | service | CRUD | `login()` / `setMode()` in same file | exact |
| `M/AppViewModel.kt` (MODIFY: react to `currentMode`, post-publish route, remove `debugSwitchScaffold`) | provider | event-driven | itself (`isLoggedIn` collector, `onLoggedIn`) | exact |
| `M/ClickMeApp.kt` (MODIFY: SignupSetup route, mode-switch, onboarding) | component | request-response | itself | exact |
| `M/screens/signup/initial/SignupInitialViewModel.kt` | viewmodel | request-response | `M/screens/login/LoginViewModel.kt` | exact |
| `M/screens/signup/initial/SignupInitialScreen.kt` | component | request-response | `M/screens/login/LoginScreen.kt` | exact |
| `M/screens/login/AuthValidators.kt` (MODIFY: `signupPassword`) | utility | transform | itself (`newPassword`) | exact |
| `M/navigation/AuthKeys.kt`, `AuthNavigator.kt`, `AuthNavHost.kt` (MODIFY: SignUpKey -> Initial; delete placeholder) | route | event-driven | themselves | exact |
| `M/navigation/SignupSetupKeys.kt`, `SignupSetupNavHost.kt` (hub back stack under `AppRoute.SignupSetup`) | route | event-driven | `AuthKeys.kt` + `AuthNavHost.kt` | role-match |
| `M/screens/signup/SignupAccumulator.kt` / setup-scoped repository (port of `SignupAccumulator.swift`) | service/store | CRUD (in-memory + PATCH) | `M/managers/UserManager.kt` (state flows) | partial |
| `M/screens/signup/overview/SignupOverviewScreen/ViewModel.kt` (hub) | component+viewmodel | request-response | `LoginViewModel` + `LoginScreen` (structure) | role-match |
| `M/screens/signup/basicinfo/*`, `timezone/*`, `tags/*` (steps) | component+viewmodel | CRUD | `ForgotPasswordViewModel` (SaveState save flow) | role-match |
| `M/screens/signup/verifyemail/*` (send on tap, 30s cooldown) | viewmodel | request-response, timer | `ForgotPasswordViewModel` + `SetNewPasswordViewModel` (OTP) | role-match |
| `M/screens/signup/profilephoto/*` + crop screen | component | file-I/O | `M/components/avatar/Avatar.kt` (display only) | partial |
| `M/screens/signup/review/*` (publish) | component+viewmodel | request-response | `ForgotPasswordViewModel.send()` | role-match |
| `M/constants/Countries.kt`, `Timezones.kt` | utility | transform | none (static data) | none |
| `M/screens/settings/modeswitching/ModeSwitchScreen/ViewModel.kt` | component | event-driven | `M/screens/login/LoginScreen.kt` + `AuthTopAppBar` | role-match |
| `M/screens/onboarding/OnboardingScreen/ViewModel.kt` (4-page carousel) | component | event-driven | `PasswordChangedScreen.kt` (static screen) | partial |
| `M/screens/clientmode/home/HomeClientScreen.kt`, `expertmode/home/HomeExpertScreen.kt` (MODIFY: avatar click, carousel gate) | component | event-driven | themselves | exact |
| `M/screens/signup/SignupSetupPlaceholder.kt`, `SignUpPlaceholderScreen.kt` (DELETE) | - | - | - | - |
| `res/values/strings_signup.xml`, `strings_onboarding.xml`, `strings_mode_switch.xml` | config | - | `strings_login.xml`, `strings_forgot_password.xml` | exact |
| Tests: `T/screens/signup/**ViewModelTest/ScreenTest`, `ApiRequestUrlsTest`, `UserManagerTest`, `AppViewModelTest` | test | - | `LoginViewModelTest`, `LoginScreenTest`, `ApiRequestUrlsTest` | exact |

## Pattern Assignments

### `SignupInitialViewModel.kt` (viewmodel, request-response)

**Analog:** `M/screens/login/LoginViewModel.kt` (lines 1-165). Copy wholesale; swap `login` for `userManager.signup`.

**Structure** (lines 19-57): `enum class XField`, `data class XUiState(... emailError: FieldError?, passwordError: FieldError?, dialogError: FieldError?, isSubmitting, didAttemptSubmit)`, `@HiltViewModel class ... @Inject constructor(userManager, savedStateHandle)`, `_uiState/uiState`, `_destination`, `_focusRequest`. Only email goes to `SavedStateHandle`; the password stays in memory.

**Validation timing** (lines 58-77): no validation until first submit, then live re-validate per edit:
```kotlin
emailError = if (state.didAttemptSubmit) AuthValidators.loginEmail(value) else null,
```
**Submit** (lines 104-136):
```kotlin
if (current.isSubmitting) return
val emailError = AuthValidators.loginEmail(current.email)
...
if (emailError != null || passwordError != null) {
    _uiState.update { it.copy(didAttemptSubmit = true, emailError = emailError, passwordError = passwordError) }
    _focusRequest.value = if (emailError != null) LoginField.Email else LoginField.Password
    return
}
...
viewModelScope.launch {
    try {
        val user = userManager.login(current.email.trim(), current.password)   // trim email
        _destination.value = ...
    } catch (e: ApiException) { handle(e) } finally { _uiState.update { it.copy(isSubmitting = false) } }
}
```
**Error split** (lines 139-160); extend for D-02: 409 -> `emailError`, 422 -> `e.fieldMessage("email")` / `fieldMessage("password")` per field, else `dialogError = e.toDialogError()`:
```kotlin
e.code == ErrorCode.AccountSuspended -> return
e.status == 422 -> { val message = e.fieldMessage("email"); if (message != null) {...; return} }
...
_uiState.update { it.copy(dialogError = e.toDialogError()) }
```
Success: emit a one-shot signal; per D-01 the nav caller replaces the root with `AppRoute.SignupSetup` (use `AppViewModel.onLoggedIn(LaunchDestination.SignupSetup)`; `toRoute()` already maps it).

### `SignupInitialScreen.kt` (component, request-response)

**Analog:** `M/screens/login/LoginScreen.kt`. Pattern: Hilt-aware wrapper (lines 66-103) + stateless `...Content(state, callbacks)` (lines 106-203) for Robolectric.
- Wrapper: `collectAsStateWithLifecycle()`, `LaunchedEffect(destination) { destination?.let { onX(it); viewModel.onDestinationConsumed() } }`.
- Content: `Box(fillMaxSize + background(surface)) { GreenGlowBlobLayer(Modifier.matchParentSize()); Column(windowInsetsPadding(WindowInsets.safeDrawing).imePadding().verticalScroll(...)) }`.
- Fields: `ValidatedField(value, onValueChange, placeholder, leadingIcon, error = state.emailError?.asString(), modifier = Modifier.focusRequester(f), isSecure = true, keyboardOptions, keyboardActions, enabled = !state.isSubmitting, contentType = ContentType.EmailAddress)`.
- Submit: `AsyncSaveButton(title, state = if (state.isSubmitting) SaveState.Saving else SaveState.Idle, onClick = onSubmit)`.
- Error dialog: `state.dialogError?.let { AuthErrorDialog(message = it.asString(), onDismiss = onDismissDialog) }` (line 202).
- Show `PasswordStrengthMeter` under the password field when non-empty (copy usage from `screens/login/setnewpassword/SetNewPasswordScreen.kt`).
- Top bar: `AuthTopAppBar(title, onBack)` from `M/screens/login/AuthTopAppBar.kt`.

### `AuthValidators.signupPassword` (utility, transform)

**Analog:** `AuthValidators.newPassword` (lines 35-42) and the `DIGIT` regex at line 8. D-02 rule is length>=8 + letter + digit (not symbol):
```kotlin
password.isEmpty() -> FieldError.Res(R.string.error_new_password_required)
password.length < MIN_PASSWORD_LENGTH -> FieldError.Res(R.string.error_password_too_short)
!DIGIT.containsMatchIn(password) || !LETTER.containsMatchIn(password) -> FieldError.Res(R.string.error_signup_password_rules)
```
Reuse `emailError(raw, requiredId)` (private, lines 57-67) via a new `signupEmail` fun. Add test cases to `T/screens/login/AuthValidatorsTest.kt`.

### Network: DTOs, `ApiRequestUrls`, `ClickMeApi`

**DTO analog:** `M/model/requests/AuthRequests.kt` and `M/model/responses/LoginData.kt`:
```kotlin
@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(val email: String, val code: String, val password: String,
    @Json(name = "password_confirmation") val passwordConfirmation: String)
```
Always `@JsonClass(generateAdapter = true)` + `@Json(name = "snake_case")`. Signup response reuses the `LoginData` shape (token, refresh_token, expires_at, user); add `email_verified` as nullable if the server returns it (D-09).
`ExpertProfileData` currently has only `@Json(name = "setup_completed") val setupCompleted: Boolean`; expand with nullable fields (name, location, timezone, languages, tags, avatar) so absent keys do not break `UserManager.isExpertProfileIncomplete`. D-25: setup PATCH request must omit untouched lists. Moshi omits nulls by default, so use nullable `List<String>?` defaults of null in `ExpertSetupRequest`; `[]` only when explicitly emptied.

**URL analog** (`ApiRequestUrls.kt` lines 9-16): add `const val SIGNUP = "auth/signup"`, `EMAIL_RESEND = "auth/email/resend"`, `EMAIL_VERIFY = "auth/email/verify"`, `EMAIL_STATUS = "auth/email/status"`, `EXPERT_PROFILE_SETUP = "expert/profile/setup"`, `META_LANGUAGES`, `META_EXPERTISE_TAGS`, `USER_AVATAR = "user/profile/avatar"`. `/auth/signup` is already in `publicPaths` (line 26); email endpoints stay authed (see KDoc lines 18-22). Add each to `T/network/ApiRequestUrlsTest.kt` (the `otherPaths_needAuthToken` list at line ~21 already contains the email paths).

**Retrofit analog** (`ClickMeApi.kt` lines 26-53):
```kotlin
@POST(ApiRequestUrls.LOGIN)
suspend fun login(@Body body: LoginRequest): SuccessDataResponse<LoginData>
@GET(ApiRequestUrls.EXPERT_PROFILE)
suspend fun getExpertProfile(): SuccessDataResponse<ExpertProfileData>
```
Add imports `retrofit2.http.PATCH`, `DELETE`, `Multipart`, `Part`. No existing multipart: use `@Multipart @POST(USER_AVATAR) suspend fun uploadAvatar(@Part image: MultipartBody.Part): SuccessDataResponse<AvatarData>` (field name `image`, D-12). Bodiless status calls return `SuccessMessageResponse` (see `forgotPassword`).

**Fake analog** (`T/testing/FakeClickMeApi.kt` lines 24-87): per endpoint one `var xHandler: suspend (Req) -> Data = { notStubbed() }`, one `@Volatile var xCalls = 0; private set`, one `override suspend fun` that increments, calls the handler, wraps with `ok(...)`. Message-only endpoints return the handler result directly. Keep `httpError(status, json)` for 409/422/429 simulation. Fake must implement every new interface method or compilation breaks.

### `UserManager.kt` additions

**`signup` analog:** `login()` (lines 104-117) - store tokens the same way:
```kotlin
val data = apiCall { api.signup(SignupRequest(email, password, "expert")) }.data
withContext(io) { tokens.save(TokenPair(data.token, data.refreshToken, data.expiresAt)) }
_authUser.value = data.user
_isLoggedIn.value = true
_wasSuspended.value = false
meStale = false
appScope.launch { bestEffort { refreshProfile() } }
return data
```
Beware: `AppViewModel`'s `isLoggedIn` collector only reacts to `false`, so flipping to true is safe.
**Mode:** `setMode` (lines 284-287) persists; D-22 publish calls `setMode(AppMode.Client)`; `markOnboardingShown` (291-294) for the carousel. `_currentMode` is already a `StateFlow` to collect in `AppViewModel`.
**Setup check (D-11/D-17):** `isExpertProfileIncomplete()` (lines 224-229) is private and swallows `ApiException` as "complete". Expose a public variant and reuse it for the mode-switch gate; for the foreground re-check hook next to `refreshIfStale()` (251-261), triggered from `MainActivity.kt:38` `repeatOnLifecycle(STARTED) { appViewModel.onAppForegrounded() }`.
**Tests:** `T/managers/UserManagerTest.kt` and the `Harness` pattern in `LoginViewModelTest` lines 38-70 (real `UserManager` built from `FakeClickMeApi`, `FakeSecureTokenStore(null)`, `SessionStore(testPreferencesDataStore(scope.backgroundScope, tmp))`, `PushTokenRegistrar()`).

### `AppViewModel.kt` (provider, event-driven)

Existing collector pattern (lines 44-51):
```kotlin
viewModelScope.launch {
    userManager.isLoggedIn.collect { loggedIn ->
        if (!loggedIn && _route.value in SESSION_ROUTES) _route.value = AppRoute.Login
    }
}
```
Add a second collector on `userManager.currentMode` that swaps `ClientHome` <-> `ExpertHome` when the route is one of those two, and a `onModeSwitchRequested(AppMode)` doing the D-17 gate (expert + incomplete -> `AppRoute.SignupSetup`; else `userManager.setMode`). Remove `debugSwitchScaffold` (lines 94-101) and its callers in `HomeClientScreen`/`HomeExpertScreen`/`ClickMeApp.kt` lines 41-44; keep the debug gallery buttons. Mapping helper `LaunchDestination.toRoute()` lines 103-112. Tests: `T/AppViewModelTest.kt`.

### Signup setup back stack (route, event-driven)

**Analog:** `M/navigation/AuthKeys.kt` + `AuthNavHost.kt` + `AuthNavigator.kt`.
- Keys: `@Serializable data object XKey : NavKey` / `data class XKey(val email: String) : NavKey` (AuthKeys lines 11-34). Never put passwords in keys.
- Host: `rememberNavigationState(RootKey, setOf(RootKey))`, `Navigator(state)`, wrapper navigator, `entryProvider<NavKey> { entry<XKey> { XScreen(...) } }`, and a Hilt-free `NavDisplay` function taking `entryProvider` for tests (AuthNavHost lines 28-108), transitions `slideTransition(forward = true/false)`.
- Navigator wrapper: `pushUnlessOnTop(key)` double-tap guard (AuthNavigator lines 57-60), `back() = navigator.goBack()`.
- Hub root should be non-popable: system Back at the hub root exits the app (as `handleBack` does at the Login root).
- Shared accumulator: Navigation 3 entries get their own ViewModel scope from decorators (AuthNavHost KDoc lines 21-25), so a shared setup-scoped state must be a `@Singleton`/`@ViewModelScoped`-free repository injected into each step ViewModel, or a ViewModel obtained from the activity store. Planner decision (CONTEXT Discretion).
- Replace `SignUpKey` entry (AuthNavHost lines 78-81) with `SignupInitialScreen`; delete `SignUpPlaceholderScreen.kt`. In `ClickMeApp.kt` line 38 replace `SignupSetupPlaceholder(onLogout = ...)` with the new host. Update `T/navigation/AuthNavigatorTest.kt`, `AuthNavDisplayTest.kt`, `T/screens/RootScreensTest.kt` which reference the placeholder.

### Step ViewModels (basic info, timezone, tags, photo, review/publish) (viewmodel, CRUD)

**Analog:** `M/screens/login/forgetpassword/ForgotPasswordViewModel.kt` - save flow with `SaveState` (lines 76-106):
```kotlin
_uiState.update { it.copy(..., saveState = SaveState.Saving) }
viewModelScope.launch {
    try {
        apiCall { api.forgotPassword(...) }
        _uiState.update { it.copy(saveState = SaveState.Saved) }
        delay(Motion.SAVED_CONFIRMATION_HOLD_MS.toLong())
        _navigateToReset.value = identity
    } catch (e: ApiException) {
        val inline = (e as? ApiException.Http)?.fieldMessage("identity", "email")
        _uiState.update { it.copy(saveState = SaveState.Idle,
            emailError = inline?.let { m -> FieldError.Text(m) } ?: it.emailError,
            dialogError = e.toDialogError()) }
    }
}
```
Plus one-shot nav `StateFlow` with `onNavigated()` reset (lines 50-51, 111-114), `SavedStateHandle` mirroring (lines 33, 66-67), `fun dismissDialog()`.
- Publish 422 `missing_fields` (D-06): parse via `ApiException.Http.fieldErrors`/`fieldMessage` and map field names to hub-step strings, then show `AuthErrorDialog`.
- Save buttons: `AsyncSaveButton`. Dialog: `AuthErrorDialog`. Top bars: `AuthTopAppBar`.

### Verify Email (viewmodel, request-response + timer)

**Analog:** `ForgotPasswordViewModel` (send) + `screens/login/setnewpassword/SetNewPasswordViewModel.kt` (6-digit code field, uses `AuthValidators.sanitizeCode` line 55 and `resetCode` lines 26-33). Cooldown: coroutine `delay(1000)` loop in `viewModelScope` (tests use `MainDispatcherRule`/`runTest`). D-08: on `ApiException.Http` with `status == 429` treat as sent and restart countdown; 422 `INVALID_CODE` inline via `fieldMessage`/`userMessage` as in `LoginViewModel.handle`. Check `M/model/enums/ErrorCode.kt` for adding `RateLimited`/`InvalidCode`.

### Profile photo + crop (component, file-I/O)

No close analog (no image picker or multipart yet). Use `rememberLauncherForActivityResult(PickVisualMedia)` (CLAUDE.md swap); display via `M/components/avatar/Avatar.kt` (Coil 3). Crop = custom Compose `Canvas` + `detectTransformGestures`; output square ~1024 px JPEG q85 -> `MultipartBody.Part.createFormData("image", ...)`. Use RESEARCH.md code examples for this file.

### Mode switch (component, event-driven)

**Analog:** `AuthTopAppBar` (title + back arrow `TopAppBar` with `Icons.AutoMirrored.Filled.ArrowBack`, `a11y_back`) inside the `LoginContent` screen layout (Box + surface background + `safeDrawing`). Pushed as a full-screen page from the home shells: `HomeClientShell` already hosts `navigator.navigate(DesignSystemGalleryKey)` / `entry<DesignSystemGalleryKey> { Screen(onBack = navigator::goBack) }` (HomeClientScreen lines 103, 117-119); copy this for `ModeSwitchKey`. Avatar click: replace `onAvatarClick = {}` at HomeClientScreen line 52 and HomeExpertScreen line 52 (the `actions = { TopBarAvatarButton(avatarUrl, onAvatarClick) }` slot, line 84).

### Onboarding carousel (component, event-driven)

Closest: `screens/login/passwordchanged/PasswordChangedScreen.kt` (static full-screen) for layout; use `HorizontalPager` for swiping. Gate in `HomeClientScreen`: collect `userManager.hasSeenOnboarding` through `HomeClientViewModel` (existing `avatarUrl`/`logout` flows, `collectAsStateWithLifecycle()`), and call `markOnboardingShown()` on Skip/Get Started. Stateless content composable for Robolectric tests.

### Strings

**Analog:** `res/values/strings_login.xml`, `strings_forgot_password.xml`, `strings_set_new_password.xml` (one per feature). Create `strings_signup.xml`, `strings_mode_switch.xml`, `strings_onboarding.xml`. Reuse existing `error_email_*`, `error_password_*`, `login_error_unknown/network`.

### Tests

- ViewModel: `T/screens/login/LoginViewModelTest.kt` - `@get:Rule MainDispatcherRule()`, `TemporaryFolder`, inner `Harness(scope)` building real `UserManager`, `runTest(dispatcherRule.testDispatcher)`, `api.xHandler = { throw api.httpError(409, """{"status":"error","message":"..."}""") }`.
- Screen: `T/screens/login/LoginScreenTest.kt` - `@RunWith(RobolectricTestRunner::class)`, `createComposeRule()` (`junit4.v2`), `ClickMeTheme { XContent(state = ..., onX = { count++ }) }`, `onNode(hasText("Login") and hasClickAction()).performClick()`.
- URL: `T/network/ApiRequestUrlsTest.kt` list-driven assertions.

## Shared Patterns

### Error handling (all ViewModels)
**Source:** `M/screens/login/FieldError.kt` - `FieldError.Res/Text`, `FieldError.asString()`, `ApiException.toDialogError()` (returns null for `ACCOUNT_SUSPENDED`, the global dialog owns it), `ApiException.Http.fieldMessage(vararg fields)`. Pattern: 401/409/422 inline; else `dialogError = e.toDialogError()`. Network calls go through `apiCall { api.x() }` (`M/network/ApiCall.kt`).

### Form state
**Source:** `LoginViewModel` - `MutableStateFlow` + `update { it.copy() }`, `didAttemptSubmit` gates live validation, `isSubmitting` guard on re-entry, one-shot `StateFlow` events consumed by `LaunchedEffect` then `onXConsumed()`.

### Screen split
**Source:** `LoginScreen.kt` - Hilt wrapper + stateless `Content` composable; `hiltViewModel()` default param; `collectAsStateWithLifecycle`.

### Theme/layout
**Source:** `LoginScreen.kt` - `Spacing.*`, `MaterialTheme.extras.onSurfaceMuted`, `GreenGlowBlobLayer`, `WindowInsets.safeDrawing` + `imePadding()`.

### Hilt
`@HiltViewModel class X @Inject constructor(...) : ViewModel()`; ViewModels that need only the public API inject `ClickMeApi` directly (ForgotPasswordViewModel); session-affecting calls go through `UserManager`.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `Countries.kt`, `Timezones.kt` | utility | transform | Static lists; port from `ios-reference/Constants/Countries.swift` and `SignupTimezoneViewModel.swift` |
| Photo crop screen + image downscale | component | file-I/O | No image picking/cropping/multipart code exists; use RESEARCH.md |
| `HorizontalPager` onboarding carousel | component | event-driven | No pager in the codebase |
| Multipart `@Part` Retrofit endpoint | service | file-I/O | No multipart endpoint yet |

## Metadata

**Analog search scope:** `app/src/main/java/.../{screens,navigation,managers,network,model,components}`, `app/src/test/java/...`, `app/src/main/res/values`
**Files scanned:** ~150 listed, ~20 read in full
**Pattern extraction date:** 2026-10-20
