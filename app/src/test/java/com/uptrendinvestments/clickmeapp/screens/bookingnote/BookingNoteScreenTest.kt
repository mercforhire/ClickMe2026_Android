package com.uptrendinvestments.clickmeapp.screens.bookingnote

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.uptrendinvestments.clickmeapp.model.requests.BookingNoteRequest
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.bookingNoteData
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

    private fun setScreen(role: NoteRole): BookingNoteViewModel {
        val viewModel = BookingNoteViewModel(BookingNoteArgs(role, "b1"), BookingNoteRepository(api))
        rule.setContent {
            ClickMeTheme { BookingNoteScreen(viewModel = viewModel, header = header, onBack = {}) }
        }
        rule.waitForIdle()
        return viewModel
    }

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

    private companion object {
        const val HINT = "Jot down your thoughts about this session — only you'll see this."
    }
}
