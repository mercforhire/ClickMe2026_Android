---
phase: 15-polish-hardening
plan: 07
subsystem: accessibility
tags: [booking, talkback, sheets, large-screen, tracer, semantics]
status: complete
requires: [15-01, 15-02, 15-04]
provides:
  - "Book Session reads \"Book Session with {name}\" (Role.Button) and Expert Profile sections cap at 600 dp"
  - "Book Now stateDescription \"In progress\" while booking (a11y_in_progress), disabled reasons unchanged"
  - "Slots heading is a polite live region: \"{n} time(s) available\" / \"No times this day\""
  - "Calendar and slots card are separate traversal groups; month title is a heading read before Previous / Next month"
  - "Day cells read Today / Selected + Available or No availability; 48 dp day and slot targets"
  - "Topic load failure is ErrorState (\"Couldn't load topics\", body, Retry); topic cards in a selectableGroup"
  - "BookingTalkBackFlowTest (SC-4 flow 2 semantics)"
affects: [15-17, 15-19]
tech-stack:
  added: []
  patterns:
    - "Tall Robolectric window (@Config qualifiers w411dp-h3000dp) so off-screen nodes keep real bounds for A11yAudit order and target checks"
key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTalkBackFlowTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingBookButton.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingCalendarCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTopicPicker.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTopicSheet.kt
    - app/src/main/res/values/strings_expert_profile.xml
    - app/src/main/res/values/strings_make_a_booking.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingCalendarSlotsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingTopicCardsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingFormCardsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreenTest.kt
key-decisions:
  - "Expert Profile layout order kept (hero, stats, buttons, About, as iOS and D-14); UI-SPEC wording 'after About' not applied, see Deviations"
  - "Slots heading description is \"Available Time Slots, {count text}\" so the title is not lost when the live region speaks"
  - "Topic cards share one lazy item holding a selectableGroup Column; PickerSheet.kt (shared) untouched"
  - "Selected day cell state is \"Today, Selected, Available\" (selected adds to, not replaces, availability)"
requirements-completed: [POL-01, POL-03, POL-04]
plan_head_before: e808da6b9c3dd62717fc017104c562ebca5b1350
plan_head_after: 67a210d370501ce62be79acd8dbf1d5383237ffb
commits: 4
actuals:
  tokens: 21000
  tasks: 2
  commits: 4
duration: ~3h wall clock (dominated by one 38 min Kotlin OOM build under parallel executor load, then 1 to 7 min builds)
completed: 2026-10-29
---

# Phase 15 Plan 07: Book a session TalkBack contract Summary

Expert Profile -> Make a Booking now meets the Flow 2 TalkBack contract on its entry and submit controls: "Book Session with {name}", Book Now "In progress", a polite "{n} times available" / "No times this day" slots announcement, separate calendar and slots traversal groups, and a named topic-load error.

## What was built

- **Book Session** (`ExpertProfileActionButtons`): new `expertName` parameter; the enabled button keeps `Role.Button` and gets `contentDescription = "Book Session with {name}"` (`expert_profile_book_session_a11y`). `ExpertProfileScreen` passes the loaded expert's name.
- **Expert Profile cap (D-02):** the `details` and `reviews` LazyColumn items wrap in `Modifier.contentMaxWidth()`; the hero (and its full-width glow) is not capped.
- **Book Now:** in-flight `stateDescription` is `a11y_in_progress` ("In progress"); the unused `make_a_booking_state_booking` string was removed. Disabled reasons were already mapped (NoSlot "Choose a time slot to continue", etc.) and are now covered by a test.
- **Slots card:** heading is a polite live region with description "Available Time Slots, {n} time(s) available" or "..., No times this day" (counts selectable slots; null while loading, failed, or the day is not loaded). The card is its own traversal group. `SectionError` stays for a failed availability load (archetype C).
- **Calendar card:** the card is a traversal group; the month row is a group whose title is `heading()` + polite live region + `traversalIndex = -1f` so it reads before Previous / Next month. Day cells now always carry a state: `Today, ` + `Selected, ` (when chosen) + `Available` or `No availability`. Day cell fills its grid cell (was only the 36 dp circle) and slot chips have a 48 dp min width.
- **Topic picker:** `TopicsState.Failed` renders `ErrorState(heading "Couldn't load topics", body = server message, retryLabel "Retry")`. Topic sheet cards sit in one `selectableGroup()` item (each card is still a merged `Role.RadioButton`).
- **Strings:** `expert_profile_book_session_a11y`; `make_a_booking_day_open` reworded to "Available"; new `make_a_booking_day_no_availability`, `make_a_booking_day_selected`, plural `make_a_booking_slots_available_a11y`, `make_a_booking_slots_none_a11y`, `make_a_booking_topics_error_heading`.

## Tests

- BookingTalkBackFlowTest 5/5 (new): bookSessionNamesExpert, bookNowDisabledReason, bookNowBusy, formNamedAndLargeEnough (`assertActionablesNamed` + `assertTouchTargets`), formOrder (title, Topic, month, slot, Meeting Type, Message, Book Now).
- MakeABookingCalendarSlotsTest 29 (8 new: count announcement for 3/1/0 slots, no count while loading, time order, separate traversal groups, month heading + live region, empty month "No availability" with nav enabled, selected day state; one existing test renamed and updated for the new day copy).
- MakeABookingTopicCardsTest 16 (3 new: named load error with one Retry call, empty copy without Retry, radio rows inside one selectableGroup).
- Stay green: ExpertProfileScreenTest 36, MakeABookingScreenTest 36, MakeABookingFormCardsTest 15, EntrySavedStateHandleTest 2, BookingRequestSentScreenTest 19.
- `ktlintCheck` clean; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- TDD: RED commits 91cc92ce and 8f17e3b0 (failures were on the planned assertions: wrong/missing descriptions and states, missing heading/groups/ErrorState), GREEN commits 6a5dfece and 67a210d3. No `gsd_run check tdd-red-evidence` record was produced (plan type is `execute`, not `tdd`); RED was confirmed by reading the failing assertion messages.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Calendar day cells and slot chips were below the 48 dp target**
- **Found during:** Task 1 (`formNamedAndLargeEnough`)
- **Issue:** the day cell's clickable wrapped only the 36 dp circle (36 x 48); a lone slot chip can be narrower than 48 dp.
- **Fix:** day `Column` uses `fillMaxSize()` inside its grid cell; slot chip gets `widthIn(min = 48.dp)`.
- **Files modified:** `MakeABookingCalendarCard.kt`, `MakeABookingTimeSlotsCard.kt`
- **Commit:** 6a5dfece

**2. [Rule 2 - Missing critical] Day cells did not say "No availability" / "Selected"**
- **Found during:** Task 2 (plan truth: "Available / No availability / Selected")
- **Issue:** only open days had a state ("Has open times"); no-availability days had none.
- **Fix:** every day cell now states Available or No availability, plus Today and Selected where they apply. The existing test `openDaysAreAnnouncedAsHavingOpenTimes` was renamed and updated.
- **Commit:** 67a210d3

**3. [Rule 1 - Bug] Month title was not a heading and read after Next month**
- **Found during:** Task 2
- **Fix:** `heading()` added (live region kept) and `traversalIndex = -1f` inside a month-row traversal group.
- **Commit:** 67a210d3

**4. Existing tests updated for the plan's new Book Now copy:** `MakeABookingFormCardsTest.bookNowInFlight...` and `MakeABookingScreenTest.paidBookNow...` asserted `stateDescription == "Booking"`; now "In progress" (plan truth).

### Plan premises that did not match the code (no architecture change made)

- **Topic error "inside the sheet":** the topic sheet only opens when topics are loaded (`state.topicList.isNotEmpty()`), so a load failure never appears inside it. The failure surface is the Topic row card in the form; the ErrorState (heading, body, Retry) was applied there, which is what `MakeABookingTopicPicker.kt` and the plan's tests/acceptance grep target. No sheet-state changes and no `heightIn(min = 240.dp)` (the row is not a sheet body). If a failure-inside-sheet flow is wanted it needs ViewModel and screen changes in `MakeABookingScreen.kt` (owned by 15-19).
- **Topic empty "EmptyState":** the picker keeps its existing compact empty card (plan behavior says "existing empty copy ... no Retry"); a full EmptyState would change the form layout.
- **Book Session "after About":** UI-SPEC says Book Session comes after the name, rating and About; the shipped layout (and iOS) puts it after name, rating and stats, before About. Reordering contradicts D-14 and iOS, so the test asserts name -> Book Session only. Hand-off: confirm intent in the device checklist (15-17).
- **Slot cell states:** UI-SPEC slot states "Available / Held, unavailable / Selected" were not in this plan's behaviors; slot chips keep "Unavailable" for held/past and the platform Selected state.

**Total deviations:** 3 auto-fixed (1 Rule 2, 2 Rule 1) plus 1 test-expectation update and 4 premise mismatches documented. **Impact:** all plan acceptance criteria met; no scope outside the listed files except the shared existing tests (`MakeABookingFormCardsTest`, `MakeABookingScreenTest`, `ExpertProfileScreenTest`) updated for the changed copy and new `expertName` parameter.

## Hand-offs

- **15-19:** `MakeABookingScreen.kt`, `BookingRequestSentScreen.kt`, `BookingRequestSentHeadline.kt`, `MakeABookingInsetsTest.kt` and `BookingRequestSentScreenTest.kt` untouched here. 15-19 owns the Make a Booking / Request Sent 600 dp cap and the footer above the IME. Note `BookingTalkBackFlowTest` renders `MakeABookingContent` directly on a 411 x 3000 dp window; if 15-19 changes `MakeABookingContent`'s parameters, update `setBooking` in that test.
- **Note for any later change to these cards:** tests need a tall Robolectric window; nodes outside the viewport get empty `boundsInRoot`, which breaks `assertTraversalOrder` and `assertTouchTargets`.

## Open items (device checklist, nothing blocks on a person)

- TalkBack speech (not just exposure) for: "Book Session with {name}"; Book Now "In progress"; day cells "Today, Selected, Available"; the slots heading announcement on day change (first composition also announces); month title reading before Previous / Next month.
- Day cell width on a 360 dp phone is (360 - 32 - 8) / 7 = 45.7 dp, still under 48 dp horizontally (height is 48). The Robolectric check passes at 411 dp (53 dp). Check on the Samsung SM-A075F or consider reducing the card padding in a later plan.
- Expert Profile at 600 dp cap on a resizable emulator (hero glow full width, sections capped).
- Topic sheet: TalkBack reads the cards as a radio group; closing the sheet returns focus to the Topic row (not asserted on the JVM).

## Known Stubs

None.

## Threat Flags

None. T-15-15 (server text in the topic ErrorState body) accepted as planned: plain `Text`, the same `FieldError.asString()` text `SectionError` showed.

## Commits

- 91cc92ce test(15-07): add failing BookingTalkBackFlowTest for Flow 2 semantics
- 6a5dfece feat(15-07): name Book Session after the expert, announce Book Now busy state
- 8f17e3b0 test(15-07): add failing calendar, slot count and topic picker semantics tests
- 67a210d3 feat(15-07): announce slot count, group calendar and slots, name topic load error

## Self-Check: PASSED

Created file `BookingTalkBackFlowTest.kt` exists; the four commits are ancestors of HEAD; all listed test classes pass with 0 failures and `ktlintCheck` plus `check-design-rules.sh` are clean.
