package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.AgoraManager
import com.uptrendinvestments.clickmeapp.managers.CallCenter
import com.uptrendinvestments.clickmeapp.managers.CallEngine
import com.uptrendinvestments.clickmeapp.managers.CallForegroundController
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.CallPermissionBroker
import com.uptrendinvestments.clickmeapp.managers.CallPermissionGate
import com.uptrendinvestments.clickmeapp.managers.CallStarter
import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.services.call.CallForegroundBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Binds the voice call stack so [CallGuard], [CallStarter] and the three ports resolve app-wide.
 *
 * [CallCenter] is also in the [SessionDataCleaner] set, so a forced logout or suspension tears the call
 * down locally. [CallCenter] must never inject `UserManager`: `UserManager` injects the cleaner set, so that
 * would be a dependency cycle (RESEARCH Pitfall 8).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CallModule {
    @Binds
    abstract fun bindCallEngine(impl: AgoraManager): CallEngine

    @Binds
    abstract fun bindCallForegroundController(impl: CallForegroundBridge): CallForegroundController

    @Binds
    abstract fun bindCallPermissionGate(impl: CallPermissionBroker): CallPermissionGate

    @Binds
    abstract fun bindCallGuard(impl: CallCenter): CallGuard

    @Binds
    abstract fun bindCallStarter(impl: CallCenter): CallStarter

    @Binds
    @IntoSet
    abstract fun bindCallSessionCleaner(impl: CallCenter): SessionDataCleaner
}
