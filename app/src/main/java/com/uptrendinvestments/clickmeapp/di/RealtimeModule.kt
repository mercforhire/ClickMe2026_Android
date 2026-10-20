package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.config.AppConfig
import com.uptrendinvestments.clickmeapp.realtime.LifecycleProcessForeground
import com.uptrendinvestments.clickmeapp.realtime.ProcessForeground
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConfig
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeService
import com.uptrendinvestments.clickmeapp.realtime.socketUrl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings for the realtime graph. [RealtimeService] reuses the `@Named("bare")` OkHttp client from
 * [NetworkModule] (never the authenticated one); this module adds no client binding of its own.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {
    @Binds
    @Singleton
    abstract fun realtimeConnection(impl: RealtimeService): RealtimeConnection

    @Binds
    @Singleton
    abstract fun processForeground(impl: LifecycleProcessForeground): ProcessForeground

    companion object {
        /** The socket URL is the REST base URL with `/api/v1` removed (D-12). */
        @Provides
        @Singleton
        fun realtimeConfig(): RealtimeConfig = RealtimeConfig(socketUrl = socketUrl(AppConfig.baseUrl))
    }
}
