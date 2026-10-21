---
phase: 04
slug: signup-onboarding-mode-switch
status: verified
threats_open: 0
asvs_level: 1
created: 2026-10-21
---

# Phase 04 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.
> Source register: the `<threat_model>` blocks of 04-01..04-20-PLAN.md (54 threats, T-04-01 .. T-04-54; T-04-54 is authored in 04-06-PLAN).
> Verified against the code at commit a80b519. `<pkg>` below means `app/src/main/java/com/uptrendinvestments/clickmeapp`; `<test>` means `app/src/test/java/com/uptrendinvestments/clickmeapp`.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| App -> backend REST | Signup credentials, profile PII and the avatar image leave the device | Email, password, name, city/country, timezone, languages, tags, JPEG avatar, 6-digit email code |
| Backend -> app | Signup tokens, profile data and error bodies are parsed from the network | JWT access/refresh tokens, profile JSON, error envelopes (may be HTML or empty) |
| System photo picker -> app | An untrusted image of any size/format arrives as a content URI | Image bytes (EXIF/GPS metadata possible) |
| App-private working files | Cropped/processed photo held in `cacheDir/signup` between steps | PII image files |
| Session boundary (logout -> next login/signup) | Draft PII and photo files must not survive into another session | Setup draft, verification send state, photo files |
| Saved navigation / instance state | Keys and ViewModel state survive process death | Email, name, city, country, languages, email code (never the password) |
| Local mode / onboarding flags -> UI routing | Local flags decide which shell renders; server authorizes every expert endpoint by JWT role | `currentMode`, `hasSeenOnboarding`, setup-deferred flag |
| Source -> release artifact | R8 shrinking of new network and UI code; bundled PNG assets | Moshi adapters, mode-switch PNGs |

---

## Threat Register

Status: all 54 closed. Evidence was located in the code (file:line), not taken from SUMMARY or PLAN text.

| Threat ID | Category | Component | Disposition | Mitigation / Evidence | Status |
|-----------|----------|-----------|-------------|-----------------------|--------|
| T-04-01 | Tampering | SetupExpertProfileRequest (mass assignment) | mitigate | Typed Moshi DTO with only schema keys, `<pkg>/model/requests/SignupRequests.kt:36-52`. `serializeNulls` appears only in a KDoc comment (line 31); Moshi built without it (`network/MoshiProvider.kt`). Exact-body MockWebServer tests `<test>/network/ClickMeApiTest.kt:267-305` | closed |
| T-04-02 | Information disclosure | SignupRequest / SignupData in logs | mitigate | `SignupRequest.toString()` redacts email and password, `SignupRequests.kt:16`; `SignupData.toString()` redacts both tokens, `model/responses/SignupData.kt:19-21`. HTTP logging is `Level.BASIC` plus `redactHeader("Authorization")` in the debug source set only, `app/src/debug/.../network/DebugNetwork.kt:15-17`, asserted at `app/src/testDebug/.../DebugNetworkTest.kt:27`. Tests `ResponseDecodingTest.kt:296-312` | closed |
| T-04-03 | Information disclosure | Bearer on /auth/signup | mitigate | `/auth/signup` in `publicPaths`, `network/ApiRequestUrls.kt:34-41`; `AuthHeaderInterceptor` strips Authorization on public paths (`network/AuthHeaderInterceptor.kt:15-17`). `ApiRequestUrlsTest.kt:11-39` asserts signup false and email/setup/meta/avatar true | closed |
| T-04-04 | Denial of service | Error-body parsing | mitigate | `missing_fields` optional, `model/responses/ApiErrorEnvelope.kt:19`; parse wrapped in `runCatching`, blank body returns null, `network/ApiErrorParser.kt:19-22`. Tests `ApiErrorParserTest.kt:194-203` (HTML/empty/null), `:286-313` (missing_fields) | closed |
| T-04-05 | Tampering | Pre-committed mode-switch PNGs | mitigate | `file` reports both `PNG image data, 512 x 512`. `git hash-object` of working-tree files equals the blobs in 24d311f for both files (mode_client e078a56, mode_expert f05871a); `git log` shows 24d311f is the only commit touching them. Used only via `R.drawable` (`screens/settings/modeswitching/subviews/ModeSwitchCard.kt:128`); no URL fetch in settings/onboarding/signup code | closed |
| T-04-06 | Tampering | country_code / timezone values | mitigate | `Countries.normalize` emits only the alpha-3 allowlist, regex-validated alpha-3, or null (`constants/Countries.kt:71,91-97`), used at `SignupSetupRepository.kt:237,354`. Timezone ids are picker entries drawn from bundled ids or `ZoneId.getAvailableZoneIds()` (`constants/Timezones.kt:29,49-58`). 422 shown inline (`SignupTimezoneViewModel.kt:136`) | closed |
| T-04-07 | Tampering | Weak signup passwords | accept | See Accepted Risks Log AR-04-07 | closed |
| T-04-08 | Spoofing | Server message in AuthErrorDialog | accept | See AR-04-08 | closed |
| T-04-09 | Tampering | Picker search input | accept | See AR-04-09 | closed |
| T-04-10 | Denial of service | Oversized or malformed image decode | mitigate | Bounds-only decode first (`screens/signup/profilephoto/AndroidPhotoProcessor.kt:50-58`), power-of-two `inSampleSize` to at most 2048 px (`:72-76`, `PhotoProcessor.kt:50-65`), failures mapped to `PhotoImportException` including OOM and SecurityException (`AndroidPhotoProcessor.kt:91-99`). `PhotoProcessorTest.kt:58,80,127,141` | closed |
| T-04-11 | Information disclosure | EXIF/GPS in the user's photo | mitigate | `ExifInterface` is only read for orientation (`AndroidPhotoProcessor.kt:62-65`); both working copy and crop are written by `Bitmap.compress(JPEG)` from pixels (`:87-89`, `:126`). No `saveAttributes` or EXIF copy anywhere in `main` | closed |
| T-04-12 | Elevation of privilege | Picker URI reuse | mitigate | URI is a lambda parameter passed once: `SignupProfilePhotoScreen.kt:79-80` -> `SignupProfilePhotoViewModel.onPicked` (`:85-90`) -> `importPicked`. The ViewModel has no SavedStateHandle and no URI field; no `rememberSaveable` in `screens/signup`; the only `Uri` references in signup/navigation code are that parameter | closed |
| T-04-13 | Information disclosure | PII image files left on disk | mitigate | Files only in `File(context.cacheDir, "signup")` (`AndroidPhotoProcessor.kt:42,185`). `clearWorkingFiles` called on save (`SignupProfilePhotoViewModel.kt:125`), remove of unsaved pick (`:156`), reset/logout/publish (`SignupSetupRepository.kt:386`), and by `UserManager` cleaners (see T-04-18). Backup excluded: `allowBackup="false"` plus `res/xml/backup_rules.xml` and `data_extraction_rules.xml` exclude all domains | closed |
| T-04-14 | Information disclosure | Signup tokens at rest | mitigate | `UserManager.signup` -> `startSession` -> `tokens.save(pair)` (`managers/UserManager.kt:152-172`), same path as login; the store is `EncryptedSharedPreferences` with AES256_GCM/AES256_SIV (`managers/EncryptedPrefsTokenStore.kt:116-125`). `SignupData.toString` redacts. Only logging in the store is exception class names (`:76,86,128,131`) | closed |
| T-04-15 | Elevation of privilege | GET /expert/profile converts client-only account | accept | See AR-04-15 (bounded further by the T-04-54 role guard) | closed |
| T-04-16 | Information disclosure | Stale UI during logout | mitigate | `_isLoggedIn.value = false` (`UserManager.kt:206`) precedes `_currentMode`/`_hasSeenOnboarding` reset (`:207-208`). Tests `UserManagerTest.kt:918` (`logoutFlipsLoggedInBeforeModeReset`) and route recorder `AppViewModelTest.kt:547-566` (`logoutFromExpertHomeNeverShowsClientHome`) | closed |
| T-04-17 | Spoofing | Local mode value tampered on rooted device | accept | See AR-04-17 | closed |
| T-04-18 | Information disclosure | Cross-user draft leakage after logout | mitigate | `@Singleton SignupSetupRepository` observes `isLoggedIn.drop(1).filter { !it }` and calls `reset()` (`SignupSetupRepository.kt:110-117`); `reset()` clears draft, hydration, send state, deletes photo files, bumps generation (`:377-387`); also called after publish (`SignupReviewViewModel.kt:96`). Belt-and-braces: `SessionDataCleaner` bound via Hilt `@IntoSet` (`di/PhotoModule.kt:23-24`) runs on logout, before login, before signup (`UserManager.kt:142,156,197,216-220`). Tests `SignupSetupRepositoryTest.kt:795,826,839`, `UserManagerTest.kt:1194,1235,1260` | closed |
| T-04-19 | Tampering | Destructive `[]` on list fields | mitigate | `listFieldFor` (`SignupSetupRepository.kt:57-65`) used by `saveBasicInfo` (`:244-245`) and `saveTags` (`:265`); `saveBasicInfo` takes `languages: List<LanguageItem>?` so an untouched list is omitted. Test `SignupSetupRepositoryTest.kt:445-448` (three cases) | closed |
| T-04-20 | Tampering | Stray keys rejected by `.strict()` | mitigate | DTO has no `profile_image_url` field (`SignupRequests.kt:36-44`); `patch` returns before the call when `hasAnyField()` is false (`SignupSetupRepository.kt:281,416-422`) | closed |
| T-04-21 | Information disclosure | PII in logs | mitigate | `grep` for `Log.`, `println`, `Timber`, `printStackTrace` across `<pkg>` matches only `EncryptedPrefsTokenStore` and `RealtimeService`, which log exception class names or event names. None in the repository or any signup screen/ViewModel | closed |
| T-04-22 | Elevation of privilege | Selecting Expert on Mode Switcher | accept | See AR-04-22 | closed |
| T-04-23 | Information disclosure | Carousel content | accept | See AR-04-23 | closed |
| T-04-24 | Denial of service | Rendering a large bitmap | mitigate | Crop source is the bounded working copy (`AndroidPhotoProcessor.kt:112,151`, max 2048 px by `importPicked`); load failure caught and shown as the "Couldn't open photo" dialog (`PhotoCropViewModel.kt:58-81`) | closed |
| T-04-25 | Information disclosure | Crop state in saved state | accept | See AR-04-25 (verified: `PhotoCropViewModel.kt:173-175` stores only zoom factor and normalised offsets) | closed |
| T-04-26 | Information disclosure | Password in SavedStateHandle / nav keys / logs | mitigate | Only `savedStateHandle[EMAIL_KEY]` is written (`SignupInitialViewModel.kt:69`); password held in `_uiState` only; `SignUpKey` is a `data object` (`navigation/AuthKeys.kt:34`); no `rememberSaveable` in the Initial screen; no password literal anywhere in `app/src/main` | closed |
| T-04-27 | Information disclosure | Account enumeration via 409 | accept | See AR-04-27 | closed |
| T-04-28 | Denial of service | Double submit | mitigate | `if (current.isSubmitting) return` (`SignupInitialViewModel.kt:99`); test `SignupInitialViewModelTest.kt:241` (`secondSubmitWhileInFlightMakesNoSecondCall`) | closed |
| T-04-29 | Spoofing | Weak password | accept | See AR-04-29 | closed |
| T-04-30 | Repudiation | Accidental logout from hub | mitigate | Overflow item calls `onRequestLogout`; `AlertDialog` gates `confirmLogout` (`screens/signup/overview/SignupOverviewScreen.kt:154,204-221`; `SignupOverviewViewModel.kt:106-115`) | closed |
| T-04-31 | Information disclosure | Draft shown after session change | mitigate | `SignupOverviewViewModel` derives `rows`/progress from `repository.draft` via `combine` and keeps no draft copy (`SignupOverviewViewModel.kt:52-69`); repository resets on logout (T-04-18) | closed |
| T-04-32 | Tampering | Free-text name and city | mitigate | `firstName.trim()`, `city.trim()`, `provinceState.trim()` into typed DTO (`SignupSetupRepository.kt:232-246`); 422 mapped to field errors (`SignupBasicInfoViewModel.mapFailure`) | closed |
| T-04-33 | Information disclosure | Name/city/country in saved-state Bundle | accept | See AR-04-33. Manifest confirmed: `android:allowBackup="false"` at `app/src/main/AndroidManifest.xml:9` | closed |
| T-04-34 | Tampering | Language ids | mitigate | Selection only via `toggleLanguage(item)` on catalog items from `api.getLanguages()` (`SignupBasicInfoViewModel.kt:267` `toggleLanguage`, catalog load `:291-296`); the one hard-coded default `ENGLISH = LanguageItem("en", "English")` is a real catalog id (`backend-reference/supabase/migrations/20260603040000_seed_languages.sql:29`). No free-text language input | closed |
| T-04-35 | Tampering | Invalid or legacy zone id | mitigate | `Timezones.pickerEntries`/`entryFor` (`constants/Timezones.kt:61-77`) feed the picker (`SignupTimezoneViewModel.kt:88-94`); user selection comes only from those entries; 422 shown inline (`:136-141`) | closed |
| T-04-36 | Tampering | Tag values | mitigate | `select(tag: ExpertiseTagItem)` operates on catalog items only (`SignupTagsViewModel.kt:133` `select`); no free-text entry path | closed |
| T-04-37 | Tampering | Accidental wipe of saved tags | mitigate | `saveTags` routes through `listFieldFor(...)`; `tagIds == null` leaves the draft untouched and sends no field (`SignupSetupRepository.kt:264-269`, with `hasAnyField` guard at `:281`) | closed |
| T-04-38 | Denial of service | Email-send abuse via Resend | mitigate | No auto-send: `send()`/`resend()` only from buttons (`SignupVerifyEmailViewModel.kt:182`; the screen's only `LaunchedEffect` keys on `done`). Cooldown guard `if (cooldownActive()) return` (`:191`), 30 s `COOLDOWN_MS` (`:284`), stored in the singleton via `repository.recordCodeSent` (`:230`; `SignupSetupRepository.kt:333-335`). `ErrorCode.RateLimited` treated as sent (`:206`) | closed |
| T-04-39 | Spoofing | Verification-code brute force | mitigate | `verify()` has no retry loop; `handleVerifyError` maps 429/`TooManyRequests` to an inline message (`SignupVerifyEmailViewModel.kt:266-285`); single in-flight guard `verifyState != Idle` | closed |
| T-04-40 | Information disclosure | Code in saved state or logs | accept | See AR-04-40 (verified: `savedStateHandle[KEY_CODE]` at `SignupVerifyEmailViewModel.kt:169`, no logging) | closed |
| T-04-41 | Elevation of privilege | Picker URI | mitigate | See T-04-12. `SignupProfilePhotoViewModel` has no `SavedStateHandle` import or field (`:49-54`) | closed |
| T-04-42 | Denial of service | Upload spam | mitigate | `save()` returns unless `saveState == Idle` (`SignupProfilePhotoViewModel.kt:117`), `requestRemove` likewise (`:152`); ApiException (incl. 429) surfaced via `toDialogError` once, no retry (`:136-145`) | closed |
| T-04-43 | Tampering | Wrong multipart part type | mitigate | `MultipartBody.Part.createFormData("image", "avatar.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))` (`SignupSetupRepository.kt:304-305`). Tests `SignupSetupRepositoryTest.kt:477`, `SignupProfilePhotoViewModelTest.kt:181` | closed |
| T-04-44 | Information disclosure | Photo files left in cache | mitigate | `clearWorkingFiles()` after upload (`SignupProfilePhotoViewModel.kt:125`) and on remove of an unsaved pick (`:156`); logout/publish/login/signup via T-04-18 | closed |
| T-04-45 | Elevation of privilege | Bypassing the client Publish gate | accept | See AR-04-45 (verified: `publish()` maps 422 `missing_fields` to steps, `SignupSetupRepository.kt:365-369`) | closed |
| T-04-46 | Information disclosure | Draft PII after publish | mitigate | `repository.reset()` after the Published! hold (`SignupReviewViewModel.kt:94-96`) | closed |
| T-04-47 | Information disclosure | Carousel flash for returning user | mitigate | `loadPersistedState()` loads `hasSeenOnboarding` before `tokens.hasSession()` in `restoreInternal` (`UserManager.kt:253-254,266-272`) and `offlineDestination` loads it when restore has not (`:352`). Gate reads the StateFlow directly (`HomeClientViewModel.kt:33`, `HomeClientScreen.kt:54-57`). Test `UserManagerTest.kt:1277` (`offlineDestinationLoadsPersistedOnboardingFlag`) | closed |
| T-04-48 | Tampering | Debug scaffold mode toggle | mitigate | Deleted. `showDebugAffordances` blocks in `HomeClientScreen.kt:135-145` and `HomeExpertScreen.kt:99-109` hold only the design gallery and force-token-refresh buttons. The only `setMode(` callers are inside `UserManager` (`:488-503`); mode changes come via `AppViewModel.switchMode` (`AppViewModel.kt:121`). No debug mode strings in `res` | closed |
| T-04-49 | Information disclosure | Data in nav keys | mitigate | All eight setup keys are `@Serializable data object` (`navigation/SignupSetupKeys.kt:14-42`: Hub, PersonalDetails, Timezone, ProfilePhoto, PhotoCrop, VerifyEmail, Tags, Review); no `data class` in that file. See Informational note I-1 on saved-state PII | closed |
| T-04-50 | Tampering | Restored deep entry with empty draft | mitigate | `LaunchedEffect(Unit) { hostViewModel.ensureHydrated() }` at the setup host (`navigation/SignupSetupNavHost.kt:40`) -> `repository.ensureHydrated()` (`SignupSetupHostViewModel.kt`) | closed |
| T-04-51 | Elevation of privilege | Returning to Signup Initial after signup | mitigate | Root `when` renders `AuthNavHost` only for `AppRoute.Login` and `SignupSetupNavHost` for `AppRoute.SignupSetup` (`ClickMeApp.kt:56-66`); the setup host has no `SignUpKey`/Login entry (`SignupSetupNavHost.kt:46-79`); `SignUpKey` is referenced only in `AuthNavHost`/`AuthNavigator` | closed |
| T-04-52 | Tampering | R8 stripping Moshi adapters | mitigate | Script-checked every class in `model/requests` and `model/responses` (38 classes): all carry `@JsonClass(generateAdapter = true)`. KSP generated adapters exist under `app/build/generated/ksp/prodRelease/...` (e.g. `SetupExpertProfileRequestJsonAdapter`, `SignupDataJsonAdapter`). `ksp(libs.moshi.kotlin.codegen)` in `app/build.gradle.kts:122`; `MoshiProvider` has no reflection factory. R8 `optimization { packageScope = androidx/kotlin/kotlinx }` leaves app packages outside shrink scope (no `model.requests` entries in the R8 mapping.txt) | closed |
| T-04-53 | Information disclosure | Secrets in new code | mitigate | `bash scripts/check-design-rules.sh` prints `Design rules: OK` (Stripe `sk_/rk_` pattern sweep). Independent grep for `sk_live/sk_test/rk_*`, AWS key ids, PEM headers, and `password|apiKey|secret = "..."` literals over `app/src` found none. Pre-existing `pk_` publishable keys and Agora app id in `build.gradle.kts` are Phase 1, public by design | closed |
| T-04-54 | Elevation of privilege | Background setup re-check on foreground | mitigate | `recheckSetupIfPending`: returns when nothing pending or deferred (`UserManager.kt:409-414`); needs known roles (`:415`); role guard clears the flag with no request unless Expert role or Expert mode (`:416-419`). Tests assert `expertProfileCalls == 0`: `UserManagerTest.kt:731,797,815` (`recheckWithNothingPendingMakesNoCall`, `meFailureMarksPendingAndRecheckWaitsForRoles`, `recheckRoleGuardSkipsClientOnlyAccountAndClearsFlag`), `AppViewModelTest.kt:601-613` (`foregroundForClientOnlyAccountNeverCallsExpertProfile`) | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

Totals: 54 threats, 41 mitigate (all closed), 13 accept (all logged below), 0 transfer.

---

## Accepted Risks Log

Rationale is carried from the plan's `<threat_model>`; the right-hand column records what the audit confirmed in code where a claim was checkable.

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-04-07 | T-04-07 | Client password rule (8+, a letter, a digit) mirrors the server for UX only; the server 422 is authoritative and mapped inline (plan 04-10). Confirmed: `AuthValidators.signupPassword` rules, 422 handling in `SignupInitialViewModel.handle` | Phase 04 plan 04-03 (threat_model) | 2026-10-21 |
| AR-04-08 | T-04-08 | Server message in AuthErrorDialog is rendered as plain Compose `Text` (no HTML or links); title is a local string resource. Confirmed: only `Text(` calls in the dialog, no `AnnotatedString`/`fromHtml`/`ClickableText` | Phase 04 plan 04-03 (threat_model) | 2026-10-21 |
| AR-04-09 | T-04-09 | Picker search text is only used in a local `contains(ignoreCase)` filter; no network call, no persistence, no query interpolation. Confirmed in `SignupTagsViewModel.kt:50`, `LanguageSheetState.visibleItems`, `SignupBasicInfoUiState.visibleCountries` | Phase 04 plan 04-04 (threat_model) | 2026-10-21 |
| AR-04-15 | T-04-15 | `GET /expert/profile` converts a client-only account via the backend `ensureExpert`. D-23 accepts this as the "become an expert" path; the server, not the client, grants the role; JWT roles refresh on next token refresh. Risk narrowed by T-04-54 (no background conversion) | Phase 04 plan 04-06 (threat_model, D-23) | 2026-10-21 |
| AR-04-17 | T-04-17 | Local mode value is a UX preference (D-17) and can be tampered with on a rooted device; every expert endpoint is authorized server-side by JWT roles | Phase 04 plan 04-06 (threat_model, D-17) | 2026-10-21 |
| AR-04-22 | T-04-22 | Mode Switcher page only reports the choice; `AppViewModel.switchMode` applies the D-17 setup gate and the server authorizes by JWT role. Confirmed: `ModeSwitchScreen.kt:59` -> `AppViewModel.switchMode` -> `UserManager.requestModeSwitch` | Phase 04 plan 04-08 (threat_model) | 2026-10-21 |
| AR-04-23 | T-04-23 | Carousel content is static bundled marketing copy and images; no user data | Phase 04 plan 04-08 (threat_model) | 2026-10-21 |
| AR-04-25 | T-04-25 | Crop state in saved state is only zoom factor and normalised offsets; no image data, path or URI. Confirmed: `PhotoCropViewModel.kt:173-175` | Phase 04 plan 04-09 (threat_model) | 2026-10-21 |
| AR-04-27 | T-04-27 | Account enumeration via 409 is the backend contract (same as iOS); the client adds no extra oracle | Phase 04 plan 04-10 (threat_model) | 2026-10-21 |
| AR-04-29 | T-04-29 | Weak-password spoofing: client rule mirrors the server for UX; the server 422 is authoritative and shown inline | Phase 04 plan 04-10 (threat_model) | 2026-10-21 |
| AR-04-33 | T-04-33 | Name, city, country in the saved-state Bundle are non-credential PII owned by the user; the Bundle stays in app process memory/system saved state and `allowBackup=false` excludes app data from backups. Confirmed: `AndroidManifest.xml:9` is `android:allowBackup="false"` (value is false, as the rationale requires); `res/xml/backup_rules.xml` and `data_extraction_rules.xml` additionally exclude every domain for cloud backup and device transfer | Phase 04 plan 04-12 (threat_model) | 2026-10-21 |
| AR-04-40 | T-04-40 | The short-lived 6-digit email code is mirrored to SavedStateHandle for rotation (Phase 3 precedent); never logged; expires in 15 min and is single-use. Confirmed: `savedStateHandle[KEY_CODE]` at `SignupVerifyEmailViewModel.kt:75,169,237`; no logging calls | Phase 04 plan 04-15 (threat_model) | 2026-10-21 |
| AR-04-45 | T-04-45 | The client Publish gate is UX only; the server rejects an incomplete publish with 422 `missing_fields`, mapped to step names (D-06). Confirmed: `SignupSetupRepository.publish()` `:365-369` | Phase 04 plan 04-17 (threat_model) | 2026-10-21 |

*Accepted risks do not resurface in future audit runs.*

---

## Unregistered Flags

All 20 SUMMARY.md `## Threat Flags` sections say "None". No unregistered flags.

## Informational Notes (not blockers, no open threat)

- I-1 (maps to T-04-49 / T-04-26 / T-04-33 / T-04-40, from 04-REVIEW IN-01): The nav-key KDoc says PII stays in the repository, but the saved-instance Bundle also holds first name, city, province, country, language ids, the signup email and the email code. This is covered by accepted risks AR-04-33 and AR-04-40 and by T-04-26 (email allowed, password never saved). The concrete T-04-49 mitigation (no fields on any nav key) holds. Suggest correcting the `SignupSetupKeys.kt` KDoc wording.
- I-2 (maps to T-04-12, from 04-REVIEW IN-02, not fixed): `PhotoImportException` messages interpolate the content URI (`AndroidPhotoProcessor.kt:57,83,149`). The exception is caught in `SignupProfilePhotoViewModel.onPicked` and replaced with a static dialog string; it is not stored or logged, and the project has no crash-reporting SDK today. Latent only: use static messages before adding Crashlytics (Firebase Messaging does not include it).
- I-3 (maps to T-04-06 / T-04-35): `Timezones.entryFor` returns `TimezoneEntry(id, id)` for a server-supplied zone id unknown to `java.time`. That value originates from the server's own saved profile, not from user input, and a rejection returns 422 shown inline. User-originated selections remain restricted to bundled or valid ids as the mitigation requires.
- I-4 (maps to T-04-46): the post-publish `repository.reset()` runs inside `viewModelScope` after the Published! hold. If the user leaves during that hold the call is skipped, but the same data is cleared at logout/login/signup by T-04-18, and the data belongs to the same session user.
- I-5 (maps to T-04-52): R8 evidence is structural (annotations, generated adapters, mapping.txt without app-package entries because the release `packageScope` excludes app code). I did not re-run `assembleProdRelease`; the audit is read-only.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-21 | 54 | 54 | 0 | gsd-security-auditor (Claude Sonnet 5.5) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-21 (automated audit; ASVS level 1; block_on high)
