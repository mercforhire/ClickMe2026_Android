package com.uptrendinvestments.clickmeapp.screens.settings

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.AccountSecurityKey
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.AccountSecurityScreen
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ClientProfileSettingsKey
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ClientProfileSettingsScreen
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfilePhotoCropKey
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.DeleteAccountKey
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.DeleteAccountScreen
import com.uptrendinvestments.clickmeapp.screens.settings.help.HelpKey
import com.uptrendinvestments.clickmeapp.screens.settings.help.HelpScreen
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoCropScreen

/**
 * The one place that maps the Phase 14 screens' callbacks to navigation keys (feature plans built the screens with
 * plain callbacks so they could be written in parallel). Every pushed screen hides the bottom bar.
 *
 * Client side: Profile Settings, the photo crop it opens, Account Security (and Delete Account behind it) and Help.
 * The expert side adds Expert Settings, Payouts, Activity and Tax & Payout Info (see [expertSettingsEntries]).
 */
fun EntryProviderScope<NavKey>.clientSettingsEntries(navigator: Navigator) {
    entry<ClientProfileSettingsKey>(metadata = BottomBar.hidden()) {
        ClientProfileSettingsScreen(
            viewModel = hiltViewModel(),
            onBack = navigator::goBack,
            onOpenCrop = { navigator.navigate(ProfilePhotoCropKey(SettingsOrigin.Profile)) },
        )
    }
    accountAndHelpEntries(navigator)
}

/** Entries both modes share: the photo crop, Account Security, Delete Account and Help. */
private fun EntryProviderScope<NavKey>.accountAndHelpEntries(navigator: Navigator) {
    entry<ProfilePhotoCropKey>(metadata = BottomBar.hidden()) {
        PhotoCropScreen(onDone = navigator::goBack, onCancel = navigator::goBack)
    }
    entry<AccountSecurityKey>(metadata = BottomBar.hidden()) {
        AccountSecurityScreen(
            onBack = navigator::goBack,
            onOpenDeleteAccount = { navigator.navigate(DeleteAccountKey) },
        )
    }
    entry<DeleteAccountKey>(metadata = BottomBar.hidden()) {
        DeleteAccountScreen(onBack = navigator::goBack)
    }
    entry<HelpKey>(metadata = BottomBar.hidden()) {
        HelpScreen(viewModel = hiltViewModel(), onBack = navigator::goBack)
    }
}
