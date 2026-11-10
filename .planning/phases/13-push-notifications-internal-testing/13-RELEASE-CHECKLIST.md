# Phase 13 Release Checklist (human-only steps)

Every step below needs your accounts, your Mac or a device, so none of it is done by code plans (D-21: manual steps never block execution). Tick a box only after its "Check:" result is what you see. Each step is its own item, so an interrupted run resumes at the first unchecked box. Steps 6 to 9 do not depend on the build and can be done in any order. Nothing here should ever be pasted into the repo: passwords and tokens stay in your password manager.

Short names used below: `AS_JBR` is `/Applications/Android Studio.app/Contents/jbr/Contents/Home`; `BACKEND` is `/Users/LeonChen/Documents/ClickMe2026 Backend`; `APP` is `/Users/LeonChen/Documents/ClickMe_Android`.

Dependency order for the build path: 3 (keystore) -> 2 (Firebase, needs the upload SHA-1) -> 4 (signed build) -> 5 (upload) -> back to 2 (Play App Signing SHA-1). Step 1 (backend) must be live before step 10.

## 1. Deploy the backend push change (13-02, D-04)

Needs: nothing. Do this before the device checks in step 10; until it is live Android falls back to its event_type mapping (13-01) and background pushes may land on the wrong channel.

1. List what is unpushed (expect `92b4827`, `3ba94b9`, `6daf5c0` on top of `bed4758`):
   `git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" log --oneline origin/main..master`
2. Push: `cd "/Users/LeonChen/Documents/ClickMe2026 Backend" && git push origin master:main` (Railway builds from GitHub).
3. Wait for the Railway deploy to finish, then:
   `curl -s https://clickme-backend-production.up.railway.app/health`

Check: the commit in the `/health` JSON is the same 7 characters as `git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" rev-parse --short=7 master` (expected `92b4827`).

Rollback: in Railway, redeploy the previous deployment, or revert the `quick-261028-a13` commits and push again. iOS tokens and platform-less rows keep the old message either way (pinned by tests).

- [ ] (1) Backend D-04 deployed: `/health` commit equals the pushed master sha

## 2. Add the Android app to the Firebase project (D-20)

Needs: step 3 done for the upload SHA-1; the Play App Signing SHA-1 is added after step 5. FCM itself does not need any SHA-1 to deliver pushes; registering all three keeps Google-service features working later and costs nothing.

1. Firebase console, the existing ClickMe iOS project (the backend's firebase-admin credential is the same project, so nothing changes server side), Project settings, Add app, Android.
2. Package name `com.uptrendinvestments.clickmeapp`, nickname `ClickMe Android`.
3. SHA-1 certificate fingerprints. Debug:
   `"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
   Upload (after step 3; it asks for the store password):
   `"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -list -v -keystore ~/keys/clickme-upload.jks -alias upload`
   Play App Signing (after step 5): Play Console, Test and release, App integrity, App signing key certificate, SHA-1. Add it in Firebase then.
4. Download `google-services.json` and save it as `app/google-services.json` in the repo checkout you build from. One file covers both flavors (same package id, D-20). The file is gitignored; never commit it.
5. Cloud Messaging needs no extra key on the app side (the backend already sends through the project's credential).

Check:
- `cd /Users/LeonChen/Documents/ClickMe_Android && git check-ignore app/google-services.json` prints the path, and `git status --porcelain app/google-services.json` prints nothing.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:processDevDebugGoogleServices` succeeds, and the Gradle warning "google-services.json is missing" no longer appears on a build.

- [ ] (2) Firebase Android app added, `app/google-services.json` in place and ignored

## 3. Create the upload keystore and keystore.properties (Phase 2 02-05 Task 2, D-15, D-16)

Needs: nothing. Claude never sees or generates the passwords.

1. `mkdir -p ~/keys`
2. `"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -genkeypair -v -keystore ~/keys/clickme-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
   Choose a strong store password and key password. Use the corporation name (Uptrend Investments Inc.) and country for the distinguished name.
3. Create `/Users/LeonChen/Documents/ClickMe_Android/keystore.properties` with four keys: `storeFile` (absolute path `/Users/LeonChen/keys/clickme-upload.jks`), `storePassword`, `keyAlias` (`upload`) and `keyPassword`. The two password values are the ones you just chose; write them only in this file. Signing activates only when the file exists (02-05).
4. Back up now (D-16): the keystore file as an attachment and both passwords in your password manager. If the upload key is lost, uploads stop until Google resets it.
5. In Play Console you will choose "Use a Google-generated key" for Play App Signing (step 5), so this key is only the upload key.

Check: `cd /Users/LeonChen/Documents/ClickMe_Android && git check-ignore keystore.properties` prints `keystore.properties`; `test -f ~/keys/clickme-upload.jks && echo ok` prints `ok`; `git ls-files | grep -cE '\.jks$|keystore\.properties'` prints `0`.

- [ ] (3) Upload keystore and `keystore.properties` created, ignored and backed up

## 4. Build and verify the signed prodRelease (02-05 Task 3)

Needs: steps 2 and 3. Both flavors build from the same sources; `prod` points at the production backend.

1. From the repo root:
   `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleProdRelease :app:assembleProdRelease`
2. Bundle signature:
   `"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/jarsigner" -verify app/build/outputs/bundle/prodRelease/app-prod-release.aab` prints `jar verified.`
3. Push config is in the release build (Pitfall 10):
   `~/Library/Android/sdk/build-tools/36.0.0/aapt2 dump resources app/build/outputs/apk/prod/release/app-prod-release.apk | grep google_app_id`
   must print a line. No line means the build has no push; do not upload it (go back to step 2).
4. Install on the Samsung (SM-A075F):
   `~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/prod/release/app-prod-release.apk`
   If it reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (a debug build with another signature is installed), run `adb uninstall com.uptrendinvestments.clickmeapp` first and reinstall.
5. Smoke on the device: log in, the socket connects (`adb logcat -s Realtime` shows state=Connected), a push arrives.
6. Confirm no signing secret is tracked: `git ls-files | grep -cE '\.jks$|keystore\.properties'` prints `0`.

- [ ] (4) Signed prodRelease built, `jar verified.`, `google_app_id` present, smoke passed

## 5. Upload to the internal testing track with Play App Signing (closes REL-03, REL-05 build half)

Needs: step 4. This is the first upload, so `versionCode = 1`, `versionName = "0.1.0"` (app/build.gradle.kts). Bump both by hand for every later upload (Phase 2 D-17); the next upload needs versionCode 2.

1. Play Console (organization account), Create app "ClickMe", English (United States), App, Free; accept the declarations (skip if the app already exists).
2. Test and release, Testing, Internal testing (not "Internal app sharing", which does not register the app), Create new release.
3. When asked about app signing, choose Play App Signing with a Google-generated app signing key (D-16).
4. Upload `app/build/outputs/bundle/prodRelease/app-prod-release.aab`. Release name `0.1.0 (1)`, release notes `0.1.0 (1) internal`.
5. Save, Review release, Start rollout to Internal testing. If Play blocks the rollout on unfinished App content (for example the foreground-service declaration, data safety or privacy policy), save the draft, copy the blocker text, finish step 7 first, then roll out (RESEARCH A6).
6. After the upload, copy the App signing key SHA-1 from App integrity into Firebase (step 2, item 3).

Check: the app dashboard (or Test and release, App integrity) shows `com.uptrendinvestments.clickmeapp`; the internal testing release shows "Available to internal testers" once published.

- [ ] (5) Build uploaded to Internal testing with Play App Signing; package registered

## 6. Add internal testers

Needs: step 5 for the opt-in link. The user list itself can be created any time.

1. Internal testing, Testers tab, Create email list `ClickMe internal`, add your own Google account (up to 100 testers are allowed on this track).
2. Tick the list on the track, Save.
3. When the release shows Published (can take hours), copy the opt-in link from the Testers tab and open it on the phone signed in with the tester account, accept, then install from Google Play.

D-22: the user is the only internal tester at this stage. A wider group on a mix of devices (different Android versions and makers) is due by Phase 16.

Check: the Play Store install of ClickMe (not the sideloaded APK) opens, logs in and reaches the Explore tab (REL-05).

- [ ] (6) Tester list set, opt-in link used, installed from Play

## 7. Paste the Play Console drafts (REL-04)

Needs: nothing (order independent). Use `13-PLAY-CONSOLE-DRAFTS.md` in this folder.

Carried by Phase 17: `17-RELEASE-CHECKLIST.md` sections 8-9 (paste from `17-CONSOLE-PASTE.md`).

1. Grow the app: Main store listing (Grow users, Store presence): paste the app name, short description, full description, category, contact email, privacy policy URL; upload the user-supplied graphics.
2. Policy and programs, App content, Data safety: enter the table answers, encryption and deletion answers, Save as draft (do not submit if you want to review the Decisions list first).
3. App content, Foreground service permissions: add `FOREGROUND_SERVICE_MICROPHONE` for `CallService` using the declaration text and video steps. Record the screen following the video steps and link it (a private unlisted video link is fine).
4. If Play blocks the internal rollout until the declaration is complete (RESEARCH A6), do this step before step 5.5.

Check: App content shows the data safety form and the foreground-service declaration as saved (draft or complete); the store listing page shows no empty required fields.

- [x] (7) Store listing, data safety and foreground-service declaration saved as drafts: done in Phase 17 (2026-11-06)

## 8. Re-check the closed-testing requirement

Needs: nothing (order independent).

1. Play Console Dashboard: look for "Production access" or "Get access to production" tasks. The 12 testers for 14 days rule is stated for personal accounts created after 2023-11-13; the page is silent on organization accounts, and RESEARCH A7 assumes this organization account is exempt.
2. Record the answer here and in STATE: shown or not shown.
3. If it applies, a closed test with the required testers must start long before the Dec 11 to 13 release, so raise it at once for Phase 17.

Check: you can state in one line whether the requirement is shown on this account.

- [ ] (8) Closed-testing requirement re-checked and the answer recorded

## 9. Payments policy for Stripe-paid live calls (RESEARCH Open Question 3, A9)

Needs: nothing (order independent). This is a human decision for Phase 17, not something Phase 13 resolves.

Carried by Phase 17: decided at the 17-08 checkpoint from `17-PAYMENTS-DECISION.md` section 1 (`17-RELEASE-CHECKLIST.md` section 6).

1. Read the Google Play Payments policy: https://support.google.com/googleplay/android-developer/answer/9858738
2. Decide whether paid live 1-on-1 expert voice calls through Stripe fit an exception (services consumed outside the app, physical goods or real-time services) or need Play billing or an alternative-billing program. Note the Play Console "earning money" answer already given was payments for services outside Google Play.
3. Write the decision and the reasoning in STATE for Phase 17 (store listing and review notes depend on it).

Check: one line in STATE says "fits exception: <which>" or "needs Play billing / alternative billing".

- [x] (9) Payments policy decision made and recorded for Phase 17: done in Phase 17 (2026-11-06, D-05 keep Stripe)

## 10. Device smoke for push (D-21; links to the deferred device list in 13-17)

Needs: steps 1, 2 and 4 (use the signed build or the Play install). Samsung SM-A075F, logged in; the iOS app or the backend can trigger events.

1. Fresh install on Android 13 or newer: the notification permission prompt appears after login (not at first launch).
2. Backend token row (13-06): in the Supabase table editor (or SQL) open `device_tokens` filtered to your user id. After login there is one row with `platform = android` (the table keeps one token per account). It is refreshed after backgrounding and returning to the app. After you log out the row is gone. If the row never appears, check `adb logcat` for a registration error and confirm step 2 (a missing `google_app_id` means no Firebase).
3. A background push per category lands on its own channel with the ClickMe bell icon and green accent: booking (Bookings), session start or review nudge (Sessions), chat message (Messages), payout or account (Account and payouts), anything else (General). Check the channel names in Android Settings, Apps, ClickMe, Notifications.
4. Taps open the right screen both from a killed app (cold start) and while the app is running: a chat push opens that thread, a booking push opens Booking Summary or the request, a session push opens the booking.
5. With the notification permission denied the app still works and shows no crash.

Check: items 1 to 5 each behave as described; note any failure with its logcat lines for the Phase 13 verifier.

- [ ] (10) Push device smoke passed (permission, token row, channels, taps)
