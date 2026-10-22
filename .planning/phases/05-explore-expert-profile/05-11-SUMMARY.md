---
phase: 05-explore-expert-profile
plan: 11
subsystem: phase-gate
tags: [phase-gate, verification, deferred-human-verify, r8]
requires:
  - "05-01..05-10: all Phase 5 code"
provides:
  - "Green full CI-equivalent gate for Phase 5 (both flavors, R8)"
  - "Completed 05-VALIDATION.md (nyquist_compliant, wave_0_complete)"
  - "[Phase 5] DEFERRED (human) checklist, hand-offs and iOS todo in STATE.md"
affects: [phase-06-booking, phase-08-bookings, phase-09-call]
tech-stack:
  added: []
  patterns: []
key-files:
  created: []
  modified:
    - .planning/phases/05-explore-expert-profile/05-VALIDATION.md
    - .planning/STATE.md
key-decisions:
  - "Device checks for ROADMAP Phase 5 SC-1..3 are logged as deferred, non-blocking open items (user preference); deferred is not done"
requirements-completed: [DISC-01, DISC-02, DISC-03]
duration: ~15 min
completed: 2026-10-21
---

# Phase 5 Plan 11: Phase gate and deferred checks Summary

The full Phase 5 automated gate is green on both flavors including unsigned R8 `assembleProdRelease`; the validation map is complete, and the on-device checks, cross-phase hand-offs and iOS follow-ups are recorded in STATE.md as non-blocking open items.

## Gate results

Command: `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease` -> BUILD SUCCESSFUL (exit 0), then `bash scripts/check-design-rules.sh` -> "Design rules: OK".

| Check | Result |
|-------|--------|
| ktlintCheck | pass |
| lintDevDebug, lintProdDebug | pass |
| testDevDebugUnitTest | 1056 tests, 0 failures, 0 errors, 0 skipped (Phase 4 baseline 876) |
| testProdDebugUnitTest | 1056 tests, 0 failures, 0 errors, 0 skipped |
| assembleDevDebug, assembleProdDebug | pass |
| assembleProdRelease (R8, unsigned) | pass; Moshi adapters, `@Serializable` keys and the assisted ViewModel survive shrinking at build level |
| Design rules | OK |

## Grep sweeps (comment lines excluded)

| Sweep | Expected | Result |
|-------|----------|--------|
| `favorite` in explore, expertprofile | nothing | nothing |
| `See all` / `see_all` in clientmode and the two strings files | nothing | nothing |
| ReportUser / MoreVert / Chat/Message icons / report-block-message strings in expertprofile | nothing | nothing |
| `SearchRoot` in explore | nothing | nothing |
| `CategoryStore` in SessionDataCleaner.kt (non-comment) | 0 | 0 |
| `// PHASE-9: join window + click (D-02)` in explore | exactly one | one (ExploreTodaysSessionsSection.kt:112) |
| `fontSize` outside ui/theme | nothing | nothing |

No secrets pattern hits (`sk_live`/`sk_test`) in app sources (T-05-36).

## Validation map

05-VALIDATION.md: all 22 task rows green with File Exists marked; Wave 0 and sign-off boxes ticked; frontmatter `status: complete`, `nyquist_compliant: true`, `wave_0_complete: true`; manual rows marked "deferred (non-blocking)". No extra test classes beyond the planned set were reported by the SUMMARYs that needed new rows (the 05-05 repository checks live in `ExploreViewModelTest` and `TodaysSessionsFilterTest`).

## Deferred (human) checklist, NOT done

Logged in STATE.md under `[Phase 5] DEFERRED (human)`; run on the Samsung SM-A075F, devDebug against production, as a client:

- [ ] SC-1 Explore home (hero, 6 category chips, Featured Topics with live `is_free` check, Recommended Experts, Today's Sessions with disabled Join Call, chip filter, sheet filter, pull to refresh, per-section Retry offline)
- [ ] SC-2 Category cache (online View all, force-stop, offline relaunch shows cached list; optional 8-day-ahead refetch)
- [ ] SC-3 Expert Profile (hero, stats, Book Session stub, sections, Free pricing, Unavailable state for suspended/deleted experts)
- [ ] Visual and accessibility (200% font scale, IME in sheet, slow-network skeletons, release smoke after the Phase 2 keystore)

## Hand-offs recorded (Pending Todos)

- Phase 6: `BookingKey` and stub body location, map 422 `CANNOT_BOOK_SELF`, `Money.format` shared.
- Phase 8/9: Today's Sessions tap wiring, `// PHASE-9` marker and `todays_session_join` tag, `ClientBookingsRepository` as My Bookings base.
- iOS report: silent 404/network failure on Expert Profile, blank headers and "—" for empty sections, ignored `is_free`, currency /100 bug, fixed en_US time format.

## Deviations from Plan

None - plan executed exactly as written. No fixes were needed; the gate passed on the first run. STATE.md's frontmatter progress and Current Position block were stale (still showing "Plan 1 of 11"); the position lines were updated per the plan and the state tooling recomputes progress.

## Threat Model

T-05-35 mitigated (R8 `assembleProdRelease` in the gate, green). T-05-36 mitigated (design rules OK, no key patterns). T-05-37 mitigated (all deferred-control sweeps empty).

## Known Stubs

None added in this plan. `MakeABookingStubScreen` is an intentional Phase 6 stub (recorded as a hand-off).

## Commits

- 60f108f: docs(05-11): complete Phase 5 validation map after green full gate
- 5d0176e: docs(05-11): log Phase 5 deferred device checks, hand-offs and iOS todo
