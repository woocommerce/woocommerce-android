package com.woocommerce.android.ui.login.qrlogin

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Authenticating
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Error
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Idle
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.WaitingForApproval
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.WarningSessionReplace
import com.woocommerce.android.ui.login.qrlogin.flow.AuthPhase
import com.woocommerce.android.ui.login.qrlogin.flow.ErrorReason
import com.woocommerce.android.ui.login.qrlogin.flow.FlowCompletion
import com.woocommerce.android.ui.login.qrlogin.flow.FlowState
import com.woocommerce.android.ui.login.qrlogin.flow.QrLoginFlow
import com.woocommerce.android.ui.login.qrlogin.flow.QrLoginFlowFactory
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the QR login flow once a payload is in hand.
 *
 *   1. Parse the deep link.
 *   2. If it's a flow payload (Ticket / WpComToken later), delegate the scan → number-match →
 *      exchange protocol to a [QrLoginFlow] picked by [QrLoginFlowFactory].
 *   3. If it's a non-flow payload (site-URL prefill, legacy app-login, wp.com magic-link),
 *      emit the matching [Dispatch] event so the fragment can hand off to the existing login UI.
 *   4. Gate every action on the user being signed out — if a session is active we surface a
 *      replace-session warning first and only proceed after logout.
 *
 * The ViewModel is flow-agnostic: it observes [FlowState] from whichever implementation the
 * factory returned and forwards it into the UI and the analytics tracker.
 */
@HiltViewModel
class QrLoginScannerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val parser: QrLoginPayloadParser,
    private val flowFactory: QrLoginFlowFactory,
    private val accountRepository: AccountRepository,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val unifiedLoginTracker: UnifiedLoginTracker,
) : ScopedViewModel(savedState) {

    private val _uiState = MutableStateFlow<UiState>(Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Set once we've fired a terminal [Dispatch] event — either a real sign-in
     * ([FlowCompletion.LoggedIn]) or a handoff to another surface (browser Custom Tab,
     * in-app login route). While true, [isIdle] rejects new scans and deep links so the
     * scanner doesn't fire a second handoff under the first. Cleared on [onScreenResumed]
     * so the user has a way back if the handoff doesn't bring them home (browser closed
     * without OAuth completing, back-stack pop from the in-app login screen).
     */
    private var terminalEventDispatched = false
    private var currentFlow: QrLoginFlow? = null
    private var flowObserverJob: Job? = null

    fun onScanResult(status: CodeScannerStatus) {
        if (!isIdle()) return
        when (status) {
            is CodeScannerStatus.Success -> handlePayload(parser.parse(status.code))
            is CodeScannerStatus.Failure -> {
                trackFailure(reason = ErrorReason.Scanner)
                _uiState.value = Error(reason = ErrorReason.Scanner, retryable = false)
            }
            CodeScannerStatus.NotFound -> Unit
        }
    }

    /**
     * Entry point when the user opens a `woocommerce://qr-login?...` deep link from a browser.
     * Reuses the same parse → route pipeline as a scanned QR, minus the camera.
     */
    fun onDeepLinkPayload(raw: String) {
        if (!isIdle()) return
        handlePayload(parser.parse(raw))
    }

    private fun isIdle(): Boolean = !terminalEventDispatched && _uiState.value is Idle

    private fun handlePayload(payload: QrLoginPayload) {
        when (payload) {
            is QrLoginPayload.Ticket -> handleHandoff(PendingHandoff.RunFlow(payload))
            is QrLoginPayload.WpComToken -> handleHandoff(PendingHandoff.RunFlow(payload))
            is QrLoginPayload.WpComMagicLinkUrl -> handleHandoff(PendingHandoff.WpComMagicLink(payload.url))
            is QrLoginPayload.SiteUrl -> handleHandoff(PendingHandoff.SiteUrlPrefill(payload.siteUrl))
            is QrLoginPayload.AppLogin.Credentials -> handleHandoff(
                PendingHandoff.AppLoginCredentials(siteUrl = payload.siteUrl, username = payload.username)
            )
            is QrLoginPayload.AppLogin.WpComEmail -> handleHandoff(
                PendingHandoff.AppLoginWpComEmail(siteUrl = payload.siteUrl, wpComEmail = payload.wpComEmail)
            )
            QrLoginPayload.InstallQrCode -> failPayload(ErrorReason.InstallQrCode)
            QrLoginPayload.Invalid -> failPayload(ErrorReason.InvalidPayload)
        }
    }

    private fun failPayload(reason: ErrorReason) {
        trackFailure(reason = reason)
        _uiState.value = Error(reason = reason, retryable = false)
    }

    /**
     * Gate every QR hand-off on the user being signed out. If a session is already active we
     * surface a confirmation screen first; the user must opt in to replacing it before we run
     * [AccountRepository.logout] and resume the original action.
     */
    private fun handleHandoff(pending: PendingHandoff) {
        if (accountRepository.isUserLoggedIn()) {
            unifiedLoginTracker.track(Flow.LOGIN_QR, Step.QR_SESSION_REPLACE_WARNING)
            _uiState.value = WarningSessionReplace(pending)
        } else {
            resumePending(pending)
        }
    }

    private fun resumePending(pending: PendingHandoff) {
        when (pending) {
            is PendingHandoff.RunFlow -> startFlow(pending.payload)
            is PendingHandoff.WpComMagicLink -> {
                // Hand the URL off to a Custom Tab; wp.com 3xx-redirects to
                // woocommerce://magic-login and MagicLinkInterceptActivity finishes sign-in.
                terminalEventDispatched = true
                unifiedLoginTracker.track(Flow.LOGIN_MAGIC_LINK, Step.MAGIC_LINK_REQUESTED)
                triggerEvent(Dispatch.OpenWpComMagicLinkUrl(url = pending.url))
            }
            is PendingHandoff.SiteUrlPrefill -> {
                terminalEventDispatched = true
                triggerEvent(Dispatch.RouteToSiteAddressEntry(siteUrl = pending.siteUrl))
            }
            is PendingHandoff.AppLoginCredentials -> {
                terminalEventDispatched = true
                trackAppLoginHandoff(flowValue = AnalyticsTracker.VALUE_NO_WP_COM)
                triggerEvent(
                    Dispatch.RouteToAppLoginCredentials(siteUrl = pending.siteUrl, username = pending.username)
                )
            }
            is PendingHandoff.AppLoginWpComEmail -> {
                terminalEventDispatched = true
                trackAppLoginHandoff(flowValue = AnalyticsTracker.VALUE_WP_COM)
                triggerEvent(
                    Dispatch.RouteToAppLoginWpComEmail(siteUrl = pending.siteUrl, wpComEmail = pending.wpComEmail)
                )
            }
        }
    }

    private fun startFlow(payload: QrLoginPayload) {
        val flow = flowFactory.create(payload, scope = this)
            ?: run {
                failPayload(ErrorReason.InvalidPayload)
                return
            }
        observeFlow(flow)
        flow.start()
    }

    private fun observeFlow(flow: QrLoginFlow) {
        flowObserverJob?.cancel()
        currentFlow = flow
        flowObserverJob = launch {
            flow.state.collect { applyFlowState(it) }
        }
    }

    private fun applyFlowState(state: FlowState) {
        when (state) {
            FlowState.Initial -> Unit // ignore — VM owns its own pre/post-flow Idle transitions.
            is FlowState.Authenticating -> {
                _uiState.value = Authenticating(phase = state.phase)
                // The three AuthPhase transitions (Scan → Exchange → Complete) all map to the
                // same funnel step, so emit one event when we first enter the phase and skip
                // the back-to-back duplicates.
                if (unifiedLoginTracker.currentStep != Step.QR_AUTHENTICATING) {
                    unifiedLoginTracker.track(Flow.LOGIN_QR, Step.QR_AUTHENTICATING)
                }
            }
            is FlowState.WaitingForApproval -> {
                _uiState.value = WaitingForApproval(
                    sessionId = state.sessionId,
                    realNumber = state.realNumber,
                    subtitleLabelRes = state.subtitleLabelRes,
                    subtitle = state.subtitle,
                    expiresAtEpochMs = state.expiresAtEpochMs,
                )
                unifiedLoginTracker.track(Flow.LOGIN_QR, Step.QR_NUMBER_MATCH)
            }
            is FlowState.Failed -> {
                _uiState.value = Error(reason = state.reason, retryable = state.retryable)
                trackFailure(reason = state.reason, failedAt = state.failedAt.name)
            }
            is FlowState.Completed -> handleCompletion(state.completion)
        }
    }

    private fun handleCompletion(completion: FlowCompletion) {
        terminalEventDispatched = true
        when (completion) {
            // Successful site login — SitePickerViewModel will fire UNIFIED_LOGIN_STEP(SUCCESS)
            // once the merchant lands on the epilogue, so no extra event here.
            is FlowCompletion.LoggedIn -> triggerEvent(Dispatch.LoggedIn(localSiteId = completion.localSiteId))
            // Hand off to the magic-link funnel — the next observed events come from
            // MagicLinkInterceptActivity, forming a continuous trail.
            is FlowCompletion.OpenMagicLink -> {
                unifiedLoginTracker.track(Flow.LOGIN_MAGIC_LINK, Step.MAGIC_LINK_REQUESTED)
                triggerEvent(Dispatch.OpenWpComMagicLinkUrl(url = completion.url))
            }
        }
    }

    /**
     * `LOGIN_APP_LOGIN_LINK_SUCCESS` is the original deeplink-era event (in production since 2023)
     * — keep firing it with `source=qr` so the existing dashboards capture QR-originated app-login
     * scans alongside browser deeplinks.
     */
    private fun trackAppLoginHandoff(flowValue: String) {
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_APP_LOGIN_LINK_SUCCESS,
            mapOf(
                AnalyticsTracker.KEY_FLOW to flowValue,
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_APP_LOGIN_SOURCE_QR
            )
        )
    }

    private fun trackFailure(reason: ErrorReason, failedAt: String? = null) {
        unifiedLoginTracker.setStep(Step.QR_ERROR)
        val description = reason::class.simpleName.orEmpty()
            .let { simpleName -> if (failedAt != null) "$simpleName:$failedAt" else simpleName }
        unifiedLoginTracker.trackFailure(description)
    }

    /**
     * Recovery hook for the case where a terminal handoff didn't carry the user home.
     * Examples: the Custom Tab was closed before the wp.com magic-link redirect fired,
     * or the user backed out of the in-app login route. Returning to the scanner with a
     * stale `terminalEventDispatched` would otherwise leave [isIdle] permanently false
     * and the UI frozen on whatever state we last set. Clearing it on resume drops us
     * back to a fresh scanner. Safe to call on every resume — it's a no-op if no terminal
     * event has been dispatched.
     */
    fun onScreenResumed() {
        if (!terminalEventDispatched) return
        terminalEventDispatched = false
        endActiveFlow()
        _uiState.value = Idle
    }

    /**
     * Cancel the in-flight number-match step. The server keeps the session in `scanned`
     * until its 90-second window elapses; the merchant-side polling auto-transitions to the
     * "denied" terminal screen.
     */
    fun onCancelNumberMatch() {
        if (_uiState.value !is WaitingForApproval) return
        unifiedLoginTracker.trackClick(Click.QR_CANCEL_NUMBER_MATCH)
        endActiveFlow()
        _uiState.value = Idle
    }

    /**
     * Reset the blocking state so the user can scan a fresh QR. Tokens are single-use, so we
     * never retry the same payload — the scanner reappears and the merchant generates a new code.
     */
    fun onStartOver() {
        if (terminalEventDispatched) return
        unifiedLoginTracker.trackClick(Click.QR_START_OVER)
        endActiveFlow()
        _uiState.value = Idle
    }

    /**
     * Re-runs the most recent retryable step. The active flow owns the retained input
     * (ticket / token / grant) so the VM just forwards the call.
     */
    fun onRetryExchange() {
        unifiedLoginTracker.trackClick(Click.QR_RETRY)
        currentFlow?.retry()
    }

    private fun endActiveFlow() {
        currentFlow?.cancel()
        flowObserverJob?.cancel()
        flowObserverJob = null
        currentFlow = null
    }

    override fun onCleared() {
        endActiveFlow()
        super.onCleared()
    }

    /**
     * The merchant accepted the session-replace warning. Run the canonical logout via
     * [AccountRepository.logout] (which handles both wp.com OAuth and per-site app-password
     * sessions, plus prefs / analytics / Zendesk / DataStore cleanup) and then resume the
     * original payload action exactly as if the user had been signed out from the start.
     *
     * We flip into [Authenticating] for the duration of the logout so the UI gives feedback while
     * [AccountRepository.logout] talks to the server.
     *
     * If logout fails (only the wp.com path can; it returns false without running cleanup, so
     * the access token and selected site are still in place), we must NOT resume — installing
     * fresh credentials on top of a live session is exactly what the warning exists to prevent.
     * Surface a generic error and let the merchant scan again, which re-shows the warning.
     */
    fun onConfirmSessionReplace() {
        val pending = (_uiState.value as? WarningSessionReplace)?.pending ?: return
        unifiedLoginTracker.trackClick(Click.SUBMIT)
        _uiState.value = Authenticating(AuthPhase.Scan)
        launch {
            if (accountRepository.logout()) {
                resumePending(pending)
            } else {
                trackFailure(reason = ErrorReason.Network, failedAt = SESSION_REPLACE_LOGOUT_FAILED)
                _uiState.value = Error(reason = ErrorReason.Network, retryable = false)
            }
        }
    }

    fun onCancelSessionReplace() {
        if (_uiState.value !is WarningSessionReplace) return
        unifiedLoginTracker.trackClick(Click.DISMISS)
        _uiState.value = Idle
    }

    sealed class Dispatch : Event() {
        data class LoggedIn(val localSiteId: Int) : Dispatch()

        /**
         * Either a wp.com magic-login QR was scanned directly, or the wp.com QR-app-login flow
         * completed and the server handed back a magic-link URL. The fragment opens [url] in a
         * Custom Tab; wp.com 3xx-redirects to `woocommerce://magic-login`, picked up by the
         * existing intent-filter on `MagicLinkInterceptActivity`.
         */
        data class OpenWpComMagicLinkUrl(val url: String) : Dispatch()

        data class RouteToSiteAddressEntry(val siteUrl: String) : Dispatch()
        data class RouteToAppLoginCredentials(val siteUrl: String, val username: String) : Dispatch()
        data class RouteToAppLoginWpComEmail(val siteUrl: String, val wpComEmail: String) : Dispatch()
    }

    sealed interface UiState {
        data object Idle : UiState
        data class Authenticating(val phase: AuthPhase) : UiState
        data class WaitingForApproval(
            val sessionId: String,
            val realNumber: String,
            @StringRes val subtitleLabelRes: Int,
            val subtitle: String,
            val expiresAtEpochMs: Long,
        ) : UiState
        data class Error(val reason: ErrorReason, val retryable: Boolean) : UiState
        data class WarningSessionReplace(val pending: PendingHandoff) : UiState
    }

    /**
     * Captures a parsed QR payload that we deferred while showing the session-replace warning.
     * On confirm we run logout and then dispatch the matching action via [resumePending];
     * on cancel we simply drop it and return to [UiState.Idle].
     */
    sealed interface PendingHandoff {
        data class RunFlow(val payload: QrLoginPayload) : PendingHandoff
        data class WpComMagicLink(val url: String) : PendingHandoff
        data class SiteUrlPrefill(val siteUrl: String) : PendingHandoff
        data class AppLoginCredentials(val siteUrl: String, val username: String) : PendingHandoff
        data class AppLoginWpComEmail(val siteUrl: String, val wpComEmail: String) : PendingHandoff
    }

    private companion object {
        const val SESSION_REPLACE_LOGOUT_FAILED = "session_replace_logout_failed"
    }
}
