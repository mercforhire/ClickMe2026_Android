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
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            viewModelScope.launch {
                try {
                    repository.save(args.role, args.bookingId, body)
                } catch (e: ApiException) {
                    // The text stays. A POST 404 lands here too: only the load maps 404 to an empty note.
                    _uiState.update { it.copy(isSaving = false, saveError = e.toDialogError()) }
                    return@launch
                }
                _uiState.update { it.copy(isSaving = false, lastPersisted = body, didSave = true) }
            }
        }
    }
