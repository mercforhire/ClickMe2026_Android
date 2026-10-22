# Phase 6: Booking Flow & Free Bookings - Context

**Gathered:** 2026-10-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace the body of `MakeABookingStubScreen` (the route `BookingKey` stays as registered in Phase 5, D-04) with the iOS Make a Booking screen. The sections, in order:

1. Expert card
2. Topic of Discussion (picker sheet)
3. Select a Time (custom month calendar)
4. Available Time Slots
5. Meeting Type
6. Message (optional, 500 chars)
7. Summary
8. Book Now

Topics come from `GET /experts/:id/details`. Availability comes from `GET /experts/:id/availability?month=yyyy-MM` for the displayed month and its two neighbors. Slots are regrouped by the client's local date using `startUtc`, and duplicates are dropped.

A free topic is booked with `POST /bookings/request`. On success, the client lands on Request Sent. The iOS expert receives the request through the server's `booking_request` push.

Out of scope:
- The paid paths: payment-intent, SetupIntent, PaymentSheet, long-lead, 3DS, the payment-methods gate, and the "How does payment work?" sheet (Phase 7)
- My Bookings, Upcoming Booking Details, and the booking-detail screens (Phase 8)
- Starting a reschedule (DEF-1)
- Promo codes

</domain>

<decisions>
## Implementation Decisions

Carried forward (not re-opened):
- **iOS is the source of truth (DEC-ios-source-of-truth).** Deviate for Android conventions, or where iOS has a defect.
- **Styling** is a Material 3 adaptation, dark only, in Nunito. Pixel parity is out of scope.
- **Route:** Phase 6 replaces the stub's body, not `BookingKey` (Phase 5 D-04). Book Session is still disabled when `is_bookable == false`.
- **Prices** use the shared Money formatter (`util/Money.kt`, Phase 5 D-18–D-20). Free topics show "Free" in green (Phase 5 D-21).
- **Times** follow the device's 12/24-hour setting via `DateFormat.getTimeFormat(context)` (Phase 5 WR-05). iOS hard-codes `h:mm a`; Android does not.
- **API** is ported feature by feature: `ApiRequestUrls` constants, `ClickMeApi` methods, and a Hilt `@Singleton` repository using `apiCall {}`. DTOs are Moshi codegen with an explicit `@Json(name=…)` on multi-word fields.

### Paid topics before Phase 7
- **D-01: Show every topic, and disable the CTA for paid ones.**
  - Paid topics stay in the picker with their prices.
  - When a paid topic is selected, Book Now is disabled, and a short note under it says paid booking is coming soon.
  - Phase 7 replaces this branch with the real payment flow, with no layout change.
  - Do not build Phase 7's "Add a Payment Method" wiring or the payment-info link now.
- **D-02: Preselect the first free topic.**
  - If the expert has no free topics, fall back to the first topic (iOS parity).
  - Changing the topic clears the selected slot. iOS regenerates `bookingAttemptId` here, which matters in Phase 7.
  - Phase 7 may switch this back to plain `topics.first`; that's for Phase 7 to decide.

### Calendar & slot display (BOOK-01, BOOK-02)
- **D-03: Mark days that have open slots.** A day gets a small dot when it has at least one slot that is available, not held, and still in the future. The screen still opens on today (iOS parity), with no auto-jump to the first open day. This is an Android addition; iOS shows no markers.
- **D-04: Unavailable slots are dimmed and can't be tapped (iOS parity).** A slot is unavailable when `available == false`, `held == true`, or its start is already past. It shows at low opacity. Unavailable slots are never hidden.
- **D-05: Show the timezone in a friendly form.**
  - Slot times are always in the client's local zone.
  - A note on the slots card says so, and names the expert's zone in readable form: the city from the IANA id plus the short zone name, for example "Times are in your time zone (EST). Expert is in Toronto (EDT)."
  - When the expert's zone and the client's zone match, show a simpler single line.
  - This replaces the raw IANA id that iOS shows.
- **D-06: Block months before the current one.** The previous-month arrow is disabled on the current month. Past days in the current month are disabled (iOS parity). Moving forward has no limit.
- **D-07: Load and regroup availability as iOS does (BOOK-02).**
  - Load the displayed month and its two neighbors.
  - Only the center month's failure is shown to the user; neighbor failures are silent.
  - Regroup every slot by the client-local `yyyy-MM-dd` of `startUtc`. Ignore the server's expert-local `day.date`.
  - Drop duplicates by start instant, and sort each day.
  - Changing the month cancels an in-flight load, so a stale response can't overwrite a newer month. iOS has this race; Android fixes it.
  - Changing the month clears the selected slot (iOS).

### Errors & meeting type
- **D-08: Offer both meeting types, and handle the 422.**
  - The options are In-app Voice Call (the default) and Skype/Zoom Call.
  - On `422 MEETING_LINK_UNAVAILABLE`, show the alert "This expert hasn't set up a Skype/Zoom link. Please choose In-app Voice instead." and switch the selection back to In-app Voice automatically.
  - The details endpoint doesn't say whether the expert has a link, so this can't be prevented up front.
- **D-09: On `409 SLOT_UNAVAILABLE`, alert, clear and reload.** Show "That time slot was just taken. Please pick another.", clear the selected slot, and re-fetch availability for the displayed month so the taken slot shows as unavailable.
- **D-10: Errors appear in a Material `AlertDialog` titled "Couldn't book" (iOS `.alert` swap).** Each error code gets a mapped message:

  | Code | Message |
  |------|---------|
  | `SLOT_UNAVAILABLE` | D-09 copy |
  | `EXPERT_NOT_BOOKABLE` | "This expert isn't accepting bookings right now." |
  | `MEETING_LINK_UNAVAILABLE` | D-08 copy |
  | `CANNOT_BOOK_SELF` | "You can't book yourself." |
  | `PAID_TOPIC_REQUIRES_PAYMENT` | Defensive; the D-01 gate should prevent it |
  | `DUPLICATE_BOOKING` | "A booking for this session already exists." |
  | Anything else | The server's `message` |

  Network errors use the app's standard network-error copy.
- **D-11: Request body matches the iOS free path exactly.**
  - `expert_id`, `topic_id`
  - `scheduled_start`: the slot's UTC instant in ISO 8601
  - `duration_minutes`: the topic's duration, default 30
  - `meeting_type`: `in_app_voice` or `skype_zoom`
  - `timezone`: the device's IANA id
  - `client_notes`: trimmed, and omitted when empty

  A tap while a request is already in flight is ignored. Book Now shows progress while the request runs.

### Request Sent & navigation (BOOK-04)
- **D-12: Request Sent replaces the booking screen in the back stack.** Back from Request Sent goes to Expert Profile, so the user can't re-submit from the filled-in form. This fixes an iOS defect: iOS pushes on top of the form.
- **D-13: "View My Bookings" clears the Explore stack and switches to the client Bookings tab.** That tab is still a placeholder until Phase 8 builds My Bookings; the wiring is final now. "Back to Home" clears the Explore stack back to its root.
- **D-14: Request Sent fetches `GET /client/bookings/:id` (iOS parity).**
  - The route carries only the booking id.
  - The screen shows a loading state, and an error state with Retry.
  - The details card shows the expert, the topic, and the local date and time range ("Oct 24, 2026 | 2:00 PM - 3:00 PM", with times in the device format).
  - Port the booking-detail DTO now, in the `mybookings` package; Phase 8 extends it.
  - For a free booking (`topic.is_free`), hide the payment-lifecycle card and use the free headline. Phase 7 adds the paid card.
- **D-15: Use a simple entry animation.** It runs once, about 0.7s: the check icon scales in, then the headline, card and buttons fade and slide up. Drop the glow pulse.

### Claude's Discretion
- Slot row layout: a horizontal chip row that scrolls to the first open slot (iOS), or a wrapping `FlowRow`.
- Skeletons, card styling, and the dot style on the calendar.
- The exact copy for the paid "coming soon" note, the free headline on Request Sent, and the timezone note.
- The topic picker as a `ModalBottomSheet` (platform swap) with the iOS row content: title, duration, price and description.
- The empty state when the expert has no topics, with Book Now disabled.
- How `Navigator` gains a replace-top operation for D-12, and the cross-tab "go to Bookings" call for D-13.
- Whether the calendar's week starts on Sunday (iOS) or follows the locale's first day of the week.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### iOS Make a Booking (source of truth)
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Make a Booking/MakeABookingViewModel.swift`: topics load, 3-month availability load and merge, `groupSlots` (rebucketing by `startUtc` and `held`), free-path `performFreeBooking`, `handlePaidFlowError` messages (D-10)
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Make a Booking/MakeABookingView.swift`: section order, `canBook`, alert, and post-success navigation
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Make a Booking/Subviews/`: CalendarCard, TimeSlotsCard (disabled rules, scroll to first open slot), TopicPicker and TopicSheet, MeetingTypeCard, NotesCard (500 limit), SummarySection, BookButton, ExpertCard. MakeABookingStripePaymentSheet and PaymentInfoSheet are Phase 7.
- `ios-reference/ClickMe2026/Model/UIModels/BookingTimeSlot.swift`, `BookingTopic.swift`, `MeetingType+Booking.swift`
- `ios-reference/ClickMe2026/Model/BookingCalendarPayload.swift`: availability response shape
- `ios-reference/ClickMe2026/Network/ClickMeAPI+Booking.swift`: `requestBooking`, `getExpertAvailability`, `getClientBookingDetail`

### iOS Request Sent (source of truth)
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Request Sent/BookingRequestSentView.swift` and `BookingRequestSentViewModel.swift`: data load, `isFree` handling, date and time label
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Request Sent/Subviews/`: CheckIcon, Headline, DetailsCard, Actions
- `ios-reference/ClickMe2026/Model/Responses/ClientBookingDetail.swift`: the DTO for D-14

### Backend contract
- `backend-reference/src/controllers/booking.controller.js` `createRequest` (around line 1283): the `POST /bookings/request` free path, its error codes (`EXPERT_NOT_BOOKABLE`, `PAID_TOPIC_REQUIRES_PAYMENT`, `MEETING_LINK_UNAVAILABLE`, `SLOT_UNAVAILABLE`), the `pending_approval` status with a 12h expiry, and the `booking_request` dispatch to the expert
- `backend-reference/docs/ios/SESSION-BUFFER.md`: slot and buffer rules behind availability
- `backend-reference/docs/ios/PRICING-LIMITS.md` §5: minor units and JPY (price display)

### Project
- `.planning/REQUIREMENTS.md`: BOOK-01 to BOOK-04; DEF-1 (reschedule deferred)
- `.planning/ROADMAP.md`: Phase 6 success criteria and planning notes (highest-risk week; test with the iOS app as the expert)
- `.planning/phases/05-explore-expert-profile/05-CONTEXT.md`: D-04 (`BookingKey`) and D-18 to D-21 (Money and Free)
- `CLAUDE.md`: platform swaps (`.sheet` → `ModalBottomSheet`, `.alert` → `AlertDialog`) and file organization

</canonical_refs>

<code_context>
## Existing Code Insights

All paths below are under `app/src/main/java/com/uptrendinvestments/clickmeapp/`.

### Reusable Assets
- `screens/clientmode/makeabooking/BookingKey.kt`: the existing route (expert id, name, title, image). Keep it.
- `screens/clientmode/makeabooking/MakeABookingStubScreen.kt`: replace its body.
- `screens/clientmode/expertprofile/ExpertProfileRepository.kt`: already calls `GET /experts/:id/details`. Reuse it for topics, or share its DTO.
- `screens/clientmode/mybookings/ClientBookingsRepository.kt`: the bookings repository. Add the booking-detail call here (D-14).
- `util/Money.kt`: the shared price formatter.
- `screens/clientmode/mybookings/TodaysSessions.kt` and `explore/subviews/ExploreTodaysSessionsSection.kt`: the pattern for formatting times with the device time format.
- `components/EmptyState.kt` (`EmptyState`, `ErrorState`), `components/avatar/Avatar.kt`, `PressScaleButton` and `AsyncSaveButton`.

### Established Patterns
- **API:** relative paths in `network/ApiRequestUrls.kt`, methods in `network/ClickMeApi.kt`, and `apiCall {}` in a `@Singleton` repository. The error body is parsed into a code and a message by the existing error mapping; reuse it for D-10.
- **Navigation:** `navigation/Navigator.kt` has `navigate`, `goBack`, `reselect` and `navigateInTab`, but no replace-top. D-12 needs one, and D-13 needs a cross-tab switch to `ClientBookingsRoot`.
- **The client shell's `entryProvider`** is in `screens/clientmode/home/HomeClientScreen.kt`. Register the Request Sent key there.

### Integration Points
- `screens/clientmode/home/HomeClientScreen.kt`: `entry<ClientBookingsRoot>` is the D-13 target, and still a placeholder.
- New files mirror the iOS tree: `screens/clientmode/makeabooking/…` (screen, ViewModel, subviews) and `screens/clientmode/requestsent/…`.

</code_context>

<specifics>
## Specific Ideas

- The day markers and the friendly timezone note (D-03, D-05) exist because the local-time conversion is the riskiest part of this phase. They make cross-timezone results easy to check by eye during the test with the iOS app as the expert.
- Verification setup: the iOS expert sets a different timezone and has slots near midnight at month edges. The checks:
  - Android shows each slot under the correct local date, with no duplicates across neighbor months.
  - Held slots are dimmed.
  - A free booking reaches the iOS expert.

</specifics>

<deferred>
## Deferred Ideas

- The paid CTA, the payment-methods gate, the "How does payment work?" sheet, and the payment-lifecycle card on Request Sent are all Phase 7.
- Auto-jumping to the first open day was considered and not chosen; it can be revisited after Phase 6.
- The full iOS staggered animation with the glow pulse was dropped (BOOK-04 marks the animation optional).

</deferred>

---

*Phase: 06-booking-flow-free-bookings*
*Context gathered: 2026-10-22*
