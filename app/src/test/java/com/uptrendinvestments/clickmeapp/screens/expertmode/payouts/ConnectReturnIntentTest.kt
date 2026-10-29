package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.MainActivity
import com.uptrendinvestments.clickmeapp.screens.expertmode.ConnectReturn
import com.uptrendinvestments.clickmeapp.screens.expertmode.DashboardLaunch
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectReturnIntentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun returnUriMapsToDone() {
        assertEquals(ConnectReturn.Done, connectReturnFrom(Uri.parse("clickme://connect/return")))
        assertEquals(ConnectReturn.LinkExpired, connectReturnFrom(Uri.parse("clickme://connect/refresh")))
    }

    @Test
    fun receiverPostsBothSlots() {
        val request = ExpertLaunchRequest()

        val handled = ConnectReturnReceiver(request).receive(Uri.parse("clickme://connect/return"))

        assertTrue(handled)
        assertEquals(DashboardLaunch, request.dashboard.pending.value)
        assertEquals(ConnectReturn.Done, request.connectReturn.pending.value)
    }

    @Test
    fun receiverPostsLinkExpiredForRefresh() {
        val request = ExpertLaunchRequest()

        assertTrue(ConnectReturnReceiver(request).receive(Uri.parse("clickme://connect/refresh")))

        assertEquals(DashboardLaunch, request.dashboard.pending.value)
        assertEquals(ConnectReturn.LinkExpired, request.connectReturn.pending.value)
    }

    @Test
    fun relaunchIntentClearsTopAndIsSingleTop() {
        val intent = mainActivityRelaunchIntent(context)

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertNull(intent.data)
        assertNull(intent.extras)
    }

    @Test
    fun unrecognisedUrisAreIgnored() {
        val ignored =
            listOf(
                "clickme://connect",
                "clickme://connect/",
                "clickme://connect/return/extra",
                "clickme://connect/done",
                "clickme://other/return",
                "https://clickme-backend-production.up.railway.app/connect/return",
                "intent://connect/return",
                "clickmex://connect/return",
            )
        assertNull(connectReturnFrom(null))
        ignored.forEach { assertNull(it, connectReturnFrom(Uri.parse(it))) }
    }

    @Test
    fun queryAndFragmentDoNotChangeTheOutcome() {
        assertEquals(
            ConnectReturn.Done,
            connectReturnFrom(Uri.parse("clickme://connect/return?status=complete&account=acct_1#x")),
        )
        assertEquals(
            ConnectReturn.LinkExpired,
            connectReturnFrom(Uri.parse("clickme://connect/refresh?url=https://evil.example")),
        )

        val request = ExpertLaunchRequest()
        ConnectReturnReceiver(request).receive(Uri.parse("clickme://connect/refresh?url=https://evil.example"))
        assertEquals(DashboardLaunch, request.dashboard.pending.value)
        assertEquals(ConnectReturn.LinkExpired, request.connectReturn.pending.value)
    }

    @Test
    fun receiverIgnoresForgedUriAndPostsNothing() {
        val request = ExpertLaunchRequest()

        val handled = ConnectReturnReceiver(request).receive(Uri.parse("clickme://connect/hack"))

        assertFalse(handled)
        assertNull(request.dashboard.pending.value)
        assertNull(request.connectReturn.pending.value)
    }

    @Test
    fun receiverIgnoresNullUri() {
        val request = ExpertLaunchRequest()

        assertFalse(ConnectReturnReceiver(request).receive(null))

        assertNull(request.dashboard.pending.value)
        assertNull(request.connectReturn.pending.value)
    }

    @Test
    fun casingOfSchemeAndHostIsTolerated() {
        assertEquals(ConnectReturn.Done, connectReturnFrom(Uri.parse("CLICKME://CONNECT/return")))
        assertNull(connectReturnFrom(Uri.parse("clickme://connect/RETURN")))
    }
}
