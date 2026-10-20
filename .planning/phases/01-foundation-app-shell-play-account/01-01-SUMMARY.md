---
phase: 01-foundation-app-shell-play-account
plan: 01
subsystem: infra
tags: [gradle, hilt, flavors, ci, ktlint, kotlin-2.4]

requires: []
provides:
  - Pinned version catalog (Kotlin 2.4.21, KSP 2.3.12, Hilt 2.60.1, Nav3 1.2.0, Compose BOM 2026.09.00)
  - dev/prod flavors (dimension env) with per-flavor BASE_URL, STRIPE_PUBLISHABLE_KEY, AGORA_APP_ID
  - ClickMeApplication (@HiltAndroidApp), AppConfig, AppLinks
  - Shared Phase 1 strings.xml and dev app_name override
  - Backup hardening (allowBackup=false, all domains excluded incl. device-transfer)
  - ktlint (.editorconfig), scripts/check-design-rules.sh, GitHub Actions CI
affects: [all later Phase 1 plans, phase 2 networking and session storage]

tech-stack:
  added: [Hilt, KSP, Retrofit+Moshi, OkHttp, Coil 3, Navigation 3, kotlinx-serialization, Robolectric, ktlint-gradle]
  patterns:
    - "AppConfig object is the only reader of BuildConfig fields; shared components take plain parameters"
    - "Flavor-specific unit tests live in src/testDev and src/testProd"
    - "Robolectric tests run with application=android.app.Application to stay Hilt-free"

key-files:
  created:
    - .editorconfig
    - .github/workflows/ci.yml
    - scripts/check-design-rules.sh
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/config/AppConfig.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/config/AppLinks.kt
    - app/src/dev/res/values/strings.xml
    - app/src/testDev/java/com/uptrendinvestments/clickmeapp/config/AppConfigDevTest.kt
    - app/src/testProd/java/com/uptrendinvestments/clickmeapp/config/AppConfigProdTest.kt
    - app/src/test/resources/robolectric.properties
  modified:
    - gradle/libs.versions.toml
    - build.gradle.kts
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/res/values/strings.xml
    - app/src/main/res/xml/backup_rules.xml
    - app/src/main/res/xml/data_extraction_rules.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Theme.kt

key-decisions:
  - "Kotlin 2.4.21 instead of CONTEXT's 2.2 (D-14): current libraries ship 2.4 metadata"
  - "navigation-compose and hilt-navigation-compose intentionally absent (Navigation 3, D-01/D-15)"
  - "dev flavor labelled 'ClickMe Dev' since both flavors share one applicationId"

patterns-established:
  - "CI command == local command: ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug"

requirements-completed: [FND-01, FND-02]

duration: 25min
completed: 2026-10-20
---

# Phase 1 Plan 01: Build Foundation Summary

**Kotlin 2.4.21 / AGP 9.4.1 build with dev and prod flavors carrying committed Stripe/Agora/base-URL config, Hilt application, ktlint, a design-rules gate and a push/PR GitHub Actions workflow.**

## Performance

- **Duration:** ~25 min
- **Tasks:** 3 completed (Task 2 as TDD: RED + GREEN)
- **Files modified:** 25 or so

## Accomplishments
- Both flavors assemble with the full locked dependency set; ktlintCheck, lintDevDebug, lintProdDebug, testDevDebugUnitTest and testProdDebugUnitTest all pass locally in one invocation.
- Per-flavor tests prove BASE_URL, Stripe key prefix (pk_test_ vs pk_live_), Agora ID and flavor name. Stripe keys were copied from iOS Environments.plist via PlistBuddy, never retyped.
- design-rules script exits 0 on the repo and 1 on seeded Color literal and `*Brand` fixtures.
- CI workflow: push (all branches) + pull_request, read-only permissions, JDK 25, no emulator step.

## Task Commits

1. **Task 1: Toolchain, dependency set, flavors, ktlint** - `6fcdec4` (feat)
2. **Task 2: Hilt app, backup hardening, AppConfig, strings, per-flavor tests**
   - RED: `0032dfd` (test)
   - GREEN: `bc176b9` (feat)
3. **Task 3: design-rules script and CI workflow** - `25ba077` (ci)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Template Color literals in a comment block failed the design-rules gate**
- **Found during:** Task 3
- **Issue:** The template `Theme.kt` carried a commented `/* Other default colors to override ... Color(0xFF...) */` block; the new script flagged it, so `check-design-rules.sh` could not exit 0 on the repo as the acceptance criteria require.
- **Fix:** Deleted the commented template block from `Theme.kt` (no functional change; plan 01-03 rewrites the file).
- **Files modified:** app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Theme.kt
- **Commit:** 25ba077

**2. [Process] Worktree base correction**
- The worktree started at dc2172d, behind the expected base a8b4aba; reset to a8b4aba per the startup check before any work.

## Known Stubs
None.

## Threat Flags
None. Only publishable pk_ keys are committed; CI has `contents: read` and no secrets; `okhttp-logging` is debugImplementation only.

## Notes for Later Plans
- The design-rules script's Stripe-secret scan covers `app`, `gradle` and root `*.kts`.
- `lintDevDebug`/`lintProdDebug` are green with `warningsAsErrors=false`; no lint-baseline was added.
- CI is not yet proven on GitHub (plan 01-09 pushes and checks the Actions run).

## Self-Check: PASSED
- Files verified present: .editorconfig, .github/workflows/ci.yml, scripts/check-design-rules.sh (mode 755), ClickMeApplication.kt, AppConfig.kt, AppLinks.kt, dev strings.xml, both config tests, robolectric.properties.
- Commits verified: 6fcdec4, 0032dfd, bc176b9, 25ba077.
