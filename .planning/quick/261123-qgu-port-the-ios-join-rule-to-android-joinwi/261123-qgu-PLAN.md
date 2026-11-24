---
phase: quick-261123-qgu
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingManagementFixtures.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/TodaysSessionsSection.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ExpertBookingFixtures.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertBookingsApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
autonomous: true
requirements: [CALL-01, CALL-08, EXP-08]

estimate:
  tokens: 80000
  raw_tokens: 80000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "isWithinJoinWindow enables Join only for confirmed, in_progress and completed (backend JOINABLE_BOOKING_STATUSES at 1d18aac, iOS JoinWindow.joinableStatuses). pending_reschedule, pending_approval, cancelled, declined, missed, expired, an unknown status and the empty string are false at every instant, even with serverCanJoin = true"
    - "For an enabled status Join opens at exactly start - 3600 s (true at start - 3600, false at start - 3601) and stays enabled through max(end, start + 3600) inclusive whatever serverCanJoin is (true, false or null)"
    - "Past max(end, start + 3600), Join stays enabled through end + 3600 inclusive only when serverCanJoin == true. null and false both give false, and end + 3601 is false even with true"
    - "completed follows the same two tiers as confirmed and in_progress. It is no longer enabled before start - 3600, and it is no longer cut off at end when the first-join edge or a server-approved reconnect is still open"
    - "pending_reschedule bookings still render their Join on every surface that showed it before (Explore and expert-dashboard Today's Sessions strips, My Bookings card, Upcoming Booking Details card, expert Upcoming card, expert Booking Summary), now disabled. JOINABLE_BOOKING_STATUSES in TodaysSessions.kt and its membership uses (strips, CalendarIntent, ExpertBookingBuckets, showsJoinCard, ExpertBookingSummaryUiState.showsJoin) are unchanged"
    - "Client Upcoming Booking Details passes the GET /client/bookings/{id} can_join. The expert Upcoming card, expert dashboard Today's Sessions and expert Booking Summary pass actions.can_join. Explore Today's Sessions and My Bookings pass null because their payloads carry no flag. serverCanJoin has no default, so every caller must choose a value"
    - "The JoinWindow KDoc records the iOS-parity change and its sources (iOS JoinWindow.swift, iOS plans 02-04 and 02-14, backend evaluateJoinWindow and JOINABLE_BOOKING_STATUSES at 1d18aac). It no longer says the server flag is ignored or that POST /join never checks status"
    - "The full :app:testDevDebugUnitTest run, ktlintCheck and lintDevDebug all pass"
  artifacts:
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt"
      provides: "JOIN_ENABLED_BOOKING_STATUSES, the lead, first-join span and reconnect-grace constants, and isWithinJoinWindow(status, start, end, now, serverCanJoin)"
      contains: "JOIN_ENABLED_BOOKING_STATUSES"
    - path: "app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt"
      provides: "edge tests one second either side of every bound, plus the status and server-flag tables"
      contains: "fun reconnect_isClosedWhenServerCanJoinIsFalseOrNull("
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt"
      provides: "ExpertBookingActionsDto.canJoin decoded from actions.can_join"
      contains: "@Json(name = \"can_join\") val canJoin: Boolean = false"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt"
      provides: "UpcomingBookingDetails.canJoin mapped from ClientBookingDetail.canJoin"
      contains: "canJoin = canJoin"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt"
      provides: "ExpertSummaryDetails.canJoin mapped from actions.canJoin"
      contains: "canJoin = actions.canJoin"
  key_links:
    - from: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt"
      to: "isWithinJoinWindow"
      via: "joinEnabled of UpcomingBookingJoinCard"
      pattern: "serverCanJoin = details\\.canJoin"
    - from: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt"
      to: "isWithinJoinWindow"
      via: "JoinCallButton enabled on the expert Booking Summary"
      pattern: "serverCanJoin = details\\.canJoin"
    - from: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt"
      to: "isWithinJoinWindow"
      via: "joinEnabled on each expert Upcoming card"
      pattern: "serverCanJoin = item\\.actions\\.canJoin"
    - from: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/TodaysSessionsSection.kt"
      to: "isWithinJoinWindow"
      via: "JoinCallButton enabled on the expert dashboard Today's Sessions card"
      pattern: "serverCanJoin = item\\.actions\\.canJoin"
    - from: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt"
      to: "isWithinJoinWindow"
      via: "JoinCallButton enabled on the Explore Today's Sessions card (no flag in the home feed)"
      pattern: "serverCanJoin = null"
    - from: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt"
      to: "isWithinJoinWindow"
      via: "joinEnabled on each My Bookings Upcoming card (no flag in GET /client/bookings)"
      pattern: "serverCanJoin = null"
---

<objective>
Port the iOS join rule (iOS `JoinWindow.isOpen`, iOS plans 02-04 and 02-14) to Android `isWithinJoinWindow`. This function decides whether Join is enabled, never whether it is shown. The rule must match the production backend at SHA 1d18aac:

- (a) Only confirmed, in_progress and completed can be enabled. pending_reschedule is shown disabled, not hidden (iOS 02-14, user decision 2026-11-10).
- (b) Join opens 60 minutes before start. A first join stays possible through max(end, start + 60 min), inclusive.
- (c) After that edge, a reconnect stays possible through end + 60 min, but only when the server's can_join is true. null and false both close it.

Purpose: a live Android user can rejoin a dropped call after the scheduled end when the server allows it, the way iOS users can, and is never offered a Join that the server would refuse with 403 SESSION_NOT_ACTIVE (pending_reschedule, completed before the window). Calls come first. The server stays the final authority on POST /bookings/:id/join.

Output: the new rule and constants in JoinWindow.kt with edge tests, can_join threaded from the client booking detail and the expert actions block to all six call sites, the KDoc rewritten with its sources, and a green full unit-test, ktlint and lint gate. All commits are local on main and are not pushed. Android only: the iOS and backend repos are read-only references.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md
@app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt
@app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt

Cross-repo references (read-only; never edit or commit in these repos):
- iOS rule: /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Utils/JoinWindow.swift (`isOpen(status:start:end:now:serverCanJoin:)`, `joinableStatuses`, `lead`, `minFirstJoinSpan`, `reconnectGrace`)
- iOS follow-up record: /Users/LeonChen/Documents/ClickMe2026/.planning/phases/02-shared-ios-plumbing/02-04-SUMMARY.md lines 141-143 ("Android Follow-up (logged, not built)") and 02-14-SUMMARY.md in the same folder
- Backend at production SHA 1d18aac, read with `git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" show 1d18aac:<path>`: `src/services/agora.js` (`evaluateJoinWindow`, `JOIN_LEAD_MS`, `FIRST_JOIN_MIN_SPAN_MS`, `JOIN_GRACE_MS`), `src/utils/bookingStatusSets.js` (`JOINABLE_BOOKING_STATUSES`), `src/controllers/call.controller.js` line 94 (the join gate now rejects non-joinable statuses with 403 SESSION_NOT_ACTIVE), `src/controllers/clientBooking.controller.js` getDetail line 1132 (`can_join`), and `src/controllers/expertBooking.controller.js` getFeed line 323 and getDetails line 436 (`actions.can_join`)

<interfaces>
Facts read at planning time, so the executor does not need to explore:

Backend at 1d18aac, already confirmed:
- `JOINABLE_BOOKING_STATUSES = ['confirmed', 'in_progress', 'completed']`, documented as "`pending_reschedule` is deliberately NOT joinable".
- `evaluateJoinWindow`: earliest = start - 60 min; latest = alreadyJoined ? end + 60 min : max(end, start + 60 min); allowed when earliest <= now <= latest, both bounds inclusive.
- `can_join` = status in JOINABLE_BOOKING_STATUSES && evaluateJoinWindow(alreadyJoined = this party's own joined_at != null).
- Payloads that carry the flag: client GET /client/bookings/{id} (top-level `can_join`); expert GET /expert/bookings (each row's `actions.can_join`); expert GET /expert/bookings/{id}/details (`actions.can_join`).
- Payloads without it: the client bookings list and the client home feed.

iOS `JoinWindow.isOpen`:
1. If the status is not joinable, false.
2. If now < start - lead, false.
3. If now <= max(end, start + minFirstJoinSpan), true.
4. Otherwise, true only when serverCanJoin == true and now <= end + reconnectGrace.

The first-join tier ignores the flag. iOS passes `serverCanJoin` from the client detail and from expert `actions.canJoin` on the expert Upcoming list (UpcomingBookingsViewModel.swift:298), the Booking Summary (ExpertBookingSummaryView.swift:301 and 308) and the dashboard (ExpertDashboardView.swift:493). Everywhere else it passes nil.

Android today:
- `fun isWithinJoinWindow(status: String, start: Instant, end: Instant, now: Instant): Boolean` at JoinWindow.kt:51. JOINABLE_BOOKING_STATUSES uses the live window and completed is enabled while end is after now. The rest of the file:
  - `showsJoinCard(status, end, now)` at :68 stays unchanged.
  - `isInLiveWindow` at :75 is private.
  - `isStartingNow` and `JOIN_WINDOW_SECONDS` (3600) drive the Starting-now badge. `JOIN_WINDOW_SECONDS` is used elsewhere only by JoinWindowTest.
- `val JOINABLE_BOOKING_STATUSES: Set<String> = setOf("confirmed", "in_progress", "pending_reschedule")` at screens/clientmode/mybookings/TodaysSessions.kt:13. It is a membership set and MUST NOT change.
- Status constants in screens/clientmode/mybookings/BookingStatusDisplay.kt:5-9: STATUS_PENDING_APPROVAL, STATUS_PENDING_RESCHEDULE, STATUS_COMPLETED, STATUS_CONFIRMED, STATUS_IN_PROGRESS.
- `ClientBookingDetail.canJoin: Boolean = false` (`@Json(name = "can_join")`, model/responses/ClientBookingDetail.kt:29). It is declared but read nowhere today. ResponseDecodingTest:869 asserts it decodes true. Keep it non-null: false and null close the reconnect tier the same way.
- `data class ExpertBookingActionsDto(canCancel: Boolean = false, canReschedule: Boolean = false)` at model/responses/ExpertBookingsPayload.kt:139. It has no can_join. It is used as `actions` on ExpertBookingItem (:33) and ExpertBookingDetail (:100). Stale KDoc sits at :22 (lists only can_reschedule), :86-88 (says the join flag is not declared because the window is computed on the device) and :133-136.
- `fun ClientBookingDetail.toUpcomingDetails(now: Instant): UpcomingBookingDetails` at UpcomingBookingUiState.kt:102, with the data class at :37.
- `fun ExpertBookingDetail.toSummaryDetails(now: Instant, locale: Locale): ExpertSummaryDetails` at ExpertBookingSummaryUiState.kt:118, with the data class at :55. Its `showsJoin` at :140 MUST NOT change.
- The six call sites (positional arguments today):
  - ExploreTodaysSessionsSection.kt:157 `session` (ClientBookingItem, no flag)
  - UpcomingBookingScreen.kt:465 `details` (UpcomingBookingDetails)
  - MyBookingsScreen.kt:354 `item` (ClientBookingItem, no flag)
  - expert dashboard TodaysSessionsSection.kt:178 `item` (ExpertBookingItem from GET /expert/bookings)
  - ExpertBookingSummaryScreen.kt:429 `details` (ExpertSummaryDetails)
  - ExpertUpcomingCard.kt:204 `item` (ExpertBookingItem)
- components/call/JoinCallButton.kt only mentions [isWithinJoinWindow] in its KDoc. That text stays accurate, so no change is needed there.
- Both detail ViewModels reload the booking on a realtime `booking.update` for their id (UpcomingBookingViewModel.kt:137, ExpertBookingSummaryViewModel.kt:141). They never patch the status in place, so a threaded can_join is always the latest server value. iOS clears its cached flag on an in-place patch; Android has no equivalent path and needs none.
- Test fixtures: `bookingDetail(...)` in app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingManagementFixtures.kt:53. `expertBookingItem(...)` (:47) and `expertBookingDetail(...)` (:81) in app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ExpertBookingFixtures.kt. None of them has a canJoin parameter.
- UpcomingBookingScreenTest has a private `details(...)` builder (around :294) that constructs UpcomingBookingDetails directly, with `detailsStart` = 2026-10-24T18:00:00Z and end defaulting to start + 3600. `setScreen(state, joinNow = ...)` and `hasJoinHint()` exist.
- ExpertBookingSummaryViewModelTest `canRescheduleFollowsTheServerFlagOnly` (:107) is the pattern for a server-flag mapping test.
- ExpertBookingsApiTest already serves `"actions":{"can_join":false,...}` JSON at :70, :93, :106, :122, :165 and :173 but asserts nothing about it.
</interfaces>

Build environment:
- Set `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` and run every command from the checkout root. In a worktree, first copy /Users/LeonChen/Documents/ClickMe_Android/local.properties to the worktree root, and never commit it.
- Never run `./gradlew --stop`. The test task needs `-Pkotlin.daemon.jvmargs=-Xmx3g`, and non-test tasks such as lintDevDebug also need `-Dorg.gradle.jvmargs=-Xmx4g` (STATE Phase 12 note).
- Style: ktlint_official with a 120-column limit. A function or call with two or more parameters puts one parameter per line. Pass `serverCanJoin` as a named argument at every call site so the key-link greps can find it.
- Count gate: Task 3 counts `serverCanJoin = ` assignments under app/src/main and expects exactly 6, one per call site. In KDoc and comments, refer to the parameter as [serverCanJoin] or in prose; never write the parameter name followed by a space, an equals sign and a value.
- Commits: local commits on main with the subject prefix `fix(quick-261123-qgu):` or `test(quick-261123-qgu):`. Do not push.
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: New join rule plus the client Upcoming Booking Details reconnect, end to end</name>
  <files>app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModelTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingManagementFixtures.kt</files>
  <precondition>The backend at 1d18aac still defines the joinable set as confirmed, in_progress, completed: `git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" show 1d18aac:src/utils/bookingStatusSets.js | grep -qF "JOINABLE_BOOKING_STATUSES = Object.freeze(['confirmed', 'in_progress', 'completed'])"` exits 0.</precondition>
  <behavior>
    - The enabled set is exactly confirmed, in_progress and completed (JOIN_ENABLED_BOOKING_STATUSES), and JOINABLE_BOOKING_STATUSES still equals confirmed, in_progress, pending_reschedule.
    - pending_reschedule is false at start - 600, at start and at end, with serverCanJoin true, false and null.
    - pending_approval, cancelled, declined, missed, expired, "something_new" and "" are false at start, with serverCanJoin true.
    - For each of confirmed, in_progress and completed: start - 3600 is true and start - 3601 is false, both with serverCanJoin null. start - 3601 with serverCanJoin true is still false (the flag never opens before the lead).
    - Short session (end = start + 1800): start + 3600 is true with null, and start + 3601 is false with null.
    - Long session (end = start + 7200): end is true with null, and end + 1 is false with null.
    - The first-join tier ignores the flag: at start with serverCanJoin false, the result is true.
    - Reconnect tier (end = start + 3600, so the first-join edge is end): end + 1800 and end + 3600 are true with serverCanJoin true, end + 3601 is false with true, and end + 1800 is false with false and with null.
    - completed with start 10 h in the future is false (it no longer ignores the start window). completed at end + 1800 is true with serverCanJoin true and false with null.
    - showsJoinCardTruthTable and isStartingNow_isSymmetricAndInclusive are unchanged and still pass.
    - toUpcomingDetails copies ClientBookingDetail.canJoin: true stays true and the fixture default gives false.
    - Screen: pending_reschedule at joinNow = detailsStart renders the Join card disabled, with the join hint.
    - Screen: in_progress with canJoin = true at joinNow = detailsStart + 5400 (end + 30 min) has an enabled Join.
    - Screen: the same booking with canJoin = false is disabled and shows the join hint.
  </behavior>
  <action>
RED first: write the JoinWindowTest cases from the behavior list, the mapper test and the three screen tests, and confirm they fail or do not compile. Then make them pass.

JoinWindowTest (rework the file, keep its existing package and style):
- Rename constants_matchDecisions to constants_matchTheBackend. It asserts the three new constants equal 3600 and JOIN_CLOCK_TICK_MS equals 30000. It keeps asserting JOIN_WINDOW_SECONDS equals 3600 (the badge constant).
- Add tests with these exact names:
  - enabledStatuses_equalTheBackendJoinableSet (also asserts JOINABLE_BOOKING_STATUSES is unchanged)
  - pendingReschedule_isNeverEnabled
  - opensExactlyOneHourBeforeStart
  - serverCanJoin_neverOpensBeforeTheLead
  - firstJoin_closesAtTheLaterOfEndAndStartPlusOneHour
  - firstJoinTier_ignoresTheServerFlag
  - reconnect_withServerCanJoinTrue_runsThroughEndPlusOneHour
  - reconnect_isClosedWhenServerCanJoinIsFalseOrNull
  - completed_followsTheSameTiers
  - terminalAndUnknownStatusesAreNeverEnabled
- Delete the tests that encode the old rule: inProgressAndPendingReschedule_followTheSameWindow, completed_isJoinableWhileEndIsInTheFuture and completed_ignoresTheStartWindow.
- Rework the WR-09 group (longSession_staysJoinableUntilItsEnd, shortSession_stillGetsTheFullHourAfterStart, longSession_opensNoEarlierThanAnHourBeforeStart, pendingApproval_isNeverJoinableEvenForALongSession) so it loops over the enabled statuses with serverCanJoin = null, and so pending_reschedule is asserted false.
- Keep showsJoinCardTruthTable and isStartingNow_isSymmetricAndInclusive byte for byte.
- Update the private `within` helper to take a serverCanJoin argument defaulting to null. This is a test helper only; the production function gets no permanent default.

JoinWindow.kt:
- Add `val JOIN_ENABLED_BOOKING_STATUSES: Set<String>` built from STATUS_CONFIRMED, STATUS_IN_PROGRESS and STATUS_COMPLETED (imported from screens.clientmode.mybookings). Its KDoc says three things:
  - it equals the backend JOINABLE_BOOKING_STATUSES at 1d18aac (src/utils/bookingStatusSets.js) and iOS JoinWindow.joinableStatuses;
  - it is deliberately separate from the Android membership set JOINABLE_BOOKING_STATUSES in TodaysSessions.kt. That set has the same name as the backend set but different members: it keeps pending_reschedule so its strips and cards still render a disabled Join (iOS 02-14, user decision 2026-11-10);
  - it decides enabling only.
- Add JOIN_LEAD_SECONDS, JOIN_MIN_FIRST_SPAN_SECONDS and JOIN_RECONNECT_GRACE_SECONDS, each 3600L. Each KDoc names its backend counterpart (JOIN_LEAD_MS, FIRST_JOIN_MIN_SPAN_MS, JOIN_GRACE_MS) and its iOS counterpart (lead, minFirstJoinSpan, reconnectGrace).
- Re-document JOIN_WINDOW_SECONDS as the Starting-now badge half-width, used only by isStartingNow. Keep its value and its name.
- Give isWithinJoinWindow a fifth parameter, `serverCanJoin` of type nullable Boolean, placed last, with a null default value. The default exists only so the five call sites this task does not touch keep compiling between commits. Task 2 and Task 3 of this plan pass the flag at those sites, and Task 3 deletes the default.
- The new body follows the iOS isOpen order:
  1. Return false unless the status is in JOIN_ENABLED_BOOKING_STATUSES.
  2. Return false when now is before start minus the lead.
  3. Return true when now is not after the later of end and start plus the first-join span.
  4. Otherwise return true only when serverCanJoin == true and now is not after end plus the reconnect grace.
- Remove the STATUS_COMPLETED special branch and the private isInLiveWindow helper. Leave showsJoinCard and isStartingNow untouched.
- Rewrite the isWithinJoinWindow KDoc. Delete the WR-09 deviation paragraph, the paragraph beginning "The server's join flag (the booking detail DTO field)" and the Phase 16.5 paragraph's claim that POST /bookings/:id/join does not check status. The new KDoc states:
  - the three tiers above;
  - that every edge is inclusive;
  - that serverCanJoin is the payload's can_join when it carries one and null otherwise, and that null and false both close the reconnect tier;
  - the sources: iOS ClickMe2026/Utils/JoinWindow.swift (iOS plans 02-04 and 02-14), and backend evaluateJoinWindow (src/services/agora.js) and JOINABLE_BOOKING_STATUSES (src/utils/bookingStatusSets.js) at 1d18aac;
  - that this replaces the Phase 9 D-02 rule. That rule ignored the flag and enabled pending_reschedule, and enabled completed until end only;
  - that the old WR-09 deviation is gone because iOS and the backend now close the first join at max(end, start + 60 min) too;
  - that the result is a display gate and never an authorization. The server keeps the final say on POST /bookings/:id/join, which since 1d18aac also rejects non-joinable statuses with 403 SESSION_NOT_ACTIVE (call.controller.js);
  - that it fails closed for terminal, unknown and empty statuses.

Client path:
- In UpcomingBookingUiState.kt, add `canJoin: Boolean` (no default) to UpcomingBookingDetails, with an @property line. The line says it is the GET /client/bookings/{id} can_join, it only opens the reconnect tier, and it is always fresh because the ViewModel reloads on booking.update. Map it in toUpcomingDetails with `canJoin = canJoin`.
- In UpcomingBookingScreen.kt, pass `serverCanJoin = details.canJoin` as a named argument in the joinEnabled call, one argument per line. Leave the showsJoinCard call unchanged.
- Add a `canJoin: Boolean = false` parameter to the UpcomingBookingScreenTest `details(...)` builder and to `bookingDetail(...)` in BookingManagementFixtures.kt, and pass it through.
- Add the mapper test toUpcomingDetailsCarriesTheServerJoinFlag to UpcomingBookingViewModelTest.
- Add three screen tests to UpcomingBookingScreenTest:
  - pendingRescheduleShowsADisabledJoinInsideTheWindow
  - pastTheEndJoinStaysEnabledWhenTheServerAllowsAReconnect
  - pastTheEndJoinIsDisabledWithoutTheServerFlag

If an existing client test asserted the old behavior, update its expectation to the new rule, never delete it, and list it in the SUMMARY. The old behavior here means pending_reschedule enabled, or completed enabled before start - 3600 or after the first-join edge without the flag. Do not touch JOINABLE_BOOKING_STATUSES, showsJoinCard, CalendarIntent, the Today's Sessions filters or the bucket logic.
  </action>
  <verify>
    <automated>JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests 'com.uptrendinvestments.clickmeapp.managers.JoinWindowTest' --tests 'com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.*' --tests 'com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.*' --tests 'com.uptrendinvestments.clickmeapp.screens.clientmode.explore.*' --tests 'com.uptrendinvestments.clickmeapp.screens.expertmode.*' ktlintCheck && grep -q 'JOIN_ENABLED_BOOKING_STATUSES' app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt && grep -q 'JOIN_RECONNECT_GRACE_SECONDS' app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt && grep -q '1d18aac' app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt && grep -q 'evaluateJoinWindow' app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt && ! grep -q 'deliberately ignored' app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt && grep -qF 'val JOINABLE_BOOKING_STATUSES: Set<String> = setOf("confirmed", "in_progress", "pending_reschedule")' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/TodaysSessions.kt && grep -q 'serverCanJoin = details.canJoin' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt && grep -q 'canJoin = canJoin' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt && ( for t in enabledStatuses_equalTheBackendJoinableSet pendingReschedule_isNeverEnabled opensExactlyOneHourBeforeStart serverCanJoin_neverOpensBeforeTheLead firstJoin_closesAtTheLaterOfEndAndStartPlusOneHour firstJoinTier_ignoresTheServerFlag reconnect_withServerCanJoinTrue_runsThroughEndPlusOneHour reconnect_isClosedWhenServerCanJoinIsFalseOrNull completed_followsTheSameTiers terminalAndUnknownStatusesAreNeverEnabled showsJoinCardTruthTable; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt || exit 1; done ) && ( for t in pendingRescheduleShowsADisabledJoinInsideTheWindow pastTheEndJoinStaysEnabledWhenTheServerAllowsAReconnect pastTheEndJoinIsDisabledWithoutTheServerFlag; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt || exit 1; done ) && grep -q 'fun toUpcomingDetailsCarriesTheServerJoinFlag(' app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModelTest.kt</automated>
  </verify>
  <done>
- isWithinJoinWindow implements the three tiers with inclusive edges and the confirmed, in_progress and completed enabled set, and fails closed for every other status.
- The JoinWindow KDoc names the iOS and backend 1d18aac sources.
- A client opening Upcoming Booking Details past the scheduled end gets an enabled Join exactly when the server's can_join is true.
- A pending_reschedule booking shows a disabled Join.
- The targeted client and expert test packages and ktlintCheck pass. The work is committed locally on main.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Expert reconnect from actions.can_join on the Upcoming list, dashboard and Booking Summary</name>
  <files>app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/TodaysSessionsSection.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ExpertBookingFixtures.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertBookingsApiTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt</files>
  <behavior>
    - ExpertBookingActionsDto decodes `"can_join":true` as canJoin true, `"can_join":false` as false, and a missing key as false. This holds for both the list row (GET /expert/bookings) and the detail (GET /expert/bookings/{id}/details).
    - Expert Booking Summary: in_progress with start = now - 5400 and end = now - 1800 (30 min past the first-join edge) and canJoin = true has an enabled Join.
    - The same booking with canJoin = false has a disabled Join.
  </behavior>
  <action>
Decision (planner discretion, recorded for the SUMMARY): the brief says to pass expert actions.can_join "if the Android DTOs have it". They do not declare it today. However:
- the backend at 1d18aac sends it on both expert endpoints;
- iOS passes it at all three expert Join sites;
- the existing ExpertBookingsApiTest JSON already carries it.

So this task declares it. The change is one additive Moshi field with no backend or contract change.

DTO, in ExpertBookingsPayload.kt:
- Add `@Json(name = "can_join") val canJoin: Boolean = false` to ExpertBookingActionsDto. It is non-null with a false default, like its sibling flags: a missing key closes the reconnect tier, which is the same effect as null in the rule.
- Update three KDocs in the same file:
  - the ExpertBookingItem KDoc: [actions] now also declares can_join;
  - the ExpertBookingDetail KDoc sentence about the server join flag and the message flag not being declared: the join flag is now declared on [actions] and opens only the reconnect tier of isWithinJoinWindow; the message flag stays undeclared;
  - the ExpertBookingActionsDto KDoc: can_join feeds isWithinJoinWindow as serverCanJoin (iOS ExpertBookingItem.Actions.canJoin).

Summary path:
- In ExpertBookingSummaryUiState.kt, add `canJoin: Boolean` (no default) to ExpertSummaryDetails, with an @property line saying it is the server's actions.can_join and only opens the reconnect tier.
- Map it in toSummaryDetails with `canJoin = actions.canJoin`. Leave showsJoin exactly as it is.
- In ExpertBookingSummaryScreen.kt, pass `serverCanJoin = details.canJoin` as a named argument.

List and dashboard:
- In ExpertUpcomingCard.kt, pass `serverCanJoin = item.actions.canJoin`.
- In the expert dashboard TodaysSessionsSection.kt, pass `serverCanJoin = item.actions.canJoin`. Its rows are ExpertBookingItem from GET /expert/bookings, backend getFeed, which carries the flag.
- Use named arguments, one per line.

Tests:
- Add a `canJoin: Boolean = false` parameter to `expertBookingItem(...)` and `expertBookingDetail(...)` in ExpertBookingFixtures.kt and pass it into ExpertBookingActionsDto.
- Add actionsCanJoinDecodesTrueFalseAndMissing to ExpertBookingsApiTest. Reuse its existing served-JSON pattern, and cover a list row and a detail with true, false and a missing key.
- Add two tests to ExpertBookingSummaryScreenTest, using its existing showScreen and now:
  - joinReconnectsPastTheEndWhenTheServerAllows
  - joinIsDisabledPastTheEndWithoutTheServerFlag

Update any existing expert test whose expectation encoded the old rule rather than deleting it, and list it in the SUMMARY. Do not touch ExpertBookingBuckets, showsJoin or the Today's Sessions filters.
  </action>
  <verify>
    <automated>JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests 'com.uptrendinvestments.clickmeapp.screens.expertmode.*' --tests 'com.uptrendinvestments.clickmeapp.network.ExpertBookingsApiTest' --tests 'com.uptrendinvestments.clickmeapp.network.ResponseDecodingTest' --tests 'com.uptrendinvestments.clickmeapp.managers.JoinWindowTest' ktlintCheck && grep -qF '@Json(name = "can_join") val canJoin: Boolean = false' app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt && ! grep -q 'The server join flag and the message flag are deliberately not declared' app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt && grep -q 'canJoin = actions.canJoin' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt && grep -q 'serverCanJoin = details.canJoin' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt && grep -q 'serverCanJoin = item.actions.canJoin' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt && grep -q 'serverCanJoin = item.actions.canJoin' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/TodaysSessionsSection.kt && grep -q 'fun actionsCanJoinDecodesTrueFalseAndMissing(' app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertBookingsApiTest.kt && ( for t in joinReconnectsPastTheEndWhenTheServerAllows joinIsDisabledPastTheEndWithoutTheServerFlag; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt || exit 1; done )</automated>
  </verify>
  <done>
- An expert can rejoin from the Upcoming list, the dashboard Today's Sessions card or the Booking Summary until end + 60 min whenever the server's actions.can_join is true, as on iOS.
- Without the flag the reconnect tier stays closed.
- The decode and screen tests, the expert packages and ktlintCheck pass. The work is committed locally on main.
  </done>
</task>

<task type="auto">
  <name>Task 3: Explicit flag at every caller, no default, and the full unit-test, ktlint and lint gate</name>
  <files>app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt</files>
  <action>
Make the flag explicit at the last two client sites:
- In ExploreTodaysSessionsSection.kt, pass `serverCanJoin = null` as a named argument. Add a one-line comment saying the client home feed carries no can_join.
- In MyBookingsScreen.kt, pass `serverCanJoin = null`, with a one-line comment saying GET /client/bookings rows carry no can_join and only the detail does.

Then delete the null default from the serverCanJoin parameter in JoinWindow.kt. The parameter line ends with just the type and a trailing comma, so the compiler forces every current and future caller to choose a value, as the iOS isOpen signature does. The test-only `within` helper in JoinWindowTest keeps its own default.

Run the full gate:
- the complete `:app:testDevDebugUnitTest` with no `--tests` filter (the brief requires the full run);
- ktlintCheck;
- lintDevDebug.

Fix every failure that comes from this plan's changes. For a test that encoded the old rule, update its expectation rather than deleting it.

If lintDevDebug reports an issue only in a file this plan did not touch, check with `git diff --name-only` against the commit before Task 1. Record it in the SUMMARY as pre-existing and leave it unfixed.

The SUMMARY must also record these follow-ups. They are out of scope, so do not build them:
1. Visibility still ends at the scheduled end for completed. The following are unchanged by the brief, so a completed booking past its end shows no Join even when the server would allow a reconnect, whereas iOS ExpertJoinVisibility shows completed Join while isOpen is true:
   - showsJoinCard and ExpertBookingSummaryUiState.showsJoin hide the card for completed once end has passed;
   - isRejoinableCompleted and its expert counterpart move completed rows to Past at end.
2. The iOS record /Users/LeonChen/Documents/ClickMe2026/.planning/phases/02-shared-ios-plumbing/02-04-SUMMARY.md still says the Android follow-up is "logged, not built". It belongs to the iOS repo and was not edited.
3. The STATE.md Phase 9 backend note about the join window (120 min, can_join ignored) is now stale.
  </action>
  <verify>
    <automated>JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest ktlintCheck && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g :app:lintDevDebug && grep -qE '^ +serverCanJoin: Boolean\?,$' app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt && grep -q 'serverCanJoin = null' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt && grep -q 'serverCanJoin = null' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt && [ "$(grep -rhoE 'serverCanJoin = (null|details\.canJoin|item\.actions\.canJoin)' app/src/main --include=*.kt | wc -l | tr -d ' ')" = 6 ] && grep -qF 'val JOINABLE_BOOKING_STATUSES: Set<String> = setOf("confirmed", "in_progress", "pending_reschedule")' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/TodaysSessions.kt</automated>
  </verify>
  <done>
- All six Join sites pass the flag explicitly: two pass details.canJoin, two pass item.actions.canJoin and two pass null.
- isWithinJoinWindow has no default for serverCanJoin.
- The full :app:testDevDebugUnitTest run, ktlintCheck and lintDevDebug pass.
- The SUMMARY lists the three follow-ups and any updated legacy tests. Every commit is local on main and not pushed.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| backend API to app | can_join and status arrive from the production backend over TLS; the app treats them as display input only |
| app to POST /bookings/:id/join | the Join tap crosses back to the server, which re-checks status and the per-party window before it mints an Agora token |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-qgu-01 | Elevation of Privilege | isWithinJoinWindow (JoinWindow.kt) | low | accept | The rule is a UX gate and never authorizes anything. At 1d18aac, call.controller.js rejects non-joinable statuses (line 94) and applies evaluateJoinWindow per party before generateToken. A forged or stale enable only produces 403 SESSION_NOT_ACTIVE, which the existing error path shows verbatim. |
| T-qgu-02 | Tampering | can_join decoding (ClientBookingDetail, ExpertBookingActionsDto) | low | mitigate | Both flags are non-null Booleans with a false default, so a missing or absent key closes the reconnect tier. The flag is consulted only after the first-join edge and never before start - 60 min (test serverCanJoin_neverOpensBeforeTheLead). Decoding is tested for true, false and a missing key (actionsCanJoinDecodesTrueFalseAndMissing, ResponseDecodingTest). |
| T-qgu-03 | Denial of Service | Join enablement on all six sites | medium | mitigate | A Join greyed out when the server would accept it costs the user a paid session. The rule mirrors evaluateJoinWindow at 1d18aac edge for edge. Every bound is tested one second either side, and the first-join tier ignores the flag, so a false can_join never closes a first join (firstJoinTier_ignoresTheServerFlag). |
| T-qgu-04 | Information Disclosure | ExpertBookingActionsDto, UpcomingBookingDetails, ExpertSummaryDetails | low | accept | canJoin is a non-sensitive Boolean. The existing toString overrides, which hide private notes, are left unchanged, and no logging is added. |
| T-qgu-SC | Tampering | npm/pip/cargo or Gradle dependency installs | low | accept | This plan installs or upgrades no package or dependency, so the package-legitimacy gate does not apply. |
</threat_model>

<verification>
- The JoinWindowTest edge suite proves (a), (b) and (c) one second either side of every bound, for each enabled status and for serverCanJoin true, false and null.
- The client tracer screen tests prove the DTO-to-UI-state-to-screen-to-rule path for the reconnect tier and for the disabled pending_reschedule Join.
- The expert decode and Booking Summary screen tests prove actions.can_join reaches the rule.
- The Task 3 greps prove all six call sites pass the flag explicitly, the parameter has no default, and JOINABLE_BOOKING_STATUSES is unchanged.
- The full :app:testDevDebugUnitTest run, ktlintCheck and lintDevDebug are green.
</verification>

<success_criteria>
- Join is enabled on Android exactly when the backend at 1d18aac would accept POST /bookings/:id/join for that party. Where the payload carries no flag, the reconnect tier is conservatively closed.
- pending_reschedule shows a disabled Join on every surface that showed Join before. completed uses the same two tiers as confirmed and in_progress.
- The KDoc cites iOS JoinWindow.swift (plans 02-04 and 02-14) and backend evaluateJoinWindow and JOINABLE_BOOKING_STATUSES at 1d18aac.
- The full unit-test suite, ktlint and lint pass, and every commit is local on main with nothing pushed.
</success_criteria>

<output>
Create `.planning/quick/261123-qgu-port-the-ios-join-rule-to-android-joinwi/261123-qgu-SUMMARY.md` when done.
</output>
