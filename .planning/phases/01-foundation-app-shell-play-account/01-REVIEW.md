---
phase: 01-foundation-app-shell-play-account
reviewed: 2026-10-20T13:35:16Z
depth: standard
files_reviewed: 71
files_reviewed_list:
  - .editorconfig
  - .github/workflows/ci.yml
  - .gitignore
  - app/build.gradle.kts
  - app/src/dev/res/values/strings.xml
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/uptrendinvestments/clickmeapp/AppRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/MainActivity.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/EmptyState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PlaceholderTabContent.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/avatar/Avatar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/avatar/TopBarAvatarButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/backgrounds/GreenGlowBlobLayer.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/buttons/AsyncSaveButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/buttonstyles/PressScale.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/web/CustomTabLauncher.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/config/AppConfig.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/config/AppLinks.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffold.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/NavigationState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/TabDefinition.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/subviews/HomeClientTab.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/DesignSystemGalleryScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/GalleryControlsSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/GalleryMediaSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/GalleryThemeSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/subviews/HomeExpertTab.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Color.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Extras.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Motion.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Shape.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Spacing.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Theme.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Type.kt
  - app/src/main/res/drawable/ic_launcher_foreground.xml
  - app/src/main/res/drawable/ic_launcher_monochrome.xml
  - app/src/main/res/drawable/splash_logo.xml
  - app/src/main/res/mipmap-anydpi/ic_launcher.xml
  - app/src/main/res/mipmap-anydpi/ic_launcher_round.xml
  - app/src/main/res/values-v27/themes.xml
  - app/src/main/res/values/colors.xml
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values/strings_gallery_controls.xml
  - app/src/main/res/values/strings_gallery_media.xml
  - app/src/main/res/values/strings_gallery_theme.xml
  - app/src/main/res/values/themes.xml
  - app/src/main/res/xml/backup_rules.xml
  - app/src/main/res/xml/data_extraction_rules.xml
  - app/src/test/java/com/uptrendinvestments/clickmeapp/AppViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/components/ControlsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/components/avatar/AvatarTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/components/web/CustomTabLauncherTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffoldTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/NavigatorTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/ui/theme/ThemeTest.kt
  - app/src/test/resources/robolectric.properties
  - app/src/testDev/java/com/uptrendinvestments/clickmeapp/config/AppConfigDevTest.kt
  - app/src/testProd/java/com/uptrendinvestments/clickmeapp/config/AppConfigProdTest.kt
  - build.gradle.kts
  - gradle/libs.versions.toml
  - scripts/check-design-rules.sh
findings:
  critical: 0
  warning: 5
  info: 9
  total: 14
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-10-20T13:35:16Z
**Depth:** standard
**Files Reviewed:** 71
**Status:** issues_found

## Summary

I reviewed the Phase 1 scaffolding at standard depth: the build and flavors, the manifest and backup rules, CI and the design-rule script, the theme, the six shared components, the Navigation 3 tab shell, the two home shells, the debug gallery and the tests. I read the tests and string resources more quickly.

I did not flag the decisions locked in 01-CONTEXT.md:
- Navigation 3
- Dark-only theme
- The stub `AppViewModel` that returns `ClientHome`
- The debug gallery gated by `BuildConfig.DEBUG`
- Both flavors using the production URL
- The committed publishable Stripe key and Agora App ID

There are no blockers. The main problems are:
- **Navigation shell:** the predictive-back animation reads stale state, so it often plays the wrong transition. `navigateInTab` and `reselect` also set the wrong transition kind.
- **Base URL:** `BASE_URL` has no trailing slash. Retrofit will reject it in Phase 2, and the iOS style of joining strings drops `/api/v1`. The flavor tests lock in the current value.
- **CI hex-color gate:** it is easy to get around. I wrote a probe file with 7 different ways of writing a color literal, and the script passed all 7.
- **`ValidatedField`:** it silently overrides the caller's keyboard options.

I checked the backup and data-extraction rules, the splash hold, edge-to-edge setup, the Custom Tab scheme allowlist and the secrets handling, and found no problems in them.

## Warnings

### WR-01: Predictive-back transition is chosen from a stale `lastTransition`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffold.kt:122-127` (with `navigation/Navigator.kt:48-57`)

**Issue:** `NavDisplay` calls `predictivePopTransitionSpec` when the back gesture *starts*. `onBack` → `Navigator.goBack()` only runs when the gesture *commits*, and that is the only place `lastTransition` gets updated for a back. So the predictive spec always sees the kind of the *previous* navigation, not the back that is happening. Two concrete wrong results:
- Push `Detail` on Explore, switch to Bookings, then switch back to Explore (`lastTransition = TabSwitch`). A predictive back now pops `Detail` inside the tab but plays `fadeThrough` instead of the slide.
- On Bookings, push a screen and press Back (`lastTransition = Pop`). A predictive back from the Bookings root then *switches tab* to Explore but plays the push/pop slide instead of the fade-through.

`reselect()` makes this worse: it sets `Pop` even when nothing was popped (see WR-05).

**Fix:** Work out the kind of the pending back from the current state, not from `lastTransition`:
```kotlin
predictivePopTransitionSpec = {
    val stack = navigationState.backStacks.getValue(navigationState.topLevelRoute)
    if (stack.size > 1) slide(forward = false) else fadeThrough()
},
```
Optionally share this logic with `Navigator.goBack()` so the committed back and the predictive animation always agree.

### WR-02: `BASE_URL` has no trailing slash, which Retrofit rejects; joining the iOS way drops `/api/v1`

**File:** `app/build.gradle.kts:31,42`; `app/src/main/java/com/uptrendinvestments/clickmeapp/config/AppConfig.kt:12`; `app/src/testDev/.../AppConfigDevTest.kt:10`; `app/src/testProd/.../AppConfigProdTest.kt:10`

**Issue:** `BASE_URL = "https://clickme-backend-production.up.railway.app/api/v1"`. In Phase 2 this goes into `Retrofit.Builder().baseUrl(AppConfig.baseUrl)`, which throws `IllegalArgumentException("baseUrl must end in /")` at startup. iOS joins strings (`baseURL + endpoint.path` in `ClickMeAPI.swift:109`) with paths that start with `/`. If those paths are ported unchanged into Retrofit annotations (`@GET("/users/me")`), Retrofit resolves them against the host root and silently drops `/api/v1`, so every call returns 404. Both flavor tests assert the value without the slash, which locks the problem in.

**Fix:** End the constant with `/` in both flavors (`.../api/v1/`), update the two tests, and record a Phase 2 rule: Retrofit paths must be relative (no leading `/`). Alternatively, normalize the URL in `AppConfig`:
```kotlin
val baseUrl: String get() = BuildConfig.BASE_URL.trimEnd('/') + "/"
```

### WR-03: The design-rule CI gate (FND-02 / D-07) is easy to get around, and the secret scan misses files

**File:** `scripts/check-design-rules.sh:10,24`

**Issue:**
- **Hex-color check:** it only matches `Color(` followed on the same line by `0x`, `red =` or a digit. I ran a probe file through it, and the script printed "Design rules: OK" for all of these:
  - multi-line `Color(\n    0xFF123456,\n)`
  - `Color(color = 0xFF00FF00)`
  - `Color(value = 0xFF00FF00uL)`
  - `Color(alpha = 1f, red = ...)`
  - `"#FF00FF".toColorInt()`
  - `android.graphics.Color.parseColor("#123456")`
  - `Color.rgb(1, 2, 3)`

  Hex literals in XML drawables and layouts are not scanned at all.
- **Stripe secret scan:** it ignores the `SRC_DIR` argument. It scans hard-coded paths relative to the working directory (`app gradle ./*.kts`), so it never looks at `gradle.properties`, `local.properties`, `.github/` or `scripts/`. Those are exactly the places a secret is most likely to be pasted.

**Fix:**
- Broaden the color pattern and scan multi-line, for example `grep -rnPzo --include='*.kt' 'Color\(\s*(0x|[0-9]|(red|alpha|color|value)\s*=)|parseColor\(|toColorInt\(|Color\.(rgb|argb)\('`.
- Add an XML check for `#[0-9A-Fa-f]{6,8}` in `res/**` other than `values/colors.xml`.
- Scan the whole repo for secrets with excludes: `git ls-files -z | xargs -0 grep -nE '(sk|rk)_(live|test)_[A-Za-z0-9]+'`.
- Better long term: a custom lint or ktlint rule.

### WR-04: `ValidatedField` silently overrides the caller's `keyboardOptions`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt:119-128`

**Issue:** For every field, the component forces `capitalization = None` and `autoCorrectEnabled = false`. For secure fields it also forces `keyboardType = Password`. Callers lose these settings even when they pass them explicitly. Later phases will hit this:
- A display-name or bio field passing `KeyboardCapitalization.Words` or `Sentences` gets no capitalization.
- A PIN field passing `KeyboardType.NumberPassword` gets a full text keyboard.

The KDoc does not mention any of this.

**Fix:** Apply the defaults only where the caller left the default, and keep `NumberPassword`:
```kotlin
val effectiveKeyboardOptions =
    if (isSecure) {
        keyboardOptions.copy(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = if (keyboardOptions.keyboardType == KeyboardType.NumberPassword)
                KeyboardType.NumberPassword else KeyboardType.Password,
        )
    } else {
        keyboardOptions
    }
```
Email and username callers should pass `KeyboardOptions(capitalization = None, autoCorrectEnabled = false, keyboardType = Email)` themselves.

### WR-05: `navigateInTab` and `reselect` record the wrong transition kind and accept invalid routes

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt:65-85`

**Issue:**
- **`navigateInTab(tab, route)` when `tab` is already selected:** it sets `lastTransition = TabSwitch`. But the visible change is a push inside the current stack, so the user sees a fade instead of a slide.
- **`navigateInTab` with a tab root as `route`:** nothing stops `route` from being a top-level key, e.g. `navigateInTab(ClientChatsRoot, ExploreRoot)`. That puts one tab's root inside another tab's stack. The entries list then contains duplicate content keys, and `navigate()` treats that key as a tab switch. This is the API that Phase 12 and Phase 13 are meant to build on, so it should validate its input.
- **`reselect(tab)`:** it sets `lastTransition = Pop` even when the stack is already at its root, or when `tab` is not the selected tab. Nothing visible changes, but the value then feeds the next transition (see WR-01).

**Fix:**
```kotlin
fun navigateInTab(tab: NavKey, route: NavKey) {
    require(tab in state.backStacks) { "Unknown tab: $tab" }
    require(route !in state.backStacks) { "Use navigate() to select a tab: $route" }
    state.lastTransition = if (tab == state.topLevelRoute) NavTransition.Push else NavTransition.TabSwitch
    state.backStacks.getValue(tab).add(route)
    state.topLevelRoute = tab
}

fun reselect(tab: NavKey) {
    require(tab in state.backStacks) { "Unknown tab: $tab" }
    val stack = state.backStacks.getValue(tab)
    if (stack.size <= 1) return
    if (tab == state.topLevelRoute) state.lastTransition = NavTransition.Pop
    while (stack.size > 1) stack.removeAt(stack.lastIndex)
}
```
Add a `NavigatorTest` case for `navigateInTab` on the current tab.

## Info

### IN-01: The Custom Tab ACTION_VIEW fallback is effectively dead code, and its test cannot tell the two paths apart

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/web/CustomTabLauncher.kt:58-68`; `app/src/test/.../CustomTabLauncherTest.kt:59-82`

**Issue:** A `CustomTabsIntent` is already an `ACTION_VIEW` intent. If no browser supports Custom Tabs, `launchUrl` still opens the URL in any browser. It only throws `ActivityNotFoundException` when *nothing* handles `ACTION_VIEW`, and in that case the fallback (with the stricter `CATEGORY_BROWSABLE`) fails too. The KDoc says the fallback runs "when no Custom Tabs provider exists", which is not what happens. The test `openCustomTab_browserRegistered_startsViewIntent` only checks `action == ACTION_VIEW`, so it passes on either path.

**Fix:** Either drop the inner fallback and call `onFailure()` on the first `ActivityNotFoundException`, or keep it and fix the KDoc. In the test, assert `started.hasExtra(CustomTabsIntent.EXTRA_SESSION)` (or the toolbar color extra) to show that the Custom Tab path ran.

### IN-02: The INTERNET permission is not declared in the app manifest

**File:** `app/src/main/AndroidManifest.xml:1-30`

**Issue:** `Avatar` loads remote images through Coil, and Phase 2 adds Retrofit. The merged manifest only has `android.permission.INTERNET` because a library manifest contributes it. If that dependency changes, networking fails with no build error. Separately, `okhttp-logging` is `debugImplementation` (`app/build.gradle.kts:99`). If Phase 2 references `HttpLoggingInterceptor` from `src/main`, release builds stop compiling.

**Fix:** Add `<uses-permission android:name="android.permission.INTERNET" />` to the manifest. Keep the logging-interceptor wiring in a `src/debug` source set, or make it `implementation` and gate it with `BuildConfig.DEBUG`.

### IN-03: The whole `Theme.ClickMe` style is duplicated in `values-v27`, and `#131313` is defined twice

**File:** `app/src/main/res/values-v27/themes.xml:4-10`; `app/src/main/res/values/themes.xml:4-9`; `app/src/main/res/values/colors.xml:3`

**Issue:** `values-v27` copies the full style just to add `windowLightNavigationBar`. API 27+ devices (nearly all of them) use the v27 copy, so a later edit to `values/themes.xml` would silently not apply on those devices. The surface color is defined in both `colors.xml` and `ClickMePalette.surface`, with nothing keeping them in sync.

**Fix:** Move the shared items into a `Base.Theme.ClickMe` parent. Have `values-v27` declare only `Theme.ClickMe` with `parent="Base.Theme.ClickMe"` plus the one v27 item. Add a unit test asserting `R.color.surface == ClickMePalette.surface.toArgb()`.

### IN-04: The launcher mark may extend past the adaptive-icon safe zone, and the monochrome layer includes the glow

**File:** `app/src/main/res/drawable/ic_launcher_foreground.xml:4`; `app/src/main/res/drawable/ic_launcher_monochrome.xml:2-4`

**Issue:** With a 17 dp inset, the 768 px mark is drawn at 74 dp on the 108 dp canvas. The visible strokes span about 13–88% of the bitmap, so the arrow tail and the top-left loop sit about 36–39 dp from the center. That is past the 33 dp safe-zone radius and at or past a circular mask's edge. The monochrome layer reuses the full-color PNG. Themed icons use only alpha, so the soft glow halo will show as a blurred outline.

**Fix:** Check on a Pixel circular mask. If clipped, raise the inset to about 21–23 dp. Give the monochrome layer a glow-free, hard-alpha version of the mark.

### IN-05: The two home shells are near-duplicates, and `ClickMeApp`'s `modifier` is only partly applied

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt:37-119`; `.../expertmode/home/HomeExpertScreen.kt:37-119`; `.../HomeClientViewModel.kt` / `HomeExpertViewModel.kt`; `app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt:31-35`

**Issue:** About 80 lines are copied between the shells. Only the tab list, the strings and the switch label differ, so a fix in one shell will be forgotten in the other. `ClickMeApp(modifier)` passes `modifier` only to `EmptySurface`, not to the home screens.

**Fix:** Extract a `HomeShell(tabs, startTab, rootEntries, debugSwitchLabelRes, ...)` composable that both shells call. Add a `modifier` parameter to `HomeClientScreen` / `HomeExpertScreen` and pass it through.

### IN-06: `Modifier.pressScale` uses `Modifier.composed`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/buttonstyles/PressScale.kt:38-53`

**Issue:** `composed {}` is discouraged: it allocates on every recomposition and cannot be skipped. It is used here by every `PressScaleButton`.

**Fix:** Inside `PressScaleButton`, read `collectIsPressedAsState` and `animateFloatAsState` directly and pass a lambda to `Modifier.graphicsLayer { scaleX = scale; ... }`. Or implement it as a `Modifier.Node`.

### IN-07: CI uses mutable action tags, runs twice per PR, and Robolectric runs below the target SDK

**File:** `.github/workflows/ci.yml:3-5,19-29`; `app/src/test/resources/robolectric.properties:1`

**Issue:**
- The actions are pinned to major tags (`@v7`, `@v6`), not commit SHAs.
- `push` on all branches plus `pull_request` runs CI twice for every PR branch. The concurrency group uses `github.ref`, which differs between the two (`refs/heads/x` vs `refs/pull/N/merge`), so the duplicates are not cancelled.
- Robolectric runs at `sdk=35` while `targetSdk` is 37.
- No job builds or lints a release variant, so R8 problems will not appear until Phase 17.

**Fix:**
- Pin the actions to SHAs.
- Limit `push` to `main` (PRs cover the other branches), or key the concurrency group on `github.head_ref || github.ref`.
- Raise the Robolectric SDK when 4.17+ supports it.
- Consider adding `assembleProdRelease` (unsigned) as a smoke test.

### IN-08: Small dead-code and magic-number items

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/GalleryThemeSection.kt:33-37`; `app/src/main/java/com/uptrendinvestments/clickmeapp/components/avatar/Avatar.kt:75`; `app/src/main/java/com/uptrendinvestments/clickmeapp/components/avatar/Avatar.kt:53-68`

**Issue:**
- `TypeSlot.name` is never read; only `caption` is used.
- `Avatar` hard-codes `.crossfade(200)`, bypassing the `Motion` tokens that every other component uses.
- `Avatar` keeps drawing the Person silhouette underneath the loaded image, so it shows through any avatar with transparency.

**Fix:**
- Remove `TypeSlot.name`.
- Add `Motion.AVATAR_CROSSFADE_MS` and use it.
- Draw the fallback only while loading or on error, using `SubcomposeAsyncImage` or `AsyncImage(onState = ...)` with a state flag.

### IN-09: The same base URL and Agora App ID are repeated in each flavor

**File:** `app/build.gradle.kts:31,37,42,48`

**Issue:** `BASE_URL` and `AGORA_APP_ID` are the same in both flavors but are declared twice. Changing one copy and not the other (for example the trailing-slash fix in WR-02) would quietly make the flavors differ.

**Fix:** Declare both in `defaultConfig`. Keep only `STRIPE_PUBLISHABLE_KEY` per flavor.

---

_Reviewed: 2026-10-20T13:35:16Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
