package com.woocommerce.android.ui.login.qrlogin

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.network.qrlogin.QrLoginExchangeException
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Authenticating
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Confirming
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Error
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.Idle
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState.WarningSessionReplace
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
    private val accountRepository: AccountRepository,
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
                _uiState.value = makeErrorState(ErrorReason.Scanner, ticket = null)
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
                _uiState.value = makeErrorState(ErrorReason.InstallQrCode, ticket = null)
            }
            QrLoginPayload.Invalid -> {
                trackScanFailure(
                    step = Step.PAYLOAD,
                    errorContext = null,
                    errorType = ErrorReason.InvalidPayload.name,
                )
                _uiState.value = makeErrorState(ErrorReason.InvalidPayload, ticket = null)
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
                    _uiState.value = makeErrorState(reason, ticket)
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
        val ticket = ((_uiState.value as? Error)?.primaryAction as? PrimaryAction.Retry)?.ticket ?: return
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
     * The merchant accepted the session-replace warning. Run the canonical logout via
     * [AccountRepository.logout] (which handles both wp.com OAuth and per-site app-password
     * sessions, plus prefs / analytics / Zendesk / DataStore cleanup) and then resume the
     * original payload action exactly as if the user had been signed out from the start.
     *
     * We flip into [Authenticating] for the duration of the logout so the UI gives feedback
     * while [AccountRepository.logout] talks to the server — for the Ticket path this is then
     * overwritten by [Confirming] so the merchant still sees the host-confirmation step.
     *
     * If logout fails (only the wp.com path can; it returns false without running cleanup, so
     * the access token and selected site are still in place), we must NOT resume — installing
     * fresh credentials on top of a live session is exactly what the warning exists to prevent.
     * Surface a generic error and let the merchant scan again, which re-shows the warning.
     */
    fun onConfirmSessionReplace() {
        val pending = (_uiState.value as? WarningSessionReplace)?.pending ?: return
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_CONFIRMED)
        _uiState.value = Authenticating
        launch {
            if (accountRepository.logout()) {
                resumePending(pending)
            } else {
                trackScanFailure(
                    step = Step.EXCHANGE,
                    errorContext = null,
                    errorType = SESSION_REPLACE_LOGOUT_FAILED,
                )
                _uiState.value = makeErrorState(ErrorReason.Network, ticket = null)
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

    /**
     * Builds the [Error] state for [reason], picking a [PrimaryAction] that matches what the
     * button can actually do. Retry-with-the-same-ticket is only offered for transient failures
     * (network / server / rate-limited) when we still hold the ticket; everything else routes to
     * the scanner. The label travels with the action so the button copy can never disagree with
     * its behavior.
     */
    private fun makeErrorState(reason: ErrorReason, ticket: QrLoginPayload.Ticket?): Error {
        val (title, body) = reason.toTitleAndBody()
        return Error(
            reason = reason,
            title = title,
            body = body,
            bodyArgs = reason.toBodyArgs(),
            primaryAction = reason.toPrimaryAction(ticket),
        )
    }

    private fun ErrorReason.toTitleAndBody(): Pair<Int, Int> = when (this) {
        ErrorReason.InvalidPayload ->
            R.string.login_qr_scanner_error_payload_title to R.string.login_qr_scanner_error_payload_body
        ErrorReason.InstallQrCode ->
            R.string.login_qr_scanner_error_install_qr_title to R.string.login_qr_scanner_error_install_qr_body
        ErrorReason.Scanner ->
            R.string.login_qr_scanner_error_generic_title to R.string.login_qr_scanner_error_generic_body
        ErrorReason.TokenRejected ->
            R.string.login_qr_scanner_error_token_title to R.string.login_qr_scanner_error_token_body
        ErrorReason.EndpointMissing ->
            R.string.login_qr_endpoint_missing_title to R.string.login_qr_endpoint_missing_body
        ErrorReason.RateLimited ->
            R.string.login_qr_scanner_error_rate_limited_title to R.string.login_qr_scanner_error_rate_limited_body
        ErrorReason.Network ->
            R.string.login_qr_scanner_error_network_title to R.string.login_qr_scanner_error_network_body
        ErrorReason.ServerError ->
            R.string.login_qr_scanner_error_server_title to R.string.login_qr_scanner_error_server_body
        ErrorReason.SiteAuthFailure ->
            R.string.login_qr_scanner_error_site_auth_title to R.string.login_qr_scanner_error_site_auth_body
        ErrorReason.NotAWooSite ->
            R.string.login_qr_scanner_error_not_woo_title to R.string.login_qr_scanner_error_not_woo_body
        ErrorReason.UserNotEligible ->
            R.string.login_qr_scanner_error_user_role_title to R.string.login_qr_scanner_error_user_role_body
        ErrorReason.Unknown ->
            R.string.login_qr_scanner_error_generic_title to R.string.login_qr_scanner_error_generic_body
    }

    /**
     * String-resource args substituted into the body via `%1$s`, `%2$s`, … placeholders. Strings
     * declare their own `<b>…</b>` markup around the placeholders so translators can't break it.
     */
    private fun ErrorReason.toBodyArgs(): List<Int> = when (this) {
        ErrorReason.InstallQrCode -> listOf(
            R.string.login_qr_scanner_error_install_qr_body_button,
            R.string.login_qr_prologue_url,
        )
        else -> emptyList()
    }

    private fun ErrorReason.toPrimaryAction(ticket: QrLoginPayload.Ticket?): PrimaryAction = when (this) {
        ErrorReason.RateLimited,
        ErrorReason.Network,
        ErrorReason.ServerError -> ticket?.let(PrimaryAction::Retry)
            ?: PrimaryAction.ScanAgain(R.string.login_qr_error_primary_retry)
        ErrorReason.Scanner -> PrimaryAction.ScanAgain(R.string.login_qr_error_primary_retry)
        ErrorReason.EndpointMissing -> PrimaryAction.ScanAgain(R.string.login_qr_endpoint_missing_retry)
        ErrorReason.InvalidPayload,
        ErrorReason.InstallQrCode,
        ErrorReason.TokenRejected,
        ErrorReason.SiteAuthFailure,
        ErrorReason.NotAWooSite,
        ErrorReason.UserNotEligible,
        ErrorReason.Unknown -> PrimaryAction.ScanAgain(R.string.login_qr_error_primary_scan)
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
        data class Error(
            val reason: ErrorReason,
            @StringRes val title: Int,
            @StringRes val body: Int,
            val bodyArgs: List<Int> = emptyList(),
            val primaryAction: PrimaryAction,
        ) : UiState
        data class WarningSessionReplace(val pending: PendingHandoff) : UiState
    }

    sealed interface PrimaryAction {
        @get:StringRes val label: Int

        /** Re-runs the exchange with the same ticket — only safe for transient failures. */
        data class Retry(val ticket: QrLoginPayload.Ticket) : PrimaryAction {
            override val label: Int = R.string.login_qr_error_primary_retry
        }

        /** Sends the user back to the scanner to capture a fresh code. */
        data class ScanAgain(@StringRes override val label: Int) : PrimaryAction
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
        const val SESSION_REPLACE_LOGOUT_FAILED = "session_replace_logout_failed"
    }
}
