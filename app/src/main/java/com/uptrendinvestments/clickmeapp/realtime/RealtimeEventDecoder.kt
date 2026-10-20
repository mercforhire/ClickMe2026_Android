package com.uptrendinvestments.clickmeapp.realtime

import com.squareup.moshi.JsonAdapter
import com.uptrendinvestments.clickmeapp.network.MoshiProvider
import javax.inject.Inject

/**
 * Pure `(event name, JSON text) -> RealtimeEvent?` decoder.
 *
 * Never throws: an unknown event, malformed JSON, a wrong type or a missing required field all
 * return null. It does not log (the socket service logs the dropped event name). `open` so the
 * protocol test can inject a throwing decoder.
 */
open class RealtimeEventDecoder
    @Inject
    constructor() {
        private val messageNew = adapter<RealtimeEvent.MessageNew>()
        private val messageRead = adapter<RealtimeEvent.MessageRead>()
        private val userStatus = adapter<RealtimeEvent.UserStatus>()
        private val typing = adapter<TypingPayload>()
        private val bookingUpdate = adapter<RealtimeEvent.BookingUpdate>()
        private val callEnded = adapter<RealtimeEvent.CallEnded>()

        open fun decode(
            event: String,
            json: String,
        ): RealtimeEvent? =
            try {
                when (event) {
                    RealtimeEventNames.MESSAGE_NEW -> messageNew.fromJson(json)
                    RealtimeEventNames.MESSAGE_READ -> messageRead.fromJson(json)
                    RealtimeEventNames.USER_STATUS -> userStatus.fromJson(json)
                    RealtimeEventNames.TYPING_START -> typing.fromJson(json)?.toStart()
                    RealtimeEventNames.TYPING_STOP -> typing.fromJson(json)?.toStop()
                    RealtimeEventNames.BOOKING_UPDATE -> bookingUpdate.fromJson(json)
                    RealtimeEventNames.CALL_ENDED -> callEnded.fromJson(json)
                    else -> null
                }
            } catch (_: Exception) {
                // JsonDataException, JsonEncodingException, IOException, DateTimeParseException: drop the event.
                null
            }

        private fun TypingPayload.toStart() = RealtimeEvent.TypingStart(threadId, senderId)

        private fun TypingPayload.toStop() = RealtimeEvent.TypingStop(threadId, senderId)

        private companion object {
            inline fun <reified T> adapter(): JsonAdapter<T> = MoshiProvider.moshi.adapter(T::class.java)
        }
    }
