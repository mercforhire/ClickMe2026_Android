# Phase 17: Play Store Release - Research

**Researched:** 2026-11-05
**Domain:** Google Play Console release (closed-test gate, policy declarations, store listing), plus three small Android code fixes (POL-02, BUG-22, BUG-34) and a signed-release build.
**Confidence:** MEDIUM-HIGH. Google's own Help pages were fetched this session and quoted. The weak spots are (a) whether the "organization accounts are exempt" claim holds (it does not match what Console showed on 2026-10-30), (b) paid tester vendor terms (vendor-stated), and (c) the root cause of the BUG-34 "does not scroll" symptom (needs a failing test first).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** First re-check the Play Console Dashboard for the production-access gate ("Apply for production" / "Get access to production"). On 2026-10-30, after the first internal upload, the gate was shown for this organization account. If it is still shown, run the 12-testers × 14-days closed test before applying for production. The release moves out by at least 14 days plus Google's review of the production-access application. That is accepted, because full iOS parity outranks the date. — **Reversibility:** costly — the 14 days cannot be shortened after the fact. Starting late pushes the release out one for one.
- **D-02:** The 12+ testers come from a **paid tester service**. Recruit a buffer above 12, so that drop-outs don't reset compliance. Research should compare the current services: cost, a guarantee that testers stay opted in for 14 days, and help with the production-access questionnaire. The user chooses and pays for the service. That is a human step.
- **D-03:** Testers only need to install the app, stay opted in and use it lightly now and then. No guided task list. Collect light feedback, enough to answer Google's production-access form (what testers did, what feedback came in, what changed).
- **D-04:** Fix builds may go to the closed track during the 14 days. The user's understanding is that Google counts opted-in testers, not builds, so the clock keeps running. **Research must confirm this against Google's current rule.** If an update does reset anything, the planner must schedule all code fixes (D-09) before the closed test starts.
- **D-05:** Payments: Stripe charges for paid live 1-on-1 expert voice sessions, not Play Billing. **Research checks the current Google Play Payments policy** (https://support.google.com/googleplay/android-developer/answer/9858738) for the exemption that covers real-time person-to-person services. The user confirms the answer before submission, so the plan needs a decision checkpoint. This closes Phase 13 release-checklist step 9. — **Reversibility:** costly — if the conclusion is wrong, the app can be rejected or get a policy strike, and moving to Play Billing would break parity with iOS and the backend.
- **D-06:** Account-deletion URL: **create a new web page**, for example `https://www.uptrendinvestments.net/clickme/delete-account.html`. The plan writes the page content: the in-app steps (Settings → Delete Account, which exists from Phase 14) and a request path that needs no app, by email to `support@clickme.app`. The page must also say what is deleted and what is kept, matching the backend deletion cascade. The user publishes it on the uptrendinvestments.net site, a human step. The URL goes into the Data safety form and the Console's account-deletion field. Also update the line in the Phase 13 drafts that says "until then the support email handles requests".
- **D-07:** App access, the reviewer login: **reuse `appreview@clickme.test`**, documented in `backend-reference/docs/APP-REVIEW-ACCOUNT.md`. It holds both the client and expert roles, has seeded sessions, and can be re-created after a reviewer deletes it. Its re-seed procedure covers Google reviewers too. The Console instructions must explain how to reach a call or booking, and that payouts are not set up. The password is in the backend doc. **Do not copy it into any planning file**. The user enters it in Console.
- **D-08:** Target audience: **18 and over only**. Content rating questionnaire: users interact (chat and voice between users), there are in-app purchases or payments (paid sessions), and there is no violence, gambling or similar content. The app does not join the Families program. — **Reversibility:** reversible — this can be changed in Console later.
- **D-09:** Fix these before the production build, ideally before the closed-test build so testers run them:
  - **POL-02:** move `BookingNoteViewModel`, `FeedbackViewModel` and `SearchExpertViewModel` to `SavedStateHandle`, so the note draft, the feedback form and the search query survive process death.
  - **BUG-22:** the call controls (Minimize, Chat, Mute, Speaker) are hidden behind the pinned End Call button in the last 5 minutes, when the banner and the time-left chip are showing. This happens at font scale 1.0.
  - **BUG-34:** the call controls can't be reached at 200% font with the largest display zoom, on the pre-join or waiting call screen.

  Use the Phase 16 bug log (`16-BUG-LOG.md`) for the details. iOS parity and the existing tests must stay green.
- **D-10:** The public release is **versionName `1.0.0`**. versionCode continues from 5 and goes up by one for every upload to any track. It is set in `app/build.gradle.kts`.
- **D-11:** Production rollout goes to **100% at once** when Google approves it. No staged rollout and no managed publishing. — **Reversibility:** costly — a rollout can be halted, but installs already done can't be pulled back. Only a higher versionCode can fix them.
- **D-12:** Countries at launch: **Canada and the United States** only. — **Reversibility:** reversible — more can be added in Console later.
- **D-13:** For the smoke test, the Android Play install **signs up as a new client**. The user's existing **iOS expert account** accepts the booking and joins the call. Only this direction is tested.
- **D-14:** The smoke booking uses a **free topic**. No money moves; the backend's Stripe is live, and real-card payments already passed in Phase 16.
- **D-15:** The smoke test runs **twice**:
  1. The full run, on the closed-track install from Play (the same binary that goes to production).
  2. A short repeat from the public Play listing once production is live. This covers success criterion 3.
- **D-16:** Store assets:
  - The **executor generates what it can**:
    - phone screenshots, captured with `adb` from the Samsung SM-A075F or an emulator (suggested: Explore, Expert Profile, Make a Booking, My Bookings, a chat thread, the in-call screen);
    - the 512 × 512 icon, exported from the launcher icon (`ic_launcher_foreground.xml` and `ic_launcher_monochrome.xml`, an adaptive icon).
  - The **user** records the 30–60 s video of the microphone foreground service, following the steps in `13-PLAY-CONSOLE-DRAFTS.md` §3, and makes the 1024 × 500 feature graphic.
  - The plan states the exact specs for every asset.

### Claude's Discretion

- How to sequence the phase around the 14 days, for example finishing the D-09 fixes and the Console paperwork while the closed test runs.
- The four open "Decisions for the user" in the data safety draft: whether card data counts as collected, the NFC permission, push content, and whether the privacy policy names Agora, Stripe and FCM. Research recommends; the user confirms at the same checkpoint as D-05.
- The wording of the release notes ("What's new") and the closed-track tester instructions.
- Whether to fix the forced-logout push-token DELETE that goes out with no bearer (milestone-audit warning 2). Do it only if it is trivial and low risk. Otherwise leave it for after release.

### Deferred Ideas (OUT OF SCOPE)

- Accepted release deviations stay open after release: FND-13, PAY-05/CR-01 (no live 3DS), PAYOUT-02, POL-04 (TalkBack), REL-05 (single internal tester), and the Phase 16 post-release bugs (BUG-04, 18–21, 23, 28).
- Reviewed todo not folded: **Sync Android bug fixes to iOS** (`.planning/todos/pending/2026-11-02-sync-android-bug-fixes-to-ios.md`, major). iOS work, outside the Play release. Track separately; it should not wait on Phase 17.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REL-06 | Play Console release requirements complete: data safety form, foreground-service (microphone) declaration and video, account-deletion URL, content rating | Console "App content" task list, FGS declaration wording and video rule, deletion-URL rules and page content from the real backend cascade, corrected data-safety table, IARC answers (sections: Console Declarations, Data Safety Corrections, Deletion Page, FGS) |
| REL-07 | Production release live on Google Play | Closed-test gate rules, production-access questionnaire, promote-from-closed-track mechanics, versionName 1.0.0, 100% rollout, CA+US countries, store smoke twice (sections: Closed Test, Release Mechanics, Sequencing) |
| REL-04 (carried from Phase 13) | Store listing, data safety, FGS declaration drafted in Console | Corrected drafts, asset specs, Console paste order |
| POL-02 (carried from Phase 15) | Process-death safety on the 16.x input screens | File/line map, reference pattern (`WriteReviewViewModel`, `MakeABookingViewModel`), test seams |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- iOS code in `ios-reference/` is the behavior source of truth. Deviate only for Android platform conventions. [VERIFIED: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md]
- Stack is Jetpack Compose + Material 3, MVVM with Compose state, coroutines, Hilt, Retrofit + Moshi, EncryptedSharedPreferences, Firebase Messaging, Agora, Stripe PaymentSheet; minSdk 26, targetSdk 37, compileSdk 37.
- File organization mirrors the iOS folder tree (screens under `screens/<area>/<screen>/`).
- Backend is the same REST contract as iOS; `backend-reference/docs/ios/` holds the authoritative specs.
- Memory directives that bind this phase: Android Studio JBR JDK 25 (`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`), never `gradlew --stop` in parallel executors, worktree executors need `local.properties` and absolute iOS paths, manual testing is deferred and logged as open (except device gates), name accounts and phones by role ("Android client on the Samsung", "iOS expert on the iPhone"), never A/B. The `phase.complete` tool can mis-point STATE to Phase 2; check `next_phase` by hand.
- Reviewer password in `backend-reference/docs/APP-REVIEW-ACCOUNT.md` must not be copied into any planning or repo file (this research does not contain it).

## Summary

**The closed-test gate is the critical path, and the calendar is dominated by it.** Google's current Help page states the rule only for "personal accounts created after November 13, 2023": 12 testers opted in continuously for the preceding 14 days, then an "Apply for production" form that Google reviews ("usually seven days or less"). Many third-party vendor pages claim organization accounts are exempt, but Google's page never says so, and Console showed the gate on this organization account on 2026-10-30. Trust the Console, not the blogs: run the closed test (D-01), and in parallel ask Play support why an organization account shows the gate. D-04 is confirmed: nothing in Google's rules resets the clock when a new release is uploaded to the closed track, only a tester opting out does, and Google's own FAQ says "Updating your app in closed testing before releasing to production helps minimize low ratings". The real risk is not the clock but **tester engagement**: Google lists "insufficient tester engagement" as a reason to extend testing and the form asks whether testers used all features. D-03 ("light use, no task list") is the weak point; pick a vendor whose testers use the app daily and bring a few real testers of your own.

**D-05's conclusion is right, but its cited basis is wrong.** The Payments policy page (9858738) contains no "real-time person-to-person services" exemption; that phrase is Apple's. The exemption that covers ClickMe is in Google's separate Help Center FAQ "Understanding Google Play's Payments policy" (10281818): *"I'm a developer of an app that offers 1:1 online paid services. Do I have to use Google Play's billing system?"* Not required if the service is between two individuals and is not replayable afterwards. Its examples include specialist advisory services and education classes. ClickMe has no call recording (verified in the backend and app code), so Stripe fits. Residual risk: it is a FAQ, not the policy text, so the user must confirm at the checkpoint and keep a dated copy of the FAQ as evidence.

**Three code fixes are small and well-seeded.** POL-02 follows `WriteReviewViewModel` (restore test pattern exists). BUG-22/34 share a cause visible in the evidence screenshots: the call hero reserves 360 dp for a glow around a 160 dp avatar, and the controls row sits at the bottom edge of a scroll viewport that End Call has shrunk. Root cause of "no scroll at 200% font" is not proven; the plan must start with a failing Robolectric test. The forced-logout push-token DELETE is **not** a trivial fix: by the time the client runs it the refresh token has been rejected, so no bearer can work; the backend already prunes dead FCM tokens on next send. Leave it for after release.

**Primary recommendation:** Start the closed test as early as possible: (1) check the Dashboard and open a Play support question today, (2) in parallel finish D-09 fixes, build vc 6 as versionName `1.0.0`, complete all Console "App content" tasks and the store listing (closed testing requires "Complete app setup"), (3) start the closed test with 20+ testers from a paid service whose terms tie refund to approval, (4) apply for production on day 14 and promote the same AAB to production at 100% in CA and US.

## Architectural Responsibility Map

This phase has no new feature tiers; the map shows who owns each release capability.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Process-death draft restore (POL-02) | Android app (ViewModel + SavedStateHandle) | — | State lives in the client; the server keeps no drafts |
| Call controls reachability (BUG-22/34) | Android app (Compose layout) | — | Pure layout; no backend or iOS change |
| Account-deletion web resource | Marketing site (uptrendinvestments.net, static HTML) | Backend admin portal (Terminate) for email requests | Google requires a web path that needs no app; the email path is fulfilled by an admin using the existing Terminate action |
| Data-safety / FGS / content-rating declarations | Play Console (human) | Repo draft file (source text) | Console is the only place the declaration lives; repo holds paste-ready text |
| Payments (paid sessions) | Stripe via backend | Android PaymentSheet | Unchanged; policy conclusion only (D-05) |
| Release signing and upload | Local Mac (keystore) + Play Console | Play App Signing | Existing pipeline, unchanged |
| Reviewer access | Backend seed script + Console "App access" | — | `appreview@clickme.test`, re-seeded by the user on the backend |

## Standard Stack

No new runtime library is added. The "stack" is the existing release toolchain.

### Core
| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| Android Gradle `bundleProdRelease` | AGP in repo, JDK 25 via Android Studio JBR | Signed AAB for Play | Already used for vc 1-5 [VERIFIED: 16-BUILD-LEDGER.md Uploads table] |
| Play App Signing + upload key `~/keys/clickme-upload.jks` | upload cert SHA-256 pinned in ledger | Signing | Existing; every AAB must print the pinned SHA-256 |
| `adb` | 1.0.41 | Screenshots, install proof | Present on the Mac [VERIFIED: `adb version` this session] |
| `aapt2`, `apksigner`, `zipalign` | build-tools 36.0.0 | Release checks B4 | Present [VERIFIED: ls build-tools] |
| `sips` | macOS built-in | Validate screenshot/icon pixel size and alpha | Present at /usr/bin/sips |
| Robolectric + Compose ui-test | already in `app/src/test` | Regression tests for BUG-22/34 | `MeetingCallScreenEndButtonRegressionTest` is the template |

### Supporting
| Tool | Purpose | When to Use |
|------|---------|-------------|
| Android Studio Image Asset Studio | 512×512 Play icon from the adaptive icon | Preferred icon export (human, 2 minutes) |
| SystemUI demo mode (`am broadcast ... com.android.systemui.demo`) | Clean status bar in screenshots | Google asks for full battery/Wi-Fi/signal and no notifications |
| Paid closed-test service | 12+ testers for 14 days | D-02 |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Paid service (D-02, locked) | Free tester-exchange apps (Testers Community free tier, Closed Test Pro credits) | Costs time not money, slower, weaker engagement control. Not recommended given D-02 |
| Upload new AAB to production | **Promote** the final closed-track release to production | Promotion keeps one binary (D-15 wants the smoke binary to equal production). Recommended |

**Installation:** none. No packages are installed in this phase.

**Version verification:** not applicable (no new dependency). Existing pins: Stripe 23.24.0, Agora 4.6.4, Firebase BoM 35.1.0 [VERIFIED: gradle/libs.versions.toml:15,16,19].

## Package Legitimacy Audit

No external packages are installed or upgraded in this phase. Audit not applicable.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 TODAY (human, 15 min)                         CODE LANE (executor)                       CONSOLE/PAPERWORK LANE
 Dashboard: gate shown?  ──yes──┐              ┌─ POL-02: 3 VMs -> SavedStateHandle        ┌─ App content tasks:
 Play support question (org      │              │   + restore tests                        │   privacy policy, App access,
 account exempt?)                │              ├─ BUG-22/34: hero glow + viewport         │   Ads=No, Target audience 18+,
                                 │              │   + reachability tests                   │   Content rating, Data safety v2,
                                 ▼              ├─ versionName 1.0.0, vc 6                 │   Financial features, FGS decl.
                       CLOSED-TEST CRITICAL PATH └─ gate B2 -> bundleProdRelease -> B4 ────┤ Store listing + graphics
                                 │                       -> archive (B5) -> ledger        │   (needs icon, screenshots,
                                 │                                                        │    feature graphic, FGS video)
   needs: app setup complete  ◄──┴────────────────────────────────────────────────────────┘
   + vc 6 on closed track + tester service engaged + countries incl. tester countries
                                 │
                       DAY 0 ───►│ 14 days continuous opt-in of >=12 testers (aim 20+)
                                 │   - fix builds may be uploaded (clock counts testers, not builds)
                                 │   - smoke run #1 on the closed-track Play install (D-15.1)
                                 │   - collect tester feedback for the form
                       DAY 14 ──►│ Dashboard: "Apply for production" (3-part form) -> Google review (~7 days)
                                 ▼
                       Approved ─► Promote the same closed-track release to Production
                                   countries CA + US, 100%, managed publishing off -> Google review
                                 ▼
                       Live on Play ─► smoke run #2 from the public listing (D-15.2)
```

### Recommended Project Structure (new files only)
```
.planning/phases/17-play-store-release/
├── 17-RESEARCH.md
├── 17-RELEASE-CHECKLIST.md      # human Console steps, ordered, checkbox + "Check:" like Phase 13
├── 17-CONSOLE-PASTE.md          # corrected store listing, data safety v2, FGS text, App access text (NO password)
├── 17-DELETE-ACCOUNT-PAGE.html  # content for uptrendinvestments.net/clickme/delete-account.html
├── 17-PAYMENTS-DECISION.md      # policy quotes + decision checkpoint (D-05)
└── assets/                      # screenshots, play-icon-512.png (gitignore or keep out of repo if it shows people)
app/src/main/java/.../screens/{bookingnote,settings/feedback,clientmode/search}/   # POL-02
app/src/main/java/.../screens/call/calling/{MeetingCallScreen.kt,subviews/MeetingCallAvatar.kt}  # BUG-22/34
app/src/test/java/.../ (same packages)  # new restore and reachability tests
```

### Pattern 1: SavedStateHandle mirror with restore (POL-02)
**What:** Inject `SavedStateHandle`, seed the initial state from it, write every user edit back. Reference: `WriteReviewViewModel` (@HiltViewModel assisted + `savedStateHandle`) and `MakeABookingViewModel` (Phase 6). Restore tests: `WriteReviewViewModelTest.savedStateRestoresForm` and `editsAreMirroredToSavedState`.
**When to use:** the three 16.x VMs.
**Per-VM file map** (all paths under `app/src/main/java/com/uptrendinvestments/clickmeapp/`):

| VM | Current state | Change | Tests to update (constructor call sites) |
|----|---------------|--------|-------------------------------------------|
| `screens/bookingnote/BookingNoteViewModel.kt` | Assisted ctor `(@Assisted args, repository)`; KDoc line 30: "there is no SavedStateHandle, so it is lost if the process dies" | Add `private val savedStateHandle: SavedStateHandle` (assisted injection already used, same as `WriteReviewViewModel`). Mirror `noteText` in `onTextChange` (key `note`). In `load()` success, if the handle holds a draft use `capText(draft, NOTE_MAX_LENGTH)` as `noteText` while `lastPersisted` stays `normalizeNote(serverText)` so `hasChanges` is true and the discard prompt still works. Remove the key after a successful `save()`. Update the KDoc | `BookingNoteViewModelTest.kt:36`, `BookingNoteScreenTest.kt:72` (both `BookingNoteViewModel(BookingNoteArgs(role, "b1"), BookingNoteRepository(api))`) |
| `screens/settings/feedback/FeedbackViewModel.kt` | `@Inject ctor (repository, userManager)`; `_uiState` at line 68; `FeedbackUiState.toString()` deliberately redacts and KDoc says "never logged or put in saved state (RESEARCH V8)" | Add `savedStateHandle`. Mirror only `selectedTypeId`, `details`, and `email` **when the user edited it** (so the signed-in prefill is not copied into saved state). Seed `_uiState` from the handle; keep the `retry()` logic (`selectedTypeId ?: types.first().id`, `email.ifEmpty { signedInEmail() }`) so restored values win. Keep `toString()` redaction. Update the KDoc | `FeedbackViewModelTest.kt:42` (`FeedbackViewModel(FeedbackRepository(api), userManager)`) |
| `screens/clientmode/search/SearchExpertViewModel.kt` | `@Inject ctor (repository, categoryStore)`; `_uiState` at line 39; KDoc: "there is no saved-state handle ... (D-02). This is a deliberate exception to the Phase 15 POL-02 restore rule" | Add `savedStateHandle`. Mirror `query`, `selectedCategorySlug`, `sort` (store `SearchSort.apiValue`, map back). Seed the state from it; the init `runSearch(Fresh)` then searches with the restored query immediately (no debounce). Update the KDoc: Phase 17 D-09 overrides Phase 16.1 D-02 | `SearchExpertViewModelTest.kt:52` (`SearchExpertViewModel(SearchRepository(api), store)`) |

**Example (sketch, follows `WriteReviewViewModelTest`):**
```kotlin
// Source: pattern in app/src/test/.../writereview/WriteReviewViewModelTest.kt (savedStateRestoresForm)
@Test
fun restoresTheNoteDraftAfterProcessDeath() =
    runTest(main.testDispatcher) {
        val handle = SavedStateHandle(mapOf("note" to "my draft"))
        val vm = BookingNoteViewModel(BookingNoteArgs(role, "b1"), handle, BookingNoteRepository(api))
        runCurrent()
        assertEquals("my draft", vm.uiState.value.noteText)
        assertTrue(vm.uiState.value.hasChanges)   // differs from the loaded server note
    }
```
**Caution:** this reverses three documented decisions (16.1 D-02 for Search, 16.5 RESEARCH V8 for Feedback, T-16.4-10/A4 for the note). D-09 locks the reversal, so do it, but (a) keep text out of logs and `toString()`, (b) the 5,000-character note cap keeps the Bundle small, (c) update the KDocs so the code no longer claims the opposite.

### Pattern 2: Failing-test-first layout fix (BUG-22/34)
**What:** Copy the harness in `MeetingCallScreenEndButtonRegressionTest.kt` (it already sets `fontScale`, a nav-bar inset via `dispatchInsets(view, navigationBarInsets(48))`, and a banner state) and assert **controls reachable**, which no existing test does (existing tests only assert End Call). Add: `rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).performScrollTo().assertIsDisplayed()` and a check that each of the four control tags (`CALL_CONTROL_MINIMIZE_TAG`, `CALL_CONTROL_CHAT_TAG`, `CALL_CONTROL_MUTE_TAG`, `CALL_CONTROL_SPEAKER_TAG`) ends above the End Call top in root coordinates after scrolling to the end.
**Cases to cover:** (1) font 1.0, 360x800, banner + chip (BUG-22); (2) font 2.0 at the BUG-34 geometry (720x1600 px at 340 dpi = about 339 x 753 dp), Waiting/pre-join state; (3) the external variant; (4) the Connected pair hero.
**What the evidence shows** (screenshots read this session, `~/ClickMe-release/logs/batch-A/BUG-10-banner.png`, `.../batch-B/FS.C3-200pct.png`):
- The scrolling area ends at the top of End Call, and the control circles are cut in half at that edge. They are not under End Call; they are below the end of the visible scroll viewport.
- A large empty band sits between the avatar ring and the name. `MeetingCallAvatar` reserves a 360 dp glow box for a 160 dp avatar: `GlowSize = 360.dp` (line 41), `SingleAvatarSize = 160.dp` (line 42), `.requiredSize(glowSize)` (line 123). That is about 200 dp of dead layout height. [ASSUMED from the screenshot geometry plus the code; the test must prove it]
- BUG-34 reports the screen does not scroll at all at 200%/zoom. Code review of `MeetingCallScreen.kt` (`BoxWithConstraints(Modifier.weight(1f))` containing `verticalScroll(...).heightIn(min = maxHeight)`, lines 107-114) shows nothing that should block scrolling, and the parent in `CallOverlayHost.kt` consumes pointer events only in the Main pass after children (line 220). Cause unproven.
**Recommended fix order** (stop at the first step that turns the tests green):
1. Make the glow not consume layout height (draw it with `drawBehind` at a larger radius, or place the avatar in a box sized to avatar + ring and let the glow overflow). Keep `avatarUnchangedOnPhone` (`MeetingCallScreenTest`, `w360dp-h800dp`) green: the avatar itself stays 160 dp on a normal phone.
2. Size the hero from the **viewport** (`BoxWithConstraints.maxHeight`) instead of `LocalConfiguration.screenHeightDp`, which ignores the banner, chip, font scale and End Call.
3. If the controls are still unreachable at 200%, pin the controls row together with End Call in the bottom (non-scrolling) region and let only hero + info scroll. Beware: TalkBack order is encoded as traversal indexes (status -1, name, timer, Mute/Speaker 0, Chat 1, Minimize 2, End 3) and index sorting applies only among direct children of a traversal group (see the KDoc at `MeetingCallScreen.kt:75-79`). Moving the row changes groups, so `MeetingCallScreenTest.rootIsATraversalGroupAndTheNameIsAHeading` and the Phase 15 a11y tests must be re-run.

### Anti-Patterns to Avoid
- **Re-fixing BUG-10 by un-pinning End Call.** BUG-10 (End Call cut off, nav bar drawn over it) is verified fixed and covered by five tests; do not regress it.
- **Adding a second paid-tester cohort or editing the iOS app.** Out of scope.
- **Copying the reviewer password into any `.planning` file or Console draft file.** The user types it into Console.
- **Declaring Play Billing or Stripe tips/credits in the listing.** Listing text must match the exemption: paid sessions only, no recording, no digital goods sold.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| 512×512 Play icon from an adaptive icon | A bespoke renderer or manual SVG conversion of `clickme_mark` | Android Studio Image Asset Studio ("Google Play Store" output, `ic_launcher-playstore.png`) | It composites background + foreground with correct safe zone. Background is `@color/surface`, foreground is `ic_launcher_foreground.xml` (an `inset` of `clickme_mark`, 17 dp) [VERIFIED: res/mipmap-anydpi/ic_launcher.xml and res/drawable/ic_launcher_foreground.xml] |
| 14-day tester tracking | A spreadsheet or script | Console's Testers tab count plus vendor dashboard | Console shows the opted-in count; vendor reports per-tester days |
| Version-code bookkeeping | Gradle derivation | The existing hand-bumped literal and ledger flow | Phase 16 D-15 policy (`16-BUILD-LEDGER.md`) |
| Release verification | New scripts | Ledger steps B2-B8 verbatim | Already proven for vc 1-5 |
| Clean status bar for screenshots | Photoshop | SystemUI demo mode | Google's own asset rule asks for full icons and no notifications |
| Account deletion fulfilment by email | A new backend endpoint | Backend admin portal "Terminate user" (runs `purgeAndLockAccount`) | Same cascade as self-delete; no new backend work (the phase must not change the backend) |

**Key insight:** every item here is configuration, copy and verification of existing assets; custom tooling would add risk without shortening the 14-day critical path.

## Runtime State Inventory

Not a rename/refactor/migration phase. Omitted. One related note: the closed test creates real accounts on the **production** backend (paid-service testers), and the reviewer account is shared state; see Pitfalls 5 and 7.

## Common Pitfalls

### Pitfall 1: Believing the "organization accounts are exempt" claim
**What goes wrong:** Plan assumes no closed test; the Dashboard still blocks production and the release slips by 3+ weeks.
**Why it happens:** Google's page says only "personal accounts created after November 13, 2023". Most of the vendor pages found repeat "organization accounts are exempt" (they sell tester services; Google never writes that sentence). Console showed the gate for this organization account on 2026-10-30.
**How to avoid:** Task 1 = open Dashboard, screenshot it, record "gate shown / not shown" in STATE. Also check Settings > Developer account for the account type, and open a Play support question (free) asking why an organization account shows the gate. Start the closed test anyway unless Console clearly unlocks Production.
**Warning signs:** Production page greyed out; "Apply for production" task on the Dashboard.

### Pitfall 2: Closed testing cannot start until app setup is complete
**What goes wrong:** The 14 days start late because the store listing, data safety or declarations were not finished.
**Why it happens:** Google's track table: closed testing requires "Complete app setup". Phase 13 found Play did not block the internal rollout on missing App content, but internal testing has no requirements. [CITED: support.google.com/googleplay/android-developer/answer/14151465]
**How to avoid:** Treat all Console paperwork (including graphics and the FGS declaration) as the first-14-days blocker. Record the FGS video **now** from the vc 5 Play install (the foreground-service behavior is unchanged by D-09). Capture the in-call screenshot last, after the BUG-22/34 fix.
**Warning signs:** "Review release" on the closed track lists unfinished App content items. [ASSUMED: exactly which items block a closed rollout; the Phase 13 note covers internal only]

### Pitfall 3: Tester engagement, not count, fails the application
**What goes wrong:** 14 days pass with 12+ opted in, Google answers "More testing required", and another 14 days are lost.
**Why it happens:** Google: "Reasons for required continued testing include having fewer than 12 opted-in testers or insufficient tester engagement during the testing period." The form asks "Whether testers used all available app features" and "Whether tester usage matched expected production user behavior". [CITED: answer/14151465] D-03's "light use, no task list" is the exposed spot.
**How to avoid:** Choose a vendor whose testers use the app daily (screenshot proof or feedback report) and ask the vendor in writing to cover sign-up, browse, chat and settings. Add 2-3 of your own real testers. Keep the feedback summary honest (do not claim testers booked paid sessions).
**Warning signs:** Android vitals / Console shows near-zero sessions for the test builds.

### Pitfall 4: A tester opting out, not a new build, breaks the streak
**What goes wrong:** One drop-out at day 12 leaves 11 qualifying testers.
**Why it happens:** "Testers who opt in, test for fewer than 14 days, and then opt out do not count... If a tester opts out and opts back in later, the 14 days must be consecutive." [CITED: answer/14151465]
**How to avoid:** Recruit 20+ for a buffer (D-02). Uploading new closed-track releases is safe: the same FAQ says "Continue running closed tests while resolving user-reported issues... Updating your app in closed testing before releasing to production helps minimize low ratings and negative reviews." Vendor sources agree (a new build does not change opt-in status).
**Warning signs:** Opted-in count in the Testers tab dropping.

### Pitfall 5: Paid-service testers hit the live production backend
**What goes wrong:** Testers create accounts against production, may book real experts or enter real cards (Stripe is live), and leave garbage accounts and chat threads.
**Why it happens:** The prod flavor has `pk_live_...` and the production `BASE_URL` [VERIFIED: app/build.gradle.kts, `create("prod")` block]. The closed track serves the same prod build.
**How to avoid:** Write the tester instructions (Claude's Discretion item): sign up as a client, browse, chat only with the support/reviewer-safe contacts if any, **free topics only**, never enter a real card. Plan a post-test cleanup (admin portal Terminate for tester accounts). Warn the user that the vendor's testers are unknown people on a live system. [ASSUMED: vendor testers follow instructions]

### Pitfall 6: Closed-track countries exclude the testers
**What goes wrong:** Vendor testers cannot see or install the app.
**Why it happens:** Each track has its own countries/regions setting; D-12 (CA + US) is for Production. Vendor tester pools are worldwide (PrimeTestLab claims 120+ countries). [ASSUMED: per-track country setting behavior]
**How to avoid:** Set the closed track to the countries the vendor states (or all) and keep Production at CA + US. Also note minSdk 26: testers on Android 7 cannot install.

### Pitfall 7: Reviewer account gets deleted or booked
**What goes wrong:** Google's reviewer (or a pre-launch crawler) deletes `appreview@clickme.test` or books its free topic, then deletion fails with 409 or login fails.
**Why it happens:** [VERIFIED: backend-reference/docs/APP-REVIEW-ACCOUNT.md] "Risk accepted (quick 261027-pf4, option-c): the account's FREE topic is now bookable", and "Re-check the account before each submission: re-run `npm run seed:app-review`". After a deletion "Re-running creates a fresh user with a new id".
**How to avoid:** Checklist step "re-run seed immediately before submitting to review (closed-track release and production release)", a backend human step. Account holds seeded **completed** sessions only, so a reviewer cannot reach a live call; say so in the App access instructions and point to the FGS video.

### Pitfall 8: Screenshot specs the Samsung cannot meet natively
**What goes wrong:** Console rejects phone screenshots.
**Why it happens:** Google: "The maximum dimension of your screenshot can't be more than twice as long as the minimum dimension", JPEG or 24-bit PNG (no alpha), 320-3840 px, recommended at least four at 1080 px minimum 9:16. The Samsung SM-A075F is 720x1600 (ratio 2.22) and the AVD `Medium_Phone` is 1080x2400 (ratio 2.22): both fail the 2:1 rule. [CITED: answer/9866151; VERIFIED: bug-log evidence screenshots are 720x1600; AVD name from ~/.android/avd]
**How to avoid:** Capture at 1080x1920 (9:16): `adb shell wm size 1080x1920` on the Samsung (reset afterwards) or a Pixel-2-class AVD. Validate with `sips -g pixelWidth -g pixelHeight -g hasAlpha`. Use the appreview/demo data (not real people's profiles: the Phase 16 call screenshot shows a real face). No notifications and full status icons.

### Pitfall 9: Store icon spec in the Phase 13 draft is wrong
**What goes wrong:** The draft says "no alpha needed"; Google's page requires "32-bit PNG (with alpha)", 512x512, max 1024 KB. The feature graphic is the one with no alpha ("JPEG or 24-bit PNG (no alpha)", 1024x500). [CITED: answer/9866151] Correct 13-PLAY-CONSOLE-DRAFTS.md.

### Pitfall 10: Treating a Help FAQ as policy text
**What goes wrong:** A reviewer enforces the Payments policy literally (no Section 3 exemption for digital services) and rejects Stripe.
**Why it happens:** The exemption is in the FAQ 10281818, not the policy 9858738.
**How to avoid:** Decision checkpoint (D-05), a dated copy of the FAQ text in `17-PAYMENTS-DECISION.md`, listing copy that matches (paid 1:1 sessions between two individuals, not recorded). If Google rejects, the fallback is an appeal citing the FAQ, not a rebuild.

## Code Examples

### Version bump (D-10), then the ledger flow
```kotlin
// Source: app/build.gradle.kts:76-77 currently reads `versionCode = 5` / `versionName = "0.1.0"`
versionCode = 6
versionName = "1.0.0"
```
Flow, from `16-BUILD-LEDGER.md` (all steps exist, reuse verbatim): F4 gate B2, F5 bump commit `chore(release): versionCode 6`, B3 build, B4 checks (including `google_app_id`, certificate pin `D8:36:BE:66:...:E1:95`), B5 archive to `/Users/LeonChen/ClickMe-release/`, B6 ledger row, B7 human upload, B8 Samsung proof. Play release name `1.0.0 (6)`.

### Phone screenshots at 1080x1920 (Samsung)
```bash
# Source: standard adb; [ASSUMED] that wm override and SystemUI demo mode behave on One UI (fallback: Pixel 2 AVD)
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB shell wm size 1080x1920 && $ADB shell wm density 420
$ADB shell settings put global sysui_demo_allowed 1
$ADB shell am broadcast -a com.android.systemui.demo -e command enter
$ADB shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000
$ADB shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
$ADB shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile show -e level 4 -e datatype none
$ADB shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
$ADB exec-out screencap -p > assets/01-explore.png      # repeat per screen
$ADB shell am broadcast -a com.android.systemui.demo -e command exit
$ADB shell wm size reset && $ADB shell wm density reset
sips -g pixelWidth -g pixelHeight -g hasAlpha assets/01-explore.png   # need 1080x1920; screencap PNGs may carry alpha: convert to JPEG with `sips -s format jpeg`
```

### Release checks before upload
```bash
# Source: 16-BUILD-LEDGER.md B3/B4
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g :app:bundleProdRelease :app:assembleProdRelease
"$JBR/jarsigner" -verify app/build/outputs/bundle/prodRelease/app-prod-release.aab     # prints "jar verified."
"$BT/aapt2" dump badging app/build/outputs/apk/prod/release/app-prod-release.apk | grep -E "versionCode|versionName"   # 6 / 1.0.0
```

## Console Declarations (REL-06, REL-04): answers and corrections

Order inside Console is theirs; the "Needs attention" tab on App content is the authority for which tasks exist. Tasks relevant to ClickMe:

| Task | Recommended answer | Basis |
|------|--------------------|-------|
| Privacy policy | `https://www.uptrendinvestments.net/clickme/privacy.html` (same as `AppLinks.PRIVACY_POLICY`) | [VERIFIED: config/AppLinks.kt, `const val PRIVACY_POLICY = "https://www.uptrendinvestments.net/clickme/privacy.html"`]. Google requires an active public URL, not a PDF, and a link inside the app too [CITED: answer/9859455] |
| App access (sign-in details) | Login required: supply `appreview@clickme.test` plus the password the user types in; instructions text below | D-07 |
| Ads | No ads | No ad SDK; merged manifest has no `AD_ID` permission [VERIFIED: app/build/intermediates/merged_manifests/prodRelease/processProdReleaseManifest/AndroidManifest.xml:11-39, only INTERNET, ACCESS_NETWORK_STATE, RECORD_AUDIO, MODIFY_AUDIO_SETTINGS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS, NFC, VIBRATE, WAKE_LOCK, c2dm RECEIVE]. Advertising ID declaration: No |
| Target audience | 18 and over only; not designed for children; no Families | D-08 |
| Content rating (IARC) | Category: social/communication-style app (users chat and talk). Answers: user interaction = yes (chat and voice between users); digital purchases/payments = yes (paid sessions); violence, sexual content, profanity, drugs, gambling = no; shares location = no. Expect an interaction notice and a teen-or-higher rating. Re-take if Console rating looks wrong ("Start new questionnaire") | [CITED: answer/9859655 for the process]. The exact question wording and categories are [ASSUMED]: read them in Console |
| Data safety | Table v2 below; deletion = Yes with the web URL | [CITED: answer/10787469, answer/13327111] |
| Financial features | "My app doesn't provide any financial features" | [CITED: answer/13849271 categories list]. [ASSUMED] that Stripe checkout for services and expert payouts are not "Mobile payments and digital wallets"; ClickMe has experts in finance topics but gives no advice itself. User confirms |
| Foreground service permissions | Microphone, text from the Phase 13 draft; use case "Background Audio Access"; video link | See FGS section |
| Government apps, Health apps, News etc. | Not applicable / No | [ASSUMED]: use what Console lists |

**App access instructions (no password in the repo):**
```
Sign-in: email appreview@clickme.test, password entered in the credentials field.
The account holds both roles. Open the Bookings tab for three completed sessions, the Messages tab for a chat thread, and Settings for Delete Account.
Voice calls need two people at a scheduled time, so a live call cannot be reached with this account alone; the in-app call and its foreground service are shown in the declaration video. Payouts are not set up on this account, so paid topics cannot be booked from it.
```

### Data safety table corrections to `13-PLAY-CONSOLE-DRAFTS.md` section 2
- **Financial info row is mis-labelled.** Google's taxonomy: "User payment info: Information about a user's financial accounts such as credit card number", "Purchase history: Information about purchases or transactions a user has made", "Other financial info: Any other financial information such as user salary or debts". [CITED: answer/10787469] Card entry in PaymentSheet is User payment info; booking amounts and Payment History screen (Phase 16.5) are Purchase history; expert earnings and payout status fit Other financial info. Split the draft's single "Other financial info" row into those.
- **Decision 1 (card data): declare collected.** Google defines "Collect" as "transmitting data from your app off a user's device", including data sent "by libraries and/or SDKs ... irrespective of whether data is transmitted to you or a third-party server". Stripe's SDK transmits the card, so it is collected. Shared: No, because Stripe is a service provider processing on the developer's behalf (Google lists "Service providers" as a transfer that does not need to be disclosed as sharing). [ASSUMED] that Google would accept Stripe as a service provider; if the user prefers zero doubt, declare Shared = Yes for payment info.
- **Decision 2 (NFC): no action.** The merged prod manifest contains `<uses-permission android:name="android.permission.NFC" />` at line 18 (from Stripe's SDK; the app's own manifest lists no NFC permission) [VERIFIED: merged manifest above]. Neither Data safety nor App content asks about it. Optionally mention card-tap input in reviewer notes. [ASSUMED: no Console prompt for NFC]
- **Decision 3 (push content): yes, say so.** The backend sends the first 140 characters of a chat message as the push body through FCM. [VERIFIED: backend-reference/src/controllers/chats.controller.js:482 `const preview = typeof content === 'string' ? content.substring(0, 140) : 'Sent you a message';` and :485 `body:         preview,`]. Messages are already declared; FCM is the service provider that delivers it. Mention message previews in the privacy policy.
- **Decision 4 (privacy policy names providers):** the live policy (fetched this session, "Last updated 31 August 2026") already says "Agora — carrying voice calls between you and the expert", "Stripe — payments, expert identity verification, and payouts" and "Apple and Google — delivering push notifications to your device", and says payment and tax records "are kept for as long as financial and tax law requires". It does not say "Firebase". [CITED: https://www.uptrendinvestments.net/clickme/privacy.html via summarizing fetch; user should re-read the exact text]. Recommended edits (user publishes): add "Firebase Cloud Messaging (Google)" and that notification text can include a booking update or a message preview; link the new deletion page; state what survives deletion (below). Data safety answers must match the policy.
- Draft line 78 ("Until then the support email handles requests") must be replaced by the deletion URL (D-06).
- Add the two Data safety deletion answers: provides an in-app path and a web link (below). "Committed to Play Families Policy: No" stays (18+ only).

## Account-deletion web page (D-06)

**Google's requirements** [CITED: answer/13327111]: the link must be functional, "relevant in scope (the pathway to request account deletion should be prominently featured and easily discoverable)", "reference the app or developer name (as it appears on your store listing)", and "should give users a way to request that their data be deleted without sending the user back to the app". A customer-service email is an accepted path. If data is retained for "security, fraud prevention or regulatory compliance", state it ("for example, within your privacy policy").

**What the backend actually does** (source for the "deleted vs kept" list) [VERIFIED: backend-reference/src/services/accountCascade.js `purgeAndLockAccount`, steps F1-F11, G, H, I]:
- Profile is scrubbed ("F1: Tombstone profiles (D-04) — scrub PII, set deleted_at"): email replaced with `deleted+<id>@deleted.invalid`, first/last name become "Deleted user", avatar, bio, job title, company, city, state/province, country, time zone cleared. Expert education and meeting URL cleared.
- Booking notes and expert messages/private notes/takeaways/decline text and meeting URLs are cleared.
- Push device tokens, notifications, notification preferences, discovery interactions, password-reset records and deletion tokens are hard-deleted.
- Sign-in is blocked (account banned, sessions revoked, original email released), and uploaded files (avatars, chat images) are removed from storage (best effort).
- **Kept:** reviews, chat messages and reports remain, attributed to "Deleted user" (code comment: "reviews, messages, reports: NO DML"); booking and payment records remain (the privacy policy says payment, payout and tax records are kept as long as law requires). The exact retention periods are not in the code: [ASSUMED] user states them or says "as required by law".
- Upcoming bookings block self-delete with 409 (`EXPERT_HAS_UPCOMING_BOOKINGS`, `SESSION_IN_PROGRESS`, `CLIENT_HAS_UPCOMING_BOOKINGS`); the page should tell users to cancel or finish sessions first, as Google requires prerequisites to be outlined.

**Page must contain:** app name "ClickMe" and the developer name exactly as on the store listing; heading "Delete your ClickMe account"; Section A, in-app steps: Settings, Delete Account, read the Warning, Continue, choose a reason, confirm your password, Delete (Phase 14 flow, `screens/settings/deleteaccount/`: Warning form, then Reason and password form, then Farewell); Section B, no-app request: email `support@clickme.app` from the account's email address with subject "Delete my ClickMe account", expected response time (user decides); deleted vs kept lists; prerequisite note (upcoming sessions).

**Fulfilment of email requests:** an admin uses the backend admin portal "Terminate user" (permanent, same cascade as self-delete, "Self-deleted accounts also show as terminated") [VERIFIED: backend-reference/docs/ADMIN-PORTAL.md terminate rows]. Verify the requester controls the account email before terminating (Security Domain). The user publishes the HTML on the site next to `privacy.html`. Optionally add a link in the app (a constant next to `PRIVACY_POLICY` in `AppLinks.kt`); not required by Google when an in-app path exists, so recommend **not** adding code.

## Foreground Service (microphone) declaration

- Manifest: `android:name=".services.call.CallService"` with `android:foregroundServiceType="microphone"` [VERIFIED: app/src/main/AndroidManifest.xml:60,62]. Permissions in the manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `RECORD_AUDIO`.
- Android docs: microphone type is for "Continue microphone capture from the background, such as voice recorders or communication apps", prerequisite: RECORD_AUDIO granted, and it "cannot [be created] while your app is in the background". The `phoneCall` type requires `MANAGE_OWN_CALLS` or the default dialer role and ConnectionService, so **do not pick "Phone call / Calling: Cellular/VoIP/Telecom APIs"** for Agora; the Phase 13 draft's "if it lists Phone call or Communication, use that" is wrong for the Telecom-based option. Choose "Background Audio Access" under Microphone. [CITED: developer.android.com/develop/background-work/services/fgs/service-types]
- Console fields [CITED: answer/13392821]: description of the functionality; user impact if deferred and if interrupted (two separate answers: the draft's single "impact" text covers both, split it); a video link; the use case. Video requirement for Background Audio Access: "Show the user initiate the microphone actively recording audio or receiving voice commands". For ClickMe: tap Join, accept the microphone prompt, show the call connected, then the background/notification behavior. The five steps in draft section 3 satisfy it; keep 30-60 s, unlisted link.
- The service starts only from a foreground Join tap, which fits the while-in-use restriction. Android 17 audio hardening (audit item EM.1) was not run on device; flag only.
- **Record the video now** on the vc 5 Play install (behavior unchanged by D-09) and re-record only if the call UI changes materially.

## Closed test and production access

| Question | Answer | Source |
|----------|--------|--------|
| Rule | 12 testers opted in **continuously for the preceding 14 days** when you apply; Console: Test and release, Production stays disabled until then | [CITED: support.google.com/googleplay/android-developer/answer/14151465] |
| Applies to organization accounts? | Google's page names only "personal accounts created after November 13, 2023" and is silent on organizations; vendors claim exempt; Console showed the gate on this org account 2026-10-30 | [CITED: same page]; [VERIFIED: STATE.md line "closed testing shown: yes (2026-10-30)"]; vendor claims [ASSUMED] |
| Does a new build reset the clock? | No rule says so. Only a tester opting out does. Official FAQ encourages updating in closed testing | [CITED: same page, FAQ] |
| Application form | Three parts. Part 1 about the closed test: how easy it was to recruit, tester engagement (used all features? matched expected production behavior?), summary of feedback and how collected. Part 2 about the app: target audience, value proposition, first-year install range. Part 3 production readiness: changes made from test learning, how readiness was determined | [CITED: same page] |
| Review time | "usually seven days or less, but can occasionally take longer"; outcome by email to the account owner | [CITED: same page] |
| What can go wrong | "fewer than 12 opted-in testers or insufficient tester engagement" means continue testing | [CITED: same page] |
| Closed track prerequisites | "Complete app setup" | [CITED: same page track table] |
| Feedback location | Console: Monitor and improve, Ratings and reviews, Testing feedback; "You must summarize your testing feedback when applying" | [CITED: same page] |

**Truthfulness:** answer Part 1 truthfully, including that testers were recruited through a testing service. Part 3 may honestly list POL-02, BUG-22 and BUG-34 as changes, but they come from internal QA, not tester feedback; do not invent tester-found issues. [ASSUMED] Google has no published rule against paid testers; the page says recruitment is up to the developer.

### Paid tester services compared (vendor-stated, fetched 2026-11-05; prices change, guarantees are marketing)

| Service | Price | Testers | 14-day opt-in | Questionnaire help | Guarantee | Notes |
|---------|-------|---------|----------------|--------------------|-----------|-------|
| Testers Community Pro | CA$33 per app | 25 | "Full 14 days"; delivery within 6 hours; page also says 16 days in places | "Pre-filled Production Access form answers" | "Approved, or your money back" | Feedback report, priority support. Starter: CA$20, 15 testers, same terms. [CITED: testerscommunity.com/pricing] |
| TestFi | $39.99 flat | 16 (20 for $49.99) | 14 days, "twelve survive" | Not mentioned (has a "Compliance report you can show Google") | "twelve opted in on day 14, or we run it again free" | Written feedback from every tester. [CITED: testfi.app/pricing] |
| PrimeTestLab | Starter $19.99 / Professional $59.99 / Enterprise $27.99 (promo) | 12 / 20 / 25 | 14 days | Not stated | "one free retest or a full refund" if access not granted | Starter has no buffer. [CITED: primetestlab.com blog pricing section] |
| onTest | $19.99 flat | 12 | 14 days plus 1 bonus day, daily screenshots | Not stated | 3-day money-back only | No buffer above 12. [CITED: ontest.app blog] |
| 12RealTesters | $49, charged only once progress shows in your Console | 14 | 14 days, one screenshot per tester per day | Not stated | "does not guarantee production access" | Pay-after-visible-progress. [CITED: 12realtesters.com blog] |
| Free exchange apps (Testers Community free, Closed Test Pro) | $0 (test other apps for credits) | 12+ | varies | n/a | none | Slower, more drop-out risk |

**Recommendation: Testers Community Pro (25 testers, CA$33).** It is the only one that combines a buffer of 13 above the minimum, pre-filled production-access answers, a feedback report (feeds Part 1 of the form) and a refund tied to approval. Runner-up: TestFi at $39.99 (16 testers, 12-on-day-14 guarantee, feedback from each tester). Avoid the 12-tester packages without a buffer (onTest, PrimeTestLab Starter), which contradict D-02. Add 2-3 testers of your own. Vendor claims are unverified; the user chooses and pays.

## Release Mechanics

- **Version:** vc 6 and `versionName "1.0.0"` for the first upload after the D-09 fixes; every further upload (closed-track fix builds) bumps by one. [VERIFIED: app/build.gradle.kts:76-77 `versionCode = 5`, `versionName = "0.1.0"`; ledger policy: bump commit changes only that line]. The ledger file says "`versionName` stays `0.1.0` for QA builds; Phase 17 decides the public name": set `1.0.0` at vc 6.
- **Build and upload:** unchanged B1-B8 (main checkout only, never a worktree, keystore never printed, never `gradlew --stop`). AAB path `app/build/outputs/bundle/prodRelease/app-prod-release.aab`.
- **Upload sequence:** closed track gets vc 6, then fix builds vc 7, 8; the last good closed-track release is **promoted** to Production (Console: Closed testing, Promote release, Production), so smoke run 1 and production use one binary. Production setup: countries Canada and United States (D-12), rollout 100% (D-11), managed publishing left off.
- **Release notes ("What's new", 500-character field, Claude's Discretion):** a short factual note, for example "First public release of ClickMe: find experts, book one-on-one voice sessions, chat, pay securely and leave reviews." No ranking or promotional words (Google metadata rule). Closed-track tester instructions: install from the opt-in link, stay opted in 14 days, sign up as a client with your own email, browse and use chat and settings, free topics only, never enter a real card, report problems via the Console feedback or support@clickme.app. [Drafts are discretionary; wording is the planner's.]
- **Asset specs** (all [CITED: answer/9866151]):

| Asset | Spec | Owner |
|-------|------|-------|
| App icon | 512x512, 32-bit PNG with alpha, max 1024 KB, no ranking/price/category text. Export with Image Asset Studio (Google Play Store output) from the existing adaptive icon (background `@color/surface`, foreground `ic_launcher_foreground.xml`); validate with `sips -g pixelWidth -g pixelHeight -g hasAlpha`. Fallback: a one-off Robolectric test that renders the adaptive drawable to a 512 bitmap [ASSUMED feasible] | executor (Studio step is human-assisted) |
| Phone screenshots | Minimum 2, up to 8; JPEG or 24-bit PNG no alpha; each side 320-3840 px; longest side at most 2x the shortest; for eligibility to be featured, at least four at 1080 px minimum, 9:16 (1080x1920); real app UI only, no device frames, no fingers, taglines at most 20% of the image, no "Best/#1/Download now"; clean status bar | executor |
| Feature graphic | 1024x500, JPEG or 24-bit PNG (no alpha); avoid fine detail and edge cutoffs | user (D-16) |
| FGS video | 30-60 s, unlisted link; steps per draft section 3 | user (D-16) |
| Short description | max 80 characters; the draft's 68-character text is fine. Avoid CTAs and ranking words | text exists |
| Full description | max 4,000; reviewed draft is fine; add nothing about Google Play ranking | text exists |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| 20 testers for 14 days (new personal accounts) | 12 testers, 14 days, plus an application Google reviews | Google's page now says 12 [CITED: answer/14151465]; vendors date the change to 2025 [ASSUMED] | Count alone is not enough; engagement is judged |
| FGS: no type needed | Type + permission + Console declaration with video for apps targeting Android 14+ | Android 14 | `microphone` type with a video is required for CallService |
| Account deletion optional | In-app path **and** a web resource required for apps with account creation | Data safety deletion questions mandatory | The web page is a hard requirement for Console submission |
| Play Billing for everything digital | Exemption FAQ for 1:1 online paid services between two individuals, not replayable | present in Help FAQ (undated) | Stripe is allowed for ClickMe's sessions |

**Deprecated/outdated:**
- "Real-time person-to-person services" as the Google exemption wording: it is Apple's phrase; do not quote it to Google.
- Phase 13 draft: "no alpha needed" for the icon, "Phone call or Communication" use case, "Other financial info" for card data.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Closed-track rollout needs the store listing (including graphics) and FGS declaration complete, not just "App content" basics | Pitfall 2 | If stricter, the 14-day clock starts later; if looser, the plan wastes a day on assets first (low cost) |
| A2 | Console per-track country setting; vendor testers outside CA/US need the closed track opened to their countries | Pitfall 6 | Testers cannot install; clock never starts |
| A3 | Vendor guarantees, prices and tester behavior are accurate | Tester table | User pays and still fails engagement review |
| A4 | Org accounts normally exempt (vendor claim) | Closed test | Plan relies on the observed gate instead; user can ask support |
| A5 | Google accepts Stripe as a "service provider" (card data not "shared") | Data safety | A review flag; fix by changing the row to Shared |
| A6 | `wm size 1080x1920` and SystemUI demo mode work on the Samsung One UI | Code Examples | Fall back to a Pixel-2-class AVD (one-time create) |
| A7 | IARC questionnaire wording/category and the resulting rating | Console Declarations | Wrong answer can mean a wrong rating or a re-take (reversible) |
| A8 | Financial features answer "no financial features" | Console Declarations | A review rejection; user confirms |
| A9 | Hero glow consumes about 200 dp of layout and is the main cause of BUG-22 | Pattern 2 | Plan step 1 may not turn the test green; steps 2-3 are the fallback |
| A10 | Production review time (new app) is about a week | Sequencing | Release date shifts, no correctness impact |
| A11 | Retention periods for payment/tax records not in code | Deletion page | Page must say "as required by law" unless the user gives numbers |
| A12 | NFC permission triggers no Console prompt | Decision 2 | A reviewer question; reviewer note covers it |
| A13 | Image Asset Studio emits the Play Store 512 PNG without altering existing launcher resources when configured to the existing drawables | Release Mechanics | A stray overwrite of mipmap XML; review the diff before committing |
| A14 | Paid testers on prod follow "free topics only, no real card" | Pitfall 5 | Unwanted live-mode transactions; mitigated by instructions and cleanup |
| A15 | SavedStateHandle restores only on process death with the task still in recents, never on a cold start | Pattern 1 | A privacy statement in the plan would need to change |

## Open Questions

1. **Is the gate really still shown?**
   - Known: shown on 2026-10-30; Google's page does not mention organization accounts.
   - Unclear: current Dashboard state; whether Google would answer an exemption query quickly.
   - Recommendation: first checklist item; support question in parallel; do not wait for a reply.
2. **Which data safety "shared" answer for Stripe?** Recommendation above (collected, not shared); user decides at the D-05 checkpoint.
3. **BUG-34 cause (no scrolling at 200%).** Unknown; Pattern 2 starts with a failing test.
4. **Unmerged branch `feature/remember-login-email`** (Android commit `0c9ecc55`, ledger "next batch input") and BUG-23: CONTEXT D-09 does not list them. If the user wants the remember-email feature in 1.0.0 it must merge before vc 6. Ask at the checkpoint; default is to leave it out.
5. **Developer name as shown on the store listing** (needed on the deletion page): the user confirms the exact string.
6. **Retention periods** for payment, payout and tax records (deletion page and privacy policy).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 (Android Studio JBR) | gradle gate/build | yes | OpenJDK 25.0.4 | none needed |
| `adb` + Samsung SM-A075F | screenshots, smoke, install proof | yes (device listed over wireless debugging) | adb 1.0.41 | emulator |
| Android emulator + `Medium_Phone` AVD | fallback screenshots | partly (AVD is 1080x2400, fails the 2:1 rule; system images android-34 and android-37.0 present) | — | create a Pixel-2 AVD at 1080x1920 |
| build-tools (aapt2, apksigner, zipalign) | release checks B4 | yes | 34.0.0, 36.0.0 | — |
| Upload keystore `~/keys/clickme-upload.jks`, `keystore.properties`, `app/google-services.json` | signed build | yes | files present | none (hard requirement) |
| `sips` | validate asset size/alpha | yes | macOS | — |
| ImageMagick, Pillow | image processing | no | — | not needed; `sips` suffices |
| Play Console (organization account) | all declarations, tracks | human | verified 2026-10-20 | none |
| uptrendinvestments.net hosting | deletion page, privacy policy edit | human | — | none |
| Paid tester service account/payment | D-02 | human | — | free exchange tier |
| iPhone with iOS expert account, backend seed access, admin portal | smoke, reviewer account, tester cleanup | human | — | — |

**Missing dependencies with no fallback:** none for code. Human steps cannot be automated.
**Missing dependencies with fallback:** a 1080x1920 AVD (create one, or use `wm size` on the Samsung).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 + Robolectric + Compose ui-test (`createAndroidComposeRule`), MockWebServer fakes, `runTest` |
| Config file | `app/build.gradle.kts` (`testOptions.unitTests.isIncludeAndroidResources = true`); ktlint via `ktlintCheck`; `scripts/check-design-rules.sh` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests '<pattern>' ktlintCheck` |
| Full suite command | Ledger gate B2: `./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug`; separately `./gradlew -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g :app:lintDevDebug :app:lintProdDebug`; then `bash scripts/check-design-rules.sh && bash scripts/check-design-rules-selftest.sh` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| POL-02 (note) | Draft restored from SavedStateHandle; `hasChanges` true; key cleared after save; text capped | unit | `... --tests '*BookingNoteViewModelTest' --tests '*BookingNoteScreenTest'` | file exists, new tests Wave 0; ctor call sites at `BookingNoteViewModelTest.kt:36`, `BookingNoteScreenTest.kt:72` |
| POL-02 (feedback) | selectedTypeId/details/user-edited email restored; prefill not duplicated into handle; `toString` stays redacted | unit | `... --tests '*FeedbackViewModelTest'` | exists; new tests Wave 0; ctor at `FeedbackViewModelTest.kt:42` |
| POL-02 (search) | query/category/sort restored; init search uses restored query without debounce | unit | `... --tests '*SearchExpertViewModelTest'` | exists; new tests Wave 0; ctor at `SearchExpertViewModelTest.kt:52` |
| BUG-22 | Banner+chip at font 1.0, 360x800: four controls reachable and above End Call | Robolectric UI | `... --tests '*MeetingCallScreen*'` | `MeetingCallScreenEndButtonRegressionTest` template; new test Wave 0 |
| BUG-34 | Font 2.0 at ~339x753 dp (720x1600 px, 340 dpi), Waiting state: controls reachable by scroll | Robolectric UI | same | new test Wave 0 (`@Config(qualifiers = ...)` with a dpi qualifier [ASSUMED supported]) |
| BUG-10 regression | End Call still pinned and clear of nav bar (5 existing tests) | Robolectric UI | `--tests '*MeetingCallScreenEndButtonRegressionTest'` | exists |
| BUG-22/34 guard | Phone avatar unchanged; a11y traversal order | Robolectric UI | `--tests '*MeetingCallScreenTest' --tests '*MeetingCallHeroTest' --tests '*A11yAuditTest'` | exist |
| REL-07 build | vc 6, `1.0.0`, signed, cert pin, `google_app_id` | script | ledger B3-B4 commands | procedure exists |
| REL-06 / REL-04 Console items | Each declaration shows saved/complete in Console | **human checkpoint** | none (Console screenshot or "Check:" line in `17-RELEASE-CHECKLIST.md`) | Wave 0: create checklist |
| Deletion URL | Page loads, shows app name, path, deleted/kept lists | script + human | `curl -sI <url>` returns 200; grep the page for "ClickMe" and "support@clickme.app" | after the user publishes |
| REL-07 live | Fresh Play install signs up, books free topic, joins call with iOS expert, twice (closed track, public listing) | **human device gate** | none; runbook rows D1.01-D1.0x style plus R1.1 | log result in the checklist |
| Screenshot/icon specs | Dimensions, alpha, ratio | script | `sips -g pixelWidth -g pixelHeight -g hasAlpha <file>` plus width/height ratio check | Wave 0 |

### Sampling Rate
- **Per task commit:** quick run command with the touched test classes.
- **Per wave merge:** full gate B2.
- **Phase gate:** B2 green on the exact build commit, B4 checks pass, then upload; closed-test day-14 and production items are human checkpoints logged in the checklist and STATE (memory: do not block on human checkpoints, except this phase's smoke/Console gates which are the phase goal).

### Wave 0 Gaps
- [ ] Restore tests for `BookingNoteViewModel`, `FeedbackViewModel`, `SearchExpertViewModel` (pattern: `WriteReviewViewModelTest`)
- [ ] Reachability tests for BUG-22 and BUG-34 (fail first)
- [ ] `17-RELEASE-CHECKLIST.md` with "Check:" lines for every human step
- [ ] Asset spec validator commands (`sips`) in the checklist
- [ ] No new framework install needed

## Security Domain

`security_enforcement` is absent in `.planning/config.json` (only `workflow._auto_chain_active`), so it is treated as enabled.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Email deletion requests: verify the sender controls the account email before an admin Terminate; reviewer account password lives only in the backend doc and Console |
| V3 Session Management | no change | Existing token handling; forced-logout flow unchanged |
| V4 Access Control | yes | Terminate is an admin-portal action; limit who can run it; testers must not receive admin or reviewer credentials |
| V5 Input Validation | yes | Note capped at `NOTE_MAX_LENGTH = 5_000` [VERIFIED: screens/bookingnote/BookingNoteUiState.kt:6]; restored text re-capped with `capText`; feedback details re-trimmed on submit |
| V6 Cryptography | no change | Signing keys stay outside the repo; `keystore.properties` and the `.jks` are git-ignored |
| V8 Data Protection | yes | User text in SavedStateHandle stays off logs and `toString()`; privacy policy and Data safety must match what is collected; deletion cascade documented on the web page |
| V14 Configuration | yes | Live Stripe key in the prod flavor: testers must not use real cards; no secrets in planning files |

### Known Threat Patterns for this phase

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Fake deletion request by email (victim's account removed) | Spoofing / Tampering | Require the request from the account's registered email, reply to confirm, then Terminate; never act on an unauthenticated request |
| Reviewer password copied into repo or Console draft files | Information disclosure | Keep it only in `backend-reference/docs/APP-REVIEW-ACCOUNT.md`; the user types it in Console; grep new files for it before commit |
| Tester accounts and real-money flows on production | Tampering / Repudiation | Free topics only, instructions to testers, post-test Terminate of tester accounts, Stripe live-mode awareness |
| Draft text persisted in saved state | Information disclosure | Mirror only the fields needed, never log them, rely on process-death-only restore [ASSUMED A15] |
| Upload key loss or leak | Elevation of privilege | Existing backup (Phase 13 step 3); never print `keystore.properties`; verify the pinned certificate on every AAB |
| Policy rejection from misdeclared data | Repudiation | Data safety must match the privacy policy and the real network traffic (FCM token, Stripe, Agora) |

## Sequencing and Human/Automatable Split

**Timeline (estimates, [ASSUMED] from Google's "seven days or less"):** if the closed test starts about Nov 9-12, day 14 is about Nov 23-26, production-access review about a week, production review a few more days, so the public release lands roughly **Dec 3-10**. Every day of delay in starting the closed test is one day of delay in release (D-01). Slack, if any, comes from starting earlier, not from the fixes.

**Lane A, executor (code, parallel, finish first so testers get the fixes):**
1. POL-02: three VMs plus tests (one plan).
2. BUG-22/34: failing tests, hero/viewport fix, guard tests (one plan; most uncertain).
3. Version bump to vc 6 / `1.0.0`, gate B2, build, B4 checks, archive, ledger row.
4. Write: `17-CONSOLE-PASTE.md` (corrected drafts), `17-DELETE-ACCOUNT-PAGE.html`, privacy-policy edit text, `17-PAYMENTS-DECISION.md`, `17-RELEASE-CHECKLIST.md`, tester instructions, release notes.
5. Screenshots (1080x1920) and the 512 icon; in-call screenshot after step 2.

**Lane B, human, start today in parallel with Lane A:**
1. Check Dashboard gate; open the Play support question; record the answer in STATE.
2. Record the FGS video on the current vc 5 Play install; make the 1024x500 feature graphic.
3. Choose and pay the tester service (D-02); confirm the closed-track country list with them.
4. Publish the deletion page and the privacy policy edit on uptrendinvestments.net.
5. Confirm the D-05 payments decision, the four data-safety decisions, financial-features answer, developer name, retention wording, and whether `feature/remember-login-email` is in 1.0.0 (one checkpoint).
6. Paste Console content; upload vc 6 to a new closed track; start the closed test; re-seed the reviewer account before review submissions.

**Can run during the 14 days:** smoke run 1 on the closed-track install; fix builds (vc 7, 8, ...); drafting the production-access answers from the vendor feedback report; re-seeding the reviewer account; preparing the production release.
**Cannot be shortened:** the 14 continuous days, Google's reviews (production access, then production release).
**Human-only:** every Console action, paying the vendor, publishing web pages, recording the video, making the feature graphic, iOS-side smoke steps, backend re-seed/admin actions. **Executor-automatable:** all code, tests, build/verification, drafts, screenshots/icon (icon via Studio is a 2-minute human assist).

## Forced-logout push-token DELETE (milestone-audit warning 2): defer

Not trivial. Facts: on a refresh rejection, `TokenAuthenticator.rejectSession()` clears tokens and emits `SessionEvent.Rejected` [VERIFIED: network/TokenAuthenticator.kt:80-83 `private fun rejectSession() { val had = tokens.hasSession(); tokens.clear(); if (had) sessionEvents.emit(SessionEvent.Rejected) }`]; `UserManager` maps `SessionEvent.Rejected -> logout()` (line 133) and `logout()` runs `pushTokenRegistrar.unregister()` first (lines 204-205, then `tokens.clear()`). On that path the access token is expired and the refresh token is rejected, so even a retained bearer would be refused; for suspension the server answers 403 `ACCOUNT_SUSPENDED` to every call. A real fix needs a backend change (an unauthenticated delete by device id) and the phase must not change the backend. Mitigations already exist: the app deletes its FCM token locally, and the backend deletes tokens on `messaging/registration-token-not-registered` or `messaging/invalid-registration-token` [VERIFIED: backend-reference/src/services/push.js:166-173 and the batch delete at lines 315-319]. Recommendation: leave it for after release and keep it in the audit's tech debt.

## Sources

### Primary (HIGH confidence, Google first-party, fetched this session)
- https://support.google.com/googleplay/android-developer/answer/14151465 - testing requirements, application form, FAQ on opt-in and updates
- https://support.google.com/googleplay/android-developer/answer/9858738 - Payments policy (no person-to-person-service exemption; Section 3 list)
- https://support.google.com/googleplay/android-developer/answer/10281818 - Payments policy FAQ (1:1 online paid services exemption)
- https://support.google.com/googleplay/android-developer/answer/13392821 - foreground service declaration and video rules
- https://developer.android.com/develop/background-work/services/fgs/service-types - microphone vs phoneCall types
- https://support.google.com/googleplay/android-developer/answer/10787469 - Data safety definitions and data types
- https://support.google.com/googleplay/android-developer/answer/13327111 - account deletion requirements
- https://support.google.com/googleplay/android-developer/answer/9866151 - preview asset specs
- https://support.google.com/googleplay/android-developer/answer/9859455 - App content declarations
- https://support.google.com/googleplay/android-developer/answer/9859655 - content ratings (IARC)
- https://support.google.com/googleplay/android-developer/answer/13849271 - Financial features declaration
- In-repo files read this session (paths cited inline)

### Secondary (MEDIUM confidence)
- https://www.uptrendinvestments.net/clickme/privacy.html - live privacy policy (summarizing fetch)
- https://www.testerscommunity.com/pricing, https://www.testfi.app/pricing - vendor pricing pages

### Tertiary (LOW confidence, vendor marketing or blogs)
- testerscommunity.com, closedtestpro.com, 12realtesters.com, testfi.app, ontest.app, primetestlab.com, vmobify.com blog pages (organization-exemption claims, new-build behavior, other vendor terms)
- Play SDK Index pages for Stripe and Agora could not be read (JavaScript-rendered); the user should open them in a browser to cross-check Data safety guidance

## Metadata

**Confidence breakdown:**
- Standard stack / mechanics: HIGH - existing pipeline, ledger procedure and files read directly.
- Policy answers (payments, FGS, deletion, data safety, assets): HIGH for what Google states; MEDIUM where the mapping to ClickMe is a judgment (Stripe as service provider, financial features, IARC answers).
- Closed-test applicability to this account: MEDIUM-LOW (conflicting sources; Console is the authority).
- Vendor comparison: LOW-MEDIUM (vendor-stated).
- BUG-22/34 root cause: LOW-MEDIUM (evidence supports the glow finding, scroll symptom unproven).

**Research date:** 2026-11-05
**Valid until:** 2026-11-19 for Play policy and vendor pricing (fast-moving); the code findings are valid until the files change.
