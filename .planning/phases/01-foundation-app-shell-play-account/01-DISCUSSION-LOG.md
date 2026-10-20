# Phase 1: Foundation, App Shell & Play Account - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-19
**Phase:** 01-foundation-app-shell-play-account
**Areas discussed:** Navigation & modules, Keys & build flavors, CI pipeline, App identity & Play

Earlier decisions carried in: Nunito font, dark only, iOS code as the stand-in for the Google Stitch designs (UI-SPEC), and the package `com.uptrendinvestments.clickmeapp`.

---

## Navigation & modules

| Option | Description | Selected |
|--------|-------------|----------|
| Navigation 3 | Newer Compose navigation; each tab's history is an app-owned list, so separate tab histories and push deep links are easy | ✓ |
| Navigation Compose 2 | Mature, type-safe routes; separate tab histories rely on saveState/restoreState | |
| You decide | The researcher picks | |

| Option | Description | Selected |
|--------|-------------|----------|
| Single app module | Matches the CLAUDE.md folder layout; simplest | ✓ |
| Core + app modules | Clearer boundaries, more Gradle setup | |
| Feature modules | Overkill for this size | |

**User's choice:** Navigation 3; a single app module.

---

## Keys & build flavors

| Option | Description | Selected |
|--------|-------------|----------|
| Committed per flavor | In build.gradle.kts, like iOS's Environments.plist | ✓ |
| local.properties + CI secrets | Kept out of git | |

| Option | Description | Selected |
|--------|-------------|----------|
| Live mode | Backend uses live Stripe keys | |
| Test mode | Backend uses test keys pre-launch | ✓ |
| Not sure | Check later | |

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, .dev suffix | Dev and prod side by side | |
| No, same ID | One install at a time; one Firebase app | ✓ |

**User's choice:** Keys committed per flavor. The backend is in Stripe test mode. Dev and prod share one app ID.
**Notes:** The iOS environment is set to Production (`pk_live`) while the backend is in test mode. This was flagged to the user to confirm on the iOS side.

---

## CI pipeline

| Option | Description | Selected |
|--------|-------------|----------|
| Lint + tests + build | Lint, unit tests, both flavor builds, hex-literal check | ✓ |
| Minimal: build only | Compile only | |
| Full: + emulator UI tests | Slow/flaky early on | |

| Option | Description | Selected |
|--------|-------------|----------|
| ktlint, auto-format | Standard style, CI fails on violations | ✓ |
| detekt + ktlint | Deeper analysis, noisier | |
| None | Android lint only | |

| Option | Description | Selected |
|--------|-------------|----------|
| Every push + PRs | All branches | ✓ |
| Only PRs and main | Fewer runs | |

**User's choice:** GitHub Actions with lint, unit tests, both flavor builds and ktlint on every push and PR.

---

## App identity & Play

| Option | Description | Selected |
|--------|-------------|----------|
| Derive from iOS icon | Adaptive + themed icon from the iOS 1024px art | ✓ |
| I'll supply Android art | Placeholder until provided | |

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, have one | | ✓ |
| No / not sure | | |
| Apple dev account has one | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Me, with a checklist | The user does the steps; the plan has a checklist | ✓ |
| Someone else at the company | | |

**User's choice:** Icon derived from iOS. The corporation has a D-U-N-S number. The user does the Play steps from a checklist.

---

## Claude's Discretion

- Package layout inside the single module, library versions, a dev launcher label or badge, CI caching and JDK setup, and how the hex-literal check is implemented.

## Deferred Ideas

- Emulator UI tests in CI (Phase 16)
- detekt
- Module split after the MVP
- Switching Stripe to live mode at release (Phase 17)
