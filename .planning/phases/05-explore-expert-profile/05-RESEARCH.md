# Phase 5: Explore & Expert Profile - Research

**Researched:** 2026-10-21
**Domain:** Android Jetpack Compose port of the iOS Explore home, All Categories sheet, Expert Profile and a stub booking route. It adds one REST group (discovery), one 7-day cache (`CategoryStore`) and a shared money formatter.
**Confidence:** HIGH. The backend contract was read from the controller and migration source. The Android patterns were read from the Phase 1-4 code. No new external packages are needed.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

Carried forward (not re-opened):
- **iOS is the source of truth (DEC-ios-source-of-truth).** Deviate for Android conventions, or for iOS defects backed by a backend spec or by the iOS code's own stated intent.
- **Styling** is a Material 3 adaptation, dark only, in Nunito. Pixel parity is out of scope. The roadmap allows simplifying the hero and visuals.
- **Navigation:** Navigation 3 with per-tab back stacks (Phase 1). Detail screens are pushed with `navigator.navigate(Key)` and registered in the client shell's `entryProvider`.
- **API groups** are ported one feature at a time: add `ApiRequestUrls` constants, `ClickMeApi` methods, and a Hilt `@Singleton` repository using `apiCall {}`.
- **DTOs** are Moshi codegen with an explicit `@Json(name=…)` on multi-word fields.

Unbuilt destinations
- **D-01: Build Today's Sessions now, without interactions.**
  - Fetch `GET /client/bookings?type=upcoming`. Keep the bookings that fall on today in the device's zone and are confirmed, in progress or pending reschedule (iOS filter).
  - Render the cards as on iOS, and hide the section when it's empty.
  - Tapping a card does nothing until Phase 8.
  - A failure fails silently and hides the section (iOS parity).
- **D-02: Join Call is always shown dimmed on the Today's Sessions cards,** using the "outside the join window" style whatever the time. Phase 9 adds the window logic and the click. The Phase 8/9 planners must find this placeholder and wire it.
- **D-03: Hide "See all"** on Featured Topics and Recommended Experts. The sections show what the server returns (8 topics, 10 experts), with no link. Search is deferred (DEF-2).
- **D-04: Book Session pushes a stub booking screen.**
  - Register the real booking route key now (the key Phase 6 will use), carrying the expert id, name, title and image (iOS `MakeABooking` input). The body says booking is coming soon.
  - Phase 6 replaces the body, not the route.
  - When `is_bookable == false`, Book Session is a disabled "Not accepting bookings" pill (iOS parity).
- **D-05: Hide every deferred control on Expert Profile:** the favorites heart, reviews "See all", Message, and the "…" Report/Block menu. Book Session is the only action. Phase 12 adds Message and Report/Block.

Categories & CategoryStore (DISC-02)
- **D-06: Build the All Categories sheet** to give DISC-02 a consumer. This pulls one slice of DEF-7 into the MVP.
  - "View all" on the Trending Categories row opens a `ModalBottomSheet` with a local search field and a multi-select list of all categories.
  - The list comes from `CategoryStore`.
  - If the fetch fails, show Retry, which forces a refresh (the iOS `refresh()`).
- **D-07: CategoryStore ports iOS `CategoryStore.swift`.**
  - Data comes from `GET /categories` (public endpoint).
  - Store the list plus the fetch time in DataStore, with an in-memory copy.
  - `fetch()` serves the cache while it is under 7 days old and re-fetches once it is older. `refresh()` always re-fetches.
  - Success criterion 2: no network call within the TTL.
- **D-08: CategoryStore is not cleared on logout** (iOS parity). The data is public and the same for every user, so don't register it with `SessionDataCleaner`.
- **D-09: The chip row uses `/client/home`'s `trending_categories` (6), not the store (iOS parity).**
  - Tapping a chip filters by that one category, re-fetching `/client/home?category=slug`. Only Recommended Experts changes; Featured Topics stays.
  - Tapping the selected chip again clears the filter.
- **D-10: The All Categories sheet is multi-select (iOS parity).**
  - Applying it re-fetches with a comma-joined list, `category=slug1,slug2`.
  - Selected trending chips show as selected in the row. Clearing resets the filter.
  - The chip row and the sheet share one selection.
- **D-11: Category icons:** port `ios-reference/Constants/CategoryIconMap.swift` as a slug → Material Symbols map, tinted with `color_accent`. Unknown slugs get a generic fallback icon.

Expert Profile & Explore states (DISC-01, DISC-03)
- **D-12: Hide empty sections (fixes an iOS bug).**
  - Expertise, Topics of Discussion and Recent Reviews are left out entirely when they have no items.
  - With zero reviews, the hero rating line reads "No reviews yet" instead of "—".
- **D-13: A 404 from `GET /experts/:id/details` shows a full-screen "unavailable" state.**
  - This applies to suspended, deleted and blocked experts (USER-REPORTING.md §5). It is an `EmptyState` saying the profile isn't available, with a back action.
  - Network and 5xx errors on details show `ErrorState` with Retry instead.
  - iOS fails silently here, which is the bug being fixed.
- **D-14: Hero: static glow, no online dot.** Reuse `GreenGlowBlobLayer` without animation. Drop the hard-coded online dot, because the server's `is_online` is a placeholder.
- **D-15: Expert Profile shows the preview first, then fills in.**
  - The pushed route carries what the card already knows: id, name, title and image, plus rating, review count and tags when they come from an expert card. Render those immediately.
  - Fetch `GET /experts/:id/details` and `GET /experts/:id/reviews?page=1&limit=3` in parallel.
  - Skip the favorites request, since the heart is hidden.
  - Skeletons stand in for sections that are still loading.
  - The hero rating and review count come from the reviews `summary` once it arrives.
- **D-16: Explore errors: per-section retry (iOS parity).**
  - While loading, show skeletons for categories and experts (iOS).
  - When `/client/home` fails, each home section shows its own error and Retry. Every Retry calls the same request.
  - Pull-to-refresh reloads everything, including Today's Sessions.
- **D-17: An empty expert list shows an inline message.** Zero experts, for example after a filter with no matches, shows "No experts in this category yet". When a filter is active it adds a "Clear filter" action. iOS shows a blank area.

Price & currency display
- **D-18: One shared Money formatter**, built now and reused by Phases 6, 7, 11 and 14.
  - It takes minor units plus an ISO currency code.
  - Decimal places come from `java.util.Currency.defaultFractionDigits`, so JPY gets 0. Divide accordingly (PRICING-LIMITS.md §5).
  - No `/meta/currencies` call (iOS doesn't use it either).
- **D-19: The formatter uses the device locale** for symbol placement and separators, as iOS `NumberFormatter` does with `Locale.current`. The currency code always comes from the server.
- **D-20: Always show full decimals** ("$50.00"). This is standard currency-formatter output.
- **D-21: Free topics show "Free" in green everywhere (fixes an iOS bug).** Use `is_free` (or `price.is_free`) on Featured Topic cards and on profile topic rows. iOS featured cards ignore `is_free`. Note: `featured_topics[].hourly_rate` is really the flat per-session price, despite its name.

### Claude's Discretion
- Skeleton design, card dimensions, and the simplified Featured Topic and Expert card layouts. Use the iOS dimensions as a guide (featured 220×176, 96pt expert image), with Material 3 equivalents.
- Exact empty-state and unavailable copy, and the Material Symbols chosen for each category slug.
- Whether Explore keeps the `TopBarAvatarButton` app bar as it is today (yes, unless the iOS layout clearly differs).
- How the shared chip/sheet selection state is held (one ViewModel state) and how the route key carries the preview data.
- Whether the Today's Sessions card and its DTO live in a bookings package that Phase 8 will extend (preferred) or in Explore.

### Deferred Ideas (OUT OF SCOPE)
- The rest of DEF-7 (the Cancellation Policies page and cosmetic animations) stays deferred. Only the All Categories sheet was pulled into Phase 5.
- The pulsing hero glow animation is cosmetic and was dropped (D-14).
- The "See all" links come back with Search (DEF-2), and the reviews "See all" with the full reviews list (DEF-3).

Also out of scope (CONTEXT domain boundary): the Search tab (DEF-2), Favorites and the heart (DEF-3), the full reviews list (DEF-3), Message/Report/Block on Expert Profile (Phase 12), the booking flow itself (Phase 6), Upcoming Booking Details and Join (Phases 8 and 9).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DISC-01 | Client sees the Explore home from `getClientHome`: categories, featured topics, today's sessions and expert cards. The hero and visuals may be simplified. | Wire shape of `GET /client/home` (Standard Stack / Code Examples). `GET /client/bookings?type=upcoming` shape and today filter. `ExploreViewModel` section-state pattern. UI-SPEC screen 1. |
| DISC-02 | Categories are cached in a `CategoryStore` with a 7-day TTL. | `CategoryStore` design: second DataStore file with its own qualifier, injectable wall `Clock`, single-flight mutex, `fetch()`/`refresh()`. Test strategy for "no network call within the TTL". |
| DISC-03 | Client can open an Expert Profile with a hero, stats, about, topics, a reviews preview, and a Book button that starts the booking flow. | `GET /experts/:id/details` and `/reviews` shapes. Nav3 assisted-injection ViewModel keyed by `ExpertProfileKey`. 404 vs error handling. `BookingKey` stub route. `Money.format`. |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- Jetpack Compose + Material 3, MVVM with Compose State, Kotlin coroutines (no RxJava), Hilt, Retrofit + Moshi. minSdk 26, targetSdk 37, compileSdk 37.
- iOS under `ios-reference/` is the source of truth for behavior. Deviate only for Android conventions (`.sheet` becomes `ModalBottomSheet`, back chevron becomes `TopAppBar` back arrow, `.task` becomes `LaunchedEffect(Unit)`).
- File organization mirrors the iOS folder tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/`. Explore becomes `screens/clientmode/explore/` and Expert Profile `screens/clientmode/expertprofile/`. The stub booking route goes under `screens/clientmode/makeabooking/`, the path Phase 6 will own. Each screen has `XScreen.kt` plus `XViewModel.kt`.
- Backend contract is the same REST contract as iOS. Specs are in `backend-reference/docs/ios/`.
- Project memory: Material styling is acceptable. Do not block on human checkpoints; log them as open items. Executors in worktrees need `local.properties`.
- Enforced by CI and carried from Phase 1: `scripts/check-design-rules.sh` (no `Color(...)` literals outside `ui/theme/Color.kt`, no `*Brand` types), ktlint, lint with `abortOnError = true`, and no `fontSize` overrides (UI-SPEC).

## Summary

Phase 5 is a read-only discovery surface. It needs five endpoints: `GET /client/home`, `GET /client/bookings?type=upcoming`, `GET /categories`, `GET /experts/{id}/details` and `GET /experts/{id}/reviews`. All five are plain authenticated GETs that the existing `apiCall {}` stack handles. `/categories` is public on the backend, but the app's allowlist already sends the bearer to it harmlessly. No new library is needed. Every UI piece maps to something already in the repo (`PickerSheet`, `EmptyState`/`ErrorState`, `Avatar`, `GreenGlowBlobLayer`, `PressScaleButton`, `PullToRefreshBox`) or to a new shared component specified in the UI-SPEC.

The backend source shows several things that the iOS code and CONTEXT do not mention. They matter for the DTOs.
- `featured_topics[]` carries a **top-level `is_free`**, a **`rating`** and an **`is_favorite`**. iOS decodes none of them. The `is_free` field is what D-21 needs.
- `/experts/{id}/details` returns `is_bookable` as a real boolean now (always present) plus `is_favorite`, `available_meeting_types` and `recent_reviews: []`.
- The reviews payload `summary.overall_rating` is nullable and `total_reviews` is an int.
- `/client/home` ignores `page` and `limit` (the schema is `.strict()` but accepts them), and `p_limit` defaults to 10. **Send only `category`.**
- The recommended-experts query filters on the scalar `profiles.role = 'expert'` and does not exclude the caller. An expert-role tester in Client mode will see their own card.

The riskiest parts are structural rather than technical.
1. **Shell testability.** `HomeClientShell` is deliberately Hilt-free and `HomeShellsTest` asserts "Nothing here yet." on the Explore tab. The Explore entry and the new detail entries need ViewModels, so the shell needs injectable slots with defaults that keep the existing test green.
2. **Per-entry ViewModels that take a route key.** The repo has no assisted-injection ViewModel yet. The official Nav3 recipe (`@HiltViewModel(assistedFactory=…)` + `hiltViewModel<VM, VM.Factory>(creationCallback=…)`) works with the `rememberViewModelStoreNavEntryDecorator` that `toDecoratedEntries` already installs.
3. **Race conditions on filter changes and parallel loads.** Latest-wins job cancellation on Explore. Per-call `try/catch` on Profile so one failed `async` does not cancel the other.

**Primary recommendation:** Build in this order: (1) DTOs + `ApiRequestUrls` + `ClickMeApi` + `FakeClickMeApi`, (2) `Money`, `CategoryStore` (+ Clock and DataStore DI), (3) shared UI components, (4) `ExploreViewModel` and screen, (5) the All Categories sheet, (6) `ExpertProfileViewModel` (assisted) and screen, (7) the `BookingKey` stub and the shell wiring with `HomeShellsTest` updated. Unit-test every ViewModel and store with the existing fake-API harness.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Home feed data (categories, topics, experts) | API / Backend (`home_feed` RPC) | Client (render only) | Ranking, moderation hiding and the category filter are server-side. The client only passes `category`. |
| Category filter selection state | Client (`ExploreViewModel`, `SavedStateHandle`) | API (applies it) | Selection is UI state shared by the chip row and the sheet. The server filters. |
| Today's Sessions filter (today, joinable status) | Client | API (returns all upcoming) | "Today" depends on the device zone. The server cannot know it. |
| Categories reference list + 7-day TTL | Client local storage (`CategoryStore`, DataStore) | API (`/categories`) | Offline-first reference data. Public and the same for every user (D-08). |
| Price formatting | Client (`Money.format`) | API (amounts in minor units + ISO code) | Locale-dependent. The backend also ships a server `label` on other endpoints, but D-18/D-19 chose a client formatter. |
| `is_bookable` gating of Book Session | API (`/details`) | Client (display only) | Display convenience. The real gate is the server 422 `EXPERT_NOT_BOOKABLE` at confirm (Phase 6/7). |
| Suspended/deleted expert handling | API (404) | Client (unavailable state) | USER-REPORTING.md §5. The server hides, the client renders. |
| Expert Profile navigation + preview seed | Client (Nav3 key) | — | The key carries the card's known fields so the hero renders before any network response. |
| Review summary (rating, count) | API (`/reviews` `summary`) | Client | Source of truth for the hero. The list-card rating is only a seed. |

## Standard Stack

No new dependencies. All versions are from `gradle/libs.versions.toml` in this repo. [VERIFIED: codebase]

### Core (already present)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose BOM / Material 3 | 2026.09.00 | `PullToRefreshBox`, `ModalBottomSheet`, `TopAppBar`, `FlowRow` | Project baseline. `FlowRow` has been stable since Compose 1.8 [CITED: android-developers.googleblog.com/2025/04/whats-new-in-jetpack-compose-april-25.html], so no `@OptIn` is expected. |
| Navigation 3 (`navigation3-runtime/ui`) | 1.2.0 | Per-tab back stacks, `entry<Key>` | Established in Phase 1. |
| `lifecycle-viewmodel-navigation3` | 2.11.0 | `rememberViewModelStoreNavEntryDecorator` (already used in `toDecoratedEntries`) | Gives one ViewModel per `NavEntry`, so a new `ExpertProfileKey` gets a new ViewModel. |
| `androidx.hilt:hilt-lifecycle-viewmodel-compose` | 1.4.0 | `hiltViewModel<VM, Factory>(creationCallback)` | Official Nav3 recipe for key arguments [CITED: developer.android.com/guide/navigation/navigation-3/recipes/passingarguments]. |
| Retrofit / OkHttp / Moshi codegen | 3.0.0 / 5.5.0 / 1.15.2 | REST + JSON | Established. |
| DataStore Preferences | 1.2.1 | `CategoryStore` persistence | Same as `SessionStore`. |
| Coil 3 | 3.6.3 | Remote images (through `Avatar`) | Established. |
| kotlinx-coroutines | 1.11.0 | Parallel loads, latest-wins jobs | Established. |

### Supporting (test)
| Library | Version | Purpose |
|---------|---------|---------|
| JUnit 4 / Robolectric / Compose ui-test-junit4 / coroutines-test / mockwebserver3 | 4.13.2 / 4.17 / BOM / 1.11.0 / 5.5.0 | All Phase 5 tests, same harness as Phase 4. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Second `DataStore<Preferences>` file for categories | Reuse the `clickme_session` store | Rejected. `SessionStore.clear()` wipes the whole file at logout, which would break D-08. |
| Assisted-injection ViewModel for `ExpertProfileKey` | Plain `hiltViewModel()` plus a `LaunchedEffect { vm.init(key) }` | The assisted form is the documented Nav3 recipe and makes the key available at construction, so tests construct the VM directly. Use assisted. |
| Home DTO decoding `UUID` | `String` ids | Moshi has no built-in UUID adapter. Use `String`. |

**Installation:** none.

## Package Legitimacy Audit

This phase installs **no external packages**. Every dependency used is already in `gradle/libs.versions.toml` and was audited in earlier phases. slopcheck does not apply.

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
                       ┌────────────────────────── Client shell (HomeClientShell, Nav3, per-tab stacks) ───────────────────────────┐
                       │                                                                                                           │
  Explore tab root     │   ExploreScreen (Hilt) ◄──── ExploreViewModel ◄──┬── ExploreRepository ──► GET /client/home[?category=a,b] │
  (ExploreRoot)        │      │   │   │                 (Section states,   │          (@Singleton,    GET /client/bookings?type=upcoming│
                       │      │   │   │                  selection in      │           apiCall{})                                   │
                       │      │   │   │                  SavedStateHandle, └── latest-wins Job per request kind                    │
                       │      │   │   │                  today filter w/ Clock+ZoneId)                                             │
                       │      │   │   └─ "View all" ─► AllCategoriesSheet (PickerSheet) ◄── CategoriesSheetState ◄── CategoryStore  │
                       │      │   │                      draft = copy(selection)              fetch(): fresh? ─yes─► memory/DataStore│
                       │      │   │                      Apply ─► vm.applySelection(draft)            └─no/stale─► GET /categories   │
                       │      │   │                                                           refresh(): always GET /categories     │
                       │      │   │                                                           (clickme_categories DataStore, wall Clock)
                       │      │   └─ chip tap ─► vm.onChipTap(slug) ─► reload experts only (selection → SavedStateHandle)            │
                       │      │                                                                                                    │
                       │      └─ expert card / featured card tap ─► navigator.navigate(ExpertProfileKey(seed))                      │
                       │                                                                                                           │
  Pushed on Explore    │   ExpertProfileScreen ◄─ ExpertProfileViewModel (@AssistedInject key) ◄─ ExpertProfileRepository          │
  stack                │      hero from key at once      ┌──────────────── async ───────────────┐                                   │
                       │                                 ├─► GET /experts/{id}/details  (404 → Unavailable, other → ErrorState)      │
                       │                                 └─► GET /experts/{id}/reviews?page=1&limit=3 (failure → keep seed)          │
                       │      Book Session ─► navigator.navigate(BookingKey(id,name,title,image))  [BottomBar.hidden()]            │
                       │                          └─► MakeABookingStubScreen (Phase 6 replaces the body, not the key)              │
                       └───────────────────────────────────────────────────────────────────────────────────────────────────────────┘
  Shared: Money.format (minor units + ISO code + Locale) · CategoryIconMap (slug → ImageVector) · RatingStars · ExpertTagChip · SkeletonBlock · SectionHeader · SectionError
```

### Recommended Project Structure
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── managers/CategoryStore.kt                         # iOS Managers/CategoryStore.swift
├── di/CategoryCacheModule.kt (or extend SessionModule)  # @CategoryCache DataStore + java.time.Clock
├── util/Money.kt                                     # shared formatter (new package; Phases 6,7,11,14 reuse)
├── constants/CategoryIconMap.kt                      # iOS Constants/CategoryIconMap.swift
├── components/{RatingStars,ExpertTagChip,SkeletonBlock,SectionHeader,SectionError}.kt
├── components/PickerSheet.kt                         # + PickerRow(leadingIcon, role)
├── model/responses/{ClientHomeData,CategoryItem,PublicProfileDetailsData,ExpertReviewsPayload,ClientBookingsPayload}.kt
├── network/ApiRequestUrls.kt, ClickMeApi.kt          # + 5 endpoints
├── screens/clientmode/explore/
│   ├── ExploreRepository.kt
│   ├── ExploreViewModel.kt, ExploreScreen.kt (+ stateless ExploreContent)
│   ├── categoriesmodal/AllCategoriesSheet.kt, CategoriesSheetViewModel.kt
│   └── subviews/{ExploreHeroHeader,TodaysSessionsSection,ExploreCategoriesSection,
│                 ExploreFeaturedTopicsSection,ExploreExpertsSection,ExpertCard}.kt
├── screens/clientmode/expertprofile/
│   ├── ExpertProfileKey.kt (NavKey), ExpertProfileRepository.kt
│   ├── ExpertProfileViewModel.kt (assisted), ExpertProfileScreen.kt (+ stateless ExpertProfileContent)
│   └── subviews/{Hero,StatsBar,AboutCard,ExpertiseSection,TopicsCard,ReviewsCard}.kt
└── screens/clientmode/makeabooking/{BookingKey.kt, MakeABookingStubScreen.kt}
```
`ExploreRoot` stays in `screens/clientmode/home/subviews/HomeClientTab.kt`.

### Pattern 1: Hilt-free shell with injectable slots (keeps `HomeShellsTest` green)
**What:** `HomeClientShell` keeps zero Hilt/BuildConfig access. Add two default-valued parameters.
```kotlin
// HomeClientShell(...)
exploreRoot: @Composable (navigator: Navigator, avatarUrl: String?) -> Unit = { _, _ -> /* existing placeholder + avatar action */ },
detailEntries: EntryProviderScope<NavKey>.(Navigator) -> Unit = {},
```
`HomeClientScreen` (the Hilt wrapper) passes the real `ExploreScreen` and registers `entry<ExpertProfileKey>` and `entry<BookingKey>(metadata = BottomBar.hidden())`. The existing shell test then still sees "Nothing here yet.", and the new Explore tests exercise `ExploreContent` directly. [VERIFIED: codebase, `HomeShellsTest` and `HomeClientShell`]
**Why:** The alternative is calling `hiltViewModel()` inside the shell, which breaks every Robolectric shell test (no Hilt graph). The avatar action and ModeSwitch entry must stay in the shell, as they are today.

### Pattern 2: Assisted-injection ViewModel keyed by a Nav3 route
```kotlin
// Source: developer.android.com/guide/navigation/navigation-3/recipes/passingarguments
@Serializable
data class ExpertProfileKey(
    val expertId: String, val name: String, val title: String, val imageUrl: String?,
    val rating: Double? = null, val reviewCount: Int? = null, val tags: List<String> = emptyList(),
) : NavKey

@HiltViewModel(assistedFactory = ExpertProfileViewModel.Factory::class)
class ExpertProfileViewModel @AssistedInject constructor(
    @Assisted private val key: ExpertProfileKey,
    private val repository: ExpertProfileRepository,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(key: ExpertProfileKey): ExpertProfileViewModel }
}

entry<ExpertProfileKey> { key ->
    val vm = hiltViewModel<ExpertProfileViewModel, ExpertProfileViewModel.Factory>(
        creationCallback = { it.create(key) },
    )
    ExpertProfileScreen(vm, onBack = navigator::goBack, onBook = { navigator.navigate(BookingKey(...)) })
}
```
Do not put `LaunchedEffect(Unit)` loading in the screen. Start the load in the ViewModel `init` (or lazily on first collection) so it survives recomposition. The `LaunchedEffect(Unit)` swap in CLAUDE.md is for screens without a ViewModel scope. [CITED: developer.android.com/guide/navigation/navigation-3/recipes/passingarguments]

### Pattern 3: Explore section states and latest-wins loading
Model each of the three home sections as `Loading | Loaded(data) | Error(message)` (one sealed type). Keep three independent request kinds, each with its own `Job`.
- `loadAll()`: full replace of categories, topics and experts, plus today's sessions. Used by first load, pull-to-refresh and any Retry.
- `loadExpertsForSelection()`: experts section only, used after chip tap, Apply or Clear filter. Keeps categories and topics as they are (D-09), because `featured_topics` is `random()` per request and would reshuffle the strip on every chip tap.
- A new request of the same kind cancels the older job (`job?.cancel()`), so a slow response for an old filter cannot overwrite a newer one.
- Today's Sessions failure is swallowed to an empty list (D-01). It never touches the home sections.
- A failed pull-to-refresh with data on screen keeps the data and raises a one-shot Snackbar event, using the repo's "one-shot StateFlow consumed by `LaunchedEffect` then `onXConsumed()`" convention.
- Selection is `Set<String>` persisted as a sorted `ArrayList<String>` in `SavedStateHandle`. Send `selection.sorted().joinToString(",")`, or no `category` when empty (iOS sends sorted slugs).

### Pattern 4: CategoryStore (port of iOS `CategoryStore.swift`)
See Code Examples. Key rules: wall-clock `Clock` (not the monotonic clock, which resets at reboot), a `Mutex` to single-flight concurrent callers, in-memory copy that also honours the TTL, and cache write only after a successful decode.

### Anti-Patterns to Avoid
- **`runCatching` around `apiCall` inside coroutines.** It swallows `CancellationException`. Catch `ApiException` explicitly (`apiCall` already rethrows cancellation).
- **`coroutineScope { async {...}; async {...} }` for details + reviews without per-call catches.** One failing child cancels the sibling. Wrap each in `try/catch (e: ApiException)` that returns a result, then combine.
- **Reading `/categories` for the chip row.** D-09 says the chips come from `/client/home.trending_categories`. Only the sheet reads the store.
- **Computing "today" with `LocalDate.now()` and a cached `ZoneId`.** Read `ZoneId.systemDefault()` per call so a timezone change is honoured.
- **Registering `CategoryStore` with `SessionDataCleaner`** (D-08).
- **Fixed heights on text-bearing cards.** Use `heightIn(min=…)` so 200% font scale does not clip (UI-SPEC).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Currency formatting and decimal places | Manual `/100` and symbol tables | `java.util.Currency.defaultFractionDigits` + `NumberFormat.getCurrencyInstance(locale)` + `BigDecimal.movePointLeft` | Symbol placement, grouping and JPY=0 digits are locale/ICU data. |
| Pull to refresh | Custom drag detector | M3 `PullToRefreshBox` (used in `SignupOverviewScreen`) | Accessibility, overscroll and indicator already handled. |
| Bottom sheet | Custom overlay | `PickerSheet` / `ModalBottomSheet` (Phase 4) | IME padding, scrim and back handling are done. Test via `PickerSheetContent`. |
| Wrapping tag chips | Manual row measuring | `FlowRow` | Stable in current Compose. |
| Local time of day | `SimpleDateFormat` or fixed "h:mm a" | `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)` with the default locale | Honours 12/24 h device preference (UI-SPEC open item 4). |
| Plurals ("1 review"/"2 reviews") | `if (n == 1)` strings | `plurals` resources + `pluralStringResource` | Required by the UI-SPEC copy contract. |
| JSON disk cache | Hand-written file IO | DataStore Preferences + Moshi adapter (as `SessionStore`) | Atomic writes, corruption handler pattern already in `SessionModule`. |
| Image loading | Custom bitmap loader | `Avatar` (Coil 3) | Fallback and size rules exist. |
| Press feedback | Custom scale animation | `Modifier.pressScale` / `PressScaleButton` | Phase 1 motion tokens. |

**Key insight:** The only genuinely new logic is the TTL rule, the section state machine and the today filter. Everything else is composition of existing parts.

## Runtime State Inventory

Not a rename/refactor/migration phase. Omitted. One persistence addition to note: a new DataStore file (`clickme_categories`) is created that **intentionally survives logout** (D-08). Nothing else is stored.

## Common Pitfalls

### Pitfall 1: Featured-topic price semantics and the free flag
**What goes wrong:** Featured cards show "$0.00" or "—" for free topics, or format `hourly_rate` as an hourly price.
**Why it happens:** `featured_topics[].hourly_rate` is `{amount, currency}` built from `price_amount`/`price_currency` and is a flat per-session price. The free flag is a **sibling top-level `is_free`**, not `price.is_free` (that nesting exists only on `/details`). For a free topic `amount` may be null or 0 [ASSUMED: nullability of `price_amount` for free topics].
**How to avoid:** DTO `FeaturedTopicDto(isFree: Boolean?, hourlyRate: MoneyDto?)` with `MoneyDto(amount: Long?, currency: String?)`. Decide in the mapper: `isFree == true` gives "Free", else amount and currency both present gives `Money.format`, else "—". Same rule for `/details` topics via `price.is_free`.
**Warning signs:** A free topic renders a price. A decoding test with a free fixture fails.

### Pitfall 2: Moshi codegen strictness
**What goes wrong:** One missing or null key throws `JsonDataException`, and `apiCall` maps it to `ApiException.Decoding`. The whole home screen errors.
**Why it happens:** Non-null Kotlin properties without defaults are required. UUIDs are not supported natively. `rating` is JSON number (Postgres `numeric`), `review_count` a bigint.
**How to avoid:** Ids as `String`. Every field the server may send as null or omit is `T? = null`. `featured_topics` is optional (`List<…>? = null`). Do not model `rating_distribution` or `reschedule` in this phase (unknown keys are ignored). `is_bookable` is `Boolean? = null`, treated as `true` when null (iOS parity), although the current backend always sends it.
**Warning signs:** `ResponseDecodingTest` fixtures built from the real controller output fail.

### Pitfall 3: Stale response overwrites newer filter
**What goes wrong:** The user taps chip A then chip B quickly. A's slower response lands last and the list shows A's experts under B's highlight.
**How to avoid:** Latest-wins `Job` per request kind (Pattern 3). Test with a fake handler that suspends on a `CompletableDeferred` for the first call.

### Pitfall 4: TTL clock and rollback
**What goes wrong:** Using `SystemClock.elapsedRealtime()` (the repo's `MonotonicClock`) for a timestamp stored on disk makes the cache look fresh or stale at random after a reboot. A user who moves the wall clock backwards makes the cache look fresh forever.
**How to avoid:** Inject a `java.time.Clock` (provide `Clock.systemUTC()` in `ClockModule`). Fresh means `0 <= now - fetchedAt < 7 days`. A negative age counts as stale. iOS does not guard this, which is a small deliberate hardening.

### Pitfall 5: Second DataStore needs a qualifier, and one instance per file
**What goes wrong:** Hilt `DuplicateBindings` for `DataStore<Preferences>`, or a runtime crash "multiple DataStores active for the same file".
**How to avoid:** Add `@Qualifier annotation class CategoryCache` and provide `@Provides @Singleton @CategoryCache DataStore<Preferences>` with `preferencesDataStoreFile("clickme_categories")`. The existing unqualified binding for `SessionStore` stays as it is. Create the store through `PreferenceDataStoreFactory.create` exactly once, as `SessionModule` does.

### Pitfall 6: Money tests are ICU-version sensitive
**What goes wrong:** Tests that compare whole strings pass on one JDK and fail on another, because the space before a currency symbol and in `h:mm a` differs (NBSP U+00A0 vs narrow NBSP U+202F) between CLDR versions [ASSUMED: CLDR version differences; verify when the tests first run on JBR 25].
**How to avoid:** For `Locale.US` the assertions "$50.00" and "¥500" are stable. For a non-US locale (de/fr) normalize whitespace first (map U+00A0 and U+202F to a plain space) or assert with `contains`. Use the same normalization for the time-range test.

### Pitfall 7: `Currency.getInstance` and pseudo-currencies
**What goes wrong:** An unknown code throws `IllegalArgumentException`. `XXX` returns `defaultFractionDigits == -1`.
**How to avoid:** Catch `IllegalArgumentException`, and treat a negative digit count as 2 (UI-SPEC). Fallback string is `"{CODE} {amount}"`. Free and missing prices are the caller's decision.

### Pitfall 8: Nav3 key identity and process death
**What goes wrong:** Two entries with equal keys share a `contentKey` and therefore a ViewModel. Keys that are not `@Serializable` crash the saved back stack.
**How to avoid:** `ExpertProfileKey` and `BookingKey` are `@Serializable data class … : NavKey`. Only the Explore stack pushes them, and one profile is on top at a time, so no duplicate arises. Keep key payloads small (strings, one double, one int, a short tag list). The release build (`assembleProdRelease`) must be part of the plan's verification, because key serialization under R8 is the one place new keys could differ from the Phase 1-4 ones [ASSUMED: no extra keep rules are needed, since existing `data object`/`data class` keys already pass R8].

### Pitfall 9: Parallel profile loads and 404 semantics
**What goes wrong:** (a) A reviews failure blanks the screen. (b) A details 404 is shown as a generic error. (c) A details failure still shows the Book button.
**How to avoid:** Map `ApiException.Http` with `status == 404` to `Unavailable`. Other `ApiException` to `Error`. A reviews failure only keeps the seed rating. While details are loading or failed, the Book slot shows a skeleton or nothing, never an enabled button (UI-SPEC). Retry re-sends both requests.

### Pitfall 10: Own-profile appears in recommended experts
**What goes wrong:** The `home_feed` query does not exclude the caller. A user with expert role in Client mode sees themselves and can reach Book Session, and the server later answers `422 CANNOT_BOOK_SELF`.
**How to avoid:** Phase 5 follows iOS (no special handling). Record it as a Phase 6 item: map `ErrorCode.CannotBookSelf` there. See Open Questions.

### Pitfall 11: Existing test and a11y expectations
`HomeShellsTest.clientShellShowsFiveTabsWithExploreSelectedAndAvatarButton` asserts "Nothing here yet.". The slot default in Pattern 1 keeps it passing. New Explore tests must not use `hasText("Explore")` alone, because the tab label, the bar title and the hero collide. Use the `isSelectable()` form from `HomeShellsTest`.

## Code Examples

### Retrofit surface (add to `ClickMeApi` and `ApiRequestUrls`)
```kotlin
// ApiRequestUrls
const val CLIENT_HOME = "client/home"
const val CLIENT_BOOKINGS = "client/bookings"
const val CATEGORIES = "categories"
const val EXPERT_DETAILS = "experts/{id}/details"
const val EXPERT_REVIEWS = "experts/{id}/reviews"

// ClickMeApi
@GET(ApiRequestUrls.CLIENT_HOME)
suspend fun getClientHome(@Query("category") category: String? = null): SuccessDataResponse<ClientHomeData>   // null is omitted by Retrofit

@GET(ApiRequestUrls.CLIENT_BOOKINGS)
suspend fun getClientBookings(@Query("type") type: String, @Query("page") page: Int = 1, @Query("limit") limit: Int = 20): SuccessDataResponse<ClientBookingsPayload>

@GET(ApiRequestUrls.CATEGORIES)
suspend fun getCategories(): SuccessDataResponse<CategoriesPayload>

@GET(ApiRequestUrls.EXPERT_DETAILS)
suspend fun getExpertDetails(@Path("id") expertId: String): SuccessDataResponse<PublicProfileDetailsData>

@GET(ApiRequestUrls.EXPERT_REVIEWS)
suspend fun getExpertReviews(@Path("id") expertId: String, @Query("page") page: Int = 1, @Query("limit") limit: Int = 3): SuccessDataResponse<ExpertReviewsPayload>
```
Kotlin default arguments on Retrofit interface methods are fine as long as callers pass them explicitly in the repository. `FakeClickMeApi` must implement all five or the test source set stops compiling. `/categories` needs no change to `publicPaths`: the bearer is attached, which the backend ignores on that route. [VERIFIED: backend `routes/index.js` serves `categories` before the authenticate guard]

### DTOs (wire shape verified against the backend source)
```kotlin
@JsonClass(generateAdapter = true)
data class ClientHomeData(
    @Json(name = "trending_categories") val trendingCategories: List<TrendingCategoryDto>,
    @Json(name = "recommended_experts") val recommendedExperts: List<RecommendedExpertDto>,
    @Json(name = "featured_topics") val featuredTopics: List<FeaturedTopicDto>? = null,
)
// TrendingCategoryDto: id (slug String), name?, icon_name?, color_accent?
// RecommendedExpertDto: expert_id, full_name?, title?, profile_image_url?, rating: Double?, review_count: Int?,
//                       years_experience: Int?, expertise_tags: List<String>?, is_favorite: Boolean? (decode, not shown)
// FeaturedTopicDto: topic_id, title, duration_mins: Int?, hourly_rate: MoneyDto?, is_free: Boolean?,
//                   expert: {expert_id, full_name?, profile_image_url?}, rating: Double? (server sends it; iOS ignores it)
// MoneyDto: amount: Long?, currency: String?

// Details: data.{is_favorite, is_bookable, available_meeting_types, profile{name,title?,bio?,stats{experience_years?,total_bookings,avg_response_time?}},
//               topics_of_discussion[{id,title,duration_mins?,description?,icon_slug?,price{amount?,currency?,is_free}}], recent_reviews: []}
// Reviews:  data.{summary{expert_id, overall_rating: Double?, total_reviews: Int}, reviews[{id, reviewer{name?,avatar_url?},
//               session_context{topic?,date?,date_label?}, rating: Int, comment?, created_at, verified_booking}], pagination}
// Bookings: data.{bookings[{booking_id, expert{id,full_name?,avatar_url?}, topic?, start_time: Instant, end_time: Instant,
//               meeting_type: String, status: String}], pagination{current_page,total_pages,total_count}}
// Categories: data.{categories[{id, name, icon_name?, color_accent?, description?}]}   (CategoryItem is also the cache element)
```
`PaginationMeta` already accepts both pagination envelopes (`has_more` and `total_count` are optional). `start_time`/`end_time` decode through the existing `InstantAdapter`. [VERIFIED: backend `clientBooking.controller.js`, `reviews.controller.js`, `publicProfile.controller.js`, migration `20261016000100`]

### CategoryStore
```kotlin
@Singleton
class CategoryStore @Inject constructor(
    private val api: ClickMeApi,
    @CategoryCache private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) {
    private val adapter = MoshiProvider.moshi.adapter<List<CategoryItem>>(
        Types.newParameterizedType(List::class.java, CategoryItem::class.java),
    )
    private val mutex = Mutex()
    private var memory: List<CategoryItem>? = null
    private var memoryFetchedAt = 0L

    suspend fun fetch(): List<CategoryItem> = mutex.withLock { freshCache() ?: fetchAndSave() }
    suspend fun refresh(): List<CategoryItem> = mutex.withLock { fetchAndSave() }

    private fun isFresh(fetchedAt: Long): Boolean = (clock.millis() - fetchedAt) in 0 until TTL_MS

    private suspend fun freshCache(): List<CategoryItem>? {
        memory?.let { if (isFresh(memoryFetchedAt)) return it }
        val prefs = dataStore.data.first()
        val at = prefs[FETCHED_AT] ?: return null
        if (!isFresh(at)) return null
        val list = prefs[DATA]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: return null
        memory = list; memoryFetchedAt = at
        return list
    }

    private suspend fun fetchAndSave(): List<CategoryItem> {
        val list = apiCall { api.getCategories() }.data.categories      // throws ApiException on failure; cache untouched
        val now = clock.millis()
        dataStore.edit { it[DATA] = adapter.toJson(list); it[FETCHED_AT] = now }
        memory = list; memoryFetchedAt = now
        return list
    }

    companion object {
        const val FILE_NAME = "clickme_categories"
        const val TTL_MS = 7L * 24 * 60 * 60 * 1000
        private val DATA = stringPreferencesKey("categories_json")
        private val FETCHED_AT = longPreferencesKey("categories_fetched_at")
    }
}
```
Do not catch the `ApiException` here. The sheet ViewModel maps it to the Retry state. Tests: first `fetch()` calls the API once; a second `fetch()` within 7 days makes **zero** further calls (assert `FakeClickMeApi.categoriesCalls`); after advancing a mutable test `Clock` past 7 days `fetch()` calls again; `refresh()` always calls; a failed refresh leaves the old cache intact; a corrupt JSON string is treated as no cache.

### Money
```kotlin
object Money {
    fun format(minorUnits: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val currency = try { Currency.getInstance(currencyCode.uppercase(Locale.ROOT)) } catch (_: IllegalArgumentException) { null }
        val digits = currency?.defaultFractionDigits?.takeIf { it >= 0 } ?: 2
        val major = BigDecimal.valueOf(minorUnits).movePointLeft(digits)
        if (currency == null) return "${currencyCode.uppercase(Locale.ROOT)} ${major.setScale(digits).toPlainString()}"
        return NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(major)
    }
}
```
Expected (Locale.US): `5000, "USD"` gives `$50.00`; `500, "JPY"` gives `¥500`; `5000, "CAD"` gives `CA$50.00`. Add one non-US-locale case and the unknown-code case (the UI-SPEC checklist asks for all four). [ASSUMED: exact symbol output per locale; verify in the first test run]

### Today's Sessions filter (device zone, injectable clock)
```kotlin
private val JOINABLE = setOf("confirmed", "in_progress", "pending_reschedule")

fun List<ClientBookingItem>.todaysSessions(clock: Clock, zone: ZoneId = ZoneId.systemDefault()): List<ClientBookingItem> {
    val today = LocalDate.now(clock.withZone(zone))
    return filter { it.status in JOINABLE && it.startTime.atZone(zone).toLocalDate() == today }
        .sortedBy { it.startTime }
}
```
Test cases: a 23:30 local booking whose UTC date is tomorrow still counts as today; a booking just after local midnight does not; `pending_approval`, `completed` and `cancelled` are excluded; ordering ascending. The time range is `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)` for start and end, joined with " – ". Request `limit=20` as iOS does. The server returns upcoming ascending, so today's items are near the front. (A confirmed booking whose end time already passed today is still listed. That is iOS behavior and is kept.)

### Parallel profile load with independent failure handling
```kotlin
suspend fun load(id: String): ProfileLoad = coroutineScope {
    val details = async { try { Result.success(apiCall { api.getExpertDetails(id) }.data) } catch (e: ApiException) { Result.failure(e) } }
    val reviews = async { try { Result.success(apiCall { api.getExpertReviews(id, 1, 3) }.data) } catch (e: ApiException) { Result.failure(e) } }
    ProfileLoad(details.await(), reviews.await())
}
// ViewModel: details.exceptionOrNull() as? ApiException.Http with status == 404 -> Unavailable; any other failure -> Error(retry)
```
`Result` here only carries `ApiException`. Cancellation is never caught, because `apiCall` rethrows it and the `catch` is typed to `ApiException`.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `NavHost` with `SavedStateHandle` route args | Nav3 keys passed by assisted injection | Nav3 stable | Use `creationCallback`; the key is a constructor argument. |
| `ExperimentalLayoutApi` for `FlowRow` | Stable | Compose 1.8 | No `@OptIn` is expected in this repo. |
| `SwipeRefresh` (accompanist) / `pullrefresh` | M3 `PullToRefreshBox` | M3 1.3+ | Already in use. |

**Deprecated/outdated:** iOS's `NumberFormatter` trick of dropping decimals on whole amounts is replaced by D-20 (always full decimals).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `price_amount` is null or 0 for free topics, so the DTO treats both amount and currency as optional | Pitfall 1 | Low. The DTO is already nullable and the `is_free` flag decides. |
| A2 | CLDR whitespace differences (NBSP vs narrow NBSP) can break whole-string locale assertions on JBR 25 | Pitfall 6 | Low. Flaky unit tests, fixed by normalizing whitespace. |
| A3 | New `@Serializable` NavKeys need no extra R8 rules, since existing keys pass `assembleProdRelease` | Pitfall 8 | Medium. A release-only crash on profile push. Mitigation: run the release build and add a keep rule if needed. |
| A4 | `Money.format` outputs `CA$50.00` for CAD under `Locale.US` and a "JP¥" prefix under `en_CA` | Code Examples | Low. Only the exact expected strings in tests change. |
| A5 | The deployed backend matches `backend-reference` (migration `20261016000100` is the live `home_feed`) | Summary, DTOs | Medium. If the live `featured_topics` lacks `is_free`, D-21 cannot be met from home data. The DTO is nullable, so nothing crashes. Verify with one real response during execution. |

## Open Questions

1. **Own card in Recommended Experts (multi-role tester)**
   - What we know: `home_feed` filters `p.role = 'expert'` and never excludes `p_user_id`. iOS has no handling. `CANNOT_BOOK_SELF` is a 422 at booking time. [VERIFIED: migration source and `ErrorCode.CannotBookSelf`]
   - What's unclear: whether the product wants the card hidden or the Book button disabled client-side.
   - Recommendation: no change in Phase 5 (iOS parity). Add `CANNOT_BOOK_SELF` handling to the Phase 6 plan. Hiding self would also need the current user id in the Explore ViewModel, so defer unless the user asks.

2. **`featured_topics[].rating` is available but D-15/UI-SPEC pass `rating = null` from featured cards**
   - What we know: the server sends a rating (0.0 when there are no reviews, so it cannot tell "no reviews" from "unrated") but no review count.
   - Recommendation: keep the UI-SPEC behavior (null seed, skeleton until the reviews summary arrives). Do not decode `rating` into the key. It is ambiguous without a count.

3. **`GET /experts/{id}` (full public profile) already returns header rating, review total, avatar and `expertise` tags**
   - What we know: it would make the Featured-Topic entry richer. D-15 locks `/details` plus `/reviews`, and CONTEXT explicitly accepts the sparser hero.
   - Recommendation: do not use it in Phase 5. Not re-opened.

4. **Manual device checks** (pull-to-refresh feel, sheet search with IME, 200% font scale, skeleton transitions on a slow network) are deferred per the project preference. Log them as open items in the plan's UAT file, as in Phase 4.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK for Gradle (Android Studio JBR) | Build and unit tests | ✓ | OpenJDK 25.0.3 at `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | — (system `java` is 18, so `JAVA_HOME` must point at the JBR) |
| Gradle wrapper + `local.properties` | Build | ✓ | wrapper present, `local.properties` present | Worktree executors must copy `local.properties` (project memory) |
| iOS reference symlink | Porting | ✓ | `ios-reference/` | — |
| Backend reference symlink | Contract check | ✓ | `backend-reference/` | — |
| Material icons (extended + core) | Category map, profile icons | ✓ | verified in the Gradle cache | Substitute the nearest icon |
| `ctx7` CLI / Context7 MCP | Docs lookup | ✗ | — | WebSearch/WebFetch of developer.android.com (used) |
| Network access to production backend | Optional manual check of live `/client/home` | not tested | — | Unit tests use fixtures |

**Icon check (UI-SPEC open item 2):** every icon in the category map and screens exists, with these notes. Core artifact (`material-icons-core`): `Outlined.Home`, `Edit`, `Info`, `Search`, `Check`, `Filled.Star`, `Filled.Person`. Extended artifact: `Memory`, `Work`, `Lightbulb`, `PersonAdd`, `RecordVoiceOver`, `DesignServices`, `School`, `Engineering`, `ShowChart`, `FitnessCenter`, `Restaurant`, `SportsEsports`, `MedicalServices`, `Gavel`, `MusicNote`, `CameraAlt`, `Speed`, `Psychology`, `Science`, `SportsBasketball`, `RocketLaunch`, `Code`, `Flight`, `Spa`, `Translate`, `Campaign`, `GridView`, `PersonOff`, `EventBusy`, `CalendarMonth`, `Schedule`, `Videocam`, `CloudOff`, `ErrorOutline`. Only `AutoMirrored` variants are needed if a direction-sensitive icon is added. [VERIFIED: class listing of the Gradle-cached jars]

**Missing dependencies with no fallback:** none.

## Validation Architecture

`workflow.nyquist_validation` is absent from `.planning/config.json`, so this section is required.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 + Compose UI test (`createComposeRule` from `junit4.v2` under `RobolectricTestRunner`) |
| Config file | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| Full suite command | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease && bash scripts/check-design-rules.sh` |

Baseline: 876 tests per flavor at the end of Phase 4 [CITED: 04-VALIDATION.md]. Pure-JVM tests take about 30 s. The full run takes several minutes.

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DISC-01 | `getClientHome` sends no `category` when unfiltered and `category=a,b` (decoded) when filtered; no `page`/`limit` | MockWebServer | `--tests '*ClickMeApiTest'` (extend) | extend |
| DISC-01 | Home, bookings, details and reviews fixtures decode (free topic, null `featured_topics`, null rating, missing optional keys) | unit | `--tests '*ResponseDecodingTest'` (extend) | extend |
| DISC-01 | ExploreViewModel: loading → loaded sections; one `/client/home` failure puts all three sections in Error with the server message; Retry re-calls the same request; Today's failure leaves home untouched | unit (fake API) | `--tests '*ExploreViewModelTest'` | ❌ Wave 0 |
| DISC-01 | Chip tap selects one slug and re-fetches `category=slug`; second tap clears; Apply sends sorted comma list; only experts change (topics unchanged); stale response from an older filter is discarded; selection survives `SavedStateHandle` recreation | unit | same | ❌ Wave 0 |
| DISC-01 | Empty experts: filter active gives "Clear filter" action, no filter gives the plain message | unit + Compose | `--tests '*ExploreViewModelTest' --tests '*ExploreScreenTest'` | ❌ Wave 0 |
| DISC-01 | Today's filter: local-zone date boundaries, joinable statuses only, ascending, time range formatting | unit | `--tests '*TodaysSessionsFilterTest'` | ❌ Wave 0 |
| DISC-01 | Explore content: section order, hidden empty Featured/Today, `Join Call` disabled with tag `todays_session_join`, "View all (n)", no "See all", no heart | Robolectric Compose | `--tests '*ExploreScreenTest'` | ❌ Wave 0 |
| DISC-01 | Free topic shows "Free"; paid shows `Money.format`; missing shows "—" | unit/Compose | `--tests '*ExploreScreenTest' --tests '*MoneyTest'` | ❌ Wave 0 |
| DISC-01 | Category icon map: every slug from `CategoryIconMap.swift` present (lower-cased), unknown slug gets the fallback | unit | `--tests '*CategoryIconMapTest'` | ❌ Wave 0 |
| DISC-02 | `CategoryStore`: fetch→cache; second fetch within 7 days makes zero API calls; stale (>7 d) refetches; `refresh()` always calls; failure keeps old cache; corrupt cache ignored; negative age treated as stale; survives a new instance on the same DataStore file; not cleared by `SessionStore.clear()` | unit (real DataStore, fake API, mutable Clock) | `--tests '*CategoryStoreTest'` | ❌ Wave 0 |
| DISC-02 | Sheet: cached list shows without spinner; error shows Retry which calls `refresh()`; search is local and case-insensitive; no-match state; Apply returns the draft; dismiss discards; Clear empties the draft only | unit + Compose (`PickerSheetContent`) | `--tests '*CategoriesSheetViewModelTest' --tests '*AllCategoriesSheetTest'` | ❌ Wave 0 |
| DISC-02 | `PickerRow` with `leadingIcon` and `Role.Checkbox`; default behavior unchanged | Compose | `--tests '*PickerComponentsTest'` (extend) | extend |
| DISC-03 | Profile VM: preview renders before network; details and reviews launched in parallel (both calls started before either completes); reviews failure keeps seed; details 404 gives Unavailable; 5xx/network gives Error; Retry re-sends both; `is_bookable=false` gives the disabled state; null `is_bookable` gives bookable | unit | `--tests '*ExpertProfileViewModelTest'` | ❌ Wave 0 |
| DISC-03 | Empty Expertise/Topics/Reviews hidden; zero reviews reads "No reviews yet"; null experience drops that cell; topic rows show Free/price/"—"; at most 3 reviews; "Anonymous" fallback | Compose | `--tests '*ExpertProfileScreenTest'` | ❌ Wave 0 |
| DISC-03 | Book Session pushes `BookingKey` with id, name, title, image; the stub screen shows the expert and "Booking is coming soon" and has no bottom bar | Compose + shell | `--tests '*ExpertProfileScreenTest' --tests '*MakeABookingStubScreenTest'` | ❌ Wave 0 |
| DISC-03 | Shell: Explore tab hosts the injected content; profile and stub entries registered; bottom bar hidden on the stub; existing placeholder test still passes with default slots | Compose | `--tests '*HomeShellsTest'` (extend) | extend |
| D-18..21 | `Money.format`: USD `$50.00`, JPY `¥500`, a non-US locale, unknown code fallback, negative digits to 2, large values | unit | `--tests '*MoneyTest'` | ❌ Wave 0 |
| Shared | `RatingStars` filled count, `SkeletonBlock` static at animator scale 0, `SectionHeader`/`SectionError` semantics, `ExpertTagChip` single line | Compose | `--tests '*Phase5ComponentsTest'` | ❌ Wave 0 |
| Wiring | `ApiRequestUrls` constants and bearer allowlist (`categories` and the four others are authed) | unit | `--tests '*ApiRequestUrlsTest'` (extend) | extend |
| Release | New `@Serializable` keys survive R8 | build | `./gradlew assembleProdRelease` | n/a |

### Sampling Rate
- **Per task commit:** the targeted `--tests` pattern for the files touched.
- **Per wave merge:** `./gradlew testDevDebugUnitTest ktlintCheck && bash scripts/check-design-rules.sh`.
- **Phase gate:** full suite green on both flavors, including `assembleProdRelease`, before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `FakeClickMeApi`: add `clientHomeHandler`, `clientBookingsHandler`, `categoriesHandler`, `expertDetailsHandler`, `expertReviewsHandler` with call counters (`categoriesCalls` is the success-criterion-2 evidence), and record the last `category` argument.
- [ ] Test fixtures: JSON strings built from the controller output (home with and without `featured_topics`, free topic, details with `is_bookable` true/false, reviews with null `overall_rating`, bookings in several statuses).
- [ ] Mutable test `Clock` helper (a small `MutableClock : Clock`) and a `Clock` provider in `ClockModule`.
- [ ] `HomeShellsTest` stays valid only if the shell's new slots default to the placeholder (Pattern 1).
- [ ] Test files listed in the map above.
- Framework install: none.

## Security Domain

`security_enforcement` is not set in config, so it is treated as enabled. This phase is read-only and handles no credentials.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (existing bearer and refresh from Phase 2) | — |
| V3 Session Management | no | Tokens stay in `EncryptedPrefsTokenStore`. Nothing in this phase touches them. |
| V4 Access Control | yes (light) | Server enforces roles (`roleCheck('client')` on `/client/home`) and moderation hiding. The client never treats `is_bookable` as authorization. |
| V5 Input Validation | yes | Render server strings as `Text` only. `@Path` encodes the expert id. The search field filters locally and sends nothing. `category` slugs come from server data, never typed text. |
| V6 Cryptography | no | — |
| V8 Data Protection | yes (light) | The `clickme_categories` DataStore holds public reference data only. `allowBackup=false` already. No PII is cached by this phase. |
| V9 Communications | yes | HTTPS base URL (existing). Image URLs load through Coil on the same network stack. |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Untrusted URL or text in server data (avatars, bios, review comments) | Tampering / Spoofing | Plain `Text` (no HTML or markdown), `Avatar` through Coil, no `LinkAnnotation` or autolinking. |
| Information leakage in logs | Information Disclosure | Do not log response bodies or expert fields. `ApiException.message` already carries status and code only. |
| Route key carrying data | Tampering | Keys hold display seed only (no tokens or secrets). The server re-validates on every call. |
| Stale cache poisoning | Tampering | Cache decoded through a typed Moshi adapter. A decode failure counts as a miss. |
| Suspended expert still reachable by id | Elevation / Information Disclosure | Server returns 404. The client shows the unavailable state (D-13). |

## Sources

### Primary (HIGH confidence)
- Backend source in `backend-reference/`: `src/controllers/discovery.controller.js` (`getHomeFeed`), `supabase/migrations/20261016000100_discovery_hide_moderated_users.sql` (live `home_feed` shape, `featured_topics.is_free`/`rating`), `src/schemas/discovery.schema.js` (`homeQuerySchema`), `src/controllers/publicProfile.controller.js` (`getExpertDetails`, 404 rules), `src/controllers/reviews.controller.js` (`expertReviews`), `src/controllers/clientBooking.controller.js` (`list`, statuses, ascending order), `src/controllers/categories.controller.js`, `docs/ios/PRICING-LIMITS.md` §5, `docs/ios/USER-REPORTING.md` §5.
- iOS source in `ios-reference/`: `ExploreClientViewModel.swift`, `CategoriesModalViewModel.swift`, `ExpertProfileViewModel.swift`, `ExpertProfileView.swift`, `ExpertProfileActionButtons.swift`, `CategoryStore.swift`, `CategoryIconMap.swift`, `ClientHomeData.swift`, `PublicProfileDetailsData.swift`, `ReviewSummary.swift`, `ReviewItem.swift`, `ClientBookingItem.swift`, `ClickMeAPI+Discovery.swift`.
- Android code: `HomeClientScreen.kt`, `HomeTabScaffold.kt`, `Navigator.kt`, `NavigationState.kt`, `ClickMeApi.kt`, `ApiRequestUrls.kt`, `SessionStore.kt`, `SessionModule.kt`, `ClockModule.kt`, `PickerSheet.kt`, `EmptyState.kt`, `Avatar.kt`, `PressScale.kt`, `GreenGlowBlobLayer.kt`, `HomeShellsTest.kt`, `FakeClickMeApi.kt`, `scripts/check-design-rules.sh`, `app/build.gradle.kts`, `gradle/libs.versions.toml`.
- Android official recipe, passing Nav3 keys to Hilt ViewModels: https://developer.android.com/guide/navigation/navigation-3/recipes/passingarguments
- Gradle-cached `material-icons-core` and `material-icons-extended` jars (icon existence).

### Secondary (MEDIUM confidence)
- What's new in Jetpack Compose, April 2025 (FlowRow stable in 1.8): https://android-developers.googleblog.com/2025/04/whats-new-in-jetpack-compose-april-25.html

### Tertiary (LOW confidence)
- None relied on. Items resting on training knowledge are in the Assumptions Log.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. Nothing new. All versions are from the repo.
- Architecture: HIGH. Follows the Phase 1-4 patterns and the official Nav3 recipe. The shell-slot approach is derived from the existing test constraints.
- Wire contract: HIGH for field names and nesting (read from source). MEDIUM for live-backend parity (A5).
- Pitfalls: HIGH for the structural ones. MEDIUM for ICU formatting (A2) and R8 (A3).

**Research date:** 2026-10-21
**Valid until:** about 2026-11-20 (stable stack). Re-check the backend `home_feed` shape if the backend repo changes before execution.
