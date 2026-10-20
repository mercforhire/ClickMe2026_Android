## Conflict Detection Report

### BLOCKERS (0)

None. Only one LOCKED ADR was ingested (source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md), so LOCKED-vs-LOCKED contradictions can't occur. Mode is new, so there is no existing CONTEXT.md to check against. No doc is UNKNOWN/low-confidence (both classifications are high-confidence and set by the manifest). The cross-ref graph has no cycles: neither ingested doc references the other, and all cross_refs point to external paths (ios-reference/, backend-reference/).

### WARNINGS (2, both resolved by the user on 2026-10-19)

[WARNING] Reschedule scope: Week 5 includes something the Deferred list seems to exclude
  Found: source: docs/PORT_ROADMAP.md, Week 5 §5 says the Android expert should "Accept/decline client reschedule requests (handle `409 SLOT_UNAVAILABLE`)". source: docs/PORT_ROADMAP.md, "Deferred" #1 defers "Client-initiated **Reschedule**, both client and expert sides." Answering a client's reschedule request is arguably the expert side of a client-initiated reschedule.
  Impact: Two readings plan differently. Reading A: only initiating a reschedule is deferred. Android keeps responding to reschedule requests from iOS users in both directions (Week 4 §4 client side, Week 5 §5 expert side). Reading B: the whole client-initiated flow is deferred, and the Week 5 accept/decline moves to v2. Under Reading B, bookings from iOS clients would sit in `pending_reschedule` when the expert is on Android. That is the same failure the PRD's Week 4 §4 gives as its reason for requiring the client-side response. Both readings are kept in .planning/intel/requirements.md under REQ-expert-bookings and DEF-1. Neither was picked.
  → RESOLVED (user): Reading A. Only initiating a reschedule is deferred; responding to one is in the MVP. Deferred #1 in docs/PORT_ROADMAP.md has been reworded.

[WARNING] applicationId uses com.example and can't be registered in Play Console
  Found: source: docs/PORT_ROADMAP.md, "Play Store account" step 3 says to "Register the package name and upload a first internal build early" in Week 1. source: /Users/LeonChen/Documents/ClickMe_Android/app/build.gradle.kts has `applicationId = "com.example.clickme_android"`. The LOCKED ADR's file-organization example (source: CLAUDE.md, "File organization") uses the same `com/example/clickme_android` path.
  Impact: Google Play rejects package names that start with `com.example`. The applicationId is permanent once uploaded, so the Week 1 registration step can't succeed as configured, and choosing it late risks rework. This doesn't contradict the ADR. The ADR's path is a source package (Gradle `namespace`), which can stay as it is while `applicationId` changes. The ADR doesn't lock the applicationId.
  → RESOLVED (user): applicationId, namespace and source folders are now `com.uptrendinvestments.clickmeapp` (matches the iOS bundle ID). CLAUDE.md's path example has been amended. The project compiles.

### INFO (6)

[INFO] iOS-defect fixes are consistent with the locked ADR
  Note: source: CLAUDE.md, "iOS reference" makes Swift the source of truth and says to "Deviate only for Android platform conventions." source: docs/PORT_ROADMAP.md tells Android not to copy several iOS behaviors. Each one was checked and found consistent with the ADR, so none was overridden. (1) Report reasons as enum values: backend-reference/docs/ios/USER-REPORTING.md defines `reason` as a six-value enum, and the ADR names that spec as authoritative. (2) Chat blocked state with composer disabled: backend-reference/docs/ios/CHAT-UNBLOCK.md, which the ADR also names. (3) Overtime cutoff of 3600s: the iOS source itself says "TEMP: cutoff is 5 min for testing; revert to 3600 (1 hour) for production" (ios-reference/ClickMe2026/Views/Screens/Call/Calling/MeetingCallViewModel.swift). (4) Wiring Join in Upcoming Booking Details: the iOS call site in MyBookingsView.swift passes only onReschedule/onCancel, which is a wiring defect in a feature iOS otherwise has. (5) Email verification code sent only on tap: backend-reference/docs/ios/EMAIL-VERIFICATION-ON-REQUEST.md. (6) Splash via the Android 12 SplashScreen API instead of a 3-second delay: a platform convention. Optional: add a line to CLAUDE.md saying that documented iOS defects and backend-spec corrections take priority over iOS code, so the rule is stated outright.

[INFO] PRD adds libraries the ADR doesn't name
  Note: source: docs/PORT_ROADMAP.md, Weeks 1, 2 and 7 add OkHttp, Coil, Navigation Compose, DataStore, socket.io-client-java v2+, the AndroidX SplashScreen API and Chrome Custom Tabs. None contradicts the LOCKED stack in CLAUDE.md "Architecture". OkHttp is Retrofit's transport. DataStore holds non-secret state, while JWTs stay in EncryptedSharedPreferences as the ADR requires. The additions are recorded as PRD requirements, not as locked decisions.

[INFO] Theme consolidation and MVP scope cuts don't violate mirror-the-iOS-tree or iOS-as-truth
  Note: source: docs/PORT_ROADMAP.md, Week 1 §4 merges ~40 per-screen `*Brand.swift` color sets into one Material 3 theme. The PRD also skips Call/Ready and the other "Do not port" items, simplifies visuals ("don't chase pixel parity"), and defers features. The LOCKED file-organization rule (source: CLAUDE.md) is read as governing where ported screens and view models go, and the iOS-as-truth rule as governing how ported features behave. Neither requires porting dead code or every feature in the MVP. Material 3 theming falls under the ADR's platform-convention exception.

[INFO] Locked token-storage library is deprecated upstream
  Note: source: CLAUDE.md, "Architecture" locks EncryptedSharedPreferences for JWT storage, and docs/PORT_ROADMAP.md Week 1 §3 follows it. Google has deprecated the androidx.security-crypto library that provides EncryptedSharedPreferences. It still works, but it won't get new features. This is not a conflict between the documents, and the locked decision stands. It's raised so it can be revisited on purpose rather than found later.

[INFO] Weeks 7 and 8 have no end-of-week checkpoint
  Note: source: docs/PORT_ROADMAP.md has "End of Week N" checkpoints for Weeks 1 to 6 only. Week 7 has none. The Week 6 checkpoint ("every MVP flow except payouts and settings works") implies that Week 7 completes payouts and settings. Week 8 has a release target ("~Dec 11–13", possibly "~Dec 7–8") rather than a checkpoint. No acceptance criteria were invented in .planning/intel/requirements.md. The roadmapper should confirm Week 7 and 8 success criteria with the user.

[INFO] Android handling of the cancellation comment is unspecified
  Note: source: docs/PORT_ROADMAP.md, "iOS issues" lists "The cancellation comment is collected but never sent." This is a report-back item. Week 4 §3 (Cancellation) does not say whether Android should collect and send the comment or leave the field out. Recorded as an open item on REQ-client-cancellation and CON-ios-cancel-comment-dropped. Resolve during phase planning by checking the `cancelClientBooking` contract.
