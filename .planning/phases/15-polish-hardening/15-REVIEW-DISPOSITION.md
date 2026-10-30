# Phase 15: Review Disposition

Every finding from 15-REVIEW.md, defaulting to `open`. Update the status column as findings are fixed or accepted.
Fix run 1 (`/gsd-code-review 15 --fix`, Critical+Warning scope): see 15-REVIEW-FIX.md.

Maintained by hand: the GSD disposition script does not parse the lane-letter IDs (`WR-A01`) and writes an empty ledger.

| ID | Status | Note |
|----|--------|------|
| IN-A01 | open | Info, out of fix scope |
| IN-A02 | open | Info, out of fix scope |
| IN-A03 | open | Info, out of fix scope |
| IN-A04 | open | Info, out of fix scope |
| IN-A05 | open | Info, out of fix scope |
| IN-A06 | open | Info, out of fix scope |
| IN-B01 | open | Info, out of fix scope |
| IN-B02 | open | Info, out of fix scope |
| IN-B03 | open | Info, out of fix scope |
| IN-B04 | open | Info, out of fix scope |
| IN-B05 | open | Info, out of fix scope |
| WR-A01 | fixed | 53e9345d; needs human verification (race/ordering) |
| WR-A02 | fixed | 53e9345d; needs human verification (lifecycle) |
| WR-A03 | skipped | Needs on-device TalkBack check: 15-DEVICE-CHECKLIST.md L3, apply fallback if only the label is spoken |
| WR-A04 | fixed | 5d8eedc1 |
| WR-A05 | fixed | ee076c36 |
| WR-A06 | fixed | 2c043ded |
| WR-A07 | fixed | e2443e4d |
| WR-B01 | fixed | 633fe822; check snackbar position on device with keyboard up |
| WR-B02 | fixed | 0087213a; needs human verification (TalkBack announce timing) |
| WR-B03 | fixed | b10b1e41 |
| WR-B04 | fixed | 70618d46; new copy `tax_profile_secrets_not_restored` may want product review |
| WR-B05 | fixed | 349e5b64 |
| WR-B06 | skipped | Conflicts with tested design (`headlineFocusedAgainOnRecreation`); change test + UI-SPEC together if once-per-entry focus is wanted |
