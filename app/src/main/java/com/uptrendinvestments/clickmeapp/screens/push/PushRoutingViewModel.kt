package com.uptrendinvestments.clickmeapp.screens.push

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.AppRoute
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Whether the signed-in user holds the expert role; a seam so tests need no `UserManager`. */
fun interface PushRoleSource {
    fun hasExpertRole(): Boolean
}

/**
 * The root push router: stage 1 to stage 2 of RESEARCH Pattern 2. It watches the root [AppRoute] (fed by
 * [onRouteChanged]) and the pending [PushLaunchRequest.destination], and once a home shell exists it resolves the tap
 * with [PushDestinationResolver] and hands the route to the shell of the matching mode ([ShellRouteRequest]) or to the
 * chat hand-off ([ChatLaunchRequest.thread]).
 *
 * A tap waits while the route is Splash (the slot keeps it through session restore, up to the 5 s splash cap). No UI
 * shows while it resolves (D-06).
 */
@HiltViewModel
class PushRoutingViewModel
    @Inject
    constructor(
        private val launchRequest: PushLaunchRequest,
        private val shellRoutes: ShellRouteRequest,
        private val chatLaunch: ChatLaunchRequest,
        private val resolver: PushDestinationResolver,
        private val roles: PushRoleSource,
        private val callGuard: CallGuard,
    ) : ViewModel() {
        private val route = MutableStateFlow(AppRoute.Splash)
        private var routingJob: Job? = null

        init {
            viewModelScope.launch {
                combine(route, launchRequest.destination.pending) { current, pending -> current to pending }
                    .collect { (current, pending) ->
                        val mode = current.homeMode()
                        if (mode != null && pending != null) {
                            val destination = launchRequest.destination.consume() ?: return@collect
                            routingJob?.cancel()
                            routingJob = launch { deliver(destination, mode) }
                        }
                    }
            }
        }

        /** Reports the current root route; call it whenever the route changes. */
        fun onRouteChanged(route: AppRoute) {
            this.route.value = route
        }

        /** Resolves [destination] for [currentMode] and posts the resulting route (same-mode only). */
        private suspend fun deliver(
            destination: PushDestination,
            currentMode: AppMode,
        ) {
            val context = PushRoutingContext(currentMode, roles.hasExpertRole(), callGuard.isCallActive.value)
            val resolved = resolver.resolve(destination, context)
            post(resolved.route)
        }

        private fun post(route: PushRoute) {
            when (route) {
                is PushRoute.Shell -> shellRoutes.post(route.route)
                is PushRoute.Chat -> chatLaunch.thread.post(route.key)
            }
        }

        private fun AppRoute.homeMode(): AppMode? =
            when (this) {
                AppRoute.ClientHome -> AppMode.Client
                AppRoute.ExpertHome -> AppMode.Expert
                else -> null
            }
    }
