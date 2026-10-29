package com.uptrendinvestments.clickmeapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.uptrendinvestments.clickmeapp.managers.CallResumeMarker
import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
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
import javax.inject.Qualifier
import javax.inject.Singleton

/** The `call_resume` Preferences DataStore behind [CallResumeMarker]: the booking id and scheduled end only. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CallResumePrefs

/**
 * Provides the call resume DataStore (its own file, so `SessionStore.clear()` is not involved) and registers the
 * marker as session data, so logout clears it (T-15-20).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CallResumeModule {
    @Binds
    @IntoSet
    abstract fun bindCallResumeCleaner(impl: CallResumeMarker): SessionDataCleaner

    companion object {
        @Provides
        @Singleton
        @CallResumePrefs
        fun callResumeDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher io: CoroutineDispatcher,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = CoroutineScope(io + SupervisorJob()),
                produceFile = { context.preferencesDataStoreFile(CallResumeMarker.FILE_NAME) },
            )
    }
}
