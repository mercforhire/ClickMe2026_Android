---
phase: 04-signup-onboarding-mode-switch
plan: 02
subsystem: constants
tags: [constants, countries, timezones, assets, png]
requires: []
provides:
  - "constants.Countries / Country (alpha-3 picker table, normalize)"
  - "constants.Timezones / TimezoneEntry (curated 10 zones, detected/pickerEntries/entryFor/label)"
  - "R.drawable.onboarding_welcome/discover/booking/communication"
  - "BundledAssetsTest covering all six bundled illustrations"
affects: [04-07, 04-12, 04-13]
tech-stack:
  added: []
  patterns: ["pure-JVM constants objects (no android imports)", "Locale cross-check test guarding a transcribed table"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/constants/Countries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/constants/Timezones.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/constants/CountriesTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/constants/TimezonesTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/BundledAssetsTest.kt
    - app/src/main/res/drawable-nodpi/onboarding_welcome.png
    - app/src/main/res/drawable-nodpi/onboarding_discover.png
    - app/src/main/res/drawable-nodpi/onboarding_booking.png
    - app/src/main/res/drawable-nodpi/onboarding_communication.png
  modified: []
key-decisions:
  - "normalize passes any well-formed unlisted alpha-3 through (deviation from iOS) so server-side countries from other clients are not dropped"
  - "Timezone labels formatted with Locale.ROOT so digits stay ASCII under any device locale"
requirements-completed: [SIGN-02, ONB-01, ONB-02]
duration: ~25 min
completed: 2026-10-21
---

# Phase 4 Plan 02: Constants and Bundled Assets Summary

Countries (50, alpha-3) and Timezones (10 curated + device-zone-first) ported from iOS as tested pure Kotlin, four onboarding PNGs bundled at 1080 px, and the two pre-committed mode-switch PNGs verified unchanged.

## Tasks

| Task | Commit | Result |
|------|--------|--------|
| 1. Countries and Timezones constants | aa1c3fc | 8 Countries tests + 9 Timezones tests pass; alpha-3 map cross-checked against JDK `Locale.isO3Country` |
| 2. Mode image verification + onboarding images | a4b6385 | BundledAssetsTest passes, `assembleDevDebug` exits 0, `check-design-rules.sh` prints "Design rules: OK" |

## Bundled image record

| File | Dimensions | Bytes | Origin |
|------|-----------|-------|--------|
| mode_client.png | 512 x 512 RGB PNG | 267379 | pre-existing (24d311f), unchanged (`git diff --stat 24d311f` empty) |
| mode_expert.png | 512 x 512 RGB PNG | 263829 | pre-existing (24d311f), unchanged |
| onboarding_welcome.png | 1080 x 810 PNG | 1018183 | iOS welcome.imageset via sips |
| onboarding_discover.png | 1080 x 810 PNG | 999510 | iOS discover.imageset via sips |
| onboarding_booking.png | 1080 x 810 PNG | 898564 | iOS booking.imageset via sips |
| onboarding_communication.png | 1080 x 810 PNG | 971280 | iOS communication.imageset via sips |

## Deviations from Plan

- `Countries.alpha2ToAlpha3` is `internal` (not private) so the Locale cross-check test can read it. No behavior change.
- Otherwise the plan's documented normalize deviation (unlisted well-formed alpha-3 passes through) was implemented as specified. No other deviations.

## Open items (manual testing deferred by project policy)

- None for this plan; no UI was added. Visual check of the carousel/mode images happens when the consuming screens land (plans 04-13 and the mode-switch plan).

## Known Stubs

None.

## Threat Flags

None. T-04-05 (mode PNG tampering) mitigated by the file-type and unchanged-since-24d311f checks; T-04-06 mitigated by normalize emitting only listed or well-formed alpha-3 codes and valid java.time zone ids.

## Self-Check: PASSED

All created files exist; commits aa1c3fc and a4b6385 exist on the worktree branch.
