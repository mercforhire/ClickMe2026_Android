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
        private val moshi = MoshiProvider.moshi
        private val messageNew: JsonAdapter<RealtimeEvent.MessageNew> = moshi.adapter(RealtimeEvent.MessageNew::class.java)
        private val messageRead: JsonAdapter<RealtimeEvent.MessageRead> = moshi.adapter(RealtimeEvent.MessageRead::class.java)
        private val userStatus: JsonAdapter<RealtimeEvent.UserStatus> = moshi.adapter(RealtimeEvent.UserStatus::class.java)
        private val typing: JsonAdapter<TypingPayload> = moshi.adapter(TypingPayload::class.java)
        private val bookingUpdate: JsonAdapter<RealtimeEvent.BookingUpdate> = moshi.adapter(RealtimeEvent.BookingUpdate::class.java)
        private val callEnded: JsonAdapter<RealtimeEvent.CallEnded> = moshi.adapter(RealtimeEvent.CallEnded::class.java)

        open fun decode(
            event: String,
            json: String,
        ): RealtimeEvent? =
            try {
                when (event) {
                    RealtimeEventNames.MESSAGE_NEW -> messageNew.fromJson(json)
                    RealtimeEventNames.MESSAGE_READ -> messageRead.fromJson(json)
                    RealtimeEventNames.USER_STATUS -> userStatus.fromJson(json)
                    RealtimeEventNames.TYPING_START -> typing.fromJson(json)?.let { RealtimeEvent.TypingStart(it.threadId, it.senderId) }
                    RealtimeEventNames.TYPING_STOP -> typing.fromJson(json)?.let { RealtimeEvent.TypingStop(it.threadId, it.senderId) }
                    RealtimeEventNames.BOOKING_UPDATE -> bookingUpdate.fromJson(json)
                    RealtimeEventNames.CALL_ENDED -> callEnded.fromJson(json)
                    else -> null
                }
            } catch (_: Exception) {
                // JsonDataException, JsonEncodingException, IOException, DateTimeParseException: drop the event.
                null
            }
    }
