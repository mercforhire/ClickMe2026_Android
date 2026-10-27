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
