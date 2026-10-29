package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** Test tag on the Save tax profile button. */
const val TAX_SAVE_TAG = "tax_save"

/** Test tag on the TIN field. */
const val TAX_TIN_TAG = "tax_tin"

/** ViewModel callbacks of the Tax & Payout Info screen, bundled so the stateless content is easy to drive in tests. */
data class TaxProfileActions(
    val onFieldChange: (TaxProfileField, String) -> Unit,
    val onDobPicked: (Long) -> Unit,
    val save: () -> Unit,
    val retry: () -> Unit,
    val dismissDialog: () -> Unit,
    val consumeSaved: () -> Unit,
    val consumeFocusTin: () -> Unit,
)

@Composable
fun ExpertTaxProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpertTaxProfileViewModel = hiltViewModel(),
) {
    Box(modifier)
}

@Composable
fun ExpertTaxProfileContent(
    state: TaxProfileUiState,
    actions: TaxProfileActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier)
}
