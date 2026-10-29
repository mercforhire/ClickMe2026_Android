# Phase 15: Polish & Hardening - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md. This log keeps the alternatives that were considered.

**Date:** 2026-10-29
**Phase:** 15-polish-hardening
**Areas discussed:** Rotation policy, Error/loading standard, Process-death depth, Verification approach

---

## Rotation policy

| Option | Description | Selected |
|--------|-------------|----------|
| Lock portrait on phones | Matches iOS; less layout work | ✓ |
| Support rotation everywhere | Landscape-usable everywhere | |
| Lock portrait, except call/chat | Mixed policy | |

| Option (large screens) | Description | Selected |
|--------|-------------|----------|
| Usable, not optimized | Nothing clipped, width caps, no tablet layouts | ✓ |
| Ignore large screens | Phones only | |
| Proper adaptive layouts | List-detail panes | |

| Option (call screen) | Description | Selected |
|--------|-------------|----------|
| Same lock app-wide | One policy | ✓ |
| You decide | | |

| Option (SC-2 verification) | Description | Selected |
|--------|-------------|----------|
| Config change + process death | Any recreation; record the reading in VERIFICATION | ✓ |
| Unlock for test build only | Debug and release differ | |

---

## Error/loading standard

| Option (first-load failure) | Description | Selected |
|--------|-------------|----------|
| Shared ErrorState + Retry | Consistent component | ✓ |
| ErrorState with server message | Body override | |
| Match iOS per screen | Inconsistent | |

| Option (refresh failure) | Description | Selected |
|--------|-------------|----------|
| Keep content + snackbar | Retry action; quiet reloads silent | ✓ |
| Keep content, silent | | |
| Replace with ErrorState | | |

| Option (offline) | Description | Selected |
|--------|-------------|----------|
| No dedicated offline UI | Falls into the error paths | ✓ |
| Offline-aware copy only | | |
| Persistent offline banner | | |

| Option (audit scope) | Description | Selected |
|--------|-------------|----------|
| Screens + data-loading sheets | | ✓ |
| Full-screen destinations only | | |
| You decide | | |

---

## Process-death depth

| Option (signup) | Description | Selected |
|--------|-------------|----------|
| Same step + unsaved field text | No secrets in the Bundle | ✓ |
| Same step, server data only | | |
| Include password fields too | | |

| Option (chat composer) | Description | Selected |
|--------|-------------|----------|
| Per-thread via SavedStateHandle | | ✓ |
| Persisted per thread (disk) | | |
| No | | |

| Option (call killed) | Description | Selected |
|--------|-------------|----------|
| Session screen with Join/Rejoin | Never auto-rejoin | ✓ |
| Auto-rejoin | | |
| Home with banner | | |

| Option (sheets/dialogs/scroll) | Description | Selected |
|--------|-------------|----------|
| Input-holding sheets restore; scroll best-effort | | ✓ |
| Everything restores exactly | | |
| You decide | | |

---

## Verification approach

| Option | Description | Selected |
|--------|-------------|----------|
| Automated gate + one device checklist | Non-blocking checklist | ✓ |
| Automated only | | |
| Device pass is blocking | | |

| Option (backlog) | Description | Selected |
|--------|-------------|----------|
| One deduplicated sweep | Stripe and live-call items stay in Phase 16 | ✓ |
| Only Phase 15's own checks | | |

| Option (priority) | Description | Selected |
|--------|-------------|----------|
| 4 core flows first | Then client, expert, settings; leftovers to Phase 16 | ✓ |
| Breadth first | | |

| Option (insets guard) | Description | Selected |
|--------|-------------|----------|
| Design-rules check + tests | | ✓ |
| Audit by hand only | | |
| You decide | | |

## Claude's Discretion

- How the orientation lock is applied, the error and snackbar copy, which ViewModels need SavedStateHandle, and the checklist structure.

## Deferred Ideas

- Disk-persisted chat drafts; adaptive tablet layouts; offline banner.
