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

Seeded from RESEARCH.md § Validation Architecture; task IDs bound at planning (2026-10-28). Revised 2026-10-28: 13-15's Task 3 (Past card pill) became 13-18 (Wave 5), and the 13-17 gate moved to Wave 6. Commands are `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest <filters> ktlintCheck` unless stated.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 13-01 T1-T3 | 13-01 | 1 | PUSH-03, PUSH-04 | T-13-01, T-13-03 | Non-UUID ids rejected; pending slot cleared on logout | unit | `--tests '*PushEventTest' --tests '*PushRouterTest' --tests '*PushLaunchRequestTest'` | ❌ W0 | ⬜ pending |
| 13-02 T1-T2 | 13-02 | 1 | PUSH-02, PUSH-03 | T-13-04 | iOS message deep-equal to the pre-change shape | jest (backend) | `cd "/Users/LeonChen/Documents/ClickMe2026 Backend" && npm test -- --silent tests/push.test.js` | ✅ extend | ⬜ pending |
| 13-03 T1-T3 | 13-03 | 1 | PUSH-01, PUSH-05, REV-01 | T-13-07, T-13-08 | Register and review bodies carry only schema keys; toString redacts token and text | unit (MockWebServer) | `--tests '*NotificationsApiTest' --tests '*PushReviewApiTest' --tests '*NotificationsRepositoryTest' --tests '*ReviewsRepositoryTest'` | ❌ W0 | ⬜ pending |
| 13-04 T1-T3 | 13-04 | 1 | PUSH-04, PUSH-05, REV-01 | T-13-10, T-13-11 | No duplicate keys across stacks in use; route consumed only by its mode's shell | unit + Compose | `--tests '*NavigatorOpenRoutedTest' --tests '*ShellRouteRequestTest' --tests '*HomeShellsTest'` | ❌ W0 / ✅ extend | ⬜ pending |
| 13-05 T1-T2 | 13-05 | 1 | PUSH-02, REL-05 | T-13-12, T-13-13 | google-services.json never committed; builds without it | unit (Robolectric) | `--tests '*PushManifestTest' --tests '*NotificationChannelsTest' --tests '*CallManifestTest'` plus `:app:assembleDevDebug` | ❌ W0 | ⬜ pending |
| 13-06 T1-T3 | 13-06 | 2 | PUSH-01 | T-13-14..T-13-17 | No register without session; unregister within 3 s; late register cannot re-bind | unit | `--tests '*FcmPushTokenRegistrarTest' --tests '*PushRegistrationManagerTest' --tests '*PushPreferencesTest' --tests '*UserManagerTest' --tests '*PushManifestTest'` | ❌ W0 / ✅ extend | ⬜ pending |
| 13-07 T1-T3 | 13-07 | 2 | PUSH-04 | T-13-18, T-13-19 | No switch to Expert without the role; no switch during a call | unit | `--tests '*PushDestinationResolverTest' --tests '*PushRouteLookupsTest'` | ❌ W0 | ⬜ pending |
| 13-08 T1-T3 | 13-08 | 2 | PUSH-05 | T-13-21, T-13-22 | meta_data ids validated; rows inert without a destination | unit | `--tests '*NotificationsGroupingTest' --tests '*NotificationBodyParserTest' --tests '*NotificationDestinationTest' --tests '*NotificationsViewModelTest'` | ❌ W0 | ⬜ pending |
| 13-09 T1-T3 | 13-09 | 2 | REV-01 | T-13-23, T-13-24 | Nothing posted without an explicit Submit | unit | `--tests '*WriteReviewViewModelTest' --tests '*ReviewLaunchRequestTest'` | ❌ W0 | ⬜ pending |
| 13-10 T1-T3 | 13-10 | 3 | PUSH-02, PUSH-04 | T-13-25..T-13-28 | Stale/history intents never re-route; immutable PendingIntent; private lock-screen visibility | unit (Robolectric) + lint | `--tests '*PushMessageHandlerTest' --tests '*PushNotificationFactoryTest' --tests '*MainActivityPushIntentTest'`, then `lintDevDebug` | ❌ W0 | ⬜ pending |
| 13-11 T1-T3 | 13-11 | 3 | PUSH-04 | T-13-29..T-13-31 | Taps dropped on Login; nothing delivered across a session end | unit + Compose | `--tests '*PushRoutingViewModelTest' --tests '*PushRoutingHostTest'` | ❌ W0 | ⬜ pending |
| 13-12 T1-T3 | 13-12 | 3 | PUSH-05 | T-13-32, T-13-33 | Only `**` markup is interpreted; settings intent never crashes | Compose (Robolectric) | `--tests '*NotificationsScreenTest' --tests '*ExploreScreenTest' --tests '*ExpertDashboardScreenTest'` | ❌ W0 / ✅ extend | ⬜ pending |
| 13-13 T1-T3 | 13-13 | 3 | REV-01 | T-13-34, T-13-35 | In-flight lock; 1000 cap | Compose (Robolectric) | `--tests '*WriteReviewScreenTest' --tests '*AuthErrorDialogTest' --tests '*LoginScreenTest'` | ❌ W0 | ⬜ pending |
| 13-14 T1-T2 | 13-14 | 3 | REL-04, REL-05 | T-13-36..T-13-38 | No secrets in planning docs | docs + STATE greps | see 13-14 verify commands (grep chains) | n/a | ⬜ pending |
| 13-15 T1-T2 | 13-15 | 4 | REV-01 | T-13-39, T-13-40 | Booking Summary hand-off consumed only for its own booking id | unit + Compose | `--tests '*BookingSummaryViewModelTest' --tests '*BookingSummaryScreenTest' --tests '*HomeShellsTest'`, then `bash scripts/check-design-rules.sh` | ✅ extend | ⬜ pending |
| 13-16 T1-T2 | 13-16 | 4 | PUSH-02 | T-13-41, T-13-42 | Prompt once; never over Login or a call | unit + Compose | `--tests '*PushPermissionGateTest' --tests '*PushPermissionRequesterTest' --tests '*PushPreferencesTest' --tests '*UserManagerTest'` | ❌ W0 / ✅ extend | ⬜ pending |
| 13-18 T1-T2 | 13-18 | 5 | REV-01 | T-13-45, T-13-46 | My Bookings consumes only its own slot; the pill is a convenience and the backend enforces review eligibility | unit + Compose | `--tests '*MyBookingsViewModelTest' --tests '*MyBookingsScreenTest' --tests '*HomeShellsTest'`, then `bash scripts/check-design-rules.sh` | ✅ extend | ⬜ pending |
| 13-17 T1 | 13-17 | 6 | all | T-13-43, T-13-44 | No Phase 13 logging; R8 keeps keys and adapters | full gate | `testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug assembleProdDebug :spikes:stripe:assembleDebug`, then `lintDevDebug lintProdDebug`, then `assembleProdRelease` (each with `-Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g`), design rules, backend `npm test` | n/a | ⬜ pending |
| REL-04 / REL-05 (human) | 13-14 | 3 | REL-04, REL-05 | T-13-38 | Release APK contains google_app_id before upload | manual (Play Console) | `./gradlew :app:bundleProdRelease` (build only); 13-RELEASE-CHECKLIST.md steps 4-7 | n/a | ⬜ pending |

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
