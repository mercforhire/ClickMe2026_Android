package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun failRetryRemove() {
        var items = ThreadReducer.appendLocal(emptyList(), mine(clientMsgId = "c1"))

        items = ThreadReducer.fail(items, "c1")
        assertEquals(Delivery.Failed, (items.single() as ThreadItem.Message).delivery)

        items = ThreadReducer.retry(items, "c1")
        val retried = items.single() as ThreadItem.Message
        assertEquals(Delivery.Sending, retried.delivery)
        assertEquals("c1", retried.clientMsgId)

        items = ThreadReducer.remove(items, "c1")
        assertTrue(items.isEmpty())

        val sent = listOf<ThreadItem>(mine(clientMsgId = "c2", serverId = "m2", delivery = Delivery.Sent))
        assertEquals(sent, ThreadReducer.remove(sent, "c2"))
    }

    @Test
    fun lostResponseThenReloadConvergesToOneSentRow() {
        var items = ThreadReducer.appendLocal(emptyList(), mine(clientMsgId = "c1"))
        items = ThreadReducer.fail(items, "c1")
        val server = mine(clientMsgId = "c1", serverId = "m9", delivery = Delivery.Sent, at = 150)

        items = ThreadReducer.mergePage(items, listOf(server))

        val row = items.single() as ThreadItem.Message
        assertEquals("m9", row.serverId)
        assertEquals(Delivery.Sent, row.delivery)
    }

    @Test
    fun concurrentSendsCoexist() {
        var items = ThreadReducer.mergePage(emptyList(), listOf(peer("p1", 10), peer("p2", 20)))
        items = ThreadReducer.appendLocal(items, mine(clientMsgId = "c1"))
        items = ThreadReducer.appendLocal(items, mine(clientMsgId = "c2"))
        items = ThreadReducer.appendLocal(items, mine(clientMsgId = "c3"))

        assertEquals(listOf("p1", "p2", null, null, null), items.map { it.serverId })
        assertEquals(
            listOf("c1", "c2", "c3"),
            items.drop(2).map { (it as ThreadItem.Message).clientMsgId },
        )
    }

    @Test
    fun incomingIgnoresOwnAndDuplicates() {
        val base = ThreadReducer.mergePage(emptyList(), listOf(peer("p1", 10)))

        val own = peer("p2", 20).copy(senderId = "me")
        assertEquals(base, ThreadReducer.onIncoming(base, own, myId = "ME"))

        val duplicate = peer("P1", 10)
        assertEquals(base, ThreadReducer.onIncoming(base, duplicate, myId = "ME"))

        val added = ThreadReducer.onIncoming(base, peer("p3", 5), myId = "ME")
        assertEquals(listOf("p3", "p1"), added.map { it.serverId })
    }

    @Test
    fun markReadFlipsMineOnlyAndNeverDowngrades() {
        val mineRow = mine(clientMsgId = "c1", serverId = "m1", delivery = Delivery.Sent, at = 10)
        val peerRow = peer("m1-peer", 20)
        var items = ThreadReducer.mergePage(emptyList(), listOf(mineRow, peerRow))

        items = ThreadReducer.markRead(items, listOf("M1", "m1-peer"))

        val byId = items.associateBy { it.serverId }
        assertEquals(Delivery.Read, (byId.getValue("m1") as ThreadItem.Message).delivery)
        assertEquals(Delivery.Sent, (byId.getValue("m1-peer") as ThreadItem.Message).delivery)

        val stale = mine(clientMsgId = "c1", serverId = "m1", delivery = Delivery.Sent, at = 10)
        items = ThreadReducer.mergePage(items, listOf(stale))
        assertEquals(Delivery.Read, (ThreadReducer.findMine(items, "c1"))?.delivery)
    }

    @Test
    fun prependOlderOverlapHasNoDuplicates() {
        var items = ThreadReducer.mergePage(emptyList(), listOf(peer("p3", 30), peer("p4", 40)))

        items = ThreadReducer.mergePage(items, listOf(peer("p1", 10), peer("p2", 20), peer("p3", 30), peer("p4", 40)))

        assertEquals(listOf("p1", "p2", "p3", "p4"), items.map { it.serverId })
    }

    @Test
    fun equalTimestampsKeepStableOrder() {
        val page = listOf(peer("b", 10), peer("a", 10), peer("c", 10))
        val items = ThreadReducer.mergePage(emptyList(), page)
        assertEquals(listOf("b", "a", "c"), items.map { it.serverId })
    }

    @Test
    fun unreadPeerIdsSkipsMineReadAndEmitted() {
        val items =
            ThreadReducer.mergePage(
                emptyList(),
                listOf(
                    peer("P1", 10),
                    peer("p2", 20, Delivery.Read),
                    peer("p3", 30),
                    peer("p4", 40),
                    mine(clientMsgId = "c1", serverId = "m1", delivery = Delivery.Sent, at = 50),
                ),
            )

        val unread = ThreadReducer.unreadPeerIds(items, alreadyEmitted = setOf("p3"))

        assertEquals(listOf("P1", "p4"), unread)
    }

    @Test
    fun ackDropsBubbleWhenServerRowAlreadyPresentWithoutClientMsgId() {
        var items = ThreadReducer.appendLocal(emptyList(), mine(clientMsgId = "c1"))
        items = ThreadReducer.mergePage(items, listOf(mine(serverId = "m9", delivery = Delivery.Read, at = 200)))
        // The page row has no client_msg_id and a different shape, so the bubble was not matched by it.
        assertEquals(2, items.size)

        items = ThreadReducer.ack(items, "c1", "M9", t(200))

        val row = items.single() as ThreadItem.Message
        assertEquals("m9", row.serverId)
        assertEquals("c1", row.clientMsgId)
        assertEquals(Delivery.Read, row.delivery)
    }

    @Test
    fun ackOnUnknownClientMsgIdChangesNothing() {
        val items = ThreadReducer.appendLocal(emptyList(), mine(clientMsgId = "c1"))
        assertEquals(items, ThreadReducer.ack(items, "zzz", "m9", t(1)))
    }

    @Test
    fun systemEventKindFromWire() {
        assertEquals(SystemEventKind.BookingRequest, SystemEventKind.fromWire("booking_request"))
        assertEquals(SystemEventKind.BookingConfirmed, SystemEventKind.fromWire("BOOKING_CONFIRMED"))
        assertEquals(SystemEventKind.BookingConfirmed, SystemEventKind.fromWire("booking_confirmed"))
        assertEquals(SystemEventKind.Unknown, SystemEventKind.fromWire(null))
        assertEquals(SystemEventKind.Unknown, SystemEventKind.fromWire("something"))
        assertTrue(SystemEventKind.BookingDeclined.isTerminal)
        assertTrue(SystemEventKind.BookingCancelled.isTerminal)
        assertTrue(SystemEventKind.BookingCompleted.isTerminal)
        assertTrue(SystemEventKind.BookingExpired.isTerminal)
        assertFalse(SystemEventKind.BookingConfirmed.isTerminal)
        assertFalse(SystemEventKind.BookingRescheduled.isTerminal)
    }

    @Test
    fun peerRowSharingMyClientMsgIdKeepsADistinctKey() {
        val own = mine(clientMsgId = "dup", serverId = "m1", delivery = Delivery.Sent, at = 100)
        var items = ThreadReducer.mergePage(emptyList(), listOf(own))
        val peerRow = peer("m2", 200, clientMsgId = "DUP")
        items = ThreadReducer.mergePage(items, listOf(own, peerRow))
        assertEquals(2, items.size)
        assertEquals(listOf("c:dup", "s:m2"), items.map { it.key })
    }

    @Test
    fun peerSocketRowKeepsItsKeyWhenAReloadAddsClientMsgId() {
        val socketRow = peer("m5", 50)
        var items = ThreadReducer.mergePage(emptyList(), listOf(socketRow))
        assertEquals("s:m5", items.single().key)
        items = ThreadReducer.mergePage(items, listOf(peer("m5", 50, clientMsgId = "x9")))
        assertEquals(1, items.size)
        assertEquals("s:m5", items.single().key)
    }

    @Test
    fun keysAreUniqueInAMixedThread() {
        val event =
            ThreadItem.SystemEvent(
                serverId = "e1",
                kind = SystemEventKind.BookingCancelled,
                bookingId = null,
                topic = null,
                startTime = null,
                content = null,
                createdAt = t(10),
            )
        var items = ThreadReducer.mergePage(emptyList(), listOf(mine("c1", "m1", Delivery.Sent, 20), event))
        items = ThreadReducer.appendLocal(items, mine(clientMsgId = "c2"))
        items = ThreadReducer.mergePage(items, listOf(peer("m3", 30, clientMsgId = "c1")))
        val keys = items.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.containsAll(listOf("c:c1", "c:c2")))
    }

    @Test
    fun deliveryFromServerStatus() {
        assertEquals(Delivery.Read, Delivery.fromServerStatus("READ"))
        assertEquals(Delivery.Sent, Delivery.fromServerStatus("sent"))
        assertEquals(Delivery.Sent, Delivery.fromServerStatus(null))
    }
}
