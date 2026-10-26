---
phase: 11-expert-requests-bookings
plan: 06
subsystem: ui
tags: [request-decision, earnings, viewmodel, compose, wave-2]

requires:
  - phase: 11-expert-requests-bookings
    provides: "11-01 BookingRequestsRepository.detail and earnings DTOs; 11-02 RequestDecisionKey and the empty requestDecisionEntries; 11-03 meetingTypeLabel/Icon and clientDisplayName"
provides:
  - RequestEarningsUi (Receipt / PricePending / Free) and requestEarnings(...) with formatCommissionPct
  - Key-scoped assisted RequestDecisionViewModel (load, retry, expiry from the key)
  - RequestDecisionScreen / RequestDecisionContent with expired banner, client header, details card, message card, earnings section
  - entry<RequestDecisionKey>(BottomBar.hidden()) registration
affects: [11-12, 11-05]

actuals:
  tokens: 14000
  tasks: 3
  commits: 3
plan_head_before: 2d62a572d451277a2d07d2d2715b4a38af23cb66
plan_head_after: 77cad49
commits: 3

tech-stack:
  added: []
  patterns:
    - "Earnings rule as a pure function returning a sealed UI model; net null never falls back to gross"
    - "Assisted key ViewModel with load/retry job cancellation (mirrors ClientCancellationViewModel)"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestEarnings.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestDecisionBlocks.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestEarningsSection.kt
    - app/src/main/res/values/strings_request_decision.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestEarningsSectionTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModelTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionEntries.kt

key-decisions:
  - "Receipt requires gross, net and a booking/expert_default fee source; anything else with a price is PricePending (never gross under an earnings label)"
  - "A blank or null topic hides the Topic row instead of showing a placeholder"
  - "Screen leaves bottom padding (xl) for the 11-12 footer; no accept/decline code in this plan"

patterns-established:
  - "RequestEarningsSection test tag request_earnings on whichever card renders"

requirements-completed: [EXP-06]

coverage:
  - id: D1
    description: "Request Decision loads the keyed request and shows client header, details card, trimmed message (hidden when blank)"
    requirement: EXP-06
    verification:
      - kind: automated_ui
        ref: "RequestDecisionScreenTest#detailRendersFromTheApi, blankMessageHidesTheMessageCard, nullMessageHidesTheMessageCard"
        status: pass
    human_judgment: false
  - id: D2
    description: "Expired banner comes from the key, load failure shows Retry"
    requirement: EXP-06
    verification:
      - kind: automated_ui
        ref: "RequestDecisionScreenTest#expiredKeyShowsBanner, loadFailureShowsRetry"
        status: pass
      - kind: unit
        ref: "RequestDecisionViewModelTest#expiredComesFromTheKey, failureThenRetryLoads"
        status: pass
    human_judgment: false
  - id: D3
    description: "Earnings card is correct for booking, expert_default, unavailable, null breakdown, zero commission, JPY and free; gross is never shown as earnings"
    requirement: EXP-06
    verification:
      - kind: unit
        ref: "RequestEarningsSectionTest (16 tests)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Visual fidelity of the screen (spacing, tints, 200% font scale) against the UI spec"
    verification: []
    human_judgment: true
    rationale: "No screenshot or device check was run; layout and colour are only asserted structurally"

duration: 40min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 06: Request Decision Read Side Summary

**Request Decision screen with a key-scoped ViewModel, expired banner from the list row, client/details/message blocks and a fee_source-driven earnings card that never shows gross as earnings, registered as a bottom-bar-hidden Nav3 route.**

## Performance

- **Duration:** about 40 min
- **Completed:** 2026-10-26
- **Tasks:** 3 (one tracer, two expansion)
- **Files modified:** 11 (10 created, 1 modified)

## Accomplishments

- Tracer: GET `expert/booking-requests/{id}` to a registered route, proven by 5 Robolectric screen tests over the real ViewModel and `FakeClickMeApi`. Tracer verify was green before expanding.
- Earnings model: `requestEarnings` returns Receipt (booking or estimated expert_default), PricePending (unavailable, null net or no breakdown, list earnings in the list currency) or Free. Zero commission is a real receipt with "Platform fee" and a zero amount; JPY has no decimals.
- Earnings section: primary-tinted receipt with "−fee" in `onSurfaceVariant` (not `error`), estimated caption, "Session price" plus "Payout amount pending", and Free.
- ViewModel contract pinned by 7 tests, including retry cancelling a stale in-flight load.

## Task Commits

1. **Task 1: Request Decision screen, ViewModel and route (tracer)** - `de3a238` (feat)
2. **Task 2: Earnings section** - `f9e8b9a` (feat)
3. **Task 3: ViewModel load contract tests** - `77cad49` (test)

## Verification

- Task 1: `*RequestDecisionScreenTest` + ktlintCheck, 5/5.
- Task 2: `*RequestEarningsSectionTest` 16/16 + `*RequestDecisionScreenTest` 5/5 + ktlintCheck.
- Task 3 / plan level: `*RequestDecisionViewModelTest` 7/7 with both other classes, ktlintCheck; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `entry<RequestDecisionKey>` = 1; `key.isExpired` in the ViewModel >= 1; `sealed interface RequestEarningsUi` = 1; `colorScheme.error` in RequestEarningsSection = 0; `@Test` counts 16 / 5 / 7; `Log.` calls in the ViewModel = 0.

## Deviations from Plan

None - plan executed exactly as written, with these notes:

- The plan's interface lists `economics: BookingRequestEconomicsDto?`, but 11-01 declared it non-null, so the ViewModel reads `economics.earningsBreakdown` directly (a malformed body fails decoding, which the screen shows as the load error).
- The plan's TDD instruction was followed as tests written first (RED confirmed as a compile failure on unresolved `RequestDecisionViewModel` / `RequestDecisionScreen`), then one commit per task, as in 11-01.
- Task 1 was committed without the earnings section call (Task 2 adds it) so each commit compiles on its own.

## Issues Encountered

None.

## Known Stubs

None. The screen has no accept/decline footer by design; 11-12 adds it.

## Threat Flags

None. No new network surface (reuses the 11-01 detail call); the client message is rendered in a selectable plain `Text` and never logged (T-11-17, T-11-18, T-11-19 mitigated as planned).

## Next Phase Readiness

11-12 can add the footer, sheets and accept/decline on top of `RequestDecisionContent` and `RequestDecisionViewModel`. 11-05 (Incoming Requests) can navigate to `RequestDecisionKey(requestId, isExpired, expiresAtMillis, listEarningsMajor, listCurrency)`.

## Self-Check: PASSED

- All 10 created files and the modified `RequestDecisionEntries.kt` exist on disk (clean `git status` after commits).
- Commits `de3a238`, `f9e8b9a`, `77cad49` are ancestors of HEAD; `git rev-list --count 2d62a57..HEAD` = 3 before this summary commit.
