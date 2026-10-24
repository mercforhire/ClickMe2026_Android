package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import kotlinx.coroutines.flow.MutableStateFlow

/** Test [CallGuard]: set [blocked] to make every attempt fail, and read the recorded attempts. */
class FakeCallGuard : CallGuard {
    override val isCallActive = MutableStateFlow(false)

    @Volatile
    var blocked: Boolean = false

    private val _attempts = mutableListOf<RestrictedAction>()
    val attempts: List<RestrictedAction> get() = synchronized(_attempts) { _attempts.toList() }

    private val _modifyAttempts = mutableListOf<Pair<String, RestrictedAction>>()
    val modifyAttempts: List<Pair<String, RestrictedAction>>
        get() = synchronized(_modifyAttempts) { _modifyAttempts.toList() }

    override fun attempt(action: RestrictedAction): Boolean {
        synchronized(_attempts) { _attempts += action }
        return !blocked
    }

    override fun attemptModifyBooking(
        bookingId: String,
        action: RestrictedAction,
    ): Boolean {
        synchronized(_modifyAttempts) { _modifyAttempts += bookingId to action }
        return !blocked
    }
}
