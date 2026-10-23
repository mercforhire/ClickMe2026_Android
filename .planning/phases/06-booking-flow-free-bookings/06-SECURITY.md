---
phase: 06
slug: booking-flow-free-bookings
status: verified
threats_open: 0
asvs_level: 1
created: 2026-10-22
---

# Phase 06 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Audited against `main` HEAD `d0a3393` (includes the post-execution review fixes `6166b0b..7bf8011`), not against plan-time code.

**Evidence path key** (all paths relative to the repo root):
`MAIN` = `app/src/main/java/com/uptrendinvestments/clickmeapp` · `MAB` = `MAIN/screens/clientmode/makeabooking` · `RS` = `MAIN/screens/clientmode/requestsent` · `TEST` = `app/src/test/java/com/uptrendinvestments/clickmeapp` · `BE` = `backend-reference/src` (Backend repo).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Backend -> app (DTO decoding) | Availability grid, booking response and booking detail JSON are parsed from the network (06-01, 06-05) | Slot instants and flags, expert IANA zone id, booking id, status, expert name, topic title; low sensitivity, untrusted |
| App -> backend (URLs and body) | Expert id, booking id and month go into URL paths and queries; the booking body is sent to a strict server schema (06-01, 06-11) | Expert id, topic id, scheduled start, duration, meeting type, timezone, client notes (user-authored, may be private) |
| UI callback -> navigation state | Screens call `Navigator` operations; a wrong call could corrupt a tab back stack (06-02, 06-13) | Route keys (`BookingKey`, `RequestSentKey`) |
| Back stack / saved state -> ViewModel | Route keys and `SavedStateHandle` values are restored after process death and seed assisted ViewModels (06-02, 06-06, 06-13) | Expert and topic ids, date, month, slot instant, meeting type, notes, attempt id, completed booking id |
| App -> app-private disk (added after planning by review fix WR-01) | `DataStoreCompletedBookingStore` writes an attempt UUID and booking id to the `clickme_session` Preferences DataStore | Booking id and random attempt UUID (no notes); plaintext, app-private, not backed up, cleared at logout |
| Server text -> user (UI) | Server-supplied topic titles, descriptions, expert names and error messages are displayed in screens and dialogs (06-04, 06-07, 06-10, 06-12) | Untrusted display strings |
| User -> screen | Taps, Back presses and text input during an in-flight request (06-09, 06-12) | Notes text, button taps, system Back |
| Build -> release artifact | R8 minification of the new Moshi codegen DTOs and screens (06-14) | Compiled DTO adapters |
| Repository -> CI | Secrets and design rules enforced by `scripts/check-design-rules.sh` (06-14) | Source tree |

---

## Threat Register

36 mitigate, 9 accept, 2 transfer = 47.

| Threat ID | Category | Component | Disposition | Mitigation (evidence in current code) | Status |
|-----------|----------|-----------|-------------|---------------------------------------|--------|
| T-06-01 | Denial of service | DTO decoding of availability and booking detail | mitigate | All optional fields are nullable or defaulted: `model/responses/ExpertAvailabilityData.kt:21-27,36-40,48-54` (whole availability DTO optional), `ClientBookingDetail.kt:13-25,31-46` (only identity fields `booking_id/status/start_time/topic/expert` are required), `BookingRequestData.kt:7-12`. All three are `@JsonClass(generateAdapter = true)`. Any decode failure is caught by `apiCall` (`network/ApiCall.kt:9-16`) and becomes `ApiException.Decoding`, which the VMs map to a section/Failed state (`MAB/BookingErrors.kt:58-59,70-71`, `RS/BookingRequestSentViewModel.kt:58-59`), never a crash. Tests: `TEST/network/ResponseDecodingTest.kt:523,549,558,577,600`. | closed |
| T-06-02 | Tampering | `experts/{id}/...` and `client/bookings/{id}` path values | mitigate | `@Path("id")` (default `encoded=false`, percent-encodes) at `network/ClickMeApi.kt:169,183`. Ids come only from server data: `BookingKey` built in `screens/clientmode/expertprofile/ExpertProfileViewModel.kt:72`, booking id from the 201 response (`MAB/MakeABookingViewModel.kt:265`); no typed text reaches a path. | closed |
| T-06-03 | Tampering | `POST /bookings/request` body | mitigate | `model/requests/BookingRequests.kt:15-24`: exactly seven keys in declared order, `client_notes` nullable default null; Moshi has no null serialization (`network/MoshiProvider.kt:13`, no `serializeNulls` anywhere in `app/src/main`); the ViewModel sends `trim().ifEmpty { null }` (`MAB/MakeABookingViewModel.kt:254`). Exact-JSON and null-omission tests: `TEST/network/ClickMeApiTest.kt:555-575`, `ResponseDecodingTest.kt:616,636`. | closed |
| T-06-04 | Information disclosure | Bearer on the new paths | mitigate | `network/ApiRequestUrls.kt:38-40` adds the three paths; `publicPaths` (`ApiRequestUrls.kt:47-54`) still holds exactly the five credential paths and `needAuthToken` uses `none { endsWith }` (line 57), so the new paths are authed. `TEST/network/ApiRequestUrlsTest.kt:113-121` asserts all three need the token and `publicPaths.size == 5`. | closed |
| T-06-05 | Information disclosure | Logging of booking bodies (notes, ids) | accept | See Accepted Risks Log AR-06-01. Verified: no logging calls in `MAB/**`, `RS/**`; HTTP logging is debug-only and BASIC (no bodies) in `app/src/debug/.../network/DebugNetwork.kt`, release stub returns an empty list in `app/src/release/.../network/DebugNetwork.kt`. | closed |
| T-06-06 | Denial of service | `Navigator.replaceTop` / `resetAndSelect` | mitigate | `navigation/Navigator.kt:90` `check(stack.size > 1) { "Cannot replace a tab root" }`; `Navigator.kt:107,109` `require(tab in state.backStacks)` and `requireNotNull(...)` validate every key before any mutation (comment at line 108). Tests: `TEST/navigation/NavigatorTest.kt:189` (`replaceTopRefusesToReplaceATabRoot`), `:230` (`resetAndSelectRejectsUnknownTabOrResetKey`). | closed |
| T-06-07 | Information disclosure | `RequestSentKey` in the saved back stack | accept | See Accepted Risks Log AR-06-02. Server scoping confirmed: `BE/controllers/clientBooking.controller.js:965-1000` (see Transfer Verification). The stale KDoc at `RS/RequestSentKey.kt` ("lives only in the saved back stack") no longer describes all copies of the id; see Rationale Drift RD-01. | closed |
| T-06-08 | Tampering | Re-submitting the booking form after success | mitigate | `screens/clientmode/home/ClientBookingFlowEntries.kt:41` `booking(key, navigator::goBack) { id -> navigator.replaceTop(RequestSentKey(id)) }`; `Navigator.replaceTop` removes the booking entry (`Navigator.kt:92-93`). `isSubmitting` also stays true after success (`MAB/MakeABookingViewModel.kt:259-265`). Test: `TEST/navigation/BookingFlowNavigationTest.kt:44,51`. | closed |
| T-06-09 | Denial of service | `timezoneNote` parsing the server zone id | mitigate | `MAB/BookingTimezoneNote.kt:37-47`: null or blank id skips parsing, `ZoneId.of(expertZoneId)` wrapped in `catch (_: DateTimeException)` returning null, then `TimezoneNote.Same` (line 48). Test: `TEST/.../makeabooking/BookingTimezoneNoteTest.kt:56` (`missingOrInvalidExpertZoneFallsBackToSingleZoneNote`). | closed |
| T-06-10 | Tampering | Notes text over the cap or with split surrogates | mitigate | `MAB/BookingFormatting.kt:19,26-33`: `NOTES_MAX_CHARS = 500`, `capNotes` cuts at 500 UTF-16 units and drops a trailing high surrogate. Applied on every input (`MAB/MakeABookingViewModel.kt:209`) and on restore (`:527`). Tests: `BookingFormattingTest.kt:27,32`. | closed |
| T-06-11 | Spoofing | Raw IANA id shown to the user | mitigate | `MAB/BookingTimezoneNote.kt:48-49` returns only `zoneCity(expert.id)` (last segment) plus abbreviations; the raw id is never in `TimezoneNote`. Tests: `BookingTimezoneNoteTest.kt:72,79` (`rawIanaIdIsNeverShown`). | closed |
| T-06-12 | Tampering | Duplicate slots with conflicting availability across neighbor months | mitigate | `MAB/BookingSlots.kt:44,50`: `existing.copy(isAvailable = existing.isAvailable && ok)` (AND of copies; `ok` folds in `held`). Tests: `BookingSlotsTest.kt:85,93`. Server hold and DB exclusion constraint confirmed: `BE/services/redis.js:68-72` (atomic SET NX EX), `BE/controllers/booking.controller.js:387,394`, `supabase/migrations/20260606000000_bookings_no_overlap_exclusion.sql`. | closed |
| T-06-13 | Spoofing | Server error `message` shown to the user | mitigate | Server text is only wrapped in `FieldError.Text` (`MAB/BookingErrors.kt:55,69`) and rendered by `AuthErrorDialog` as `Text(text = message)` (`screens/login/AuthErrorDialog.kt:32-37`); `FieldError.asString()` returns the raw string (`screens/login/FieldError.kt:25-29`). No HTML, markdown or link APIs exist anywhere under `MAB/**` or `RS/**` (grep for Html, fromHtml, Linkify, LinkAnnotation, ClickableText, AndroidView, WebView found only the styled-name `AnnotatedString` in the Request Sent headline, which has no link annotations). | closed |
| T-06-14 | Elevation of privilege | Client-side paid-topic gate | transfer | See Transfer Verification TR-06-01: server rejects with `PAID_TOPIC_REQUIRES_PAYMENT` at `BE/controllers/booking.controller.js:1328-1330`, route `BE/routes/booking.js:40`. Client display gate present: `MAB/MakeABookingUiState.kt:124` and defensive mapping `MAB/BookingErrors.kt:48-50`. | closed |
| T-06-15 | Information disclosure | `ACCOUNT_SUSPENDED` handling | mitigate | `MAB/BookingErrors.kt:34` `ErrorCode.AccountSuspended -> BookingFailure.Silent`; the VM only clears `isSubmitting` for `Silent` (`MAB/MakeABookingViewModel.kt:276`). Test: `MakeABookingViewModelBookTest.kt:788` (`suspendedAccountShowsNoBookingDialog`). | closed |
| T-06-16 | Information disclosure | `GET /client/bookings/:id` with a foreign id (IDOR) | transfer | See Transfer Verification TR-06-02: server scopes by `client_id` and returns 404 (`BE/controllers/clientBooking.controller.js:978-998`). Client side: every `ApiException` ends in `Failed` with fixed copy (`RS/BookingRequestSentViewModel.kt:58-59`, `res/values/strings_request_sent.xml` `request_sent_error_*`); test `BookingRequestSentViewModelTest.kt:119` (`notFoundEndsFailed`). | closed |
| T-06-17 | Denial of service | Detail decoding / repeated Retry | mitigate | Lenient DTO (T-06-01) and latest-wins load: `RS/BookingRequestSentViewModel.kt:51` `loadJob?.cancel()` before each launch. Test: `BookingRequestSentViewModelTest.kt:148` (`retryWhileLoadingCancelsTheFirstCall`). | closed |
| T-06-18 | Information disclosure | Logging the booking id or details | accept | See Accepted Risks Log AR-06-03. Same verification as T-06-05. | closed |
| T-06-19 | Tampering | Stale availability response overwriting a newer month | mitigate | `MAB/MakeABookingViewModel.kt:330` `availabilityJob?.cancel()` on every load; `:342` `if (_uiState.value.displayedMonth != month) return@launch`; `:352` same guard on failure. Test: `MakeABookingViewModelTest.kt:280` (`staleResponseNeverOverwritesTheNewerMonth`). | closed |
| T-06-20 | Tampering | Restored slot from saved state | mitigate | `MAB/MakeABookingViewModel.kt:378-391` (`applyAvailability`): a pending (restored) slot is kept only if `slots.any { it.start == start && it.isSelectable(now) }`, otherwise dropped (`:401-402`). Test: `MakeABookingViewModelTest.kt:423` (`restoredSlotThatIsNoLongerAvailableIsDropped`), `MakeABookingViewModelBookTest.kt:569`. | closed |
| T-06-21 | Information disclosure | Notes text in SavedStateHandle | accept | See Accepted Risks Log AR-06-04. Re-validated against current code: notes are written only to `SavedStateHandle` (`MAB/MakeABookingViewModel.kt:211`), never to disk (`MAB/CompletedBookingStore.kt:84-87` stores only attempt id and booking id) and never logged. | closed |
| T-06-22 | Tampering | Selecting a past, held or unavailable slot | mitigate | `MAB/MakeABookingViewModel.kt:195` `if (slot == null \|\| !slot.isSelectable(now)) return`; `:180` `if (isPastDay(date, ...)) return`; `isSelectable = isAvailable && start.isAfter(now)` at `MAB/BookingSlots.kt:23`. Tests: `MakeABookingViewModelTest.kt:328`, `MakeABookingViewModelBookTest.kt:584,595,610`. | closed |
| T-06-23 | Spoofing | Topic title/description and expert name rendering | mitigate | Rendered only as `Text(text = ...)` arguments: `MAB/subviews/MakeABookingTopicSheet.kt:138,164`, `MakeABookingTopicPicker.kt:159`, `MakeABookingSummarySection.kt:79`, `MakeABookingExpertCard.kt:53,62`; a11y strings pass titles as `%s` arguments (`MakeABookingTopicPicker.kt:209`). No HTML or link APIs under `MAB/**` (see T-06-13). | closed |
| T-06-24 | Elevation of privilege | Paid topics selectable in the sheet | accept | See Accepted Risks Log AR-06-05. Verified: selecting a paid topic yields `BookDisabledReason.PaidTopic` (`MAB/MakeABookingUiState.kt:124`) and the server rejects (TR-06-01). | closed |
| T-06-25 | Tampering | Tapping a past, held or unavailable slot | mitigate | `MAB/subviews/MakeABookingTimeSlotsCard.kt:165` `enabled = slot.isSelectable(now)` feeds `.selectable(enabled = enabled && interactive, ...)` at `:230`; the ViewModel re-checks on select (`MakeABookingViewModel.kt:195`) and on book (`:232-241`). Tests: `MakeABookingCalendarSlotsTest.kt:224` (`onlySelectableSlotCallsOnSelectSlot`). | closed |
| T-06-26 | Denial of service | Malformed expert zone id | mitigate | `MAB/subviews/MakeABookingTimeSlotsCard.kt:118` uses `timezoneNote(...)` (T-06-09 handling). Test: `MakeABookingCalendarSlotsTest.kt:273` (`nullExpertZoneShowsSingleLine`). | closed |
| T-06-27 | Spoofing | Showing raw server zone id | mitigate | Only `TimezoneNote` city and abbreviation values reach the UI (`MAB/BookingTimezoneNote.kt:48-49`). Test: `MakeABookingCalendarSlotsTest.kt:259-269` asserts no node contains `"America/"`. | closed |
| T-06-28 | Tampering | Double tap on Book Now | mitigate | `MAB/subviews/MakeABookingBookButton.kt:62` `onClick = { if (!isSubmitting) onClick() }`; the ViewModel returns early (`MakeABookingViewModel.kt:231`) and sets `isSubmitting = true` synchronously on the main dispatcher (`:242`); server answers a duplicate with 409 (BE hold, T-06-12). Tests: `MakeABookingViewModelBookTest.kt:121`, `MakeABookingScreenTest.kt:183`. | closed |
| T-06-29 | Elevation of privilege | Booking a paid topic from the free path | mitigate | Button `enabled = disabledReason == null` (`MakeABookingBookButton.kt:69`) with `PaidTopic` reason (`MakeABookingUiState.kt:124`); ViewModel `if (!topic.isFree) return` before any call (`MakeABookingViewModel.kt:238`); server enforces (TR-06-01). Test: `MakeABookingViewModelBookTest.kt:422` (`paidTopicNeverReachesTheApi`). | closed |
| T-06-30 | Information disclosure | Notes field contents | accept | See Accepted Risks Log AR-06-06. Verified: trimmed before send (`MakeABookingViewModel.kt:254`), not logged, not on disk. | closed |
| T-06-31 | Spoofing | Expert name and topic title rendering | mitigate | `RS/subviews/BookingRequestSentHeadline.kt:38` formats the template with a fixed `NAME_MARKER` argument (the name is never a pattern), then `append(expertName)` inside `withStyle(SpanStyle)` (`:65-78`), with no link annotations; `RS/subviews/BookingRequestSentDetailsCard.kt:90,96,106,110` render the name and title as `Text` and `%s` arguments. Tests: `BookingRequestSentScreenTest.kt:79,88`. | closed |
| T-06-32 | Tampering | Accidental taps during the entry animation | mitigate | `RS/BookingRequestSentScreen.kt:187` `actionsVisible = progress[ACTIONS_INDEX].value >= 1f`, passed as `enabled = actionsVisible` (`:222`) to `BookingRequestSentActions`, which applies it to both buttons (`RS/subviews/BookingRequestSentActions.kt`). Test: `BookingRequestSentScreenTest.kt:239` (`actionsAreNotClickableUntilTheEntryAnimationEnds`). | closed |
| T-06-33 | Information disclosure | Error state hiding the real booking state | accept | See Accepted Risks Log AR-06-07. Verified fixed copy: `res/values/strings_request_sent.xml` `request_sent_error_body` = "Your request was sent, but we couldn't load the details...". | closed |
| T-06-34 | Tampering | Double submit / replay | mitigate | `MAB/MakeABookingViewModel.kt:231` `if (_uiState.value.isSubmitting) return`; success leaves `isSubmitting` true (`:259-265`); server hold and exclusion constraint (T-06-12). Tests: `MakeABookingViewModelBookTest.kt:121` (`aSecondTapWhileInFlightIsIgnored`), `:172`. | closed |
| T-06-35 | Tampering | Past or arbitrary `scheduled_start` | mitigate | `MAB/MakeABookingViewModel.kt:232-241`: `book()` re-syncs the clock, aborts if a held slot is no longer selectable, and only proceeds with `s.selectedSlot` (selectable at `s.now`); `scheduledStart = slot.start.toString()` (`:250`). Server does not validate (backend note logged at `.planning/STATE.md:87`). Test: `MakeABookingViewModelBookTest.kt:445` (`aSlotThatHasStartedIsClearedAndNotSent`). | closed |
| T-06-36 | Tampering | Client-chosen `duration_minutes` | accept | See Accepted Risks Log AR-06-08. Verified: the topic's own duration is sent (`MAB/MakeABookingViewModel.kt:251`) and the Phase 7 backend note exists (`.planning/STATE.md:87`). | closed |
| T-06-37 | Elevation of privilege | Paid topic through the free path | mitigate | `MAB/MakeABookingViewModel.kt:238` `if (!topic.isFree) return`; server rejects (TR-06-01). Test: `MakeABookingViewModelBookTest.kt:422,759`. | closed |
| T-06-38 | Information disclosure | Suspended-account response | mitigate | Same code as T-06-15 (`MAB/BookingErrors.kt:34`, `MakeABookingViewModel.kt:276`); no booking dialog is set for `Silent`. Test: `MakeABookingViewModelBookTest.kt:788`. | closed |
| T-06-39 | Repudiation | Back during an in-flight booking hides a booking that exists | mitigate | `MAB/MakeABookingScreen.kt:127` `BackHandler(enabled = state.isSubmitting) {}` and `:145` `backEnabled = !state.isSubmitting`, which disables the arrow (`screens/login/AuthTopAppBar.kt:40`). Tests: `MakeABookingScreenTest.kt:246,294`. | closed |
| T-06-40 | Spoofing | Server message in the "Couldn't book" dialog | mitigate | `MAB/MakeABookingScreen.kt:244-249` renders via `AuthErrorDialog(message = error.asString(), ...)`, which uses plain `Text` (`screens/login/AuthErrorDialog.kt:32-37`). Test: `MakeABookingScreenTest.kt:220`. | closed |
| T-06-41 | Tampering | Navigating to Request Sent twice | mitigate | `MAB/MakeABookingScreen.kt:75-79`: `consumeCompletedBooking()` runs before `onBooked(id)` in a `LaunchedEffect(state.completedBookingId)`; the VM nulls the id and the saved copy (`MakeABookingViewModel.kt:307-311`). Tests: `MakeABookingScreenTest.kt:464` (`freeBookingSendsOneRequestAndCallsOnBookedOnce`), `:489` (restored after process death navigates once). | closed |
| T-06-42 | Tampering | Re-submitting the filled booking form after success | mitigate | Same `replaceTop` wiring as T-06-08 (`ClientBookingFlowEntries.kt:41`). Test: `TEST/screens/HomeShellsTest.kt:343` (`bookingSuccessReplacesBookingWithRequestSent`) and `BookingFlowNavigationTest.kt:44,51`. | closed |
| T-06-43 | Denial of service | Duplicate `entry<BookingKey>` registration crashing the shell | mitigate | Exactly one `entry<BookingKey>` in `app/src/main`: `screens/clientmode/home/ClientBookingFlowEntries.kt:40`; the shell wires it once via `HomeClientScreen.kt:94`. `HomeShellsTest.kt:303` registers through the real helper. | closed |
| T-06-44 | Information disclosure | Booking id in RequestSentKey across process death | accept | See Accepted Risks Log AR-06-09 and Rationale Drift RD-01 (the id now also has an on-disk copy). | closed |
| T-06-45 | Denial of service | R8 stripping Moshi adapters of the new DTOs | mitigate | Every new DTO has `@JsonClass(generateAdapter = true)` and explicit `@Json` names for snake_case keys: `BookingRequests.kt:15`, `BookingRequestData.kt:7`, `ClientBookingDetail.kt:13,31,40`, `ExpertAvailabilityData.kt:21,36,48`. Audit re-ran `./gradlew assembleProdRelease` at HEAD (R8 `minifyProdReleaseWithR8` BUILD SUCCESSFUL) and confirmed `BookingRequestBodyJsonAdapter`, `BookingRequestDataJsonAdapter`, `ClientBookingDetailJsonAdapter`, `BookingDetailTopicDtoJsonAdapter`, `BookingDetailExpertDtoJsonAdapter`, `ExpertAvailabilityDataJsonAdapter`, `AvailabilityDayDtoJsonAdapter`, `AvailabilitySlotDtoJsonAdapter` are all present, unrenamed, in the release APK dex. R8 scope is `androidx/kotlin/kotlinx` only (`app/build.gradle.kts:79-82`), so app classes are not processed; there is no `app/proguard-rules.pro` (keep rules live in `app/src/main/keepRules/rules.keep`). Device smoke remains on the deferred list (06-VERIFICATION). | closed |
| T-06-46 | Information disclosure | Stripe secret keys or hex color literals entering the tree | mitigate | `scripts/check-design-rules.sh:10` (Color literals), `:24` (`sk_live\|sk_test\|rk_live\|rk_test` pattern); run in CI (`.github/workflows/ci.yml:25-28`). Audit ran it at HEAD: "Design rules: OK". | closed |
| T-06-47 | Elevation of privilege | Phase 7 paid path partially leaking into Phase 6 | mitigate | Sweep of `MAB/**` and `RS/**` for PaymentSheet, payment-intent, setup-intent, PaymentLifecycle and "How does payment work" returns 0 matches; no `bookings/confirm` or payment constant in `network/ApiRequestUrls.kt` or `network/ClickMeApi.kt`. (The only "Stripe" word is a KDoc comment at `MAB/BookingRepository.kt:31`.) | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

The plans and the phase brief both list the accepted set; the plans contain **nine** accept dispositions (T-06-05, 07, 18, 21, 24, 30, 33, 36, 44), not eight.

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-06-01 | T-06-05 | Release has no body logging (OkHttp logging is debug-only since Phase 1, BASIC level, Authorization redacted); no new logging added in Phase 6. Re-verified in code. | plan 06-01 (user-approved plan) | 2026-10-22 |
| AR-06-02 | T-06-07 | `RequestSentKey` holds only a booking id, which is not secret; the server scopes `GET /client/bookings/:id` to the caller (404 otherwise). Server scoping re-verified (TR-06-02). See RD-01 for the added on-disk copy. | plan 06-02 (user-approved plan) | 2026-10-22 |
| AR-06-03 | T-06-18 | No logging of booking id or details is added; release has no HTTP body logging. Re-verified in code. | plan 06-05 (user-approved plan) | 2026-10-22 |
| AR-06-04 | T-06-21 | Notes text in `SavedStateHandle`: app-private saved state only, never logged. Re-verified: notes are never written to the durable store or disk. The saved-state key set has since grown by two ids (`booking_attempt_id`, `booking_completed_id`), which does not touch the notes exposure. | plan 06-06 (user-approved plan) | 2026-10-22 |
| AR-06-05 | T-06-24 | Paid topics are listed (D-01); selection only disables Book Now and the server rejects unpaid bookings of paid topics (TR-06-01). | plan 06-07 (user-approved plan) | 2026-10-22 |
| AR-06-06 | T-06-30 | Notes are not logged, held only in UI and app-private saved state, trimmed, and sent only to the booking endpoint. Re-verified. | plan 06-09 (user-approved plan) | 2026-10-22 |
| AR-06-07 | T-06-33 | Request Sent error state uses fixed copy that says the request was sent; Phase 8 My Bookings is the recovery path (UI-SPEC Open Item 4). Re-verified in `strings_request_sent.xml`. | plan 06-10 (user-approved plan) | 2026-10-22 |
| AR-06-08 | T-06-36 | The topic's own duration is sent; free topics only, so no money impact; recorded as a Phase 7 backend note (`.planning/STATE.md:87`). | plan 06-11 (user-approved plan) | 2026-10-22 |
| AR-06-09 | T-06-44 | Booking id in `RequestSentKey` across process death: non-secret id in app-private state; the server scopes the detail read to the caller. **Rationale drift: see RD-01. The id now also has an on-disk copy; user acknowledgement requested.** | plan 06-13 (user-approved plan) | 2026-10-22 |

*Accepted risks do not resurface in future audit runs.*

---

## Transfer Verification

| Transfer ID | Threat Ref | Transferred To | Evidence that the receiving party enforces it |
|-------------|------------|----------------|-----------------------------------------------|
| TR-06-01 | T-06-14 (also backs T-06-24, T-06-29, T-06-37) | Backend (`POST /bookings/request`) | `BE/controllers/booking.controller.js:1319-1330`: loads the topic and `if (!topic.is_free) throw new AppError(422, 'PAID_TOPIC_REQUIRES_PAYMENT', ...)`; route `BE/routes/booking.js:40` (`roleCheck('client')`, strict `requestSchema`, `rejectSelfBooking`, `rejectUnavailableExpert`). The Android code maps this code defensively (`MAB/BookingErrors.kt:48-50`). |
| TR-06-02 | T-06-16 (also backs T-06-07, T-06-44) | Backend (`GET /client/bookings/:id`) | `BE/routes/clientBooking.js:27` (`roleCheck('client')`); `BE/controllers/clientBooking.controller.js:965-998`: `getDetail` reads with `.eq('id', bookingId).eq('client_id', clientId)` (clientId from `req.user.id`) and throws `AppError(404, 'NOT_FOUND', 'Booking not found.')` for not-owned, not-found or malformed ids ("no existence leak"). |

---

## Unregistered Flags (WARNING, not a blocker)

No SUMMARY.md `## Threat Flags` section lists an unmapped flag (all of 06-01 to 06-14 that have the section say "None"; 06-06-SUMMARY.md and 06-13-SUMMARY.md have no such section at all, so their mitigations were verified directly from code). The post-execution review fixes, however, added attack surface that has no threat mapping:

| Flag ID | Source | Surface | Assessment |
|---------|--------|---------|------------|
| UF-01 | Review fix WR-01 (`d9b2300`, refined in `0f04b42`) | `DataStoreCompletedBookingStore` (`MAB/CompletedBookingStore.kt:72-131`, bound in `MAIN/di/BookingModule.kt:16-21`) persists a random attempt UUID and the booking id in the plaintext `clickme_session` Preferences DataStore. | Controls verified: app-private file; `android:allowBackup="false"` plus extraction rules excluding all domains (`AndroidManifest.xml:9-11`, `res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml`); one record, keyed by the attempt id held only in the restored ViewModel's saved state (`CompletedBookingStore.kt:101`), so it cannot resurface in an unrelated flow; discarded on consume (`MakeABookingViewModel.kt:310`); wiped at logout (`UserManager.kt:197` runs the registered cleaner, `:204` `sessionStore.clear()` clears the whole DataStore), and before login and signup (`UserManager.kt:142,156`). Test: `DataStoreCompletedBookingStoreTest.kt:80` (`theSessionCleanerAndLogoutBothRemoveTheRecord`). Notes are never stored. Residual: a non-secret, server-scoped id sits unencrypted in app-private storage for the window between a successful request and Request Sent being shown (or until logout if the user never returns). Needs user acknowledgement (RD-01). |

---

## Rationale Drift

| ID | Affected | What changed | Auditor assessment |
|----|----------|--------------|--------------------|
| RD-01 | AR-06-02 (T-06-07), AR-06-09 (T-06-44); also cited by RESEARCH "Security Domain" (`06-RESEARCH.md` lines ~765-777: "key is non-secret and is serialized in the back stack only", "SavedStateHandle holds only ids, a date, ...") | After planning, the booking id gained two more copies: `booking_completed_id` in the booking ViewModel's `SavedStateHandle` (`MakeABookingViewModel.kt:264,309`, commit `9089cb2`) and an on-disk record in the `clickme_session` DataStore (UF-01, commit `d9b2300`). `RequestSentKey.kt`'s KDoc ("lives only in the saved back stack") is now stale. | The two load-bearing parts of the acceptance still hold in current code and backend: (1) the id is non-secret (a UUID), and (2) the server scopes the detail read to the caller (TR-06-02), so a leaked id exposes nothing. The storage location, however, is no longer "saved state only", so the wording of AR-06-02 and AR-06-09 is understated. The auditor has kept both closed under the original user-approved acceptance because the risk class (non-secret id in app-private storage) is unchanged and every compensating control is verified, but this is **not** a fresh user acceptance. Requested: the user acknowledges (a) the on-disk plaintext copy of the booking id and attempt UUID (UF-01) and (b) updating the `RequestSentKey` KDoc. AR-06-04 (T-06-21, notes) is unaffected: notes never leave `SavedStateHandle`. |

---

## Audit Notes

- Verified against current `main` HEAD `d0a3393`, including all review-fix commits `6166b0b..7bf8011`. Implementation files were not modified.
- Runtime evidence gathered at HEAD (not just code reading): `./gradlew testDevDebugUnitTest` for the `screens.clientmode.*`, `navigation.*`, `network.*` and `HomeShellsTest` packages: 598 tests, 0 failures, 0 errors; `./gradlew assembleProdRelease` (R8): BUILD SUCCESSFUL; `bash scripts/check-design-rules.sh`: "Design rules: OK". Full-suite figures (1340 tests) come from `06-REVIEW-FIX.md` and were not re-run here.
- Not verifiable without a device and keystore (already on the deferred list in `06-VERIFICATION.md`): the signed `prodRelease` R8 smoke for T-06-45.
- The file `app/proguard-rules.pro` named in the audit brief does not exist; R8 inputs are `app/src/main/keepRules/rules.keep` and the `optimization { packageScope }` block in `app/build.gradle.kts`.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-22 | 47 | 47 | 0 | gsd-security-auditor |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-22 (auditor). Open follow-up for the user: acknowledge RD-01 / UF-01 (on-disk completed-booking record) and the stale `RequestSentKey` KDoc.
