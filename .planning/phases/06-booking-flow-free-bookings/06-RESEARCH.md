# Phase 6: Booking Flow & Free Bookings - Research

**Researched:** 2026-10-22
**Domain:** Android Jetpack Compose port of the iOS Make a Booking screen (topic, custom month calendar, client-local slot regrouping, free `POST /bookings/request`) and the Request Sent screen. It adds three REST calls, one repository, two assisted-injection ViewModels, and two Navigator operations.
**Confidence:** HIGH. The backend contract was read from the controller, middleware, schema and service source. The iOS behavior was read from the Swift source. The Android patterns were read from the Phase 1-5 code. No new external packages are needed.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

Carried forward (not re-opened):
- **iOS is the source of truth (DEC-ios-source-of-truth).** Deviate for Android conventions, or where iOS has a defect.
- **Styling** is a Material 3 adaptation, dark only, in Nunito. Pixel parity is out of scope.
- **Route:** Phase 6 replaces the stub's body, not `BookingKey` (Phase 5 D-04). Book Session is still disabled when `is_bookable == false`.
- **Prices** use the shared Money formatter (`util/Money.kt`, Phase 5 D-18–D-20). Free topics show "Free" in green (Phase 5 D-21).
- **Times** follow the device's 12/24-hour setting via `DateFormat.getTimeFormat(context)` (Phase 5 WR-05). iOS hard-codes `h:mm a`; Android does not.
- **API** is ported feature by feature: `ApiRequestUrls` constants, `ClickMeApi` methods, and a Hilt `@Singleton` repository using `apiCall {}`. DTOs are Moshi codegen with an explicit `@Json(name=…)` on multi-word fields.

#### Paid topics before Phase 7
- **D-01: Show every topic, and disable the CTA for paid ones.**
  - Paid topics stay in the picker with their prices.
  - When a paid topic is selected, Book Now is disabled, and a short note under it says paid booking is coming soon.
  - Phase 7 replaces this branch with the real payment flow, with no layout change.
  - Do not build Phase 7's "Add a Payment Method" wiring or the payment-info link now.
- **D-02: Preselect the first free topic.**
  - If the expert has no free topics, fall back to the first topic (iOS parity).
  - Changing the topic clears the selected slot. iOS regenerates `bookingAttemptId` here, which matters in Phase 7.
  - Phase 7 may switch this back to plain `topics.first`; that's for Phase 7 to decide.

#### Calendar & slot display (BOOK-01, BOOK-02)
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

#### Errors & meeting type
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

#### Request Sent & navigation (BOOK-04)
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

### Deferred Ideas (OUT OF SCOPE)
- The paid CTA, the payment-methods gate, the "How does payment work?" sheet, and the payment-lifecycle card on Request Sent are all Phase 7.
- Auto-jumping to the first open day was considered and not chosen; it can be revisited after Phase 6.
- The full iOS staggered animation with the glow pulse was dropped (BOOK-04 marks the animation optional).

Also out of scope (CONTEXT domain boundary): the paid paths (payment-intent, SetupIntent, PaymentSheet, long-lead, 3DS, payment-methods gate), My Bookings / Upcoming Booking Details (Phase 8), starting a reschedule (DEF-1), promo codes.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| BOOK-01 | Client can make a booking by picking a topic, then a day on a custom month calendar and a slot, then a meeting type and optional notes. | iOS `MakeABookingView` section order and `canBook`; `/experts/:id/details` topics (already parsed by `PublicProfileDetailsData`); pure month-grid function; selection-clearing rules; `SavedStateHandle` persistence; Compose building blocks that already exist (`PickerSheet`, `SectionHeader`, `SkeletonGroup`). |
| BOOK-02 | Slots load for the previous, current and next month. They are regrouped by the client's local date using `startUtc`, with duplicates dropped. The expert's timezone is shown, and `held` slots can't be selected. | Verified wire shape of `GET /experts/:id/availability` (it is `start_utc`, `end_utc`, `available`; `held` is not sent). `groupSlots` pure function with `ZoneId` parameter. Parallel three-month fetch with latest-wins cancellation. Timezone note builder. Golden cross-timezone test cases. |
| BOOK-03 | Client can book a free topic (`POST /bookings/request`). | Verified controller/schema/middleware contract: strict body, `duration_minutes` multiple of 30, error codes and statuses, 12 h `pending_approval` expiry, `booking_request` dispatch. Error-to-message map. Single-flight and no-null-serialization behavior of the Moshi converter. |
| BOOK-04 | After sending a booking request, the client sees the Request Sent confirmation screen. The staggered animation is optional. | `GET /client/bookings/:id` verified shape. `Navigator.replaceTop` and a reset-and-select operation. Assisted ViewModel keyed by `RequestSentKey`. Entry animation recipe, played once. |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- Jetpack Compose + Material 3, MVVM with Compose State, Kotlin coroutines (no RxJava), Hilt, Retrofit + Moshi. minSdk 26, targetSdk 37, compileSdk 37. (`java.time` is native at minSdk 26, no desugaring needed.)
- iOS under `ios-reference/` is the source of truth for behavior. Deviate only for Android conventions: `.sheet` becomes `ModalBottomSheet`, `.alert` becomes `AlertDialog`, back chevron becomes `TopAppBar` back arrow, `.task` becomes `LaunchedEffect(Unit)`.
- File organization mirrors the iOS folder tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/`: `screens/clientmode/makeabooking/…` and `screens/clientmode/requestsent/…`. Each screen is `XScreen.kt` plus `XViewModel.kt`.
- Backend contract is the same REST contract as iOS. Specs are in `backend-reference/docs/ios/`.
- Project memory: Material styling is acceptable; do not block on human checkpoints (log them as open items); worktree executors need `local.properties` and absolute iOS paths; JDK is the Android Studio JBR.
- CI gates carried from Phase 1: `scripts/check-design-rules.sh` (no `Color(...)` literals outside `ui/theme/Color.kt`, no `*Brand` types, no Stripe secret key patterns), ktlint, lint with `abortOnError = true`, no `fontSize` overrides, all copy in string resources.
- UI-SPEC (approved) fixes layout, copy, tokens, semantics and motion. This research does not restate it; it supplies the technical contract the UI-SPEC depends on.

## Summary

Phase 6 is mostly client-side logic around one subtle data problem: the server buckets availability by the expert's local day, but the client must show slots under its own local date. The backend source confirms exactly what iOS assumes. `GET /experts/:id/availability?month=YYYY-MM` builds the grid per **expert-local** calendar day, emits each slot as `start_utc`, `end_utc`, `start_local`, `end_local`, `available`, and never filters out past slots. The right answer is therefore: fetch month M-1, M and M+1, flatten every slot, drop duplicates by `start_utc`, and group by `startUtc.atZone(deviceZone).toLocalDate()`. This should be a pure, zone-parameterised function with golden tests, because it is the riskiest logic in the phase (ROADMAP: "highest-risk week").

The backend source also shows several things CONTEXT and iOS do not mention, and two of them touch locked decisions:
1. **`/experts/:id/details` already returns `available_meeting_types`** (`["in_app_voice"]` or `["in_app_voice","skype_zoom"]`), and the Android DTO `PublicProfileDetailsData.availableMeetingTypes` already parses it. D-08's premise ("the details endpoint doesn't say whether the expert has a link") is therefore incorrect. D-08 as written (offer both, handle the 422) still works; hiding Skype/Zoom when the list excludes it is a small improvement that needs a user decision (Open Question 1).
2. **The availability slot has no `held` field on the wire.** Redis-held slots are folded into `available: false`. The OpenAPI file documents `held` and `start_time`, but the code emits neither. `held` stays an optional DTO field (iOS parity); `available` is what actually blocks held slots.
3. **The availability endpoint takes an optional `topic_id`** that makes the server compute the grid with the topic's duration. iOS never sends it, so a 60-minute topic is shown the 30-minute grid, and the server's booking check (Redis hold plus DB conflict only, no availability-window check) can answer 409 for a slot that looked open. This is a quality issue, not a blocker (Open Question 2).
4. **`POST /bookings/request` is `.strict()`** (an unknown key is a 422). It requires `duration_minutes` to be a positive multiple of 30, accepts `client_notes` up to 1000 characters, and rejects `null` for `client_notes`. The Moshi converter omits nulls, which is exactly right, but a test should lock it in.
5. The server does **not** reject past `scheduled_start` values. The client must guard against past slots.

No new libraries are needed. The structural work is: move the `BookingKey` entry out of the Hilt-free shell into `detailEntries` (it now needs an assisted ViewModel), add `RequestSentKey`, and add two Navigator operations (replace-top, and reset-tabs-and-select).

**Primary recommendation:** Build in this order: (1) DTOs, `ApiRequestUrls`, `ClickMeApi`, `FakeClickMeApi` and decoding tests; (2) the pure logic (`groupSlots`, month grid, timezone note, time formatting) with golden tests, before any UI; (3) `Navigator.replaceTop` and `Navigator.resetAndSelect` with tests; (4) `BookingRepository` and `MakeABookingViewModel` (assisted, with `SavedStateHandle`); (5) the booking screen subviews; (6) Request Sent; (7) shell wiring plus updated `HomeShellsTest`, delete the stub test. Treat the cross-timezone grouping and the request-body shape as the two things that must be proven by automated tests, because the device test needs an iOS expert in another timezone and can only run later.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Slot grid generation, buffers, held slots | API / Backend (`generateSlotsForMonth`) | — | Server owns availability, the 30-min buffer, and Redis holds. Client only renders `available`. |
| Regrouping slots by client-local date | Client (pure function + `ZoneId`) | — | The device zone is unknown to the server (it uses the profile zone). `start_utc` is the only reliable key (D-07). |
| Past-slot dimming and open-day dots | Client | — | The server never filters past slots; "now" and the device zone are client facts. |
| Topic list, price, duration | API (`/experts/:id/details`) | Client (`Money`, `PriceDisplay`) | Display only. The server re-derives free vs paid at booking time. |
| Free-vs-paid booking gate | Client (D-01 display gate) | API (`422 PAID_TOPIC_REQUIRES_PAYMENT`) | Client disables the CTA; the server is the real gate. |
| Meeting link availability | API (`available_meeting_types`, `422 MEETING_LINK_UNAVAILABLE`) | Client | Server is authoritative; client may pre-filter (Open Question 1). |
| Slot conflict resolution | API (Redis hold + DB exclusion constraint → `409`) | Client (alert, clear, reload) | Race-safe only on the server. |
| Booking creation, 12 h expiry, push to expert | API (`createRequest`, `dispatchBookingEvent`) | — | Android only sends the request; the iOS expert gets the `booking_request` push from the server. |
| Notes length cap, trim | Client (500 cap, UX) | API (`max(1000)`) | Client cap is parity with iOS; server is the real validator. |
| Request Sent data | API (`GET /client/bookings/:id`, scoped by JWT) | Client (format dates in device zone) | Server returns UTC instants; formatting is local. |
| Back-stack replace and cross-tab reset | Client (`Navigator`) | — | Pure navigation state. |

## Standard Stack

No new dependencies. All versions are from `gradle/libs.versions.toml` in this repo. [VERIFIED: codebase]

### Core (already present)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose BOM / Material 3 | 2026.09.00 | `ModalBottomSheet`, `AlertDialog`, `FlowRow`, `RadioButton`, `Crossfade`, `Modifier.selectable` | Project baseline. `FlowRow` is stable (Phase 5 research, cited). |
| Navigation 3 (`navigation3-runtime/ui`) | 1.2.0 | Per-tab back stacks, `entry<Key>` | Phase 1. `NavBackStack` is a snapshot list, so `removeAt` + `add` in one call is one visible change. |
| `lifecycle-viewmodel-navigation3` | 2.11.0 | One ViewModel per `NavEntry` | Already installed by `toDecoratedEntries`. |
| `androidx.hilt:hilt-lifecycle-viewmodel-compose` + Hilt | 1.4.0 / 2.60.1 | `hiltViewModel<VM, Factory>(creationCallback)` | Same pattern as `ExpertProfileViewModel`. |
| Retrofit / OkHttp / Moshi codegen | 3.0.0 / 5.5.0 / 1.15.2 | REST + JSON | Established. `InstantAdapter` already parses `…000Z`, `+00:00` and microsecond fractions. |
| `java.time` | native (minSdk 26) | `ZoneId`, `YearMonth`, `LocalDate`, `WeekFields`, `Instant` | No desugaring needed. |
| kotlinx-coroutines | 1.11.0 | Parallel `async`, latest-wins `Job` | Established. |

### Supporting (test)
| Library | Version | Purpose |
|---------|---------|---------|
| JUnit 4 / Robolectric / Compose ui-test-junit4 / coroutines-test / mockwebserver3 | 4.13.2 / 4.17 / BOM / 1.11.0 / 5.5.0 | All Phase 6 tests, same harness as Phase 5 (`FakeClickMeApi`, `MainDispatcherRule`, `MutableClock`). |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Hand-built month grid (`Column` of `Row`s) | `LazyVerticalGrid` | Rejected. A lazy grid inside a vertically scrolling parent throws an infinite-height measure exception. 42 cells do not need laziness. |
| Third-party calendar library (Kizitonwose, etc.) | Custom grid | Rejected. The UI-SPEC needs a specific cell (48dp, dot, ring), the logic is about 25 lines, and there is no dependency to vet. |
| `Column + verticalScroll` for the screen | `LazyColumn` | Either is allowed by the UI-SPEC. Prefer `Column + verticalScroll`: eight fixed sections, `BringIntoViewRequester` works the same, and the `FlowRow` and calendar are not lazy anyway. |
| `TimeZone.getDisplayName(daylight, SHORT)` | `DateTimeFormatter.ofPattern("zzz")` | Both give EST/EDT. UI-SPEC names the first; use it. |

**Installation:** none.

## Package Legitimacy Audit

This phase installs **no external packages**. Every dependency used is already in `gradle/libs.versions.toml` and was audited in earlier phases. slopcheck does not apply.

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 Expert Profile ──(Book Session: navigate(BookingKey))──────────────────────────────────────────────┐
                                                                                                     ▼
 ┌────────────────────────── Client shell (HomeClientShell, Nav3, per-tab stacks) ───────────────────────────┐
 │  Explore stack: [ExploreRoot, ExpertProfileKey, BookingKey]                                                │
 │                                                                                                            │
 │  BookingKey entry ──► MakeABookingScreen ◄── MakeABookingViewModel (assisted by BookingKey,                │
 │   (BottomBar.hidden)     │  sections 1-8       SavedStateHandle: topicId, date, month, slot, meeting, notes)│
 │                          │                          │                                                      │
 │                          │            ┌─────────────┼───────────────────────────────┐                      │
 │                          │            ▼             ▼                               ▼                      │
 │                          │   ExpertProfileRepository   BookingRepository      Clock + ZoneId.systemDefault │
 │                          │   .details(id) ─► topics    .availability(id,ym)   (today, past-slot, zone)     │
 │                          │            │                .requestBooking(body)                               │
 │                          │            ▼                         │                                          │
 │                          │   GET /experts/:id/details           ├─► GET /experts/:id/availability?month=   │
 │                          │                                      │      center ┐                            │
 │                          │                                      │      prev   ├ async, center error shown  │
 │                          │                                      │      next   ┘ neighbors silent           │
 │                          │                                      │      ─► flatten days ─► dedupe by        │
 │                          │                                      │         start_utc ─► groupBy             │
 │                          │                                      │         atZone(device).toLocalDate()     │
 │                          │                                      └─► POST /bookings/request (free only)     │
 │                          │                                              │ 201 {booking_id}                  │
 │                          │                          409 SLOT_UNAVAILABLE│ 422 MEETING_LINK_UNAVAILABLE ...  │
 │                          │                                  │           │                                   │
 │                          │         AlertDialog "Couldn't book" ◄────────┤ (409: clear slot + reload month;  │
 │                          │                                              │  422 link: meeting := in-app)     │
 │                          ▼                                              ▼                                   │
 │   Book Now (single flight) ──────────────────────────► navigator.replaceTop(RequestSentKey(bookingId))    │
 │                                                                         │                                   │
 │  Explore stack: [ExploreRoot, ExpertProfileKey, RequestSentKey]  ◄──────┘                                   │
 │   RequestSentKey entry ──► RequestSentScreen ◄── BookingRequestSentViewModel (assisted by key)             │
 │                                 │                          └─► ClientBookingsRepository.detail(id)         │
 │                                 │                                 └─► GET /client/bookings/:id             │
 │                                 ├─ "View My Bookings" ─► navigator.resetAndSelect(ClientBookingsRoot,       │
 │                                 │                                   reset = {ExploreRoot, ClientBookingsRoot})│
 │                                 └─ "Back to Home"     ─► navigator.reselect(ExploreRoot)                   │
 └────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
 Server side (not Android): createRequest ─► pending_approval (12 h expiry) ─► dispatchBookingEvent('booking_request')
                                              ─► push to the iOS expert
```

### Recommended Project Structure
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── model/responses/
│   ├── ExpertAvailabilityData.kt        # NEW: availability DTOs (start_utc, end_utc, available, held)
│   ├── BookingRequestData.kt            # NEW: POST /bookings/request response
│   └── ClientBookingDetail.kt           # NEW: GET /client/bookings/:id (Phase 8 extends)
├── model/requests/
│   └── BookingRequestBody.kt            # NEW: strict request body
├── network/
│   ├── ApiRequestUrls.kt                # + EXPERT_AVAILABILITY, BOOKING_REQUEST, CLIENT_BOOKING_DETAIL
│   └── ClickMeApi.kt                    # + getExpertAvailability, requestBooking, getClientBookingDetail
├── navigation/Navigator.kt              # + replaceTop, resetAndSelect
├── screens/clientmode/makeabooking/
│   ├── BookingKey.kt                    # unchanged
│   ├── MakeABookingScreen.kt            # RENAMED from MakeABookingStubScreen (stub deleted)
│   ├── MakeABookingViewModel.kt         # assisted by BookingKey
│   ├── MakeABookingUiState.kt
│   ├── BookingRepository.kt             # availability + requestBooking
│   ├── BookingSlots.kt                  # PURE: groupSlots, mergeAvailability, BookingTimeSlot
│   ├── BookingCalendar.kt               # PURE: monthCells, weekday order, isPast, month bounds
│   ├── BookingTimezoneNote.kt           # PURE: city + abbreviation builder
│   ├── BookingErrors.kt                 # ApiException -> BookingError (message + side effect)
│   └── subviews/                        # one composable per file, names per UI-SPEC
├── screens/clientmode/requestsent/
│   ├── RequestSentKey.kt                # @Serializable NavKey(bookingId)
│   ├── BookingRequestSentScreen.kt
│   ├── BookingRequestSentViewModel.kt   # assisted by RequestSentKey
│   └── subviews/                        # CheckIcon, Headline, DetailsCard, Actions
└── screens/clientmode/mybookings/
    └── ClientBookingsRepository.kt      # + detail(id)
```

### Pattern 1: Pure grouping, zone as a parameter
**What:** One function turns raw server days into `Map<LocalDate, List<BookingTimeSlot>>`. It takes the `ZoneId` as an argument, never reads `ZoneId.systemDefault()` itself, so tests can pass any zone.
**When to use:** Always for BOOK-02. The ViewModel calls it with `ZoneId.systemDefault()`.
**Example:**
```kotlin
// Source: ports iOS MakeABookingViewModel.groupSlots + the merge/dedupe in loadAvailability; D-07
data class BookingTimeSlot(val start: Instant, val end: Instant?, val isAvailable: Boolean)

fun groupSlots(days: List<AvailabilityDayDto>, zone: ZoneId): Map<LocalDate, List<BookingTimeSlot>> {
    val byStart = LinkedHashMap<Instant, BookingTimeSlot>()
    for (day in days) {
        for (s in day.slots.orEmpty()) {
            val start = s.startUtc ?: continue                       // iOS: guard let start
            val ok = (s.available ?: true) && s.held != true         // iOS: (available ?? true) && !(held ?? false)
            val prev = byStart[start]
            // Duplicate instants: a slot is bookable only if every copy says so (conservative).
            byStart[start] =
                if (prev == null) BookingTimeSlot(start, s.endUtc, ok)
                else prev.copy(isAvailable = prev.isAvailable && ok)
        }
    }
    return byStart.values.sortedBy { it.start }.groupBy { it.start.atZone(zone).toLocalDate() }
}

// Caller: flatten prev + center + next days into ONE list, then one groupSlots call.
// val days = listOfNotNull(prev, center, next).flatMap { it.days }
```
Notes: ignore `day.date` entirely. `groupBy` keeps the sorted order inside each key. Do not key the map by `String`; `LocalDate` removes the `yyyy-MM-dd` formatting step and any locale/calendar-system hazard (iOS needed `en_US_POSIX` for this).

### Pattern 2: Parallel three-month load with latest-wins and silent neighbors
**What:** Center failure is the only visible failure; neighbors return `null` on `ApiException`. A new month cancels the previous `Job`.
**Example:**
```kotlin
// Source: ports iOS loadAvailability, D-07 (parallel instead of sequential; stale-write race fixed)
private var availabilityJob: Job? = null

private fun loadAvailability(month: YearMonth, keepVisible: Boolean) {
    availabilityJob?.cancel()
    if (!keepVisible) _state.update { it.copy(availability = AvailabilityState.Loading) }
    availabilityJob = viewModelScope.launch {
        try {
            val merged = coroutineScope {
                val center = async { repository.availability(expertId, month) }          // throws ApiException
                val prev = async { neighbor(month.minusMonths(1)) }
                val next = async { neighbor(month.plusMonths(1)) }
                Triple(prev.await(), center.await(), next.await())
            }
            val days = listOfNotNull(merged.first, merged.second, merged.third).flatMap { it.days }
            if (_state.value.displayedMonth != month) return@launch                      // belt and braces
            _state.update {
                it.copy(
                    availability = AvailabilityState.Loaded(groupSlots(days, ZoneId.systemDefault())),
                    expertZoneId = merged.second.expertTimezone,
                )
            }
        } catch (e: ApiException) {            // apiCall() rethrows CancellationException untouched
            _state.update { it.copy(availability = AvailabilityState.Failed(e.toMessage())) }
        }
    }
}

private suspend fun neighbor(month: YearMonth): ExpertAvailabilityData? =
    try { repository.availability(expertId, month) } catch (e: ApiException) { null }
```
Do **not** use `runCatching` for the neighbors: it would swallow `CancellationException` and break latest-wins. `apiCall` already converts every non-cancellation `Throwable` to `ApiException`, so catching `ApiException` is complete.

### Pattern 3: Month grid without a lazy layout
**What:** A pure `monthCells(month, firstDayOfWeek): List<LocalDate?>` padded to full weeks, rendered as `Column { rows.forEach { Row { cells.forEach { Box(Modifier.weight(1f).height(48.dp)) } } } }`.
**Example:**
```kotlin
// Source: ports iOS generateDays; UI-SPEC "Day grid" (locale first day of week, empty out-of-month cells)
fun monthCells(month: YearMonth, firstDow: DayOfWeek): List<LocalDate?> {
    val lead = (month.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val used = lead + days.size
    val trail = (7 - used % 7) % 7
    return List(lead) { null } + days + List(trail) { null }
}
// firstDow = WeekFields.of(locale).firstDayOfWeek
// header order = (0..6).map { firstDow.plus(it.toLong()) }.map { it.getDisplayName(TextStyle.SHORT, locale) }
```
iOS has a different out-of-month quirk (shows dimmed numbers); the UI-SPEC deliberately empties them.

### Pattern 4: Assisted ViewModel plus saved state
**What:** Same wiring as `ExpertProfileViewModel`, plus selection state mirrored to `SavedStateHandle` so rotation and process death keep the form.
**Example:**
```kotlin
// Source: existing ExpertProfileViewModel + HomeClientScreen pattern; Hilt assisted injection docs
@HiltViewModel(assistedFactory = MakeABookingViewModel.Factory::class)
class MakeABookingViewModel @AssistedInject constructor(
    @Assisted private val key: BookingKey,
    private val savedState: SavedStateHandle,              // see Assumption A1 / Open Question 3
    private val profiles: ExpertProfileRepository,
    private val repository: BookingRepository,
    private val clock: Clock,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(key: BookingKey): MakeABookingViewModel }
}

// HomeClientScreen detailEntries (BookingKey MOVES here from HomeClientShell):
entry<BookingKey>(metadata = BottomBar.hidden()) { key ->
    val vm = hiltViewModel<MakeABookingViewModel, MakeABookingViewModel.Factory>(
        creationCallback = { it.create(key) },
    )
    MakeABookingScreen(viewModel = vm, onBack = navigator::goBack,
        onBooked = { navigator.replaceTop(RequestSentKey(it)) })
}
entry<RequestSentKey>(metadata = BottomBar.hidden()) { key -> /* assisted VM, same shape */ }
```
Persist as primitives: topic id (String), selected date (`LocalDate.toString()`), displayed month (`YearMonth.toString()`), slot start (epoch millis `Long`), meeting type (wire String), notes (String). After process death, re-apply a saved slot only if it appears in the freshly loaded slots and is still selectable.

### Pattern 5: Two Navigator operations
**Example:**
```kotlin
// Source: extends navigation/Navigator.kt (Phase 1); D-12, D-13. Tests go in NavigatorTest.
/** Replace the top of the current tab's stack (booking -> Request Sent). Never replaces a tab root. */
fun replaceTop(route: NavKey) {
    val stack = currentStack()
    check(stack.size > 1) { "Cannot replace a tab root" }
    state.lastTransition = NavTransition.Push
    stack.removeAt(stack.lastIndex)
    stack.add(route)
}

/** Pop every tab in [reset] to its root, then select [tab] (iOS showMyBookings: explorePath.reset(); bookingsPath.reset(); selectedTab = .bookings). */
fun resetAndSelect(tab: NavKey, reset: Collection<NavKey>) {
    require(tab in state.backStacks) { "Unknown tab: $tab" }
    reset.forEach { r ->
        val stack = state.backStacks[r] ?: error("Unknown tab: $r")
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
    state.lastTransition = NavTransition.TabSwitch
    state.topLevelRoute = tab
}
```
"Back to Home" needs no new operation: `navigator.reselect(ExploreRoot)` already pops the Explore stack to its root and keeps the Explore tab selected (iOS `returnToHome`).

### Anti-Patterns to Avoid
- **Reading the server's `day.date`:** it is the expert's local date. Ignoring it is the whole point of BOOK-02.
- **`Date.getTime() + 24h` arithmetic for days:** use `LocalDate` and `YearMonth`. DST days are 23 or 25 hours long (US fall-back is Sunday 2026-11-01).
- **`ZoneId.systemDefault()` inside pure functions:** it makes the cross-timezone tests impossible.
- **Registering `entry<BookingKey>` twice** (once in the shell, once in `detailEntries`): remove it from `HomeClientShell`.
- **`LazyVerticalGrid` for the calendar** inside a scrolling parent.
- **`runCatching` around suspend network calls:** it catches `CancellationException`.
- **Serializing `null` `client_notes`:** the strict schema rejects it. Leave the field nullable with no default-serialization override.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| ISO-8601 timestamp parsing | A regex or `SimpleDateFormat` | Existing `InstantAdapter` (`OffsetDateTime.parse`) | Already handles `…000Z`, `+00:00`, microseconds. iOS needed a regex; Android does not. |
| Currency formatting | New price formatting | `Money.format` / `PriceDisplay.of` / `PriceText` | Phase 5 D-18–D-21 (JPY, "Free" in green). |
| Error body to code mapping | Parsing error JSON in the ViewModel | `ApiException.Http.code` (`ErrorCode`) and `userMessage` | `SlotUnavailable`, `MeetingLinkUnavailable`, `ExpertNotBookable`, `CannotBookSelf`, `PaidTopicRequiresPayment`, `DuplicateBooking`, `ExpertUnavailable` already exist in `ErrorCode`. |
| Suspended-account handling | A booking-specific dialog | `ApiException.toDialogError()` semantics (returns null for `ACCOUNT_SUSPENDED`, which the global dialog owns) | Phase 2 D-08. |
| Time-of-day labels | `SimpleDateFormat("h:mm a")` | `DateFormat.getTimeFormat(context)` cloned with the zone (pattern in `formatSessionTimeRange`) | 12/24-hour setting (Phase 5 WR-05). |
| Localized date lines | Hard-coded patterns | `DateFormat.getBestDateTimePattern(locale, "EEEE, MMMM d")` + `DateTimeFormatter.ofPattern(p, locale)`; `FormatStyle.MEDIUM` for "Oct 24, 2026" | Locale-correct. |
| Week start and weekday names | Fixed "Su Mo Tu…" | `WeekFields.of(locale).firstDayOfWeek`, `DayOfWeek.getDisplayName(SHORT)` | UI-SPEC decision. |
| Topic sheet | A new sheet component | `PickerSheet` with custom `item {}` rows | UI-SPEC. |
| Skeletons, section errors, empty state | New loading UI | `SkeletonBlock`/`SkeletonGroup`, `SectionError`, `EmptyState` | Phase 5 components. |
| Press-scale buttons | New button wrapper | `PressScaleButton` (`filledColors`, `tonalColors`) | Phase 1. |
| The error dialog | A new `AlertDialog` | `AuthErrorDialog(title = "Couldn't book")` (or lift it to `components/` unchanged) | UI-SPEC. |
| Back-stack serialization | Manual save/restore of the stack | `rememberNavBackStack` (already used) | Survives process death. |

**Key insight:** every hard problem here (timestamp parsing, error codes, money, sheets, skeletons) already has a Phase 1-5 answer. The only genuinely new logic is the grouping function, the month grid, the timezone note, and two Navigator operations. All four are small, pure, and testable.

## Common Pitfalls

### Pitfall 1: Trusting `day.date` or `start_local`
**What goes wrong:** A slot the server files under `2026-11-01` (expert-local) is `2026-10-31` on the client. Slots appear on the wrong day or vanish at month edges.
**Why it happens:** `generateSlotsForMonth` iterates **expert-local** days (`DateTime.local(year, mon, day, { zone: expertTzId })`). `start_local`/`end_local` use the **profile** timezone, which may differ from the device zone.
**How to avoid:** Group only by `startUtc.atZone(ZoneId.systemDefault()).toLocalDate()`. Fetch M-1, M, M+1 so both month edges are complete.
**Warning signs:** First or last day of a month missing slots; a slot appearing twice; the date line disagreeing with the chip time.

### Pitfall 2: Expecting `held` or `start_time` on the wire
**What goes wrong:** Code that requires `held` or `start_time` finds nothing; held slots look selectable if the code checks only `held`.
**Why it happens:** OpenAPI documents `start_time`, `end_time`, `held`, but the controller emits `start_utc`, `end_utc`, `start_local`, `end_local`, `available`. Redis-held slots become `available: false`.
**How to avoid:** DTO fields `start_utc`, `end_utc`, `available`, `held` all optional; selectable = `(available ?: true) && held != true && start > now`. A decoding test must use a payload copied from the generator, not from OpenAPI.
**Warning signs:** Every slot decoding as `startUtc == null` (and the grid being empty).

### Pitfall 3: Past slots are not filtered by the server
**What goes wrong:** Today's earlier slots, and the whole earlier part of the month, show as bookable. A tap books the past. The server's `createRequest` does not validate `scheduled_start` against now.
**How to avoid:** D-04 dimming plus a guard in `book()`: if `slot.start <= clock.instant()`, do not call the API; clear the slot. Use the injected `Clock` for "now".

### Pitfall 4: Stale response overwrites a newer month
**What goes wrong:** The user taps next-next quickly; the older request returns last and replaces the grid.
**How to avoid:** Cancel the previous `Job` on every load, and re-check `displayedMonth == month` before writing state (Pattern 2). Test with a fake API that suspends the first call.

### Pitfall 5: Double submit and Back during a request
**What goes wrong:** A double tap sends two requests; the second gets `409 SLOT_UNAVAILABLE` (the first booking occupies the slot), which looks like a failure after a success. Back during a request cancels `viewModelScope`, so the user never sees the confirmation although the booking exists.
**How to avoid:** `if (state.isSubmitting) return` at the top of `book()` (D-11). While submitting, swallow Back with `BackHandler(enabled = isSubmitting) {}` and ignore the app-bar arrow. (Recommendation; the UI-SPEC does not cover this case. It is a few lines and removes a confusing state.)

### Pitfall 6: Strict request body
**What goes wrong:** A `422 VALIDATION_ERROR` from an extra key, a `null` `client_notes`, or `duration_minutes` that is not a multiple of 30.
**How to avoid:** Request class with exactly the seven fields; `client_notes` nullable with no default so Moshi omits it; trim before send and send `null` when blank. A MockWebServer test asserts the exact JSON and a second one asserts `client_notes` absence. `duration_minutes` = `topic.durationMins ?: 30`.

### Pitfall 7: `BookingKey` entry lives in the Hilt-free shell
**What goes wrong:** `HomeClientShell` registers `entry<BookingKey>` with the stub and has no Hilt access. A real screen needs an assisted ViewModel, and a second registration is ambiguous.
**How to avoid:** Delete the shell entry, register `BookingKey` and `RequestSentKey` in `detailEntries` inside `HomeClientScreen`. Rewrite `HomeShellsTest.bookingRouteShowsStubWithoutBottomBarAndBackRestoresIt` to register a fake `entry<BookingKey>(metadata = BottomBar.hidden())` through its existing `detail` parameter. Delete `MakeABookingStubScreenTest`, remove the `make_a_booking_coming_soon_*` strings.

### Pitfall 8: Notes cap splitting a surrogate pair
**What goes wrong:** `text.take(500)` can cut an emoji in half and leave a lone surrogate, which then serializes as `\uD83D` and may be rejected or mangled.
**How to avoid:** When truncating, if `Character.isHighSurrogate(text[499])` drop that last unit. iOS counts grapheme clusters, Android counts UTF-16 units, so the visible counter can differ slightly on emoji; the server limit (1000) is not at risk.

### Pitfall 9: Timezone abbreviations and unknown zone ids
**What goes wrong:** `ZoneId.of(expertTimezone)` throws for an unknown id; `Asia/Calcutta` vs `Asia/Kolkata` string-compare unequal on the same zone; some zones print `GMT+9`.
**How to avoid:** Wrap `ZoneId.of` in try/catch and fall back to the single-line note ("expert zone unknown"). Compare by id; a redundant two-zone note is harmless. Do not special-case `GMT+9` (UI-SPEC Open Item 2). Compute the abbreviation for the **selected date** (`TimeZone.inDaylightTime`), so EST and EDT switch correctly.

### Pitfall 10: Month change keeps the selected day
**What goes wrong:** The selected day can be in another month than the displayed one. After replacing the availability map on month change, the selected date's slots are gone, so the slots card shows "No available slots on this day."
**Why it happens:** iOS semantics (also the UI-SPEC rule) keep `selectedDate` and replace `availabilityByDate` with the new three-month merge.
**How to avoid:** Follow iOS and the UI-SPEC; the date line on the slots card removes the ambiguity (UI-SPEC Open Item 1). Do not add a raw-month cache without a decision, because it can show stale taken slots.

### Pitfall 11: `entry<>` ViewModel scope after `replaceTop`
**What goes wrong:** Expecting the booking ViewModel (and the form) to survive after Request Sent replaces it.
**How to avoid:** That is intended (D-12). The `rememberViewModelStoreNavEntryDecorator` clears the `BookingKey` entry's store on removal. Request Sent re-fetches by id, so restore after process death works.

## Code Examples

### Request body and API surface
```kotlin
// Source: backend-reference/src/schemas/booking.schema.js requestSchema (.strict()); iOS ClickMeAPI+Booking.requestBooking
@JsonClass(generateAdapter = true)
data class BookingRequestBody(
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "topic_id") val topicId: String,
    @Json(name = "scheduled_start") val scheduledStart: String,      // Instant.toString(): "2026-10-24T14:00:00Z"
    @Json(name = "duration_minutes") val durationMinutes: Int,        // positive multiple of 30
    @Json(name = "meeting_type") val meetingType: String,             // "in_app_voice" | "skype_zoom"
    val timezone: String,                                             // ZoneId.systemDefault().id
    @Json(name = "client_notes") val clientNotes: String? = null,     // null is OMITTED by the converter; never send ""
)

// ClickMeApi (+ ApiRequestUrls: EXPERT_AVAILABILITY = "experts/{id}/availability",
//             BOOKING_REQUEST = "bookings/request", CLIENT_BOOKING_DETAIL = "client/bookings/{id}")
@GET(ApiRequestUrls.EXPERT_AVAILABILITY)
suspend fun getExpertAvailability(
    @Path("id") expertId: String,
    @Query("month") month: String,                                   // YearMonth.toString() == "2026-10"
    // @Query("topic_id") topicId: String? = null,                   // Open Question 2: backend supports it, iOS never sends it
): SuccessDataResponse<ExpertAvailabilityData>

@POST(ApiRequestUrls.BOOKING_REQUEST)
suspend fun requestBooking(@Body body: BookingRequestBody): SuccessDataResponse<BookingRequestData>

@GET(ApiRequestUrls.CLIENT_BOOKING_DETAIL)
suspend fun getClientBookingDetail(@Path("id") bookingId: String): SuccessDataResponse<ClientBookingDetail>
```
None of the three paths is in `publicPaths`, so they carry the bearer. The 201 from `/bookings/request` is a normal 2xx for Retrofit.

### Availability and detail DTOs (lenient)
```kotlin
// Source: backend availability.js generateSlotsForMonth output; availability.controller.js response; clientBooking.controller.js getDetail
@JsonClass(generateAdapter = true)
data class ExpertAvailabilityData(
    @Json(name = "expert_timezone") val expertTimezone: String? = null,
    @Json(name = "client_timezone") val clientTimezone: String? = null,
    val month: String? = null,
    val days: List<AvailabilityDayDto> = emptyList(),
)
@JsonClass(generateAdapter = true)
data class AvailabilityDayDto(val date: String? = null, val slots: List<AvailabilitySlotDto>? = null)
@JsonClass(generateAdapter = true)
data class AvailabilitySlotDto(
    @Json(name = "start_utc") val startUtc: Instant? = null,         // "2026-10-24T14:00:00.000Z"
    @Json(name = "end_utc") val endUtc: Instant? = null,
    val available: Boolean? = null,
    val held: Boolean? = null,                                       // not sent today; kept for iOS parity
)

@JsonClass(generateAdapter = true)
data class BookingRequestData(
    @Json(name = "booking_id") val bookingId: String,
    val status: String? = null,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class ClientBookingDetail(                                     // Phase 8 extends; unknown keys are ignored
    @Json(name = "booking_id") val bookingId: String,
    val status: String,
    @Json(name = "start_time") val startTime: Instant,              // "2026-10-24T14:00:00+00:00"
    @Json(name = "end_time") val endTime: Instant? = null,
    @Json(name = "meeting_type") val meetingType: String? = null,
    @Json(name = "expert_timezone") val expertTimezone: String? = null,
    @Json(name = "payment_status") val paymentStatus: String? = null,
    val topic: BookingDetailTopicDto,
    val expert: BookingDetailExpertDto,
    @Json(name = "client_notes") val clientNotes: String? = null,
)
@JsonClass(generateAdapter = true)
data class BookingDetailTopicDto(
    val id: String? = null, val title: String = "",
    @Json(name = "duration_mins") val durationMins: Int? = null,
    @Json(name = "is_free") val isFree: Boolean = false,            // topic level, sibling of price
    val price: TopicPriceDto? = null,                               // reuse; its is_free defaults false
)
@JsonClass(generateAdapter = true)
data class BookingDetailExpertDto(
    val id: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val title: String? = null,
)
```
End time for display: `endTime ?: startTime + (topic.durationMins ?: 30) minutes` (UI-SPEC).

### Error mapping with side effects
```kotlin
// Source: iOS handlePaidFlowError (free subset), D-08, D-09, D-10; ApiException.toDialogError precedent
sealed interface BookingFailure {
    data object Silent : BookingFailure                              // ACCOUNT_SUSPENDED: global dialog owns it
    data class Show(val message: FieldError, val clearSlotAndReload: Boolean = false,
                    val resetMeetingType: Boolean = false) : BookingFailure
}

fun ApiException.toBookingFailure(): BookingFailure = when (this) {
    is ApiException.Network -> BookingFailure.Show(FieldError.Res(R.string.login_error_network))
    is ApiException.Decoding -> BookingFailure.Show(FieldError.Res(R.string.login_error_unknown))
    is ApiException.Http -> when (code) {
        ErrorCode.AccountSuspended -> BookingFailure.Silent
        ErrorCode.SlotUnavailable -> BookingFailure.Show(FieldError.Res(R.string.booking_error_slot_taken), clearSlotAndReload = true)
        ErrorCode.MeetingLinkUnavailable -> BookingFailure.Show(FieldError.Res(R.string.booking_error_no_link), resetMeetingType = true)
        ErrorCode.ExpertNotBookable -> BookingFailure.Show(FieldError.Res(R.string.booking_error_not_bookable))
        ErrorCode.CannotBookSelf -> BookingFailure.Show(FieldError.Res(R.string.booking_error_self))
        ErrorCode.PaidTopicRequiresPayment -> BookingFailure.Show(FieldError.Res(R.string.booking_error_needs_payment))
        ErrorCode.DuplicateBooking -> BookingFailure.Show(FieldError.Res(R.string.booking_error_duplicate))
        else -> BookingFailure.Show(userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown))
    }
}
```
`EXPERT_UNAVAILABLE` (409, suspended or terminated expert) and `VALIDATION_ERROR` (422) fall through to the server message, which is display-ready ("This expert is not available for booking.").

### Timezone note
```kotlin
// Source: D-05, UI-SPEC "Timezone note"
fun timezoneNote(client: ZoneId, expertId: String?, on: LocalDate, locale: Locale): TimezoneNote {
    fun abbr(zone: ZoneId): String {
        val tz = TimeZone.getTimeZone(zone)
        val noon = on.atTime(12, 0).atZone(zone).toInstant()
        return tz.getDisplayName(tz.inDaylightTime(Date.from(noon)), TimeZone.SHORT, locale)
    }
    val expert = expertId?.let { runCatching { ZoneId.of(it) }.getOrNull() }
    return if (expert == null || expert.id == client.id) TimezoneNote.Same(abbr(client))
    else TimezoneNote.Different(abbr(client), expert.id.substringAfterLast('/').replace('_', ' '), abbr(expert))
}
```
(`runCatching` is fine here: no suspend call inside.)

### Sample availability payload for the decoding test (shape from the generator, not OpenAPI)
```json
{"status":"success","data":{"expert_timezone":"Pacific/Auckland","client_timezone":"UTC","month":"2026-11",
 "days":[{"date":"2026-11-01","slots":[
   {"start_utc":"2026-10-31T20:00:00.000Z","end_utc":"2026-10-31T20:30:00.000Z","start_local":"8:00 PM","end_local":"8:30 PM","available":true},
   {"start_utc":"2026-10-31T20:30:00.000Z","end_utc":"2026-10-31T21:00:00.000Z","start_local":"8:30 PM","end_local":"9:00 PM","available":false}]}]}}
```

### Entry animation (D-15), sketch
```kotlin
// 700 ms total, FastOutSlowIn; blocks keep their layout space (alpha 0) until their start time.
val played = rememberSaveable { mutableStateOf(false) }            // not replayed on rotation
var go by remember { mutableStateOf(played.value) }
LaunchedEffect(loaded) { if (loaded && !played.value) { go = true; played.value = true } }
val iconSpec = tween<Float>(300, 0, FastOutSlowInEasing)
val headline = tween<Float>(300, 150, FastOutSlowInEasing)          // card: delay 250, actions: delay 400
val iconAlpha by animateFloatAsState(if (go || played.value) 1f else 0f, iconSpec, label = "icon")
```
Buttons are not clickable while their alpha is 0 (`enabled = alpha == 1f` or `Modifier.alpha` plus a state gate).

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `SimpleDateFormat`/`Calendar` | `java.time` (`YearMonth`, `LocalDate`, `ZoneId`) | Native since API 26; minSdk is 26 | No desugaring, DST-safe date math. |
| Fragment navigation / Nav2 | Navigation 3 back stacks are plain snapshot lists | Nav3 1.0+ | Replace-top is a list operation, no `popUpTo`. |
| `SavedStateHandle.toRoute()` for args | Assisted injection with the `NavKey` | Nav3 recipes | The key is a constructor argument; `SavedStateHandle` is only for UI state. |

**Deprecated/outdated:**
- OpenAPI description of the availability slot (`start_time`, `end_time`, `held`): stale relative to the service code. Trust the code and the iOS decoder.
- CONTEXT D-08's claim that the details endpoint cannot say whether the expert has a Skype/Zoom link: stale. `available_meeting_types` was added in backend task 04-09.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `SavedStateHandle` can be a non-assisted constructor parameter of an `@AssistedInject` `@HiltViewModel`. Neither the Android Hilt page nor dagger.dev state it explicitly. | Pattern 4 | If it does not compile or resolve at runtime, mirror state to `rememberSaveable` in the screen instead, or build the handle with `createSavedStateHandle()`. Wave 0 should compile it first (Open Question 3). |
| A2 | Compose `tween`/`animate*AsState` animations honor the system animator duration scale (scale 0 = final state at once), as the UI-SPEC requires. | Entry animation | The "animations off" manual check would fail; an explicit `LocalContext` scale read would be needed. Low impact, manual check deferred. |
| A3 | `TimeZone.getDisplayName(daylight, SHORT, locale)` returns "EST"/"EDT" style abbreviations for common zones on Android's ICU data. | Timezone note | Some zones print "GMT+9". UI-SPEC Open Item 2 already accepts that. |
| A4 | The production backend currently runs the same `generateSlotsForMonth` and `createRequest` as the `backend-reference` checkout. | Everywhere | Wire shape could differ on a stale deploy. iOS decodes `start_utc` against production today, which supports the claim. |
| A5 | Topic durations on the server are always multiples of 30, so `duration_minutes` passes `multipleOf(30)`. | Request body | A 45-minute topic would 422 with a server message (shown by the generic branch). Not verified against the topic-create schema. |

## Open Questions

1. **D-08 premise: `available_meeting_types` exists.**
   - What we know: `/experts/:id/details` returns `available_meeting_types` (backend `meetingTypes.js`, task 04-09). `PublicProfileDetailsData.availableMeetingTypes` already parses it. iOS does not read it.
   - What's unclear: whether the user still wants both options always visible (D-08 as written) or wants Skype/Zoom hidden/disabled when the list is present and lacks it.
   - Recommendation: keep D-08's 422 handling and automatic reset regardless (it is the backstop). Put the pre-filter to the user as a one-line decision; it costs about five lines and removes a guaranteed dead end for experts with no link. Absent or null list = show both (old backend).

2. **Send `topic_id` with the availability request?**
   - What we know: the backend narrows the slot grid to the topic's duration when `?topic_id=` is present (consecutive cells, windows). iOS never sends it. `createRequest` checks only the Redis hold and DB conflict, not the availability window.
   - What's unclear: whether a 60-minute topic shown the 30-minute grid causes visible 409s or off-window bookings in practice.
   - Recommendation: D-07 says "as iOS does", so default to no `topic_id`. Add the optional `@Query("topic_id")` parameter to the API method now (free), and have the user decide whether a topic change should reload availability. If yes, a topic change then reloads three months and keeps the D-02 slot clearing.

3. **`SavedStateHandle` with `@AssistedInject` (A1).**
   - Recommendation: the first ViewModel task compiles and unit-tests this (`SavedStateHandle` built by hand in the test). If it fails, fall back to `rememberSaveable` for the six selection values.

4. **Block Back while a request is in flight** (Pitfall 5). Not covered by the UI-SPEC. Recommend yes. Needs no decision unless the user objects.

5. **Rename `MakeABookingStubScreen` to `MakeABookingScreen`.** CLAUDE.md naming convention says yes; the stub test and `HomeShellsTest` change either way.

6. **DTO location for the booking detail.** CONTEXT D-14 says "in the `mybookings` package". Phase 5 put every DTO in `model/responses` and the STATE hand-off calls `ClientBookingsPayload.kt` the base. Recommend `model/responses/ClientBookingDetail.kt` with the repository method in `mybookings/ClientBookingsRepository.kt`. Low risk.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK (Android Studio JBR) | Gradle build and unit tests | yes | OpenJDK 25.0.3 at `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | — |
| Gradle wrapper, Android SDK build tooling | Build, ktlint, lint | yes (used through Phase 5) | wrapper in repo | — |
| `adb` on PATH | Device checks | no | — | Run from Android Studio; device checks are deferred anyway |
| Samsung SM-A075F test device | Manual checks | not probed | — | Deferred (project preference) |
| iOS app as the expert, set to a different timezone | Success criteria 2 and 3 (manual) | external, not probed | — | Deferred. The automated golden tests stand in for the grouping logic. |
| Production backend (Railway) | Manual end-to-end | external | — | Unit and MockWebServer tests cover the contract |
| `ctx7` CLI / Context7 MCP | Docs lookup | no | — | Official docs via WebFetch (used) |

**Missing dependencies with no fallback:** none for planning or automated verification.
**Missing dependencies with fallback:** the device and iOS peer are needed only for the deferred manual checks. They must be logged as open items (project preference), not blocking.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 + Compose UI test (`createComposeRule` under `RobolectricTestRunner`) |
| Config file | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| Full suite command | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease && bash scripts/check-design-rules.sh` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| BOOK-02 | Availability JSON (with `.000Z`, no `held`, null `slots`) decodes; `start_utc` parsed | unit | `--tests '*ResponseDecodingTest'` | extend |
| BOOK-02/03/04 | New paths carry the bearer; request line, `month` query, JSON body exact; `client_notes` omitted when null; 409/422 bodies map to `ErrorCode` | MockWebServer | `--tests '*ApiRequestUrlsTest' --tests '*ClickMeApiTest'` | extend |
| BOOK-02 | Regroup by client-local date: expert Pacific/Auckland vs client America/Toronto, UTC, Asia/Kolkata (+5:30), Pacific/Kiritimati (+14); slot at a month boundary lands on the previous/next local date; US fall-back day 2026-11-01; duplicates across neighbor months dropped; dup with mixed `available` is unavailable; sorted per day; `held=true` and `available=false` unselectable; `day.date` ignored | unit (pure) | `--tests '*BookingSlotsTest'` | ❌ Wave 0 |
| BOOK-01 | Month grid: leading/trailing empties, Sunday vs Monday first day, Feb 2028, 5- and 6-row months; previous-month bound; past-day rule | unit (pure) | `--tests '*BookingCalendarTest'` | ❌ Wave 0 |
| BOOK-02 | Timezone note: different zones, same zone, unknown/invalid expert zone, EST vs EDT by date, city extraction (`America/Argentina/Buenos_Aires`) | unit (pure) | `--tests '*BookingTimezoneNoteTest'` | ❌ Wave 0 |
| BOOK-01/02 | ViewModel: first free topic preselected, else first, none = disabled; topic/day/month change clears slot; meeting type and notes do not; initial load issues center+prev+next; center failure = Failed, neighbor failure silent; month change cancels the older load (stale suspended call); open-day dot set; saved state restore | unit (fake API, `MainDispatcherRule`, `MutableClock`) | `--tests '*MakeABookingViewModelTest'` | ❌ Wave 0 |
| BOOK-03 | `book()`: request body fields, notes trim/omit, `duration` default 30, timezone id; single-flight; past slot guard; paid topic disabled, no call; each error code maps to its message; 409 clears slot and reloads displayed month; 422 link resets meeting type; suspended = silent; success emits `bookingId` once | unit | `--tests '*MakeABookingViewModelTest'` | ❌ Wave 0 |
| BOOK-01/02/03 | Screen: section order, chips (dimmed non-clickable, selected state), calendar day states, Book Now enabled/disabled rules and paid note, notes counter and 500 cap, summary rows, error dialog | Compose (Robolectric) | `--tests '*MakeABookingScreenTest' --tests '*MakeABookingSubviewsTest'` | ❌ Wave 0 |
| BOOK-04 | Navigator: `replaceTop` swaps the top and sets Push, refuses a tab root; `resetAndSelect` pops Explore and Bookings to root and selects Bookings; `reselect(Explore)` for Back to Home | unit | `--tests '*NavigatorTest'` | extend |
| BOOK-04 | Shell: booking route hides the bottom bar and Back restores it (rewritten to inject a fake entry); Request Sent route hides the bar | Compose | `--tests '*HomeShellsTest'` | extend (rewrite one test) |
| BOOK-04 | Request Sent VM: loading, loaded (free hides lifecycle card), error then Retry, end-time fallback; screen: details rows, buttons call the callbacks, final state with animations settled | unit + Compose | `--tests '*BookingRequestSentViewModelTest' --tests '*BookingRequestSentScreenTest'` | ❌ Wave 0 |
| BOOK-03 (e2e) | Android books a free topic; iOS expert gets the push | manual | — | deferred (project preference) |

Use `TimeZone.setDefault(...)` plus `@After` reset only for tests that go through `ZoneId.systemDefault()`; the pure functions take the zone as a parameter and need no global state.

### Sampling Rate
- **Per task commit:** the targeted `--tests` pattern for the files touched (about 30 s).
- **Per wave merge:** `./gradlew testDevDebugUnitTest ktlintCheck && bash scripts/check-design-rules.sh`.
- **Phase gate:** full suite green on both flavors, including `assembleProdRelease`, before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `FakeClickMeApi`: add `expertAvailabilityHandler`, `requestBookingHandler`, `clientBookingDetailHandler` and call counters (adding methods to `ClickMeApi` breaks compilation of the fake until this is done).
- [ ] `BookingSlotsTest`, `BookingCalendarTest`, `BookingTimezoneNoteTest`: the pure-logic golden tests, written before the UI.
- [ ] `MakeABookingViewModelTest`, `BookingRequestSentViewModelTest`: fake API plus `MutableClock` fixed to a known instant.
- [ ] Compose tests for the screen and subviews.
- [ ] Delete `MakeABookingStubScreenTest`; rewrite the booking test in `HomeShellsTest`.
- [ ] A compile check for `SavedStateHandle` + `@AssistedInject` (A1).
- Framework install: none.

## Security Domain

`security_enforcement` is not set to false in `.planning/config.json`, so this section applies.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (existing bearer flow) | Bearer via `AuthHeaderInterceptor`; the three new paths are authed (not in `publicPaths`). |
| V3 Session Management | no | Unchanged. |
| V4 Access Control | yes | The server scopes `GET /client/bookings/:id` by JWT (404 for another user's id, no existence leak) and rejects self-booking (`CANNOT_BOOK_SELF`). The client sends no `client_id`. |
| V5 Input Validation | yes | Client: notes capped at 500, trimmed, blank omitted. Server: strict zod schema is the real validator. Do not build a second validator. |
| V6 Cryptography | no | No new crypto; TLS and token storage unchanged. |
| V7 Logging | yes | Do not log request bodies, notes, or booking ids in release; the Moshi/Retrofit stack has no body logging in prod. |
| V13 API | yes | Typed DTOs; unknown keys ignored; server error text rendered as plain `Text`. |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Server-supplied strings (topic title, description, expert name, error `message`) rendered in the UI | Tampering / Spoofing | Render as plain `Text`; no HTML, markdown or links (Phase 5 T-05-09 precedent). |
| IDOR on `RequestSentKey(bookingId)` | Information disclosure | Server returns 404 for a booking not owned by the caller; the screen shows the retry error state. The key is non-secret and is serialized in the back stack only. |
| Replay or double submit of `POST /bookings/request` | Tampering | Single-flight in the ViewModel; the server's Redis hold and DB exclusion constraint produce a `409` instead of a second booking. |
| Client-chosen `duration_minutes` | Tampering | The server trusts the body value (`duration_minutes \|\| topic.duration_mins`). Free topics only here, so no money impact. Send the topic's own duration; record as a backend note for Phase 7 where it would matter. |
| Past or arbitrary `scheduled_start` | Tampering | Not validated server-side. Client guards past slots; the Redis hold and conflict check still protect other bookings. Record as a backend note. |
| Notes content (user-authored) | Injection into the expert's UI | Out of Android's control; server stores as text, max 1000. |
| Sensitive data in saved state | Information disclosure | `SavedStateHandle` holds only ids, a date, a slot instant, a meeting-type enum and the notes text. Notes may be private; they are in app-private saved state only, never logged. |

## Sources

### Primary (HIGH confidence)
- Backend source read directly: `backend-reference/src/controllers/availability.controller.js` (`getClientCalendar`), `src/services/availability.js` (`generateSlotsForMonth`), `src/controllers/booking.controller.js` (`createRequest`), `src/schemas/booking.schema.js` (`requestSchema`), `src/routes/booking.js`, `src/middleware/rejectSelfBooking.js`, `src/middleware/rejectUnavailableExpert.js`, `src/controllers/clientBooking.controller.js` (`getDetail`), `src/controllers/publicProfile.controller.js` (`getExpertDetails`), `src/utils/meetingTypes.js`, `docs/ios/SESSION-BUFFER.md`.
- iOS source read directly: `MakeABookingViewModel.swift`, `MakeABookingView.swift`, `MakeABookingTimeSlotsCard.swift`, `MakeABookingCalendarCard.swift`, `MakeABookingTopicPicker.swift`, `BookingRequestSentViewModel.swift`, `BookingRequestSentView.swift`, `ExpertAvailabilityData.swift`, `ClientBookingDetail.swift`, `BookingRequestData.swift`, `ClickMeAPI+Booking.swift`, `ClickMeAPI+Availability.swift`, `HomeClientView.swift` (showMyBookings / returnToHome).
- Android codebase: `Navigator.kt`, `NavigationState.kt`, `HomeClientScreen.kt`, `ExpertProfileViewModel.kt`, `ExpertProfileRepository.kt`, `InstantAdapter.kt`, `ApiException.kt`, `ApiErrorParser.kt`, `ErrorCode.kt`, `ClickMeApi.kt`, `ApiRequestUrls.kt`, `Money.kt`, `PriceDisplay.kt`, `TodaysSessions.kt`, shared components, `FakeClickMeApi.kt`, `NavigatorTest.kt`, `HomeShellsTest.kt`, `ClockModule.kt`.
- `.planning/phases/06-booking-flow-free-bookings/06-CONTEXT.md`, `06-UI-SPEC.md`; `.planning/phases/05-explore-expert-profile/05-RESEARCH.md`, `05-VALIDATION.md`, Phase 5 summaries; `.planning/STATE.md`.
- [Hilt with other Jetpack libraries (developer.android.com)](https://developer.android.com/training/dependency-injection/hilt-jetpack) - assisted injection for ViewModels (does not state the `SavedStateHandle` combination).
- [Hilt ViewModel (dagger.dev)](https://dagger.dev/hilt/view-model) - assisted injection section (does not state the `SavedStateHandle` combination).

### Secondary (MEDIUM confidence)
- [Why SavedStateHandle Crashes in Jetpack Navigation 3 (ProAndroidDev)](https://proandroiddev.com/resolving-savedstatehandle-crashes-with-hilt-assisted-injection-in-jetpack-navigation-3-63ebd50b88af) - community report that `savedStateHandle.toRoute` fails under Nav3; supports using the assisted `NavKey` rather than route arguments (we do not use `toRoute`).
- [The Type-Safe Pattern Without SavedStateHandle (ProAndroidDev)](https://proandroiddev.com/the-type-safe-pattern-without-savedstatehandle-hilt-assisted-injection-with-jetpack-navigation-3-aae82c76bd02) - same pattern as the repo's existing `ExpertProfileViewModel`.

### Tertiary (LOW confidence)
- None relied on.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. Nothing new; every dependency is in the repo.
- Architecture: HIGH. Backend and iOS behavior read from source; Android wiring follows the Phase 5 pattern. The one unverified point is `SavedStateHandle` with assisted injection (A1), with a defined fallback.
- Pitfalls: HIGH for the data-shape and request-contract items (read from source). MEDIUM for the behavioral recommendations (Back during flight, surrogate-safe cap), which are engineering judgment.

**Research date:** 2026-10-22
**Valid until:** 2026-11-21 (stable stack; re-check the backend `generateSlotsForMonth` output shape if the backend is redeployed with availability changes).
