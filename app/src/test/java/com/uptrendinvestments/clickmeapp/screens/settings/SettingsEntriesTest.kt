package com.uptrendinvestments.clickmeapp.screens.settings

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.NavigationState
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.AccountSecurityKey
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ClientProfileSettingsKey
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfilePhotoCropKey
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.DeleteAccountKey
import com.uptrendinvestments.clickmeapp.screens.settings.help.HelpKey
import org.junit.Assert.assertEquals
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
        ).forEach { key ->
            assertEquals("bottom bar must be hidden on $key", true, hiddenOf(provider, key))
        }
    }
}
