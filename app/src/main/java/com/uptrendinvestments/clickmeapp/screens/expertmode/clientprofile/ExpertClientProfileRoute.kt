package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * The Client Profile route: binds the assisted [ExpertClientProfileViewModel] to [key] and pops on back. Registered
 * in the expert shell only (T-16.4-16).
 */
@Composable
fun ExpertClientProfileRoute(
    key: ExpertClientProfileKey,
    onBack: () -> Unit,
) {
    val viewModel =
        hiltViewModel<ExpertClientProfileViewModel, ExpertClientProfileViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) },
        )
    ExpertClientProfileScreen(viewModel = viewModel, onBack = onBack)
}
