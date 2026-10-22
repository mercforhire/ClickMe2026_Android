---
phase: 05-explore-expert-profile
plan: 02
subsystem: ui-foundations
tags: [money, currency, formatter, icons, clock]
requires: []
provides:
  - util.Money.format (minor units + ISO code, device locale)
  - util.PriceDisplay (Free / Paid / Unknown) and components.PriceText
  - constants.CategoryIconMap (31 iOS slugs -> Outlined Material icons, GridView fallback)
  - di.ClockModule.provideWallClock (java.time.Clock, systemUTC)
  - testing.MutableClock (test helper)
affects: [05-04, 05-05, 05-07, 05-08, phase-06, phase-07, phase-11, phase-14]
tech-stack:
  added: []
  patterns: [BigDecimal.movePointLeft for minor units, NumberFormat.getCurrencyInstance with fixed fraction digits]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/util/Money.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/util/PriceDisplay.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PriceText.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/constants/CategoryIconMap.kt
    - app/src/main/res/values/strings_money.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/MutableClock.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/util/MoneyTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/PriceTextTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/constants/CategoryIconMapTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ClockModule.kt
key-decisions:
  - "CAD under Locale.US formats as CA$50.00 on the JBR; matches the RESEARCH A4 expectation, no adjustment needed"
  - "All 31 icon names resolved in material-icons-extended; no substitutions (UI-SPEC open item 2 closed)"
requirements-completed: [DISC-01, DISC-03]
duration: ~25 min
completed: 2026-10-21
---

# Phase 5 Plan 02: Money, PriceText, CategoryIconMap and wall Clock Summary

One shared minor-units currency formatter (digits from ISO data, device-locale placement), a Free/Paid/Unknown price decision with a primary-colored "Free" PriceText, the 31-slug iOS category icon map, and an injectable wall `java.time.Clock` with a `MutableClock` test helper.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Money formatter, PriceDisplay and PriceText | 727a442 |
| 2 | Category icon map, wall Clock provider and MutableClock | 16c2aa9 |

## Verification

- `:app:testDevDebugUnitTest --tests '*MoneyTest' '*PriceTextTest' '*CategoryIconMapTest'`, `ktlintCheck`, `assembleDevDebug` all pass (Hilt graph accepts the new `java.time.Clock` binding).
- `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `defaultFractionDigits` present, one `getCurrencyInstance`, no `/ 100` divisor, one `colorScheme.primary` in PriceText, `price_free` string present.

## Deviations from Plan

- Tests and implementation were committed together per task (one commit per task) rather than as separate RED/GREEN commits; the test classes were written before the implementation and could not compile until it existed. Otherwise none; plan executed as written.
- Minor: ktlint rejected the single-line `setOf(...)` of 31 slugs in the test; reformatted to one slug per line.

## Known Stubs

None.

## Threat Flags

None. T-05-06 (unknown/negative currency digits) and T-05-07 (is_free first, ISO digits) are mitigated and pinned by MoneyTest/PriceTextTest.

## Notes for downstream plans

- 05-04 and 05-05 can inject `java.time.Clock` (no further ClockModule edits needed) and use `testing.MutableClock` in tests.
- 05-07/05-08 tint `CategoryIconMap.icon(slug)` with the primary color at call sites.
- `payment-intent` amounts must not go through `Money.format`.

## Self-Check: PASSED
