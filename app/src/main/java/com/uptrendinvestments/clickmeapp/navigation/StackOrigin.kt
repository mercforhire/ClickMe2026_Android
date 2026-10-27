package com.uptrendinvestments.clickmeapp.navigation

import kotlinx.serialization.Serializable

/**
 * Which stack pushed a shared route key: [Default] for the stack the route was first built for, [Chats] when a chat
 * thread pushes it onto the Chats stack.
 *
 * It only keeps keys distinct across stacks, because `NavigationState.toDecoratedEntries` flattens the start stack with
 * the selected one and two equal keys would produce duplicate content keys (RESEARCH Pitfall 7; the same trick as
 * `AvailabilityOrigin` and `ExpertBookingOrigin`). Screens ignore it.
 */
@Serializable
enum class StackOrigin { Default, Chats }
