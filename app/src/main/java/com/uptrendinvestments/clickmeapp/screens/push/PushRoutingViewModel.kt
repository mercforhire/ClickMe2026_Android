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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

        private val _switchRequest = MutableStateFlow<AppMode?>(null)

        /** The mode the host must switch to so the tapped destination can open, or null. */
        val switchRequest: StateFlow<AppMode?> = _switchRequest.asStateFlow()

        init {
            viewModelScope.launch {
                combine(route, launchRequest.destination.pending) { current, pending -> current to pending }
                    .collect { (current, pending) ->
                        val mode = current.homeMode()
                        when {
                            // D-14, RESEARCH A3: a logged-out, expired or mid-setup session holds no tap. Cancel any
                            // resolution in flight too, so nothing lands in the next session.
                            current == AppRoute.Login || current == AppRoute.SignupSetup -> {
                                routingJob?.cancel()
                                routingJob?.join()
                                launchRequest.destination.consume()
                            }

                            mode != null && pending != null -> {
                                val destination = launchRequest.destination.consume() ?: return@collect
                                // D-15: the newest tap wins, so an older resolution is cancelled before it posts. The
                                // join lets its cleanup finish before the new one touches the switch request.
                                routingJob?.cancel()
                                routingJob?.join()
                                routingJob = launch { deliver(destination, mode) }
                            }
                        }
                    }
            }
        }

        /** The host calls this once it has started the requested switch. */
        fun onSwitchRequestHandled() {
            _switchRequest.value = null
        }

        /** Reports the current root route; call it whenever the route changes. */
        fun onRouteChanged(route: AppRoute) {
            this.route.value = route
        }

        /**
         * Resolves [destination] for [currentMode] and posts the resulting route. When the route belongs to the other
         * mode, asks the host for a switch ([switchRequest]) and waits up to [SWITCH_TIMEOUT_MS] for the matching home
         * route; a timeout, Login or SignupSetup drops the tap.
         */
        private suspend fun deliver(
            destination: PushDestination,
            currentMode: AppMode,
        ) {
            val context = PushRoutingContext(currentMode, roles.hasExpertRole(), callGuard.isCallActive.value)
            val resolved = resolver.resolve(destination, context)
            // The route may have changed while the lookups ran (logout, a manual switch): never post into it.
            if (route.value != currentMode.homeRoute()) return
            if (resolved.targetMode != currentMode && !switchTo(resolved.targetMode)) return
            if (route.value != resolved.targetMode.homeRoute()) return
            post(resolved.route)
        }

        /** Requests a switch to [target] and returns true when its home route appears in time. */
        private suspend fun switchTo(target: AppMode): Boolean {
            val targetHome = target.homeRoute()
            _switchRequest.value = target
            val landed =
                try {
                    withTimeoutOrNull(SWITCH_TIMEOUT_MS) {
                        route.first { it == targetHome || it == AppRoute.Login || it == AppRoute.SignupSetup }
                    }
                } finally {
                    _switchRequest.value = null
                }
            return landed == targetHome
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

        private fun AppMode.homeRoute(): AppRoute =
            when (this) {
                AppMode.Client -> AppRoute.ClientHome
                AppMode.Expert -> AppRoute.ExpertHome
            }

        companion object {
            /** How long a mode switch for a tapped push may take before the tap is dropped (RESEARCH Pattern 2). */
            const val SWITCH_TIMEOUT_MS = 5_000L
        }
    }
