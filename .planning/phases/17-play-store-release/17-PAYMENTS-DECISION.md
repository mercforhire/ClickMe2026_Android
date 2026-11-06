# Payments Policy Decision and 17-08 Answer Sheet (Phase 17, plan 03)

Purpose: records the evidence behind D-05 (keep Stripe, no Google Play Billing), and lists every answer you confirm at the 17-08 decision checkpoint, each with a recommended default and its source. The checkpoint presents the table in section 2 and writes your answers back into the `Answer` column.

## 1. Payments policy (D-05)

### Correction to the D-05 basis

CONTEXT D-05 cited "real-time person-to-person services". That phrase is Apple's wording, not Google's. Google's Payments policy page (answer 9858738) has no such exemption. The exemption that covers ClickMe is in a different page, Google's Help Center FAQ "Understanding Google Play's Payments policy" (answer 10281818). The conclusion of D-05 stands; its cited basis is replaced by this one.

### Evidence: Google FAQ, copied verbatim

Source: https://support.google.com/googleplay/android-developer/answer/10281818 (page "Understanding Google Play's Payments policy"). **Fetched 2026-11-06** with curl and read from the returned HTML.

> I'm a developer of an app that offers 1:1 online paid services. Do I have to use Google Play's billing system?
>
> If your app provides a 1:1 online paid service, you are not required to use Google Play's billing system if:
> - the paid service is between two individuals
> - the paid service is not available for replay afterwards (that is, the session is not recorded and cannot be accessed or used again) in any Play-distributed app
>
> Examples of 1:1 online paid services that do not require the use of Google Play's billing system include:
> - education or hobby classes (such as music or art lessons)
> - health coaching (such as personal trainer sessions or counseling services)
> - specialist advisory services (such as financial guidance, excluding customer support, or similar)

Note on punctuation: the page uses curly apostrophes (Google Play's); they are shown straight here.

### Evidence: Payments policy page 9858738

Fetched 2026-11-06 from https://support.google.com/googleplay/android-developer/answer/9858738. The policy text has no 1:1 or person-to-person services exemption. It requires Play Billing for in-app purchases of items, subscriptions, app functionality and cloud software, "unless Section 3, 8, or 9 applies". The list of cases where "Google Play's billing system must not be used" is the one containing physical goods, physical services, credit card and utility bill remittances, "peer-to-peer payments, online auctions, and tax exempt donations", gambling, and categories barred by Google's Payments Center content policies. (The FAQ above uses "peer-to-peer payment" for payments between users, which is not what ClickMe sells.) The 1:1 services statement exists only in the FAQ.

### ClickMe facts that put it inside the FAQ

| Fact | Basis |
|------|-------|
| The product is a paid live one-on-one voice session between two individuals, a client and an expert | App and backend: bookings link one client and one expert; calls are Agora channels for the two of them |
| Sessions are scheduled and charged through Stripe for the expert's time | Phase 7 and later payment flow (PaymentSheet) |
| Calls are not recorded and cannot be replayed | Code check below, and RESEARCH's backend finding that no recording exists |
| No digital goods, credits, coins or subscriptions are sold | No billing library and no in-app product catalogue in the app |
| Experts give specialist advice and teaching, which matches "specialist advisory services" and "education" in the FAQ examples | FAQ examples above |

Code check run 2026-11-06 at the plan base commit, over `app/src/main` (Agora and Android audio-recording calls):

```
grep -rniE 'startAudioRecording|startRecording|MediaRecorder' app/src/main
```

Result: **prints nothing** (baseline at HEAD: nothing). The same search over the backend `src` directory, with `AudioRecord` added, also printed nothing. The Agora recording APIs are not used and the app has no MediaRecorder. `RECORD_AUDIO` is used only to send the live call audio.

### Conclusion

Stripe is permitted. Play Billing is not required for ClickMe sessions, under FAQ 10281818 (the service is between two individuals and is not replayable).

### Residual risk

This is a Help Center FAQ, not the policy text (RESEARCH Pitfall 10). A reviewer who enforces only the policy page could reject Stripe. Keep this dated copy as evidence. D-05 is rated costly, which is why you confirm it at 17-08 (row 1) rather than it being assumed.

### Fallback if a reviewer objects

Appeal the rejection citing FAQ 10281818 and this dated copy. Do not rebuild payments on Play Billing as a first response. If the appeal fails, the choice at that point is a Play Billing rebuild or ending the Play release, which is a new decision.

### Listing-copy rule

The store listing and screenshots describe paid one-on-one sessions between two people only. They never mention recordings, replays, credits, coins, subscriptions or any digital goods, since any of these would break the two conditions of the FAQ.

## 2. Answers for the 17-08 checkpoint

`Recommended` is the default if you say "all recommended". `Answer` is filled at 17-08.

| # | Question | Recommended | Source | Answer |
|---|----------|-------------|--------|--------|
| 1 | D-05 payments: which path? Options: keep Stripe under FAQ 10281818 / move to Play Billing / ask Play support before submitting | Keep Stripe under FAQ 10281818, with the dated copy in section 1 as evidence | D-05; RESEARCH Summary, Pitfall 10; section 1 above | (pending 17-08) |
| 2 | Card data in Data safety: collected and shared? | Collected = Yes (Stripe's SDK transmits it from the device); Shared = No, Stripe is a service provider (flagged assumption A5). Alternative if you want zero doubt: Shared = Yes | RESEARCH Data safety Decision 1; A5 | (pending 17-08) |
| 3 | NFC permission (added by Stripe's SDK): any Console action? | No Console action; optional reviewer note that card-tap input is possible (A12) | RESEARCH Data safety Decision 2; merged prod manifest | (pending 17-08) |
| 4 | Push content: declare that notifications carry booking updates and message previews? | Yes, declare it, and publish privacy edit E2 | RESEARCH Data safety Decision 3; `chats.controller.js` 140-character preview; `17-PRIVACY-POLICY-EDITS.md` E2 | (pending 17-08) |
| 5 | Privacy policy names Agora, Stripe and Firebase Cloud Messaging? | Publish edits E1 to E4 (Agora and Stripe are already named; E1 adds Firebase Cloud Messaging) | RESEARCH Data safety Decision 4; `17-PRIVACY-POLICY-EDITS.md` | (pending 17-08) |
| 6 | Financial features declaration | "My app doesn't provide any financial features" (flagged assumption A8) | RESEARCH Console Declarations; A8 | (pending 17-08) |
| 7 | Developer name on the deletion page | "Uptrend Investments Inc." (confirm the exact string shown on the store listing) | Plan 17-03 Task 1; RESEARCH Open Question 5 | (pending 17-08) |
| 8 | Retention wording for payment, payout and tax records | "As long as financial and tax law requires" (flagged assumption A11); or give explicit periods | RESEARCH A11 and Open Question 6; live policy sentence "kept for as long as financial and tax law requires" | (pending 17-08) |
| 9 | Email deletion requests: response time | Reply from clickmeapp@uptrendinvestments.net to confirm the request, then delete within 30 days of confirmation. Contact address settled by the user on 2026-11-06: clickmeapp@uptrendinvestments.net (support@clickme.app is wrong and is no longer used) | Plan 17-03 Task 1; `17-PRIVACY-POLICY-EDITS.md` finding 1 | (pending 17-08: response time only) |
| 10 | Branch `feature/remember-login-email` (Android 0c9ecc55) in 1.0.0 | No (default; vc 6 is built without it). Yes means it merges and ships as a closed-track fix build before production (D-04 allows fix builds) | RESEARCH Open Question 4; STATE.md line 372 | (pending 17-08) |
| 11 | Ongoing-call notification style (Phase 9 open item "ask before release") | Keep the plain style (default). CallStyle would be a closed-track fix build | STATE.md line 141; Phase 9 | (pending 17-08) |
| 12 | Production-access gate on the Play Console Dashboard today (D-01): shown or not shown | Observation, no default. Shown means 17-09 runs the closed test; not shown means 17-09 can go straight to production | D-01; RESEARCH Open Question 1 | (pending 17-08) |
| 13 | Tester service (D-02) | Testers Community Pro, 25 testers, CA$33 (runner-up TestFi, 16 testers, $39.99); buy at least 20 testers | D-02; RESEARCH "Paid tester services compared" (vendor-stated, fetched 2026-11-05) | (pending 17-08) |

## Flagged assumptions awaiting 17-08

- A5: Google accepts Stripe as a "service provider", so card data is collected but not shared (row 2).
- A8: ClickMe has no "financial features" in Google's sense (row 6).
- A11: the code holds no retention periods, so the page and policy say "as long as financial and tax law requires" (row 8).
