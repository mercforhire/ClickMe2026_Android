---
phase: 10
slug: expert-topics-availability
status: draft
nyquist_compliant: false
wave_0_complete: false
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
| EXP-01 | DTO decodes real server shape (`price` only, `currency:""`) | 10-01 T1 | unit (decode) | `--tests "*ExpertTopicDtoTest"` | ❌ W0 (10-01) | ⬜ pending |
| EXP-02 | PATCH body has explicit `"icon_slug":null` / `"description":null`; create body has exactly the schema keys | 10-01 T1, T2 | adapter unit + MockWebServer | `--tests "*ExpertTopicRequestJsonTest" --tests "*ExpertApiTest"` | ❌ W0 (10-01) | ⬜ pending |
| Regression | `UserManager.expertHasTopics` works with the typed payload | 10-01 T2 | unit | `--tests "*UserManagerTest" --tests "*AppViewModelTest" --tests "*LoginViewModelTest"` | ✅ (fixtures migrated) | ⬜ pending |
| EXP-03 | Hydrate, default schedule, overlap (touching allowed), add-hours clamp, sorted `HH:mm` body, `24:00` round trip | 10-02 T1 | pure unit | `--tests "*AvailabilityScheduleLogicTest"` | ❌ W0 (10-02) | ⬜ pending |
| EXP-03 / EXP-04 | Picker options with injected off-grid/24:00 value, UTC-pinned display, saved-state codec, name join; UTC detection, auto-fill, unbundled label, picker entries | 10-02 T2 | pure unit | `--tests "*AvailabilityScheduleLogicTest" --tests "*AvailabilityTimezoneLogicTest"` | ❌ W0 (10-02) | ⬜ pending |
| EXP-02 | Price parse (blank, 0, decimals, comma, JPY, negative, overflow), floor lookup/fallback, free wire amount, currency resolution | 10-03 T1 | pure unit | `--tests "*TopicPriceTest"` | ❌ W0 (10-03) | ⬜ pending |
| EXP-01 | Icon list, duration bounds, caps, tag filter/cap/apply, lower-case ids | 10-03 T2 | pure unit | `--tests "*TopicEditorRulesTest"` | ❌ W0 (10-03) | ⬜ pending |
| D-13 hand-off | `restrictedActionText(EditAvailability)` -> "End the call to edit your availability." | 10-04 T1 | Robolectric | `--tests "*CallDialogsTest"` | ✅ (extended) | ⬜ pending |
| EXP-01..04 | SelectableChip `enabled`, expert error mapper, duration text | 10-04 T2 | Robolectric + unit | `--tests "*PickerComponentsTest" --tests "*ExpertErrorsTest" --tests "*TopicFormattingTest"` | ❌ W0 (10-04) | ⬜ pending |
| EXP-01 / EXP-03 | Stateless repositories, profile-tag save body, TopicEvents drained on logout | 10-05 T1, T2 | unit | `--tests "*ExpertRepositoriesTest" --tests "*TopicEventsTest"` | ❌ W0 (10-05) | ⬜ pending |
| EXP-01 | Topic card, overflow, delete spinner, delete confirm, skeleton, empty/error | 10-06 T1, T2 | Robolectric | `--tests "*TopicComponentsTest"` | ❌ W0 (10-06) | ⬜ pending |
| EXP-01 / EXP-02 | Icon strip, duration stepper, description counter, price field helper/error, tag picker states | 10-07 T1, T2 | Robolectric | `--tests "*TopicEditorSubviewsTest"` | ❌ W0 (10-07) | ⬜ pending |
| EXP-03 | Day card, overlap banner, Save action states, skeleton | 10-08 T1 | Robolectric | `--tests "*AvailabilityComponentsTest"` | ❌ W0 (10-08) | ⬜ pending |
| EXP-03 / EXP-04 | Time-range sheet: start < end filtering, current off-grid value injected, Done/Cancel; timezone sheet extra row and check | 10-08 T2 | Robolectric | `--tests "*TimeRangePickerSheetTest" --tests "*TimezonePickerSheetTest"` | ❌ W0 (10-08) | ⬜ pending |
| EXP-01 | Topics list load/empty/error/refresh; delete busy + failure dialog; saved event merges in place | 10-09 T1 | unit (VM) | `--tests "*TopicsViewModelTest"` | ❌ W0 (10-09) | ⬜ pending |
| EXP-01 | Topics screen states, FAB only when topics exist, overflow Edit/Delete, delete confirm, Availability action | 10-09 T2 | Robolectric | `--tests "*TopicsScreenTest"` | ❌ W0 (10-09) | ⬜ pending |
| EXP-01 / EXP-02 | Editor seed from key, currency rules, `/me` refreshed once, live floor error, tags load/cap/drop | 10-10 T1 | unit (VM) | `--tests "*TopicEditorKeyTest" --tests "*TopicEditorViewModelTest"` | ❌ W0 (10-10) | ⬜ pending |
| EXP-02 | Required-field errors, free/paid wire bodies, 422 `PRICE_BELOW_MINIMUM` on the price field, other errors -> dialog, saved event + finish | 10-10 T2 | unit (VM) | `--tests "*TopicEditorViewModelTest"` | ❌ W0 (10-10) | ⬜ pending |
| EXP-01 (D-08) | Expertise sheet seed (primary first), guard on empty, save body, failure dialog; signup tags unchanged | 10-11 T1, T2 | unit + Robolectric | `--tests "*ExpertiseEditorViewModelTest" --tests "*ExpertiseEditorSheetTest" --tests "*SignupTagsScreenTest"` | ❌ W0 (10-11) | ⬜ pending |
| EXP-03 / EXP-04 | Hydrate, edits, refresh overwrite, unbundled `display_name`, Auto-detect local only, process-death restore | 10-12 T1 | unit (VM) | `--tests "*AvailabilityViewModelTest"` | ❌ W0 (10-12) | ⬜ pending |
| EXP-03 / EXP-04 | Save guard first (`attempts == [EditAvailability]`, no PUT), overlap re-check, Saving -> Saved -> Idle at 2000 ms, failure dialog; silent UTC PUT once, skipped when device UTC / saving / overlap / `isCallActive`, failure swallowed, never `attempt()` | 10-12 T2 | unit (VM + FakeCallGuard + virtual time) | `--tests "*AvailabilityViewModelTest"` | ❌ W0 (10-12) | ⬜ pending |
| EXP-01 / EXP-02 | Editor screen: titles, Save/saving lock, helper vs error lines, dialog, expertise sheet hand-off | 10-13 T2 | Robolectric | `--tests "*TopicEditorScreenTest"` | ❌ W0 (10-13) | ⬜ pending |
| EXP-03 / EXP-04 | Availability screen: states, banner text, Save enabled rule, sheet slots, day card callbacks | 10-14 T2 | Robolectric | `--tests "*AvailabilityScreenTest"` | ❌ W0 (10-14) | ⬜ pending |
| D-01 | Topics slot, editor and Availability entries, Profile Availability row (both stacks) | 10-15 T2 | Robolectric | `--tests "*HomeShellsTest"` | ✅ (extended) | ⬜ pending |
| All | Full gate both flavors + lint + R8 + design rules + sweeps | 10-16 T1 | build | see 10-16 verify chain | n/a | ⬜ pending |
| SC-4 | iOS client sees topic and matching slots | 10-16 T2 (logged) | manual cross-client | none (deferred human check in STATE) | manual | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `FakeClickMeApi` handlers for `getMyTopics` (typed), `createTopic`, `updateTopic`, `deleteTopic`, `getMyAvailability`, `updateMyAvailability`, `updateExpertProfileTags` (10-01; no `getMyTopic`: the editor is always seeded from the list item)
- [ ] Update the 5 `MyExpertTopicsPayload(listOf(mapOf(...)))` fixtures in `AppViewModelTest` (1), `UserManagerTest` (3) and `LoginViewModelTest` (1) to the typed item (`topicItem()` helper, 10-01)
- [ ] New test files from the map above, plus shared helpers (`topicItem()`, `availabilityData()`)
- [ ] `SelectableChip(enabled=)` addition covered by a component test
- [ ] No framework install needed

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| iOS client sees the Android expert's new topic with booking slots matching saved availability | EXP-01, EXP-03 (SC-4) | Needs iOS app + two accounts | Create topic + availability on Android, open expert profile on iOS client, compare slots |
| Decimal-comma price entry on a French-locale device | EXP-02 | Device keyboard behaviour | Set device locale to fr-FR, enter `12,50`, save, confirm stored amount |

*Per memory: if no device available, log as open items rather than blocking.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
