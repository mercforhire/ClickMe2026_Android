package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey
import com.uptrendinvestments.clickmeapp.screens.push.ClientBookingProbe
import com.uptrendinvestments.clickmeapp.screens.push.ClientBookingProbeResult
import com.uptrendinvestments.clickmeapp.screens.push.PendingRequestLookup

/** Scriptable [ClientBookingProbe]; [probed] records every booking id asked about. */
class FakeClientBookingProbe(
    var result: suspend (String) -> ClientBookingProbeResult = { ClientBookingProbeResult.Unknown },
) : ClientBookingProbe {
    private val _probed = mutableListOf<String>()
    val probed: List<String> get() = _probed

    override suspend fun probe(bookingId: String): ClientBookingProbeResult {
        _probed += bookingId
        return result(bookingId)
    }
}

/** Scriptable [PendingRequestLookup]; [fail] makes every lookup answer null like a failed list call. */
class FakePendingRequestLookup(
    var keys: Map<String, RequestDecisionKey> = emptyMap(),
    var fail: Boolean = false,
) : PendingRequestLookup {
    private val _lookedUp = mutableListOf<String>()
    val lookedUp: List<String> get() = _lookedUp

    override suspend fun decisionKeyFor(requestId: String): RequestDecisionKey? {
        _lookedUp += requestId
        return if (fail) null else keys[requestId]
    }
}
