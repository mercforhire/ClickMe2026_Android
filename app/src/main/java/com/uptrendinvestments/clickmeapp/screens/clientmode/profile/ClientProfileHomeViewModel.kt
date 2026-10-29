package com.uptrendinvestments.clickmeapp.screens.clientmode.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * What the Profile hub's user card shows. Every field is null when unknown: the card then reads "Your Profile", omits
 * the email and draws the person silhouette (UI-SPEC "User card" fallbacks).
 */
data class ProfileHubCard(
    val name: String?,
    val email: String?,
    val avatarUrl: String?,
)

/**
 * Maps the cached session state to the hub card. The cached profile wins; the login payload ([authUser]) and `/me`
 * fill gaps before the profile has been fetched. Blank values become null. No network call (the hubs never show a
 * skeleton or an error).
 */
fun profileHubCard(
    profile: UserProfileData?,
    authUser: AuthUser?,
    me: MeData?,
): ProfileHubCard {
    val personal = profile?.personalDetails
    val name =
        joinName(personal?.firstName, personal?.lastName)
            ?: joinName(authUser?.firstName, authUser?.lastName)
    val email = personal?.email.nonBlank() ?: me?.email.nonBlank()
    val avatar = personal?.avatarUrl.nonBlank() ?: authUser?.avatarUrl.nonBlank()
    return ProfileHubCard(name = name, email = email?.trim(), avatarUrl = avatar)
}

private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

private fun joinName(
    first: String?,
    last: String?,
): String? =
    listOfNotNull(first?.trim(), last?.trim())
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifEmpty { null }

/** Backs the client Profile hub. Log out and mode switching stay with the shell, which passes them as callbacks. */
@HiltViewModel
class ClientProfileHomeViewModel
    @Inject
    constructor(
        userManager: UserManager,
    ) : ViewModel() {
        val card: StateFlow<ProfileHubCard> =
            combine(userManager.profile, userManager.authUser, userManager.me, ::profileHubCard)
                .stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    profileHubCard(userManager.profile.value, userManager.authUser.value, userManager.me.value),
                )
    }
