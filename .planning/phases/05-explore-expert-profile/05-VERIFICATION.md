---
phase: 05-explore-expert-profile
verified: 2026-10-22T00:00:00Z
status: human_needed
score: 3/3 must-haves verified
overrides_applied: 0
human_verification:
  - test: "SC-1 Explore on device (Samsung SM-A075F, devDebug against production, logged in as a client)"
    expected: "Explore shows the hero, Trending Categories (6 chips with icons), Featured Topics and Recommended Experts from /client/home. A free featured topic reads 'Free' in green. With a confirmed booking today, Today's Sessions shows it with a disabled Join Call. A chip tap filters experts only and a second tap clears it. View all opens the sheet with search, multi-select and Apply filters, and the chip row shows the selection. A filter with no matches shows 'No experts in this category yet' plus Clear filter. Pull to refresh works. Airplane mode on first load shows per-section Retry."
    why_human: "Live backend data, visual layout and gesture behavior cannot be checked by grep or unit tests. The user deferred manual testing."
  - test: "SC-2 Category cache on device"
    expected: "Open View all online (GET /categories), force-stop, relaunch in airplane mode, open View all: the list shows from cache with no error. Optional: set the device date 8 days ahead and confirm a refetch."
    why_human: "Needs a real process restart and the device network and clock. The TTL logic itself is covered by CategoryStoreTest."
  - test: "SC-3 Expert Profile on device"
    expected: "Tapping an expert card shows the hero (static glow, ring, no online dot), stats, Book Session, About, Expertise, Topics (Free in green, prices with full decimals) and up to 3 reviews. Empty sections are absent. Opening from a Featured Topic fills the hero in. Book Session opens the 'Book a Session' stub without the bottom bar, and Back returns. A suspended or deleted expert id shows 'This profile isn't available'."
    why_human: "Visual fidelity, navigation feel and live data. A suspended or deleted expert id needs the backend agent."
  - test: "Visual and accessibility pass"
    expected: "200% font scale on Explore, the sheet and the profile (cards grow, nothing clips). Sheet search with the IME. Pull-to-refresh feel. Skeleton-to-content transitions on a slow network. Release build smoke once the Phase 2 keystore exists."
    why_human: "Visual and device-only checks."
---

# Phase 5: Explore & Expert Profile Verification Report

**Phase Goal:** A client can browse experts from the Explore home and open an expert's full profile to start a booking.
**Verified:** 2026-10-22
**Status:** human_needed (no code gaps; only the user-deferred device checks remain)
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | The client's Explore tab shows categories, featured topics, today's sessions and expert cards from `getClientHome` | VERIFIED | `HomeClientScreen.exploreRoot` renders `ExploreScreen`, which is a Hilt-injected `ExploreViewModel`. `ExploreContent` is a `LazyColumn` with hero, Today's Sessions, categories, featured topics and experts items. `ExploreRepository.home()` calls `api.getClientHome(category)` (`GET client/home`). `ExploreViewModel.withHome` maps `trendingCategories`, `featuredTopics` and `recommendedExperts` into section state. Today's Sessions comes from `ClientBookingsRepository.list(upcoming).todaysSessions(clock, zone)`. A failure is silent and hides the strip. Per-section Loading/Error/Loaded states are modelled. |
| 2 | Categories load from the local cache without a network fetch for 7 days, and refresh after the TTL expires | VERIFIED | `CategoryStore.fetch()` serves memory, then DataStore, while `(clock.millis() - fetchedAt) in 0 until TTL_MS` (`TTL_MS = 604_800_000`), and only then calls the network. `refresh()` always refetches. It uses its own `clickme_categories` DataStore (logout-proof). `CategoriesSheetViewModel` calls `categoryStore.fetch()` on open and `refresh()` on Retry. `CategoryStoreTest` covers within-TTL no-network, a new instance over the same DataStore serving from disk, refetch after TTL, negative age, corrupt JSON, single-flight, and survival across a session clear. Targeted test run is green. |
| 3 | Tapping an expert card opens Expert Profile with hero, stats, about, topics and reviews preview. Its Book button takes the client into the booking flow | VERIFIED | `ExploreScreen` maps `ExpertCardUi` and `FeaturedTopicUi` to `ExpertProfileKey` (`ExploreNavigation.kt`) and calls `navigator.navigate`. `HomeClientScreen` registers `entry<ExpertProfileKey>` with the assisted `ExpertProfileViewModel`. The ViewModel loads `getExpertDetails` and `getExpertReviews(page 1, limit 3)` in independent jobs. A 404 gives the Unavailable state. `ExpertProfileContent` renders the hero, `ExpertProfileStatsBar`, `ExpertProfileActionButtons` (Book Session), About, Expertise, Topics and the reviews card. Book Session calls `onBook(viewModel.bookingKey())`, which navigates to `BookingKey`. `BookingKey` is registered with `BottomBar.hidden()` and renders `MakeABookingStubScreen`. |

**Score:** 3/3 truths verified

Note on SC-3: the Book button reaches the real `BookingKey` route and a stub body. This is by design (D-04). Phase 6 replaces the stub body, and Phase 6 BOOK-01 owns the actual picker UI.

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `network/ClickMeApi.kt` and `ApiRequestUrls.kt` (getClientHome, getClientBookings, getCategories, getExpertDetails, getExpertReviews) | VERIFIED | The endpoints exist with the correct paths and parameters. Used by the repositories. |
| `managers/CategoryStore.kt`, `di/CategoryCacheModule.kt`, `Qualifiers.kt` | VERIFIED | Substantive and wired into `CategoriesSheetViewModel`. |
| `screens/clientmode/explore/*` (ViewModel, Repository, Screen, UiState, Navigation, subviews, categoriesmodal) | VERIFIED | 233-line screen and 270-line ViewModel. Wired from `HomeClientScreen`. |
| `screens/clientmode/expertprofile/*` (Key, Repository, ViewModel, Screen, subviews) | VERIFIED | Wired from the `HomeClientScreen` Nav3 entry. |
| `screens/clientmode/makeabooking/BookingKey.kt`, `MakeABookingStubScreen.kt` | VERIFIED (stub by design) | Registered in `HomeClientShell`. Phase 6 replaces the body. |
| `util/Money.kt`, `PriceDisplay.kt`, `components/*` (PriceText, RatingStars, ExpertTagChip, Skeleton*, SectionHeader/Error), `CategoryIconMap` | VERIFIED | Used by the Explore and profile subviews. |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `HomeClientScreen` | `ExploreScreen` | `exploreRoot` slot | WIRED |
| `ExploreScreen` expert card or featured topic | `ExpertProfileKey` route | `toProfileKey` then `navigator.navigate` | WIRED |
| `HomeClientScreen` entry | `ExpertProfileViewModel` | `hiltViewModel` with assisted `Factory.create(key)` | WIRED |
| `ExpertProfileScreen` Book Session | `BookingKey` and `MakeABookingStubScreen` | `onBook(viewModel.bookingKey())` | WIRED |
| `ExploreViewModel` | `GET client/home` and `client/bookings` | `ExploreRepository` and `ClientBookingsRepository` | WIRED |
| `CategoriesSheetViewModel` | `CategoryStore` | `fetch()` and `refresh()` | WIRED |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|----------|------|--------|-----------|--------|
| `ExploreContent` | `state.categories`, `featuredTopics`, `experts` | `repository.home()` calling the Retrofit `getClientHome` | Yes, from the API DTOs | FLOWING |
| `ExploreContent` | `todaysSessions` | `ClientBookingsRepository.list("upcoming")` with the today filter | Yes | FLOWING |
| `ExpertProfileContent` | details, reviews | `getExpertDetails` and `getExpertReviews` | Yes | FLOWING |
| `AllCategoriesSheet` | catalog | `CategoryStore` (cache or `getCategories`) | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Phase 5 targeted unit tests (CategoryStore, ExploreViewModel, ExpertProfile*, ExploreScreen, HomeClient*) | `./gradlew --offline :app:testDevDebugUnitTest --tests ...` | BUILD SUCCESSFUL | PASS |
| Full suite, ktlint, lint, assembleDevDebug/ProdRelease | Reported green by the orchestrator (1056 tests); not re-run here | n/a | Not independently re-run |

### Probe Execution

No probes declared by this phase. SKIPPED.

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|--------------|-------------|--------|----------|
| DISC-01 | 05-01, 02, 03, 05, 07, 10, 11 | Explore home from `getClientHome`: categories, featured topics, today's sessions, expert cards | SATISFIED | Truth 1 |
| DISC-02 | 05-01, 03, 04, 08, 10, 11 | `CategoryStore` with 7-day TTL | SATISFIED | Truth 2 |
| DISC-03 | 05-01, 02, 03, 06, 09, 10, 11 | Expert Profile with hero, stats, about, topics, reviews preview and Book button | SATISFIED | Truth 3 |

All three IDs appear in the PLAN frontmatter and in REQUIREMENTS.md (marked Complete, mapped to Phase 5). No orphaned requirements.

### Anti-Patterns Found

No TBD/FIXME/XXX/TODO/HACK markers in files changed by the phase. The code review (05-REVIEW.md) found 0 critical and 5 advisory warnings. None block the phase goal:

| ID | Issue | Severity |
|----|-------|----------|
| WR-01 | `CategoryStore` lets DataStore `IOException` escape (read or write). `CategoriesSheetViewModel.load()` catches only `ApiException`, so a real disk failure could crash the sheet. | Warning (rare I/O failure; should be fixed in hardening, Phase 15) |
| WR-02 | Double taps push duplicate routes (expert card, featured topic, Book Session) | Warning |
| WR-03 | A failed initial home load can overwrite sections that a concurrent experts load already filled | Warning |
| WR-04 | One-shot appear animations replay when lazy items scroll back into view | Warning |
| WR-05 | Today's Sessions time range ignores the 24-hour setting and goes stale on a locale change | Warning |

Info items IN-01 to IN-08 are non-blocking (for example, unknown ratings become zero on Explore cards, and Retry re-fetches reviews).

### Human Verification Required

Four device-check items are deferred by the user and logged in STATE.md. See the frontmatter above (SC-1, SC-2, SC-3, and the visual and accessibility pass). These are not gaps.

### Gaps Summary

There are no code gaps. All three roadmap success criteria are backed by substantive, wired code with real data flow, and the targeted tests pass. The remaining work is the user-deferred on-device verification and the advisory review warnings (WR-01 to WR-05), which are best scheduled for Phase 15 hardening.

---

_Verified: 2026-10-22_
_Verifier: Claude (gsd-verifier)_
