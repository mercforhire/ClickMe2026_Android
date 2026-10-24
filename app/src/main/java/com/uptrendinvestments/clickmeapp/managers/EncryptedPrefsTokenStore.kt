package com.uptrendinvestments.clickmeapp.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

/**
 * [SecureTokenStore] backed by EncryptedSharedPreferences (locked DEC-jwt-encrypted-sharedprefs).
 * The library is deprecated upstream; it is kept on purpose, hence the DEPRECATION suppression.
 *
 * - The prefs are created lazily on first access. Creation is guarded: on a corrupt file or lost Keystore
 *   key (restore, OS update) the file and the Keystore alias are deleted and creation is retried once.
 *   If it still fails the tokens live in memory only, so the next cold start is a logged-out launch.
 * - Values are cached in @Volatile fields after the first read.
 * - [save] and [clear] write with commit() (durable before return), then update the cache.
 *
 * This class cannot run under Robolectric (KeyStoreException); it is verified on the device in plan 02-15.
 */
@Suppress("DEPRECATION")
class EncryptedPrefsTokenStore(
    private val context: Context,
) : SecureTokenStore {
    private val lock = Any()

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var prefsInitialised = false

    @Volatile
    private var loaded = false

    @Volatile
    private var cachedAccess: String? = null

    @Volatile
    private var cachedRefresh: String? = null

    @Volatile
    private var cachedExpiresAt: Long? = null

    override val accessToken: String?
        get() {
            ensureLoaded()
            return cachedAccess
        }

    override val refreshToken: String?
        get() {
            ensureLoaded()
            return cachedRefresh
        }

    override val expiresAt: Long?
        get() {
            ensureLoaded()
            return cachedExpiresAt
        }

    @Synchronized
    override fun save(tokens: TokenPair) {
        ensureLoaded()
        val ok =
            prefs
                ?.edit()
                ?.putString(KEY_ACCESS, tokens.accessToken)
                ?.putString(KEY_REFRESH, tokens.refreshToken)
                ?.putLong(KEY_EXPIRES_AT, tokens.expiresAt)
                ?.commit()
        if (ok == false) Log.w(TAG, "Credential write did not commit; keeping them in memory only")
        cachedAccess = tokens.accessToken
        cachedRefresh = tokens.refreshToken
        cachedExpiresAt = tokens.expiresAt
    }

    @Synchronized
    override fun clear() {
        ensureLoaded()
        val ok = prefs?.edit()?.clear()?.commit()
        if (ok == false) Log.w(TAG, "Token clear did not commit")
        cachedAccess = null
        cachedRefresh = null
        cachedExpiresAt = null
    }

    private fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            val p = openPrefs()
            if (p != null) {
                cachedAccess = p.getString(KEY_ACCESS, null)
                cachedRefresh = p.getString(KEY_REFRESH, null)
                cachedExpiresAt = if (p.contains(KEY_EXPIRES_AT)) p.getLong(KEY_EXPIRES_AT, 0L) else null
            }
            loaded = true
        }
    }

    private fun openPrefs(): SharedPreferences? {
        if (prefsInitialised) return prefs
        prefs = create() ?: recreateAfterReset()
        prefsInitialised = true
        return prefs
    }

    private fun create(): SharedPreferences? =
        try {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Encrypted prefs unavailable: ${e.javaClass.simpleName}")
            null
        } catch (e: IOException) {
            Log.w(TAG, "Encrypted prefs unavailable: ${e.javaClass.simpleName}")
            null
        }

    private fun recreateAfterReset(): SharedPreferences? {
        context.deleteSharedPreferences(FILE_NAME)
        runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
        return create()
    }

    companion object {
        const val FILE_NAME = "clickme_secure"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val TAG = "TokenStore"
    }
}
