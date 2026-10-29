package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Stripe Connect only supports Canada in v1 (CONTEXT D-08), so the country starts as `CA`. */
const val DEFAULT_TAX_COUNTRY = "CA"

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
fun canSaveTaxProfile(form: TaxProfileForm): Boolean = false

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
    /** The info banner for `saved: false, tin_required: true`. Not an error. */
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

/** State holder of Tax & Payout Info (PAYOUT-04, port of iOS `ExpertTaxProfileViewModel`). */
@HiltViewModel
class ExpertTaxProfileViewModel
    @Inject
    constructor(
        private val repository: TaxProfileRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TaxProfileUiState())
        val uiState: StateFlow<TaxProfileUiState> = _uiState.asStateFlow()

        fun onFieldChange(
            field: TaxProfileField,
            value: String,
        ) = Unit

        fun onDobPicked(epochMillisUtc: Long) = Unit

        fun save() = Unit

        fun retry() = Unit

        fun dismissDialog() = Unit

        fun consumeSaved() = Unit

        fun consumeFocusTin() = Unit
    }
