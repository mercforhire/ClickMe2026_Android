# Phase 16 Build Ledger

Purpose: the one place that records every signed Phase 16 release build and the repeatable procedures that produce, check, upload and prove it on the device (D-08, D-15).

Rule: this file holds file names, versionCodes, git shas, sha256 sums and certificate fingerprints only (a certificate fingerprint is public). No passwords, no keystore contents, no keystore.properties values.

## Policy (D-15)

- `versionCode` is a hand-bumped literal in `app/build.gradle.kts` defaultConfig (RESEARCH Pattern 2).
- The first upload is 1. Every later upload is the newest Uploads row plus 1, made in one commit `chore(release): versionCode N` that changes only that line, built from that commit, and then recorded by a `docs` commit that appends the ledger rows with the chore commit's sha.
- From versionCode 6 the versionName is `1.0.0` (Phase 17 D-10). The vc 6 bump commit changed the versionCode and versionName lines together; every later upload to any track (closed-track fix builds, production) is the newest Uploads row plus 1 in a commit that changes only the versionCode line. Play release name is `1.0.0 (N)`. Builds up to versionCode 5 were `0.1.0`.
- Play rejects a reused versionCode. No per-fix sideloading: every fix reaches the Samsung through the Play internal track.
- Rejected alternatives: `-P` properties (a forgotten flag silently builds the wrong code) and git-count derivation (jumps on every docs commit, not reproducible).

## Pinned upload certificate

Upload certificate SHA-256 (from `keytool -printcert -jarfile` on the versionCode 1 AAB):

`D8:36:BE:66:14:BC:4C:5C:22:F6:47:73:12:D4:A4:D9:48:AD:A1:50:09:09:BF:39:17:75:18:B2:90:8E:E1:95`

Subject: CN=Leon Chen, OU=Uptrend Investments Inc., O=Uptrend Investments Inc., L=Toronto, ST=Ontario, C=CA. Alias `upload`.

Every later AAB must print this same value. A mismatch means the wrong key signed it: do not upload.

## Build and check procedure

Set once per shell: `JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin"`, `BT=/Users/LeonChen/Library/Android/sdk/build-tools/36.0.0`, `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

- **B1 Where.** The main checkout `/Users/LeonChen/Documents/ClickMe_Android` with all fix commits merged. Never a worktree (it has no keystore.properties). Never print or open keystore.properties. Never `./gradlew --stop`.
- **B2 Gate.** In order:
  1. `./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug`
  2. separately: `./gradlew -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g :app:lintDevDebug :app:lintProdDebug`
  3. `bash scripts/check-design-rules.sh && bash scripts/check-design-rules-selftest.sh`
- **B3 Build.** `./gradlew -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g :app:bundleProdRelease :app:assembleProdRelease`
- **B4 Checks.** All must pass before anything is handed to the user:
  - `"$JBR/jarsigner" -verify app/build/outputs/bundle/prodRelease/app-prod-release.aab` prints `jar verified.`
  - `"$BT/apksigner" verify --print-certs -v app/build/outputs/apk/prod/release/app-prod-release.apk` verifies
  - `"$JBR/keytool" -printcert -jarfile <aab>` SHA256 equals the pin above and, normalized to lowercase without colons, the APK "Signer #1 certificate SHA-256 digest"
  - `"$BT/aapt2" dump badging <apk>` shows `versionCode='N'` and the current versionName (`1.0.0` from vc 6; `0.1.0` for vc 1-5)
  - `"$BT/aapt2" dump resources <apk>` prints a `google_app_id` line
  - `git ls-files | grep -cE '\.jks$|keystore\.properties'` prints 0
- **B5 Archive.** Copy to `/Users/LeonChen/ClickMe-release/`: `clickme-<versionName>-N.aab`, `clickme-<versionName>-N.apk`, `mapping-<versionName>-N.txt` (from `app/build/outputs/mapping/prodRelease/mapping.txt`); record `shasum -a 256`.
- **B6 Ledger.** Append the Uploads row.
- **B7 Upload (human).** Play Console, Closed testing (or Internal testing), Create new release, upload the archived AAB, release name `<versionName> (N)`, notes from 17-CONSOLE-PASTE.md section 11, Save, Review, Start rollout. Play status values: `not uploaded`, `uploaded (in review)`, `available`.
- **B8 Prove on the Samsung.** Runbook Harness H1-H2: `pm list packages -i` shows installer `com.android.vending`, `dumpsys package` shows versionCode N and a lastUpdateTime after the upload. If Play has not updated yet, open the Play listing and tap Update.

## Fix batch procedure

Used by 16-05, 16-07 and any gap plan.

- **F1 Input rows.** Bug-log rows with Owner android and Status open or re-test failed; Severity blocking first, then fix-if-cheap.
- **F2 Per bug.** A JVM regression test (JUnit, Robolectric, Compose ui-test or MockWebServer) that fails before the fix; the fix; the quick run `./gradlew :app:testDevDebugUnitTest --tests 'TESTCLASS' ktlintCheck`; commit `fix(16-NN): BUG-XX summary`; the bug row gets Status `fixed (pending re-test)` and the commit sha. A bug no JVM test can reproduce (TalkBack speech, OEM battery, Play behavior) gets "no JVM repro, device re-test only" in its row.
- **F3 Cross-repo rows.** ios or backend rows that block by D-07 and block the Android release are not edited by the executor: list them in the SUMMARY under "Cross-repo fixes" for the orchestrator (sequential, no worktree, orchestrator commits, D-12).
- **F4 Gate.** B2.
- **F5 Bump.** Set `versionCode` to the newest Uploads row plus 1 and commit only that line as `chore(release): versionCode N`.
- **F6 Build.** B3-B6 from that commit, then a `docs` commit with the ledger rows citing the chore commit's sha.
- **F7 Human.** B7-B8, then re-run every row of the batch plus the smoke (R1.1 login, one booking, one chat message, one call join, one push to a swiped-away app).
- **F8 Results.** Write results to the bug rows (`verified` or `re-test failed`) and the runbook Result cells, and the Fix batches row.

## Uploads

| versionCode | versionName | git sha | AAB sha256 (first 16) | Archive | Cert pin match | Uploaded (date) | Play status | Samsung proof (versionCode, lastUpdateTime, installer) | Rows re-run |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0.1.0 | d31aab18 | be68ecc836fe949e | /Users/LeonChen/ClickMe-release/clickme-0.1.0-1.aab | yes | 2026-10-30 | available | vc 1, 2026-10-30 17:57:21, com.android.vending | |
| 2 | 0.1.0 | f2d6745c | 98e11a4b12e69676 | /Users/LeonChen/ClickMe-release/clickme-0.1.0-2.aab | yes | 2026-11-02 | available; on the Samsung 2026-11-02 20:27:12 before vc 3; superseded by vc 3, no rows run | | |
| 3 | 0.1.0 | 5e776574 | 8bdadb7e4bc4bbf4 | /Users/LeonChen/ClickMe-release/clickme-0.1.0-3.aab | yes | 2026-11-02 | available | vc 3, 2026-11-02 20:43:25, com.android.vending | |
| 4 | 0.1.0 | 469acd78 | f4ea03f0c5add845 | /Users/LeonChen/ClickMe-release/clickme-0.1.0-4.aab | yes | 2026-11-03 | available | vc 4, 2026-11-03 20:25:37, com.android.vending | D1.10, D1.18, D1.19, D1.23, D1.SP1, smoke |
| 5 | 0.1.0 | d5fd82d9 | 5da687b5bfeeef81 | /Users/LeonChen/ClickMe-release/clickme-0.1.0-5.aab | yes | 2026-11-05 | available | vc 5, 2026-11-05 18:12:45, com.android.vending | D2.17 (BUG-30, BUG-31), D2.RS3 and D1.PH1 (BUG-29), smoke 5/5, 34 of the 70 D-14 settings rows |
| 6 | 1.0.0 | 271edc58 | 9ec7495080f93805 | /Users/LeonChen/ClickMe-release/clickme-1.0.0-6.aab | yes | | not uploaded | | |

## Fix batches

| Batch | After session | Bugs fixed | versionCode | Gate | Uploaded | Re-test |
|---|---|---|---|---|---|---|
| A | session-A | BUG-05, BUG-06, BUG-07, BUG-08, BUG-09, BUG-10, BUG-11, BUG-12 | 4 | green | 2026-11-03 | 7 verified, BUG-08 not exercised; smoke pass |
| B | session-B | BUG-29, BUG-30, BUG-31 | 5 | green | 2026-11-05 | 3 verified; smoke 5/5 pass; 33 of 34 settings rows run pass (FS.C3 fail, BUG-34), 36 skipped by user decision |

## Notes

- Gate B2 for versionCode 1 passed on HEAD 2a532a0b (test + ktlint + assembleDevDebug; lintDevDebug + lintProdDebug at -Xmx4g; design rules + self-test), all BUILD SUCCESSFUL. Commits after 2a532a0b touch only `.planning/` docs (verified with `git diff --name-only 2a532a0b HEAD | grep -v '^\.planning/'` printing nothing), so the gate was not re-run.
- vc 1 archive sha256: aab be68ecc836fe949e5bfb52f570368cab2d8ff2627cc4c8744d0923fca292e8d1, apk 8ae07d7ee6bb9e17a5ec616a5d33f128a2c9f69018e981d83cca5e22776ea6b8, mapping 1ac2bb1f41b12525057cf8d7fda6151ed443e95e88db2b5c7b4ca04c825f8dce (mapping path app/build/outputs/mapping/prodRelease/mapping.txt confirmed, RESEARCH A12). APK is signed with the v2 scheme (sufficient for minSdk 26).
- vc 1 R8 cold start: Status ok, TotalTime 783 ms, crash lines 0, Login screen visible (button label is "Login", not "Log in" as the plan text assumed; email, password, Forgot Password?, Sign Up all present) (2026-10-30). Fresh install of the archived apk on SM_A075F; no keep rule needed.
- vc 1 available on internal testing (2026-10-30). Installed on SM_A075F from Google Play (installer com.android.vending, versionCode 1, lastUpdateTime 2026-10-30 17:57:21, crash buffer 0), logged in as account B. The sideload was uninstalled first. The Play-generated split reports minSdk 32 for this device's variant; that is bundletool device targeting, not a config change.
- Play Console App integrity upload key certificate SHA-256 confirmed by the user to equal the pin above.
- Play app signing cert (Google-generated key) of the installed base.apk, for Firebase registration (optional user follow-up, not done yet): SHA-256 979e9011c606c65faee98f0895bc3e4e71e57c616ed050936f5c01c2ecc91064, SHA-1 8db73866795076c1d571fb0ffdebd0521cdb0d6a (DN CN=Android, O=Google Inc.).
- Plan-text deviation: the Task 3 check `logcat -s Realtime | state=Connected` cannot pass as written, because no code in app/src/main logs `state=` (the Realtime tag only logs warnings and dropped events, RealtimeService.kt). Realtime is exercised functionally by runbook rows R1 and D1 (chat, push). Not forced to pass.
- versionCode 2 was set at aa36dd17 and first built without being uploaded. The 16.5 WR-01 fix (f2d6745c) then landed, so vc 2 was rebuilt from f2d6745c and replaced in the archive. Because vc 2 never reached Play, the number stays 2. This one build does not follow the policy that the bump commit is the build commit. B7-B8 (upload, Play install) are still open.
- vc 2 gate: B2 passed in full on f2d6745c (2026-11-02): 5236 tests per flavor with 0 failures, ktlint, assembleDevDebug, both lint variants, design rules and self-test.
- vc 2 B4 checks all passed: `jar verified.`, apksigner verifies (v2), AAB and APK certificate equal the pin, badging shows versionCode 2 and versionName 0.1.0, `google_app_id` present, 0 tracked secrets. jarsigner also prints about 1350 "signed in JarFile but is not signed in JarInputStream" lines; vc 1 prints the same count within 2, and Play accepted vc 1.
- vc 2 archive sha256: aab 98e11a4b12e6967693c3d6c40bb71ceb1090632b5527e037f5bb178550933f35, apk fd8dff3e9f527f5cc14ca522cb15e9ae8137e3e2aeac08d8087a8d1d8cfe4692, mapping 888b69d86003bac4c4214cc2afaa83ca5fd69d21be7a4d8e3e70392806537e25.
- Archive incident (found 2026-11-02): /Users/LeonChen/ClickMe-release/ held only .DS_Store (modified 2026-11-01 23:36). The vc 1 archive, sessions/preflight-results.md and logs/preflight/ were gone, cause unknown. The vc 1 AAB, APK and mapping were restored from app/build/outputs before the vc 2 build overwrote them, and all three match the sha256 values above. The preflight results and P0.7 log were not recovered; 16-03 Task 1 must re-create them.
- vc 3 (2026-11-02) adds the two splash fixes to vc 2: the system splash no longer shows the cropped logo (2466fc09), and the wordmark and tagline use the iOS sizes (cb3b68c6). Built from the bump commit 5e776574. B2 passed on cb3b68c6: 5238 tests per flavor with 0 failures, ktlint, assembleDevDebug, both lint variants, design rules and self-test. The bump commit changes only the versionCode line. All B4 checks pass.
- vc 3 archive sha256: aab 8bdadb7e4bc4bbf462e635d367b3d02aca68bf1d6f4780b0ce3848dc1eb7192f, apk ffb1525d26d8995b3d66fc4c3f1085a730f5adf955f0b05545bbf068867e260e, mapping ad38c1fb3fce7608373cceaabeac4857c4b1c30a5a57d0d99b805dccd65bd955.
- Samsung install history: on 2026-11-02 the Samsung held a debug sideload (installer null, Android Debug key, versionCode 1) that blocked Play updates. It was uninstalled with the user's approval, and Play then installed vc 1 at 19:26:16. Do not sideload to the Samsung during the matrix.
- vc 5 (2026-11-05, batch B) adds BUG-31 (e928cc8e, photo crop circle), BUG-30 (5c3be4c4, Profile Setup Next and Publish clear the navigation bar) and BUG-29 (33951e9c, booking ID on Payment History and expert booking rows) to vc 4. Built from the bump commit d5fd82d9 on branch gsd/phase-16-plan-07, which changes only the versionCode line. B2 passed on 85423656 (the later test-only color-constant commit 32a88358 re-ran the photo tests, ktlint and design rules): 5276 tests in the dev flavor with 0 failures, prod unit tests, ktlint, assembleDevDebug, lintDevDebug and lintProdDebug at -Xmx4g, design rules and self-test. All B4 checks pass: `jar verified.` (1351 "signed in JarFile" lines as before), apksigner verifies (v2), AAB and APK certificate equal the pin, badging versionCode 5 versionName 0.1.0, `google_app_id` present, 0 tracked secrets.
- vc 5 archive sha256: aab 5da687b5bfeeef81804a1acd0714b5c00a0a7e0e373c81cdac6a418e7ffd2b9f, apk 6ab05852c766cdb1f04b810928e7edc8d111ea8686a9e4b35843cf486606d203, mapping 8e66a2c5303be6ab344d44e5941daee4b53e848148a132a1f6e734094af66ede.
- Archive incident 2 (found 2026-11-05 at the vc 5 archive step): /Users/LeonChen/ClickMe-release/ held no vc 1-4 files (aab, apk, mapping), only logs/, sessions/ and .DS_Store (modified 17:54 that day). The sha256 values above stay as the record, but the vc 4 mapping file needed to retrace R8 frames from session B is gone; a vc 4 stack trace cannot be retraced. The user deleted the vc 1-4 aab, apk and mapping files themselves to save disk space, so there is no unknown cause here (incident 1's cause stays unknown). The vc 5 files were copied after the incident and are present.
- Batch B scope: BUG-22 and BUG-23 are deferred to the next batch (fix-if-cheap budget of 3 rows was used by BUG-29, BUG-30, BUG-31). BUG-28's Android half waits for a backend change (no client booking read exposes `decline_reason_text` yet).
- Batch B re-test (2026-11-05): the Samsung updated from Play to vc 5 (installer com.android.vending, lastUpdateTime 2026-11-05 18:12:45, after the 17:59 archive). BUG-29, BUG-30 and BUG-31 verified. New BUG-34 (call screen unreachable controls at font 200%, fix-if-cheap, pairs with BUG-22). 36 of the 70 D-14 rows were skipped by user decision (all 23 TalkBack rows, FS.FB1, FS.Y12, PD.Y3, IM.K7 IM.K8 IM.K9 IM.C5 IM.Y8 IM.Y9 IM.Y16, PL.C9, PL.K12, EM.1), so TalkBack (POL-04 / Phase 15 SC-4) is unconfirmed on a device.
- Next batch input: BUG-22, BUG-34, BUG-23, the BUG-28 Android half once the backend exposes `decline_reason_text`, and one feature request (not a bug): pre-fill the last email on Login after any sign-out (user request 2026-11-05), committed on branch feature/remember-login-email: Android 0c9ecc55 (worktree /Users/LeonChen/Documents/ClickMe_Android-remember-email, gate green, 5292 tests) and iOS b0629d9 (worktree /Users/LeonChen/Documents/ClickMe2026-remember-email, for the user to merge and ship to TestFlight).
- vc 6 (2026-11-06, Phase 17 plan 17-07, D-10) is the public-release build, versionName 1.0.0. Built from the bump commit 271edc58 (changes only the versionCode and versionName lines of app/build.gradle.kts) on top of 4ccd046c. Contains 17-01 (POL-02) and 17-02 (BUG-22, BUG-34), plus the orchestrator fix b80f99e9 (`AppLinks.SUPPORT_EMAIL = "clickmeapp@uptrendinvestments.net"`, the user-confirmed contact). Built from an agent worktree, not the main checkout (plan adaptation: keystore.properties was copied in, gitignored, same absolute storeFile); signing resolves to the same upload key (pin match below).
- vc 6 gate: B2 passed on 4ccd046c (the bump diff touches only two literals): testDevDebugUnitTest 5298 tests and testProdDebugUnitTest 5298 tests, 0 failures, 0 errors; ktlintCheck and assembleDevDebug BUILD SUCCESSFUL; lintDevDebug and lintProdDebug at -Xmx4g BUILD SUCCESSFUL; design rules and self-test OK.
- vc 6 B4 checks all passed: `jar verified.` (1351 "signed in JarFile" lines as before), apksigner verifies (v2), AAB certificate equals the pin and the APK Signer #1 digest d836be6614bc4c5c22f6477312d4a4d948ada1500909bf39177518b2908ee195 matches, badging versionCode 6 versionName 1.0.0, `google_app_id` present, 0 tracked secrets.
- vc 6 archive sha256: aab 9ec7495080f93805bfa9412d075bf6c252bd1bcd048b11f11325e6ddd651e81e, apk ef5d0d0804a121fe30a4fff2457e389d5cf6810f22f94e4981d274019d535d97, mapping b48c18df4ec8472e98951dc292bf6be9ee88e2d8ea099f14b2cd0e65bca5118e. The files were built in the 17-07 executor worktree; the orchestrator copies them to /Users/LeonChen/ClickMe-release/ as clickme-1.0.0-6.aab, clickme-1.0.0-6.apk and mapping-1.0.0-6.txt and re-checks these sums before the worktree is removed.
- vc 6 exported components (merged release manifest, `android:exported` true): `MainActivity` (MAIN/LAUNCHER); `screens.expertmode.payouts.ConnectReturnActivity` (VIEW, BROWSABLE, `clickme://connect`). Library components: Stripe `LinkRedirectHandlerActivity` (`link-popup://complete`), Stripe `StripeBrowserProxyReturnActivity` (`stripesdk://payment_return_url`), Stripe `FinancialConnectionsSheetLiteRedirectActivity` (`stripe://financial-connections-lite`), all browser return deep links with no permission guard (standard Stripe SDK manifest entries); Stripe `StripeGooglePayPaymentDataCallbacksService` (permission `com.google.android.gms.permission.BIND_PAYMENTS_CALLBACK_SERVICE`); Firebase `FirebaseInstanceIdReceiver` (permission `com.google.android.c2dm.permission.SEND`); androidx `ProfileInstallReceiver` (permission `android.permission.DUMP`). `CallService` and `ClickMeMessagingService` are exported=false, as are Firebase's other components and the FirebaseInitProvider.
