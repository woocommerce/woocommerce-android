package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Authenticating
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Error
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Idle
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.WaitingForApproval
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.WarningSessionReplace
import com.woocommerce.android.ui.login.qrlogin.flow.AuthPhase
import com.woocommerce.android.ui.login.qrlogin.flow.ErrorReason
import com.woocommerce.android.ui.login.qrlogin.flow.FailureStep
import com.woocommerce.android.ui.login.qrlogin.flow.FlowAnalyticsEvent
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
 * The ViewModel is flow-agnostic: it observes [FlowState] / [FlowAnalyticsEvent] from whichever
 * implementation the factory returned and forwards them into the UI and the analytics tracker.
 */
@HiltViewModel
class QrLoginScannerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val parser: QrLoginPayloadParser,
    private val flowFactory: QrLoginFlowFactory,
    private val accountRepository: AccountRepository,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {

    private val _uiState = MutableStateFlow<UiState>(Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var loggedIn = false
    private var currentFlow: QrLoginFlow? = null
    private var flowObserverJob: Job? = null

    fun onScanResult(status: CodeScannerStatus) {
        if (!isIdle()) return
        when (status) {
            is CodeScannerStatus.Success -> handlePayload(parser.parse(status.code))
            is CodeScannerStatus.Failure -> {
                trackScanFailure(
                    step = "scanner",
                    errorContext = status.type::class.java.simpleName,
                    errorType = ErrorReason.Scanner::class.simpleName,
                )
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

    private fun isIdle(): Boolean = !loggedIn && _uiState.value is Idle

    private fun handlePayload(payload: QrLoginPayload) {
        when (payload) {
            is QrLoginPayload.Ticket -> handleHandoff(PendingHandoff.RunFlow(payload))
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
        trackScanFailure(step = "payload", errorContext = null, errorType = reason::class.simpleName)
        _uiState.value = Error(reason = reason, retryable = false)
    }

    /**
     * Gate every QR hand-off on the user being signed out. If a session is already active we
     * surface a confirmation screen first; the user must opt in to replacing it before we run
     * [AccountRepository.logout] and resume the original action.
     */
    private fun handleHandoff(pending: PendingHandoff) {
        if (accountRepository.isUserLoggedIn()) {
            analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_WARNING_SHOWN)
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
                loggedIn = true
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_WP_COM_MAGIC_LINK)
                triggerEvent(Dispatch.OpenWpComMagicLinkUrl(url = pending.url))
            }
            is PendingHandoff.SiteUrlPrefill -> {
                loggedIn = true
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_SITE_URL_PREFILL)
                triggerEvent(Dispatch.RouteToSiteAddressEntry(siteUrl = pending.siteUrl))
            }
            is PendingHandoff.AppLoginCredentials -> {
                loggedIn = true
                trackAppLoginHandoff(flowValue = AnalyticsTracker.VALUE_NO_WP_COM)
                triggerEvent(
                    Dispatch.RouteToAppLoginCredentials(siteUrl = pending.siteUrl, username = pending.username)
                )
            }
            is PendingHandoff.AppLoginWpComEmail -> {
                loggedIn = true
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
            launch { flow.state.collect { applyFlowState(it) } }
            launch { flow.analyticsEvents.collect { trackFlowEvent(it) } }
        }
    }

    private fun applyFlowState(state: FlowState) {
        when (state) {
            FlowState.Initial -> Unit // ignore — VM owns its own pre/post-flow Idle transitions.
            is FlowState.Authenticating -> _uiState.value = Authenticating(phase = state.phase)
            is FlowState.WaitingForApproval -> _uiState.value = WaitingForApproval(
                sessionId = state.sessionId,
                realNumber = state.realNumber,
                subtitle = state.subtitle,
                expiresAtEpochMs = state.expiresAtEpochMs,
            )
            is FlowState.Failed -> _uiState.value = Error(reason = state.reason, retryable = state.retryable)
            is FlowState.Completed -> handleCompletion(state.completion)
        }
    }

    private fun handleCompletion(completion: FlowCompletion) {
        loggedIn = true
        when (completion) {
            is FlowCompletion.LoggedIn -> {
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SUCCESS)
                triggerEvent(Dispatch.LoggedIn(localSiteId = completion.localSiteId))
            }
            is FlowCompletion.OpenMagicLink -> {
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_WP_COM_MAGIC_LINK)
                triggerEvent(Dispatch.OpenWpComMagicLinkUrl(url = completion.url))
            }
        }
    }

    private fun trackFlowEvent(event: FlowAnalyticsEvent) {
        if (event !is FlowAnalyticsEvent.Failure) return
        trackScanFailure(
            step = event.step.toAnalyticsKey(),
            errorContext = event.errorContext,
            errorType = event.reason::class.simpleName,
            extras = event.extras,
        )
    }

    private fun FailureStep.toAnalyticsKey(): String = when (this) {
        FailureStep.Scan -> "scan"
        FailureStep.Poll -> "poll"
        FailureStep.Approve -> "approve"
        FailureStep.Exchange -> "exchange"
        FailureStep.Auth -> "auth"
    }

    private fun trackAppLoginHandoff(flowValue: String) {
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_APP_LOGIN_LINK_SUCCESS,
            mapOf(
                AnalyticsTracker.KEY_FLOW to flowValue,
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_APP_LOGIN_SOURCE_QR
            )
        )
    }

    private fun trackScanFailure(
        step: String,
        errorContext: String?,
        errorType: String?,
        extras: Map<String, Any> = emptyMap(),
    ) {
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
            mapOf(AnalyticsTracker.KEY_STEP to step) + extras,
            errorContext = errorContext,
            errorType = errorType,
            errorDescription = null
        )
    }

    /**
     * Cancel the in-flight number-match step. The server keeps the session in `scanned`
     * until its 90-second window elapses; the merchant-side polling auto-transitions to the
     * "denied" terminal screen.
     */
    fun onCancelNumberMatch() {
        if (_uiState.value !is WaitingForApproval) return
        endActiveFlow()
        _uiState.value = Idle
    }

    /**
     * Reset the blocking state so the user can scan a fresh QR. Tokens are single-use, so we
     * never retry the same payload — the scanner reappears and the merchant generates a new code.
     */
    fun onStartOver() {
        if (loggedIn) return
        endActiveFlow()
        _uiState.value = Idle
    }

    /**
     * Re-runs the most recent retryable step. The active flow owns the retained input
     * (ticket / token / grant) so the VM just forwards the call.
     */
    fun onRetryExchange() {
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
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_CONFIRMED)
        _uiState.value = Authenticating(AuthPhase.Scan)
        launch {
            if (accountRepository.logout()) {
                resumePending(pending)
            } else {
                trackScanFailure(
                    step = "exchange",
                    errorContext = null,
                    errorType = SESSION_REPLACE_LOGOUT_FAILED,
                )
                _uiState.value = Error(reason = ErrorReason.Network, retryable = false)
            }
        }
    }

    fun onCancelSessionReplace() {
        if (_uiState.value !is WarningSessionReplace) return
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_DISMISSED)
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
