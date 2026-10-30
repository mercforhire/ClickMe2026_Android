# Phase 16: Cross-Platform QA - Context

**Gathered:** 2026-10-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Prove that the Android app works with the iOS app in both directions on every money and session path, on the signed release build testers install from Play. The phase delivers:

- the signed release build on the internal track (this closes the 999.1 release items),
- one ordered matrix runbook, run with an Android client and iOS expert, then with an iOS client and Android expert,
- the tier-1 rows of `15-DEVICE-CHECKLIST.md`,
- a bug log with triage, plus fixes for the release-blocking bugs (QA-01, QA-02, QA-03).

No new features. Play Console release requirements and the production rollout belong to Phase 17.

</domain>

<decisions>
## Implementation Decisions

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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` § Phase 16 — goal and success criteria 1–3. § Backlog 999.1 is the Phase 2 release items this phase closes (D-08).
- `.planning/REQUIREMENTS.md` — QA-01, QA-02, QA-03. Also REL-03 and REL-05 (closed by D-08), and FND-13 and FND-14 (replaced by matrix rows, see D-08).

### Device checks and gaps
- `.planning/phases/15-polish-hardening/15-DEVICE-CHECKLIST.md` — the tier-1 rows (D-06), the "Decisions for the user" table, and the "Gap list for Phase 16" (Stripe-card, live-call and cross-device rows owned by this phase).
- `.planning/phases/15-polish-hardening/15-REVIEW-FIX.md` — the fixes that need device checks (WR-A01/A02 push vs. call resume, WR-A03 TalkBack row L3, WR-B01 snackbar above the IME, WR-B02 peer announcements).
- `.planning/phases/15-polish-hardening/15-UAT.md` — Phase 15 UAT, paused at test 1 (the SC-4 TalkBack flows). It overlaps the tier-1 rows.
- `.planning/STATE.md` § Blockers/Concerns — the deferred human-check lists for Phases 3–15, which hold the source rows for the matrix (Phase 9 call list, Phase 11 G-11-1, G-11-5 and G-11-6 still open, Phase 12 chat, Phase 13 push, Phase 14 payouts). § Pending Todos holds the report-to-iOS list (D-12).

### Release build and Play Console
- `.planning/phases/13-push-notifications-internal-testing/13-RELEASE-CHECKLIST.md` — the keystore, signed bundle and internal-track upload steps (D-08).
- `.planning/phases/13-push-notifications-internal-testing/13-PLAY-CONSOLE-DRAFTS.md` — Play Console text drafts (Phase 17 uses them; listed here for internal-track setup).
- `.planning/phases/13-push-notifications-internal-testing/13-CONTEXT.md` — D-20 to D-22 (Firebase project, the human checklist plan, solo tester).

### Money paths
- `.planning/phases/07-payment-methods-paid-booking/07-LOCAL-STRIPE-TESTING.md` — the sandbox runbook. It is not used for the matrix (D-01), but its safety rules still apply if any one-sided sandbox re-test is run. Never run `scripts/live-cutover-cleanup.js`. Keep `DISABLE_CRONS=true` on a local backend.
- `backend-reference/docs/ios/` — the backend contracts for earnings, payouts and the Connect onboarding link (D-05).

### iOS peer
- `/Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Environments.plist` — iOS `Current = Production` (live `pk_live`, production BaseURL). The matrix runs with this config unchanged (D-01).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/build.gradle.kts` release signing reads `keystore.properties` through `providers.fileContents`, and is unsigned when the file is absent. `keystore.properties` and `~/keys/` do not exist yet (D-08). `app/google-services.json` is present, so release builds get FCM.
- `scripts/check-design-rules.sh` (plus its self-test) is part of the gate for every fix plan.
- Phase gate: `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug`. The full gate adds `testProdDebugUnitTest lint assembleProdRelease`. It OOMs as one invocation, so run lint and R8 separately (see the build-env memory).

### Established Patterns
- Human-only steps go in one non-blocking checklist plan (Phase 13 D-21). Phase 16 keeps the checklist form, but the phase blocks on the result (D-09).
- Deferred device rows live in STATE.md and the per-phase UAT files. The runbook should cite source row IDs (for example the `15-DEVICE-CHECKLIST.md` IDs L3, B4, C2, K3) so results can be written back.
- Device re-tests must first install the new build and confirm it with `dumpsys package ... lastUpdateTime` (stale-build lesson from Phase 11).

### Integration Points
- `versionCode = 1` and `versionName = "0.1.0"` in `app/build.gradle.kts` need a bump policy before the second upload (D-15).
- Fixes for iOS- or backend-owned bugs (D-12) touch `/Users/LeonChen/Documents/ClickMe2026/` or `/Users/LeonChen/Documents/ClickMe2026 Backend/`, through absolute paths and sequential execution.

</code_context>

<specifics>
## Specific Ideas

- The user accepts real money moving during QA. Keep it to minimum-price topics on their own accounts, and refund through the in-app cancel paths.
- Push "from a killed app" means swiping the app away (not force-stop) on the Samsung, and the same on the iPhone. Samsung battery behaviour is part of the call-survival rows.

</specifics>

<deferred>
## Deferred Ideas

- An `androidTest` source set with `ui-test-junit4-accessibility` and `enableAccessibilityChecks()` on the four core flows (from the 15 Gap list). After release.
- A wider internal or closed tester group on mixed devices (the SC-5 / REL-05 gap, D-10). After release.
- A hosted test-mode staging backend for safe Stripe test-card runs. Not built for the MVP (D-01 chose live mode).
- Finishing a full Connect onboarding with a new expert (D-05). After release.
- Tier 2–4 device rows not run in Phase 16 (D-06). After release.

</deferred>

---

*Phase: 16-cross-platform-qa*
*Context gathered: 2026-10-29*
