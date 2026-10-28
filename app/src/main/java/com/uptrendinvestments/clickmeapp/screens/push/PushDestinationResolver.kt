package com.uptrendinvestments.clickmeapp.screens.push

import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsKeyFor
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsRootFor
import javax.inject.Inject

/** Upper bound on each lookup a tap triggers, so a slow network never leaves a tap pending for long (D-06). */
const val LOOKUP_TIMEOUT_MS = 5_000L

/** What the resolver needs to know about the app right now. */
data class PushRoutingContext(
    val currentMode: AppMode,
    val hasExpertRole: Boolean,
    val callActive: Boolean,
)

/** A resolved route: a key chain for a shell, or a chat thread that Phase 12 opens on the Chats tab. */
sealed interface PushRoute {
    data class Shell(
        val route: ShellRoute,
    ) : PushRoute

    data class Chat(
        val key: ChatThreadKey,
    ) : PushRoute
}

/** The mode the shell must be in and the route to open there. */
data class ResolvedPush(
    val targetMode: AppMode,
    val route: PushRoute,
)

/** Skeleton, filled in by the GREEN commit. */
class PushDestinationResolver
    @Inject
    constructor(
        private val bookingProbe: ClientBookingProbe,
        private val pendingRequests: PendingRequestLookup,
    ) {
        @Suppress("UnusedParameter")
        suspend fun resolve(
            destination: PushDestination,
            context: PushRoutingContext,
        ): ResolvedPush =
            ResolvedPush(
                context.currentMode,
                PushRoute.Shell(
                    ShellRoute(
                        context.currentMode,
                        notificationsRootFor(context.currentMode),
                        listOf(notificationsKeyFor(context.currentMode)),
                    ),
                ),
            )
    }
