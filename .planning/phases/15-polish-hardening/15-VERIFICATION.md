---
phase: 15-polish-hardening
verified: 2026-10-29T23:30:00Z
status: human_needed
score: 4/4 roadmap success criteria verified at the JVM level (device residuals routed to human verification)
covered_files:
  - ".planning/phases/15-polish-hardening/15-01-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-01-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-02-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-02-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-03-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-03-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-04-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-04-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-05-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-05-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-06-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-06-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-07-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-07-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-08-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-08-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-09-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-09-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-10-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-10-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-11-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-11-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-12-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-12-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-13-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-13-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-14-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-14-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-15-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-15-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-16-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-16-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-17-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-17-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-18-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-18-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-19-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-19-SUMMARY.md"
  - ".planning/phases/15-polish-hardening/15-20-PLAN.md"
  - ".planning/phases/15-polish-hardening/15-20-SUMMARY.md"
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/components/EmptyState.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/components/RefreshFailedSnackbar.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCard.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/components/buttons/AsyncSaveButton.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/components/call/CallResumeNote.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/di/CallResumeModule.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallResumeMarker.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallResumeNotice.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHost.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallAvatar.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallControls.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallInfo.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ChatComposer.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/MessageBubble.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/subviews/ConversationRow.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/subviews/CancellationConfirmStep.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/subviews/CancellationReasonStep.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/categoriesmodal/AllCategoriesSheet.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrors.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingBookButton.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingCalendarCard.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTopicPicker.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTopicSheet.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/subviews/ProfileHubComponents.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/WriteReviewFooter.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/subviews/ConfirmedDetailsCard.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/TransactionsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestActionFooter.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/TopicsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/LoginScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/passwordchanged/PasswordChangedScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/onboarding/OnboardingScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/subviews/AccountSecurityCards.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/subviews/DeleteAccountComponents.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/modeswitching/ModeSwitchScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/ContentWidth.kt"
  - "app/src/main/res/values/strings.xml"
  - "app/src/main/res/values/strings_all_categories.xml"
  - "app/src/main/res/values/strings_call_resume.xml"
  - "app/src/main/res/values/strings_chat_conversations.xml"
  - "app/src/main/res/values/strings_chat_thread.xml"
  - "app/src/main/res/values/strings_expert_profile.xml"
  - "app/src/main/res/values/strings_explore.xml"
  - "app/src/main/res/values/strings_help.xml"
  - "app/src/main/res/values/strings_make_a_booking.xml"
  - "app/src/main/res/values/strings_payment_methods.xml"
  - "app/src/main/res/values/strings_payouts.xml"
  - "app/src/main/res/values/strings_payouts_activity.xml"
  - "scripts/check-design-rules-selftest.sh"
  - "scripts/check-design-rules.sh"
covered_digest: "v3:sha256:eee4bb7c491d1b529efe4fbf1bdcb03860cde5b97a6c8cfd23d24e6dcef49f6d"
behavior_unverified: 0
overrides_applied: 0
re_verification: null
advisory:
  - finding: "15-07 plan truth 'Book Session comes after the name, rating and About' is not met: the shipped order is name, rating, stats, Book Session, About (iOS parity)"
    category: other
    reason: "CLAUDE.md makes iOS the source of truth for behavior; the plan truth follows UI-SPEC, which contradicts iOS and CONTEXT D-14/D-08. Judgment call, not a roadmap criterion."
    evidence_status: "code read (ExpertProfileScreen.kt LoadedDetails); 15-07-SUMMARY records the deviation"
  - finding: "15-19 plan truth speaks of a 'pinned Book Now footer'; Book Now is the last item of the scrolling form (iOS parity)"
    category: other
    reason: "The substantive truth (Book Now reachable above a 300 px IME) is covered by the passing MakeABookingInsetsTest. Judgment call."
    evidence_status: "MakeABookingInsetsTest 2/2 green in app/build/test-results"
coincidental_reliance_items: []
human_verification:
  - test: "L1/B1/C1/K1 - complete each SC-4 flow with real TalkBack (log in, book a free session, join a call, send a chat message)"
    expected: "Reading order, names, roles, states, headings and focus match the semantics the Robolectric tests assert; nothing is spoken twice"
    why_human: "Compose accessibility checks and TalkBack speech cannot run under Robolectric; the JVM audit proves semantics exist, not that TalkBack speaks them sensibly"
  - test: "Y1-Y7, Y11 - rotate the Samsung, toggle dark mode and font scale, use 'Don't keep activities', force-stop during a call, and install prodRelease (R8) then restore from a deep screen"
    expected: "Same screen and typed text kept, secrets empty, call never auto-rejoins, back stack restores under R8 (add a NavKey keep rule if it lands on the start screen)"
    why_human: "Process death and the minified build need a device; JVM proves SavedStateHandle round-trips and the manifest lock only"
  - test: "Y2, Y8, Y9, Y16, C4, C5 - resizable emulator (900x400, 900 dp square) plus cutout, gesture and 3-button navigation"
    expected: "No content clipped or hidden behind bars, cutouts or the IME; 600 dp column centered; call controls reachable on short windows"
    why_human: "Real insets, cutouts and window resizing are device behavior"
  - test: "Y12 - run Accessibility Scanner over the four SC-4 flows"
    expected: "No touch-target, contrast or label findings (or findings logged)"
    why_human: "The D-14 literal item (androidTest enableAccessibilityChecks) was replaced by the JVM audit; the device scan is the other half"
  - test: "SC-1 Week 7 checkpoint - user confirmation that loading, empty and error-with-retry states look right on the screens (rows E3, M2, M3, N3, T5, T7, Y13)"
    expected: "Skeleton, empty and error with Retry on each screen; failed refresh keeps content and shows one Retry snackbar"
    why_human: "The roadmap marks this criterion 'user-confirmed'"
  - test: "M8 / decision D6 - after a successful Retry on Upcoming Booking Details or the expert Booking Summary, where does TalkBack focus land?"
    expected: "Plan 15-18 truth (verification: backstop) says the app bar title; it was NOT implemented. Decide: implement, accept, or move to Phase 16"
    why_human: "Backstop truth, non-inferable; also unimplemented, so a person must accept the deviation or ask for the work"
  - test: "B4 / decision D7 - tap reliability of the 45.7 dp calendar day cells on a 360 dp Samsung"
    expected: "No missed taps; otherwise reduce card padding"
    why_human: "Touch reliability is device behavior"
---

# Phase 15: Polish & Hardening Verification Report

**Phase Goal:** Every MVP screen handles loading, empty and error states and holds up under real-device conditions.
**Verified:** 2026-10-29
**Status:** human_needed
**Re-verification:** No - initial verification

## D-03 reading of success criterion SC-2 (recorded as required by CONTEXT D-03 and 15-VALIDATION.md)

With phones locked to portrait (`android:screenOrientation="portrait"` on MainActivity), "rotating the device" means any Activity recreation: dark-mode or font-scale change, window resize, "Don't keep activities", and process death. Release and debug builds behave the same; no debug-only rotation exists. SC-2 is therefore proven on the JVM by the `StateRestorationTester` / `SavedStateHandle` tests, `ManifestOrientationTest` and `ContentMaxWidthTest`, and is completed by the open device rows Y1-Y7 and Y11 of `15-DEVICE-CHECKLIST.md`. For "a call", CONTEXT D-11 defines the restored place as the booking detail with a Join action (never an auto-rejoin), which is what `CallResumeMarker` and the app-start routing implement.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| SC-1 | Every MVP screen shows a loading, an empty and an error state with retry | VERIFIED (code) / user confirmation open | Of 53 `*Screen.kt`, every data-loading screen carries a named first-load error with a Retry action: via `ErrorState` (ClientProfileSettings, ExpertProfile, UpcomingBooking, ExpertBookingSummary, topic picker) or `EmptyState(... EmptyStateAction(retry))` with a named heading (MyBookings, Conversations, ChatThread, Notifications, Dashboard, Availability, Help, Transactions, and others). Strings "Couldn't load ..." exist for each. 14 screens use `RefreshFailedSnackbarEffect` for the failed-refresh-keeps-content rule (D-05). The screens with no retry reference are static shells or forms (Home tabs, profile hubs, Login/Recovery, Signup Initial/Review/Timezone/VerifyEmail, ReportAndBlock, BookingConfirmed, MeetingCall, debug gallery). `RefreshFailedSnackbarTest` 5/5 green; sweep for action-less refresh snackbars is 0 (baseline 12). The roadmap tag "user-confirmed" is a human item. |
| SC-2 | Recreation / process death in signup, booking, chat or a call returns the user to the same place with input kept | VERIFIED (JVM, D-03 reading) / device rows open | `SavedStateHandle` in 31 main files; signup step + field text restore (`SignupSetupRestorationTest` 2/2); chat composer per thread (`ChatThreadViewModel` KEY_COMPOSER written on every edit and cleared on send); booking unchanged and covered by `EntrySavedStateHandleTest`; settings, Tax and Delete forms restore non-secrets; TIN, financial account, passwords, reset and verify codes are deliberately excluded (`savedStateKey()` returns null for Tin and FinancialAccount; no `verify_code` / `reset_code` literals); call: `CallResumeMarker` started in `ClickMeApplication`, `CallResumeMarkerTest` 8/8, `CallOverlayHostTest` 17/17 (never joins on restore). Nav back stack uses `rememberNavBackStack`. Process death on a device and the R8 build are open rows. |
| SC-3 | Edge-to-edge, nothing hidden by system bars, cutouts or keyboard | VERIFIED (JVM) / device rows open | `enableEdgeToEdge` with transparent bars in `MainActivity`; `adjustResize`; horizontal `displayCutout` padding on the call overlay root and pill; `contentMaxWidth()` on 46 of 53 screens (the seven exceptions are shells, the call screen, the debug gallery and the full-bleed photo crop, whose hubs cap inside `ProfileHubScaffold`); IME tests green: `LoginInsetsTest`, `ChatComposerInsetsTest`, `MakeABookingInsetsTest`, `ClientProfileSettingsInsetsTest`, `CallOverlayHostInsetsTest`; `scripts/check-design-rules.sh` adds an insets rule with a failing fixture, re-run by me: "Design rules: OK" and "Design rules self-test: OK". Real cutouts and gesture/3-button bars are open device rows (Y8, Y9, Y16). |
| SC-4 | A TalkBack user can log in, book a session, join a call and send a chat message | VERIFIED at the semantics level; TalkBack speech is human | `LoginTalkBackFlowTest` 6, `BookingTalkBackFlowTest` 5, `CallTalkBackFlowTest` 6, `ChatTalkBackFlowTest` 5, `A11yAuditTest` 5, `PaletteContrastTest` 9, all green: names, roles, states, headings, traversal order. No TalkBack row of the checklist is marked passed, correctly (RESEARCH Pitfall 9). The literal `androidTest` + `enableAccessibilityChecks()` item of D-14 is NOT built (decision `jvm-audit-plus-gap`, recorded Phase 16 gap, tier 1). |

**Score:** 4/4 roadmap criteria verified at the JVM level; the device-only half of each is carried as human verification, not as a gap.

### Plan-level truths checked beyond the roadmap

| Truth | Status | Evidence |
|-------|--------|----------|
| Portrait lock (15-02, D-01) | VERIFIED | `android:screenOrientation="portrait"` in the manifest, 0 `requestedOrientation`, `ManifestOrientationTest` 3/3 |
| No offline UI (15-01 prohibition, D-06) | VERIFIED | `ConnectivityManager` / `NetworkCallback` grep over `app/src/main/java`: 0 hits |
| No secrets in saved state (15-05, 15-06, 15-15 prohibitions) | VERIFIED | Tax `savedStateKey()` excludes Tin and FinancialAccount; no code-literal keys; ViewModel tests per VALIDATION map |
| Debt markers (TBD / FIXME / XXX) in the 114 files changed by the phase | VERIFIED | none found |
| 15-07: Book Session after About | DEVIATION (advisory) | Shipped before About, matching iOS. See assessment below |
| 15-19: Book Now pinned footer | DEVIATION (advisory) | Last item of the scrolling form (iOS parity); IME visibility test green |
| 15-18: TalkBack focus to the app bar title after a successful Retry (backstop) | NOT IMPLEMENTED (WARNING) | No focus request in `UpcomingBookingScreen`; recorded as D6, M8 and a tier-2 gap. Routed to human decision |

### Required Artifacts / Key Links

| Artifact | Status | Details |
|----------|--------|---------|
| `components/EmptyState.kt` `ErrorState(heading, body, retryLabel)` | VERIFIED | Defaults preserved, heading is a heading, error is a polite live region |
| `components/RefreshFailedSnackbar.kt` `RefreshFailedSnackbarEffect` | VERIFIED and WIRED | Used by 14 screens |
| `ui/theme/ContentWidth.kt` `contentMaxWidth()` | VERIFIED and WIRED | 600 dp cap; used by 46 screens |
| `managers/CallResumeMarker.kt` (+ `CallResumeNotice`, `di/CallResumeModule.kt`) | VERIFIED and WIRED | Started in `ClickMeApplication`; consumed by Upcoming Booking and Expert Booking Summary |
| `scripts/check-design-rules.sh` insets / R7 rules + selftest | VERIFIED | Both scripts pass when run here |
| `testing/A11yAudit.kt`, `testing/InsetsTestSupport.kt` | VERIFIED | Present, with their own tests; used by the TalkBack and insets tests above |

### Behavioral spot-checks

I did not re-run Gradle. I read the existing XML results from the orchestrator's 4316-test run (`app/build/test-results/testDevDebugUnitTest`, written 2026-10-29 22:14): all 15 named classes above show 0 failures, 0 errors, 0 skipped. I re-ran `check-design-rules.sh` and its self-test myself: both OK.

### Requirements Coverage

| Requirement | Source plans | Status | Evidence |
|-------------|--------------|--------|----------|
| POL-01 (loading, empty, error states) | 15-01, 06, 07, 11, 12, 13, 14, 16, 17, 18 | SATISFIED in code; user checkpoint open | SC-1 evidence |
| POL-02 (config change and process death) | 15-05, 06, 08, 09, 10, 11, 12, 13, 14, 15, 17, 18, 20 | SATISFIED on the JVM; device rows open | SC-2 evidence |
| POL-03 (edge-to-edge, cutouts, IME) | 15-02, 03, 05, 06, 07, 08, 10-16, 17-20 | SATISFIED on the JVM; device rows open | SC-3 evidence |
| POL-04 (TalkBack core flows) | 15-04, 05, 07, 08, 10, 11, 14, 16, 17, 19, 20 | PARTIAL: semantics proven, speech and ATF suite open | SC-4 evidence |

All four IDs are claimed by plans and map to Phase 15 in REQUIREMENTS.md. There are no orphaned requirements. REQUIREMENTS.md still shows POL-01..04 as unchecked / Pending; ticking them is left to the orchestrator after the human items are accepted.

### Assessment of the open product decisions

| Decision | Verdict |
|----------|---------|
| D1 height-adaptive call layout (15-08) vs D-02 | Judgment call, not a gap. It is a window-height rule that serves D-02's "never clipped or unreachable"; it adds no tablet or list-detail layout |
| D2 Book Session before About (15-07) | Judgment call, advisory deviation from a plan truth. iOS parity is the project rule (CLAUDE.md). Not a roadmap criterion. Suggested override below |
| D3 Book Now not pinned (15-19) | Judgment call. The roadmap-relevant behavior (reachable above the IME) is tested |
| D4 empty-catalog copy (15-12) | Copy review only |
| D5 Farewell button full width (15-20) | Cosmetic, tier 4. Not a gap against SC-1..SC-4 |
| D6 post-Retry focus not implemented (15-18) | The only decision that is an unmet plan truth. WARNING, not a blocker: it is a backstop (non-inferable) truth outside the four roadmap criteria and was disclosed. Needs the user to accept, schedule or implement |
| D7 45.7 dp day cells | Device check (B4). No JVM-provable failure |
| D8 `jvm-audit-plus-gap` | Applied as the plan's documented default. The literal D-14 androidTest suite is a recorded Phase 16 gap (tier 1). I do not treat it as a blocker, because the plan explicitly provides this substitute |

Suggested overrides, if the user accepts D2 and D3:

```yaml
overrides:
  - must_have: "On Expert Profile the Book Session button comes after the name, rating and About"
    reason: "iOS parity (CLAUDE.md: iOS is the source of truth); UI-SPEC wording contradicts iOS and D-14"
    accepted_by: "{user}"
    accepted_at: "{ISO timestamp}"
  - must_have: "Book Now pinned footer background stays full width"
    reason: "Book Now is the last item of the scrolling form (iOS parity); IME reachability is covered by MakeABookingInsetsTest"
    accepted_by: "{user}"
    accepted_at: "{ISO timestamp}"
```

### Anti-Patterns Found

None blocking. No TBD / FIXME / XXX in the 114 changed files; no offline monitor; no `requestedOrientation`.

### Human Verification Required

See the `human_verification` frontmatter. In short: the 101 open rows of `15-DEVICE-CHECKLIST.md` (none passed, by design), plus the SC-1 "user-confirmed" checkpoint, the D6 focus decision and the B4 tap reliability check.

### Gaps Summary

No blocking gaps. All four roadmap criteria have implemented, wired and test-backed behavior. The status is `human_needed` rather than `passed` because device-only behavior (TalkBack speech, process death on hardware, R8 restore, large-window resize, cutouts) and the roadmap's user-confirmed checkpoint are unproven, and because the 15-18 backstop truth is unimplemented. Phase 16 inherits the gap list in section 13 of the checklist, including the literal `androidTest` accessibility-check suite.

---

_Verified: 2026-10-29_
_Verifier: Claude (gsd-verifier)_
