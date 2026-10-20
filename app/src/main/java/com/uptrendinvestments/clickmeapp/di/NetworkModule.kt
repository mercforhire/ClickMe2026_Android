package com.uptrendinvestments.clickmeapp.di

import android.content.Context
import com.uptrendinvestments.clickmeapp.config.AppConfig
import com.uptrendinvestments.clickmeapp.managers.EncryptedPrefsTokenStore
import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.DebugNetwork
import com.uptrendinvestments.clickmeapp.network.MoshiProvider
import com.uptrendinvestments.clickmeapp.network.NetworkFactory
import com.uptrendinvestments.clickmeapp.network.TokenRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt graph for the network stack, delegating construction to [NetworkFactory].
 *
 * The transport depends only on the token store and [SessionEvents]; the session manager is deliberately
 * not part of this graph, which avoids a dependency cycle (the manager depends on [ClickMeApi]).
 * The unqualified [OkHttpClient] is the authenticated one; `@Named("bare")` is the refresh-only client.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun secureTokenStore(
        @ApplicationContext context: Context,
    ): SecureTokenStore = EncryptedPrefsTokenStore(context)

    @Provides
    @Singleton
    @Named("bare")
    fun bareClient(): OkHttpClient = NetworkFactory.bareClient(DebugNetwork.interceptors())

    @Provides
    @Singleton
    fun tokenRefresher(
        @Named("bare") client: OkHttpClient,
    ): TokenRefresher = TokenRefresher(client, AppConfig.baseUrl, MoshiProvider.moshi)

    @Provides
    @Singleton
    fun apiClient(
        tokens: SecureTokenStore,
        sessionEvents: SessionEvents,
        refresher: TokenRefresher,
    ): OkHttpClient = NetworkFactory.apiClient(tokens, sessionEvents, refresher, DebugNetwork.interceptors())

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient): Retrofit = NetworkFactory.retrofit(AppConfig.baseUrl, client)

    @Provides
    @Singleton
    fun clickMeApi(retrofit: Retrofit): ClickMeApi = retrofit.create(ClickMeApi::class.java)
}
