package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.JsonDataException
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

class ApiErrorParserTest {
    private val iosWireValues =
        listOf(
            "MISSING_TOKEN",
            "INVALID_TOKEN",
            "INVALID_CODE",
            "AUTH_SERVICE_UNAVAILABLE",
            "EXPIRED_TOKEN",
            "EMAIL_TAKEN",
            "FORBIDDEN",
            "NOT_FOUND",
            "NO_RELATIONSHIP",
            "VALIDATION_ERROR",
            "VALIDATION_FAILED",
            "INVALID_INPUT",
            "INVALID_MONTH",
            "INVALID_WEEK_START",
            "SLOT_UNAVAILABLE",
            "DUPLICATE_BOOKING",
            "INVALID_STATUS",
            "ALREADY_CANCELLED",
            "ALREADY_PROCESSED",
            "SESSION_NOT_ACTIVE",
            "SESSION_IN_PROGRESS",
            "EXPERT_HAS_UPCOMING_BOOKINGS",
            "CALL_UNAVAILABLE",
            "CANNOT_BOOK_SELF",
            "EXPERT_NOT_BOOKABLE",
            "MEETING_LINK_UNAVAILABLE",
            "RESCHEDULE_OUTSIDE_AUTH_WINDOW",
            "PAYMENT_METHOD_REQUIRED",
            "THREAD_BLOCKED",
            "NOT_BLOCKER",
            "CANNOT_BLOCK_SELF",
            "EXPERT_NOT_FOUND",
            "PAYMENT_FAILED",
            "PAYMENT_REQUIRES_ACTION",
            "INTENT_EXPIRED",
            "PI_CONTEXT_MISMATCH",
            "PI_AMOUNT_MISMATCH",
            "AMOUNT_BELOW_MINIMUM",
            "PRICE_BELOW_MINIMUM",
            "PAID_TOPIC_REQUIRES_PAYMENT",
            "INVALID_PROMO",
            "INVALID_PAYMENT_METHOD",
            "BOOKING_ATTEMPT_CONFLICT",
            "LONG_LEAD_REQUIRES_SAVED_CARD",
            "COMMISSION_NOT_CONFIGURED",
            "DIRECT_CHARGE_DISABLED",
            "REFUND_AMOUNT_MISSING",
            "NO_PAYOUT_METHOD",
            "KYC_INCOMPLETE",
            "CONFIG_MISSING",
            "INVALID_COUNTRY",
            "CONNECT_NOT_RATIFIED",
            "ENDPOINT_RETIRED",
            "EMAIL_REQUIRED",
            "CONFIRMATION_REQUIRED",
            "TOKEN_USER_MISMATCH",
            "ACCOUNT_SUSPENDED",
            "CANNOT_REPORT_SELF",
            "EXPERT_UNAVAILABLE",
            "CLIENT_UNAVAILABLE",
            "RECIPIENT_UNAVAILABLE",
            "UPLOAD_ERROR",
            "TOO_MANY_REQUESTS",
            "RATE_LIMITED",
            "INTERNAL_ERROR",
        )

    private fun httpException(
        status: Int,
        body: String,
    ): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(status, responseBody))
    }

    @Test
    fun errorCode_hasExactly65Cases() {
        assertEquals(65, ErrorCode.entries.size)
        assertEquals(65, iosWireValues.size)
    }

    @Test
    fun errorCode_wireValuesAreUniqueAndMatchIosInOrder() {
        assertEquals(iosWireValues, ErrorCode.entries.map { it.wire })
        assertEquals(
            65,
            ErrorCode.entries
                .map { it.wire }
                .toSet()
                .size,
        )
    }

    @Test
    fun errorCode_fromWireRoundTripsEveryIosValue() {
        iosWireValues.forEach { raw ->
            assertEquals(raw, ErrorCode.fromWire(raw)?.wire)
        }
    }

    @Test
    fun errorCode_unknownAndNullMapToNull() {
        assertNull(ErrorCode.fromWire("BRAND_NEW_CODE"))
        assertNull(ErrorCode.fromWire(null))
    }

    @Test
    fun errorCode_isTokenErrorOnlyForTheThreeTokenCodes() {
        val tokenCodes = setOf(ErrorCode.MissingToken, ErrorCode.InvalidToken, ErrorCode.ExpiredToken)
        ErrorCode.entries.forEach { code ->
            assertEquals(code.name, code in tokenCodes, code.isTokenError)
        }
    }

    @Test
    fun validationEnvelope_withFieldErrors_usesFirstFieldMessage() {
        val body =
            """{"status":"error","code":"VALIDATION_ERROR",""" +
                """"errors":[{"field":"email","message":"Invalid email"}]}"""
        val e = ApiErrorParser.httpError(422, body)
        assertEquals(422, e.status)
        assertEquals(ErrorCode.ValidationError, e.code)
        assertEquals(1, e.fieldErrors.size)
        assertEquals("email", e.fieldErrors[0].field)
        assertEquals("Invalid email", e.userMessage)
    }

    @Test
    fun standardEnvelope_slotUnavailable_usesServerMessage() {
        val body = """{"status":"error","code":"SLOT_UNAVAILABLE","message":"That slot was just taken"}"""
        val e = ApiErrorParser.httpError(409, body)
        assertEquals(ErrorCode.SlotUnavailable, e.code)
        assertEquals("That slot was just taken", e.userMessage)
    }

    @Test
    fun envelopeWithoutCode_hasNullCodeAndKeepsMessage() {
        val e = ApiErrorParser.httpError(401, """{"status":"error","message":"Invalid email or password"}""")
        assertNull(e.code)
        assertNull(e.rawCode)
        assertEquals("Invalid email or password", e.userMessage)
    }

    @Test
    fun statusOnlyEnvelope_hasNullCodeAndNullUserMessage() {
        val e = ApiErrorParser.httpError(400, """{"status":"error"}""")
        assertNull(e.code)
        assertNull(e.userMessage)
        assertTrue(e.fieldErrors.isEmpty())
    }

    @Test
    fun unknownCode_isNullButRawCodeIsKept() {
        val e = ApiErrorParser.httpError(409, """{"status":"error","code":"BRAND_NEW_CODE","message":"x"}""")
        assertNull(e.code)
        assertEquals("BRAND_NEW_CODE", e.rawCode)
    }

    @Test
    fun paymentRequiresAction_exposesDetail() {
        val body =
            """{"status":"error","code":"PAYMENT_REQUIRES_ACTION","message":"Authentication required",""" +
                """"detail":{"client_secret":"pi_1_secret_2","stripe_account":"acct_123"}}"""
        val e = ApiErrorParser.httpError(402, body)
        assertEquals(ErrorCode.PaymentRequiresAction, e.code)
        assertEquals("acct_123", e.detail?.get("stripe_account"))
        assertEquals("pi_1_secret_2", e.detail?.get("client_secret"))
    }

    @Test
    fun nonJsonEmptyAndNullBodies_doNotThrowAndHaveNullCode() {
        val html = ApiErrorParser.httpError(502, "<html>Bad gateway</html>")
        assertEquals(502, html.status)
        assertNull(html.code)
        assertNull(html.userMessage)
        assertNull(ApiErrorParser.httpError(500, "").code)
        assertNull(ApiErrorParser.httpError(500, null).code)
        assertNull(ApiErrorParser.parseEnvelope("   "))
        assertNull(ApiErrorParser.parseEnvelope("[1,2,3]"))
        assertNull(ApiErrorParser.codeOf("<html/>"))
    }

    @Test
    fun codeOf_returnsKnownCodeFromBody() {
        assertEquals(ErrorCode.InvalidToken, ApiErrorParser.codeOf("""{"status":"error","code":"INVALID_TOKEN"}"""))
    }

    @Test
    fun from_httpException_mapsStatusAndCode() {
        val t =
            ApiErrorParser.from(
                httpException(403, """{"status":"error","code":"ACCOUNT_SUSPENDED","message":"Suspended"}"""),
            )
        assertTrue(t is ApiException.Http)
        t as ApiException.Http
        assertEquals(403, t.status)
        assertEquals(ErrorCode.AccountSuspended, t.code)
    }

    @Test
    fun from_ioException_isNetwork() {
        assertTrue(ApiErrorParser.from(IOException("boom")) is ApiException.Network)
    }

    @Test
    fun from_jsonDataException_isDecoding() {
        assertTrue(ApiErrorParser.from(JsonDataException("bad")) is ApiException.Decoding)
        assertTrue(ApiErrorParser.from(IllegalStateException("other")) is ApiException.Decoding)
    }

    @Test
    fun from_apiException_returnsSameInstance() {
        val original = ApiErrorParser.httpError(400, null)
        assertSame(original, ApiErrorParser.from(original))
    }

    @Test
    fun httpException_messageNeverCarriesBodyText() {
        val e =
            ApiErrorParser.httpError(
                409,
                """{"status":"error","code":"SLOT_UNAVAILABLE","message":"secret server text"}""",
            )
        assertFalse(e.message.orEmpty().contains("secret server text"))
        assertEquals("HTTP 409 SLOT_UNAVAILABLE", e.message)
    }

    @Test
    fun apiCall_rethrowsCancellationUnchanged() =
        runTest {
            val ex = CancellationException("cancelled")
            try {
                apiCall<Unit> { throw ex }
                fail("expected CancellationException")
            } catch (e: CancellationException) {
                assertSame(ex, e)
            }
        }

    @Test
    fun apiCall_mapsSocketTimeoutToNetwork() =
        runTest {
            try {
                apiCall<Unit> { throw SocketTimeoutException() }
                fail("expected ApiException.Network")
            } catch (e: ApiException.Network) {
                assertTrue(e.cause is SocketTimeoutException)
            }
        }

    @Test
    fun apiCall_returnsValueOnSuccess() =
        runTest {
            assertEquals(7, apiCall { 7 })
        }
}
