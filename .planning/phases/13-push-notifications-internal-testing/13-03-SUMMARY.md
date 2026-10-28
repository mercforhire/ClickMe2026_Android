---
phase: 13-push-notifications-internal-testing
plan: 03
subsystem: api
tags: [retrofit, moshi, notifications, reviews, device-tokens, mockwebserver, wave-0]

requires:
  - phase: 12-chat-and-reporting
    provides: ClickMeApi/FakeClickMeApi conventions, strict-body request KDoc pattern, ChatsApiTest harness
provides:
  - Five Phase 13 endpoints on ClickMeApi (register/delete device token, list notifications, review context, submit review)
  - NotificationsRepository (page 1, limit 50) and ReviewsRepository (context, submit with trimmed/omitted comment)
  - FakeClickMeApi handlers and recorders for all five endpoints, NotificationFixtures, ReviewFixtures
affects: [13-push-notifications-internal-testing, push token registration, notifications list screen, review screen]

actuals:
  tokens: 11000
  tasks: 3
  commits: 5
plan_head_before: 1c9c187606c03a3bdbc76c63f8635978b6b8849d
plan_head_after: 91f44e68bd2e2ae657cb6a30720df534e96a118e

tech-stack:
  added: []
  patterns:
    - "Strict zod body DTOs: schema keys only, nullable fields default to null and are omitted by the app Moshi"
    - "Redacting toString on every DTO that carries a push token, notification text or review comment"
    - "Lenient string category on NotificationItem so an unknown server category never fails decoding"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/PushRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ReviewRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/NotificationPayloads.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ReviewPayloads.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/reviews/ReviewsRepository.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/NotificationsApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/PushReviewApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/NotificationFixtures.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ReviewFixtures.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsRepositoryTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/reviews/ReviewsRepositoryTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt

key-decisions:
  - "NotificationItem.category stays a String (iOS uses an enum) so unknown categories decode and still open the list (D-10)"
  - "ReviewRequests.kt keeps the plan's filename with a file-level ktlint filename suppression, matching existing precedent in the repo"
  - "FakeClickMeApi non-token handlers default to not-stubbed; token handlers default to a success response"

patterns-established:
  - "Wave 0 network plan: later Phase 13 plans never edit ApiRequestUrls, ClickMeApi, FakeClickMeApi or the two fixture files"

requirements-completed: [PUSH-01, PUSH-05, REV-01]

coverage:
  - id: D1
    description: "GET notifications sends page=1 and limit=50 and decodes rows with free-form meta_data, nullable sub_category, unknown category and missing pagination"
    requirement: PUSH-05
    verification:
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/network/NotificationsApiTest.kt (4 tests)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Device token register sends exactly device_id, token, platform=android and optional app_version (never null); delete carries the device id in the path"
    requirement: PUSH-01
    verification:
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/network/PushReviewApiTest.kt#registerSendsOnlySchemaKeys"
        status: pass
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/network/PushReviewApiTest.kt#deleteUsesDeviceIdPath"
        status: pass
    human_judgment: false
  - id: D3
    description: "Review context decodes with and without prefill; submit sends trimmed comment or none; 404 and 422 reach ApiException.Http with the server message"
    requirement: REV-01
    verification:
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/network/PushReviewApiTest.kt (6 tests)"
        status: pass
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/reviews/ReviewsRepositoryTest.kt (4 tests)"
        status: pass
    human_judgment: false
  - id: D4
    description: "No token, notification text or review comment is printed by toString on the new DTOs"
    verification:
      - kind: unit
        ref: "NotificationsApiTest#toStringHidesTitleAndBody, PushReviewApiTest#toStringHidesTokenAndComment"
        status: pass
    human_judgment: false
  - id: D5
    description: "FakeClickMeApi scripts and records all five endpoints; NotificationFixtures and ReviewFixtures exist for later plans"
    verification:
      - kind: unit
        ref: "NotificationsRepositoryTest, ReviewsRepositoryTest#fakeRecordsTokenCalls"
        status: pass
    human_judgment: false

duration: 70min
completed: 2026-10-28
status: complete
---

# Phase 13 Plan 03: Push, Notifications and Reviews Network Contract Summary

**Retrofit/Moshi contract for device-token register and delete, the first-50 notifications list, and review context and submit, with strict-schema bodies, redacting toString, stateless repositories, MockWebServer wire tests and shared FakeClickMeApi/fixture seams**

## Performance

- **Duration:** about 70 min (most of it Gradle runs; one run hit a Kotlin daemon OOM and was re-run with a 3g heap)
- **Completed:** 2026-10-28T15:54Z
- **Tasks:** 3
- **Files modified:** 15 (12 created, 3 modified)

## Accomplishments
- Five endpoint constants and Retrofit methods; the tracer slice (notifications list through repository) was proven end to end by MockWebServer before the rest was built.
- Request bodies match the backend `.strict()` zod schemas: `app_version` and blank review comments are omitted rather than sent as null, `platform` is always `android`, no `tags` key.
- Review errors (404 unknown booking, 422 validation) surface as `ApiException.Http` with the server message.
- Wave 0 seams: `FakeClickMeApi` handlers and recorders for all five endpoints, `NotificationFixtures`, `ReviewFixtures`, so later plans edit none of the shared network or fixture files.

## Task Commits

1. **Task 1 (tracer): notifications list round-trip** - `24d0a70` (test, RED), `dc15c05` (feat)
2. **Task 2: token and review endpoints with strict bodies** - `54896c9` (test, RED), `1131560` (feat)
3. **Task 3: fixtures and repository tests** - `91f44e6` (test)

**Plan metadata:** committed with this SUMMARY (docs: complete plan).

## Files Created/Modified
See `key-files` above. Notable: `ClickMeApi.kt` and `ApiRequestUrls.kt` gained a Phase 13 group; `FakeClickMeApi.kt` gained five handlers, four recorders plus the notifications recorder, and the five overrides.

## Decisions Made
- `NotificationItem.category` is a plain String so an unknown category never fails decoding.
- `ReviewRequests.kt` holds a single class, so it carries `@file:Suppress("ktlint:standard:filename")` (same precedent as `BookingActionResponses.kt`) rather than renaming away from the plan's path.
- The `getNotifications` fake handler and recorder were added in Task 1 (not Task 3) because adding the method to `ClickMeApi` breaks compilation of `FakeClickMeApi`, its only implementer.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] FakeClickMeApi overrides added earlier than planned**
- **Found during:** Task 1
- **Issue:** Adding `getNotifications` to `ClickMeApi` stops `FakeClickMeApi` compiling. The plan allowed this for Task 1 only if needed.
- **Fix:** Added the notifications handler, recorder and override in Task 1 and the other four endpoints in Task 2 (their API methods land there). Task 3 then added fixtures and repository tests.
- **Files modified:** `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt`
- **Committed in:** `dc15c05`, `1131560`

**2. [Rule 3 - Blocking] ktlint filename rule on single-class ReviewRequests.kt**
- **Found during:** Task 3 verify (ktlintCheck)
- **Issue:** ktlint wanted `SubmitReviewRequest.kt`; the plan fixes the file name as `ReviewRequests.kt`.
- **Fix:** File-level `ktlint:standard:filename` suppression.
- **Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ReviewRequests.kt`
- **Verification:** ktlintCheck passes.
- **Committed in:** `1131560`

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** No scope change; both were ordering and lint mechanics.

## Issues Encountered
- The first RED Gradle run printed a Kotlin daemon `OutOfMemoryError: GC overhead limit exceeded` alongside the expected unresolved-reference errors; subsequent runs used `-Pkotlin.daemon.jvmargs=-Xmx3g` and were clean.
- The plan-level `<verification>` ran green: the five-class test command with `ktlintCheck` exited 0 (NotificationsApiTest 4, PushReviewApiTest 6, NotificationsRepositoryTest 1, ReviewsRepositoryTest 4, ChatsRepositoryTest 5, no failures) and `scripts/check-design-rules.sh` printed "Design rules: OK".

## Known Stubs
None. The FakeClickMeApi handlers that default to "not stubbed" are the existing test-seam convention, not product stubs.

## User Setup Required
None - no external service configuration required.

## Open Items (human or device)
None for this plan. Real-device push delivery and token registration are covered by later Phase 13 plans.

## Threat Flags
None beyond the plan's threat model. T-13-07 (token, notification text and comment redaction) and T-13-08 (strict bodies) are mitigated and tested; T-13-09 (booking-id IDOR) is accepted, the server enforces participation.

## Next Phase Readiness
- Token manager, notifications screen and review screen plans can depend on `ClickMeApi`, the two repositories and the fakes and fixtures without editing shared files.

## Self-Check: PASSED

- All 12 created files and 3 modified files exist (verified by acceptance greps and the green test run).
- Commits `24d0a70`, `dc15c05`, `54896c9`, `1131560`, `91f44e6` are ancestors of HEAD (`git rev-list --count` from the plan base measured 5).

---
*Phase: 13-push-notifications-internal-testing*
*Completed: 2026-10-28*
