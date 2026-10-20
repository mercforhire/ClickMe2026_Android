# Phase 1: Foundation, App Shell & Play Account - Research

**Researched:** 2026-10-19
**Domain:** Android project scaffolding (AGP 9 / Kotlin / Hilt / Compose M3), Navigation 3 tab shell, Android 12 splash, CI, Play Console org account
**Confidence:** HIGH for the stack and nav pattern (every recommendation below was compiled or run in a scratch copy of this project); MEDIUM for the Play Console steps (Google's help pages are partial)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Use **Navigation 3** (androidx.navigation3), not Navigation Compose 2.
  - The app owns each tab's back stack as a plain list. That gives saved per-tab history (FND-12) and makes push deep links into a tab easy later (Phase 13).
  - The root `AppRoute` switch (splash → login | signupSetup | clientHome | expertHome) is app state, as on iOS. It is not a nav destination.
- **D-02:** **Single `app` Gradle module.**
  - Packages mirror the iOS folder tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/` (DEC-file-organization).
  - Suggested top-level packages: `ui/theme`, `ui/components`, `navigation`, `screens/...`, `config`. The planner can refine these.
  - Don't split into modules during the MVP.
- **D-03:** Two product flavors, **dev** and **prod**. Both point at the production backend `https://clickme-backend-production.up.railway.app/api/v1`, matching iOS's Environments.plist.
- **D-04:** **The Stripe publishable key and the Agora App ID are committed per flavor** as `BuildConfig` fields in `app/build.gradle.kts`.
  - They aren't secrets; iOS commits them in `Environments.plist`.
  - Copy the values from `ios-reference/ClickMe2026/Environments.plist`:
    - dev uses the Development entry (`pk_test_…`).
    - prod uses the Production entry (`pk_live_…`).
    - The Agora App ID is the same in both.
  - Real secrets are still never committed: signing keystores, `keystore.properties`, `google-services.json`. `.gitignore` already covers these.
- **D-05:** **The backend's Stripe account is currently in test mode** (pre-launch). Do day-to-day development and testing on the dev flavor (`pk_test`, Stripe test cards).
  - The prod flavor's `pk_live` only works once the backend switches to live mode at launch. Track this as a release item, not a Phase 1 bug.
  - Flag for the user: the iOS `Environments.plist` has `Current = Production` (`pk_live`) while the backend is in test mode. Worth confirming on the iOS side.
- **D-06:** **dev and prod share one applicationId** (`com.uptrendinvestments.clickmeapp`, no `.dev` suffix). Only one can be installed at a time, and one Firebase Android app is enough. The dev flavor can still use a distinct launcher label if that's convenient (Claude's discretion).
- **D-07:** **GitHub Actions** runs on **every push to any branch and on pull requests**. Each run does:
  - Android lint
  - Unit tests
  - Debug builds of **both** flavors
  - ktlint check
  - The UI-SPEC rule that blocks hex color literals outside `ui/theme/Color.kt`
- **D-08:** **ktlint** is the code style checker. Provide a local auto-format Gradle task; CI fails on violations. No detekt for now.
- **D-09:** No emulator/instrumented UI tests in CI during Phase 1. Revisit in the QA phase (Phase 16).
- **D-10:** The app's display name is **"ClickMe"**, the same as the iOS `CFBundleDisplayName`.
- **D-11:** **Launcher icon derived from the iOS assets:**
  - The adaptive icon uses the logo from `ios-reference/ClickMe2026/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png` as the foreground on a solid background. The UI-SPEC/theme decides the background color.
  - The themed (monochrome) icon comes from `AppIcon-1024-Tinted.png`.
  - The splash mark comes from `SplashLogo.imageset` (per UI-SPEC: mark only, on `#131313`).
- **D-12:** **The corporation already has a D-U-N-S number** (REL-01 is effectively satisfied once it's confirmed).
- **D-13:** **The user does the Play Console steps personally.** The plan must include an exact, ordered checklist for:
  - Creating the org account with the D-U-N-S number
  - Paying the one-time fee
  - Submitting org verification
  - Recording the verification wait in STATE.md

  These are `checkpoint:human-action` tasks, not code tasks.

### Claude's Discretion
- Exact package layout inside the single module, as long as it mirrors iOS.
- Version catalog entries and library versions. Use current stable releases compatible with AGP 9.4 / Kotlin 2.2 / Gradle 9.6, which are already in the template.
- Whether the dev flavor gets a distinct launcher label or icon badge.
- Exact GitHub Actions caching and JDK setup.
- How the hex-literal check is implemented (Gradle task, grep step, or lint rule).

### Deferred Ideas (OUT OF SCOPE)
- Emulator/instrumented UI tests in CI → Phase 16 (Cross-Platform QA).
- detekt static analysis → revisit after the MVP if needed.
- Splitting into Gradle modules → after the MVP, only if build times hurt.
- Switching the prod flavor and backend to Stripe live mode → release work (Phase 17).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FND-01 | Builds with Hilt, Retrofit + Moshi, OkHttp, coroutines, Coil, Navigation (Compose). dev/prod flavors on the production URL. Stripe key + Agora App ID from build config. | Verified dependency set and versions (Standard Stack); verified flavor + `buildConfigField` DSL on AGP 9.4.1; **Kotlin must be bumped to 2.4.21** (Pitfall 1); Navigation 3 replaces "Navigation Compose" per D-01 (Open Question 1). |
| FND-02 | Lint and baseline CI run on every push. | ktlint-gradle 14.2.0 verified on AGP 9; `.editorconfig` needed for Compose naming; lint error found in the UI-SPEC theme XML (Pitfall 3); CI workflow + design-rules script (Code Examples). |
| FND-09 | Dark M3 color scheme + bundled rounded font; merge the per-screen palettes. | Compose BOM 2026.09.00 → M3 1.4.0; Nunito variable font option (Pitfall 5); every theme/field API in the UI-SPEC compiled against this stack. |
| FND-10 | Six shared components. | Compile-verified: `OutlinedTextFieldDefaults.Container`, `Modifier.shadow(ambientColor, spotColor)`, `ContentType.Password` semantics, Coil 3 `AsyncImage` + `crossfade`, `CustomTabsIntent` dark scheme (Code Examples). |
| FND-11 | Android 12 SplashScreen API, no fixed delay, `AppRoute` routing. | core-splashscreen 1.2.0; theme attrs compile-verified; keep-on-screen hook design; splash/icon asset findings (Pitfall 4). |
| FND-12 | Client and expert 5-tab scaffolds with per-tab saved back stacks. | Official Nav3 `multiplestacks` recipe, compiled against nav3 1.2.0; JVM unit test of Navigator passed; re-tap-to-root, back rule, transitions verified to compile. |
| REL-01 | D-U-N-S confirmed or requested. | Play Console human-action checklist (Play Account section). |
| REL-02 | Play org account created and submitted for verification. | Same checklist; fee and flow cited from Google help pages. |
</phase_requirements>

## Summary

Phase 1 is mostly standard Compose scaffolding. The risky part is the toolchain. The template pins Kotlin 2.2.10, but the current stable library set (Hilt 2.60.1, OkHttp 5.5.0, Retrofit 3.0.0, coroutines 1.11.0) pulls `kotlin-stdlib` 2.4.10. With Kotlin 2.2.10 the build fails: `Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin. The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read versions up to 2.3.0`. I reproduced that failure, then fixed it by moving the `kotlin` version in the catalog to **2.4.21**. With that single change, AGP 9.4.1 + Gradle 9.6.0 + KSP 2.3.12 + Hilt 2.60.1 + Navigation 3 1.2.0 + Compose BOM 2026.09.00 + Coil 3.6.3 build both flavors (debug and an R8 release), pass lint, and run JVM unit tests. All of these results come from a scratch copy in `/tmp`, not from the repo.

Navigation 3 has an official "multiple back stacks" recipe (`android/nav3-recipes`, `multiplestacks`) that already implements what FND-12 and the UI-SPEC require: one back stack per top-level tab, state kept while the tab is hidden, Back at a non-home tab root returns to the home tab, and Back at the home root exits. Copy its `NavigationState` and `Navigator` (Apache 2.0) and add re-tap-to-root. This **conflicts with the UI-SPEC**, which describes Navigation Compose 2 (`NavHost`, nested graphs, `popUpTo { saveState }`). CONTEXT D-01 is a locked decision and wins; the UI-SPEC behavior (per-tab stacks, re-tap-to-root, Back rule) is preserved, only the mechanism changes.

Four more findings change the plan. (1) The UI-SPEC's `Theme.ClickMe` XML uses `android:windowLightNavigationBar`, which is API 27+. With minSdk 26 that is a lint **error** and would fail FND-02. (2) `AppIcon-1024-Tinted.png` is a solid white 1024x1024 image (every pixel is 255,255,255), so D-11's monochrome icon source is unusable. Derive the monochrome layer from the transparent `SplashLogo` PNG instead. (3) Nunito has no static TTFs in the upstream repo, only a variable font, which Compose supports on API 26+, which is our minSdk. (4) ktlint's default `ktlint_official` style flags every `@Composable` function name unless `.editorconfig` says otherwise.

**Primary recommendation:** Bump Kotlin to 2.4.21 in the first plan task and keep every other version as listed in Standard Stack. Build the shell on the Nav3 `multiplestacks` recipe, and start the Play Console human-action checklist on day one in parallel with the code plans.

## Architectural Responsibility Map

This is a single-tier native app, so the "tiers" are in-app layers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Splash and first-frame background | Android OS / Activity theme (XML) | `MainActivity` hold condition | The SplashScreen API is system-owned. Compose only supplies the keep-on-screen predicate. |
| `AppRoute` root switch | App state (`AppViewModel`/Hilt) | `ClickMeApp` composable | Matches iOS: route is app state, not a nav destination (D-01). |
| Per-tab back stacks | Navigation state holder (`NavigationState`/`Navigator`) | `NavDisplay` | The app owns the lists (D-01); `NavDisplay` only renders. |
| Theme tokens (color, type, shape, spacing, motion) | `ui/theme` | — | One source of truth; components never hold hex or font sizes. |
| Shared components | `components/` (stateless composables) | `ui/theme` | Read only from `MaterialTheme`. Keep them Hilt-free so JVM tests can render them. |
| Build config (base URL, Stripe key, Agora ID) | Gradle `BuildConfig` | `config/AppConfig` wrapper | Per-flavor constants, committed per D-04. |
| CI gates (lint, ktlint, tests, hex rule) | GitHub Actions + Gradle | `scripts/` shell | Same commands run locally. |
| Play Console org account | Human (user) | STATE.md log | No code involved (D-13). |

## Standard Stack

### Core

All versions below were read from Google Maven / Maven Central metadata on 2026-10-19 (latest stable, no alpha/beta/rc), and the whole set was compiled together. `[VERIFIED: Google Maven + Maven Central metadata; built in scratch project]`

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| AGP `com.android.application` | 9.4.1 (already in template) | Build | Gradle 9.6.0 and compileSdk 37 are the AGP 9.4 requirements `[CITED: developer.android.com/build/releases/agp-9-4-0-release-notes]` |
| Kotlin (via `org.jetbrains.kotlin.plugin.compose` / `.serialization`) | **2.4.21** (template has 2.2.10, must change) | Language | Needed to read stdlib 2.4.x metadata (Pitfall 1). AGP 9 uses built-in Kotlin, so there is **no** `kotlin-android` plugin; the version comes from the compose/serialization plugin versions. |
| KSP `com.google.devtools.ksp` | 2.3.12 | Hilt annotation processing | KSP 2.3.x is decoupled from the Kotlin version; 2.3.10+ fixes R-class resolution under AGP 9 built-in Kotlin `[CITED: github.com/google/ksp/releases]` |
| Hilt (`hilt-android`, `hilt-android-compiler`, Gradle plugin `com.google.dagger.hilt.android`) | 2.60.1 | DI | CLAUDE.md-locked. 2.59.1+ requires AGP 9 `[CITED: github.com/google/dagger/releases]` |
| `androidx.hilt:hilt-lifecycle-viewmodel-compose` | 1.4.0 | `hiltViewModel()` (new package `androidx.hilt.lifecycle.viewmodel.compose`) | The Nav3 Hilt recipe uses this. Do not use `hilt-navigation-compose` (it drags in `navigation-compose:2.9.0`). |
| Navigation 3 `navigation3-runtime` + `navigation3-ui` | 1.2.0 | Tab shell | Locked by D-01. 1.2.0 is the latest stable. |
| `androidx.lifecycle:lifecycle-viewmodel-navigation3` | 2.11.0 | Per-entry ViewModel scoping decorator | Required by the multiple-stacks recipe |
| `org.jetbrains.kotlin.plugin.serialization` + `kotlinx-serialization-core` | plugin = Kotlin version (2.4.21); core 1.12.0 | `@Serializable` `NavKey`s (process-death-safe back stacks) | Nav3 `rememberNavBackStack` needs serializable keys |
| Compose BOM | **2026.09.00** (template: 2026.02.01) | Compose + M3 versions | Resolves `material3` 1.4.0, `material-icons-extended` 1.7.8, foundation 1.12.1 |
| `androidx.core:core-splashscreen` | 1.2.0 | Android 12 splash + compat to API 26 | UI-SPEC FND-11 |
| `androidx.browser:browser` | 1.10.0 | Custom Tabs | UI-SPEC component 6 |
| `io.coil-kt.coil3:coil-compose` + `coil-network-okhttp` | 3.6.3 | Avatar images | Coil 3 uses a new group id (`io.coil-kt.coil3`) and package (`coil3.*`). It needs the OkHttp network artifact to load URLs. |
| `com.squareup.okhttp3:okhttp` + `logging-interceptor` | 5.5.0 | HTTP; logging interceptor in **debug only** | PRD planning note |
| `com.squareup.retrofit2:retrofit` + `converter-moshi` | 3.0.0 | REST | CLAUDE.md-locked |
| `com.squareup.moshi:moshi` | 1.15.2 | JSON | CLAUDE.md-locked. Add `moshi-kotlin-codegen` (via `ksp`) in Phase 2, when the first model exists. |
| `kotlinx-coroutines-android` | 1.11.0 | Coroutines | CLAUDE.md-locked |
| `androidx.activity:activity-compose` | 1.13.0 | `enableEdgeToEdge`, `setContent` | |
| `androidx.lifecycle:lifecycle-runtime-compose` / `lifecycle-viewmodel-compose` | 2.11.0 | `collectAsStateWithLifecycle`, `viewModel()` | |
| `androidx.core:core-ktx` | 1.19.1 | Extensions | |
| `androidx.compose.material:material-icons-extended` | via BOM (1.7.8) | Icons (UI-SPEC) | The artifact is frozen at 1.7.8; fine for Phase 1. Revisit only if APK size matters (R8 shrinks it in release). |

### Supporting (build and test)

| Library / plugin | Version | Purpose | When to Use |
|------------------|---------|---------|-------------|
| `org.jlleitschuh.gradle.ktlint` | 14.2.0 | ktlint check + `ktlintFormat` (D-08) | Verified working with AGP 9.4.1 / Gradle 9.6 `[VERIFIED: Gradle plugin portal; ran ktlintCheck/ktlintFormat]` |
| JUnit | 4.13.2 | JVM unit tests | Already in the template |
| Robolectric | 4.17 | JVM-run Compose UI tests (no emulator, satisfies D-09) | Component and shell render tests. Pin `@Config(sdk = [35])`; this ran green. |
| `androidx.compose.ui:ui-test-junit4` (+ `ui-test-manifest` as `debugImplementation`) | via BOM | Compose test rule | Use `androidx.compose.ui.test.junit4.v2.createComposeRule` (the old one is deprecated) |
| `kotlinx-coroutines-test` | 1.11.0 | ViewModel tests | `AppViewModel` resolver test |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Kotlin 2.4.21 | Kotlin 2.3.x | Likely also works (the compiler reads metadata one minor ahead), but untested. Do not spend time on it; 2.4.21 is verified end to end. |
| `hilt-lifecycle-viewmodel-compose` | `hilt-navigation-compose` 1.4.0 | Works, but pulls in Navigation Compose 2.9.0 transitively, which the project decided against. |
| Nunito variable font | Static Regular/SemiBold TTFs | UI-SPEC says static, but upstream only ships a variable file (Pitfall 5). Static instances can be cut with `fonttools varLib.instancer`. |
| ktlint-gradle plugin | ktlint CLI in the CI workflow | Plugin gives the local `ktlintFormat` task D-08 asks for. Keep the plugin. |

**Installation (version catalog, `gradle/libs.versions.toml`):** see Code Examples, "Version catalog".

**Version verification** (commands used, 2026-10-19):
```bash
curl -s https://dl.google.com/dl/android/maven2/androidx/navigation3/navigation3-ui/maven-metadata.xml   # 1.2.0 latest stable (1.3.0-alpha02 is the newest alpha)
curl -s https://repo1.maven.org/maven2/com/google/dagger/hilt-android/maven-metadata.xml                 # 2.60.1
curl -s https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml          # 2026.09.00
```

## Package Legitimacy Audit

`slopcheck` was installed (`pip install slopcheck`) but it only scans npm/PyPI-style dependency files and has no Maven support, so it could not be run against these coordinates. Per the protocol I cross-checked a different way: every coordinate below was (a) listed in Google Maven or Maven Central metadata under the publisher's own group id, (b) used in the official `android/nav3-recipes` / library docs, and (c) actually resolved and built in the scratch project. These are first-party or long-established publishers, so I rate them Approved. The only third-party Gradle plugin is ktlint-gradle.

| Package | Registry | Publisher / group | Source Repo | slopcheck | Disposition |
|---------|----------|-------------------|-------------|-----------|-------------|
| androidx.* (navigation3, lifecycle, core, browser, activity, hilt, compose BOM) | Google Maven | Google | android.googlesource.com / github.com/androidx | n/a (Maven) | Approved |
| com.google.dagger:hilt-* (2.60.1) | Maven Central | Google | github.com/google/dagger | n/a | Approved |
| com.google.devtools.ksp (2.3.12) | Maven Central / Gradle portal | Google | github.com/google/ksp | n/a | Approved |
| io.coil-kt.coil3:* (3.6.3) | Maven Central | Coil | github.com/coil-kt/coil | n/a | Approved |
| com.squareup.okhttp3 / retrofit2 / moshi | Maven Central | Square | github.com/square/* | n/a | Approved |
| org.jetbrains.kotlin(x):* | Maven Central | JetBrains | github.com/JetBrains/kotlin | n/a | Approved |
| org.jlleitschuh.gradle.ktlint (14.2.0) | Gradle plugin portal | Jeremy Leitschuh | github.com/JLLeitschuh/ktlint-gradle | n/a | Approved (widely used; ran successfully) |
| org.robolectric:robolectric (4.17) | Maven Central | Robolectric | github.com/robolectric/robolectric | n/a | Approved |

**Packages removed due to slopcheck [SLOP] verdict:** none (tool not applicable to Maven)
**Packages flagged as suspicious [SUS]:** none. Optional: the planner may add a one-line human check on the ktlint-gradle plugin id since it is the only non-Google/JetBrains/Square publisher.

## Architecture Patterns

### System Architecture Diagram

```
 Launcher tap
      |
      v
 Android 12+ system splash  (Theme.ClickMe.Splash: #131313 bg + splash_logo, no custom animation)
      |   MainActivity.onCreate: installSplashScreen() BEFORE super.onCreate
      |   setKeepOnScreenCondition { !appViewModel.destinationResolved }   <-- Phase 2 plugs auto-login here
      v
 postSplashScreenTheme = Theme.ClickMe  (windowBackground #131313, no white flash)
      |
      v
 setContent { ClickMeTheme { ClickMeApp(appViewModel) } }
      |
      v
 AppRoute (StateFlow in AppViewModel)  ---- plain `when`, NOT a nav destination
   |-- Login        (empty placeholder, unreachable in Phase 1)
   |-- SignupSetup  (empty placeholder, unreachable in Phase 1)
   |-- ClientHome ---> HomeClientScreen ----+
   `-- ExpertHome ---> HomeExpertScreen ----+   (Phase 1 stub resolver returns ClientHome)
                                            v
                         HomeTabScaffold(tabs: List<TabDefinition>)
                           Scaffold { bottomBar = NavigationBar (5 items), content = NavDisplay }
                                            |
              NavigationState  (one NavBackStack per tab + `topLevelRoute`)  <---- Navigator
              ^                                                                     ^
              | toDecoratedEntries(entryProvider)                  navigate(tab) / navigate(key) / goBack() / reselect(tab)
              v                                                                     |
        NavDisplay(entries, onBack = navigator::goBack, transitionSpec/popTransitionSpec) <-- NavigationBar clicks, system Back
              |
              v
        entry<ExploreRoot> { PlaceholderTabContent }  ... 10 tab roots
        entry<DesignSystemGallery> (debug only, pushed on Profile tab)

 CI (push/PR): hex+Brand+secret grep -> ktlintCheck -> lintDevDebug/lintProdDebug -> unit tests -> assembleDevDebug + assembleProdDebug
```

### Recommended Project Structure

```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── ClickMeApplication.kt          # @HiltAndroidApp
├── MainActivity.kt                # @AndroidEntryPoint: splash, edge-to-edge, ClickMeTheme { ClickMeApp() }
├── ClickMeApp.kt                  # AppRoute `when` switch
├── AppRoute.kt, AppViewModel.kt   # route state + destinationResolved (stub resolver -> ClientHome)
├── config/                        # AppConfig (reads BuildConfig), AppLinks (privacy URL from iOS AppEnvironment.swift)
├── navigation/                    # NavigationState.kt, Navigator.kt (from nav3 recipe), TabDefinition.kt, HomeTabScaffold.kt
├── ui/theme/                      # Color, Theme, Extras, Type, Shape, Spacing, Motion (exact files per UI-SPEC)
├── components/
│   ├── buttonstyles/PressScale.kt
│   ├── buttons/AsyncSaveButton.kt
│   ├── ValidatedField.kt
│   ├── avatar/{Avatar,TopBarAvatarButton}.kt
│   ├── backgrounds/GreenGlowBlobLayer.kt
│   ├── web/CustomTabLauncher.kt
│   └── EmptyState.kt, PlaceholderTabContent.kt
└── screens/
    ├── clientmode/home/{HomeClientScreen,HomeClientViewModel}.kt
    ├── expertmode/home/{HomeExpertScreen,HomeExpertViewModel}.kt
    └── debug/DesignSystemGalleryScreen.kt       # gated by BuildConfig.DEBUG
app/src/main/res/{font/nunito.ttf, drawable-nodpi/, values/{strings,colors,themes}.xml, values-v27/themes.xml, xml/}
app/src/{dev,prod}/res/            # dev-only launcher label override (optional)
scripts/check-design-rules.sh
.editorconfig
.github/workflows/ci.yml
OFL.txt (Nunito license) at repo root or app/src/main/assets/licenses/
```

### Pattern 1: Per-tab back stacks with Navigation 3 (copy the official recipe)

**What:** `NavigationState` holds one `NavBackStack` per top-level tab plus a saved `topLevelRoute`. `Navigator` mutates it. `toDecoratedEntries` wraps each stack's entries with `rememberSaveableStateHolderNavEntryDecorator` and `rememberViewModelStoreNavEntryDecorator`, and returns the entries for the start tab plus the selected tab. This is the "exit through home" pattern.
**When to use:** both shells. Create one `NavigationState` per shell (client: start = Explore; expert: start = Dashboard).
**Behavior vs the UI-SPEC:**
- Switching tabs keeps each tab's stack and saved UI state (the recipe's README states this and its counters survive config change and process death). The Phase 1 acceptance check "push a debug destination in one tab, switch away and back" is still required on a device.
- Back at a non-home tab root selects the home tab; Back at the home tab root exits (NavDisplay's handler is only active when more than one entry is shown). This is exactly the UI-SPEC rule.
- Re-tap to root is **not** in the recipe. Add it in the NavigationBar click handler (compiled in the spike):

```kotlin
// Source: github.com/android/nav3-recipes (multiplestacks) + re-tap addition, compiled against navigation3 1.2.0
NavigationBarItem(
    selected = tab.key == state.topLevelRoute,
    onClick = {
        if (tab.key == state.topLevelRoute) {
            val stack = state.backStacks.getValue(tab.key)
            while (stack.size > 1) stack.removeAt(stack.lastIndex)   // pop to tab root
        } else {
            navigator.navigate(tab.key)
        }
    },
    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
    label = { Text(stringResource(tab.labelRes)) },
    alwaysShowLabel = true,
)
```

Keys are `@Serializable data object ExploreRoot : NavKey`. The recipe files carry an Apache-2.0 header; keep it. They fail ktlint as pasted, so run `ktlintFormat` after copying.

### Pattern 2: Push/pop transitions

`NavDisplay(entries = ..., onBack = ..., transitionSpec = {...}, popTransitionSpec = {...})` accepts the specs directly on the `entries` overload (compiled). Use a 300 ms `FastOutSlowInEasing` horizontal slide + fade per the UI-SPEC Motion table. `predictivePopTransitionSpec` is also available.
**Tab-switch fade-through (150 ms in after 90 ms out)** is an open detail: when the tab changes, `NavDisplay` sees a list change and will pick the push or pop spec. Two options, both untested: (a) inside `transitionSpec`/`popTransitionSpec`, compare `initialState` and `targetState` scenes and return a fade-through when the two sides belong to different tabs; (b) per-entry metadata (`NavDisplay.TransitionKey` / `PopTransitionKey`, shown in the recipe `animations`). Time-box this; if neither is clean, ship the default spec for tab switches and log it as a polish item. It does not affect any success criterion.

### Pattern 3: Splash hold hook

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()                       // before super.onCreate
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !appViewModel.destinationResolved.value }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { ClickMeTheme { ClickMeApp(appViewModel) } }
    }
}
```
Phase 1: `AppViewModel` resolves to `AppRoute.ClientHome` on init, so `destinationResolved` is already true on the first frame. Phase 2 swaps in auto-login and adds the upper bound.

### Pattern 4: ViewModels in Nav3 entries

Use `@HiltViewModel` + `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()` inside an `entry<…> { }`. It compiled with `rememberViewModelStoreNavEntryDecorator()` in place (which is what `toDecoratedEntries` installs). Runtime scoping on a device was not exercised. Keep tab scaffolds and shared components free of Hilt so Robolectric can render them; only the screen wrappers (`HomeClientScreen`) call `hiltViewModel()`.

### Anti-Patterns to Avoid
- **`org.jetbrains.kotlin.android` plugin:** AGP 9 has built-in Kotlin; applying it fails with "Cannot add extension with name 'kotlin'" `[CITED: developer.android.com/build/migrate-to-built-in-kotlin]`. Don't add `kotlin-android` or `kotlin-kapt`.
- **Putting `AppRoute` inside the nav back stack** (D-01).
- **`Modifier.blur` for the glow** (UI-SPEC: gradient only).
- **Defining hex colors in XML other than the two necessary ones** (`#131313` `surface` for window/splash background). The CI grep only covers `.kt`, so keep XML color usage to that one named resource.
- **`@Preview` composables that read `BuildConfig` or Hilt.** They break previews and Robolectric tests.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Multiple back stacks and per-tab state | A custom saver or `SavedStateHandle` plumbing | Nav3 recipe `NavigationState`/`Navigator` + `rememberNavBackStack` + `SaveableStateHolder`/`ViewModelStore` decorators | Process-death and ViewModel scoping are already handled |
| Splash screen | A custom splash Activity or a Compose splash with a delay | `core-splashscreen` + `setKeepOnScreenCondition` | System-owned, no white flash, back-compat to API 26 |
| Press ripple + scale | Custom gesture detection | `MutableInteractionSource` + `collectIsPressedAsState` + `animateFloatAsState`, wrapped around M3 `Button` | Keeps ripple, a11y and semantics |
| Field borders | A custom border-drawing text field | `OutlinedTextField` + `OutlinedTextFieldDefaults.Container(focusedBorderThickness, unfocusedBorderThickness)` | Compiled against M3 1.4.0 |
| Chrome Custom Tabs | A WebView | `androidx.browser.CustomTabsIntent` (fallback `ACTION_VIEW`) | UI-SPEC component 6 |
| Image loading + crossfade | Manual bitmap loading | Coil 3 `AsyncImage` + `ImageRequest.crossfade(200)` | |
| Code-style rules | Custom lint rules for naming | ktlint + `.editorconfig` | D-08 |
| Hex-literal gate | A custom lint detector | 20-line grep script (`scripts/check-design-rules.sh`, tested) | D-07 allows a grep step |

**Key insight:** every piece of Phase 1 has an off-the-shelf answer. The effort is in wiring them to the UI-SPEC exactly and keeping the build green on the new toolchain.

## Runtime State Inventory

Not a rename/refactor/migration phase. Greenfield on top of the Android Studio template; the package rename already happened. Omitted deliberately. One related check: the template's `strings.xml` still says `app_name = ClickMe_Android`, and the theme is `Theme.ClickMe_Android`. Both change in this phase (D-10), and `AndroidManifest.xml` references both.

## Common Pitfalls

### Pitfall 1: Kotlin 2.2.10 cannot compile against the current library set
**What goes wrong:** `Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin. The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read versions up to 2.3.0`, on every Kotlin file. `[VERIFIED: reproduced in scratch build]`
**Why it happens:** Hilt 2.60.1, OkHttp 5.5.0, Retrofit 3.0.0 and coroutines 1.11.0 resolve `kotlin-stdlib` to 2.4.10. A Kotlin compiler only reads metadata one minor version ahead of itself.
**How to avoid:** Set `kotlin = "2.4.21"` in the catalog. It drives `org.jetbrains.kotlin.plugin.compose` and `.serialization`, and AGP 9's built-in Kotlin uses that KGP. `[VERIFIED: both flavors, release R8, lint, tests built on 2.4.21]`. CONTEXT says to use versions "compatible with Kotlin 2.2"; that is not possible with current libraries, so this is a deliberate deviation (see Open Question 2).
**Warning signs:** the error above, or a Gradle resolution mention of `kotlin-stdlib:2.4.x` while `kotlin = "2.2.x"`.

### Pitfall 2: ktlint flags Compose and template code
**What goes wrong:** `function-naming` on every `@Composable`, plus many `ktlint_official` rules (trailing commas, `function-signature`, `final-newline`). The template's `ExampleUnitTest` and `ExampleInstrumentedTest` use `import org.junit.Assert.*`, which `ktlintFormat` cannot fix (wildcard import). `[VERIFIED: ran ktlintCheck/ktlintFormat]`
**How to avoid:** Add `/.editorconfig` with `ktlint_code_style = ktlint_official`, `max_line_length = 120`, and `ktlint_function_naming_ignore_when_annotated_with = Composable` (verified: main sources pass with it). Run `./gradlew ktlintFormat` once over the whole repo (it covers `*.kts` too), then fix the wildcard imports by hand or delete the two template example tests. The recipe files you copy also need a `ktlintFormat` pass.
**Warning signs:** the first CI run fails on `ktlintKotlinScriptCheck` or `ktlintAndroidTestSourceSetCheck`.

### Pitfall 3: UI-SPEC theme XML fails lint (FND-02)
**What goes wrong:** `android:windowLightNavigationBar` requires API 27 and minSdk is 26: `NewApi` is a lint **error**, so `lintDevDebug` fails the build. `[VERIFIED: lintDevDebug failed on exactly this]`
**How to avoid:** Put `windowLightNavigationBar=false` in `res/values-v27/themes.xml` (an override of `Theme.ClickMe`), not in the base `values/themes.xml`. Dark system bar icons are the default on API 26 anyway since `windowLightStatusBar=false` and the nav bar is dark by default. Other lint warnings seen: `UnusedAttribute` for `enableOnBackInvokedCallback` (harmless, add `tools:ignore`), `IconLocation` (put the PNG in `drawable-nodpi`), `RedundantLabel` (drop `android:label` from the activity), `UnusedResources` (delete the template `colors.xml` entries), and the "newer Gradle available" info message (ignore, do not bump Gradle).
**Lint policy:** `lint { abortOnError = true; warningsAsErrors = false }`. Run `lintDevDebug lintProdDebug` in CI. Only add a `lint-baseline.xml` if something outside our control appears.

### Pitfall 4: Icon and splash artwork
**What goes wrong:** (a) `AppIcon-1024-Tinted.png` is pure white everywhere (every pixel 255,255,255), so a monochrome layer from it is a solid white square. (b) `AppIcon-1024.png` is opaque with a `#171717` background baked in, so used as an adaptive foreground it hides the background layer and the system mask crops a square art tile. (c) `SplashLogo@3x.png` is transparent RGBA 768x768; the visible mark spans about x 89–685, y 83–690 (78% of the canvas), with a soft glow around it. `[VERIFIED: decoded the PNGs]`
**How to avoid:**
- Adaptive foreground and monochrome: both from `SplashLogo@3x.png` (transparent mark), scaled so the visible mark sits inside the 66 dp safe zone of the 108 dp canvas (image about 74 dp square, since the mark is 78% of it). Background layer: solid `#131313` (UI-SPEC) as a color resource. The system tints the monochrome layer by alpha, so the glow becomes a soft halo; if it looks muddy, clip the alpha at about 40% with `sips`/a one-off script. This deviates from D-11's "AppIcon-1024 foreground / Tinted monochrome", for the reasons above; flag it in the plan.
- Resize with macOS `sips -Z <px> in.png --out out.png` (available) for the density buckets, or ship a single `drawable-nodpi` 432 px PNG.
- Splash: a `layer-list` wrapper sizes the mark. UI-SPEC wants about 132 dp visible inside the 192 dp circle, so draw the bitmap at about 171 dp (132 / 0.78) centered in the 288 dp canvas. This compiled; **verify visually on a device and an API 26 emulator** (the `layer-list` item `width`/`height` is the least certain part).

```xml
<!-- res/drawable/splash_logo.xml -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:width="171dp" android:height="171dp" android:gravity="center">
        <bitmap android:src="@drawable/splash_logo_mark" android:gravity="fill" />
    </item>
</layer-list>
```

### Pitfall 5: Nunito is a variable font upstream
**What goes wrong:** the UI-SPEC says "static files `nunito_regular.ttf` / `nunito_semibold.ttf`", but `googlefonts/nunito` publishes only `fonts/variable/Nunito[wght].ttf` (277 KB, plus italic and ital+wght variants). `[VERIFIED: GitHub API listing]`
**How to avoid:** Copy `Nunito[wght].ttf` as `res/font/nunito.ttf` (res names must be lowercase, no brackets) and declare two weights from the one file. Variable fonts are supported on API 26+ in Compose `[CITED: developer.android.com/develop/ui/compose/text/fonts]`, which equals minSdk.

```kotlin
// ui/theme/Type.kt  (Source: developer.android.com/develop/ui/compose/text/fonts, adapted)
@OptIn(ExperimentalTextApi::class)
val NunitoFamily = FontFamily(
    Font(R.font.nunito, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)
```
Add the SIL OFL text (`OFL.txt` from the same repo) to the repo. Confirm rendering on the API 26 emulator. If the variable font misbehaves, cut static instances with `fonttools varLib.instancer "Nunito[wght].ttf" wght=400` and `wght=600` and follow the UI-SPEC literally.

### Pitfall 6: `BuildConfig` is off by default and `DEBUG` is per build type
**What goes wrong:** AGP 9 does not generate `BuildConfig` unless `buildFeatures { buildConfig = true }`. And `BuildConfig.DEBUG` is true for the `debug` build type, so the gallery and scaffold switch appear in **both** `devDebug` and `prodDebug` and are absent in release. That matches the UI-SPEC, but don't expect "dev flavor only".
**How to avoid:** enable `buildConfig`; gate debug affordances with `BuildConfig.DEBUG`; check that `assembleProdRelease` contains no gallery strings if that matters.

### Pitfall 7: Gradle daemon requires JDK 25
**What goes wrong:** `gradle/gradle-daemon-jvm.properties` (tracked) sets `toolchainVersion=25`. Locally Gradle already has a Temurin 25 in `~/.gradle/jdks`. CI must provide JDK 25 or let the foojay resolver download it.
**How to avoid:** `actions/setup-java` with `java-version: 25`. The `ubuntu-latest` image also ships JDK 25 and Android platform 37.0 (rev 2) `[CITED: github.com/actions/runner-images Ubuntu2404-Readme]`, so no extra SDK install step is needed.

### Pitfall 8: Same applicationId + Auto Backup + EncryptedSharedPreferences (carry-forward to Phase 2)
`AndroidManifest.xml` currently has `allowBackup="true"` with the template backup rules. Backed-up encrypted prefs are restored on a new device without the Keystore key, which crashes on first read. Set `android:allowBackup="false"` now (or exclude the prefs file in `data_extraction_rules.xml` / `backup_rules.xml`) so Phase 2 does not hit this. `[ASSUMED]` (well-known behavior, not re-verified this session)

### Pitfall 9: Robolectric and Hilt
Robolectric Compose tests that touch Hilt need `hilt-android-testing`, a custom runner and more setup. Avoid it by design: `HomeTabScaffold`, components and `PlaceholderTabContent` take plain parameters. Unit-test the Hilt wrappers only by their stateless logic (`AppViewModel` resolver).

## Code Examples

### Version catalog (target state; every entry was resolved and built)

```toml
[versions]
agp = "9.4.1"
kotlin = "2.4.21"            # was 2.2.10; see Pitfall 1
ksp = "2.3.12"
hilt = "2.60.1"
composeBom = "2026.09.00"
navigation3 = "1.2.0"
lifecycle = "2.11.0"
coil = "3.6.3"
okhttp = "5.5.0"
retrofit = "3.0.0"
moshi = "1.15.2"
ktlint = "14.2.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.19.1" }
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version = "1.2.0" }
androidx-browser = { group = "androidx.browser", name = "browser", version = "1.10.0" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.13.0" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-navigation3 = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
androidx-hilt-lifecycle-viewmodel-compose = { group = "androidx.hilt", name = "hilt-lifecycle-viewmodel-compose", version = "1.4.0" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
kotlinx-serialization-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-core", version = "1.12.0" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version = "1.11.0" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version = "1.11.0" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-moshi = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "retrofit" }
moshi = { group = "com.squareup.moshi", name = "moshi", version.ref = "moshi" }
robolectric = { group = "org.robolectric", name = "robolectric", version = "4.17" }
junit = { group = "junit", name = "junit", version = "4.13.2" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
```
Root `build.gradle.kts`: every plugin above with `apply false`. Module plugins: `android.application`, `kotlin.compose`, `kotlin.serialization`, `ksp`, `hilt`, `ktlint`. **No** `kotlin-android` and **no** `kapt`.

### Flavors and BuildConfig (compiled on AGP 9.4.1)

```kotlin
android {
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://clickme-backend-production.up.railway.app/api/v1\"")
            buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"pk_test_…copy from Environments.plist Development…\"")
            buildConfigField("String", "AGORA_APP_ID", "\"483eaa4d11fb44b6acc91bf5ceae36c7\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://clickme-backend-production.up.railway.app/api/v1\"")
            buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"pk_live_…copy from Environments.plist Production…\"")
            buildConfigField("String", "AGORA_APP_ID", "\"483eaa4d11fb44b6acc91bf5ceae36c7\"")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    lint { abortOnError = true; warningsAsErrors = false }
}
```
The Agora App ID is the same in all three iOS environments, and I copied it from `Environments.plist` (`483eaa4d…`). The exact Stripe key strings are in that plist; copy, don't retype. Optional dev label: `app/src/dev/res/values/strings.xml` with `<string name="app_name">ClickMe Dev</string>` overrides the main value `[ASSUMED: standard flavor resource merging]`. If used, ensure the label appears on the launcher only; D-10 default is "ClickMe".

### Dependencies block (verified set)

```kotlin
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
    implementation(libs.coil.compose); implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp); debugImplementation(libs.okhttp.logging)
    implementation(libs.retrofit); implementation(libs.retrofit.converter.moshi); implementation(libs.moshi)
    implementation(libs.kotlinx.coroutines.android); implementation(libs.kotlinx.serialization.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit); testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom)); testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
```
Keep the existing `release { optimization { enable = true … } }` block. An R8 release build with Hilt, serialization and Coil on the classpath built successfully (`assembleProdRelease`). The `okhttp-logging` interceptor is `debugImplementation`, so it cannot be referenced from main source; Phase 2 should add it through a debug-only `OkHttpClient` hook or a debug source set.

### Theme XML (corrected for lint)

```xml
<!-- res/values/themes.xml -->
<style name="Theme.ClickMe" parent="android:Theme.Material.NoActionBar">
    <item name="android:windowBackground">@color/surface</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
    <item name="android:windowLightStatusBar">false</item>
</style>
<style name="Theme.ClickMe.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/surface</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/splash_logo</item>
    <item name="postSplashScreenTheme">@style/Theme.ClickMe</item>
</style>
<!-- res/values-v27/themes.xml -->
<style name="Theme.ClickMe" parent="android:Theme.Material.NoActionBar">
    <item name="android:windowLightNavigationBar">false</item>
</style>
```
`res/values/colors.xml` holds only `<color name="surface">#131313</color>` (delete the purple/teal template entries). The v27 file must repeat the base items it overrides, or declare the style with the same parent; the scratch build only exercised the base theme, so double-check this in the first build.

### Design rules script (tested on fixtures: clean exits 0, dirty exits 1)

```bash
#!/usr/bin/env bash
# scripts/check-design-rules.sh
set -euo pipefail
SRC="app/src"; THEME_COLOR="ui/theme/Color.kt"; fail=0
hits=$(grep -rnE --include='*.kt' 'Color\((0x|red *=|[0-9])' "$SRC" | grep -v "$THEME_COLOR" || true)
[ -n "$hits" ] && { echo "Color literals outside $THEME_COLOR:"; echo "$hits"; fail=1; }
hits=$(grep -rnE --include='*.kt' '(object|class|enum class|interface) +[A-Za-z0-9_]*Brand\b' "$SRC" || true)
[ -n "$hits" ] && { echo "Forbidden *Brand classes:"; echo "$hits"; fail=1; }
hits=$(grep -rnE '(sk_live|sk_test|rk_live|rk_test)_[A-Za-z0-9]+' app gradle *.kts 2>/dev/null || true)
[ -n "$hits" ] && { echo "Stripe secret key pattern:"; echo "$hits"; fail=1; }
exit $fail
```
The third check is a cheap guard that no Stripe secret key lands in the repo next to the committed publishable keys.

### `.editorconfig`

```ini
root = true

[*.{kt,kts}]
indent_size = 4
max_line_length = 120
ktlint_code_style = ktlint_official
ktlint_function_naming_ignore_when_annotated_with = Composable
```

### GitHub Actions (`.github/workflows/ci.yml`)

```yaml
name: CI
on:
  push:           # all branches (D-07)
  pull_request:
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true
jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with: { distribution: temurin, java-version: '25' }
      - uses: gradle/actions/setup-gradle@v6
      - name: Design rules (hex colors, *Brand, secrets)
        run: bash scripts/check-design-rules.sh
      - name: ktlint, lint, unit tests, both flavors
        run: ./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest assembleDevDebug assembleProdDebug --stacktrace
      - uses: actions/upload-artifact@v7
        if: failure()
        with: { name: reports, path: app/build/reports }
```
Action majors come from each repo's latest release on 2026-10-19: checkout v7.0.1, setup-java v6.1.0, gradle/actions v6.4.0, upload-artifact v7.0.2 `[VERIFIED: GitHub releases API]`. The workflow YAML itself is **not** run-verified; the commands inside it are. A push to GitHub is required to see it pass (the remote exists: `mercforhire/ClickMe2026_Android`; no `gh` CLI locally, so check the Actions tab in the browser).

### Compose/M3 APIs from the UI-SPEC, confirmed to compile

```kotlin
// Source: compiled against BOM 2026.09.00 / material3 1.4.0 / coil 3.6.3 / browser 1.10.0
OutlinedTextFieldDefaults.Container(enabled = true, isError = false, interactionSource = src,
    shape = CircleShape, focusedBorderThickness = 2.dp, unfocusedBorderThickness = 1.dp)
Modifier.shadow(12.dp, CircleShape, ambientColor = c, spotColor = c)
Modifier.semantics { error("msg"); contentType = ContentType.Password   // import androidx.compose.ui.autofill.ContentType
                     stateDescription = "Saving"; liveRegion = LiveRegionMode.Polite }
KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false)
AsyncImage(model = ImageRequest.Builder(ctx).data(url).crossfade(200).build(), contentDescription = null, contentScale = ContentScale.Crop) // coil3.*
CustomTabsIntent.Builder().setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
    .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(c).setNavigationBarColor(c).build())
    .setShowTitle(true).setUrlBarHidingEnabled(true).build().launchUrl(ctx, uri)
```
Note the `ContentType` import; `androidx.compose.ui.semantics.ContentType` does not exist.

### JVM test of the navigator (passed)

```kotlin
val stacks = mapOf<NavKey, NavBackStack<NavKey>>(Explore to NavBackStack(Explore), Bookings to NavBackStack(Bookings))
val state = NavigationState(Explore, mutableStateOf<NavKey>(Explore), stacks)
val nav = Navigator(state)
nav.navigate(Detail); nav.navigate(Bookings); nav.navigate(Explore)
assertEquals(listOf<NavKey>(Explore, Detail), stacks.getValue(Explore).toList())   // Explore kept its pushed screen
nav.navigate(Bookings); nav.goBack()
assertEquals(Explore, state.topLevelRoute)                                           // Back at non-home root -> home tab
```
This is the automated proof of the FND-12 per-tab-stack criterion that needs no emulator.

## Play Console Organization Account (REL-01, REL-02), human-action checklist

Google's pages are partial (they do not state fee or timelines in one place), so the plan should treat these as steps to confirm in the live UI. Sources: `[CITED: support.google.com/googleplay/android-developer/answer/6112435]`, `/answer/13628312`, `/answer/10788890`.

1. **Confirm the D-U-N-S number (REL-01).** Find the 9-digit number; look it up on Dun & Bradstreet's site. Check the **legal name and address in the D&B record match your corporation's legal name and address exactly**, because Google compares the org name/address to D&B, and mismatches are the most common cause of a failed verification. If the record is wrong, correct it with D&B before applying (D&B updates are free; a *new* D-U-N-S can take up to 30 days per Google). Do not pay third-party D-U-N-S "expediting" services. If the corporation has several D-U-N-S numbers, use the one whose details match.
2. **Prepare** a Google account for the org (a shared company mailbox is better than a personal Gmail, since it is permanent and recoverable `[ASSUMED: best practice]`), the corporation's legal name and address, a website, a contact phone and email, and a **non-prepaid** credit/debit card (prepaid cards are rejected).
3. **Sign up** at the Play Console sign-up page with that Google account (must be 18+). Accept the Play Developer Distribution Agreement.
4. **Pay the one-time US$25 registration fee** (non-refundable if identity information is judged invalid).
5. **Choose account type: Organization.** Enter the legal name, address, D-U-N-S, contact and developer email/phone. Create or select the Google Payments profile; its name/address is what Google matches against D&B, and it is used for identity only unless you later sell paid apps.
6. **Verify contact email and phone** with the one-time passwords Google sends. Do the same for the developer (public) email/phone.
7. **Submit for verification** and upload any documents Google asks for (for example proof of incorporation, government ID of the account holder). The verification time is not stated in Google's pages; the PRD says "days", third-party guides say days to a month `[ASSUMED]`.
8. **Record the wait in STATE.md** (this is the Phase 1 success criterion 4): date submitted, D-U-N-S status (confirmed/requested), who must respond to Google, the expected-by date, and that REL-03 (first signed upload, Phase 2) is blocked on it. Update the existing `Blockers/Concerns` "OUTSIDE WAIT" line rather than adding a new one.
9. **After approval** (not Phase 1): REL-03 uploads a signed internal build to register `com.uptrendinvestments.clickmeapp`, which is permanent.

**Org vs personal testing rules:** Google's closed-test requirement (12 testers for 14 days) is documented for *personal* accounts created after 13 Nov 2023; the same pages do not mention organization accounts. The PRD relies on the org account being exempt, which is consistent with Google's wording but not explicitly stated, so treat it as `[ASSUMED]` until the Console shows no such requirement. The Android-device verification requirement is likewise described only for personal accounts. A local emulator and `adb` already exist on this machine if a device check is requested anyway.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `org.jetbrains.kotlin.android` plugin | AGP 9 built-in Kotlin; Kotlin version comes from the compose/serialization/KSP plugins | AGP 9.0 | Do not apply `kotlin-android` or `kotlin-kapt` |
| Navigation Compose 2 (`NavHost`, nested graphs, `saveState/restoreState`) | Navigation 3 (`NavDisplay`, app-owned `NavBackStack` lists, entry decorators) | Nav3 stable 1.x; 1.2.0 current | Per-tab stacks are plain state; the UI-SPEC's nav section needs translating |
| `hilt-navigation-compose` | `hilt-lifecycle-viewmodel-compose` (`androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`) | hilt 1.3/1.4 | Avoids dragging in Navigation Compose 2 |
| Coil 2 (`io.coil-kt:coil-compose`, package `coil.*`) | Coil 3 (`io.coil-kt.coil3:*`, package `coil3.*`, separate network artifact) | Coil 3 | UI-SPEC says "Coil (`coil-compose`)": use the Coil 3 coordinates |
| kapt for Hilt | KSP | | `ksp(libs.hilt.compiler)` verified |
| `createComposeRule` (v1) | `androidx.compose.ui.test.junit4.v2.createComposeRule` | compose-ui-test 1.12 | Deprecation warning in v1 |
| Android 12 `setOnExitAnimationListener` custom splash | System default exit | | UI-SPEC: no listener |

**Deprecated/outdated:**
- `EncryptedSharedPreferences` is deprecated upstream; the decision is locked (STATE.md), nothing to do in Phase 1 beyond Pitfall 8.
- Moshi `moshi-kotlin-codegen` prints a "Kapt support is deprecated" javac warning when it is on the classpath even with KSP; harmless, but that is the reason to add it in Phase 2 instead of now.

**API 37 note (targetSdk 37):** large-screen resizability/orientation restrictions can no longer be opted out of `[CITED: developer.android.com/about/versions/17/behavior-changes-17]`, so do not set `screenOrientation`. Background audio hardening (Phase 9) and certificate transparency (on by default, no action) also apply later.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Auto Backup restoring EncryptedSharedPreferences without its Keystore key crashes the app | Pitfall 8 | Low: setting `allowBackup=false` is harmless either way |
| A2 | Flavor-specific `src/dev/res/values/strings.xml` overrides `app_name` | Code Examples | Low: fall back to `resValue` in the flavor block |
| A3 | Tab-switch fade-through can be done via `transitionSpec` scene comparison or entry metadata | Pattern 2 | Low: fall back to default specs |
| A4 | Organization accounts are exempt from the 12-tester/14-day closed test and from device verification | Play section | Medium: could add 2 weeks to the release path; check in the Console after step 7 |
| A5 | Org verification takes "days" (PRD) to about a month | Play section, step 7 | Medium: affects REL-03/REL-05 scheduling, logged in STATE.md |
| A6 | A shared company mailbox is better than a personal Google account as the Play account owner | Play section, step 2 | Low: user preference |
| A7 | Using the transparent `SplashLogo` mark for the adaptive/monochrome icon looks acceptable (the glow becomes a halo in the monochrome layer) | Pitfall 4 | Low: may need alpha cleanup; needs a visual check |
| A8 | The `values-v27` theme override works as written (only the base theme was built) | Code Examples | Low: caught by the first `assemble` + lint |
| A9 | The `layer-list` splash drawable renders at the intended size on API 26–30 (compat) and 31+ | Pitfall 4 | Low-medium: visual check on two emulators |
| A10 | GitHub secret scanning does not block committed `pk_live`/`pk_test` publishable keys | Code Examples | Low: if it flags, dismiss as false positive |

## Open Questions

1. **Navigation library: CONTEXT D-01 vs UI-SPEC vs FND-01 wording**
   - What we know: D-01 (locked) says Navigation 3. The UI-SPEC "Other libraries" row and "App Shell and Navigation" section say `navigation-compose`, `NavHost`, nested graphs and `popUpTo { saveState }`. REQUIREMENTS FND-01 says "Navigation Compose".
   - What's unclear: whether the user wants the docs reconciled.
   - Recommendation: follow D-01. Treat the UI-SPEC's *behavior* rules as binding and its *mechanism* text as superseded. The plan should note the supersession so the plan checker does not flag a "missing navigation-compose".
2. **Kotlin 2.2.10 to 2.4.21 bump**
   - What we know: CONTEXT says "compatible with AGP 9.4 / Kotlin 2.2 … already in the template"; that combination does not build with current libraries (reproduced).
   - Recommendation: bump Kotlin (verified). Keep AGP 9.4.1 and Gradle 9.6.0 as is. Do not bump Gradle (lint advises 9.8.2; unnecessary).
3. **Monochrome and foreground icon source (D-11)**: the Tinted PNG is blank white. Recommendation in Pitfall 4 uses the transparent splash mark. The user may want to provide a proper tinted/monochrome asset later; the layer can be swapped without code changes.
4. **Nunito static vs variable file**: variable recommended (one file, API 26+ supported); static instances are the fallback.
5. **iOS `Current = Production` with backend in Stripe test mode** (D-05 flag): remains a note for the user to check on the iOS side; no Android action.
6. **Can CI be proven green in this phase?** Only by pushing to GitHub. The plan needs an explicit "push and confirm the Actions run is green" task, and the repo currently has no `.github/` folder.
7. **`BuildConfig.DEBUG` in prodDebug**: the debug gallery also shows in `prodDebug` builds. Acceptable; confirm that is what the user wants.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 (Gradle daemon toolchain) | All builds | ✓ (Gradle-managed Temurin in `~/.gradle/jdks`) | 25.0.3 | foojay resolver auto-downloads |
| Gradle wrapper | All builds | ✓ | 9.6.0 | — |
| Android SDK platform 37 | compileSdk 37 | ✓ | android-37.0 | — |
| Android build-tools | AGP 9.4 default | ✓ | 36.0.0 | — |
| Emulator + system images | Manual device checks (splash, API 26 font, tab stacks) | ✓ images for android-34 and android-37.0 | — | Physical device. No API 26 image installed: install one for the Nunito/splash compat check. |
| `adb` | Installing builds | ✓ (`platform-tools` present; not on PATH in my shell) | — | Run via full path |
| Network access to Google Maven / Maven Central | Dependency resolution | ✓ | — | — |
| `sips` (macOS) | Resizing icon PNGs | ✓ | — | Android Studio Image Asset |
| `gh` CLI | Checking CI | ✗ | — | GitHub web UI |
| GitHub remote | CI run | ✓ `origin` = github.com/mercforhire/ClickMe2026_Android | — | — |
| Dun & Bradstreet / Google Play Console access | REL-01/02 | User-only | — | n/a (human-action) |

**Missing dependencies with no fallback:** none for the code work. The Play account steps depend entirely on the user.
**Missing dependencies with fallback:** API 26 system image (install via SDK Manager for the compat check); `gh` (use the web UI).

## Validation Architecture

(`.planning/config.json` does not exist, so `nyquist_validation` is treated as enabled.)

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + Compose `ui-test-junit4` (JVM only, satisfies D-09) |
| Config file | none yet; Wave 0 adds `testOptions { unitTests.isIncludeAndroidResources = true }` and the test dependencies |
| Quick run command | `./gradlew :app:testDevDebugUnitTest --tests "<class>"` |
| Full suite command | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest assembleDevDebug assembleProdDebug && bash scripts/check-design-rules.sh` |

A JVM `NavigatorTest` and a Robolectric Compose test (`@Config(sdk = [35])`) both ran green in the scratch project.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FND-01 | Both flavors build; `BASE_URL` is the production URL; dev key starts `pk_test_`, prod `pk_live_`; Agora ID non-empty | unit (per-flavor source sets `src/testDev`, `src/testProd`) + build | `./gradlew assembleDevDebug assembleProdDebug testDevDebugUnitTest testProdDebugUnitTest` | ❌ Wave 0 |
| FND-02 | ktlint + lint + design-rule script pass; CI workflow exists | script + gradle | `./gradlew ktlintCheck lintDevDebug lintProdDebug && bash scripts/check-design-rules.sh` | ❌ Wave 0 |
| FND-02 | Design-rule script fails on a seeded `Color(0x…)` / `*Brand` | script negative test (fixture dir) | `bash scripts/check-design-rules.sh` against a fixture | ❌ Wave 0 (optional) |
| FND-09 | Color scheme slots match the UI-SPEC hex; every typography slot uses one of 12/16/20/28 sp; `NunitoFamily` has 400 and 600 | unit | `./gradlew :app:testDevDebugUnitTest --tests "*ThemeTest"` | ❌ Wave 0 |
| FND-10 | `AsyncSaveButton` Idle/Saving/Saved semantics (stateDescription, not clickable when Saving/Saved); `ValidatedField` error semantics; `Avatar` fallback for null URL; `EmptyState` requires heading/body | Robolectric Compose | `./gradlew :app:testDevDebugUnitTest --tests "*ComponentsTest"` | ❌ Wave 0 |
| FND-10 | Custom Tab launcher: non-http(s) URI rejected, `onFailure` called when no activity resolves | Robolectric unit | `--tests "*CustomTabLauncherTest"` | ❌ Wave 0 |
| FND-11 | Stub resolver yields `ClientHome`; `destinationResolved` true on first read | unit (coroutines-test) | `--tests "*AppViewModelTest"` | ❌ Wave 0 |
| FND-11 | Splash visual, no white flash, no fixed delay, API 26 and 31+ | **manual** (needs a device/emulator; D-09 excludes instrumented CI) | — | manual checklist |
| FND-12 | Per-tab stacks kept across tab switches; Back at non-home root selects home; re-tap pops to root | JVM unit | `--tests "*NavigatorTest"` | ❌ Wave 0 (logic verified in spike) |
| FND-12 | Client shows 5 labels (Explore, Search, Bookings, Chats, Profile) and expert 5 labels (Dashboard, Bookings, Topics, Chats, Profile); selected tab exposes selected state | Robolectric Compose on `HomeTabScaffold` with fake tab lists | `--tests "*HomeTabScaffoldTest"` | ❌ Wave 0 |
| FND-12 | Push a debug destination, switch tab and return: stack and scroll position intact, on a real build | **manual** (device) | — | manual checklist |
| REL-01 / REL-02 | D-U-N-S confirmed; org account submitted; wait logged | **manual** (human-action); gate on a STATE.md entry | `grep -n "Play Console" .planning/STATE.md` | n/a |

### Sampling Rate
- **Per task commit:** `./gradlew ktlintCheck :app:testDevDebugUnitTest` (about 30–60 s warm).
- **Per wave merge:** the full suite command.
- **Phase gate:** full suite green on GitHub Actions, plus the manual device checklist, before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `.editorconfig`, `scripts/check-design-rules.sh`, `.github/workflows/ci.yml`
- [ ] Test dependencies and `testOptions` in `app/build.gradle.kts`; delete or fix the template `ExampleUnitTest`/`ExampleInstrumentedTest` (wildcard imports fail ktlint)
- [ ] Test files listed in the table above under `app/src/test/java/com/uptrendinvestments/clickmeapp/…`
- [ ] Framework install: none (Gradle resolves it)
- [ ] Manual checklist doc for splash, per-tab stack on device, API 26 font and splash, 200% font scale

## Security Domain

`security_enforcement` is not set in config (absent = enabled). Phase 1 has no auth, storage or network calls, so the surface is small.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (Phase 2/3) | — |
| V3 Session Management | no (Phase 2) | — |
| V4 Access Control | no | — |
| V5 Input Validation | partly | `ValidatedField` is presentation only; real validation arrives with the screens that use it. Custom Tab launcher must allowlist `https` (and `http` for dev links) and reject other schemes. |
| V6 Cryptography | no | None hand-rolled; Phase 2 uses EncryptedSharedPreferences |
| V8 Data protection / V14 Config | yes | No secrets committed (publishable Stripe key and Agora App ID only; CI grep blocks `sk_*`/`rk_*`); `allowBackup=false`; debug-only logging interceptor; debug UI gated by `BuildConfig.DEBUG`; cleartext HTTP not permitted (default for targetSdk 28+; the backend is HTTPS) |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Intent/URL injection via the link launcher | Tampering / Elevation | Scheme allowlist in `openCustomTab`; `mailto:` handled separately via `ACTION_SENDTO` |
| Secret leakage in the repo | Information disclosure | `.gitignore` (keystores, `google-services.json`, `keystore.properties`) already present; CI grep for Stripe secret key patterns |
| Verbose network logs in release | Information disclosure | `logging-interceptor` is `debugImplementation` only; redact `Authorization` when added in Phase 2 |
| Exported components | Elevation | Only `MainActivity` exported (launcher intent filter); do not add other exported components |
| Debug affordances reaching production | Elevation | `BuildConfig.DEBUG` gating; verify the release APK has no gallery route |
| Dependency confusion / slopsquatting | Spoofing | Coordinates verified against publisher group ids and official recipes; versions pinned in the catalog |

## Project Constraints (from CLAUDE.md)

- The iOS code in `ios-reference/` is the source of truth for behavior; deviate only for Android platform conventions.
- Locked stack: Jetpack Compose + Material 3, MVVM with Compose State, Kotlin coroutines (no RxJava), Hilt, Retrofit + Moshi, EncryptedSharedPreferences for JWT, Firebase Messaging, Agora Android SDK, Stripe Android SDK with PaymentSheet. minSdk 26, targetSdk 37, compileSdk 37.
- Platform swaps: `.sheet` → `ModalBottomSheet`; iOS back chevron → `TopAppBar` back arrow; `.alert` → `AlertDialog`; iOS switch → Material `Switch`; `PhotosPicker` → `PickVisualMedia` launcher; `.toolbar` title → `TopAppBar` title; `.task` → `LaunchedEffect(Unit)`.
- File organization mirrors the iOS folder tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/` (ViewModel next to its Screen).
- Same REST contract as iOS; `backend-reference/docs/ios/` specs are authoritative (not used in Phase 1).
- Project skills: `.claude/skills/` and `.agents/skills/` do not exist, so none apply.
- Global instruction from the user's memory: MVP scope, dark theme only, Nunito font, Material styling acceptable (matches the UI-SPEC).

## Sources

### Primary (HIGH confidence)
- Scratch-project builds in `/tmp/gsdr/proj` (copy of this repo): `assembleDevDebug`, `assembleProdDebug`, `assembleProdRelease` (R8), `lintDevDebug`, `ktlintCheck`/`ktlintFormat`, `testDevDebugUnitTest`, compile of the UI-SPEC component APIs. Logs in `/tmp/gsdr/b*.log`.
- Google Maven and Maven Central `maven-metadata.xml` for every coordinate and version listed.
- https://github.com/android/nav3-recipes (`multiplestacks`, `animations`, `passingarguments/viewmodels/hilt`): source read in full and compiled.
- https://developer.android.com/build/releases/agp-9-4-0-release-notes (Gradle 9.6.0, JDK 17+, compileSdk 37)
- https://developer.android.com/build/migrate-to-built-in-kotlin (no `kotlin-android`, KSP/kapt rules)
- https://developer.android.com/develop/ui/compose/text/fonts (variable fonts, API 26+)
- https://developer.android.com/develop/ui/views/launch/splash-screen (icon canvas 288/192 dp, theme attrs)
- https://developer.android.com/about/versions/17/behavior-changes-17
- https://github.com/google/dagger/releases and https://github.com/google/ksp/releases (AGP 9 / Kotlin notes)
- https://github.com/actions/runner-images (Ubuntu 24.04 readme: JDK 25, Android platform 37.0)
- https://support.google.com/googleplay/android-developer/answer/6112435 (fee, account types, steps), /answer/13628312 (org accounts, D-U-N-S), /answer/10788890 (required info), /answer/14151465 (personal-account testing rule)
- `ios-reference/ClickMe2026/Environments.plist`, `Configuration/AppEnvironment.swift`, `ClickMe2026App.swift`, `Assets.xcassets` (decoded PNGs)

### Secondary (MEDIUM confidence)
- https://developer.android.com/guide/navigation/navigation-3 (overview only; API details taken from the recipes instead)
- Web search summaries of the Play org verification flow (third-party guides on D-U-N-S name/address matching and timelines)

### Tertiary (LOW confidence)
- Org-account exemption from closed testing and device verification: inferred from Google's wording ("personal accounts"), not stated.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. Versions read from registries and the full set built, linted and tested in a scratch copy.
- Architecture (Nav3 multi-stack): HIGH for the logic (recipe + JVM test), MEDIUM for on-device behavior (stack and scroll retention across switching, tab-switch animation not yet run on a device).
- Pitfalls: HIGH for 1–3, 5–7 (reproduced or measured); MEDIUM for 4 (asset findings measured, rendering unverified) and 8 (from general knowledge).
- Play Console steps: MEDIUM. Fee and sign-up order are from Google's page; timelines and document requests are not stated by Google.

**Research date:** 2026-10-19
**Valid until:** about 2026-11-16 (30 days; AGP/Kotlin/Compose release cadence is fast, so re-run the metadata checks if planning slips past that)
