package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import java.time.Instant

/**
 * Pure rules for the list of rows in one thread. No coroutines or Compose, so every rule is a unit test.
 *
 * Rules (12-RESEARCH Pattern 1):
 * - A server row is matched to an existing row by server id first, then, for the sender's own messages, by
 *   client_msg_id. A match is replaced by the server row but keeps the client_msg_id, so the list key does not
 *   change and the bubble is not recreated.
 * - A Read message never goes back to Sent on a later merge.
 * - Existing rows with no match are kept: older pages and local Sending or Failed bubbles.
 * - Server-backed rows are ordered by createdAt with a stable sort, then local unsent bubbles in their order.
 *
 * The socket never echoes the sender's own message and carries no client_msg_id, so client_msg_id matching
 * happens only when REST pages are merged (Pitfall 3). Every id comparison ignores case because the backend
 * compares UUIDs case-insensitively.
 */
object ThreadReducer {
    /** Adds a local optimistic bubble. The caller passes a Sending message with no server id. */
    fun appendLocal(
        items: List<ThreadItem>,
        message: ThreadItem.Message,
    ): List<ThreadItem> = order(items + message)

    /**
     * The send of [clientMsgId] succeeded. The bubble takes [serverId] and [timestamp] and becomes Sent (Read stays
     * Read). When a reload already added the server row, the two collapse into that row.
     */
    fun ack(
        items: List<ThreadItem>,
        clientMsgId: String,
        serverId: String,
        timestamp: Instant,
    ): List<ThreadItem> {
        val bubble = findMine(items, clientMsgId) ?: return items
        val existingRow = items.firstOrNull { it !== bubble && it.serverId.sameId(serverId) }
        val result =
            if (existingRow != null) {
                items.mapNotNull { item ->
                    when {
                        item === bubble -> null
                        item === existingRow && item is ThreadItem.Message ->
                            item.copy(clientMsgId = item.clientMsgId ?: bubble.clientMsgId)
                        else -> item
                    }
                }
            } else {
                items.map { item ->
                    if (item === bubble) {
                        bubble.copy(
                            serverId = serverId,
                            createdAt = timestamp,
                            delivery = if (bubble.delivery == Delivery.Read) Delivery.Read else Delivery.Sent,
                        )
                    } else {
                        item
                    }
                }
            }
        return order(result)
    }

    /** Merges a page of server rows into [items] without dropping or duplicating anything. */
    fun mergePage(
        items: List<ThreadItem>,
        page: List<ThreadItem>,
    ): List<ThreadItem> {
        val result = items.toMutableList()
        for (incoming in page) {
            val index = indexOfMatch(result, incoming)
            if (index < 0) {
                result.add(incoming)
            } else {
                result[index] = reconcile(result[index], incoming)
            }
        }
        return order(result)
    }

    /** The send of [clientMsgId] failed: the Sending bubble becomes Failed. */
    fun fail(
        items: List<ThreadItem>,
        clientMsgId: String,
    ): List<ThreadItem> = updateLocal(items, clientMsgId, from = Delivery.Sending, to = Delivery.Failed)

    /** Retry: the Failed bubble goes back to Sending with the same client_msg_id (D-01). */
    fun retry(
        items: List<ThreadItem>,
        clientMsgId: String,
    ): List<ThreadItem> = updateLocal(items, clientMsgId, from = Delivery.Failed, to = Delivery.Sending)

    /** Deletes a local unsent bubble. A row that already has a server id is never removed. */
    fun remove(
        items: List<ThreadItem>,
        clientMsgId: String,
    ): List<ThreadItem> =
        items.filterNot {
            it is ThreadItem.Message && it.mine && it.serverId == null && it.clientMsgId.sameId(clientMsgId)
        }

    /**
     * A `message.new` socket event. Ignored when [myId] sent it (the sender is never echoed, but be safe) or when
     * the server id is already shown; otherwise it is added in createdAt order.
     */
    fun onIncoming(
        items: List<ThreadItem>,
        message: ThreadItem.Message,
        myId: String?,
    ): List<ThreadItem> {
        if (message.senderId.sameId(myId)) return items
        if (items.any { it.serverId.sameId(message.serverId) }) return items
        return order(items + message)
    }

    /** A `message.read` event: my messages whose server id is in [ids] become Read. */
    fun markRead(
        items: List<ThreadItem>,
        ids: Collection<String>,
    ): List<ThreadItem> =
        items.map { item ->
            val serverId = item.serverId
            val isMineWithId = item is ThreadItem.Message && item.mine && serverId != null
            if (isMineWithId && item is ThreadItem.Message && ids.any { it.equals(serverId, ignoreCase = true) }) {
                item.copy(delivery = Delivery.Read)
            } else {
                item
            }
        }

    /**
     * Server ids of peer messages that are not Read yet and not in [alreadyEmitted] (lower-cased ids), in list
     * order. The caller emits a read receipt for them.
     */
    fun unreadPeerIds(
        items: List<ThreadItem>,
        alreadyEmitted: Set<String>,
    ): List<String> {
        val emitted = alreadyEmitted.map { it.lowercase() }.toSet()
        return items.mapNotNull { item ->
            val id = item.serverId
            when {
                item !is ThreadItem.Message || item.mine || id == null -> null
                item.delivery == Delivery.Read -> null
                id.lowercase() in emitted -> null
                else -> id
            }
        }
    }

    private fun updateLocal(
        items: List<ThreadItem>,
        clientMsgId: String,
        from: Delivery,
        to: Delivery,
    ): List<ThreadItem> =
        items.map {
            val isTarget = it is ThreadItem.Message && it.mine && it.serverId == null && it.delivery == from
            if (isTarget && it is ThreadItem.Message && it.clientMsgId.sameId(clientMsgId)) {
                it.copy(delivery = to)
            } else {
                it
            }
        }

    /** The own message with [clientMsgId], or null. */
    fun findMine(
        items: List<ThreadItem>,
        clientMsgId: String,
    ): ThreadItem.Message? =
        items.firstOrNull {
            it is ThreadItem.Message && it.mine && it.clientMsgId.sameId(clientMsgId)
        } as ThreadItem.Message?

    private fun indexOfMatch(
        existing: List<ThreadItem>,
        incoming: ThreadItem,
    ): Int {
        if (incoming.serverId != null) {
            val byServerId = existing.indexOfFirst { it.serverId.sameId(incoming.serverId) }
            if (byServerId >= 0) return byServerId
        }
        if (incoming is ThreadItem.Message && incoming.mine && incoming.clientMsgId != null) {
            return existing.indexOfFirst {
                it is ThreadItem.Message && it.mine && it.clientMsgId.sameId(incoming.clientMsgId)
            }
        }
        return -1
    }

    private fun reconcile(
        existing: ThreadItem,
        incoming: ThreadItem,
    ): ThreadItem =
        if (existing is ThreadItem.Message && incoming is ThreadItem.Message) {
            incoming.copy(
                clientMsgId = existing.clientMsgId ?: incoming.clientMsgId,
                delivery = if (existing.delivery == Delivery.Read) Delivery.Read else incoming.delivery,
            )
        } else {
            incoming
        }

    /** Server-backed rows by createdAt (stable), then local unsent rows in their current order. */
    internal fun order(items: List<ThreadItem>): List<ThreadItem> {
        val (server, local) = items.partition { it.serverId != null }
        return server.sortedBy { it.createdAt } + local
    }

    private fun String?.sameId(other: String?): Boolean {
        if (this == null || other == null) return false
        return equals(other, ignoreCase = true)
    }
}
