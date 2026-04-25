package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.network.qrlogin.QrLoginExchangeException
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import javax.inject.Inject

/**
 * Drives the QR login flow once the camera has produced a scan result.
 */
@HiltViewModel
class QrLoginScannerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val parser: QrLoginPayloadParser,
    private val authenticator: QrLoginAuthenticator,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _endpointMissing = MutableStateFlow(false)
    val endpointMissing: StateFlow<Boolean> = _endpointMissing.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _currentError = MutableStateFlow<ErrorReason?>(null)
    val currentError: StateFlow<ErrorReason?> = _currentError.asStateFlow()

    private var inFlight = false
    private var loggedIn = false

    fun onScanResult(status: CodeScannerStatus) {
        if (isBusy()) return

        when (status) {
            is CodeScannerStatus.Success -> handlePayload(parser.parse(status.code))
            is CodeScannerStatus.Failure -> {
                _currentError.value = ErrorReason.Scanner
            }
            CodeScannerStatus.NotFound -> Unit
        }
    }

    /**
     * Entry point when the user opens a `woocommerce://qr-login?...` deep link from a browser.
     * Reuses the same parse → confirm pipeline as a scanned QR, minus the camera.
     */
    fun onDeepLinkPayload(raw: String) {
        if (isBusy()) return
        handlePayload(parser.parse(raw))
    }

    private fun isBusy(): Boolean =
        loggedIn || inFlight || _endpointMissing.value ||
            _pendingConfirmation.value != null || _currentError.value != null

    private fun handlePayload(payload: QrLoginPayload) {
        when (payload) {
            is QrLoginPayload.Ticket -> _pendingConfirmation.value = PendingConfirmation(
                ticket = payload,
                host = payload.siteUrl.toDisplayHost()
            )
            QrLoginPayload.Invalid -> _currentError.value = ErrorReason.InvalidPayload
        }
    }

    private fun startExchange(ticket: QrLoginPayload.Ticket) {
        inFlight = true
        _isAuthenticating.value = true
        launch {
            try {
                authenticator.authenticate(ticket).fold(
                    onSuccess = { localSiteId ->
                        loggedIn = true
                        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SUCCESS)
                        triggerEvent(Dispatch.LoggedIn(localSiteId))
                    },
                    onFailure = { failure ->
                        val reason = failure.toReason()
                        if (reason == ErrorReason.EndpointMissing) {
                            _endpointMissing.value = true
                        } else {
                            _currentError.value = reason
                        }
                    }
                )
            } finally {
                inFlight = false
                _isAuthenticating.value = false
            }
        }
    }

    fun onConfirmSite() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        startExchange(pending.ticket)
    }

    fun onCancelSite() {
        _pendingConfirmation.value = null
    }

    /**
     * Reset the blocking state so the user can scan a fresh QR. Tokens are single-use, so we
     * never retry the same payload — the scanner reappears and the merchant generates a new code.
     */
    fun onStartOver() {
        _endpointMissing.value = false
        _currentError.value = null
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

    private fun Throwable.toReason(): ErrorReason = when (this) {
        QrLoginExchangeException.TokenRejected -> ErrorReason.TokenRejected
        QrLoginExchangeException.EndpointMissing -> ErrorReason.EndpointMissing
        QrLoginExchangeException.RateLimited -> ErrorReason.RateLimited
        QrLoginExchangeException.Network -> ErrorReason.Network
        QrLoginExchangeException.MalformedResponse -> ErrorReason.ServerError
        is QrLoginExchangeException.HttpError -> {
            WooLog.w(WooLog.T.LOGIN, "QR login exchange returned HTTP $code")
            ErrorReason.ServerError
        }
        is QrLoginExchangeException.Unknown -> ErrorReason.Unknown
        QrLoginAuthenticationException.NotAWooSite -> ErrorReason.NotAWooSite
        is QrLoginAuthenticationException.UserNotEligible -> ErrorReason.UserNotEligible
        is CookieNonceAuthenticationException -> ErrorReason.SiteAuthFailure
        is OnChangedException -> {
            val siteError = error as? SiteError
            if (siteError == null) {
                WooLog.w(
                    WooLog.T.LOGIN,
                    "QR login: unmapped OnChangedException error type ${error.javaClass.simpleName}"
                )
            }
            siteError?.type.toErrorReason()
        }
        else -> {
            WooLog.e(WooLog.T.LOGIN, "QR login: unmapped failure type ${this.javaClass.simpleName}", this)
            ErrorReason.Unknown
        }
    }

    private fun SiteErrorType?.toErrorReason(): ErrorReason = when (this) {
        SiteErrorType.UNAUTHORIZED,
        SiteErrorType.NOT_AUTHENTICATED -> ErrorReason.SiteAuthFailure
        SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR -> ErrorReason.Network
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login: unmapped SiteErrorType $this")
            ErrorReason.Unknown
        }
    }

    sealed class Dispatch : Event() {
        data class LoggedIn(val localSiteId: Int) : Dispatch()
    }

    data class PendingConfirmation(
        val ticket: QrLoginPayload.Ticket,
        val host: String
    )

    enum class ErrorReason {
        InvalidPayload,
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
