@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CallCenterGuardTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun CallCenterHarness.startLive(bookingId: String = "b1") {
        api.joinCallHandler = { inAppJoinResponse(bookingId = bookingId) }
        center.startCall(request(bookingId = bookingId))
    }

    @Test
    fun noCall_attemptPassesAndQueuesNothing() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            assertTrue(h.center.attempt(RestrictedAction.LogOut))
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
        }

    @Test
    fun liveCall_attemptBlocksWithEndCallDialog() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.startLive()
            runCurrent()
            assertFalse(h.center.attempt(RestrictedAction.SwitchModes))
            assertEquals(
                listOf<CallDialog>(CallDialog.Guard(GuardMessage.EndCallTo(RestrictedAction.SwitchModes))),
                h.center.dialogs.value,
            )
        }

    @Test
    fun attemptModifyBooking_blocksOnlyTheLiveBooking() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.startLive("b1")
            runCurrent()
            assertTrue(h.center.attemptModifyBooking("b2", RestrictedAction.CancelBooking))
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
            assertFalse(h.center.attemptModifyBooking("b1", RestrictedAction.CancelBooking))
            assertEquals(
                listOf<CallDialog>(CallDialog.Guard(GuardMessage.SessionInProgress(RestrictedAction.CancelBooking))),
                h.center.dialogs.value,
            )
        }

    @Test
    fun attemptModifyBooking_withNoCall_passes() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            assertTrue(h.center.attemptModifyBooking("b1", RestrictedAction.CancelBooking))
        }

    @Test
    fun secondStartCall_showsAlreadyOnCallGuard() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.startLive("b1")
            runCurrent()
            h.center.startCall(h.request(bookingId = "b2"))
            runCurrent()
            assertEquals(1, h.api.joinCallCalls)
            assertEquals(
                "b1",
                h.center.session.value!!
                    .bookingId,
            )
            assertEquals(
                listOf<CallDialog>(CallDialog.Guard(GuardMessage.AlreadyOnCall)),
                h.center.dialogs.value,
            )
        }

    @Test
    fun dialogQueue_isFifo_dismissDropsHead_andSkipsTailDuplicates() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.startLive()
            runCurrent()
            h.center.attempt(RestrictedAction.LogOut)
            h.center.attempt(RestrictedAction.LogOut)
            h.center.attempt(RestrictedAction.SwitchModes)
            val first = CallDialog.Guard(GuardMessage.EndCallTo(RestrictedAction.LogOut))
            val second = CallDialog.Guard(GuardMessage.EndCallTo(RestrictedAction.SwitchModes))
            assertEquals(listOf<CallDialog>(first, second), h.center.dialogs.value)
            h.center.dismissDialog()
            assertEquals(listOf<CallDialog>(second), h.center.dialogs.value)
            h.center.dismissDialog()
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
        }

    @Test
    fun minimizeAndExpand_flipTheFlag_andNoOpWithoutSession() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.center.minimize()
            assertFalse(h.center.isMinimized.value)
            h.startLive()
            runCurrent()
            h.center.minimize()
            assertTrue(h.center.isMinimized.value)
            h.center.expand()
            assertFalse(h.center.isMinimized.value)
        }
}
