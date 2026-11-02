package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/**
 * What the Client Profile screen shows. [NoRelationship] (D-14) and [Unavailable] (D-15) are terminal for a retry:
 * neither offers Retry, only pull-to-refresh.
 */
sealed interface ClientProfileLoad {
    data object Loading : ClientProfileLoad

    data class Failed(
        val message: FieldError,
    ) : ClientProfileLoad

    data object NoRelationship : ClientProfileLoad

    data object Unavailable : ClientProfileLoad

    data class Loaded(
        val profile: ExpertClientProfileData,
        val history: List<ClientHistoryRow>,
    ) : ClientProfileLoad
}

/** [isRefreshing] is a pull over loaded content; [refreshFailed] is raised once when that pull fails. */
data class ExpertClientProfileUiState(
    val load: ClientProfileLoad = ClientProfileLoad.Loading,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)
