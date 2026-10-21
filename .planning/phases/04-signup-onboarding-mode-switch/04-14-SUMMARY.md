---
phase: 04-signup-onboarding-mode-switch
plan: 14
subsystem: signup
tags: [compose, tags, search, chips, viewmodel]
requires:
  - phase: 04-01
    provides: ExpertiseTagItem, ClickMeApi.getExpertiseTags, FakeClickMeApi
  - phase: 04-03
    provides: AuthTopAppBar, AuthErrorDialog title, signup common strings
  - phase: 04-04
    provides: SelectableChip, PickerLoading, picker strings
  - phase: 04-07
    provides: SignupSetupRepository.saveTags (D-25 rule), SignupTestHarness
provides:
  - SignupTagsViewModel, SignupTagsUiState, TagCatalog
  - SignupTagsScreen and stateless SignupTagsContent
  - strings_signup_tags.xml
affects: [04-19 (registers TagsKey)]
tech-stack:
  patterns:
    - "Selection seeded from the repository draft until the first edit, then mirrored to SavedStateHandle"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsScreen.kt
    - app/src/main/res/values/strings_signup_tags.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsScreenTest.kt
key-decisions:
  - "The ViewModel does not call ensureHydrated; the hub owns hydration and the untouched selection re-seeds when the draft changes"
  - "Back and the top-bar arrow are disabled while Saving/Saved so the request and confirmation are not cancelled (same as Set New Password)"
requirements-completed: [SIGN-02]
completed: 2026-10-21
---

# Phase 4 Plan 14: Expertise Tags Step Summary

Expertise Tags step with a searchable catalog from GET /meta/expertise-tags, removable chips kept in tap order (first is primary), an always-enabled Save routed through the repository's D-25 empty-list rule, and loading, error-with-Retry and no-match states.

## Tasks
1. SignupTagsViewModel (catalog load/retry, case-insensitive filtering, tap-order select/remove, SavedStateHandle restore, 600 ms Saved hold, dialog on failure) - `ac9a7b7`. 10 tests.
2. SignupTagsScreen, strings, screen tests - `a28f3a1`. 10 tests.

## Verification
- `:app:testDevDebugUnitTest --tests '*SignupTags*'`: 20 tests pass.
- `ktlintCheck` and `lintDevDebug` pass; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: one `repository.saveTags(`, one `api.getExpertiseTags()`, no "sorted" or "custom" in the ViewModel, no "customtag" in the screen, `SelectableChip(` and `ImeAction.Search` present.

## Deviations from Plan
- Tests and implementation were committed together per task rather than as separate RED/GREEN commits.
- Screen tests use `@Config(qualifiers = "w411dp-h891dp")`; under the default small Robolectric window the lazy list did not compose the Law row or the Retry button (test-only fix).
- The wave-3 sibling plan 04-12 (Personal Details) is not in this base, so the "mirror 04-12" structure was taken from SetNewPasswordViewModel and the 04-07 repository instead.
- ktlintFormat reformatted the ViewModel test (committed with Task 2).

## Known Stubs
None.

## Threat Flags
None. T-04-36 (only catalog ids selectable, no free text) and T-04-37 (`[]` only via repository D-25 rule) are implemented as planned.

## Manual checks (deferred by project policy)
- On device: pick tags, confirm chip order matches tap order and the primary helper shows; open the keyboard and confirm Save stays above it.
- On device: with saved tags, remove all, Save, and confirm the server list clears; re-open with an empty list and Save to confirm nothing is wiped.

## Self-Check: PASSED
All five created files exist; commits `ac9a7b7` and `a28f3a1` exist.
