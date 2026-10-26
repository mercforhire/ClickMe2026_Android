---
status: complete
phase: 10-expert-topics-availability
source: [10-VERIFICATION.md]
started: 2026-10-26T01:16:07Z
updated: 2026-10-26T01:18:20.543Z
---

## Current Test

[testing complete]

## Tests

### 1. SC-1 Topics on device: create a paid topic, edit it (12.50 existing price shows '12.50'), delete with the confirmation; a price below the floor shows the live error; a server 422 PRICE_BELOW_MINIMUM shows on the price field; blank price saves a free topic shown as 'Free'; Edit Expertise adds a profile tag that then appears in the editor picker
expected: All flows work against the production backend as an expert on the Samsung SM-A075F (devDebug)
result: skipped
reason: "Deferred: user skipped device verification on 2026-10-25 ('Skip verification. Continue to the next phase.'); batch with other deferred device checks"

### 2. SC-2 Availability on device: edit slots with Select Time Range; two overlapping slots show the banner and disable Save; Save shows the check for 2 s; reopen shows the same HH:mm; during a live call Save shows 'End the call to edit your availability.' and nothing is sent; while Save is spinning, day toggles and slot edits do nothing
expected: As described
result: skipped
reason: "Deferred: user skipped device verification on 2026-10-25 ('Skip verification. Continue to the next phase.'); batch with other deferred device checks"

### 3. SC-3 Timezone on device: expert with a UTC server zone and no availability on a non-UTC device sees the device zone filled without a prompt and it is stored server-side (reopen); picking a zone then Save persists it
expected: As described
result: skipped
reason: "Deferred: user skipped device verification on 2026-10-25 ('Skip verification. Continue to the next phase.'); batch with other deferred device checks"

### 4. SC-4 cross-client: on the iOS client, the Android expert's new topic is listed and booking slots match the saved availability in the expert's timezone
expected: Topic visible; slots match HH:mm and timezone
result: skipped
reason: "Deferred: user skipped device verification on 2026-10-25 ('Skip verification. Continue to the next phase.'); batch with other deferred device checks"

### 5. French (Canada) locale: type '12,50' in the price field and save
expected: 1250 minor units sent
result: skipped
reason: "Deferred: user skipped device verification on 2026-10-25 ('Skip verification. Continue to the next phase.'); batch with other deferred device checks"

### 6. 200% font scale on Topics, Topic Editor with an error line, an overlapping day and the Time Range sheet; rotation on the editor with an error showing; TalkBack on the Time Range sheet; focus movement into the first invalid field
expected: No clipping, error and focus preserved
result: skipped
reason: "Deferred: user skipped device verification on 2026-10-25 ('Skip verification. Continue to the next phase.'); batch with other deferred device checks"

### 7. Edit Expertise seed failure on device (gap closure, item g): with the network blocked after the app is open, open Edit Expertise; then restore the network and tap Retry. Also open it on a slow connection and tap a tag row while 'Loading your tags...' shows
expected: 'Couldn't load your current tags' with Retry, Save Expertise disabled, no PATCH sent; after Retry the profile tags appear (primary first) and Save enables; taps during Loading do nothing
result: skipped
reason: "Deferred: user skipped device verification on 2026-10-25 ('Skip verification. Continue to the next phase.'); batch with other deferred device checks"

## Summary

total: 7
passed: 0
issues: 0
pending: 0
skipped: 7
blocked: 0

## Gaps
