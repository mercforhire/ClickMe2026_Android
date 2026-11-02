package com.uptrendinvestments.clickmeapp.screens.settings

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.openUpcomingBookingFromProfile
import com.uptrendinvestments.clickmeapp.screens.clientmode.payments.ClientTransactionsKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.payments.ClientTransactionsScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.PayoutsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.PayoutsScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.TransactionsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.TransactionsScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertProfileSettingsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertProfileSettingsScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile.ExpertTaxProfileScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile.TaxProfileKey
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
 * Client side: Profile Settings, the photo crop it opens, Payment History (its rows open the booking on the Profile
 * stack), Account Security (and Delete Account behind it), Help, and the shared Notifications and Send Feedback
 * entries. The expert side is [expertSettingsEntries].
 */
fun EntryProviderScope<NavKey>.clientSettingsEntries(navigator: Navigator) {
    entry<ClientTransactionsKey>(metadata = BottomBar.hidden()) {
        ClientTransactionsScreen(
            onBack = navigator::goBack,
            onOpenBooking = navigator::openUpcomingBookingFromProfile,
        )
    }
    entry<ClientProfileSettingsKey>(metadata = BottomBar.hidden()) {
        ClientProfileSettingsScreen(
            viewModel = hiltViewModel(),
            onBack = navigator::goBack,
            onOpenCrop = { navigator.navigate(ProfilePhotoCropKey(SettingsOrigin.Profile)) },
        )
    }
    accountAndHelpEntries(navigator)
}

/**
 * Expert side: Expert Settings, Payouts (with its Activity and Tax & Payout Info cross-links) and the shared account
 * entries. The [SettingsOrigin] a screen was opened with travels on to every key it pushes, so the Profile and
 * Dashboard stacks never hold equal keys.
 */
fun EntryProviderScope<NavKey>.expertSettingsEntries(navigator: Navigator) {
    entry<ExpertProfileSettingsKey>(metadata = BottomBar.hidden()) { key ->
        ExpertProfileSettingsScreen(
            viewModel = hiltViewModel(),
            origin = key.origin,
            onBack = navigator::goBack,
            onOpenCrop = { origin -> navigator.navigate(ProfilePhotoCropKey(origin)) },
        )
    }
    entry<PayoutsKey>(metadata = BottomBar.hidden()) { key ->
        PayoutsScreen(
            origin = key.origin,
            onBack = navigator::goBack,
            onOpenActivity = { origin -> navigator.navigate(TransactionsKey(origin)) },
            onOpenTaxProfile = { origin -> navigator.navigate(TaxProfileKey(origin)) },
        )
    }
    entry<TransactionsKey>(metadata = BottomBar.hidden()) {
        TransactionsScreen(onBack = navigator::goBack)
    }
    entry<TaxProfileKey>(metadata = BottomBar.hidden()) {
        ExpertTaxProfileScreen(onBack = navigator::goBack)
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
