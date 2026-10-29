package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.SaveTaxProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.TaxAddressBody
import com.uptrendinvestments.clickmeapp.model.responses.SaveTaxProfileData
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

/** Stripe Connect only supports Canada in v1 (CONTEXT D-08), so the country starts as `CA`. */
const val DEFAULT_TAX_COUNTRY = "CA"

private const val COUNTRY_CODE_LENGTH = 2

/** The inputs of the Tax & Payout Info form. */
enum class TaxProfileField { LegalName, Dob, Line1, Line2, City, Region, PostalCode, Country, FinancialAccount, Tin }

/** The editable form. In memory only; [toString] prints field names, never values (threat T-14-31). */
data class TaxProfileForm(
    val legalName: String = "",
    /** ISO `yyyy-MM-dd`, the wire format. */
    val dob: String = "",
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val region: String = "",
    val postalCode: String = "",
    val country: String = DEFAULT_TAX_COUNTRY,
    val financialAccount: String = "",
    val tin: String = "",
) {
    override fun toString(): String =
        "TaxProfileForm(legalName, dob, line1, line2, city, region, postalCode, country, financialAccount, tin)"
}

/** iOS `canSave`: the seven required fields are non-blank. Line 2, financial account and TIN are optional. */
fun canSaveTaxProfile(form: TaxProfileForm): Boolean =
    form.legalName.isNotBlank() &&
        form.dob.isNotBlank() &&
        form.line1.isNotBlank() &&
        form.city.isNotBlank() &&
        form.region.isNotBlank() &&
        form.postalCode.isNotBlank() &&
        form.country.isNotBlank()

sealed interface TaxProfileLoad {
    data object Loading : TaxProfileLoad

    data object Ready : TaxProfileLoad

    data class Failed(
        val message: FieldError,
    ) : TaxProfileLoad
}

data class TaxProfileUiState(
    val load: TaxProfileLoad = TaxProfileLoad.Loading,
    val form: TaxProfileForm = TaxProfileForm(),
    /** True once the server holds a TIN (from the load or a `saved: true` answer). The TIN itself is never shown. */
    val tinOnFile: Boolean = false,
    val saveState: SaveState = SaveState.Idle,
    val fieldErrors: Map<TaxProfileField, String> = emptyMap(),
    /** The info banner for a save the server deferred because it still needs the TIN. Not an error. */
    val banner: FieldError? = null,
    val dialog: FieldError? = null,
    /** One-shot: a save went through, show the snackbar. */
    val savedEvent: Boolean = false,
    /** One-shot: move focus to the TIN field. */
    val focusTinEvent: Boolean = false,
) {
    val saving: Boolean get() = saveState == SaveState.Saving
    val canSave: Boolean get() = canSaveTaxProfile(form)
}

/**
 * State holder of Tax & Payout Info (PAYOUT-04, port of iOS `ExpertTaxProfileViewModel`).
 *
 * The form lives in memory only: no `SavedStateHandle`, so neither the TIN nor the personal details survive process
 * death and the form reloads from the server (threat T-14-31). The TIN is typed, sent once and cleared after a save.
 */
@HiltViewModel
class ExpertTaxProfileViewModel
    @Inject
    constructor(
        private val repository: TaxProfileRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TaxProfileUiState())
        val uiState: StateFlow<TaxProfileUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            load()
        }

        fun retry() = load()

        private fun load() {
            loadJob?.cancel()
            _uiState.update { it.copy(load = TaxProfileLoad.Loading) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val data = repository.load()
                        _uiState.update {
                            it.copy(
                                load = TaxProfileLoad.Ready,
                                form = data.toForm(),
                                tinOnFile = data.tinOnFile,
                                fieldErrors = emptyMap(),
                                banner = null,
                            )
                        }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = TaxProfileLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }

        fun onFieldChange(
            field: TaxProfileField,
            value: String,
        ) {
            _uiState.update { state ->
                if (state.saving) return@update state
                val form = state.form
                val next =
                    when (field) {
                        TaxProfileField.LegalName -> form.copy(legalName = value)
                        TaxProfileField.Dob -> form.copy(dob = value)
                        TaxProfileField.Line1 -> form.copy(line1 = value)
                        TaxProfileField.Line2 -> form.copy(line2 = value)
                        TaxProfileField.City -> form.copy(city = value)
                        TaxProfileField.Region -> form.copy(region = value)
                        TaxProfileField.PostalCode -> form.copy(postalCode = value)
                        TaxProfileField.Country ->
                            form.copy(country = value.filter(Char::isLetter).uppercase().take(COUNTRY_CODE_LENGTH))
                        TaxProfileField.FinancialAccount -> form.copy(financialAccount = value)
                        TaxProfileField.Tin -> form.copy(tin = value)
                    }
                state.copy(form = next, fieldErrors = state.fieldErrors - field)
            }
        }

        /** The Material date picker answers in UTC milliseconds; the wire wants the calendar day as `yyyy-MM-dd`. */
        fun onDobPicked(epochMillisUtc: Long) {
            val day = Instant.ofEpochMilli(epochMillisUtc).atZone(ZoneOffset.UTC).toLocalDate()
            onFieldChange(TaxProfileField.Dob, day.toString())
        }

        fun save() {
            val state = _uiState.value
            if (state.load != TaxProfileLoad.Ready || state.saving || !state.canSave) return
            val request = state.form.toRequest()
            _uiState.update {
                it.copy(saveState = SaveState.Saving, fieldErrors = emptyMap(), banner = null, dialog = null)
            }
            viewModelScope.launch {
                try {
                    applySaved(repository.save(request))
                } catch (e: ApiException) {
                    _uiState.update { it.copy(saveState = SaveState.Idle, dialog = e.toDialogError()) }
                }
            }
        }

        private fun applySaved(result: SaveTaxProfileData) {
            _uiState.update { state ->
                if (result.saved) {
                    // The TIN is sent once; it must not linger in memory after the server has it.
                    state.copy(
                        saveState = SaveState.Idle,
                        form = state.form.copy(tin = ""),
                        tinOnFile = true,
                        savedEvent = true,
                    )
                } else {
                    state.copy(saveState = SaveState.Idle)
                }
            }
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialog = null) }
        }

        fun consumeSaved() {
            _uiState.update { it.copy(savedEvent = false) }
        }

        fun consumeFocusTin() {
            _uiState.update { it.copy(focusTinEvent = false) }
        }
    }

/** The server answers `region` or `province`; the TIN is never part of the answer, so it always starts empty. */
private fun TaxProfileData.toForm(): TaxProfileForm {
    val serverCountry = address?.country.orEmpty()
    return TaxProfileForm(
        legalName = legalName.orEmpty(),
        dob = dob.orEmpty(),
        line1 = address?.line1.orEmpty(),
        line2 = address?.line2.orEmpty(),
        city = address?.city.orEmpty(),
        region = address?.regionOrProvince.orEmpty(),
        postalCode = address?.postalCode.orEmpty(),
        country = serverCountry.trim().uppercase().ifEmpty { DEFAULT_TAX_COUNTRY },
        financialAccount = financialAccountIdentifier.orEmpty(),
        tin = "",
    )
}

private fun TaxProfileForm.toRequest(): SaveTaxProfileRequest =
    SaveTaxProfileRequest(
        legalName = legalName.trim(),
        address =
            TaxAddressBody(
                line1 = line1.trim(),
                line2 = line2.trim().takeIf { it.isNotEmpty() },
                city = city.trim(),
                region = region.trim(),
                postalCode = postalCode.trim(),
                country = country.trim().uppercase(),
            ),
        dob = dob.trim(),
        tin = tin.trim().takeIf { it.isNotEmpty() },
        financialAccountIdentifier = financialAccount.trim().takeIf { it.isNotEmpty() },
    )
