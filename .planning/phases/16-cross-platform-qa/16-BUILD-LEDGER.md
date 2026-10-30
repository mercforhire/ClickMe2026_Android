# Phase 16 Build Ledger

Purpose: the one place that records every signed Phase 16 release build and the repeatable procedures that produce, check, upload and prove it on the device (D-08, D-15).

Rule: this file holds file names, versionCodes, git shas, sha256 sums and certificate fingerprints only (a certificate fingerprint is public). No passwords, no keystore contents, no keystore.properties values.

## Policy (D-15)

- `versionCode` is a hand-bumped literal in `app/build.gradle.kts` defaultConfig (RESEARCH Pattern 2).
- The first upload is 1. Every later upload is the newest Uploads row plus 1, made in one commit `chore(release): versionCode N` that changes only that line, built from that commit, and then recorded by a `docs` commit that appends the ledger rows with the chore commit's sha.
- `versionName` stays `0.1.0` for QA builds; Phase 17 decides the public name. Play release name is `0.1.0 (N)`.
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
  - `"$BT/aapt2" dump badging <apk>` shows `versionCode='N' versionName='0.1.0'`
  - `"$BT/aapt2" dump resources <apk>` prints a `google_app_id` line
  - `git ls-files | grep -cE '\.jks$|keystore\.properties'` prints 0
- **B5 Archive.** Copy to `/Users/LeonChen/ClickMe-release/`: `clickme-0.1.0-N.aab`, `clickme-0.1.0-N.apk`, `mapping-0.1.0-N.txt` (from `app/build/outputs/mapping/prodRelease/mapping.txt`); record `shasum -a 256`.
- **B6 Ledger.** Append the Uploads row.
- **B7 Upload (human).** Play Console, Internal testing, Create new release, upload the archived AAB, release name `0.1.0 (N)`, notes `0.1.0 (N) internal QA`, Save, Review, Start rollout. Play status values: `not uploaded`, `uploaded (in review)`, `available`.
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

## Fix batches

| Batch | After session | Bugs fixed | versionCode | Gate | Uploaded | Re-test |
|---|---|---|---|---|---|---|

## Notes

- Gate B2 for versionCode 1 passed on HEAD 2a532a0b (test + ktlint + assembleDevDebug; lintDevDebug + lintProdDebug at -Xmx4g; design rules + self-test), all BUILD SUCCESSFUL. Commits after 2a532a0b touch only `.planning/` docs (verified with `git diff --name-only 2a532a0b HEAD | grep -v '^\.planning/'` printing nothing), so the gate was not re-run.
- vc 1 archive sha256: aab be68ecc836fe949e5bfb52f570368cab2d8ff2627cc4c8744d0923fca292e8d1, apk 8ae07d7ee6bb9e17a5ec616a5d33f128a2c9f69018e981d83cca5e22776ea6b8, mapping 1ac2bb1f41b12525057cf8d7fda6151ed443e95e88db2b5c7b4ca04c825f8dce (mapping path app/build/outputs/mapping/prodRelease/mapping.txt confirmed, RESEARCH A12). APK is signed with the v2 scheme (sufficient for minSdk 26).
- vc 1 R8 cold start: Status ok, TotalTime 783 ms, crash lines 0, Login screen visible (button label is "Login", not "Log in" as the plan text assumed; email, password, Forgot Password?, Sign Up all present) (2026-10-30). Fresh install of the archived apk on SM_A075F; no keep rule needed.
- vc 1 available on internal testing (2026-10-30). Installed on SM_A075F from Google Play (installer com.android.vending, versionCode 1, lastUpdateTime 2026-10-30 17:57:21, crash buffer 0), logged in as account B. The sideload was uninstalled first. The Play-generated split reports minSdk 32 for this device's variant; that is bundletool device targeting, not a config change.
- Play Console App integrity upload key certificate SHA-256 confirmed by the user to equal the pin above.
- Play app signing cert (Google-generated key) of the installed base.apk, for Firebase registration (optional user follow-up, not done yet): SHA-256 979e9011c606c65faee98f0895bc3e4e71e57c616ed050936f5c01c2ecc91064, SHA-1 8db73866795076c1d571fb0ffdebd0521cdb0d6a (DN CN=Android, O=Google Inc.).
- Plan-text deviation: the Task 3 check `logcat -s Realtime | state=Connected` cannot pass as written, because no code in app/src/main logs `state=` (the Realtime tag only logs warnings and dropped events, RealtimeService.kt). Realtime is exercised functionally by runbook rows R1 and D1 (chat, push). Not forced to pass.
