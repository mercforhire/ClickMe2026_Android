---
phase: 10-expert-topics-availability
verified: 2026-10-26T00:00:00Z
status: passed
score: 11/12 must-haves verified
covered_files:
  - .planning/phases/10-expert-topics-availability/10-01-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-01-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-02-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-02-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-03-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-03-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-04-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-04-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-05-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-05-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-06-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-06-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-07-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-07-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-08-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-08-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-09-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-09-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-10-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-10-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-11-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-11-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-12-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-12-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-13-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-13-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-14-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-14-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-15-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-15-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-16-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-16-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-17-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-17-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-18-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-18-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-19-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-19-SUMMARY.md
  - .planning/phases/10-expert-topics-availability/10-20-PLAN.md
  - .planning/phases/10-expert-topics-availability/10-20-SUMMARY.md
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ExpertRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/NullPreservingAdapters.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScheduleLogic.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorRules.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/subviews/TopicTagPicker.kt
covered_digest: "v3:sha256:9acd873fd5052f278d05165837995df1801948b95469d004570f149ac9aca8c9"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 6/7
  gaps_closed:
    - "Edit Expertise cannot wipe the profile's tags by accident (10-11 must-have, threat T-10-34); CR-01, closed by 10-17"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "SC-1 Topics on device: create a paid topic, edit it (12.50 existing price shows '12.50'), delete with the confirmation; a price below the floor shows the live error; a server 422 PRICE_BELOW_MINIMUM shows on the price field; blank price saves a free topic shown as 'Free'; Edit Expertise adds a profile tag that then appears in the editor picker"
    expected: "All flows work against the production backend as an expert on the Samsung SM-A075F (devDebug)"
    why_human: "Real device and live backend; deferred by the user (STATE.md [Phase 10] DEFERRED (human) a)"
  - test: "SC-2 Availability on device: edit slots with Select Time Range; two overlapping slots show the banner and disable Save; Save shows the check for 2 s; reopen shows the same HH:mm; during a live call Save shows 'End the call to edit your availability.' and nothing is sent; while Save is spinning, day toggles and slot edits do nothing"
    expected: "As described"
    why_human: "Visual, timing and live-call behavior; deferred (b)"
  - test: "SC-3 Timezone on device: expert with a UTC server zone and no availability on a non-UTC device sees the device zone filled without a prompt and it is stored server-side (reopen); picking a zone then Save persists it"
    expected: "As described"
    why_human: "Needs a UTC-zone expert account and a live backend; deferred (c)"
  - test: "SC-4 cross-client: on the iOS client, the Android expert's new topic is listed and booking slots match the saved availability in the expert's timezone"
    expected: "Topic visible; slots match HH:mm and timezone"
    why_human: "Requires the iOS app and a live backend; no Android-side code can prove it. Wire shapes were cross-checked against the backend schemas by the code review (including the omitted-versus-null rule for duration_mins); deferred (d)"
  - test: "French (Canada) locale: type '12,50' in the price field and save"
    expected: "1250 minor units sent"
    why_human: "Device locale; deferred (e)"
  - test: "200% font scale on Topics, Topic Editor with an error line, an overlapping day and the Time Range sheet; rotation on the editor with an error showing; TalkBack on the Time Range sheet; focus movement into the first invalid field"
    expected: "No clipping, error and focus preserved"
    why_human: "Visual and accessibility; deferred (f)"
  - test: "Edit Expertise seed failure on device (gap closure, item g): with the network blocked after the app is open, open Edit Expertise; then restore the network and tap Retry. Also open it on a slow connection and tap a tag row while 'Loading your tags...' shows"
    expected: "'Couldn't load your current tags' with Retry, Save Expertise disabled, no PATCH sent; after Retry the profile tags appear (primary first) and Save enables; taps during Loading do nothing"
    why_human: "Real network failure and slow-response timing on a device; deferred (g). The behavior is covered by JVM tests with a gated fake."
---

# Phase 10: Expert Topics & Availability Verification Report

**Phase Goal:** An Android expert can publish bookable topics and weekly availability that clients can see.
**Verified:** 2026-10-26
**Status:** human_needed
**Re-verification:** Yes, after gap closure (plans 10-17..10-20). Previous status gaps_found, 6/7.

## Previous Gap: CR-01 / 10-11 must-have T-10-34 — CLOSED

The old failure mode: the selection was seeded by a separate async `GET /expert/profile` whose failure was swallowed and whose late result was discarded once the user touched the selection; Save was gated only on a non-empty selection, so a one-tag save replaced every existing tag.

Evidence in the code, read directly (not from the SUMMARY):

- `ExpertiseEditorViewModel.kt:39-47` defines `ExpertiseSeedState { Loading, Failed(error), Ready }`. `ExpertiseEditorUiState.seed` defaults to `Loading` (fails closed, `:55`).
- `canSave` is `seed == Ready && selected.isNotEmpty() && !isSaving` (`:77-78`).
- `save()` returns unless `seed == Ready` (`:201`), so the PATCH is unreachable while Loading or Failed.
- `toggle()` (`:177`) and `updateSelection()` (`:191`, which `remove()` also uses) return unless `seed == Ready`. Nothing can change the selection before it holds the full profile, so the late seed is never discarded and never needs merging (`:152-154` writes `selected = primaryFirst(tags)` and `Ready` together).
- A failed fetch sets `Failed` (`:149`) instead of returning silently; `retry()` (`:164-168`) reloads it. The sheet renders a Retry body (`ExpertiseEditorSheet.kt:193-203`, string `expertise_editor_seed_error`) and a Loading body (`:206-208`, `expertise_editor_seed_loading`); both strings exist in `strings_expert_topics.xml:88-89`.
- Process-death restore (`:108`) is treated as Ready. This is sound because the `SavedStateHandle` selection keys are written only by `updateSelection`, which is itself gated on Ready.
- Regression tests ran and pass (see Behavioral Spot-Checks): `aProfileLoadFailureFailsTheSeedAndDisablesSave` (profile fetch throws: seed Failed, toggle ignored, `save()` sends 0 PATCHes), `aTagTappedBeforeTheSeedLandsIsIgnoredAndTheLateSeedKeepsEveryExistingTag` (gated fake: taps ignored, late seed lands in full, final PATCH contains all three tags with primary first), `retryAfterASeedFailureSeedsTheSelectionAndEnablesSave`, `savedStateRestoresTheSelectionAndQueryInsteadOfSeeding`.

CR-01 is closed. Truth 7 moves from FAILED (partial) to VERIFIED.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC-1: Expert creates and edits a topic in the Topics tab; a price below the `/me` `pricing_limits` floor shows `PRICE_BELOW_MINIMUM` on the price field | VERIFIED | Unchanged from the previous report: `TopicEditorViewModel.save()` uses `repository.create/update`; local floor check via `floorNow()`; `onSaveFailed` maps `ErrorCode.PriceBelowMinimum` to `PriceFieldError.Server` on the price field (`TopicEditorViewModel.kt` onSaveFailed). `priceBelowMinimumFromTheServerLandsOnThePriceField` passes in `TopicEditorViewModelTest` (39 tests, 0 failures). |
| 2 | SC-2: Weekly availability edited with the time-range picker sheet; overlapping ranges on a day rejected; saved slots round-trip as `HH:mm` | VERIFIED | `save()` returns when `overlappingDays` is non-empty (`AvailabilityViewModel.kt:224`); `wireRanges`/`hydrate` codec and 24:00 round trip covered by `AvailabilityScheduleLogicTest` (36 pass) and `AvailabilityViewModelTest` (23 pass). |
| 3 | SC-3: Expert picks availability timezone; if the server zone is UTC the device zone is filled quietly | VERIFIED | `applyInitial` calls `autoFillZone(serverId, Timezones.detected())` once per session (`:122-129`), then the silent PUT. Now additionally gated on a lossless hydrate (`:140`, see truth 10). The fill itself is unchanged; tests for the silent PUT, for no fill when not UTC, for restore and for in-call pass. |
| 4 | SC-4: An iOS client sees the Android expert's new topic with booking slots matching the saved availability | UNCERTAIN (human) | Cannot be proven from this repo. Wire shapes cross-checked against backend schemas in 10-REVIEW.md (explicit-null PATCH adapter registered; `duration_mins` omitted when unchanged, which the backend `patchTopicSchema` accepts as optional; `HH:mm`; tag UUID case). Routed to human item SC-4. |
| 5 | EXP-02: price floor comes from `/me` `pricing_limits`, fetched fresh, not cached across launches | VERIFIED | Unchanged; `TopicEditorViewModel.floorNow()` reads the in-memory `/me` value at each use and refreshes when null. No gap-closure change touched it (10-18 changed only duration and the tag picker). |
| 6 | D-13 hand-off: availability Save guarded by `CallGuard.attempt(RestrictedAction.EditAvailability)` | VERIFIED | `AvailabilityViewModel.kt:223`, before any state change; the silent PUT reads `isCallActive` only (`:251`). `saveDuringACallSendsNothingAndStaysIdle` passes. |
| 7 | 10-11 / 10-17: Edit Expertise can never wipe the profile's tags by accident (T-10-34) | VERIFIED | See "Previous Gap: CR-01" above. Behavior-dependent (state-transition and ordering invariant); backed by passing named tests with a gated fake, not by presence alone. |
| 8 | 10-17: while the profile tags load, taps/removals are ignored and Save is disabled; a failed load shows the error, server message and Retry; Retry seeds and enables Save | VERIFIED | `ExpertiseEditorViewModel.kt:142-168, 176-196`; sheet bodies `ExpertiseEditorSheet.kt:193-208`; `retryAfterASeedFailureSeedsTheSelectionAndEnablesSave` passes. |
| 9 | 10-18 (WR-02): a stored off-grid or above-480 duration is shown unchanged; an unrelated edit sends a PATCH without `duration_mins`; a changed duration is a 30-minute-grid value in 30..480; buttons snap from off-grid (45 -> 30/60, 600 -> 480 with + disabled); new topics still always send `duration_mins` | VERIFIED | `TopicEditorRules.seedDuration` keeps any positive stored value (`:84`); `stepDuration` snaps to the grid and coerces to 30..480 (`:91-104`). Edit branch sends `durationMins = current.durationMins.takeIf { it != seedDuration(seed.durationMins) }` (`TopicEditorViewModel.kt` save); `UpdateExpertTopicRequest.durationMins: Int?` (`ExpertRequests.kt:39`); the adapter writes the key only when non-null (`NullPreservingAdapters.kt`, `value.durationMins?.let`), while `description` and `icon_slug` still go out as explicit null; the create branch passes `current.durationMins` always. Tests pass: `anUnchangedDurationIsLeftOutOfThePatch`, `anOffGridStoredDurationIsKeptUntilTheUserStepsIt`, `aStoredDurationAboveTheCapIsShownAndKeptOnAnUnrelatedEdit`, `aChangedDurationIsSentInThePatch`, `patchBodyOmitsAnUnchangedDuration`, `patchBodyWritesExplicitNullsForClearedFields`. |
| 10 | 10-18 (WR-05): when the Topic Editor's profile tags fail to load, the selected tags stay visible as removable chips above Retry; a removed tag is not sent | VERIFIED | `TopicTagPicker.kt` `loadFailed` branch renders `SelectableChip` per selected tag with `onToggle`, then `FailedRow`. `aTagRemovedWhileTheProfileTagsFailedIsNotSent` passes; `TopicEditorSubviewsTest` (25) passes. |
| 11 | 10-19 (WR-04, WR-01): the silent UTC PUT is sent only when hydrate kept every server slot; while a Save is in flight edits and pull-to-refresh do nothing; an edit during the Saved hold ends the hold and a later Save's spinner is not cut short | VERIFIED | `isLossless` shares `parseRange` with `hydrate` (`AvailabilityScheduleLogic.kt:98-106`); `applyInitial` sends only if `fill != null && isLossless(rawRanges(data))` (`:140`) and the body is still the fully hydrated schedule. `edit()` returns on Saving and cancels `saveJob` and sets Idle on Saved (`:201-217`); `refresh()` returns on Saving (`:88`). Tests pass: `aSilentPutIsSkippedWhenHydrateDroppedAServerSlot`, `editsAndRefreshAreIgnoredWhileSaving`, `anEditDuringTheSavedHoldEndsTheHoldAndANewSaveIsNotCutShort`. The stated truth is the pull-after-save order only; the reverse order is the open WR-01 below (does not contradict the written truth). |
| 12 | 10-20: full gate green on the merged tree; review ledger has no open prior finding | VERIFIED | Gate per `10-20-SUMMARY.md` (a69bcc3; `git log -1 -- app/src/main` shows no later code change): 2558 tests per flavor, 0 failures, ktlint, lint both flavors, design rules, R8 `assembleProdRelease` green. I did not rerun the full suite (scoped runs only, below). `10-REVIEW-DISPOSITION.md` is consistent with the code: CR-01/WR-01/WR-02/WR-04/WR-05 fixed (confirmed in code above); WR-03 and IN-01..IN-07 deferred with reasons. |

**Score:** 11/12 truths verified (SC-4 counted as a human item, not as verified; 0 failed; 0 behavior-unverified)

### Required Artifacts (gap-closure plans)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ExpertiseEditorViewModel.kt` | `sealed interface ExpertiseSeedState`, seed-gated save/edit | VERIFIED | Present, substantive, gated (see above) |
| `ExpertiseEditorSheet.kt` | seed Loading and Failed bodies with Retry | VERIFIED | Contains `expertise_editor_seed_error`, wired to `viewModel::retry` (`:122`, `:201`) |
| `strings_expert_topics.xml` | `expertise_editor_seed_loading`, `expertise_editor_seed_error` | VERIFIED | Lines 88-89 |
| `ExpertRequests.kt` | `UpdateExpertTopicRequest.durationMins: Int?` | VERIFIED | Line 39 |
| `NullPreservingAdapters.kt` | `duration_mins` written only when non-null | VERIFIED | `value.durationMins?.let { ... }` |
| `TopicEditorRules.kt` | `seedDuration` keeps stored value; `stepDuration` snaps | VERIFIED | Lines 84, 91-104 |
| `TopicTagPicker.kt` | selected chips in load-failed state with `Locale.ROOT` | VERIFIED | `loadFailed` branch |
| `AvailabilityScheduleLogic.kt` | `isLossless(raw)` with shared parser | VERIFIED | Lines 98-106 |
| `AvailabilityViewModel.kt` | lossless-gated silent PUT; Saving guard; cancellable hold | VERIFIED | `:88`, `:140`, `:201-217`, `saveJob` |
| `10-REVIEW-DISPOSITION.md`, `10-VALIDATION.md` | ledger and rows | VERIFIED | Present; ledger matches code |

### Key Link Verification

| From | To | Status | Details |
|------|----|--------|---------|
| `ExpertiseEditorUiState.canSave` | `ExpertiseSeedState.Ready` | WIRED | `:78` |
| Sheet seed Failed item | `ExpertiseEditorViewModel.retry` | WIRED | `EmptyStateAction(... onRetry)` at `ExpertiseEditorSheet.kt:201`, `onRetry = viewModel::retry` at `:122` |
| `ExpertiseEditorViewModel.save` | `ExpertiseTagsRepository.saveProfileTags` | WIRED | Reached only when seed Ready (`:201, :205`) |
| `TopicEditorViewModel.save` (edit) | `UpdateExpertTopicRequestAdapter` | WIRED | `durationMins = null` when unchanged yields no key |
| `TopicEditorViewModel` init | `TopicEditorRules.seedDuration` | WIRED | stored duration shown as-is |
| `TopicTagPicker` loadFailed | `toggleTag` | WIRED | `onToggle(tag)` on chips |
| `AvailabilityViewModel.applyInitial` | `AvailabilityScheduleLogic.isLossless` | WIRED | `:140` |
| `AvailabilityViewModel.edit/refresh` | `SaveState.Saving` | WIRED | `:88`, `:205` |
| All prior links (HomeExpertScreen routes, TopicEvents, MoshiProvider adapter, CallGuard) | | WIRED | Unchanged since the previous report; no regression found |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real Data | Status |
|----------|------|--------|-----------|--------|
| `ExpertiseEditorViewModel.selected` | profile tags | `repository.profileTags()` (`GET /expert/profile`) | Yes | FLOWING |
| `TopicsViewModel` list | `topics` | `api.getMyTopics()` | Yes | FLOWING |
| `AvailabilityViewModel` days/timezone | schedule | `api.getMyAvailability()` | Yes | FLOWING |
| `TopicEditorViewModel` floor | `pricingLimits` | in-memory `/me` | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Gap-closure and phase invariants | `./gradlew --offline :app:testDevDebugUnitTest --tests` ExpertiseEditorViewModelTest (12), ExpertiseEditorSheetTest (11), ExpertTopicRequestJsonTest (6), TopicEditorRulesTest (14), TopicEditorViewModelTest (39), TopicEditorSubviewsTest (25), AvailabilityViewModelTest (23), AvailabilityScheduleLogicTest (36) | BUILD SUCCESSFUL; result XML: 166 tests, 0 skipped, 0 failures, 0 errors; the named regression tests listed in truths 7-11 are present and ran | PASS |
| Full gate (2558 tests per flavor, ktlint, lint, R8, design rules) | Not rerun by me; recorded in 10-20-SUMMARY.md on a69bcc3 with no later code change | n/a | SKIP (scoped run only) |

### Probe Execution

No probes declared. Skipped.

### Requirements Coverage

All four IDs appear in PLAN frontmatter (10-01, 10-04, 10-16 and 10-20 claim all four; 10-17 EXP-01; 10-18 EXP-01, EXP-02; 10-19 EXP-03, EXP-04) and in REQUIREMENTS.md lines 119-122 and the traceability table (lines 310-313). No orphaned IDs.

| Requirement | Source Plans | Status | Evidence |
|-------------|--------------|--------|----------|
| EXP-01 (topics list, create/edit, tag source) | 10-01,03..07,09..11,13,15..18,20 | SATISFIED | Truths 1, 7, 8, 9, 10; the CR-01 caveat is closed |
| EXP-02 (price floor from fresh `pricing_limits`, `PRICE_BELOW_MINIMUM` on field) | 10-01,03,04,07,10,13,16,18,20 | SATISFIED | Truths 1, 5 |
| EXP-03 (weekly availability, `HH:mm`, picker sheet, overlap validation, 24:00 round trip) | 10-01,02,04,05,08,12,14..16,19,20 | SATISFIED | Truths 2, 11 |
| EXP-04 (timezone pick, UTC auto-fill) | 10-01,02,04,05,08,12,14..16,19,20 | SATISFIED | Truths 3, 11 |

Note: REQUIREMENTS.md still shows EXP-01..04 unchecked and "Pending" (lines 119-122, 310-313); ROADMAP.md still shows Phase 10 unchecked. The orchestrator should flip them once the deferred human checks are accepted or completed. This verifier does not edit them.

### Anti-Patterns Found

No TBD/FIXME/XXX/TODO/HACK markers in `screens/expertmode` or `NullPreservingAdapters.kt` (grep returned nothing). No stubs, no empty handlers, no hard-coded empty data flowing to UI in the changed files.

Open findings from the incremental re-review (10-REVIEW.md: 0 critical, 3 warning, 5 info), each assessed against the must-haves, confirmed by reading the code:

| Finding | Severity | Defeats a must-have? | Assessment |
|---------|----------|----------------------|------------|
| WR-01 Save can start while a pull-to-refresh is in flight; the late pre-PUT GET then overwrites the sent schedule | Warning | No | Confirmed: `save()` (`AvailabilityViewModel.kt:220-240`) neither cancels `loadJob` nor gates on `isRefreshing`, and `applyPull` (`:143`) is unconditional. The written 10-19 truth covers pull-after-save-start, which holds and is tested. Narrow, user-initiated race, display-only (the server holds the saved copy; reopening shows it). SC-2 happy path and EXP-03 are intact. Recommended: `loadJob?.cancel()` in `save()` plus a test. |
| WR-02 Silent timezone PUT not serialized with a user Save | Warning | No | Confirmed: `silentSaveIfNeeded` (`:247-260`) launches an untracked PUT, and its Saving guard is dead code (IN-04). It needs the user to edit and Save within one network round trip of opening a screen whose server zone was UTC and whose schedule was lossless; the last-processed PUT wins. Silent data loss is possible but very narrow. It does not contradict any written must-have (D-15 skips are enumerated: lossless, no call, no overlaps); EXP-03/EXP-04 are met on the normal path. Recommended before release: send the timezone only, or join the silent job in `save()`. |
| WR-03 Stale profile-tags fetch can overwrite `onExpertiseSaved` | Warning | No | Confirmed: `onExpertiseSaved` (`TopicEditorViewModel.kt`) calls `applyProfileTags` without cancelling `tagsJob`. Requires a load/Retry GET issued before the sheet's PATCH and answered after it. Does not touch the EXP-01 happy path or the T-10-34 profile-wipe guard (it affects the topic editor's local view, not the profile PATCH). Recommended one-line fix: `tagsJob?.cancel()`. |
| IN-01..IN-05 | Info | No | Cosmetic or UX (selection editable during the sheet save, selected chips hidden while tags Loading, no feedback for ignored edits during Saving, dead guard, default-locale `lowercase()`). None changes persisted data. |
| Previously deferred WR-03 (lenient `expertHasTopics()`) and IN-01..IN-07 of the first review | Deferred by the user | No | Recorded in 10-REVIEW-DISPOSITION.md and STATE todos. |

Note on ledger numbering: `10-REVIEW-DISPOSITION.md` top table lists the three new warnings and five infos as `open`; they are the new review's IDs, not the first review's. They are non-blocking and the user's standing preference is to defer them; they should be carried to a follow-up pass or the STATE todo list.

### Human Verification Required

See the `human_verification` frontmatter: SC-1 through SC-4 device and cross-client checks, French locale price entry, 200% font scale/rotation/TalkBack, and the new item (g) for the Edit Expertise seed failure. These are the user's deferred items (STATE.md "[Phase 10] DEFERRED (human)") and are recorded as non-blocking per the user's standing preference.

### Gaps Summary

No gaps. The one blocking gap from the previous report (CR-01, 10-11 must-have T-10-34) is closed in code and by passing regression tests: the Edit Expertise save is now unreachable until the profile tags have loaded in full, edits are ignored until then, a failed load shows Retry, and the late seed always lands in full. The other gap-closure fixes (WR-02 duration, WR-05 failed-state chips, WR-04 lossless silent PUT, WR-01 save-time guard) are present, wired, and covered by passing tests. The 3 new warnings from the incremental re-review are real but narrow races that do not defeat a must-have; fixing them (cancel `loadJob` in `save()`, serialize or narrow the silent PUT, cancel `tagsJob` in `onExpertiseSaved`) is recommended before release. The overall status is human_needed only because of the on-device and cross-client checks the user deferred, including SC-4, which cannot be proven in this repository.

---

_Verified: 2026-10-26_
_Verifier: Claude (gsd-verifier)_
