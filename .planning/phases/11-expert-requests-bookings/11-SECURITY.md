---
phase: 11
slug: expert-requests-bookings
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: 2026-10-27
---

# Phase 11 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Audited against `main` HEAD `b2042e8` plus the 11-UAT completion commit (includes review fixes and gap-closure plans 11-16, 11-17, 11-18), and backend `4c694e7` for 11-18.

**Evidence path key:**
`A` = `app/src/main/java/com/uptrendinvestments/clickmeapp` · `T` = `app/src/test/java/com/uptrendinvestments/clickmeapp` · `BE` = `ClickMe2026 Backend` repo.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Backend -> app (DTO decoding) | Incoming requests, expert bookings, booking detail, dashboard and earnings JSON | Client names, topics, client messages, prices (gross/net), payout status; untrusted |
| App -> backend (bodies and paths) | Accept / decline / cancel / reschedule-response bodies and booking ids in paths; strict server schema | Decline reason and note, cancel reason, booking ids |
| Realtime socket -> app | `booking.update` events trigger reloads | Event payload (ignored; reload only) |
| Server URL -> Custom Tab | Single-use Stripe onboarding link opened in the browser | Onboarding URL (secret, ~5 min) |
| App -> calendar app (intent) | Add-to-Calendar insert intent with booking text extras | Title, times, client message |
| UI -> call guard | Booking-modifying and payout actions during a live call | RestrictedAction decisions |
| App -> Agora SDK / logcat | Engine config, channel join, failure and remote-presence logging | Token, channel, uid (never logged) |
| Backend join -> Agora | Server mints the RTC token for the canonical booking channel (11-18) | Token, lowercase booking id as channel |
| Build -> release artifact | R8 shrinking of nav keys and DTO adapters | Compiled classes |

---

## Threat Register

70 unique threats (87 plan rows; T-11-SC repeats in all 18 plans): 59 mitigate, 11 accept, 0 transfer. 15 high, 24 medium, 31 low.

| Threat ID | Category | Component | Severity | Disposition | Mitigation (evidence) | Status |
|-----------|----------|-----------|----------|-------------|-----------------------|--------|
| T-11-01 | Tampering | Accept/decline/cancel/reschedule bodies | medium | mitigate | `T/network/BookingRequestsApiTest.kt:149-171`, `ExpertBookingsApiTest.kt:155,176-180`; `A/model/requests/ExpertBookingRequests.kt:12-25` | closed |
| T-11-02 | Information disclosure | Repositories (message, reason, onboarding URL) | high | mitigate | "never logged" KDoc `BookingRequestsRepository.kt:15-20`; 0 `Log.` in `expertmode` | closed |
| T-11-03 | Denial of service | Expert booking detail URL | medium | mitigate | `ApiRequestUrls.kt:85`; `ExpertBookingsApiTest.kt:133`, `ApiRequestUrlsTest.kt:211,230` | closed |
| T-11-04 | Spoofing | Unknown/extra server keys | low | mitigate | `ExpertBookingsPayload.kt:21,56-57,80` (private note, accent, legacy earnings not declared) | closed |
| T-11-05 | Tampering | `RequestDecisionKey.isExpired` | low | accept | AR-11-01 | closed |
| T-11-06 | Information disclosure | `BookingConfirmedKey` in saved state | low | accept | AR-11-02 | closed |
| T-11-07 | Repudiation | One-shot hand-off | low | mitigate | `ExpertLaunchRequest.kt:12-26` (`getAndUpdate { null }`); `ExpertLaunchRequestTest.kt` | closed |
| T-11-08 | Elevation of privilege | `RestrictedAction` additions | medium | mitigate | Exhaustive `when` `CallDialogs.kt:44-52`; `CallDialogsTest.kt:166,175` | closed |
| T-11-09 | Tampering | Expert card on own proposal | low | mitigate | `RescheduleResponseCard.kt:159-163`; `RescheduleResponseCardTest.kt:218-257`; 403 -> reload | closed |
| T-11-10 | Spoofing | Reschedule note / client name | low | mitigate | Plain `Text` `RescheduleResponseCard.kt:240,266`; no link/HTML APIs | closed |
| T-11-11 | Information disclosure | Realtime event payload | low | mitigate | Payload ignored `ExpertDashboardViewModel.kt:116` | closed |
| T-11-12 | Denial of service | Burst of `booking.update` | low | mitigate | `loadJob?.cancel()` `ExpertBookingsViewModel.kt:191`, `ExpertDashboardViewModel.kt:228`; burst tests | closed |
| T-11-13 | Elevation of privilege | Join gate | low | accept | AR-11-03 | closed |
| T-11-14 | Spoofing | Client name / topic rendering | low | mitigate | `IncomingRequestCard.kt:130-138` plain `Text`, ellipsis | closed |
| T-11-15 | Tampering | Expired request still actionable | low | mitigate | `RequestDecisionViewModel.kt:152`; 409 backstop mapped; expired tests | closed |
| T-11-16 | Information disclosure | Gross shown as earnings | medium | mitigate | Net only `IncomingRequestsLogic.kt:74-88`; `IncomingRequestsLogicTest.kt:109,117` | closed |
| T-11-17 | Information disclosure | Earnings section gross as net | medium | mitigate | `RequestEarnings.kt:51-80` -> `PricePending`; `RequestEarningsSectionTest.kt:71,84,168` | closed |
| T-11-18 | Spoofing | Client message rendering | low | mitigate | `RequestDecisionBlocks.kt:167` plain `Text` in `SelectionContainer` | closed |
| T-11-19 | Tampering | Wrong-unit money | low | mitigate | 0 `potential_earnings`; `Money.formatMajor` throughout `expertmode` | closed |
| T-11-20 | Tampering | Untrusted text in calendar extras | low | mitigate | `CalendarIntent.kt:51-56` fixed URI, string extras; `CalendarIntentTest.kt:54` | closed |
| T-11-21 | Elevation of privilege | Calendar permissions | medium | mitigate | No `CALENDAR` permission in manifest; intent-only | closed |
| T-11-22 | Denial of service | No calendar app | low | mitigate | `CalendarIntent.kt:100-108` catches `ActivityNotFoundException`; `CalendarIntentTest.kt:72` | closed |
| T-11-23 | Elevation of privilege | Cancel entry during live call | medium | mitigate | `ExpertBookingSummaryScreen.kt:117`, VM `:170`; `attemptCancelGoesThroughTheCallGuard` | closed |
| T-11-24 | Tampering | `can_cancel` / join gate | low | accept | AR-11-04 | closed |
| T-11-25 | Spoofing | Preparation notes rendering | low | mitigate | `ExpertMessageCard.kt:40` plain `Text` | closed |
| T-11-26 | Information disclosure | Private note | low | mitigate | Not declared on any expert DTO | closed |
| T-11-27 | Tampering | Double-submit of cancel | high | mitigate | `ExpertCancellationViewModel.kt:156`; buttons/back disabled; `singleFlight` test; server atomic guard `expertBooking.controller.js:1091` | closed |
| T-11-28 | Elevation of privilege | Cancel during live call | medium | mitigate | `ExpertCancellationViewModel.kt:157`; `guardBlocksTheCall` | closed |
| T-11-29 | Repudiation | Misleading refund promise | medium | mitigate | `ExpertRefundPreview.kt:62`; server `refundAmount`; `ExpertCancellationScreenTest.kt:174-183` | closed |
| T-11-30 | Information disclosure | Reason/comment in logs | medium | mitigate | 0 `Log.` in cancellation; UAT test 4 passed | closed |
| T-11-31 | Denial of service | Event bursts / overlapping pulls | low | mitigate | Latest-wins `loadJob`; burst and pull tests | closed |
| T-11-32 | Information disclosure | Realtime payload / names in logs | low | mitigate | Payload ignored `ExpertBookingsViewModel.kt:105`; 0 `Log.` | closed |
| T-11-33 | Elevation of privilege | Join gate | low | accept | AR-11-03 | closed |
| T-11-34 | Repudiation | Cancel notice twice / never | low | mitigate | `consumeNotice` `ExpertBookingsViewModel.kt:165`; `cancelNoticeShowsOnce` | closed |
| T-11-35 | Spoofing | Client name on Past cards | low | mitigate | `ExpertPastCard.kt:80-98` plain text | closed |
| T-11-36 | Tampering | Double-submit accept/decline | high | mitigate | `RequestDecisionViewModel.kt:149-155`; sheets undismissable in flight; single-flight tests; server 409 | closed |
| T-11-37 | Repudiation | Stale request accepted after cancel | medium | mitigate | `RequestDecisionErrors.kt:36-39`; `alreadyProcessed*` tests | closed |
| T-11-38 | Tampering | Strict schema rejection (422) | medium | mitigate | `DeclineReason.kt:35-40,55-57`; `DeclineReasonTest` | closed |
| T-11-39 | Information disclosure | Message/note in logs | medium | mitigate | 0 `Log.` in `requestdecision` | closed |
| T-11-40 | Elevation of privilege | Reschedule response during live call | medium | mitigate | `ExpertBookingSummaryViewModel.kt:155,161`; `aLiveCallOnThisBookingBlocksBothResponses` | closed |
| T-11-41 | Tampering | Double-submit / accept-decline race | high | mitigate | `RescheduleResponse.kt:132-165`; `singleFlight`, `idempotentRepeatIsSuccess` | closed |
| T-11-42 | Tampering | Acting on expert-initiated proposal | low | mitigate | `ExpertBookingSummaryUiState.kt:134`; card gate; 403 reload | closed |
| T-11-43 | Repudiation | Optimistic display after SLOT_UNAVAILABLE | medium | mitigate | `ExpertRescheduleErrors.kt`; `RescheduleResponse.kt:196-200`; slot-unavailable test | closed |
| T-11-44 | Information disclosure | Onboarding URL (single-use) | high | mitigate | Memory-only `ExpertDashboardUiState.kt:30,44`; read-once `consumeOnboardingUrl()`; test `ExpertDashboardViewModelTest.kt:384-385` | closed |
| T-11-45 | Tampering | Opening server-supplied URL | high | mitigate | https + host check `DashboardFinancials.kt:58-64`; `CustomTabLauncher.kt:20-24,38-41`; `DashboardFinancialsTest.kt:70-96` | closed |
| T-11-46 | Denial of service | Repeat taps minting links | medium | mitigate | `ExpertDashboardViewModel.kt:187-189`; `repeatTapsWhileMintingAreIgnored` | closed |
| T-11-47 | Elevation of privilege | Payout setup during live call | medium | mitigate | `ExpertDashboardViewModel.kt:190`; `guardBlocksMint` | closed |
| T-11-48 | Repudiation | Misleading payout state | low | mitigate | `DashboardFinancials.kt:36-52`; truth-table tests | closed |
| T-11-49 | Denial of service | R8 stripping serializers/adapters | medium | mitigate | `app/build.gradle.kts:78-85` packageScope excludes app classes; release APK + mapping built | closed |
| T-11-50 | Information disclosure | Logging/persisting expert or client data | high | mitigate | 0 `Log.`, 0 `DataStore`/`SharedPreferences` in `expertmode` | closed |
| T-11-51 | Elevation of privilege | Guard regressions | medium | mitigate | 3 `callGuard.attempt(` + 4 `attemptModifyBooking(` call sites confirmed | closed |
| T-11-52 | Repudiation | Unverified cross-client behaviour | low | accept | AR-11-05 | closed |
| T-11-53 | Information disclosure | AgoraManager failure logs | high | mitigate | `describeEngineFailure` `AgoraEngineConfig.kt:72-76`; no token/channel/uid in logs; `AgoraManagerCreateEngineTest.kt:38-45` | closed |
| T-11-54 | Denial of service | Engine creation failing (G-11-1) | high | mitigate | `createEngine()` uses captured app context; explicit null-create failure; test through `join()`; UAT test 7 | closed |
| T-11-55 | Elevation of privilege | Internal factory constructor | low | accept | AR-11-06 | closed |
| T-11-56 | Repudiation | Fix shipped without device proof | low | accept | AR-11-07 (superseded by UAT test 7) | closed |
| T-11-57 | Information disclosure | New handler log lines | high | mitigate | Codes/elapsed only; `AgoraManagerCreateEngineTest.kt:94-140` | closed |
| T-11-58 | Denial of service | Android/iOS peers never meet (G-11-5) | high | mitigate | `mChannelProfile = COMMUNICATION` `AgoraEngineConfig.kt:36-43`; tests; UAT test 7 | closed |
| T-11-59 | Denial of service | Native engine leak after setup throws | medium | mitigate | `createEngine()` destroys on failure; `setupFailureAfterCreationDestroysEngineAndAllowsRetry` | closed |
| T-11-60 | Tampering | `token` shadowing in options scope | low | mitigate | Explicit assignments, no scope function; `AgoraEngineConfigTest.kt:46` | closed |
| T-11-61 | Repudiation | Fix ships at ~65% diagnosis | medium | accept | AR-11-07 (superseded by UAT test 7) | closed |
| T-11-62 | Tampering | Join channel/token from raw `req.params.id` | high | mitigate | `BE/src/controllers/call.controller.js:50-58,81,100,111,116`; `agora.js` lowercases; `BE/tests/call.test.js:821-870` | closed |
| T-11-63 | Elevation of privilege | Participant/window gates before token | high | mitigate | Order unchanged `call.controller.js:64-73`; test (e) 403 and no token minted | closed |
| T-11-64 | Information disclosure | Response leaking Agora credentials | high | mitigate | Response shape unchanged `call.controller.js:113-125`; `call.test.js:241-242` | closed |
| T-11-65 | Tampering / Repudiation | Executor pushes/deploys backend | high | mitigate | Push and deploy were user actions (`11-18-SUMMARY.md`); verified via public `/health` only | closed |
| T-11-66 | Tampering | Unrelated backend files / history rewrite | medium | mitigate | `git diff f686d27 4c694e7 --name-only` = the 6 planned files; linear history | closed |
| T-11-67 | Information disclosure | Device evidence / prod credentials | medium | mitigate | `.planning/debug/evidence/` untracked; deploy check used public `/health` only | closed |
| T-11-68 | Denial of service | Channel changes mid-call on deploy | low | accept | AR-11-08 | closed |
| T-11-69 | Repudiation | Fix ships on 85-90% diagnosis | medium | mitigate | Two-sided device re-test passed (11-UAT test 7: `remote joined elapsed=148`, `remote audio state=2`) | closed |
| T-11-SC | Tampering | Package installs (templated) | low | accept | AR-11-09 | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-11-01 | T-11-05 | `isExpired` is a UI gate; the server's atomic `pending_approval` check returns 409 `ALREADY_PROCESSED` (`bookingRequests.controller.js:262`) | plan 11-01 threat model | 2026-10-27 |
| AR-11-02 | T-11-06 | `BookingConfirmedKey` saved state is app-private and process-scoped; holds only on-screen data, no tokens or payment data | plan threat model | 2026-10-27 |
| AR-11-03 | T-11-13, T-11-33 | Join window is a UX gate; `POST /join` enforces participant then window server-side (`call.controller.js:64-73`) | plan threat model | 2026-10-27 |
| AR-11-04 | T-11-24 | `can_cancel` is a UI gate; server `CANCELLABLE_STATUSES` check and atomic update are authoritative | plan threat model | 2026-10-27 |
| AR-11-05 | T-11-52 | Cross-client device checks tracked in STATE; later exercised in UAT tests 2, 3 and 7 | plan threat model | 2026-10-27 |
| AR-11-06 | T-11-55 | Internal constructor limited to `:app`; Hilt binds only the `@Inject` constructor | plan 11-16 threat model | 2026-10-27 |
| AR-11-07 | T-11-56, T-11-61 | Process risk; superseded by the passed two-sided device re-test (11-UAT test 7) | plans 11-16, 11-17 | 2026-10-27 |
| AR-11-08 | T-11-68 | No working Android/iOS call existed before; same-platform casing agrees; a re-join converges | plan 11-18 threat model | 2026-10-27 |
| AR-11-09 | T-11-SC | No new runtime dependencies in Phase 11; only a test-scope `mockito-core 5.24.0` (review fix `ff49ad4`) | plan threat models | 2026-10-27 |

*Accepted risks do not resurface in future audit runs.*

---

## Advisories (non-blocking)

1. `mockito-core` (testImplementation) was added after the plans by review fix `ff49ad4`; documented in 11-REVIEW-FIX.md. Not in the app.
2. Review WR-01 / IN-02 remain open per 11-REVIEW-DISPOSITION.md: a minted onboarding URL can survive screen navigation (stale single-use link, ~5 min expiry), and the URL check pins https + host rather than `*.stripe.com`. Optional hardening.
3. Review IN-06: `launchCalendarInsert` catches only `ActivityNotFoundException`, not `SecurityException` (resilience).
4. T-11-21 has no persistent test asserting the manifest lacks calendar permissions.
5. `.planning/debug/evidence/` is untracked but not git-ignored (accidental-commit risk).
6. Signed-release device smoke of R8 still deferred (Phase 2 STATE item).

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-27 | 70 | 70 | 0 | gsd-security-auditor (ASVS L1, block_on high) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-27
