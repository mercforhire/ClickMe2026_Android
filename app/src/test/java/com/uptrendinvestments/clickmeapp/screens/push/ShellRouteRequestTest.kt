package com.uptrendinvestments.clickmeapp.screens.push

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@Serializable
private data object SrTab : NavKey

@Serializable
private data class SrKey(
    val id: String,
) : NavKey

class ShellRouteRequestTest {
    private fun route(
        mode: AppMode,
        id: String = "a",
    ) = ShellRoute(mode, SrTab, listOf(SrKey(id)))

    @Test
    fun consumeForMatchingModeClears() {
        val request = ShellRouteRequest()
        val posted = route(AppMode.Client)
        request.post(posted)

        assertEquals(posted, request.consumeFor(AppMode.Client))
        assertNull(request.pending.value)
        assertNull(request.consumeFor(AppMode.Client))
    }

    @Test
    fun consumeForOtherModeLeavesPending() {
        val request = ShellRouteRequest()
        val posted = route(AppMode.Expert)
        request.post(posted)

        assertNull(request.consumeFor(AppMode.Client))
        assertEquals(posted, request.pending.value)
        assertEquals(posted, request.consumeFor(AppMode.Expert))
    }

    @Test
    fun newerPostReplaces() {
        val request = ShellRouteRequest()
        request.post(route(AppMode.Client, "first"))
        val second = route(AppMode.Expert, "second")
        request.post(second)

        assertEquals(second, request.pending.value)
        assertNull(request.consumeFor(AppMode.Client))
    }

    @Test
    fun clearSessionDataDrops() =
        runTest {
            val request = ShellRouteRequest()
            request.post(route(AppMode.Client))

            request.clearSessionData()

            assertNull(request.pending.value)
        }
}
