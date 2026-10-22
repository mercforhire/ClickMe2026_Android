---
phase: 5
slug: explore-expert-profile
status: verified
threats_open: 0
asvs_level: 1
created: 2026-10-21
---

# Phase 5 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.
> Code verified at commit 341817d (after the 05-REVIEW-FIX changes WR-01 to WR-05). Paths below are relative to
> `app/src/main/java/com/uptrendinvestments/clickmeapp/` unless they start with `app/src/test/` or `app/src/main/`.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Backend -> app (JSON) | Home feed, categories, expert details, reviews and client bookings are decoded from the network | Public expert/category content, the caller's own bookings (authenticated) |
| App -> backend (URL) | Expert ids go into `experts/{id}/...` paths and category slugs into the `category` query | Server-issued ids and slugs only; never typed text |
| Backend -> UI | Names, titles, tags, bios, review comments, topic text, error messages and avatar URLs from the server are rendered | Untrusted display strings and image URLs |
| Backend -> disk cache | `/categories` list persisted to app-private `clickme_categories` DataStore, read back for up to 7 days | Public reference data, no PII, no tokens |
| Nav back stack (saved state) -> screens | `ExpertProfileKey` / `BookingKey` are serialized into saved state and restored after process death | Display seed only (id, name, title, image URL, rating, tags) |
| UI -> ViewModel | Chip taps, sheet selections and sheet search text | Slugs from server lists; search text stays local |
| Source -> release artifact | R8 shrinking of the new DTOs, serializable keys and the assisted ViewModel | Build-time only |

---

## Threat Register

Mitigation column cites the code (or test) found during this audit. All 30 `mitigate` threats were confirmed by reading
the cited code, not by the SUMMARY claims. The 7 `accept` threats are in the Accepted Risks Log below.

| Threat ID | Category | Component | Disposition | Mitigation / evidence | Status |
|-----------|----------|-----------|-------------|-----------------------|--------|
| T-05-01 | Denial of service | DTO decoding of home/details/reviews | mitigate | Optional fields are nullable or defaulted in `model/responses/{ClientHomeData,PublicProfileDetailsData,ExpertReviewsPayload,ClientBookingsPayload,CategoriesPayload}.kt` (lists default `emptyList()`, scalars `?`, nested objects default-constructed). Pinned by `app/src/test/.../network/ResponseDecodingTest.kt`: `clientHomeWithoutFeaturedTopicsDecodesNullAndSparseExpert` (L396, missing keys), `expertDetailsWithoutIsBookableDecodesNull` (L456), null fields in L359-L393, L465-L496, and unknown keys (`is_favorite`, `avg_response_time`, `rating_distribution`, `private_note_preview`, `reschedule`) in L499-L515. `apiCall` (`network/ApiCall.kt:9-16`) turns any decode failure into `ApiException.Decoding`, which each ViewModel catches. | closed |
| T-05-02 | Tampering | Expert id in `experts/{id}/...` | mitigate | `network/ClickMeApi.kt:146,152` use `@Path("id")` with default (encoding) mode; no `encoded = true` anywhere in `app/src/main/java`. Ids originate only from `RecommendedExpertDto.expertId` / `FeaturedTopicDto.expert.expertId` via `screens/clientmode/explore/ExploreNavigation.kt:8,23`; no text field feeds a path. Path shape asserted in `ClickMeApiTest` (`expertDetailsAndReviewsUseIdPathAndPaging`). | closed |
| T-05-03 | Tampering | `category` query value | mitigate | `network/ClickMeApi.kt:128` `@Query("category")`; the only caller is `screens/clientmode/explore/ExploreRepository.kt:22` with sorted, comma-joined slugs from the selection set. Slugs enter that set only from `ExploreCategoriesSection.kt:70` (`category.slug`, server trending list) and `AllCategoriesSheet.kt:133` (`category.id`, `/categories` list). Test `getClientHomeFilteredSendsOneCommaJoinedCategoryAndNoPaging`. | closed |
| T-05-06 | Denial of service | `Money.format` with unknown/malformed currency | mitigate | `util/Money.kt:27-32` catches `IllegalArgumentException` from `Currency.getInstance`; `:33` maps negative `defaultFractionDigits` to 2; `:35` falls back to `"{CODE} {amount}"`. `util/MoneyTest.kt`: `unknownCode_fallsBackToCodeAndAmount` ("ZZZ 50.00"), `noCurrencyCode_xxx_treatsNegativeDigitsAsTwo`. `PriceDisplay.of` is the only caller (`util/PriceDisplay.kt:33`). | closed |
| T-05-07 | Spoofing | Misleading price display (free shown as paid, JPY /100) | mitigate | `util/PriceDisplay.kt:32` `isFree == true -> Free` is the first branch; digits come from `Currency.defaultFractionDigits` (`util/Money.kt:33-34`). `MoneyTest`: `usd_showsTwoDecimals`, `jpy_hasNoDecimals`, `kwd_hasThreeDecimals`, `eur_inGermanLocale_...`. `PriceTextTest.of_explicitFreeWins` (free wins even with amount+currency present). Both screens route prices only through `PriceDisplay.of` (`ExploreViewModel.kt:261`, `ExpertProfileViewModel.kt:147`). | closed |
| T-05-09 | Tampering | Server text in `ExpertTagChip` / `SectionError` | mitigate | `components/ExpertTagChip.kt:28-35` plain `Text(text = label, maxLines = 1, overflow = Ellipsis)`; `components/SectionError.kt:57-62` plain `Text(text = message)`. Sweep of `components`, `screens/clientmode`, `managers`, `constants` for `AnnotatedString`, `LinkAnnotation`, `ClickableText`, `HtmlCompat`, `Linkify`, `autoLink`, `WebView`, `Markdown` found nothing. | closed |
| T-05-11 | Tampering | Cached categories JSON (stale-cache poisoning) | mitigate | `managers/CategoryStore.kt:48-51,75` decodes through the typed `List<CategoryItem>` Moshi adapter inside `runCatching`; a failed or null decode returns `null` (miss) and `fetch()` then calls `fetchAndSave()` (`:57`). `di/CategoryCacheModule.kt:37` `ReplaceFileCorruptionHandler { emptyPreferences() }`. `CategoryStoreTest.corruptCachedJsonIsAMiss`. | closed |
| T-05-13 | Tampering | Device clock rollback extending the TTL | mitigate | `managers/CategoryStore.kt:62` `(clock.millis() - fetchedAt) in 0 until TTL_MS`, so a negative age is stale; applied to both memory and disk copies (`:65,74`). Wall clock injected: `di/ClockModule.kt:27` `Clock.systemUTC()`, constructor param `CategoryStore.kt:46`. `CategoryStoreTest.negativeCacheAgeIsStale`. | closed |
| T-05-14 | Denial of service | Concurrent fetches on cold cache | mitigate | `managers/CategoryStore.kt:52,57,60` `Mutex.withLock` around both `fetch()` and `refresh()`. `CategoryStoreTest.concurrentFetchesOnEmptyCacheShareOneNetworkCall` asserts one network call for two callers. | closed |
| T-05-15 | Tampering | Category filter slugs | mitigate | Same flow as T-05-03: slugs come only from server lists (`ExploreCategoriesSection.kt:70`, `AllCategoriesSheet.kt:133`); `CategoriesSheetViewModel.kt:119-124` `toggle(slug)` only adds ids passed from rows. Sheet query is not an input to the draft or the request (`:114-117`, `:36-45` local filter only). Retrofit encodes the query (T-05-03). | closed |
| T-05-16 | Information disclosure | Logging of bookings or expert data | mitigate | Grep for `Log.`, `Timber`, `println`, `printStackTrace`, `HttpLoggingInterceptor` across `screens/clientmode/{explore,expertprofile,mybookings,makeabooking,home}`, `managers/CategoryStore.kt`, `util`, the Phase 5 components and `network/` returns nothing. `network/ApiException.kt:18,27` Http message is `"HTTP $status $rawCode"` only (T-02-08). | closed |
| T-05-17 | Denial of service | Rapid chip taps | mitigate | `screens/clientmode/explore/ExploreViewModel.kt:107-108,136` cancel `homeJob` / `expertsJob` before launching; `:144` additionally drops a response whose `selection != requested`. `ExploreViewModelTest.staleResponseNeverOverwritesANewerFilter` (L307) holds the first response while a second chip is tapped. | closed |
| T-05-19 | Elevation of privilege / Information disclosure | Moderated expert opened by id | mitigate | `screens/clientmode/expertprofile/ExpertProfileViewModel.kt:89-96` maps `ApiException.Http` 404 to `DetailsState.Unavailable`, with no details merged. `ExpertProfileScreen.kt:106-107` renders only `UnavailableBody` for that state. `ExpertProfileViewModelTest.details404IsUnavailableAndReviewsStillLoad` (L266); `ExpertProfileScreenTest.unavailableReplacesTheBodyAndGoBackPops` (L327). | closed |
| T-05-20 | Tampering | `ExpertProfileKey` / `BookingKey` in saved state | mitigate | `screens/clientmode/expertprofile/ExpertProfileKey.kt:14-22` fields are expertId, name, title, imageUrl, rating, reviewCount, tags; `screens/clientmode/makeabooking/BookingKey.kt:12-17` fields are expertId, expertName, expertTitle, expertImageUrl. No token, credential or secret field in either. `BookingKey` KDoc states the server re-checks bookability at confirm. | closed |
| T-05-21 | Denial of service | One failing request blanking the screen | mitigate | `ExpertProfileViewModel.kt:81-82` launches `loadDetails` and `loadReviews` as two independent jobs; each has its own `catch (e: ApiException)` (`:89`, `:115`). Tests: `details404IsUnavailableAndReviewsStillLoad`, `reviewsFailureKeepsSeedRatingAndLeavesDetailsAlone` (L242), `details500AndNetworkFailureAreErrors` (L277). | closed |
| T-05-22 | Spoofing | Reviewer names | mitigate | `expertprofile/subviews/ExpertProfileReviewsCard.kt:56-57` `Text(text = review.author ?: stringResource(R.string.expert_profile_anonymous))` (plain `Text`); `ExpertProfileViewModel.kt:112` passes the nullable name through. `ExpertProfileScreenTest.reviewsCardShowsThreeRowsAnonymousFallbackAndNoSeeAll`. | closed |
| T-05-23 | Tampering / Spoofing | Server strings in Explore cards | mitigate | Plain `Text` with caps: `explore/subviews/ExpertCard.kt:99-104` (name, `maxLines = 2`) and `:137-142` (title, `maxLines = 2`); tags via `ExpertTagChip` (`:151`); `ExploreFeaturedTopicsSection.kt:124-129` (topic title, 2 lines) and `:138-143` (expert name, 1 line); `ExploreTodaysSessionsSection.kt:88-100` (name 2, topic 1); `ExploreCategoryCard.kt:104-109` (2). No rich-text or link APIs (T-05-09 sweep). | closed |
| T-05-24 | Tampering | Avatar URLs | mitigate | `components/avatar/Avatar.kt:82-93` is the only `AsyncImage`/`ImageRequest` use in `app/src/main/java` (grep). It always draws the silhouette first (`:60-74`), so a failed or blocked load shows the fallback; blank URL skips the load. Every Phase 5 image goes through `Avatar` (`ExpertCard.kt:96`, `ExploreFeaturedTopicsSection.kt:136`, `ExploreTodaysSessionsSection.kt:85`, `ExpertProfileHero.kt:75`, `MakeABookingStubScreen.kt:51`). `AvatarTest.unresolvableUrl_keepsFallbackUnderImage`. See Notes (Coil loader). | closed |
| T-05-25 | Elevation of privilege | Join Call before Phase 9 | mitigate | `explore/subviews/ExploreTodaysSessionsSection.kt:123-125` `PressScaleButton(onClick = {}, enabled = false)`. No Agora, `RtcEngine`, `joinChannel` or call code under `screens/clientmode` (grep). `ExploreSectionsTest.todaysSessions_showsCardWithFallbackTopicAndDisabledJoin` (L103-109) asserts `assertIsNotEnabled()`. | closed |
| T-05-26 | Tampering / Injection | Search field text | mitigate | `explore/categoriesmodal/CategoriesSheetViewModel.kt:43` `it.name.contains(needle, ignoreCase = true)` is the only use of `query` besides display and SavedStateHandle; `CategoryStore` calls take no argument. `CategoriesSheetViewModelTest.queryFiltersLocallyCaseInsensitivelyInServerOrder` asserts the server call count stays at 1 while searching (L137-158). | closed |
| T-05-27 | Tampering | Cached category names | mitigate | `components/PickerSheet.kt:202-203` `Text(text = label)` inside `PickerRow`, fed by `AllCategoriesSheet.kt:131` `label = category.name`. Cache decoded through the typed adapter (T-05-11). | closed |
| T-05-29 | Tampering / Spoofing | Bio, review comments, topic text | mitigate | `expertprofile/subviews/ExpertProfileAboutCard.kt:61-65` (bio) and `ExpertProfileReviewsCard.kt:66-70` (comment) are plain `Text`; topic rows via `ExpertProfileTopicsCard`. Sweep for annotated/linkified text across `screens/clientmode` returned nothing (T-05-09). | closed |
| T-05-30 | Information disclosure | Unavailable expert preview data | mitigate | `expertprofile/ExpertProfileScreen.kt:106-108` `if (state.details is DetailsState.Unavailable) UnavailableBody(...) else LazyColumn { hero, details, reviews }`, so the hero is inside the else branch. `ExpertProfileScreenTest.unavailableReplacesTheBodyAndGoBackPops` (L332) asserts the seeded name "Ada Lovelace" count is 0. | closed |
| T-05-31 | Elevation of privilege | Book Session for unbookable experts | mitigate | `expertprofile/subviews/ExpertProfileActionButtons.kt:47-66`: enabled button only when `isBookable`, else `PressScaleButton(onClick = {}, enabled = false)`. `ExpertProfileScreen.kt:199-204` shows a skeleton pill while loading (`:223-224`) and `ErrorState` with no Book button on error. Tests: `notBookableShowsDisabledPillAndNoBookSession`, `loadingShowsHeroAndSkeletonsWithoutBookSession` (L316-323), `detailsErrorKeepsHeroAndRetries` (L344). | closed |
| T-05-32 | Tampering | Keys restored from saved state | mitigate | Keys hold display seed only (T-05-20). `ExpertProfileViewModel.kt:64-66,74-83` runs `load()` in `init`, so a restored key always re-fetches details and reviews and a 404 yields `Unavailable`. Entry wiring `screens/clientmode/home/HomeClientScreen.kt:81-91`. | closed |
| T-05-33 | Tampering | R8 shrinking of keys / assisted factory | mitigate | `assembleProdRelease` is in the 05-10 and 05-11 verify (SUMMARY: BUILD SUCCESSFUL). Independent artifact check: `app/build/outputs/mapping/prodRelease/configuration.txt:1057-1058` keeps `ExpertProfileViewModel`; `usage.txt` lists zero `clickmeapp` entries (nothing from the app package was removed); `configuration.txt:867-895` carries the kotlinx-serialization consumer rules for the `@Serializable` keys. No app-level keep file exists (`app/proguard-rules.pro` is absent) and none was required. | closed |
| T-05-34 | Repudiation / Spoofing | Avatar entry to Mode Switch preserved | mitigate | `screens/clientmode/home/HomeClientScreen.kt:73-78` `exploreRoot` passes `onAvatarClick = { navigator.navigate(ModeSwitchKey) }`; `explore/ExploreScreen.kt:144` renders `TopBarAvatarButton(avatarUrl, onClick = onAvatarClick)`. `HomeShellsTest.clientAvatarOpensModeSwitcherWithoutBottomBarAndClientActive` (L105) and `exploreRootReceivesShellAvatarUrl` (L324) exist; the suite was green in the 05-REVIEW-FIX gate (1065 tests, 0 failures; tests were not re-run in this audit). See Notes (Hilt lambda not directly tested). | closed |
| T-05-35 | Tampering | R8 stripping Moshi adapters / key serializers | mitigate | All 20 Phase 5 DTO classes carry `@JsonClass(generateAdapter = true)` (per-file class vs annotation counts: 2/2, 3/3, 6/6, 4/4, 5/5); `MoshiProvider.kt` is reflection-free. `prodRelease/configuration.txt` holds an expanded keep rule for each of the 20 `*JsonAdapter` classes (moshi 1.15.2 consumer rules, `:952-975`). Both keys are `@Serializable` (`ExpertProfileKey.kt:13`, `BookingKey.kt:11`). | closed |
| T-05-36 | Information disclosure | Secrets or PII in new code or caches | mitigate | `bash scripts/check-design-rules.sh` re-run during this audit: "Design rules: OK" (Stripe secret key pattern check). Secret grep across the Phase 5 sources found only a KDoc comment ("no tokens or secrets"). Cache holds `CategoryItem` list and a timestamp only (`CategoryStore.kt:105-106`). Keys contain no tokens (T-05-20). | closed |
| T-05-37 | Elevation of privilege | Deferred controls shipping early | mitigate | Re-ran the sweeps: no favorite/heart, "See all", report/block/more-options/message controls in `screens/clientmode/{explore,expertprofile,makeabooking}` code or in any `res/values/*.xml`; no call/Agora code (T-05-25). Exactly one `// PHASE-9` marker (`ExploreTodaysSessionsSection.kt:122`). `ExpertProfileScreenTest.deferredControlsNeverAppear` (L358). | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

Accepted threats (dispositions from the plans; rationale and corroboration in the Accepted Risks Log):

| Threat ID | Category | Component | Disposition | Mitigation / evidence | Status |
|-----------|----------|-----------|-------------|-----------------------|--------|
| T-05-04 | Information disclosure | Bearer sent on `/categories` | accept | AR-05-01 | closed |
| T-05-05 | Elevation of privilege | Client trusting `is_bookable` | accept | AR-05-02 | closed |
| T-05-08 | Tampering | Unknown category slug | accept | AR-05-03 | closed |
| T-05-10 | Denial of service | Infinite skeleton animation | accept | AR-05-04 | closed |
| T-05-12 | Information disclosure | `clickme_categories` file | accept | AR-05-05 | closed |
| T-05-18 | Information disclosure | Today's Sessions of another user | accept | AR-05-06 | closed |
| T-05-28 | Denial of service | Retry spamming | accept | AR-05-07 | closed |

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-05-01 | T-05-04 | `/categories` is public on the backend and ignores the bearer. `AuthHeaderInterceptor` attaches the token to every path outside the five credential paths (`network/ApiRequestUrls.kt:42-52`), so the token goes to the same first-party host it already goes to on every other call. No new recipient. | Phase 5 plan 05-01 (disposition set at plan time); logged by gsd-security-auditor | 2026-10-21 |
| AR-05-02 | T-05-05 | `is_bookable` only drives the Book Session pill (display-only, D-04). The server enforces it at confirm: `backend-reference/src/controllers/booking.controller.js:331` and `:598` throw 422 `EXPERT_NOT_BOOKABLE`. A tampered client cannot book an unbookable expert. DTO KDoc documents this (`PublicProfileDetailsData.kt:9-10`). Phase 6 must surface that 422. | Phase 5 plan 05-01; logged by gsd-security-auditor | 2026-10-21 |
| AR-05-03 | T-05-08 | A category slug only selects an icon. `constants/CategoryIconMap.kt:83` returns `table[slug.lowercase(Locale.ROOT)] ?: fallback`, so an unknown slug gets the generic tile and nothing else happens. | Phase 5 plan 05-02; logged by gsd-security-auditor | 2026-10-21 |
| AR-05-04 | T-05-10 | `SkeletonBlock` uses `rememberInfiniteTransition` (`components/SkeletonBlock.kt:50`), which stops when the block leaves composition. `Phase5ComponentsTest.skeletonBlock_rendersAndInfinitePulseDoesNotHangTestClock` (L86) shows the test clock still idles. Cost is a few animated alpha values on screen only while loading. | Phase 5 plan 05-03; logged by gsd-security-auditor | 2026-10-21 |
| AR-05-05 | T-05-12 | The `clickme_categories` DataStore holds public reference data only, in app-private storage. Backup and transfer are blocked: `AndroidManifest.xml:9` `allowBackup="false"`, and `res/xml/data_extraction_rules.xml` excludes all domains for cloud backup and device transfer. Kept across logout on purpose (D-08); `CategoryStore` is not referenced by `SessionDataCleaner`. | Phase 5 plan 05-04; logged by gsd-security-auditor | 2026-10-21 |
| AR-05-06 | T-05-18 | `/client/bookings` is scoped to the caller on the server: `backend-reference/src/controllers/clientBooking.controller.js:215` and `:330` use `.eq('client_id', clientId)`. The client only filters its own rows for today (`screens/clientmode/mybookings/TodaysSessions.kt:20-27`). | Phase 5 plan 05-05; logged by gsd-security-auditor | 2026-10-21 |
| AR-05-07 | T-05-28 | Each Retry is one `CategoryStore.refresh()` behind the `Mutex` (`managers/CategoryStore.kt:60`), so taps queue serially and make at most one request each. The endpoint is public and cheap. | Phase 5 plan 05-08; logged by gsd-security-auditor | 2026-10-21 |

*Accepted risks do not resurface in future audit runs.*

---

## Unregistered Flags

None. Every `## Threat Flags` section in 05-01 to 05-11 SUMMARY.md says "None" or maps to registered IDs (T-05-01, 04 to 07, 11, 13 to 17, 19 to 25, 26, 27, 29 to 37). This audit did not find new attack surface outside the register. The review fixes WR-01 to WR-05 (best-effort cache I/O, navigator double-tap guard, section-state handling, appear-animation state, time formatting) add no new trust boundary.

---

## Notes (non-blocking observations from verification)

1. **No `app/proguard-rules.pro`.** The release build type uses the AGP 9 `optimization { enable = true; packageScope = ... }` block with library consumer rules only. R8 evidence is the existing `prodRelease` output (generated 2026-10-21 22:17, before the 23:09 review-fix commits). The fixes added no DTO, serializable key or ViewModel, so the conclusion holds, but a fresh `assembleProdRelease` is the final proof if wanted.
2. **Coil uses its default image loader.** `app/build.gradle.kts:125-126` adds `coil-compose` and `coil-network-okhttp`; there is no custom `ImageLoader`. Avatar fetches therefore do not pass through the app's OkHttp client or `AuthHeaderInterceptor`, so the bearer is never sent to avatar hosts (better than the plan text, which said "app network stack"). Server-supplied URLs are fetched as given; cleartext HTTP is blocked only by the platform default (no `usesCleartextTraffic` or network security config in the manifest). Consider adding an https-only check if avatar URLs from untrusted sources ever appear.
3. **T-05-34 production lambda.** The exact `exploreRoot` lambda in `HomeClientScreen` is Hilt-backed and not exercised by Robolectric; the shell default and the `ExploreScreen` avatar button are tested. The wiring was verified by reading the source.
4. **T-05-01 edge.** `PublicProfileDto.name` is `String = ""`, so a JSON `null` for it (as opposed to a missing key) would throw in Moshi. The result is `ApiException.Decoding`, which the profile shows as the Error state with Retry, not a crash. Required fields (ids, titles, booking times/status) match the iOS contract.
5. **T-05-11 corruption handler.** `ReplaceFileCorruptionHandler` is configured in `CategoryCacheModule`; the unit test covers corrupt JSON content, not a corrupt DataStore file.
6. **Deferred human checks** (on-device flows, 200% font scale, release smoke) are tracked in 05-HUMAN-UAT.md and STATE.md; they are not security mitigations.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-21 | 37 | 37 | 0 | gsd-security-auditor (Claude) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-21
