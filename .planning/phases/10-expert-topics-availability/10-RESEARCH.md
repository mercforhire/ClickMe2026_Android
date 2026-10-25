# Phase 10: Expert Topics & Availability - Research

**Researched:** 2026-10-24
**Domain:** Android (Compose + Hilt + Retrofit/Moshi) port of iOS expert Topics CRUD, Topic Editor with price floor, weekly Availability with time-range picker and timezone, plus the `EditAvailability` call guard. Backend contract read from `backend-reference/src`.
**Confidence:** HIGH (backend behaviour read from source and executed locally; Android integration points read from the repo; no new libraries)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

Carried forward (not re-opened):
- iOS is the source of truth (DEC-ios-source-of-truth). Material 3, dark only, Nunito. Pixel parity is out of scope.
- API pattern: `ApiRequestUrls` constants, `ClickMeApi` methods, and a Hilt `@Singleton` repository using `apiCall {}`. DTOs are Moshi codegen with explicit `@Json(name=…)`.
- Money: amounts are minor units, and `util/Money.kt` is the shared formatter (JPY has no decimal places).
- Error copy follows the Phase 6–8 patterns: show the server `message`, in a Material `AlertDialog`.
- Phase 9 D-13: availability edits are guarded with `CallGuard.attempt(RestrictedAction.EditAvailability)`. Add the enum entry and its `call_action_*` string.

Availability entry point & screen
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

Topics tab & Topic Editor
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

Price field & currency (EXP-02)
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

Timezone (EXP-04) & unsaved edits
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

### Deferred Ideas (OUT OF SCOPE)
- A bigger timezone list or a full IANA searchable picker. A later improvement, shared with signup.
- An unsaved-changes discard prompt. Explicitly declined for now (iOS parity).
- A currency picker for topics. Only CAD and USD are live.
- Out of scope per the phase boundary: Expert Dashboard (Phase 11), Expert Profile hub and Edit Profile (Phase 14), availability overrides (`CreateAvailabilityOverrideRequest`).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| EXP-01 | Expert sees topics in the Topics tab and creates/edits in the Topic Editor; needs at least one topic to receive bookings | `GET/POST/PATCH/DELETE /expert/topics` contract and response shape (Findings 3, 4, 5); list/editor state design (Architecture); empty-state copy fixed by UI-SPEC |
| EXP-02 | Price floor from `/me` `pricing_limits`, fetched fresh; `422 PRICE_BELOW_MINIMUM` on the price field | Server floor logic and exact error shape (Finding 2); `MinSessionAmount.minMinorUnits` already exists; price parse/format rules (Finding 6, Pitfalls 3, 4) |
| EXP-03 | Weekly availability (minutes from midnight, sent as `HH:mm`) with time-range picker sheet and per-day overlap validation | `PUT /expert/availability` validation rules, 15-min alignment, overlap rule identical to the client check (Finding 1); "24:00" research answer (Finding 7); hydration/format rules (Pitfalls 5, 6) |
| EXP-04 | Pick availability timezone; server UTC means the device zone is filled in quietly | GET default is `UTC` when no row exists; silent PUT is safe and writes the row (Finding 1); `Timezones.kt` behaviour (Finding 9); guard must read `isCallActive`, not `attempt()` (Pitfall 7) |
</phase_requirements>

## Summary

This phase is a port with one real backend dependency surface (`/expert/topics`, `/expert/availability`, `PATCH /expert/profile`, `/me` `pricing_limits`) and no new libraries. Everything needed already exists in the repo: `ValidatedField`, `PickerField`/`PickerSheet`, `SelectableChip`, `EmptyState`, `AuthErrorDialog`, `AuthTopAppBar`, `ProfileMenuRow`, `Money`, `Timezones`, `CategoryIconMap` (all 24 curated slugs are present), `CallGuard`, `MinSessionAmount.minMinorUnits()`, `ErrorCode.PriceBelowMinimum`, and `PullToRefreshBox`/`SnackbarHost` usage in `MyBookingsScreen`. The work is: new DTOs and `ClickMeApi` methods, two repositories, three ViewModels (Topics, TopicEditor, Availability) plus an ExpertiseEditor ViewModel, the screens listed in UI-SPEC, two nav keys, and wiring in `HomeExpertScreen`.

Reading the backend source turned up several places where the CONTEXT/iOS assumptions do not match the server, and the planner should treat these as requirements, not nice-to-haves: (1) `DELETE /expert/topics/:id` is a soft delete that never returns 409 (the iOS comment is stale); (2) the server serializes `price` only (no `hourly_rate`), and `price.currency` can be an empty string for topics created without a currency; (3) Moshi here has no `serializeNulls`, so a PATCH cannot clear the icon or the description unless a custom adapter writes explicit `null` (iOS has the same latent bug because Swift omits nil); (4) the backend does accept an `end_time` of `"24:00"` and generates correct slots through midnight (verified by running the backend code), while `"23:59"` is rejected, so Android must round-trip 1440 minutes even if its picker stops at 23:30; (5) `java.time.LocalTime.of(24, 0)` throws, so time formatting must special-case 1440; (6) the guard for the silent UTC save must read `callGuard.isCallActive.value`, because `attempt()` queues the user-visible dialog.

**Primary recommendation:** Build three layers in this order: (a) pure Kotlin logic with unit tests (`AvailabilityScheduleLogic`: hydrate/overlap/add-hours/format/build-request; `TopicPrice`: parse/format/floor), (b) DTOs + `ClickMeApi` + repositories + the null-preserving PATCH adapter, (c) ViewModels and screens from UI-SPEC. Keep topic data flowing through a stateless repository plus a seed carried in `TopicEditorKey` and a tiny one-shot event channel for "saved" (no singleton topic cache, because nothing in this codebase clears singletons on logout).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Topic list, create, edit, delete persistence | API / Backend (`/expert/topics`) | Android ViewModel (optimistic merge of saved item) | Server owns ids, `sort_order`, `is_free` -> `price_amount = 0`, soft delete |
| Price floor enforcement | API / Backend (authoritative `422 PRICE_BELOW_MINIMUM`) | Android client (UX-only pre-check from in-memory `/me`) | PRICING-LIMITS §3: client check is never authoritative |
| Minor-unit parsing and display of price | Android client | — | Currency fraction digits are a presentation concern; wire is integer minor units |
| Weekly schedule persistence and validation (15-min grid, end > start, overlap) | API / Backend | Android client (same overlap check before PUT) | Server rejects with 422; client pre-check avoids a round trip and names the days |
| Client booking slot generation from saved availability (SC-4) | API / Backend (`generateSlotsForMonth`) | — | Android expert only writes the schedule; both iOS and Android clients read generated slots |
| Timezone choice and UTC auto-fill | Android client (decides to fill) | API / Backend (stores `timezone_id`) | Server returns `UTC` when no row; only the client knows the device zone |
| Call-in-progress guard | Android client (`CallGuard`) | — | Local state; no server call |
| Expert's profile expertise tags (editor tag source) | API / Backend (`GET /expert/profile`, subset rule on topic writes) | Android ViewModel (filter, cap 3) | Server rejects topic tags not on the profile (422) |
| Navigation of editor and Availability | Android UI (Nav3 per-tab back stacks) | — | Each tab owns its stack; Availability is pushed from two tabs |

## Standard Stack

### Core (all already in the project; no new dependencies)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose BOM / Material 3 | 2026.09.00 | UI: `ModalBottomSheet`, `PullToRefreshBox`, `DropdownMenu`, `Switch`, `FloatingActionButton`, `SnackbarHost` | Already used in Phases 3-9 [VERIFIED: gradle/libs.versions.toml] |
| Navigation 3 | 1.2.0 | Per-tab back stacks; `@Serializable data class ... : NavKey` | Existing `Navigator`/`HomeTabScaffold` [VERIFIED: libs.versions.toml, navigation/Navigator.kt] |
| Hilt (+ `hilt-lifecycle-viewmodel-compose`) | 2.60.1 / 1.4.0 | `@HiltViewModel` with `@AssistedInject` for key-carrying VMs | `MakeABookingViewModel` precedent (`creationCallback`) [VERIFIED: repo] |
| Retrofit + Moshi (codegen, no Kotlin reflection) | 3.0.0 / 1.15.2 | DTOs, `ClickMeApi` | Project architecture [VERIFIED: libs.versions.toml, MoshiProvider.kt] |
| kotlinx-coroutines | 1.11.0 | VM scopes, `delay` for the 2 s Saved hold | Project standard |
| JUnit 4.13.2 + Robolectric 4.17 + Compose ui-test-junit4 + coroutines-test | as listed | Unit and Robolectric Compose tests | Existing test stack [VERIFIED: app/build.gradle.kts] |

### Supporting (existing project components to reuse)
| Component | Location | Purpose | When to Use |
|-----------|----------|---------|-------------|
| `ValidatedField` (`trailingBadge`) | `components/ValidatedField.kt` | Title and Price fields | Price currency code goes in `trailingBadge` |
| `PickerField`, `PickerSheet`/`PickerRow` | `components/PickerField.kt`, `PickerSheet.kt` | Timezone row and sheet | Same as signup timezone step |
| `SelectableChip` | `components/SelectableChip.kt` | Tag chips | Has no `enabled` parameter today; add one (UI-SPEC) |
| `EmptyState`, `EmptyStateAction`, `SkeletonBlock` | `components/` | Empty, error, loading | Topics and Availability |
| `AuthTopAppBar` (`backEnabled`, `actions`) | `screens/login/AuthTopAppBar.kt` | Pushed screens | Editor and Availability |
| `AuthErrorDialog`, `toDialogError()` | `screens/login/` | Single-button error alerts | All failure alerts |
| `Money.format(Long, code)` | `util/Money.kt` | Card price, floor hint/error | Free renders as "Free" separately |
| `MinSessionAmount.minMinorUnits(currency)` | `model/responses/MeData.kt` | Floor lookup with fallback | Already implements `by_currency ?: default_minor_units` |
| `Timezones` (`all`, `detected()`, `entryFor()`, `pickerEntries()`) | `constants/Timezones.kt` | Timezone list and detection | EXP-04 |
| `rememberDeviceTimeFormat()` | `screens/clientmode/makeabooking/BookingFormatting.kt` | 12/24 h display | Day rows and picker |
| `CallGuard.attempt`, `isCallActive` | `managers/CallGuard.kt` | EditAvailability guard | Save only; silent save reads `isCallActive` |
| `PaymentMethodsViewModel` / `PaymentMethodsScreen` / `PaymentMethodRemoveDialog` | `screens/settings/paymentmethods/` | Template for one-operation-at-a-time spinner, overflow, confirm dialog | Topics list |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom null-writing Moshi adapter for `UpdateExpertTopicRequest` | Send `description = ""` and omit `icon_slug` | `icon_slug` is a strict enum (`""` is a 422) so clearing an icon needs a real `null`; do the adapter |
| Stateless repository + `TopicEditorKey` seed + one-shot event channel | Singleton topic cache | Nothing in the codebase clears singletons on logout, so a cache could show user A's topics to user B |
| `PickerSheet` for the timezone sheet | New sheet | Reuse is the UI-SPEC and D-14 |

**Installation:** none. No new packages are recommended.

**Version verification:** versions above were read from `gradle/libs.versions.toml` in this repo, not from a registry, because nothing new is installed.

## Package Legitimacy Audit

No external packages are installed by this phase, so slopcheck was not run and no registry lookups were needed.

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| (none) | — | — | — | — | n/a | n/a |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 Expert shell (HomeExpertScreen, per-tab Nav3 stacks)
 ┌────────────────────────────────────────────────────────────────────────┐
 │ Topics tab stack                       Profile tab stack               │
 │  TopicsRoot ──tap card/FAB──> TopicEditorKey(topicId?, seed?)          │
 │      │  └──app bar icon──> AvailabilityKey     ExpertProfileRoot       │
 │      │                                            └─row─> AvailabilityKey│
 └──────┼───────────────────────────────────────────────┼─────────────────┘
        │                                               │
 TopicsViewModel                 TopicEditorViewModel    │      AvailabilityViewModel
  load/refresh/delete             form state, price      │       hydrate / edit / save
        │                         parse + floor          │            │   ▲
        │   ◄── TopicSavedEvent ── (one-shot Channel) ───┘            │   │ callGuard.attempt (Save)
        ▼                               │                              │   │ callGuard.isCallActive (silent save)
 ExpertTopicsRepository ◄───────────────┘                              ▼   │
  list/get/create/patch/delete          ExpertiseEditorViewModel  AvailabilityRepository
  profileTags()                          (sheet; PATCH profile)    get()/put()
        │                                       │                        │
        └────────────── ClickMeApi (Retrofit/Moshi, apiCall{}) ──────────┘
                                   │
   GET/POST/PATCH/DELETE /expert/topics[/:id]   GET/PUT /expert/availability
   GET/PATCH /expert/profile                    GET /me (pricing_limits, in-memory via UserManager.me)
                                   │
                              Backend (Express + Supabase)
        zod validate -> merged-state checks -> assertMeetsMinimumPrice -> write
        availability_schedule (jsonb per weekday, timezone_id) ──> generateSlotsForMonth
                                   │
              GET /experts/:id/availability  (iOS and Android CLIENTS read this: SC-4)
```

Primary trace (SC-1): editor Save -> `TopicPrice.parse` -> local floor check (if `pricing_limits` known) -> `POST/PATCH /expert/topics` -> 422 `PRICE_BELOW_MINIMUM` returns to the price field, other errors to `AuthErrorDialog`, success sends `TopicSavedEvent` and pops.

### Recommended Project Structure
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── model/requests/ExpertTopicRequests.kt        # Create/Update topic, UpdateExpertProfileRequest (tags only), UpdateAvailabilityRequest
├── model/responses/ExpertTopicItem.kt           # ExpertTopicItem, MyExpertTopicsPayload(topics: List<ExpertTopicItem>), MyExpertTopicPayload
├── model/responses/ExpertAvailabilityScheduleData.kt  # timezone envelope + recurring_schedule (7 keys)
├── network/ApiRequestUrls.kt                    # + EXPERT_TOPIC = "expert/topics/{id}", EXPERT_MY_AVAILABILITY = "expert/availability"
├── network/NullPreservingAdapters.kt            # JsonAdapter that writes explicit null for icon_slug/description
├── managers/CallDialog.kt                       # + RestrictedAction.EditAvailability
├── screens/call/calling/CallDialogs.kt          # + when branch -> call_action_edit_availability
├── res/values/strings_call.xml                  # + call_action_edit_availability = "edit your availability"
├── res/values/strings_expert_topics.xml, strings_expert_availability.xml
└── screens/expertmode/
    ├── topics/            TopicsScreen, TopicsViewModel, TopicsUiState, ExpertTopicsRepository, TopicEvents, subviews/
    │   └── editor/        TopicEditorKey, TopicEditorScreen, TopicEditorViewModel, TopicPrice.kt, subviews/ (+ ExpertiseEditorSheet/VM)
    └── availability/      AvailabilityKey, AvailabilityScreen, AvailabilityViewModel, AvailabilityScheduleLogic.kt, subviews/
```
(`TopicsRoot` and `ExpertProfileRoot` stay in `screens/expertmode/home/subviews/HomeExpertTab.kt`.)

### Pattern 1: Keyed ViewModel via assisted injection (existing precedent)
**What:** `@HiltViewModel(assistedFactory = ...)` with `@AssistedInject constructor(@Assisted key: TopicEditorKey, SavedStateHandle, ...)`, created in the shell entry with `hiltViewModel(creationCallback = { f: Factory -> f.create(key) })`.
**When to use:** `TopicEditorViewModel`. `AvailabilityKey` is a `data object`, so a plain `hiltViewModel()` works, but because Availability is pushed on two different tab stacks, each stack entry gets its own VM (Nav3 decorated entries scope VMs per entry), which is desired.
**Example:**
```kotlin
// Source: app/src/main/java/.../screens/clientmode/makeabooking/MakeABookingViewModel.kt (existing)
@HiltViewModel(assistedFactory = TopicEditorViewModel.Factory::class)
class TopicEditorViewModel @AssistedInject constructor(
    @Assisted key: TopicEditorKey,
    private val savedStateHandle: SavedStateHandle,
    private val repository: ExpertTopicsRepository,
    private val userManager: UserManager,
    private val events: TopicEvents,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(key: TopicEditorKey): TopicEditorViewModel }
}
```

### Pattern 2: Topic seed travels in the nav key
**What:** `@Serializable data class TopicEditorKey(val topicId: String?, val seed: TopicEditorSeed? = null) : NavKey` where the seed holds title, description, durationMins, amountMinor, currency, isFree, iconSlug, and the tag id + label pairs. Same approach as `ExpertProfileKey` / `BookingKey`, and it survives process death.
**Why:** UI-SPEC "Open editor: seeded from the list item (no fetch)". If a seed is ever missing for a non-null id (should not happen), fall back to `GET /expert/topics/{id}`, which exists server-side.

### Pattern 3: One-shot "topic saved" event, not a nav result
**What:** a `@Singleton class TopicEvents { val channel = Channel<TopicSavedEvent>(Channel.BUFFERED) }` carrying only `{added|updated, ExpertTopicItem}`. `TopicsViewModel` collects it, merges the item in place (replace on edit, append on create, matching iOS), emits the snackbar message, and also does a quiet reload.
**Why:** UI-SPEC says the pending message travels as a one-shot field on the screen's state holder. The event holds no user identity beyond a topic the signed-in expert just saved, and is consumed immediately by the live Topics VM (it stays alive under the pushed editor).

### Pattern 4: Pure logic objects, tested without Android
**What:** `AvailabilityScheduleLogic` (parse `HH:mm`, hydrate a `DaySchedule` list, `overlappingDays`, `addHours`, `toRequest`) and `TopicPrice` (`parse(text, currency): ParseResult`, `formatForField(minor, currency)`, `floorFor(limits, currency)`). Both are plain Kotlin with no Android types, so JVM unit tests cover most EXP-02/03/04 behaviour.

### Pattern 5: Silent UTC auto-save (EXP-04)
**What:** After `hydrate` succeeds, if `pendingAutoDetectSave` was set, call `repository.put(buildRequest())` inside `runCatching` only when: not `isSaving`, no overlaps, and `!callGuard.isCallActive.value`. Never touch `saveState`/dialog. Set `didAutoDetect = true` at the first hydrate regardless of outcome (VM field, survives rotation, resets with a new screen session).

### Anti-Patterns to Avoid
- **Calling `callGuard.attempt()` from the silent save.** It queues the "Can't do that during a call" dialog. Read `isCallActive.value` instead (D-03, D-15).
- **A singleton topic cache.** No logout clearing exists for singletons.
- **Sending `description = null` / `iconSlug = null` through the default Moshi adapter on PATCH.** The keys are omitted and the server keeps the old value (see Pitfall 1).
- **`LocalTime.of(minutes / 60, ...)` for formatting.** `LocalTime.of(24, 0)` throws.
- **Using `UserManager.me` snapshot at construction time.** Read `me.value` at validation time; the editor refreshes `/me` once if null (D-12).
- **Persisting `pricing_limits`.** `MeData.kt` forbids it.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Currency symbol, fraction digits | Own tables | `java.util.Currency.defaultFractionDigits` via `Money.kt` | JPY 0, others 2; already the shared formatter |
| Floor lookup with fallback | New lookup | `MinSessionAmount.minMinorUnits(currency)` | Implements PRICING-LIMITS §2 exactly |
| Timezone list, detection | New list/detector | `Timezones.all/detected()/entryFor()/pickerEntries()` | Already handles offset matching and device region ids |
| Picker bottom sheet with check row | New sheet | `PickerSheet` + `PickerRow` | Same as signup timezone step (D-14) |
| Error envelope parsing | Own parsing | `ApiErrorParser` / `ApiException.Http.code == ErrorCode.PriceBelowMinimum` + `userMessage` | Already maps `message` and `errors[]` |
| Time-of-day display with 12/24 h | `String.format` | `rememberDeviceTimeFormat()` on a UTC-pinned clone | Device preference; DST-safe (Pitfall 8) |
| Pull to refresh, snackbar | Custom | `PullToRefreshBox`, `SnackbarHost` as in `MyBookingsScreen` | Established |
| Tag picker body | Re-write | Extract the list/search/selected body from `SignupTagsScreen` into a shared composable (A15) | The VM is not reusable (bound to `SignupSetupRepository`), the composable body is |

**Key insight:** the risky parts are not UI. They are wire-format subtleties (null vs omitted, `"24:00"`, empty currency, locale decimal separators). Put those in pure, unit-tested code.

## Runtime State Inventory

Not applicable. This is a feature phase, not a rename/refactor/migration. No stored data, OS registrations, secrets or build artifacts carry a renamed identifier. (One related note: `MyExpertTopicsPayload` changes its element type, which touches `UserManager.expertHasTopics` and 8 test call sites; see Wave 0 Gaps.)

## Findings (backend and integration facts the plan depends on)

### Finding 1: `/expert/availability` contract [VERIFIED: backend-reference/src/controllers/availability.controller.js, schemas/availability.schema.js, services/availability.js]
- `GET`: no row yet -> `timezone.id = "UTC"` and all seven days `[]`. Response `{timezone:{id, display_name, utc_offset(hours, float)}, recurring_schedule:{monday..sunday:[{start_time,end_time}]}}`. `display_name` is `"(GMT-05:00) America/Toronto"` style, built server-side.
- `PUT`: body `{timezone_id?, schedule?}`, `.strict()`; each day is `[{start_time, end_time}]`, `HH:mm` matched by `^\d{2}:\d{2}$` only. Server rejects (422 `VALIDATION_ERROR`, `errors[]`): `end <= start` (string compare), minutes not on a 15-minute boundary, overlapping slots within a day (`prev.end > next.start`, so touching slots such as 09:00-10:00 and 10:00-11:00 are allowed), invalid IANA id. Response `{status, message:"Availability updated successfully.", data:{updated_days}}` (map to `SuccessMessageResponse`).
- Android overlap rule must match: `sorted[i].start < sorted[i-1].end` (strict), so back-to-back slots are valid on both sides. Note the iOS check compares each slot only with the immediately preceding sorted slot; use a running max of ends to be safe (a long slot containing two short ones), it stays consistent with the server for all realistic inputs.
- A PUT with every day present overwrites those days; a day sent as `[]` is stored as `[]`.
- `ValidationError` has `message: "Validation failed"` plus `errors[]`; `ApiException.userMessage` returns `serverMessage ?: firstFieldError`, so for field-envelope 422s the user would see "Validation failed". Prefer `fieldErrors.firstOrNull()?.message ?: serverMessage` for availability and topic save failures.

### Finding 2: Price floor and error shape [VERIFIED: backend utils/pricing.js, expertTopics.controller.js, PRICING-LIMITS.md]
- Paid topics: `assertMeetsMinimumPrice(amount, currency)` runs on the merged state before any write. 422, `code: "PRICE_BELOW_MINIMUM"`, `message: "A paid session must be at least CA$5."` (server symbol table: `CA$`, `A$`, `$`, `¥`...). Standard envelope `{status, code, message}` so `ApiException.Http.code == ErrorCode.PriceBelowMinimum` and `serverMessage` are enough. `ErrorCode.PriceBelowMinimum` already exists.
- Free topics are exempt. Floors today: 500 for all except JPY 800; a global kill switch can set every floor to 0.
- Server message uses `CA$`, while the client message (`Money.format`) renders by device locale (for example `$5.00`). The two differ cosmetically; that is expected. Show the server text verbatim when it arrives.

### Finding 3: Topics API shapes [VERIFIED: backend expertTopics.schema.js and controller]
- Create body: `{title(1..200), description?(<=2000, nullable), duration_mins(int, >=30, <=1440, %30==0), is_free(bool, required), hourly_rate?{amount>=0 int, currency 3 letters}, icon_slug?(enum, nullable), expertise_tag_ids?(uuid[] max 3)}`, `.strict()`.
- Patch body: all optional. `is_free:true` forces `price_amount = 0`. Currency is only written when `hourly_rate` is present and the result is **not** free. Tag ids: omitted = untouched, `[]` = clear, array = replace.
- The server now accepts `hourly_rate.amount = 0` (quick task 260826-iqv). The CONTEXT/iOS premise "server requires amount > 0 for free" is stale on this backend version. D-11 (send the floor, or 100) is still safe and still the locked decision; the deployed server may be older.
- Responses: `data.topic` (create 201, patch 200) and `data.topics` (list) use `serializeTopic`: `{id, title, description|null, duration_mins|null, icon_slug|null, price:{amount, currency, is_free, label}, expertise_tags:[{id,label,category_id}]}`. **There is no `hourly_rate` key** in responses. `price.currency` is `""` when the stored currency is null (a free topic created without `hourly_rate`). `price.label` is `"Free"` or `"$250 / session"`.
- Editing a free topic: `price.amount` is 0, `is_free` true. Seed the field blank (UI-SPEC). The stored currency may be `""`, so currency resolution for an existing topic must be `hourly_rate?.currency?.takeIf { it.isNotBlank() } ?: price.currency.takeIf { it.isNotBlank() } ?: defaultCurrency`.
- Tag ids must be a subset of the caller's profile tags, else `422 VALIDATION_ERROR` "One or more expertise tags are not in your profile...". Ids are compared as stored lowercase strings; `ExpertiseTagItem.id` from the API is already lowercase, so just never uppercase it.
- `discussion_topics.price_amount` is a Postgres `INTEGER` (max 2,147,483,647). The zod schema has no upper bound, so an absurd amount becomes a 500. Cap input client-side (Pitfall 4).

### Finding 4: Delete never returns 409 [VERIFIED: expertTopics.controller.js `softDeleteTopic`; no 409 in controller or tests]
`DELETE /expert/topics/:id` sets `is_active=false`, returns `200 {status, message:"Topic archived"}`, or `404 NOT_FOUND`. There is no active-booking check. D-06's "409 shows the server message" path will not trigger today (the iOS comment is stale). Keep the generic failure alert (server message, else generic copy); do not write a 409-specific branch. Add to the iOS follow-up list. The delete confirmation copy "clients won't be able to book it" is accurate (topic hidden); existing bookings are unaffected.

### Finding 5: `PATCH /expert/profile` for tags [VERIFIED: expertProfile.schema.js, expert.controller.js]
Body `{expertise_tags:[{tag_id:string, is_primary:boolean}]}` is a **full replace** of the expert's tags (delete then insert). Response `data:{profile_id, updated_fields}` only, so the new tag list is not returned: hand the saved selection (id + label) back to the editor and, optionally, re-`GET /expert/profile` quietly. The backend has no tag-count cap on this path; iOS's sheet requires at least one tag ("Pick at least one expertise tag before saving.") which UI-SPEC does not list: keep the iOS guard (Save Expertise disabled or inline message when the selection is empty) to avoid wiping the profile's tags by accident. First selected tag is primary (iOS preserves the previous primary unless removed; Android signup uses tap order, so use: keep existing primary if still selected, else first).
`ExpertProfileData.expertiseTags` (`ExpertTag(id,label,isPrimary)`) is the editor's tag source: `getExpertProfile()` already exists. There is **no** `expertProfile` field on `UserManager` (iOS has one), so use a repository call (`ExpertTopicsRepository.profileTags()`) rather than adding global state.

### Finding 6: `/me` and `pricing_limits` [VERIFIED: MeData.kt, UserManager.kt, PRICING-LIMITS.md]
`UserManager.me: StateFlow<MeData?>` is in-memory only; `refreshMe()` throws `ApiException`. `MinSessionAmount.minMinorUnits(code)` uppercases and falls back. Default currency: `me.value?.pricingLimits?.minSessionAmount?.defaultCurrency ?: "CAD"`. For D-12: at editor open, if `me.value?.pricingLimits == null`, `runCatching { userManager.refreshMe() }` once, in the background so the form is usable immediately.

### Finding 7: Research item "24:00" / "23:59" [VERIFIED: ran backend-reference code locally 2026-10-24; the deployed prod version was not checked]
- `"23:59"`: rejected (`isAlignedTo15Min("23:59") == false`, 422 "must align to a 15-minute boundary").
- `"24:00"`: accepted by the zod regex, passes alignment and end>start, and `generateSlotsForMonth` handles it correctly (Luxon `set({hour:24})` rolls to 00:00 of the next day). A Monday range `22:00-24:00` with a 60 min topic produced slots 22:00, 22:30, 23:00 local (the last ends 12:00 AM). So midnight end is safe server-side.
- iOS can already produce it: its "Add hours" computes `min(newStart+60, 24*60)` and `formatHHMM(1440) = "24:00"`, so existing experts may have `24:00` ends stored, and iOS `minutesToString(1440)` shows "24:00" (its `minutesToAMPM(1440)` shows "12:00 PM", an iOS display bug).
- **Recommendation:** keep the End picker at the 23:30 cap (CONTEXT/UI-SPEC default, no new scope) **but make the Android model, hydrate and request builder lossless for 1440**: allow `0..1440`, format `1440 -> "24:00"` on the wire, and when a slot's current start or end is not in the 30-minute grid or exceeds the cap (for example 1440, or a `:15` value written by another client), inject that current value into the corresponding picker list so Done without changes preserves it. Offering "12:00 AM (midnight)" as a selectable End is a one-row extension the user can approve later (open question 1).
- "Add hours" must clamp to the grid: `newStart = min(maxEnd + 30, 23*60)`, `newEnd = min(newStart + 60, 23*60 + 30)` (UI-SPEC A9), never 1440.

### Finding 8: Session buffer and success criterion 4 [VERIFIED: SESSION-BUFFER.md, services/availability.js]
No client change. The buffer blocks other bookings and does not shrink the window; slot cells step every 30 minutes from each range's start for the topic duration. The Android expert only writes the schedule; `GET /experts/:id/availability` (already used by Phase 6) generates slots for iOS and Android clients alike, in the expert's saved timezone. Practical consequences for the plan: (1) saving a timezone is what makes booking times correct for clients, so the UTC auto-save matters; (2) a topic is only discoverable once active (discovery SQL requires an active topic, migrations `20260718010000`, `20260724000000`), which matches EXP-01's copy.

### Finding 9: `Timezones.kt` behaviour [VERIFIED: constants/Timezones.kt]
`all` has 10 bundled entries (including `UTC`). `detected()` returns a bundled entry, else the device's own region id with a computed label, else an offset match, else UTC. `entryFor(id)` returns a bundled entry, else a runtime label (for any Java-known zone), else `(id, id)`. **Conflict with D-13:** D-13 says "show the server's `display_name`" for unlisted zones, but `entryFor()` returns a runtime label for those. Resolve it as: `Timezones.all.firstOrNull { it.id == serverId }?.label ?: server display_name`. That matches iOS (`Timezones.entry(forId:)` is bundled-only) and the UI-SPEC. Use `entryFor()` only to build the `PickerSheet` extra row for a zone that is not bundled. UTC test: `id.lowercase() in setOf("utc", "etc/utc")`.

### Finding 10: Existing Android surfaces the phase must touch [VERIFIED: repo]
- `RestrictedAction` (managers/CallDialog.kt) has 4 entries; `restrictedActionText` in `screens/call/calling/CallDialogs.kt` is an exhaustive `when`, so adding the enum entry breaks compilation until the string branch is added (good: compiler-enforced). `FakeCallGuard` needs no change.
- `HomeExpertScreen.kt`: `entry<TopicsRoot> { ExpertTabRoot(TopicsRoot) }` placeholder to replace; `ExpertProfileRoot` footer gets the `ProfileMenuRow`. Pushed entries use `metadata = BottomBar.hidden()`.
- `ClickMeApi.getMyTopics()` returns `MyExpertTopicsPayload(topics: List<Map<String, Any?>>)`, used by `UserManager.expertHasTopics()` (`topics.isNotEmpty()`). Changing the element type to `ExpertTopicItem` keeps that call valid, but `AppViewModelTest` (3 sites) and `UserManagerTest` (5 sites) construct `MyExpertTopicsPayload(listOf(mapOf("id" to "t1")))` and must be updated.
- `FakeClickMeApi implements ClickMeApi`: every new Retrofit method needs a handler there or the test source set stops compiling.
- `ApiRequestUrls.EXPERT_AVAILABILITY = "experts/{id}/availability"` is the public client route. Add a distinct `EXPERT_MY_AVAILABILITY = "expert/availability"` (note the singular/plural trap) and `EXPERT_TOPIC = "expert/topics/{id}"`.
- No existing request class writes explicit JSON nulls; `SignupRequests.kt` documents that omission is intentional there.
- Design gate `scripts/check-design-rules.sh`: no `Color(0x...)` outside `ui/theme/Color.kt`, no `*Brand` types.

## Common Pitfalls

### Pitfall 1: Clearing the icon or description on PATCH silently does nothing
**What goes wrong:** Expert taps the selected icon to clear it, or empties the description, saves, and the old value returns. Moshi (no `serializeNulls`) drops null fields, and the server treats a missing key as "unchanged". iOS has the same latent bug (Swift `Encodable` omits nil).
**Why:** `icon_slug` is `z.enum(...).nullable().optional()`, so `""` is a 422; only literal `null` clears it. `description` accepts `""` but then stores `""` (clients may render an empty block).
**How to avoid:** write the PATCH body with a small hand-written `JsonAdapter<UpdateExpertTopicRequest>` (registered in `MoshiProvider`, takes precedence over codegen) that sets `writer.serializeNulls = true` for `icon_slug` and `description` and omits genuinely unset fields (`title` etc. are always sent from the editor). Simplest: always send a full body on edit (title, description-or-null, duration, is_free, hourly_rate, icon_slug-or-null, expertise_tag_ids). Add a MockWebServer test asserting `"icon_slug":null` appears in the request body when cleared.
**Warning signs:** a unit test that serializes the DTO shows no `icon_slug` key.

### Pitfall 2: Currency is blank or missing on an existing topic
**What goes wrong:** Editing a free topic created without `hourly_rate` yields `price.currency == ""`; sending `currency:""` fails zod (`length(3)`).
**How to avoid:** the resolution chain in Finding 3; trim and uppercase; validate length 3 before sending, else use the default.

### Pitfall 3: Locale decimal separator
**What goes wrong:** `KeyboardType.Decimal` shows a comma on many locales (French Canada is a real CAD audience); filtering to digits and "." drops the comma and the user cannot type decimals, or `"12,50".toBigDecimal()` throws.
**How to avoid:** accept both `.` and `,` as the separator in the filter, normalize to `.` before `BigDecimal`, allow at most one separator and at most `fractionDigits` digits after it; for 0-digit currencies accept digits only. Seed the field for edit with `BigDecimal.movePointLeft(digits).toPlainString()` (always `.`). [ASSUMED: exact keyboard behaviour per OEM/locale; verify on the Samsung device with a French locale.]

### Pitfall 4: Integer overflow and rounding
**What goes wrong:** `"99999999999"` becomes a Postgres INTEGER overflow 500; floating-point parsing rounds `12.35` wrongly.
**How to avoid:** parse with `BigDecimal` (never `Double`), `setScale` check, `movePointRight(digits)`, `longValueExact()`, and cap length (for example max 7 integer digits so minor units stay below about 10^9). Over-cap is "invalid" (required-field copy).

### Pitfall 5: Off-grid or out-of-cap times from other clients
**What goes wrong:** the server accepts any 15-minute value and `24:00`. Hydrating `09:15` or `24:00` and opening the picker finds no selected row (iOS has the same flaw), or Done silently changes the value.
**How to avoid:** the picker list always includes the draft's current start and end values (Finding 7); hydrate keeps them as-is; the model range is `0..1440`.

### Pitfall 6: `LocalTime.of(24, 0)` and wire formatting
**What goes wrong:** crash when formatting a 1440 end for display.
**How to avoid:** `fun minutesToHhmm(m: Int) = "%02d:%02d".format(Locale.ROOT, m / 60, m % 60)` for the wire (`1440 -> "24:00"`); for display, format via a UTC-pinned `DateFormat` over `Date(m * 60_000L)` (1440 becomes next-day 00:00, rendered "12:00 AM" or "00:00"). Use `Locale.ROOT` for the wire string so Arabic-Indic digits never leak into the request.

### Pitfall 7: Guard misuse around the silent save
**What goes wrong:** calling `callGuard.attempt()` from the auto-save shows the "End the call to edit your availability" dialog when the expert merely opened the screen during a call.
**How to avoid:** `if (callGuard.isCallActive.value) skip`. Only the explicit Save button calls `attempt(EditAvailability)`, and it must run before any state change (spinner, request), like `PaymentMethodsViewModel.addCard`.

### Pitfall 8: DST-gap labels
**What goes wrong:** building a `Calendar` in the device zone for 02:30 on a spring-forward day shows 03:30.
**How to avoid:** format minutes through a `DateFormat` clone with `timeZone = UTC` on `Date(minutes * 60_000L)`.

### Pitfall 9: Nav3 saved state and double-fire on resume
**What goes wrong:** returning to Topics from the editor triggers both `LaunchedEffect(Unit)` and `ON_RESUME` (Phase 7 A10 saw this in Robolectric), causing a double reload; the list flickers to skeleton.
**How to avoid:** follow `MyBookingsScreen` (`onScreenShown()` guarded by an in-flight flag); only the first load shows the skeleton; later loads are quiet. The saved event merges the item immediately, so a late reload is invisible.

### Pitfall 10: Process death on Availability
**What goes wrong:** UI-SPEC wants edits to survive rotation and process death; `List<DaySchedule>` is not directly `SavedStateHandle`-friendly.
**How to avoid:** keep the working copy in the VM `StateFlow` and mirror to `SavedStateHandle` as a compact string (for example `"540-1020,1080-1200;;..."`, seven `;`-separated days, plus a 7-bit enabled mask and the timezone id). On creation, if the saved string exists, restore it and mark `hydratedFromServer = false` until the first successful load decides: after process death prefer the restored edits and skip overwriting them on the first load (set `didAutoDetect` appropriately). Pull-to-refresh overwrites (iOS parity). Keep this logic in `AvailabilityScheduleLogic` (encode/decode) so it is JVM-testable.

### Pitfall 11: Deleting while another operation runs
**What goes wrong:** two taps delete two cards at once. **How to avoid:** `busy` operation in state (Phase 7 `PaymentMethodsViewModel` pattern); every overflow disabled while busy.

### Pitfall 12: Tag-picker cap with a `SelectableChip` that cannot be disabled
**How to avoid:** add `enabled: Boolean = true` to `SelectableChip` (38% content alpha, no click, `stateDescription` "Limit reached"); default keeps all existing callers unchanged.

## Code Examples

### Hydrate and overlap (pure logic)
```kotlin
// Ports iOS AvailabilitySettingsViewModel.hydrate/overlappingDayNames. No Android types.
data class Slot(val start: Int, val end: Int)           // minutes from midnight, 0..1440
data class Day(val name: DayOfWeek, val enabled: Boolean, val slots: List<Slot>)

fun parseHhmm(s: String): Int? {
    val p = s.split(":")
    if (p.size != 2) return null
    val h = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    return if (h in 0..24 && m in 0..59) h * 60 + m else null   // 24:00 allowed, see Finding 7
}

fun hydrateSlots(api: List<TimeSlotDto>): List<Slot> =
    api.mapNotNull { dto ->
        val s = parseHhmm(dto.startTime) ?: return@mapNotNull null
        val e = parseHhmm(dto.endTime) ?: return@mapNotNull null
        if (e > s) Slot(s, e) else null
    }

fun overlappingDays(days: List<Day>): List<DayOfWeek> =
    days.filter { d ->
        d.enabled && d.slots.size > 1 && d.slots.sortedBy { it.start }.let { sorted ->
            var maxEnd = sorted.first().end
            sorted.drop(1).any { s -> (s.start < maxEnd).also { maxEnd = maxOf(maxEnd, s.end) } }
        }
    }.map { it.name }

fun minutesToHhmm(m: Int) = String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60)
```

### Price parsing (D-09, D-11)
```kotlin
sealed interface PriceParse { data class Ok(val minor: Long) : PriceParse; data object Invalid : PriceParse }

fun parsePrice(text: String, currencyCode: String): PriceParse {
    val digits = runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }.getOrDefault(2).coerceAtLeast(0)
    val t = text.trim().replace(',', '.')
    if (t.isEmpty()) return PriceParse.Ok(0)                      // blank = free
    val bd = t.toBigDecimalOrNull() ?: return PriceParse.Invalid
    if (bd.signum() < 0 || bd.scale() > digits || bd.precision() - bd.scale() > 7) return PriceParse.Invalid
    return PriceParse.Ok(bd.movePointRight(digits).longValueExact())
}

// wire amount for a free topic (D-11)
fun wireAmount(minor: Long, floor: Int?): Long = if (minor == 0L) (floor ?: 100).toLong() else minor
```

### Floor check and server error on the price field
```kotlin
val floor: Int? = userManager.me.value?.pricingLimits?.minSessionAmount?.minMinorUnits(currency)
if (minor > 0 && floor != null && minor < floor) priceError = PriceError.BelowFloor(floor)

catch (e: ApiException.Http) {
    if (e.code == ErrorCode.PriceBelowMinimum) priceServerError = e.serverMessage   // clears on next keystroke
    else dialog = e.toDialogError()
}
```

### Explicit-null PATCH adapter (Pitfall 1)
```kotlin
// Source: Moshi JsonAdapter API (JsonWriter.serializeNulls). Registered with Moshi.Builder().add(...) in MoshiProvider.
class UpdateExpertTopicRequestAdapter : JsonAdapter<UpdateExpertTopicRequest>() {
    override fun toJson(w: JsonWriter, v: UpdateExpertTopicRequest?) {
        if (v == null) { w.nullValue(); return }
        val prev = w.serializeNulls
        w.beginObject()
        w.name("title").value(v.title)
        w.serializeNulls = true                 // explicit null clears these two on the server
        w.name("description").value(v.description)
        w.name("icon_slug").value(v.iconSlug)
        w.serializeNulls = prev
        w.name("duration_mins").value(v.durationMins.toLong())
        w.name("is_free").value(v.isFree)
        w.name("hourly_rate").beginObject().name("amount").value(v.amount).name("currency").value(v.currency).endObject()
        w.name("expertise_tag_ids").beginArray().apply { v.tagIds.forEach { value(it) } }.endArray()
        w.endObject()
    }
    override fun fromJson(r: JsonReader): UpdateExpertTopicRequest = throw UnsupportedOperationException()
}
```
(The `fromJson` is never used; a request-only adapter is fine.)

### Guarded Save and silent auto-save
```kotlin
fun save() {
    if (!callGuard.attempt(RestrictedAction.EditAvailability)) return   // dialog queued, nothing sent
    val s = _uiState.value
    if (s.saveState == SaveState.Saving) return
    val overlaps = overlappingDays(s.days)
    if (overlaps.isNotEmpty()) { /* banner already visible; Save is disabled in UI */ return }
    // ... Saving -> PUT -> Saved -> delay(AVAILABILITY_SAVED_HOLD_MS) -> Idle
}

private suspend fun silentSaveIfNeeded() {
    if (!pendingAutoDetectSave) return
    pendingAutoDetectSave = false
    if (_uiState.value.saveState == SaveState.Saving) return
    if (overlappingDays(_uiState.value.days).isNotEmpty()) return
    if (callGuard.isCallActive.value) return          // NOT attempt(): no dialog
    runCatching { repository.put(buildRequest()) }    // swallow everything except cancellation
        .onFailure { if (it is CancellationException) throw it }
}
```

### Entry wiring (HomeExpertShell)
```kotlin
entry<TopicsRoot> { TopicsScreen(onAddTopic = { navigator.navigate(TopicEditorKey(null)) },
                                 onEditTopic = { navigator.navigate(it) },
                                 onOpenAvailability = { navigator.navigate(AvailabilityKey) }, /* avatar action etc. */) }
entry<TopicEditorKey>(metadata = BottomBar.hidden()) { key ->
    TopicEditorScreen(viewModel = hiltViewModel<TopicEditorViewModel, TopicEditorViewModel.Factory>(
        creationCallback = { it.create(key) }), onBack = navigator::goBack)
}
entry<AvailabilityKey>(metadata = BottomBar.hidden()) { AvailabilityScreen(onBack = navigator::goBack) }
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Free topic requires `hourly_rate.amount > 0` | Server accepts amount 0 with `is_free:true` | Backend quick task 260826-iqv | D-11's floor placeholder is no longer required but remains safe |
| Topic response with `hourly_rate` | `price` only | Backend TOPIC-02 | Android DTO must treat `hourly_rate` as optional and read `price` |
| iOS "delete returns 409 on active bookings" | Soft delete, no check | current backend | 409 branch is dead code today |
| Topic tag source = full catalog | Subset of the expert's profile tags (422 otherwise) | Backend quick task 260725-mv0 | Editor lists profile tags only (already locked) |
| Session buffer absent | 30-min trailing buffer enforced server-side | quick 260811-u4j | No client change |

**Deprecated/outdated:**
- iOS comment on `deleteMyTopic` ("Returns 409 if the topic has active bookings"): not implemented server-side.
- `EncryptedSharedPreferences` is deprecated upstream, irrelevant here (locked project decision).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The production backend matches `backend-reference/src` (24:00 accepted, no 409 on delete, amount 0 accepted) | Findings 1, 4, 7 | A stricter/older prod would 422 a `24:00` hydrated value or free-topic amount 0. Mitigation: Android only emits `24:00` if it was hydrated; D-11 still sends a positive amount; generic error alerts cover the rest |
| A2 | `KeyboardType.Decimal` yields a comma separator on some device locales | Pitfall 3 | If never true, accepting both separators is harmless |
| A3 | A request-only Moshi `JsonAdapter` registered via `Moshi.Builder().add(...)` overrides the generated adapter without extra R8 config | Pitfall 1 | If shadowed incorrectly the PATCH omits nulls; covered by the proposed MockWebServer test and the R8 `assembleProdRelease` gate |
| A4 | Nav3 1.2.0 scopes a `hiltViewModel` per stack entry so Availability pushed on two stacks gets two VMs | Pattern 1 | Shared VM would share unsaved edits across tabs; low impact |
| A5 | Keeping the iOS "at least one tag" guard on the Expertise sheet is desired (UI-SPEC does not mention it) | Finding 5 | Without it an expert could wipe all profile tags; confirm with the user |
| A6 | Offering midnight (12:00 AM) as a selectable End time is wanted | Finding 7, Open Question 1 | Purely a scope call; default keeps 23:30 |

## Open Questions

1. **Allow a midnight End in the picker?**
   - Known: server accepts `24:00` and slots are correct; iOS can already store it; Android must round-trip it.
   - Unclear: whether the user wants it selectable now. UI-SPEC A10 says "extend the End list by one row and nothing else changes" if safe.
   - Recommendation: ship with the 23:30 cap plus lossless round-trip; treat the one-row extension as a follow-up unless the user says otherwise.
2. **"At least one tag" rule on the Expertise sheet (A5).**
   - Recommendation: keep the iOS rule (disable Save Expertise when empty).
3. **Wire currency for a free topic edited from an old record with a blank currency.**
   - Recommendation: send the default currency (Finding 3 chain); this also repairs the stored currency only when the topic is paid, per server behaviour.
4. **Where the Availability "Saved" snackbar vs check lives** is decided by UI-SPEC (check in app bar); no further input needed.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 (Android Studio JBR) | Gradle build/test | ✓ (per project memory) | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` | — |
| Samsung SM-A075F / `Medium_Phone_API_37.0` | Manual device checks (deferred by user) | ✓ per memory | API 36 / 37 | Manual items logged as open (project preference) |
| Backend (production Railway) | Live checks incl. SC-4 | ✓ (single shared backend) | — | Unit and MockWebServer tests cover the contract |
| iOS app as the second client for SC-4 | Success criterion 4 (cross-client) | Manual | — | Deferred human check; backend `generateSlotsForMonth` verified locally |
| New external tools or services | — | none required | — | — |

**Missing dependencies with no fallback:** none for automated work.
**Missing dependencies with fallback:** device and iOS cross-client checks are manual and deferred (project preference); record them as open items.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2, Robolectric 4.17, Compose `ui-test-junit4`, `kotlinx-coroutines-test` 1.11.0, `mockwebserver3` (all existing) |
| Config file | `app/build.gradle.kts` (no separate config); shared rules in `app/src/test/.../testing/` (`MainDispatcherRule`, `FakeClickMeApi`, `FakeCallGuard`) |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests "com.uptrendinvestments.clickmeapp.screens.expertmode.*"` |
| Full suite command | `JAVA_HOME=... ./gradlew testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug assembleProdDebug && bash scripts/check-design-rules.sh` (lint and R8 `assembleProdRelease` in a separate invocation after `./gradlew --stop`, per project memory; never `--stop` while parallel executors run) |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EXP-01 | Topics list load/empty/error/refresh states; delete busy + failure dialog; saved event merges in place; DTO decodes real server shape (`price` only, `currency:""`) | unit (VM) + DTO decode | `... --tests "*TopicsViewModelTest" --tests "*ExpertTopicDtoTest"` | ❌ Wave 0 |
| EXP-01 | Topics screen renders states, FAB only when topics exist, overflow Edit/Delete, delete confirm | Robolectric Compose | `... --tests "*TopicsScreenTest"` | ❌ Wave 0 |
| EXP-01 | Editor required-title error, tag cap 3, tag drop after profile tag change, seed from key | unit (VM) | `... --tests "*TopicEditorViewModelTest"` | ❌ Wave 0 |
| EXP-02 | Price parse (blank, 0, decimals, comma, JPY, negative, overflow), floor lookup/fallback, free wire amount (floor/100), live and on-Save floor error | pure unit | `... --tests "*TopicPriceTest"` | ❌ Wave 0 |
| EXP-02 | 422 `PRICE_BELOW_MINIMUM` lands on the price field and keeps the editor open; other errors -> dialog; `/me` refreshed once when limits null | unit (VM, FakeClickMeApi httpError) | `... --tests "*TopicEditorViewModelTest"` | ❌ Wave 0 |
| EXP-02 | PATCH body contains explicit `"icon_slug":null` / `"description":null` when cleared; create body has `is_free` and no extra keys | MockWebServer / adapter unit | `... --tests "*ExpertTopicRequestJsonTest"` | ❌ Wave 0 |
| EXP-03 | Hydrate (drop malformed/end<=start, enabled iff slot), default schedule, overlap detection (touching allowed), add-hours clamp, sorted `HH:mm` body with 7 days, `24:00` round-trip | pure unit | `... --tests "*AvailabilityScheduleLogicTest"` | ❌ Wave 0 |
| EXP-03 | Time-range sheet: start < end filtering, current off-grid value injected, Done writes / Cancel discards | Robolectric Compose | `... --tests "*TimeRangePickerSheetTest"` | ❌ Wave 0 |
| EXP-03 | Save disabled with overlap; Save runs guard first (blocked -> no PUT, `attempts == [EditAvailability]`); saving -> saved -> idle after 2000 ms; failure dialog uses field-error message | unit (VM + FakeCallGuard + virtual time) | `... --tests "*AvailabilityViewModelTest"` | ❌ Wave 0 |
| EXP-04 | UTC/Etc/UTC (case-insensitive) with non-UTC device fills detected zone and sends one silent PUT after hydrate; none when device is UTC, saving, overlap, or `isCallActive`; once per VM; failure swallowed; silent path never calls `attempt()` | unit (VM) | `... --tests "*AvailabilityViewModelTest"` | ❌ Wave 0 |
| EXP-04 | Unbundled server zone shows `display_name`; picker sheet extra row; Auto-detect changes local state only | unit + Robolectric | `... --tests "*AvailabilityViewModelTest" --tests "*TimezonePickerSheetTest"` | ❌ Wave 0 |
| D-13 hand-off | `restrictedActionText(EditAvailability)` resolves; guard dialog text "End the call to edit your availability." | Robolectric / string test | `... --tests "*CallDialogsTest"` (extend existing if present) | partial |
| Regression | `UserManager.expertHasTopics` still works with typed payload | unit | `... --tests "*UserManagerTest" --tests "*AppViewModelTest"` | ✅ (update fixtures) |
| SC-4 | iOS client sees topic and matching slots | manual cross-client | none (deferred human check, logged as open item) | manual |

### Sampling Rate
- **Per task commit:** the task's `--tests` filter plus `./gradlew ktlintCheck` (as in Phase 9).
- **Per wave merge:** `testDevDebugUnitTest ktlintCheck assembleDevDebug` + `bash scripts/check-design-rules.sh`.
- **Phase gate:** full suite green (both flavors), separate lint + R8 `assembleProdRelease` invocation, before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `FakeClickMeApi`: handlers for `getMyTopics` (typed), `getMyTopic`, `createTopic`, `patchTopic`, `deleteTopic`, `getMyAvailability`, `putMyAvailability`, `patchExpertProfile` (otherwise the test source set does not compile once `ClickMeApi` grows).
- [ ] Update the 8 `MyExpertTopicsPayload(listOf(mapOf("id" to "t1")))` fixtures in `AppViewModelTest` and `UserManagerTest` to the typed item (add a `topicItem()` test helper).
- [ ] New test files listed in the map above, plus shared helpers (`topicItem()`, `availabilityData()`, fake `ExpertTopicsRepository` if VM tests prefer a repository fake over `FakeClickMeApi`).
- [ ] `SelectableChip(enabled=)` addition covered by an existing/new component test.
- Framework install: none.

## Security Domain

`security_enforcement` is not disabled in `.planning/config.json` (the key is absent), so this section applies.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (existing session/JWT, unchanged) | Existing `TokenAuthenticator`; all new endpoints are authed by default (not in `publicPaths`) |
| V3 Session Management | no | Unchanged |
| V4 Access Control | yes (server-enforced) | Backend scopes every topic/availability read and write by `req.user.id` (IDOR guard, `ensureExpert`); Android passes no expert id on these calls |
| V5 Input Validation | yes | Client: title trim and 200 cap, description 2000 cap, `BigDecimal` price parse with digit cap, duration clamp 30..480 step 30, 15-minute-grid times, lowercase tag ids; server zod `.strict()` remains authority |
| V6 Cryptography | no | None; nothing hand-rolled |
| V7 Error handling / logging | yes | Show server `message` as plain text (`Text`, never HTML); do not log request bodies or topic text; no token in logs |
| V12/V13 API | yes | Send exactly the documented keys (strict schemas reject extras); typed DTOs only |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Mass assignment via extra JSON keys | Tampering | Typed Moshi DTOs emit only schema keys; `.strict()` server-side |
| Client-side floor bypass | Tampering | Treated as UX only; server returns `PRICE_BELOW_MINIMUM` (PRICING-LIMITS §3) |
| Oversized numeric input causing 500 / overflow | DoS | Digit cap and `longValueExact()` before send (Pitfall 4) |
| Stale per-user data shown after logout/login as another user | Information disclosure | No singleton topic cache; VM-scoped state (Architecture, Pattern 3) |
| Server message rendered as markup | Tampering/XSS | Compose `Text` only; no `AndroidView`/HTML |
| Persisting `pricing_limits` | Integrity | Read from in-memory `UserManager.me` only (MeData.kt rule) |
| Acting during a live call (availability change) | Integrity (business rule) | `CallGuard.attempt(EditAvailability)` on Save |

## Sources

### Primary (HIGH confidence)
- `backend-reference/src/controllers/availability.controller.js`, `schemas/availability.schema.js`, `services/availability.js`: availability contract, validation, slot generation (also executed locally with Luxon for `24:00`/`23:59`).
- `backend-reference/src/controllers/expertTopics.controller.js`, `schemas/expertTopics.schema.js`, `utils/pricing.js`, `routes/expertTopics.js`: topic contract, floor, soft delete.
- `backend-reference/src/schemas/expertProfile.schema.js`, `controllers/expert.controller.js`: `PATCH /expert/profile` tag replace.
- `backend-reference/docs/ios/PRICING-LIMITS.md`, `SESSION-BUFFER.md`: floor semantics, no-cache rule, buffer behaviour.
- iOS sources read: `AvailabilitySettingsViewModel.swift`, `AvailabilitySettingsView.swift`, `AvailabilityDayRow.swift`, `TimeRangePickerSheet.swift`, `TopicsSetupViewModel.swift`, `TopicEditorViewModel.swift`, `ExpertiseEditorViewModel.swift`, `ExpertTopicRequest.swift`, `ExpertTopicItem.swift`, `AvailabilityScheduleData.swift`, `UpdateMyAvailabilityRequest.swift`, `UpdateExpertProfileRequest.swift`.
- Android repo files read: `ClickMeApi.kt`, `ApiRequestUrls.kt`, `ApiErrorParser.kt`, `ApiException.kt`, `MoshiProvider.kt`, `MeData.kt`, `UserManager.kt`, `CallGuard.kt`, `CallDialog.kt`, `CallDialogs.kt`, `Timezones.kt`, `Money.kt`, `CategoryIconMap.kt`, `HomeExpertScreen.kt`, `HomeExpertTab.kt`, `PaymentMethodsViewModel.kt`, `SignupTagsViewModel.kt`, shared components, `FakeClickMeApi.kt`, `FakeCallGuard.kt`, `libs.versions.toml`, `app/build.gradle.kts`.
- `.planning/phases/10-expert-topics-availability/10-CONTEXT.md`, `10-UI-SPEC.md`, `REQUIREMENTS.md`, `STATE.md`, `09-CONTEXT.md` (D-12, D-13).

### Secondary (MEDIUM confidence)
- Project memory (`build-env-gotchas`): JDK path, test commands, `--stop` hazard.

### Tertiary (LOW confidence)
- Locale-specific `KeyboardType.Decimal` behaviour (A2): from general Android knowledge, not verified in this session.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. No new libraries; every component read from the repo.
- Architecture: HIGH. Mirrors existing Phase 6-9 patterns (assisted VM, per-tab stacks, repository with `apiCall`).
- Backend contract: HIGH for the repo copy; MEDIUM that production matches it (A1).
- Pitfalls: HIGH for 1, 2, 5, 6, 7 (read from code); MEDIUM for 3 (device behaviour) and 10 (design choice).

**Research date:** 2026-10-24
**Valid until:** 2026-11-23 (backend contract is stable; recheck `pricing_limits` JPY floor placeholder and the topic delete behaviour if the backend changes)
