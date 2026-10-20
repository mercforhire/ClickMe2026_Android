# Phase 2: Networking, Session & SDK Spikes - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-20
**Phase:** 02-networking-session-sdk-spikes
**Areas discussed:** Launch & offline session, Temporary login screen, Spike app setup, Release signing & upload

---

## Launch & offline session

| Option | Description | Selected |
|--------|-------------|----------|
| Keep session, use cache | Route home from cached profile and mode on a network or 5xx `/me` failure; only an auth failure or suspension logs out | ✓ |
| Match iOS: log out | Any `/me` failure clears the session | |
| Retry screen | Keep tokens, block on a retry state until `/me` succeeds | |

**User's choice:** Keep session, use cache (a deliberate deviation from iOS `tryAutoLogin`)

| Option | Description | Selected |
|--------|-------------|----------|
| ~5 seconds | Splash cap, then the cached path or login | ✓ |
| ~3 seconds | Snappier, falls back more often | |
| You decide | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Match iOS: fail soft | Log out only on a 401 from `/auth/refresh` or a missing refresh token | ✓ |
| Log out on any failure | Strict roadmap reading | |

| Option | Description | Selected |
|--------|-------------|----------|
| Reactive only, like iOS | Refresh only on a 401 through the Authenticator | ✓ |
| Also refresh near expiry | Proactive refresh using `expiresAt` | |

| Option | Description | Selected |
|--------|-------------|----------|
| Detect it, route to stub | Port `setupIncomplete` → `SignupSetup` placeholder | ✓ |
| Treat as home for now | Phase 4 adds the branch | |

| Option | Description | Selected |
|--------|-------------|----------|
| No indicator in Phase 2 | Silent; banner later | ✓ |
| Snackbar on launch | One-time offline snackbar | |

---

## Temporary login screen

| Option | Description | Selected |
|--------|-------------|----------|
| Minimal, real location | Plain M3 form at `screens/login`; Phase 3 restyles it | ✓ |
| Debug-only throwaway | Gated to debug builds | |
| Full iOS port now | Pulls Phase 3 work forward | |

| Option | Description | Selected |
|--------|-------------|----------|
| AlertDialog on login | iOS parity; the flag clears on dismiss | ✓ |
| Inline error on login | Less intrusive | |

| Option | Description | Selected |
|--------|-------------|----------|
| Placeholder Profile tab | Logout in both shells' placeholder tab | ✓ |
| Debug gallery only | Not reachable in release | |

---

## Spike app setup

| Option | Description | Selected |
|--------|-------------|----------|
| spikes/ modules in this repo | `:spikes:stripe`, `:spikes:agora`, own applicationIds | ✓ |
| Separate repo/folder | Fully isolated | |
| Debug screens in :app | Throwaway code in the production module | |

| Option | Description | Selected |
|--------|-------------|----------|
| Real backend, test account | SetupIntent endpoint + booking → `PAYMENT_REQUIRES_ACTION` → PaymentLauncher | ✓ |
| Stripe dashboard / CLI | Manual client secrets; needs the backend's secret key | |

| Option | Description | Selected |
|--------|-------------|----------|
| Agora console temp token | Fixed test channel, no booking | |
| Real backend join | `POST /bookings/:id/join` for a real booking | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Login form in each spike | Bare Retrofit `/auth/login` | ✓ |
| Paste a JWT | Tokens expire | |
| Credentials in local.properties | Auto-login from a gitignored file | |

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, I have them | Test client + Connect-onboarded expert exist | ✓ |
| No, set them up via iOS | | |
| Not sure | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Book on iOS, join from spike | iOS expert is the other side of the call | ✓ |
| Reuse the Stripe spike's booking | Couples the spikes | |

---

## Release signing & upload

| Option | Description | Selected |
|--------|-------------|----------|
| Local only | Gitignored `keystore.properties`, conditional signingConfig | ✓ |
| Local + CI secrets | Keystore in GitHub secrets | |

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, Google-generated key | Play App Signing with a separate upload key | ✓ |
| Upload my own app signing key | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Manual in gradle | `versionCode = 1`, `versionName = "0.1.0"` | ✓ |
| Derived from git | | |

| Option | Description | Selected |
|--------|-------------|----------|
| prod | Upload `prodRelease` for registration | ✓ |
| dev | Upload `devRelease` | |

---

## Claude's Discretion

- Networking and session package layout, Moshi codegen vs reflection (must survive R8), the `/me` retry trigger after an offline launch, how the suspended event reaches `AppViewModel`, debug OkHttp logging and redaction, and the `ErrorCode` → message mapping.

## Deferred Ideas

- Global offline banner (Polish phase)
- CI-signed release bundles (Phase 17)
- Proactive token refresh (not planned)
