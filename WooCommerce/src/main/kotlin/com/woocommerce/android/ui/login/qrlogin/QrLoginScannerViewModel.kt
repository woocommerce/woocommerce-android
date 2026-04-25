package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
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
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import java.net.URI
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
 * are tracked via [AnalyticsEvent.LOGIN_QR_SCAN_FAILED] with a [KEY_STEP] property.
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

    private var inFlight = false
    private var loggedIn = false

    fun onScanResult(status: CodeScannerStatus) {
        if (isBusy()) return

        when (status) {
            is CodeScannerStatus.Success -> handlePayload(parser.parse(status.code))
            is CodeScannerStatus.Failure -> {
                trackScanFailure(
                    step = Step.SCANNER,
                    errorContext = status.type.toString(),
                    errorType = ErrorReason.Scanner.name,
                    errorDescription = status.error
                )
                triggerEvent(Dispatch.RecoverableError(ErrorReason.Scanner))
            }
            CodeScannerStatus.NotFound -> Unit
        }
    }

    /**
     * Entry point when the user opens a `woocommerce://qr-login?...` deep link from a browser.
     * Reuses the same parse → confirm → exchange pipeline as a scanned QR, minus the camera.
     */
    fun onDeepLinkPayload(raw: String) {
        if (isBusy()) return
        handlePayload(parser.parse(raw))
    }

    private fun isBusy(): Boolean =
        loggedIn || inFlight || _endpointMissing.value || _pendingConfirmation.value != null

    private fun handlePayload(payload: QrLoginPayload) {
        when (payload) {
            is QrLoginPayload.Ticket -> _pendingConfirmation.value = PendingConfirmation(
                ticket = payload,
                host = payload.siteUrl.toHostOrSelf()
            )
            QrLoginPayload.Invalid -> {
                trackScanFailure(
                    step = Step.PAYLOAD,
                    errorContext = null,
                    errorType = ErrorReason.InvalidPayload.name,
                    errorDescription = "Scanned QR did not match the expected deep link format"
                )
                triggerEvent(Dispatch.RecoverableError(ErrorReason.InvalidPayload))
            }
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
                        val httpCode = (failure as? QrLoginExchangeException.HttpError)?.code
                        trackScanFailure(
                            step = Step.EXCHANGE,
                            errorContext = this@QrLoginScannerViewModel::class.java.simpleName,
                            errorType = reason.name,
                            errorDescription = failure.message,
                            extras = httpCode?.let { mapOf(AnalyticsTracker.KEY_ERROR_CODE to it) }.orEmpty()
                        )
                        if (reason == ErrorReason.EndpointMissing) {
                            _endpointMissing.value = true
                        } else {
                            triggerEvent(Dispatch.RecoverableError(reason))
                        }
                    }
                )
            } finally {
                inFlight = false
                _isAuthenticating.value = false
            }
        }
    }

    fun onRetryAfterBlockingError() {
        _endpointMissing.value = false
        inFlight = false
    }

    fun onConfirmSite() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        startExchange(pending.ticket)
    }

    fun onCancelSite() {
        _pendingConfirmation.value = null
    }

    private fun String.toHostOrSelf(): String =
        runCatching { URI(this).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: this

    private fun trackScanFailure(
        step: Step,
        errorContext: String?,
        errorType: String?,
        errorDescription: String?,
        extras: Map<String, Any> = emptyMap()
    ) {
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
            mapOf(AnalyticsTracker.KEY_STEP to step.name.lowercase()) + extras,
            errorContext = errorContext,
            errorType = errorType,
            errorDescription = errorDescription
        )
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
                    "QR login: unmapped OnChangedException error type ${error?.javaClass?.simpleName}"
                )
            }
            siteError?.type.toErrorReason()
        }
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login: unmapped failure type ${this.javaClass.simpleName}", this)
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
        data class RecoverableError(val reason: ErrorReason) : Dispatch()
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
}
