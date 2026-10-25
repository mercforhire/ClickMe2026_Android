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

To be filled by the planner/executor per task. Requirement → test map from RESEARCH.md:

| Req ID | Behavior | Test Type | Automated Command | File Exists | Status |
|--------|----------|-----------|-------------------|-------------|--------|
| EXP-01 | Topics list load/empty/error/refresh; delete busy + failure dialog; saved event merges in place; DTO decodes real server shape (`price` only, `currency:""`) | unit (VM) + DTO decode | `--tests "*TopicsViewModelTest" --tests "*ExpertTopicDtoTest"` | ❌ W0 | ⬜ pending |
| EXP-01 | Topics screen states, FAB only when topics exist, overflow Edit/Delete, delete confirm | Robolectric Compose | `--tests "*TopicsScreenTest"` | ❌ W0 | ⬜ pending |
| EXP-01 | Editor required-title error, tag cap 3, tag drop after profile tag change, seed from key | unit (VM) | `--tests "*TopicEditorViewModelTest"` | ❌ W0 | ⬜ pending |
| EXP-02 | Price parse (blank, 0, decimals, comma, JPY, negative, overflow), floor lookup/fallback, free wire amount, live and on-Save floor error | pure unit | `--tests "*TopicPriceTest"` | ❌ W0 | ⬜ pending |
| EXP-02 | 422 `PRICE_BELOW_MINIMUM` lands on price field, editor stays open; other errors → dialog; `/me` refreshed once when limits null | unit (VM) | `--tests "*TopicEditorViewModelTest"` | ❌ W0 | ⬜ pending |
| EXP-02 | PATCH body has explicit `"icon_slug":null` / `"description":null` when cleared; create body has exactly the schema keys | MockWebServer / adapter unit | `--tests "*ExpertTopicRequestJsonTest"` | ❌ W0 | ⬜ pending |
| EXP-03 | Hydrate, default schedule, overlap detection (touching allowed), add-hours clamp, sorted `HH:mm` body with 7 days, `24:00` round-trip | pure unit | `--tests "*AvailabilityScheduleLogicTest"` | ❌ W0 | ⬜ pending |
| EXP-03 | Time-range sheet: start < end filtering, off-grid current value injected, Done writes / Cancel discards | Robolectric Compose | `--tests "*TimeRangePickerSheetTest"` | ❌ W0 | ⬜ pending |
| EXP-03 | Save disabled with overlap; guard before PUT (`attempts == [EditAvailability]`); saving → saved → idle after 2000 ms; failure dialog | unit (VM + FakeCallGuard + virtual time) | `--tests "*AvailabilityViewModelTest"` | ❌ W0 | ⬜ pending |
| EXP-04 | UTC server zone + non-UTC device → silent fill + one PUT; none when device UTC / saving / overlap / `isCallActive`; once per VM; failure swallowed; never calls `attempt()` | unit (VM) | `--tests "*AvailabilityViewModelTest"` | ❌ W0 | ⬜ pending |
| EXP-04 | Unbundled server zone shows `display_name`; picker sheet extra row; Auto-detect changes local state only | unit + Robolectric | `--tests "*AvailabilityViewModelTest" --tests "*TimezonePickerSheetTest"` | ❌ W0 | ⬜ pending |
| D-13 hand-off | `restrictedActionText(EditAvailability)` resolves to "End the call to edit your availability." | Robolectric | `--tests "*CallDialogsTest"` | partial | ⬜ pending |
| Regression | `UserManager.expertHasTopics` works with typed payload | unit | `--tests "*UserManagerTest" --tests "*AppViewModelTest"` | ✅ (fixtures) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `FakeClickMeApi` handlers for `getMyTopics` (typed), `getMyTopic`, `createTopic`, `patchTopic`, `deleteTopic`, `getMyAvailability`, `putMyAvailability`, `patchExpertProfile`
- [ ] Update the 8 `MyExpertTopicsPayload(listOf(mapOf(...)))` fixtures in `AppViewModelTest` and `UserManagerTest` to the typed item (`topicItem()` helper)
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
