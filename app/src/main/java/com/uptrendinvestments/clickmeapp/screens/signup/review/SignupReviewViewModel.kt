package com.uptrendinvestments.clickmeapp.screens.signup.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.Countries
import com.uptrendinvestments.clickmeapp.constants.Timezones
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.screens.signup.SignupDraft
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Review screen state. A null value means the item is not set and the screen shows "Required".
 * [missing] holds the steps that block Publish (D-03: province and verified email never do).
 */
data class SignupReviewUiState(
    val firstName: String?,
    val location: String?,
    val languages: String?,
    val timezoneLabel: String?,
    val avatarUrl: String?,
    val skills: String?,
    val missing: List<SetupStep>,
    val canPublish: Boolean,
    val saveState: SaveState = SaveState.Idle,
    val missingFieldsDialog: List<SetupStep>? = null,
    val dialogError: FieldError? = null,
    /** Steps the local publish refusal names (iOS `missingMessage`); null while no refusal dialog is open. */
    val publishRefusal: List<SetupStep>? = null,
)

/**
 * Port of iOS `SignupReviewViewModel`. Shows the shared signup draft, gates Publish on the server's required
 * fields (D-03, D-05), publishes through [SignupSetupRepository] and maps a 422 `missing_fields` to hub steps
 * (D-06). After a successful publish the repository is reset (T-04-46) and [published] flips so the app can
 * land on client home (D-22, wired by the navigation plan through `AppViewModel.onSetupPublished`).
 */
@HiltViewModel
class SignupReviewViewModel
    @Inject
    constructor(
        private val repository: SignupSetupRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(repository.draft.value.toUiState())
        val uiState: StateFlow<SignupReviewUiState> = _uiState.asStateFlow()

        private val _published = MutableStateFlow(false)
        val published: StateFlow<Boolean> = _published.asStateFlow()

        private val _returnToHub = MutableStateFlow(false)
        val returnToHub: StateFlow<Boolean> = _returnToHub.asStateFlow()

        /** Once true the state stops following the draft, so the reset after publish does not blank the screen. */
        private var frozen = false

        init {
            viewModelScope.launch {
                repository.draft.collect { draft ->
                    if (frozen) return@collect
                    _uiState.update { previous ->
                        draft.toUiState().copy(
                            saveState = previous.saveState,
                            missingFieldsDialog = previous.missingFieldsDialog,
                            dialogError = previous.dialogError,
                            publishRefusal = previous.publishRefusal,
                        )
                    }
                }
            }
        }

        /**
         * iOS keeps the Publish button usable and refuses here, naming the missing steps, without a request
         * (`SignupReviewViewModel.publish`, iOS 05-04). The server stays the enforcer through its 422.
         */
        fun publish() {
            val current = _uiState.value
            if (current.saveState != SaveState.Idle) return
            if (!current.canPublish) {
                _uiState.update { it.copy(publishRefusal = current.missing) }
                return
            }
            _uiState.update { it.copy(saveState = SaveState.Saving, dialogError = null) }
            viewModelScope.launch {
                try {
                    repository.publish()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(saveState = SaveState.Idle) }
                    handle(e)
                    return@launch
                }
                frozen = true
                _uiState.update { it.copy(saveState = SaveState.Saved) }
                delay(Motion.SAVED_CONFIRMATION_HOLD_MS.toLong())
                repository.reset()
                _published.value = true
            }
        }

        fun dismissMissingFields() {
            _uiState.update { it.copy(missingFieldsDialog = null) }
            _returnToHub.value = true
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialogError = null, publishRefusal = null) }
        }

        fun onPublishedConsumed() {
            _published.value = false
        }

        fun onReturnToHubConsumed() {
            _returnToHub.value = false
        }

        private fun handle(e: ApiException) {
            if (e is ApiException.Http && e.status == UNPROCESSABLE && e.missingFields.isNotEmpty()) {
                val steps =
                    e.missingFields
                        .mapNotNull(SetupStep::fromMissingField)
                        .distinct()
                        .sortedBy { it.ordinal }
                if (steps.isNotEmpty()) {
                    _uiState.update { it.copy(missingFieldsDialog = steps) }
                    return
                }
            }
            _uiState.update { it.copy(dialogError = e.toDialogError()) }
        }

        private companion object {
            const val UNPROCESSABLE = 422
        }
    }

internal fun SignupDraft.toUiState(): SignupReviewUiState =
    SignupReviewUiState(
        firstName = firstName.trim().ifBlank { null },
        location = reviewLocation(),
        languages =
            (languages.map { it.label } + customLanguages.filter { it.isNotBlank() })
                .joinToString(", ")
                .ifBlank { null },
        timezoneLabel = timezone.takeIf { it.isNotBlank() }?.let { Timezones.entryFor(it).label },
        avatarUrl = avatarUrl?.takeIf { it.isNotBlank() },
        skills = expertiseTags.joinToString(", ") { it.label }.ifBlank { null },
        missing = missingForPublish,
        canPublish = canPublish,
    )

private fun SignupDraft.reviewLocation(): String? {
    val cityValue = city.trim()
    val country = countryCode.trim()
    if (cityValue.isEmpty() || country.isEmpty()) return null
    return listOf(cityValue, provinceState.trim(), Countries.displayName(country))
        .filter { it.isNotEmpty() }
        .joinToString(", ")
}
