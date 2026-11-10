---
phase: 17-play-store-release
reviewed: 2026-11-10T23:04:13Z
depth: standard
diff_base: 25942db1
files_reviewed: 16
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/main/java/com/uptrendinvestments/clickmeapp/config/AppLinks.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/bookingnote/BookingNoteViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallAvatar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/search/SearchExpertUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/search/SearchExpertViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/feedback/FeedbackViewModel.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/RootScreensTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/bookingnote/BookingNoteScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/bookingnote/BookingNoteViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreenControlsReachableTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/search/SearchExpertViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/feedback/FeedbackViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpScreenTest.kt
  - scripts/play-store-screenshot.sh
findings:
  critical: 0
  warning: 2
  info: 5
  total: 7
status: issues_found
---

# Phase 17: Code Review Report

**Reviewed:** 2026-11-10T23:04:13Z
**Depth:** standard
**Files Reviewed:** 16
**Status:** issues_found

## Summary

I reviewed `git diff 25942db1..HEAD` for the 16 files in scope, plus the code they call: `capText`, the Nav3 entry decorators in `NavigationState.kt`, `FeedbackScreen`, `SearchExpertScreen`, `SearchExpertCategoryChips`, the backend `feedback.schema.js`, and the precedent ViewModels `DeleteAccountViewModel` and `ReportAndBlockViewModel`. I did not run gradle, as asked. I checked the trap behaviour of the screenshot script under the bash on this machine (`/bin/bash` 3.2.57): the `EXIT` trap runs on TERM, HUP and INT.

The POL-02 restore logic is sound in all three ViewModels:
- BookingNote keeps `lastPersisted` as the server note, so a restored draft still raises the discard prompt.
- A draft typed while a save is in flight is kept.
- Feedback never writes the signed-in email prefill to the handle, keeps an edited email that the user cleared, and falls back when a restored type is no longer offered.
- Search runs the restored query at once, with no debounce.

The BUG-22 glow change removes the 360 dp layout reservation as intended. The support email is changed everywhere it is used: `HelpScreen`, `SuspendedAccountDialog` and `supportEmailIntent` all read `AppLinks.SUPPORT_EMAIL`, and nothing hard-codes the old address. The version bump is correct.

There are no blockers. There are two warnings:
- Two new SavedStateHandle mirrors store unbounded user text. This breaks the project's own cap-before-persist pattern and can crash the app when it goes to the background.
- The screenshot script's `restore` resets the phone's display to factory values instead of putting back what was there before. That is not the "always restore" the script promises.

## Narrative Findings (AI reviewer)

## Warnings

### WR-01: Feedback details and search query are mirrored to SavedStateHandle with no length cap

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/feedback/FeedbackViewModel.kt:136`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/search/SearchExpertViewModel.kt:76`

**Issue:** `onDetailsChange` writes `text` to `KEY_FEEDBACK_DETAILS` exactly as typed, and `onQueryChange` writes `text` to `KEY_SEARCH_QUERY`. Neither field has a maximum length:
- `FeedbackDetailsField` passes `onValueChange` straight through.
- The backend schema is `details: z.string().min(10)`, with no `.max`.

The saved-state Bundle is sent to system_server in `onStop`. With targetSdk 24 or higher, a parcel over the binder limit (about 1 MB in total, shared with all other saved state) throws `TransactionTooLargeException`, which `ActivityThread` rethrows as a crash. A large paste into Send Feedback (a log, for example) followed by switching apps would crash the process.

Every earlier SavedStateHandle mirror in this codebase caps the text before persisting it:
- `DeleteAccountViewModel` clamps to `FEEDBACK_MAX` (1000) on edit and on restore.
- `ReportAndBlockViewModel` caps to `REPORT_DETAILS_MAX`.
- `BookingNoteViewModel` in this same phase caps to `NOTE_MAX_LENGTH`.

These two new mirrors are the exceptions. The real risk is low because it needs a very large paste, but the result is a crash in production code.

**Fix:** Cap before writing to the handle and again on restore, the same way as DeleteAccount and ReportAndBlock. If the full text has to keep working in memory, cap only what is persisted:
```kotlin
// FeedbackValidation.kt
const val FEEDBACK_DETAILS_MAX = 5_000

// FeedbackViewModel
details = capText(savedStateHandle.get<String>(KEY_FEEDBACK_DETAILS).orEmpty(), FEEDBACK_DETAILS_MAX),
...
fun onDetailsChange(text: String) {
    val capped = capText(text, FEEDBACK_DETAILS_MAX)
    savedStateHandle[KEY_FEEDBACK_DETAILS] = capped
    _uiState.update { it.copy(details = capped, showDetailsError = it.showDetailsError && feedbackDetailsTooShort(capped)) }
}

// SearchExpertViewModel: a search query has no reason to exceed a few hundred characters
private const val SEARCH_QUERY_MAX = 200
fun onQueryChange(text: String) {
    val capped = capText(text, SEARCH_QUERY_MAX)
    savedStateHandle[KEY_SEARCH_QUERY] = capped
    _uiState.update { it.copy(query = capped) }
    runSearch(SearchMode.Fresh, debounce = true)
}
```
Do the same for the email field (`KEY_FEEDBACK_EMAIL`, line 143), for example capped at 254 characters.

### WR-02: `restore` resets the display to factory values instead of restoring the previous state

**File:** `scripts/play-store-screenshot.sh:59-79`

**Issue:** `cmd_prep` reads the current `wm size`, `wm density` and `sysui_demo_allowed`, but only echoes them (line 60). `cmd_restore` then runs `wm size reset`, `wm density reset` and `settings put global sysui_demo_allowed 0` unconditionally (lines 76-78).

On Android, and on One UI ("Screen zoom"), the user's display-size setting is itself a forced density, which is exactly what `wm density` overrides. If the phone's owner has a non-default display zoom, `restore` silently wipes it. The phase's own tests show this phone has been run that way: `MeetingCallScreenControlsReachableTest` describes the BUG-34 device as "720x1600 px at 340 dpi with the largest display zoom", against a physical density of 300 (17-05-SUMMARY). The same applies to a pre-existing size override and to `sysui_demo_allowed=1`.

This did no harm in the 17-05 run, because the phone was at physical values. But the script's header calls a leftover override "a denial of service for its owner", and it is parameterised (`MODEL`, `ADB`) for reuse. Silently changing the owner's display zoom is the failure the script says it prevents.

**Fix:** Record the prior state in `prep` and put that state back in `restore`. Fall back to `reset` only when there was no override before.
```bash
STATE_FILE="${ASSETS_DIR}/.display-before"
cmd_prep() {
    mkdir -p "$ASSETS_DIR"
    if [ ! -f "$STATE_FILE" ]; then   # never overwrite a saved baseline from an earlier prep
        {
            echo "size=$(adb_t shell wm size | sed -n 's/^Override size: //p' | tr -d '\r')"
            echo "density=$(adb_t shell wm density | sed -n 's/^Override density: //p' | tr -d '\r')"
            echo "demo=$(adb_t shell settings get global sysui_demo_allowed | tr -d '\r')"
        } >"$STATE_FILE"
    fi
    ...
}
cmd_restore() {
    local size="" density="" demo="0"
    [ -f "$STATE_FILE" ] && . "$STATE_FILE"
    demo -e command exit || echo "warn: demo exit failed" >&2
    adb_t shell settings put global sysui_demo_allowed "${demo:-0}" || true
    if [ -n "$size" ]; then adb_t shell wm size "$size"; else adb_t shell wm size reset; fi || echo "warn: wm size failed" >&2
    if [ -n "$density" ]; then adb_t shell wm density "$density"; else adb_t shell wm density reset; fi || echo "warn: wm density failed" >&2
    rm -f "$STATE_FILE"
    ...
}
```
(`settings get` prints `null` when the key is unset. Map that to `0`.)

## Info

### IN-01: A restored category that no longer exists filters the search with no chip selected

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/search/SearchExpertViewModel.kt:50`

**Issue:** `selectedCategorySlug` is restored and sent as the `category` filter without checking it against the loaded chips. If the category was removed between process death and restore, `SearchExpertCategoryChips` highlights nothing (`selected = selectedSlug == ALL_CATEGORIES_SLUG` / `== category.slug`, lines 64 and 72). The results are filtered by an invisible category, often to an empty list. `FeedbackViewModel` handles the same situation for its type (lines 116-118); Search does not.

**Fix:** In `loadCategories`, after the chips land, if they are non-empty and the selected slug is neither `ALL_CATEGORIES_SLUG` nor one of them, fall back to All and re-run the search with `onCategorySelected(ALL_CATEGORIES_SLUG)`.

### IN-02: The swipe tests cannot fail now that the controls fit at rest

**File:** `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreenControlsReachableTest.kt:178-198, 230-246`

**Issue:** `swipeUntilControlsAboveEndCall` only asserts that the swipe moved the content when `needsScroll` is true. `controlsFitAboveEndCallAtRestAtTwoHundredPercentInTheWaitingState` proves that, with the glow out of layout, the controls already fit in exactly the setup the two swipe tests use (339x753 dp, 2.0 font, Waiting). So `needsScroll` is always false: the "did the swipe move the content" assertion never runs, and the final `assertControlsAboveEndCall` duplicates the at-rest test. The KDoc admits the device's no-scroll cause cannot be reproduced on the JVM. As written, these tests cannot detect a broken scroll container.

**Fix:** Use a state or window that is still taller than the space above End Call (for example, the banner and chip at 2.0 font in a shorter window). Assert `needsScroll` is true as a precondition, so the swipe assertion always runs.

### IN-03: The "keep the draft typed during a save" branch has no test

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/bookingnote/BookingNoteViewModel.kt:120`

**Issue:** `if (normalizeNote(_uiState.value.noteText) == body) savedStateHandle.remove(...)` is the only guard against losing text typed while a save is in flight. `savedDraftIsClearedAfterASuccessfulSave` covers only the equal case. If someone removed the condition, every test would still pass.

**Fix:** Add a test: `onTextChange("New")`, `save()`, `onTextChange("Newer")` before `runCurrent()`, then assert that `handle.get<String>("note") == "Newer"` after the save completes.

### IN-04: The glow now paints over earlier siblings and is clipped during a variant cross-fade

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallAvatar.kt:117-132`

**Issue:** The glow (radius `glowSize / 2`, 180 dp at normal height) is now drawn by the hero's own node, outside its roughly 184 dp box. It therefore overlays any sibling drawn earlier within about 90 dp above the hero, such as `EndingSoonBanner` when the weighted spacer collapses on a tall-content screen.

While `AnimatedContent` cross-fades `connectionType` (`VARIANT_FADE_MS`), its default `SizeTransform(clip = true)` applies `clipToBounds()` to the animating content. The glow then shows a hard top edge for 200 ms and pops back when the fade ends. Both effects are cosmetic (alpha 0.10 to 0.28, radial fade).

**Fix:** Accept it as a cosmetic trade-off for BUG-22, or pass `transitionSpec = { (fadeIn(...) togetherWith fadeOut(...)).using(SizeTransform(clip = false)) }` to keep the glow from being clipped.

### IN-05: Smaller robustness gaps in the screenshot script

**File:** `scripts/play-store-screenshot.sh:40-52, 58-70, 129-130`

**Issue:**
- `transport()` runs `exit 3` inside `$(...)` in `adb_t`. That only ends the subshell, so adb still runs with `-t ""`, and the documented exit code 3 never reaches the caller.
- Standalone `prep` has no trap. If `wm size` succeeds and a later step fails (for example, `wm density` while the wireless link drops), the script exits on `set -e` with the override still active and no hint to run `restore`.
- If `screencap` fails, `capture` leaves an empty `$name.png` behind.
- The defaults hard-code `/Users/LeonChen/...` paths.

**Fix:**
- Resolve the transport once at the top of each command (`T=$(transport) || exit 3`) and use `"$ADB" -t "$T"`.
- In `cmd_prep`, add `trap 'echo "prep failed: run restore now" >&2' ERR`, or call `cmd_restore` on failure.
- Add `trap 'rm -f "$png"' ERR` in `cmd_capture`.
- Default `ADB` to `"${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"` and `ASSETS_DIR` under `$HOME`.

---

_Reviewed: 2026-11-10T23:04:13Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
