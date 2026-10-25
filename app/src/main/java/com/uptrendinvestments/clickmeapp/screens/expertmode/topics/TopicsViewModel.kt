package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.toExpertDialogError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State holder for the Topics tab (EXP-01). Port of iOS `TopicsSetupViewModel`.
 *
 * Three load modes: [LoadMode.Initial] (first load and Retry: skeleton, then list or error), [LoadMode.Pull]
 * (pull-to-refresh: indicator, a failure raises a snackbar) and [LoadMode.Quiet] (tab or editor return: no
 * skeleton, a failure changes nothing). A generation counter lets only the newest load apply its answer, so a
 * reload in flight can never put a deleted topic back (D-06).
 *
 * One delete at a time (D-06). A topic saved in the editor arrives through [TopicEvents] and is merged in place
 * (edit) or appended (create) before a quiet reload confirms it (D-07).
 */
@HiltViewModel
class TopicsViewModel
    @Inject
    constructor(
        private val repository: ExpertTopicsRepository,
        private val topicEvents: TopicEvents,
    ) : ViewModel() {
        private enum class LoadMode { Initial, Pull, Quiet }

        private val _uiState = MutableStateFlow(TopicsUiState())
        val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        /** Bumped by every load and by a successful delete; only the newest load may apply its answer. */
        private var generation = 0

        init {
            load(LoadMode.Initial)
            viewModelScope.launch {
                topicEvents.events.collect { event -> merge(event) }
            }
        }

        fun retry() = load(LoadMode.Initial)

        fun refresh() = load(LoadMode.Pull)

        /**
         * The tab or the screen is shown again (also after the editor closes). The screen calls this from both
         * `LaunchedEffect(Unit)` and `LifecycleEventEffect(ON_RESUME)`; a load already in flight makes the second
         * call a no-op, so first composition never loads twice.
         */
        fun onScreenShown() {
            if (_uiState.value.list !is TopicsListState.Loaded) return
            if (loadJob?.isActive == true) return
            load(LoadMode.Quiet)
        }

        private fun load(mode: LoadMode) {
            loadJob?.cancel()
            val current = ++generation
            _uiState.update {
                when (mode) {
                    LoadMode.Initial -> it.copy(list = TopicsListState.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val topics = repository.list()
                        if (current == generation) {
                            _uiState.update { it.copy(list = TopicsListState.Loaded(topics), isRefreshing = false) }
                        }
                    } catch (e: ApiException) {
                        if (current != generation) return@launch
                        _uiState.update {
                            when (mode) {
                                LoadMode.Initial ->
                                    it.copy(list = TopicsListState.Failed(e.toSectionMessage()), isRefreshing = false)
                                LoadMode.Pull ->
                                    it.copy(isRefreshing = false, snackbar = TopicsSnackbar.RefreshFailed)
                                LoadMode.Quiet -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }

        /** Applies a topic saved in the editor, then confirms with the server's list. */
        private fun merge(event: TopicSavedEvent) {
            _uiState.update { state ->
                val list = state.list as? TopicsListState.Loaded ?: return@update state
                // Updated replaces in place and Added appends; either way an id already present is replaced.
                state.copy(
                    list = TopicsListState.Loaded(list.topics.replaceOrAppend(event.topic)),
                    snackbar =
                        when (event.kind) {
                            TopicSavedKind.Updated -> TopicsSnackbar.Updated
                            TopicSavedKind.Added -> TopicsSnackbar.Added
                        },
                )
            }
            // A load still showing the skeleton restarts as Initial: a quiet failure would leave it stuck.
            load(if (_uiState.value.list is TopicsListState.Loading) LoadMode.Initial else LoadMode.Quiet)
        }

        private fun List<ExpertTopicItem>.replaceOrAppend(topic: ExpertTopicItem): List<ExpertTopicItem> =
            if (any { it.id == topic.id }) map { if (it.id == topic.id) topic else it } else this + topic

        /** Called after the user confirms the delete in the screen's dialog (D-06). */
        fun delete(topicId: String) {
            val state = _uiState.value
            if (state.deletingId != null || state.list !is TopicsListState.Loaded) return
            _uiState.update { it.copy(deletingId = topicId) }
            viewModelScope.launch {
                try {
                    repository.delete(topicId)
                    // A reload in flight predates the delete: cancel it and invalidate its answer.
                    loadJob?.cancel()
                    generation++
                    _uiState.update { current ->
                        val remaining =
                            (current.list as? TopicsListState.Loaded)
                                ?.let { TopicsListState.Loaded(it.topics.filterNot { t -> t.id == topicId }) }
                        current.copy(
                            list = remaining ?: current.list,
                            isRefreshing = false,
                            deletingId = null,
                            snackbar = TopicsSnackbar.Deleted,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { current ->
                        current.copy(
                            deletingId = null,
                            dialog =
                                e.toExpertDialogError()?.let { TopicsDialog(R.string.topics_delete_error_title, it) }
                                    ?: current.dialog,
                        )
                    }
                } finally {
                    _uiState.update { it.copy(deletingId = null) }
                }
            }
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialog = null) }
        }

        fun onSnackbarShown() {
            _uiState.update { it.copy(snackbar = null) }
        }
    }
