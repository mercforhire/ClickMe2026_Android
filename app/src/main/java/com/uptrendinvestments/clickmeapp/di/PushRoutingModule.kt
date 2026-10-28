package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.push.ShellRouteRequest
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Push routing seams. A resolved route that no shell consumed is dropped with the session. 13-07 adds the resolver
 * lookup bindings here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PushRoutingModule {
    @Binds
    @IntoSet
    abstract fun bindShellRouteCleaner(impl: ShellRouteRequest): SessionDataCleaner
}
