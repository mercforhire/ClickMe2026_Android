---
phase: 14-payouts-account-settings
plan: 06
subsystem: settings
tags: [help, faq, mailto, support, compose]

requires:
  - phase: 14-payouts-account-settings
    provides: "14-01 getFaqs endpoint, FaqsPayload types, FakeClickMeApi faqsHandler, SettingsFixtures faq builders"
provides:
  - HelpKey route (registered by 14-14)
  - HelpRepository, HelpViewModel (HelpLoad, buildHelpSections), HelpScreen / HelpContent
  - HelpFaqCard and HelpContactRow components
affects: [14-14, profile-hubs]

actuals:
  tokens: 14000
  tasks: 2
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Mail intent through a launchEmail seam on the stateless content; ActivityNotFoundException caught into a snackbar naming the address"
    - "Expanded FAQ ids kept in rememberSaveable via a listSaver over SnapshotStateList"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/subviews/HelpComponents.kt
    - app/src/main/res/values/strings_help.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpScreenTest.kt
  modified: []

key-decisions:
  - "HelpContent takes a launchEmail: (Intent) -> Unit seam instead of an onEmailSupport lambda, so the ActivityNotFoundException catch and snackbar live in the content and are testable without the platform"
  - "refresh() with nothing loaded is a retry (no list to keep), so a pull on the failed state shows the skeleton again"

patterns-established:
  - "No resolveActivity / <queries> for mailto: start the intent and catch the miss"

requirements-completed: [PROF-05]

duration: 45min
completed: 2026-10-29
status: complete
plan_head_before: 4b40aff415a7e5278fcf17fe862865fb5524c4e7
plan_head_after: 9ba4c760fda876325c4abc223fddc0704e2bde76
---

# Phase 14 Plan 06: Help / FAQ Summary

**Help screen that loads GET help/faqs (empty categories dropped, stable sort by display_order), expands categories in place with an Expanded/Collapsed state, and keeps an Email Us mailto row reachable in every state, degrading to a snackbar that names support@clickme.app.**

## Accomplishments

- `buildHelpSections` filters categories with no featured articles and stable-sorts by `displayOrder`; `HelpViewModel` loads in `init` with a single latest-wins job, `retry()`, quiet `refresh()` that keeps the list and flags a failure, and `consumeRefreshFailure()`.
- `HelpFaqCard`: `surfaceContainerLow` card, 56dp header as one `Role.Button` with `stateDescription` "Expanded"/"Collapsed", chevron rotating over 200 ms, 200 ms expand/shrink body, missing snippet omitted, plain `Text` only.
- `HelpScreen`/`HelpContent`: `PullToRefreshBox` over a `LazyColumn`; skeleton on first load; failed state is `EmptyState` + Retry with Contact Us still below; empty-after-filter shows only Contact Us; snackbars for refresh failure and no mail app.
- 16 tests (7 view model, 9 screen), all green; `ktlintCheck`, `assembleDevDebug` and `scripts/check-design-rules.sh` pass.

## Task Commits

1. Task 1 RED: `2be14c6` test(14-06) failing tests with stubs (4 of 5 failing on assertions or a ClassCastException from the unloaded stub)
2. Task 1 GREEN: `38e85e6` feat(14-06) categories in server order, expandable cards
3. Task 2 RED: `6c76d50` test(14-06) mail fallback, failed state, refresh (6 failing)
4. Task 2 GREEN: `9ba4c76` feat(14-06) Email Us fallback, failed and refresh states

## TDD Gate Compliance

RED then GREEN commits exist for both tasks. RED evidence is the Gradle console run (no `tdd-red-evidence` JSON record, as the runner is JUnit via Gradle and plan type is `execute`, not `tdd`). Semantic assessment: Task 1 RED failed on the planned assertions (section list empty, nodes missing, Loading state cast); Task 2 RED failed on the planned behaviors (ActivityNotFoundException uncaught, error heading and refresh flag absent). Two tests (`allEmptyCategoriesGiveNoSections`, `emptyListShowsOnlyContactUs`) and the retry/network VM tests passed already at RED because Task 1 behavior covered them; they are regression guards, not RED targets.

## Deviations from Plan

### Auto-fixed Issues

None for correctness. Two interpretation notes:

1. **Seam shape.** The plan described an `onEmailSupport: () -> Unit` parameter with the try/catch in `HelpScreen`'s default. The snackbar must be shown by the content that owns the `SnackbarHostState`, so the seam is `launchEmail: (Intent) -> Unit` on `HelpContent` and the try/catch sits there. `HelpScreen` supplies `context.startActivity`. Tests inject a throwing lambda as the plan intended, and `emailUsStartsSendTo` additionally drives the real `HelpScreen` and asserts the started Activity via Robolectric.
2. **Task staging.** The stub commit for RED included minimal stubs of the key, view model and `HelpContent` so the tests fail on assertions rather than compile errors (INVALID_RED otherwise).

## Authentication Gates

None.

## Known Stubs

None.

## Threat Flags

None. T-14-18 (plain `Text`, no markup parsing) and T-14-19 (ActivityNotFoundException caught, address in snackbar, `noMailAppShowsAddress` test) are implemented as planned.

## Open Items

- PROF-05 edge probe stays flagged for the verifier (as the plan assumed).
- On-device check of the real mail client hand-off and TalkBack announcement of Expanded/Collapsed is a manual backstop (deferred, per the Phase 2 manual-testing note).
- `HelpKey` must be registered in both shells by 14-14; it is not wired into navigation here.

## Self-Check: PASSED

All eight created files exist; commits 2be14c6, 38e85e6, 6c76d50 and 9ba4c76 are ancestors of HEAD.
