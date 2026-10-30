# Phase 16: Cross-Platform QA - Research

**Researched:** 2026-10-30
**Domain:** Release engineering (signed AAB, Play internal track) plus a human, two-device Android/iOS QA matrix on Stripe live mode, with a bug-triage and batched-fix loop
**Confidence:** MEDIUM-HIGH. Repo, backend-code and checklist facts are HIGH (read this session). Play Console timing and live-mode 3DS behaviour are MEDIUM or ASSUMED (flagged inline and in the Assumptions Log).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

### Money-path backend
- **D-01: The cross-platform money rows run in Stripe live mode against the production backend, with real cards and small charges.** Both apps stay on their production configs, so iOS needs no repointing and no sandbox backend is used for the matrix. This replaces the Phase 7 D-02 "never use real cards" rule for Phase 16 only, and only with the accounts named in D-03. Real money moves. — **Reversibility:** one-way — real charges and Stripe fees can't be undone, only refunded
- **D-02: 3DS is tested with a real card that forces a 3DS challenge in live mode.** The user has to get one, for example a card from a bank that always challenges. Decline cases use a real card that fails (for example insufficient funds), or are recorded as not run. Stripe test cards are not used. VERIFICATION records that "test cards including 3DS" was met with a real live 3DS card.
- **D-03: The live paid bookings use the user's own real expert account, whose live Connect account already accepts charges, on a topic at Stripe's minimum price (about $1 CAD).** Never use `appreview@clickme.test`, the store-review account. The client side is a second real account the user owns.
- **D-04: Refunds go through the app's own cancel flows.** The matrix's paid-cancel and refund rows produce the refunds, which tests those paths at the same time. Any charge still standing at the end is refunded by hand in the Stripe dashboard. The runbook lists every paid booking it creates, with an end-of-run refund checklist, so nothing is missed.
- **D-05: Payout onboarding is proven to the onboarding link only.** The Connect onboarding link opens and the app handles the return or refresh deep link, in both directions. Finishing a new Connect account is out of the bar. This matches the planned schedule-risk cut ("payouts down to the onboarding link only").

### Release-blocking bar
- **D-06: The must-pass set is the QA-02 and QA-03 matrix in both directions, plus every tier-1 row of `15-DEVICE-CHECKLIST.md`.** Tier 1 is log in, book, call and chat, including their 200% font, TalkBack, process-death and IME variants, and the tier-1 items in its Gap list. Tier 2–4 rows are run if time allows; their failures are triaged by D-07 and don't block automatically. Rows not run are left open with a post-release note.
- **D-07: What counts as release-blocking.** A bug blocks the release if it:
  - loses money or charges the wrong amount,
  - prevents a booked session or call from happening,
  - loses user data,
  - crashes the app,
  - or breaks a store-policy requirement (for example the foreground-service or account-deletion rules).

  Visual, accessibility polish and edge-case bugs get fixed if cheap; otherwise they're logged for after release.
- **D-08: The matrix runs on the signed prodRelease build installed from the Play internal track.** So Phase 16 starts with the 999.1 and Phase 13 release-checklist steps:
  - create the upload keystore and `keystore.properties`,
  - make a signed `bundleProdRelease` and check its signature,
  - run the R8 cold start,
  - upload to the internal track (REL-03, plus the REL-05 track existing).

  This covers 02-15 and 02-16. 02-09 (the Stripe spike) is replaced by the live matrix's card and 3DS rows, and FND-14 by the matrix's 5+ minute background call row. — **Reversibility:** one-way — the first upload permanently binds `com.uptrendinvestments.clickmeapp` and the upload key to the Play app
- **D-09: The phase blocks on the human checks.** The code plans (runbook, bug log, fix plans) run without waiting. Phase 16 can't close, and Phase 17 can't start, until the user has confirmed the matrix in both directions and the tier-1 rows. This overrides the "don't block on human checkpoints" rule from earlier phases for this phase only.

### Testers & bug intake
- **D-10: The user stays the only internal tester.** The SC-5 / REL-05 "a few real users on a mix of devices" gap is accepted at release (it carries Phase 13 D-22 forward). Record this in VERIFICATION and STATE. The emulator may cover a second Android version for non-hardware rows.
- **D-11: One bug log, `16-BUG-LOG.md`, in the phase directory.** One row per bug with these columns: ID, source (runbook row or free-form), steps, device and build, owner (android / ios / backend), severity (blocking or not, per D-07), status, fix commit, re-test result. The user reports results in the conversation, and Claude records and triages them. QA-01's "bugs reported from internal testing" means the user's own findings plus failed runbook rows.
- **D-12: Failures in the iOS app or the backend are logged with an owner tag and added to the report-to-iOS list in STATE.** They're fixed in the iOS or backend repo during Phase 16 only if they block by D-07 and also block the Android release (the way the Phase 11 G-11-6 backend fix was handled). Backend edits follow the Phase 13 rule: run them sequentially without worktree isolation, and the orchestrator commits.

### Test-session logistics
- **D-13: The hardware is the Samsung SM-A075F (Android) and the user's real iPhone (iOS), each signed in to a different real account (D-03).** Calls, push from a killed app and 3DS use real hardware on both sides. Simulators aren't used for matrix rows.
- **D-14: Claude's role is runbook, live log capture, and fix loop.** Claude writes one ordered `16-MATRIX-RUNBOOK.md`. Rows are grouped to keep account, app and direction switches to a minimum, with expected results and the evidence to capture for each row. During a session, Claude runs `adb logcat` and `dumpsys` on the Samsung (per the build-env notes: use the `-t <transport_id>` form when only wireless ADB is attached), the user reports pass or fail per row, and Claude logs, triages and fixes.
- **D-15: Fix builds are batched per session.** Blocking fixes found in a session are made, `versionCode` is bumped (it is hard-coded to `1` in `app/build.gradle.kts` today), and one new internal-track build is uploaded. Then the failed rows are re-run, plus a short smoke test. There is no per-fix sideloading as the normal path.

### Claude's Discretion
- How the runbook is ordered and grouped, and the exact evidence to capture per row (screenshots, logcat filters, the Stripe dashboard state).
- How `versionCode` is managed (a manual bump, or derived from a property), as long as every upload gets a higher code.
- Whether automated regression tests come with each blocking fix (the default is yes, where a JVM test can reproduce it).

### Deferred Ideas (OUT OF SCOPE)
- An `androidTest` source set with `ui-test-junit4-accessibility` and `enableAccessibilityChecks()` on the four core flows (from the 15 Gap list). After release.
- A wider internal or closed tester group on mixed devices (the SC-5 / REL-05 gap, D-10). After release.
- A hosted test-mode staging backend for safe Stripe test-card runs. Not built for the MVP (D-01 chose live mode).
- Finishing a full Connect onboarding with a new expert (D-05). After release.
- Tier 2–4 device rows not run in Phase 16 (D-06). After release.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| QA-01 | Bugs reported from internal testing are triaged, and the release-blocking ones are fixed. | Bug-log schema and D-07 severity rubric (Architecture Pattern 4), batched fix-build loop with versionCode ledger (Pattern 3, versionCode policy), evidence capture commands (Device Log Capture), Validation Architecture (regression test per fix). |
| QA-02 | The cross-platform matrix passes with an Android client and an iOS expert: Stripe (live card incl. 3DS), push from a killed app, a call kept alive in the background and across rejoin, and payout onboarding. | Source-row inventory (Matrix Source Rows A-C), live-mode Stripe constraints (price floor, 3DS, void-vs-refund), Connect link/return/refresh mechanics, killed-app push method, call evidence commands. |
| QA-03 | The same matrix passes with an iOS client and an Android expert. | Same rows mirrored (direction 2), account/device-token switch pitfalls, expert-side Android rows (accept/decline/Join/onboarding). |
| REL-03 (closed here via D-08) | Package registered by uploading the first signed internal build. | Release pipeline section (keystore, signed bundle, verification, first upload binding). |
| REL-05 (track half, D-08/D-10) | A build is on the internal track. | Play internal-track mechanics, opt-in link, install-from-Play timing and signature pitfall. |
| FND-13 / FND-14 (replaced per D-08) | Stripe spike / background Agora. | Replaced by matrix rows MX card/3DS and 5+ minute background call; the Phase 2 plans 02-09/02-12 stay unexecuted. |
</phase_requirements>

## Summary

Phase 16 is a human-in-the-loop QA phase with a small amount of code. The code side is: a release-signing and upload pipeline (keystore, signed `bundleProdRelease`, `versionCode` policy, R8 cold start), a runbook document, a bug log, and batched fixes with JVM regression tests. The device side is a two-direction matrix on the Samsung SM-A075F plus the user's iPhone, in Stripe live mode. Almost everything required already exists: the build already signs from `keystore.properties` when the file is present, `google-services.json` is in place with the right package, the backend push change is already deployed, and ~100 device rows are already enumerated in `15-DEVICE-CHECKLIST.md` and `STATE.md`. The research job is mostly to expose four places where CONTEXT assumptions do not match what the code and docs say, and to give the planner exact commands and a row inventory.

**Four CONTEXT assumptions that the planner must correct or confirm with the user (details in Common Pitfalls):**
1. **D-03 "about $1 CAD" is not reachable.** The backend refuses any paid topic or booking under 500 minor units (CA$5.00 by default, env-overridable) with `422 PRICE_BELOW_MINIMUM`; Stripe's own CAD floor is CA$0.50. The cheapest live paid session is CA$5.00 unless production overrides `MIN_PAID_SESSION_MINOR_UNITS` (not verifiable from here).
2. **D-04 "cancel flows produce the refunds" is wrong for pre-session cancels.** With manual capture, a pre-session cancel (client or expert) *voids* the authorization (`cancelPI`), so no charge, no refund and no Stripe fee occur. A real captured charge exists only after a paid session *completes* (capture about 15 minutes after the end time). Only those need a hand refund in the Stripe dashboard, and Stripe does not return the processing fee on a refund.
3. **3DS cannot be forced from the app or backend.** The backend sets no `request_three_d_secure`; whether a live card is challenged is the issuer's (or a Radar rule's) choice. The user should find a card that challenges *before* session day, and Add Payment Method (a SetupIntent, no charge) is the cheap pre-check.
4. **Checklist step 1 (backend push deploy) is already done.** Production `/health` returns commit `8d4450f`, which contains `92b4827`. Only the upload-side steps (3, 4, 5, 6) plus the Play-side items remain.

**Primary recommendation:** Plan three code-light plans first (release pipeline, runbook+bug-log generation, versionCode ledger), run the Play upload on day 1 of the phase (first-time test link can take hours and the temporary store name up to 48 h), keep `versionCode` as a hand-bumped literal in `app/build.gradle.kts` with a per-upload ledger and an `aapt2 dump badging` check, build the runbook from the source-row IDs in the Matrix Source Rows section grouped by device setting and direction, and treat the matrix, tier-1 rows and the user confirmation as manual-only gates (D-09).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Signing and packaging the release | Build (Gradle, local machine) | Play Console (re-signs with Play App Signing key) | Upload key signs locally (`keystore.properties`); Google re-signs for distribution (13-RELEASE-CHECKLIST step 5.3). |
| Paid booking authorization and capture | API / Backend (Stripe direct charge on the expert's connected account) | Client (collects `pm_`, runs 3DS via `PaymentLauncher`) | Path A: the app never confirms the booking PaymentIntent; server confirms, client only completes 3DS on a 402 (`STRIPE-INTEGRATION.md` section 1, 2.4). |
| Void vs refund on cancel | API / Backend | Stripe | `clientBooking.controller.js` decides void or refund from the live PI status (lines 882-922). |
| Connect onboarding link and return | API / Backend (mints link, 302 bridge `/connect/return`) | Browser/Custom Tab then Android `ConnectReturnActivity` | Stripe needs https return URLs; backend bounces to `clickme://connect/*` (`app.js:223-224`); app treats it as a re-check signal only. |
| Push delivery to a killed app | FCM / Android OS | Backend (sends FCM), app (token registration) | System-displayed notification payload; app code not running (no app logs exist for it). |
| Call keep-alive in background | Android foreground service (`CallService`, microphone type) | Agora SDK, OS battery policy (Samsung) | Samsung battery management can override; evidence is device-side. |
| Evidence capture | Developer machine (adb, Stripe dashboard, Supabase table editor, Railway logs) | — | Release build has no HTTP logging (`debugImplementation(okhttp.logging)`), so API-side proof comes from the backend and Stripe. |
| Bug triage and fix loop | Planning docs (`16-BUG-LOG.md`) | Android repo, iOS repo, backend repo (owner-tagged) | D-11, D-12. |

## Standard Stack

No new libraries are added in this phase. The "stack" is existing project tooling and exact versions read this session.

### Core
| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| Gradle/AGP | AGP 9.4.1, Kotlin 2.4.21 [VERIFIED: gradle/libs.versions.toml:2-3 `agp = "9.4.1"` `kotlin = "2.4.21"`] | `bundleProdRelease`, `assembleProdRelease` | Already the build. |
| Release signing wiring | `keystore.properties` read via `providers.fileContents`; release signs only when the file is non-empty [VERIFIED: app/build.gradle.kts:25-30, 113-131 `if (!keystoreProps.isEmpty) signingConfig = signingConfigs.getByName("release")`] | Upload-key signing | Nothing to change; just create the file. |
| `keytool`, `jarsigner` | Android Studio JBR `.../jbr/Contents/Home/bin` [VERIFIED: `ls` shows `jarsigner`, `keytool`] | Create key; verify AAB signature | `apksigner` cannot verify an `.aab` (APK only) [CITED: developer.android.com/tools/apksigner]; AAB is a JAR so `jarsigner -verify` is the check. |
| `apksigner`, `aapt2`, `zipalign` | build-tools 36.0.0 (also 34.0.0) [VERIFIED: `ls ~/Library/Android/sdk/build-tools/36.0.0`] | Verify the release APK certificate; read versionCode/`google_app_id` | `apksigner verify --print-certs -v app.apk` [CITED: developer.android.com/tools/apksigner]. |
| adb | `~/Library/Android/sdk/platform-tools/adb` [VERIFIED: path exists]; no device attached at research time (`adb devices -l` listed none) | Live log capture, installs, `dumpsys` | Per D-14 use `-t <transport_id>`. |
| Test stack | JUnit 4.13.2, Robolectric 4.17, Mockito 5.24.0, Compose ui-test, MockWebServer3 [VERIFIED: libs.versions.toml:63-65; app/build.gradle.kts:191-197] | Regression tests for fixes | Existing; ~4316 tests per flavor at the Phase 15 gate (STATE.md). |

### Supporting
| Tool | Purpose | When to Use |
|------|---------|-------------|
| Stripe Dashboard (live) | Inspect PaymentIntents (`requires_capture`, `canceled`, `succeeded`), refund captured charges | Every money row and the end-of-run refund checklist. Direct charges live on the connected account [CITED: docs.stripe.com/refunds "Stripe debits the connected account directly for refunds to direct charge payments"]. |
| Supabase table editor | Check `device_tokens.platform` after each login/account switch | Push rows (13-RELEASE-CHECKLIST step 10.2). |
| Railway logs | Backend-side proof (confirm-connection once, join, capture) | Call and money rows; the release app has no HTTP log. |
| `Medium_Phone_API_37.0` AVD | Android 17 audio-hardening row; large-window rows | Non-hardware rows only (D-10). [VERIFIED: `emulator -list-avds` prints `Medium_Phone_API_37.0`; `system-images/android-37.0` present] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Manual Play Console upload | Play Developer API / fastlane | Needs a service account and extra secrets for ~3-6 uploads; not worth it for the MVP. [ASSUMED] |
| Hand-bumped `versionCode` literal | `-P` property or git-commit-count derivation | See versionCode policy; hand bump is loud-on-mistake and matches Phase 2 D-17. |

**Installation:** none. No external packages are installed in this phase.

**Version verification:** not applicable (no new packages). `gsd-tools query package-legitimacy check` was not run because nothing is installed; the audit table below records "none".

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| (none: this phase installs no external packages) | — | — | — | — | — | — |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
                       DEV MACHINE (Mac)
  ┌─────────────────────────────────────────────────────────────┐
  │ keystore.properties (gitignored) ──► ./gradlew bundleProdRelease
  │ ~/keys/clickme-upload.jks             │ (AGP 9.4.1, R8 scoped to androidx/kotlin/kotlinx)
  │                                       ▼
  │            app-prod-release.aab ──jarsigner -verify──► ledger row (versionCode, sha256, git sha)
  │                    │  (also assembleProdRelease APK ──apksigner/aapt2 checks, sideload R8 cold start)
  └────────────────────┼────────────────────────────────────────┘
                       │ manual upload (Play Console, Internal testing, Play App Signing)
                       ▼
             GOOGLE PLAY (org account)  ── re-signs with Google key ──► opt-in link ──► Samsung SM-A075F (Play install)
                                                                                         │
   iPhone (production iOS app, pk_live) ◄───────── production backend (Railway, sk_live) ─┤  Android prodRelease (pk_live)
        ▲   ▲                                       │  │  │                              │
        │   └─ FCM/APNs push ◄──────────────────────┘  │  └─► Stripe LIVE: direct charge on expert's Connect acct
        │                                              │          authorize ──► (cancel: VOID, $0) | (session done: CAPTURE ~15 min after end)
        └─ Agora channel (booking id) ◄────────────────┴─► Agora ◄─ CallService (FGS microphone) on Samsung
                                                       
   EVIDENCE: adb -t <id> logcat/dumpsys (Samsung) | Stripe dashboard | Supabase device_tokens | Railway logs
   RESULTS:  user says pass/fail per row ─► Claude writes 16-BUG-LOG.md ─► triage (D-07) ─► batched fix ─► versionCode bump ─► new AAB ─► re-run failed rows + smoke
```

### Recommended Project Structure
```
.planning/phases/16-cross-platform-qa/
├── 16-CONTEXT.md / 16-RESEARCH.md / 16-VALIDATION.md
├── 16-MATRIX-RUNBOOK.md     # ordered rows, source row IDs, evidence per row, refund checklist
├── 16-BUG-LOG.md            # D-11 columns
├── 16-BUILD-LEDGER.md       # one row per upload: versionCode, versionName, git sha, aab sha256, mapping.txt copy, Play status, installed-on-device proof
└── 16-VERIFICATION.md       # QA-01..03, D-10 accepted gap, "live 3DS card used"
~/ClickMe-release/           # outside the repo (02-15 convention): clickme-<name>-<code>.aab/.apk + mapping.txt per upload
```

### Pattern 1: Release pipeline (do first, one-way steps flagged)
**What:** Follow `13-RELEASE-CHECKLIST.md` steps 3, 4, 5, 6 in that order; step 2 (Firebase) is already satisfied by `app/google-services.json` (package `com.uptrendinvestments.clickmeapp`, project `clickme-a006b` [VERIFIED: parsed with python3]). Step 1 is done (below).
**Ordered recipe:**
1. (Human, one-way key) `mkdir -p ~/keys`; `keytool -genkeypair -v -keystore ~/keys/clickme-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000` using the JBR keytool; write `keystore.properties` with `storeFile` (absolute), `storePassword`, `keyAlias=upload`, `keyPassword`. `~/keys` does not exist and `keystore.properties` does not exist today [VERIFIED: `ls ~/keys` error; `ls keystore.properties` error]. Both are ignored by git [VERIFIED: `git check-ignore keystore.properties` printed the path; `git ls-files | grep -cE '\.jks$|keystore\.properties'` printed 0]. Back up the jks and passwords immediately. Claude never sees or generates the passwords; Gradle reads the file silently, so an executor can then run the build without printing it.
2. Build from the **main checkout**, not a worktree (a worktree has no `keystore.properties`): `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleProdRelease :app:assembleProdRelease` (add `-Dorg.gradle.jvmargs=-Xmx4g` if memory pressure; never `gradlew --stop`).
3. Verify: `jarsigner -verify app/build/outputs/bundle/prodRelease/app-prod-release.aab` prints `jar verified.`; compare the signer with the keystore: `keytool -printcert -jarfile <aab>` SHA-1/SHA-256 equals `keytool -list -v -keystore ~/keys/clickme-upload.jks -alias upload` (02-15 Task 1 step 2). APK: `~/Library/Android/sdk/build-tools/36.0.0/apksigner verify --print-certs -v app/build/outputs/apk/prod/release/app-prod-release.apk`. Push config present: `aapt2 dump resources <apk> | grep google_app_id` must print a line. Version: `aapt2 dump badging <apk> | grep -E "package:|versionCode"` equals the ledger's expected code. [aapt2 dump badging: ASSUMED standard usage]
4. R8 cold start (the 02-15 Task 3 recipe, adapted: the temporary Login screen and "Force token refresh" button from the Phase 2 plan no longer exist; debug affordances are `BuildConfig.DEBUG`-gated [VERIFIED: HomeClientScreen.kt:102, HomeExpertScreen.kt:99]): `adb -t T uninstall com.uptrendinvestments.clickmeapp`; `adb -t T install <apk>`; `adb -t T logcat -c`; `adb -t T shell am start -W -n com.uptrendinvestments.clickmeapp/.MainActivity` (record `Status`, `TotalTime`); `adb -t T logcat -d -b crash | grep -c com.uptrendinvestments.clickmeapp` prints 0; `adb -t T exec-out uiautomator dump /dev/tty | grep -c 'Log in'` is at least 1. Then log in, confirm `adb -t T logcat -d -s Realtime` shows `state=Connected`.
5. Archive: copy AAB, APK and `app/build/outputs/mapping/prodRelease/mapping.txt` to `~/ClickMe-release/` with `shasum -a 256` (02-15 convention). A rebuild changes the mapping, so keep it per upload for retracing a crash. [mapping path: ASSUMED standard AGP output]
6. (Human, one-way) Play Console, Internal testing (not "Internal app sharing", which does not register the app [CITED: 13-RELEASE-CHECKLIST step 5.2]), choose Play App Signing with a Google-generated key (D-16 of Phase 2), upload the AAB. The first upload fixes the package name permanently [CITED: support.google.com/googleplay/android-developer/answer/9845334]. Then add the Play App Signing SHA-1 to Firebase (optional: FCM delivery does not need it, 13-RELEASE-CHECKLIST step 2).
7. Testers: email list with the user's Google account; use the opt-in link when status is Published (the link appears only then [CITED: answer/9845334]); install from Play.

**R8 risk is lower than usual here:** `optimization { enable = true; packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**") }` limits R8 to those packages [VERIFIED: app/build.gradle.kts:127-130], so Moshi/Stripe/Agora/Socket.IO classes are not renamed or removed. `rules.keep` holds only a Socket.IO keep rule and contains no `-assumenosideeffects`, so `Log.i/w` lines survive in release [VERIFIED: rules.keep read]. The known residual risk is Navigation3 `NavKey` saved-state restore under R8 (checklist row Y11).

### Pattern 2: versionCode policy (recommendation: hand-bumped literal plus ledger)
**Rule:** each Play upload needs a `versionCode` not used before; Play max is 2100000000; the system blocks downgrades [CITED: developer.android.com/studio/publish/versioning]. A forgotten bump fails loudly at upload (Play rejects the reused code), costing minutes.
**Recommendation:** keep `versionCode = N` as a literal in `defaultConfig` (matches Phase 2 D-17 "bumped by hand for each upload" [VERIFIED: 02-CONTEXT.md:92]). First upload is `versionCode = 1`, `versionName = "0.1.0"` (already the file's values [VERIFIED: build.gradle.kts:76-77]); each batched fix build is N+1 in one `chore(release): versionCode N` commit that also appends a `16-BUILD-LEDGER.md` row. Keep `versionName` at `0.1.0` for QA builds (the release name carries `0.1.0 (N)`); Phase 17 decides the public `versionName`.
**Why not the alternatives:** a `-P` property defaulting to 1 silently builds a code-1 AAB if the flag is forgotten (same failure mode, less visible); a git-commit-count derivation jumps on every docs commit, differs between worktrees and shallow clones, and is not reproducible from a tag. Neither earns its complexity for about 3-6 uploads.
**Guard (cheap, scriptable):** after each build run `aapt2 dump badging <apk> | grep versionCode` and compare to the ledger's expected value and to "previous + 1".

### Pattern 3: Batched fix loop (D-15)
Session -> user reports pass/fail per row -> Claude logs each fail in `16-BUG-LOG.md` and triages by D-07 -> blocking fixes made with a JVM regression test where reproducible -> gate (below) -> bump versionCode -> build, verify, archive -> upload -> install from Play -> **prove the build on the device before re-testing**: `adb -t T shell dumpsys package com.uptrendinvestments.clickmeapp | grep -E "versionCode|lastUpdateTime|installerPackageName"`; `versionCode` must equal the new code and `lastUpdateTime` must be later than the upload (the Phase 11 stale-build lesson: 11-UAT tests 5 and 6 ran stale builds and wasted two cycles [VERIFIED: 11-UAT.md notes]). Re-run failed rows plus a short smoke (login, one booking, one chat, one call join, one push).

### Pattern 4: Bug log (D-11)
Columns exactly: `ID | source (runbook row id or free-form) | steps | device and build (device, versionCode, installerPackageName) | owner (android/ios/backend) | severity (blocking or not, with the D-07 clause) | status | fix commit | re-test result`. Owner `ios` or `backend` items also append one line to the report-to-iOS list in STATE.md (D-12). Evidence files (logcat slices) live in the phase directory or `~/ClickMe-release/`, never containing tokens (see Security).

### Pattern 5: Runbook ordering (D-14 grouping)
Two accounts: **A = the user's real expert (live Connect charges_enabled)** and **B = the second real client account** (D-03). Direction 1 (QA-02): Samsung = B (client), iPhone = A (expert). Direction 2 (QA-03): Samsung = A (expert), iPhone = B (client). Order to minimise switches:
0. Pre-flight (accounts, availability, topics, card pre-check, tooling).
1. Single-device release smoke on the Play-installed build.
2. Direction 1 sessions: free flow (book, accept from iOS, push to killed Android, chat, call incl. 5+ min screen-off background, hang-up/rejoin, final-5-minute), then paid flow (Add Payment Method with live card, 3DS card, decline card, paid booking hold, expert accept, cancel = void; one paid session completed to capture), reschedule response, report/block, `device_tokens` check.
3. Switch accounts (log out on both phones; verify `device_tokens` platform flips for A and B).
4. Direction 2 sessions mirrored (iOS client books, Android expert gets request push on a killed app, accept/decline, Join from Dashboard/Upcoming/Summary, chat, paid booking from iOS, payout onboarding link on Android with return and refresh, reschedule proposed from iOS, account deletion of a throwaway account).
5. Samsung-only setting passes on the Play build, each done once while an account state is already prepared: Font 200% pass, TalkBack pass, "Don't keep activities"/process-death pass, IME/insets pass, system rows.
6. End-of-run refund checklist, Stripe dashboard review (no PaymentIntent left in `requires_capture`), logout cleanup.

## Matrix Source Rows (for `16-MATRIX-RUNBOOK.md`)

The runbook should cite these IDs verbatim so results can be written back to the originals. Prior evidence exists for some (noted) but all ran on `devDebug`, not the Play-signed `prodRelease`; Phase 16 re-runs them on the release build, so prior passes shorten the expectation but do not replace the row.

### A. Tier-1 single-device rows from `15-DEVICE-CHECKLIST.md` (D-06)

Tiering rule from Phase 15 D-16: tier 1 = the four SC-4 core flows (login, booking free and paid, call, chat) [VERIFIED: 15-CONTEXT.md:50-55]. The checklist has no tier column, so this inventory is derived. Group by the device setting so each setting is switched once.

| Setting pass | Row IDs (checklist) | Notes |
|---|---|---|
| Normal config, with a peer | K1 (also TalkBack), K2, K4, K5, K7 (IME), K10, K11, K12; B1 (TalkBack), B3, B4, B5, B6, B8; C1, C2, C5, C9 | Peer rows need the second account on the iPhone. |
| Font 200% (Settings > Display > Font size and Display size at max) | L4, L5, B2, B10 (re-scope: now live paid screens, not sandbox), C3, K6, Y7 (font part) | One continuous pass. |
| TalkBack on | L1, L2, L3, L7, B1, B3, B4, B5, B8, C1, C2, K1, K2, K3, K4, K5, K10, K11, Y12 (Accessibility Scanner over the four flows), Y14 | L3 and C2 and K3 are conditional-fix rows (see Gap list C). SC-4 runs L1/B1/C1/K1 are the user's Week-7 carry-over (15-UAT test 1). |
| "Don't keep activities" / process death | L8, B5 (open sheet), B7, C6, C7 (`am force-stop` during a call), C8 (cold-start notification tap), K8, K9, Y3, Y4, Y5, Y6, Y7, Y11 (R8 restore from a deep screen), Y1 (orientation) | Y11 is naturally satisfied by the release build; if a deep screen lands on the start screen add a NavKey keep rule (checklist row text). |
| IME and insets | L2, L4, L5, L6, B6, K7, K8, K9, Y8, Y9, Y16 | Y9 = gesture vs 3-button navigation. |
| Large window (emulator only) | L9, B9, C4, C10, K13, Y2 | D-13 says no simulators for matrix rows and D-06 lists only 200% font, TalkBack, process-death and IME variants for tier 1. Recommend treating these six as "run if time allows" on the existing AVD or a Resizable AVD, and confirm with the user (Open Question 4). |

Tier 2-4 rows (S*, E*, M*, N*, X*, T*, remaining Y*) are run if time allows and are triaged by D-07 only.

### B. Cross-device rows left in place in STATE.md and the UAT files (the QA-02/QA-03 matrix proper)

| Source ID | Content | Direction | Money? | Prior evidence |
|---|---|---|---|---|
| P7 SC-1 (07-HUMAN-UAT 3) | Payment Methods: add card (PaymentSheet setup mode), set default, remove. Re-scope: live real card, not 4242 | D1 (Android client) | no charge | none (blocked) |
| P7 SC-2 (07-HUMAN-UAT 4) | Short-lead paid booking within 6 days: pick saved card, Request Sent shows hold, iOS expert receives request; dismiss sheet = silent cancel; airplane mode during confirm then retry has no duplicate booking | D1 | auth hold | none |
| P7 SC-3 | No-saved-card gate "Add a Payment Method". The long-lead half is DESCOPED (a slot >6 days out is dimmed) | D1 | no | none |
| P7 SC-4 (07-HUMAN-UAT 2) | 3DS challenge on the connected account passes and booking confirms; cancelling the challenge shows "Payment wasn't completed. Your card wasn't charged." | D1 and D2 (iOS client's 3DS) | auth hold | none; re-scope to a real 3DS card |
| P7 SC-5 (07-HUMAN-UAT 6) | JPY no decimals (no JPY topics exist in production, so not runnable live: record as not run), rotation during sheet/challenge launches neither twice | D1 | no | none |
| P8 SC-1 to SC-4 (08-HUMAN-UAT 1-4) | My Bookings realtime, Upcoming Details, Cancel (free and held paid, with comment; iOS expert sees it), reschedule response from an iOS-proposed reschedule, `PAYMENT_METHOD_REQUIRED` recovery | D1 | paid cancel = void | none (11-UAT 2 passed the expert side) |
| P9 SC-1 (09-HUMAN-UAT 1) | Join from Upcoming Details (via Explore and via My Bookings), My Bookings card, Today's Sessions; mic prompt deny once then permanently; Skype/Zoom opens native app or Custom Tab | D1 and D2 | no | partial |
| P9 SC-2 (09-HUMAN-UAT 2) | 5+ minutes backgrounded, screen off, two-way audio, notification title and chronometer, notification tap after task swipe, End call from notification. Replaces FND-14 | D1 and D2 | no | none |
| P9 SC-3 (09-HUMAN-UAT 4) | Pill across tabs; Client/Expert switch, logout, add-payment-method and Cancel guards; predictive back minimizes | D1 | no | none |
| P9 SC-4 (09-HUMAN-UAT 5) | Peer hang-up shows "Waiting for other person"; rejoin; final-5-minute amber state and banner | D1 and D2 | no | 11-UAT 7 passed `remote joined` only |
| P9 SC-5 (09-HUMAN-UAT 6) | Full paid session end to end, `confirm-connection` exactly once (backend logs) | D1 (and D2 if time) | **real capture** | blocked until now |
| P9 sweep (09-HUMAN-UAT 7, 8, 9) | Bluetooth headset routing, incoming GSM call during a call, Samsung battery/sleeping-apps, CallStyle product decision, review-fix checks WR-05 (refused foreground start on API 34+), WR-06 (notification tap with a Custom Tab open: single MainActivity), WR-07 (unopenable meeting link: "Couldn't open meeting", no confirm), WR-09 (90-minute booking rejoinable after start+60 min) | D1 | no | none |
| P9 Android 17 (09-HUMAN-UAT 3) | `adb shell cmd audio set-enable-hardening enable` on `Medium_Phone_API_37.0`; targetSdk 37 local-network permission does not block Agora | emulator | no | none |
| P11 (a) | Expert Dashboard, payout onboarding card opens Custom Tab, return refreshes status | D2 | no | 11-UAT 3 passed on devDebug |
| P11 (b) | iOS client request appears; accept with and without message; Add to Calendar; decline four reasons; expired request read-only; accept a request the client cancelled meanwhile = ALREADY_PROCESSED | D2 | no | 11-UAT 1 (partly) |
| P11 (c) + G-11-1/5/6 (11-UAT 5, 6, 7; 09-HUMAN-UAT 1, 2, 5) | Join from Dashboard card, Upcoming card, Summary with iOS client, two-way audio, mute, speaker, leave and rejoin. **Capture** `remote joined elapsed=` and `remote audio state=2`. Rejoin and mute/speaker/leave were not proven by 11-UAT 7 | D2 | no | 11-UAT 7 passed (2026-10-27) |
| P11 (d) | iOS client proposes a reschedule; accept and decline from Summary; SLOT_UNAVAILABLE with two back-to-back bookings (30-minute buffer); expert cancel of a free and a held paid booking | D2 | paid cancel = void | 11-UAT 2 passed (free) |
| P12 (a)-(d) | Chat cross-client: live both ways, typing, Read, Load earlier, booking system rows live, failed bubble retry, report/block/unblock | D1 and D2 | no | 12-UAT 1-5 all passed on devDebug |
| P13 (a)-(g) + release checklist step 10 | Permission prompt once, `device_tokens` row platform=android and removed on logout, five channels, taps from a **killed** and running app route correctly, mode switch first, no banner for the open thread, notifications list, Write Review | D1 and D2 | no | 13-UAT 1, 4-7 passed on devDebug; (f) internal-track install skipped |
| P14 (a) / 14-UAT 1 | Payouts > Set up payouts > Stripe page in Custom Tab > `clickme://connect/return` returns and refreshes; expired link > `/refresh`; forged intent only reloads | D2 (Android expert) and D1 (iOS expert does the same on iOS) | no | none |
| P14 (d) / 14-UAT 2 | Delete a throwaway account: Warning > Reason > Delete > Farewell > Back to Login; then iOS and a fresh Android login both reject it; expert with upcoming bookings sees 409. Store-policy relevant (D-07) | D1 and D2 | no | none |
| 15-UAT tests 1-7 | SC-4 TalkBack runs (L1/B1/C1/K1), Y1-Y7/Y11, large windows, Y12, SC-1 states, M8/D6, B4/D7 | overlaps tier 1 | no | none |

### C. Items from `15-REVIEW-FIX.md` that need a device (assign to existing rows)

| Fix | Device check | Attach to |
|---|---|---|
| WR-A01/A02 (53e9345d): call-resume vs explicit push-tap destination; marker survives until consumed | Start a call, `am force-stop`, then **tap a push** before reopening: the push destination wins and the "You were on a call" note is not shown over it; a background FCM wake-up that dies before UI keeps the marker | C7, C8 |
| WR-A03 (skipped, ValidatedField contentDescription may shadow the typed value) | TalkBack speaks both name and value on a filled field. If only the label is spoken, apply the fallback in Gap list below | L3 |
| WR-B01 (633fe822): refresh-failed snackbar above the IME in Chat Thread and Conversations | Keyboard up, trigger a failed refresh: snackbar sits above keyboard and composer | K7, K11 |
| WR-B02 (0087213a): peer announcement not repeated; identical back-to-back texts re-announced | Peer sends the same text twice; delete a failed send of mine: peer message is not re-spoken | K4 |
| WR-A04 | Choose a different day with the same slot count: heading re-announces | B3 |
| WR-B06 (skipped) | Booking Confirmed re-grabs focus on recreation: accepted by design, only note | X5 (tier 3) |

### D. Gap list for Phase 16 (checklist section 13) mapped to scope

| Gap item | Tier | In Phase 16? |
|---|---|---|
| `androidTest` + `enableAccessibilityChecks()` suite | 1 | **No.** CONTEXT Deferred Ideas says after release. |
| Stripe-card rows | 1 | Yes, rows P7 SC-1 to SC-5 above (live). |
| Live-call rows with iOS peer (incl. Android 17 hardening, Bluetooth, GSM call, Samsung battery, expert Join, 09-HUMAN-UAT) | 1 | Yes (P9 rows, G-11 rows). |
| Cross-device chat, booking and request rows | 1 | Yes (P8, P11, P12, P13 rows). |
| Photo picker on API 26-32, avatar crop on API 26-32 emulator | 2 | No (tier 2; the AVD list has only API 37, with an `android-34` system-image dir [VERIFIED: ls]). |
| Day cells 45.7 dp (B4) | 1 | Yes, row B4 on the Samsung. Fix only if taps miss (reduce card padding). |
| TalkBack focus after Retry on detail screens (M8) | 2 | Tier 2: log only. |
| C2 pill-first (conditional fix: focus request on the pill) | 1 | Yes, conditional. |
| L3 name+value (conditional fix: add name to semantics without hiding the value) | 1 | Yes, conditional. |
| K3 duplicate bubble speech (conditional fix: drop Text from merged node) | 1 | Yes, conditional. |
| Book Now scroll-footer variant under 480 dp tall | 1 | No (unreachable on portrait-locked phones, checklist text). |
| Cap Farewell "Back to Login" | 4 | No. |
| Wider tester group + killed-app push in the matrix | 1 | Killed-app push yes; wider group is the accepted D-10 gap. |
| Portrait lock review after Play pre-launch reports | 2 | No (needs Play data). |

## Live-mode Stripe and Connect Constraints

### Price floor (corrects D-03)
- Backend refuses paid topics and bookings below `MIN_PAID_SESSION_MINOR_UNITS`, default **500 minor units = CA$5.00** (also USD), via `422 PRICE_BELOW_MINIMUM` on topic create/patch and as a booking backstop on the payment-intent path. [VERIFIED: pricing.js header "Default $5.00 CAD/USD (500 minor units), env-configurable via MIN_PAID_SESSION_MINOR_UNITS" and `DEFAULT_MIN_PAID_SESSION_MINOR_UNITS = 500`; STRIPE-INTEGRATION and PRICING-LIMITS.md sections 1, 3]
- Stripe's own floor is separate: **0.50 CAD** (`AMOUNT_BELOW_MINIMUM` is the booking-flow error for that) [CITED: docs.stripe.com/currencies "0.50 CAD"; STRIPE-INTEGRATION.md:385 "Total under Stripe's 50-cent floor"].
- Whether production overrides the 500 default cannot be verified from the repo (only `.env.example` mentions it, `MIN_PAID_SESSION_MINOR_UNITS=500`). The live value is visible in the app: the Topic Editor reads `pricing_limits` from `GET /me` [VERIFIED: TopicPrice.kt:25-28, MeData.kt:23] and the server `PRICE_BELOW_MINIMUM` message is display-ready ("A paid session must be at least CA$5.").
- Planner action: runbook prerequisite "read the live floor in the expert Topic Editor (account A); the paid-topic price for the matrix is that floor", budget about CA$5 per authorization and per captured session. Also note `duration_minutes` is a multiple of 30 [CITED: STRIPE-INTEGRATION.md section 2.1], so price per 30 minutes is the only knob.

### Authorization, void and capture (corrects D-04)
- `POST /bookings/confirm` authorizes only (`capture_method: manual`); capture happens ~15 minutes after the session ends; a decline, expiry or cancel releases the hold [CITED: STRIPE-INTEGRATION.md sections 2.2, 2.5].
- Client cancel: the controller reads the live PI status; `requires_capture` (and similar uncaptured states) -> `cancelPI` (void, `refundAmountMinorUnits = 0`); `succeeded` -> `createRefund` per the 24-hour binary rule [VERIFIED: clientBooking.controller.js:882-922; refund.js header "D-05: Cancelling pending_approval (uncaptured) voids authorization = full release. This utility is for CONFIRMED (captured) bookings only"]. STATE.md already records that the "Full refund / No refund within 24 hours" lines are dormant (Backend notes, Phase 8).
- Consequence for the runbook: every **paid cancel, decline, and expiry** row yields a **void ($0 and no Stripe fee)**; the in-app *refund* path is only exercised on a captured booking, which the UI does not offer to cancel (session complete). So: (1) label those rows "void" not "refund"; (2) exactly the completed paid sessions (P9 SC-5; at most one per direction) create real captures; (3) the end-of-run refund checklist covers only those, refunded by hand in the Stripe dashboard on the **connected account**; (4) Stripe's original processing fee is not returned on refund [CITED: docs.stripe.com/refunds "Stripe's processing fees from the original transaction aren't returned"]; (5) a direct-charge refund debits the connected account; if its available balance is insufficient the card refund stays pending until covered [CITED: docs.stripe.com/refunds]. The backend's own `createRefund` passes `refund_application_fee: true` [VERIFIED: stripe.js:381]; a dashboard refund on the connected account may not refund the platform application fee, so check the application-fee line after refunding [ASSUMED].
- Open holds are a real-world side effect: a booking left in `pending_approval` keeps a hold on the user's real card until accept/decline/expiry (authorization lifetime about 7 days [CITED: STRIPE-INTEGRATION.md section 2.5 "~7-day hold"]). The runbook closing step must cancel or decline every leftover paid request.
- The cap: paid sessions bookable only up to 6 days ahead (`LONG_LEAD_REQUIRES_SAVED_CARD`, `LONG_LEAD_NOT_AVAILABLE`, `RESCHEDULE_TOO_FAR_AHEAD`) [CITED: STRIPE-INTEGRATION.md section 0.9]. Paid slots are chosen inside the next 6 days, and a call row needs the booking inside the join window, so create a paid booking for a slot soon, accept it, and join within the window. The code join window is 120 min before / 120 min after the scheduled time while docs say 10/0 (STATE Backend notes, Phase 9; the client never gates on it, early taps get `403 SESSION_NOT_ACTIVE`). If the backend tightens it before release, same-day paid session timing changes.

### 3DS in live mode (D-02)
- The backend sets no `request_three_d_secure` anywhere [VERIFIED: grep of `src/` for `request_three_d_secure|three_d_secure|payment_method_options` returned no match], so Stripe's default "automatic" applies: a challenge happens only if the issuer (or a Radar rule on the account) asks for it. Neither app can force it.
- Confirm happens on-session on the server; if the bank wants SCA the server returns `402 PAYMENT_REQUIRES_ACTION` with `detail.client_secret` and `detail.stripe_account`, creating no booking; the client runs the challenge against that connected account, then retries confirm with the same `booking_attempt_id` [CITED: STRIPE-INTEGRATION.md section 2.4]. Android: `PaymentLauncher.handleNextActionForPaymentIntent` keyed by `challengeAccount` [VERIFIED: PaidFlowStripeHost.kt:48-52, 114]. This path has never run on a device (Pitfall 16 note in that file's KDoc and STATE: "the saved-card 402 PAYMENT_REQUIRES_ACTION path was never reproduced").
- Practical: ask the user now which real card challenges. Cheap pre-check: Payment Methods > Add Payment Method runs a SetupIntent through PaymentSheet with no charge; a challenge at setup shows the bank is SCA-enforcing (SetupIntent limit: 10 per 15 min per user, STATE). [challenge-at-setup behaviour: ASSUMED] If no card challenges in Canada, the realistic candidates are an EEA/UK-issued card (SCA applies) or a Radar rule requesting 3DS; creating a Radar rule is a Stripe dashboard action for the user. If nothing works the row is "not run" and VERIFICATION must say so (D-02 requires a real 3DS card to claim the criterion).
- Decline row: use a real card that fails (insufficient funds), or record as not run; expect `402 PAYMENT_FAILED`, same `booking_attempt_id`, choose another card [CITED: STRIPE-INTEGRATION.md section 2.4 retry table].
- iOS direction (QA-03): the iOS client runs the same server flow; iOS shows no alert when 3DS is cancelled (STATE iOS follow-ups) - log, do not block.

### Connect onboarding link, return and refresh (D-05)
- `POST /expert/connect/onboard` reuses the account when a `payout_accounts.stripe_connect_account_id` exists and returns a fresh single-use link (expires about 5 minutes); live-mode account **creation** is gated by `CONNECT_NOT_RATIFIED` (403) unless `CONNECT_ACCOUNT_TYPE_RATIFIED=true`, but an expert who already has an account never reaches that check [VERIFIED: connect.controller.js:62-86 (Step 2a creates, "Step 2b: Existing account — reuse; do NOT create a second account"); stripe.js:580-585 `isLiveMode = secretKey.startsWith('sk_live')` ... `'CONNECT_NOT_RATIFIED'` inside `createConnectAccount`]. The user's expert already has a live charges-enabled account (D-03), so the gate does not apply to the matrix.
- The link is `account_onboarding` type [VERIFIED: stripe.js:612-617]. For an already-complete account Stripe's page content is not documented in what was read; the matrix bar is only "link opens and the return/refresh deep link is handled", so record whatever Stripe shows (an immediate return is acceptable). [Stripe behaviour for complete accounts: ASSUMED]
- Stripe redirects to `return_url` when the user "leaves or completes" the flow, and to `refresh_url` when the link is expired, previously visited, or invalid [CITED: docs.stripe.com/api/account_links/create]. So the return deep link is never proof of completion (also stated in STRIPE-INTEGRATION 3.1). To exercise `/refresh`: open the link, wait past the expiry (about 5 minutes), then act on the tab; or re-open a used link.
- Both URLs are https on the backend and 302 to `clickme://connect/return` and `clickme://connect/refresh` [VERIFIED: app.js:223-224]. iOS completes it inside an `ASWebAuthenticationSession` keyed on the `clickme` scheme and also declares Universal Links (AASA) [VERIFIED: app.js:192-222 (AASA object and the comment "completes onboarding inside an ASWebAuthenticationSession keyed on the clickme:// custom scheme")]. Android handles it with `ConnectReturnActivity` (exported, VIEW/BROWSABLE, scheme `clickme`, host `connect`) which maps `/return` -> `ConnectReturn.Done` and `/refresh` -> `ConnectReturn.LinkExpired`, posts the Dashboard and Payouts reload slots, then relaunches MainActivity (CLEAR_TOP|SINGLE_TOP) [VERIFIED: AndroidManifest.xml:41-57; ConnectReturnActivity.kt:34-83]. No Android App Link exists.
- **Residual risk (RESEARCH A1 of Phase 14, still unverified on a device):** whether Chrome Custom Tabs follows the server 302 into a custom scheme. If it does not, the ON_RESUME reloads still refresh status but the `/refresh` recovery never fires; the fix would be a backend HTML bridge page or App Links. This is a legitimate D-07 candidate only if it prevents payouts setup entirely; otherwise log and defer.
- Forged-intent check (exported component): `adb -t T shell am start -a android.intent.action.VIEW -d clickme://connect/return` must only reload, never mark anything complete [CITED: 14-UAT / STATE Phase 14 (a)].
- "In both directions": onboarding is expert-only. Run it on **Android as expert A in QA-03** and on **iOS as expert A in QA-02**; Android's role in QA-02 is to see the result (the expert's `is_bookable`/charges_enabled paid topic still bookable from the Android client).

## Device Log Capture (adb with `-t <transport_id>`)

Resolve once per session: `ADB=~/Library/Android/sdk/platform-tools/adb`; `$ADB devices -l` and read `transport_id:N` for the SM_A075F line (it may be listed twice over wireless ADB, as in Phase 2). `T=N`. All commands below use `$ADB -t $T`. The Phase 11 serial was `R8YYC1QFA6H` [CITED: STATE.md G-11-5]. Device at research time: none attached.

| Purpose | Command |
|---|---|
| Prove the installed build (do this before every re-test) | `$ADB -t $T shell dumpsys package com.uptrendinvestments.clickmeapp \| grep -E "versionCode\|versionName\|lastUpdateTime\|firstInstallTime\|installerPackageName"` (Play install shows `installerPackageName=com.android.vending`; `pm list packages -i` also prints the installer) [dumpsys fields: ASSUMED; `lastUpdateTime` use is established in STATE G-11-5 (1)] |
| Clean slate | `$ADB -t $T logcat -c` |
| Rolling capture to a file (background) | `$ADB -t $T logcat -v threadtime -s Realtime ClickMeCall CallService RtcEngine AndroidRuntime > ~/ClickMe-release/logs/<row>.log` (app tags: `Realtime`, `ClickMeCall`, `CallService`, `TokenStore` [VERIFIED: grep of `const val TAG`]) |
| Crash buffer | `$ADB -t $T logcat -d -b crash \| grep -c com.uptrendinvestments.clickmeapp` (expect 0) |
| Socket state | `grep "state=" ` in the Realtime capture: `state=Connecting` then `state=Connected`; backgrounding logs `Disconnected` about 1 s later (STATE Phase 3 SC-3/lifecycle) |
| Remote peer in a call | `ClickMeCall` lines `remote joined elapsed=`, `remote audio state=2`, `remote offline reason=` [VERIFIED: AgoraManager.kt:90-116] |
| Call/background evidence | `$ADB -t $T shell dumpsys audio > <row>-audio.txt` while backgrounded (audio focus, hardening); `$ADB -t $T shell dumpsys activity services com.uptrendinvestments.clickmeapp` (CallService foreground) [ASSUMED]; `$ADB -t $T pull /sdcard/Android/data/com.uptrendinvestments.clickmeapp/files/agoraapi.log` (channelProfile:0, STATE G-11-5 (3)) |
| Push posted while killed | Confirm killed: `$ADB -t $T shell pidof com.uptrendinvestments.clickmeapp` prints nothing after swiping the task away (swipe from Recents, **not** `force-stop`, which blocks FCM delivery to the stopped app [ASSUMED known Android behaviour]). After the push: `$ADB -t $T shell dumpsys notification --noredact \| grep -A25 "pkg=com.uptrendinvestments.clickmeapp"` shows the posted notification and its channel [ASSUMED]. The app itself writes no log for a system-displayed push (privacy design), so this is the only device evidence. |
| Samsung battery state | One UI Settings > Battery > Background usage limits > Sleeping apps / Deep sleeping apps must not contain ClickMe; `$ADB -t $T shell am get-inactive com.uptrendinvestments.clickmeapp` for App Standby [CITED: developer.android.com/training/monitoring-device-state/doze-standby shows `am set-inactive`/`get-inactive` and `dumpsys deviceidle force-idle`]; `am get-standby-bucket` [ASSUMED] |
| Doze test during a call | `$ADB -t $T shell dumpsys deviceidle force-idle` then `... unforce` [CITED: same page]; this is a stress test, not a normal case |
| Android 17 hardening (emulator) | `adb shell cmd audio set-enable-hardening enable` on `Medium_Phone_API_37.0` (STATE Phase 9 open item) |
| Forged Connect return | `$ADB -t $T shell am start -a android.intent.action.VIEW -d clickme://connect/return` |
| UI proof for R8 smoke | `$ADB -t $T exec-out uiautomator dump /dev/tty \| grep -c 'Log in'` (02-15 Task 3) |

**Release build limits:** OkHttp logging is `debugImplementation` only [VERIFIED: build.gradle.kts:176], so there is no `ClickMeHttp` tag in prodRelease; API-level proof (confirm-connection once, join, capture) must come from Railway logs or the Stripe dashboard. `Log.i/w` in `Realtime`, `ClickMeCall`, `CallService` remain in release (no strip rule). Never paste a captured file containing tokens into the repo; the app logs ids and codes only, but check before committing.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Verifying the AAB signature | A script that unzips and parses certs | `jarsigner -verify` + `keytool -printcert -jarfile` compared to `keytool -list -v` of the keystore | Standard JDK tools; apksigner is APK-only [CITED: developer.android.com/tools/apksigner]. |
| Reading versionCode/google_app_id from a build | Parsing the manifest by hand | `aapt2 dump badging` / `aapt2 dump resources` | Already used in the Phase 13 checklist. |
| Forcing 3DS | A debug flag or backend change to request 3DS | Real SCA-enforcing card / Radar rule | Backend change would be new code outside D-12 unless it blocks the release. |
| Refunding the live charges | A custom refund tool or `createRefund` script | Stripe dashboard refund on the connected account (bulk refund supported for full refunds) [CITED: docs.stripe.com/refunds] | Auditable and reversible; never run `scripts/live-cutover-cleanup.js` (07-LOCAL-STRIPE-TESTING safety rules). |
| Deriving "was this fixed" | Trusting `assembleDevDebug` output | `dumpsys package ... versionCode/lastUpdateTime` on the device | Stale-build lesson from Phase 11. |
| Matrix completeness check | Reading the runbook by eye | A grep loop over row IDs from `15-DEVICE-CHECKLIST.md` (Validation Architecture) | Cheap, deterministic. |

**Key insight:** this phase is verification of existing code. Resist building tooling; every mechanism needed is an adb/JDK/Stripe-dashboard command or a markdown table.

## Common Pitfalls

### Pitfall 1: Planning to a CA$1 paid topic
**What goes wrong:** The runbook assumes about $1 per paid booking (D-03). The backend rejects any paid topic or booking below CA$5.00 by default, so the user either cannot create the topic or the booking fails with `PRICE_BELOW_MINIMUM`.
**Why:** `MIN_PAID_SESSION_MINOR_UNITS` default 500 [VERIFIED: pricing.js]; CA$0.50 is only Stripe's floor [CITED: docs.stripe.com/currencies].
**How to avoid:** Prerequisite row reads the live floor in the Topic Editor; money estimates use that floor. If the user wants it cheaper, that is a backend env change (out of scope, D-12 only for blockers) - ask, do not assume.
**Warning signs:** `PRICE_BELOW_MINIMUM` text "A paid session must be at least CA$5."

### Pitfall 2: Expecting refunds from the cancel rows, and leaving authorization holds on the real card
**What goes wrong:** Rows labelled "refund" actually void (no money, no fee), so the refund path is never exercised; a booking left pending keeps an authorization on the user's real card for up to about 7 days; a completed paid session silently captures real money about 15 minutes after the end time (crons run in production).
**How to avoid:** Label rows void/capture precisely; list every paid booking id with its Stripe PI id and expected end state in the runbook's refund table; end of run: dashboard shows zero PaymentIntents in `requires_capture` for the matrix bookings; refund each captured one by hand (full refunds can be bulk-selected [CITED: docs.stripe.com/refunds]); expect the original Stripe fee to be lost.

### Pitfall 3: The 3DS row cannot be satisfied on demand
**What goes wrong:** The user's cards never trigger a challenge, so the "test cards including 3DS" criterion can not be met on the day.
**How to avoid:** Ask for the card now; pre-check with Add Payment Method (no charge); decide fallback (EEA/UK card or a Radar rule) before the first paid session; if still impossible record "not run" and say so in VERIFICATION (D-02). Do not substitute a Stripe test card (D-02) and do not add a backend flag without the user's consent.

### Pitfall 4: Installing the Play build over a build with another signature
**What goes wrong:** The sideloaded upload-key APK (checklist step 4.4) and any `devDebug` install share `applicationId` `com.uptrendinvestments.clickmeapp` but not the signing key (the Play-delivered APK is signed with Google's app-signing key) so Android refuses the update (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`-class failure; the debug-vs-release case is documented in 13-RELEASE-CHECKLIST step 4.4 and 02-15). The Play-over-upload-key case is the same Android rule [ASSUMED].
**How to avoid:** `adb -t T uninstall com.uptrendinvestments.clickmeapp` before the first Play install. Uninstalling wipes the session (EncryptedSharedPreferences) and the notification permission, so expect a fresh login and a fresh permission prompt. Never sideload again after the Play install (D-15: no per-fix sideloading).

### Pitfall 5: Re-testing a stale build
**What goes wrong:** Two Phase 11 re-tests ran builds that predated the fix (STATE G-11-5; 11-UAT notes).
**How to avoid:** Ledger row + `dumpsys package` proof (Pattern 3) before every re-test; Play can take a few minutes to update the tester's device, so open the Play listing and Update manually, then check `lastUpdateTime` [CITED: answer/9845334 "updates ... within a few minutes"].

### Pitfall 6: First-upload timing
**What goes wrong:** The test link takes hours to appear; the store listing shows a temporary app name for up to 48 hours; the opt-in link shows only when status is Published [CITED: answer/9845334]. A rollout may be blocked by unfinished App content (foreground-service declaration) [ASSUMED, 13-RESEARCH A6; web search could not confirm either way].
**How to avoid:** Upload on day 1; if blocked, save the draft, copy the blocker, complete the declaration drafts from `13-PLAY-CONSOLE-DRAFTS.md` (step 7), then roll out. Do the closed-testing check (13 step 8) early: the 12-testers-for-14-days rule is stated for **personal** accounts created after 2023-11-13 [CITED: answer/9845334]; this is an organization account, but confirm in the Console because a 14-day test would have to start by about Nov 29 to protect a Dec 11-13 release.

### Pitfall 7: Push token follows the account, not the phone
**What goes wrong:** Backend keeps one `device_tokens` row per account [CITED: 13-RELEASE-CHECKLIST step 10.2]; two test accounts shared one token earlier (07-LOCAL-STRIPE-TESTING (b)). After a direction switch a push can go to the wrong phone or nowhere.
**How to avoid:** Log out on the old phone, log in on the new, then check `device_tokens` (platform flips ios/android) before any push row. The iPhone build also needs working push (an Xcode-installed iOS build uses the APNs sandbox environment; whether the backend's FCM-to-APNs key covers it is iOS-side and unverified [ASSUMED]); if iOS push is broken, log owner `ios` and use chat/booking realtime instead for those rows.

### Pitfall 8: "Killed" versus "force-stopped"
**What goes wrong:** `am force-stop` puts the app in the stopped state where FCM does not wake it [ASSUMED]; the push row then false-fails. C7 intentionally force-stops (it simulates process death), push rows must not.
**How to avoid:** Swipe from Recents (CONTEXT Specifics), confirm with `pidof`, then trigger the push from the iPhone action.

### Pitfall 9: Call rows need a booking inside the window
**What goes wrong:** No in-window booking exists, or the paid session is more than 6 days out, or the request expired before the expert accepted.
**How to avoid:** Pre-flight: expert A sets availability for today, books slots soon, accepts promptly; one booking per call row group (free for audio rows; one paid for the end-to-end capture row). A 5+ minute background row needs a session long enough (30-minute topic); the client is cut off at scheduled end + 3600 s (CALL-07), which is not a concern for 5 minutes.

### Pitfall 10: Samsung battery management
**What goes wrong:** The call's audio or the socket dies when the screen is off; pushes are delayed on a "deep sleeping" app. 09-HUMAN-UAT 7 names Samsung sleeping-apps behaviour (Pitfall 14 of Phase 9).
**How to avoid:** Record the Samsung setting state before the 5-minute row; run once with defaults (the real-user case) and note any manual exemption needed (that would be a release note, not a code fix).

### Pitfall 11: Stale or wrong planning inputs
**What goes wrong:** Executors follow outdated text. Examples found: 02-15/02-16 describe a temporary Login screen and a debug "Force token refresh" button that no longer exist; STATE.md still lists checklist step 1 (backend push deploy) as open although production is at `8d4450f` [VERIFIED: `curl /health` returned `{"status":"success","commit":"8d4450f"}`; `git merge-base --is-ancestor 92b4827 8d4450f` true; `git log origin/main..master` empty]; the Phase 15 checklist header says `devDebug`; 07-LOCAL-STRIPE-TESTING is a sandbox runbook (do not use for matrix, D-01).
**How to avoid:** Runbook states the build under test (Play prodRelease, `versionCode` N) per row; mark checklist step 1 done in STATE; use 02-15/02-16 only for their R8 and upload commands.

### Pitfall 12: Build environment
**What goes wrong:** The full gate OOMs as one invocation; worktree executors lack `keystore.properties` and `local.properties`; `gradlew --stop` kills parallel executors.
**How to avoid:** Run lint and R8 separately with `-Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g` for non-test tasks (STATE Phase 12 note); do signed builds in the main checkout; backend edits sequential, no worktree, orchestrator commits (D-12); never `gradlew --stop`.

### Pitfall 13: Real money data in the repo
**What goes wrong:** Screenshots of the Stripe dashboard or the PaymentSheet leak card digits, PI ids with customer emails, or tokens in logcat slices.
**How to avoid:** Evidence goes to `~/ClickMe-release/`, not the repo, unless redacted; the bug log stores ids and codes only; `git ls-files | grep -cE '\.jks$|keystore\.properties'` stays 0.

## Code Examples

### versionCode bump + ledger (hand edit, one commit)
```kotlin
// app/build.gradle.kts, defaultConfig (existing literals at lines 76-77, VERIFIED)
versionCode = 2          // N+1 per Play upload; ledger row appended in the same commit
versionName = "0.1.0"    // unchanged for QA builds
```

### `16-BUILD-LEDGER.md` row template
```
| versionCode | versionName | git sha | aab sha256 | uploaded (time) | Play status | installed on Samsung (versionCode, lastUpdateTime, installer) | rows re-run |
```

### Post-build checks (copy into the plan's verify steps)
```bash
JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin"
BT=~/Library/Android/sdk/build-tools/36.0.0
AAB=app/build/outputs/bundle/prodRelease/app-prod-release.aab
APK=app/build/outputs/apk/prod/release/app-prod-release.apk
"$JBR/jarsigner" -verify "$AAB" | grep -q "jar verified" && echo AAB-OK
"$BT/apksigner" verify --print-certs -v "$APK"          # [CITED: developer.android.com/tools/apksigner]
"$BT/aapt2" dump badging "$APK" | grep -E "package:|versionCode"
"$BT/aapt2" dump resources "$APK" | grep google_app_id   # must print a line, else no push
git ls-files | grep -cE '\.jks$|keystore\.properties'    # must print 0
```

### Runbook coverage check (Nyquist-style, deterministic)
```bash
RB=.planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md
for id in L1 L2 L3 L4 L5 L6 L7 L8 B1 B2 B3 B4 B5 B6 B7 B8 B10 C1 C2 C3 C5 C6 C7 C8 C9 \
          K1 K2 K3 K4 K5 K6 K7 K8 K9 K10 K11 K12 Y1 Y3 Y4 Y5 Y6 Y7 Y8 Y9 Y11 Y12 Y16; do
  grep -qw "$id" "$RB" || echo "MISSING $id"
done
```

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| `minifyEnabled`/`proguardFiles` in the release build type | AGP 9 `optimization { enable = true; packageScope = ... }` [VERIFIED: build.gradle.kts:127-130] | R8 is scoped to androidx/kotlin/kotlinx here, lowering stripping risk for Stripe/Agora/Moshi. |
| Stripe: client-side confirm | Server confirms (Path A); client only completes 3DS on a 402 [CITED: STRIPE-INTEGRATION.md section 1, 2.4] | The Android 3DS path is device-unproven. |
| Capture at accept | Authorize at confirm, capture after the session [CITED: section 2.5] | Cancel rows void; only completed sessions charge. |
| Paid long-lead bookings | Retired: 6-day cap [CITED: section 0.9] | Paid rows must be short-lead. |
| Play: sideload-signed bundles | Play App Signing: upload key vs Google app-signing key [CITED: 13-RELEASE-CHECKLIST step 5] | Device installs from Play carry Google's certificate. |

**Deprecated/outdated:** the Phase 2 spikes (`:spikes:stripe`, `:spikes:agora` - Agora spike already deleted; the Stripe spike plan 02-09 is replaced by live rows); 07-LOCAL-STRIPE-TESTING for matrix use.

## Runtime State Inventory

Not a rename/refactor/migration phase. Omitted. (State that does persist across the QA work: `device_tokens` rows per account, live Stripe PaymentIntents/holds, the Play app registration and upload key, saved cards on account B's Stripe customer; all are covered by the runbook's cleanup list.)

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Production does not override `MIN_PAID_SESSION_MINOR_UNITS` below 500 (so the cheapest paid session is CA$5.00) | Live-mode Stripe constraints | If lower, the user may already have a cheaper topic; budget smaller. If higher, budget bigger. Read it from the Topic Editor. |
| A2 | Add Payment Method (SetupIntent) will show a 3DS challenge for an SCA-enforcing card, making it a no-charge pre-check | 3DS | Pre-check is inconclusive; the first paid booking is then the first real test. |
| A3 | A dashboard refund on the connected account may not refund the platform application fee | Void vs refund | Small extra money left; check the application-fee line after each refund. |
| A4 | Stripe's `account_onboarding` page for an already-complete Express account returns quickly or shows a review page | Connect | The "link opens" evidence differs from expectation; record what is shown. |
| A5 | Play-signed APK over an upload-key-signed sideload fails the same way as debug-over-release | Pitfall 4 | If it works, the uninstall is merely harmless; if the user skips it and it fails, they lose minutes. |
| A6 | An incomplete foreground-service/App content declaration may block the internal rollout | Pitfall 6 | If it never blocks, step 7 can wait for Phase 17; if it blocks, a day of declaration work is on the critical path. |
| A7 | Organization accounts are exempt from the 12-testers/14-days closed test (page speaks only of personal accounts) | Pitfall 6 | If not exempt, Phase 17 slips 14+ days. Console check is Phase 13 step 8 (still open). |
| A8 | `am force-stop` blocks FCM until the next launch; `pidof` empty after a Recents swipe means the process is gone | Pitfall 8, log capture | Push rows may false-pass/false-fail; mitigate by also checking `dumpsys notification`. |
| A9 | `dumpsys notification --noredact`, `dumpsys activity services <pkg>`, `am get-standby-bucket`, `pm list packages -i`, `dumpsys package` field names behave as described on One UI / API 36 | Device log capture | Alternate commands needed; Claude can adapt live. |
| A10 | An Xcode-installed iOS build can receive the backend's FCM pushes (APNs key covers sandbox and production) | Pitfall 7 | iOS-direction push rows cannot run; log owner `ios`. |
| A11 | Tier-1 row selection by flow (inventory in Matrix Source Rows A) matches the user's intent, including excluding large-window rows | Matrix Source Rows | Wrong scope: either too much effort or a missed must-pass row. Confirm with the user. |
| A12 | `aapt2 dump badging` prints `versionCode` and `app/build/outputs/mapping/prodRelease/mapping.txt` is the mapping path | Release pipeline | Trivial path adjustment. |
| A13 | Manual upload is preferable to Play Developer API automation for 3-6 uploads | Alternatives | Slightly slower uploads. |

## Open Questions

1. **What is the live paid-topic floor, and does the user accept about CA$5 per authorization/capture instead of about $1?**
   - Known: default floor CA$5.00; Stripe floor CA$0.50; only env override can lower it (A1).
   - Unclear: the production env value.
   - Recommendation: planner adds a pre-flight task to read it in the expert Topic Editor, and one question to the user before session day; update the runbook budget.
2. **Which real card will force a 3DS challenge, and a second card that declines?**
   - Known: nothing in app or backend can force it.
   - Unclear: the user's cards and issuer behaviour.
   - Recommendation: user checkpoint in the pre-flight plan; fallback to "not run" recorded in VERIFICATION.
3. **Are the large-window rows (L9, B9, C4, C10, K13, Y2) in the must-pass set?**
   - Known: D-06 names 200% font, TalkBack, process-death and IME variants; D-13 says no simulators for matrix rows; D-10 allows the emulator for non-hardware rows.
   - Recommendation: treat as run-if-time and confirm.
4. **Is the Play closed-testing requirement shown on the organization account?** 13 checklist step 8 is still open and is calendar-critical for Dec 11-13. Do it during the first Play Console session.
5. **Does the foreground-service declaration block the internal rollout (A6)?** Found out at first upload; have `13-PLAY-CONSOLE-DRAFTS.md` ready.
6. **Is iOS push working on the user's iPhone build (A10)?** Check in pre-flight with one chat or booking push to the iPhone.
7. **Who creates the throwaway account for the account-deletion row (P14 (d))?** It needs a real email on production; the user signs up on Android (also exercises the signup flow) and deletes it.
8. **CallStyle ongoing-call notification decision (09-HUMAN-UAT 8)** is a product call owed "before release"; ask once during the call session.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Android Studio JBR (java, keytool, jarsigner) | Key creation, AAB verify | ✓ | JBR `.../jbr/Contents/Home` (keytool, jarsigner present) | — |
| System `java` | n/a (use JBR) | ✓ | 18.0.1.1 | Gradle must use the JBR `JAVA_HOME` (build-env notes) |
| build-tools 36.0.0 (apksigner, aapt2, zipalign) | Verification | ✓ | 36.0.0 (and 34.0.0) | — |
| adb | Log capture | ✓ | platform-tools | — |
| Samsung SM-A075F | All hardware rows | ✗ (not attached at research time) | API 36 per Phase 2 plan | None (D-13); attach by wireless ADB, resolve transport id |
| iPhone with production iOS app | All cross-device rows | Unknown (user) | `Environments.plist` `Current = Production`, `pk_live_51U8OTJE...` [VERIFIED: Environments.plist:5-6, 32] | None |
| Upload keystore `~/keys/clickme-upload.jks` + `keystore.properties` | Signed build | ✗ | — | Human step 3 of the Phase 13 checklist |
| `app/google-services.json` | Push in release | ✓ | package `com.uptrendinvestments.clickmeapp`, project `clickme-a006b` | — |
| Play Console organization account | Upload | ✓ per STATE (verified 2026-10-20) | — | — |
| Production backend | All | ✓ | `/health` commit `8d4450f` | — |
| Stripe live dashboard access (platform and connected account) | Holds, refunds | Unknown (user) | — | None; needed for D-04 |
| Supabase table editor, Railway logs | `device_tokens`, backend proof | Unknown (user) | — | Ask the user to read rows aloud |
| Real 3DS-challenging card; second declining card | D-02 | ✗ unknown | — | Row recorded as "not run" |
| AVD `Medium_Phone_API_37.0`, `android-37.0` image | Android 17 row, large window | ✓ | API 37 | — |
| API 26-32 emulator | Photo picker (tier 2) | ✗ (only API 37 AVD; `android-34` image dir present) | — | Tier 2, not run |

**Missing dependencies with no fallback:** the Samsung attached to the Mac, the upload keystore (human), Stripe dashboard access (user), the iPhone.
**Missing dependencies with fallback:** the 3DS card (record not run), API 26-32 emulator (tier 2).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + Mockito 5.24.0 + Compose ui-test + MockWebServer3 + kotlinx-coroutines-test [VERIFIED: libs.versions.toml, app/build.gradle.kts:191-197] |
| Config file | `app/build.gradle.kts` (`unitTests.isIncludeAndroidResources = true`); no `androidTest` source set (deferred) |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*<FixTest>' ktlintCheck` |
| Phase gate (per CONTEXT) | `JAVA_HOME=... ./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug`; full gate adds `testProdDebugUnitTest`, then separately `lintDevDebug lintProdDebug` and `assembleProdRelease` (with `-Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g`), plus `bash scripts/check-design-rules.sh` (about 80 s, printed `Design rules: OK` this session) and its self-test `bash scripts/check-design-rules-selftest.sh` |
| Estimated runtime | quick about 60 s; gate about 10 min |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| QA-01 | Every failed runbook row has a bug-log row with severity per D-07; each blocking fix has a regression test where a JVM test can reproduce it | unit (per fix) + doc grep | `./gradlew :app:testDevDebugUnitTest --tests '*<FixTest>' ktlintCheck`; `grep -c '^| BUG-' 16-BUG-LOG.md` vs failed rows | ❌ Wave 0: `16-BUG-LOG.md`, `16-MATRIX-RUNBOOK.md` |
| QA-01 | Fix build is the one on the device | device check | `adb -t T shell dumpsys package com.uptrendinvestments.clickmeapp \| grep -E "versionCode\|lastUpdateTime"` equals ledger | manual-assisted |
| QA-02 | Matrix passes, Android client and iOS expert (live card, 3DS, killed-app push, background call and rejoin, payout onboarding link) | **manual-only (human device matrix)**; user-confirmed Week 8 checkpoint | none; evidence by adb, Stripe dashboard, Railway, Supabase | ❌ runbook |
| QA-03 | Same, iOS client and Android expert | **manual-only** | none | ❌ runbook |
| D-06 | Every tier-1 checklist row appears in the runbook | doc grep | the coverage loop in Code Examples | ❌ runbook |
| REL-03 | Signed AAB verifies and is uploaded | tool checks + manual upload | `jarsigner -verify`, `apksigner verify --print-certs -v`, `aapt2 dump resources \| grep google_app_id`, `git ls-files \| grep -cE '\.jks$\|keystore\.properties'` prints 0 | commands only |
| REL-05 (track half) | Internal track build installs from Play | manual + `pm list packages -i` / `dumpsys package` installer line | installer is `com.android.vending` | manual |
| D-10 | Single-tester gap recorded | doc grep | `grep -ci "D-10" 16-VERIFICATION.md` > 0 and STATE entry | ❌ VERIFICATION |

### Sampling Rate
- **Per task commit:** quick command with the touched test classes plus `ktlintCheck`.
- **Per wave merge:** `testDevDebugUnitTest ktlintCheck assembleDevDebug`, then `bash scripts/check-design-rules.sh`.
- **Phase gate:** full gate green (both flavors, lint, `assembleProdRelease`, design rules) before each upload build; `/gsd-verify-work` additionally blocks on the user's matrix confirmation (D-09).

### Wave 0 Gaps
- [ ] `.planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md` - covers QA-02, QA-03, D-06
- [ ] `.planning/phases/16-cross-platform-qa/16-BUG-LOG.md` - covers QA-01 (D-11 columns)
- [ ] `.planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md` - versionCode and build proof
- [ ] `~/ClickMe-release/` directory for archived artifacts and logs (outside the repo)
- Framework install: none. Existing test infrastructure covers regression tests for fixes.

## Security Domain

`security_enforcement` is not set to false in `.planning/config.json` (the file only contains `workflow._auto_chain_active`), so this section applies.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (two live accounts; account deletion rows) | Existing app flows; do not reuse `appreview@clickme.test` (D-03). |
| V3 Session Management | yes | Logout deletes the push token row; JWT in EncryptedSharedPreferences (existing). Check on account switch. |
| V4 Access Control | yes | Exported `ConnectReturnActivity` treats the URI as a re-check signal only [VERIFIED: ConnectReturnActivity.kt:30-32, 71-72]; forged-intent row. |
| V5 Input Validation | limited | Deep-link parser reads scheme, host, exact path only. |
| V6 Cryptography | yes | Upload keystore: JDK `keytool` RSA 2048, passwords only in the gitignored file and a password manager; never hand-roll. |
| V8/V9 Data protection and communication | yes | Live card data and tokens must not enter the repo, bug log or screenshots; HTTP logging is debug-only. |
| V14 Configuration | yes | `keystore.properties`, `*.jks`, `google-services.json` gitignored [VERIFIED: .gitignore "Signing and secrets" block]; check `git ls-files` count is 0. |

### Known Threat Patterns for this phase
| Pattern | STRIDE category | Standard Mitigation |
|---------|----------------|---------------------|
| Upload key or passwords leaked or lost | Information disclosure / DoS | Gitignore, offline backup of jks and passwords; Play App Signing allows an upload-key reset |
| Real card data or PI ids in evidence files | Information disclosure | Evidence outside repo; redact; ids only in bug log |
| Forged `clickme://connect/*` intent | Spoofing | Parser is a no-data re-check signal; verify with the adb forged intent |
| Stray live authorization holds or captures on the user's card | Tampering (financial) | Refund table, dashboard sweep at end of every session |
| JWT or message text in logcat slices | Information disclosure | App logs ids/codes only; review before sharing |
| Accidental use of the store-review account or live-cutover script | Elevation / destructive | Hard rule in runbook header: never `appreview@clickme.test`, never `scripts/live-cutover-cleanup.js`, `DISABLE_CRONS` not applicable to prod |
| Publishing the wrong build (stale or debug-signed) | Tampering | Ledger sha256 and `jarsigner`/`apksigner` cert fingerprint match |

## Project Constraints (from CLAUDE.md)

- iOS code under `ios-reference/` is the behavioural source of truth; deviate only for Android platform conventions.
- Architecture is fixed: Jetpack Compose + Material 3, MVVM with Compose State, coroutines, Hilt, Retrofit + Moshi, EncryptedSharedPreferences for JWT, Firebase Messaging, Agora Android SDK, Stripe Android SDK with PaymentSheet; minSdk 26, targetSdk 37, compileSdk 37. Fixes must stay inside these.
- Mirror the iOS folder tree for any new screen files (fixes touch existing files, so mostly n/a).
- Backend contract is the same REST contract as iOS; authoritative specs in `backend-reference/docs/ios/`.
- Memory/standing rules that apply: JDK via the Android Studio JBR; worktree executors need `local.properties` and absolute iOS paths; never `gradlew --stop` in parallel executors; Samsung device; manual testing deferral is overridden for Phase 16 only (D-09).
- Backend edits (D-12): sequential, no worktree isolation, orchestrator commits.
- `scripts/check-design-rules.sh` (and self-test) is part of every fix plan's gate (CONTEXT code_context).

## Sources

### Primary (HIGH confidence, read this session)
- `/Users/LeonChen/Documents/ClickMe_Android/app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/keepRules/rules.keep`, `ConnectReturnActivity.kt`, `PaidFlowStripeHost.kt`, `gradle/libs.versions.toml`, `.gitignore`
- `.planning/STATE.md`, `.planning/REQUIREMENTS.md` (QA/REL/FND rows), `.planning/ROADMAP.md` (Phase 16, 999.1), `16-CONTEXT.md`
- `.planning/phases/13-.../13-RELEASE-CHECKLIST.md`, `13-UAT.md`, `.planning/phases/15-polish-hardening/15-DEVICE-CHECKLIST.md`, `15-REVIEW-FIX.md`, `15-UAT.md`, `15-CONTEXT.md`
- `.planning/phases/02-.../02-15-PLAN.md`, `02-CONTEXT.md`; `09-HUMAN-UAT.md`, `11-UAT.md`, `12-UAT.md`, `14-UAT.md`, `08-HUMAN-UAT.md`, `07-HUMAN-UAT.md`, `07-LOCAL-STRIPE-TESTING.md`
- Backend repo (read-only): `src/utils/pricing.js`, `src/utils/refund.js`, `src/controllers/clientBooking.controller.js` (882-922), `src/controllers/connect.controller.js`, `src/services/stripe.js`, `src/app.js` (192-224), `.env.example`; `backend-reference/docs/ios/STRIPE-INTEGRATION.md`, `PRICING-LIMITS.md`
- iOS: `ClickMe2026/Environments.plist`
- Live checks: `curl https://clickme-backend-production.up.railway.app/health` -> commit `8d4450f`; `design-rules` run -> OK

### Secondary (MEDIUM confidence, official docs fetched)
- https://support.google.com/googleplay/android-developer/answer/9845334 (internal testing: 100 testers, minutes vs hours, 48 h temp name, opt-in only when Published, personal-account testing rule)
- https://developer.android.com/studio/publish/versioning (versionCode rules, max 2100000000)
- https://developer.android.com/tools/apksigner (verify syntax; APK only)
- https://docs.stripe.com/currencies (CAD minimum 0.50)
- https://docs.stripe.com/refunds (void vs refund, fees not returned, direct-charge refunds debit connected account, dashboard cancel/refund, bulk refund)
- https://docs.stripe.com/api/account_links/create (return_url/refresh_url semantics)
- https://developer.android.com/training/monitoring-device-state/doze-standby (`am set-inactive`/`get-inactive`, `dumpsys deviceidle`)

### Tertiary (LOW confidence, marked ASSUMED in text)
- Web search on foreground-service declaration and internal-track blocking (inconclusive), Play signature-mismatch search (community only)

## Metadata

**Confidence breakdown:**
- Standard stack/release pipeline: HIGH - build files, checklist and docs read; Play timing MEDIUM (official page).
- Architecture (runbook grouping, direction/account model): MEDIUM - derived from CONTEXT plus checklist; user confirmation needed for scope (A11).
- Live Stripe behaviour: HIGH for backend code paths (void vs refund, price floor, no 3DS forcing), LOW for how a given real card and Stripe's onboarding page for a complete account behave (A2, A4).
- Pitfalls: HIGH for repo-derived, MEDIUM for OS/OEM behaviour (A8-A10).

**Research date:** 2026-10-30
**Valid until:** 2026-11-13 for Play/Stripe docs (fast-moving); repo facts are valid until the next commit touching `app/build.gradle.kts` or the backend `main`.

