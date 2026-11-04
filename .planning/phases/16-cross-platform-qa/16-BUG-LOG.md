# Phase 16 Bug Log

QA-01: one log for the user's own findings plus every failed runbook row (D-11). The user reports results in the conversation; Claude records and triages them here. A failed row in `16-MATRIX-RUNBOOK.md` gets a Result of `[!] fail (BUG-NN, vc N)` and one row below.

## Rules

- One row per bug. IDs are `BUG-01`, `BUG-02` and so on.
- No secrets, tokens, passwords, Stripe keys or card numbers in any cell. Ids and error codes only; cards by brand and last 4.
- Evidence is a path under `/Users/LeonChen/ClickMe-release` (outside the repo). Raw logcat and screenshots never enter the repo.
- Device and build cell: device, versionCode and installer package (for example SM-A075F, vc 2, com.android.vending).

## Severity rubric (D-07)

A bug blocks the release if it:

- loses money or charges the wrong amount,
- prevents a booked session or call from happening,
- loses user data,
- crashes the app,
- or breaks a store-policy requirement (for example the foreground-service or account-deletion rules).

Everything else is not blocking:

- `fix-if-cheap`: visual, accessibility polish and edge-case bugs; fixed when the change touches at most 2 files and needs no product decision.
- `post-release`: logged and left for after release.

Write the Severity cell as `blocking (clause)`, `fix-if-cheap` or `post-release`.

## Owner routing (D-12)

- **android:** goes into the next fix batch (one versionCode bump and one internal-track upload per batch, D-15).
- **ios** and **backend:** one line is added to the report-to-iOS list in the STATE Pending Todos section. The bug is fixed in the owning repo during Phase 16 only when it is blocking by D-07 AND it blocks the Android release. Backend and iOS edits are run by the orchestrator sequentially without worktree isolation, and the orchestrator commits. Write "blocks Android release: yes" or "blocks Android release: no" inside the Severity cell for ios and backend rows.

## Status lifecycle

`open` then `fixed (pending re-test)` then `verified` or `re-test failed`. Non-blocking rows may end as `post-release`. Rows owned by other repos and not fixed here end as `reported (ios)` or `reported (backend)`.

## Bugs

| ID | Source (runbook row or free-form) | Steps | Device and build | Owner (android/ios/backend) | Severity (D-07) | Status | Fix commit | Re-test result |
|---|---|---|---|---|---|---|---|---|

| BUG-01 | FREE (preflight) | Cold start: the system splash showed a cropped logo and no title, unlike iOS. | SM-A075F, vc 3, com.android.vending | android | fix-if-cheap | verified | 2466fc09, cb3b68c6 | User accepted the splash on vc 3, 2026-11-02; D1.SP1 still runs |
| BUG-02 | FREE (2026-10-30, carried over) | Client Search tab was a placeholder on vc 1 (DEF-2). | SM-A075F, vc 3, com.android.vending | android | fix-if-cheap | verified | Phase 16.1 | User confirmed Search works on vc 3, 2026-11-02 |
| BUG-03 | FREE (2026-10-30, carried over) | Stripe tax ID (TIN) prompt seen on client account B. Clients have no payouts, so it is not needed. A Connect status was not read because P0.8 was skipped. | SM-A075F, vc 1, com.android.vending | backend | post-release | reported (backend) |  |  |
| BUG-04 | FREE (preflight) | Settings, Apps, ClickMe, Notifications shows only allow and vibration settings; the five channels exist in dumpsys. May be behind a Notification categories entry on One UI. | SM-A075F, vc 3, com.android.vending | android | post-release | post-release |  |  |
| BUG-05 | D1.18 | Book Now on a paid topic, pick the saved card, Continue. Stripe SetupIntent confirm succeeds (Stripe log), the sheet closes, the form returns, no booking and no hold. A second Book Now shows "Couldn't present the payment sheet". Reproduced twice, second time with a new SetupIntent. Cause from Stripe 23.24.0 source: on Completed the FlowController sends the option callback with no option, the app read it as a dismissal and dropped Completed. Evidence logs/session-A/D1.18-full.log, D1.18-retry-full.log. | SM-A075F, vc 3, com.android.vending | android | blocking (prevents a booked session or call from happening) | fixed (pending re-test) | f94d6bbf |  |
| BUG-06 | free-form: stuck spinner under the payment sheet | Found in code review of BUG-05, not seen on a device. The host activity is recreated while the sheet is open, then the user picks a card: the option callback runs before the new controller is stored, confirm does nothing, Book Now spins forever. D1.23 exercises this. Cause confirmed from Stripe 23.24.0 source (build() registers the launcher and replays the pending result while the activity is started). The JVM test covers the confirm gate, not a real activity recreation, so device re-test D1.23. | SM-A075F, vc 3, com.android.vending | android | fix-if-cheap | fixed (pending re-test) | 0c2a12bc |  |
| BUG-07 | free-form: Stripe Link in the payment sheet | Found in code review of BUG-05, not seen on a device. If Link is enabled on the live account, Link can dismiss with no callback (stuck spinner) or its logout can drop the chosen card. The sheet configuration leaves Link at its default. JVM test reads the configuration back; whether Link is really gone from the sheet is a device check. | SM-A075F, vc 3, com.android.vending | android | fix-if-cheap | fixed (pending re-test) | 85c1e6da |  |
| BUG-08 | free-form: payment error dialog hides the cause | Any non-card Stripe failure shows "Couldn't present the payment sheet", and the real cause is discarded and not logged. Seen in D1.18. Fix is the log line (tag StripeSheet); the dialog wording is unchanged. Next time it shows, read logcat tag StripeSheet for the cause. | SM-A075F, vc 3, com.android.vending | android | fix-if-cheap | fixed (pending re-test) | d063f51c |  |
| BUG-09 | D1.10; free-form: stuck on Waiting | The iPhone expert hangs up and rejoins. The Samsung stays on "Waiting for other person" though voice works both ways. Log has no remote offline or remote joined, only remote audio state changes. Likely cause: call.ended sets the remote Disconnected and only RemoteJoined restores it. Evidence logs/session-A/D1.10-retest.log, D1.10-retest-after-rejoin.png; contrast D1.SP2.log, where offline and joined both fired. Fix: Agora remote audio state decoding now counts as the peer being back. JVM tests cover the callback mapping and the event; the real rejoin is a device re-test. Residual: audio decoding just after call.ended with the peer still in the channel could flash Active until the offline callback. | SM-A075F, vc 3, com.android.vending | android | fix-if-cheap | fixed (pending re-test) | 16578211 |  |
| BUG-10 | free-form: End Call cut off | 720x1600, font 1.0. In the last 5 minutes, with the banner and time-left chip both showing, call content overflows and End Call is cut off at the bottom; swiping up reveals it. The navigation bar also draws over End Call whenever the banner shows. Evidence logs/session-A/D1.07-call-screen.png, D1.07-after-end.png. Fix: End Call is pinned below the scrolling content (Robolectric test at 360x640 and 360x800 with a 48 dp nav inset, 320x640 at font 2.0). The test models the nav bar as an inset, so confirm on the Samsung with the banner showing. 15-DEVICE-CHECKLIST D-02: still a height rule, no device-type layout. | SM-A075F, vc 3, com.android.vending | android | fix-if-cheap | fixed (pending re-test) | fada5e4f |  |
| BUG-11 | D1.SP1; free-form: splash glow | Cold start: during the logo fade-in the glow is cut to a hard-edged square, then jumps to round when the fade ends. Likely the fade layer clipping the glow to the logo size. Evidence logs/session-A/D1.SP1-cold.mp4, D1.SP1-sheet.png. | SM-A075F, vc 3, com.android.vending | android | fix-if-cheap | open |  |  |
| BUG-12 | free-form: Dashboard tab label wraps | Font 1.3, expert mode: the selected bottom tab label breaks as "Dashboar / d". Fine at 1.0. Evidence logs/session-A/D1.SP4-font-sheet.png. | SM-A075F, vc 3, com.android.vending | android | fix-if-cheap | open |  |  |
| BUG-13 | D1.05; free-form: Read label | The Read label does not update live in an open chat on either app and needs a reopen; the server saved it. Waiting 10 s before the peer opens the chat does not help, so a send-ack race is not the cause. Points at the backend, not proven. | SM-A075F, vc 3, com.android.vending; iPhone, production iOS | backend | fix-if-cheap. Blocks Android release: no. | reported (backend) |  |  |
| BUG-14 | free-form: overtime cutoff | iOS ends the call at scheduled end + 5 min (marked TEMP, confirmed on device), Android at end + 60 min. Booking closes at end + 15 min. | iPhone, production iOS | ios | post-release. Blocks Android release: no. | reported (ios) |  |  |
| BUG-15 | free-form: Join window | On a booking longer than 60 minutes, iOS Join closes at start + 60 min while Android stays open until scheduled end (Phase 9 WR-09 user decision). Seen on the 90-minute booking. Sync iOS to the Android rule. | iPhone, production iOS | ios | post-release. Blocks Android release: no. | reported (ios) |  |  |
| BUG-16 | D1.SE2; free-form: tag pills | iOS Search result tag pills truncate with an ellipsis; Android shows the full text. User prefers Android. | iPhone, production iOS | ios | post-release. Blocks Android release: no. | reported (ios) |  |  |
| BUG-17 | D1.15; free-form: lone card default | A lone saved card shows no Default badge: the server does not promote a card when none is default or when the default is removed. Booking still works. Set as default works. | SM-A075F, vc 3, com.android.vending | backend | post-release. Blocks Android release: no. | reported (backend) |  |  |
| BUG-18 | free-form: private note on Upcoming Details | Product request: show the private note on Upcoming Booking Details on both apps. Today neither shows it (16.4 D-07, iOS parity). | SM-A075F, vc 3, com.android.vending; iPhone, production iOS | android, ios | post-release. Blocks Android release: no. | post-release |  |  |
| BUG-19 | free-form: expert sees the review | Product request: the expert Booking Summary should show that the client left a review. Neither app shows it; the expert booking controller has no review field in a grep, so a backend field is likely needed. | iPhone, production iOS | ios, android, backend | post-release. Blocks Android release: no. | post-release |  |  |
| BUG-20 | free-form: wrong account shown once | 17:02 client account; about 17:04 logout and login reported as the client account; 17:09 cold start showed the expert Dashboard with the expert photo. Not reproduced by a plain relaunch at 17:32. The user later said the A and B labels were unclear, so the login report is unreliable; most likely a login mix-up. Act only if it recurs. Evidence logs/session-A/D1.SP4-font-sheet.png. | SM-A075F, vc 3, com.android.vending | android | post-release | post-release |  |  |
| BUG-21 | free-form: malformed client secret | Found in code review of BUG-05, not seen on a device. The payment method reader catches only Stripe and IO exceptions; a malformed server secret may crash. Low risk, the secret comes from our server. | SM-A075F, vc 3, com.android.vending | android | post-release | post-release |  |  |
