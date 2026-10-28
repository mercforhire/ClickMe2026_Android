---
phase: "13"
slug: "push-notifications-internal-testing"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-10-28"
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + Compose ui-test + MockWebServer3 + kotlinx-coroutines-test; backend jest |
| **Config file** | `app/build.gradle.kts` (`unitTests.isIncludeAndroidResources = true`); backend `jest.config.js` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*PushRouterTest' --tests '*PushEventTest' ktlintCheck` |
| **Full suite command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDevDebugUnitTest testProdDebugUnitTest ktlintCheck -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g`, then separately `lintDevDebug lintProdDebug assembleProdRelease`, plus `bash scripts/check-design-rules.sh`; backend `npm test` |
| **Estimated runtime** | ~60 seconds (quick), ~10 minutes (full) |

---

## Sampling Rate

- **After every task commit:** Run the quick command with the task's own test classes plus `ktlintCheck`
- **After every plan wave:** Run `testDevDebugUnitTest` with `-Xmx4g`, then `bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite (both flavors, lint, `assembleProdRelease`, backend `npm test`) must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

Seeded from RESEARCH.md § Validation Architecture; task IDs are bound by the planner.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | PUSH-01 | — | Register body exactly {device_id, token, platform:"android", app_version}; no-op without session | unit | `--tests '*FcmPushTokenRegistrarTest' --tests '*PushRegistrationManagerTest' --tests '*UserManagerTest'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | PUSH-02 | — | N/A | unit (Robolectric) + jest | `--tests '*NotificationChannelsTest' --tests '*PushManifestTest' --tests '*PushPermissionGateTest'`; backend `npm test -- --silent tests/push.test.js` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | PUSH-03 | — | Non-UUID ids rejected | unit | `--tests '*PushEventTest' --tests '*PushRouterTest'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | PUSH-04 | — | Stale/history intents never re-route; no mode switch without Expert role | unit + Compose | `--tests '*MainActivityPushIntentTest' --tests '*PushDestinationResolverTest' --tests '*NavigatorOpenRoutedTest' --tests '*PushRoutingViewModelTest' --tests '*HomeShellsTest' --tests '*PushMessageHandlerTest'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | PUSH-05 | — | N/A | unit + Compose | `--tests '*NotificationsGroupingTest' --tests '*NotificationBodyParserTest' --tests '*NotificationDestinationTest' --tests '*NotificationsViewModelTest' --tests '*NotificationsScreenTest'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | REV-01 | — | Body has no extra keys | unit + Compose | `--tests '*ReviewsRepositoryTest' --tests '*WriteReviewViewModelTest' --tests '*BookingSummary*' --tests '*PastBookingCard*'` | ❌ W0 / ✅ extend | ⬜ pending |
| TBD | TBD | TBD | REL-04 / REL-05 | — | N/A | build only | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleProdRelease` | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] All test files in the map above (none exist yet for push, notifications or reviews)
- [ ] `FakeClickMeApi` handlers for the new notifications/reviews endpoints
- [ ] Resolver lookup fakes (`FakeBookingProbe` or equivalent interfaces)
- [ ] `PushTokenSource` interface seam so JVM tests never touch Firebase
- [ ] Backend: new cases in `tests/push.test.js` (file exists, extend)

*No framework install needed.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Push tap from killed app and from running app opens right tab/back stack on a real device | PUSH-04 | Needs real FCM delivery to a device | Send booking/chat/session pushes to the Samsung test device in both states; confirm screen and back behaviour |
| Android 13+ permission prompt and channels visible in system settings | PUSH-02 | System UI | Fresh install on API 33+; confirm prompt and the defined channels in App info → Notifications |
| Store listing, data safety, FGS declaration drafted; signed bundle on internal track; testers added on a mix of devices | REL-04, REL-05 | Play Console / Firebase console are not scriptable | Follow the human checklist plan; confirm `google_app_id` present in the release APK |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
