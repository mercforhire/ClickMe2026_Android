# Phase 5: Explore & Expert Profile - Context

**Gathered:** 2026-10-21
**Status:** Ready for planning

<domain>
## Phase Boundary

The client's Explore tab replaces the `PlaceholderTabContent` with the iOS Explore home, in this order:

1. Static hero text
2. Today's Sessions
3. Trending Categories chip row, with a "View all" All Categories sheet
4. Featured Topics
5. Recommended Experts

The data comes from `GET /client/home`. Today's Sessions comes from `GET /client/bookings?type=upcoming`.

Tapping an expert card, or a featured topic card, pushes Expert Profile inside the Explore tab's back stack. Expert Profile shows these sections:

- hero
- stats bar
- Book Session
- About
- Expertise
- Topics of Discussion
- up to 3 recent reviews

Book Session pushes a stub booking screen that Phase 6 fills in.

A `CategoryStore` caches `GET /categories` for 7 days. The All Categories sheet reads from it.

Out of scope:
- The Search tab and every "See all" link that leads to it (DEF-2)
- Favorites and the heart (DEF-3)
- The full reviews list (DEF-3)
- Message, Report and Block on Expert Profile (Phase 12)
- The booking flow itself (Phase 6)
- Upcoming Booking Details and Join (Phases 8 and 9)

</domain>

<decisions>
## Implementation Decisions

Carried forward (not re-opened):
- **iOS is the source of truth (DEC-ios-source-of-truth).** Deviate for Android conventions, or for iOS defects backed by a backend spec or by the iOS code's own stated intent.
- **Styling** is a Material 3 adaptation, dark only, in Nunito. Pixel parity is out of scope. The roadmap allows simplifying the hero and visuals.
- **Navigation:** Navigation 3 with per-tab back stacks (Phase 1). Detail screens are pushed with `navigator.navigate(Key)` and registered in the client shell's `entryProvider`.
- **API groups** are ported one feature at a time: add `ApiRequestUrls` constants, `ClickMeApi` methods, and a Hilt `@Singleton` repository using `apiCall {}`.
- **DTOs** are Moshi codegen with an explicit `@Json(name=…)` on multi-word fields.

### Unbuilt destinations
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

### Categories & CategoryStore (DISC-02)
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

### Expert Profile & Explore states (DISC-01, DISC-03)
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

### Price & currency display
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### iOS Explore (source of truth)
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Explore/ExploreClientView.swift`: section order, pull-to-refresh, navigation
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Explore/ExploreClientViewModel.swift`: home fetch, category filter, today's-sessions filter
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Explore/Subviews/`: HeroHeader, TodaysSessionsSection, CategoriesSection/CategoryCard, FeaturedTopicsSection, ExpertsSection/ExpertCard/ExpertTagChip, ExploreBrand
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Explore/Categories Modal/`: All Categories sheet and its view model (D-06, D-10)
- `ios-reference/ClickMe2026/Managers/CategoryStore.swift`: 7-day TTL cache (D-07)
- `ios-reference/Constants/CategoryIconMap.swift`: slug → icon map (D-11)

### iOS Expert Profile (source of truth)
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Expert Profile/ExpertProfileView.swift` and `ExpertProfileViewModel.swift`: sections, Book Session, `is_bookable`, data loading
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Expert Profile/Subviews/`: Hero, StatsBar, ActionButtons, AboutCard, ExpertiseSection, TopicsCard, ReviewsCard (ReportUserSheet is Phase 12)

### Backend contract
- `backend-reference/src/controllers/discovery.controller.js`: `GET /client/home` (no pagination; `category` is comma-separated slugs), `GET /categories`, expert details and reviews
- `backend-reference/supabase/migrations/20261016000100_discovery_hide_moderated_users.sql`: home RPC (moderated users hidden)
- `backend-reference/docs/ios/PRICING-LIMITS.md` §5: minor units, JPY has 0 decimals (D-18)
- `backend-reference/docs/ios/USER-REPORTING.md` §5: suspended or deleted experts return 404 and are treated as unavailable (D-13)

### Project
- `.planning/REQUIREMENTS.md`: DISC-01, DISC-02, DISC-03; DEF-2, DEF-3, DEF-7
- `.planning/ROADMAP.md`: Phase 5 success criteria and planning notes
- `CLAUDE.md`: platform swaps (`.sheet` → `ModalBottomSheet`) and file organization (mirror the iOS tree)

</canonical_refs>

<code_context>
## Existing Code Insights

All paths below are under `app/src/main/java/com/uptrendinvestments/clickmeapp/`.

### Reusable Assets
- `components/avatar/Avatar.kt`: Coil 3 `AsyncImage` with a fallback, in sizes 32/40/56/96/160 dp. Use it for expert cards, featured topics and the profile hero.
- `components/EmptyState.kt`: `EmptyState(icon, heading, body, action)` and `ErrorState(onRetry)`. Use them for D-13, D-16 and D-17.
- `components/backgrounds/GreenGlowBlobLayer.kt`: the static glow for the profile hero (D-14).
- `components/PressScaleButton`, `AsyncSaveButton`: Book Session and the card press feel.
- `PullToRefreshBox` usage in `screens/signup/overview/SignupOverviewScreen.kt`: pattern for Explore pull-to-refresh.
- `model/responses/SuccessDataResponse`, `PaginationMeta`: response wrappers (reviews use pagination).

### Established Patterns
- **API:** add paths to `network/ApiRequestUrls.kt` (relative, no leading slash) and methods to `network/ClickMeApi.kt`, wrapped in `apiCall {}` inside a Hilt `@Singleton` repository (see `screens/signup/SignupSetupRepository.kt`). The bearer token is attached automatically; `/categories` being public is fine.
- **Caching:** `managers/SessionStore.kt` stores JSON in Preferences DataStore through a Moshi adapter. Copy that for CategoryStore, using its own DataStore file so it survives logout (D-08).
- **Navigation:** `navigation/Navigator.kt` and `HomeTabScaffold.kt` push detail screens; each entry has its own ViewModel scope. Note the open Phase 1 WR-05 (`navigateInTab` and `reselect` transitions). It doesn't block in-tab pushes.
- **Shared field:** `ValidatedField` currently forces capitalization to None (WR-04, open). The All Categories search field may need it fixed, or should use a plain search field.

### Integration Points
- `screens/clientmode/home/HomeClientScreen.kt`: `ExploreRoot` currently renders `PlaceholderTabContent`. Replace it with the Explore screen, and register the Expert Profile and stub booking keys in the client shell's `entryProvider`.
- `screens/clientmode/home/subviews/HomeClientTab.kt`: tab keys.
- New files mirror the iOS tree: `screens/clientmode/explore/…`, `screens/clientmode/expertprofile/…`, and a booking stub under the path Phase 6 will use for `MakeABooking`.

</code_context>

<specifics>
## Specific Ideas

- Today's Sessions is deliberately visible but inert (D-01, D-02), so the Explore home looks complete for demos. Phases 8 and 9 own the interactions.
- The All Categories sheet is the one MVP surface for `CategoryStore`. Search (DEF-2) will reuse the store later.
- Opening the profile from a featured topic carries only the expert's id, name and image. Details doesn't return tags, avatar or rating, so the hero shows what it has, and Expertise is hidden when no tags are known (D-12).

</specifics>

<deferred>
## Deferred Ideas

- The rest of DEF-7 (the Cancellation Policies page and cosmetic animations) stays deferred. Only the All Categories sheet was pulled into Phase 5.
- The pulsing hero glow animation is cosmetic and was dropped (D-14).
- The "See all" links come back with Search (DEF-2), and the reviews "See all" with the full reviews list (DEF-3).

</deferred>

---

*Phase: 05-explore-expert-profile*
*Context gathered: 2026-10-21*
