---
phase: quick-261123-rdc
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBucketsTest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBuckets.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBucketsTest.kt
autonomous: true
requirements: [CALL-01, CALL-08, EXP-08, BOOK-05, BOOK-06]

estimate:
  tokens: 50000
  raw_tokens: 50000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "On client Upcoming Booking Details and on the expert Booking Summary, a completed booking shows Join exactly while isWithinJoinWindow is true for it: through max(end, start + 3600) inclusive whatever can_join is, and past that through end + 3600 inclusive only when can_join is true. It is hidden at end + 3601, hidden past max(end, start + 3600) when can_join is false or null, and hidden before start - 3600. A visible completed Join is therefore always enabled"
    - "confirmed, in_progress and pending_reschedule (JOINABLE_BOOKING_STATUSES) still show Join on both detail screens at every instant, enabled only inside the window. cancelled, declined, missed, expired, pending_approval, unknown and empty statuses never show it, even with can_join true. On the expert summary this equals iOS ExpertJoinVisibility.shows; on the client screen it keeps Android's Phase 16.5 D-03 rule (no live action where it cannot apply)"
    - "Both detail screens evaluate visibility against the live JoinClock (joinNow), so Join appears and disappears at the reconnect edge without a reload, as iOS does with its 60 s TimelineView. ExpertSummaryDetails no longer stores a load-time showsJoin, and toSummaryDetails no longer takes a time argument"
    - "My Bookings and the expert Upcoming list are unchanged: a completed row moves to Past once its scheduled end is no longer after now, whatever can_join says (iOS isRejoinableCompleted, endTime > now, on both lists). The Explore and expert-dashboard Today's Sessions strips are unchanged (iOS excludes completed from both). Both bucket KDocs and the SUMMARY record this"
    - "isWithinJoinWindow, JOIN_ENABLED_BOOKING_STATUSES and JOINABLE_BOOKING_STATUSES (confirmed, in_progress, pending_reschedule) are unchanged"
    - "The full :app:testDevDebugUnitTest run, ktlintCheck and lintDevDebug all pass. Every commit is local on main and nothing is pushed"
  artifacts:
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt"
      provides: "showsJoinCard(status, start, end, now, serverCanJoin): JOINABLE_BOOKING_STATUSES or isWithinJoinWindow, shared by both detail screens"
      contains: "ExpertJoinVisibility"
    - path: "app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt"
      provides: "showsJoinCard edge tests at end, end + 1, end + 3600 and end + 3601 for can_join true, false and null, the short-session first-join tier, and a per-second sweep proving completed visibility equals isWithinJoinWindow"
      contains: "fun showsJoinCard_completedVisibilityEqualsTheJoinWindow("
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt"
      provides: "Join Session shown through showsJoinCard against joinNow"
      contains: "showsJoinCard("
  key_links:
    - from: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt"
      to: "showsJoinCard"
      via: "Join card visibility, with start and the client detail can_join"
      pattern: "serverCanJoin = details\\.canJoin"
    - from: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt"
      to: "showsJoinCard"
      via: "JoinCallButton visibility, with actions.can_join, evaluated at joinNow"
      pattern: "showsJoinCard\\("
    - from: "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt"
      to: "isWithinJoinWindow"
      via: "showsJoinCard body, so a completed Join is visible exactly while it can be enabled"
      pattern: "isWithinJoinWindow\\("
---

<objective>
Show Join for completed bookings until the reconnect window closes, on the surfaces where iOS keeps Join visible after the scheduled end. This follows quick task 261123-qgu (commits 7d540a33, fd5c700d, d22f4b83), which fixed enabling. Visibility still ended at the scheduled end, so a server-approved reconnect could be enabled but never seen.

Per-surface decision, from reading the iOS sources:
- (a) Client Upcoming Booking Details: iOS keeps the card after the end (`JoinWindow.showsJoinButton`, status-only). Android now shows it for completed while the join window is open. The Phase 16.5 D-03 hide stays for every other non-live status.
- (b) Expert Booking Summary: iOS `ExpertJoinVisibility.shows` = always for confirmed, in_progress, pending_reschedule, else `isOpen`. Android matches it exactly and re-evaluates it on the live JoinClock.
- (c) Client My Bookings and expert Upcoming lists: iOS moves completed rows to Past at the scheduled end on both. Android stays as is; this is recorded, not changed.
- (d) Explore and expert-dashboard Today's Sessions strips: iOS excludes completed. Android stays as is; this is recorded.

Purpose: a live Android user (client or expert) whose call dropped near the end can see and tap Join during the server-approved reconnect window, as on iOS. Calls come first. The server stays the final authority on POST /bookings/:id/join.

Output: one shared visibility rule in JoinWindow.kt with edge tests, both detail screens wired to it at joinNow, the bucket parity recorded in KDoc and pinned by tests, and a green full unit-test, ktlint and lint gate. All commits are local on main and are not pushed. Android only: the iOS and backend repos are read-only references.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md
@.planning/quick/261123-qgu-port-the-ios-join-rule-to-android-joinwi/261123-qgu-SUMMARY.md
@app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt

Cross-repo references (read-only; never edit or commit in these repos):
- /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Utils/JoinWindow.swift: `showsJoinButton(status:)` at :54 is `status != .pendingApproval`; `isOpen(...)` at :67-84.
- /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Utils/ExpertJoinVisibility.swift: `alwaysShownStatuses = [.confirmed, .inProgress, .pendingReschedule]` at :30. `shows(...)` = alwaysShown or `JoinWindow.isOpen`. `isEnabled(...)` = `isOpen`.
- /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Expert Mode/Booking Summary/ExpertBookingSummaryView.swift:289-317: `TimelineView(.periodic(from: .now, by: 60))` re-evaluates `shows` and `isEnabled`. The outer gate (`canMessage || canJoin || alwaysShown`) never hides Join in practice, because the backend sends `actions.can_message: true` on every expert detail (`src/controllers/expertBooking.controller.js:440`).
- /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Client Mode/Upcoming Booking Details/UpcomingBookingViewModel.swift:200-205: `showsJoinCard` = `JoinWindow.showsJoinButton(status:)`.
- /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Expert Mode/Upcoming Bookings/UpcomingBookingsViewModel.swift:208-210 (`isRejoinableCompleted` = completed and `endTime > now`) and :232-237 (`mapPastIfApplicable` keeps completed out of Past only while end > now).
- /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Client Mode/My Bookings/MyBookingsViewModel.swift:201 and :213-216: the same end-based rule.
- /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Expert Mode/Expert Dashboard/ExpertDashboardViewModel.swift:373-381 and /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Client Mode/Explore/ExploreClientViewModel.swift:215-224: `isJoinable` excludes completed from both Today's Sessions strips.

<interfaces>
Facts read at planning time, so the executor does not need to explore:

Android today:
- JoinWindow.kt:97-106 has `fun showsJoinCard(status: String, end: Instant, now: Instant): Boolean`. Its body is JOINABLE_BOOKING_STATUSES membership or a completed term that ends at end. Its only caller is UpcomingBookingScreen.kt:451. Its KDoc cites Phase 16.5 D-03.
- `isWithinJoinWindow(status, start, end, now, serverCanJoin: Boolean?)` sits just above it in the same file. Leave it unchanged.
- JOINABLE_BOOKING_STATUSES = confirmed, in_progress, pending_reschedule (screens/clientmode/mybookings/TodaysSessions.kt:13). That is exactly iOS `ExpertJoinVisibility.alwaysShownStatuses`. Do not change its membership.
- UpcomingBookingScreen.kt:450-475: the comment at :450 cites "Phase 16.5 D-03", and `val showsJoin = showsJoinCard(details.status.raw, details.end, joinNow)` at :451 drives the spacer and `UpcomingBookingJoinCard`. `joinEnabled` already passes `serverCanJoin = details.canJoin`. `UpcomingBookingDetails` has `start`, `end`, `status.raw` and `canJoin: Boolean = false`. The KDoc at UpcomingBookingUiState.kt:35-36 says canJoin "only opens the reconnect tier".
- Phase 16.5 D-03: Payment History opens Upcoming Booking Details for any booking, including past and completed ones, and the screen must offer no live action where it cannot apply. That is why the client keeps hiding non-live statuses where iOS shows a disabled card.
- ExpertBookingSummaryUiState.kt:
  - KDoc `@property showsJoin` at :51, and `@property canJoin` says it "only opens the reconnect tier".
  - The data-class property at :73.
  - `fun ExpertBookingDetail.toSummaryDetails(now: Instant, locale: Locale)` at :116-147. `now` is used only by the showsJoin assignment at :143, and the KDoc above says so.
  - The imports JOINABLE_BOOKING_STATUSES and STATUS_COMPLETED are used only by that assignment.
  - `ExpertSummaryDetails` has `statusCode`, `start`, `end` and `canJoin`, and is constructed only in this file.
- ExpertBookingSummaryScreen.kt:426 reads the load-time `details.showsJoin`. `JoinCallButton(enabled = isWithinJoinWindow(... now = joinNow, serverCanJoin = details.canJoin))` follows. `isWithinJoinWindow` is imported at :70.
- ExpertBookingSummaryViewModel.kt has the constructor parameter `clock: Clock` at :72 (private val). Its only use is :274, which passes the clock's current instant and `Locale.getDefault()` to toSummaryDetails. The java.time Clock import is at :37, and `joinNow` comes from the injected JoinClock (:93-94). Hilt builds the VM through `@AssistedInject`, so removing a constructor parameter needs no DI change. The only manual constructions are ExpertBookingSummaryViewModelTest.kt:67 (`vm()`) and ExpertBookingSummaryScreenTest.kt:121 (`showScreen`), where `clock` is the fifth positional argument.
- Call sites that pass a time to the expert toSummaryDetails:
  - ExpertBookingSummaryScreenTest.kt :215, :289, :594, :607 and :969.
  - About 17 in ExpertBookingSummaryViewModelTest.kt, all with `now` plus Locale.US, except one with `start.plusSeconds(7200)` at :209.
- Tests that change because of the new rule:
  - ExpertBookingSummaryViewModelTest `mapsTheDetailForDisplay` asserts the property at :103.
  - ExpertBookingSummaryViewModelTest `showsJoinFollowsTheStatusAndTheEndTime` (:201-210) tests the removed property.
  - JoinWindowTest `showsJoinCardTruthTable` (:206-224) uses the old signature.
  - UpcomingBookingScreenTest `completedAfterEndShowsNoJoinCard` (:815) sets joinNow exactly at end with canJoin false. Under the new rule the end is inclusive, so the card is now shown and enabled.
  - Existing tests that stay valid: `completedBeforeEndKeepsAnEnabledJoin`, `aCompletedBookingWhoseEndHasNotPassedCanBeRejoined`, `terminalStatusesShowNoLiveAction`, and the expert screen tests `joinHiddenForACompletedBookingThatHasEnded` (canJoin false) and `joinShownForACompletedBookingThatEndsInTheFuture`.
- Fixtures:
  - JoinWindowTest has `now = 2026-10-24T12:00:00Z`, the private `neverJoinable` list (cancelled, declined, missed, expired, something_new, empty) and a `within(...)` helper.
  - UpcomingBookingScreenTest: `detailsStart = 2026-10-24T18:00:00Z`. `details(status, start, end = start + 3600, canJoin: Boolean = false, ...)`, `setScreen(state, joinNow = ...)` and `UPCOMING_BOOKING_JOIN_TAG`. The live-clock pattern is `keepsRescheduleWhenTheJoinClockPassesThe24hMark` (:569), which uses `var now by mutableStateOf(...)` and `UpcomingBookingContent(...)` directly.
  - ExpertBookingSummaryScreenTest: `start = 2026-10-24T18:00:00Z` and `now = start - 1800`. `showScreen(detail)` goes through the real VM with `MutableClock(now)` and `JoinClock`, so joinNow is `now` there. `showContent(state, joinNow = ...)` (:228-262) calls `ExpertBookingSummaryContent` directly, and `EXPERT_SUMMARY_JOIN_TAG` is the button tag.
  - `expertBookingDetail(statusCode, start, end, canJoin, ...)` and `expertBookingItem(id, statusCode, start, end, canJoin, ...)` are in app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ExpertBookingFixtures.kt.
  - ExpertBookingBucketsTest has `now = 2026-10-22T16:00:00Z` and an `item(...)` helper without canJoin, so call `expertBookingItem(...)` directly for a flagged row. BookingBucketsTest has the same `now` and an `item(id, status, start, end)` helper that takes ISO strings; ClientBookingItem has no can_join.
- Bucket code:
  - ExpertBookingBuckets.kt:26-31 is `ExpertBookingItem.isRejoinableCompleted(now)` (completed and end after now). Its KDoc says "iOS `isRejoinableCompleted`, D-15".
  - BookingBuckets.kt:22-26 is the client equivalent.
  - Neither KDoc mentions can_join or the reconnect window.
</interfaces>

Per-surface record for the SUMMARY (copy this table into it):

| Surface | iOS rule (source) | Android before | Android after |
|---------|-------------------|----------------|---------------|
| Client Upcoming Booking Details | every status but pending_approval shows the card, `isOpen` enables it (UpcomingBookingViewModel.swift:203) | live statuses, or completed until end | live statuses, or isWithinJoinWindow (completed visible exactly while enabled); D-03 hide kept for terminal statuses |
| Expert Booking Summary | ExpertJoinVisibility.shows: alwaysShown or `isOpen`, every 60 s | live statuses, or completed until end, fixed at load | live statuses, or isWithinJoinWindow, at joinNow (exact iOS match) |
| Client My Bookings buckets | completed stays Upcoming while end > now (MyBookingsViewModel.swift:213) | same | unchanged (iOS drops at end) |
| Expert Upcoming buckets | completed stays Upcoming while end > now (UpcomingBookingsViewModel.swift:208) | same | unchanged (iOS drops at end) |
| Explore and expert-dashboard Today's Sessions | completed excluded (isJoinable) | completed excluded | unchanged |

Known side effect, matching iOS ExpertJoinVisibility: a completed booking more than 60 minutes before its start, which cannot happen in practice, is now hidden. The old rule showed it.

Build environment:
- Set `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` and run every command from the checkout root. In a worktree, first copy /Users/LeonChen/Documents/ClickMe_Android/local.properties to the worktree root, and never commit it.
- Never run `./gradlew --stop`. The test task needs `-Pkotlin.daemon.jvmargs=-Xmx3g`. Non-test tasks such as lintDevDebug also need `-Dorg.gradle.jvmargs=-Xmx4g` (STATE Phase 12 note).
- Style: ktlint_official with a 120-column limit. A function or call with two or more parameters puts one parameter per line. Pass `serverCanJoin` as a named argument at every new showsJoinCard call so the key-link greps can find it. In KDoc and comments, refer to the parameter as [serverCanJoin] or in prose; never write the parameter name followed by a space, an equals sign and a value. Remove any import that becomes unused, because ktlint's no-unused-imports rule fails the build.
- Parallel work: quick task 261123-rde runs on main before or after this one. Do not touch cancellation (screens/expertmode/cancellation, client cancel), card-delete, payment-intent or booking error-mapping files. None are in files_modified.
- Commits: local commits on main with the subject prefix `fix(quick-261123-rdc):` or `test(quick-261123-rdc):`. Do not push.
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: Shared visibility rule plus the client Upcoming Booking Details card, end to end</name>
  <files>app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt</files>
  <precondition>iOS still defines the expert always-shown set as confirmed, in_progress, pending_reschedule: `grep -qF 'static let alwaysShownStatuses: Set<BookingStatus> = [.confirmed, .inProgress, .pendingReschedule]' /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Utils/ExpertJoinVisibility.swift` exits 0.</precondition>
  <behavior>
    - showsJoinCard_liveStatusesShowAtEveryInstant: confirmed, in_progress and pending_reschedule return true at start - 2 days, start, end and end + 1 day, with serverCanJoin true, false and null.
    - showsJoinCard_completedShowsThroughTheFirstJoinEdgeWhateverTheFlag: in a 60-minute session, completed is true at start - 3600, end - 1 and end (inclusive) for true, false and null, and false at start - 3601 for all three.
    - showsJoinCard_completedPastTheEndShowsOnlyWithTheServerFlag: in a 60-minute session, at end + 1 and end + 3600 the result is true for true and false for false and null. At end + 3601 it is false for all three.
    - showsJoinCard_shortCompletedSessionShowsUntilStartPlusOneHour: in a 30-minute session (end = start + 1800), null is true at end + 1 and at start + 3600. At start + 3601, null and false are false and true is true. With true, end + 3600 is true and end + 3601 is false.
    - showsJoinCard_completedVisibilityEqualsTheJoinWindow: for session lengths 1800, 3600 and 5400 s, at every second from start - 3601 through end + 3601, and for each of true, false and null, showsJoinCard for completed equals isWithinJoinWindow for completed. A visible completed Join is never disabled.
    - showsJoinCard_terminalUnknownAndPendingApprovalNeverShow: every neverJoinable status plus pending_approval is false at start, at end and at end + 1800, even with true.
    - Screen tests in UpcomingBookingScreenTest:
      - completedAtExactlyTheEndKeepsAnEnabledJoin: joinNow = detailsStart + 3600, canJoin false, so the Join tag exists and is enabled.
      - completedPastTheEndWithoutTheServerFlagShowsNoJoinCard: joinNow = detailsStart + 3601, canJoin false, so the Join tag count is 0.
      - completedPastTheEndWithTheServerFlagKeepsAnEnabledJoin: joinNow = detailsStart + 7200 (end + 3600), canJoin true, so the button is enabled.
      - completedJoinCardLeavesWhenTheJoinClockPassesTheReconnectWindow: canJoin true, with a mutable joinNow at detailsStart + 7200. The button is enabled, then joinNow moves to detailsStart + 7201 and the Join tag count becomes 0 without a new state.
  </behavior>
  <action>
RED first:
- In JoinWindowTest, replace `showsJoinCardTruthTable` with the six showsJoinCard tests in the behavior block. Use local start and end instants built from the class `now`, and the existing `neverJoinable` list.
- In UpcomingBookingScreenTest, replace `completedAfterEndShowsNoJoinCard`, whose expectation is now wrong because the end is inclusive, with the four completed screen tests in the behavior block. Build the live-clock test the way `keepsRescheduleWhenTheJoinClockPassesThe24hMark` is built.
- Run the two test classes and confirm they fail. A compile failure on the new showsJoinCard signature counts as RED.
- Commit with the prefix `test(quick-261123-rdc):`.

GREEN:
1. In JoinWindow.kt, change showsJoinCard to take `status: String`, `start: Instant`, `end: Instant`, `now: Instant` and `serverCanJoin: Boolean?`, one per line. There is no default for serverCanJoin, matching isWithinJoinWindow.
   - Its body is JOINABLE_BOOKING_STATUSES membership OR `isWithinJoinWindow(...)` called with the same five values. This replaces the old completed term that ended at end.
   - Rewrite its KDoc. It is the shared Join visibility rule for client Upcoming Booking Details and the expert Booking Summary, and it equals iOS `ExpertJoinVisibility.shows` (ClickMe2026/Utils/ExpertJoinVisibility.swift). iOS `alwaysShownStatuses` is the same set as JOINABLE_BOOKING_STATUSES.
   - The KDoc also says that a completed booking therefore shows Join exactly while it can be enabled, including the server-approved reconnect tier through end + 60 min (quick 261123-rdc, the follow-up to 261123-qgu).
   - Record the client deviation: iOS client details shows the card for every status but pending_approval (`JoinWindow.showsJoinButton`), but Android keeps Phase 16.5 D-03, so a booking that can no longer be joined shows no card.
   - Leave isWithinJoinWindow, its constants and JOIN_ENABLED_BOOKING_STATUSES unchanged.
2. In UpcomingBookingScreen.kt, change the call at :451 to the new showsJoinCard with named arguments: status = details.status.raw, start = details.start, end = details.end, now = joinNow and serverCanJoin = details.canJoin. Update the :450 comment to say that a completed booking keeps the card while its join window is open. Leave the spacer and pending-banner logic as they are.
3. In UpcomingBookingUiState.kt, update the `canJoin` KDoc (:35-36). It opens the reconnect tier of isWithinJoinWindow and, for a completed booking, keeps the Join card visible through that tier (`showsJoinCard`). It is still always fresh because the ViewModel reloads on booking.update.
4. Run the verify command and confirm it is green. Commit with the prefix `fix(quick-261123-rdc):`.
  </action>
  <verify>
    <automated>JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests 'com.uptrendinvestments.clickmeapp.managers.JoinWindowTest' --tests 'com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.*' ktlintCheck && F=app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt && grep -A6 'fun showsJoinCard(' "$F" | grep -qE '^ +start: Instant,$' && grep -A6 'fun showsJoinCard(' "$F" | grep -qE '^ +serverCanJoin: Boolean\?,$' && grep -A12 'fun showsJoinCard(' "$F" | grep -q 'isWithinJoinWindow(' && ! grep -qF 'status == STATUS_COMPLETED && end.isAfter(now)' "$F" && grep -q 'ExpertJoinVisibility' "$F" && [ "$(grep -o 'serverCanJoin = details.canJoin' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt | wc -l | tr -d ' ')" -ge 2 ] && grep -q 'showsJoinCard' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt && ( for t in showsJoinCard_liveStatusesShowAtEveryInstant showsJoinCard_completedShowsThroughTheFirstJoinEdgeWhateverTheFlag showsJoinCard_completedPastTheEndShowsOnlyWithTheServerFlag showsJoinCard_shortCompletedSessionShowsUntilStartPlusOneHour showsJoinCard_completedVisibilityEqualsTheJoinWindow showsJoinCard_terminalUnknownAndPendingApprovalNeverShow; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt || exit 1; done ) && ( for t in completedAtExactlyTheEndKeepsAnEnabledJoin completedPastTheEndWithoutTheServerFlagShowsNoJoinCard completedPastTheEndWithTheServerFlagKeepsAnEnabledJoin completedJoinCardLeavesWhenTheJoinClockPassesTheReconnectWindow; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt || exit 1; done )</automated>
  </verify>
  <done>
- showsJoinCard takes start and serverCanJoin and returns JOINABLE_BOOKING_STATUSES membership or isWithinJoinWindow.
- Client Upcoming Booking Details shows an enabled Join for a completed booking through the reconnect window when can_join is true, and hides it after the window or past the first-join edge without the flag.
- All new JoinWindowTest and UpcomingBookingScreenTest cases pass, and so does ktlintCheck.
- Two local commits (test, then fix) are on main.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Expert Booking Summary shows Join through the reconnect window, evaluated live (exact iOS ExpertJoinVisibility)</name>
  <files>app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModelTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt</files>
  <behavior>
    - joinShownForACompletedBookingInTheReconnectTier: through the real VM path with showScreen, a completed detail with start = now - 5400, end = now - 1800 and canJoin true shows the Join tag, enabled.
    - joinHiddenForACompletedBookingPastTheEndWithoutTheServerFlag: the same times with canJoin false give a Join tag count of 0.
    - joinHiddenForACompletedBookingAfterTheReconnectWindow: start = now - 7201, end = now - 3601 and canJoin true give a Join tag count of 0.
    - completedJoinLeavesWhenTheJoinClockPassesTheReconnectWindow: through ExpertBookingSummaryContent with a mutable joinNow, a completed detail (start, end = start + 3600, canJoin true) shows an enabled Join at joinNow = start + 7200. When joinNow moves to start + 7201, the count becomes 0 with the same state. This proves visibility is no longer fixed at load.
    - Existing expert summary tests still pass unchanged in meaning: joinHiddenForCancelled/Declined/Missed/Expired, joinHiddenForACompletedBookingThatHasEnded, joinShownForACompletedBookingThatEndsInTheFuture, joinReconnectsPastTheEndWhenTheServerAllows and joinIsDisabledPastTheEndWithoutTheServerFlag.
  </behavior>
  <action>
RED first:
- Add the four screen tests in the behavior block to ExpertBookingSummaryScreenTest. For the live-clock test, copy the showContent setup (:228-262) and hold joinNow in a `mutableStateOf`.
- Run the class. joinShownForACompletedBookingInTheReconnectTier and the live-clock test fail against the load-time rule.
- Commit with the prefix `test(quick-261123-rdc):`.

GREEN:
1. ExpertBookingSummaryUiState.kt:
   - Remove the `showsJoin` property from ExpertSummaryDetails and its `@property showsJoin` KDoc line. Visibility is now computed on screen, per iOS ExpertJoinVisibility (shows is re-evaluated by a 60 s TimelineView).
   - Remove the time parameter from toSummaryDetails, so its only parameter is `locale: Locale`. Update the KDoc above it, which currently says the time decides whether a completed booking is still rejoinable: the mapping no longer depends on the clock.
   - Remove the JOINABLE_BOOKING_STATUSES and STATUS_COMPLETED imports if nothing else in the file uses them.
   - Update the `@property canJoin` KDoc. It opens the reconnect tier of isWithinJoinWindow and keeps a completed booking's Join visible through it (`showsJoinCard`).
2. ExpertBookingSummaryScreen.kt: replace the read of the removed property at :426 with a local `showsJoin` computed by showsJoinCard, using named arguments status = details.statusCode, start = details.start, end = details.end, now = joinNow and serverCanJoin = details.canJoin. Import it from the managers package next to isWithinJoinWindow. The JoinCallButton enabled argument stays as it is.
3. ExpertBookingSummaryViewModel.kt: the `clock` constructor parameter (:72) has no other use once the mapping takes only a locale.
   - Remove it and its now-unused java.time Clock import.
   - Change :274 to call toSummaryDetails with Locale.getDefault() only.
   - Keep joinClock and every other parameter in their current order.
4. Tests:
   - ExpertBookingSummaryViewModelTest:
     - Drop the `clock` argument from the `vm()` construction (:67). The test's own clock still feeds JoinClock.
     - Change every toSummaryDetails call to pass Locale.US only.
     - Delete the property assertion in `mapsTheDetailForDisplay` (:103).
     - Delete `showsJoinFollowsTheStatusAndTheEndTime`. Its cases now live in JoinWindowTest's showsJoinCard tests from Task 1.
   - ExpertBookingSummaryScreenTest: drop the `clock` argument from the VM construction in `showScreen` (:121) and change every toSummaryDetails call to pass Locale.US only.
5. Run the verify command and confirm it is green. Commit with the prefix `fix(quick-261123-rdc):`.
  </action>
  <verify>
    <automated>JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests 'com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.*' --tests 'com.uptrendinvestments.clickmeapp.managers.JoinWindowTest' ktlintCheck && D=app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary && ! grep -q 'val showsJoin' "$D/ExpertBookingSummaryUiState.kt" && ! grep -rqE 'toSummaryDetails\((now|clock\.instant\(\)|start\.plusSeconds\([0-9]+\)), Locale' app/src && ! grep -q 'private val clock: Clock' "$D/ExpertBookingSummaryViewModel.kt" && ! grep -q '^import java.time.Clock$' "$D/ExpertBookingSummaryViewModel.kt" && grep -q 'showsJoinCard(' "$D/ExpertBookingSummaryScreen.kt" && [ "$(grep -o 'serverCanJoin = details.canJoin' "$D/ExpertBookingSummaryScreen.kt" | wc -l | tr -d ' ')" -ge 2 ] && grep -q 'showsJoinCard' "$D/ExpertBookingSummaryUiState.kt" && ( for t in joinShownForACompletedBookingInTheReconnectTier joinHiddenForACompletedBookingPastTheEndWithoutTheServerFlag joinHiddenForACompletedBookingAfterTheReconnectWindow completedJoinLeavesWhenTheJoinClockPassesTheReconnectWindow joinHiddenForACompletedBookingThatHasEnded joinReconnectsPastTheEndWhenTheServerAllows; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt || exit 1; done )</automated>
  </verify>
  <done>
- The expert Booking Summary shows Join for confirmed, in_progress and pending_reschedule at every instant, and for completed exactly while isWithinJoinWindow is true. It is evaluated at joinNow, so it leaves at end + 3601 with no reload.
- ExpertSummaryDetails carries no load-time visibility, and the ViewModel no longer takes a Clock.
- The expert bookingsummary tests, JoinWindowTest and ktlintCheck pass.
- Two local commits (test, then fix) are on main.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Record the list-bucket iOS parity, pin it with tests, and run the full gate</name>
  <files>app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBucketsTest.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBuckets.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBucketsTest.kt</files>
  <precondition>iOS still moves completed rows to Past at the scheduled end on both lists: `grep -qF 'item.status.code == .completed && item.session.endTime > now' "/Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Expert Mode/Upcoming Bookings/UpcomingBookingsViewModel.swift"` and `grep -qF 'return item.endTime > Date()' "/Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Client Mode/My Bookings/MyBookingsViewModel.swift"` both exit 0. If either fails, stop and report: the iOS list rule changed and this task's keep-as-is decision must be revisited.</precondition>
  <behavior>
    - ExpertBookingBucketsTest aCompletedPastRowInTheReconnectWindowStaysInPastEvenWithTheServerFlag: a completed Past row from `expertBookingItem(statusCode = "completed", start = now - 5400, end = now - 1800, canJoin = true)` stays in Past, Upcoming is empty, and isRejoinableCompleted(now) is false.
    - BookingBucketsTest completedBookingInsideTheReconnectWindowStaysInPast: a completed Past row that ended 30 minutes before `now` (start 2026-10-22T14:30:00Z, end 2026-10-22T15:30:00Z) stays in Past, and Upcoming is empty.
    - Both tests pass against the current code. They pin the iOS-parity decision, they do not change behavior.
  </behavior>
  <action>
1. Add the two tests in the behavior block. They are characterization tests and should pass immediately.
2. Append one KDoc paragraph to `isRejoinableCompleted` in ExpertBookingBuckets.kt and to the one in BookingBuckets.kt. Each paragraph says:
   - The list keeps the end-based edge on purpose, whatever can_join says. iOS `isRejoinableCompleted` (UpcomingBookingsViewModel.swift and MyBookingsViewModel.swift) moves a completed row to Past at its scheduled end on both lists.
   - Only the detail screens extend a completed booking's Join visibility into the reconnect window (`showsJoinCard`, quick 261123-rdc). Use the words "reconnect window" and "can_join" in the paragraph.
   - The client list payload carries no can_join at all. Add this point in BookingBuckets.kt only.
   No logic changes in either file.
3. Run the full gate (verify command below): the full :app:testDevDebugUnitTest, ktlintCheck and lintDevDebug. Fix any failure in the files this plan owns. If a failure is outside them, stop and report it.
4. Commit with the prefix `test(quick-261123-rdc):`, covering the tests and the KDoc.
5. Write the SUMMARY. It includes:
   - the per-surface table from the context, with the iOS rule per surface;
   - the D-03 deviation on client details (iOS shows a disabled card for any non-pending_approval status, Android shows Join for completed only while it can be enabled);
   - the side effect where a completed booking before start - 60 min is hidden;
   - the removed VM Clock parameter;
   - the legacy tests that were replaced or deleted, with reasons;
   - the test count and gate results;
   - follow-up 1 of the qgu SUMMARY marked resolved;
   - every commit local on main and not pushed.
  </action>
  <verify>
    <automated>JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest ktlintCheck && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g :app:lintDevDebug && grep -q 'reconnect window' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt && grep -q 'can_join' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt && grep -q 'reconnect window' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBuckets.kt && grep -q 'can_join' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBuckets.kt && grep -q 'fun aCompletedPastRowInTheReconnectWindowStaysInPastEvenWithTheServerFlag(' app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBucketsTest.kt && grep -q 'fun completedBookingInsideTheReconnectWindowStaysInPast(' app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBucketsTest.kt && grep -qF 'val JOINABLE_BOOKING_STATUSES: Set<String> = setOf("confirmed", "in_progress", "pending_reschedule")' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/TodaysSessions.kt && grep -qF 'statusCode == STATUS_COMPLETED && endTime.isAfter(now)' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt</automated>
  </verify>
  <done>
- Both bucket KDocs record why the lists keep the end edge (iOS parity), and two characterization tests pin it.
- The last two greps are invariant checks that pass at HEAD on purpose: JOINABLE_BOOKING_STATUSES membership and the expert bucket logic are unchanged.
- The full :app:testDevDebugUnitTest has 0 failures, and ktlintCheck and lintDevDebug pass.
- The SUMMARY is written with the per-surface table. Every commit is local on main and not pushed.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| backend API to app | status, schedule and can_join arrive from the production backend over TLS. The app treats them as display input only |
| app to POST /bookings/:id/join | a Join tap crosses back to the server, which re-checks status and the per-party window before it mints an Agora token |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-rdc-01 | Elevation of Privilege | showsJoinCard (JoinWindow.kt) | low | accept | Visibility is a UX gate and authorizes nothing. call.controller.js at 1d18aac rejects non-joinable statuses and applies evaluateJoinWindow per party before generateToken. A Join shown in error only produces 403 SESSION_NOT_ACTIVE, which the existing error path shows. |
| T-rdc-02 | Denial of Service | completed Join on both detail screens | medium | mitigate | A Join hidden when the server would accept a reconnect costs the user a paid session. The visibility for completed is defined as isWithinJoinWindow, and showsJoinCard_completedVisibilityEqualsTheJoinWindow sweeps every second from start - 3601 to end + 3601 for three session lengths and all three flag values. The screen tests prove the client and expert paths, including the live JoinClock flip. |
| T-rdc-03 | Tampering | stale can_join on a detail loaded earlier | low | mitigate | can_join is computed server-side at read time and can go stale while the screen is open. Both detail ViewModels reload on booking.update. The client-side cap (end + 3600, evaluated live at joinNow) hides Join once the reconnect window has closed, so a stale true can never show Join past the window. |
| T-rdc-04 | Information Disclosure | ExpertSummaryDetails, UpcomingBookingDetails | low | accept | No new fields. A field is removed, and the toString overrides that hide private notes are unchanged. No logging is added. |
| T-rdc-SC | Tampering | npm/pip/cargo or Gradle dependency installs | low | accept | This plan installs or upgrades no package or dependency, so the package-legitimacy gate does not apply. |
</threat_model>

<verification>
- The JoinWindowTest showsJoinCard suite proves visibility at end, end + 1, end + 3600 and end + 3601 for can_join true, false and null, the short-session first-join tier, the lead edge, and the never-show statuses. The per-second sweep proves a visible completed Join is always enabled.
- The client tracer screen tests prove the state-to-screen-to-rule path for the reconnect tier, including the live JoinClock flip.
- The expert screen tests prove actions.can_join reaches visibility through the real ViewModel, and that visibility follows joinNow.
- The bucket characterization tests and KDoc record and pin the iOS-parity decision to keep the end edge on both lists.
- The full :app:testDevDebugUnitTest run, ktlintCheck and lintDevDebug are green.
</verification>

<success_criteria>
- A completed booking shows an enabled Join on client Upcoming Booking Details and on the expert Booking Summary through end + 60 min when the server's can_join is true, and through max(end, start + 60 min) otherwise. It disappears at the edge without a reload.
- The expert Booking Summary visibility equals iOS ExpertJoinVisibility.shows. The client screen keeps the Phase 16.5 D-03 hide for statuses that can no longer be joined.
- The lists and Today's Sessions strips are unchanged, matching iOS, and the reason is recorded.
- The full unit-test suite, ktlint and lint pass, and every commit is local on main with nothing pushed.
</success_criteria>

<output>
Create `.planning/quick/261123-rdc-show-join-for-completed-bookings-until-t/261123-rdc-SUMMARY.md` when done.
</output>
