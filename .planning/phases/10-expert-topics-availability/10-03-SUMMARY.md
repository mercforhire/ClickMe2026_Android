---
phase: 10-expert-topics-availability
plan: 03
subsystem: expert-mode-topics
tags: [pure-logic, tdd, money, topics]
requires: []
provides:
  - "TopicPrice (PriceParse, parse/filterInput/fieldText/floorFor/isBelowFloor/wireAmount/resolveCurrency)"
  - "TopicEditorRules (icon, duration, caps, tag selection)"
affects: [10-10 topic editor ViewModel]
tech-stack:
  added: []
  patterns: ["BigDecimal-only money parsing", "pure object rules with JVM tests"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicPrice.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRules.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicPriceTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRulesTest.kt
  modified: []
key-decisions:
  - "Price parsing uses BigDecimal with a 7-integer-digit cap and longValueExact; no Double (T-10-09)"
  - "Free topic wire amount is the currency floor, else 100 (D-11)"
  - "applyProfileTagsUpdate keeps the selected objects (not the profile copies) and only drops removed tags"
requirements-completed: [EXP-01, EXP-02]
duration: 25min
completed: 2026-10-25
status: complete
actuals:
  tasks: 2
  commits: 2
plan_head_before: bc1f77406ae16cf9279280e323115b90dcd6b967
plan_head_after: c38608813a82308364cbf36bb2fec09d2434d2a3
commits: 2
---

# Phase 10 Plan 03: Topic editor pure rules Summary

Pure, Android-free price and form rules for the topic editor: BigDecimal price parsing by currency fraction digits with floor and free-topic wire amount, plus ported icon, duration, cap and tag rules, all JVM unit tested.

## Tasks

1. TopicPrice (commit c2a02b5): `PriceParse`, `filterInput`, `parse`, `fieldText` ("12.50" for 1250 CAD), `floorFor` (delegates to `minMinorUnits`), `isBelowFloor`, `wireAmount`, `resolveCurrency`. 10 tests.
2. TopicEditorRules (commit c386088): 24 curated slugs, `iconOptions`/`toggleIcon`/`iconLabel`, duration 30..480 step 30 default 60, surrogate-safe `capTitle`/`capDescription` (200/2000 via `capNotes`), profile-only tag selection capped at 3, lower-case `wireTagIds`. 13 tests.

Verification: `:app:testDevDebugUnitTest --tests '*TopicPriceTest' --tests '*TopicEditorRulesTest' ktlintCheck` passes (23 tests, 0 failures).

## Deviations from Plan

None - plan executed exactly as written. (Two ktlint formatting fixes during development: a chained call in `fieldText` and a long `listOf` in the test; no behavior change.)

## Known Stubs

None.

## Threat Flags

None. T-10-09 (7 integer digits, BigDecimal, longValueExact), T-10-11 (surrogate-safe caps) and T-10-12 (profile-only, lower-cased tag ids) are mitigated as planned.

## Self-Check: PASSED

All four files exist; commits c2a02b5 and c386088 are on the branch.
