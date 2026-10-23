package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.AcceptRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.CancelBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ClientBookingsRepositoryTest {
    private val api = FakeClickMeApi()
    private val repository = ClientBookingsRepository(api)

    @Test
    fun cancelReturnsHandlerDataAndRecordsTheReason() =
        runTest {
            val data = CancelBookingData(12.5, "CAD")
            api.cancelClientBookingHandler = { _, _ -> data }

            val result = repository.cancel("b1", "Change of plans: Moved")

            assertEquals(data, result)
            val expected = listOf("b1" to CancelBookingRequest("Change of plans: Moved"))
            assertEquals(expected, api.cancelClientBookingRequests)
        }

    @Test
    fun acceptRescheduleRecordsNullOrSuppliedPaymentMethod() =
        runTest {
            api.acceptClientRescheduleHandler = { id, _ -> RescheduleResponseData(id, "confirmed") }

            repository.acceptReschedule("b1")
            repository.acceptReschedule("b1", "pm_1")

            assertEquals(
                listOf(
                    "b1" to AcceptRescheduleRequest(null),
                    "b1" to AcceptRescheduleRequest("pm_1"),
                ),
                api.acceptClientRescheduleRequests,
            )
        }

    @Test
    fun declineRescheduleRecordsNullOrSuppliedReason() =
        runTest {
            api.declineClientRescheduleHandler = { id, _ -> RescheduleResponseData(id, "confirmed") }

            repository.declineReschedule("b1", null)
            repository.declineReschedule("b1", "Busy")

            assertEquals(
                listOf(
                    "b1" to DeclineRescheduleRequest(null),
                    "b1" to DeclineRescheduleRequest("Busy"),
                ),
                api.declineClientRescheduleRequests,
            )
        }

    @Test
    fun acceptRescheduleSurfacesPaymentMethodRequiredCode() =
        runTest {
            api.acceptClientRescheduleHandler = { _, _ ->
                throw api.httpError(
                    422,
                    """{"status":"error","code":"PAYMENT_METHOD_REQUIRED","message":"x"}""",
                )
            }

            val error = runCatching { repository.acceptReschedule("b1") }.exceptionOrNull()

            assertTrue(error is ApiException.Http)
            error as ApiException.Http
            assertEquals(422, error.status)
            assertEquals(ErrorCode.PaymentMethodRequired, error.code)
        }

    @Test
    fun cancelSurfacesInvalidStatusAndNetworkFailures() =
        runTest {
            api.cancelClientBookingHandler = { _, _ ->
                throw api.httpError(409, """{"status":"error","code":"INVALID_STATUS","message":"x"}""")
            }
            val http = runCatching { repository.cancel("b1", "r") }.exceptionOrNull()
            assertTrue(http is ApiException.Http)
            assertEquals(ErrorCode.InvalidStatus, (http as ApiException.Http).code)

            api.cancelClientBookingHandler = { _, _ -> throw IOException("offline") }
            val network = runCatching { repository.cancel("b1", "r") }.exceptionOrNull()
            assertTrue(network is ApiException.Network)
        }
}
