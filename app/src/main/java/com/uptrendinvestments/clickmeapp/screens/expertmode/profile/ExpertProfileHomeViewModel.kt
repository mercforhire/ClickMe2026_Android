package com.uptrendinvestments.clickmeapp.screens.expertmode.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ProfileHubCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.profileHubCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Backs the expert Profile hub: the same cached-profile card as the client hub, plus the key for "View My Public
 * Profile". The iOS hard-coded online dot is not ported (D-06).
 */
@HiltViewModel
class ExpertProfileHomeViewModel
    @Inject
    constructor(
        private val userManager: UserManager,
    ) : ViewModel() {
        val card: StateFlow<ProfileHubCard> =
            combine(userManager.profile, userManager.authUser, userManager.me, ::profileHubCard)
                .stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    profileHubCard(userManager.profile.value, userManager.authUser.value, userManager.me.value),
                )

        /**
         * The self-view key for the signed-in expert's public profile (D-02), or null when no user id is known yet.
         * The fields are only a display seed; the screen loads the real profile by [ExpertProfileKey.expertId].
         */
        fun publicProfileKey(): ExpertProfileKey? {
            val id = userManager.authUser.value?.id ?: userManager.me.value?.id ?: return null
            val shown = card.value
            return ExpertProfileKey(
                expertId = id,
                name = shown.name.orEmpty(),
                title =
                    userManager.profile.value
                        ?.professionalDetails
                        ?.jobTitle
                        ?.trim()
                        .orEmpty(),
                imageUrl = shown.avatarUrl,
                isSelfView = true,
            )
        }
    }
