# Phase 15 Device Checklist (non-blocking)

One consolidated list of every deferred visual, accessibility and system check from Phases 3-15, ordered by flow and deduplicated (CONTEXT D-14, D-15). It is a human item **logged as open**: nothing in the phase, the verifier or the release waits on it (standing preference: manual testing is deferred, not blocking). Every cell starts `[ ]` or `n/a`. A box is ticked only by a person who ran the check on a device. Automated tests prove that semantics, saved state and insets exist (RESEARCH Pitfall 9); they do not prove TalkBack speech, so no TalkBack row or SC-4 is marked passed from automated results.

## Header

- **Device:** Samsung SM-A075F, `devDebug` build, **production backend** unless a row says "sandbox backend". Accounts are referenced by role or by the `uat.*@clickme.test` pattern only; never paste a password, token or key into this file.
- **Large-window rows:** a resizable emulator. Create it from the installed `android-37.0` system image with the "Resizable (Experimental)" device profile (RESEARCH A8). Test at least a phone size, 900x400 (short and wide), and 900 dp wide and tall. No foldable device is available.
- **Build for the R8 rows:** `prodRelease` (signed, or unsigned for the install-and-restore check). It needs the Phase 13 release checklist keystore steps only if you want a signed build.
- **D-03 reading of SC-2:** With phones locked to portrait, "rotating the device" means any Activity recreation (dark or font-scale change, window resize, "Don't keep activities", process death), and release and debug builds behave the same (D-03). Record this reading in VERIFICATION.md.
- **Out of this checklist (D-15):** Stripe-card rows and live-call rows that need an iOS peer stay with Phase 16 (they are listed in the gap list below and left in place in STATE.md).
- **Columns:** `Font 200%` = Settings > Display > Font size set to the largest, then Display size to the largest. `TalkBack` = TalkBack on. `Config change / process death` = dark mode toggle, font-scale change, window resize, Developer options "Don't keep activities", or `adb shell am force-stop com.uptrendinvestments.clickmeapp` then relaunch. `IME / insets` = the keyboard, the display cutout, gesture and 3-button navigation. `Large window` = the resizable emulator.
- **Source column:** `P15-NN` is the Phase 15 plan that built the behavior; `P3 SC-2` and similar name the original STATE.md deferred item this row supersedes.

## 1. Login and recovery

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| L1 SC-4 run: log in with TalkBack (P15-04, P15-05, P3 SC-1) | Complete a login using only TalkBack: Login reads in order (title as a heading, email, password, Log in), one "Don't have an account? Sign Up" node, Log in says "In progress" while busy, then lands on the right home | n/a | [ ] | n/a | n/a | n/a | [ ] |
| L2 Wrong-password focus (P15-05, P3 SC-1) | After a wrong password, the server message is spoken and focus lands on the password field | n/a | [ ] | n/a | [ ] | n/a | [ ] |
| L3 Filled field speech (P15-04, UI-SPEC A6) | On Login with email and password filled, TalkBack speaks the field name together with its value (also check one field on Signup and one on Settings). If only one is spoken, log it in the gap list | n/a | [ ] | n/a | n/a | n/a | [ ] |
| L4 Login layout and keyboard (P3 rotation line, P15-05) | Login on the small phone at 200% font: nothing clips, IME Next/Done moves email to password to submit, autofill offers the saved login, and Log in stays reachable above the open keyboard | [ ] | n/a | n/a | [ ] | n/a | [ ] |
| L5 Recovery screens at 200% (P3 rotation line) | Login, Forgot Password, Reset Code, Set New Password and Password Changed at 200% with the keyboard open wrap rather than clip | [ ] | n/a | n/a | [ ] | n/a | [ ] |
| L6 Code autofill and first-error focus (P3 SC-2) | SMS/OTP autofill fills the 6-digit reset code; on submit with errors focus moves to the first failing field | n/a | n/a | n/a | [ ] | n/a | [ ] |
| L7 Password Changed (P3 SC-2, P15-05) | TalkBack focus lands on the "Password Changed" headline first; reading order and the glow are sensible; Go to Login still shows Login with the email pre-filled and an empty password | n/a | [ ] | n/a | n/a | n/a | [ ] |
| L8 Secrets after process death (P3 rotation line corrected by D-09, P15-05) | On Forgot and Set New Password, toggle dark mode (the typed email stays) and then use "Don't keep activities" and return: the reset code and both passwords come back empty with no error text | n/a | n/a | [ ] | n/a | n/a | [ ] |
| L9 Login and recovery on a large window (P15-05) | Login and the four recovery screens are centered at 600 dp, app bars span the window, nothing clipped | n/a | n/a | n/a | n/a | [ ] | [ ] |

## 2. Signup

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| S1 All signup surfaces at 200% (P4 visual bullet) | All 12 signup surfaces (Initial, hub, Personal details, Timezone, Profile Photo and crop, Verify Email, Add expertise, Review, Overview, Onboarding, search sheets) at 200% font with the keyboard open: no clipped text, buttons stack where two do not fit | [ ] | n/a | n/a | [ ] | n/a | [ ] |
| S2 Search sheets with the IME (P4 visual bullet) | The country, language and expertise `ModalBottomSheet` search fields stay visible above the keyboard and the list scrolls | n/a | n/a | n/a | [ ] | n/a | [ ] |
| S3 Crop custom actions (P4 visual bullet) | TalkBack on the Profile Photo crop offers and performs the custom zoom and move actions | n/a | [ ] | n/a | n/a | n/a | [ ] |
| S4 Recreation mid-signup (P15-06, P4 SC-3) | With "Don't keep activities", background at each signup step and return: same step, typed text kept; Verify Email returns with an empty code and no focus ring; password fields empty | n/a | n/a | [ ] | n/a | n/a | [ ] |
| S5 Signup on a large window (P15-06) | Signup screens capped at 600 dp: app bars full width, Onboarding imagery and glow full width, Add expertise list scrolls | n/a | n/a | n/a | n/a | [ ] | [ ] |
| S6 Review and Overview with TalkBack (P15-06) | Review and Overview announce their in-flight and error states; Publish is reachable; retry after an error works | n/a | [ ] | n/a | n/a | n/a | [ ] |

## 3. Explore and Expert Profile

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| E1 Explore, categories sheet and profile at 200% (P5 visual bullet) | Explore, the View all sheet and the Expert Profile at 200%: cards grow, nothing clips, Book Session stays reachable | [ ] | n/a | n/a | n/a | n/a | [ ] |
| E2 Category sheet search with the IME (P5 visual bullet) | Sheet search field and results stay usable with the keyboard open | n/a | n/a | n/a | [ ] | n/a | [ ] |
| E3 Pull-to-refresh feel (P5 visual bullet) | Pull to refresh on Explore feels normal; on a failed refresh (airplane mode) all loaded sections stay and one "Retry" snackbar appears; Retry reloads (P15-12) | n/a | [ ] | n/a | n/a | n/a | [ ] |
| E4 Empty catalog copy (P15-12) | With no categories, the screen reads "No categories yet" and "Check back soon."; decide whether the copy is right (see Decisions for the user) | n/a | n/a | n/a | n/a | n/a | [ ] |
| E5 Expert Profile with TalkBack (P15-07) | "Book Session with {name}" is spoken; sections read in order; headings navigate | n/a | [ ] | n/a | n/a | n/a | [ ] |
| E6 Expert Profile on a large window (P15-07) | Hero glow full width, sections capped at 600 dp | n/a | n/a | n/a | n/a | [ ] | [ ] |

## 4. Booking (free; paid on the sandbox backend)

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| B1 SC-4 run: book a free session with TalkBack (P15-04, P15-07, P15-19) | Book a free session using only TalkBack, from Expert Profile to Request Sent, with a person completing every step | n/a | [ ] | n/a | n/a | n/a | [ ] |
| B2 Form at 200% (P6 visual and system, P15-19) | Make a Booking at 200%: topic row, calendar, slots, notes wrap; Book Now and its reason stay reachable | [ ] | n/a | n/a | n/a | n/a | [ ] |
| B3 Calendar and slots speech (P15-07) | Day cells read "Today, Selected, Available" or "No availability"; the month title is read before Previous and Next month; choosing a day announces how many times are free; the slots heading announces on day change; Book Now says why it is disabled and "In progress" while busy | n/a | [ ] | n/a | n/a | n/a | [ ] |
| B4 Day cell width on a 360 dp phone (P15-07) | Day cells are about 45.7 dp wide on a 360 dp phone, under 48 dp. Check that taps are reliable on the Samsung; if not, reduce card padding | n/a | [ ] | n/a | n/a | n/a | [ ] |
| B5 Topic sheet (P15-07, P6 visual and system) | TalkBack reads the topic cards as a radio group; closing the sheet returns focus to the Topic row; a failed topic load shows the named error and Retry inside the sheet; the open sheet survives a config change | n/a | [ ] | [ ] | n/a | n/a | [ ] |
| B6 Notes field and keyboard (P15-19, P6 SC-1) | With the real keyboard: caret stays visible while typing, tap outside clears focus, a drag dismisses the keyboard, and Book Now (the last item of the form) can be scrolled above the keyboard | n/a | n/a | n/a | [ ] | n/a | [ ] |
| B7 Recreation mid-booking (P6 visual and system, P15-19) | With "Don't keep activities", background mid-form and return: topic, day, month, slot, meeting type and notes are kept; the Request Sent animation does not replay; system Back during an in-flight booking is ignored | n/a | n/a | [ ] | n/a | n/a | [ ] |
| B8 Request Sent headline (P15-19) | On entering Request Sent TalkBack speaks "Request Sent!" first | n/a | [ ] | n/a | n/a | n/a | [ ] |
| B9 Booking on a large window (P15-19, P15-07) | Make a Booking and Request Sent capped at 600 dp; resizing the window during a booking keeps every selection and the typed notes | n/a | n/a | [ ] | n/a | [ ] | [ ] |
| B10 Paid screens at 200% (P7 SC-5 visual part) | **Sandbox backend.** Payment Methods, its info sheet and a paid Request Sent at 200%. Opening Payment Methods needs no card; the paid Request Sent needs a paid booking made on the sandbox backend (card rows themselves stay with Phase 16) | [ ] | n/a | n/a | n/a | n/a | [ ] |

## 5. My Bookings and Cancel

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| M1 Screens at 200% (P8 visual and system) | My Bookings, Upcoming Details with a reschedule card, Cancel step 2 and Booking Summary at 200%: no clipping, buttons stack, footer and Retry reachable | [ ] | n/a | n/a | n/a | n/a | [ ] |
| M2 Failed refresh (P15-01, P15-12) | In airplane mode pull to refresh, and trigger a realtime refetch (change a booking from iOS): rows and tab stay, exactly one "Couldn't refresh. Check your connection and try again." snackbar with "Retry"; Retry refreshes; TalkBack announces it and the Retry action | n/a | [ ] | n/a | n/a | n/a | [ ] |
| M3 Heading and error semantics (P15-01) | With TalkBack, the ErrorState and EmptyState headings navigate as headings and announce once; a long error heading wraps at 200% | [ ] | [ ] | n/a | n/a | n/a | [ ] |
| M4 Cancel step 2 recreation (P8 visual and system, P15-12) | Kill the process on the Cancel confirm step and reopen: the confirm step is shown and nothing cancels until Confirm is tapped; system Back during an in-flight cancel is ignored | n/a | n/a | [ ] | n/a | n/a | [ ] |
| M5 Refund snackbar formatting (P8 visual and system) | A JPY booking cancel shows the refund snackbar with no decimals | [ ] | n/a | n/a | n/a | n/a | [ ] |
| M6 Decline-reschedule sheet (P15-03) | Open Decline from client Upcoming Details, client Booking Summary and expert Booking Summary: with the keyboard up the reason, counter and Decline button stay visible and the sheet scrolls; kill the process with text typed and the sheet reopens with the text | n/a | n/a | [ ] | [ ] | n/a | [ ] |
| M7 Detail screens on a large window (P15-18, P15-12) | My Bookings, Upcoming Details, Booking Summary and Cancel capped at 600 dp; pull a realtime update while offline and check the snackbar and progress bar | n/a | n/a | n/a | n/a | [ ] | [ ] |
| M8 Focus after a successful Retry (P15-18) | After Retry succeeds on a detail screen, check where TalkBack focus lands. The plan's backstop truth is the app bar title; this was not implemented (see gap list) | n/a | [ ] | n/a | n/a | n/a | [ ] |

## 6. Call

Live-call rows that need the iOS peer (two-way audio, hang-up, rejoin, 5+ minute background run) stay with Phase 16. The rows here need only the Android phone joining a free booking inside its window.

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| C1 SC-4 run: join a call with TalkBack (P15-04, P15-08, P9 last bullet) | Join a call from Upcoming Details using TalkBack. The status line is read first when the layer opens; the order is status, name, timer, Mute, Speaker, Chat, Minimize, End call; "Connecting" is not announced twice | n/a | [ ] | n/a | n/a | n/a | [ ] |
| C2 Call pill first (P15-08, UI-SPEC A8) | While a call is active, the pill is the first TalkBack node on screens underneath, across tabs. If it is not first, log it (fallback: a focus request on the pill when a screen appears) | n/a | [ ] | n/a | n/a | n/a | [ ] |
| C3 Call layer at 200% (P9 last bullet) | The Connected pair, the final-5-minute banner and the Skype/Zoom external variant at 200%: names wrap to two lines, Mute, Speaker and End call never leave the screen | [ ] | n/a | n/a | n/a | n/a | [ ] |
| C4 Call layer on short and wide windows (P15-08, P15-02) | On the resizable emulator at 900x400, Mute, Speaker and End call show without scrolling; a narrow short window scrolls; the phone layout is visually unchanged | n/a | n/a | n/a | n/a | [ ] | [ ] |
| C5 Pill and layer insets (P9 last bullet, P15-02) | The pill sits correctly over gesture navigation and 3-button navigation, and clears a side display cutout | n/a | n/a | n/a | [ ] | n/a | [ ] |
| C6 Recreation never re-joins (P15-08) | Dark mode toggle, font-scale change and "Don't keep activities" during a call: the call continues in the pill or layer, and nothing joins a second time or opens the mic by itself | n/a | n/a | [ ] | n/a | n/a | [ ] |
| C7 Process kill during a live call (P15-09, P15-18, P9 last bullet) | Start an in-app call, then `adb shell am force-stop com.uptrendinvestments.clickmeapp` (`am kill` is ignored for a foreground service) and relaunch. Expect the booking detail (client Upcoming Details, expert Booking Summary) with Join Call, the one-time "You were on a call" note above it (TalkBack announces it), no auto-join and no microphone indicator; reopening the screen does not show the note again | n/a | [ ] | [ ] | n/a | n/a | [ ] |
| C8 Cold-start notification tap (P9 last bullet) | With a call running, swipe the task away and tap the ongoing-call notification: the app starts to Splash and auth, then the right place | n/a | n/a | [ ] | n/a | n/a | [ ] |
| C9 Notification permission denied on API 33+ (P9 last bullet) | With POST_NOTIFICATIONS denied the call still runs and the pill remains | n/a | n/a | n/a | n/a | n/a | [ ] |
| C10 Resize with a call in progress (P15 UI Consideration) | On the emulator resize the window during a call: no clipped controls, call state kept | n/a | n/a | [ ] | n/a | [ ] | [ ] |

## 7. Chat and Report/Block

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| K1 SC-4 run: send a chat message with TalkBack (P15-04, P15-10, P12 (e)) | Send a message using only TalkBack: Conversations row, thread reading order, "You: {text}, {time}, Sent" or "Read", the composer explains why Send is disabled, and focus and the keyboard stay after Send | n/a | [ ] | n/a | n/a | n/a | [ ] |
| K2 Conversation row (P15-11) | A row reads as one stop: "{name}, {last message}, {time}, {n} unread", without reading the child text again | n/a | [ ] | n/a | n/a | n/a | [ ] |
| K3 Bubble speech (P15-10) | TalkBack does not speak a bubble twice (merged description then child text). If it does, log it (fix: drop the Text from the merged node) | n/a | [ ] | n/a | n/a | n/a | [ ] |
| K4 Live announcements (P15-10) | A peer message arriving is announced once; a newest own message appearing from history is not announced on first composition | n/a | [ ] | n/a | n/a | n/a | [ ] |
| K5 Bubble actions and reason group (P12 (e)) | TalkBack on a failed bubble offers Retry and Delete; the Report reason group reads as a group | n/a | [ ] | n/a | n/a | n/a | [ ] |
| K6 Chat at 200% (P12 (e)) | Conversations (row name and time wrap), a thread with a failed bubble (bubbles and the composer grow), the blocked banner, Report & Block, the Report sheet and the Request Decision footer (buttons stack) | [ ] | n/a | n/a | n/a | n/a | [ ] |
| K7 Composer and keyboard (P12 (e), P15-10) | IME pushes the thread up with the cutout and with 3-button navigation, cursor stays stable, the long-press Delete menu works | n/a | n/a | n/a | [ ] | n/a | [ ] |
| K8 Draft after process death (P15-10) | Type an unsent message, use "Don't keep activities" and return: the draft is back in the same thread and the keyboard does not reopen | n/a | n/a | [ ] | [ ] | n/a | [ ] |
| K9 Report sheet on hardware (P15-11) | The Report `ModalBottomSheet` with the real keyboard keeps its input visible; reason and details are restored after "Don't keep activities" | n/a | n/a | [ ] | [ ] | n/a | [ ] |
| K10 Blocked banner (P15-11) | Opening a blocked thread announces the blocked banner | n/a | [ ] | n/a | n/a | n/a | [ ] |
| K11 Failed refresh and reconnect (P15-11) | With the network off, pull to refresh in Conversations and in a thread: rows and messages stay, one "Retry" snackbar; toggle airplane mode inside a thread to see the reconnect failure path | n/a | [ ] | n/a | n/a | n/a | [ ] |
| K12 Feel (P12 (e)) | The jump-to-latest control and the list move-to-top animation feel right | n/a | n/a | n/a | n/a | n/a | [ ] |
| K13 Chat on a large window (P15-11) | Conversations rows and Report & Block capped at 600 dp on the emulator | n/a | n/a | n/a | n/a | [ ] | [ ] |

## 8. Notifications and Reviews

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| N1 Screens at 200% (P13 (g)) | The notifications list (with and without the permission-off row), Write Review in both states, Booking Summary with Feedback and the Past card pill at 200% | [ ] | n/a | n/a | n/a | n/a | [ ] |
| N2 TalkBack on reviews (P13 (g)) | The star group, the nested Past card action and the notification rows read correctly | n/a | [ ] | n/a | n/a | n/a | [ ] |
| N3 Failed reload after a push (P15-12) | In airplane mode receive a foreground push while on Notifications: rows stay, one "Retry" snackbar, Retry reloads | n/a | [ ] | n/a | n/a | n/a | [ ] |
| N4 Notifications and Write Review on a large window (P15-12) | Centered 600 dp column; app bars and pull-to-refresh span the window; the Write Review footer background spans the window while its button is capped | n/a | n/a | n/a | n/a | [ ] | [ ] |

## 9. Expert side (Topics, Availability, Dashboard, Requests, Bookings)

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| X1 Topics and Availability at 200% (P10 (f)) | Topics, the Topic Editor with an error line, an overlapping day with the banner and the Time Range sheet at 200% | [ ] | n/a | n/a | n/a | n/a | [ ] |
| X2 Time Range sheet (P10 (f), P15-13) | TalkBack reads the Time Range sheet; the sheet keeps its state after rotation-equivalent recreation and process death | n/a | [ ] | [ ] | n/a | n/a | [ ] |
| X3 Topic Editor focus and IME (P10 (f), P15-14) | Focus moves to the first invalid field and scrolls into view; the description field stays above a real keyboard; editor rotation with an error showing keeps the error | n/a | n/a | [ ] | [ ] | n/a | [ ] |
| X4 Dashboard and requests at 200% (P11 (e)) | Dashboard, a request card, Request Decision with the earnings card and footer, the Decline sheet, Confirmed, an Upcoming card with the reschedule badge and Cancel step 2 at 200%; JPY amounts in the earnings card | [ ] | n/a | n/a | n/a | n/a | [ ] |
| X5 TalkBack on expert sheets (P11 (e), P15-14) | TalkBack on the Decline and Accept sheets; Booking Confirmed starts on its headline and Back restores focus sensibly | n/a | [ ] | n/a | n/a | n/a | [ ] |
| X6 Quiet reloads and animation (P11 (e)) | Tab return and realtime events reload without flicker; the Confirmed entry with animations off shows its final state at once | n/a | n/a | n/a | n/a | n/a | [ ] |
| X7 Decline and Accept after process death (P15-14) | With the Decline sheet open and a reason and note typed, use "Don't keep activities" and return: reason, note and the sheet are back and nothing was sent; same for Accept with a message | n/a | n/a | [ ] | n/a | n/a | [ ] |
| X8 Retry snackbars on expert screens (P15-13) | With the network off, force a failed refresh on Dashboard, Bookings, Requests, Topics and Availability: one "Retry" snackbar each, TalkBack announces and focuses Retry; on Availability with a gesture or 3-button bar and the keyboard open (a timezone search) the snackbar sits above the bar and the keyboard | n/a | [ ] | n/a | [ ] | n/a | [ ] |
| X9 Expert screens on a large window (P15-13, P15-14) | Dashboard, Bookings, Requests, Topics, Availability, Request Decision and the Topic Editor capped and centered at 600 dp; app bars, tabs and the Request Decision footer background full width; pull-to-refresh works from the side margins | n/a | n/a | n/a | n/a | [ ] | [ ] |

## 10. Settings and Payouts

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| T1 Phase 14 screens at 200% (P14 (f)) | Both hubs, Client and Expert Profile Settings, Account Security, Delete Account, Help, Payouts, Activity and Tax & Payout Info reflow with no clipped text and the footer reachable | [ ] | n/a | n/a | n/a | n/a | [ ] |
| T2 Phase 14 screens with TalkBack (P14 (f)) | TalkBack announces the save status, the password toggle state, the FAQ expanded or collapsed state and the selected delete reason | n/a | [ ] | n/a | n/a | n/a | [ ] |
| T3 Forms after process death (P15-15) | Edit each of the four forms (client settings, expert settings, Tax, Delete), use "Don't keep activities" and return: typed values, country, languages and meeting link reappear; the TIN, account identifier and deletion password come back empty with no error text; a restored profile edit is saved ("Saved" indicator) a moment after return | n/a | n/a | [ ] | n/a | n/a | [ ] |
| T4 Tax snackbar (P15-15) | The Tax "saved" snackbar sits above the gesture bar and the keyboard | n/a | n/a | n/a | [ ] | n/a | [ ] |
| T5 Failed refresh on Payouts, Activity, Help (P15-16) | With the network off, pull to refresh: content stays, one "Retry" snackbar, Retry reloads, TalkBack announces it | n/a | [ ] | n/a | n/a | n/a | [ ] |
| T6 Account Security errors (P15-16) | A wrong current password puts the cursor in Current Password and the keyboard stays up; a server error dialog "Couldn't update password" keeps all three typed values after "Got it" | n/a | n/a | n/a | [ ] | n/a | [ ] |
| T7 Payment Methods skeleton (P15-16) | The loading skeleton matches the loaded card rows without a jump | n/a | n/a | n/a | n/a | n/a | [ ] |
| T8 Farewell and Delete Account (P15-20) | After deleting a throwaway `uat.*@clickme.test` account, TalkBack announces the Farewell headline first; "Back to Login" is still full width on a large window (see gap list) | n/a | [ ] | n/a | n/a | [ ] | [ ] |
| T9 Log out and Delete never auto-confirm (P15-20, P15-15) | With the Log out dialog or a Delete step open, use "Don't keep activities": the dialog or step returns and nothing happens until the user taps | n/a | n/a | [ ] | n/a | n/a | [ ] |
| T10 Settings screens on a large window (P15-15, P15-16, P15-20) | The three forms, Payouts, Activity, Help, Payment Methods, Account Security, Delete Account, both hubs and Mode Switch are centered at 600 dp with full-width app bars | n/a | n/a | n/a | n/a | [ ] | [ ] |

## 11. System rows

| Item (source phases) | Check | Font 200% | TalkBack | Config change / process death | IME / insets | Large window | Result |
|---|---|---|---|---|---|---|---|
| Y1 Orientation lock on phone (P15-02, D-01) | Rotate the Samsung through the app: it stays portrait on every screen, including during a call | n/a | n/a | [ ] | n/a | n/a | [ ] |
| Y2 Tablet and foldable resize (P15-02, D-02) | On the resizable emulator resize through phone, 900x400 and 900 dp square sizes: content never clipped, input kept, max width 600 dp, no tablet layout needed | n/a | n/a | [ ] | n/a | [ ] | [ ] |
| Y3 "Don't keep activities": Signup (P15-06, P4 SC-3) | Walk the whole signup with "Don't keep activities" on, backgrounding at each step (specifics in S4) | n/a | n/a | [ ] | n/a | n/a | [ ] |
| Y4 "Don't keep activities": Booking (P15-19, P6 visual and system) | Walk Expert Profile to Request Sent with it on (specifics in B7) | n/a | n/a | [ ] | n/a | n/a | [ ] |
| Y5 "Don't keep activities": Chat (P15-10) | Walk Conversations to a thread, type, background and return (specifics in K8) | n/a | n/a | [ ] | n/a | n/a | [ ] |
| Y6 "Don't keep activities": a call (P15-08, P15-09) | Join, background and return; then the force-stop case in C7. Nothing re-joins | n/a | n/a | [ ] | n/a | n/a | [ ] |
| Y7 Dark mode and font-scale recreation (D-03, P3 rotation line) | Toggle dark mode and change the font scale while on a form in each flow: typed text kept, secrets empty, no crash | [ ] | n/a | [ ] | n/a | n/a | [ ] |
| Y8 IME with the cutout (P15-02, P15-10) | With the keyboard open on the Samsung's cutout, no text or control sits under the status bar, a side cutout or the keyboard | n/a | n/a | n/a | [ ] | n/a | [ ] |
| Y9 Gesture versus 3-button navigation (P9 last bullet, P15-13, P15-15) | Switch between the two: snackbars, sticky footers and the call pill clear the bar in both | n/a | n/a | n/a | [ ] | n/a | [ ] |
| Y10 Animations off (P6 visual and system, P11 (e)) | Developer options animation scales off: Request Sent, Booking Confirmed and sheets show their final state at once | n/a | n/a | n/a | n/a | n/a | [ ] |
| Y11 R8 release restore (P3 release smoke, P6 visual and system, P7 SC-5, P12 (e), RESEARCH Pitfall 8) | Install `prodRelease` (signed or unsigned), log in, then with "Don't keep activities" background and return from a deep screen (booking detail, a chat thread). If it lands on the start screen, add a NavKey keep rule. Also confirm login and the realtime connection work under R8 | n/a | n/a | [ ] | n/a | n/a | [ ] |
| Y12 Accessibility Scanner (P15-04) | Run Accessibility Scanner on the Samsung over the four SC-4 flows (log in, book, call, chat) and log touch-target, contrast and label findings (the device half of the D-14 substitute) | [ ] | [ ] | n/a | n/a | n/a | [ ] |
| Y13 State transitions on a slow network (P5 visual bullet, P15 UI Consideration) | Throttle the network: skeleton to content, content to snackbar, and error to retried content show no layout jump and no flicker on Explore, My Bookings, Conversations and the expert Dashboard | n/a | n/a | n/a | n/a | n/a | [ ] |
| Y14 Retry snackbar duration under TalkBack (RESEARCH A6, UI-SPEC A2) | The "Retry" snackbar stays long enough with TalkBack to be reached and activated | n/a | [ ] | n/a | n/a | n/a | [ ] |
| Y15 Scroll position after restore (P15 UI Consideration, D-12) | After "Don't keep activities" the scroll position is restored where the data is available; it is best effort, so only note regressions | n/a | n/a | [ ] | n/a | n/a | [ ] |
| Y16 Cutouts and system bars (P15-02) | No text or control sits under the status bar, a side cutout or the navigation bar on any of the four SC-4 flows | n/a | n/a | n/a | [ ] | n/a | [ ] |

## 12. Decisions for the user

These are product calls the executors made as documented defaults. Each is open until you answer; none blocks anything.

| # | Decision | Source | Default applied |
|---|---|---|---|
| D1 | The short-window call layout adapts by height (a side-by-side hero at 560 dp wide or more when under 480 dp tall). Confirm that this is compatible with D-02 "no tablet-specific layouts" | P15-08 | Kept; it is a height rule, not a device-type rule |
| D2 | Book Session stays before About on Expert Profile (iOS parity) versus UI-SPEC "after About" | P15-07 | iOS parity (before About) |
| D3 | Book Now is the last item of the scrolling form (iOS parity), not a pinned footer. Pin it or keep | P15-19 | Last item of the form |
| D4 | The new empty-catalog copy "No categories yet" and "Check back soon." needs a copy review | P15-12 | Shipped as written |
| D5 | The Farewell's "Back to Login" button is still full width on large windows; cap it as a follow-up | P15-20 | Left full width |
| D6 | TalkBack focus moving to the app bar title after a successful Retry on a detail screen was not implemented | P15-18 | Not implemented; check on the device (row M8) |
| D7 | Day cells are about 45.7 dp wide on 360 dp phones, under the 48 dp target | P15-07 | Left; check on the Samsung (row B4) |
| D8 | D-14: the accessibility checks run as a JVM audit (A11yAudit plus PaletteContrastTest) with the `androidTest` ATF suite as a gap item, not built now | P15-04 | `jvm-audit-plus-gap` (orchestrator-applied default) |
| D9 | UI-SPEC A1 (`ErrorState` extended with default heading, body and Retry label), A2 (refresh-failed snackbar uses Long duration with a Retry action), A4 (the "You were on a call" note depends on a persisted marker, which now exists: 15-09), A7 (calendar days with no availability stay focusable) | UI-SPEC | Applied as written |
| D10 | Review the portrait lock after Play pre-launch reports show large-screen behavior | UI-SPEC, CONTEXT D-01 | Phones locked to portrait |

## 13. Gap list for Phase 16

Anything not finished within Phase 15 and any item owned by Phase 16, with its D-16 tier (1 = the four SC-4 core flows; 2 = remaining client screens; 3 = expert screens; 4 = settings).

| Item | D-16 tier | Source | Why it is left |
|---|---|---|---|
| An `androidTest` source set with `ui-test-junit4-accessibility` and `enableAccessibilityChecks()` on the four SC-4 flows (log in, book, call, chat), run on the emulator (CONTEXT D-14 literal item) | 1 | 15-04 D-14 decision `jvm-audit-plus-gap` | Compose accessibility checks are a no-op under Robolectric (RESEARCH Pitfall 1); the JVM audit and the palette table are the substitute for now |
| Stripe-card rows: Payment Methods add card, short-lead paid booking, no-saved-card gate, 3DS, paid cancel and refund, paid expert cancel, paid session end to end, Connect onboarding return | 1 | P7 SC-1..SC-5, P8 SC-3, P9 SC-5, P11 (d), P14 (a) | Blocked on a test-mode backend path; owned by Phase 16 (D-15); left in place in STATE.md |
| Live-call rows with the iOS peer: Join from every entry point, mic prompts, 5+ minute background run, hang-up and rejoin, final-5-minute banner, Android 17 audio hardening, Bluetooth routing, incoming GSM call, Samsung battery behavior, expert Join and the 09-HUMAN-UAT items | 1 | P9 SC-1..SC-4, P9 last bullet (non-visual part), P11 (c), G-11-1 and G-11-5 | Needs an iOS peer; owned by Phase 16 (D-15); left in place in STATE.md |
| Cross-device chat, booking and request rows: live messages both ways, typing, Read, Load earlier, report and block with iOS, accept and decline from iOS, reschedule response | 1 | P12 (a)-(d), P8 SC-1 to SC-4, P11 (a), (b), (d), P13 (a)-(f) | Needs an iOS peer or push from a killed app; owned by Phase 16 (D-15) |
| Photo picker on API 26-32 (no AVD available) and avatar pick and crop on an API 26-32 emulator | 2 | P4 visual bullet, P14 (c) | Needs an API 26-32 emulator; the original sub-item stays in STATE.md |
| Day cells are about 45.7 dp wide on 360 dp phones; reduce the calendar card padding if the Samsung check shows missed taps | 1 | 15-07, row B4 | Pinned for a device check; no JVM test fails |
| TalkBack focus to the app bar title after a successful Retry on the two detail screens | 2 | 15-18, row M8 | Not implemented; layout is replaced in place |
| If row C2 shows the pill is not first: add a focus request on the pill when a screen appears during a live call | 1 | 15-08, UI-SPEC A8 | Conditional on the device result |
| If row L3 shows only the name or only the value is spoken: add the name to the field's semantics without hiding the value | 1 | 15-04, UI-SPEC A6 | Conditional on the device result |
| If row K3 shows duplicate bubble speech: drop the Text from the merged node | 1 | 15-10 | Conditional on the device result |
| Book Now "scroll footer with content" variant for a phone under 480 dp tall (only reachable in split screen or a resized window) | 1 | 15-19 | Phones are locked to portrait; no footer exists to scroll |
| Cap the Farewell "Back to Login" button on large windows | 4 | 15-20, D5 | Outside 15-20's scope |
| Wider tester group and push from a killed app in the two-way matrix | 1 | P13 D-22, P13 hand-off | Phase 16 scope |
| Review the portrait lock after Play pre-launch reports show large-screen behavior | 2 | UI-SPEC open items, D10 | Needs Play data |
