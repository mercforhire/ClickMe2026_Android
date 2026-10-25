package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** Load state of the expert's topic list. An empty list is [Loaded], not a failure (EXP-01). */
sealed interface TopicsListState {
    data object Loading : TopicsListState

    data class Failed(
        val message: FieldError,
    ) : TopicsListState

    data class Loaded(
        val topics: List<ExpertTopicItem>,
    ) : TopicsListState
}

/** An error alert: [title] is a string resource, [message] the server text or standard copy. */
data class TopicsDialog(
    @StringRes val title: Int,
    val message: FieldError,
)

/** One-shot snackbar messages of the Topics tab (D-05, D-06, D-07). */
enum class TopicsSnackbar { Added, Updated, Deleted, RefreshFailed }

/**
 * State of the Topics tab. [deletingId] is the single in-flight delete (D-06); [isRefreshing] drives the
 * pull-to-refresh indicator; [snackbar] and [dialog] are consumed by the screen.
 */
data class TopicsUiState(
    val list: TopicsListState = TopicsListState.Loading,
    val isRefreshing: Boolean = false,
    val deletingId: String? = null,
    val dialog: TopicsDialog? = null,
    val snackbar: TopicsSnackbar? = null,
)
