---
status: testing
phase: 14-payouts-account-settings
source: [14-VERIFICATION.md]
started: 2026-10-29T16:31:10.605Z
updated: 2026-10-29T16:31:10.605Z
---

## Current Test

number: 1
name: Stripe Connect round trip on device (SC1)
expected: |
  See behavior_unverified_items. After return, balance and Connect status reflect the backend answer; ON_RESUME reload is the backstop if the redirect is blocked
awaiting: user response

## Tests

### 1. Stripe Connect round trip on device (SC1)
expected: See behavior_unverified_items. After return, balance and Connect status reflect the backend answer; ON_RESUME reload is the backstop if the redirect is blocked
result: [pending]

### 2. Delete Account end to end on device (SC4, PROF-04)
expected: Warning, Reason plus password, one delete-request plus one DELETE; the Farewell stays visible through the session teardown; Back to Login lands on Login; process death after the DELETE leaves the next launch on Login
result: [pending]

### 3. Avatar change with the system photo picker, then crop, then upload (PROF-02)
expected: Picker opens, crop screen opens, the new avatar shows in the hub and Profile Settings after upload
result: [pending]

### 4. 200% font scale and TalkBack pass over hubs, Settings, Account Security, Delete, Help, Payouts, Activity and Tax (plan 14-16 backstop truths)
expected: No clipped text; save status, password toggle state, FAQ expanded state and the selected delete reason are announced
result: [pending]

### 5. Decision on review WR-B01 and WR-B02 (security hardening, see Advisory below)
expected: Developer chooses: fix before release (about 10 lines, two tests) or accept and record
result: [pending]

## Summary

total: 5
passed: 0
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps
