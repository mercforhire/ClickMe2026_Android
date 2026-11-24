package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.ConsultationFeeDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RESCHEDULE_INITIATED_BY_CLIENT
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant

/** Wire-level tests (MockWebServer + the real Moshi) for the Phase 11 expert booking endpoints. */
class ExpertBookingsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ExpertBookingsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(OkHttpClient())
                .addConverterFactory(MoshiConverterFactory.create(MoshiProvider.moshi))
                .build()
                .create(ClickMeApi::class.java)
        repository = ExpertBookingsRepository(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(
        code: Int,
        body: String,
    ) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    private val feedItem =
        """{"booking_id":"b1","client":{"id":"c1","name":"Ada Lovelace","avatar_url":null},""" +
            """"session":{"topic":"Taxes","start_time":"2026-11-02T15:00:00Z","end_time":"2026-11-02T16:00:00Z",""" +
            """"meeting_type":"in_app_voice","consultation_fee":{"amount":75,"currency":"USD",""" +
            """"label":"${'$'}75.00 / session"},"earnings_breakdown":{"gross_amount":75,"platform_fee":11.25,""" +
            """"net_amount":63.75,"commission_pct":15,"currency":"USD","fee_source":"booking"}},""" +
            """"status":{"label":"New Time Proposed","code":"pending_reschedule","color_accent":"#f6c23e"},""" +
            """"actions":{"can_join":false,"can_reschedule":true,"can_message":true},""" +
            """"private_note_preview":"Bring receipts",""" +
            """"reschedule":{"proposed_start":"2026-11-05T15:00:00Z","initiated_by":"client"}}"""

    private fun detailJson(
        feeAmount: String = "75",
        includeFee: Boolean = true,
    ): String {
        val fee =
            if (includeFee) {
                """"consultation_fee":{"amount":$feeAmount,"currency":"USD","label":"x"},"""
            } else {
                ""
            }
        return """{"status":"success","data":{"booking_id":"abc",""" +
            """"status":{"label":"Pending","code":"pending_reschedule","color_accent":"#fff"},""" +
            """"reschedule":{"proposed_start":"2026-11-05T15:00:00Z","proposed_end":"2026-11-05T16:00:00Z",""" +
            """"proposed_timezone":"America/Toronto","initiated_by":"client","note":"Sorry, a clash",""" +
            """"created_at":"2026-11-01T09:00:00Z"},"authorization_expires_at":"2026-11-04T09:00:00Z",""" +
            """"client":{"id":"c1","name":"Ada Lovelace","avatar_url":null},""" +
            """"session":{"topic_title":"Taxes",$fee"earnings_breakdown":null,""" +
            """"schedule":{"start_time":"2026-11-02T15:00:00Z","end_time":"2026-11-02T16:00:00Z"},""" +
            """"meeting_type":"skype_zoom"},"preparation_notes":{"text":"Bring receipts"},""" +
            """"expert_private_note":"secret","actions":{"can_join":false,"can_reschedule":true,""" +
            """"can_cancel":true,"can_message":true}}}"""
    }

    private fun feedPayload(item: String) =
        """{"status":"success","data":{"view_state":"populated","bookings":[$item],""" +
            """"pagination":{"total_count":1,"total_pages":1,"current_page":1}}}"""

    @Test
    fun listRowDecodesCanReschedule() =
        runTest {
            val withActions =
                feedItem.replace(
                    """"actions":{"can_join":false,"can_reschedule":true,"can_message":true}""",
                    """"actions":{"can_cancel":true,"can_reschedule":true,"can_message":true}""",
                )
            enqueue(200, feedPayload(withActions))

            val item = repository.list(ExpertBookingsRepository.TYPE_UPCOMING).bookings.single()

            assertTrue(item.actions.canReschedule)
            assertTrue(item.actions.canCancel)
        }

    @Test
    fun listRowWithoutActionsDefaultsToFalse() =
        runTest {
            val withoutActions =
                feedItem.replace(
                    """"actions":{"can_join":false,"can_reschedule":true,"can_message":true},""",
                    "",
                )
            enqueue(200, feedPayload(withoutActions))

            val item = repository.list(ExpertBookingsRepository.TYPE_UPCOMING).bookings.single()

            assertFalse(item.actions.canReschedule)
            assertFalse(item.actions.canCancel)
        }

    @Test
    fun listRowDecodesThePreview() =
        runTest {
            val withoutPreview = feedItem.replace(""""private_note_preview":"Bring receipts",""", "")
            enqueue(200, feedPayload(feedItem))
            enqueue(200, feedPayload(withoutPreview))

            val withNote = repository.list(ExpertBookingsRepository.TYPE_UPCOMING).bookings.single()
            val absent = repository.list(ExpertBookingsRepository.TYPE_UPCOMING).bookings.single()

            assertEquals("Bring receipts", withNote.privateNotePreview)
            assertNull(absent.privateNotePreview)
        }

    @Test
    fun listRowToStringOmitsThePreview() =
        runTest {
            enqueue(200, feedPayload(feedItem))

            val item = repository.list(ExpertBookingsRepository.TYPE_UPCOMING).bookings.single()

            assertEquals("Bring receipts", item.privateNotePreview)
            assertEquals("ExpertBookingItem(bookingId=b1, status=pending_reschedule)", item.toString())
            assertFalse(item.toString().contains("Bring receipts"))
        }

    @Test
    fun detailDecodesCanReschedule() =
        runTest {
            enqueue(
                200,
                detailJson().replace(
                    """"actions":{"can_join":false,"can_reschedule":true,""" +
                        """"can_cancel":true,"can_message":true}""",
                    """"actions":{"can_cancel":false,"can_reschedule":true}""",
                ),
            )
            enqueue(
                200,
                detailJson().replace(
                    """"actions":{"can_join":false,"can_reschedule":true,""" +
                        """"can_cancel":true,"can_message":true}""",
                    """"actions":{"can_cancel":false}""",
                ),
            )

            val withFlag = repository.detail("abc")
            val withoutFlag = repository.detail("abc")

            assertTrue(withFlag.actions.canReschedule)
            assertFalse(withFlag.actions.canCancel)
            assertFalse(withoutFlag.actions.canReschedule)
        }

    @Test
    fun actionsCanJoinDecodesTrueFalseAndMissing() =
        runTest {
            val feedFalse = """"actions":{"can_join":false,"can_reschedule":true,"can_message":true}"""
            val detailFalse =
                """"actions":{"can_join":false,"can_reschedule":true,""" +
                    """"can_cancel":true,"can_message":true}"""
            enqueue(200, feedPayload(feedItem.replace(feedFalse, """"actions":{"can_join":true}""")))
            enqueue(200, feedPayload(feedItem))
            enqueue(200, feedPayload(feedItem.replace(feedFalse, """"actions":{"can_reschedule":true}""")))
            enqueue(200, detailJson().replace(detailFalse, """"actions":{"can_join":true}"""))
            enqueue(200, detailJson())
            enqueue(200, detailJson().replace(detailFalse, """"actions":{"can_cancel":true}"""))

            val upcoming = ExpertBookingsRepository.TYPE_UPCOMING
            val rows = List(3) { repository.list(upcoming).bookings.single() }
            val details = List(3) { repository.detail("abc") }

            assertEquals(listOf(true, false, false), rows.map { it.actions.canJoin })
            assertEquals(listOf(true, false, false), details.map { it.actions.canJoin })
        }

    @Test
    fun detailDecodesPaymentActionRequiredAndPaymentNotChargedTrueFalseAndMissing() =
        runTest {
            val anchor = """"authorization_expires_at":"2026-11-04T09:00:00Z","""

            fun withKeys(keys: String) = detailJson().replace(anchor, anchor + keys)
            enqueue(200, withKeys(""""payment_action_required":true,"payment_not_charged":true,"""))
            enqueue(200, withKeys(""""payment_action_required":false,"payment_not_charged":false,"""))
            enqueue(200, detailJson())

            val details = List(3) { repository.detail("abc") }

            assertEquals(listOf<Boolean?>(true, false, null), details.map { it.paymentActionRequired })
            assertEquals(listOf<Boolean?>(true, false, null), details.map { it.paymentNotCharged })
        }

    @Test
    fun detailDecodesTheExpertNote() =
        runTest {
            val explicitNull =
                detailJson().replace(
                    """"expert_private_note":"secret",""",
                    """"expert_private_note":null,""",
                )
            val absent = detailJson().replace(""""expert_private_note":"secret",""", "")
            enqueue(200, detailJson())
            enqueue(200, explicitNull)
            enqueue(200, absent)

            assertEquals("secret", repository.detail("abc").expertPrivateNote)
            assertNull(repository.detail("abc").expertPrivateNote)
            assertNull(repository.detail("abc").expertPrivateNote)
        }

    @Test
    fun detailToStringOmitsTheNote() =
        runTest {
            enqueue(200, detailJson())

            val detail = repository.detail("abc")

            assertEquals("secret", detail.expertPrivateNote)
            assertEquals("ExpertBookingDetail(bookingId=abc, status=pending_reschedule)", detail.toString())
            assertFalse(detail.toString().contains("secret"))
        }

    @Test
    fun feedSendsTypePageLimitAndDecodesStatusObject() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"view_state":"populated","bookings":[$feedItem],""" +
                    """"pagination":{"total_count":1,"total_pages":1,"current_page":1}}}""",
            )

            val payload = repository.list(ExpertBookingsRepository.TYPE_UPCOMING)

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/bookings", request.url.encodedPath)
            assertEquals("upcoming", request.url.queryParameter("type"))
            assertEquals("1", request.url.queryParameter("page"))
            assertEquals("50", request.url.queryParameter("limit"))
            val item = payload.bookings.single()
            assertEquals("pending_reschedule", item.status.code)
            assertEquals("pending_reschedule", item.statusCode)
            assertEquals(RESCHEDULE_INITIATED_BY_CLIENT, item.reschedule?.initiatedBy)
            assertEquals(75.0, item.session.consultationFee?.amount ?: 0.0, 0.0)
            assertEquals("Taxes", item.topic)
            assertEquals(Instant.parse("2026-11-02T15:00:00Z"), item.startTime)
            assertEquals(63.75, item.session.earningsBreakdown?.netAmount ?: 0.0, 0.0)
            assertEquals(1, payload.pagination?.totalCount)
        }

    @Test
    fun detailUsesDetailsPath() =
        runTest {
            enqueue(200, detailJson())

            val detail = repository.detail("abc")

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/bookings/abc/details", request.url.encodedPath)
            assertEquals("Taxes", detail.session.topicTitle)
            assertEquals(Instant.parse("2026-11-02T15:00:00Z"), detail.session.schedule.startTime)
            assertEquals(Instant.parse("2026-11-02T15:00:00Z"), detail.startTime)
            assertEquals("Bring receipts", detail.preparationNotes?.text)
            assertEquals(Instant.parse("2026-11-05T16:00:00Z"), detail.reschedule?.proposedEnd)
            assertEquals("Sorry, a clash", detail.reschedule?.note)
            assertEquals("pending_reschedule", detail.status.code)
            assertTrue(detail.actions.canCancel)
            assertEquals(Instant.parse("2026-11-04T09:00:00Z"), detail.authorizationExpiresAt)
        }

    @Test
    fun cancelSendsReasonAndReturnsMajorRefund() =
        runTest {
            enqueue(200, """{"status":"success","message":"ok","data":{"refund_amount":0,"currency":"USD"}}""")

            val result = repository.cancel("abc", "Personal Emergency: sick")

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/expert/bookings/abc/cancel", request.url.encodedPath)
            assertEquals("""{"reason":"Personal Emergency: sick"}""", request.body?.utf8())
            assertEquals(0.0, result.refundAmount, 0.0)
            assertEquals("USD", result.currency)
        }

    @Test
    fun acceptRescheduleSendsEmptyObject() =
        runTest {
            enqueue(200, """{"status":"success","data":{"booking_id":"abc","status":"confirmed"}}""")
            enqueue(200, """{"status":"success","data":{"booking_id":"abc","status":"confirmed"}}""")
            enqueue(200, """{"status":"success","data":{"booking_id":"abc","status":"confirmed"}}""")

            val accepted = repository.acceptReschedule("abc")
            val accept = server.takeRequest()
            repository.declineReschedule("abc", null)
            val declineWithoutReason = server.takeRequest()
            repository.declineReschedule("abc", "Busy")
            val declineWithReason = server.takeRequest()

            assertEquals("POST", accept.method)
            assertEquals("/api/v1/expert/bookings/abc/accept-reschedule", accept.url.encodedPath)
            assertEquals("{}", accept.body?.utf8())
            assertEquals("confirmed", accepted.status)
            assertEquals("/api/v1/expert/bookings/abc/decline-reschedule", declineWithoutReason.url.encodedPath)
            assertEquals("{}", declineWithoutReason.body?.utf8())
            assertEquals("""{"reason":"Busy"}""", declineWithReason.body?.utf8())
        }

    @Test
    fun slotUnavailableIsTyped() =
        runTest {
            enqueue(409, """{"status":"error","code":"SLOT_UNAVAILABLE","message":"That time was just taken."}""")
            enqueue(409, """{"status":"error","code":"ALREADY_CANCELLED","message":"Already cancelled."}""")

            try {
                repository.acceptReschedule("abc")
                fail("Expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(409, e.status)
                assertEquals(ErrorCode.SlotUnavailable, e.code)
            }
            try {
                repository.cancel("abc", "x")
                fail("Expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(ErrorCode.AlreadyCancelled, e.code)
            }
        }

    @Test
    fun freeBookingFeeIsNotPaid() =
        runTest {
            assertFalse(ConsultationFeeDto(amount = 0.0, currency = "USD", label = null).isPaid)
            assertTrue(ConsultationFeeDto(amount = 75.0, currency = "USD", label = null).isPaid)
            assertFalse(ConsultationFeeDto(amount = null, currency = null, label = null).isPaid)

            enqueue(200, detailJson(feeAmount = "0"))
            enqueue(200, detailJson(feeAmount = "75"))
            enqueue(200, detailJson(includeFee = false))
            val free: ExpertBookingDetail = repository.detail("abc")
            val paid = repository.detail("abc")
            val missing = repository.detail("abc")

            assertFalse(free.isPaid)
            assertTrue(paid.isPaid)
            assertFalse(missing.isPaid)
        }
}
