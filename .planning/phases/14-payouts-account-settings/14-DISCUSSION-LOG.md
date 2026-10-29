# Phase 14: Payouts & Account Settings - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-28
**Phase:** 14-payouts-account-settings
**Areas discussed:** Extra hub rows, Delete account flow, Payouts depth, Edit profile scope

---

## Extra hub rows

| Option | Description | Selected |
|--------|-------------|----------|
| Hide them | Hubs show only working rows; the extras go to the backlog | |
| Show 'coming soon' | Match the iOS layout with placeholder screens | |
| Hide, plus cheap links | Hide the rest, but add Privacy (Custom Tab) and View My Public Profile (existing Expert Profile screen) | ✓ |

**User's choice:** Hide, plus cheap links.

---

## Delete account flow

| Question | Options | Selected |
|----------|---------|----------|
| Where Delete lives | Inside Security (iOS) / Own hub row too | Inside Security |
| Pause Account card | Drop it / Keep, inert | Other: "add a backend request for this feature" |
| Pause scope until the endpoint exists | Build if ready, else hide / Block on backend | Build if ready, else hide |
| Delete steps | Full iOS flow / Trimmed | Full iOS flow |

**Notes:** The user wants Pause Account as a real feature, so a backend request was drafted (`14-BACKEND-REQUEST.md`).

---

## Payouts depth

| Question | Options | Selected |
|----------|---------|----------|
| Transactions | Short list + See all (iOS) / Short list only | Short list + See all |
| Tax & Payout Info | Include it / Defer, ask backend | Include it |

**Notes:** The tax profile goes past the listed requirements. Its gating questions are in the backend request.

---

## Edit profile scope

| Question | Options | Selected |
|----------|---------|----------|
| Saving | Auto-save like iOS / Explicit Save button | Auto-save like iOS |
| Screens | One shared screen / Two screens like iOS | Two screens like iOS |
| Editors | Reuse signup editors / You decide | Reuse signup editors |

---

## Claude's Discretion

- Row order and icons within hub sections, how shared field composables are split, and the auto-save debounce interval.

## Deferred Ideas

- Saved Experts, Payment History, Notification settings, Send Feedback (hidden for v1).
- Pause Account, if the backend doesn't ship it before planning.
- iOS follow-up: the inert Pause card.
