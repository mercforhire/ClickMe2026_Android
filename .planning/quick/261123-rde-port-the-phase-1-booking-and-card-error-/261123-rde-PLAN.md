---
phase: quick-261123-rde
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
  - app/src/main/res/values/strings_make_a_booking.xml
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowErrorTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelPaidTest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreview.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/subviews/ExpertCancelConfirmStep.kt
  - app/src/main/res/values/strings_expert_cancel.xml
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ExpertBookingFixtures.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreviewTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertBookingsApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrorsTest.kt
autonomous: true
requirements: [PAY-06, EXP-09, PAY-01, BOOK-07]

estimate:
  tokens: 95000
  raw_tokens: 95000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "Paid flow, BACK-15 (iOS D-15): a 409 DUPLICATE_BOOKING whose detail.booking_id is a canonical UUID string, received while the current attempt id is the one whose /bookings/confirm ended with an unknown outcome, opens Request Sent for that booking id (completedBookingId set, no dialog, isSubmitting stays true)"
    - "Paid flow, iOS D-04: every other DUPLICATE_BOOKING (no unknown outcome on this attempt, or booking_id missing, blank, not a string or not a canonical UUID, in the create-intent or the confirm phase) clears the selected slot, mints a new attempt id, reloads availability and shows the dialog titled \"Couldn't book\" with \"You may already have this booking. Check My Bookings.\""
    - "Free path is unchanged: a DUPLICATE_BOOKING on POST /bookings/request still shows \"A booking for this session already exists.\" (iOS BookingErrorMapper; backend createRequest at 1d18aac never returns the code)"
    - "Expert cancel, D-14/D-16 (BACK-19/D-17): the confirm step shows a client-money line only when the fee amount is above 0 and payment_action_required and payment_not_charged are both present and false. Any other combination (free, either key missing, card authentication pending, nothing charged) shows no money line and no footnote"
    - "Expert cancel copy matches iOS v1.1 ExpertCancelRefundLine (WR-03 f649900): a live hold (authorization_expires_at non-null) reads \"The client's hold of up to {amount} will be released.\" with the lock icon and no footnote; otherwise \"The client will get a full refund of up to {amount}\" with the card icon plus the footnote \"Refunds typically appear in the client's account within 3–5 business days.\"; with no usable currency or a non-finite amount the sentences read \"The client's hold will be released.\" and \"The client will get a full refund.\""
    - "No expert cancel surface states a within-24-hours no-refund outcome or the generic policy line, because an expert cancel always releases or refunds in full (backend expertBooking.controller.js cancel, D-17 at 1d18aac)"
    - "BACK-07: a card delete answered 409 PAYMENT_METHOD_IN_USE shows the server message \"This card is linked to an upcoming booking. You can remove it once that booking is completed or cancelled.\" under \"Couldn't remove\" and the card stays in the list (iOS shows error.userMessage)"
    - "BACK-12: a client cancel answered 409 SESSION_ALREADY_ENDED shows the server message \"This session has already ended, so it can't be cancelled.\" under \"Couldn't cancel booking\", stays on the confirm step and does not leave for My Bookings (iOS cancelError = error.userMessage)"
  artifacts:
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt"
      provides: "PaidFailure.OpenBooking(bookingId) and the DUPLICATE_BOOKING rule keyed on detail.booking_id plus afterUnknownOutcome"
      contains: "data class OpenBooking("
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt"
      provides: "applyPaidFailure branch that hands the server booking id to completedBookingId"
      contains: "is PaidFailure.OpenBooking"
    - path: "app/src/main/res/values/strings_make_a_booking.xml"
      provides: "make_a_booking_error_may_already_booked (iOS mayAlreadyBeBooked message)"
      contains: "You may already have this booking. Check My Bookings."
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt"
      provides: "ExpertBookingDetail.paymentActionRequired and paymentNotCharged, nullable, additive"
      contains: "@Json(name = \"payment_not_charged\") val paymentNotCharged: Boolean? = null"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreview.kt"
      provides: "Port of iOS ExpertCancelRefundLine.make: None, HoldReleased, FullRefund"
    - path: "app/src/main/res/values/strings_expert_cancel.xml"
      provides: "iOS v1.1 expert money sentences and the unchanged 3–5 business days footnote"
      contains: "The client will get a full refund of up to %1$s</string>"
  key_links:
    - from: "PaidFlowError.kt DUPLICATE_BOOKING row"
      to: "MakeABookingViewModel.applyPaidFailure -> completedBookingId -> MakeABookingScreen LaunchedEffect -> onBooked -> replaceTop(RequestSentKey(id))"
      via: "PaidFailure.OpenBooking(bookingId) built from ApiException.Http.detail[\"booking_id\"]"
      pattern: "is PaidFailure.OpenBooking"
    - from: "ExpertBookingDetail.paymentActionRequired / paymentNotCharged / authorizationExpiresAt"
      to: "ExpertCancellationViewModel.previewFor -> expertRefundPreview -> ExpertCancelConfirmStep RefundRow and footnote"
      via: "named arguments in previewFor"
      pattern: "paymentNotCharged = detail.paymentNotCharged"
---

<objective>
Port four Android follow-ups logged in the iOS project (iOS .planning/STATE.md, "Android follow-ups from Phase 1" and "from Phase 3"), matching iOS v1.1 behavior and copy and the production backend at 1d18aac:

1. BACK-15: route a paid-flow 409 DUPLICATE_BOOKING to the server's own booking (detail.booking_id) exactly where iOS does (iOS D-15). Otherwise use the iOS D-04 fallback.
2. BACK-19 / D-17, applying the iOS Phase 3 D-14/D-16 rule (iOS 03-05 and 03-08, reworded by WR-03): the expert cancel confirm step makes a client-money claim only when the payload proves money is held or was taken.
3. BACK-07 (PAYMENT_METHOD_IN_USE on card delete) and BACK-12 (SESSION_ALREADY_ENDED on client cancel): Android already matches iOS here. Both platforms show the server message, and the card stays or the cancel stays on its confirm step. These get pinning tests only.

Purpose: Android users must not be told the client gets a refund that will never happen, or be shown a dead-end message when the server has already pointed to their booking.

Output: code and string changes for items 1 and 2, regression tests for all four items, and a green full unit-test, ktlint and lint run. All commits are local on main, and nothing is pushed.

Recorded finding (the brief asked for it): the earlier Android port of iOS 08-12 (Android commit 2ffb5d23) left the expert footnote alone for two reasons. iOS ExpertCancellationMainCard says the same thing, and the iOS todo marked expert-cancel wording out of scope (the iOS Phase 6 Q2 wording question). Sources: iOS .planning/phases/08-release/08-12-SUMMARY.md line 54 and iOS .planning/todos/completed/2026-11-13-android-cancellation-policy-link.md lines 30 and 40. The first reason still holds: iOS v1.1 still shows "Refunds typically appear in the client's account within 3–5 business days.", for the full-refund line only. So this plan keeps that footnote text unchanged, shows it only with the full-refund line, and does not copy the client "Your bank decides..." wording onto the expert sheet. The final report must flag this to the user.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md

Cross-repo sources (absolute paths, read-only; never edit, commit or push in these repos):
- iOS rule and copy: /Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Utils/ExpertCancelRefundLine.swift, its tests /Users/LeonChen/Documents/ClickMe2026/ClickMe2026Tests/Utils/ExpertCancelRefundLineTests.swift, and the footnote in "/Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Expert Mode/Cancellation/Subviews/ExpertCancellationMainCard.swift" (line 38, shown only when showsFootnote)
- iOS duplicate routing: "/Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Views/Screens/Client Mode/Make a Booking/MakeABookingViewModel.swift" lines 843-864 (case .duplicateBooking) and MakeABookingViewModel+PaidFlow.swift lines 15-50 (mayAlreadyBeBooked title "Couldn't book", message "You may already have this booking. Check My Bookings.")
- Backend at production SHA 1d18aac (read with `git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" show 1d18aac:<path>`): src/controllers/booking.controller.js createPaymentIntent lines 478-500 (DUPLICATE_BOOKING with detail.booking_id, only the caller's own booking); src/controllers/expertBooking.controller.js lines 163-186 (paymentNotCharged), 405-416 (detail payload) and 1150-1168 (cancel, D-17); src/controllers/paymentMethods.controller.js lines 288-293; src/controllers/clientBooking.controller.js line 846

<interfaces>
Paid flow (Task 1):
- PaidFlowError.kt: `sealed interface PaidFailure` has `Silent`, `StartChallenge(clientSecret, stripeAccount)` and `Show(message: FieldError, paymentReference, newAttemptId, clearSlotAndReload, resetMeetingType, outcomeUnknown)`. `fun ApiException.toPaidFailure(phase: PaidPhase, paymentIntentId: String?, afterUnknownOutcome: Boolean = false): PaidFailure`. The private `ApiException.Http.toHttpPaidFailure(confirming, reference, afterUnknownOutcome)` has the row `ErrorCode.DuplicateBooking -> res(R.string.make_a_booking_error_duplicate)` at line 127. `challengeOrFailure` shows how `detail` is read (`detail?.get("client_secret") as? String`).
- ApiException.Http fields: `status`, `code: ErrorCode?`, `rawCode`, `serverMessage`, `fieldErrors`, `detail: Map<String, Any?>?`, `userMessage`.
- MakeABookingViewModel.kt: `unknownOutcomeAttemptId` (line 142); `applyPaidFailure(e, phase, piId)` (around line 580) computes `afterUnknown` and switches on the failure; the confirmPaid success block (around lines 561-572) is the terminal pattern (`clearPaidSecrets()`, `savedStateHandle[KEY_COMPLETED_BOOKING_ID] = id`, update `paidStep = PaidStep.Idle, stripeRequest = null, challengeAccount = null, completedBookingId = id`, isSubmitting left true); `resetPaidFlow()` (line 684) also clears `scaAttempted` and `KEY_CHALLENGE_PENDING` but sets isSubmitting false. The create-intent calls are at lines 440 and 449 (`applyPaidFailure(e, PaidPhase.CreateIntent, null)`).
- MakeABookingScreen.kt line 108: `LaunchedEffect(state.completedBookingId)` calls onBooked, and the shell maps it to `replaceTop(RequestSentKey(id))`. Request Sent loads the booking through GET /client/bookings/:id.
- BookingErrors.kt line 51 (free path, `toBookingFailure`): stays as is.
- Tests: PaidFlowErrorTest has `http(status, code, message, detail)` at line 14, `createIntent(error)` at line 30, `confirm(...)` at line 32, and the table test `mapped_codes_use_their_copy_and_carry_no_reference_even_in_confirm` (line 92), which lists DuplicateBooking. MakeABookingViewModelPaidTest has `http(status, code, message, detail)` at line 71, `paidVm()`, `bookAndRun(vm)`, `bookThroughTheSheet(vm)`, `dialog(vm)`, `api.paymentIntentHandler`, `api.confirmBookingHandler` and `api.paymentIntentRequests`. The pattern test is `aSlotConflictAfterAnUnknownOutcomeOnTheSameAttemptPointsToMyBookingsOnce` (line 692).

Expert cancel (Task 2):
- ExpertBookingsPayload.kt: `ExpertBookingDetail` (line 93) already has `@Json(name = "authorization_expires_at") val authorizationExpiresAt: Instant? = null`; `ConsultationFeeDto(amount: Double?, currency: String?, label: String?)` with `isPaid`. The toString override must stay.
- ExpertRefundPreview.kt today: the `PaymentState` enum, a sealed `ExpertRefundPreview` with five variants (a captured full refund, a captured within-24-hours no-refund, a held release, a captured price-unknown fallback, None), `expertRefundPreview(payment, amountMajor, currency, start, now)` using the client 24-hour constant from clientmode/cancellation/RefundPreview.kt, and `ExpertBookingDetail.paymentState()`. Only `ExpertCancellationViewModel.previewFor` (line 91) consumes them.
- ExpertCancellationViewModel.kt: the constructor `clock: Clock` (line 52) is used only by previewFor (line 98). Test construction sites: ExpertCancellationViewModelTest.kt line 47 (MutableClock at line 44, passed at line 51) and ExpertCancellationScreenTest.kt line 82.
- ExpertCancelConfirmStep.kt: the footnote `if (refund is ...)` is at line 208; `RefundRow(refund, locale)` is at line 292; `amountEmphasised(text, amount, style)` is at line 317. Icons used: Icons.Outlined.Lock, Icons.Outlined.CreditCard.
- Money.kt: `Money.formatMajor(major: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String` (line 35). BigDecimal.valueOf throws for NaN and Infinity, so check finiteness before calling it.
- Fixtures and tests: `expertBookingDetail(...)` in testing/ExpertBookingFixtures.kt (line 82) has no authorizationExpiresAt or payment-key parameters. ExpertCancellationViewModelTest `stubDetail(fee)` is at line 56. ExpertBookingsApiTest `detailJson(feeAmount, includeFee)` is at line 74, and its JSON at line 88 carries authorization_expires_at. With Locale.US, the existing screen tests render USD as "US$75.00".

Pins (Task 3):
- PaymentMethodsViewModel.remove -> runCardOperation catch: `dialog = e.toDialogError()?.let { PaymentMethodsDialog(errorTitle, it) }`; the list is not reloaded on failure. Pattern test: PaymentMethodsViewModelTest `removeFailureShowsRemoveDialogAndSuspendedShowsNothing` (line 192), `vm.cards()` (line 49), `api.httpError(status, json)`.
- Client cancel: `ApiException.toCancelFailure()` in clientmode/mybookings/RescheduleErrors.kt (line 106) sends unknown codes to `CancelFailure.Show(serverTextOrUnknown())` with returnToBookings false. Pattern tests: ClientCancellationViewModelTest `otherErrorsShowTheServerMessageAndAllowARetry` (line 392) and RescheduleErrorsTest `cancel_other_errors_stay_on_the_confirm_step` (line 163).
- Neither code is an Android or iOS ErrorCode entry, and none is needed: neither platform branches on it, and ErrorCode.fromWire maps an unknown code to null, which reaches the server-message row.
</interfaces>

Build environment and rules:
- Set `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` and run every command from the checkout root. Never run `./gradlew --stop`. The test task needs `-Pkotlin.daemon.jvmargs=-Xmx3g`. lintDevDebug also needs `-Dorg.gradle.jvmargs=-Xmx4g`.
- Style: ktlint_official with a 120-column limit. A function or call with two or more parameters puts one parameter per line. Pass the preview inputs in previewFor as named arguments so the key-link greps can find them.
- No production calls: every test uses the existing fakes and MockWebServer. Do not run the app against the live backend.
- Another quick task (261123-rdc: JoinWindow.kt, ExpertBookingBuckets, ExpertBookingSummaryUiState) runs sequentially on main. Do not touch, stage or revert those files. At planning time the working tree already had uncommitted changes to JoinWindow.kt, JoinWindowTest.kt, UpcomingBookingScreen.kt and UpcomingBookingUiState.kt. Leave them exactly as they are. Re-read ExpertBookingFixtures.kt and ExpertBookingsPayload.kt immediately before editing, in case 261123-rdc changed them, and keep its changes.
- Commits: local commits on main, with the subject prefix `fix(quick-261123-rde):` or `test(quick-261123-rde):`, one per task. Stage only this plan's files, never the unrelated .idea or .planning changes in the working tree. Do not push.
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: Paid DUPLICATE_BOOKING opens the server's booking end to end (BACK-15), with the iOS D-04 fallback</name>
  <files>app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt, app/src/main/res/values/strings_make_a_booking.xml, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowErrorTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelPaidTest.kt</files>
  <precondition>The production backend still sends the caller's own booking id with this 409: `git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" show 1d18aac:src/controllers/booking.controller.js | grep -qF "throw new AppError(409, 'DUPLICATE_BOOKING', 'You already have a booking for this time.', {"` exits 0.</precondition>
  <behavior>
    - toPaidFailure(CreateIntent, afterUnknownOutcome = true) on 409 DuplicateBooking with detail booking_id "3fa85f64-5717-4562-b3fc-2c963f66afa6" returns PaidFailure.OpenBooking("3fa85f64-5717-4562-b3fc-2c963f66afa6"). The uppercase form of the same UUID is accepted too.
    - The same 409 with afterUnknownOutcome = false, in both phases, returns Show(Res(make_a_booking_error_may_already_booked), newAttemptId = true, clearSlotAndReload = true) and carries no payment reference.
    - With afterUnknownOutcome = true but booking_id absent, null, "", "not-a-uuid", "1-1-1-1-1" (which UUID.fromString accepts but which is not canonical) or the number 42, the result is the same fallback Show.
    - DuplicateBooking is no longer a row in the mapped-codes table test, and every other row of that table is unchanged.
    - ViewModel: after a confirm that throws IOException("timeout"), dismissError, then a payment-intent 409 DUPLICATE_BOOKING with a valid booking_id: completedBookingId equals that id, dialogError is null, isSubmitting is true, and paidStep is Idle.
    - ViewModel: a first-attempt payment-intent 409 DUPLICATE_BOOKING with a valid booking_id and no earlier unknown outcome: dialogError is Res(make_a_booking_error_may_already_booked), completedBookingId is null, selectedSlotStart is null, and the next payment-intent request carries a different bookingAttemptId.
  </behavior>
  <action>
Write the failing tests named in the verify block first, then the code (RED, then GREEN).

1. strings_make_a_booking.xml: add `make_a_booking_error_may_already_booked` = "You may already have this booking. Check My Bookings." (word for word from iOS PaidFlowAlert.mayAlreadyBeBooked). Leave `make_a_booking_error_duplicate` in place, because the free path still uses it. The dialog title is already "Couldn't book" (`make_a_booking_error_title`), which matches the iOS title.

2. PaidFlowError.kt: add `data class OpenBooking(val bookingId: String) : PaidFailure`. Its KDoc says this is the server's own booking for this expert, topic and start (backend createPaymentIntent at 1d18aac, BACK-15; the id is only ever the caller's). Replace the DuplicateBooking row with a call to a new private `ApiException.Http.duplicateBookingFailure(afterUnknownOutcome: Boolean)`. That function reads `detail?.get("booking_id") as? String` and keeps the id only when `java.util.UUID.fromString` parses it inside runCatching and the parsed `toString()` equals the id lowercased with Locale.ROOT. iOS `UUID(uuidString:)` accepts only the canonical 36-character form, either case. When afterUnknownOutcome is true and the id is usable, return OpenBooking with the id as received. Otherwise return `PaidFailure.Show(FieldError.Res(R.string.make_a_booking_error_may_already_booked), newAttemptId = true, clearSlotAndReload = true)`, the same in both phases. The KDoc must cite iOS MakeABookingViewModel `case .duplicateBooking` (D-15 for routing, D-04 for the fallback). It must say that Android's gate is the existing per-attempt unknown-outcome record (D-16/D-19), where iOS uses its UnconfirmedBookingLedger keyed by expert and slot, so Android routes in a narrower set of cases and shows the message otherwise. It must also say that the fallback reloads availability through the existing clear-and-reload flag, where iOS only clears the slot.

3. MakeABookingViewModel.kt applyPaidFailure: add a branch for the new OpenBooking failure that ends the flow the way a successful confirm does. Clear `unknownOutcomeAttemptId`, call clearPaidSecrets(), set scaAttempted to false, remove KEY_CHALLENGE_PENDING from the saved state, write `savedStateHandle[KEY_COMPLETED_BOOKING_ID]`, and update the state to `paidStep = PaidStep.Idle, stripeRequest = null, challengeAccount = null, completedBookingId = id`. Do not call resetPaidFlow(), which would unlock Book Now during the push to Request Sent. Do not write the durable CompletedBookingStore: it is keyed to a confirm attempt id, which the create-intent step does not set. The existing screen LaunchedEffect and the shell's replaceTop(RequestSentKey) then navigate, and Request Sent re-checks ownership through GET /client/bookings/:id. Update the applyPaidFailure KDoc to list BACK-15.

4. PaidFlowErrorTest: remove DuplicateBooking from the mapped-codes table, then add the three mapper tests below. MakeABookingViewModelPaidTest: add the two ViewModel tests below, modelled on the test at line 692. The free-path test `duplicateBooking` in MakeABookingViewModelBookTest stays as it is and must still pass.

Commit as `fix(quick-261123-rde): open the existing booking on a paid DUPLICATE_BOOKING (iOS BACK-15 parity)`.
  </action>
  <verify>
    <automated>JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests 'com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.*' ktlintCheck && grep -qF 'You may already have this booking. Check My Bookings.' app/src/main/res/values/strings_make_a_booking.xml && grep -q 'name="make_a_booking_error_may_already_booked"' app/src/main/res/values/strings_make_a_booking.xml && grep -q 'data class OpenBooking(' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt && grep -qF '"booking_id"' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt && grep -q 'is PaidFailure.OpenBooking' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt && grep -A1 'ErrorCode.DuplicateBooking ->' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrors.kt | grep -q 'make_a_booking_error_duplicate' && ( for t in duplicate_booking_after_an_unknown_outcome_opens_the_server_booking duplicate_booking_without_an_unknown_outcome_points_to_my_bookings_and_clears_the_slot duplicate_booking_with_an_unusable_booking_id_points_to_my_bookings; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowErrorTest.kt || exit 1; done ) && ( for t in aDuplicateBookingAfterAnUnknownOutcomeOpensTheExistingBooking aDuplicateBookingWithoutAnUnknownOutcomePointsToMyBookingsAndClearsTheSlot; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelPaidTest.kt || exit 1; done )</automated>
  </verify>
  <done>A paid DUPLICATE_BOOKING after an unknown confirm outcome with a canonical booking_id lands on Request Sent for that id. Every other paid DUPLICATE_BOOKING clears the slot and shows the iOS "You may already have this booking. Check My Bookings." message. The free path is unchanged. The makeabooking tests and ktlint are green, and one local commit is made.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Expert cancel money line follows the iOS D-14/D-16 rule and iOS v1.1 copy (BACK-19 / D-17)</name>
  <files>app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreview.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt, app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/subviews/ExpertCancelConfirmStep.kt, app/src/main/res/values/strings_expert_cancel.xml, app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ExpertBookingFixtures.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreviewTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModelTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreenTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertBookingsApiTest.kt</files>
  <precondition>Production sends both keys on the expert detail and iOS v1.1 ships the "up to" wording: `git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" show 1d18aac:src/controllers/expertBooking.controller.js | grep -qF 'payment_not_charged: paymentNotCharged(booking),' && git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" show 1d18aac:src/controllers/expertBooking.controller.js | grep -qF '...paymentActionFields(booking),' && git -C /Users/LeonChen/Documents/ClickMe2026 show v1.1:ClickMe2026/Utils/ExpertCancelRefundLine.swift | grep -qF 'return "The client will get a full refund of up to \(amount)"'` exits 0.</precondition>
  <behavior>
    - Mirror iOS ExpertCancelRefundLineTests case by case. Fee 0 or null gives None, whatever the keys read. Either key null, or both null, gives None, with or without a hold. paymentActionRequired true gives None. paymentNotCharged true gives None, even with a stale hold date.
    - Both keys false with a non-null authorizationExpiresAt gives HoldReleased(75.0, "USD"). Both keys false with a null authorizationExpiresAt gives FullRefund(75.0, "USD").
    - Screen (Locale.US): HoldReleased(75.0, "USD") shows "The client's hold of up to US$75.00 will be released." and no node containing "Refunds typically appear". FullRefund(75.0, "USD") shows "The client will get a full refund of up to US$75.00" and "Refunds typically appear in the client's account within 3–5 business days.". FullRefund(12.5, "USD") shows "...up to US$12.50". None shows neither sentence nor the footnote.
    - Screen fallbacks: HoldReleased(75.0, null) shows "The client's hold will be released.". FullRefund(Double.POSITIVE_INFINITY, "USD") shows "The client will get a full refund." and still shows the footnote, as iOS does.
    - ViewModel: a paid detail with both keys false and a hold reaches the confirm step with HoldReleased(75.0, "USD"). A paid detail with both keys absent gets None. The saved-state restore test now expects HoldReleased(75.0, "USD") once the stub carries both keys false and a hold.
    - Decoding: GET /expert/bookings/{id}/details JSON with payment_action_required and payment_not_charged true, false or missing decodes to true, false or null on each field.
  </behavior>
  <action>
Write the failing tests named in the verify block first, then the code (RED, then GREEN). Re-read ExpertBookingsPayload.kt and ExpertBookingFixtures.kt first (261123-rdc may have changed them).

1. ExpertBookingsPayload.kt ExpertBookingDetail: add, right after authorizationExpiresAt, `@Json(name = "payment_action_required") val paymentActionRequired: Boolean? = null` and `@Json(name = "payment_not_charged") val paymentNotCharged: Boolean? = null`. Their KDoc says the backend added both at 65bb2ed (live in 1d18aac), that iOS reads them the same way (ExpertBookingDetail.swift lines 86-101), and that a missing key means unknown and fails closed. Both fields are additive and nullable, so older payloads still decode. Keep the toString override unchanged.

2. ExpertRefundPreview.kt: rewrite it as a port of iOS ExpertCancelRefundLine. The sealed `ExpertRefundPreview` has exactly three members: `data object None`, `data class HoldReleased(val amountMajor: Double, val currency: String?)` and `data class FullRefund(val amountMajor: Double, val currency: String?)`. The rule is `fun expertRefundPreview(amountMajor: Double?, currency: String?, authorizationExpiresAt: Instant?, paymentActionRequired: Boolean?, paymentNotCharged: Boolean?): ExpertRefundPreview`. It returns None unless amountMajor is non-null and above 0. With both keys exactly false it returns HoldReleased when authorizationExpiresAt is non-null and FullRefund otherwise. Every other combination returns None. Delete the payment-state enum, the paymentState() extension, the captured within-24-hours variant and the captured price-unknown fallback variant, and drop the import of the client 24-hour constant. The KDoc must cite iOS ExpertCancelRefundLine.swift (D-14, D-16, WR-03) and backend expert cancel D-17 at 1d18aac: an expert cancel always releases or refunds in full, so no within-24-hours outcome exists. It must also note that the amount is the list fee, which is why the copy says "up to". Remove the KDoc claim that the expert detail exposes no payment status.

3. ExpertCancellationViewModel.kt previewFor: call expertRefundPreview with named arguments `amountMajor = fee?.amount`, `currency = fee?.currency`, `authorizationExpiresAt = detail.authorizationExpiresAt`, `paymentActionRequired = detail.paymentActionRequired` and `paymentNotCharged = detail.paymentNotCharged`. The rule no longer depends on time, so remove the `clock: Clock` constructor parameter and its import (Hilt needs nothing else), and reword the KDoc on previewFor, which still mentions the 24-hour boundary. Update both test construction sites: ExpertCancellationViewModelTest line 47 (and drop its MutableClock if nothing else in that file uses it) and ExpertCancellationScreenTest line 82.

4. strings_expert_cancel.xml: set the values to match iOS v1.1 word for word, apostrophes escaped as \'. `expert_cancel_refund_hold` = "The client's hold will be released." Add `expert_cancel_refund_hold_amount` = "The client's hold of up to %1$s will be released." Change `expert_cancel_refund_full` to "The client will get a full refund of up to %1$s". iOS has no trailing period on this sentence; copy that exactly. Add `expert_cancel_refund_full_no_amount` = "The client will get a full refund." Delete the within-24-hours no-refund string, because it is no longer used and lint would flag it as unused. Keep `expert_cancel_refund_note` exactly as it is, because iOS v1.1 still ships the same footnote (see the recorded finding in the objective). Leave `expert_cancel_result_refunded` alone: it is worded from the server's refund_amount after the cancel and is outside this item.

5. ExpertCancelConfirmStep.kt: RefundRow handles the three members. HoldReleased uses Icons.Outlined.Lock, FullRefund uses Icons.Outlined.CreditCard, and None shows nothing. Remove the within-24-hours row and the generic policy row from this screen. `cancel_booking_refund_policy` stays, because the client flow uses it. Format the amount through a private helper that returns `Money.formatMajor(amount, currency, locale)` only when the currency is non-null and non-blank and the amount is finite, and null otherwise. A non-null amount uses the "_amount" or "%1$s" string, with the amount set through the existing amountEmphasised helper (semibold, as iOS does). A null amount uses the plain sentence. Keep the footnote condition as "refund is FullRefund", which is iOS showsFootnote.

6. Tests: in ExpertBookingFixtures.kt, add `authorizationExpiresAt: Instant? = null`, `paymentActionRequired: Boolean? = null` and `paymentNotCharged: Boolean? = null` to `expertBookingDetail(...)` and pass them through. Rewrite ExpertRefundPreviewTest against the new rule (the cases in behavior). In ExpertCancellationScreenTest, replace the three dormant-row tests with the new screen tests and keep every other test unchanged. In ExpertCancellationViewModelTest, give `stubDetail` parameters for the keys and the hold, and update the two existing assertions on the hold variant at lines 122 and 383 to the new shape. Add the decode test to ExpertBookingsApiTest.

Commit as `fix(quick-261123-rde): expert cancel money line only when money is held or taken (iOS D-14/D-16, BACK-19 parity)`.
  </action>
  <verify>
    <automated>JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests 'com.uptrendinvestments.clickmeapp.screens.expertmode.*' --tests 'com.uptrendinvestments.clickmeapp.network.ExpertBookingsApiTest' ktlintCheck && grep -qF '@Json(name = "payment_action_required") val paymentActionRequired: Boolean? = null' app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt && grep -qF '@Json(name = "payment_not_charged") val paymentNotCharged: Boolean? = null' app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt && grep -q 'paymentNotCharged = detail.paymentNotCharged' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt && grep -q 'paymentActionRequired = detail.paymentActionRequired' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt && grep -q 'authorizationExpiresAt = detail.authorizationExpiresAt' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt && grep -qF 'The client will get a full refund of up to %1$s</string>' app/src/main/res/values/strings_expert_cancel.xml && grep -qF 'The client will get a full refund.</string>' app/src/main/res/values/strings_expert_cancel.xml && grep -qF 'hold of up to %1$s will be released.</string>' app/src/main/res/values/strings_expert_cancel.xml && grep -qF 's hold will be released.</string>' app/src/main/res/values/strings_expert_cancel.xml && grep -qF 'Refunds typically appear in the client' app/src/main/res/values/strings_expert_cancel.xml && ! grep -q 'Your client will receive a full refund' app/src/main/res/values/strings_expert_cancel.xml && ! grep -q 'The client.*payment hold will be released' app/src/main/res/values/strings_expert_cancel.xml && ! grep -rq 'expert_cancel_refund_none_24h' app/src/main && ! grep -rqE 'ClientNoRefundWithin24h|PolicyFallback' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation && ! grep -rq 'REFUND_FULL_WINDOW' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode && ( for t in holdLineStatesTheAmountWithNoFootnote fullRefundLineStatesTheAmountAndTheFootnote noMoneyLineShowsNeitherSentenceNorFootnote sentencesWithoutAnAmountFallBackToThePlainText; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreenTest.kt || exit 1; done ) && ( for t in freeBookingHasNoLineWhateverTheKeysRead eitherKeyMissingShowsNoLine cardAuthenticationPendingShowsNoLine notChargedShowsNoLine liveHoldReadsAsReleased paidWithoutAHoldReadsAsFullRefund; do grep -q "fun $t(" app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreviewTest.kt || exit 1; done ) && grep -q 'fun previewFollowsBothPaymentKeysAndTheHold(' app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModelTest.kt && grep -q 'fun detailDecodesPaymentActionRequiredAndPaymentNotChargedTrueFalseAndMissing(' app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertBookingsApiTest.kt</automated>
  </verify>
  <done>The expert cancel confirm step makes a client-money claim only when both keys read false, worded exactly as iOS v1.1. The 3–5 business days footnote is unchanged and appears only with the full-refund line. No within-24-hours or policy row remains on the expert screen. The expert and decode tests and ktlint are green, and one local commit is made.</done>
</task>

<task type="auto">
  <name>Task 3: Pin PAYMENT_METHOD_IN_USE (BACK-07) and SESSION_ALREADY_ENDED (BACK-12), then run the full gate</name>
  <files>app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModelTest.kt, app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrorsTest.kt</files>
  <precondition>Production returns both messages verbatim: `git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" show 1d18aac:src/controllers/paymentMethods.controller.js | grep -qF 'This card is linked to an upcoming booking. You can remove it once that booking is completed or cancelled.' && git -C "/Users/LeonChen/Documents/ClickMe2026 Backend" show 1d18aac:src/controllers/clientBooking.controller.js | grep -qF 'This session has already ended, so it can'` exits 0.</precondition>
  <action>
iOS handles both codes with no ErrorCode case and no local copy: it shows `error.userMessage`, which is the server message. Android already does the same through toDialogError and toCancelFailure. So this task adds tests only, with the real wire JSON, and changes no production code. If a test fails, the production code has drifted: stop and report it rather than adding a local string.

1. PaymentMethodsViewModelTest: add `removeACardInUseShowsTheServerMessageAndKeepsTheCard`. Load the cards, set `api.deletePaymentMethodHandler` to throw `api.httpError(409, ...)` with the JSON body status "error", code "PAYMENT_METHOD_IN_USE" and the exact backend message, call `vm.remove("pm_2")` and runCurrent. Then assert three things: the dialog equals `PaymentMethodsDialog(R.string.payment_methods_error_remove_title, FieldError.Text(<exact message>))`, `vm.cards()` still equals the loaded list including pm_2, and `busy` is null.

2. ClientCancellationViewModelTest: add `aSessionThatAlreadyEndedShowsTheServerMessageAndStaysOnConfirm`, modelled on the test at line 392. Make the cancel handler throw `apiError(409, "SESSION_ALREADY_ENDED", "This session has already ended, so it can't be cancelled.")`. Assert that the error equals `CancelErrorDialog(FieldError.Text(<that message>), false)` and that the step stays Confirm. After dismissError, also assert that no launch request is pending and isFinished is false.

3. RescheduleErrorsTest: add `cancel_session_already_ended_shows_the_server_message_and_stays`. A 409 with an unknown code (null code, rawCode "SESSION_ALREADY_ENDED") and the server message maps to `CancelFailure.Show(FieldError.Text(<message>), returnToBookings = false)`.

4. Run the full gate: the whole `:app:testDevDebugUnitTest`, `ktlintCheck` and `lintDevDebug`. Fix any failure that this plan caused. Report, rather than fix, any failure that was already there at HEAD or that comes from 261123-rdc's uncommitted files.

Commit as `test(quick-261123-rde): pin the server message for PAYMENT_METHOD_IN_USE and SESSION_ALREADY_ENDED (iOS BACK-07, BACK-12 parity)`.
  </action>
  <verify>
    <automated>JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest ktlintCheck && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g :app:lintDevDebug && grep -q 'fun removeACardInUseShowsTheServerMessageAndKeepsTheCard(' app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt && grep -qF 'This card is linked to an upcoming booking. You can remove it once that booking is completed or cancelled.' app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt && grep -q 'fun aSessionThatAlreadyEndedShowsTheServerMessageAndStaysOnConfirm(' app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModelTest.kt && grep -q 'fun cancel_session_already_ended_shows_the_server_message_and_stays(' app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrorsTest.kt && grep -q 'is PaidFailure.OpenBooking' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt && grep -q 'paymentNotCharged = detail.paymentNotCharged' app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt && ! grep -q 'Your client will receive a full refund' app/src/main/res/values/strings_expert_cancel.xml && GS="$(git status --porcelain -- app/src)" && [ -z "$(printf '%s\n' "$GS" | grep -E 'makeabooking/|expertmode/cancellation/|ExpertBookingsPayload|strings_make_a_booking|strings_expert_cancel|ExpertBookingFixtures|ExpertBookingsApiTest|PaymentMethodsViewModelTest|ClientCancellationViewModelTest|RescheduleErrorsTest')" ] && ! git merge-base --is-ancestor HEAD origin/main</automated>
  </verify>
  <done>Both server messages are pinned by ViewModel tests and a mapper test, with the card kept and the cancel kept on its confirm step. The full unit suite, ktlintCheck and lintDevDebug are green. app/src is clean, with every change committed locally on main and nothing pushed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| backend -> app (409 error body) | `detail.booking_id` is server-supplied and becomes a navigation target |
| backend -> app (expert detail payload) | `payment_action_required` and `payment_not_charged` decide whether the app makes a claim about the client's money |
| test run -> production backend | The production backend is live with real money; tests must not reach it |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-rde-01 | Spoofing | PaidFlowError duplicateBookingFailure, detail.booking_id | low | mitigate | The id is used only when it is a canonical UUID string and only after this attempt's confirm ended with an unknown outcome. Otherwise the user gets the message. At 1d18aac the backend only ever puts the caller's own booking in the field (T-01-28), and Request Sent loads it through GET /client/bookings/:id, which re-checks ownership. Tests cover the missing, blank, non-string, non-UUID and non-canonical ids. |
| T-rde-02 | Tampering | Expert cancel money line (ExpertRefundPreview, ExpertCancelConfirmStep) | medium | mitigate | A false refund or hold claim misinforms the expert about real money. The rule fails closed: a missing key, card authentication pending or nothing charged shows no line. Every key combination is unit-tested against the iOS truth table, and decoding is tested for true, false and missing. |
| T-rde-03 | Information Disclosure | PaidFailure.OpenBooking, ExpertBookingDetail | low | accept | A booking id and two Booleans are not sensitive. No logging is added, and the ExpertBookingDetail toString override is unchanged. |
| T-rde-04 | Denial of Service | Test and lint runs | high | mitigate | Every test uses the existing fakes and MockWebServer. No task runs the app against production or calls a backend endpoint. Backend facts are read only through `git show 1d18aac:<path>`, and nothing is pushed. |
| T-rde-SC | Tampering | npm/pip/cargo or Gradle dependency installs | low | accept | This plan installs or upgrades no package or dependency, so the package-legitimacy gate does not apply. |
</threat_model>

<verification>
- PaidFlowErrorTest proves the DUPLICATE_BOOKING truth table: routing only with an unknown outcome plus a canonical id, and the D-04 fallback everywhere else, in both phases.
- The MakeABookingViewModelPaidTest tracer proves the path from the 409 body through the mapper and ViewModel to completedBookingId, the existing trigger for Request Sent. The fallback test proves the slot is cleared and the attempt id rotated.
- ExpertRefundPreviewTest mirrors iOS ExpertCancelRefundLineTests. ExpertCancellationScreenTest proves the exact iOS v1.1 sentences, the icons' rows and that the footnote appears only with a full refund. The decode test proves both keys reach the model.
- The Task 3 tests pin the BACK-07 and BACK-12 server messages and the kept card and confirm step.
- The full :app:testDevDebugUnitTest, ktlintCheck and lintDevDebug runs are green. Every commit is local on main.
</verification>

<success_criteria>
- Android handles a paid DUPLICATE_BOOKING the way iOS v1.1 does: it opens the existing booking when a lost confirm left one, and otherwise points to My Bookings with the iOS message.
- The Android expert cancel sheet says exactly what iOS v1.1 says, and only when the production payload proves money is held or was taken.
- PAYMENT_METHOD_IN_USE and SESSION_ALREADY_ENDED are pinned at iOS parity (server message, no state loss), and no production code changed for them.
- The SUMMARY records the 08-12 footnote finding and the user-facing decisions listed in the output section.
</success_criteria>

<output>
Create `.planning/quick/261123-rde-port-the-phase-1-booking-and-card-error-/261123-rde-SUMMARY.md` when done. Besides the standard sections, it must record:
- The 08-12 footnote finding from the objective, with its two sources.
- That Android's full-refund row was dormant before this change, so the 3–5 business days footnote now appears on Android for the first time, for a paid booking without a live hold.
- That the expert copy follows iOS v1.1 ("up to", WR-03 f649900), not the iOS 03-08 text in the original follow-up, including the missing trailing period on the full-refund sentence.
- The known deviations from iOS: the fallback dialog has a single button where iOS adds "Check My Bookings"; Android's routing gate is per attempt, where iOS's ledger is keyed by expert and slot; the fallback reloads availability, which iOS does not do.
- That items 1 and 2 needed tests only, and that the card error title differs from iOS ("Couldn't remove" against "Something went wrong"). That title difference predates this task and was left unchanged.
</output>
