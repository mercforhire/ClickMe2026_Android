---
phase: 14
review: 14-REVIEW.md
titles: json
findings:
  - id: WR-A01
    severity: warning
    disposition: open
    title: "Irreversible delete-account request pair runs in `viewModelScope` and can be cancelled between `DELETE` success and `markDeleted()`"
  - id: WR-A02
    severity: warning
    disposition: open
    title: "Profile Settings shows an infinite spinner with no error or retry when the first profile load fails"
  - id: WR-A03
    severity: warning
    disposition: open
    title: "Avatar upload on the application scope only catches `ApiException`; any I/O or decode failure crashes the process"
  - id: WR-A04
    severity: warning
    disposition: open
    title: "`ProfileTextField`'s `KeyboardCapitalization.Words` is silently overridden, so names and city do not auto-capitalize (iOS regression)"
  - id: WR-A05
    severity: warning
    disposition: open
    title: "Change-password validation reports only the first failing field, diverging from iOS"
  - id: WR-A06
    severity: warning
    disposition: open
    title: "422 field-error mapping drops messages for unmatched fields and is lossy versus iOS"
  - id: WR-A07
    severity: warning
    disposition: open
    title: "Edits flushed on leave that then fail are lost silently"
  - id: IN-A01
    severity: info
    disposition: open
    title: "Delete Account button shows a bare spinner with no accessible label while deleting"
  - id: IN-A02
    severity: info
    disposition: open
    title: "Top-bar back arrow and system Back behave differently on the Reason step"
  - id: IN-A03
    severity: info
    disposition: open
    title: "Text clamping with `String.take` can split a surrogate pair"
  - id: IN-A04
    severity: info
    disposition: open
    title: "\"Saved\" check flashes even when `buildProfilePatch` returns null and nothing was sent"
  - id: WR-B01
    severity: warning
    disposition: open
    title: "A spoofed `clickme://connect/refresh` can force a Stripe link re-mint and Custom Tab at any time after the first setup"
  - id: WR-B02
    severity: warning
    disposition: open
    title: "`isAllowedOnboardingUrl` accepts any https host, so it is not an allow-list"
  - id: WR-B03
    severity: warning
    disposition: open
    title: "`retryPage()` during a pull-to-refresh can skip a ledger page"
  - id: WR-B04
    severity: warning
    disposition: open
    title: "One rejected meeting link makes every later auto-save report failure"
  - id: IN-B01
    severity: info
    disposition: open
    title: "Clearing the meeting link looks like it worked but does nothing"
  - id: IN-B02
    severity: info
    disposition: open
    title: "Side effect inside a `MutableStateFlow.update` lambda"
  - id: IN-B03
    severity: info
    disposition: open
    title: "First load shows \"Ready\" or \"On hold\" when the payout summary call failed"
  - id: IN-B04
    severity: info
    disposition: open
    title: "\"See all\" derivation differs between Payouts and the Activity list"
  - id: WR-C01
    severity: warning
    disposition: open
    title: "A Payouts push opened while Payouts is already open from the Profile hub stacks a second Payouts instance"
  - id: IN-C01
    severity: info
    disposition: open
    title: "Leftover string `profile_log_out` is now unused"
  - id: IN-C02
    severity: info
    disposition: open
    title: "Self-view still shows two action-button skeleton pills, then they collapse"
  - id: IN-C03
    severity: info
    disposition: open
    title: "Hub scaffold sets `scrolledContainerColor` without a scroll behavior, so it never takes effect"
  - id: IN-C04
    severity: info
    disposition: open
    title: "A few Phase 14 strings read poorly under TalkBack"
  - id: IN-C05
    severity: info
    disposition: open
    title: "Plural item uses non-positional `%d`"
open: 25
total: 25
recorded: 2026-10-29T16:26:47.864Z
---

# Phase 14: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-A01 | warning | open | - |
| WR-A02 | warning | open | - |
| WR-A03 | warning | open | - |
| WR-A04 | warning | open | - |
| WR-A05 | warning | open | - |
| WR-A06 | warning | open | - |
| WR-A07 | warning | open | - |
| IN-A01 | info | open | - |
| IN-A02 | info | open | - |
| IN-A03 | info | open | - |
| IN-A04 | info | open | - |
| WR-B01 | warning | open | - |
| WR-B02 | warning | open | - |
| WR-B03 | warning | open | - |
| WR-B04 | warning | open | - |
| IN-B01 | info | open | - |
| IN-B02 | info | open | - |
| IN-B03 | info | open | - |
| IN-B04 | info | open | - |
| WR-C01 | warning | open | - |
| IN-C01 | info | open | - |
| IN-C02 | info | open | - |
| IN-C03 | info | open | - |
| IN-C04 | info | open | - |
| IN-C05 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
