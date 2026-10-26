---
phase: 10-expert-topics-availability
reviewed: 2026-10-25T00:00:00Z
depth: standard
files_reviewed: 18
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ExpertRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/NullPreservingAdapters.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScheduleLogic.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRules.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicTagPicker.kt
  - app/src/main/res/values/strings_expert_topics.xml
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertTopicRequestJsonTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScheduleLogicTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorSheetTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRulesTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicEditorSubviewsTest.kt
findings:
  critical: 0
  warning: 3
  info: 5
  total: 8
status: issues_found
---

# Phase 10: Code Review Report (incremental, gap-closure plans 10-17..10-19)

**Reviewed:** 2026-10-25
**Depth:** standard
**Files Reviewed:** 18
**Status:** issues_found

## Summary

Re-review of the gap-closure changes since `6eb390b`. The fixes for CR-01, WR-02, WR-04 and WR-05 are real and correct in the main path. WR-01 is only half fixed: edits and refresh are blocked during a save, but a save can still start while a pull-to-refresh is in flight. No blockers remain. The new code introduces no omitted-versus-null JSON problems: I checked `duration_mins` omission against the backend `patchTopicSchema` and `patchTopic` controller (optional field, `updateRow` is built only from fields that are present), so omission is valid and an explicit null would have been rejected, as the adapter comment says.

## Prior findings status

| ID | Status | Notes |
|----|--------|-------|
| CR-01 (Edit Expertise overwrite) | Fixed | `ExpertiseSeedState` gates `canSave`, `toggle`, `remove` and `save`. The late seed always lands in full because edits are ignored until Ready. A failed seed shows Retry. A restored selection is only ever written after Ready, so treating it as Ready is sound. `SavedStateHandle` is keyed per `hiltViewModel(key=...)`, so a new opening re-seeds. Residual: IN-01. |
| WR-01 (Availability editable/refreshable while saving) | Partially fixed | `edit()` and `refresh()` now return while Saving, and an edit during Saved cancels the hold. The converse is still open: Save is allowed while a pull is in flight (new WR-01 below). |
| WR-02 (topic duration rewritten on save) | Fixed | `seedDuration` no longer clamps or rounds, `UpdateExpertTopicRequest.durationMins` is nullable, and the adapter omits the key when null. The VM omits it when the value equals the seed. Stepper bounds still hold (stored 600: increase is disabled, decrease goes to 480; stored 45 steps to 30/60). Create is unchanged. |
| WR-04 (silent UTC PUT deleting dropped slots) | Fixed for the stated case | `isLossless` shares `parseRange` with `hydrate`, so the two cannot drift. The silent PUT is skipped when any server range was dropped, and `didAutoDetect` is set before the skip, so it is not retried. A different, narrower problem remains: WR-02 below. |
| WR-05 (failed tag load hides selected tags) | Fixed for the Failed state | Selected tags render as removable chips above Retry, and removing one is not sent. The same hiding still happens in the Loading state (IN-02). |

## Warnings

### WR-01: Save can start while a pull-to-refresh is in flight, and the pull then overwrites the schedule that was sent

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt:86-100, 143-157, 220-240`, `AvailabilityUiState.kt:45`

**Issue:** The WR-01 guard only covers one direction (refresh while Saving). The opposite order is open:
1. The user pulls to refresh. `load(Pull)` sets `isRefreshing = true` and launches a GET. Edits are still allowed, which is iOS parity.
2. The user edits, then taps Save. `canSave` ignores `isRefreshing`, and `save()` does not check `loadJob`. The PUT starts with the edited schedule.
3. The GET, issued before the PUT, returns the pre-PUT copy. `applyPull` runs unconditionally. It replaces `days` and `timezoneId` and removes the process-death mirror. This happens during Saving or just after it.

The result is the original WR-01 symptom: the screen shows the old server copy while the server holds the newly saved schedule. The "Saved matches what was sent" invariant in the class KDoc does not hold. No test covers this order. `editsAndRefreshAreIgnoredWhileSaving` only covers pull after save start.

**Fix:** Cancel the in-flight pull when a save starts, and gate `canSave` on it.
```kotlin
fun save() {
    ...
    val request = buildRequest(state)
    loadJob?.cancel()   // drop any in-flight pull (a failed pull must also reset the flag)
    _uiState.update { it.copy(saveState = SaveState.Saving, isRefreshing = false) }
```
Cancelling is safe for `Pull` only. Do not cancel an Initial load, though `save()` already requires Loaded, so none can be in flight. Alternatively, ignore the result in `applyPull` when `saveState != Idle`. Add a test: start a pull behind a gate, edit, save, release the gate, and assert `days` still equals the edited schedule.

### WR-02: The silent timezone PUT is not serialized with user edits or an explicit Save

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt:139-140, 247-260`

**Issue:** `silentSaveIfNeeded` launches an untracked `NonCancellable` PUT. It does not set `saveState`, so the screen stays interactive and `canSave` is true while it runs. If the user edits and taps Save within one network round trip of opening the screen, two PUTs race. The silent request carries the hydrated schedule and the device zone, so if it is processed last it silently reverts the user's saved edit. The explicit `Saved` check mark is already showing by then. The window is narrow, but the failure is silent data loss. The `state.saveState == SaveState.Saving` guard inside `silentSaveIfNeeded` is dead code (see IN-04), so it gives no protection.

**Fix:** Run the silent write through the same path. For example, track it in a `silentJob`, have `save()` `join()` it (or cancel the result handling) before sending, or send a timezone-only body. The backend PUT accepts `timezone_id` alone, so `UpdateMyAvailabilityRequest.schedule` could become nullable. A timezone-only body also makes the `isLossless` skip unnecessary.

### WR-03: A stale profile-tags fetch can overwrite tags saved from the Edit Expertise sheet

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModel.kt:247-275`

**Issue:** `onExpertiseSaved` applies the sheet's saved tags but does not cancel `tagsJob`. The "Add Expertise" header is enabled while tags are Loading. If a load or Retry GET was issued before the sheet's PATCH, its late response goes through `applyProfileTags` and replaces `profileTags` with the pre-save list. `applyProfileTagsUpdate` then drops any selected topic tag that was only just added to the profile. The window is narrow (the GET usually returns long before a PATCH finishes), but the stale write is unconditional.

**Fix:**
```kotlin
fun onExpertiseSaved(tags: List<ExpertiseTagItem>) {
    tagsJob?.cancel()
    applyProfileTags(tags)
}
```

## Info

### IN-01: Edit Expertise selection can still be changed while its save is in flight

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModel.kt:186-196` (rows and chips in `ExpertiseEditorSheet.kt` are not disabled while saving)

**Issue:** `updateSelection` checks `seed` but not `isSaving`. A tap during the spinner changes the visible selection and the process-death mirror, while `save()` sends and returns the snapshot taken before the tap. The sheet then closes, so the user's extra tap is silently lost. This is not an overwrite: what is sent is what the expert last confirmed. It is the same class of problem the Availability fix addressed.

**Fix:** `if (_uiState.value.isSaving) return` in `updateSelection`, and pass `enabled = !state.isSaving` to the rows.

### IN-02: Selected topic tags are hidden while profile tags are Loading, and Save stays enabled

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicTagPicker.kt:90`

**Issue:** The WR-05 fix covers `loadFailed` only. In the `isLoading` state (initial load, or after tapping Retry) `LoadingRow()` hides the selected chips and the topic can be saved. The selection is the seed, so nothing is corrupted, but the user cannot see what would be sent. After Retry the chips also disappear and reappear.

**Fix:** Render the selected-chips row for both `isLoading` and `loadFailed`, or disable Save while tags are Loading.

### IN-03: Edits made during Saving are dropped with no feedback

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt:201-208`, `AvailabilityScreen.kt:183-190, 242-250`

**Issue:** The screen passes no saving flag to the day cards or pickers, so they look interactive. A tap on the day Switch, Add hours or a slot is ignored by `edit()` and the control snaps back. The window is the length of one PUT.

**Fix:** Pass `enabled = state.saveState != SaveState.Saving` to the cards, or briefly accept it as designed.

### IN-04: Dead guard and an unreliable comment in `silentSaveIfNeeded`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt:247-250`

**Issue:** It is called only from `applyInitial`, where `saveState` is always Idle, so `state.saveState == SaveState.Saving` can never be true. The KDoc line "Skipped while saving" is therefore misleading and hides the real race (WR-02).

**Fix:** Remove the check, or replace it with the serialization from WR-02.

### IN-05: Tag chips still use default-locale `lowercase()`, and a test helper is awkwardly formatted

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicTagPicker.kt:118-121` (and `ExpertiseEditorSheetTest.kt` `clickingARowTogglesThatTag`)

**Issue:** The new `loadFailed` branch uses `Locale.ROOT`, but the `else` branch directly below still uses `it.lowercase()` and `tag.id.lowercase()`. Under a Turkish locale `"I".lowercase()` yields a dotless i, so a hex or UUID id could fail to match. This was the previous IN-02 and it was not fixed. `selectedIds` is already lower-cased in `TopicEditorUiState`. In the sheet test, the `onToggle` lambda was reformatted into an odd multi-line assignment.

**Fix:** Use `selectedIds` directly and `tag.id.lowercase(Locale.ROOT)` in both branches, and tidy the lambda.

---

_Reviewed: 2026-10-25_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
