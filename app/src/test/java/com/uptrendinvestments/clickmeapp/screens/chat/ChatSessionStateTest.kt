package com.uptrendinvestments.clickmeapp.screens.chat

import kotlinx.coroutines.flow.first
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

    @Test
    fun unsentTextIsPerOwnerAndThreadAndTakenOnce() {
        val state = ChatSessionState()
        state.keepUnsentText("T1", "Me", "hi")

        assertNull(state.takeUnsentText("t1", "other"))
        assertNull(state.takeUnsentText("t2", "me"))
        assertNull(state.takeUnsentText("t1", null))
        assertEquals("hi", state.takeUnsentText("t1", "me"))
        assertNull(state.takeUnsentText("t1", "me"))
    }

    @Test
    fun unsentTextsForOneThreadJoinInOrder() {
        val state = ChatSessionState()
        state.keepUnsentText("t1", "me", "a")
        state.keepUnsentText("T1", "ME", "b")

        assertEquals("a\nb", state.takeUnsentText("t1", "me"))
    }

    @Test
    fun unsentKeysSignalCarriesNoText() =
        runTest {
            val state = ChatSessionState()
            state.keepUnsentText("t1", "me", "secret words")

            val keys = state.unsentKeys.first()

            assertEquals(1, keys.size)
            assertFalse(keys.any { it.contains("secret") })
        }

    @Test
    fun clearSessionDataDropsUnsentText() =
        runTest {
            val state = ChatSessionState()
            state.keepUnsentText("t1", "me", "hi")

            state.clearSessionData()

            assertNull(state.takeUnsentText("t1", "me"))
        }
}
