package com.woocommerce.android.ui.login.qrlogin

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
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
            is QrLoginPayload.Ticket -> _uiState.value = Confirming(
                ticket = payload,
                host = payload.siteUrl.toDisplayHost()
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
