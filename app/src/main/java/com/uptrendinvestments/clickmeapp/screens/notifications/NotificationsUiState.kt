package com.uptrendinvestments.clickmeapp.screens.notifications

import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** The load state of the notifications list. */
sealed interface NotificationsLoad {
    data object Loading : NotificationsLoad

    data class Failed(
        val message: FieldError,
    ) : NotificationsLoad

    /** [sections] is empty when the user has no notifications (the screen shows the empty state). */
    data class Loaded(
        val sections: List<NotificationSection>,
    ) : NotificationsLoad
}

data class NotificationsUiState(
    val load: NotificationsLoad = NotificationsLoad.Loading,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)
