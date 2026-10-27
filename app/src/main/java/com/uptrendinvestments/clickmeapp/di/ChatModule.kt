package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ClientMsgIdGenerator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.util.UUID

/**
 * Chat seams: the session-local chat state is cleared with the session, and the current user id and the
 * client_msg_id generator are injectable so tests can fake them.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {
    @Binds
    @IntoSet
    abstract fun bindChatSessionCleaner(impl: ChatSessionState): SessionDataCleaner

    companion object {
        @Provides
        fun currentUserIdProvider(userManager: UserManager): CurrentUserIdProvider =
            CurrentUserIdProvider { userManager.me.value?.id ?: userManager.authUser.value?.id }

        @Provides
        fun clientMsgIdGenerator(): ClientMsgIdGenerator = ClientMsgIdGenerator { UUID.randomUUID().toString() }
    }
}
