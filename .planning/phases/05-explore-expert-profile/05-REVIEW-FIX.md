---
phase: 05-explore-expert-profile
fixed_at: 2026-10-22T03:20:00Z
review_path: .planning/phases/05-explore-expert-profile/05-REVIEW.md
iteration: 1
findings_in_scope: 5
fixed: 5
skipped: 0
status: all_fixed
---

# Phase 5: Code Review Fix Report

**Fixed at:** 2026-10-22T03:20:00Z
**Source review:** .planning/phases/05-explore-expert-profile/05-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 5 (Critical + Warning; the 8 Info findings are out of scope)
- Fixed: 5
- Skipped: 0

**Gate (run after the last fix):** `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` BUILD SUCCESSFUL, 1065 tests with 0 failures. `scripts/check-design-rules.sh` reports "Design rules: OK".

## Fixed Issues

### WR-01: CategoryStore lets DataStore IOExceptions escape, which crashes the All Categories sheet

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CategoryStore.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CategoryStoreTest.kt`
**Commit:** ba5a0c4
**Applied fix:** The disk cache is now best-effort. In `freshCache()`, an `IOException` from `dataStore.data.first()` counts as a cache miss. In `fetchAndSave()`, the in-memory copy is set before the write, and an `IOException` from `dataStore.edit {}` is swallowed, so the downloaded list is still returned. A network failure still throws `ApiException`, so the sheet's Retry path works as before. KDoc updated. Tests: a `FailingDataStore` whose reads and writes both throw. One test covers fetch succeeding, the memory cache serving within the TTL, and refresh. A second checks that a network failure still surfaces as `ApiException.Network`.

### WR-02: Double taps push duplicate routes (expert card, featured topic, Book Session)

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/NavigatorTest.kt`
**Commit:** 4e674e5
**Applied fix:** `Navigator.navigate()` now ignores a push when the top of the current stack already equals the route, and leaves `lastTransition` unchanged in that case. This one guard covers all three call sites (HomeClientScreen expert/topic, Book Session). An equal route deeper in the stack (A, B, A) still pushes. `navigateInTab` was left as it was. Tests: a double push leaves one entry and one Back returns to root; an equal route below the top still pushes.

### WR-03: A failed initial home load overwrites sections a concurrent experts load already filled

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModel.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModelTest.kt`
**Commit:** 04e4fb0
**Status:** fixed: requires human verification (state-handling logic)
**Applied fix:** In `withHomeFailure`, `orError()` now keeps any section that is currently `Loaded`, whatever `keepData` is. A non-refresh load resets every section to Loading, so a section that is Loaded when the failure arrives was filled by a concurrent experts request. `refreshFailed` still depends only on `keepData`. KDoc updated. New test `failedInitialHomeKeepsSectionsAConcurrentApplyAlreadyFilled`: the initial home request is gated and then fails after an Apply succeeds, and categories, featured topics and experts stay Loaded. The test was confirmed to fail against the pre-fix code.

### WR-04: One-shot appear animations replay whenever a lazy item scrolls back into view

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/SectionAppear.kt` (new), `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/components/SectionAppearTest.kt` (new)
**Commit:** fbc72bc
**Applied fix:** New shared `components/SectionAppear`. Its `MutableTransitionState` is held in `rememberSaveable` with a saver that restores it as already shown. LazyColumn keeps saveable state for items scrolled out of composition, so an item that scrolls back appears at full height with no expand from zero. A section that is genuinely removed and re-added still animates. Both private duplicates (`AnimatedSection` in Explore, `SectionAppear` in Expert Profile) and their duplicate 200 ms constants were removed. This also resolves the duplicate-helper half of IN-04. Tests (Robolectric): the first composition still expands in; after scrolling the item out of composition and back, it is at full height on the first frames. The second test was confirmed to fail with the old plain `remember`. A quick on-device scroll check on the Expert Profile is recommended (open item, per the Phase 5 manual-testing policy).

### WR-05: Today's Sessions time range ignores the user's 24-hour setting and goes stale on a locale change

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/TodaysSessions.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreUiState.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModel.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreenTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreSectionsTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModelTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/TodaysSessionsFilterTest.kt`
**Commit:** f8a1f0a
**Applied fix:**
- `TodaysSessionUi.timeRange: String` is replaced by `startTime` / `endTime` `Instant`s, and the ViewModel no longer formats or reads `Locale`.
- `formatSessionTimeRange(start, end, zone, timeFormat: java.text.DateFormat)` formats a clone of the given format in the zone, so the caller's format is not changed.
- The card formats the range in composition with `android.text.format.DateFormat.getTimeFormat(context)`, which honors the device 12/24-hour setting.
- The activity has no `configChanges`, so a locale change recreates it and the text is recomposed in the new locale.
- Added `TODAYS_SESSION_TIME_TAG`.

Tests:
- JVM: 12-hour and 24-hour formats, plus zone handling that leaves the format unchanged.
- Robolectric: en-US shows "3:00 PM – 4:00 PM" by default and "15:00 – 16:00" when `Settings.System.TIME_12_24` is "24".

The new format helper uses Android's sanctioned `getTimeFormat` path instead of `getBestDateTimePattern` with `java.time`, because ICU patterns can contain letters such as `B` that `java.time` on API 26 does not support.

## Notes

- Commits were made with `git commit` on explicit file paths instead of `gsd-tools query commit`, so that each message carries the required `Co-Authored-By` trailer. None of the unrelated pre-existing changes (`.idea/*`, `.planning/config.json`) were staged.
- Info findings IN-01 to IN-08 were out of scope (`fix_scope: critical_warning`). WR-04 resolved the duplicate-helper part of IN-04. `MAX_REVIEWS` duplicating `REVIEWS_PREVIEW_LIMIT` is still open.

---

_Fixed: 2026-10-22T03:20:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
