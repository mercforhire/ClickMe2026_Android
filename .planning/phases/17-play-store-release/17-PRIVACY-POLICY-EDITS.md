# Privacy Policy Edits for ClickMe (Phase 17, plan 03, D-06)

What this is: an edit list for the live privacy policy at https://www.uptrendinvestments.net/clickme/privacy.html. You publish the edits on uptrendinvestments.net. They make the policy agree with the Play Console Data safety answers and with the new deletion page (`17-DELETE-ACCOUNT-PAGE.html`, published at https://www.uptrendinvestments.net/clickme/delete-account.html). Google compares the three, and a Data safety form that contradicts the policy is rejected.

Live page fetched: 2026-11-06. "Last updated" value seen on the live page: **31 August 2026**. Anchors below are quoted from that fetch (tags stripped). Publish the deletion page first, or at the same time as E3, so the link in E3 works.

Answers they must match (confirmed at 17-08): questions 4 and 5 in the table in `17-PAYMENTS-DECISION.md`, section 2.

## Edits

### E1. Name Firebase Cloud Messaging (Google) as the Android push provider

Anchor: Apple and Google — delivering push notifications to your device

Change: replace this provider line (section 4, "Service providers").

New text: Apple Push Notification service, and Firebase Cloud Messaging (Google) — delivering push notifications to your device

Note: the policy then names the Android provider explicitly, so the Data safety form can be traced to it.

### E2. Notification text can carry a booking update or a message preview

Anchor: Push notifications. We may request to send you push notifications regarding your account or certain features of the application(s). If you turn them on we store a device token so we can send them.

Change: insert after this sentence (section on information we collect, "Push notifications").

New text: The notification text can include a booking update (for example, that a session was requested, confirmed, changed, or cancelled) or the first part of a chat message, up to 140 characters, so you can read it on your lock screen. Notifications are delivered through Apple's and Google's push services (Firebase Cloud Messaging on Android). You can turn notifications off in your device's settings or by category in the app.

Source: the backend sends the first 140 characters of a chat message as the push body (`src/controllers/chats.controller.js`, `content.substring(0, 140)`, with "Sent you a message" as the fallback when the content is not text).

### E3. How to delete an account

Anchor: Request deletion of your account from within the app.

Change: replace this list item (section 8, "Account information").

New text: Delete your account in the app (Profile, Security & Password, Delete Account), or request deletion without the app on https://www.uptrendinvestments.net/clickme/delete-account.html.

### E4. What survives deletion

Anchor: Upon your request to terminate your account, we will deactivate or delete your account and information from our active databases.

Change: insert after the sentence that ends "chiefly payment and tax records, which are kept for the required period." (same paragraph as the anchor).

New text: When an account is deleted, your name is replaced by "Deleted user" and your email address, photo, bio and other profile details are removed. Reviews, chat messages and reports involving your account stay in place and are shown as coming from "Deleted user". Records of bookings, payments, payouts and taxes are kept for as long as financial and tax law requires. The deletion page above lists exactly what is deleted and what is kept.

Note: the retention wording "as long as financial and tax law requires" is the default (RESEARCH A11; the code holds no retention periods). It matches the existing sentence "Records of payments, payouts, and tax information are kept for as long as financial and tax law requires, which can be longer than your account lasts." If you give explicit periods at 17-08 (question 8), use them here and on the deletion page.

### E5. Update the date

Anchor: Last updated 31 August 2026

Change: replace the date with the day you publish E1 to E4.

New text: Last updated <publish date, in the same format, for example 8 November 2026>

## Findings to read before publishing

1. **Contact address differs.** The live policy gives clickmeapp@uptrendinvestments.net as the privacy contact (5 mentions). The deletion page and the Console answers use support@clickme.app, as the plan and RESEARCH require. Confirm that support@clickme.app is a mailbox someone reads. If you would rather use clickmeapp@uptrendinvestments.net, change the address on the deletion page (two places in the footer and request section) before publishing.
2. **In-app copy overpromises.** The in-app Delete Account warning (`delete_account_body`) says "All your data, including bookings, profile information, and payment details, will be permanently removed." The backend keeps reviews, chat messages, reports, and booking and payment records. The web page and E4 say what is really kept. This is the same wording Phase 14 copied from iOS; it is not changed in this plan (no app code is touched here). Decide whether to correct it in a closed-track fix build (D-04 allows fix builds). Google compares the in-app text with the page.
3. **Data safety deletion answers.** Declare both an in-app path and the web link, as the page offers both.
