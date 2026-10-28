package com.uptrendinvestments.clickmeapp.screens.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.AppRoute
import com.uptrendinvestments.clickmeapp.model.enums.AppMode

/**
 * Non-visual root host of the push router. It forwards the root [route] to [PushRoutingViewModel] and performs the mode
 * switches the router asks for through [onSwitchMode] (the existing `AppViewModel.switchMode`). It lives at the root
 * because a destination may need a mode switch before any shell exists (RESEARCH F-7).
 *
 * The resolver already refuses a switch during a call (13-07), so `switchMode`'s call guard shows no dialog for a push.
 */
@Composable
fun PushRoutingHost(
    route: AppRoute,
    onSwitchMode: (AppMode) -> Unit,
    viewModel: PushRoutingViewModel = hiltViewModel(),
) {
    val switchTo by viewModel.switchRequest.collectAsStateWithLifecycle()
    LaunchedEffect(route) { viewModel.onRouteChanged(route) }
    LaunchedEffect(switchTo) {
        switchTo?.let {
            onSwitchMode(it)
            viewModel.onSwitchRequestHandled()
        }
    }
}
