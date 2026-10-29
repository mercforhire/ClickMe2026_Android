# Phase 14: Payouts & Account Settings - Pattern Map

**Mapped:** 2026-10-28
**Files analyzed:** 27 (new or modified, grouped)
**Analogs found:** 25 / 27

All analog paths are relative to `app/src/main/java/com/uptrendinvestments/clickmeapp/` (call it `$M`) unless they start with `app/src/test`. All were checked as git-tracked source.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `network/ApiRequestUrls.kt` (mod: 8 consts) | config | n/a | same file, lines 21-33, 90-93 | exact |
| `network/ClickMeApi.kt` (mod: ~10 endpoints incl. `@HTTP` DELETE) | service | request-response | same file, lines 150-178, 364-368, 462-479 | exact |
| `model/requests/AccountRequests.kt`, `ProfileRequests.kt`, `TaxProfileRequests.kt` (new) | model | request-response | `model/requests/ExpertRequests.kt`, `AuthRequests.kt` | exact |
| `model/responses/EarningsTransactionsData.kt`, `TaxProfileData.kt`, `FaqData.kt`, `DeletionRequestData.kt` (new) | model | request-response | `model/responses/PaymentMethodsData.kt`, `ExpertFinancialData.kt`, `PaginationMeta.kt` | exact |
| `managers/CallDialog.kt` (mod: add `ChangePassword`, `DeleteAccount`) | config | event-driven | same file lines 11-19 | exact |
| `screens/expertmode/ExpertLaunchRequest.kt` (mod: `connectReturn` slot) | store | pub-sub | same file lines 51-58 | exact |
| `screens/expertmode/payouts/ConnectReturnActivity.kt` + manifest (new) | controller | event-driven | `MainActivity.kt` push-intent handling (no true analog) | partial |
| `screens/settings/profilehub/ClientProfileHomeScreen.kt`, `ExpertProfileHomeScreen.kt` (new) | component | request-response | `screens/settings/paymentmethods/PaymentMethodsScreen.kt` + `components/ProfileMenuRow.kt` | role-match |
| `screens/clientmode/home/HomeClientScreen.kt` (mod, footer ~L309-345, add `entry<>`) | route | request-response | same file | exact |
| `screens/expertmode/home/HomeExpertScreen.kt` (mod, footer ~L215-249, add `entry<ExpertProfileKey>`) | route | request-response | same file; client `detailEntries` L129 | exact |
| `screens/settings/accountsecurity/AccountSecurityViewModel.kt` + Screen + Key (new) | viewmodel | request-response | `screens/login/setnewpassword/SetNewPasswordViewModel.kt` | exact |
| `screens/settings/deleteaccount/DeleteAccountViewModel.kt` + Screen + Farewell + `AccountDeletionState` (new) | viewmodel | request-response | `PaymentMethodsViewModel.kt` (busy guard, CallGuard) + `SetNewPasswordViewModel.kt` | role-match |
| `screens/settings/help/HelpViewModel.kt`, `HelpScreen.kt`, `HelpRepository.kt` (new) | viewmodel | request-response | `PaymentMethodsViewModel.kt` + `PaymentMethodsRepository.kt` | role-match |
| `screens/settings/clientprofile/ClientProfileSettingsViewModel.kt` + Screen (new) | viewmodel | CRUD, debounced | `SignupBasicInfoViewModel.kt` (fields) + `SignupProfilePhotoViewModel.kt` (avatar) | role-match |
| `screens/expertmode/settings/ExpertProfileSettingsViewModel.kt` + Screen (new) | viewmodel | CRUD, debounced | same as above + `ExpertiseEditorSheet.kt` | role-match |
| `screens/expertmode/payouts/PayoutsViewModel.kt`, `PayoutsScreen.kt`, `PayoutsLogic.kt` (new) | viewmodel | request-response | `screens/expertmode/dashboard/ExpertDashboardViewModel.kt` + `DashboardFinancials.kt` | exact |
| `screens/expertmode/payouts/TransactionsViewModel.kt`, `TransactionsScreen.kt`, `EarningsTransactionRow.kt` (new) | viewmodel | paged list | `ExpertBookingsPayload` list screens (`screens/expertmode/upcomingbookings/`) | role-match |
| `screens/expertmode/taxprofile/*` (new) | viewmodel | CRUD | `PaymentMethodsViewModel` + `SetNewPasswordViewModel` field-error mapping | role-match |
| `screens/expertmode/dashboard/subviews/ExpertToolsGrid.kt`, `ExpertDashboardScreen.kt` (mod: replace `dashboard_coming_soon` snackbars L151-162) | component | event-driven | same files | exact |
| `screens/push/PushDestination.kt`, `PushDestinationResolver.kt` (mod, `PHASE-14` markers) | utility | event-driven | same files | exact |
| `app/src/test/.../screens/settings/**` new tests | test | n/a | `screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt` | exact |

## Pattern Assignments

### Navigation key (all new screens) - analog `screens/settings/paymentmethods/PaymentMethodsKey.kt` (L1-11)
```kotlin
@Serializable
data object PaymentMethodsKey : NavKey
```
For keys reachable from two tab stacks (`PayoutsKey`), add an origin field like `ExpertProfileKey.origin: StackOrigin = StackOrigin.Default` (`screens/clientmode/expertprofile/ExpertProfileKey.kt` L21-31). For self-view add `val isSelfView: Boolean = false` (defaulted).

### Retrofit endpoints - analog `network/ClickMeApi.kt`
Multipart avatar and plain DELETE (L170-178), body PATCH (L364-368):
```kotlin
@PATCH(ApiRequestUrls.EXPERT_PROFILE)
suspend fun updateExpertProfileTags(@Body body: UpdateExpertTagsRequest): SuccessMessageResponse

@Multipart @POST(ApiRequestUrls.USER_AVATAR)
suspend fun uploadAvatar(@Part image: MultipartBody.Part): SuccessDataResponse<AvatarUploadData>
```
New: `@HTTP(method = "DELETE", path = ApiRequestUrls.USER_ACCOUNT, hasBody = true)` for delete (no existing analog; plain `@DELETE` at L177). Constants follow `ApiRequestUrls.kt` (relative path, no leading slash, e.g. `USER_PROFILE = "user/profile"`). Add a separate request type for `meeting_url`; do not extend `UpdateExpertTagsRequest`. A `FakeClickMeApi` handler must be added per endpoint (`app/src/test/.../testing/FakeClickMeApi`).

### Repository (stateless, apiCall) - analog `screens/expertmode/dashboard/ExpertFinancialRepository.kt` (L18-36)
```kotlin
@Singleton
class ExpertFinancialRepository @Inject constructor(private val api: ClickMeApi) {
    suspend fun payoutSummary(): PayoutSummaryData = apiCall { api.getPayoutSummary() }.data
    suspend fun connectStatus(): ConnectStatusData = apiCall { api.getConnectStatus() }.data
    suspend fun onboardingLink(refresh: Boolean): String =
        apiCall { if (refresh) api.refreshConnectLink() else api.startConnectOnboarding() }.data.url
}
```
Copy for `HelpRepository`, `TaxProfileRepository`, `AccountRepository`, and extend this class with `transactions(page, limit)`. Only `ApiException` escapes; never cache or log the URL, token or TIN.

### `ExpertLaunchRequest` slot - analog `screens/expertmode/ExpertLaunchRequest.kt` L51-58
```kotlin
val dashboard = OneShotSlot<DashboardLaunch>()
// add: val connectReturn = OneShotSlot<ConnectReturn>()
```
Define `ConnectReturn` as a sealed interface beside `DashboardLaunch` (L39). The trampoline posts both `dashboard` and `connectReturn`.

### `RestrictedAction` - analog `managers/CallDialog.kt` L11-19
Append `ChangePassword`, `DeleteAccount`; update the KDoc line "Phase 14 still adds ...". Also add the `call_action_*` strings and extend the exhaustive `when` in `CallDialogs.kt` (L45-51); `CallDialogsTest` covers it.

### Form ViewModel with field errors (AccountSecurity, TaxProfile) - analog `SetNewPasswordViewModel.kt`
UiState data class with `FieldError?` per field, `saveState`, `dialogError`, `didAttemptSubmit` (L29-43); in-flight guard and submit (L111-157); HTTP status mapping and 422 field mapping (L171-204):
```kotlin
if (current.saveState != SaveState.Idle) return
...
_uiState.update { it.copy(saveState = SaveState.Saving) }
viewModelScope.launch {
    try { apiCall { api.resetPassword(...) }; _uiState.update { it.copy(saveState = SaveState.Saved) } }
    catch (e: ApiException) { _uiState.update { it.copy(saveState = SaveState.Idle) }; handle(e) }
}
private fun handle(e: ApiException) {
    if (e is ApiException.Http && e.status == 422 && mapFieldErrors(e)) return
    _uiState.update { it.copy(dialogError = e.toDialogError()) }
}
for (fieldError in e.fieldErrors) { when (fieldError.field) { "password" -> ... } }
```
Imports: `components.PasswordStrength`, `components.buttons.SaveState`, `network.{ApiException, apiCall}`, `screens.login.{FieldError, toDialogError, AuthValidators}`. For change password map fields `current_password|new_password`, 403 to current-password error. Secrets live in memory only (do not use `SavedStateHandle`, unlike the reset `code`). Button composable: `components/buttons/AsyncSaveButton.kt` (`SaveState { Idle, Saving, Saved }`).

### Delete account - analog `PaymentMethodsViewModel.kt` (L32-49, busy op guard + `CallGuard`) and `SetNewPasswordViewModel`
Hilt shape to copy:
```kotlin
@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    private val repository: PaymentMethodsRepository,
    private val monotonicClock: MonotonicClock,
    private val callGuard: CallGuard,
) : ViewModel() { private val _uiState = MutableStateFlow(...); val uiState = _uiState.asStateFlow() ...
```
Guard with `callGuard.attempt(RestrictedAction.DeleteAccount)` (see how `PaymentMethodsViewModel` uses it after L130). Map 401/403 to password error, 429 to too-many, 409 to a dialog with the server message. `AccountDeletionState` (new `@Singleton` `StateFlow<Boolean>`) is set at the DELETE 200 and rendered above the route switch in `ClickMeApp` (not inside the shell). No analog exists for root-level farewell hosting.

### Payouts screen - analog `screens/expertmode/dashboard/ExpertDashboardViewModel.kt`, `DashboardFinancials.kt`
Reuse (do not rebuild) `resolvePayoutOnboarding`, `isAllowedOnboardingUrl`, `Money.format`, `requestEarnings()`. Reload on ON_RESUME as the Dashboard does. Launch Custom Tab via `rememberCustomTabLauncher { }` (`components/web/CustomTabLauncher.kt`). Section-error pattern: `toSectionMessage()` (from `screens/clientmode/makeabooking`), as in `PaymentMethodsViewModel` L73-77. Reload sequencing with a generation counter (L45-46, L62-70) applies for racing reloads (return deep link vs ON_RESUME).

### Hub screens - analog `components/ProfileMenuRow.kt` (L36-42) and the current footers
```kotlin
ProfileMenuRow(
    title = stringResource(R.string.payment_methods_title),
    icon = Icons.Outlined.CreditCard,
    onClick = { navigator.navigate(PaymentMethodsKey) },
    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
)
```
Replace the `footer = { ... }` lambdas of `ClientTabRoot(key = ClientProfileRoot)` (HomeClientScreen ~L312) and `ExpertTabRoot(key = ExpertProfileRoot)` (HomeExpertScreen ~L218). Keep the `if (showDebugAffordances) { ... }` block and `onLogout` (wrap in an `AlertDialog` confirmation). Keep the testTag convention (`profile_availability_row`). Nav registration pattern: `entry<ModeSwitchKey>(metadata = BottomBar.hidden()) { ... onBack = navigator::goBack }`; detail screens go into `detailEntries = { navigator -> entry<...> { ... } }` (HomeClientScreen L124-139). `entry<ExpertProfileKey>` exists only in the client shell (L129); copy it to the expert shell, using `hiltViewModel<ExpertProfileViewModel, ExpertProfileViewModel.Factory>(creationCallback = { factory -> factory.create(key) })`.

### Settings screens (auto-save, avatar, shared editors)
No exact analog for debounced auto-save. Copy field and picker handling from `screens/signup/basicinfo/SignupBasicInfoViewModel.kt` and its stateless sheets (`LanguageSelectionSheet`, `CountryPickerSheet`, `PickerField`, `ValidatedField`); avatar from `SignupProfilePhotoViewModel.kt` (L124 `repository.uploadAvatar(bytes)`) plus `PhotoProcessor`/`PhotoCropScreen`; expert expertise via `screens/expertmode/settings/subviews/ExpertiseEditorSheet.kt` (used in `topics/editor/TopicEditorScreen.kt` L139). Upload goes through `SignupSetupRepository.uploadAvatar(jpeg)` (L302-306), which is signup-coupled; add a thin settings repository calling `api.uploadAvatar`. After a successful save call `userManager.refreshProfile()`. Debounce 1.2 s, snapshot compare, flush on leave per RESEARCH Pattern 3.

### Privacy and Help links
`AppLinks.PRIVACY_POLICY` through `rememberCustomTabLauncher`; support row uses `supportEmailIntent()` (`screens/login/SuspendedAccountDialog.kt` L36: `Intent(Intent.ACTION_SENDTO, "mailto:${AppLinks.SUPPORT_EMAIL}".toUri())`) with try/catch `ActivityNotFoundException`.

### Test pattern - analog `app/src/test/.../screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt`
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val api = FakeClickMeApi()
    private val guard = FakeCallGuard()
    init { api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(visa, master)) } }
    private fun serverError(message: String) = api.httpError(500, """{"status":"error","message":"$message"}""")
    @Test fun x() = runTest(main.testDispatcher) { val vm = vm(); runCurrent(); ... }
}
```
Add one handler per new endpoint to `FakeClickMeApi`. Existing tests to update: `PushRouterTest.landsOnNotificationsList` (L103-111), `ExpertDashboardScreenTest`, `CallDialogsTest`.

## Shared Patterns

### Error handling
**Source:** `network/ApiException`, `apiCall {}`, `screens/login/FieldError`, `toDialogError()`, `toSectionMessage()`
**Apply to:** every new ViewModel. Catch only `ApiException`; map `ApiException.Http` by `status` and `fieldErrors`; use `FieldError.Res` or `FieldError.Text` for UI.

### CallGuard
**Source:** `managers/CallGuard.kt`, `RestrictedAction`
**Apply to:** change password, delete account (both step buttons), payout setup (reuse `SetUpPayouts`), logout (already guarded in `HomeExpertViewModel.logout()`).

### Money
**Source:** `util/Money.kt` (`format` for minor units, `formatMajor` for dollars)
**Apply to:** Payouts, Transactions row (cents), earnings breakdown (dollars, via `requestEarnings()`).

### Hilt + Compose state
`@HiltViewModel`, `MutableStateFlow` private + `asStateFlow()`, `viewModelScope.launch`, `.update {}`. Compose-side collection per existing screens.

### Secrets
Passwords, `deletion_token`, TIN, Account Link: memory only, never logged, cleared after use.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `ConnectReturnActivity` + manifest `VIEW/BROWSABLE` filter | controller | event-driven | App has no deep-link filter (manifest only has MAIN/LAUNCHER); follow RESEARCH Pitfall 1 XML. Mirror `MainActivity` `onNewIntent` extras discipline |
| Debounced auto-save settings VMs | viewmodel | CRUD | No existing debounce or auto-save VM; follow RESEARCH Pattern 3 |
| `AccountDeletionState` + root Farewell host | store/component | pub-sub | No root-level overlay outside the route switch except `CallDialogs` (root overlay); use that as the structural hint |
| `@HTTP` DELETE with body | service | request-response | Only plain `@DELETE` exists |

## Metadata

**Analog search scope:** `$M/screens/{settings,login,signup,expertmode,clientmode/home,clientmode/expertprofile}`, `$M/components`, `$M/managers`, `$M/network`, `$M/model`, `app/src/test`
**Files scanned:** ~40 listed, ~12 read
**Pattern extraction date:** 2026-10-28
