package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * The Write Review route: binds the assisted [WriteReviewViewModel] to [key] and pops on back and on a successful
 * submit. The screen that opened Write Review reloads and shows the thanks snackbar (13-15, 13-18).
 */
@Composable
fun WriteReviewRoute(
    key: WriteReviewKey,
    navigator: Navigator,
) {
    val viewModel =
        hiltViewModel<WriteReviewViewModel, WriteReviewViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) },
        )
    WriteReviewScreen(
        viewModel = viewModel,
        onBack = navigator::goBack,
        onSubmitted = navigator::goBack,
    )
}
