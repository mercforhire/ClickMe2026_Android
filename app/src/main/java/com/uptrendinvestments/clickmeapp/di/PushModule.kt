package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Push seams. The pending push destination is cleared with the session. Later push plans add their injectable seams
 * here (for example the FCM token seams in 13-06).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PushModule {
    @Binds
    @IntoSet
    abstract fun bindPushLaunchCleaner(impl: PushLaunchRequest): SessionDataCleaner
}
