package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.model.requests.ChatActionRequest
import com.uptrendinvestments.clickmeapp.model.requests.InitiateChatRequest
import com.uptrendinvestments.clickmeapp.model.requests.ReportUserRequest
import com.uptrendinvestments.clickmeapp.model.responses.InitiateChatData
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.reportData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ProfileSafetyViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private val session = ChatSessionState()

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) =
        ProfileSafetyViewModel(handle, ChatsRepository(api), session)

    private fun apiError(
        status: Int,
        code: String? = null,
        message: String = "msg",
    ) = api.httpError(
        status,
        if (code == null) {
            """{"status":"error","message":"$message"}"""
        } else {
            """{"status":"error","code":"$code","message":"$message"}"""
        },
    )

    @Test
    fun reportSendsWireValueClosesSheetAndThanks() =
        runTest(main.testDispatcher) {
            api.reportUserHandler = { _, _ -> reportData() }
            val vm = vm()

            vm.openReport()
            vm.selectReason(UserReportReason.Impersonation)
            vm.onDetailsChange("  fake  ")
            vm.submitReport("e1")
            runCurrent()

            assertEquals(listOf("e1" to ReportUserRequest("impersonation", "fake")), api.reportUserRequests)
            val state = vm.state.value
            assertFalse(state.sheetOpen)
            assertFalse(state.reportSubmitting)
            assertEquals(ProfileSafetySnackbar.ReportThanks, state.snackbar)
            assertNull(state.reason)
            assertEquals("", state.details)

            vm.consumeSnackbar()
            assertNull(vm.state.value.snackbar)
        }

    @Test
    fun submitWithoutReasonSendsNothing() =
        runTest(main.testDispatcher) {
            val vm = vm()
            vm.openReport()
            vm.submitReport("e1")
            runCurrent()

            assertEquals(0, api.reportUserRequests.size)
        }

    @Test
    fun blockInitiatesThenBlocksAndMarksSession() =
        runTest(main.testDispatcher) {
            api.initiateChatHandler = { InitiateChatData(threadId = "t1") }
            val vm = vm()

            vm.openBlock()
            assertTrue(vm.state.value.blockConfirmOpen)
            vm.confirmBlock("e1", "Ada")
            runCurrent()

            assertEquals(listOf(InitiateChatRequest("e1")), api.initiateChatRequests)
            assertEquals(listOf("t1" to ChatActionRequest("block")), api.chatActions)
            assertTrue(session.isBlockedByMe("t1"))
            val state = vm.state.value
            assertFalse(state.blockConfirmOpen)
            assertFalse(state.blockInFlight)
            assertEquals(ProfileSafetySnackbar.Blocked("Ada"), state.snackbar)
            assertNull(state.dialog)
        }

    @Test
    fun repeatConfirmWhileInFlightIgnored() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.initiateChatHandler = {
                gate.await()
                InitiateChatData(threadId = "t1")
            }
            val vm = vm()
            vm.openBlock()

            vm.confirmBlock("e1", "Ada")
            vm.confirmBlock("e1", "Ada")
            runCurrent()
            assertTrue(vm.state.value.blockInFlight)
            vm.dismissBlock()
            assertTrue(vm.state.value.blockConfirmOpen)

            gate.complete(Unit)
            runCurrent()

            assertEquals(1, api.initiateChatRequests.size)
            assertEquals(1, api.chatActions.size)
        }

    @Test
    fun initiate404ShowsUnavailable() =
        runTest(main.testDispatcher) {
            api.initiateChatHandler = { throw apiError(404) }
            val vm = vm()
            vm.openBlock()
            vm.confirmBlock("e1", "Ada")
            runCurrent()

            val dialog = vm.state.value.dialog
            assertEquals(R.string.block_error_title, dialog?.titleRes)
            assertEquals(FieldError.Res(R.string.chat_entry_error_unavailable), dialog?.message)
            assertEquals(0, api.chatActions.size)
            assertFalse(session.isBlockedByMe("t1"))
            assertFalse(vm.state.value.blockInFlight)
        }

    @Test
    fun blockFailureUsesBlockMapping() =
        runTest(main.testDispatcher) {
            api.initiateChatHandler = { InitiateChatData(threadId = "t1") }
            api.chatActionHandler = { _, _ -> throw apiError(404) }
            val vm = vm()
            vm.openBlock()
            vm.confirmBlock("e1", "Ada")
            runCurrent()

            val state = vm.state.value
            assertEquals(FieldError.Res(R.string.block_error_conversation_unavailable), state.dialog?.message)
            assertFalse(state.blockInFlight)
            assertFalse(state.blockConfirmOpen)
            assertNull(state.snackbar)
            assertFalse(session.isBlockedByMe("t1"))

            vm.dismissDialog()
            assertNull(vm.state.value.dialog)
            vm.openBlock()
            assertTrue(vm.state.value.blockConfirmOpen)
        }

    private fun ProfileSafetyViewModel.fillReport() {
        openReport()
        selectReason(UserReportReason.Spam)
        onDetailsChange("details")
    }

    @Test
    fun report429KeepsInputsAndSheet() =
        runTest(main.testDispatcher) {
            api.reportUserHandler = { _, _ -> throw apiError(429) }
            val vm = vm()
            vm.fillReport()
            vm.submitReport("e1")
            runCurrent()

            val state = vm.state.value
            assertEquals(R.string.report_error_title, state.dialog?.titleRes)
            assertEquals(FieldError.Res(R.string.report_error_rate_limited), state.dialog?.message)
            assertTrue(state.sheetOpen)
            assertEquals(UserReportReason.Spam, state.reason)
            assertEquals("details", state.details)
            assertFalse(state.reportSubmitting)
            assertNull(state.snackbar)
        }

    @Test
    fun report404AndNetworkUseTheirCopy() =
        runTest(main.testDispatcher) {
            api.reportUserHandler = { _, _ -> throw apiError(404) }
            val vm = vm()
            vm.fillReport()
            vm.submitReport("e1")
            runCurrent()
            assertEquals(
                FieldError.Res(R.string.report_error_unavailable),
                vm.state.value.dialog
                    ?.message,
            )

            vm.dismissDialog()
            api.reportUserHandler = { _, _ -> throw IOException("offline") }
            vm.submitReport("e1")
            runCurrent()
            assertEquals(
                FieldError.Res(R.string.login_error_network),
                vm.state.value.dialog
                    ?.message,
            )
            assertTrue(vm.state.value.sheetOpen)
        }

    @Test
    fun dismissIgnoredWhileSubmitting() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.reportUserHandler = { _, _ ->
                gate.await()
                reportData()
            }
            val vm = vm()
            vm.fillReport()
            vm.submitReport("e1")
            vm.submitReport("e1")
            runCurrent()
            assertTrue(vm.state.value.reportSubmitting)

            vm.dismissReport()
            vm.selectReason(UserReportReason.Harassment)
            assertTrue(vm.state.value.sheetOpen)
            assertEquals(UserReportReason.Spam, vm.state.value.reason)

            gate.complete(Unit)
            runCurrent()
            assertEquals(1, api.reportUserRequests.size)
            assertFalse(vm.state.value.sheetOpen)
        }

    @Test
    fun stateRestoredFromSavedState() {
        val handle = SavedStateHandle(mapOf("sheet_open" to true, "reason" to "spam", "details" to "x"))
        val state = vm(handle).state.value

        assertTrue(state.sheetOpen)
        assertEquals(UserReportReason.Spam, state.reason)
        assertEquals("x", state.details)
    }
}
