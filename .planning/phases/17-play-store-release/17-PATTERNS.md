# Phase 17: Play Store Release - Pattern Map

**Mapped:** 2026-11-05
**Files analyzed:** 15 (8 code or config, 7 planning or web artifacts)
**Analogs found:** 14 / 15
**All analog paths verified git-tracked** (`git ls-files`). Android main root below: `app/src/main/java/com/uptrendinvestments/clickmeapp/` (written `$M`); test root `app/src/test/java/com/uptrendinvestments/clickmeapp/` (written `$T`).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `$M/screens/bookingnote/BookingNoteViewModel.kt` (mod) | viewmodel | CRUD + draft restore | `$M/screens/clientmode/writereview/WriteReviewViewModel.kt` | exact (assisted + handle) |
| `$M/screens/settings/feedback/FeedbackViewModel.kt` (mod) | viewmodel | request-response form | `WriteReviewViewModel.kt` (handle) | role-match (this VM is plain `@Inject`) |
| `$M/screens/clientmode/search/SearchExpertViewModel.kt` (mod) | viewmodel | request-response search | `WriteReviewViewModel.kt` (handle) | role-match |
| `$T/.../bookingnote/BookingNoteViewModelTest.kt`, `BookingNoteScreenTest.kt` (mod: ctor call sites, new restore tests) | test | n/a | `$T/screens/clientmode/writereview/WriteReviewViewModelTest.kt` | exact |
| `$T/.../settings/feedback/FeedbackViewModelTest.kt` (mod) | test | n/a | `WriteReviewViewModelTest.kt` | exact |
| `$T/.../clientmode/search/SearchExpertViewModelTest.kt` (mod) | test | n/a | `WriteReviewViewModelTest.kt` | exact |
| `$M/screens/call/calling/MeetingCallScreen.kt` (mod) | component | layout | itself (lines 95-135), BUG-10 pinned-End structure | self |
| `$M/screens/call/calling/subviews/MeetingCallAvatar.kt` (mod) | component | layout / draw | itself (lines 30-50, 110-130) | self |
| `$T/screens/call/calling/MeetingCallScreenControlsReachableTest.kt` (new) | test (Robolectric Compose) | n/a | `$T/screens/call/calling/MeetingCallScreenEndButtonRegressionTest.kt` | exact |
| `app/build.gradle.kts` (mod, lines 76-77) | config | n/a | itself | self |
| `$M/config/AppLinks.kt` (mod, optional) | config | n/a | itself | self (research recommends no change) |
| `17-RELEASE-CHECKLIST.md` (new) | planning doc | human steps | `.planning/phases/13-push-notifications-internal-testing/13-RELEASE-CHECKLIST.md` | exact |
| `17-CONSOLE-PASTE.md` (new) | planning doc | paste-ready text | `13-push-notifications-internal-testing/13-PLAY-CONSOLE-DRAFTS.md` | exact |
| `13-PLAY-CONSOLE-DRAFTS.md` (mod: deletion line ~78, icon alpha, financial rows, FGS type) | planning doc | edit | itself | self |
| `17-DELETE-ACCOUNT-PAGE.html` (new) | web page | static | none in repo | no analog |
| `17-PAYMENTS-DECISION.md` (new) | planning doc | decision checkpoint | none (use RESEARCH policy quotes) | no analog |

## Pattern Assignments

### POL-02: all three VMs (reference `WriteReviewViewModel.kt`)

**Imports** (WriteReviewViewModel.kt lines 3-5):
```kotlin
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
```

**Constructor with assisted + handle** (lines 31-38). Apply to `BookingNoteViewModel` (already `@AssistedInject`; insert handle between args and repository, keeping the Factory unchanged):
```kotlin
@HiltViewModel(assistedFactory = WriteReviewViewModel.Factory::class)
class WriteReviewViewModel
    @AssistedInject
    constructor(
        @Assisted private val key: WriteReviewKey,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ReviewsRepository,
        private val launchRequest: ReviewLaunchRequest,
    ) : ViewModel() {
```
For `FeedbackViewModel` and `SearchExpertViewModel` (plain `@HiltViewModel` + `@Inject constructor`) just add `private val savedStateHandle: SavedStateHandle` to the constructor. Hilt supplies it, no assisted wiring.

**Restore on load, validate the restored value** (lines 68-82):
```kotlin
savedStateHandle.get<Int>(KEY_RATING)?.takeIf { it in RATING_MIN..RATING_MAX } ?: DEFAULT_RATING
...
savedStateHandle.get<String>(KEY_COMMENT).orEmpty().take(ReviewsRepository.COMMENT_MAX)
```
BookingNote: in `load()` (currently lines 66-69 `it.copy(noteText = text, lastPersisted = normalizeNote(text), ...)`), use the draft for `noteText` (cap with existing `capText(draft, NOTE_MAX_LENGTH)`) while `lastPersisted` stays `normalizeNote(serverText)`, so `hasChanges` is true and the discard prompt still works. Mirror in `onTextChange` (line ~77); remove the key after a successful `save()` (inside the success `_uiState.update`, line ~102).

**Mirror on edit** (lines 105-107):
```kotlin
savedStateHandle[KEY_RATING] = stars
_uiState.update { it.copy(rating = stars) }
```
Keys live as private consts in a companion/file (WriteReviewViewModel uses `KEY_RATING`, `KEY_COMMENT`; test uses literals `"rating"`, `"comment"`).

**Per-VM specifics**
- Feedback: mirror only `selectedTypeId`, `details`, and `email` when user-edited (do not store the signed-in prefill). Initial `_uiState` at line 68 `MutableStateFlow(FeedbackUiState())` must seed from the handle. Keep `FeedbackUiState.toString()` redaction (line 55). Update KDoc lines 36-37 ("never ... put in saved state (RESEARCH V8)"). `retry()` keeps `selectedTypeId ?: types.first().id` and `email.ifEmpty { signedInEmail() }`.
- Search: `_uiState` at line 39 `MutableStateFlow(SearchExpertUiState())`. Mirror `query`, `selectedCategorySlug`, `sort` (store `SearchSort.apiValue`, map back; enum at `SearchExpertUiState.kt:36`). Init already runs `runSearch(SearchMode.Fresh)` (lines 49-53), so it searches the restored query immediately with no debounce. Rewrite KDoc lines 26-28 (D-02 exception) to say Phase 17 D-09 supersedes it.
- BookingNote: rewrite KDoc lines 28-29 ("there is no SavedStateHandle ... RESEARCH A4"); keep "note text is never logged (T-16.4-10)".

**Tests: constructor helper with default handle** (WriteReviewViewModelTest.kt lines 38-42):
```kotlin
private fun vm(
    key: WriteReviewKey = WriteReviewKey(bookingId = "B1"),
    handle: SavedStateHandle = SavedStateHandle(),
) = WriteReviewViewModel(key, handle, ReviewsRepository(api), launchRequest)
```
**Restore and mirror tests** (lines 194-216; copy for each VM):
```kotlin
@Test
fun savedStateRestoresForm() =
    runTest(main.testDispatcher) {
        val vm = vm(handle = SavedStateHandle(mapOf("rating" to 2, "comment" to "Later")))
        runCurrent()
        assertEquals(2, vm.uiState.value.rating)
    }

@Test
fun editsAreMirroredToSavedState() =
    runTest(main.testDispatcher) {
        val handle = SavedStateHandle()
        val vm = vm(handle = handle)
        runCurrent()
        vm.onRatingSelected(4)
        assertEquals(4, handle.get<Int>("rating"))
    }
```
Call sites to update (SavedStateHandle() default; `BookingNoteScreenTest` mirrors `WriteReviewScreenTest.kt:137` which passes `SavedStateHandle()`): `BookingNoteViewModelTest.kt:36`, `BookingNoteScreenTest.kt:72`, `FeedbackViewModelTest.kt:42`, `SearchExpertViewModelTest.kt:52`. New BookingNote assertion: restored draft makes `hasChanges` true.

---

### BUG-22 / BUG-34: `MeetingCallScreen.kt`, `MeetingCallAvatar.kt`

**Current layout to change** (`MeetingCallScreen.kt` lines 98-114): scroll region is `BoxWithConstraints(Modifier.weight(1f).fillMaxWidth())` containing `Column(... .verticalScroll(rememberScrollState()).heightIn(min = maxHeight) ...)`; End Call is pinned below it (BUG-10 fix, do not unpin). TalkBack: root and the scroll column are `isTraversalGroup`, controls use traversal indexes (KDoc at lines 75-79). Moving the controls row into the pinned region changes groups and requires re-running `MeetingCallScreenTest.rootIsATraversalGroupAndTheNameIsAHeading` and the Phase 15 a11y tests.

**Dead-height source** (`MeetingCallAvatar.kt` lines 38-42, 123):
```kotlin
private val HeroMaxWidth = 320.dp
private val GlowSize = 360.dp
private val SingleAvatarSize = 160.dp
...
Modifier.requiredSize(glowSize).drawBehind { drawCircle(Brush.radialGradient(... radius = size.minDimension / 2f)) }
```
Fix step 1: make the glow overflow (draw it with `drawBehind` on a box sized to avatar plus ring) instead of occupying a 360 dp layout box. Keep `avatarUnchangedOnPhone` (`MeetingCallScreenTest`, `w360dp-h800dp`) green. Fix step 2: size the hero from `BoxWithConstraints.maxHeight`, not `LocalConfiguration.screenHeightDp` (`ShortWindowHeight = 480.dp`, line 45).

**New test harness: copy `MeetingCallScreenEndButtonRegressionTest.kt` lines 51-125.**

Rule, config, state:
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MeetingCallScreenEndButtonRegressionTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = start.plusSeconds(1800)
    private val navBar = 48
    ... MeetingCallUiState(connectionType = ..., status = CallStatus.Active,
        timer = callTimerState(start, end, end.minusSeconds(120)), ...)
```
`show()` with font scale + inset (lines 88-112):
```kotlin
rule.setContent {
    view = LocalView.current
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = fontScale)) {
        ClickMeTheme { Box(Modifier.size(width, height)) { MeetingCallScreen(state = state(connectionType), onMinimize = {}, onToggleMute = {}, onToggleSpeaker = {}, onEndCall = { endCalls++ }, onOpenExternal = {}, zone = ZoneOffset.UTC, timeFormat = timeFormat) } }
    }
}
rule.dispatchInsets(view, navigationBarInsets(navBar))
rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertExists()
```
Bounds assertion style (lines 114-119):
```kotlin
rule.onNodeWithTag(CALL_END_BUTTON_TAG).assertIsDisplayed()
val bottom = rule.onNodeWithTag(CALL_END_BUTTON_TAG).getUnclippedBoundsInRoot().bottom
assertTrue("End Call bottom is $bottom, ...", bottom <= limit)
```
New assertions to add (tags per RESEARCH): `CALL_CONTROL_MINIMIZE_TAG`, `CALL_CONTROL_CHAT_TAG`, `CALL_CONTROL_MUTE_TAG`, `CALL_CONTROL_SPEAKER_TAG`; `performScrollTo().assertIsDisplayed()` and control bottom <= End Call top (root coordinates). Cases: 360x800 fontScale 1 with banner+chip (BUG-22); about 339x753 dp fontScale 2 in Waiting/pre-join state (BUG-34); `ConnectionType.SkypeZoom`; Connected pair hero. Existing tests only assert End Call, so write the failing test first. Existing test names to keep green: `endCallStaysAboveTheNavigationBarWhenTheBannerAndChipShow`, `...OnTheTestPhoneWindow`, `endCallStaysOnScreenAtTwoHundredPercentFontScale`, `endSessionStaysAboveTheNavigationBarInTheExternalVariant`.

---

### `app/build.gradle.kts` (lines 71-78)
```kotlin
defaultConfig {
    applicationId = "com.uptrendinvestments.clickmeapp"
    minSdk = 26
    targetSdk = 37
    versionCode = 5
    versionName = "0.1.0"
```
Change only those two lines: `versionCode = 6`, `versionName = "1.0.0"`; later uploads bump by one. Commit message form `chore(release): versionCode 6` (ledger policy, `16-BUILD-LEDGER.md`, bump commit touches only that line).

### `$M/config/AppLinks.kt` (whole file)
```kotlin
object AppLinks {
    const val PRIVACY_POLICY = "https://www.uptrendinvestments.net/clickme/privacy.html"
    /** Support contact (iOS HelpView `supportEmail`). */
    const val SUPPORT_EMAIL = "support@clickme.app"
}
```
Only if the app is to link the deletion page: add `const val DELETE_ACCOUNT = "https://www.uptrendinvestments.net/clickme/delete-account.html"` with a one-line KDoc. RESEARCH recommends not adding code (Google needs only the web URL in Console).

---

### Planning docs

**`17-RELEASE-CHECKLIST.md`** (analog `13-RELEASE-CHECKLIST.md`). Copy structure: intro paragraph (human-only, manual steps never block execution, tick only after "Check:" matches, nothing secret pasted into the repo, short names `AS_JBR`, `APP`), numbered `## N. Title (decision id)` sections each with `Needs:`, numbered sub-steps with exact commands, `Check:`, optional `Rollback:`, then a trailing checkbox line `- [ ] (N) ...`. Example line pattern: `- [ ] (1) Backend D-04 deployed: /health commit equals the pushed master sha`. Also open with a dependency-order sentence like line 7 of the analog. Use role names ("Android client on the Samsung", "iOS expert on the iPhone"), never A/B. Reviewer password: write "from backend-reference/docs/APP-REVIEW-ACCOUNT.md; type it into Console, never paste here".

**`17-CONSOLE-PASTE.md` and edits to `13-PLAY-CONSOLE-DRAFTS.md`**: analog sections are `## 1. Store listing`, `## 2. Data safety`, `## 3. Foreground service permission declaration`. Edits per RESEARCH: replace the "Until then the support email handles requests" line (~line 78) with the deletion URL; fix icon spec to 32-bit PNG with alpha; split the financial info row into User payment info, Purchase history, Other financial info; FGS use case "Background Audio Access", not Phone call; separate "if deferred" and "if interrupted" answers. No password anywhere.

## Shared Patterns

### Verify command (all code plans)
**Source:** CONTEXT code_context. `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests '<class>' ktlintCheck`, plus `bash scripts/check-design-rules.sh`. Never `gradlew --stop`.

### Test dispatcher and API fake
**Source:** `WriteReviewViewModelTest.kt` (`runTest(main.testDispatcher)`, `FakeClickMeApi`, `runCurrent()`). Apply to all three VM test files.

### Privacy of user text
**Source:** `FeedbackUiState.toString()` redaction and BookingNote T-16.4-10. Mirror text into `SavedStateHandle` only; never log it.

### Release build checks (B2-B8)
**Source:** `16-BUILD-LEDGER.md`: gate, bump commit, `bundleProdRelease`, `jarsigner`/`aapt2` checks, archive to `/Users/LeonChen/ClickMe-release/`, ledger row, human upload. Main checkout only.

## No Analog Found

| File | Role | Reason |
|---|---|---|
| `17-DELETE-ACCOUNT-PAGE.html` | static web page | No HTML pages in the repo; build from RESEARCH "Account-deletion web page" (app and developer name, in-app steps from `screens/settings/deleteaccount/`, email path `support@clickme.app`, deleted vs kept lists from `accountCascade.js`, upcoming-bookings prerequisite). Match the look of the site's `privacy.html` if the user supplies it. |
| `17-PAYMENTS-DECISION.md` | decision checkpoint | New format; use RESEARCH D-05 section (FAQ 10281818 quote, dated copy, listing copy) |
| Store assets (screenshots, 512 icon) | binary | Specs in RESEARCH "Asset specs"; icon source is `res/mipmap-anydpi/ic_launcher.xml` and `res/drawable/ic_launcher_foreground.xml` |

## Metadata

**Analog search scope:** `app/src/main`, `app/src/test` (writereview, bookingnote, feedback, search, call/calling), `app/build.gradle.kts`, `config/`, Phase 13 planning docs.
**Files read:** 9 (BookingNoteViewModel, WriteReviewViewModel, FeedbackViewModel and SearchExpertViewModel heads, WriteReviewViewModelTest excerpts, EndButtonRegressionTest, MeetingCallAvatar and MeetingCallScreen excerpts, AppLinks, 13 checklist head).
**RESEARCH.md lines 437-621 were not read** (output was truncated at 436); content above relies on lines 1-436.
**Pattern extraction date:** 2026-11-05
