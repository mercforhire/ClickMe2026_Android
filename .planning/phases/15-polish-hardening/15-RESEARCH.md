# Phase 15: Polish & Hardening - Research

**Researched:** 2026-10-29
**Domain:** Android hardening sweep on an existing Compose + Material 3 + Navigation 3 + Hilt app (loading/empty/error states, state restoration, edge-to-edge insets, TalkBack)
**Confidence:** HIGH on codebase findings and tooling limits (read or executed this session); MEDIUM on TalkBack runtime behavior (needs the device checklist)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Orientation and large screens**
- **D-01: Lock phones to portrait app-wide**, including the call screen. This matches iOS (`INFOPLIST_KEY_UISupportedInterfaceOrientations = UIInterfaceOrientationPortrait`). There's one policy for the single Activity, with no per-screen exceptions. Today the manifest declares no orientation. Reversibility: reversible.
- **D-02: Large screens (sw600dp+) must be usable, not optimized.** At targetSdk 37 the system ignores the orientation lock on large displays, so tablets and foldables will rotate and resize. Content must never be clipped or unreachable, input must survive resizing, and content that would stretch badly gets a max width and is centered. No tablet-specific or list-detail layouts. Verify on a resizable emulator.
- **D-03: SC-2's "rotating the device" means any Activity recreation.** With phones locked, verify it with config changes (dark/font-scale change, resize on an emulator or large screen) and with "Don't keep activities" / process death. Record this reading in VERIFICATION.md. Release and debug builds behave the same, with no debug-only rotation.

**Loading, empty and error standard (POL-01)**
- **D-04: A failed first load (nothing to show) uses the shared `ErrorState(onRetry)`** from `components/EmptyState.kt` on every screen, in place of ad-hoc text or an AlertDialog. Today only 2 real screens use it (ClientProfileSettings, ExpertProfile).
- **D-05: A failed refresh with content already showing keeps the content and shows a snackbar** ("Couldn't refresh") with a Retry action. This covers pull-to-refresh, explicit reloads and realtime-triggered refetches. **Quiet** tab-return reloads fail silently, with no snackbar.
- **D-06: No dedicated offline UI.** No ConnectivityManager monitoring and no offline banner. Network failures go through the D-04 and D-05 paths. The planner may distinguish "no connection" (IOException) from a server error in the copy, but this is optional.
- **D-07: Audit scope = every `*Screen.kt` destination plus every bottom sheet that fetches data** (topic picker, time slots, and similar). For static forms and dialogs, check only their in-flight state and their submit-error state.
- **D-08: Keep the existing loading idioms.** Skeletons are already widespread (~56 files) and pull-to-refresh is on 14 screens. Fill gaps with the same skeleton pattern. Don't rework screens that already comply.

**Process death and recreation (POL-02)**
- **D-09: Signup restores the current step plus the unsaved field text** (name, city, etc.) through `SavedStateHandle`. It also re-hydrates the app-scoped `SignupDraft` (an in-memory `@Singleton` in `SignupSetupRepository`) from the server. **Passwords, OTP codes and other secrets are never written to the saved-state Bundle.** After process death those fields come back empty. Reversibility: reversible.
- **D-10: The chat composer's unsent text survives per thread through `SavedStateHandle`** (`ChatThreadUiState.composerText` is ViewModel-only today). It survives recreation and process death while the thread is in the back stack. It is not persisted across app restarts. Failed bubbles still don't persist (Phase 12 D-02).
- **D-11: If the process is killed during a live call,** relaunch lands on the booking/session detail screen with a Join/Rejoin action. **Never auto-rejoin, and never open the mic without a user tap.** Re-fetch the microphone permission and the Agora token on rejoin. The foreground `CallService` makes this rare.
- **D-12: Sheets and dialogs that hold user input reopen with that input** after recreation and process death (examples: report details, decline reason, the time range sheet). Confirm-only dialogs may simply close. Scroll position is best-effort: kept where the data loads synchronously, and not guaranteed otherwise.
- **D-13: Booking already complies** (Phase 6: `SavedStateHandle` with assisted injection, device-verified). Use it as the reference pattern. The 3DS bank step returning an idle form after process death is still accepted (Phase 7 D-17).

**Verification**
- **D-14: An automated gate plus one consolidated, non-blocking device checklist.** The automated gate:
  - Robolectric/Compose tests for loading, empty and error states.
  - `StateRestorationTester` and `SavedStateHandle` tests for the core flows.
  - Semantics assertions (content descriptions, roles, state descriptions, traversal order) for the 4 SC-4 TalkBack flows.
  - Compose accessibility checks enabled in the UI tests.

  The device checklist is a human item. Following the standing preference, it is logged as open and does not block phase close.
- **D-15: The device checklist absorbs every deferred visual, accessibility and system item from Phases 3–14 in STATE.md** (200% font scale, TalkBack, rotation/process death, IME and keyboard checks). It is deduplicated and ordered by flow, and the originals are marked superseded by Phase 15. **Stripe-card items** (blocked on the test-mode backend) and **live-call items** (need an iOS peer) stay with Phase 16.
- **D-16: Priority if the work exceeds Week 7:**
  1. The 4 SC-4 core flows get the full treatment first: login, booking (free and paid), call, chat.
  2. Then the remaining client screens.
  3. Then the expert screens.
  4. Then settings.

  Leftovers go on a logged gap list handed to Phase 16.
- **D-17: Automated insets guard (POL-03).** Extend `scripts/check-design-rules.sh` to flag screens and Scaffolds that are missing insets handling. Add a few Robolectric tests with IME and cutout insets on the chat composer and the form screens. `enableEdgeToEdge` (in `MainActivity.kt`) and `adjustResize` are already set.

### Claude's Discretion
- How to apply the orientation lock (manifest `screenOrientation` versus a runtime setting), as long as D-01 and D-02 hold.
- The exact snackbar and ErrorState copy, and whether to tell IOException apart from server errors (D-06).
- Which ViewModels without `SavedStateHandle` (34 of 58) actually need one. Read-only screens that just reload are fine without it.
- The structure of the device checklist file and its plan (it follows the Phase 13 D-21 "one non-blocking checklist plan" pattern).

### Deferred Ideas (OUT OF SCOPE)
- Persisting chat drafts to disk across app restarts (beyond POL-02). Possible post-MVP polish.
- Proper adaptive or tablet layouts (list-detail panes). Post-MVP.
- An app-wide offline banner driven by a NetworkCallback. Not required by iOS parity or by any requirement.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| POL-01 | Every MVP screen has loading, empty and error states. | Screen/state coverage matrix below; 12 existing refresh-failed snackbars lack the D-05 Retry action; `ErrorState` extension and a shared snackbar helper; compliance-test pattern (Architecture Patterns 1-2). |
| POL-02 | Core flows survive configuration changes and process death without losing user input or position. | Most of this is already built (24 of 58 ViewModels use `SavedStateHandle`; signup steps, chat composer and input sheets already restore). Real gaps: 2 OTP mirrors that violate D-09, 4 unmirrored forms, D-11 verification, R8/Nav3 risk (Gap inventory, Pitfalls 2, 8). |
| POL-03 | The app draws edge-to-edge with correct insets for system bars, display cutouts and the IME. | `enableEdgeToEdge` + `adjustResize` already set; IME handling is widespread; cutout handling (R5) and the portrait lock are new; IME-inset Robolectric test technique proven this session (Code Examples 3). |
| POL-04 | Core flows pass a TalkBack check. | Compose accessibility checks are a no-op under Robolectric (verified); replace with a hand-written semantics audit helper plus a device checklist; concrete gaps found in Login and `ValidatedField` (Gap inventory, Pitfall 1, 9). |
</phase_requirements>

## Summary

Phase 15 is an audit, gap-fill, test and guard phase, not a build phase. A read of the code shows the earlier phases already delivered most of what the four requirements ask for: 53 `*Screen.kt` destinations, 30 files using `SkeletonGroup`, 21 `SnackbarHost`s, 24 of 58 ViewModels on `SavedStateHandle` (all signup steps with text input, the chat composer, cancel steps, write review, report sheets, availability, topic editor), 318 `contentDescription`s, 84 `stateDescription`s, widespread `imePadding()` and `navigationBarsPadding()`. Several CONTEXT statements are stale: the chat composer text is already mirrored (`ChatThreadViewModel.KEY_COMPOSER`), signup hydration after process death is already triggered by `SignupSetupHostViewModel.ensureHydrated()`, and the Decline and Accept sheets already use `rememberSaveable`. The planner should therefore size this phase as roughly 20 specific code changes plus a large test and verification layer, and should not budget for re-building what exists.

Three findings change the plan. First, D-14's "Compose accessibility checks enabled in the UI tests" cannot work in this project's JVM test setup: the library detects Robolectric and returns without enabling anything (decompiled `ui-test-junit4-accessibility-android-1.12.1`: it logs "Accessibility checks are currently not supported by Robolectric"), and there is no `androidTest` source set. A hand-written semantics audit helper replaces it. Second, D-09 is violated by existing code: the Verify Email code (`"verify_code"`) and the Set New Password reset code (`"reset_code"`) are mirrored into `SavedStateHandle` today, so removing them is real work plus test updates. Third, the Login flow has verifiable TalkBack gaps: the wordmark is not a heading, the Log in button announces "Saving" instead of a busy state, and `ValidatedField` loses its accessible name once the field has a value (verified in the merged semantics tree).

The remaining new work is concentrated in a few shared pieces: the portrait lock (manifest, one attribute), a `Modifier.contentMaxWidth()`, the `ErrorState` parameters, a shared refresh-failed snackbar helper applied to 12 screens, cutout handling at two roots, the insets rule in `scripts/check-design-rules.sh` (it would flag 5 Scaffold files and 1 AlertDialog-with-TextField today), SavedStateHandle mirrors for four settings/tax/delete forms, and the device checklist that absorbs STATE.md lines 161-304.

**Primary recommendation:** Build the shared pieces first (Wave 0), then run the D-16 priority order as parallel per-flow plans that each combine code gaps and tests; replace Compose ATF checks with a hand-written semantics audit helper plus the device checklist; remove the two OTP mirrors; keep compliant screens untouched (D-08).

## Project Constraints (from CLAUDE.md)

- Jetpack Compose + Material 3; MVVM with Compose State; Kotlin coroutines; Hilt; Retrofit + Moshi; EncryptedSharedPreferences for JWT; Firebase Messaging; Agora; Stripe PaymentSheet. minSdk 26, targetSdk 37, compileSdk 37.
- iOS code under `ios-reference/` is the source of truth for behavior; deviate only for Android conventions.
- Platform swaps: `.sheet` to `ModalBottomSheet`, back chevron to `TopAppBar` back arrow, `.alert` to Material `AlertDialog`, switch to Material `Switch`, `.task` to `LaunchedEffect(Unit)`.
- File organization mirrors the iOS tree (`screens/<area>/<screen>/XScreen.kt` + `XViewModel.kt`).
- Same REST contract as iOS; `backend-reference/docs/ios/` are the authoritative specs. This phase touches no endpoint.
- Standing user preferences (MEMORY.md): manual testing is deferred and logged as open, never a blocking checkpoint; dark theme only; Material styling OK; JDK 25 via the Android Studio JBR; never run `gradlew --stop`.
- Design gates that must stay green (UI-SPEC "Existing guards"): no `Color(0x` outside `ui/theme/Color.kt`, no `fontSize` outside the theme, no hard-coded `Text("...")`.

## Architectural Responsibility Map

This is a single-tier Android client phase. The tiers below are the app's internal layers, which is what the planner needs for task placement.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Loading / empty / error rendering | Composable (screen/component) | ViewModel (`*Load` sealed state, `refreshFailed` flag) | State is derived in the ViewModel; the screen only maps it to skeleton, `EmptyState`, `ErrorState` |
| Failed-refresh snackbar + Retry | Composable (`SnackbarHostState` effect) | ViewModel (one-shot `refreshFailed` flag + `consume*`) | Existing pattern: flag in UiState, effect shows and consumes it |
| State restoration (inputs) | ViewModel (`SavedStateHandle`) | Composable (`rememberSaveable` for sheet open flags and sheet-local text) | Booking is the reference pattern (D-13); `rememberSaveable` already covers sheets |
| Back stack and position restoration | Navigation (`rememberNavBackStack`, serializable `NavKey`s) | Entry decorators (saveable state holder, ViewModel store) | Already wired in `navigation/NavigationState.kt` |
| Orientation lock | Android manifest (`MainActivity`) | None | One Activity, one attribute (D-01) |
| Large-window max width | Composable layout modifier | None | A single modifier applied at screen/host level |
| Insets (bars, IME, cutout) | Composable modifiers | `MainActivity.enableEdgeToEdge` (already) | Compose must apply IME inset itself on API 30+ with edge-to-edge |
| Insets guard | Build tooling (`scripts/check-design-rules.sh`) | Robolectric tests | Static rule plus dynamic bounds tests |
| TalkBack semantics | Composable semantics modifiers | Test helper (semantics audit) | Only the UI tier can set names, roles, states, traversal |
| Call resilience (process kill) | Navigation back stack restore + `CallCenter` (memory) | `CallService` | No persisted call marker exists; restore lands on the underlying screen with Join Call (see D-11 analysis) |

## Standard Stack

No new runtime dependency is needed. Everything below is already in `gradle/libs.versions.toml` and `app/build.gradle.kts`.

### Core (existing, versions read this session)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose BOM | `2026.09.00` | Resolves `ui` 1.12.1, `material3` 1.4.0 | [VERIFIED: `gradle/libs.versions.toml` line `composeBom = "2026.09.00"`; BOM POM at dl.google.com lists `material3` 1.4.0, `ui` 1.12.1, `ui-test-junit4-accessibility` 1.12.1] |
| `androidx.navigation3` runtime/ui | `1.2.0` | Back stack, `rememberNavBackStack` (saved through serialization) | [VERIFIED: libs.versions.toml `navigation3 = "1.2.0"`] |
| `androidx.lifecycle` | `2.11.0` | `SavedStateHandle`, `lifecycle-viewmodel-navigation3` | [VERIFIED: libs.versions.toml `lifecycle = "2.11.0"`] |
| Hilt | `2.60.1` | `@HiltViewModel`, assisted injection (booking and chat pattern) | [VERIFIED: libs.versions.toml `hilt = "2.60.1"`] |
| `androidx.activity` | `1.13.0` | `enableEdgeToEdge` | [VERIFIED: libs.versions.toml; bytecode of `EdgeToEdgeApi28.adjustLayoutInDisplayCutoutMode` does `iconst_1; putfield layoutInDisplayCutoutMode`, i.e. `SHORT_EDGES`] |

### Supporting (test)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Robolectric | `4.17` (`sdk=35` in `app/src/test/resources/robolectric.properties`) | JVM Compose tests | All state, restoration, insets and semantics tests |
| `ui-test-junit4` | via BOM (1.12.1) | `createComposeRule` (the `v2` rule is the repo convention), `StateRestorationTester` | State and restoration tests; see `EntrySavedStateHandleTest.kt` |
| JUnit 4.13.2, Mockito 5.24.0, coroutines-test 1.11.0 | as in `libs.versions.toml` | ViewModel `SavedStateHandle` round-trip tests | Process-death emulation without Robolectric |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Hand-written semantics audit helper | `androidx.compose.ui:ui-test-junit4-accessibility` (`enableAccessibilityChecks()`) | **Rejected.** No-op under Robolectric (verified). Would need an `androidTest` source set, an emulator in CI and instrumented-test wiring that the project does not have. Revisit only if the user wants instrumented tests. |
| Per-screen `contentMaxWidth()` (UI-SPEC) | Clamp at the navigation-host level | Host clamp is far fewer edits (about 4 hosts versus about 50 screens) but narrows app bars with the content, which UI-SPEC says must stay full width. Decision needed (Open Question 3). |

**Installation:** none. No `libs.versions.toml` or `app/build.gradle.kts` change is required.

**Version verification:** BOM-resolved versions were read from the BOM POM (`https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom`) and from the local Gradle cache (`material3-android/1.4.0`, `ui-android/1.12.1`). `ui-test-junit4-accessibility` latest on Google Maven is `1.13.0-beta02`; the BOM pins 1.12.1.

## Package Legitimacy Audit

No external package is installed by this phase. The one candidate considered, `androidx.compose.ui:ui-test-junit4-accessibility`, is a first-party AndroidX artifact managed by the Compose BOM, and is **not recommended** because it cannot run in this project's JVM test setup (see Pitfall 1). The `gsd-tools package-legitimacy` seam covers npm/PyPI/crates only and does not apply to Maven coordinates.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `androidx.compose.ui:ui-test-junit4-accessibility` | Google Maven | since 1.8.0 | n/a | android.googlesource.com/platform/frameworks/support | not run (Maven) | Not adopted (Robolectric no-op) |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
                        Process death / "Don't keep activities" / resize
                                        |
                                        v
 MainActivity.onCreate(savedInstanceState)  --(enableEdgeToEdge, portrait lock)-->  setContent
        |                                                                         |
        |  AppViewModel.route (memory only) --> Splash --> resolves from tokens   v
        |                                                                  ClickMeApp
        |                                          CallOverlayHost  (call state = CallCenter singleton, memory)
        |                                                 |
        |             +-----------------+-----------------+------------------+
        |             v                 v                 v                  v
        |        AuthNavHost     SignupSetupNavHost   HomeClientScreen   HomeExpertScreen
        |        (rememberNavBackStack: serializable NavKeys restore the stack)
        |             |                 |                 |
        |             v                 v                 v
        |         NavDisplay entry --> entry decorators (saveable holder + ViewModelStore)
        |                                   |
        |            +----------------------+-----------------------+
        |            v                                              v
        |   Hilt ViewModel (SavedStateHandle: inputs, step, text)   Composable (rememberSaveable: sheet open + sheet text)
        |            |                                              |
        |            v                                              v
        |   UiState: *Load { Loading | Failed(msg) | Loaded }   Scaffold(innerPadding, snackbarHost)
        |            |                                              |
        |            +--> first load fail  --> ErrorState(onRetry)  |
        |            +--> empty            --> EmptyState           |
        |            +--> refresh fail (content showing, Loud/Pull)--> flag --> snackbar + Retry action
        |            +--> quiet reload fail --> silent
        v
   Inputs that NEVER enter SavedStateHandle: passwords, OTP codes, TIN, deletion password
```

### Recommended Project Structure (additions only)
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── components/
│   ├── EmptyState.kt              # ErrorState gains heading/body/retryLabel; EmptyState heading semantics
│   └── RefreshFailedSnackbar.kt   # NEW: SnackbarHostState.showRefreshFailed(...) helper (one definition)
├── ui/theme/
│   └── ContentWidth.kt            # NEW: ContentMaxWidth = 600.dp + Modifier.contentMaxWidth()
app/src/main/AndroidManifest.xml   # screenOrientation="portrait" on MainActivity only
scripts/check-design-rules.sh      # insets rule (a)(b)(c) + AlertDialog/TextField rule (R7)
app/src/test/java/.../testing/
├── A11yAudit.kt                   # NEW: semantics audit helper (labels, roles, 48dp, traversal order)
└── InsetsTestSupport.kt           # NEW: dispatchIme(...) / dispatchCutout(...) helpers
.planning/phases/15-polish-hardening/15-DEVICE-CHECKLIST.md   # NEW: the one non-blocking checklist
```

### Pattern 1: State compliance test per screen (POL-01)
**What:** One Robolectric test per audited screen asserting the four cells the contract defines: the "Loading" node, the empty heading, the error heading plus a "Retry" action that re-invokes the load, and (where a refresh path exists) the refresh-failed snackbar with its action. The repo already tests this per screen (for example `ExpertDashboardScreenTest.hardFailureShowsTheErrorStateAndRetryRecovers`, `ControlsTest` for `ErrorState`), so the audit is "which cells have no test" rather than "which screens are broken".
**When to use:** Every archetype A to F screen. Compliant screens get a test only; non-compliant screens get a fix and a test (D-08).

### Pattern 2: One-shot `refreshFailed` flag plus shared snackbar helper (D-05)
**What:** The existing screens already follow the flag-and-consume shape (`ConversationsViewModel.consumeRefreshFailure`, `state.refreshFailed`). Only the action is missing on 12 of them. `SignupOverviewScreen` is the one screen that already does the full pattern and is the template.
**Where the gap is (verified by reading the call sites):** `showSnackbar(refreshFailedMessage, ...)` with no `actionLabel` in HelpScreen:121, ConversationsScreen:155, ChatThreadScreen:194, ExploreScreen:136, MyBookingsScreen:187, IncomingRequestsScreen:130, ExpertDashboardScreen:187, AvailabilityScreen:169, ExpertBookingsScreen:171, TransactionsScreen:122, PayoutsScreen:150, NotificationsScreen:144. TopicsScreen uses a `TopicsSnackbar.RefreshFailed` enum value instead. **Template (existing code, `SignupOverviewScreen.kt:131-141`):**
```kotlin
LaunchedEffect(state.refreshFailed) {
    if (state.refreshFailed) {
        val result =
            snackbarHostState.showSnackbar(
                message = failedMessage,
                actionLabel = retryLabel,
                duration = SnackbarDuration.Indefinite,
            )
        if (result == SnackbarResult.ActionPerformed) onRefresh()
        onRefreshErrorShown()
    }
}
```
Extract this once into `components/RefreshFailedSnackbar.kt` (UI-SPEC rules: `SnackbarDuration.Long`, dismiss the current snackbar first, at most one refresh-failed snackbar at a time) and replace the 12 call sites mechanically. Note that in the existing screens `showSnackbar` is launched on a separate `scope.launch` so that consuming the flag does not cancel it; keep that ordering (Pitfall 5).

### Pattern 3: `SavedStateHandle` mirror (D-09, D-13)
**What:** Write the value on every edit (`savedStateHandle[KEY] = value`), read it when building the initial `UiState`, and let restored values win over server-hydrated ones via a `touched` flag. Reference: `SignupBasicInfoViewModel` (`KEY_TOUCHED`, `KEY_CITY_EDITED`) and `MakeABookingViewModel`. Test technique that needs no Robolectric: build the ViewModel with `SavedStateHandle()`, edit, then construct a second ViewModel with `SavedStateHandle(mapOf(...copied values...))` and assert the state (this emulates process death, because a new process gets a new ViewModel and a restored handle).
**Types:** only `Bundle`-able values. Use `ArrayList<String>` for lists (the repo already does: `KEY_SELECTED_IDS`).

### Pattern 4: Insets (POL-03)
Apply UI-SPEC rules R1-R9 as written. The repo's established idiom is `Scaffold` default insets, `padding(innerPadding).consumeWindowInsets(innerPadding)` on the content, then a single `imePadding()` (examples: `AccountSecurityScreen.kt:85-86`, `SignupBasicInfoScreen.kt:160-161`). Pinned footers use `navigationBarsPadding()` then `imePadding()` (`WriteReviewFooter.kt:62-63`).

### Pattern 5: Process death during a call (D-11), what actually happens today
**Finding:** no persisted in-call marker exists; `CallService` returns `START_NOT_STICKY` and the call lives in the `CallCenter` singleton (memory). On process kill the call is gone. On relaunch the Activity restores its saved state, and Navigation 3 restores the back stack, so the user lands on the screen that was underneath the call layer (usually Upcoming Booking Details or a Summary screen), where Join Call is a normal user-tap action; nothing auto-joins. D-11's "lands on the booking/session detail screen" is therefore satisfied by the restored back stack where the user started the call from such a screen, and by the Explore/Home root otherwise.
**Recommendation:** do not add a persisted marker (UI-SPEC Assumption A4 allows the plain screen). Add a test asserting no join happens on composition and a device-checklist row. If the user wants guaranteed landing on the booking detail, that needs a persisted booking id (DataStore) and a router; flag as Open Question 4.

### Recommended plan decomposition (for the planner)
1. **Wave 0, shared pieces (blocks everything):** `ErrorState` params + strings; `showRefreshFailed` helper; `Modifier.contentMaxWidth()` + a bounds test at `w900dp`; manifest portrait lock; `A11yAudit` and `InsetsTestSupport` test helpers; `check-design-rules.sh` insets rule with its opt-out comments.
2. **Wave 1, SC-4 flows (D-16 tier 1), one plan each, parallel:** Login and recovery (OTP mirror removal, Login semantics, `ValidatedField` name, busy state); Booking (Expert Profile -> Make a Booking -> Request Sent: calendar/slot semantics already exist, verify and fill; time-slots card and topic picker sheet errors; zero-inset Scaffold snackbar offset); Call (traversal, pill-first, status live region, large-window short-height reflow, D-11 test); Chat (composer semantics, restoration test, IME test, bubble live regions).
3. **Wave 2, remaining tiers, parallel:** remaining client screens; expert screens; settings (SavedStateHandle for ClientProfileSettings, ExpertProfileSettings, ExpertTaxProfile, DeleteAccount; convert the R7 AlertDialog).
4. **Wave 3:** device checklist file, STATE.md "SUPERSEDED by Phase 15 checklist" marks, gap list for Phase 16, VERIFICATION.md note on the D-03 reading.
Shared `strings.xml` is a merge-conflict hotspot when plans run in parallel: put new per-screen strings in each screen's own `strings_*.xml` and add the shared ones in Wave 0 only.

### Anti-Patterns to Avoid
- **Mirroring a secret into `SavedStateHandle`.** Passwords, OTP codes, TIN, deletion password stay in memory (D-09). Two violations exist today (Pitfall 2).
- **Calling `ErrorState` on a compliant screen just to satisfy D-04.** D-08 and UI-SPEC rule 4 say compliant screens are not rewritten.
- **A second `imePadding()` below a Scaffold that already consumed insets.** Doubles the gap (Phase 13-13 fixed one).
- **Declaring `android:configChanges` or `screenOrientation` on `ConnectReturnActivity`.** It is translucent; an orientation attribute throws on it, and `configChanges` would defeat the recreation tests.
- **Claiming TalkBack coverage from a unit test.** Semantics assertions prove what is exposed, not what TalkBack reads (Pitfall 9).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Error state UI | New per-screen error composables | `ErrorState` / `EmptyState` / `SectionError` in `components/` | Already the standard; consistent copy and semantics |
| Loading UI | Spinners or a shimmer library | `SkeletonGroup` / `SkeletonBlock` | One idiom (D-08); exposes one "Loading" node |
| Retry snackbar | Per-screen `showSnackbar` plus ad-hoc action handling | One `SnackbarHostState.showRefreshFailed` helper | 12 call sites must behave identically (dismiss-previous, Long duration, action runs the Loud refresh) |
| Error message mapping | A second classifier | `ApiException.toSectionMessage()` in `screens/clientmode/makeabooking/BookingErrors.kt:67` | Already maps `Http` to server message else unknown, `Network` to `login_error_network`, `Decoding` to unknown [VERIFIED: file read] |
| Back stack persistence | Custom Bundle save of navigation | `rememberNavBackStack` + `@Serializable NavKey` | Already wired; keys carry no PII beyond email (T-04-49) |
| Insets | Manual `WindowInsets` arithmetic in `Modifier.padding` | `Scaffold` insets, `consumeWindowInsets`, `imePadding`, `navigationBarsPadding`, `windowInsetsPadding` | Standard; avoids double counting |
| Contrast checks | Eyeballing | A pure unit test over `ClickMePalette` token pairs with the WCAG formula | The compose ATF contrast check cannot run on the JVM; token pairs are finite and static |
| Instrumented a11y suite | A new `androidTest` source set | Hand-written `A11yAudit` helper + device checklist + Accessibility Scanner on the Samsung | No emulator in the loop, standing preference defers device checks |

**Key insight:** every cell this phase needs already has a shipped example somewhere in the repo; the work is to find the screens that diverge and to prove, with tests, that the rest do not.

## Runtime State Inventory

Not a rename or migration phase. Omitted. (The one persisted-data change is removing the `"verify_code"` and `"reset_code"` keys from `SavedStateHandle`; saved-state Bundles do not outlive an app update, so no data migration is needed.)

## Gap Inventory (verified by reading code this session)

### POL-01
| Item | Evidence |
|------|----------|
| 12 refresh-failed snackbars have no Retry action | `showSnackbar(` call sites listed in Pattern 2 |
| `ErrorState` has no heading/body/retryLabel parameters; its heading is not a TalkBack heading | `components/EmptyState.kt` lines 78-90: `fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier)` with fixed `R.string.error_state_heading/body/retry` |
| Strings missing | `grep` of `res/values/*.xml`: `error_state_heading` "Something went wrong", `error_state_body` "Check your connection and try again.", `error_state_retry` "Retry connection", `section_retry` "Retry" exist; `common_refresh_failed`, `common_load_error_body_server`, `a11y_in_progress` do not exist |
| First-load error UIs that are ad-hoc but structurally compliant (leave alone unless a test shows a gap) | `ConversationsError`, `ChatThreadError`, `MyBookingsError`, `ExpertBookingsError`, `FailedBody` (Cancel screens, Request Decision, Payment Methods, Request Sent), `TopicsErrorContent`, `HelpFailed`, `NotificationsFailed`, inline `Column` in BookingSummary, UpcomingBooking, ExpertBookingSummary, Availability, TaxProfile, ExpertProfileSettings |
| Data-loading sheets use `SectionError` | `MakeABookingTimeSlotsCard.kt`, `MakeABookingTopicPicker.kt` use `SectionError`; UI-SPEC archetype E asks for `ErrorState` inside the sheet. Decide whether that is "non-compliant" (Open Question 5) |
| Screens with no state code per the keyword scan (confirm static form vs data load by reading) | `SignupReviewScreen`, `ReportAndBlockScreen`, `AccountSecurityScreen`, `DeleteAccountScreen`, `LoginScreen`, `ForgotPasswordScreen`, `SetNewPasswordScreen`, `PasswordChangedScreen`, `OnboardingScreen`, `ModeSwitchScreen`, `SignupInitialScreen`, `SignupTimezoneScreen`, `HomeClientScreen`/`HomeExpertScreen` (tab shells) |

### POL-02
| Item | Evidence |
|------|----------|
| OTP mirrored into saved state (violates D-09) | `SignupVerifyEmailViewModel.kt:75` `Local(code = savedStateHandle.get<String>(KEY_CODE).orEmpty())`, `:169` `savedStateHandle[KEY_CODE] = code`, `:289` `private const val KEY_CODE = "verify_code"`. `SetNewPasswordViewModel.kt:58`, `:74` `savedStateHandle[KEY_CODE] = code`, `:207` `const val KEY_CODE = "reset_code"`. Their own KDoc says "Only the code is mirrored", an earlier decision (T-03-20) that D-09 now overrides |
| Already restoring (do not rebuild; add tests only) | Signup: Initial (email only, `EMAIL_KEY`), BasicInfo, Timezone, Tags, VerifyEmail (cooldown), PhotoCrop. Chat: composer (`ChatThreadViewModel.KEY_COMPOSER = "chat_thread_composer"`, read at `:89`), Conversations query. Cancel steps, Write Review, Report sheets (`ReportAndBlockViewModel`, `ProfileSafetyViewModel`), Explore selection, Categories sheet, My Bookings/Expert Bookings tab, Availability schedule, Topic Editor, Expertise editor, Login (email only), Forgot (email). Sheets: `RequestDecisionScreen.kt:138-142` (`acceptOpen`, `acceptText`, `declineOpen`, `declineReasonName`, `declineNote`), `RescheduleResponseCard.kt:101-103` (`showAccept`, `showDecline`, `declineText`), `TimeRangePickerSheet.kt:98-99`, `AvailabilityScreen.kt:162-163`, `MakeABookingScreen.kt:159-160` |
| Signup hydration after process death already exists | `SignupSetupNavHost.kt`: `LaunchedEffect(Unit) { hostViewModel.ensureHydrated() }`; `SignupSetupHostViewModel` KDoc cites D-10 and Pitfall 5 |
| Forms with user input and no `SavedStateHandle` | `ClientProfileSettingsViewModel` (`onFieldChange`, country/language selections), `ExpertProfileSettingsViewModel` (`onFieldChange`, `onMeetingUrlChange`, languages, country), `ExpertTaxProfileViewModel` (`onFieldChange`, `onDobPicked`; TIN is a secret), `DeleteAccountViewModel` (`selectReason`, `onFeedbackChange`; password is a secret). `AccountSecurityViewModel` holds only passwords, so it needs nothing |
| Not user-input VMs (no change needed) | Notifications, Help, Payment Methods, Payouts, Transactions, Dashboard, Topics, Incoming Requests, Home*, Profile Home*, Booking Summary, Upcoming Booking, Request Sent, Booking Confirmed, Expert Profile, Request Decision (sheet state is `rememberSaveable`) |
| Call | No persisted marker; see Pattern 5 |

### POL-03
| Item | Evidence |
|------|----------|
| No orientation attribute | `AndroidManifest.xml` MainActivity block: `android:launchMode="singleTop"`, `android:theme="@style/Theme.ClickMe.Splash"`, `android:windowSoftInputMode="adjustResize"`, no `screenOrientation` |
| Cutout handling absent | `grep displayCutout` over `app/src/main` returns nothing; only `WindowInsets.safeDrawing.only(Horizontal)` in `MeetingCallScreen.kt:84`. `enableEdgeToEdge` sets `SHORT_EDGES` (bytecode, above), so content can extend into a side cutout on a resized or large window |
| Insets guard hits today (rules (a)(b)(c) of UI-SPEC run against `app/src/main`) | (a) flags 5 files: `SignupTimezoneScreen.kt`, `HomeClientScreen.kt`, `ClientProfileHomeScreen.kt`, `HomeExpertScreen.kt`, `ExpertProfileHomeScreen.kt`. The first has its own `Scaffold(` with `topBar` and no `innerPadding` token (read it and decide); the other four delegate to `HomeTabScaffold`, which already uses `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)` (`HomeTabScaffold.kt:98`), so they need an `// insets-ok:` comment. (b) flags 0 files (all four zero-inset Scaffolds also use `navigationBarsPadding`/`imePadding`/`windowInsetsBottomHeight`). (c) flags 0 files |
| R7 violation (text field inside an `AlertDialog`) | `components/RescheduleResponseCard.kt` `DeclineDialog` (`AlertDialog(` at line 445 containing `OutlinedTextField(` at line 465). Used by Booking Summary, Upcoming Booking and Expert Booking Summary. Convert to a `ModalBottomSheet` with `imePadding()` like `DeclineRequestSheet`, keeping `rememberSaveable` state |
| Snackbar under the gesture bar on zero-inset Scaffolds | `AvailabilityScreen.kt:178` and `ExpertTaxProfileScreen.kt:175` set `contentWindowInsets = WindowInsets(0, 0, 0, 0)` and also show snackbars (`:169`, `:164`). M3 positions the snackbar from the bottom by `contentWindowInsets` when there is no bottom bar [ASSUMED], so the host may sit under the nav bar. Add a Robolectric bounds test; fix by padding the `SnackbarHost` with `navigationBarsPadding()` |
| IME handling | Present on all form screens listed by `grep imePadding` (25 sites). Tests do not exist; none of the 345 test files mention `WindowInsets` |

### POL-04 (the four flows, against UI-SPEC)
| Flow | Gap found |
|------|-----------|
| Log in | `LoginHeader` wordmark `Text` has no `heading()` (`LoginScreen.kt`, `LoginHeader`). "Don't have an account?" and "Sign Up" are two nodes (`Row` of `Text` + `AuthLink`), UI-SPEC wants one node, one action. The Log in button uses `AsyncSaveButton(state = SaveState.Saving)` whose busy description is `a11y_state_saving` = "Saving" (`AsyncSaveButton.kt:60`, `strings.xml:23`), wrong word for a login; add a `busyDescription` parameter defaulting to the current string and pass "In progress" from Login. `ValidatedField` loses its name once filled (next row) |
| `ValidatedField` (used by every text input) | Merged-tree dump this session: empty field has `Text = '[Email]'`; filled field has only `EditableText = 'abc@x.com'` and no `Text`, no `ContentDescription`. So the name exists only while the field is empty. The error is exposed as `Error = 'Bad'` on the field and as a separate `Text` node (not merged). Fix and device-check (Pitfall 9) |
| Book a session | Calendar day cells and slots already set `contentDescription` + `stateDescription` + `Role.RadioButton` (`MakeABookingCalendarCard.kt:230-234`), month title is a polite live region (`:160`). Verify the rest of the UI-SPEC list by test: "Book Session with {name}", slot cell strings, Book Now disabled reason, Request Sent initial focus |
| Join a call | `traversalIndex` is used 0 times in `app/src/main`; `isTraversalGroup` appears only in `MeetingCallScreen.kt`. The pill-first requirement (A8) needs a new `traversalIndex = -1f`. Entering the layer does not move TalkBack focus (UI-SPEC wants focus on the status line); the underlying content is hidden with `clearAndSetSemantics { }` while expanded (`CallOverlayHost.kt:160`), which is good |
| Send a chat message | Composer has the live-region cap message (`ChatComposer.kt:132-134`) and a Send `contentDescription`; check "Message" name, Send disabled `stateDescription` "Type a message to send", focus retention after send, bubble live regions |
| Global | 0 uses of `toggleableState`, 0 explicit `minimumInteractiveComponentSize` (M3 components apply it internally; custom `clickable` boxes do not); 4 uses of `error(` |

## Common Pitfalls

### Pitfall 1: Compose accessibility checks silently do nothing on the JVM
**What goes wrong:** D-14 assumes `enableAccessibilityChecks()` will fail tests on missing labels, small targets and contrast. In this project it does not.
**Why it happens:** [VERIFIED: decompiled `ui-test-junit4-accessibility-android-1.12.1`, `javap -c AndroidComposeTestRuleExt_androidKt`] the extension first calls `getHasRobolectricFingerprint()` and, if true, logs `"Accessibility checks are currently not supported by Robolectric"` via `Log.w` and returns. This session's spike agrees: with `@GraphicsMode(NATIVE)`, enabling checks and calling `onRoot().tryPerformAccessibilityChecks()` on a 50 dp unlabeled `clickable` Box with `contentDescription = ""` produced no exception (printed `result=null`). With legacy graphics, a vector-icon screen additionally threw `NullPointerException: Cannot invoke "android.graphics.Bitmap.setHasAlpha(boolean)" because "bm" is null`.
**How to avoid:** do not add the artifact. Use `A11yAudit` (Code Examples 2) and the device checklist. UI-SPEC "Verification Hooks" already allows this: "any check that cannot run under Robolectric is listed in the device checklist instead".
**Warning signs:** a plan task that adds `ui-test-junit4-accessibility` or an `androidTest` directory.

### Pitfall 2: Secrets and OTP codes already in the saved-state Bundle
**What goes wrong:** D-09 forbids it; two ViewModels do it. A plan that only "adds SavedStateHandle to signup" would miss them and leave a policy violation.
**How to avoid:** remove `savedStateHandle[KEY_CODE] = ...` and the `get(KEY_CODE)` seeding in `SignupVerifyEmailViewModel` (keep `KEY_CODE_SENT` and `KEY_COOLDOWN_END`, which are not secrets) and in `SetNewPasswordViewModel` (the code field returns empty). Update `SignupVerifyEmailViewModelTest` and `SetNewPasswordViewModelTest`, and the comment in `STATE.md` line 181 ("Rotation on Forgot and Set New Password keeps the typed email, code and passwords") which is now wrong about the code. Add a test per VM asserting that after typing a code the handle's keys do not contain it.

### Pitfall 3: The portrait lock is ignored at the sizes where it matters
**What goes wrong:** Testing "portrait lock" on a tablet or resizable window shows rotation.
**Why:** [CITED: developer.android.com/about/versions/17/behavior-changes-17] "this opt-out will no longer be available for apps that target Android 17 (API level 37) or higher": orientation, resizability and aspect-ratio restrictions are ignored on sw >= 600dp. Robolectric does not simulate this.
**How to avoid:** apply `android:screenOrientation="portrait"` on `MainActivity` only; do not touch `ConnectReturnActivity` (translucent `Theme.Translucent.NoTitleBar`); do not add `configChanges`; test large windows with `@Config(qualifiers = "w900dp-h600dp")` layout tests, and treat the lock itself as a manifest assertion plus a device-checklist row. The system image `android-37.0` and the AVD `Medium_Phone_API_37.0` exist locally, but that AVD is a phone profile, not a resizable one (Environment Availability).

### Pitfall 4: Double insets
**What goes wrong:** `imePadding()` under a Scaffold whose `innerPadding` already holds the navigation inset leaves a gap equal to the nav bar height when the keyboard opens.
**How to avoid:** follow R3 and the repo idiom `padding(innerPadding).consumeWindowInsets(innerPadding)` then one `imePadding()`. The inset tests (Code Examples 3) assert the footer's bottom edge, not pixels.

### Pitfall 5: Snackbar suspension and flag ordering
**What goes wrong:** `showSnackbar` suspends until the snackbar is dismissed. Calling `consume...()` before launching it in a separate scope cancels it; calling it inline in a `LaunchedEffect` keyed on the flag delays the consume until dismissal and can re-trigger.
**How to avoid:** keep the existing order (`scope.launch { showSnackbar(...) }` then `onConsumed()`), or the `SignupOverviewScreen` order where the effect itself awaits and consumes afterwards (that screen's flag is derived, not one-shot). Put whichever you choose in the shared helper once, and dismiss `currentSnackbarData` before showing a new refresh-failed snackbar so repeated realtime failures never queue.

### Pitfall 6: `SavedStateHandle` limits
**What goes wrong:** Non-`Bundle` types throw at save; large payloads (a whole message list, a bitmap) risk `TransactionTooLargeException` because saved state is shared across the process's binder transaction.
**How to avoid:** mirror only the user's input and ids, as every existing VM does. Never mirror server data (lists, DTOs). Use `ArrayList`, not `List`.

### Pitfall 7: Restored input must beat server hydration
**What goes wrong:** After process death the form first renders the restored text, then a late server response overwrites it.
**How to avoid:** copy `SignupBasicInfoViewModel`'s pattern: persist a `touched`/`edited` flag next to the value and only seed from the server when the flag is false. Cover with a test that restores a handle with the flag set, then completes the fetch and asserts the restored text remains.

### Pitfall 8: Release (R8) behavior of back stack restoration is unverified
**What goes wrong:** Every process-death check so far (Phase 6 UAT) ran on a non-minified debug build. `rememberNavBackStack(key)` is the reflection-based overload on Android (release notes of Navigation 3 1.0.0-alpha10: "Restrict the reflection-based `NavBackStackSerializer` overload to Android"). The release build enables R8 with `optimization { enable = true; packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**") }` (`app/build.gradle.kts`) and the repo has no `proguard-rules.pro`.
**How to avoid:** this is [ASSUMED] to be a risk, not a confirmed defect. The cheapest check is a device row: signed or unsigned `prodRelease` installed on the Samsung, "Don't keep activities" on, background and return from a deep screen. Put it in the checklist as an explicit R8 row; if it fails, add `-keep class * implements androidx.navigation3.runtime.NavKey { *; }` style rules.

### Pitfall 9: Semantics assertions do not prove the TalkBack experience
**What goes wrong:** A test shows a node has `contentDescription`; TalkBack still reads something different (merging, reading order, role announcements, text-field names).
**How to avoid:** keep the device checklist TalkBack rows for the four flows as acceptance, and use tests for regressions only. Specific unverified items: how TalkBack reads a `ValidatedField` after the fix (a `contentDescription` on an editable node may replace the value in speech; the alternative is a label node kept in the merged tree), and whether `traversalIndex = -1f` on the call pill puts it first when it also sits at a higher `zIndex` (the traversal guide says to keep ordered elements at the same `zIndex`).

### Pitfall 10: Robolectric `sdk=35` is below targetSdk 37
**What goes wrong:** Behavior that only exists at API 36/37 (orientation ignoring, edge-to-edge opt-out removal) is not exercised by tests.
**How to avoid:** do not write tests that claim to prove API 37 platform behavior. Test the app's own layout at large-window qualifiers instead.

## Code Examples

### 1. `ErrorState` extension (UI-SPEC A1), defaults equal today's strings so the existing tests stay green
```kotlin
// Source: UI-SPEC "ErrorState extension"; current signature in components/EmptyState.kt:78-90
@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    heading: String = stringResource(R.string.error_state_heading),
    body: String = stringResource(R.string.error_state_body),
    retryLabel: String = stringResource(R.string.error_state_retry),
) {
    EmptyState(
        icon = Icons.Outlined.CloudOff,
        heading = heading,
        body = body,
        modifier = modifier.semantics(mergeDescendants = false) { liveRegion = LiveRegionMode.Polite },
        action = EmptyStateAction(retryLabel, onRetry),
    )
}
// EmptyState: wrap the heading Text with Modifier.semantics { heading() }.
```

### 2. `A11yAudit` semantics helper (replaces the Compose ATF checks on the JVM)
```kotlin
// Design for app/src/test/.../testing/A11yAudit.kt [ASSUMED design; every API below is public Compose test API]
fun ComposeTestRule.assertNoUnnamedActionables() {
    onAllNodes(hasClickAction() or hasSetTextAction(), useUnmergedTree = false)
        .fetchSemanticsNodes()
        .forEach { node ->
            val c = node.config
            val named = c.getOrNull(SemanticsProperties.ContentDescription)?.isNotEmpty() == true ||
                c.getOrNull(SemanticsProperties.Text)?.isNotEmpty() == true
            check(named) { "Actionable node ${node.id} has no name: $c" }
        }
}
fun ComposeTestRule.assertMinTouchTarget(min: Dp = 48.dp) {
    onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
        val size = node.boundsInRoot  // px == dp at the default mdpi test density
        check(size.width >= min.value && size.height >= min.value) { "Target ${node.id} is $size" }
    }
}
```
Traversal order: compute `(traversalIndex, top, left)` over focusable merged nodes, following the documented rule that indices compare only within a group, and assert the expected label sequence per flow.

### 3. IME and cutout insets in a Robolectric Compose test (spike-verified this session)
```kotlin
// Spike result printed: "SPIKE before bottom=800.0.dp" then "SPIKE after bottom=500.0.dp" for a
// 50 dp bar at the bottom of a Box with Modifier.fillMaxSize().imePadding() after dispatching a 300 px IME inset.
@get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

fun dispatchIme(view: View, bottomPx: Int) {
    val ime = WindowInsetsCompat.Type.ime()
    val insets = WindowInsetsCompat.Builder()
        .setInsets(ime, Insets.of(0, 0, 0, bottomPx))
        .setVisible(ime, true)
        .build()
    rule.runOnUiThread {
        ViewCompat.dispatchApplyWindowInsets(rule.activity.window.decorView, insets)
        ViewCompat.dispatchApplyWindowInsets(view, insets)   // view = LocalView.current captured in setContent
    }
    rule.waitForIdle()
}
// Assert on getUnclippedBoundsInRoot() of the composer/footer node: its bottom == root height - ime inset (+ tolerance).
// Use WindowInsetsCompat.Type.displayCutout() with Insets.of(left, 0, 0, 0) for the cutout case.
// Use @Config(qualifiers = "w360dp-h800dp") as the repo does, so dp == px.
```
The spike used `createAndroidComposeRule<ComponentActivity>()` (not the `v2.createComposeRule()` the repo mostly uses) because it needs `rule.activity`; both exist in `ui-test-junit4`.

### 4. `SavedStateHandle` process-death emulation without Robolectric
```kotlin
// Source: pattern of EntrySavedStateHandleTest.kt (Compose-level) reduced to the ViewModel level [ASSUMED shape]
val first = ClientProfileSettingsViewModel(/* fakes */, savedStateHandle = SavedStateHandle())
first.onFieldChange(ProfileField.FirstName, "Mia")
val restored = SavedStateHandle(first.savedKeysSnapshot())          // copy handle.keys() to a map in the test
val second = ClientProfileSettingsViewModel(/* fakes */, savedStateHandle = restored)
assertEquals("Mia", second.uiState.value.firstName)
// For secrets: assertFalse(handle.keys().any { it.contains("password") || it.contains("code") })
```

### 5. Manifest portrait lock
```xml
<!-- Source: UI-SPEC "Orientation"; current block in app/src/main/AndroidManifest.xml -->
<!-- iOS parity, CONTEXT D-01 -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTop"
    android:screenOrientation="portrait"
    android:theme="@style/Theme.ClickMe.Splash"
    android:windowSoftInputMode="adjustResize"
    tools:ignore="LockedOrientationActivity">
```
(`tools:ignore` value is [ASSUMED]; run `:app:lintDevDebug` once and keep it only if lint reports the manifest warning.)

### 6. `Modifier.contentMaxWidth()` idiom
```kotlin
// [ASSUMED idiom; the Wave 0 bounds test at w900dp and w360dp proves it]
val ContentMaxWidth = 600.dp
fun Modifier.contentMaxWidth(): Modifier =
    this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = ContentMaxWidth).fillMaxWidth()
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `adjustResize` resizes the window for the IME | Compose applies `imePadding()` itself; `adjustResize` no longer resizes on API 30+ with edge-to-edge | targetSdk 35 enforces edge-to-edge | Every field screen needs explicit IME handling (already present) |
| Orientation lock honored everywhere | Orientation, resizability and aspect-ratio restrictions ignored on sw >= 600dp; opt-out gone at targetSdk 37 | Android 16 (API 36) introduced, API 37 removes the opt-out [CITED: developer.android.com/about/versions/17/behavior-changes-17] | Portrait lock is a phone policy only; large windows must work (D-02) |
| `rememberNavController` with route strings | Navigation 3: you own the back stack, `@Serializable NavKey`s, `rememberNavBackStack` | Navigation 3 1.0 | Restoration works if keys are serializable (they are) |
| View-based ATF checks | Compose ATF (`enableAccessibilityChecks`, since Compose 1.8.0) | Compose 1.8.0 | Instrumented only; no Robolectric support |

**Deprecated/outdated:**
- CONTEXT claim "`composerText` is ViewModel-only today": stale; mirrored since Phase 12.
- UI-SPEC claim that `enableAccessibilityChecks` can run "under Robolectric": false in this setup.

## Assumptions Log

> Claims tagged `[ASSUMED]` need user or device confirmation before they become decisions.

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | R8 + `rememberNavBackStack` (reflection overload) restores the back stack correctly in a minified release build | Pitfall 8 | Process-death restore silently lands on the start screen in production; mitigated by one device row and a keep rule |
| A2 | M3 `Scaffold` places the snackbar from the bottom using `contentWindowInsets` when no bottom bar exists, so the 2 zero-inset screens with snackbars (Availability, Tax) draw it under the nav bar | Gap Inventory POL-03 | Either unnecessary work or a missed overlap; a bounds test settles it in minutes |
| A3 | A `contentDescription` (or hidden merged label) on `ValidatedField` will make TalkBack read both name and value | Pitfall 9, Gap Inventory | The "fix" could make TalkBack stop reading the typed value; needs the device TalkBack row before it is called done |
| A4 | `traversalIndex = -1f` on the call pill makes it first across tabs despite its higher z-order | Gap Inventory POL-04 | Pill is reached last; device row, fallback is a focus-request on entering a screen while a call is active |
| A5 | `fillMaxWidth().wrapContentWidth(CenterHorizontally).widthIn(max).fillMaxWidth()` centers and caps a column in every parent | Code Examples 6 | Wrong placement in `Box`/`Column` parents; caught by the Wave 0 bounds test |
| A6 | `SnackbarDuration.Long` with an action is extended by M3 when accessibility services are on | UI-SPEC A2 | Snackbar may time out under TalkBack before the user reaches Retry |
| A7 | `tools:ignore="LockedOrientationActivity"` is the correct lint id and is needed | Code Examples 5 | Harmless extra attribute, or a lint warning left in place (`warningsAsErrors = false`) |
| A8 | A resizable AVD (Resizable experimental profile) can be created from the installed `android-37.0` system image | Environment Availability | D-02 large-window verification has no target; fall back to Robolectric `w900dp` layout tests plus a window-resize note in the checklist |
| A9 | Removing the OTP mirrors does not break a flow that relied on them (a rotation retaining the typed code is lost) | Pitfall 2 | A user who rotates/recreates loses the 6 digits; this is the intended D-09 trade-off but should be confirmed since earlier phases chose otherwise |

## Open Questions

1. **Is the D-14 "Compose accessibility checks" substitute acceptable?**
   - What we know: the artifact is a Robolectric no-op (verified); there is no `androidTest` source set; UI-SPEC already permits listing unrunnable checks in the device checklist.
   - What's unclear: whether the user wants an `androidTest` source set and an emulator run after all.
   - Recommendation: accept `A11yAudit` + contrast unit test + Accessibility Scanner on the Samsung; record the deviation in VERIFICATION.md.

2. **Remove the OTP mirrors (D-09) even though Phases 3 and 4 chose to keep them?**
   - What we know: D-09 is explicit and locked; both mirrors exist today with tests.
   - What's unclear: nothing technical; only that this overrides T-03-20 / T-04-26-era decisions.
   - Recommendation: remove, update the two ViewModel tests and the STATE.md line 181 wording.

3. **`contentMaxWidth()` per screen or at the navigation hosts?**
   - What we know: UI-SPEC says per screen with full-width app bars and tab bar; there are about 50 `Scaffold` files and no shared screen scaffold.
   - What's unclear: whether the Week 7 budget allows 50 mechanical edits.
   - Recommendation: default to the UI-SPEC per-screen form for the SC-4 flows and shared components (`PickerSheet`, footers); apply a host-level clamp for the long tail (settings, expert screens) and record the narrower app bar as an accepted deviation if the budget requires it.

4. **Should relaunch after a process kill during a call land on the booking detail even when the user started the call elsewhere?**
   - What we know: no persisted marker; restoration returns to whatever screen was under the layer.
   - What's unclear: whether "lands on the booking/session detail" (D-11) is a hard requirement or an expectation.
   - Recommendation: treat the restored-screen behavior as sufficient; if not, scope a small DataStore "last call booking id" as its own task (UI-SPEC A4).

5. **Are `SectionError` instances inside the topic picker sheet and time-slots card compliant, or must they become `ErrorState`?**
   - What we know: UI-SPEC archetype E says `ErrorState` inside the sheet; D-08 says do not rework compliant screens; `SectionError` has a heading-less message plus a tonal Retry.
   - Recommendation: keep `SectionError` for the time-slots card (it is a section of a form, archetype C-like) and convert only the picker sheet if its error lacks a heading; confirm at plan time.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK (Android Studio JBR) | All Gradle commands | yes | 25.0.3 (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`) | None needed |
| Gradle + AGP | Build and unit tests | yes | AGP 9.4.1, Gradle daemon 9.6.0 | None needed |
| Robolectric unit tests | Automated gate | yes | 4.17, `sdk=35` | None needed. Observed incremental run: one test class in about 11-45 s |
| Samsung SM-A075F | Device checklist | yes (connected over adb wireless, `R8YYC1QFA6H`) | n/a | Checklist stays open (standing preference) |
| Android emulator + AVD | Large-window and API-26-32 rows | partial | AVD `Medium_Phone_API_37.0` (phone profile); system images `android-34`, `android-37.0` | Create a Resizable AVD on `android-37.0` (A8); otherwise log as a gap for Phase 16 |
| `androidTest` source set / instrumented runner | Compose ATF checks | no | `app/src/androidTest` does not exist | Not used (Pitfall 1) |
| iOS peer for live calls | Call checklist rows | not checked | n/a | Rows stay with Phase 16 (D-15) |
| Sandbox Stripe backend | Paid booking checklist rows | not checked | n/a | Rows stay with Phase 16 (D-15) |

**Missing dependencies with no fallback:** none for the automated gate.
**Missing dependencies with fallback:** resizable emulator (create one, or defer large-window device rows).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 (`sdk=35`) + Compose `ui-test-junit4` 1.12.1 (BOM) + coroutines-test 1.11.0 |
| Config file | `app/src/test/resources/robolectric.properties` (`sdk=35`, `application=android.app.Application`); `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*SomeTest'` |
| Full suite command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest ktlintCheck :app:assembleDevDebug` then `bash scripts/check-design-rules.sh` (and `:app:testProdDebugUnitTest`, lint, `assembleProdRelease` for the phase gate, as Phase 14) |
| Large-window layout tests | `@Config(qualifiers = "w900dp-h600dp")`; phone tests use `w360dp-h800dp` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| POL-01 | Per screen: "Loading" node, empty heading, error heading + "Retry" re-invokes load; `ErrorState` params and heading semantics | Robolectric Compose | `... --tests '*ControlsTest'` plus one `*ScreenTest` per audited screen | Many exist (for example `ExpertDashboardScreenTest`, `ControlsTest`); gaps are Wave 0 |
| POL-01 | Refresh-failed snackbar shows the "Retry" action, Retry runs the Loud refresh, quiet reload shows nothing, only one snackbar at a time | Robolectric Compose | `... --tests '*RefreshFailedSnackbarTest'` and the 12 screen tests | Helper test is new (Wave 0) |
| POL-02 | OTP/secret keys are absent from the handle after typing | JVM unit | `... --tests '*SignupVerifyEmailViewModelTest' --tests '*SetNewPasswordViewModelTest'` | Exist; need updates |
| POL-02 | `SavedStateHandle` round-trip for Settings, Tax, Delete forms; restored text beats hydration | JVM unit | `... --tests '*ClientProfileSettingsViewModelTest'` etc. | Exist; new cases |
| POL-02 | Chat composer text restored per thread; failed bubbles not restored | JVM unit + Compose `StateRestorationTester` | `... --tests '*ChatThreadViewModelTest'` | Exist; new cases |
| POL-02 | Signup step + typed text restored through nav entry decorators | Robolectric (pattern of `EntrySavedStateHandleTest`) | `... --tests '*EntrySavedStateHandleTest'` | Pattern exists |
| POL-02 | Confirm-only dialogs do not re-open confirmed; call layer never auto-joins on composition | Robolectric Compose | `... --tests '*CallOverlayHostTest'` | Exists; new cases |
| POL-03 | IME inset keeps composer, Login submit, booking footer, one Settings form, Report sheet above the keyboard | Robolectric Compose with `InsetsTestSupport` | `... --tests '*InsetsTest'` | New (Wave 0 helper, per-flow cases) |
| POL-03 | Cutout inset on the two roots; large-window cap and centering of `contentMaxWidth()` | Robolectric Compose (`w900dp`) | `... --tests '*ContentMaxWidthTest'` | New |
| POL-03 | Insets rule (a)(b)(c) and R7 pass on `app/src` | Script | `bash scripts/check-design-rules.sh` | Extend existing script; it must exit 0 after opt-outs and the R7 fix |
| POL-03 | Manifest declares `screenOrientation="portrait"` on `MainActivity` only | JVM unit (parse the merged manifest) or a script check | `... --tests '*ManifestOrientationTest'` | New (cheap) |
| POL-04 | The 4 flows expose names, roles, states, headings, live regions and order per UI-SPEC | Robolectric Compose with `A11yAudit` | `... --tests '*TalkBackFlowTest'` (Login, Booking, Call, Chat) | New |
| POL-04 | Contrast ratios of token pairs meet the UI-SPEC table | JVM unit | `... --tests '*PaletteContrastTest'` | New |
| POL-04 | Real TalkBack reading, font 200%, large window, "Don't keep activities", R8 restore | Manual (device checklist) | n/a | `15-DEVICE-CHECKLIST.md` (new) |

### Sampling Rate
- **Per task commit:** the single touched test class (for example `./gradlew :app:testDevDebugUnitTest --tests '*LoginScreenTest' ktlintCheck`), about 15-60 s incremental.
- **Per wave merge:** `./gradlew :app:testDevDebugUnitTest ktlintCheck :app:assembleDevDebug` and `bash scripts/check-design-rules.sh`.
- **Phase gate:** full suite on both flavors (`testDevDebugUnitTest`, `testProdDebugUnitTest`), lint, `assembleProdRelease` (R8), design rules green before `/gsd-verify-work`. The device checklist is logged open and does not block (D-14).

### Wave 0 Gaps
- [ ] `app/src/test/.../testing/A11yAudit.kt` - semantics audit helper (POL-04)
- [ ] `app/src/test/.../testing/InsetsTestSupport.kt` - IME and cutout dispatch helpers (POL-03)
- [ ] `RefreshFailedSnackbarTest`, `ContentMaxWidthTest`, `PaletteContrastTest`, `ManifestOrientationTest`
- [ ] `scripts/check-design-rules.sh` insets rule and its fixtures (a deliberately bad sample file under a temp dir to prove the rule fails)
- [ ] Framework install: none (no new dependency)

## Security Domain

`security_enforcement` is not set to false in `.planning/config.json` (the file contains only `workflow._auto_chain_active`), so this section applies.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (Login, recovery flows are in scope for TalkBack and restoration) | Existing `AuthValidators`, backend rate limits; do not weaken. Password and OTP fields never persisted client-side beyond memory |
| V3 Session Management | no change | Existing EncryptedSharedPreferences JWT store and `TokenAuthenticator` untouched |
| V4 Access Control | no | No new endpoint or role logic |
| V5 Input Validation | yes | Existing field validators and caps (`capText`, `COMPOSER_MAX`); restored values must pass through the same caps, as `ReportAndBlockViewModel` does (`capText(...)` on read) |
| V6 Cryptography | no | No new crypto; never hand-roll |
| V8 Data Protection | yes | Keep secrets out of `SavedStateHandle` and `NavKey`s (D-09); `allowBackup="false"` already set |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Secret (password, OTP, TIN) written to saved-state Bundle, readable from a device backup, a debuggable process or the system's saved-state store | Information disclosure | Never mirror; add a test that asserts absent keys (Pitfall 2). Today `"verify_code"` and `"reset_code"` violate this |
| Restored input bypasses validation caps | Tampering | Re-apply caps on read (existing pattern) |
| Exported `MainActivity` re-processing a stale intent after recreation | Spoofing/Elevation | Already handled: `shouldReadLaunchPush(savedInstanceState, intent)` and extras removed after consume (`MainActivity.kt`); keep it untouched |
| Auto-rejoin of a call after restore opens the microphone without consent | Information disclosure | Never auto-join; mic permission and token re-fetched on a user tap (D-11); test that composition alone does not call join |
| PII in `NavKey`s persisted in saved state | Information disclosure | Existing keys carry only email (`ForgotPasswordKey`, `SetNewPasswordKey`, `PasswordChangedKey`); add no new PII to keys |

## Sources

### Primary (HIGH confidence)
- Repository files read this session: `15-CONTEXT.md`, `15-UI-SPEC.md`, `AndroidManifest.xml`, `MainActivity.kt`, `ClickMeApp.kt`, `navigation/NavigationState.kt`, `navigation/SignupSetupKeys.kt`, `navigation/AuthKeys.kt`, `navigation/SignupSetupNavHost.kt`, `components/EmptyState.kt`, `components/SectionError.kt`, `components/ValidatedField.kt`, `components/buttons/AsyncSaveButton.kt`, `components/RescheduleResponseCard.kt`, `screens/login/LoginScreen.kt`, `screens/signup/overview/SignupOverviewScreen.kt`, `screens/signup/verifyemail/SignupVerifyEmailViewModel.kt`, `screens/login/setnewpassword/SetNewPasswordViewModel.kt`, `screens/chat/chatting/ChatThreadViewModel.kt`, `screens/call/calling/CallOverlayHost.kt`, `screens/clientmode/makeabooking/BookingErrors.kt`, `scripts/check-design-rules.sh`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/test/resources/robolectric.properties`, `.planning/STATE.md` (lines 161-304).
- Executed this session (spike files created and deleted, `git status` clean afterwards): IME inset dispatch under Robolectric; `enableAccessibilityChecks` behavior under Robolectric (NATIVE and legacy graphics); merged semantics dump of `ValidatedField`.
- Decompiled `ui-test-junit4-accessibility-android-1.12.1.aar` (`javap -c`): Robolectric fingerprint check and log text.
- Decompiled `activity-1.13.0.aar`: `EdgeToEdgeApi28.adjustLayoutInDisplayCutoutMode` sets `layoutInDisplayCutoutMode = 1`.
- Compose BOM POM: https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom
- Android 17 behavior changes (orientation/resizability opt-out removed at targetSdk 37): https://developer.android.com/about/versions/17/behavior-changes-17
- Compose accessibility traversal order (`isTraversalGroup`, `traversalIndex`, zIndex note): https://developer.android.com/develop/ui/compose/accessibility/traversal

### Secondary (MEDIUM confidence)
- Compose accessibility testing (setup, checks performed, API-34 requirement): https://developer.android.com/develop/ui/compose/accessibility/testing
- Navigation 3 release notes (reflection-based `NavBackStackSerializer` restricted to Android, `rememberNavBackStack` polymorphism): https://developer.android.com/jetpack/androidx/releases/navigation3

### Tertiary (LOW confidence)
- Third-party summary that Compose ATF is unsupported under Robolectric (confirmed independently by the bytecode above, so no longer relied on).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - versions read from `libs.versions.toml` and the BOM POM; no new dependency.
- Architecture and gap inventory: HIGH - read from the code this session; the state-matrix scan was keyword-based, so the per-screen "static form vs data load" calls for the screens listed under "Screens with no state code" must be confirmed by reading at plan time.
- Test technique (IME insets, SavedStateHandle round-trip): HIGH - IME technique executed; ATF limitation verified in bytecode and by execution.
- TalkBack runtime behavior, R8 restore, snackbar offset, traversal-first pill: LOW to MEDIUM - listed as assumptions with a concrete check each.

**Research date:** 2026-10-29
**Valid until:** 2026-11-28 (the stack is pinned; re-check only if Compose BOM or Robolectric versions change)
