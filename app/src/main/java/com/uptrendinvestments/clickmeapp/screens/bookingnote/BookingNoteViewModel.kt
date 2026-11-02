package com.uptrendinvestments.clickmeapp.screens.bookingnote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.chat.capText
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isNotFound
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The private note editor for both roles. Port of iOS `ClientBookingNoteViewModel` and `ExpertBookingNoteViewModel`,
 * which are identical except for the endpoint: [args] carries the role and the repository picks the endpoint pair.
 *
 * Deliberate deviations from iOS: D-01 (leaving with unsaved changes asks first, iOS discards silently), D-02 (the
 * text is capped at [NOTE_MAX_LENGTH], iOS has no cap so a long note only fails on save) and D-04 (a whitespace-only
 * note is saved as an empty string, iOS saves it as typed).
 *
 * The typed draft lives in memory only: there is no SavedStateHandle, so it is lost if the process dies (RESEARCH A4),
 * and the note text is never logged (T-16.4-10).
 */
@HiltViewModel(assistedFactory = BookingNoteViewModel.Factory::class)
class BookingNoteViewModel
    @AssistedInject
    constructor(
        @Assisted private val args: BookingNoteArgs,
        private val repository: BookingNoteRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(args: BookingNoteArgs): BookingNoteViewModel
        }

        private val _uiState = MutableStateFlow(BookingNoteUiState())
        private var savedJob: Job? = null
        val uiState: StateFlow<BookingNoteUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        /** Loads the note again after a failure. A loaded editor is never reloaded, so an edit is never overwritten. */
        fun retry() {
            if (_uiState.value.load == BookingNoteLoad.Loaded) return
            load()
        }

        private fun load() {
            _uiState.update { it.copy(load = BookingNoteLoad.Loading) }
            viewModelScope.launch {
                val note =
                    try {
                        repository.get(args.role, args.bookingId)
                    } catch (e: ApiException) {
                        // The server answers a uniform 404 for "no note yet" and "not yours": both open an empty editor.
                        if (e.isNotFound()) {
                            null
                        } else {
                            _uiState.update { it.copy(load = BookingNoteLoad.Failed(e.toSectionMessage())) }
                            return@launch
                        }
                    }
                val text = note.orEmpty()
                _uiState.update {
                    it.copy(noteText = text, lastPersisted = normalizeNote(text), load = BookingNoteLoad.Loaded)
                }
            }
        }

        /** Sets the text, cut at [NOTE_MAX_LENGTH] without splitting a surrogate pair. */
        fun onTextChange(raw: String) {
            _uiState.update { it.copy(noteText = capText(raw, NOTE_MAX_LENGTH)) }
        }

        /**
         * Saves the note. Ignored unless [BookingNoteUiState.canSave]: nothing changed, still loading or a save is in
         * flight. The text sent is [normalizeNote] of the field, and what counts as persisted afterwards is what was
         * sent, never the server's answer (RESEARCH Pitfall 4: the server answers null for an empty note).
         */
        fun save() {
            val state = _uiState.value
            if (!state.canSave) return
            val body = normalizeNote(state.noteText)
            _uiState.update { it.copy(isSaving = true, didSave = false, saveError = null) }
            viewModelScope.launch {
                try {
                    repository.save(args.role, args.bookingId, body)
                } catch (e: ApiException) {
                    // The text stays. A POST 404 lands here too: only the load maps 404 to an empty note.
                    // toDialogError() is null only for ACCOUNT_SUSPENDED, and that needs no dialog here: the
                    // SuspensionInterceptor has already cleared the tokens and raised SessionEvent.Suspended, so
                    // UserManager logs out, AppViewModel moves this home route to Login (the editor leaves
                    // composition) and ClickMeApp shows the global suspended dialog. Same as every sibling screen.
                    _uiState.update { it.copy(isSaving = false, saveError = e.toDialogError()) }
                    return@launch
                }
                _uiState.update { it.copy(isSaving = false, lastPersisted = body, didSave = true) }
                savedJob?.cancel()
                savedJob =
                    viewModelScope.launch {
                        delay(SAVED_STATE_MS)
                        _uiState.update { it.copy(didSave = false) }
                    }
            }
        }

        /** Closes the save error dialog. The text stays in the field. */
        fun dismissSaveError() {
            _uiState.update { it.copy(saveError = null) }
        }

        /**
         * Asks whether the screen may close. True when nothing changed. Otherwise the discard prompt is raised and the
         * result is false, so the caller stays on the screen until the user answers (D-01).
         */
        fun requestLeave(): Boolean {
            if (!_uiState.value.hasChanges) return true
            _uiState.update { it.copy(showDiscardPrompt = true) }
            return false
        }

        /** Closes the discard prompt and keeps editing. */
        fun dismissDiscardPrompt() {
            _uiState.update { it.copy(showDiscardPrompt = false) }
        }

        private companion object {
            /** How long the saved mark shows after a successful save (D-03). */
            const val SAVED_STATE_MS = 1_500L
        }
    }
