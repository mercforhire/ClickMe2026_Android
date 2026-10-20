# Phase 3: Login, Password Recovery & Realtime - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-20
**Phase:** 03-login-password-recovery-realtime
**Areas discussed:** Login screen behavior, Recovery flow gaps, Socket lifecycle, Event surface & proof

---

## Login screen behavior

| Question | Options | Selected |
|----------|---------|----------|
| 401 display | Inline only / Match iOS exactly (inline + alert) / You decide | Inline only |
| Sign Up link before Phase 4 | Placeholder route / Hide until Phase 4 / Disabled footer | Placeholder route |
| Validation timing | Match iOS (after first submit, then live) / Validate on blur | Match iOS |
| Submit button when invalid | Always enabled / Disabled until valid | Always enabled |

---

## Recovery flow gaps

| Question | Options | Selected |
|----------|---------|----------|
| Resend code | Match iOS (none) / Add resend w/ 30s cooldown | Match iOS |
| Return to Login | Email pre-filled / Exactly as left / You decide | Email pre-filled |
| Back on Password Changed | Back = go to Login / Normal back | Back = go to Login |
| Auth flow navigation | You decide / Nested Navigation Compose | You decide |

---

## Socket lifecycle

| Question | Options | Selected |
|----------|---------|----------|
| Background behavior | Disconnect on bg, reconnect on fg / Stay connected / Grace period | Disconnect on bg, reconnect on fg |
| INVALID_TOKEN handshake | Refresh via REST then reconnect / Keep retrying | Refresh via REST then reconnect |
| Offline launch | Connect with stored token / Wait for /me success | Connect with stored token |
| Connection-state StateFlow | Yes, no UI / No | Yes, no UI |

---

## Event surface & proof

| Question | Options | Selected |
|----------|---------|----------|
| Event shape | Sealed typed events / Typed + per-event flows / Raw JSON | Sealed typed events |
| Client→server emits | Build now, unused / Defer to Phase 12 | Build now |
| Proof of SC-3 | Debug Logcat collector / Debug event viewer screen | Debug Logcat collector |
| Buffering | No replay, small buffer / You decide | No replay, small buffer |

---

## Claude's Discretion

- Auth-flow back-stack implementation under `AppRoute.Login`
- Realtime package layout, lifecycle/session observer placement, socket.io version, password strength meter visuals, recovery screen file names

## Deferred Ideas

- Resend-code with cooldown (Polish, only if requested)
- Socket alive during call foreground service (Phase 9)
- Connection-state UI (not planned)
