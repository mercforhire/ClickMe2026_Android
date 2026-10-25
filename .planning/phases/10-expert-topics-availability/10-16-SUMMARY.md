---
phase: 10-expert-topics-availability
plan: 16
subsystem: expert-mode
tags: [gate, release-build, sweeps, validation, follow-ups]
requires: [10-01, 10-02, 10-03, 10-04, 10-05, 10-06, 10-07, 10-08, 10-09, 10-10, 10-11, 10-12, 10-13, 10-14, 10-15]
provides:
  - "Green Phase 10 gate on both flavors (tests, ktlint, lint, R8 release, design rules)"
  - "10-VALIDATION.md signed off (nyquist_compliant: true)"
  - "STATE / ROADMAP text for the orchestrator to apply (see below)"
affects: [STATE.md, ROADMAP.md]
key-files:
  modified:
    - .planning/phases/10-expert-topics-availability/10-VALIDATION.md
    - .planning/REQUIREMENTS.md
decisions:
  - "STATE.md and ROADMAP.md were not edited here (orchestrator owns them in worktree mode); their Phase 10 content is supplied below for the orchestrator to apply"
metrics:
  completed: 2026-10-25
status: complete
actuals:
  tasks: 2
  commits: 2
plan_head_before: 52018a59d3db081d40537755ca6fb4397794401e
plan_head_after: 6514c2d77b5a022da0c8b2260f6c3a0796333afa
commits: 2
---

# Phase 10 Plan 16: Gate and close-out Summary

Phase 10 gate is green on both flavors (2541 unit tests each, ktlint, lint, R8 `assembleProdRelease`, design rules), all scope and safety sweeps are clean, and VALIDATION is signed off; the STATE and ROADMAP close-out text is below for the orchestrator.

## Gate results

| Step | Result |
|------|--------|
| `testDevDebugUnitTest` / `testProdDebugUnitTest` | 2541 tests each, 0 failures, 0 errors, 0 skipped |
| `ktlintCheck`, `assembleDevDebug`, `assembleProdDebug`, `:spikes:stripe:assembleDebug` | pass (invocation 1, 4m41s, with `-Xmx3g/-Xmx4g` flags, no OOM) |
| `scripts/check-design-rules.sh` and `... spikes` | OK / OK |
| `lintDevDebug lintProdDebug` (own invocation after `--stop`) | pass |
| `assembleProdRelease` (R8, own invocation after `--stop`) | pass |

## Sweeps (E/ = `screens/expertmode`)

| Sweep | Result |
|-------|--------|
| `Text("[A-Za-z]` literals | 0 |
| literal `contentDescription/stateDescription/onClickLabel` | 1, inside `@Preview` `AvailabilityTimeButtonPreview` (preview sample only, not shipped UI); accepted |
| `fontSize` / `Color(0x` | 0 / 0 |
| `LocalTime.of` in availability (non-comment) | 0 |
| DataStore / SharedPreferences / PreferenceManager | 0 |
| `Log.x(` | 0 |
| `callGuard.attempt(` | 2 in E/: `AvailabilityViewModel.save` (EditAvailability, the Phase 10 one) and the pre-existing Phase 9 `HomeExpertViewModel` LogOut guard (commit 3bd06e9). The plan's "exactly 1" assumed an availability-only scope; within the Phase 10 code it is exactly 1 (D-03 holds) |
| `"expert/availability"` in `app/src/main/java` | 1 (`ApiRequestUrls`) |

## Deviations from Plan

**1. [Process] STATE.md and ROADMAP.md not edited.** Plan Task 2 edits them, but this run's dispatch and worktree rules reserve both files for the orchestrator. The exact text is in "Orchestrator close-out content" below. `REQUIREMENTS.md` (EXP-03 note) and `10-VALIDATION.md` were edited and committed.

**2. [Interpretation] callGuard sweep scope** (see table). No defect found; no code changes were needed in this plan.

Auth gates: none. Stubs: none. Threat flags: none.

## Orchestrator close-out content

### STATE.md, "Blockers/Concerns" (add)

- [Phase 10] DEFERRED (human): non-blocking (user defers manual testing). Automated gate green (2541 tests per flavor, ktlint, lint, R8 `assembleProdRelease`, design rules). Run on the Samsung SM-A075F (devDebug, production backend) logged in as an expert, with the iOS app as the client:
  - [ ] (a) SC-1 Topics: create a paid topic, edit it (an existing 12.50 price shows "12.50"), delete with the confirmation; a price below the floor shows the live error, and a server `422 PRICE_BELOW_MINIMUM` shows on the price field; a blank price saves a free topic shown as "Free"; Edit Expertise adds a profile tag that then appears in the editor's picker.
  - [ ] (b) SC-2 Availability: edit slots with Select Time Range; two overlapping slots show the banner and disable Save; Save shows the check for 2 s; reopening shows the same HH:mm values; during a live call Save shows "End the call to edit your availability." and nothing is sent.
  - [ ] (c) SC-3 Timezone: an expert whose server zone is UTC (no availability saved yet) on a non-UTC device sees the device zone filled without a prompt and it is stored server-side (reopen); picking a zone then Save persists it.
  - [ ] (d) SC-4 cross-client: on the iOS client, the Android expert's new topic is listed and the booking slots match the saved availability in the expert's timezone (research open question 3).
  - [ ] (e) French (Canada) locale: typing "12,50" saves 1250 minor units (RESEARCH A2, Pitfall 3).
  - [ ] (f) 200% font scale on Topics, the Topic Editor with an error line, an overlapping day with the banner and the Time Range sheet; rotation on the editor with an error showing; TalkBack on the Time Range sheet. Also from 10-13: focus movement into the first invalid field and scroll-into-view in the Topic Editor.

### STATE.md, "Pending Todos" (add)

- [Phase 10] iOS follow-ups found while porting: delete asks nothing (Android confirms, D-06); prices are whole units x100 (Android uses the currency's decimals, D-09); edit always sends the /me default currency (Android keeps the topic's own, D-10); a PATCH cannot clear the icon or description because Swift omits nil (Android sends explicit null); the time-range picker writes through with no Cancel revert; "Add hours" can produce a 24:00 end that the picker cannot show and `minutesToAMPM(1440)` shows "12:00 PM"; off-grid stored times (for example 09:15) have no selected row in the picker; the `deleteMyTopic` comment about a 409 is stale.
- [Phase 10] Backend notes: `DELETE /expert/topics/:id` is a soft delete with no active-booking check (never 409); `price_amount` has no upper bound in zod, so an absurd amount is a Postgres INTEGER overflow 500 (Android caps at 7 integer digits); the server now accepts `hourly_rate.amount = 0` for free topics while Android still sends the floor or 100 (D-11); `price.currency` is "" for free topics created without a currency; `PATCH /expert/profile` tag replace has no count cap; a 422 `VALIDATION_ERROR` carries "Validation failed" with the useful text in `errors[]`.
- [Phase 10] Open items (ask the user): offering midnight (12:00 AM, "24:00") as a selectable End time is a one-row change in `AvailabilityScheduleLogic.endOptions` (backend accepts it, Android already round-trips it) - research open question 1; the Edit Expertise sheet keeps the iOS "at least one tag" guard - confirm (research open question 2). Plan deviations worth knowing from the 10-xx SUMMARYs: 10-09 restarts an Initial load (not Quiet) when a saved-topic event arrives while the list is Loading; 10-11 matches tag ids case-insensitively; 10-10 round-trips `TopicEditorKey` through a hand-written encoder in the test (no `kotlinx-serialization-json` on the test classpath).
- [Phase 10] Hand-offs: Phase 11 adds the Dashboard entry to Availability by pushing `AvailabilityKey(AvailabilityOrigin.Dashboard)` (add the enum entry; the origin keeps keys distinct across tab stacks); Phase 14 replaces the expert Profile "Availability" placeholder row (D-01) and reuses `ExpertiseEditorSheet` / `ExpertiseTagsRepository` from `screens/expertmode/settings/`.
- Mark the existing "[Phase 10 hand-off] Add `RestrictedAction.EditAvailability` ..." todo as "DONE 2026-10-25 (10-04, 10-12)".

### ROADMAP.md (add "Planning notes" lines; change nothing else)

- Phase 11: "Phase 10 hand-off: add the Dashboard Availability entry by pushing `AvailabilityKey(AvailabilityOrigin.Dashboard)` (new `AvailabilityOrigin` value)."
- Phase 14 (append to its existing Planning notes): "Phase 10 hand-off: replace the Phase 10 Availability placeholder row in the expert Profile; reuse `ExpertiseEditorSheet` in expert settings."

## Self-Check: PASSED

- Commits cf71d1a (VALIDATION) and 6514c2d (REQUIREMENTS) exist on the branch.
- `grep -c 'nyquist_compliant: true'` on 10-VALIDATION.md prints 1.
- `Phase 10: a stored 24:00 end` present in REQUIREMENTS.md.
- STATE.md / ROADMAP.md deliberately untouched (acceptance greps for them are the orchestrator's to satisfy from the content above).
