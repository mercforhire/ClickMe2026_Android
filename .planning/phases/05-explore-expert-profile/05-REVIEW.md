---
phase: 05-explore-expert-profile
reviewed: 2026-10-22T02:48:42Z
depth: standard
files_reviewed: 58
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ExpertTagChip.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PickerSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PriceText.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/RatingStars.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SectionError.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SectionHeader.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SkeletonBlock.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/constants/CategoryIconMap.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/CategoryCacheModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ClockModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/Qualifiers.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CategoryStore.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/CategoriesPayload.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientBookingsPayload.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientHomeData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertReviewsPayload.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/PublicProfileDetailsData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileAboutCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileExpertiseSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileHero.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileReviewsCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileStatsBar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileTopicsCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreNavigation.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/categoriesmodal/AllCategoriesSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/categoriesmodal/CategoriesSheetViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExpertCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreCategoriesSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreCategoryCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreExpertsSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreFeaturedTopicsSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreHeroHeader.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingStubScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/ClientBookingsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/TodaysSessions.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/util/Money.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/util/PriceDisplay.kt
  - app/src/main/res/values/strings_all_categories.xml
  - app/src/main/res/values/strings_discovery_shared.xml
  - app/src/main/res/values/strings_expert_profile.xml
  - app/src/main/res/values/strings_explore.xml
  - app/src/main/res/values/strings_make_a_booking.xml
  - app/src/main/res/values/strings_money.xml
findings:
  critical: 0
  warning: 5
  info: 8
  total: 13
status: issues_found
---

# Phase 5: Code Review Report

**Reviewed:** 2026-10-22T02:48:42Z
**Depth:** standard
**Files Reviewed:** 58
**Status:** issues_found

## Narrative Findings (AI reviewer)

## Summary

I reviewed the Phase 5 discovery surface: Explore (ViewModel, sections, All Categories sheet), Expert Profile (ViewModel, screen, subviews), CategoryStore and its DataStore module, the new DTOs and Retrofit endpoints, the shared components, Money/PriceDisplay, and the client shell wiring. I also checked the code against the iOS reference (`ExploreClientViewModel`, `CategoryStore.swift`, `ClickMeAPI+Discovery`) and the backend (`discovery.controller.js`, `clientBooking.controller.js`, `reviews.controller.js`, `publicProfile.controller.js`, the `home_feed` RPC) to confirm DTO shapes, the meaning of `category` slugs, and sort order.

The DTOs match the server contract. The job cancellation and "newest selection wins" guards in `ExploreViewModel` are mostly sound, and coroutine cancellation is handled correctly through `apiCall`. Nav3 entries are scoped to their own ViewModel stores, so each profile gets its own ViewModel.

I found no blockers. The main defects:
- `CategoryStore` can throw a non-`ApiException` that crashes the app.
- Double taps push duplicate Nav3 keys.
- A race between the home load and an experts load can replace freshly loaded sections with errors.
- One-shot appear animations replay inside lazy lists.
- The time formatting ignores the user's 24-hour setting.

## Warnings

### WR-01: CategoryStore lets DataStore IOExceptions escape, which crashes the All Categories sheet

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CategoryStore.kt:63,76-79` (caught in `screens/clientmode/explore/categoriesmodal/CategoriesSheetViewModel.kt:104-109`)
**Issue:** `freshCache()` calls `dataStore.data.first()` and `fetchAndSave()` calls `dataStore.edit {}`. Both throw `IOException` on a real read or write failure, such as a full disk or an I/O error. `ReplaceFileCorruptionHandler` only covers `CorruptionException`. These exceptions do not go through `apiCall`, so they reach the caller as plain `IOException`. `CategoriesSheetViewModel.load()` catches only `ApiException`, so the exception escapes `viewModelScope.launch` with no handler and crashes the app. Separately, when the network fetch succeeds but the write fails, the list that was just downloaded is thrown away and the user sees a failure, even though the data is in memory.
**Fix:** Treat the cache as best-effort, so cache I/O can never fail the fetch:
```kotlin
private suspend fun freshCache(): List<CategoryItem>? {
    memory?.let { if (isFresh(memoryFetchedAt)) return it }
    val prefs = try { dataStore.data.first() } catch (_: IOException) { return null }
    ...
}

private suspend fun fetchAndSave(): List<CategoryItem> {
    val list = apiCall { api.getCategories() }.data.categories
    val now = clock.millis()
    memory = list
    memoryFetchedAt = now
    try {
        dataStore.edit { it[DATA_KEY] = adapter.toJson(list); it[FETCHED_AT_KEY] = now }
    } catch (_: IOException) { /* cache write is best-effort */ }
    return list
}
```

### WR-02: Double taps push duplicate routes (expert card, featured topic, Book Session)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt:77,89`, `screens/clientmode/explore/ExploreScreen.kt:83-84`, `screens/clientmode/expertprofile/ExpertProfileScreen.kt:86`
**Issue:** `Navigator.navigate()` always appends a non-tab route. The expert card, featured topic card and Book Session button are still composed and still accept input while the push transition runs, because AnimatedContent does not block input on outgoing content. A quick double tap therefore pushes two equal `ExpertProfileKey`s or `BookingKey`s. Both are data classes, so both entries get the same default Nav3 `contentKey`. The Nav3 1.2.0 KDoc says entries with the same `contentKey` are "handled as sharing the same content and/or NavEntryDecorator state". The two back-stack entries then share one ViewModel and one saveable state, and the user has to press Back twice to get back to Explore.
**Fix:** Guard the push. For example, skip it when the top of the current stack already equals the route:
```kotlin
fun navigate(route: NavKey) {
    ...
    } else {
        val stack = currentStack()
        if (stack.lastOrNull() == route) return
        state.lastTransition = NavTransition.Push
        stack.add(route)
    }
}
```
Alternatively, debounce the click handlers.

### WR-03: A failed initial home load overwrites sections a concurrent experts load already filled

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModel.kt:130-133, 230-244`
**Issue:** Steps to reproduce:
1. The initial `loadHome(refresh = false)` is in flight. All sections are Loading and `keepData = false`.
2. The user opens All Categories and taps Apply. `loadExperts()` does not cancel `homeJob`.
3. The experts request succeeds first. It fills `categories` and `featuredTopics`, because they were not yet Loaded (lines 153-158).
4. `homeJob` then fails. `withHomeFailure(keepData = false, ...)` calls `orError()`, which returns `Error` whenever `keepData` is false, whatever the current state.

Valid Trending Categories and Featured Topics on screen get replaced by error panels. The doc comment ("sections that were not loaded still show the error") says this should not happen.
**Fix:** Have `orError()` keep any section that is currently Loaded, whatever `keepData` is, because only sections still waiting on this request should fail:
```kotlin
fun <T> SectionState<T>.orError(): SectionState<T> =
    if (this is SectionState.Loaded) this else SectionState.Error(message)
```
Alternatively, have `loadExperts()` cancel an in-flight `homeJob` and take over filling all three sections.

### WR-04: One-shot appear animations replay whenever a lazy item scrolls back into view

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt:224-233`, `screens/clientmode/expertprofile/ExpertProfileScreen.kt:292-305`
**Issue:** `AnimatedSection` and `SectionAppear` keep their `MutableTransitionState(false)` in a plain `remember` inside `LazyColumn` items ("today", "featured", "details", "reviews"). When an item scrolls fully out of view, LazyColumn disposes it and the remembered state is lost. When it scrolls back in, the `fadeIn + expandVertically` runs again from zero height. The item grows by its full height over 200 ms while the user is scrolling toward it, so the content above or below visibly jumps. On a long expert profile, the whole details item (About, Expertise, Topics) collapses and re-expands each time the user scrolls back up from Reviews.
**Fix:** Run the appear animation only on the state change, not on composition. One option is to hoist an "already shown" flag into the ViewModel or UI state. Another is to use `rememberSaveable { mutableStateOf(false) }` and skip the animation once it is set. A third is to use `Modifier.animateItem()` on the lazy items instead of `AnimatedVisibility` inside them.

### WR-05: Today's Sessions time range ignores the user's 24-hour setting and goes stale on a locale change

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/TodaysSessions.kt:29-38`, `screens/clientmode/explore/ExploreViewModel.kt:188-194`
**Issue:** The KDoc promises "the device's 12 or 24 hour style", but `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)` only uses the locale's default pattern. A user in `en_US` who has turned on 24-hour time in Android settings still sees "3:00 PM". Also, the string is built in the ViewModel with `Locale.getDefault()` and stored in `TodaysSessionUi.timeRange`. The ViewModel survives a locale configuration change, so the text stays in the old locale until the next pull-to-refresh.
**Fix:** Keep `startTime` and `endTime` (as `Instant`s) in `TodaysSessionUi` and format them in the composable. Use `android.text.format.DateFormat.getTimeFormat(context)`, or `DateFormat.is24HourFormat(context)` to pick between the `"H:mm"` and `"h:mm a"` skeletons with `DateFormat.getBestDateTimePattern(locale, ...)`.

## Info

### IN-01: Explore card mapping turns "unknown" ratings into zero, which breaks the ExpertProfileKey contract

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModel.kt:257-258`, `screens/clientmode/explore/ExploreNavigation.kt:12-13`, `screens/clientmode/expertprofile/ExpertProfileViewModel.kt:108,155`
**Issue:** `ExpertProfileKey` says a null `rating` or `reviewCount` means "unknown, not zero". `RecommendedExpertDto.toUi()` maps null to `0.0` / `0`, so a card with missing values seeds `HeroRating.NoReviews`. If the reviews request then fails, "No reviews yet" stays on screen. Separately, `overallRating ?: seedRatingValue()` shows "0.0 (N reviews)" when the summary has a count but no average. The current `home_feed` RPC COALESCEs both values, so this is not reachable today.
**Fix:** Keep `rating` and `reviewCount` nullable in `ExpertCardUi` and pass null through to the key. When `overallRating` is null but `total > 0`, show `Hidden` instead of `0.0`.

### IN-02: ClientHomeData KDoc overstates how tolerant the decode is

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientHomeData.kt:6-8,39-51`
**Issue:** The KDoc says "every scalar is nullable so a single missing key cannot fail the whole feed decode". However, `FeaturedTopicDto.title`, `FeaturedTopicDto.expert`, `topicId`, `RecommendedExpertDto.expertId` and `TrendingCategoryDto.id` are required. One featured topic with a null title fails `/client/home` and puts all three sections into the error state. This matches iOS strictness, but the comment is wrong.
**Fix:** Correct the KDoc, or make `title` nullable and drop invalid rows in the mapper.

### IN-03: Today's Sessions depends on page 1 of `upcoming` and is never recomputed across midnight

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModel.kt:171-203`
**Issue:** The backend orders `upcoming` by `start_time` ascending across `pending_approval`, `confirmed`, `pending_reschedule` and `in_progress`, with a limit of 20. Stale earlier-day bookings that never moved out of those statuses come first and can push today's sessions off the page. The list is also filtered once, at init or on refresh, so a screen left open past midnight keeps showing yesterday's sessions.
**Fix:** Accept and document this as a known limit, or page until `startTime` passes the end of today. Recompute the list on `ON_RESUME` or when the date changes.

### IN-04: Duplicated constants and helpers

**File:** `screens/clientmode/expertprofile/subviews/ExpertProfileReviewsCard.kt:25` vs `ExpertProfileRepository.kt:11`; `ExploreScreen.kt:224-233` vs `ExpertProfileScreen.kt:292-305`
**Issue:** `MAX_REVIEWS = 3` duplicates `REVIEWS_PREVIEW_LIMIT`. `AnimatedSection` and `SectionAppear` are the same composable defined twice.
**Fix:** Reuse `REVIEWS_PREVIEW_LIMIT`. Move a single appear helper into `components/` (and fix WR-04 there once).

### IN-05: Unused catch parameters

**File:** `screens/clientmode/expertprofile/ExpertProfileViewModel.kt:115`, `screens/clientmode/explore/categoriesmodal/CategoriesSheetViewModel.kt:107`
**Issue:** `catch (e: ApiException)` never uses `e`.
**Fix:** Use `catch (_: ApiException)`, as `ExploreViewModel.kt:197` already does.

### IN-06: Cross-feature coupling to the login package and an inline fully qualified call

**File:** `screens/clientmode/explore/ExploreUiState.kt:3`, `screens/clientmode/makeabooking/MakeABookingStubScreen.kt:22`, `screens/clientmode/expertprofile/subviews/ExpertProfileHero.kt:66-67`
**Issue:** Explore's UI state depends on `screens.login.FieldError`, and the booking stub uses `screens.login.AuthTopAppBar`. The hero calls `com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer` by its fully qualified name inside the body instead of importing it.
**Fix:** Move `FieldError` and `AuthTopAppBar` to a shared package (for example `components/`) and import `GreenGlowBlobLayer`.

### IN-07: A blank currency code produces malformed price text

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/util/PriceDisplay.kt:33`, `util/Money.kt:26-35`
**Issue:** `PriceDisplay.of` only checks `currency != null`. A blank `""` reaches `Money.format`, `Currency.getInstance("")` throws and is caught, and the result is `" 50.00"` with a leading space.
**Fix:** Use `!currency.isNullOrBlank()` in `PriceDisplay.of`.

### IN-08: Expert Profile Retry re-fetches reviews and flickers the hero rating

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileViewModel.kt:74-83`
**Issue:** Retry only appears in the details error state, but `load()` also cancels and re-requests reviews and resets `rating` to the seed. A loaded "4.8 (12 reviews)" briefly turns back into the seed or a Pending skeleton.
**Fix:** On retry, re-request only the failed request (details), and leave reviews alone when they are already Loaded.

---

_Reviewed: 2026-10-22T02:48:42Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
