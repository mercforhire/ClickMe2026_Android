package com.uptrendinvestments.clickmeapp.managers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallPermissionBrokerTest {
    @Test
    fun ensureMicrophone_publishesPendingAndResumesWithGranted() =
        runTest {
            val broker = CallPermissionBroker()
            val result = async { broker.ensureMicrophone() }
            runCurrent()
            val pending = broker.pending.value
            assertNotNull(pending)
            broker.complete(pending!!.id, MicPermissionResult.Granted)
            assertEquals(MicPermissionResult.Granted, result.await())
            assertNull(broker.pending.value)
        }

    @Test
    fun complete_withStaleId_isIgnored() =
        runTest {
            val broker = CallPermissionBroker()
            val result = async { broker.ensureMicrophone() }
            runCurrent()
            val pending = broker.pending.value!!
            broker.complete(pending.id + 100, MicPermissionResult.Granted)
            runCurrent()
            assertEquals(pending, broker.pending.value)
            broker.complete(pending.id, MicPermissionResult.DeniedOnce)
            assertEquals(MicPermissionResult.DeniedOnce, result.await())
        }

    @Test
    fun ensureMicrophone_unanswered_cancelsAfterTimeout() =
        runTest {
            val broker = CallPermissionBroker()
            val result = async { broker.ensureMicrophone() }
            runCurrent()
            advanceTimeBy(MIC_PERMISSION_TIMEOUT_MS - 1)
            runCurrent()
            assertNotNull(broker.pending.value)
            advanceTimeBy(2)
            assertEquals(MicPermissionResult.Cancelled, result.await())
            assertNull(broker.pending.value)
        }

    @Test
    fun secondRequest_cancelsTheFirst() =
        runTest {
            val broker = CallPermissionBroker()
            val first = async { broker.ensureMicrophone() }
            runCurrent()
            val firstId = broker.pending.value!!.id
            val second = async { broker.ensureMicrophone() }
            runCurrent()
            assertEquals(MicPermissionResult.Cancelled, first.await())
            val secondPending = broker.pending.value
            assertNotNull(secondPending)
            assertEquals(false, secondPending!!.id == firstId)
            broker.complete(secondPending.id, MicPermissionResult.DeniedPermanently)
            assertEquals(MicPermissionResult.DeniedPermanently, second.await())
            assertNull(broker.pending.value)
        }
}
