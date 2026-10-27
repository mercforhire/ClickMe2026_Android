package com.uptrendinvestments.clickmeapp.screens.chat

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSessionStateTest {
    @Test
    fun openThreadShownAndHidden() {
        val state = ChatSessionState()

        state.onThreadShown("T1")
        assertEquals("T1", state.openThreadId.value)

        state.onThreadHidden("t2")
        assertEquals("T1", state.openThreadId.value)

        state.onThreadHidden("t1")
        assertNull(state.openThreadId.value)
    }

    @Test
    fun blockedByMeIsCaseInsensitive() {
        val state = ChatSessionState()

        state.markBlockedByMe("T1")
        assertTrue(state.isBlockedByMe("t1"))

        state.clearBlockedByMe("t1")
        assertFalse(state.isBlockedByMe("T1"))
    }

    @Test
    fun clearSessionDataResetsEverything() =
        runTest {
            val state = ChatSessionState()
            state.markBlockedByMe("T1")
            state.onThreadShown("T1")

            state.clearSessionData()

            assertNull(state.openThreadId.value)
            assertFalse(state.isBlockedByMe("T1"))
        }
}
