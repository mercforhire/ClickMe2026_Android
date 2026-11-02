package com.uptrendinvestments.clickmeapp.screens.bookingnote

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.BookingNoteRequest
import com.uptrendinvestments.clickmeapp.model.responses.BookingNoteData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.bookingNoteData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BookingNoteViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    init {
        api.getClientBookingNoteHandler = { bookingNoteData(it, "Old") }
        api.saveClientBookingNoteHandler = { id, body -> bookingNoteData(id, body.note.ifEmpty { null }) }
    }

    private fun vm(role: NoteRole = NoteRole.Client) =
        BookingNoteViewModel(BookingNoteArgs(role, "b1"), BookingNoteRepository(api))

    private fun BookingNoteViewModel.state() = uiState.value

    // Cap

    @Test
    fun capCutsALongPasteAtFiveThousand() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("a".repeat(5_100))
            assertEquals(5_000, vm.state().noteText.length)
            assertTrue(vm.state().atCap)
        }

    @Test
    fun capNeverSplitsASurrogatePair() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("a".repeat(4_999) + "😀")
            assertEquals(4_999, vm.state().noteText.length)
            assertFalse(Character.isHighSurrogate(vm.state().noteText.last()))
        }

    // Whitespace and the change check

    @Test
    fun whitespaceOnlySendsAnEmptyString() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("   \n ")
            vm.save()
            runCurrent()
            assertEquals(listOf("b1" to BookingNoteRequest("")), api.saveClientBookingNoteRequests)
        }

    @Test
    fun textIsSentAsTyped() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("  hi  ")
            vm.save()
            runCurrent()
            assertEquals(listOf("b1" to BookingNoteRequest("  hi  ")), api.saveClientBookingNoteRequests)
        }

    @Test
    fun whitespaceOverAnEmptyNoteIsNoChange() =
        runTest(main.testDispatcher) {
            api.getClientBookingNoteHandler = { bookingNoteData(it, null) }
            val vm = vm()
            runCurrent()
            vm.onTextChange("   ")
            assertFalse(vm.state().hasChanges)
            vm.save()
            runCurrent()
            assertEquals(0, api.saveClientBookingNoteCalls)
        }

    @Test
    fun aLoadedWhitespaceNoteIsNoChange() =
        runTest(main.testDispatcher) {
            api.getClientBookingNoteHandler = { bookingNoteData(it, "   ") }
            val vm = vm()
            runCurrent()
            assertEquals(BookingNoteLoad.Loaded, vm.state().load)
            assertFalse(vm.state().hasChanges)
            assertFalse(vm.state().canSave)
        }

    @Test
    fun afterAWhitespaceSaveThereIsNoChange() =
        runTest(main.testDispatcher) {
            // The server answers note: null for an empty note.
            api.saveClientBookingNoteHandler = { id, _ -> bookingNoteData(id, null) }
            val vm = vm()
            runCurrent()
            vm.onTextChange("   ")
            vm.save()
            runCurrent()
            assertFalse(vm.state().hasChanges)
            assertTrue(vm.requestLeave())
            assertFalse(vm.state().showDiscardPrompt)
            // What was typed stays in the field.
            assertEquals("   ", vm.state().noteText)
        }

    @Test
    fun clearingANoteIsAChangeAndSendsEmpty() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("")
            assertTrue(vm.state().hasChanges)
            vm.save()
            runCurrent()
            assertEquals(listOf("b1" to BookingNoteRequest("")), api.saveClientBookingNoteRequests)
        }

    // Saved state

    @Test
    fun savedStateLastsOnePointFiveSeconds() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("New")
            vm.save()
            runCurrent()
            assertTrue(vm.state().didSave)
            advanceTimeBy(1_499)
            assertTrue(vm.state().didSave)
            advanceTimeBy(1)
            runCurrent()
            assertFalse(vm.state().didSave)
        }

    @Test
    fun aSecondSaveRestartsTheSavedTimer() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("One")
            vm.save()
            runCurrent()
            advanceTimeBy(1_000)
            vm.onTextChange("Two")
            vm.save()
            runCurrent()
            assertTrue(vm.state().didSave)
            // The first timer would have fired by now; the second save restarted it.
            advanceTimeBy(1_499)
            assertTrue(vm.state().didSave)
            advanceTimeBy(1)
            runCurrent()
            assertFalse(vm.state().didSave)
        }

    // Guards

    @Test
    fun aSecondSaveWhileSavingSendsNothing() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<BookingNoteData>()
            api.saveClientBookingNoteHandler = { _, _ -> gate.await() }
            val vm = vm()
            runCurrent()
            vm.onTextChange("New")
            vm.save()
            vm.save()
            runCurrent()
            assertEquals(1, api.saveClientBookingNoteRequests.size)
            assertTrue(vm.state().isSaving)
            gate.complete(bookingNoteData("b1", "New"))
            runCurrent()
            assertFalse(vm.state().isSaving)
        }

    @Test
    fun noChangeMeansNoSave() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.save()
            runCurrent()
            assertEquals(0, api.saveClientBookingNoteCalls)
        }

    @Test
    fun typingDuringASaveStaysDirty() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<BookingNoteData>()
            api.saveClientBookingNoteHandler = { _, _ -> gate.await() }
            val vm = vm()
            runCurrent()
            vm.onTextChange("a")
            vm.save()
            runCurrent()
            vm.onTextChange("ab")
            gate.complete(bookingNoteData("b1", "a"))
            runCurrent()
            assertEquals("a", vm.state().lastPersisted)
            assertTrue(vm.state().hasChanges)
            assertEquals("ab", vm.state().noteText)
        }

    // Failures

    @Test
    fun saveFailureKeepsTheTextAndShowsTheServerMessage() =
        runTest(main.testDispatcher) {
            api.saveClientBookingNoteHandler =
                { _, _ -> throw api.httpError(422, """{"status":"error","message":"Too long"}""") }
            val vm = vm()
            runCurrent()
            vm.onTextChange("New")
            vm.save()
            runCurrent()
            assertEquals(FieldError.Text("Too long"), vm.state().saveError)
            assertEquals("New", vm.state().noteText)
            assertFalse(vm.state().isSaving)
            assertTrue(vm.state().hasChanges)
            assertFalse(vm.state().didSave)
        }

    @Test
    fun saveNetworkFailureShowsTheConnectionMessage() =
        runTest(main.testDispatcher) {
            api.saveClientBookingNoteHandler = { _, _ -> throw IOException("offline") }
            val vm = vm()
            runCurrent()
            vm.onTextChange("New")
            vm.save()
            runCurrent()
            assertEquals(FieldError.Res(R.string.login_error_network), vm.state().saveError)
        }

    @Test
    fun saveNotFoundIsAnErrorNotAnEmptyNote() =
        runTest(main.testDispatcher) {
            api.saveClientBookingNoteHandler =
                { _, _ -> throw api.httpError(404, """{"status":"error","message":"Not found"}""") }
            val vm = vm()
            runCurrent()
            vm.onTextChange("New")
            vm.save()
            runCurrent()
            assertTrue(vm.state().saveError != null)
            assertEquals("New", vm.state().noteText)
            assertEquals(BookingNoteLoad.Loaded, vm.state().load)
        }

    @Test
    fun suspendedSaveShowsNoDialog() =
        runTest(main.testDispatcher) {
            api.saveClientBookingNoteHandler =
                { _, _ ->
                    throw api.httpError(
                        403,
                        """{"status":"error","code":"ACCOUNT_SUSPENDED","message":"Account suspended."}""",
                    )
                }
            val vm = vm()
            runCurrent()
            vm.onTextChange("New")
            vm.save()
            runCurrent()
            assertNull(vm.state().saveError)
            assertFalse(vm.state().isSaving)
        }

    @Test
    fun dismissSaveErrorClearsIt() =
        runTest(main.testDispatcher) {
            api.saveClientBookingNoteHandler = { _, _ -> throw api.httpError(500, "{}") }
            val vm = vm()
            runCurrent()
            vm.onTextChange("New")
            vm.save()
            runCurrent()
            assertTrue(vm.state().saveError != null)
            vm.dismissSaveError()
            assertNull(vm.state().saveError)
            assertEquals("New", vm.state().noteText)
        }

    // Load

    @Test
    fun otherLoadFailureShowsFailedThenRetryLoads() =
        runTest(main.testDispatcher) {
            api.getClientBookingNoteHandler = { throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            val vm = vm()
            runCurrent()
            assertEquals(BookingNoteLoad.Failed(FieldError.Text("Boom")), vm.state().load)
            api.getClientBookingNoteHandler = { bookingNoteData(it, "Back") }
            vm.retry()
            runCurrent()
            assertEquals(BookingNoteLoad.Loaded, vm.state().load)
            assertEquals("Back", vm.state().noteText)
        }

    @Test
    fun notFoundOnLoadIsAnEmptyEditor() =
        runTest(main.testDispatcher) {
            api.getClientBookingNoteHandler = { throw api.httpError(404, """{"status":"error","message":"Nope"}""") }
            val vm = vm()
            runCurrent()
            assertEquals(BookingNoteLoad.Loaded, vm.state().load)
            assertEquals("", vm.state().noteText)
            assertFalse(vm.state().hasChanges)
        }

    @Test
    fun retryAfterLoadedDoesNotReload() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("Edited")
            vm.retry()
            runCurrent()
            assertEquals(1, api.getClientBookingNoteCalls)
            assertEquals("Edited", vm.state().noteText)
        }

    @Test
    fun expertRoleLoadsAndSavesOnTheExpertEndpointsOnly() =
        runTest(main.testDispatcher) {
            api.getBookingNoteHandler = { bookingNoteData(it, "Mine") }
            api.saveBookingNoteHandler = { id, body -> bookingNoteData(id, body.note) }
            val vm = vm(NoteRole.Expert)
            runCurrent()
            vm.onTextChange("Mine!")
            vm.save()
            runCurrent()
            assertEquals(listOf("b1" to BookingNoteRequest("Mine!")), api.saveBookingNoteRequests)
            assertEquals(0, api.getClientBookingNoteCalls)
            assertEquals(0, api.saveClientBookingNoteCalls)
        }

    // Leave gate

    @Test
    fun requestLeaveWhenCleanReturnsTrue() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            assertTrue(vm.requestLeave())
            assertFalse(vm.state().showDiscardPrompt)
        }

    @Test
    fun requestLeaveWhenDirtyPromptsAndReturnsFalse() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("Edited")
            assertFalse(vm.requestLeave())
            assertTrue(vm.state().showDiscardPrompt)
        }

    @Test
    fun dismissDiscardPromptKeepsTheText() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onTextChange("Edited")
            vm.requestLeave()
            vm.dismissDiscardPrompt()
            assertFalse(vm.state().showDiscardPrompt)
            assertEquals("Edited", vm.state().noteText)
        }

    // Privacy

    @Test
    fun toStringNeverPrintsTheNote() {
        val state = BookingNoteUiState(noteText = "secret-note-text", lastPersisted = "secret-old")
        assertFalse(state.toString().contains("secret"))
        assertTrue(state.toString().contains("noteLength=16"))
    }
}
