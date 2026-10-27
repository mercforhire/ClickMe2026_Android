package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/** The Report & Block route: builds the ViewModel for [key] and renders the screen. */
@Composable
fun ReportAndBlockRoute(
    key: ChatReportBlockKey,
    navigator: Navigator,
    mode: AppMode,
) {
    ReportAndBlockScreen(
        viewModel =
            hiltViewModel<ReportAndBlockViewModel, ReportAndBlockViewModel.Factory>(
                creationCallback = { factory -> factory.create(key) },
            ),
        onBack = navigator::goBack,
        onBlocked = navigator::goBack,
        onUnblocked = navigator::goBack,
    )
}
