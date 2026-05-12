package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.network.qrlogin.QrLoginCredentials
import com.woocommerce.android.network.qrlogin.QrLoginExchangeException
import com.woocommerce.android.network.qrlogin.QrLoginRestClient
import com.woocommerce.android.network.qrlogin.QrLoginScanResult
import com.woocommerce.android.network.qrlogin.QrLoginSessionStatus
import com.woocommerce.android.network.qrlogin.QrLoginSessionStatusException
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Authenticating
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Error
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Idle
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.WarningSessionReplace
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.WaitingForApproval
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

/**
 * Drives the QR login flow once the camera has produced a scan result:
 *
 *   1. Parse the deep link.
 *   2. Call /qr-login-scan to start the number-matching challenge.
 *   3. Show the real number on screen and poll /qr-login-session-status until the merchant
 *      taps the matching tile in wc-admin.
 *   4. On approval, exchange the resulting grant for an Application Password.
 *   5. Persist credentials, resolve the selected site, and tell the activity to land in the
 *      main app via [Dispatch.LoggedIn].
 *
 * Recoverable failures (camera misread, invalid payload, transient network) keep the
 * scanner running and are tracked via [AnalyticsEvent.LOGIN_QR_SCAN_FAILED] with a
 * [AnalyticsTracker.KEY_STEP] property.
 */
@HiltViewModel
class QrLoginScannerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val parser: QrLoginPayloadParser,
    private val restClient: QrLoginRestClient,
    private val authenticator: QrLoginAuthenticator,
    private val accountRepository: AccountRepository,
    private val errorMapper: QrLoginErrorMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {

    private val _uiState = MutableStateFlow<UiState>(Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var loggedIn = false
    private var pollJob: Job? = null

    fun onScanResult(status: CodeScannerStatus) {
        if (!isIdle()) return
        when (status) {
            is CodeScannerStatus.Success -> handlePayload(parser.parse(status.code))
            is CodeScannerStatus.Failure -> {
                trackScanFailure(
                    step = Step.SCANNER,
                    errorContext = status.type::class.java.simpleName,
                    errorType = ErrorReason.Scanner.name,
                )
                _uiState.value = Error(reason = ErrorReason.Scanner, retryTicket = null)
            }
            CodeScannerStatus.NotFound -> Unit
        }
    }

    /**
     * Entry point when the user opens a `woocommerce://qr-login?...` deep link from a browser.
     * Reuses the same parse → scan → approve → exchange pipeline as a scanned QR, minus the
     * camera.
     */
    fun onDeepLinkPayload(raw: String) {
        if (!isIdle()) return
        handlePayload(parser.parse(raw))
    }

    private fun isIdle(): Boolean = !loggedIn && _uiState.value is Idle

    private fun handlePayload(payload: QrLoginPayload) {
        when (payload) {
            is QrLoginPayload.Ticket -> handleHandoff(
                PendingHandoff.Ticket(ticket = payload, host = payload.siteUrl.toDisplayHost())
            )
            is QrLoginPayload.WpComMagicLinkUrl -> handleHandoff(
                PendingHandoff.WpComMagicLink(url = payload.url)
            )
            is QrLoginPayload.SiteUrl -> handleHandoff(
                PendingHandoff.SiteUrlPrefill(siteUrl = payload.siteUrl)
            )
            QrLoginPayload.InstallQrCode -> {
                trackScanFailure(
                    step = Step.PAYLOAD,
                    errorContext = null,
                    errorType = ErrorReason.InstallQrCode.name,
                )
                _uiState.value = Error(reason = ErrorReason.InstallQrCode, retryTicket = null)
            }
            QrLoginPayload.Invalid -> {
                trackScanFailure(
                    step = Step.PAYLOAD,
                    errorContext = null,
                    errorType = ErrorReason.InvalidPayload.name,
                )
                _uiState.value = Error(reason = ErrorReason.InvalidPayload, retryTicket = null)
            }
        }
    }

    /**
     * Gate every QR hand-off (ticket / wp.com magic link / site-URL prefill) on the user being
     * signed out. If a session is already active we surface a confirmation screen first; the
     * user must opt in to replacing it before we run [AccountRepository.logout] and resume the
     * original action.
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
            is PendingHandoff.Ticket -> startScan(pending.ticket)
            is PendingHandoff.WpComMagicLink -> {
                // Hand the URL off to the browser; wp.com then 3xx-redirects to
                // woocommerce://magic-login → MagicLinkInterceptActivity. Lock the state machine
                // because the user is leaving the scanner and the outcome is no longer ours to
                // handle. This is the happy path — we track the handoff, not a scan failure.
                loggedIn = true
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_WP_COM_MAGIC_LINK)
                triggerEvent(Dispatch.OpenWpComMagicLinkUrl(url = pending.url))
            }
            is PendingHandoff.SiteUrlPrefill -> {
                // Site-URL-only QR — no token to exchange, just route the merchant to the
                // existing site-address login screen with the URL prefilled and validation
                // auto-started. Same lock + happy-path tracking as the wp.com branch.
                loggedIn = true
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_SITE_URL_PREFILL)
                triggerEvent(Dispatch.RouteToSiteAddressEntry(siteUrl = pending.siteUrl))
            }
        }
    }

    private fun startScan(ticket: QrLoginPayload.Ticket) {
        _uiState.value = Authenticating(AuthPhase.ScanInFlight)
        launch {
            restClient.scan(ticket.siteUrl, ticket.token).fold(
                onSuccess = { scan -> beginWaitingForApproval(ticket, scan) },
                onFailure = { failure ->
                    val reason = errorMapper.toScanReason(failure)
                    trackScanFailure(
                        step = Step.SCAN,
                        errorContext = failure.javaClass.simpleName,
                        errorType = reason.name,
                    )
                    _uiState.value = Error(
                        reason = reason,
                        retryTicket = ticket.takeIf { errorMapper.isRetryEligible(reason) }
                    )
                }
            )
        }
    }

    private fun beginWaitingForApproval(ticket: QrLoginPayload.Ticket, scan: QrLoginScanResult) {
        val expiresAt = System.currentTimeMillis() + scan.expiresInSeconds * MILLIS_PER_SECOND
        _uiState.value = WaitingForApproval(
            ticket = ticket,
            host = ticket.siteUrl.toDisplayHost(),
            realNumber = scan.realNumber,
            sessionId = scan.sessionId,
            expiresAtEpochMs = expiresAt,
        )
        startPolling(ticket = ticket, sessionId = scan.sessionId)
    }

    private fun startPolling(ticket: QrLoginPayload.Ticket, sessionId: String) {
        pollJob?.cancel()
        WooLog.d(WooLog.T.LOGIN, "QR login poll: starting")
        pollJob = launch {
            var consecutiveErrors = 0
            // Fire one poll immediately on entry — no point waiting two seconds for the first
            // tick when the server-side state may already have advanced by the time we arrive.
            var firstTick = true
            while (_uiState.value is WaitingForApproval) {
                if (firstTick) {
                    firstTick = false
                } else {
                    delay(POLL_INTERVAL_MS)
                }
                if (_uiState.value !is WaitingForApproval) return@launch

                val callResult = restClient.checkSessionStatus(ticket.siteUrl, sessionId, ticket.token)
                // Guard after the await: cancel/start-over may have flipped state away from
                // WaitingForApproval while the call was in flight. If so, drop the response
                // on the floor so the user doesn't get bounced into a "Signing in…" spinner
                // they already cancelled out of.
                if (_uiState.value !is WaitingForApproval) return@launch
                callResult.fold(
                    onSuccess = { status ->
                        consecutiveErrors = 0
                        WooLog.d(WooLog.T.LOGIN, "QR login poll: response=$status")
                        if (handleStatus(ticket, status)) return@launch
                    },
                    onFailure = { failure ->
                        consecutiveErrors++
                        val reason = errorMapper.toPollReason(failure)
                        WooLog.w(
                            WooLog.T.LOGIN,
                            "QR login poll: failed (consecutive=$consecutiveErrors): $failure"
                        )
                        if (failure is QrLoginSessionStatusException.RateLimited ||
                            failure is QrLoginSessionStatusException.EndpointMissing ||
                            consecutiveErrors >= MAX_CONSECUTIVE_POLL_ERRORS
                        ) {
                            trackScanFailure(
                                step = Step.POLL,
                                errorContext = failure.javaClass.simpleName,
                                errorType = reason.name,
                            )
                            _uiState.value = Error(
                                reason = reason,
                                retryTicket = ticket.takeIf { errorMapper.isRetryEligible(reason) }
                            )
                            return@launch
                        }
                    }
                )
            }
            WooLog.d(WooLog.T.LOGIN, "QR login poll: loop exited (state=${_uiState.value::class.simpleName})")
        }
    }

    /** @return true if the polling loop should stop. */
    private fun handleStatus(ticket: QrLoginPayload.Ticket, status: QrLoginSessionStatus): Boolean = when (status) {
        QrLoginSessionStatus.Scanned -> false
        is QrLoginSessionStatus.Approved -> {
            startExchange(ticket = ticket, exchangeGrant = status.grant)
            true
        }
        QrLoginSessionStatus.Rejected -> {
            trackScanFailure(
                step = Step.APPROVE,
                errorContext = null,
                errorType = ErrorReason.MatchRejected.name,
            )
            _uiState.value = Error(reason = ErrorReason.MatchRejected, retryTicket = null)
            true
        }
        QrLoginSessionStatus.Expired -> {
            trackScanFailure(
                step = Step.APPROVE,
                errorContext = null,
                errorType = ErrorReason.MatchTimedOut.name,
            )
            _uiState.value = Error(reason = ErrorReason.MatchTimedOut, retryTicket = null)
            true
        }
    }

    private fun startExchange(ticket: QrLoginPayload.Ticket, exchangeGrant: String) {
        pollJob?.cancel()
        _uiState.value = Authenticating(AuthPhase.ExchangeInFlight)
        launch {
            val credentialsResult = restClient.exchange(ticket.siteUrl, ticket.token, exchangeGrant)
            credentialsResult.fold(
                onSuccess = { credentials -> completeLogin(ticket, credentials) },
                onFailure = { failure ->
                    val reason = errorMapper.toExchangeReason(failure)
                    val httpCode = (failure as? QrLoginExchangeException.HttpError)?.code
                    trackScanFailure(
                        step = Step.EXCHANGE,
                        errorContext = failure.javaClass.simpleName,
                        errorType = reason.name,
                        extras = httpCode?.let { mapOf(AnalyticsTracker.KEY_ERROR_CODE to it) }.orEmpty()
                    )
                    _uiState.value = Error(
                        reason = reason,
                        retryTicket = ticket.takeIf { errorMapper.isRetryEligible(reason) },
                        retryExchangeGrant = exchangeGrant.takeIf { errorMapper.isRetryEligible(reason) }
                    )
                }
            )
        }
    }

    private suspend fun completeLogin(ticket: QrLoginPayload.Ticket, credentials: QrLoginCredentials) {
        _uiState.value = Authenticating(AuthPhase.SiteFetchInFlight)
        authenticator.completeLogin(ticket, credentials).fold(
            onSuccess = { localSiteId ->
                loggedIn = true
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SUCCESS)
                triggerEvent(Dispatch.LoggedIn(localSiteId))
            },
            onFailure = { failure ->
                val reason = errorMapper.toAuthReason(failure)
                trackScanFailure(
                    step = Step.AUTH,
                    errorContext = failure.javaClass.simpleName,
                    errorType = reason.name,
                )
                _uiState.value = Error(reason = reason, retryTicket = null)
            }
        )
    }

    /**
     * Cancel the in-flight number-match step. The server keeps the session in `scanned`
     * until its 90-second window elapses; wc-admin's polling then auto-transitions to the
     * "denied" terminal screen. We don't call any cancel endpoint — the natural expiry
     * is the contract.
     */
    fun onCancelNumberMatch() {
        if (_uiState.value !is WaitingForApproval) return
        pollJob?.cancel()
        pollJob = null
        _uiState.value = Idle
    }

    /**
     * Reset the blocking state so the user can scan a fresh QR. Tokens are single-use, so we
     * never retry the same payload — the scanner reappears and the merchant generates a new code.
     */
    fun onStartOver() {
        if (loggedIn) return
        pollJob?.cancel()
        pollJob = null
        _uiState.value = Idle
    }

    /**
     * Re-runs the most recent step using the retained ticket. Used by error screens for
     * transient failures (network, rate-limit) where the token is most likely still valid.
     */
    fun onRetryExchange() {
        val error = _uiState.value as? Error ?: return
        val ticket = error.retryTicket ?: return
        error.retryExchangeGrant
            ?.let { exchangeGrant -> startExchange(ticket, exchangeGrant) }
            ?: startScan(ticket)
    }

    override fun onCleared() {
        pollJob?.cancel()
        pollJob = null
        super.onCleared()
    }

    /**
     * The merchant accepted the session-replace warning. Run the canonical logout via
     * [AccountRepository.logout] (which handles both wp.com OAuth and per-site app-password
     * sessions, plus prefs / analytics / Zendesk / DataStore cleanup) and then resume the
     * original payload action exactly as if the user had been signed out from the start.
     *
     * We flip into [Authenticating] for the duration of the logout so the UI gives feedback while
     * [AccountRepository.logout] talks to the server. For the Ticket path this is then overwritten
     * by the scan / number-match flow.
     *
     * If logout fails (only the wp.com path can; it returns false without running cleanup, so
     * the access token and selected site are still in place), we must NOT resume — installing
     * fresh credentials on top of a live session is exactly what the warning exists to prevent.
     * Surface a generic error and let the merchant scan again, which re-shows the warning.
     */
    fun onConfirmSessionReplace() {
        val pending = (_uiState.value as? WarningSessionReplace)?.pending ?: return
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_CONFIRMED)
        _uiState.value = Authenticating(AuthPhase.SessionReplaceInFlight)
        launch {
            if (accountRepository.logout()) {
                resumePending(pending)
            } else {
                trackScanFailure(
                    step = Step.EXCHANGE,
                    errorContext = null,
                    errorType = SESSION_REPLACE_LOGOUT_FAILED,
                )
                _uiState.value = Error(reason = ErrorReason.Network, retryTicket = null)
            }
        }
    }

    fun onCancelSessionReplace() {
        if (_uiState.value !is WarningSessionReplace) return
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_DISMISSED)
        _uiState.value = Idle
    }

    /**
     * Render the host portion the user is being asked to trust. We display the ASCII / punycode
     * form because OkHttp normalizes IDN hosts for us — homograph attacks like `my-stōre.example`
     * surface as `xn--my-stre-1za.example`, which the user can read accurately. Non-default ports
     * are surfaced explicitly. Falls back to the raw URL only if parsing fails (defence in depth;
     * the parser already gates this).
     */
    private fun String.toDisplayHost(): String {
        val parsed = this.toHttpUrlOrNull() ?: return this
        val defaultPort = if (parsed.scheme == "https") HTTPS_DEFAULT_PORT else HTTP_DEFAULT_PORT
        return if (parsed.port == defaultPort) parsed.host else "${parsed.host}:${parsed.port}"
    }

    private fun trackScanFailure(
        step: Step,
        errorContext: String?,
        errorType: String?,
        extras: Map<String, Any> = emptyMap()
    ) {
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
            mapOf(AnalyticsTracker.KEY_STEP to step.name.lowercase()) + extras,
            errorContext = errorContext,
            errorType = errorType,
            errorDescription = null
        )
    }

    sealed class Dispatch : Event() {
        data class LoggedIn(val localSiteId: Int) : Dispatch()

        /**
         * The merchant scanned a wp.com magic-login URL. The fragment hands [url] to the browser;
         * wp.com then 3xx-redirects to `woocommerce://magic-login` which the existing
         * intent-filter routes to `MagicLinkInterceptActivity` — the same end-to-end path a
         * 3rd-party scanner (Google Lens, etc.) takes today.
         */
        data class OpenWpComMagicLinkUrl(val url: String) : Dispatch()

        /**
         * The merchant scanned a `woocommerce://qr-login?siteUrl=…` deeplink without a token.
         * The fragment routes the merchant to the existing site-address login screen with
         * [siteUrl] prefilled and validation auto-started.
         */
        data class RouteToSiteAddressEntry(val siteUrl: String) : Dispatch()
    }

    sealed interface UiState {
        data object Idle : UiState
        data class Authenticating(val phase: AuthPhase) : UiState
        data class WaitingForApproval(
            val ticket: QrLoginPayload.Ticket,
            val host: String,
            val realNumber: String,
            val sessionId: String,
            val expiresAtEpochMs: Long,
        ) : UiState
        data class Error(
            val reason: ErrorReason,
            val retryTicket: QrLoginPayload.Ticket?,
            val retryExchangeGrant: String? = null,
        ) : UiState
        data class WarningSessionReplace(val pending: PendingHandoff) : UiState
    }

    /**
     * Captures a parsed QR payload that we deferred while showing the session-replace warning.
     * On confirm we run logout and then dispatch the matching action via [resumePending];
     * on cancel we simply drop it and return to [UiState.Idle].
     */
    sealed interface PendingHandoff {
        data class Ticket(val ticket: QrLoginPayload.Ticket, val host: String) : PendingHandoff
        data class WpComMagicLink(val url: String) : PendingHandoff
        data class SiteUrlPrefill(val siteUrl: String) : PendingHandoff
    }

    enum class AuthPhase {
        SessionReplaceInFlight,
        ScanInFlight,
        ExchangeInFlight,
        SiteFetchInFlight,
    }

    enum class ErrorReason {
        InvalidPayload,
        InstallQrCode,
        Scanner,
        TokenRejected,
        EndpointMissing,
        RateLimited,
        Network,
        ServerError,
        SiteAuthFailure,
        NotAWooSite,
        UserNotEligible,
        MatchRejected,
        MatchTimedOut,
        MatchAlreadyScanned,
        MatchInvalidGrant,
        Unknown
    }

    enum class Step {
        SCANNER, PAYLOAD, SCAN, POLL, APPROVE, EXCHANGE, AUTH
    }

    private companion object {
        const val HTTPS_DEFAULT_PORT = 443
        const val HTTP_DEFAULT_PORT = 80
        const val POLL_INTERVAL_MS = 2_000L
        const val MILLIS_PER_SECOND = 1_000L
        const val MAX_CONSECUTIVE_POLL_ERRORS = 4
        const val SESSION_REPLACE_LOGOUT_FAILED = "session_replace_logout_failed"
    }
}
