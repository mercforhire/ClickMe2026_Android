package com.uptrendinvestments.clickmeapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.uptrendinvestments.clickmeapp.managers.CategoryStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * The `clickme_categories` Preferences DataStore behind [CategoryStore]. A separate file and a qualified
 * binding, so `SessionStore.clear()` at logout never wipes it (D-08) and there is exactly one DataStore
 * instance per file.
 */
@Module
@InstallIn(SingletonComponent::class)
object CategoryCacheModule {
    @Provides
    @Singleton
    @CategoryCache
    fun categoryCacheDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(io + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(CategoryStore.FILE_NAME) },
        )
}
