---
phase: 13-push-notifications-internal-testing
plan: 09
subsystem: reviews
tags: [reviews, viewmodel, saved-state, hand-off]
requires:
  - phase: 13-03
    provides: ReviewsRepository, review DTOs, FakeClickMeApi review handlers, review fixtures
  - phase: 13-04
    provides: WriteReviewKey, ReviewOpener, strings_write_review.xml
provides:
  - WriteReviewViewModel (assisted) with WriteReviewUiState
  - ReviewLaunchRequest (per-opener slots, session reviewed-ids set) bound as SessionDataCleaner
affects: [13-13, 13-15, 13-18]
tech-stack:
  added: []
  patterns: [assisted Hilt ViewModel with SavedStateHandle, singleton hand-off slot per consumer]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/reviews/ReviewLaunchRequest.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ReviewModule.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/reviews/ReviewLaunchRequestTest.kt
  modified:
    - app/src/main/res/values/strings_write_review.xml
key-decisions:
  - "Submit sets submitting=true synchronously before launching, so a second tap in the same frame is ignored"
  - "Saved state (rating, comment) is ignored for an already-submitted review; the prefill wins"
  - "A restored out-of-range saved rating falls back to the 5-star default"
requirements-completed: [REV-01]
status: complete
commits: 3
plan_head_before: 6fa01f099efa3257b758f859ff491cd82e307b64
plan_head_after: 17aedb4bc79931b38a733aa742d5ee334fe02a87
actuals:
  tokens: 14000
  tasks: 3
  commits: 3
duration: ~25min
completed: 2026-10-28
---

# Phase 13 Plan 09: Write Review state machine Summary

Write Review ViewModel with iOS 5-star default, 1000-char comment cap, read-only already-reviewed state, in-flight lock, SavedStateHandle persistence, and a per-opener post-submit hand-off singleton.

## Tasks

1. Tracer: context load, default submit, hand-off (commit 8a8031c). `:app:assembleDevDebug` passed, so the assisted Hilt graph is valid.
2. Star/comment rules, cap, read-only prefill, in-flight lock, saved state (commit affe94d).
3. Context failure with `retry()` and the submit error dialog that keeps the form (commit 17aedb4).

## Verification

- `WriteReviewViewModelTest` (21 tests) and `ReviewLaunchRequestTest` (3 tests) pass; `ktlintCheck` clean; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Prohibition (no review without explicit Submit): `contextLoadsIntoReadyWithDefaults` and `noRequestWithoutSubmit` assert no POST happens on load or after edits.

## Deviations from Plan

None - plan executed exactly as written. (Minor: the `FieldError` mapping for submit lives in a private extension in the ViewModel instead of reusing `toDialogError`, because the plan requires the write-review-specific generic string instead of `login_error_unknown`.)

## Open items for human/device

None blocking. The rendering (13-13) and the consumers (13-15, 13-18) are what a device check will exercise.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

Files and commits 8a8031c, affe94d, 17aedb4 verified present.
