# Phase 17 Release Checklist (human-only steps)

Every step below needs your accounts, your Mac or your phones, so none of it is done by code plans (manual steps never block code). Tick a box only after its "Check:" result is what you see. Each section is its own item, so an interrupted run resumes at the first unchecked box. Passwords and tokens stay in your password manager and in the backend reviewer document (`APP-REVIEW-ACCOUNT.md` in the backend repo's `docs/` folder); nothing secret is ever pasted into this repo. Accounts and phones are named by role: the "Android client on the Samsung" and the "iOS expert on the iPhone".

Short names used below: `APP` is `/Users/LeonChen/Documents/ClickMe_Android`; `BACKEND` is `/Users/LeonChen/Documents/ClickMe2026 Backend`; `RELEASE` is `/Users/LeonChen/ClickMe-release`; `ASSETS` is `/Users/LeonChen/ClickMe-release/store-assets`; `ADB` is `/Users/LeonChen/Library/Android/sdk/platform-tools/adb`.

## Timeline

Closed test day 0 to day 14, then Google's production-access review (usually seven days or less), then the production review: roughly three to four weeks from day 0. Every day the closed test starts later moves the public release by one day (D-01). Sections 1 to 5 can start today and need no code, so the 14-day clock is never held up by the code plans.

## Branch

Section 1 decides it. Gate shown: run sections 10 to 13 (closed track, 14 days, apply). Gate not shown: skip sections 11 and 13, release vc 6 on Internal testing, run smoke run 1 on that Play install (the same AAB), then go to section 14.

## 1. Production-access gate (D-01)

Needs: Play Console, organization account.

1. Open the Dashboard and look for "Apply for production" or "Get access to production".
2. Settings, Developer account: note the account type. Save a screenshot.

Check: one line "gate shown: yes or no (date)".

- [ ] (1) Gate answer recorded

## 2. Play support question (D-01)

Needs: Play Console.

1. Send a question to Play developer support about the closed-testing requirement. Do not wait for the reply.

Check: the case number is recorded.

- [ ] (2) Support question sent

## 3. Tester service (D-02, D-03)

Needs: a payment card.

1. Choose a tester service and pay for at least 20 testers.

Check: the vendor confirms the order.

- [ ] (3) Tester service chosen and paid

## 4. Foreground-service video (D-16)

Needs: the Samsung with the Play install.

1. Record the 30 to 60 second video and upload it as Unlisted.

Check: the link plays in a private browser window.

- [ ] (4) Foreground-service video recorded

## 5. Feature graphic (D-16)

Needs: an image editor.

1. Make the 1024 x 500 graphic.

Check: `sips` prints 1024, 500 and `hasAlpha: no`.

- [ ] (5) Feature graphic saved

## 6. Decisions at the 17-08 checkpoint (D-05)

Needs: `17-PAYMENTS-DECISION.md`.

1. Answer the rows of section 2 of that file.

Check: every Answer cell is filled.

- [ ] (6) Decisions answered

## 7. Publish the deletion page and privacy edits (D-06)

Needs: access to the uptrendinvestments.net site.

1. Upload `17-DELETE-ACCOUNT-PAGE.html` and apply edits E1 to E5.

Check: the deletion page returns 200.

- [ ] (7) Deletion page and privacy edits live

## 8. Console App content (REL-06, D-07, D-08)

Needs: Play Console and the `17-CONSOLE-PASTE.md` file.

1. Re-seed the reviewer account, then paste sections 1 to 9.

Check: App content shows nothing under "Needs attention".

- [ ] (8) App content complete

## 9. Store listing and graphics (REL-04, D-16)

Needs: the store assets.

1. Paste section 10 of `17-CONSOLE-PASTE.md` and upload the graphics.

Check: the main store listing saves with no field error.

- [ ] (9) Store listing saved

## 10. Upload vc 6 and open the closed track (D-10)

Needs: the archived AAB.

1. Create the closed track and upload `/Users/LeonChen/ClickMe-release/clickme-1.0.0-6.aab`.

Check: the closed release reads Available.

- [ ] (10) vc 6 on the closed track

## 11. Closed test, 14 days (D-01, D-04)

Needs: at least 12 opted-in testers.

1. Record day 0 and the opted-in count daily.

Check: 14 consecutive days with at least 12 opted in.

- [ ] (11) 14 days completed

## 12. Smoke run 1 (D-13, D-14, D-15)

Needs: the Samsung and the iPhone.

1. Run the smoke table on the closed-track install.

Check: every row is marked pass.

- [ ] (12) Smoke run 1 passed

## 13. Apply for production (D-01)

Needs: day 14 reached.

1. Submit the three-part application.

Check: the confirmation email arrives.

- [ ] (13) Production access applied for

## 14. Production release (D-11, D-12)

Needs: production access granted.

1. Promote the closed release to Production at 100% in Canada and the US.

Check: the public listing returns 200.

- [ ] (14) Production release live

## 15. Smoke run 2 (D-15)

Needs: the Samsung and the iPhone.

1. Repeat the short smoke from the public listing.

Check: every row is marked pass.

- [ ] (15) Smoke run 2 passed

## 16. Close-out

Needs: the repo checkout.

1. Tick the requirements and record the dates.

Check: the requirement boxes are ticked.

- [ ] (16) Release closed out
