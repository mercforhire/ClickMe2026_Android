---
phase: 10-expert-topics-availability
verified: 2026-10-25T00:00:00Z
status: gaps_found
score: 6/7 must-haves verified
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
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorViewModel.kt
covered_digest: "v3:sha256:eee23e3432c5a9b85faea9b001312fe9dcfcc787584a4ce906e7f4a9d4c580c1"
behavior_unverified: 0
overrides_applied: 0
gaps:
  - truth: "Edit Expertise cannot wipe the profile's tags by accident (10-11 must-have, threat T-10-34); the sheet is the EXP-01 tag-source for topics"
    status: partial
    reason: "CR-01 (open). The selection is seeded by a separate async GET /expert/profile whose failure is swallowed and whose late result is discarded once the user touches the selection. Save is gated only on selected.isNotEmpty(), so with a failed or slow seed the user can save a one-tag selection and PATCH /expert/profile (full replace) silently drops every existing tag including the primary. The empty-selection guard holds, but the stated purpose (no accidental wipe) does not."
    artifacts:
      - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/subviews/ExpertiseEditorViewModel.kt"
        issue: "loadProfileTags() returns silently on ApiException; 'if (touched) return@launch' drops the late seed; canSave does not wait for the seed"
    missing:
      - "A seed state (Loading / Failed / Ready) gating canSave on Ready, with a Retry affordance on failure"
      - "Merge (not discard) the late profile tags when the user has already touched the selection, or block toggling until Ready"
      - "Regression tests: profile fetch fails then Save is disabled; late seed after a toggle keeps existing tags"
human_verification:
  - test: "SC-1 Topics on device: create a paid topic, edit it (12.50 existing price shows '12.50'), delete with the confirmation; a price below the floor shows the live error; a server 422 PRICE_BELOW_MINIMUM shows on the price field; blank price saves a free topic shown as 'Free'; Edit Expertise adds a profile tag that then appears in the editor picker"
    expected: "All flows work against the production backend as an expert on the Samsung SM-A075F (devDebug)"
    why_human: "Real device and live backend; deferred by the user (STATE.md [Phase 10] DEFERRED (human) a)"
  - test: "SC-2 Availability on device: edit slots with Select Time Range; two overlapping slots show the banner and disable Save; Save shows the check for 2 s; reopen shows the same HH:mm; during a live call Save shows 'End the call to edit your availability.' and nothing is sent"
    expected: "As described"
    why_human: "Visual, timing and live-call behavior; deferred (b)"
  - test: "SC-3 Timezone on device: expert with a UTC server zone and no availability on a non-UTC device sees the device zone filled without a prompt and it is stored server-side (reopen); picking a zone then Save persists it"
    expected: "As described"
    why_human: "Needs a UTC-zone expert account and a live backend; deferred (c)"
  - test: "SC-4 cross-client: on the iOS client, the Android expert's new topic is listed and booking slots match the saved availability in the expert's timezone"
    expected: "Topic visible; slots match HH:mm and timezone"
    why_human: "Requires the iOS app and a live backend; no Android-side code can prove it. Wire shapes were cross-checked against the backend schemas by the code review; deferred (d)"
  - test: "French (Canada) locale: type '12,50' in the price field and save"
    expected: "1250 minor units sent"
    why_human: "Device locale; deferred (e)"
  - test: "200% font scale on Topics, Topic Editor with an error line, an overlapping day and the Time Range sheet; rotation on the editor with an error showing; TalkBack on the Time Range sheet; focus movement into the first invalid field"
    expected: "No clipping, error and focus preserved"
    why_human: "Visual and accessibility; deferred (f)"
---

# Phase 10: Expert Topics & Availability Verification Report

**Phase Goal:** An Android expert can publish bookable topics and weekly availability that clients can see.
**Verified:** 2026-10-25
**Status:** gaps_found
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC-1: Expert creates and edits a topic in the Topics tab; a price below the `/me` `pricing_limits` floor shows `PRICE_BELOW_MINIMUM` on the price field | VERIFIED | `HomeExpertScreen.kt:69-85` wires `TopicsScreen` (Add/Edit) to `TopicEditorScreen` via `TopicEditorViewModel.Factory`. `TopicEditorViewModel.save()` uses `repository.create/update` (real `ClickMeApi.createTopic/updateTopic`, `ExpertTopicsRepository.kt`). Local floor check via `floorNow()`; server 422 maps `ErrorCode.PriceBelowMinimum` to `PriceFieldError.Server` (`:370-377`), rendered at `TopicEditorScreen.kt:316-325`. Test `priceBelowMinimumFromTheServerLandsOnThePriceField` passes (scoped run). |
| 2 | SC-2: Weekly availability edited with the time-range picker sheet; overlapping ranges on a day rejected; saved slots round-trip as `HH:mm` | VERIFIED | `AvailabilityScreen.kt` wires `TimeRangePickerSheet` to `updateSlot`. `AvailabilityScheduleLogic.dayOverlaps/overlappingDays` (touching ok, running-max-end rule). `AvailabilityViewModel.save()` returns when `overlappingDays` non-empty, `canSave` disables the Save action; `wireRanges` emits sorted `HH:mm` via `Locale.ROOT`, `parseHhmm` accepts 24:00 (1440), `hydrate` round-trips it. Overlap, hydrate and codec tests pass. |
| 3 | SC-3: Expert picks availability timezone; if the server zone is UTC the device zone is filled quietly | VERIFIED | `TimezonePickerSheet` wired through `selectTimezone`; `applyInitial` calls `AvailabilityTimezoneLogic.autoFillZone(serverId, Timezones.detected())` once per session then `silentSaveIfNeeded()` (one PUT, skipped in call or with overlaps). Tests `aUtcServerZoneIsReplacedByTheDeviceZoneWithOneSilentPut`, `etcUtcInAnyCase...`, `noFillWhen...`, `aRestoredViewModelNeverAutoFills`, `duringACallTheZoneIsFilledButNothingIsSent` pass. |
| 4 | SC-4: An iOS client sees the Android expert's new topic with booking slots matching the saved availability | UNCERTAIN (human) | Cross-client outcome is not provable from this repo. Request shapes (explicit-null PATCH adapter registered in `MoshiProvider.kt:22`, `HH:mm`, tag UUID case, free-topic positive amount) were cross-checked against backend schemas in 10-REVIEW.md and match. Routed to human item SC-4. |
| 5 | EXP-02: price floor comes from `/me` `pricing_limits`, fetched fresh, not cached across launches | VERIFIED | `MeData.pricingLimits` documented as never persisted; `SessionStore` does not store it. `TopicEditorViewModel` reads `userManager.me.value?.pricingLimits` via `floorNow()` at each use and calls `userManager.refreshMe()` when limits are null. |
| 6 | D-13 hand-off: availability Save guarded by `CallGuard.attempt(RestrictedAction.EditAvailability)` | VERIFIED | `RestrictedAction.EditAvailability` in `managers/CallDialog.kt:15`; copy mapped in `CallDialogs.kt:49`; guard runs before any state change at `AvailabilityViewModel.kt:197`. Test `saveDuringACallSendsNothingAndStaysIdle` passes. |
| 7 | 10-11 must-have: Edit Expertise can never wipe the profile's tags by accident (T-10-34) | FAILED (partial) | Empty selection is blocked, but CR-01 shows a failed or slow profile-tag seed lets a one-tag save replace all existing tags. See gap. |

**Score:** 6/7 truths verified (SC-4 is counted as a human item, not as verified; one truth FAILED)

### Required Artifacts

`verify.artifacts` over all 16 plans: every declared artifact exists and passes (4/4, 2/2, 2/2, 4/4, 4/4, 3/3, 3/3, 4/4, 2/2, 2/2, 3/3, 2/2, 1/1, 1/1, 1/1, 2/2). No stubs. Phase source tree under `screens/expertmode` totals about 6k lines with real logic.

### Key Link Verification

The `verify.key-links` verb could not evaluate the plans (their `from` fields are symbol descriptions, not file paths), so links were checked manually.

| From | To | Status | Details |
|------|----|--------|---------|
| `HomeExpertScreen` topicsRoot | `TopicsScreen` / `TopicEditorKey` / `AvailabilityKey` | WIRED | `HomeExpertScreen.kt:69-88` |
| `TopicEditorViewModel.save` | `TopicEvents.emit` -> `TopicsViewModel.merge` | WIRED | `TopicEditorViewModel.kt` emits `TopicSavedEvent`; `TopicsViewModel` init collects `topicEvents.events` |
| `MoshiProvider` | `UpdateExpertTopicRequestAdapter` | WIRED | `MoshiProvider.kt:22` |
| `ClickMeApi.updateTopic/createTopic/deleteTopic/getMyAvailability/updateMyAvailability` | repositories | WIRED | `ExpertTopicsRepository.kt`, `AvailabilityRepository.kt` |
| `AvailabilityViewModel.save` | `CallGuard.attempt(EditAvailability)` | WIRED | `:197`; silent save reads `isCallActive` only |
| `ExpertiseEditorViewModel.save` | `ExpertiseTagsRepository.saveProfileTags` | WIRED | PATCH `/expert/profile`; functionally flawed, see CR-01 |
| Profile footer / Topics | `AvailabilityKey(Profile/Topics)` | WIRED | `HomeExpertScreen.kt:73,140` |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real Data | Status |
|----------|------|--------|-----------|--------|
| `TopicsViewModel` list | `topics` | `api.getMyTopics()` | Yes | FLOWING |
| `AvailabilityViewModel` days/timezone | `ExpertAvailabilityScheduleData` | `api.getMyAvailability()` | Yes | FLOWING |
| `TopicEditorViewModel` floor | `pricingLimits` | in-memory `/me` (`refreshMe`) | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Availability VM, schedule logic, topic editor VM, price rules, expertise VM | `./gradlew --offline testDevDebugUnitTest --tests` (AvailabilityViewModelTest, AvailabilityScheduleLogicTest, TopicEditorViewModelTest, TopicPriceTest, ExpertiseEditorViewModelTest) | BUILD SUCCESSFUL | PASS |
| Full gate (2541 tests per flavor, ktlint, lint, R8, design rules) | Not rerun; claimed in 10-16-SUMMARY.md | n/a | SKIP (scoped run only) |

### Probe Execution

No probes declared. Skipped.

### Requirements Coverage

All four IDs appear in PLAN frontmatter (10-01 and 10-04 and 10-16 claim all four) and in REQUIREMENTS.md; no orphans.

| Requirement | Source Plans | Status | Evidence |
|-------------|--------------|--------|----------|
| EXP-01 (topics list, create/edit, at least one topic) | 10-01,03..07,09..11,13,15,16 | SATISFIED (with CR-01 caveat on the Edit Expertise sheet) | Truths 1, 7; list/editor/delete wired and tested |
| EXP-02 (price floor from fresh `pricing_limits`, `PRICE_BELOW_MINIMUM` on field) | 10-01,03,04,07,10,13,16 | SATISFIED | Truths 1, 5 |
| EXP-03 (weekly availability, `HH:mm`, picker sheet, overlap validation, 24:00 round trip) | 10-01,02,04,05,08,12,14,15,16 | SATISFIED | Truth 2 |
| EXP-04 (timezone pick, UTC auto-fill) | 10-01,02,04,05,08,12,14,15,16 | SATISFIED | Truth 3 |

Note: REQUIREMENTS.md still shows EXP-01..04 as unchecked / "Pending"; the orchestrator should flip them after gaps close.

### Anti-Patterns Found

No TBD/FIXME/XXX/TODO/HACK or placeholder markers in the phase files. No stubs. Open code review findings assessed against must-haves:

| Finding | Severity | Defeats a must-have? | Assessment |
|---------|----------|----------------------|------------|
| CR-01 Edit Expertise overwrites tags with an incomplete selection | Critical | Yes (10-11 truth / T-10-34) | Confirmed in code. Reported as gap. |
| WR-01 Availability editable/refreshable while Save in flight; Saved can show an unsent schedule | Warning | No | Edge race; SC-2 happy path intact. Fix recommended before release (`edit`/`refresh` guard on `SaveState.Saving`). |
| WR-02 Opening a stored topic clamps duration to 30..480 and rewrites it on save | Warning | No | Legacy or off-grid data only; silent change of a bookable session length. Fix recommended. |
| WR-03 Strict `ExpertTopicItem` decode in `expertHasTopics()` could flip first-launch mode to Client | Warning | No | Latent; shape matches today. Keep the launch lookup lenient. |
| WR-04 Silent UTC auto-fill PUT sends the hydrated schedule and could delete slots hydrate dropped | Warning | No | Matches plan truth (D-15) and is rare; sending timezone only is safer. |
| WR-05 Failed profile-tag load hides selected tags but Save still sends them | Warning | No | Seed preserved, no loss; UX. |
| IN-01..IN-07 | Info | No | Cosmetic and cleanup. |

### Human Verification Required

See the `human_verification` frontmatter: SC-1 through SC-4 device and cross-client checks, French locale price entry, and 200% font scale, rotation and TalkBack. These are the user's deferred items (STATE.md "[Phase 10] DEFERRED (human)"), recorded as non-blocking.

### Gaps Summary

The functional core of the phase is present, wired and tested: topics list and editor with fresh `pricing_limits` floor and server `PRICE_BELOW_MINIMUM` on the price field, the availability screen with the time-range picker, overlap rejection, lossless `HH:mm` (including 24:00), the timezone picker with silent UTC auto-fill, and the call guard. Scoped tests for these invariants pass.

One gap remains: CR-01. The Edit Expertise sheet, which feeds the Topic Editor's tag picker, can silently replace an expert's profile tags (including primary) with an incomplete selection when the profile-tag seed fails or arrives late. This defeats the stated purpose of the 10-11 guard and should be closed (seed state gating Save, merge on late seed, regression tests) before the phase is considered complete. WR-01, WR-02 and WR-04 are recommended fixes in the same pass but do not defeat a must-have. SC-4 (iOS client sees the topic and matching slots) cannot be proven in this repo and is a deferred human check.

---

_Verified: 2026-10-25_
_Verifier: Claude (gsd-verifier)_
