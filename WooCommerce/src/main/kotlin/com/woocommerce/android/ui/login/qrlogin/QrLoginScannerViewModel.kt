package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.network.qrlogin.QrLoginExchangeException
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Authenticating
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Confirming
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Error
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Idle
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import javax.inject.Inject

/**
 * Drives the QR login flow once the camera has produced a scan result:
 *
 *   1. Parse the deep link.
 *   2. Exchange the ticket for an Application Password.
 *   3. Persist credentials and resolve the selected site.
 *   4. Tell the activity to land in the main app via [Dispatch.LoggedIn].
 *
 * Recoverable failures (camera misread, invalid payload, network) keep the scanner running and
 * are tracked via [AnalyticsEvent.LOGIN_QR_SCAN_FAILED] with a [AnalyticsTracker.KEY_STEP] property.
 */
@HiltViewModel
class QrLoginScannerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val parser: QrLoginPayloadParser,
    private val authenticator: QrLoginAuthenticator,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {

    private val _uiState = MutableStateFlow<UiState>(Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var loggedIn = false

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
     * Reuses the same parse → confirm → exchange pipeline as a scanned QR, minus the camera.
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

    private fun handleHandoff(pending: PendingHandoff) {
        resumePending(pending)
    }

    private fun resumePending(pending: PendingHandoff) {
        when (pending) {
            is PendingHandoff.Ticket -> _uiState.value = Confirming(
                ticket = pending.ticket,
                host = pending.host
            )
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

    private fun startExchange(ticket: QrLoginPayload.Ticket) {
        _uiState.value = Authenticating
        launch {
            authenticator.authenticate(ticket).fold(
                onSuccess = { localSiteId ->
                    loggedIn = true
                    analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SUCCESS)
                    triggerEvent(Dispatch.LoggedIn(localSiteId))
                },
                onFailure = { failure ->
                    val reason = failure.toReason()
                    val httpCode = (failure as? QrLoginExchangeException.HttpError)?.code
                    trackScanFailure(
                        step = Step.EXCHANGE,
                        errorContext = failure.javaClass.simpleName,
                        errorType = reason.name,
                        extras = httpCode?.let { mapOf(AnalyticsTracker.KEY_ERROR_CODE to it) }.orEmpty()
                    )
                    _uiState.value = Error(
                        reason = reason,
                        retryTicket = ticket.takeIf { reason.isRetryEligible() }
                    )
                }
            )
        }
    }

    /**
     * Reset the blocking state so the user can scan a fresh QR. Tokens are single-use, so we
     * never retry the same payload — the scanner reappears and the merchant generates a new code.
     */
    fun onStartOver() {
        if (loggedIn) return
        _uiState.value = Idle
    }

    /**
     * Re-runs the exchange with the same ticket from the last attempt. Wired up to error
     * screens for transient failures (network, server, rate-limited) where the token is most
     * likely still valid. If the server actually consumed the token before the failure, the
     * retry surfaces [ErrorReason.TokenRejected] which then routes the user to the scanner
     * via the standard "Scan a new code" action.
     */
    fun onRetryExchange() {
        val ticket = (_uiState.value as? Error)?.retryTicket ?: return
        startExchange(ticket)
    }

    fun onConfirmSite() {
        val pending = _uiState.value as? Confirming ?: return
        startExchange(pending.ticket)
    }

    fun onCancelSite() {
        if (_uiState.value is Confirming) _uiState.value = Idle
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

    private fun ErrorReason.isRetryEligible(): Boolean = when (this) {
        ErrorReason.Network,
        ErrorReason.ServerError,
        ErrorReason.RateLimited -> true
        else -> false
    }

    private fun Throwable.toReason(): ErrorReason = when (this) {
        QrLoginExchangeException.TokenRejected -> ErrorReason.TokenRejected
        QrLoginExchangeException.EndpointMissing -> ErrorReason.EndpointMissing
        QrLoginExchangeException.RateLimited -> ErrorReason.RateLimited
        QrLoginExchangeException.Network -> ErrorReason.Network
        QrLoginExchangeException.MalformedResponse -> ErrorReason.ServerError
        is QrLoginExchangeException.HttpError -> ErrorReason.ServerError
        is QrLoginExchangeException.Unknown -> ErrorReason.Unknown
        QrLoginAuthenticationException.NotAWooSite -> ErrorReason.NotAWooSite
        is QrLoginAuthenticationException.UserNotEligible -> ErrorReason.UserNotEligible
        is CookieNonceAuthenticationException -> ErrorReason.SiteAuthFailure
        is OnChangedException -> (error as? SiteError)?.type.toErrorReason()
        // Catches DNS, socket, SSL handshake, and read failures during the post-exchange site
        // discovery + AP save chain — those layers throw raw IOException without going through
        // QrLoginExchangeException.
        is IOException -> ErrorReason.Network
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login: unmapped failure type ${this.javaClass.simpleName}")
            ErrorReason.Unknown
        }
    }

    private fun SiteErrorType?.toErrorReason(): ErrorReason = when (this) {
        SiteErrorType.UNAUTHORIZED,
        SiteErrorType.NOT_AUTHENTICATED -> ErrorReason.SiteAuthFailure
        SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR -> ErrorReason.Network
        else -> ErrorReason.Unknown
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
        data class Confirming(val ticket: QrLoginPayload.Ticket, val host: String) : UiState
        data object Authenticating : UiState
        data class Error(val reason: ErrorReason, val retryTicket: QrLoginPayload.Ticket?) : UiState
    }

    /**
     * Captures a parsed QR payload that has been routed through [handleHandoff]. Splitting the
     * dispatch into "build the pending action" and "resume the pending action" lets us insert
     * additional gates (e.g. session checks) between the two halves without duplicating the
     * per-payload branch logic.
     */
    sealed interface PendingHandoff {
        data class Ticket(val ticket: QrLoginPayload.Ticket, val host: String) : PendingHandoff
        data class WpComMagicLink(val url: String) : PendingHandoff
        data class SiteUrlPrefill(val siteUrl: String) : PendingHandoff
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
        Unknown
    }

    enum class Step {
        SCANNER, PAYLOAD, EXCHANGE
    }

    private companion object {
        const val HTTPS_DEFAULT_PORT = 443
        const val HTTP_DEFAULT_PORT = 80
    }
}
