# Phase 4: Signup, Onboarding & Mode Switch - Discussion Log

> **Audit trail only.** Do not use this as input to planning, research or execution agents.
> Decisions are in CONTEXT.md. This log keeps the alternatives that were considered.

**Date:** 2026-10-21
**Phase:** 04-signup-onboarding-mode-switch
**Areas discussed:** publish rules vs backend; email verification & resume; step details; mode switch & carousel

---

## Publish rules vs backend

| Question | Options | Selected |
|---|---|---|
| Photo required to publish? | Require photo / iOS parity / Map the 422 only | Require photo ✓ |
| Province/state | Optional / Required (iOS parity) | Optional ✓ |
| Signup password rule | Match server / Reuse recovery rule / iOS parity | Match server ✓ (8+ characters, a letter and a digit, with strength meter) |
| When is Publish allowed? | Gate Publish only / Gate Next too / No gating | Gate Publish only ✓ |
| Publish 422 `missing_fields` | Name the steps / Generic dialog | Name the steps ✓ |
| Signup role | Expert only / Add a role choice | Expert only ✓ |
| Step save validation | Save what's filled in / Validate per step | Save what's filled in ✓ |

**User's choice:** All recommended options.

---

## Email verification & resume

| Question | Options | Selected |
|---|---|---|
| How to know the email is verified | Check status / iOS parity | Check status ✓ |
| Verified email required to publish? | Not required / Required | Not required ✓ |
| 429 `RATE_LIMITED` on Send code | Restart cooldown / Show message | Restart cooldown ✓ |
| Where resume lands | Hub, pre-filled / First unfinished step | Hub, pre-filled ✓ |
| `GET /expert/profile` fails during the setup check | iOS parity + recheck / Block on setup | iOS parity + recheck ✓ |

**User's choice:** All recommended options.

---

## Step details

| Question | Options | Selected |
|---|---|---|
| Photo crop | Square center-crop / Real crop UI / Upload as picked | **Real crop UI** ✓ (not the recommendation) |
| Crop implementation | Custom Compose / Library / You decide | Custom Compose ✓ |
| Crop output | Circle guide, square file / Free aspect | Circle guide, square file ✓ |
| Crop flow | Pick → crop → preview / Inline | Pick → crop → preview ✓ |
| Timezone list | iOS list + device zone / Full IANA list / Strict iOS parity | iOS list + device zone ✓ |
| Primary tag and language | Selection order / Explicit primary / iOS parity | Selection order ✓ |
| Tag limits | Min 1, no max, no custom tags / Min 1, max 5 | Min 1, no max, no custom tags ✓ |
| Country list | Port the iOS 50 / Full ISO list | Port the iOS 50 ✓ |

**User's choice:** The user chose a real crop UI over the recommended square center-crop. The follow-ups pinned it down as a custom Compose cropper with a circular guide that saves a square file.

---

## Mode switch & carousel

| Question | Options | Selected |
|---|---|---|
| Gate switching to expert | Route by setup status / iOS parity | Route by setup status ✓ |
| How often the carousel shows | Per login / Once per install | Per login ✓ |
| Where Publish lands | Client home / Expert home | Client home ✓ |
| Entry points | Top-bar avatar / Avatar + settings tab | Top-bar avatar ✓ |
| Card images | Bundled drawables / Same remote URLs / Icons only | Bundled drawables ✓ |
| Presentation | ModalBottomSheet / Full screen | **Full screen** ✓ (not the recommendation) |

**User's choice:** All recommended options except presentation, where the user chose a full-screen page over a bottom sheet.

---

## Claude's Discretion

- Hub and Review layout details (Material 3 adaptation; adding a Timezone card to Review is fine)
- Navigation placement of the signup screens and the setup-scoped state holder (port of the iOS signup accumulator)
- How the foreground re-check of setup status hooks in
- Crop math and gesture limits, and the image downscale implementation
- Carousel illustration assets, copied from the iOS asset catalog

## Deferred Ideas

- Client-role signup
- Custom expertise tags
- An explicit primary tag and language picker
- Change-email
- The mode switch card on the profile hubs (Phase 15)
- A maximum tag count
