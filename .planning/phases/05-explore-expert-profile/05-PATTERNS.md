# Phase 5: Explore & Expert Profile - Pattern Map

**Mapped:** 2026-10-21
**Files analyzed:** 40 (new/modified, grouped)
**Analogs found:** 36 / 40

Base path: `app/src/main/java/com/uptrendinvestments/clickmeapp/` (written `M/`); tests `app/src/test/java/com/uptrendinvestments/clickmeapp/` (written `T/`).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `M/network/ApiRequestUrls.kt` (MODIFY: 5 consts) | config | request-response | itself (Phase 4 block, lines 16-24) | exact |
| `M/network/ClickMeApi.kt` (MODIFY: 5 GET methods) | service | request-response | itself (`getMe`, `getExpertProfile`) | exact |
| `M/model/responses/ClientHomeData.kt`, `CategoryItem.kt`, `PublicProfileDetailsData.kt`, `ExpertReviewsPayload.kt`, `ClientBookingsPayload.kt` | model | request-response | `M/model/responses/MetaPayloads.kt`, `PaginationMeta.kt` | exact |
| `T/testing/FakeClickMeApi.kt` (MODIFY: 5 handlers + counters) | test util | request-response | itself | exact |
| `M/managers/CategoryStore.kt` | store (cache) | CRUD (TTL cache) | `M/managers/SessionStore.kt` | role-match (TTL logic from RESEARCH) |
| `M/di/CategoryCacheModule.kt` + `@CategoryCache` in `M/di/Qualifiers.kt`; `java.time.Clock` in `M/di/ClockModule.kt` | config | n/a | `M/di/SessionModule.kt`, `ClockModule.kt`, `Qualifiers.kt` | exact |
| `M/util/Money.kt` | utility | transform | none (RESEARCH code) | no analog |
| `M/constants/CategoryIconMap.kt` | utility | transform | `M/constants/Countries.kt` (static table) | partial |
| `M/components/{RatingStars,ExpertTagChip,SkeletonBlock,SectionHeader,SectionError}.kt` | component | n/a | `M/components/EmptyState.kt`, `SelectableChip.kt` | role-match |
| `M/components/PickerSheet.kt` (MODIFY: `PickerRow` `leadingIcon`, `Role.Checkbox`) | component | event-driven | itself | exact |
| `M/screens/clientmode/explore/ExploreRepository.kt` | service | request-response | `M/screens/signup/SignupSetupRepository.kt` | role-match |
| `M/screens/clientmode/explore/ExploreViewModel.kt` | viewmodel | request-response (3 sections, latest-wins) | `M/screens/signup/tags/SignupTagsViewModel.kt` | role-match |
| `M/screens/clientmode/explore/ExploreScreen.kt` (+ stateless `ExploreContent`) | component | request-response | `M/screens/signup/overview/SignupOverviewScreen.kt` | role-match |
| `M/screens/clientmode/explore/subviews/*.kt` | component | n/a | `SignupOverviewScreen.kt` (StepRow/cards), `Avatar.kt` | partial |
| `M/screens/clientmode/explore/categoriesmodal/AllCategoriesSheet.kt`, `CategoriesSheetViewModel.kt` | component+viewmodel | CRUD (draft selection) | `M/components/PickerSheet.kt` + `SignupTagsViewModel.kt` | role-match |
| `M/screens/clientmode/expertprofile/ExpertProfileKey.kt` | route | n/a | `M/screens/clientmode/home/subviews/HomeClientTab.kt` (`@Serializable NavKey`) | exact |
| `M/screens/clientmode/expertprofile/ExpertProfileRepository.kt` | service | request-response (parallel) | `SignupSetupRepository.kt` | role-match |
| `M/screens/clientmode/expertprofile/ExpertProfileViewModel.kt` (assisted) | viewmodel | request-response | `SignupTagsViewModel.kt` | partial (no assisted VM exists) |
| `M/screens/clientmode/expertprofile/ExpertProfileScreen.kt` + `subviews/*` | component | request-response | `SignupOverviewScreen.kt`, `EmptyState.kt` | role-match |
| `M/screens/clientmode/makeabooking/BookingKey.kt`, `MakeABookingStubScreen.kt` | route+component | n/a | `ModeSwitchKey` + `entry<ModeSwitchKey>(metadata = BottomBar.hidden())` in `HomeClientScreen.kt` | exact |
| `M/screens/clientmode/home/HomeClientScreen.kt` (MODIFY: `exploreRoot`, `detailEntries` slots) | component | event-driven | itself | exact |
| `M/res/values/strings_explore.xml`, `strings_expert_profile.xml`, plurals | config | n/a | `strings_login.xml`, `strings_signup.xml` | exact |
| `T/.../CategoryStoreTest`, `MoneyTest`, `ExploreViewModelTest`, `ExpertProfileViewModelTest`, `CategoriesSheetViewModelTest`, `TodaysSessionsFilterTest`, `CategoryIconMapTest` | test | n/a | `T/screens/signup/tags/SignupTagsViewModelTest.kt` + `T/testing/SignupTestHarness.kt` | exact |
| `T/.../ExploreScreenTest`, `ExpertProfileScreenTest`, `AllCategoriesSheetTest`, `MakeABookingStubScreenTest`, `Phase5ComponentsTest` | test | n/a | `T/screens/HomeShellsTest.kt`, `T/components/PickerComponentsTest.kt` | exact |
| `T/screens/HomeShellsTest.kt`, `T/network/ClickMeApiTest.kt`, `ResponseDecodingTest.kt`, `ApiRequestUrlsTest.kt` (MODIFY) | test | n/a | themselves | exact |

## Pattern Assignments

### `M/network/ApiRequestUrls.kt` and `ClickMeApi.kt` (config/service, request-response)

**Analog:** the files themselves. Add a `// Phase 5: discovery` block under the Phase 4 block (lines 16-24):
```kotlin
// Phase 4: signup, email verification, ...
const val SIGNUP = "auth/signup"
const val USER_AVATAR = "user/profile/avatar"
```
Add `CLIENT_HOME`, `CLIENT_BOOKINGS`, `CATEGORIES`, `EXPERT_DETAILS = "experts/{id}/details"`, `EXPERT_REVIEWS = "experts/{id}/reviews"`. Do NOT touch `publicPaths` (lines 36-43). All five stay "needs auth token". Update the class KDoc that lists phases.

Retrofit methods follow `ClickMeApi.kt` (lines 26-53), relative paths via the constant, return `SuccessDataResponse<T>`:
```kotlin
@GET(ApiRequestUrls.ME)
suspend fun getMe(): SuccessDataResponse<MeData>
```
Use the exact signatures in RESEARCH "Retrofit surface" (add `retrofit2.http.Path`, `Query` imports). Add each to `T/network/ApiRequestUrlsTest.kt` (`otherPaths_needAuthToken` list).

### Response DTOs (model, request-response)

**Analog:** `M/model/responses/MetaPayloads.kt` (payload wrapper plus item) and `PaginationMeta.kt`:
```kotlin
@JsonClass(generateAdapter = true)
data class LanguagesPayload(val languages: List<LanguageItem>)

@JsonClass(generateAdapter = true)
data class PaginationMeta(
    @Json(name = "current_page") val currentPage: Int,
    @Json(name = "total_count") val totalCount: Int? = null,
    @Json(name = "has_more") val hasMore: Boolean? = null,
)
```
Rules: `@JsonClass(generateAdapter = true)`, explicit `@Json(name = "snake_case")` on multi-word fields, ids as `String`, optional server fields `T? = null`, `featured_topics` nullable list. `CategoriesPayload(categories: List<CategoryItem>)`; `CategoryItem` is also the cache element. Reuse `PaginationMeta` for reviews and bookings. Put the today-filter extension (`List<ClientBookingItem>.todaysSessions(clock, zone)`) in a bookings package (`M/screens/clientmode/bookings/` or `M/model/`) as CONTEXT prefers, so Phase 8 extends it.

### `T/testing/FakeClickMeApi.kt` (MODIFY)

**Analog:** itself (lines 36-60 and 140-215). Per endpoint: handler var, counter, override. Compilation breaks until all five are added.
```kotlin
var languagesHandler: suspend () -> LanguagesPayload = { notStubbed() }
@Volatile var languagesCalls = 0
    private set
override suspend fun getLanguages(): SuccessDataResponse<LanguagesPayload> {
    languagesCalls++
    return ok(languagesHandler())
}
```
Add `clientHomeHandler: suspend (String?) -> ClientHomeData`, `clientBookingsHandler`, `categoriesHandler: suspend () -> CategoriesPayload` + `categoriesCalls` (success-criterion-2 evidence), `expertDetailsHandler: suspend (String) -> PublicProfileDetailsData`, `expertReviewsHandler: suspend (String) -> ExpertReviewsPayload`. Record the last `category` arg in a list (like `setupProfileRequests`, lines 98-101). Use `httpError(404, ...)` for the unavailable case.

### `M/managers/CategoryStore.kt` (store, TTL cache)

**Analog:** `M/managers/SessionStore.kt` for the DataStore + Moshi adapter + corrupt-JSON-is-miss pattern (lines 23-60):
```kotlin
@Singleton
class SessionStore @Inject constructor(private val dataStore: DataStore<Preferences>) {
    private val profileAdapter = MoshiProvider.moshi.adapter(UserProfileData::class.java)
    suspend fun cachedProfile(): UserProfileData? {
        val json = dataStore.data.first()[PROFILE_KEY] ?: return null
        return runCatching { profileAdapter.fromJson(json) }.getOrNull()
    }
    suspend fun setCachedProfile(profile: UserProfileData) {
        val json = profileAdapter.toJson(profile)
        dataStore.edit { it[PROFILE_KEY] = json }
    }
    companion object { const val FILE_NAME = "clickme_session"; private val PROFILE_KEY = stringPreferencesKey("cached_profile_json") }
}
```
Differences: inject `@CategoryCache DataStore<Preferences>` (NOT the unqualified one, since `SessionStore.clear()` wipes its file), inject `java.time.Clock`, add `Mutex`, memory copy, `fetch()`/`refresh()`. Copy the full class body from RESEARCH "CategoryStore" (lines 421-467). Wrap network via `apiCall { api.getCategories() }` (`M/network/ApiCall.kt`) and do not catch `ApiException`. Do NOT register with `M/managers/SessionDataCleaner.kt` (D-08).

### DI: `@CategoryCache` DataStore, `Clock`

**Analog:** `M/di/SessionModule.kt` (lines 24-35), `Qualifiers.kt`, `ClockModule.kt`:
```kotlin
@Provides @Singleton
fun preferencesDataStore(@ApplicationContext context: Context, @IoDispatcher io: CoroutineDispatcher): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(io + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(SessionStore.FILE_NAME) },
    )
```
Copy with `@CategoryCache` and `CategoryStore.FILE_NAME`. Qualifier shape (Qualifiers.kt):
```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CategoryCache
```
Add to `ClockModule`: `@Provides @Singleton fun provideWallClock(): java.time.Clock = Clock.systemUTC()` (keep the existing `MonotonicClock` provider, lines 14-16; its type is distinct).

### `M/util/Money.kt` (utility, transform)

No analog in the codebase (new `util` package). Use the RESEARCH "Money" block verbatim. Tests use `Locale.US` for exact strings and normalize U+00A0/U+202F for other locales.

### `M/constants/CategoryIconMap.kt` (utility, transform)

**Analog:** `M/constants/Countries.kt` / `Timezones.kt` (static data ports; check file head for object style). Port `ios-reference/Constants/CategoryIconMap.swift` as `object CategoryIconMap { fun icon(slug: String): ImageVector }` with lower-cased keys and a fallback (`Icons.Outlined.GridView` or similar). Icon list is verified in RESEARCH lines 559.

### `ExploreViewModel.kt` (viewmodel, request-response)

**Analog:** `M/screens/signup/tags/SignupTagsViewModel.kt` (lines 21-120): sealed load-state, `_uiState` + `asStateFlow`, `SavedStateHandle` restore, `viewModelScope.launch` with typed `ApiException` catch:
```kotlin
sealed interface TagCatalog {
    data object Loading : TagCatalog
    data class Loaded(val items: List<ExpertiseTagItem>) : TagCatalog
    data object Failed : TagCatalog
}
...
viewModelScope.launch {
    val catalog = try { TagCatalog.Loaded(apiCall { api.getExpertiseTags() }.data.tags) }
        catch (e: ApiException) { TagCatalog.Failed }
    _uiState.update { it.copy(catalog = catalog) }
}
```
Extend to `Loading | Loaded | Error(message)` per section (three sections), plus per-kind `Job?` cancelled before relaunch (RESEARCH Pattern 3). Selection as sorted `ArrayList<String>` in `SavedStateHandle` (same style as `KEY_SELECTED_IDS`, lines 54-57). One-shot refresh-failure Snackbar via StateFlow + `onXConsumed()` (see `SignupTagsViewModel._saved`, `LoginViewModel` destination pattern). `@HiltViewModel class ... @Inject constructor(repository, clock, savedStateHandle)`. Error text via `ApiException.toDialogError()` / `FieldError` from `M/screens/login/FieldError.kt`. Never `runCatching` around `apiCall`.

### `ExploreScreen.kt` (component, request-response)

**Analog:** `M/screens/signup/overview/SignupOverviewScreen.kt` (lines 160-215): `Scaffold` with `snackbarHost`, `PullToRefreshBox(isRefreshing, onRefresh, Modifier.fillMaxSize().padding(innerPadding))` wrapping a `verticalScroll` Column with `padding(horizontal = Spacing.lg)`.
```kotlin
PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh,
    modifier = Modifier.fillMaxSize().padding(innerPadding)) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg)) { ... }
}
```
Pattern: Hilt wrapper (`hiltViewModel()` default param, `collectAsStateWithLifecycle()`) plus stateless `ExploreContent(state, callbacks)` for Robolectric (as `LoginScreen`/`LoginContent`, see 04-PATTERNS). Keep the `TopBarAvatarButton` in the top bar via the shell slot (see `HomeClientScreen` below). Errors/empties: `ErrorState(onRetry)` / `EmptyState(icon, heading, body, action = EmptyStateAction(label, onClick))` from `M/components/EmptyState.kt` (lines 38-75, 86-99). Cards use `PressScaleButton`/`Modifier.pressScale` (`M/components/buttonstyles/PressScale.kt`); images via `Avatar` (`M/components/avatar/Avatar.kt`, sizes 32/40/56/96/160).

### `AllCategoriesSheet.kt` and `CategoriesSheetViewModel.kt` (component, draft selection)

**Analog:** `M/components/PickerSheet.kt`. Use `PickerSheet(title, onDismiss, searchQuery, searchPlaceholder, onSearchQueryChange, footer) { items }` (lines 52-84) and `PickerRow(label, selected, onClick, minHeight)` (lines 135-170), `PickerLoading(text)` (lines 173-195). Test via stateless `PickerSheetContent` (lines 92-132), because `ModalBottomSheet` needs a window.
```kotlin
PickerSheet(title = ..., onDismiss = onDismiss, searchQuery = state.query, onSearchQueryChange = vm::onQuery,
    footer = { /* Apply / Clear */ }) {
    items(state.filtered, key = { it.id }) { c -> PickerRow(label = c.name, selected = c.id in draft, onClick = { toggle(c.id) }) }
}
```
Modify `PickerRow` to add optional `leadingIcon: ImageVector? = null` and `role: Role = Role.RadioButton` (default behavior unchanged; extend `T/components/PickerComponentsTest.kt`). The search field uses `ValidatedField` inside `PickerSheetContent`, so the WR-04 capitalization issue is already handled there. ViewModel: sealed `Loading/Loaded/Failed` as in `SignupTagsViewModel.TagCatalog`; `visible` filter is case-insensitive `contains(needle, ignoreCase = true)` (as `SignupTagsUiState.suggested`, lines 41-47); Retry calls `categoryStore.refresh()`.

### `ExpertProfileKey.kt` and `BookingKey.kt` (route)

**Analog:** `M/screens/clientmode/home/subviews/HomeClientTab.kt` (lines 21-34):
```kotlin
@Serializable
data object ExploreRoot : NavKey
```
Use `@Serializable data class ExpertProfileKey(...) : NavKey` and `BookingKey(expertId, name, title, imageUrl) : NavKey` (RESEARCH Pattern 2). Never put secrets in keys.

### `ExpertProfileViewModel.kt` (assisted viewmodel)

No existing assisted-injection VM. Base on `SignupTagsViewModel` (HiltViewModel, StateFlow state, `viewModelScope.launch`, load in `init`) and use RESEARCH Pattern 2 for `@HiltViewModel(assistedFactory = ...)` + `@AssistedInject` and `hiltViewModel<VM, VM.Factory>(creationCallback = { it.create(key) })` (import `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`, same as `HomeClientScreen` line 16). Parallel load with per-call `try/catch (e: ApiException)` (RESEARCH "Parallel profile load"); 404 maps to `Unavailable` via `(e as? ApiException.Http)?.status == 404`. Tests construct the VM directly with the key and a `FakeClickMeApi`.

### `ExpertProfileScreen.kt` and `subviews/*`

**Analog:** `SignupOverviewScreen.kt` (Scaffold + `TopAppBar` back arrow; see `AuthTopAppBar` in `M/screens/login/AuthTopAppBar.kt`) and `EmptyState`/`ErrorState` for D-13. Hero reuses `GreenGlowBlobLayer(Modifier.matchParentSize())` (static, no animation). Stateless `ExpertProfileContent` for tests. Hide empty sections (D-12), no heart/Message/menu (D-05).

### `MakeABookingStubScreen.kt` and shell registration

**Analog:** `entry<ModeSwitchKey>(metadata = BottomBar.hidden())` in `HomeClientScreen.kt` (lines 119-125):
```kotlin
entry<ModeSwitchKey>(metadata = BottomBar.hidden()) {
    ModeSwitchScreen(currentMode = AppMode.Client, onSwitchMode = onSwitchMode, onBack = navigator::goBack)
}
```
Stub screen takes `(key: BookingKey, onBack)` and shows "Booking is coming soon" under a `TopAppBar` with back arrow.

### `HomeClientScreen.kt` (MODIFY: injectable slots)

**Analog:** itself. `HomeClientShell` (lines 85-143) is Hilt-free. Current Explore entry (lines 106-111):
```kotlin
entry<ExploreRoot> {
    ClientTabRoot(key = ExploreRoot,
        actions = { TopBarAvatarButton(avatarUrl, onClick = { navigator.navigate(ModeSwitchKey) }) })
}
```
Add default-valued params so `HomeShellsTest` (which asserts "Nothing here yet.", line 85) stays green:
```kotlin
exploreRoot: @Composable (navigator: Navigator, avatarUrl: String?) -> Unit = { nav, url -> /* the existing ClientTabRoot placeholder */ },
detailEntries: EntryProviderScope<NavKey>.(Navigator) -> Unit = {},
```
The Hilt wrapper `HomeClientScreen` (lines 47-68) passes the real `ExploreScreen` and `entry<ExpertProfileKey>` / `entry<BookingKey>(metadata = BottomBar.hidden())`. `ExploreRoot` stays in `HomeClientTab.kt`. Leave the profile/debug entries untouched.

### Strings

**Analog:** one resource file per feature: `res/values/strings_login.xml`, `strings_signup.xml`. Create `strings_explore.xml`, `strings_expert_profile.xml` with `<plurals>` for review counts (consumed with `pluralStringResource`). No `fontSize` overrides, no `Color(...)` literals outside `ui/theme/Color.kt` (`scripts/check-design-rules.sh`).

### Tests

**ViewModel/store analog:** `T/screens/signup/tags/SignupTagsViewModelTest.kt` (lines 1-75): `@get:Rule val main = MainDispatcherRule()`, `TemporaryFolder`, `runTest(main.testDispatcher)`, `advanceUntilIdle()`, handler lambdas on `FakeClickMeApi`, assert `api.xCalls`. For `CategoryStore` build a real DataStore with `testPreferencesDataStore(scope.backgroundScope, tmp)` (as `SignupTestHarness` line 38) and a mutable test `Clock` (new `T/testing/MutableClock.kt`). Stale-response test: handler suspends on a `CompletableDeferred`.
**Compose analog:** `T/screens/HomeShellsTest.kt`: `@RunWith(RobolectricTestRunner::class)`, `createComposeRule()` from `junit4.v2`, `ClickMeTheme { XContent(...) }`, tab matcher `hasText(label) and isSelectable()` (line 70). Do not use bare `hasText("Explore")` (collides with title and hero).

## Shared Patterns

### Network call and error mapping
**Source:** `M/network/ApiCall.kt` (`apiCall {}`), `M/screens/login/FieldError.kt` (`toDialogError()`, `fieldMessage`). Catch `ApiException` explicitly (it already rethrows cancellation).
**Apply to:** `CategoryStore`, `ExploreRepository`, `ExpertProfileRepository`, all new ViewModels.
```kotlin
try { ...apiCall { api.x() }.data } catch (e: ApiException) { /* map to Error/Unavailable */ }
```

### Hilt singleton repository
**Source:** `M/screens/signup/SignupSetupRepository.kt` (lines 69-80): `@Singleton class X @Inject constructor(private val api: ClickMeApi, ...)`. Apply to `ExploreRepository`, `ExpertProfileRepository`. ViewModels needing only the public API may inject `ClickMeApi` directly (`SignupTagsViewModel`).

### Screen split
**Source:** `LoginScreen.kt`/`HomeClientScreen.kt`: Hilt wrapper with `hiltViewModel()` + `collectAsStateWithLifecycle()`, plus stateless `...Content` for Robolectric.

### One-shot events and state
**Source:** `SignupTagsViewModel` (`_saved` StateFlow consumed then reset) and `LoginViewModel`. Apply to Explore refresh-failure Snackbar and Profile navigation signals.

### Navigation push
**Source:** `M/navigation/Navigator.kt` (`navigate(route)`, `goBack()`), entries in `entryProvider<NavKey> { entry<Key> { ... } }` inside `HomeTabScaffold`.

### Theme/layout
`Spacing.*`, `MaterialTheme.extras.onSurfaceMuted`, `MaterialTheme.shapes`, `heightIn(min = ...)` (never fixed heights on text cards), `WindowInsets.safeDrawing` as in 04-PATTERNS.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `M/util/Money.kt` | utility | transform | No currency formatting exists. Use RESEARCH "Money" block. |
| Assisted-injection ViewModel (`ExpertProfileViewModel`) | viewmodel | request-response | No `@AssistedInject` VM in the repo. Use the Nav3 recipe in RESEARCH Pattern 2. |
| `CategoryStore` TTL/Mutex/Clock logic | store | CRUD | `SessionStore` has no TTL or fetch. Use RESEARCH "CategoryStore". |
| `T/testing/MutableClock.kt` | test util | n/a | No `java.time.Clock` test helper exists. |

## Metadata

**Analog search scope:** `M/{components,di,managers,model/responses,navigation,network,screens/clientmode,screens/signup}`, `T/{testing,screens,components,network}`
**Files scanned:** ~120 listed, 16 read
**Pattern extraction date:** 2026-10-21
