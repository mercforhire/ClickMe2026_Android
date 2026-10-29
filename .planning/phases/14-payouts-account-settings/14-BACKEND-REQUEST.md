# Phase 14: requests for the backend agent

**Written:** 2026-10-28 · **Status:** not sent yet (update when sent and answered) · **Context:** `14-CONTEXT.md` D-06, D-08

## Message (paste as is; no secrets)

> **Context:** ClickMe Android port, Phase 14 (profile hubs, account deletion, expert payouts). Two items.
>
> **1. Feature request: Pause Account.** The iOS delete-account screen shows a "Pause Account: temporarily hide your profile and services" card. The button does nothing (`DeleteAccountView` `onPauseAccount` defaults to `{}`), and we found no backend endpoint for it. The user wants it as a real feature. Can you add one? What we'd need:
> - An endpoint to pause and to resume, for example `POST /me/pause` and `POST /me/resume`, or a field on `PATCH /me`. Tell us the auth requirements (password?) and the response shape.
> - Precisely what "paused" means. Is the expert hidden from discovery and search, and their topics not bookable? What happens to existing upcoming bookings: kept, cancelled, or blocked? Can a paused user still log in, chat and join existing sessions? Does it apply to clients, experts, or both?
> - How the app learns the state, for example a `paused` flag on `GET /me`. How a paused user resumes: automatically on login, or with an explicit Resume action?
> - Any push or notification side effects.
> - Error codes. Please add them to `docs/ios/` like the other contracts.
>
> We plan Phase 14 soon. If this can't ship before then, Android hides the card and Pause moves to a later phase. Please give a rough ETA either way.
>
> **2. Tax & Payout Info.** Android is porting the expert tax profile (iOS `ExpertTaxProfileView`, `ClickMeAPI+TaxProfile`).
> - Does a missing or incomplete tax profile block payouts or Connect onboarding? If so, which error or status does the app see?
> - When is the TIN required (the `saved: false, tin_required` response)? Is CA still the only supported country?
> - Is there a contract doc for the tax-profile endpoints (request and response, validation, error codes)? If not, please add one to `docs/ios/`.

## Answers

_(paste the backend agent's reply here)_

## Android follow-ups (once answered)

- **Pause endpoint ready before planning:** add the Pause card to the delete flow (D-06) and a requirement ID for it.
- **Pause not ready:** keep the card hidden and add a backlog item for Pause Account.
- **Tax profile answers:** feed them into the D-08 plan (gating, TIN handling, error mapping).
