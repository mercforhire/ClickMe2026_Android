package com.uptrendinvestments.clickmeapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.uptrendinvestments.clickmeapp.managers.FcmPushTokenRegistrar
import com.uptrendinvestments.clickmeapp.managers.FirebasePushTokenSource
import com.uptrendinvestments.clickmeapp.managers.PushPreferences
import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import com.uptrendinvestments.clickmeapp.managers.PushTokenSource
import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Push seams. The pending push destination is cleared with the session. The FCM token registrar and token
 * source are bound here, and the `clickme_push` DataStore holds the device id, in its own file so that
 * `SessionStore.clear()` at logout never wipes it. Later push plans add their injectable seams here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PushModule {
    @Binds
    @IntoSet
    abstract fun bindPushLaunchCleaner(impl: PushLaunchRequest): SessionDataCleaner

    @Binds
    abstract fun bindPushTokenRegistrar(impl: FcmPushTokenRegistrar): PushTokenRegistrar

    @Binds
    abstract fun bindPushTokenSource(impl: FirebasePushTokenSource): PushTokenSource

    companion object {
        @Provides
        @Singleton
        @PushPrefs
        fun pushPreferencesDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher io: CoroutineDispatcher,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = CoroutineScope(io + SupervisorJob()),
                produceFile = { context.preferencesDataStoreFile(PushPreferences.FILE_NAME) },
            )
    }
}
