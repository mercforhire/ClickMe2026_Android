---
phase: "16"
slug: "cross-platform-qa"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
block_on: high
register_authored_at_plan_time: true
created: "2026-11-05"
---

# Phase 16 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Phase 16 added no app code, endpoints or schema. Its threats are about process: secrets in committed docs, the upload key, real money on live Stripe, deleting real accounts, and proving which build was tested. Mitigations are checked against the committed runbook, bug log, build ledger, STATE and plan summaries. Evidence logs live outside the repo under `/Users/LeonChen/ClickMe-release`.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| repo docs -> private GitHub remote | Runbook, bug log and ledger are committed | ids, verdicts, hashes; must hold no secrets or card data |
| runbook -> person running live money rows | A wrong instruction can move real money or delete a real account | operator instructions (high impact) |
| Mac -> Google Play | The upload key signs what Play re-signs and distributes | signed AAB (integrity-critical) |
| user's real card -> Stripe live via production backend | Paid rows hold or capture real money | PaymentIntents, live card (brand + last 4 only in docs) |
| Samsung logcat -> developer machine | Logs can hold ids, or tokens if a bug leaks them | logs (sensitive; kept out of repo) |
| main checkout -> signed artifact -> Google Play | What is uploaded must be exactly the gated commits | release builds |
| executor -> iOS and backend repos | Cross-repo edits are routed, not made, by plans | source changes (routed to orchestrator) |
| exported ConnectReturnActivity -> any app on the device | A forged `clickme://connect` intent can reach the app | deep-link intent (untrusted) |
| user's real accounts -> production delete endpoint | A deletion is permanent | account deletion |
| planning docs -> phase gate | The sign-off line is what lets Phase 17 start | sign-off record |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-16-01 | Information disclosure | 16-MATRIX-RUNBOOK.md, 16-BUG-LOG.md | high | mitigate | Runbook header line 10 forbids passwords, tokens, keys and card numbers. A grep of all phase docs finds no Stripe key, no JWT and no 16-digit card number. The `sk_live`/`sk_test` hits are prefix mentions only, and `<from password manager>` is a placeholder. Evidence root is outside the repo. | closed |
| T-16-02 | Elevation of privilege | Store-review account / live-cutover cleanup script | high | mitigate | Runbook header lines 10-11 name both as never-use rules. P0.1 confirms neither account is the store-review account. E7.7 passed: no live-cutover or bulk script was run. | closed |
| T-16-03 | Tampering | Delete-account rows on a real account | high | mitigate | P0.10 and D2.17 limit deletion to the Throwaway. E7.5 passed: both throwaways were deleted, and accounts A and B are still active. | closed |
| T-16-04 | Information disclosure | Upload keystore and keystore.properties | high | mitigate | `.gitignore` lines 19-21 cover `*.jks`, `*.keystore` and `keystore.properties`. `git ls-files` finds no match. There is no `Password=` in the ledger, and the ledger rule (line 5) forbids keystore values. The 16-02 summary says keystore.properties was never opened. | closed |
| T-16-05 | Tampering | Wrong, unsigned or debug build uploaded | high | mitigate | Ledger B4 checks (jarsigner, apksigner, keytool cert pin, aapt2 versionCode, google_app_id). Every Uploads row (vc 1-5) shows Cert pin match = yes with the AAB sha256. | closed |
| T-16-06 | Denial of service | Upload key lost | medium | mitigate | 16-02 user_setup required a password-manager backup. Play App Signing allows an upload-key reset through Play support. The backup is the user's own action and cannot be checked from the repo. | closed |
| T-16-07 | Tampering | Matrix runs on a stale or sideloaded build | high | mitigate | Harness H2. Every ledger row and Session log row records installer `com.android.vending` with the versionCode and lastUpdateTime. | closed |
| T-16-08 | Tampering | Real charges before the user accepts the money facts | high | mitigate | P0.9 plan decided by the user. 16-03 key-decisions: "Money plan option-a stays", with the 3DS change made by the user on 2026-11-02. No paid row ran in 16-03. | closed |
| T-16-09 | Information disclosure | Card details or dashboard data in results/runbook | medium | mitigate | Cards are referenced by brand only (e.g. "Mastercard 2018"). The Paid bookings table holds only booking id first 8 and PI last 6. No card numbers were found. | closed |
| T-16-10 | Spoofing | Push/realtime to the wrong phone via a stale token row | medium | mitigate | P0.6 passed (vc 3, 2026-11-02): A has one ios row and B has one android row in `device_tokens`. The leftover-token-on-logout case was found and logged as BUG-24 (ios). | closed |
| T-16-11 | Tampering | Holds/captures left on the real card | high | mitigate | The Paid bookings table has 10 rows, each with an Actual end: 8 voids and 2 captures. E7.3 found 0 matrix PIs in requires_capture. | closed |
| T-16-12 | Information disclosure | Logcat slices with a JWT or message text | medium | mitigate | Logs are kept under `/Users/LeonChen/ClickMe-release/logs`. Bug rows carry ids, codes and log paths only. No token-leak bug row was needed, and no JWT pattern appears in the repo docs. | closed |
| T-16-13 | Repudiation | Result recorded against an unproven/stale build | high | mitigate | Each Result cell carries `vc N` and a date. The Session log records the dumpsys versionCode and the installer for every session. | closed |
| T-16-14 | Tampering | Batch A build from an unmerged tree or another key | high | mitigate | Ledger vc 4 (469acd78) has Cert pin match = yes and Play proof via `com.android.vending`. Fix batch A gate is green. | closed |
| T-16-15 | Repudiation | Bug downgraded/verified without device re-test | high | mitigate | Bug log status flow (line 36) is `verified` only after re-test. 12 verified rows each cite a vc and date. Ledger batch A: "7 verified, BUG-08 not exercised". | closed |
| T-16-16 | Elevation of privilege | Executor editing the iOS/backend repo (16-05) | medium | mitigate | Cross-repo bugs are filed as `reported (ios)`/`reported (backend)` and handed to the STATE report list. The 16-05 summary records no cross-repo edits. | closed |
| T-16-17 | Spoofing | Forged `clickme://connect/return` intent | medium | mitigate | D2.16 passed (vc 4, 2026-11-05) using H8 `am start` with the forged intent. Caveat: the proof is weak because the account was already complete. | closed |
| T-16-18 | Tampering | Delete-account flow on account A or B | high | mitigate | Deletion was limited to the Throwaway, and the email was confirmed before each Delete tap (16-06 summary). E7.5 shows A and B active. | closed |
| T-16-19 | Tampering | iOS-client holds/captures left on the real card | high | mitigate | Paid bookings rows 8-10 (D2.*) were settled: 2 voids and 1 capture. The capture was kept by user decision (see AR-16-01). | closed |
| T-16-20 | Tampering | Batch B build from an unmerged tree or another key | high | mitigate | Ledger vc 5 (d5fd82d9) has Cert pin match = yes and Play proof. The 16-07 summary says T-16-20 held. | closed |
| T-16-21 | Repudiation | Bug verified without device re-test (16-07) | high | mitigate | Ledger batch B: "3 verified" on the H2-proven vc 5. The 16-07 summary says T-16-21 held. | closed |
| T-16-22 | Elevation of privilege | Executor editing the iOS/backend repo (16-07) | medium | mitigate | 16-07 summary: "no iOS or backend repo was edited". BUG-28's backend half is routed as a cross-repo fix. | closed |
| T-16-23 | Tampering | Matrix hold or capture left unsettled | high | mitigate | E7.1 shows every row has an actual end. E7.3 found 0 requires_capture. All 8 holds were voided. The 2 captures (C3eR3Z, See2oK) are settled and deliberately not refunded (AR-16-01, E7.4 `[-]` by user decision). | closed |
| T-16-24 | Repudiation | Phase 16 closed without user confirmation | high | mitigate | Runbook Section 8 line 173: "Confirmed by the user on 2026-11-05: option-confirm". STATE line 192 records the same dated option. | closed |
| T-16-25 | Information disclosure | Write-back copying ids/emails/log text into STATE | medium | mitigate | The 16-08 write-back carries versionCodes, row ids and BUG ids only. The one email in STATE (`uat.expert.preonboard@clickme.test`, a reserved `.test` sandbox account) dates from Phase 14 (f214b0a9); 16-08 only re-wrapped that line. No card digits and no log lines. | closed |
| T-16-26 | Elevation of privilege | Bulk or live-cutover script used for cleanup | high | mitigate | E7.7 passed: no script was run. Voids and refund decisions went through the Stripe dashboard. The 16-08 summary says the prohibition held. | closed |
| T-16-SC | Tampering | npm/pip/cargo installs | low | accept | No package installs in any Phase 16 plan. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-16-01 | T-16-11, T-16-19, T-16-23 | Deviation from D-04: the 2 live captures (D1.25 C3eR3Z, D2.12 See2oK; CA$5.00 each) were not refunded. The user owns both the client and expert accounts and the platform. The charges remain in earnings, payout and revenue records, and the Stripe fee is kept either way. | User (runbook Section 7 / Section 8) | 2026-11-05 |
| AR-16-02 | T-16-SC | No package installs in Phase 16, so there is no supply-chain surface. | Plan-time disposition (16-01..16-08) | 2026-10-30 |
| AR-16-03 | T-16-17 | D2.16's forged-intent proof is weak: the expert account was already complete, so the intent could not prove that an incomplete account stays incomplete. Accepted as a pass caveat at sign-off. | User (Section 8 caveats) | 2026-11-05 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-05 | 27 | 27 | 0 | /gsd-secure-phase (orchestrator, L1 grep-depth; auditor skipped per short-circuit: plan-time register, ASVS 1) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-11-05
