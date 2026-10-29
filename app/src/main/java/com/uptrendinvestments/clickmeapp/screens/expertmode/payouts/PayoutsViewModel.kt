package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionItem
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionsData
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.ConnectReturn
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertFinancialRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.PayoutOnboarding
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.isAllowedOnboardingUrl
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.resolvePayoutOnboarding
import com.uptrendinvestments.clickmeapp.screens.expertmode.toExpertDialogError
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository.Companion.TYPE_UPCOMING
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Load state of the Payouts screen: the skeleton, an error with Retry, or the content. */
sealed interface PayoutsLoad {
    data object Loading : PayoutsLoad

    data class Failed(
        val message: FieldError,
    ) : PayoutsLoad

    data object Loaded : PayoutsLoad
}

/**
 * State of the Payouts screen. [content] is null until a load has answered. [isRefreshing] drives the pull indicator
 * and [refreshFailed] the one-shot snackbar. [onboardingUrl] is the single-use Stripe link, held in memory only until
 * the screen opens it; [onboardingError] is the error dialog for a failed mint. [recentRows] is the first ledger page
 * for the Recent activity preview and [hasMoreActivity] says the server has more rows; both stay empty when the
 * ledger fetch fails, which never fails the screen.
 */
data class PayoutsUiState(
    val load: PayoutsLoad = PayoutsLoad.Loading,
    val content: PayoutsContent? = null,
    val summary: PayoutSummaryData? = null,
    val onboarding: PayoutOnboarding = PayoutOnboarding.Unknown,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val isMintingLink: Boolean = false,
    val onboardingUrl: String? = null,
    val onboardingError: FieldError? = null,
    val recentRows: List<EarningsTransactionItem> = emptyList(),
    val hasMoreActivity: Boolean = false,
)

/**
 * Expert Payouts state machine (PAYOUT-01, port of iOS `PayoutsViewModel`). One latest-wins load fetches the Connect
 * status, the payout summary and (best effort) the upcoming bookings for the held count in parallel. The Connect
 * status decides whether setup is required ([resolvePayoutOnboarding]); the summary only matters once onboarded.
 *
 * The first load shows the skeleton ([LoadMode.Loud]). A reload on resume is quiet: it never returns to the skeleton
 * and a failure is swallowed. A pull-to-refresh failure raises [PayoutsUiState.refreshFailed]. A reload that only lost
 * the payout summary keeps what is shown rather than replacing a real balance with "ready" or "on hold".
 *
 * Nothing is logged: the responses hold money and the onboarding link is single use.
 */
@HiltViewModel
class PayoutsViewModel
    @Inject
    constructor(
        private val financial: ExpertFinancialRepository,
        private val bookings: ExpertBookingsRepository,
        private val callGuard: CallGuard,
        private val launchRequest: ExpertLaunchRequest,
    ) : ViewModel() {
        private enum class LoadMode {
            /** Shows the skeleton and a failure message. */
            Loud,

            /** Silent: keeps the previous data and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,
        }

        private class Snapshot(
            val status: ConnectStatusData?,
            val summary: PayoutSummaryData?,
            val summaryWas400: Boolean,
            val summaryError: ApiException?,
            val heldCount: Int,
            val ledger: EarningsTransactionsData?,
        )

        private val _uiState = MutableStateFlow(PayoutsUiState())
        val uiState: StateFlow<PayoutsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        /**
         * This screen handed a link to the browser and it has not been used up: only then may a refresh deep link
         * re-mint one. Cleared by a re-mint, by [ConnectReturn.Done] and once the account is onboarded.
         */
        private var openedOnboarding = false

        init {
            // A return posted before this screen existed is covered by the fresh load below and must never open a
            // tab by itself (T-14-28).
            launchRequest.connectReturn.consume()
            startLoad(LoadMode.Loud)
            // A return posted while this screen lives (the Custom Tab closed) is consumed once.
            viewModelScope.launch {
                launchRequest.connectReturn.pending.filterNotNull().collect {
                    launchRequest.connectReturn.consume()?.let(::onConnectReturn)
                }
            }
        }

        /**
         * The Stripe return deep link (D-09). It only triggers calls: [ConnectReturn.Done] re-fetches the status
         * (never assuming success, T-14-29) and [ConnectReturn.LinkExpired] re-mints a link only when this screen
         * opened one that has not been re-minted yet and setup is still required, otherwise it just reloads (T-14-28).
         */
        private fun onConnectReturn(value: ConnectReturn) {
            val reload = if (_uiState.value.load is PayoutsLoad.Loaded) LoadMode.Quiet else LoadMode.Loud
            when (value) {
                ConnectReturn.Done -> {
                    openedOnboarding = false
                    startLoad(reload)
                }
                // One re-mint per link this screen opened, and only while setup is still required: a stray or hostile
                // refresh deep link can never mint a link for an onboarded expert or a second one for the same tab.
                ConnectReturn.LinkExpired ->
                    if (openedOnboarding && _uiState.value.content is PayoutsContent.SetupRequired) {
                        openedOnboarding = false
                        reopenOnboarding()
                    } else {
                        startLoad(reload)
                    }
            }
        }

        fun retry() = startLoad(LoadMode.Loud)

        fun refresh() = startLoad(LoadMode.Pull)

        /**
         * The screen is shown again (`ON_RESUME` and first composition). A call while a current load runs does
         * nothing, so first composition never double-loads; a loaded screen reloads quietly.
         */
        fun onScreenShown() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(if (_uiState.value.load is PayoutsLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
        }

        /** The screen was hidden or disposed; the next [onScreenShown] restarts a load that may predate a change. */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        /**
         * Starts Stripe Connect onboarding (PAYOUT-02, D-09): runs the call guard, then mints a fresh single-use link,
         * a refresh link when an account already exists and the first onboarding link otherwise. Repeat taps while a
         * link is minting or waiting to be opened are ignored. The URL must pass [isAllowedOnboardingUrl] (https with
         * a host), is opened once by the screen and is never logged, saved or cached (T-11-44, T-14-23, T-14-24).
         */
        fun startPayoutSetup() {
            val content = _uiState.value.content as? PayoutsContent.SetupRequired ?: return
            mintLink(refresh = content.useRefreshLink)
        }

        /** The onboarding link expired while the user was in the browser: mint a refresh link and reopen once. */
        private fun reopenOnboarding() = mintLink(refresh = true)

        private fun mintLink(refresh: Boolean) {
            val current = _uiState.value
            if (current.isMintingLink || current.onboardingUrl != null) return
            if (!callGuard.attempt(RestrictedAction.SetUpPayouts)) return
            _uiState.update { it.copy(isMintingLink = true) }
            viewModelScope.launch {
                try {
                    val url = financial.onboardingLink(refresh = refresh)
                    // The flag is set outside `update`: its lambda is a CAS loop that may run more than once.
                    val allowed = isAllowedOnboardingUrl(url)
                    if (allowed) openedOnboarding = true
                    _uiState.update {
                        if (allowed) {
                            it.copy(onboardingUrl = url)
                        } else {
                            it.copy(onboardingError = FieldError.Res(R.string.expert_error_generic))
                        }
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(onboardingError = e.toExpertDialogError()) }
                } finally {
                    _uiState.update { it.copy(isMintingLink = false) }
                }
            }
        }

        /** The screen took the one-shot onboarding URL; it is dropped so it can never be opened twice. */
        fun consumeOnboardingUrl() {
            _uiState.update { it.copy(onboardingUrl = null) }
        }

        fun dismissOnboardingError() {
            _uiState.update { it.copy(onboardingError = null) }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            loadIsCurrent = true
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = PayoutsLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    val snapshot = fetch()
                    val onboarding = resolvePayoutOnboarding(snapshot.status, snapshot.summary, snapshot.summaryWas400)
                    val content = selectPayoutsContent(onboarding, snapshot.summary, snapshot.heldCount)
                    if (onboarding == PayoutOnboarding.Onboarded) openedOnboarding = false
                    val lostSummary = snapshot.summaryError != null && !snapshot.summaryWas400
                    _uiState.update { applyResult(it, mode, snapshot, onboarding, content, lostSummary) }
                }
        }

        @Suppress("LongParameterList")
        private fun applyResult(
            state: PayoutsUiState,
            mode: LoadMode,
            snapshot: Snapshot,
            onboarding: PayoutOnboarding,
            content: PayoutsContent?,
            lostSummary: Boolean,
        ): PayoutsUiState {
            val hadContent = state.content != null && state.load is PayoutsLoad.Loaded
            val failed = content == null || (mode != LoadMode.Loud && hadContent && lostSummary)
            return when {
                !failed ->
                    state.copy(
                        load = PayoutsLoad.Loaded,
                        content = content,
                        summary = snapshot.summary,
                        onboarding = onboarding,
                        isRefreshing = false,
                        recentRows = snapshot.ledger?.transactions ?: keptRows(state, mode),
                        hasMoreActivity = hasMore(snapshot.ledger, state, mode),
                    )
                mode == LoadMode.Loud ->
                    state.copy(
                        load = PayoutsLoad.Failed(snapshot.summaryError?.toSectionMessage() ?: GENERIC_FAILURE),
                        isRefreshing = false,
                    )
                mode == LoadMode.Pull -> state.copy(refreshFailed = true, isRefreshing = false)
                else -> state.copy(isRefreshing = false)
            }
        }

        /** A reload that lost only the ledger keeps the rows already shown; a first load shows none. */
        private fun keptRows(
            state: PayoutsUiState,
            mode: LoadMode,
        ) = if (mode == LoadMode.Loud) emptyList() else state.recentRows

        private fun hasMore(
            ledger: EarningsTransactionsData?,
            state: PayoutsUiState,
            mode: LoadMode,
        ) = if (ledger != null) {
            ledger.pagination?.hasMoreOrDerived == true
        } else {
            mode != LoadMode.Loud && state.hasMoreActivity
        }

        private suspend fun fetch(): Snapshot =
            coroutineScope {
                val status =
                    async {
                        try {
                            financial.connectStatus()
                        } catch (_: ApiException) {
                            null
                        }
                    }
                val summary =
                    async {
                        try {
                            financial.payoutSummary() to null
                        } catch (e: ApiException) {
                            null to e
                        }
                    }
                val held =
                    async {
                        try {
                            heldBookingCount(bookings.list(TYPE_UPCOMING, page = 1, limit = HELD_PAGE_SIZE).bookings)
                        } catch (_: ApiException) {
                            0
                        }
                    }
                val ledger =
                    async {
                        try {
                            financial.transactions(page = 1, limit = TRANSACTIONS_PAGE_SIZE)
                        } catch (_: ApiException) {
                            null
                        }
                    }
                val (summaryData, summaryError) = summary.await()
                Snapshot(
                    status = status.await(),
                    summary = summaryData,
                    summaryWas400 = (summaryError as? ApiException.Http)?.status == HTTP_BAD_REQUEST,
                    summaryError = summaryError,
                    heldCount = held.await(),
                    ledger = ledger.await(),
                )
            }

        private companion object {
            const val HTTP_BAD_REQUEST = 400
            const val HELD_PAGE_SIZE = 100
            val GENERIC_FAILURE = FieldError.Res(R.string.login_error_unknown)
        }
    }
