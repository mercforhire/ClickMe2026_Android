# Phase 17: Play Store Release - Context

**Gathered:** 2026-11-05
**Status:** Ready for planning

<domain>
## Phase Boundary

ClickMe Android goes public on Google Play from the organization account. The phase covers:
- **REL-06:** the Play Console release requirements. These are the data safety form, the foreground-service (microphone) declaration and its video, the account-deletion URL and the content rating, plus App access, target audience and the store listing.
- **REL-07:** the production release, live in Canada and the US.
- **REL-04:** carried over from Phase 13. The drafts in `13-PLAY-CONSOLE-DRAFTS.md` were never pasted into Console.
- **The closed test:** if production access is still gated, the 12-testers × 14-days closed test happens here.
- **Pre-release fixes:** POL-02 and BUG-22/BUG-34 are fixed before the production build (D-09).
- **Store smoke test:** a fresh install from the store signs up, books a session with the iOS app and joins the call against production.

The phase does not add app features. It does not change iOS. The backend changes nothing, except that it may host or link the deletion page.

</domain>

<decisions>
## Implementation Decisions

### Closed-testing route
- **D-01:** First re-check the Play Console Dashboard for the production-access gate ("Apply for production" / "Get access to production"). On 2026-10-30, after the first internal upload, the gate was shown for this organization account. If it is still shown, run the 12-testers × 14-days closed test before applying for production. The release moves out by at least 14 days plus Google's review of the production-access application. That is accepted, because full iOS parity outranks the date. — **Reversibility:** costly — the 14 days cannot be shortened after the fact. Starting late pushes the release out one for one.
- **D-02:** The 12+ testers come from a **paid tester service**. Recruit a buffer above 12, so that drop-outs don't reset compliance. Research should compare the current services: cost, a guarantee that testers stay opted in for 14 days, and help with the production-access questionnaire. The user chooses and pays for the service. That is a human step.
- **D-03:** Testers only need to install the app, stay opted in and use it lightly now and then. No guided task list. Collect light feedback, enough to answer Google's production-access form (what testers did, what feedback came in, what changed).
- **D-04:** Fix builds may go to the closed track during the 14 days. The user's understanding is that Google counts opted-in testers, not builds, so the clock keeps running. **Research must confirm this against Google's current rule.** If an update does reset anything, the planner must schedule all code fixes (D-09) before the closed test starts.

### Policy answers
- **D-05:** Payments: Stripe charges for paid live 1-on-1 expert voice sessions, not Play Billing. **Research checks the current Google Play Payments policy** (https://support.google.com/googleplay/android-developer/answer/9858738) for the exemption that covers real-time person-to-person services. The user confirms the answer before submission, so the plan needs a decision checkpoint. This closes Phase 13 release-checklist step 9. — **Reversibility:** costly — if the conclusion is wrong, the app can be rejected or get a policy strike, and moving to Play Billing would break parity with iOS and the backend.
- **D-06:** Account-deletion URL: **create a new web page**, for example `https://www.uptrendinvestments.net/clickme/delete-account.html`. The plan writes the page content: the in-app steps (Settings → Delete Account, which exists from Phase 14) and a request path that needs no app, by email to `support@clickme.app`. The page must also say what is deleted and what is kept, matching the backend deletion cascade. The user publishes it on the uptrendinvestments.net site, a human step. The URL goes into the Data safety form and the Console's account-deletion field. Also update the line in the Phase 13 drafts that says "until then the support email handles requests".
- **D-07:** App access, the reviewer login: **reuse `appreview@clickme.test`**, documented in `backend-reference/docs/APP-REVIEW-ACCOUNT.md`. It holds both the client and expert roles, has seeded sessions, and can be re-created after a reviewer deletes it. Its re-seed procedure covers Google reviewers too. The Console instructions must explain how to reach a call or booking, and that payouts are not set up. The password is in the backend doc. **Do not copy it into any planning file**. The user enters it in Console.
- **D-08:** Target audience: **18 and over only**. Content rating questionnaire: users interact (chat and voice between users), there are in-app purchases or payments (paid sessions), and there is no violence, gambling or similar content. The app does not join the Families program. — **Reversibility:** reversible — this can be changed in Console later.

### Release build & rollout
- **D-09:** Fix these before the production build, ideally before the closed-test build so testers run them:
  - **POL-02:** move `BookingNoteViewModel`, `FeedbackViewModel` and `SearchExpertViewModel` to `SavedStateHandle`, so the note draft, the feedback form and the search query survive process death.
  - **BUG-22:** the call controls (Minimize, Chat, Mute, Speaker) are hidden behind the pinned End Call button in the last 5 minutes, when the banner and the time-left chip are showing. This happens at font scale 1.0.
  - **BUG-34:** the call controls can't be reached at 200% font with the largest display zoom, on the pre-join or waiting call screen.

  Use the Phase 16 bug log (`16-BUG-LOG.md`) for the details. iOS parity and the existing tests must stay green.
- **D-10:** The public release is **versionName `1.0.0`**. versionCode continues from 5 and goes up by one for every upload to any track. It is set in `app/build.gradle.kts`.
- **D-11:** Production rollout goes to **100% at once** when Google approves it. No staged rollout and no managed publishing. — **Reversibility:** costly — a rollout can be halted, but installs already done can't be pulled back. Only a higher versionCode can fix them.
- **D-12:** Countries at launch: **Canada and the United States** only. — **Reversibility:** reversible — more can be added in Console later.

### Store smoke & assets
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Play Console content (REL-04 / REL-06)
- `.planning/phases/13-push-notifications-internal-testing/13-PLAY-CONSOLE-DRAFTS.md`: paste-ready drafts of the store listing, data safety table and foreground-service declaration (text and video steps), and the open "Decisions for the user".
- `.planning/phases/13-push-notifications-internal-testing/13-RELEASE-CHECKLIST.md`: human release steps. Step 7 (paste the drafts), step 8 (closed testing) and step 9 (payments policy) are still unchecked.
- `.planning/v1.0-MILESTONE-AUDIT.md`: the release verdict, the requirements that need attention (REL-04/06/07, POL-02), and the accepted deviations.

### Reviewer access & account deletion
- `backend-reference/docs/APP-REVIEW-ACCOUNT.md`: the reviewer account (`appreview@clickme.test`), what the reviewer sees, the deletion guard, and the re-seed procedure. The password stays in this doc only.
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/`: the in-app deletion flow that the web page must describe.
- `app/src/main/java/com/uptrendinvestments/clickmeapp/config/AppLinks.kt`: `PRIVACY_POLICY` URL. Add a deletion-page constant here if the app links to it.

### Pre-release fixes (D-09)
- `.planning/phases/16-cross-platform-qa/16-BUG-LOG.md`: rows for BUG-10, BUG-22 and BUG-34, with the evidence.
- `.planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md`: the device setup and accounts used in Phase 16, to reuse for the smoke test.

### Project
- `.planning/REQUIREMENTS.md`: REL-04, REL-05, REL-06, REL-07.
- `.planning/STATE.md`: the Phase 13/16 release notes, the Stripe live-mode notes, and the decision to block long-lead paid bookings.
- `CLAUDE.md`: the project conventions (the iOS reference is the source of truth).

### External
- Google Play Payments policy: https://support.google.com/googleplay/android-developer/answer/9858738
- Google Play's testing requirements for new developer accounts (12 testers × 14 days) and the production-access application. Research finds the current pages.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- The signed release pipeline already exists: the upload keystore `~/keys/clickme-upload.jks`, `keystore.properties`, `bundleProdRelease`, Play App Signing, and the internal track at versionCode 5 (`app/build.gradle.kts:76-77`).
- In-app account deletion (Phase 14, `screens/settings/deleteaccount/`) meets Play's in-app requirement. Only the web URL is missing.
- Verify-command pattern: run JDK 25 via the Android Studio JBR, then `./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests ... ktlintCheck`, plus `bash scripts/check-design-rules.sh`.

### Established Patterns
- Process-death safety uses `SavedStateHandle` with assisted injection. Phase 6 (booking) is the reference implementation for POL-02.
- The call screen lives in `screens/call/calling/MeetingCallScreen.kt`. The BUG-10 fix (End Call pinned) is what caused BUG-22.
- Human-only Console steps belong in a checklist file, as Phase 13 did with `13-RELEASE-CHECKLIST.md`. Manual checkpoints don't block automation (memory: manual testing is deferred), except at Phase 16-style device gates.

### Integration Points
- Play Console: the closed track, production, App content, store listing, and countries.
- The uptrendinvestments.net website hosts the deletion page next to `privacy.html`.
- The production backend (Railway) and the iOS production app take part in the smoke test.

</code_context>

<specifics>
## Specific Ideas

- Smoke accounts: a brand-new client email created on the Play install, and the user's existing iOS expert account with a free topic. Join inside the join window.
- Name accounts and phones by role ("Android client on the Samsung", "iOS expert on the iPhone"), never as A/B (memory: say client/expert, not A/B).
- The foreground-service video follows the five steps in `13-PLAY-CONSOLE-DRAFTS.md` §3.

</specifics>

<deferred>
## Deferred Ideas

- Accepted release deviations stay open after release: FND-13, PAY-05/CR-01 (no live 3DS), PAYOUT-02, POL-04 (TalkBack), REL-05 (single internal tester), and the Phase 16 post-release bugs (BUG-04, 18–21, 23, 28).

### Reviewed Todos (not folded)
- **Sync Android bug fixes to iOS** (`.planning/todos/pending/2026-11-02-sync-android-bug-fixes-to-ios.md`, major). This is iOS work and outside the Play release. It includes the iOS 5-minute overtime cutoff that affects live users. Track it separately; it should not wait on Phase 17.

</deferred>

---

*Phase: 17-play-store-release*
*Context gathered: 2026-11-05*
