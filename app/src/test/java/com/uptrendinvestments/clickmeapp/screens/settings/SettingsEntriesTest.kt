package com.uptrendinvestments.clickmeapp.screens.settings

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.NavigationState
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.payments.ClientTransactionsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.expertProfileEntry
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.PayoutsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.TransactionsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertProfileSettingsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile.TaxProfileKey
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.AccountSecurityKey
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ClientProfileSettingsKey
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfilePhotoCropKey
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.DeleteAccountKey
import com.uptrendinvestments.clickmeapp.screens.settings.feedback.FeedbackKey
import com.uptrendinvestments.clickmeapp.screens.settings.help.HelpKey
import com.uptrendinvestments.clickmeapp.screens.settings.notifications.NotificationSettingsKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The registration functions are the one place that maps Phase 14 screens to keys. Entry metadata can be read
 * without composing the (Hilt-backed) content, so these tests need no Android runtime.
 */
class SettingsEntriesTest {
    private object Root : NavKey

    private fun navigator(): Navigator =
        Navigator(
            NavigationState(
                startRoute = Root,
                topLevelRoute = mutableStateOf<NavKey>(Root),
                backStacks = mapOf(Root to NavBackStack<NavKey>(Root)),
            ),
        )

    private fun hiddenOf(
        provider: (NavKey) -> androidx.navigation3.runtime.NavEntry<NavKey>,
        key: NavKey,
    ): Any? = provider(key).metadata[BottomBar.HIDE_KEY]

    @Test
    fun clientEntriesRegisterEveryKeyHidden() {
        val provider = entryProvider<NavKey> { clientSettingsEntries(navigator()) }
        listOf<NavKey>(
            ClientProfileSettingsKey,
            ProfilePhotoCropKey(SettingsOrigin.Profile),
            AccountSecurityKey,
            DeleteAccountKey,
            HelpKey,
            ClientTransactionsKey,
            NotificationSettingsKey,
            FeedbackKey,
        ).forEach { key ->
            assertEquals("bottom bar must be hidden on $key", true, hiddenOf(provider, key))
        }
    }

    @Test
    fun expertEntriesRegisterEveryKeyHidden() {
        val provider = entryProvider<NavKey> { expertSettingsEntries(navigator()) }
        listOf<NavKey>(
            ExpertProfileSettingsKey(SettingsOrigin.Profile),
            ExpertProfileSettingsKey(SettingsOrigin.Dashboard),
            PayoutsKey(SettingsOrigin.Profile),
            PayoutsKey(SettingsOrigin.Dashboard),
            TransactionsKey(SettingsOrigin.Dashboard),
            TaxProfileKey(SettingsOrigin.Profile),
            ProfilePhotoCropKey(SettingsOrigin.Dashboard),
            AccountSecurityKey,
            DeleteAccountKey,
            HelpKey,
            NotificationSettingsKey,
            FeedbackKey,
        ).forEach { key ->
            assertEquals("bottom bar must be hidden on $key", true, hiddenOf(provider, key))
        }
    }

    @Test
    fun expertEntriesDoNotRegisterPaymentHistory() {
        val provider = entryProvider<NavKey> { expertSettingsEntries(navigator()) }
        assertThrows(IllegalStateException::class.java) { provider(ClientTransactionsKey) }
    }

    @Test
    fun expertShellProfileEntryHidesTheBar() {
        val provider = entryProvider<NavKey> { expertProfileEntry(navigator()) }
        val key = ExpertProfileKey("e1", "Ada", "Engineer", null, isSelfView = true)
        assertEquals(true, hiddenOf(provider, key))
    }
}
