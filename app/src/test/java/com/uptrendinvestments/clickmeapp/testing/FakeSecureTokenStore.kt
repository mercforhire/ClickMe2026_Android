package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import com.uptrendinvestments.clickmeapp.managers.TokenPair

/**
 * In-memory [SecureTokenStore] for JVM tests. EncryptedSharedPreferences cannot run under Robolectric
 * (KeyStoreException), so everything above the store is tested against this fake.
 */
class FakeSecureTokenStore(
    initial: TokenPair? = null,
) : SecureTokenStore {
    @Volatile
    override var accessToken: String? = initial?.accessToken
        private set

    @Volatile
    override var refreshToken: String? = initial?.refreshToken
        private set

    @Volatile
    override var expiresAt: Long? = initial?.expiresAt
        private set

    @Volatile
    override var lastLoginEmail: String? = null
        private set

    @Volatile
    var saveCount: Int = 0
        private set

    @Volatile
    var clearCount: Int = 0
        private set

    @Synchronized
    override fun save(tokens: TokenPair) {
        accessToken = tokens.accessToken
        refreshToken = tokens.refreshToken
        expiresAt = tokens.expiresAt
        saveCount++
    }

    /** Like the real store, the last email survives [clear]. */
    @Synchronized
    override fun saveLastLoginEmail(email: String?) {
        lastLoginEmail = email
    }

    @Synchronized
    override fun clear() {
        accessToken = null
        refreshToken = null
        expiresAt = null
        clearCount++
    }

    /** Test setup helper that sets only a refresh token (no access token). */
    @Synchronized
    fun setRefreshOnly(refresh: String) {
        accessToken = null
        refreshToken = refresh
        expiresAt = null
    }

    /** Test setup helper that sets only an access token (no refresh token). */
    @Synchronized
    fun setAccessOnly(access: String) {
        accessToken = access
        refreshToken = null
        expiresAt = null
    }
}
