package com.uptrendinvestments.clickmeapp.screens.bookingnote

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.requests.BookingNoteRequest
import com.uptrendinvestments.clickmeapp.model.responses.BookingNoteData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.bookingNoteData
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookingNoteScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()

    private val header =
        BookingNoteHeader(
            name = "Ada Lovelace",
            topic = "Algorithms",
            startMillis = 1_793_000_000_000L,
            endMillis = 1_793_000_000_000L + 3_600_000L,
        )

    private class Calls {
        var back = 0
        var retry = 0
        var dismissError = 0
        var keep = 0
        var discard = 0
        var save = 0
        var leave = 0
    }

    private var dispatcher: OnBackPressedDispatcher? = null

    private fun setScreen(
        role: NoteRole,
        calls: Calls = Calls(),
    ): BookingNoteViewModel {
        val viewModel =
            BookingNoteViewModel(BookingNoteArgs(role, "b1"), SavedStateHandle(), BookingNoteRepository(api))
        rule.setContent {
            ClickMeTheme {
                dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                BookingNoteScreen(viewModel = viewModel, header = header, onBack = { calls.back++ })
            }
        }
        rule.waitForIdle()
        return viewModel
    }

    private val loaded = BookingNoteUiState(load = BookingNoteLoad.Loaded)

    @Composable
    private fun Content(
        state: BookingNoteUiState,
        calls: Calls,
        topic: String?,
    ) {
        BookingNoteContent(
            state = state,
            header = header.copy(topic = topic),
            onLeave = { calls.leave++ },
            onRetry = { calls.retry++ },
            onTextChange = {},
            onSave = { calls.save++ },
            onDismissSaveError = { calls.dismissError++ },
            onKeepEditing = { calls.keep++ },
            onDiscard = { calls.discard++ },
            zone = ZoneId.of("UTC"),
            timeFormat = SimpleDateFormat("HH:mm", Locale.US),
            locale = Locale.US,
        )
    }

    private fun setContent(
        state: BookingNoteUiState,
        calls: Calls = Calls(),
        topic: String? = "Algorithms",
    ): Calls {
        rule.setContent { ClickMeTheme { Content(state, calls, topic) } }
        return calls
    }

    // Tracer: load, edit, save for both roles

    @Test
    fun clientNoteLoadsEditsAndSaves() {
        api.getClientBookingNoteHandler = { bookingNoteData(it, "Old") }
        api.saveClientBookingNoteHandler = { id, body -> bookingNoteData(id, body.note) }
        setScreen(NoteRole.Client)
        rule.onNodeWithText("My Note").assertExists()
        rule.onNodeWithText("Old").assertExists()
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(BOOKING_NOTE_FIELD_TAG).performTextReplacement("Old!")
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertIsEnabled()
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).performClick()
        rule.waitForIdle()
        assertEquals(listOf("b1" to BookingNoteRequest("Old!")), api.saveClientBookingNoteRequests)
        assertEquals(0, api.getBookingNoteCalls)
        assertEquals(0, api.saveBookingNoteCalls)
    }

    @Test
    fun expertNoteUsesOnlyTheExpertEndpoints() {
        api.getBookingNoteHandler = { bookingNoteData(it, "Old") }
        api.saveBookingNoteHandler = { id, body -> bookingNoteData(id, body.note) }
        setScreen(NoteRole.Expert)
        rule.onNodeWithText("Old").assertExists()
        rule.onNodeWithTag(BOOKING_NOTE_FIELD_TAG).performTextReplacement("Old!")
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).performClick()
        rule.waitForIdle()
        assertEquals(listOf("b1" to BookingNoteRequest("Old!")), api.saveBookingNoteRequests)
        assertEquals(0, api.getClientBookingNoteCalls)
        assertEquals(0, api.saveClientBookingNoteCalls)
    }

    @Test
    fun notFoundOpensAnEmptyEditor() {
        api.getClientBookingNoteHandler = { throw api.httpError(404, """{"status":"error","message":"Not found"}""") }
        setScreen(NoteRole.Client)
        rule.onNodeWithText(HINT).assertExists()
        rule.onNodeWithText("Couldn't load note").assertDoesNotExist()
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertIsNotEnabled()
    }

    @Test
    fun nullNoteOpensAnEmptyEditor() {
        api.getClientBookingNoteHandler = { bookingNoteData(it, null) }
        setScreen(NoteRole.Client)
        rule.onNodeWithText(HINT).assertExists()
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertIsNotEnabled()
    }

    // States over fixed state

    @Test
    fun savingShowsASpinnerAndNoSave() {
        setContent(loaded.copy(noteText = "x", isSaving = true))
        rule.onNodeWithTag(BOOKING_NOTE_SAVING_TAG).assertExists()
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertDoesNotExist()
    }

    @Test
    fun savedShowsTheCheckMark() {
        setContent(loaded.copy(didSave = true))
        rule.onNodeWithTag(BOOKING_NOTE_SAVED_TAG).assertExists()
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertDoesNotExist()
    }

    @Test
    fun anEditRightAfterASaveBringsSaveBack() {
        // UI-SPEC C2: the check shows only while the text is unchanged since the save.
        setContent(loaded.copy(noteText = "newer", lastPersisted = "older", didSave = true))
        rule.onNodeWithTag(BOOKING_NOTE_SAVED_TAG).assertDoesNotExist()
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertIsEnabled()
    }

    @Test
    fun saveIsDisabledWithNoChange() {
        setContent(loaded)
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertIsNotEnabled()
    }

    @Test
    fun saveTapReachesTheCallback() {
        val calls = setContent(loaded.copy(noteText = "x"))
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).performClick()
        assertEquals(1, calls.save)
    }

    @Test
    fun counterIsHiddenBelowTheThreshold() {
        setContent(loaded.copy(noteText = "a".repeat(4_499)))
        rule.onNodeWithTag(BOOKING_NOTE_COUNTER_TAG).assertDoesNotExist()
    }

    @Test
    fun counterShowsFromFortyFiveHundred() {
        setContent(loaded.copy(noteText = "a".repeat(4_500), lastPersisted = "x"))
        rule.onNodeWithTag(BOOKING_NOTE_COUNTER_TAG).assertTextEquals("4,500 / 5,000")
    }

    @Test
    fun headerShowsNameTopicAndDateLine() {
        setContent(loaded)
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText("Oct 26, 2026, 07:33 – 08:33").assertExists()
    }

    @Test
    fun headerFallsBackToConsultationWithNoTopic() {
        // UI-SPEC C3: the card the user came from reads "Consultation" for a null topic.
        setContent(loaded, topic = null)
        rule.onNodeWithText("Consultation").assertExists()
    }

    @Test
    fun loadingShowsTheSkeleton() {
        setContent(BookingNoteUiState(load = BookingNoteLoad.Loading))
        rule.onNodeWithTag(BOOKING_NOTE_SKELETON_TAG).assertExists()
        rule.onNodeWithTag(BOOKING_NOTE_FIELD_TAG).assertDoesNotExist()
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertIsNotEnabled()
    }

    @Test
    fun failedShowsCouldntLoadNoteWithRetry() {
        val calls =
            setContent(BookingNoteUiState(load = BookingNoteLoad.Failed(FieldError.Text("Server is down"))))
        rule.onNodeWithText("Couldn't load note").assertExists()
        rule.onNodeWithText("Server is down").assertExists()
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).assertIsNotEnabled()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, calls.retry)
    }

    @Test
    fun saveErrorShowsTheDialog() {
        val calls = setContent(loaded.copy(noteText = "x", saveError = FieldError.Text("Too long")))
        rule.onNodeWithText("Couldn't save note").assertExists()
        rule.onNodeWithText("Too long").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, calls.dismissError)
    }

    @Test
    fun discardDialogShowsWithBothChoices() {
        val calls = setContent(loaded.copy(noteText = "x", showDiscardPrompt = true))
        rule.onNodeWithText("Discard changes?").assertExists()
        rule.onNodeWithText("Your changes to this note won't be saved.").assertExists()
        rule.onNodeWithText("Keep editing").performClick()
        assertEquals(1, calls.keep)
        rule.onNodeWithText("Discard").performClick()
        assertEquals(1, calls.discard)
    }

    @Test
    fun noDialogsByDefault() {
        setContent(loaded)
        rule.onNodeWithText("Discard changes?").assertDoesNotExist()
        rule.onNodeWithText("Couldn't save note").assertDoesNotExist()
    }

    // Bound to a real ViewModel

    @Test
    fun arrowWhenCleanLeavesWithNoPrompt() {
        api.getClientBookingNoteHandler = { bookingNoteData(it, "Old") }
        val calls = Calls()
        setScreen(NoteRole.Client, calls)
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, calls.back)
        rule.onNodeWithText("Discard changes?").assertDoesNotExist()
    }

    @Test
    fun arrowWhenDirtyPromptsThenDiscardLeaves() {
        api.getClientBookingNoteHandler = { bookingNoteData(it, "Old") }
        val calls = Calls()
        setScreen(NoteRole.Client, calls)
        rule.onNodeWithTag(BOOKING_NOTE_FIELD_TAG).performTextReplacement("Edited")
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(0, calls.back)
        rule.onNodeWithText("Discard changes?").assertIsDisplayed()
        rule.onNodeWithText("Discard").performClick()
        assertEquals(1, calls.back)
    }

    @Test
    fun keepEditingStaysWithTheTextIntact() {
        api.getClientBookingNoteHandler = { bookingNoteData(it, "Old") }
        val calls = Calls()
        setScreen(NoteRole.Client, calls)
        rule.onNodeWithTag(BOOKING_NOTE_FIELD_TAG).performTextReplacement("Edited")
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Keep editing").performClick()
        assertEquals(0, calls.back)
        rule.onNodeWithText("Discard changes?").assertDoesNotExist()
        rule.onNodeWithText("Edited").assertExists()
    }

    @Test
    fun systemBackWhenDirtyPrompts() {
        api.getClientBookingNoteHandler = { bookingNoteData(it, "Old") }
        val calls = Calls()
        setScreen(NoteRole.Client, calls)
        rule.onNodeWithTag(BOOKING_NOTE_FIELD_TAG).performTextReplacement("Edited")
        rule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        rule.waitForIdle()
        assertEquals(0, calls.back)
        rule.onNodeWithText("Discard changes?").assertExists()
    }

    @Test
    fun systemBackWhileSavingIsSwallowed() {
        api.getClientBookingNoteHandler = { bookingNoteData(it, "Old") }
        val gate = CompletableDeferred<BookingNoteData>()
        api.saveClientBookingNoteHandler = { _, _ -> gate.await() }
        val calls = Calls()
        setScreen(NoteRole.Client, calls)
        rule.onNodeWithTag(BOOKING_NOTE_FIELD_TAG).performTextReplacement("Edited")
        rule.onNodeWithTag(BOOKING_NOTE_SAVE_TAG).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(BOOKING_NOTE_SAVING_TAG).assertExists()
        rule.onNodeWithContentDescription("Back").assertIsNotEnabled()
        rule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        rule.waitForIdle()
        assertEquals(0, calls.back)
        rule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assertEquals(1, api.saveClientBookingNoteRequests.size)
        gate.complete(bookingNoteData("b1", "Edited"))
        rule.waitForIdle()
    }

    @Test
    fun everythingIsNamedAndLargeEnough() {
        setContent(loaded.copy(noteText = "dirty", lastPersisted = "clean"))
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    private companion object {
        const val HINT = "Jot down your thoughts about this session — only you'll see this."
    }
}
