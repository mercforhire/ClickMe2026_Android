package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.CallStarter

/** Test [CallStarter] that records every [CallStartRequest] instead of starting a call. */
class FakeCallStarter : CallStarter {
    private val _requests = mutableListOf<CallStartRequest>()
    val requests: List<CallStartRequest> get() = synchronized(_requests) { _requests.toList() }

    override fun startCall(request: CallStartRequest) {
        synchronized(_requests) { _requests += request }
    }
}
