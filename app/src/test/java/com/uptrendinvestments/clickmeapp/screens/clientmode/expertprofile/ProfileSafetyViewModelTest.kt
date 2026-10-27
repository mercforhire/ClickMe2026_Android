package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.model.requests.ReportUserRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.reportData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSafetyViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) = ProfileSafetyViewModel(handle, ChatsRepository(api))

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
}
