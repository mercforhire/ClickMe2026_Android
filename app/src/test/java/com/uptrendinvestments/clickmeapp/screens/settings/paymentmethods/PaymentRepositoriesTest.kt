package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.PaymentIntentRequest
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmBookingData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentIntentResponse
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodSetupData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PaymentRepositoriesTest {
    private val api = FakeClickMeApi()
    private val paymentMethods = PaymentMethodsRepository(api)
    private val booking = BookingRepository(api)

    private val intentRequest = PaymentIntentRequest("e1", "t2", "2026-10-23", "01:00", 60, "attempt-1")
    private val confirmRequest =
        ConfirmBookingRequest(
            paymentIntentId = "pi_1",
            expertId = "e1",
            topicId = "t2",
            startTime = "2026-10-23T01:00:00Z",
            meetingType = "in_app_voice",
            stripePaymentMethodId = "pm_1",
        )

    @Test
    fun paymentMethodsRepositoryReturnsServerListInOrder() =
        runTest {
            val items = listOf(PaymentMethodItem("pm_1", isDefault = true), PaymentMethodItem("pm_2"))
            api.getPaymentMethodsHandler = { PaymentMethodsData(items) }

            assertEquals(items, paymentMethods.list())
            assertEquals(1, api.getPaymentMethodsCalls)
        }

    @Test
    fun createSetupIntentReturnsSetupDataWithOneCall() =
        runTest {
            val setup = PaymentMethodSetupData("seti_1_secret_x", "cus_1", "ek_1")
            api.setupIntentHandler = { setup }

            assertEquals(setup, paymentMethods.createSetupIntent())
            assertEquals(1, api.setupIntentCalls)
        }

    @Test
    fun deleteAndSetDefaultRecordTheirIds() =
        runTest {
            api.deletePaymentMethodHandler = { SuccessMessageResponse(status = "success") }
            api.setDefaultPaymentMethodHandler = { SuccessMessageResponse(status = "success") }

            paymentMethods.delete("pm_1")
            paymentMethods.setDefault("pm_2")

            assertEquals(listOf("pm_1"), api.deletedPaymentMethodIds)
            assertEquals(listOf("pm_2"), api.defaultedPaymentMethodIds)
        }

    @Test
    fun bookingRepositoryReturnsBarePaymentIntentAndConfirmData() =
        runTest {
            val response = PaymentIntentResponse("pi_1", "pi_1_secret_x", "cad")
            api.paymentIntentHandler = { response }
            api.confirmBookingHandler = { ConfirmBookingData("b9", "pending_approval") }

            assertEquals(response, booking.paymentIntent(intentRequest))
            assertEquals("b9", booking.confirm(confirmRequest).bookingId)
            assertEquals(listOf(intentRequest), api.paymentIntentRequests)
            assertEquals(listOf(confirmRequest), api.confirmBookingRequests)
        }

    @Test
    fun confirm402PreservesThreeDsDetail() =
        runTest {
            api.confirmBookingHandler = {
                throw api.httpError(
                    402,
                    """{"status":"error","code":"PAYMENT_REQUIRES_ACTION","message":"Authentication required",""" +
                        """"detail":{"client_secret":"pi_1_secret_x","stripe_account":"acct_1"}}""",
                )
            }

            val error = runCatching { booking.confirm(confirmRequest) }.exceptionOrNull()

            assertTrue(error is ApiException.Http)
            error as ApiException.Http
            assertEquals(ErrorCode.PaymentRequiresAction, error.code)
            assertEquals("pi_1_secret_x", error.detail?.get("client_secret"))
            assertEquals("acct_1", error.detail?.get("stripe_account"))
        }

    @Test
    fun paymentIntentLongLead422AndNetworkFailuresSurfaceAsApiException() =
        runTest {
            api.paymentIntentHandler = {
                throw api.httpError(
                    422,
                    """{"status":"error","code":"LONG_LEAD_REQUIRES_SAVED_CARD","message":"Use a saved card"}""",
                )
            }
            val http = runCatching { booking.paymentIntent(intentRequest) }.exceptionOrNull()
            assertTrue(http is ApiException.Http)
            assertEquals(ErrorCode.LongLeadRequiresSavedCard, (http as ApiException.Http).code)

            api.paymentIntentHandler = { throw IOException("offline") }
            val network = runCatching { booking.paymentIntent(intentRequest) }.exceptionOrNull()
            assertTrue(network is ApiException.Network)
        }
}
