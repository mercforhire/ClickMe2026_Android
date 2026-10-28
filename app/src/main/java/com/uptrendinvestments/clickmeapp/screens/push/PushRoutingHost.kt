package com.uptrendinvestments.clickmeapp.screens.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.uptrendinvestments.clickmeapp.AppRoute
import com.uptrendinvestments.clickmeapp.model.enums.AppMode

/**
 * Non-visual root host of the push router. It forwards the root [route] to [PushRoutingViewModel] and performs the mode
 * switches the router asks for through [onSwitchMode] (the existing `AppViewModel.switchMode`). It lives at the root
 * because a destination may need a mode switch before any shell exists (RESEARCH F-7).
 */
@Composable
fun PushRoutingHost(
    route: AppRoute,
    onSwitchMode: (AppMode) -> Unit,
    viewModel: PushRoutingViewModel = hiltViewModel(),
) {
    LaunchedEffect(route) { viewModel.onRouteChanged(route) }
}
