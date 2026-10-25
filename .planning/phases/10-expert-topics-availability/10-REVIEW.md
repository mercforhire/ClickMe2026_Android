---
phase: 10-expert-topics-availability
reviewed: 2026-10-25T00:00:00Z
depth: standard
files_reviewed: 58
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ExpertiseTagPickerParts.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SelectableChip.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ExpertModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ExpertRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertAvailabilityScheduleData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertTopicItem.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/MyExpertTopicsPayload.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/MoshiProvider.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/NullPreservingAdapters.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogs.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertErrors.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScheduleLogic.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityTimezoneLogic.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilityDayCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilityOverlapBanner.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilitySaveAction.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilitySkeleton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/AvailabilityTimeButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/TimeRangePickerSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/subviews/TimezonePickerSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertiseTagsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/ExpertTopicsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicEvents.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicFormatting.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRules.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicPrice.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicDescriptionField.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicDurationStepper.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicIconPicker.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicPriceField.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicTagPicker.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/subviews/TopicCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/subviews/TopicDeleteDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/subviews/TopicsEmptyState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/subviews/TopicsSkeleton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Motion.kt
  - app/src/main/res/values/strings_call.xml
  - app/src/main/res/values/strings_expert_availability.xml
  - app/src/main/res/values/strings_expert_topics.xml
findings:
  critical: 1
  warning: 5
  info: 7
  total: 13
status: issues_found
---

# Phase 10: Code Review Report

**Reviewed:** 2026-10-25
**Depth:** standard
**Files Reviewed:** 58
**Status:** issues_found

## Summary

Reviewed the Phase 10 expert topics, topic editor, expertise editor and availability code, plus the Phase 10 diffs in the pre-existing files (`ClickMeApi.kt`, `CallDialog.kt`, `Motion.kt`, `SignupTagsScreen.kt`, `HomeExpertScreen.kt`, `SelectableChip.kt`).

I cross-checked wire shapes against the backend (`expertTopics.schema.js`, `expertTopics.controller.js`, `availability.schema.js`, `availability.controller.js`). Request and response shapes line up. That covers the explicit-null PATCH adapter, `24:00` and the 15-minute alignment (the Android 30-minute grid is a subset), lower-case tag UUIDs, and a free topic sending a positive `hourly_rate.amount`.

The pure logic is solid: schedule hydrate/encode/decode, overlap detection, price parsing with `BigDecimal`, and the topic list generation counter. I found no injection, secret or path-traversal issues. Server text is rendered as plain `Text`.

The real defects are in state hand-off. One is a silent-overwrite path in the Edit Expertise sheet. Others are Availability edits and refresh racing an in-flight save, silent normalisation of stored topic durations, and a new launch-routing coupling.

## Critical Issues

### CR-01: Edit Expertise sheet can overwrite the profile's tags with an incomplete selection

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModel.kt:96-137` (also `:155-174`, `:177-189`)

**Issue:** The selection is seeded from `GET /expert/profile` in a separate async call (`loadProfileTags`). `PATCH /expert/profile` is a full replace (`saveProfileTags`). Two paths let the user save a selection that is missing the expert's existing tags:

1. **Profile fetch fails.** `loadProfileTags` swallows the `ApiException` and returns, so `selected` stays empty. The sheet shows "Pick at least one expertise tag", which tells the user they have none. The catalog can still load. Picking one tag enables Save, and the PATCH replaces all existing tags (including the primary) with that one tag.
2. **Slow profile fetch.** Any `toggle`/`remove` sets `touched = true`. A late profile response is then discarded (`if (touched) return@launch`), so the existing tags are never merged in. `canSave` only checks `selected.isNotEmpty()`, so Save is available before the seed has landed.

iOS seeds synchronously from the cached `expertProfile`, so it cannot hit either case. The topic editor then also drops topic tags that are no longer on the profile (`applyProfileTagsUpdate`), so the loss cascades.

**Fix:** Track the seed state and gate Save on it, and surface the failure.
```kotlin
sealed interface SeedState { data object Loading : SeedState; data object Failed : SeedState; data object Ready : SeedState }
// ExpertiseEditorUiState: val seed: SeedState = SeedState.Loading
// canSave: selected.isNotEmpty() && !isSaving && seed == SeedState.Ready
// loadProfileTags(): on ApiException -> _uiState.update { it.copy(seed = SeedState.Failed) } and show a Retry row
// success -> if (!touched) selected = ordered; always set seed = Ready
```
For the slow-fetch case, merge instead of discard: when the profile arrives and `touched` is true, prepend the profile tags that are not already selected. Alternatively, block toggling until the seed is Ready.

## Warnings

### WR-01: Availability stays editable and refreshable while Save is in flight, so "Saved" can show a schedule that was never sent

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt:132-146, 188-213`

**Issue:**
- `edit()` has no `saveState == Saving` guard. This differs from `TopicEditorViewModel.edit`, which blocks edits while saving. Toggling a day or changing a slot during the spinner changes `days`. `buildRequest` already captured the old state, so the check mark appears for a schedule that does not match the screen.
- On success, `savedStateHandle.remove(KEY_SCHEDULE)` discards the process-death mirror of those newer edits.
- `refresh()` (pull-to-refresh) is also allowed during Saving. `applyPull` can land the pre-PUT server copy into `days` and remove the mirror. After the PUT succeeds the UI shows the old schedule while the server holds the new one.

**Fix:** Ignore `edit`/`load(Pull)` while saving, and disable the pull gesture during that time.
```kotlin
private fun edit(transform: (AvailabilityUiState) -> AvailabilityUiState) {
    if (_uiState.value.saveState == SaveState.Saving) return
    ...
}
fun refresh() { if (_uiState.value.saveState == SaveState.Saving) return; load(LoadMode.Pull) }
```
Alternatively, re-encode the mirror after success instead of removing it unconditionally.

### WR-02: Opening a stored topic and saving silently rewrites its duration

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRules.kt:78-83` (used at `TopicEditorViewModel.kt:97-99`)

**Issue:** `seedDuration` clamps to 30..480 and rounds to 30-minute steps. The server stores up to 1440 minutes (`durationMinsSchema` allows `<= 1440`). Legacy rows may also be off-grid, because the server docs say "no backfill of existing off-grid rows". A topic stored at 600 minutes is loaded as 480 and sent in the PATCH on any unrelated edit (for example fixing a typo). The expert's session length, which clients are booking against, is changed with no indication. The 480 cap is iOS parity, but the Android PATCH is always a full body, so the change is silent.

**Fix:** Do not clamp stored values. Seed the stepper with the stored value (as `iconOptions` does for non-curated slugs). Keep the 30/480 bounds only for the +/- buttons, with `canIncrease` false at or above 480. Or, if the cap is intentional, show a note when the stored duration was changed.

### WR-03: Typing `/expert/topics` strictly now couples first-launch mode routing to the topic DTO

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/MyExpertTopicsPayload.kt:11-13`, consumed at `managers/UserManager.kt:337-341`

**Issue:** Before Phase 10, `topics: List<Map<String, Any?>>` could not fail to decode. Now `topics: List<ExpertTopicItem>` fails the whole payload if any element violates the typed schema. Examples are a null `id`/`title`, a non-integer `duration_mins`, or `price: null`. `expertHasTopics()` catches `ApiException` (which includes `Decoding`) and returns `false`, so an expert who does have topics is routed to `AppMode.Client` on first launch. The server shape matches today, so this is latent, but the comment says this outcome is accepted. A display-only schema change shouldn't be able to flip app mode.

**Fix:** Keep the launch lookup lenient. Add a separate lightweight payload for `expertHasTopics` (for example `topics: List<JsonObject-like Map>` or `List<TopicIdOnly>`), and use the strict `ExpertTopicItem` list only in `ExpertTopicsRepository`. Or make `ExpertTopicItem` fields nullable and tolerant.

### WR-04: Silent PUT during timezone auto-fill can overwrite server data the client could not parse

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt:109-130, 219-232` with `AvailabilityScheduleLogic.hydrate` (`:87-96`)

**Issue:** `hydrate` drops malformed times and `end <= start` slots. When the server zone is UTC and the device is not, `silentSaveIfNeeded` PUTs the hydrated schedule with no user action. Any slot that `hydrate` dropped is permanently deleted from the server by a screen that was merely opened. The server only writes values that passed its own validation, so this is rare. It is still an unrequested destructive write whose only purpose is to persist a timezone.

**Fix:** Send only the timezone in the silent write. The server's PUT is a partial update (`timezone_id` and `schedule` are both optional). Make `UpdateMyAvailabilityRequest.schedule` nullable and omit it for the silent save. The same change removes the risk of the silent save also racing a concurrent edit.

### WR-05: Hidden selection is saved when the topic editor's profile tags fail to load

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicTagPicker.kt:43-46` with `TopicEditorViewModel.kt:247-259, 294-330`

**Issue:** In Edit mode `selectedTags` is seeded from the topic. If the profile fetch fails, `TopicTagPicker` shows only the "Couldn't load your tags" row (`loadFailed` takes priority), so the expert cannot see which tags are selected. Save is still allowed and PATCHes `expertise_tag_ids` from the hidden selection. This is not data loss, because the seed is preserved, but the user cannot review or remove them, and the tags are not checked against the profile subset until the server rejects them.

**Fix:** In the failed state, still render `selectedTags` as removable chips above the Retry row, or disable Save until `tags` is `Loaded`.

## Info

### IN-01: Saved-topic event and snackbar are dropped when the list is not Loaded

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsViewModel.kt:102-117`

**Issue:** `merge` returns state unchanged when `list` is Loading or Failed, so the "Topic added/updated" snackbar is never shown in that case. The follow-up reload still shows the topic, but the confirmation is lost.

**Fix:** Set the `snackbar` even when the list is not `Loaded`, and only skip the list merge.

### IN-02: Locale-default `lowercase()` next to `Locale.ROOT` usage

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicTagPicker.kt:50, 57`

**Issue:** `selectedIds` is already lower-cased with `Locale.ROOT` in `TopicEditorUiState`. This composable lower-cases it again, and the tag id, with the default locale, on every recomposition. The rest of the phase uses `Locale.ROOT` deliberately.

**Fix:** Use `selectedIds` directly and `tag.id.lowercase(Locale.ROOT)`.

### IN-03: Redundant `finally` and double reset of `deletingId` in delete

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsViewModel.kt:133-155`

**Issue:** Both the success and `catch` branches set `deletingId = null`, and the `finally` does it again.

**Fix:** Keep only the `finally`.

### IN-04: `/2000` is hard-coded in the counter string

**File:** `app/src/main/res/values/strings_expert_topics.xml` (`topic_editor_description_counter`) vs `TopicEditorRules.DESCRIPTION_MAX`

**Issue:** The limit exists in two places and can drift.

**Fix:** Use `%1$d/%2$d` and pass `TopicEditorRules.DESCRIPTION_MAX`.

### IN-05: Title and description caps reuse a booking-notes helper

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRules.kt:5, 94-96`

**Issue:** `capNotes` is imported from `clientmode.makeabooking`, which couples the expert feature to the booking flow under a misleading name.

**Fix:** Move the helper to a shared util (for example `capText`).

### IN-06: A new ViewModel per Edit Expertise opening is retained until the editor is popped

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorScreen.kt:76-85`

**Issue:** `hiltViewModel(key = "expertise_editor_$session")` creates a new `ExpertiseEditorViewModel` on each open, and each stays in the entry's ViewModel store until the editor entry is popped. The cost is small, but state such as `touched`, the saved selection mirror and the catalog request lives longer than the sheet.

**Fix:** Acceptable as is. If it becomes a concern, clear the instance on dismiss, or scope the key to a counter and remove the old one.

### IN-07: Misleading comment on the overlap strings

**File:** `app/src/main/res/values/strings_expert_availability.xml:19`

**Issue:** The comment says "Overlap dialog" but the strings feed `AvailabilityOverlapBanner`, which is an inline banner.

**Fix:** Change the comment to "Overlap banner".

---

_Reviewed: 2026-10-25_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
