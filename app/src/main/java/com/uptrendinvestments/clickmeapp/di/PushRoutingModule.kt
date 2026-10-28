package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.UserRole
import com.uptrendinvestments.clickmeapp.screens.push.ClientBookingProbe
import com.uptrendinvestments.clickmeapp.screens.push.PendingRequestLookup
import com.uptrendinvestments.clickmeapp.screens.push.PushRoleSource
import com.uptrendinvestments.clickmeapp.screens.push.RepositoryClientBookingProbe
import com.uptrendinvestments.clickmeapp.screens.push.RepositoryPendingRequestLookup
import com.uptrendinvestments.clickmeapp.screens.push.ShellRouteRequest
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Push routing seams. A resolved route that no shell consumed is dropped with the session. The resolver's two lookups
 * (client-booking probe, pending-request lookup) are bound to their repository implementations here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PushRoutingModule {
    @Binds
    @IntoSet
    abstract fun bindShellRouteCleaner(impl: ShellRouteRequest): SessionDataCleaner

    @Binds
    abstract fun bindClientBookingProbe(impl: RepositoryClientBookingProbe): ClientBookingProbe

    @Binds
    abstract fun bindPendingRequestLookup(impl: RepositoryPendingRequestLookup): PendingRequestLookup

    companion object {
        /** The roles come from `/me` once loaded, else from the login user. */
        @Provides
        fun pushRoleSource(userManager: UserManager): PushRoleSource =
            PushRoleSource {
                val meRoles = userManager.me.value?.userRoles
                val loginRoles = userManager.authUser.value?.userRoles
                UserRole.Expert in (meRoles ?: loginRoles.orEmpty())
            }
    }
}
