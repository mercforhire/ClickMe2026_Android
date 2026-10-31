package com.uptrendinvestments.clickmeapp.navigation

import kotlinx.serialization.Serializable

/**
 * Which stack pushed a shared route key: [Default] for the stack the route was first built for, [Chats] when a chat
 * thread pushes it onto the Chats stack, [Search] when a Search result pushes it onto the Search stack (Phase 16.1,
 * RESEARCH Pitfall 2: the same expert open on Explore and on Search never shares a content key).
 *
 * It only keeps keys distinct across stacks, because `NavigationState.toDecoratedEntries` flattens the start stack with
 * the selected one and two equal keys would produce duplicate content keys (RESEARCH Pitfall 7; the same trick as
 * `AvailabilityOrigin` and `ExpertBookingOrigin`). Screens ignore it.
 *
 * Residual exposure: only the profile key carries an origin. `BookingKey`, `RequestSentKey` and `PaymentMethodsKey` do
 * not, so the same expert's booking flow open on two stacks would produce equal keys. That cannot happen through the
 * UI, since those routes hide the bottom bar and the user cannot switch tabs mid-booking; only a programmatic tab
 * change (for example push-notification routing) while a booking is open on one stack, followed by the same flow on
 * another, could reach it. This predates the Search tab, which adds a second stack that can host the Profile to Book
 * flow. Thread an origin through those keys if that path ever becomes reachable.
 */
@Serializable
enum class StackOrigin { Default, Chats, Search }
