---
phase: quick-261027-vow
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthTopAppBar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/DesignSystemGalleryScreen.kt
autonomous: true
requirements: [FND-09]

estimate:
  tokens: 12000
  raw_tokens: 12000
  tasks: 1
  confidence: low

must_haves:
  truths:
    - "Every back arrow in the app draws Icons.AutoMirrored.Outlined.ArrowBack: the shared AuthTopAppBar (used by 29 call sites), the chat thread top bar, the Expert Profile top bar, the Forgot Password 'Back to Login' text button and the Design System Gallery top bar. Each one still mirrors under RTL"
    - "Every Material icon in screens/chat, components/chat and components/safety is Outlined or AutoMirrored.Outlined, with no exception left"
    - "Nothing else changes: contentDescription (R.string.a11y_back on the 4 top bars, null on the decorative Back to Login icon), onClick, enabled, size and tint are byte-identical, so TalkBack still announces 'Back'"
    - "No other Filled icon is touched. App-wide non-comment Filled usage lines go from 27 to 22, and Filled import lines go from 27 to 22. Expert Profile's Filled MoreVert stays"
    - ":app:testDevDebugUnitTest, ktlintCheck and :app:assembleDevDebug pass, and scripts/check-design-rules.sh reports OK"
  artifacts:
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthTopAppBar.kt"
      provides: "Shared auth/signup/profile top bar back arrow, Outlined"
      contains: "Icons.AutoMirrored.Outlined.ArrowBack"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt"
      provides: "Chat thread top bar back arrow, Outlined (closes the v9i exception)"
      contains: "Icons.AutoMirrored.Outlined.ArrowBack"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt"
      provides: "Expert Profile top bar back arrow, Outlined"
      contains: "Icons.AutoMirrored.Outlined.ArrowBack"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreen.kt"
      provides: "Back to Login text-button glyph, Outlined"
      contains: "Icons.AutoMirrored.Outlined.ArrowBack"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/DesignSystemGalleryScreen.kt"
      provides: "Debug gallery top bar back arrow, Outlined"
      contains: "Icons.AutoMirrored.Outlined.ArrowBack"
  key_links:
    - from: "AuthTopAppBar navigationIcon"
      to: "the 29 AuthTopAppBar(...) call sites in app/src/main"
      via: "the shared composable, so one edit restyles every screen that uses it"
      pattern: "Icons\\.AutoMirrored\\.Outlined\\.ArrowBack"
    - from: "the 5 call-site files"
      to: "material-icons-core 1.7.8 (androidx/compose/material/icons/automirrored/outlined/ArrowBackKt.class)"
      via: "import androidx.compose.material.icons.automirrored.outlined.ArrowBack"
      pattern: "^import androidx\\.compose\\.material\\.icons\\.automirrored\\.outlined\\.ArrowBack$"
---

<objective>
Switch all 5 back-arrow icons in the app from the AutoMirrored Filled ArrowBack to `Icons.AutoMirrored.Outlined.ArrowBack`, so the app follows the Outlined icon rule in Phase 12 UI-SPEC line 40 ("Material Icons Extended, Outlined") with no exception.

Purpose: this follows up on quick task 261027-v9i. That task left the chat thread back arrow Filled so it would match the other four back arrows, and it proposed switching all five together. The user chose to do that now. The change is presentation only, with no behavior, API, string or a11y change.
Output: 5 usage swaps and 5 import swaps across 5 files.

Requirement mapping: FND-09 covers the app-wide visual language (theme, type, iconography). No phase requirement changes state.

Discretion choices (Claude's discretion; this quick task has no CONTEXT.md):
- Scope is back arrows only. The other 22 Filled icons in app/src/main are out of scope, including Expert Profile's top-bar MoreVert, the bottom-nav selected icons (01-UI-SPEC line 30: "Outlined for unselected, Filled for selected") and the AuthComponentsTest Filled icons in app/src/test.
- Tracer-first: there is one task, and it is the tracer. The whole change is one thin slice (import, composable, build, Compose tests), so there are no expansion tasks.
- Completed-phase docs are not edited. 03-UI-SPEC.md line 30 names the Filled back arrow, and the v9i SUMMARY records "back arrow stays Filled". Both stay as historical records. This plan's SUMMARY records that they are superseded.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md
@.planning/quick/261027-v9i-phase-12-ui-review-fixes-externalise-rea/261027-v9i-SUMMARY.md

Facts grounded at plan time (HEAD ae3394f):
- The 5 call sites are listed below. Each file imports the AutoMirrored Filled ArrowBack exactly once and uses it exactly once. Every usage line has the form `imageVector = <icon>,`.
  - screens/login/AuthTopAppBar.kt: import line 5, usage line 42 (navigationIcon IconButton, enabled = backEnabled, contentDescription a11y_back). It is used by 29 `AuthTopAppBar(` call sites.
  - screens/chat/chatting/ChatThreadScreen.kt: import line 27, usage line 479 (navigationIcon, contentDescription a11y_back).
  - screens/clientmode/expertprofile/ExpertProfileScreen.kt: import line 17, usage line 264 (navigationIcon). This file ALSO imports `androidx.compose.material.icons.filled.MoreVert` (line 18) and uses Filled MoreVert at line 294. Both stay.
  - screens/login/forgetpassword/ForgotPasswordScreen.kt: import line 18, usage line 175 (the glyph inside the "Back to Login" TextButton, contentDescription null, Modifier.size(BackIconSize)).
  - screens/debug/DesignSystemGalleryScreen.kt: import line 9, usage line 54 (navigationIcon).
  All paths are under app/src/main/java/com/uptrendinvestments/clickmeapp/.
- None of the 5 files imports any other automirrored Filled icon. The only extra Filled import in them is MoreVert in ExpertProfileScreen.kt.
- Import ordering: in all 5 files the new `...icons.automirrored.outlined.ArrowBack` import sorts into the same slot as the line it replaces. It comes after `...icons.Icons` (uppercase sorts first in ASCII) and before `...icons.filled.*` / `...icons.outlined.*` / `androidx.compose.material3.*`. So a straight in-place replacement keeps ktlint's lexicographic order.
- Classpath: `androidx/compose/material/icons/automirrored/outlined/ArrowBackKt.class` exists in material-icons-core 1.7.8, which is already a dependency next to material-icons-extended (app/build.gradle.kts:108). The same `automirrored.outlined` package is already imported in app/src/main, for example ChatComposer.kt (Send) and CancellationReasonStep.kt (ArrowForward). No dependency change is needed.
- Tests: nothing in app/src/test references ArrowBack. The Compose tests find back buttons by contentDescription ("Back"), which does not change. There is no screenshot-test tooling (no Roborazzi or Paparazzi), so there are no goldens to update.
- Gate baselines at HEAD (each deliverable gate fails at HEAD):
  - Filled ArrowBack lines in app/src: 10. Target 0.
  - Outlined ArrowBack import files in app/src/main: 0. Target 5.
  - Outlined ArrowBack usage lines: 0. Target 5.
  - Filled usage lines across screens/chat, components/chat and components/safety: 1, the thread back arrow. Target 0.
  - App-wide non-comment Filled usage lines: 27. Filled import lines: 27. Both should drop by exactly 5. The gate checks this through the diff, not an exact grep count (see the next item).
  - Files changed under app/src/main since ae3394f: 0. Target 5.
  - Diff guard: every added or removed line under app/src/main since ae3394f must contain ArrowBack. This passes vacuously at HEAD and fails if any other icon or line is touched. It is a preservation guard, like the Expert Profile MoreVert check, which passes at HEAD on purpose.
- The working tree has unrelated dirty files (.idea/*, .planning/config.json, .planning/state.json, .planning/debug/evidence/). Stage only the 5 files in files_modified plus this task's SUMMARY. Never run `./gradlew --stop`.
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Switch all 5 back arrows to AutoMirrored Outlined ArrowBack (imports + usages), proven by gates, ktlint, compile and the touched screens' Compose tests</name>
  <files>app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthTopAppBar.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreen.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/DesignSystemGalleryScreen.kt</files>
  <action>
Per Phase 12 UI-SPEC line 40 (Material Icons Extended, Outlined) and the user's choice to switch the back arrows app-wide, make the same two-line edit in each of the 5 files:
- Change the icon import from the `automirrored` `filled` package to `androidx.compose.material.icons.automirrored.outlined.ArrowBack`. Replace it in place, on the same line, because the new import keeps its lexical slot.
- Change the `imageVector =` argument from the AutoMirrored Filled ArrowBack to `Icons.AutoMirrored.Outlined.ArrowBack`. Keep the trailing comma.

The files and lines (from the plan-time grep):
- AuthTopAppBar.kt: import line 5, usage line 42. Do this one first. It is the shared top bar behind 29 screens, so it carries most of the visual change.
- ChatThreadScreen.kt: import line 27, usage line 479. This closes the one exception v9i left in the chat directories.
- ExpertProfileScreen.kt: import line 17, usage line 264. Leave the `filled.MoreVert` import (line 18) and the Filled MoreVert usage (line 294) exactly as they are. That icon is out of scope.
- ForgotPasswordScreen.kt: import line 18, usage line 175. Keep `contentDescription = null` and `Modifier.size(BackIconSize)`. The glyph is decorative next to the "Back to Login" label.
- DesignSystemGalleryScreen.kt: import line 9, usage line 54.

Do not touch any other argument (contentDescription, onClick, enabled, modifier, tint). Do not touch any other icon, file or string. Do not add comments about the icon family. A comment that names the old icon would also trip the negative gate. If ktlintCheck reports import ordering anyway, run `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew ktlintFormat` and recheck. Then confirm with `git diff --stat` that only these 5 files changed, with 2 lines each.

Commit as `style(quick-261027-vow): AutoMirrored Outlined back arrows app-wide`, staging only the 5 files.
  </action>
  <verify>
    <automated>! grep -rqE 'automirrored\.filled\.ArrowBack|AutoMirrored\.Filled\.ArrowBack' app/src && test "$(grep -rlE '^import androidx\.compose\.material\.icons\.automirrored\.outlined\.ArrowBack$' app/src/main | wc -l | tr -d ' ')" = 5 && test "$(grep -rhE 'Icons\.AutoMirrored\.Outlined\.ArrowBack,' app/src/main | wc -l | tr -d ' ')" = 5 && test "$(grep -rhE 'Icons\.(Filled|AutoMirrored\.Filled)\.' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat app/src/main/java/com/uptrendinvestments/clickmeapp/components/chat app/src/main/java/com/uptrendinvestments/clickmeapp/components/safety | grep -vcE '^\s*(//|/?\*)')" = 0 && N="$(git diff --name-only ae3394f -- app/src/main)" && test "$(printf '%s\n' "$N" | wc -l | tr -d ' ')" = 5 && D="$(git diff ae3394f -- app/src/main)" && test -n "$D" && ! printf '%s\n' "$D" | grep -E '^[-+][^-+]' | grep -qv 'ArrowBack' && grep -q 'Icons.Filled.MoreVert' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew ktlintCheck :app:testDevDebugUnitTest --tests '*ChatThreadScreenTest' --tests '*ExpertProfileScreenTest' --tests '*ForgotPasswordScreenTest' --tests '*SetNewPasswordScreenTest' --tests '*AuthStaticScreensTest' --tests '*LoginScreenTest'</automated>
  </verify>
  <done>No Filled ArrowBack remains anywhere in app/src (baseline 10 lines). Exactly 5 files import the AutoMirrored Outlined ArrowBack, and there are exactly 5 usages (baseline 0). screens/chat, components/chat and components/safety have zero Filled usages (baseline 1). Exactly 5 files under app/src/main differ from ae3394f (baseline 0), and every added or removed line in that diff contains ArrowBack. So only the 5 back arrows changed (App-wide Filled lines go from 27 to 22), and Expert Profile's Filled MoreVert is intact. ktlintCheck is clean, and the Compose tests for the chat thread, Expert Profile, Forgot Password, Set New Password, auth static screens and Login pass. One commit holds exactly the 5 files.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| none | Presentation-only change: one icon vector swapped at 5 call sites. There is no new input, network, storage, IPC or permission path, and no a11y label change |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-261027-vow-SC | Tampering | npm/pip/cargo/Gradle dependency installs | low | accept | No package installs and no dependency or version-catalog change. The Outlined ArrowBack comes from material-icons-core 1.7.8, already resolved with material-icons-extended (app/build.gradle.kts:108) and confirmed on the local classpath at plan time (automirrored/outlined/ArrowBackKt.class). files_modified contains no build or catalog file |
</threat_model>

<verification>
Run sequentially on the main working tree with a long Bash timeout (or in the background), since the full unit suite is slow. Never run `./gradlew --stop`.

1. JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest ktlintCheck :app:assembleDevDebug. Expect BUILD SUCCESSFUL.
2. bash scripts/check-design-rules.sh. Expect "Design rules: OK".
3. `grep -rn 'automirrored.filled.ArrowBack\|AutoMirrored.Filled.ArrowBack' app/src` prints nothing.
4. `grep -rn 'Icons\.\(Filled\|AutoMirrored\.Filled\)' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat app/src/main/java/com/uptrendinvestments/clickmeapp/components/chat app/src/main/java/com/uptrendinvestments/clickmeapp/components/safety` prints nothing.
5. `git status --short` shows no further changes from this plan beyond the committed 5 files and the SUMMARY. The pre-existing .idea/ and untracked .planning/ entries stay unstaged.

Record in the SUMMARY (decisions section):
- This supersedes the v9i decision "Thread back arrow stays AutoMirrored.Filled.ArrowBack". Phase 12 UI-SPEC line 40 now holds with no exception.
- 03-UI-SPEC.md line 30 lists the Filled back arrow by name. It is a completed-phase record and was not edited. The app-wide convention is now AutoMirrored Outlined for back arrows.

Record in the SUMMARY (open items, non-blocking, per the project's deferred-manual-testing practice):
- On-device glance on the Samsung test device: the Login/Signup top bar, the chat thread, Expert Profile and Forgot Password show the outlined arrow. With an RTL locale, the arrow still points toward the start edge. Fold this into the Phase 15 polish pass.
- Observation only, not changed: Expert Profile's top-bar overflow is still Filled MoreVert next to the now-Outlined back arrow. It is a candidate for the Phase 15 icon sweep.
</verification>

<success_criteria>
- All 5 back arrows in app/src/main are `Icons.AutoMirrored.Outlined.ArrowBack`, imported from `androidx.compose.material.icons.automirrored.outlined.ArrowBack`. Zero Filled ArrowBack references remain in app/src.
- screens/chat, components/chat and components/safety have zero Filled icon usages. The v9i exception is closed.
- Exactly 5 Filled usages and 5 Filled imports left the app (27 to 22 each). No other icon, argument, string or file changed.
- The full unit suite, ktlintCheck and assembleDevDebug pass, and check-design-rules.sh reports OK.
</success_criteria>

<output>
Create `.planning/quick/261027-vow-switch-all-app-back-arrows-to-automirror/261027-vow-SUMMARY.md` when done
</output>
