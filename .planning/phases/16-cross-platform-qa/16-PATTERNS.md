# Phase 16: Cross-Platform QA - Pattern Map

**Mapped:** 2026-10-30
**Files analyzed:** 6 (4 new docs, 1 modified build file, 1 gitignore check)
**Analogs found:** 6 / 6 (phase is docs and release wiring; no app source is created)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `.planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md` | doc (human-run checklist) | batch / manual | `.planning/phases/15-polish-hardening/15-DEVICE-CHECKLIST.md` | exact |
| `.planning/phases/16-cross-platform-qa/16-RELEASE-STEPS` (release pipeline section of runbook, if split out) | doc (human-only steps) | manual, ordered | `.planning/phases/13-push-notifications-internal-testing/13-RELEASE-CHECKLIST.md` | exact |
| `.planning/phases/16-cross-platform-qa/16-BUG-LOG.md` | doc (table log) | CRUD (append rows) | `15-DEVICE-CHECKLIST.md` table style, `15-UAT.md` / `13-UAT.md` result tracking | role-match |
| `.planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md` | doc (table log) | append-only | `13-RELEASE-CHECKLIST.md` Check/Rollback step form (no direct ledger analog) | partial |
| `app/build.gradle.kts` (versionCode bump) | config | one-line edit | itself, lines 76-77 | exact |
| `keystore.properties` (template, gitignored) | config | read at build | `app/build.gradle.kts` lines 21-29, 113-126; `.gitignore` lines 20-21 | exact |

All analog paths are git-tracked (verified with `git ls-files`: 13-RELEASE-CHECKLIST, 13-UAT, 15-DEVICE-CHECKLIST, 15-UAT).

## Pattern Assignments

### `16-MATRIX-RUNBOOK.md` (doc, manual batch)

**Analog:** `.planning/phases/15-polish-hardening/15-DEVICE-CHECKLIST.md`

**Header block** (lines 1-17): title, a one-paragraph purpose statement, then a `## Header` section with Device, build, accounts rule, and a Columns legend. Copy this. Adapt it as follows:
- Device = Samsung SM-A075F plus the real iPhone.
- Build = Play internal-track `prodRelease`.
- Backend = production, Stripe live mode.
- Keep the rule "Accounts referenced by role only; never paste a password, token or key into this file." Use roles A (real expert) and B (second real client) from the 16-RESEARCH Pattern 5.
- Drop the "non-blocking" wording. D-09 makes this blocking.

**Row table pattern** (line 21 header, rows follow). Section = flow, one row per check, with a source ID so results can be written back:
```markdown
| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| L1 SC-4 run: log in with TalkBack (P15-04, P15-05, P3 SC-1) | Complete a login using only TalkBack: ... | n/a | [ ] | n/a | n/a | n/a | [ ] |
```
For the runbook, replace the variant columns with: `Row ID | Source row ID | Direction (D1 Android client / D2 Android expert) | Steps | Expected | Evidence to capture | Result`. Keep these conventions: every Result cell starts `[ ]`, `n/a` for non-applicable cells, and the row ID prefix is the source ID (L3, B4, C2, K3 and so on).

**Ordering:** follow 16-RESEARCH Pattern 5 (sections 0 through 6). Add a "Paid bookings created" table with an end-of-run refund checklist (D-04).

**Step form for the setup/pre-flight section:** see the 13-RELEASE-CHECKLIST excerpt below.

---

### `16-MATRIX-RUNBOOK.md` release pipeline section (doc, ordered human steps)

**Analog:** `.planning/phases/13-push-notifications-internal-testing/13-RELEASE-CHECKLIST.md`

**Intro and dependency-order line** (lines 3-8):
```markdown
Every step below needs your accounts, your Mac or a device ... Tick a box only after its "Check:" result is what you see. Each step is its own item, so an interrupted run resumes at the first unchecked box. ... Nothing here should ever be pasted into the repo: passwords and tokens stay in your password manager.

Short names used below: `AS_JBR` is `/Applications/Android Studio.app/Contents/jbr/Contents/Home`; ... `APP` is `/Users/LeonChen/Documents/ClickMe_Android`.
```

**Per-step structure** (lines 10-27): `## N. Title (plan, decision)`, a `Needs:` line, numbered commands, `Check:`, `Rollback:`, and a closing tickbox.
```markdown
## 1. Deploy the backend push change (13-02, D-04)
Needs: ...
1. ...
Check: the commit in the `/health` JSON is the same 7 characters as ...
Rollback: ...
- [ ] (1) Backend D-04 deployed: `/health` commit equals the pushed master sha
```
Use this for: keystore creation (one-way), signed `bundleProdRelease`, jarsigner/apksigner verify, R8 cold start, internal-track upload (one-way). Mark the one-way steps. The exact commands are in 16-RESEARCH Pattern 1 (steps 1-7). Reuse its Firebase step 4 `git check-ignore app/google-services.json` check form.

---

### `16-BUG-LOG.md` (doc, append-only table)

**Analog:** table form of `15-DEVICE-CHECKLIST.md`. There is no existing bug-log file.

Use the exact D-11 columns from 16-RESEARCH Pattern 4:
```markdown
| ID | Source (runbook row id or free-form) | Steps | Device and build (device, versionCode, installerPackageName) | Owner (android/ios/backend) | Severity (blocking or not, D-07 clause) | Status | Fix commit | Re-test result |
|---|---|---|---|---|---|---|---|---|
```
Add a short header like 15-DEVICE-CHECKLIST's: no secrets or tokens in rows, and logcat evidence lives outside the repo or is scrubbed. Owner `ios` or `backend` rows also append one line to the report-to-iOS list in `.planning/STATE.md` (D-12).

---

### `16-BUILD-LEDGER.md` (doc, append-only table)

**Analog:** none direct. Use the 16-RESEARCH layout: one row per upload with `versionCode | versionName | git sha | aab sha256 | mapping.txt copy path | Play status | installed-on-device proof (dumpsys versionCode + lastUpdateTime + installerPackageName)`. Use the Check/Rollback phrasing of 13-RELEASE-CHECKLIST for the "prove the build on device" step: `adb -t T shell dumpsys package com.uptrendinvestments.clickmeapp | grep -E "versionCode|lastUpdateTime|installerPackageName"`.

---

### `app/build.gradle.kts` (config, one-line edit per upload)

**Analog:** itself, `defaultConfig` lines 72-80:
```kotlin
defaultConfig {
    applicationId = "com.uptrendinvestments.clickmeapp"
    minSdk = 26
    targetSdk = 37
    versionCode = 1
    versionName = "0.1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}
```
Policy (16-RESEARCH Pattern 2): keep a hand-bumped literal. Each batched fix build is a single commit `chore(release): versionCode N` that changes only `versionCode` and appends a ledger row. Leave `versionName` at `0.1.0`. Do not derive from git or a `-P` property.

**Release signing** (lines 113-126) already reads `keystore.properties` and falls back to unsigned. Do not modify:
```kotlin
signingConfigs {
    if (!keystoreProps.isEmpty) {
        create("release") {
            storeFile = file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }
}
...
if (!keystoreProps.isEmpty) signingConfig = signingConfigs.getByName("release")
```

---

### `keystore.properties` (config, human-created, never committed)

**Analog:** property names above (lines 25-29, 113-126). `.gitignore` already ignores `*.keystore` (line 20) and `keystore.properties` (line 21). Note that `*.jks` is **not** matched by the `*.keystore` line. The 16-RESEARCH check showed no tracked `.jks` and the jks lives in `~/keys/`, outside the repo, so no change is needed. If a template is wanted, create `keystore.properties.example` containing only these keys, with placeholder values and no real passwords:
```
storeFile=/Users/<you>/keys/clickme-upload.jks
storePassword=<from password manager>
keyAlias=upload
keyPassword=<from password manager>
```
Claude never generates or sees the passwords. Build from the main checkout, not a worktree, because worktrees lack `keystore.properties`.

## Shared Patterns

### No secrets in docs
**Source:** `15-DEVICE-CHECKLIST.md` header and `13-RELEASE-CHECKLIST.md` line 3.
**Apply to:** all four new docs. Reference accounts by role only, and never include a password, token, card number or key.

### Source-ID traceability
**Source:** `15-DEVICE-CHECKLIST.md` "Source column" convention (line 17) and CONTEXT code_context.
**Apply to:** runbook and bug log. Cite source row IDs (L3, B4, C2, K3, and Phase 9, 11, 12, 13, 14 STATE.md items) so results can be written back.

### Tickbox and resumable steps
**Source:** `13-RELEASE-CHECKLIST.md` (`- [ ] (n) ...` after a `Check:` line); `15-DEVICE-CHECKLIST.md` (`[ ]` / `n/a` cells).
**Apply to:** runbook rows and release steps.

### Fix-plan gate
**Source:** CONTEXT code_context. `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` plus `scripts/check-design-rules.sh`. The full gate adds `testProdDebugUnitTest lint assembleProdRelease`, run lint and R8 separately because of the OOM. Use `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`, add `-t <transport_id>` to adb when only wireless ADB is attached, and never `gradlew --stop`.
**Apply to:** every later fix plan.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `16-BUG-LOG.md` | log | append | No prior bug-log file; use the D-11 columns and a table style from 15-DEVICE-CHECKLIST |
| `16-BUILD-LEDGER.md` | log | append | No prior ledger; use the 16-RESEARCH layout |

Later fix plans have no analog until bugs are logged. They will touch app source, so map them then via `gsd-pattern-mapper` against the specific files.

## Metadata

**Analog search scope:** `.planning/phases/13-*`, `.planning/phases/15-*`, `app/build.gradle.kts`, `.gitignore`
**Files scanned:** 6
**Pattern extraction date:** 2026-10-30
