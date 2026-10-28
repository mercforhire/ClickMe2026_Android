---
phase: "13"
slug: "push-notifications-internal-testing"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-28"
---

# Phase 13 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| FCM → app | Push payloads (notification + data) delivered by Google to `ClickMeMessagingService` or the system tray | Category, entity ids, title/body text (medium) |
| Notification tap / launcher intent → MainActivity | Exported activity receives extras from system-displayed notifications or any caller | Push keys and ids; untrusted until UUID-validated |
| App → backend | `POST/DELETE notifications/tokens`, `GET notifications`, review context and submit, all with the user's bearer | FCM token, device id (medium); review text (low) |
| Backend → FCM | `push.js` sends via firebase-admin service account | Device tokens, channel id, category |
| Device lock screen | Notifications visible without unlocking | Chat text, names, amounts (medium) |
| Repo / planning docs → git | Firebase config, keystore, release checklist | `google-services.json`, signing secrets (high if leaked) |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-13-01 | Tampering | `PushEvent.from` ids | medium | mitigate | `canonicalUuid` in `PushEvent.kt`; `PushEventTest.nonUuidIdsAreDropped` | closed |
| T-13-02 | Spoofing | Forged category/event_type | low | accept | See Accepted Risks AR-13-01 | closed |
| T-13-03 | Information disclosure | Pending destination after logout | low | mitigate | `PushLaunchRequestTest.clearSessionDataDropsPending` | closed |
| T-13-04 | Tampering | iOS message shape (backend) | medium | mitigate | Platform branch in `push.js`; deep-equal + `apns` assertions in `tests/push.test.js` | closed |
| T-13-05 | Information disclosure | Extra Android data keys | low | accept | AR-13-02 | closed |
| T-13-06 | Denial of service | Unknown category channel | low | mitigate | `channelIdFor` defaults to `general` (`push.js:95`); manifest default channel | closed |
| T-13-07 | Information disclosure | DTO `toString()` | medium | mitigate | Redacting `toString()`; `toStringHides*` tests (NotificationsApiTest, PushReviewApiTest, NotificationDestinationTest) | closed |
| T-13-08 | Tampering | Strict zod bodies | low | mitigate | `PLATFORM_ANDROID` constant, nulls omitted (`PushRequests.kt`); key-set wire tests | closed |
| T-13-09 | Elevation of privilege | Booking id IDOR in review paths | medium | accept | AR-13-03 | closed |
| T-13-10 | Denial of service | Duplicate content keys across stacks | medium | mitigate | `NavigatorOpenRoutedTest.destinationInStartStackIsReused`, `repeatedChainKeyIsCutFirst` | closed |
| T-13-11 | Information disclosure | Resolved route after logout | low | mitigate | `ShellRouteRequestTest.clearSessionDataDrops` | closed |
| T-13-12 | Denial of service | Release without google-services.json | medium | mitigate | Gradle `logger.warn` (`app/build.gradle.kts:17`); checklist step 4 `aapt2 … google_app_id` | closed |
| T-13-13 | Information disclosure | Committed Firebase config | low | mitigate | `git check-ignore app/google-services.json` matches; `git ls-files` empty | closed |
| T-13-14 | Information disclosure | Previous user's pushes after offline logout | medium | mitigate | `UNREGISTER_TIMEOUT_MS` DELETE then `deleteToken()` (`FcmPushTokenRegistrar.kt`) | closed |
| T-13-15 | Tampering | Registration racing logout | medium | mitigate | Mutex + same-access-token guard; `registerAfterUnregisterInSameSessionIsSkipped` | closed |
| T-13-16 | Information disclosure | Token/device id in logs | medium | mitigate | No `Log.`/`println(` in push services/managers (sweep 2026-10-28 clean); redacted request `toString()` | closed |
| T-13-17 | Spoofing | Registering without a session | low | mitigate | `tokens.hasSession()` gate in `FcmPushTokenRegistrar.register`; backend bearer scoping | closed |
| T-13-18 | Elevation of privilege | Push-driven switch to Expert for client-only account | high | mitigate | `allowSwitch` role check; `PushDestinationResolverTest.noExpertRoleNeverSwitchesToExpert` | closed |
| T-13-19 | Denial of service | Mode switch during live call | medium | mitigate | `PushDestinationResolverTest.switchBlockedDuringCallOpensCurrentList` | closed |
| T-13-20 | Information disclosure | Probing another user's booking | low | accept | AR-13-04 | closed |
| T-13-21 | Tampering | `meta_data` ids reaching a route | low | mitigate | `PushEvent.fromRow` UUID validation; `NotificationDestinationTest` | closed |
| T-13-22 | Information disclosure | Row titles/bodies in logs | low | mitigate | `NotificationRowModel.toString()` prints id + category (`NotificationRows.kt:24`); no logging | closed |
| T-13-23 | Repudiation | Review posted without explicit Submit | medium | mitigate | `WriteReviewViewModelTest.noRequestWithoutSubmit`, `inFlightIgnoresInput` | closed |
| T-13-24 | Information disclosure | Review text in logs/state dumps | low | mitigate | `WriteReviewUiState.toString()` override (length only) | closed |
| T-13-25 | Spoofing | Forged extras to MainActivity | medium | mitigate | String-only `PUSH_KEYS`, UUID-validated; `MainActivityPushIntentTest.nonStringExtrasIgnored` | closed |
| T-13-26 | Tampering | Notification PendingIntent hijack | medium | mitigate | `FLAG_IMMUTABLE`, explicit component; `PushNotificationFactoryTest.immutableIntentWithOnlyPushKeys` | closed |
| T-13-27 | Information disclosure | Push content on lock screen | medium | mitigate | Foreground: `VISIBILITY_PRIVATE` + generic public version (`privateWithGenericPublicVersion`). Background (REVIEW WR-02): backend `android.notification.visibility: 'private'` (quick-261028-nb7, `abde83a`, live in prod `2a7cf19`) | closed |
| T-13-28 | Repudiation | Replay of old notification intent | low | mitigate | `MainActivityPushIntentTest.restoredActivityDoesNotReread`, `extrasConsumedOnce` | closed |
| T-13-29 | Information disclosure | Tap delivered after logout / into next account | medium | mitigate | `PushRoutingViewModelTest.logoutDuringResolutionDrops`; SessionDataCleaner slots | closed |
| T-13-30 | Elevation of privilege | Unwanted mode switch | medium | mitigate | Resolver gate (T-13-18) + `AppViewModel.switchMode` call guard (`callGuard.attempt(SwitchModes)`) | closed |
| T-13-31 | Denial of service | Switch never lands, router stuck | low | mitigate | 5 s timeout, cleared in `finally`; `PushRoutingViewModelTest.switchTimeoutDrops` | closed |
| T-13-32 | Tampering | Markup injection in bodies | low | mitigate | Only `**` pairs bold (notifications parser); no HTML/link rendering | closed |
| T-13-33 | Denial of service | Missing settings activity crash | low | mitigate | `openNotificationSettings` catches `ActivityNotFoundException`, falls back to app details (`NotificationsListParts.kt:150`) | closed |
| T-13-34 | Tampering | Comment length/content | low | mitigate | `COMMENT_MAX = 1000` (`ReviewsRepository.kt:46`) + backend zod max; plain-text render | closed |
| T-13-35 | Repudiation | Double submission | low | mitigate | `WriteReviewScreenTest.submittingLocksForm` | closed |
| T-13-36 | Information disclosure | Secrets in planning docs | high | mitigate | Checklist names keys/paths only; password-value grep over `13-RELEASE-CHECKLIST.md` returns nothing | closed |
| T-13-37 | Repudiation | Inaccurate data safety declaration | medium | mitigate | Drafts derived from manifest + sent data (`13-PLAY-CONSOLE-DRAFTS.md`); human judgment check remains with UAT test 3 | closed |
| T-13-38 | Denial of service | Push-less release uploaded to testers | medium | mitigate | Checklist step 4 `aapt2 … grep google_app_id` gate before upload | closed |
| T-13-39 | Information disclosure | Other participant's review on summary | low | accept | AR-13-05 | closed |
| T-13-40 | Repudiation | Stale hand-off thanks wrong screen | low | mitigate | Booking-id check; `BookingSummaryViewModelTest.submittedHandOffReloadsAndThanks` | closed |
| T-13-41 | Repudiation | Re-prompting for notification permission | low | mitigate | `PushPermissionGateTest.askedSurvivesLogoutAndANewGate` | closed |
| T-13-42 | Denial of service | Prompt over live call / login | low | mitigate | `PushPermissionRequesterTest.waitsDuringCallThenLaunches` | closed |
| T-13-43 | Information disclosure | Push/token/review text in logs (phase-wide) | medium | mitigate | `Log.`/`println(` sweep over services/push, Push managers, screens/notifications, screens/push, writereview: clean | closed |
| T-13-44 | Denial of service | R8 stripping adapters/service | medium | mitigate | `assembleProdRelease` (R8) with dex check for `ClickMeMessagingService`, JSON adapters, nav keys (13-17-SUMMARY gate 3) | closed |
| T-13-45 | Repudiation | My Bookings consuming Summary hand-off | low | mitigate | `MyBookingsViewModelTest.summaryHandOffIsLeftAlone` | closed |
| T-13-46 | Elevation of privilege | Review pill shown for unreviewable booking | low | accept | AR-13-06 | closed |
| T-13-SC | Tampering | Package installs (firebase-bom, firebase-messaging, google-services plugin, coroutines-play-services) | low | mitigate / accept | 13-05: pinned official coordinates in version catalog; all other plans: no installs | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-13-01 | T-13-02 | Every push destination is a read-only screen that re-fetches with the user's own token; server enforces ownership (404/403) | Plan 13-01 threat model | 2026-10-28 |
| AR-13-02 | T-13-05 | `category`/`sub_category` are taxonomy strings already in the notifications row; no PII added | Plan 13-02 threat model | 2026-10-28 |
| AR-13-03 | T-13-09 | Server checks participation (403) and completion (404); client never authorizes | Plan 13-03 threat model | 2026-10-28 |
| AR-13-04 | T-13-20 | Probe uses the user's own token; server answers 404/403; resolver only learns "not my booking" | Plan 13-07 threat model | 2026-10-28 |
| AR-13-05 | T-13-39 | Context endpoint returns only the caller's own review (`reviewer_id = req.user.id`) | Plan 13-15 threat model | 2026-10-28 |
| AR-13-06 | T-13-46 | Pill is a convenience; `POST /bookings/:id/reviews` and context endpoint enforce participant + completion server-side | Plan 13-18 threat model | 2026-10-28 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-28 | 47 | 47 | 0 | /gsd-secure-phase (orchestrator, ASVS L1 grep verification; auditor short-circuited) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-28
