@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CallCenterPermissionTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun start(
        h: CallCenterHarness,
        result: MicPermissionResult,
    ) {
        h.api.joinCallHandler = { h.inAppJoinResponse() }
        h.permission.result = result
        h.center.startCall(h.request())
    }

    @Test
    fun deniedPermanently_queuesMicDialogOnly() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            start(h, MicPermissionResult.DeniedPermanently)
            runCurrent()
            assertNull(h.center.session.value)
            assertEquals(listOf<CallDialog>(CallDialog.MicPermissionDenied), h.center.dialogs.value)
            assertEquals(0, h.api.endCallCalls)
            assertEquals(0, h.foreground.starts.size)
            assertEquals(0, h.engine.joins.size)
        }

    @Test
    fun deniedOnce_tearsDownSilently() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            start(h, MicPermissionResult.DeniedOnce)
            runCurrent()
            assertNull(h.center.session.value)
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
            assertEquals(0, h.api.endCallCalls)
            assertEquals(0, h.foreground.starts.size)
        }

    @Test
    fun cancelled_tearsDownSilently() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            start(h, MicPermissionResult.Cancelled)
            runCurrent()
            assertNull(h.center.session.value)
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
            assertEquals(0, h.api.endCallCalls)
            assertEquals(0, h.foreground.starts.size)
        }
}
