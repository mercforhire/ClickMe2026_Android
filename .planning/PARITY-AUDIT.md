# ClickMe Android vs iOS: feature-parity gap audit (2026-10-30)

Read-only audit. iOS root: `/Users/LeonChen/Documents/ClickMe2026/ClickMe2026/` (shortened to `iOS/` below). Android root: `/Users/LeonChen/Documents/ClickMe_Android/app/src/main/java/com/uptrendinvestments/clickmeapp/` (shortened to `and/`).

## Summary

**14 gaps.** Most map to deferred items DEF-1, DEF-2, DEF-3, DEF-4 (client side) and DEF-6 (partly).

| Size | Count | Items |
|---|---|---|
| L (>2 days) | 2 | #1 Search tab, #10 Client reschedule start |
| M (0.5–2 days) | 9 | #2 Favorites list, #4 Client payment history, #5 Notification preferences, #6 Feedback, #7 Client private note, #8 Expert private note, #9 Client Profile (expert side), #11 Expert reschedule start, #3+#2b if combined |
| S (<0.5 day) | 3 (+2 optional) | #2b Favorite heart on Expert Profile, #3 Full Reviews list, #12 "View Past History" button; optional: #13 splash animation, #14 Delete Account "Before you go" card |

Rough total: about 18–22 executor-days.

**DEF items already done on Android (no gap):**
- DEF-4 expert half: `and/screens/expertmode/payouts/TransactionsScreen.kt`
- DEF-5 Tax Profile: `and/screens/expertmode/taxprofile/`
- DEF-6 Expertise-tag editor: `and/screens/expertmode/settings/subviews/ExpertiseEditorSheet.kt`
- DEF-7 Categories modal: `and/screens/clientmode/explore/categoriesmodal/AllCategoriesSheet.kt`

**DEF items that are not parity gaps:**
- **DEF-8 (analytics, Firebase Ads):** iOS never calls the analytics endpoints. `getAnalyticsSummary`, `getAnalyticsTrends` and `getPopularTopics` have 0 call sites outside `Network/`. iOS also imports no Firebase Analytics or Ads module: the only Firebase imports are FirebaseCore and FirebaseMessaging.
- **DEF-7 Cancellation Policies page:** `iOS/Views/Screens/Client Mode/Cancellation Policies/CancellationPoliciesView.swift` is never instantiated, so iOS users can't reach it.

### Method
1. Listed all 618 iOS Swift files and every Android `.kt` file outside `subviews/`.
2. Mapped each iOS screen to its Android screen.
3. Checked every iOS `ClickMeAPI+*.swift` function for real call sites (outside `Network/`) and compared them with the `@GET/@POST/...` endpoints in `and/network/ClickMeApi.kt`.
4. Compared the callback parameters (`onX:`) on matching screens and cards to find missing actions.
5. Grepped Android for TODO, placeholder, "coming soon" and DEF markers. Apart from the Search tab, the hits were previews and comments, not live stubs.

### iOS endpoints that iOS calls and Android never calls
- `GET /experts/search`
- `GET /client/favorites`, `POST /client/favorites/:id`, `DELETE /client/favorites/:id`
- `GET /client/transactions`
- `PATCH /client/bookings/:id/reschedule`, `PATCH /expert/bookings/:id/reschedule`
- `GET/POST /client/bookings/:id/note`, `GET/POST /bookings/:id/note`
- `GET /expert/clients/:id`
- `GET/PATCH /notifications/preferences`
- `GET /feedback/types`, `POST /feedback/submit`

`GET /experts/:id/reviews` is already on Android (`EXPERT_REVIEWS`), but only feeds the 3-review preview card.

These iOS endpoints are defined but never called on iOS, so they are **not** gaps:
- analytics ×3
- availability overrides ×3
- `/discovery/feed`, `/discovery/random`, `/discovery/interact`
- `/experts/:id/topics`, `/experts/:id`
- `/bookings/initiate`
- `/expert/bookings/:id/summary`, `/expert/bookings/:id/takeaways`, `/expert/bookings/:id/status`
- `/expert/bookings/:id/accept`, `/expert/bookings/:id/decline`
- `/meta/currencies`
- `/media/upload`
- `/help/faqs/search`, `/help/faqs/articles/:id`, `/help/support/tickets`
- `/auth/email/status` (Android does call this one, which is fine)

## Gap table

| # | Feature | iOS evidence | Android state | Backend endpoint(s) | DEF | Size | Dependencies / notes |
|---|---|---|---|---|---|---|---|
| 1 | **Search tab**: text search (debounced), category chips, sort ("Most popular" / "Highest rated"), result cards to Expert Profile, empty and error states. Also Explore's **"See all"** on Featured Experts and Featured Topics, which switches to the Search tab. | `Client Mode/Search/SearchExpertView.swift`, `SearchExpertViewModel.swift` (sortOptions, onSearchTextChanged, loadCategories), `Subviews/SearchExpert*.swift`; `Client Mode/Home/HomeClientView.swift:181-200` (`onSeeAllExperts: { selectedTab = .search }`); `Explore/ExploreClientView.swift:244,273` | **Missing.** `and/screens/clientmode/home/HomeClientScreen.kt:343` `entry<SearchRoot> { ClientTabRoot(SearchRoot) }` renders `PlaceholderTabContent`. Explore has no "See all" callbacks (`and/screens/clientmode/explore/ExploreScreen.kt:110-123` lists only onViewAll for categories). | `GET /experts/search` | DEF-2 | L | User has decided to port it. Reuse `CategoryStore`, `SelectableChip`, explore `ExpertCard`, `RatingStars`, `EmptyState` / `ErrorState`, `RefreshFailedSnackbar`. Needs a new DTO and Retrofit call. |
| 2 | **Favorites (Saved Experts) list**: list, open profile, remove with confirm dialog, empty state, pro-tip card. Reached from the client Profile hub "Saved Experts" row. | `Client Mode/Favorites/FavoritesView.swift` (remove dialog :92), `FavoritesViewModel.swift:67,99`; `Client Mode/Profile/ClientProfileHomeScreen.swift:95,206` | **Missing.** No screen and no endpoints. The client hub has no Saved Experts row (`and/screens/clientmode/profile/ClientProfileHomeScreen.kt:105-123` has only Security, Payment, Help and Privacy). | `GET /client/favorites`, `DELETE /client/favorites/:id` | DEF-3 | M | Reuse `ProfileMenuRow`, `Avatar`, `EmptyState`, the `AlertDialog` pattern and `ExpertProfileKey` navigation. |
| 2b | **Favorite heart on Expert Profile**: optimistic toggle with rollback, error alert, and on load a check of `getClientFavorites(limit:100)` to set the heart's initial state. | `Client Mode/Expert Profile/ExpertProfileViewModel.swift:17,70-95,212-238`; `ExpertProfileView.swift:99` | **Missing.** No favorite code in `and/screens/clientmode/expertprofile/`. | `POST` / `DELETE /client/favorites/:id`, `GET /client/favorites` | DEF-3 | S | Do with #2 (same repository). Hide the heart on the self-view. |
| 3 | **Full Reviews list**: overall rating card, total count, list of review cards. Opened from "See all" on the Expert Profile reviews card. | `Client Mode/Reviews/ExpertReviewsView.swift`, `ExpertReviewsViewModel.swift:102`; `Expert Profile/Subviews/ExpertProfileReviewsCard.swift:15,45`; `ExpertProfileView.swift:326` | **Partial.** `and/screens/clientmode/expertprofile/subviews/ExpertProfileReviewsCard.kt:28` says "at most MAX_REVIEWS rows and no 'See all'". `ReviewsRepository.kt` exists. | `GET /experts/:id/reviews` (already in `ClickMeApi.kt:222`) | DEF-3 | S | Reuse `RatingStars` and `ReviewRow`. iOS loads only page 1. |
| 4 | **Client Payment History**: paged list, tail-triggered next page, a 500 on page >1 treated as end of list, inline page error, row tap opens the booking. Profile hub "Payment History" row. | `Client Mode/Payments/ClientTransactionsView.swift:71-79` (row opens `UpcomingBookingView`), `ClientTransactionsViewModel.swift:12-20`, `Subviews/ClientTransactionRow.swift`; `ClientProfileHomeScreen.swift:107,215`; backend `docs/ios/CLIENT-TRANSACTIONS.md` | **Missing.** No endpoint, screen or hub row. | `GET /client/transactions` | DEF-4 | M | Reuse the paging and state pattern from `and/screens/expertmode/payouts/TransactionsViewModel.kt` and `TransactionsScreen.kt`, plus Money / `PriceText`. Row tap goes to `UpcomingBookingKey`. |
| 5 | **Notification preferences**: per-category toggle sections, quiet-hours card with time pickers, debounced auto-save with saving/saved/error status. Reached from the "Notifications" row in **both** hubs. | `Settings/Notifications/NotificationSettingsView.swift`, `NotificationSettingsViewModel.swift:79,111-138`, `Subviews/NotificationQuietHoursCard.swift`, `NotificationTimePickerRow.swift`; `ClientProfileHomeScreen.swift:113,210`; `Expert Mode/Profile/ExpertProfileHomeScreen.swift:132,237` | **Missing** in both modes. | `GET` / `PATCH /notifications/preferences` | DEF-6 | M | Reuse the debounce from `and/screens/settings/clientprofilesettings/ProfileAutoSave.kt` and a Material `Switch` and `TimePicker`. Needs new DTOs. Check whether the Android push-channel setup should honor quiet hours (server-side, probably). |
| 6 | **Send Feedback**: feedback type picker loaded from the server, details field with validation, email prefilled from the user, submit, thanks state, error handling. "Send Feedback" row in **both** hubs. | `Settings/Feedback/FeedbackView.swift`, `FeedbackViewModel.swift:108,125,152-204`; hubs `:125,224` (client), `:144,247` (expert) | **Missing** in both modes. | `GET /feedback/types`, `POST /feedback/submit` | DEF-6 | M (about 1 day) | Reuse `PickerSheet` / `PickerField`, `ValidatedField`, `AsyncSaveButton`. |
| 7 | **Client private booking note**: load, edit, save with a 1.5 s "Saved" state. Entry points: "Private Note" on past booking cards, upcoming booking cards, and Booking Summary. | `Client Mode/Booking Note/ClientBookingNoteView.swift`, `ClientBookingNoteViewModel.swift:66,97`; `My Bookings/MyBookingsView.swift:35-40,124,156-170,356,375`; `My Bookings/Subviews/PastBookingCard.swift:16`, `UpcomingBookingCard.swift:23`; `Booking Summary/BookingSummaryView.swift:28` | **Missing.** Android cards have no note action (`and/screens/clientmode/mybookings/subviews/UpcomingBookingCard.kt:88-93`, `PastBookingCard.kt:73-75`). The DTOs omit the note on purpose (`model/responses/ClientBookingsPayload.kt:16`: "`private_note_preview` is deliberately not declared (D-16)"; `ClientBookingDetail.kt:11`). | `GET` / `POST /client/bookings/:id/note` | DEF-3 | M | Add `private_note_preview` and `private_note` to the DTOs, a new NoteKey and screen, and entries in `ClientBookingManagementEntries.kt`. Share the editor composable with #8. |
| 8 | **Expert private booking note**: "My Note" card on Expert Booking Summary showing the note inline (or an empty prompt) and opening the editor; "Private note" on expert upcoming and past cards. | `Expert Mode/Booking Note/ExpertBookingNoteView.swift`, `ExpertBookingNoteViewModel.swift:61,89`; `Expert Mode/Booking Summary/ExpertBookingSummaryView.swift:74,600-630`; `Upcoming Bookings/UpcomingBookingsView.swift:81-86,180-187`; `Subviews/PastSessionCard.swift:20`, `UpcomingBookingsSessionCard.swift:23` | **Missing.** `and/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt:89`: "There is no Reschedule or Private Note control (DEF-1, Phase 8 D-16)". The summary shows only the client's preparation notes (`ExpertMessageCard`). | `GET` / `POST /bookings/:id/note` | DEF-3 | M | Same editor as #7. Add `expert_private_note` and `private_note_preview` to `ExpertBookingsPayload.kt`. |
| 9 | **Client Profile (expert side)**: avatar, name and "Client" badge, About/Bio and personal info, "Your Sessions Together" card, booking history built from expert upcoming + past bookings filtered to this client. Opened from "View Profile" on Expert Booking Summary and from the chat thread header in expert mode. | `Expert Mode/Client Profile/ClientProfileView.swift`, `ClientProfileViewModel.swift:78-90`, `Subviews/*`; `Expert Booking Summary/ExpertBookingSummaryView.swift:297-306`; `Expert Mode/Home/HomeExpertView.swift:218-221`; `Chat/Chatting/ChattingView.swift:319-326` | **Missing.** Android chat only shows View profile in client mode (`and/screens/chat/chatting/ChatThreadScreen.kt:239` `showViewProfile = mode == AppMode.Client && state.peerIsExpert == true`). The expert summary has no View Profile link. | `GET /expert/clients/:id`, plus `GET /expert/bookings?type=upcoming/past` | DEF-3 | M | Reuse `ExpertBookingsRepository`, `BookingStatusBadge`, `Avatar`. Extend `openChatPeerProfile` in `and/screens/chat/` to route to the new key in expert mode. |
| 10 | **Client reschedule start**: month calendar and slots for the expert, optional message, current-slot guard, PATCH propose, and a **PAYMENT_METHOD_REQUIRED recovery** (fetch saved cards, retry with the default or first `pm_` as `stripe_payment_method_id`, otherwise ask the user to add a card). Also the payment-conversion disclosure (`requiresPaymentConversionDisclosure`, `chargeWillFireSoon`), RESCHEDULE_OUTSIDE_AUTH_WINDOW copy, and refusal of moves <24 h out. Entry points: "Reschedule" on Upcoming Booking Details and on upcoming cards in My Bookings. | `Client Mode/Reschedule/RescheduleView.swift`, `RescheduleViewModel.swift:272-362,401-418`, `Subviews/*`; `Upcoming Booking Details/Subviews/UpcomingBookingSecondaryActions.swift:14-23`; `My Bookings/MyBookingsView.swift:31,110,152,353`; backend `docs/ios/RESCHEDULE-PAYMENT-CONVERSION.md` §1–§5 | **Missing.** Android has only *respond* (accept/decline): `and/screens/clientmode/mybookings/RescheduleResponse.kt`, `RescheduleErrors.kt`. `UpcomingBookingScreen.kt` has no Reschedule button (callbacks at :126-190 are Back, Cancel, Join, Message, Accept, Decline). | `PATCH /client/bookings/:id/reschedule` (body: proposed_start, timezone, note, optional stripe_payment_method_id), `GET /experts/:id/availability`, `GET /me/payment-methods` | DEF-1 | L | **Highest risk (money).** Reuse `BookingCalendar.kt`, `BookingSlots.kt` / `groupSlots`, `BookingTimezoneNote.kt`, the MakeABooking calendar and slot cards, and the D-02 recovery in `RescheduleResponse.kt` (same retry-once, no-loop rule). Must add to the cross-platform matrix (authorized booking moved past the auth window; one-off PaymentSheet card leading to PAYMENT_METHOD_REQUIRED twice). |
| 11 | **Expert reschedule start**: same picker against the expert's own availability. Gated by `actions.canReschedule`. Entry points: "Reschedule" on Expert Booking Summary and on expert upcoming cards. | `Expert Mode/Reschedule/ExpertRescheduleView.swift`, `ExpertRescheduleViewModel.swift:259-298`; `ExpertBookingSummaryView.swift:194-202`; `Upcoming Bookings/Subviews/UpcomingBookingsSessionCard.swift:15`; `HomeExpertView.swift:215`; `Model/Responses/ExpertBookingDetail.swift:56` | **Missing.** The Android `ExpertBookingActionsDto` has `can_cancel` but no `can_reschedule` (`model/responses/ExpertBookingsPayload.kt:93,126`). | `PATCH /expert/bookings/:id/reschedule` | DEF-1 | M | Build after #10 and share its picker. Server-side the expert proposal creates a pending reschedule that the client accepts through the existing Android flow (with PM recovery), so test the iOS client ↔ Android expert pair. |
| 12 | **Expert Bookings empty state "View Past History"** button, which switches to the Past tab. | `Expert Mode/Upcoming Bookings/UpcomingBookingsView.swift:223-232`, `Subviews/UpcomingBookingsEmptyState.swift:13` | **Shipped.** `and/screens/expertmode/upcomingbookings/ExpertBookingsScreen.kt` has the `TextButton` calling `onSelectTab(BookingsTab.Past)`, label `expert_bookings_view_past`; `ExpertBookingsScreenTest.emptyUpcomingOffersAvailabilityAndPast` covers it (corrected in Phase 16.5). | none | none | S | Trivial: call `onSelectTab(BookingsTab.Past)`. |
| 13 | *(optional, cosmetic)* Animated splash: logo fade/scale and glow pulse. | `iOS/Views/Screens/Splash/SplashView.swift:17-26` (`SplashScreenView`, used by `ClickMe2026App.swift:46`) | Android uses the system SplashScreen API (Phase 1, 01-04). | none | DEF-7 | S | Platform-acceptable difference. Recommend leaving it as is. |
| 14 | *(optional)* Delete Account "Before you go…" pause card | `Settings/Delete account/DeleteAccountView.swift:91-95`, `Subviews/DeleteAccountPauseCard.swift` | Not in `and/screens/settings/deleteaccount/` (no "pause" match). | none | none | S | **On iOS `onPauseAccount` defaults to `{}` and `AccountSecurityView.swift:207` never passes one**, so the card does nothing on iOS. Recommend skipping it. |

Hub-row wiring for #2, #4, #5 and #6 sits in `ClientProfileHomeScreen.kt` / `ExpertProfileHomeScreen.kt` (`ProfileHubActions`) and the shell entries (`and/screens/settings/SettingsEntries.kt`). It is counted inside each item.

### Checked and at parity (no gap found)
- Auth: Login, Forgot / Set New / Password Changed.
- Signup: Initial, Overview, Basic Info + Language sheet, Timezone, Tags, Verify Email (send-on-tap per `EMAIL-VERIFICATION-ON-REQUEST.md`), Profile Photo + crop, Review.
- Onboarding, Mode Switch.
- Client booking flows: Explore (sections, categories sheet, Today's Sessions), Expert Profile (Book, Message, Report/Block, self-view), Make a Booking (topics, calendar, slots, PaymentSheet, payment-info sheet), Request Sent (+ PaymentLifecycleCard), My Bookings, Upcoming Booking Details (Join, Copy link, Cancel, Message, reschedule response), Booking Summary (Book Again, Message Expert, Leave review, reschedule response), Client Cancellation, Write Review.
- Call: MeetingCall incl. external/Skype provider and the overlay pill.
- Chat: Conversations (with search), Chatting, Report & Block.
- Notifications list, push routes (all 8 `PushRouter` destinations).
- Expert mode: Dashboard (all 6 Expert Tools), Incoming Requests → Request Decision → Booking Confirmed (calendar intent), Expert Booking Summary (except #8, #9, #11), Expert Cancellation, Topics + Topic Editor, Availability, Expert Settings + Expertise editor, Payouts + Transactions, Tax Profile.
- Settings: Account Security, Delete Account, Help (mailto), Client Profile Settings, Payment Methods (incl. set default).
- Realtime event keys match.

## Corrections (Phase 16.5, 2026-11-02)

- **#5 Notification preferences:** iOS never instantiates its quiet-hours card and the backend stores no such field, so none was built (D-01). This also answers the question under "Things I'm unsure about".
- **#12 View Past History:** already shipped, no code change; verified in Phase 16.5 (see the corrected row above).
- **#13 Animated splash:** built as a Compose stage after the system splash, overriding this audit's recommendation by user decision (D-07 to D-09).
- **#14 Delete Account pause card:** deliberately not built, because the iOS card does nothing and no backend route exists (D-10).

## Proposed phases (ordered by user value and risk)

**Phase A: Search tab (DEF-2).** About 3 plans, L.
1. Network: `/experts/search` DTO and Retrofit call, fake handler, wire tests.
2. `SearchViewModel`: debounced query, category from `CategoryStore`, sort, latest-wins, state.
3. `SearchScreen` (search field, chips via `SelectableChip`, sort as a `ModalBottomSheet` or menu, result cards reusing explore `ExpertCard`, empty/error/refresh); replace `ClientTabRoot(SearchRoot)`; add "See all" on Explore Featured Experts and Topics that selects the Search tab via `Navigator`.

**Phase B: Reschedule start, client + expert (DEF-1). Highest risk, so schedule second to leave Phase-16 matrix time.** About 4–5 plans, L+M.
1. Network: both PATCH endpoints with the optional `stripe_payment_method_id`; add `can_reschedule` to client and expert action DTOs (iOS `ExpertBookingDetail.swift:56`, `ExpertBookingItem.swift:51`); error codes RESCHEDULE_OUTSIDE_AUTH_WINDOW and PAYMENT_METHOD_REQUIRED.
2. Shared reschedule picker (extract from MakeABooking: `BookingCalendar`, `BookingSlots`, calendar and slot cards, `BookingTimezoneNote`) plus a message card.
3. Client `RescheduleViewModel`: current-slot guard, propose, PM recovery mirroring `RescheduleResponse.kt`, disclosure rules from RESCHEDULE-PAYMENT-CONVERSION §2, error copy from iOS `RescheduleViewModel.swift:352-361`.
4. Expert reschedule screen.
5. Entry points: Upcoming Details secondary row, My Bookings upcoming card, Expert Summary, expert upcoming card; success pops to root and reloads lists. Add matrix rows to the Phase 16 runbook.

**Phase C: Expert Profile extras + Saved Experts (DEF-3 part).** About 3 plans, M+S+S.
1. Favorites repository (3 endpoints) and heart on Expert Profile with optimistic rollback and initial-state reconcile.
2. Favorites screen and client hub "Saved Experts" row.
3. Full Reviews list screen and "See all" on `ExpertProfileReviewsCard`.

**Phase D: Booking notes + Client Profile (DEF-3 rest).** About 4 plans, M+M+M.
1. Note DTO fields and both note endpoints.
2. Shared `BookingNoteEditor` screen (client and expert keys).
3. Entry points: client past/upcoming cards, Booking Summary, expert "My Note" card, expert cards.
4. Client Profile screen; Expert Summary "View Profile"; expert-mode chat header View profile.

**Phase E: Settings extras (DEF-4 client, DEF-6 rest).** About 4 plans, M+M+M+S.
1. Client Payment History (paged, 500-past-end rule, row to Upcoming Details) and hub row.
2. Notification Preferences (both hubs, debounced auto-save, quiet hours).
3. Feedback (both hubs).
4. Small polish: "View Past History" button (#12).

The timeline is tight. Today is Oct 30, and Phase 16 QA and Phase 17 release still have to fit before Dec 11–13. A and B carry the most user value. C, D and E are independent of each other and can run in parallel worktrees. If you have to cut, cut in this order: E3 Feedback, then E2 Notification prefs, then D4 Client Profile.

### Riskiest items
1. **#10 client reschedule start.** It moves an authorized payment; see `RESCHEDULE-PAYMENT-CONVERSION.md` §1 (`authorization_expires_at` no longer gates the move), §3.1 (PAYMENT_METHOD_REQUIRED fires even with a saved card, because a one-off PaymentSheet card is single-use) and §5 (the T-24h failure policy). It needs live cross-platform QA in both directions.
2. **#11 expert reschedule.** The client accepting the expert's proposal triggers the payment-method recovery that already exists on Android. Re-test it once proposals can come from an Android expert.
3. **#1 Search.** Moderate risk: it's a new endpoint and a new tab root, and it interacts with per-tab back stacks and tab re-tap.

## Things I'm unsure about
- **The `/experts/search` contract:** I didn't open the iOS `ClickMeAPI+Discovery.swift` parameter list or the backend. Check the query parameter names (q / category / sort / page) before planning A.
- **Notification preferences shape:** I didn't confirm whether quiet hours are stored on the server (in the PATCH body) or only locally on iOS. Read `NotificationSettingsViewModel.swift:138` `buildPreferenceInputs` before planning.
- **Client reschedule gating:** I didn't confirm whether iOS hides the client "Reschedule" button based on a server action flag (the client detail DTO on iOS may not have `can_reschedule`) or always shows it and lets the server refuse.
- **Explore search bar:** `ExploreSearchBar.swift` exists on iOS but is never used. I assumed it's not a requirement.
- **Unreachable iOS screens:** `ReadyToStartCallView` and `CancellationPoliciesView` are never instantiated on iOS, so I excluded them as dead code. Confirm with the iOS owner if unsure.
- **Small actions:** I compared callback signatures and screen wiring, not every string or button inside each screen. Small actions inside screens marked "at parity" (for example individual error dialogs) could still differ. Phase 16 QA is the backstop.
- **Sizes:** the estimates assume executor-days with the current reuse level. Reschedule could run over if the slot-picker extraction from MakeABooking turns out to be entangled.
