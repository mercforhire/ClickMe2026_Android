package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.managers.PushTokenSource

/** Test [PushTokenSource]: set [token] to what FCM should return; [deleteToken] rotates it like FCM does. */
class FakePushTokenSource(
    var token: String? = "fcm-token-1",
) : PushTokenSource {
    var currentTokenCalls = 0
        private set
    var deleteCalls = 0
        private set

    override suspend fun currentToken(): String? {
        currentTokenCalls++
        return token
    }

    override suspend fun deleteToken() {
        deleteCalls++
        token = "fcm-token-rotated"
    }
}
