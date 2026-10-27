package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ThreadReducerTest {
    private fun t(seconds: Long): Instant = Instant.ofEpochSecond(seconds)

    private fun mine(
        clientMsgId: String? = null,
        serverId: String? = null,
        delivery: Delivery = Delivery.Sending,
        at: Long = 100,
        text: String = "hi",
    ) = ThreadItem.Message(
        serverId = serverId,
        clientMsgId = clientMsgId,
        senderId = "ME",
        text = text,
        createdAt = t(at),
        mine = true,
        delivery = delivery,
    )

    private fun peer(
        serverId: String,
        at: Long,
        delivery: Delivery = Delivery.Sent,
        clientMsgId: String? = null,
    ) = ThreadItem.Message(
        serverId = serverId,
        clientMsgId = clientMsgId,
        senderId = "PEER",
        text = "yo",
        createdAt = t(at),
        mine = false,
        delivery = delivery,
    )

    @Test
    fun ownMessageSendingThenAckThenReloadStaysOneRow() {
        val bubble = mine(clientMsgId = "c1")
        var items = ThreadReducer.appendLocal(emptyList(), bubble)
        val keyBefore = items.single().key

        items = ThreadReducer.ack(items, "c1", "m9", t(200))
        val acked = items.single() as ThreadItem.Message
        assertEquals("m9", acked.serverId)
        assertEquals(Delivery.Sent, acked.delivery)
        assertEquals(t(200), acked.createdAt)

        val server = mine(clientMsgId = "c1", serverId = "m9", delivery = Delivery.Sent, at = 200)
        items = ThreadReducer.mergePage(items, listOf(server))
        assertEquals(1, items.size)
        assertEquals(keyBefore, items.single().key)
    }

    @Test
    fun incomingPageMergesIntoEmptyList() {
        val page = listOf(peer("b", 20), peer("a", 10), peer("c", 30))
        val merged = ThreadReducer.mergePage(emptyList(), page)
        assertEquals(listOf("a", "b", "c"), merged.map { it.serverId })
    }

    @Test
    fun ackAfterReloadAlreadyAddedTheRowKeepsOne() {
        var items = ThreadReducer.appendLocal(emptyList(), mine(clientMsgId = "c1"))
        val server = mine(clientMsgId = "c1", serverId = "m9", delivery = Delivery.Sent, at = 200)
        items = ThreadReducer.mergePage(items, listOf(server))
        assertEquals(1, items.size)

        items = ThreadReducer.ack(items, "c1", "m9", t(200))
        assertEquals(1, items.size)
        assertEquals("m9", items.single().serverId)
    }
}
