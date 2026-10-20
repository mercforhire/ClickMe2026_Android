# Phase 1: Foundation, App Shell & Play Account - Pattern Map

**Mapped:** 2026-10-19
**Files analyzed:** 44 new/modified (grouped below)
**Analogs found:** 30 / 44 (the rest are net-new infrastructure; use RESEARCH.md code examples)

Greenfield port. Two analog sources:
- **T** = Android Studio template file already in `app/` or `gradle/` (modify in place).
- **iOS** = Swift source of truth under `ios-reference/ClickMe2026/` (port behavior; visuals come from 01-UI-SPEC.md).
Android root: `app/src/main/java/com/uptrendinvestments/clickmeapp/` (abbreviated `$PKG`).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `gradle/libs.versions.toml` (modify) | config | n/a | T same file | exact |
| `build.gradle.kts` (root, modify) | config | n/a | T (plugin aliases) | exact |
| `app/build.gradle.kts` (modify) | config | n/a | T same file + iOS `Environments.plist` | exact |
| `.editorconfig`, `scripts/check-design-rules.sh`, `.github/workflows/ci.yml` | config | batch | RESEARCH Code Examples | no analog |
| `app/src/main/AndroidManifest.xml` (modify) | config | n/a | T same file | exact |
| `res/values/{strings,colors,themes}.xml`, `values-v27/themes.xml` (modify/new) | config | n/a | T same files | exact |
| `res/drawable/splash_logo*.xml`, launcher icon layers (modify) | config | n/a | T `ic_launcher_*` + iOS `SplashLogo.imageset` | role-match |
| `res/font/nunito.ttf` + `OFL.txt` | asset | n/a | none | no analog |
| `$PKG/ClickMeApplication.kt` | config (Hilt app) | n/a | none | no analog |
| `$PKG/MainActivity.kt` (modify) | activity | request-response | T same file | exact |
| `$PKG/ClickMeApp.kt`, `AppRoute.kt`, `AppViewModel.kt` | root switch / state | event-driven | iOS `ClickMe2026App.swift` | exact |
| `$PKG/config/AppConfig.kt`, `AppLinks.kt` | config | n/a | iOS `Configuration/AppEnvironment.swift` | exact |
| `$PKG/ui/theme/Color.kt`, `Theme.kt`, `Type.kt` (rewrite) | theme | n/a | T files + iOS `Theme/Brand.swift`, `BrandFont.swift` | exact |
| `$PKG/ui/theme/{Extras,Shape,Spacing,Motion}.kt` | theme | n/a | iOS `Brand.swift` (overlay whites) + UI-SPEC | partial |
| `$PKG/components/buttonstyles/PressScale.kt` | component | event-driven | iOS `ButtonStyles/PressScaleButtonStyle.swift` | exact |
| `$PKG/components/buttons/AsyncSaveButton.kt` | component | request-response | iOS `Buttons/AsyncSaveButton.swift` | exact |
| `$PKG/components/ValidatedField.kt` | component | transform | iOS `Components/ValidatedField.swift` | exact |
| `$PKG/components/avatar/{Avatar,TopBarAvatarButton}.kt` | component | request-response | iOS `Avatar/TopBarAvatarButton.swift` | exact |
| `$PKG/components/backgrounds/GreenGlowBlobLayer.kt` | component | transform | iOS `Backgrounds/GreenGlowBlobLayer.swift` | exact |
| `$PKG/components/web/CustomTabLauncher.kt` | utility | request-response | iOS `Web/SafariView.swift` | role-match |
| `$PKG/components/{EmptyState,PlaceholderTabContent}.kt` | component | n/a | UI-SPEC only | no analog |
| `$PKG/navigation/{NavigationState,Navigator}.kt` | store/service | event-driven | nav3-recipes `multiplestacks` (external) + iOS `HomeNavigationPath.swift` | role-match |
| `$PKG/navigation/{TabDefinition,HomeTabScaffold}.kt` | component | event-driven | iOS `HomeClientView.swift` / `HomeClientTab.swift` | role-match |
| `$PKG/screens/clientmode/home/HomeClientScreen.kt` + `HomeClientViewModel.kt` | screen + VM | event-driven | iOS `Client Mode/Home/HomeClientView.swift`, `Subviews/HomeClientTab.swift` | exact |
| `$PKG/screens/expertmode/home/HomeExpertScreen.kt` + `HomeExpertViewModel.kt` | screen + VM | event-driven | iOS `Expert Mode/Home/HomeExpertView.swift`, `Subviews/HomeExpertTab.swift` | exact |
| `$PKG/screens/debug/DesignSystemGalleryScreen.kt` | screen | n/a | UI-SPEC only | no analog |
| `app/src/test/.../*Test.kt` (Theme, Components, CustomTabLauncher, AppViewModel, Navigator, HomeTabScaffold, per-flavor config) | test | n/a | T `ExampleUnitTest.kt` (delete) + RESEARCH "JVM test of the navigator" | partial |

## Pattern Assignments

### `gradle/libs.versions.toml` and `app/build.gradle.kts` (config)

**Analog:** the template files themselves. Keep the existing style: `alias(libs.plugins.*)`, `version.ref`, kebab-case library names mapped to dotted accessors.

Existing plugin/version style (`gradle/libs.versions.toml` lines 1-26):
```toml
[versions]
agp = "9.4.1"
kotlin = "2.2.10"          # change to 2.4.21 (D-14)
composeBom = "2026.02.01"  # change to 2026.09.00
[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```
Rename/replace `androidx-lifecycle-runtime-ktx` (2.6.1) with `lifecycle-runtime-compose`; bump coreKtx and activityCompose. Full target catalog: RESEARCH "Version catalog". Add plugins `kotlin-serialization`, `ksp`, `hilt`, `ktlint`. No `kotlin-android`, no kapt.

Existing `app/build.gradle.kts` blocks to KEEP (lines 8-36): `namespace`, `compileSdk { version = release(37) }`, minSdk 26 / targetSdk 37, and:
```kotlin
buildTypes { release { optimization { enable = true
    packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**") } } }
```
ADD: `flavorDimensions += "env"` with `dev`/`prod` flavors using `buildConfigField` (BASE_URL, STRIPE_PUBLISHABLE_KEY, AGORA_APP_ID), `buildFeatures { compose = true; buildConfig = true }`, `testOptions { unitTests.isIncludeAndroidResources = true }`, `lint { abortOnError = true }`. Values come from iOS `Environments.plist`:
```xml
<key>BaseURL</key>  https://clickme-backend-production.up.railway.app/api/v1   (all envs)
<key>AgoraAppId</key> 483eaa4d11fb44b6acc91bf5ceae36c7                         (all envs)
Development StripePublishableKey: pk_test_...   -> dev flavor
Production  StripePublishableKey: pk_live_...   -> prod flavor
```
Copy the key strings from the plist; do not retype. `applicationId` stays shared (D-06). Replace the dependencies block per RESEARCH "Dependencies block" (current block lines 39-52 uses the same `implementation(libs.x)` style).

---

### `AndroidManifest.xml` (config, modify)

**Analog:** template `app/src/main/AndroidManifest.xml` lines 1-30.
Changes:
- `android:theme="@style/Theme.ClickMe_Android"` (application and activity) -> `@style/Theme.ClickMe.Splash` on the activity; application theme `Theme.ClickMe`.
- `android:allowBackup="true"` -> `"false"` (Pitfall 8).
- Drop `android:label` on `<activity>` (lint RedundantLabel); add `android:name=".ClickMeApplication"` on `<application>`.
- Add `tools:ignore="UnusedAttribute"` if `enableOnBackInvokedCallback` is set. Do not set `screenOrientation`. Keep `windowSoftInputMode="adjustResize"` and the single exported launcher activity.

### `res/values/{strings,colors,themes}.xml`, `values-v27/themes.xml`

**Analog:** template `res/values/*.xml` (rename `app_name` from `ClickMe_Android` to `ClickMe`; replace `Theme.ClickMe_Android`; delete purple/teal colors, keep only `surface` = #131313). Use the corrected theme XML in RESEARCH ("Theme XML"). `windowLightNavigationBar` goes only in `values-v27/themes.xml` (Pitfall 3).

### Launcher icon / splash (`mipmap-anydpi/ic_launcher*.xml`, `drawable/ic_launcher_*.xml`, `drawable/splash_logo.xml`)

**Analog:** template adaptive icon XML (`mipmap-anydpi/ic_launcher.xml`, `drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml`) for structure; replace foreground/monochrome with the transparent mark from iOS `Assets.xcassets/SplashLogo.imageset` (D-11 revised). Splash `layer-list` sizing: RESEARCH Pitfall 4. Put PNG in `drawable-nodpi`.

---

### `$PKG/MainActivity.kt` (activity, modify)

**Analog:** template `MainActivity.kt` lines 1-33 (keep `enableEdgeToEdge()` + `setContent { Theme { ... } }` shape; delete `Greeting` and preview).
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ClickMe_AndroidTheme { Scaffold(...) { ... } } }
    }
}
```
Target (RESEARCH Pattern 3): `@AndroidEntryPoint`, `installSplashScreen()` BEFORE `super.onCreate`, `setKeepOnScreenCondition { !appViewModel.destinationResolved.value }`, `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT) x2)`, `setContent { ClickMeTheme { ClickMeApp(appViewModel) } }`. Rename `ClickMe_AndroidTheme` -> `ClickMeTheme` everywhere.

### `ClickMeApp.kt`, `AppRoute.kt`, `AppViewModel.kt` (root switch, event-driven)

**Analog:** iOS `ClickMe2026App.swift` lines 14-85. Route is app state, not a nav destination.
```swift
private enum AppRoute { case splash, login, signupSetup, clientHome, expertHome }
switch route {
case .splash: SplashScreenView { destination in ... }
case .login: LoginView { mode in route = mode == .expert ? .expertHome : .clientHome }
case .signupSetup: LoginView(startAtSignupSetup: true, ...)
case .clientHome: HomeClientView()
case .expertHome: HomeExpertView()
}
```
Kotlin port: `sealed interface`/enum `AppRoute { Login, SignupSetup, ClientHome, ExpertHome }` (splash is the OS splash, not a route); `ClickMeApp` is a plain `when (route)`; Login/SignupSetup are empty placeholders. `AppViewModel` is `@HiltViewModel`, exposes `route: StateFlow<AppRoute>` (stub resolves to `ClientHome` on init) and `destinationResolved: StateFlow<Boolean>`. Later phases port the `.onChange(userManager.isLoggedIn / currentMode)` rules; not Phase 1.

### `$PKG/config/AppConfig.kt`, `AppLinks.kt`

**Analog:** iOS `Configuration/AppEnvironment.swift` (struct with `baseURL`, `stripePublishableKey`, `agoraAppId` read from plist; a separate non-env-switched URL holder at line ~49-53):
```swift
static let privacyPolicy = URL(string: "https://www.uptrendinvestments.net/clickme/privacy.html")!
```
Kotlin: `object AppConfig { val baseUrl = BuildConfig.BASE_URL; ... }` and `object AppLinks { val privacyPolicy = "https://www.uptrendinvestments.net/clickme/privacy.html" }`. Components/previews must not read BuildConfig.

### `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` (theme)

**Analog (structure):** template `ui/theme/Theme.kt` (`private val DarkColorScheme = darkColorScheme(...)`, `@Composable fun ...Theme(...)` wrapping `MaterialTheme`). Delete `LightColorScheme`, `dynamicColor`, `isSystemInDarkTheme` branches and Purple/Pink `Color.kt` entries. Dark only.

**Analog (values):** iOS `Theme/Brand.swift` lines 13-62; single source of truth for hex, which ONLY lives in `Color.kt` (CI rule). Mapping to carry over:
```swift
surface #131313, surfaceContainerLowest #0e0e0e, Low #1c1b1b, surfaceContainer #201f1f,
High #2a2a2a, Highest #353534; onSurface #e5e2e1, onSurfaceVariant #bacbbc;
outline #859587, outlineVariant #3c4a3f; primary #44f697, primaryContainer #00d97e,
onPrimary #00391d, onPrimaryContainer #005930; error #ffb4ab, onError #690005,
destructive rgb(1.0,.3,.3); overlayWhite05/06/10/15/20/22/60/80 = white @ alpha
```
Map to `darkColorScheme(...)` slots per UI-SPEC. `destructive`, `onSurfaceMuted/Faint`, overlay whites have no M3 slot -> `ui/theme/Extras.kt` (`CompositionLocal`). Do NOT create any `*Brand` class (CI forbids).

**Type.kt:** iOS `BrandFont.swift` maps semantic styles to rounded system fonts; Android equivalent is Nunito at 12/16/20/28 sp (UI-SPEC). Use RESEARCH Pitfall 5 `NunitoFamily` with two variable-weight `Font(R.font.nunito, ...)` entries.

---

### `components/buttonstyles/PressScale.kt` (component, event-driven)

**Analog:** iOS `ButtonStyles/PressScaleButtonStyle.swift` lines 8-15:
```swift
configuration.label
    .scaleEffect(configuration.isPressed ? 0.96 : 1.0)
    .animation(.easeInOut(duration: 0.12), value: configuration.isPressed)
```
Kotlin: `Modifier.pressScale(interactionSource)` using `collectIsPressedAsState()` + `animateFloatAsState(if (pressed) 0.96f else 1f, tween(120, easing = FastOutSlowInEasing))` + `graphicsLayer`. Wrap M3 `Button` so ripple/semantics stay (RESEARCH Don't Hand-Roll).

### `components/buttons/AsyncSaveButton.kt` (component, request-response)

**Analog:** iOS `Buttons/AsyncSaveButton.swift` lines 14-55.
```swift
enum SaveState { case idle, saving, saved }
struct AsyncSaveButton: View { let title: String; let state: SaveState
  var savedTitle: String = "Saved!"; var action: () -> Void
  // Capsule primary fill, shadow(primary 0.50, radius 18, y 5), height 56
  // .saving -> spinner tinted onPrimary; .saved -> check.circle.fill + savedTitle (bold)
  // .disabled(state != .idle); PressScaleButtonStyle
```
Kotlin: `enum class SaveState`, `@Composable fun AsyncSaveButton(title, state, modifier, savedTitle = "Saved!", onClick)`; `enabled = state == SaveState.Idle`; `Modifier.shadow(ambientColor/spotColor = primary.copy(0.5f))`; `Modifier.semantics { stateDescription = ... }` (RESEARCH API list). Stateless, no Hilt.

### `components/ValidatedField.kt` (component, transform)

**Analog:** iOS `Components/ValidatedField.swift` lines 13-106. Behavior to port: params `placeholder, text, icon, isSecure, keyboardType, error`; border `destructive` 1.8 when error else `outlineVariant` 1.2; corner 28, height 54, fill `surfaceContainer`; leading icon turns destructive on error; secure fields have visibility toggle with password autofill kept (`textContentType(.password)`; no capitalization/autocorrect on the plain branch); border animates 0.2 s.
```swift
let borderColor = hasError ? Brand.destructive : Brand.outlineVariant
let borderWidth: CGFloat = hasError ? 1.8 : 1.2
RoundedRectangle(cornerRadius: 28).fill(Brand.surfaceContainer).frame(height: 54)
```
Kotlin: `OutlinedTextField` + `OutlinedTextFieldDefaults.Container(...)`; `semantics { error(msg); contentType = ContentType.Password }`; `KeyboardOptions(capitalization = None, autoCorrectEnabled = false)`; visibility state is local `remember`.

### `components/avatar/{Avatar,TopBarAvatarButton}.kt` (component, request-response)

**Analog:** iOS `Avatar/TopBarAvatarButton.swift` lines 15-56. 32 pt circle, remote image when URL non-empty and parseable, otherwise silhouette fallback (also on load failure).
```swift
if let urlString = avatarURL, !urlString.isEmpty, let url = URL(string: urlString) {
    AsyncImage(url: url) { phase in switch phase { case .success(let image): image.resizable().scaledToFill(); default: fallback } }
} else { fallback }
```
Kotlin: Coil 3 `AsyncImage(model = ImageRequest.Builder(ctx).data(url).crossfade(200).build(), contentScale = Crop)` with `error`/`placeholder` painter fallback (`Icons.Filled.AccountCircle`, tint `onSurfaceVariant`); `clip(CircleShape)`; button has `onClick` + contentDescription. Imports are `coil3.*`.

### `components/backgrounds/GreenGlowBlobLayer.kt` (component, transform)

**Analog:** iOS `Backgrounds/GreenGlowBlobLayer.swift` lines 11-38: two radial blobs, opacity 0.18, radius 300, at (width+30, 10% height) and (-20, 65% height), `Brand.primary` -> clear.
Kotlin: `Canvas` + `Brush.radialGradient(listOf(primary.copy(0.18f), Transparent), radius = 150.dp)` at those offsets. No `Modifier.blur` (UI-SPEC anti-pattern).

### `components/web/CustomTabLauncher.kt` (utility)

**Analog:** iOS `Web/SafariView.swift` (in-process hosted page for policy links). Kotlin: `fun openCustomTab(context, url, onFailure)` with scheme allowlist (https/http), `CustomTabsIntent.Builder().setColorScheme(COLOR_SCHEME_DARK)...launchUrl`, fallback `ACTION_VIEW`, `onFailure` when nothing resolves (snippet in RESEARCH "Compose/M3 APIs").

---

### `navigation/{NavigationState,Navigator,TabDefinition,HomeTabScaffold}.kt` (tab shell, event-driven)

**Behavior analog:** iOS `HomeClientView.swift` lines 17-78 and `Views/Extensions/HomeNavigationPath.swift` (one path object per tab, alive for the shell's lifetime, so each tab keeps its trail across switches):
```swift
@StateObject private var explorePath = HomeNavigationPath()  // x5, one per tab
TabView(selection: $selectedTab) { exploreTab.tabItem { Label("Explore", systemImage: "sparkles") }.tag(.explore) ... }
.tint(Brand.primary)
```
Android mechanism is Navigation 3 (D-01/D-15): copy `NavigationState` / `Navigator` from `android/nav3-recipes` `multiplestacks` (keep Apache-2.0 header, run `ktlintFormat`), add re-tap-to-root. Use the `NavigationBarItem` snippet in RESEARCH Pattern 1 and the push/pop transitions in Pattern 2. `TabDefinition(key: NavKey, labelRes, icon, selectedIcon)`. Keys: `@Serializable data object ExploreRoot : NavKey`, etc. `HomeTabScaffold(tabs: List<TabDefinition>, ...)` takes plain parameters (no Hilt) so Robolectric can render it.

**Tab lists (iOS `HomeClientTab.swift`, `HomeExpertTab.swift`):**
```swift
Client: explore, search, bookings, chats, profile   // titles: Explore, Search, Bookings, Chats, Profile
Expert: dashboard, bookings, topics, chats, profile // titles: Dashboard, Bookings, Topics, Chats, Profile
```
Icons map from SF Symbols to Material icons per UI-SPEC (not literal).

### `screens/clientmode/home/HomeClientScreen.kt` + `HomeClientViewModel.kt`; `screens/expertmode/home/HomeExpertScreen.kt` + `HomeExpertViewModel.kt`

**Analog:** iOS `Client Mode/Home/HomeClientView.swift` and `Expert Mode/Home/HomeExpertView.swift`. Each is the shell for its mode; Android wrapper `HomeClientScreen` calls `hiltViewModel()` (`androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`) and passes plain state into `HomeTabScaffold` with 5 `PlaceholderTabContent` entries. Client start tab = Explore, expert start tab = Dashboard. ViewModels live beside screens (CLAUDE.md file organization). Tab-specific push-notification routing (`PushRouter`) and `openChatThread` are NOT Phase 1.

### `screens/debug/DesignSystemGalleryScreen.kt`

No analog; see UI-SPEC "Debug gallery". Gate with `BuildConfig.DEBUG`; entry pushed on the Profile tab stack in both shells.

---

### Tests (`app/src/test/java/com/uptrendinvestments/clickmeapp/...`)

**Analog:** template `ExampleUnitTest.kt` uses `import org.junit.Assert.*` (fails ktlint; delete it and `ExampleInstrumentedTest.kt`). Use JUnit4 + Robolectric (`@Config(sdk=[35])`) + `androidx.compose.ui.test.junit4.v2.createComposeRule`. Navigator test pattern: RESEARCH "JVM test of the navigator". Test list and commands: RESEARCH "Phase Requirements -> Test Map".

## Shared Patterns

### Design tokens only through `MaterialTheme` / `ClickMeExtras`
**Source:** iOS `Theme/Brand.swift` -> `ui/theme/Color.kt`
**Apply to:** every composable. No `Color(0x...)` outside `ui/theme/Color.kt`; no class named `*Brand`; enforced by `scripts/check-design-rules.sh` (RESEARCH "Design rules script").

### Stateless components, Hilt only at screen wrappers
**Source:** RESEARCH Pattern 4 / Pitfall 9
**Apply to:** `components/*`, `HomeTabScaffold`, `PlaceholderTabContent`. Previews/tests must not touch BuildConfig or Hilt.

### Hilt wiring
**Source:** RESEARCH Pattern 3
**Apply to:** `ClickMeApplication` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`), VMs (`@HiltViewModel`, `@Inject constructor`). KSP, not kapt; no `kotlin-android` plugin.

### Lint and style gates
**Source:** RESEARCH Pitfalls 2-3, `.editorconfig`, CI yaml
**Apply to:** all Kotlin and XML. `ktlint_function_naming_ignore_when_annotated_with = Composable`, `max_line_length = 120`; run `./gradlew ktlintFormat` after pasting recipe code; `lint { abortOnError = true }`.

### Platform swaps (CLAUDE.md)
Apply when porting iOS idioms: `.sheet` -> `ModalBottomSheet`, `.alert` -> `AlertDialog`, `.task` -> `LaunchedEffect(Unit)`, nav title -> `TopAppBar`.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `.editorconfig`, `scripts/check-design-rules.sh`, `.github/workflows/ci.yml` | config | batch | Repo has no `.github/` or scripts; use RESEARCH examples |
| `ClickMeApplication.kt` | app class | n/a | No Hilt in template |
| `res/font/nunito.ttf`, `OFL.txt` | asset | n/a | Net-new (variable font, upstream googlefonts/nunito) |
| `components/EmptyState.kt`, `PlaceholderTabContent.kt` | component | n/a | Defined only in UI-SPEC (iOS has no equivalent in `Views/Components`) |
| `NavigationState`/`Navigator` | store | event-driven | External official recipe, not in repo; iOS `HomeNavigationPath` is behavioral reference only |
| `DesignSystemGalleryScreen.kt` | screen | n/a | Android-only debug aid per UI-SPEC |
| Play Console checklist (REL-01/02) | human-action | n/a | No code; RESEARCH "Play Console" section steps 1-9, record in `.planning/STATE.md` |

## Metadata

**Analog search scope:** `app/`, `gradle/`, `ios-reference/ClickMe2026/{Theme,Configuration,Views/Components,Views/Extensions,Views/Screens/*/Home,ClickMe2026App.swift,Environments.plist}`
**Files scanned:** ~35
**Pattern extraction date:** 2026-10-19
