---
status: resolved
trigger: "Android: CAD price shows no CA$ prefix on a device whose region is Canada, while USD shows US$ (D-24 regression, iOS regression R8 step 2 on dev build e311a74c). Money.format in app/src/main/java/com/uptrendinvestments/clickmeapp/util/Money.kt:62-79 pins USD->US$ / CAD->CA$ by overriding DecimalFormatSymbols.currencySymbol on NumberFormat.getCurrencyInstance(locale). JVM unit tests (MoneyTest, PriceTextTest) pass. Suspects: (1) CAD row bypasses Money.format (PriceDisplay.kt or another formatter), (2) topic currency null/empty, (3) Android ICU ignores/reset the currencySymbol override for the locale's own currency (en_CA + CAD). Source todo: /Users/LeonChen/Documents/ClickMe2026/.planning/todos/pending/2026-11-23-android-cad-price-missing-ca-prefix.md. Fix with a test that reproduces the cause (Robolectric if ICU). Android only, local commits, do not push."
created: 2026-11-23
updated: 2026-11-23
---

## Symptoms

- **Expected:** on every screen and in every locale, a USD price starts with `US$` and a CAD price starts with `CA$` (D-24, D-32; iOS 1.1 (7) does this).
- **Actual:** on the Android dev build at `e311a74c`, on the page where client A sees expert B's USD and CAD topics (an expert profile or Explore), USD shows `US$` but the CAD price has no `CA$` prefix.
- **Errors:** none shown.
- **Timeline:** found 2026-11-23 in the iOS 08-22 TestFlight regression, R8 step 2. The pinning was added in Android commit `50626142` (fix(ios-p8): show US$ and CA$ regardless of locale).
- **Reproduction:** Android phone with region set to Canada; open an expert profile or Explore showing a USD topic and a CAD topic; read both prices.

## Constraints

- Android repo only (`ClickMe_Android`). Local commits, do not push.
- Add a test that reproduces the actual cause (Robolectric or instrumented if it is ICU-specific; the JVM tests already pass).
- The live backend is production: no production writes.

## Current Focus

bug_class: Bohrbug on a specific platform (deterministic per device ICU/libcore + locale); JVM tests pass, so platform-dependent
hypothesis: SUPPORTED BY THE DEVICE CHECK OF THE FIX (old failure mechanism on Android 16 still not observed directly). The single point of failure is the DecimalFormatSymbols.currencySymbol override: Android formats through android.icu.text.DecimalFormat, whose currency-symbol resolution is ICU's, and the override is not reproduced as failing on Android 17 (API 37). The phone (Samsung SM_A075F, Android 16) was not reachable by me, so the exact mechanism there is not observed.
test: done on API 37 (override honoured, S3 not reproduced). API 34 emulator could not boot (disk full, 7.3 GB needed). API 36 image not installed.
expecting: n/a
next_action: none. Resolved 2026-11-23: user confirmed on the Samsung SM-A075F that the CAD price check passes with fix 3e478ba1 (on origin/main at f9985f3a). Session archived to .planning/debug/resolved/.

reasoning_checkpoint:
  hypothesis: "On the phone the pinned CA$ is lost because the symbol travels through DecimalFormatSymbols.currencySymbol into ICU's DecimalFormat, and ICU (not OpenJDK) decides the symbol; unit tests run OpenJDK java.text so they cannot see it. NOT observed directly on Android 16."
  confirming_evidence:
    - "Unpinned en_CA output (USD US$50.00, CAD $50.00) is exactly the reported screen, so the report is also explained by the pin having no effect for CAD"
    - "Reflection on the emulator: java.text.DecimalFormat holds icuDecimalFormat, DecimalFormatSymbols holds cachedIcuDFS, so formatting is ICU's, not OpenJDK's"
    - "Code audit: one formatter (Money.format) serves every topic price surface, so no bypass exists"
  falsification_test: "After the fix, CAD on the Samsung (Android 16, region Canada) still shows a bare $ -> the cause is not the symbol override (look at build contents / locale / data next)"
  fix_rationale: "Put the symbol in the pattern as quoted literal text. A literal is not subject to any currency-symbol resolution, so the output no longer depends on how an ICU version treats a DecimalFormatSymbols override. Placement, grouping and separators still come from the locale's own currency pattern and symbols (D-32 outcome kept)."
  blind_spots: "Android 16 / One UI behaviour never observed; the device build contents are assumed from git ancestry; what exactly the phone showed for CAD (bare $, nothing, suffix) is not known"
  candidate_causes:
    - "code/platform: ICU on Android 16 does not apply the symbol override (unobserved)"
    - "environment: APK on the phone lacked 50626142 (weak: same build passed the Delete Account line from 2a65e7c7, which is later)"
    - "data: CAD topic currency null/empty (weak: null gives the Unknown placeholder, not a bare $)"
    - "config/locale: phone locale not an English locale (fr-CA would put CA$ after the number, which reads as no CA$ prefix) - not excluded"
  and_gate: "no single contributing-condition pair identified; the fix removes the dependency on the override, so it holds under any of the platform variants"

## Evidence

- timestamp: 2026-11-23
  checked: every caller of Money.format / PriceDisplay.of for topic price surfaces (ExpertProfileViewModel.toRow, ExploreViewModel.FeaturedTopicDto.toUi, MakeABookingUiState, BookingSummaryUiState)
  found: all four go PriceDisplay.of(isFree, amount, currency) -> Money.format(amount, currency, locale); PriceText renders Paid.text verbatim. grep found no other price formatter (no NumberFormat.getCurrencyInstance, no "$" string templates) outside Money.kt; the only other NumberFormat uses are integer formats (BookingNoteScreen.kt:425, RequestEarnings.kt:83)
  implication: suspect 1 (CAD row bypasses Money.format) is not supported for the expert profile and Explore surfaces

- timestamp: 2026-11-23
  checked: PriceDisplay.of null handling and backend public-profile / featured serialization (publicProfile.controller.js:352-360, DTOs TopicPriceDto/MoneyDto)
  found: a paid topic with null currency gives PriceDisplay.Unknown (the localized price_unknown string), and an empty currency gives "" + amount via the unknown-code branch of Money.format. Neither produces a formatted bare "$50.00". Backend sends price_currency straight from the DB column; Money.format upper-cases it, so casing is handled. Backend 412f1aa only touched serializeTopic (expert-owned topics list), not the public profile or featured topics
  implication: suspect 2 (null/empty/miscased currency) cannot produce "a price with no CA$ prefix" on these surfaces; weakens it, pending confirmation of what the device text actually showed

- timestamp: 2026-11-23
  checked: Robolectric setup (app/build.gradle.kts:141-142, libs.versions.toml robolectric 4.17) and the android-all-instrumented-15 jar in ~/.m2
  found: Robolectric is available. The android-all jar contains android.icu.* and libcore.icu.* but NOT java.text.DecimalFormat, so Robolectric runs java.text on the host JVM
  implication: Robolectric cannot reproduce an Android-libcore-specific DecimalFormat behaviour; the existing JVM tests passing says nothing about the device

- timestamp: 2026-11-23
  checked: environment for a real Android runtime
  found: ~/Library/Android/sdk has emulator and system-images (android-34, android-37.0) and an AVD Medium_Phone / Medium_Phone_API_37.0
  implication: an emulator may let me run Money.format on real Android libcore with an en_CA locale

- timestamp: 2026-11-23
  checked: the symptom itself against locale arithmetic, using the real compiled Money.class run through dalvikvm on the Medium_Phone_API_37.0 emulator (Android 17, real libcore + android.icu)
  found: UNPINNED en_CA formatting is USD "US$50.00" and CAD "$50.00". That is exactly what the tester saw (USD has US$, CAD has no CA$), so "USD shows US$" does not show the pin works at all on the phone; USD already reads US$ in en_CA without it. The pin matters for CAD only.
  implication: the observation is equally explained by "the pin is not applied for CAD on the phone"; the question is why

- timestamp: 2026-11-23
  checked: compiled devDebug Money.class (matches current Money.kt, contains "CA$") via dalvikvm on the API 37 emulator, locales en-US, en-CA, en-US-u-rg-cazzzz, en-CA-u-rg-uszzzz, fr-CA, en-AU, plus Locale.setDefault(en-CA)
  found: CAD -> "CA$50.00" in every English locale incl. en-CA and the rg/cu extension variants; lowercase "cad" works; USD -> "US$50.00"; fr-CA -> "50,00<nbsp>CA$". The override IS honoured on Android 17.
  implication: suspect 3 is NOT reproduced on Android 17. Remaining platform gap: the phone is a Samsung SM-A075F on Android 16 (API 36, per .planning notes); I have no API 36 image and may not use the physical phone

- timestamp: 2026-11-23
  checked: reflection on the emulator's java.text.DecimalFormat / DecimalFormatSymbols
  found: Android's java.text.DecimalFormat wraps android.icu.text.DecimalFormat (field icuDecimalFormat) and DecimalFormatSymbols keeps cachedIcuDFS, with getIcuDecimalFormatSymbols()/fromIcuInstance(). Formatting is done by ICU, not by OpenJDK's DecimalFormat
  implication: the JVM unit tests and Robolectric (host JVM java.text, android-all has no java.text) exercise a different implementation from the device, so they cannot prove or disprove the override. Any ICU-version-specific handling of a DFS currencySymbol override (ICU resolves the symbol itself unless the DFS symbol differs from the CLDR default) is invisible to them

- timestamp: 2026-11-23
  checked: decision D-32 (iOS 08-CONTEXT.md:104) and git history
  found: D-32 mandates "override only the currency symbol via DecimalFormatSymbols, keep locale placement and grouping". Commit 50626142 (Nov 19) is an ancestor of e311a74c and precedes 2a65e7c7 (Delete Account saved-cards line, Nov 22), which the same tester recorded as PASS on the same Android build, so the phone build contains the pin (stale-APK explanation is weak)
  implication: fix must keep locale placement and grouping, but should not depend on the symbol-override mechanism

- timestamp: 2026-11-23
  checked: environment limits
  found: the physical phone (Samsung SM_A075F, wireless adb) appeared in `adb devices` during the session but was deliberately not used (instruction: device not available to the debugger); every adb call after that used `-s emulator-5554`. Disk had 2.8 to 4.5 GB free, so the API 34 AVD (needs 7.3 GB for userdata) could not boot and an API 36 image could not be downloaded
  implication: Android 16 behaviour of the old override is unobserved. A decisive on-device check exists: the old-mechanism probe at /private/tmp/claude-502/-Users-LeonChen-Documents-ClickMe2026/d9d6181e-2daf-4ef9-805d-fbf114317307/scratchpad/probe/dexout/classes.dex (source Probe.java beside it) prints, per locale, CAD "unpinned" and "pinned" (the old currencySymbol override). Run: adb -s <phone serial> push <that dex> /data/local/tmp/probe.dex, then adb -s <phone serial> shell dalvikvm -cp /data/local/tmp/probe.dex Probe. If en_CA "CAD pinned" prints $50.00 instead of CA$50.00 the override is ignored on Android 16 and the fix addresses the cause

- timestamp: 2026-11-23
  checked: human verification of fix 3e478ba1 on the failing device (the confirmed checkpoint, awaiting_human_verify)
  found: the user installed a fresh devDebug build on the Samsung SM-A075F (Android 16, region Canada), read a USD and a CAD topic price, and reported "the CAD price check on the Samsung, pass". The falsification test in reasoning_checkpoint (CAD still shows a bare $ after the fix) did not trigger
  implication: the fix resolves the reported symptom on the device where it failed. This supports the Android-ICU symbol-resolution hypothesis, but the old override's behaviour on Android 16 was still never observed (the dalvikvm probe was not run on the phone), so the exact mechanism is inferred, not seen

## Eliminated

- hypothesis: S1 - the CAD row bypasses Money.format (PriceDisplay.kt or another formatter)
  evidence: every topic price surface (expert profile, Explore featured, make-a-booking, booking summary, expert TopicCard) goes through PriceDisplay.of -> Money.format; no other currency formatter exists in app/src/main; PriceText renders Paid.text verbatim
  timestamp: 2026-11-23

- hypothesis: S2 - topic currency null/empty/miscased produces a price with no CA$
  evidence: null currency yields PriceDisplay.Unknown (placeholder string, not a price), empty yields "" + amount via the unknown-code branch, lowercase "cad" is upper-cased and formatted correctly (verified on the emulator); none produce a bare "$50.00"
  timestamp: 2026-11-23

- hypothesis: S3 on Android 17 - ICU ignores/reset the currencySymbol override for en_CA + CAD
  evidence: real Money.class on the API 37 emulator returns CA$50.00 for en-CA (see Evidence). NOT eliminated for Android 16 (API 36), which is untested
  timestamp: 2026-11-23

## Resolution

root_cause: The pinned CA$ travelled through a DecimalFormatSymbols.currencySymbol override into Android's ICU-backed DecimalFormat, and on the Samsung (Android 16, region Canada) that override did not take effect for CAD, so en_CA printed the native "$50.00". Status of the evidence: the fix, which removes the dependency on the override, was confirmed on that phone by the user on 2026-11-23 (CAD check pass). The old override's failure on Android 16 was never observed directly (no API 36 image, phone not used by the debugger, probe not run on it), so the precise ICU mechanism is inferred from the fix result plus the evidence below rather than seen. Established: (a) no code path bypasses Money.format and null/empty/miscased currency cannot produce a bare "$50.00", so the fault is in how the symbol is produced, not in which formatter runs; (b) the report is exactly what UNPINNED en_CA formatting prints (USD "US$50.00" natively, CAD "$50.00"), so the observation is consistent with the pin having no effect for CAD on the phone; (c) Android's java.text.DecimalFormat delegates to android.icu.text.DecimalFormat (reflection: icuDecimalFormat, cachedIcuDFS), so the currencySymbol override is subject to ICU's own symbol resolution, which JVM unit tests and Robolectric (host-JVM java.text; the android-all jar has no java.text) cannot exercise. NOT reproduced: the real compiled Money.format on the API 37 (Android 17) emulator returns CA$50.00 for en-CA and all rg/cu locale variants, so the override works there. The phone is Android 16 (API 36) and was not reachable; no API 36 or API 34 image could be run (disk full). This is therefore an Android-ICU-only suspicion, unobserved on the failing platform.
fix: Money.currencyFormat (new internal fun used by Money.format) writes the pinned US$/CA$ into the pattern as quoted literal text, taken from the locale's own currency pattern (toPattern with the U+00A4 sign replaced) and built with DecimalFormat(pattern, localeSymbols). No DecimalFormatSymbols.currencySymbol override and no ICU symbol resolution is involved for USD and CAD any more. Placement, grouping, separators and digits still come from the locale (D-32 outcome kept; the D-32 mechanism "via DecimalFormatSymbols" is replaced). Non-pinned currencies (EUR, JPY, KWD...) take the old path unchanged.
verification:
  oracle_type: specified (D-24/D-32: USD reads US$ and CAD reads CA$ in every locale; locale placement and grouping kept)
  target_test: { result: pass, test: "app/src/test/.../util/MoneyTest.kt pinnedSymbol_survivesTheFormatterSymbolsBeingReset", note: "stands in for a platform that resets/ignores the symbol override by replacing the formatter's DecimalFormatSymbols with the locale defaults; it simulates the failure mode, it is not a reproduction of the phone" }
  mutation_check: { result: skipped, reason_if_skipped: "no Stryker for Kotlin/Android in this project", manual_substitute: "put the old currencySymbol-override body back: the target test fails with 'CAD in en_CA expected:<CA$50.00> but was:<$50.00>', the reported symptom" }
  no_op_deletion: { result: pass, deletion_justified_by_rca: false, note: "diff replaces a mechanism, adds the literal path; nothing is deleted without replacement" }
  adjacent_tests: { result: pass, suites_run: ["full testDevDebugUnitTest: 5439 testcases in the results dir, 0 failures, 0 errors", "ktlintCheck clean", "MoneyTest 21/21 incl. 4 new", "PriceTextTest 6/6"] }
  revert_and_reconfirm: { result: pass, bug_returned_on_revert: true, fixed_on_reapply: true, note: "true for the simulated failure on the JVM; the device failure itself was never observed" }
  device_check_real_android: "Real compiled Money.class run through dalvikvm on the API 37 emulator (emulator only; the physical phone was not touched): CAD CA$1,234.56 / USD US$1,234.56 in en-US, en-CA, rg variants, en-AU; fr-CA 1<nbsp>234,56<nbsp>CA$; de-DE 1.234,56<nbsp>CA$; negative -CA$5.00; EUR and JPY unchanged; after replacing the formatter's symbols with the locale defaults the output stays CA$50.00 (pattern 'CA$'#,##0.00)"
  guardrail_verdict: accepted
  human_verification: { result: pass, date: 2026-11-23, device: "Samsung SM-A075F, Android 16, region Canada", note: "user reported the CAD price check passes after installing a fresh devDebug build; fix commit 3e478ba1, on origin/main at f9985f3a" }
postmortem:
  why_not_caught: "MoneyTest and PriceTextTest ran on the host JVM (OpenJDK java.text; Robolectric's android-all jar has no java.text), so they could not see Android's ICU-backed DecimalFormat resolving the currency symbol itself. No gate existed for ICU currency formatting on a real Android runtime."
  guard: "Money.currencyFormat writes the US$/CA$ symbol into the pattern as quoted literal text, so no ICU symbol resolution is involved; MoneyTest.pinnedSymbol_survivesTheFormatterSymbolsBeingReset simulates a platform that resets the symbols (a stand-in, not a reproduction of the phone)."
files_changed:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/util/Money.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/util/MoneyTest.kt
