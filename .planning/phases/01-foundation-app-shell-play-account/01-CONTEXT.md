# Phase 1: Foundation, App Shell & Play Account - Context

**Gathered:** 2026-10-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1 delivers:
- The project scaffolding (dependencies, flavors, build config, CI).
- The app-wide Material 3 design system and shared components.
- The Android 12+ splash.
- The `AppRoute` root switch.
- The client and expert 5-tab scaffolds, with a saved back stack per tab and placeholder tab content.
- The start of the Play Console organization account.

Requirements: FND-01, FND-02, FND-09, FND-10, FND-11, FND-12, REL-01, REL-02.

Not in scope:
- Networking and session (Phase 2).
- Real login/signup screens (Phases 3–4).
- Any feature screen content.

</domain>

<decisions>
## Implementation Decisions

### Navigation & modules
- **D-01:** Use **Navigation 3** (androidx.navigation3), not Navigation Compose 2.
  - The app owns each tab's back stack as a plain list. That gives saved per-tab history (FND-12) and makes push deep links into a tab easy later (Phase 13).
  - The root `AppRoute` switch (splash → login | signupSetup | clientHome | expertHome) is app state, as on iOS. It is not a nav destination.
- **D-02:** **Single `app` Gradle module.**
  - Packages mirror the iOS folder tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/` (DEC-file-organization).
  - Suggested top-level packages: `ui/theme`, `ui/components`, `navigation`, `screens/...`, `config`. The planner can refine these.
  - Don't split into modules during the MVP.

### Keys & build flavors
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

### CI pipeline
- **D-07:** **GitHub Actions** runs on **every push to any branch and on pull requests**. Each run does:
  - Android lint
  - Unit tests
  - Debug builds of **both** flavors
  - ktlint check
  - The UI-SPEC rule that blocks hex color literals outside `ui/theme/Color.kt`
- **D-08:** **ktlint** is the code style checker. Provide a local auto-format Gradle task; CI fails on violations. No detekt for now.
- **D-09:** No emulator/instrumented UI tests in CI during Phase 1. Revisit in the QA phase (Phase 16).

### App identity & Play
- **D-10:** The app's display name is **"ClickMe"**, the same as the iOS `CFBundleDisplayName`.
- **D-11:** **Launcher icon derived from the iOS assets** (revised 2026-10-19 after research — `AppIcon-1024.png` has its background baked in and `AppIcon-1024-Tinted.png` is solid white, so neither is usable):
  - The adaptive icon foreground AND the themed (monochrome) layer are both built from the transparent mark in `ios-reference/ClickMe2026/Assets.xcassets/SplashLogo.imageset`, on a solid background. The UI-SPEC/theme decides the background color.
  - The splash mark comes from `SplashLogo.imageset` (per UI-SPEC: mark only, on `#131313`).
- **D-14:** **Kotlin 2.4.21 + KSP 2.3.12** (user-approved 2026-10-19). Kotlin 2.2.10 from the template fails to build against current Hilt/OkHttp/Retrofit/coroutines (they pull kotlin-stdlib 2.4.x). This supersedes the "Kotlin 2.2" wording in Claude's Discretion below.
- **D-15:** Navigation 3 (D-01) supersedes the Navigation Compose 2 *mechanism* wording in the UI-SPEC and FND-01; the UI-SPEC's tab *behavior* rules (per-tab stacks, back-to-home, re-tap-to-root) still apply.
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase design contract
- `.planning/phases/01-foundation-app-shell-play-account/01-UI-SPEC.md` — approved UI design contract:
  - Color scheme, Nunito type scale (12/16/20/28 sp), spacing tokens
  - Splash and tab scaffold specs, including the per-tab back stack, re-tap-to-root and Back rule
  - The six shared components
  - Debug gallery and scaffold switch
  - The hex-literal CI rule

### Project rules
- `CLAUDE.md` — locked stack, platform swaps, file organization (mirror the iOS tree)
- `.planning/PROJECT.md` — locked decisions (DEC-*), constraints, "do not port" list
- `.planning/REQUIREMENTS.md` — FND-01, FND-02, FND-09..12, REL-01, REL-02
- `.planning/ROADMAP.md` §Phase 1 — goal and success criteria

### iOS reference (read-only)
- `ios-reference/ClickMe2026/Environments.plist` — base URL, Stripe publishable keys, Agora App ID per environment
- `ios-reference/ClickMe2026/Configuration/AppEnvironment.swift` — how the environment config is loaded
- `ios-reference/ClickMe2026/ClickMe2026App.swift` — the `AppRoute` root switch
- `ios-reference/ClickMe2026/Views/Screens/Splash/` — splash behavior (Android drops the 3-second delay)
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Home/HomeClientView.swift`, `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Home/HomeExpertView.swift` — tab bars
- `ios-reference/ClickMe2026/Views/Extensions/HomeNavigationPath.swift` — per-tab navigation paths
- `ios-reference/ClickMe2026/Theme/Brand.swift`, `ios-reference/ClickMe2026/Theme/BrandFont.swift` — palette and type mapping (already distilled into the UI-SPEC)
- `ios-reference/ClickMe2026/Views/Components/` — shared components to port
- `ios-reference/ClickMe2026/Assets.xcassets/AppIcon.appiconset/`, `ios-reference/ClickMe2026/Assets.xcassets/SplashLogo.imageset/` — icon and splash artwork

### Original plan
- `docs/PORT_ROADMAP.md` §Week 1 and §Play Store account — original scope notes

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- The Android Studio template only:
  - `MainActivity.kt`
  - `ui/theme/{Color,Theme,Type}.kt`, with Purple/Pink and light/dynamic color code that the UI-SPEC says to delete
  - Example tests
- Nothing else to reuse.

### Established Patterns
- Gradle version catalog (`gradle/libs.versions.toml`) is already in use. Add all new dependencies there.
- AGP 9.4.1, Kotlin 2.2.10, Gradle 9.6.0, Compose BOM 2026.02.01, Java 11 target.
- compileSdk/targetSdk 37, minSdk 26.
- Release builds already enable R8 optimization (`optimization { enable = true }`).
- No Hilt, Navigation, Coil, OkHttp or Retrofit yet, and no `kotlin-android`/KSP plugin aliases yet. Hilt needs KSP.
- Package already renamed to `com.uptrendinvestments.clickmeapp` (namespace, applicationId and source folders).

### Integration Points
- `MainActivity` becomes the single activity:
  - Installs the SplashScreen
  - Applies `ClickMeTheme`
  - Hosts the `AppRoute` switch
- The splash "keep on screen" condition must be a hook that Phase 2's auto-login can plug into (UI-SPEC note). Phase 2 also adds an upper bound on the hold.
- Phase 4 replaces the debug scaffold switch with the real mode switch.

</code_context>

<specifics>
## Specific Ideas

- Keep parity with how iOS handles environments: committed per-environment config, and all environments on the production backend.
- The user will do the Play account steps from a checklist, so make that checklist precise and in order.

</specifics>

<deferred>
## Deferred Ideas

- Emulator/instrumented UI tests in CI → Phase 16 (Cross-Platform QA).
- detekt static analysis → revisit after the MVP if needed.
- Splitting into Gradle modules → after the MVP, only if build times hurt.
- Switching the prod flavor and backend to Stripe live mode → release work (Phase 17).

</deferred>

---

*Phase: 01-foundation-app-shell-play-account*
*Context gathered: 2026-10-19*
