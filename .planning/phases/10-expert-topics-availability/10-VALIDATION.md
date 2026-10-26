---
phase: 10
slug: expert-topics-availability
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-10-24
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + Compose UI test + kotlinx-coroutines-test 1.11.0 + mockwebserver3 (all existing) |
| **Config file** | `app/build.gradle.kts`; shared test helpers in `app/src/test/.../testing/` (`MainDispatcherRule`, `FakeClickMeApi`, `FakeCallGuard`) |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests "com.uptrendinvestments.clickmeapp.screens.expertmode.*"` |
| **Full suite command** | `JAVA_HOME=... ./gradlew testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug assembleProdDebug && bash scripts/check-design-rules.sh` (lint + R8 `assembleProdRelease` in a separate invocation; never `--stop` while parallel executors run) |
| **Estimated runtime** | ~180 seconds (quick ~60s) |

---

## Sampling Rate

- **After every task commit:** Run the task's `<automated>` command (tests for touched files) + `ktlintCheck`
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` + `bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

Filled at planning time (plan/task ids from 10-01..10-16); statuses are set by the gate plan 10-16. Quick-run prefix: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest`.

| Req ID | Behavior | Plan / Task | Test Type | Automated Command | File Exists | Status |
|--------|----------|-------------|-----------|-------------------|-------------|--------|
| EXP-01 | DTO decodes real server shape (`price` only, `currency:""`) | 10-01 T1 | unit (decode) | `--tests "*ExpertTopicDtoTest"` | ✅ (10-01) | ✅ green |
| EXP-02 | PATCH body has explicit `"icon_slug":null` / `"description":null`; create body has exactly the schema keys | 10-01 T1, T2 | adapter unit + MockWebServer | `--tests "*ExpertTopicRequestJsonTest" --tests "*ExpertApiTest"` | ✅ (10-01) | ✅ green |
| Regression | `UserManager.expertHasTopics` works with the typed payload | 10-01 T2 | unit | `--tests "*UserManagerTest" --tests "*AppViewModelTest" --tests "*LoginViewModelTest"` | ✅ (fixtures migrated) | ✅ green |
| EXP-03 | Hydrate, default schedule, overlap (touching allowed), add-hours clamp, sorted `HH:mm` body, `24:00` round trip | 10-02 T1 | pure unit | `--tests "*AvailabilityScheduleLogicTest"` | ✅ (10-02) | ✅ green |
| EXP-03 / EXP-04 | Picker options with injected off-grid/24:00 value, UTC-pinned display, saved-state codec, name join; UTC detection, auto-fill, unbundled label, picker entries | 10-02 T2 | pure unit | `--tests "*AvailabilityScheduleLogicTest" --tests "*AvailabilityTimezoneLogicTest"` | ✅ (10-02) | ✅ green |
| EXP-02 | Price parse (blank, 0, decimals, comma, JPY, negative, overflow), floor lookup/fallback, free wire amount, currency resolution | 10-03 T1 | pure unit | `--tests "*TopicPriceTest"` | ✅ (10-03) | ✅ green |
| EXP-01 | Icon list, duration bounds, caps, tag filter/cap/apply, lower-case ids | 10-03 T2 | pure unit | `--tests "*TopicEditorRulesTest"` | ✅ (10-03) | ✅ green |
| D-13 hand-off | `restrictedActionText(EditAvailability)` -> "End the call to edit your availability." | 10-04 T1 | Robolectric | `--tests "*CallDialogsTest"` | ✅ (extended) | ✅ green |
| EXP-01..04 | SelectableChip `enabled`, expert error mapper, duration text | 10-04 T2 | Robolectric + unit | `--tests "*PickerComponentsTest" --tests "*ExpertErrorsTest" --tests "*TopicFormattingTest"` | ✅ (10-04) | ✅ green |
| EXP-01 / EXP-03 | Stateless repositories, profile-tag save body, TopicEvents drained on logout | 10-05 T1, T2 | unit | `--tests "*ExpertRepositoriesTest" --tests "*TopicEventsTest"` | ✅ (10-05) | ✅ green |
| EXP-01 | Topic card, overflow, delete spinner, delete confirm, skeleton, empty/error | 10-06 T1, T2 | Robolectric | `--tests "*TopicComponentsTest"` | ✅ (10-06) | ✅ green |
| EXP-01 / EXP-02 | Icon strip, duration stepper, description counter, price field helper/error, tag picker states | 10-07 T1, T2 | Robolectric | `--tests "*TopicEditorSubviewsTest"` | ✅ (10-07) | ✅ green |
| EXP-03 | Day card, overlap banner, Save action states, skeleton | 10-08 T1 | Robolectric | `--tests "*AvailabilityComponentsTest"` | ✅ (10-08) | ✅ green |
| EXP-03 / EXP-04 | Time-range sheet: start < end filtering, current off-grid value injected, Done/Cancel; timezone sheet extra row and check | 10-08 T2 | Robolectric | `--tests "*TimeRangePickerSheetTest" --tests "*TimezonePickerSheetTest"` | ✅ (10-08) | ✅ green |
| EXP-01 | Topics list load/empty/error/refresh; delete busy + failure dialog; saved event merges in place | 10-09 T1 | unit (VM) | `--tests "*TopicsViewModelTest"` | ✅ (10-09) | ✅ green |
| EXP-01 | Topics screen states, FAB only when topics exist, overflow Edit/Delete, delete confirm, Availability action | 10-09 T2 | Robolectric | `--tests "*TopicsScreenTest"` | ✅ (10-09) | ✅ green |
| EXP-01 / EXP-02 | Editor seed from key, currency rules, `/me` refreshed once, live floor error, tags load/cap/drop | 10-10 T1 | unit (VM) | `--tests "*TopicEditorKeyTest" --tests "*TopicEditorViewModelTest"` | ✅ (10-10) | ✅ green |
| EXP-02 | Required-field errors, free/paid wire bodies, 422 `PRICE_BELOW_MINIMUM` on the price field, other errors -> dialog, saved event + finish | 10-10 T2 | unit (VM) | `--tests "*TopicEditorViewModelTest"` | ✅ (10-10) | ✅ green |
| EXP-01 (D-08) | Expertise sheet seed (primary first), guard on empty, save body, failure dialog; signup tags unchanged | 10-11 T1, T2 | unit + Robolectric | `--tests "*ExpertiseEditorViewModelTest" --tests "*ExpertiseEditorSheetTest" --tests "*SignupTagsScreenTest"` | ✅ (10-11) | ✅ green |
| EXP-03 / EXP-04 | Hydrate, edits, refresh overwrite, unbundled `display_name`, Auto-detect local only, process-death restore | 10-12 T1 | unit (VM) | `--tests "*AvailabilityViewModelTest"` | ✅ (10-12) | ✅ green |
| EXP-03 / EXP-04 | Save guard first (`attempts == [EditAvailability]`, no PUT), overlap re-check, Saving -> Saved -> Idle at 2000 ms, failure dialog; silent UTC PUT once, skipped when device UTC / saving / overlap / `isCallActive`, failure swallowed, never `attempt()` | 10-12 T2 | unit (VM + FakeCallGuard + virtual time) | `--tests "*AvailabilityViewModelTest"` | ✅ (10-12) | ✅ green |
| EXP-01 / EXP-02 | Editor screen: titles, Save/saving lock, helper vs error lines, dialog, expertise sheet hand-off | 10-13 T2 | Robolectric | `--tests "*TopicEditorScreenTest"` | ✅ (10-13) | ✅ green |
| EXP-03 / EXP-04 | Availability screen: states, banner text, Save enabled rule, sheet slots, day card callbacks | 10-14 T2 | Robolectric | `--tests "*AvailabilityScreenTest"` | ✅ (10-14) | ✅ green |
| D-01 | Topics slot, editor and Availability entries, Profile Availability row (both stacks) | 10-15 T2 | Robolectric | `--tests "*HomeShellsTest"` | ✅ (extended) | ✅ green |
| All | Full gate both flavors + lint + R8 + design rules + sweeps | 10-16 T1 | build | see 10-16 verify chain | n/a | ✅ green |
| EXP-01 (CR-01) | Seed Failed disables Save and Retry seeds | 10-17 T1 | unit (VM) + Robolectric | `--tests "*ExpertiseEditorViewModelTest" --tests "*ExpertiseEditorSheetTest"` | ✅ (gap) | ✅ green |
| EXP-01 (CR-01) | Edits blocked until the seed is Ready, late seed kept | 10-17 T2 | unit (VM) + Robolectric | `--tests "*ExpertiseEditorViewModelTest" --tests "*ExpertiseEditorSheetTest"` | ✅ (gap) | ✅ green |
| EXP-01 / EXP-02 (WR-02) | Unchanged duration omitted from PATCH | 10-18 T1 | adapter unit + VM | `--tests "*ExpertTopicRequestJsonTest" --tests "*TopicEditorViewModelTest"` | ✅ (gap) | ✅ green |
| EXP-01 (WR-02) | Stored duration kept, stepper snaps onto the grid | 10-18 T2 | pure unit + VM | `--tests "*TopicEditorRulesTest" --tests "*TopicEditorViewModelTest"` | ✅ (gap) | ✅ green |
| EXP-01 (WR-05) | Failed tag load keeps selected chips removable | 10-18 T3 | Robolectric + VM | `--tests "*TopicEditorSubviewsTest" --tests "*TopicEditorViewModelTest"` | ✅ (gap) | ✅ green |
| EXP-04 (WR-04) | Silent PUT skipped when hydrate is lossy | 10-19 T1 | pure unit + VM | `--tests "*AvailabilityScheduleLogicTest" --tests "*AvailabilityViewModelTest"` | ✅ (gap) | ✅ green |
| EXP-03 (WR-01) | Edits and refresh ignored while saving, edit ends the Saved hold | 10-19 T2 | VM + virtual time | `--tests "*AvailabilityViewModelTest"` | ✅ (gap) | ✅ green |
| All | Gap-pass full gate (2558 tests per flavor) | 10-20 T1 | build | see the 10-20 verify chain | n/a | ✅ green |
| SC-4 | iOS client sees topic and matching slots | 10-16 T2 (logged) | manual cross-client | none (deferred human check in STATE) | manual | ✅ logged (device check deferred, see STATE) |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `FakeClickMeApi` handlers for `getMyTopics` (typed), `createTopic`, `updateTopic`, `deleteTopic`, `getMyAvailability`, `updateMyAvailability`, `updateExpertProfileTags` (10-01; no `getMyTopic`: the editor is always seeded from the list item)
- [x] Update the 5 `MyExpertTopicsPayload(listOf(mapOf(...)))` fixtures in `AppViewModelTest` (1), `UserManagerTest` (3) and `LoginViewModelTest` (1) to the typed item (`topicItem()` helper, 10-01)
- [x] New test files from the map above, plus shared helpers (`topicItem()`, `availabilityData()`)
- [x] `SelectableChip(enabled=)` addition covered by a component test
- [x] No framework install needed

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| iOS client sees the Android expert's new topic with booking slots matching saved availability | EXP-01, EXP-03 (SC-4) | Needs iOS app + two accounts | Create topic + availability on Android, open expert profile on iOS client, compare slots |
| Decimal-comma price entry on a French-locale device | EXP-02 | Device keyboard behaviour | Set device locale to fr-FR, enter `12,50`, save, confirm stored amount |

*Both rows are tracked in STATE under "[Phase 10] DEFERRED (human)" (non-blocking; user defers manual testing).*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 180s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-10-25 (plan 10-16 gate)

Gap closure 10-17..10-20 verified 2026-10-25
