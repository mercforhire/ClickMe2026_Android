package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
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
private const val HTTP_UNPROCESSABLE = 422

private const val KEY_LEGAL_NAME = "tax_profile_legal_name"
private const val KEY_DOB = "tax_profile_dob"
private const val KEY_LINE1 = "tax_profile_line1"
private const val KEY_LINE2 = "tax_profile_line2"
private const val KEY_CITY = "tax_profile_city"
private const val KEY_REGION = "tax_profile_region"
private const val KEY_POSTAL_CODE = "tax_profile_postal_code"
private const val KEY_COUNTRY = "tax_profile_country"
private const val KEY_TOUCHED = "tax_profile_touched"

/**
 * The saved-state key of a field that survives process death, or null for the two that never do: the TIN (a secret,
 * CONTEXT D-09) and the financial account identifier (bank details). Both stay in memory only.
 */
private fun TaxProfileField.savedStateKey(): String? =
    when (this) {
        TaxProfileField.LegalName -> KEY_LEGAL_NAME
        TaxProfileField.Dob -> KEY_DOB
        TaxProfileField.Line1 -> KEY_LINE1
        TaxProfileField.Line2 -> KEY_LINE2
        TaxProfileField.City -> KEY_CITY
        TaxProfileField.Region -> KEY_REGION
        TaxProfileField.PostalCode -> KEY_POSTAL_CODE
        TaxProfileField.Country -> KEY_COUNTRY
        TaxProfileField.FinancialAccount, TaxProfileField.Tin -> null
    }

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
 * Process death (CONTEXT D-12): the non-secret fields and the date of birth are mirrored to [SavedStateHandle] as
 * Strings, with a touched flag, and laid over the server prefill when it loads, so a restored edit wins over the
 * fetched profile (RESEARCH Pitfall 7). The TIN and the financial account identifier are never written there
 * (CONTEXT D-09, threat T-15-31): after process death the TIN comes back empty and the account shows the server's
 * value, with no error text but an info banner saying they were not kept. The TIN is typed, sent
 * once and cleared after a save, which also clears the touched flag so the next visit shows the server's data.
 */
@HiltViewModel
class ExpertTaxProfileViewModel
    @Inject
    constructor(
        private val repository: TaxProfileRepository,
        private val savedStateHandle: SavedStateHandle,
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
                                form = restoredOver(data.toForm()),
                                tinOnFile = data.tinOnFile,
                                fieldErrors = emptyMap(),
                                // A restored draft cannot carry the account or the TIN; say so rather than show the
                                // server's account beside edited identity data as if it were the draft.
                                banner = secretsNotRestoredHint(),
                            )
                        }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = TaxProfileLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }

        /** The info banner shown when a restored draft overlaid the server data; null when nothing was restored. */
        private fun secretsNotRestoredHint(): FieldError? =
            if (savedStateHandle.get<Boolean>(KEY_TOUCHED) == true) {
                FieldError.Res(R.string.tax_profile_secrets_not_restored)
            } else {
                null
            }

        /** [server] with every non-secret field the user edited before recreation put back. */
        private fun restoredOver(server: TaxProfileForm): TaxProfileForm {
            if (savedStateHandle.get<Boolean>(KEY_TOUCHED) != true) return server

            fun saved(field: TaxProfileField): String? = field.savedStateKey()?.let { savedStateHandle.get<String>(it) }
            return server.copy(
                legalName = saved(TaxProfileField.LegalName) ?: server.legalName,
                dob = saved(TaxProfileField.Dob) ?: server.dob,
                line1 = saved(TaxProfileField.Line1) ?: server.line1,
                line2 = saved(TaxProfileField.Line2) ?: server.line2,
                city = saved(TaxProfileField.City) ?: server.city,
                region = saved(TaxProfileField.Region) ?: server.region,
                postalCode = saved(TaxProfileField.PostalCode) ?: server.postalCode,
                country =
                    saved(TaxProfileField.Country)
                        ?.filter(Char::isLetter)
                        ?.uppercase()
                        ?.take(COUNTRY_CODE_LENGTH)
                        ?: server.country,
            )
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
                mirror(field, next)
                state.copy(form = next, fieldErrors = state.fieldErrors - field)
            }
        }

        /** Writes the edited non-secret [field] to the saved state; the TIN and the account identifier are skipped. */
        private fun mirror(
            field: TaxProfileField,
            form: TaxProfileForm,
        ) {
            val key = field.savedStateKey() ?: return
            savedStateHandle[key] =
                when (field) {
                    TaxProfileField.LegalName -> form.legalName
                    TaxProfileField.Dob -> form.dob
                    TaxProfileField.Line1 -> form.line1
                    TaxProfileField.Line2 -> form.line2
                    TaxProfileField.City -> form.city
                    TaxProfileField.Region -> form.region
                    TaxProfileField.PostalCode -> form.postalCode
                    TaxProfileField.Country -> form.country
                    TaxProfileField.FinancialAccount, TaxProfileField.Tin -> return
                }
            savedStateHandle[KEY_TOUCHED] = true
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
                    _uiState.update { it.copy(saveState = SaveState.Idle) }
                    handleSaveError(e)
                }
            }
        }

        private fun applySaved(result: SaveTaxProfileData) {
            _uiState.update { state ->
                when {
                    result.saved -> {
                        // The server has the data now: a later recreation shows what it holds, not this draft.
                        savedStateHandle[KEY_TOUCHED] = false
                        // The TIN is sent once; it must not linger in memory after the server has it.
                        state.copy(
                            saveState = SaveState.Idle,
                            form = state.form.copy(tin = ""),
                            tinOnFile = true,
                            savedEvent = true,
                        )
                    }
                    // The server wrote nothing because it still needs the TIN: a prompt, not an error (iOS parity).
                    // Every field stays so the expert only adds the TIN and saves again.
                    result.tinRequired ->
                        state.copy(
                            saveState = SaveState.Idle,
                            banner = messageOr(result.message, R.string.tax_profile_tin_required_fallback),
                            focusTinEvent = true,
                        )
                    // `saved: false` without a TIN request is not a documented answer; never read it as success.
                    else ->
                        state.copy(
                            saveState = SaveState.Idle,
                            dialog = messageOr(result.message, R.string.login_error_unknown),
                        )
                }
            }
        }

        /** A 422 puts the server's messages on the fields they name; anything else opens the save dialog. */
        private fun handleSaveError(e: ApiException) {
            if (e is ApiException.Http && e.status == HTTP_UNPROCESSABLE && mapFieldErrors(e)) return
            _uiState.update { it.copy(dialog = e.toDialogError()) }
        }

        /** Returns true when at least one server field error landed on a field. The last message for a field wins. */
        private fun mapFieldErrors(e: ApiException.Http): Boolean {
            val errors = mutableMapOf<TaxProfileField, String>()
            for (fieldError in e.fieldErrors) {
                val field = fieldFor(fieldError.field) ?: continue
                errors[field] = fieldError.message
            }
            if (errors.isEmpty()) return false
            _uiState.update {
                it.copy(fieldErrors = errors, focusTinEvent = it.focusTinEvent || TaxProfileField.Tin in errors)
            }
            return true
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

/** The field a server validation error names; paths like `address.postal_code` match on their last segment. */
private fun fieldFor(serverField: String): TaxProfileField? =
    when (serverField.substringAfterLast('.')) {
        "tin" -> TaxProfileField.Tin
        "dob" -> TaxProfileField.Dob
        "legal_name" -> TaxProfileField.LegalName
        "postal_code" -> TaxProfileField.PostalCode
        "line1" -> TaxProfileField.Line1
        "line2" -> TaxProfileField.Line2
        "city" -> TaxProfileField.City
        "region", "province" -> TaxProfileField.Region
        "country" -> TaxProfileField.Country
        "financial_account_identifier" -> TaxProfileField.FinancialAccount
        else -> null
    }

private fun messageOr(
    message: String?,
    fallback: Int,
): FieldError = if (message.isNullOrBlank()) FieldError.Res(fallback) else FieldError.Text(message)

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
