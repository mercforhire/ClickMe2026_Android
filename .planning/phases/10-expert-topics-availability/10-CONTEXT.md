# Phase 10: Expert Topics & Availability - Context

**Gathered:** 2026-10-24
**Status:** Ready for planning

<domain>
## Phase Boundary

An Android expert can publish bookable topics and weekly availability that clients can see (EXP-01 to EXP-04).

In scope:
- The Topics tab (list, empty state, delete).
- The Topic Editor (add and edit), with the `pricing_limits` floor and `PRICE_BELOW_MINIMUM` on the price field.
- The Availability screen: weekly slots, the time-range picker sheet, overlap validation, the timezone picker and the UTC auto-fill.
- The `EditAvailability` call guard.

Out of scope:
- Expert Dashboard (Phase 11).
- Expert Profile hub and Edit Profile (Phase 14).
- Availability overrides (`CreateAvailabilityOverrideRequest`). iOS has the models but no screen in this flow.

</domain>

<decisions>
## Implementation Decisions

Carried forward (not re-opened):
- iOS is the source of truth (DEC-ios-source-of-truth). Material 3, dark only, Nunito. Pixel parity is out of scope.
- API pattern: `ApiRequestUrls` constants, `ClickMeApi` methods, and a Hilt `@Singleton` repository using `apiCall {}`. DTOs are Moshi codegen with explicit `@Json(name=…)`.
- Money: amounts are minor units, and `util/Money.kt` is the shared formatter (JPY has no decimal places).
- Error copy follows the Phase 6–8 patterns: show the server `message`, in a Material `AlertDialog`.
- Phase 9 D-13: availability edits are guarded with `CallGuard.attempt(RestrictedAction.EditAvailability)`. Add the enum entry and its `call_action_*` string.

### Availability entry point & screen
- **D-01: Two entry points.**
  - An "Availability" action in the Topics tab's TopAppBar.
  - A placeholder "Availability" row on the Expert Profile tab, next to Log out. This follows the Phase 7 D-04 Payment Methods precedent; Phase 14's Profile hub replaces the row.
  - Phase 11 adds the iOS Dashboard entry later.
  - The screen mirrors the iOS tree: `screens/expertmode/availability/`.
- **D-02: Save is a TopAppBar action (iOS parity).**
  - It shows a spinner while saving, then a brief "Saved" or checkmark confirmation (about 2s, like iOS `didSave`).
  - Save is disabled while any enabled day has overlapping slots.
  - An overlap banner names the days: "Time slots on {days} overlap. Please adjust them before saving." This is the iOS `overlapMessage` copy, with the days joined as a list.
  - A failed save shows the server message.
- **D-03: The call guard fires on Save, not on opening the screen.** The expert can open and view Availability during a call. Tapping Save runs `CallGuard.attempt(EditAvailability)`, which shows "End the call to edit your availability." and sends no request. The silent UTC auto-save (D-15) must not run during an active call either.
- **D-04: Port the iOS `TimeRangePickerSheet` as a `ModalBottomSheet`.**
  - Title "Select Time Range". Two scrolling columns, Start Time and End Time, in 30-minute steps.
  - Start only lists times before End, and End only lists times after Start.
  - The list scrolls so the selected time is centered when the sheet opens.
  - Cancel and Done buttons.
- Ported as-is from iOS (no discussion needed):
  - Day rows Monday to Sunday, each with an enable Switch, slot start/end buttons, slot remove, and "Add hours". A new slot avoids overlapping existing ones where possible.
  - Hydration from `GET /expert/availability`. A day is enabled exactly when it has at least one valid slot. Malformed `HH:mm` values, or slots with end ≤ start, are dropped.
  - Before the first load, the default schedule is every day 17:00–21:00.
  - The PUT body sends slots sorted by start time, as `HH:mm` from minutes since midnight. Disabled days send `[]`, and the body always contains all 7 days.
  - The local overlap check runs again before the PUT.
  - Loading, error and Retry states follow iOS.

### Topics tab & Topic Editor
- **D-05: Topics tab layout.**
  - Topic cards (icon, title, price, duration) with a trailing ⋮ `DropdownMenu` containing Edit and Delete. Tapping the card opens the editor.
  - With no topics: an empty state saying "Add your first topic", a line explaining that clients can't book until a topic exists (EXP-01), and an Add Topic button.
  - With topics: a FAB adds a new one.
  - Pull-to-refresh, plus loading, error and Retry states, as in iOS `TopicsSetupView`.
- **D-06: Delete asks first.**
  - An `AlertDialog` asks "Delete this topic?" (Delete / Cancel) before `DELETE /expert/topics/:id` (Phase 7 D-06 pattern).
  - A 409 (the topic has active bookings) or any other failure shows the server message in an alert.
  - This deviates from iOS, which deletes straight away. Log an iOS follow-up.
- **D-07: The Topic Editor is a full-screen route pushed onto the Topics tab back stack.**
  - Its TopAppBar has a back arrow, the title "Add Topic" or "Edit Topic", and a Save action.
  - A save failure keeps the editor open and shows the error: `PRICE_BELOW_MINIMUM` on the price field (D-11), anything else in an alert.
  - A successful save pops back and updates the list. iOS replaces the item in place on edit and appends on create.
- **D-08: Port the inline "add expertise to my profile" sheet (iOS `ExpertiseEditorSheet` / `ExpertiseEditorViewModel`) as a `ModalBottomSheet`.**
  - It PATCHes `/expert/profile`.
  - Reuse the signup tags picker logic (`screens/signup/tags/`) where possible.
  - When it closes, the editor's available tags are refreshed, and any selections no longer on the profile are dropped (`applyProfileTagsUpdate`).
- Ported as-is from iOS (no discussion needed):
  - Fields: title (required), description (optional), and duration from 30 to 480 minutes in 30-minute steps, default 60.
  - Icon picker: the 24 curated slugs, plus the topic's current slug if it isn't in the list. Tapping the selected icon clears it.
  - Tag picker: only the expert's **profile** expertise tags (not the full `/meta` catalog), sorted, at most 3, with a local substring filter. Selected tags stay visible while filtering. Tag IDs are sent in lowercase.
  - The required-field error copy is "Please fill in all required fields with valid values."

### Price field & currency (EXP-02)
- **D-09: The price accepts decimals, using the currency's decimal places.**
  - Parse the field into minor units using the topic currency's fraction digits: 0 for JPY, 2 for the others. Use `java.util.Currency` or the `Money.kt` helpers.
  - Editing a topic priced 1250 CAD shows "12.50", not iOS's truncated "12".
  - Reject invalid or negative input with the required-field error copy.
  - This deviates from iOS (whole units, always ×100). Log an iOS follow-up.
- **D-10: Currency.**
  - An existing topic keeps its own currency (`hourly_rate.currency`, falling back to `price.currency`).
  - A new topic uses `pricing_limits.min_session_amount.default_currency`, or `"CAD"` if `/me` hasn't loaded.
  - There is no currency picker. The currency code or symbol shows as a prefix or suffix on the field.
  - This deviates from iOS, which always sends the `/me` default even when editing. Log an iOS follow-up.
- **D-11: The floor and free sessions.**
  - Blank or 0 means a free session (iOS parity). `is_free` is derived from the amount.
  - When the topic is free, the wire `hourly_rate.amount` is the currency's floor, or 100 if limits aren't loaded, because the server requires amount > 0.
  - Helper text under the field: "Leave blank or enter 0 for a free session — otherwise min {floor}". Hide it while `pricing_limits` is null.
  - The floor is looked up as `by_currency[currency]`, falling back to `default_minor_units` (PRICING-LIMITS §2).
  - A paid price below the floor shows the field's error state, both live and on Save: "A paid session must be at least {floor}."
  - A server `422 PRICE_BELOW_MINIMUM` shows the server message on the price field's `supportingText` and keeps the editor open.
- **D-12: Get the floor fresh, without caching it.**
  - Read `pricing_limits` from the in-memory `/me` in `UserManager`, which is fetched each launch and never persisted (MeData.kt already documents this).
  - If it's null when the editor opens, refresh `/me` once.
  - If there's still no floor, skip the client-side check. The server's 422 is the authority.

### Timezone (EXP-04) & unsaved edits
- **D-13: Reuse `constants/Timezones.kt` from the signup timezone step**, including `detected()` and its offset matching and `entryFor()`.
  - If the server returns a zone that isn't in the list, show the server's `display_name`, as iOS does.
  - Expanding the list is a later improvement.
- **D-14: Timezone picker.**
  - Tapping the timezone row opens a `ModalBottomSheet` list with the current zone checked. This replaces the iOS `confirmationDialog`.
  - Keep the "Auto-detect" text button under the row.
  - Picking a zone or auto-detecting changes local state only. It is sent on the next Save.
- **D-15: The UTC auto-fill does a silent PUT (iOS parity).**
  - On the first hydrate of a screen session, if the server zone is `UTC` or `Etc/UTC` (case-insensitive) and the device zone isn't UTC, fill in the detected device zone.
  - After hydration finishes, PUT the fully hydrated schedule silently: no spinner, no flash, no error UI, and failures are swallowed.
  - Skip the PUT if a save is in flight, any day overlaps, or a call is active (D-03).
  - It runs once per screen session, so a later explicit UTC save is not overridden. If it fails, the next open tries again.
- **D-16: No unsaved-changes prompt (iOS parity).** Leaving Availability, or the Topic Editor, with unsaved edits discards them silently. Don't track dirty state.

### Claude's Discretion
- Exact visual layout, spacing and copy beyond what is pinned above. Follow the iOS layouts as stand-ins.
- Route key names. Whether Availability lives in the Topics tab back stack, the Profile tab back stack, or both (each entry pushes onto its own tab's stack).
- How the "Saved" confirmation looks (checkmark icon or snackbar).
- Whether the time picker columns use `LazyColumn` with snapping or a plain scroll.
- The DTOs and repository for `GET`/`PUT /expert/availability` and topic CRUD. Reuse or extend `MyExpertTopicsPayload` and the existing availability DTOs, and add `ApiRequestUrls` constants as needed.
- Research item: confirm whether the backend accepts an end time of "24:00" or "23:59". iOS caps End at 23:30. Keep the iOS cap unless research shows a safe way to allow midnight.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Backend specs
- `backend-reference/docs/ios/PRICING-LIMITS.md`: the `pricing_limits` shape, lookup and fallback, `PRICE_BELOW_MINIMUM`, the free-topic exemption, and the no-cache rule.
- `backend-reference/docs/ios/SESSION-BUFFER.md`: check whether it affects how saved availability turns into client booking slots (success criterion 4).

### iOS source of truth: Topics
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Topics/TopicsSetupView.swift` and `TopicsSetupViewModel.swift`: list, load, save (create or patch, the `isFree` wire amount) and delete.
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Topics/Subviews/` (`TopicsSetupTopicCard.swift`, `TopicsSetupTopicsSection.swift`, `TopicsSetupFlowLayout.swift`)
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Topics/Topic Editor/TopicEditorView.swift` and `TopicEditorViewModel.swift`: fields, validation, floor and hint copy, icon slugs, tag rules.
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Settings/Subviews/ExpertiseEditorSheet.swift` and `ExpertiseEditorViewModel.swift`: the inline profile-tag editor (D-08).
- `ios-reference/ClickMe2026/Model/Requests/ExpertTopicRequest.swift` and `ios-reference/ClickMe2026/Model/Responses/Children/ExpertTopicItem.swift`

### iOS source of truth: Availability
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Availability/AvailabilitySettingsView.swift` and `AvailabilitySettingsViewModel.swift`: hydrate, overlap check, save, UTC auto-fill with silent PUT.
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Availability/Subviews/` (`AvailabilityDayRow.swift`, `TimeRangePickerSheet.swift`, `AvailabilityTimezoneRow.swift`)
- `ios-reference/ClickMe2026/Network/ClickMeAPI+Availability.swift`, `ios-reference/ClickMe2026/Model/Requests/UpdateMyAvailabilityRequest.swift` and `ios-reference/ClickMe2026/Model/Responses/AvailabilityScheduleData.swift`
- `ios-reference/Constants/Timezones.swift`

### Project
- `.planning/REQUIREMENTS.md`: EXP-01 to EXP-04.
- `.planning/ROADMAP.md`: Phase 10 success criteria and the Phase 9 D-13 hand-off note.
- `.planning/phases/09-voice-call/09-CONTEXT.md`: D-12 and D-13 (the guard API and call sites).
- `.planning/phases/07-payment-methods-paid-booking/07-CONTEXT.md`: D-04 (the placeholder Profile row precedent) and D-06 (the confirm-before-destructive pattern).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `managers/CallGuard.kt` and `managers/CallDialog.kt` (`RestrictedAction` enum): add `EditAvailability` and its `call_action_*` string.
- `util/Money.kt`: minor-unit formatting for the floor hint and the error copy.
- `components/buttons/AsyncSaveButton.kt` and the shared validated text field: available if needed. The Save action itself lives in the TopAppBar.
- `constants/Timezones.kt` (`TimezoneEntry`, `detected()`, `entryFor()`): already ported for signup.
- `model/responses/MeData.kt` (`PricingLimits`) and `managers/UserManager.kt`: the in-memory `/me` with `pricing_limits`.
- `model/responses/MyExpertTopicsPayload.kt` and `model/responses/ExpertAvailabilityData.kt` (`AvailabilityDayDto`, `AvailabilitySlotDto`): existing DTOs to reuse or extend.
- `network/ApiRequestUrls.kt`: `EXPERT_TOPICS`, `EXPERT_PROFILE` and `META_EXPERTISE_TAGS` already exist. Add the `expert/availability` constant (the current `EXPERT_AVAILABILITY` is the public `experts/{id}/availability`).
- `screens/signup/tags/SignupTagsViewModel.kt`: tag-picker logic to reuse in the D-08 expertise sheet.
- `screens/settings/paymentmethods/`: patterns for the overflow menu, the remove confirm, and the one-operation-at-a-time spinner.

### Established Patterns
- Expert shell: `screens/expertmode/home/HomeExpertScreen.kt` has `entry<TopicsRoot> { ExpertTabRoot(TopicsRoot) }` as a placeholder, which Phase 10 replaces with the real Topics screen. `entry<ExpertProfileRoot>` has the Log out footer, where the Availability placeholder row goes.
- Each tab keeps its own back stack (Navigation 3 `NavKey` entries). Pushed screens use a TopAppBar with a back arrow.

### Integration Points
- The Topics tab root and the Profile tab footer in `HomeExpertScreen.kt`.
- `CallGuard` (root-level dialog) for the Save action.
- Client side: Expert Profile and Make a Booking (Phases 5 and 6) already read public topics and availability. Success criterion 4 is checked from iOS, plus the Android client view.

</code_context>

<specifics>
## Specific Ideas

- iOS follow-ups to log: delete confirmation (D-06), decimal and currency-aware price parsing (D-09), keeping the topic's own currency on edit (D-10).
- iOS caps the latest end time at 23:30 (the 30-minute grid stops at 23:30). Keep this unless research shows the backend accepts a midnight end.

</specifics>

<deferred>
## Deferred Ideas

- A bigger timezone list or a full IANA searchable picker. A later improvement, shared with signup.
- An unsaved-changes discard prompt. Explicitly declined for now (iOS parity).
- A currency picker for topics. Only CAD and USD are live.

</deferred>

---

*Phase: 10-expert-topics-availability*
*Context gathered: 2026-10-24*
