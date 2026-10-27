package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatLaunchRequestTest {
    private val first = ChatThreadKey("t1", "p1", "Ada Lovelace", null, true)
    private val second = ChatThreadKey("t2", "p2", "Grace Hopper", null, false)

    @Test
    fun newStoreIsEmpty() {
        val store = ChatLaunchRequest()
        assertNull(store.thread.pending.value)
        assertNull(store.conversationsNotice.pending.value)
        assertNull(store.threadNotice.pending.value)
    }

    @Test
    fun postedKeyIsConsumedOnce() {
        val store = ChatLaunchRequest()
        store.thread.post(first)
        assertEquals(first, store.thread.pending.value)
        assertEquals(first, store.thread.consume())
        assertNull(store.thread.pending.value)
        assertNull(store.thread.consume())
    }

    @Test
    fun laterPostReplacesUnconsumed() {
        val store = ChatLaunchRequest()
        store.thread.post(first)
        store.thread.post(second)
        assertEquals(second, store.thread.consume())
        assertNull(store.thread.consume())
    }

    @Test
    fun slotsAreIndependent() {
        val store = ChatLaunchRequest()
        store.thread.post(first)
        store.conversationsNotice.post(ConversationsNotice.Blocked("Ada Lovelace"))
        assertNull(store.threadNotice.consume())
        assertEquals(ConversationsNotice.Blocked("Ada Lovelace"), store.conversationsNotice.consume())
        assertEquals(first, store.thread.consume())

        store.threadNotice.post(ThreadNotice.Unblocked("t1", "Ada Lovelace"))
        assertNull(store.thread.consume())
        assertNull(store.conversationsNotice.consume())
        assertEquals(ThreadNotice.Unblocked("t1", "Ada Lovelace"), store.threadNotice.consume())
    }
}
