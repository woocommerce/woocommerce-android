package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.SiteStore.SiteError
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

    private val _isAuthenticating = MutableLiveData(false)
    val isAuthenticating: LiveData<Boolean> = _isAuthenticating

    private val _endpointMissing = MutableLiveData(false)
    val endpointMissing: LiveData<Boolean> = _endpointMissing

    private val _pendingConfirmation = MutableLiveData<PendingConfirmation?>(null)
    val pendingConfirmation: LiveData<PendingConfirmation?> = _pendingConfirmation

    private var inFlight = false
    private var loggedIn = false

    fun onScanResult(status: CodeScannerStatus) {
        if (loggedIn || inFlight || _endpointMissing.value == true || _pendingConfirmation.value != null) return

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
            authenticator.authenticate(ticket).fold(
                onSuccess = { localSiteId ->
                    loggedIn = true
                    _isAuthenticating.value = false
                    analyticsTracker.track(AnalyticsEvent.LOGIN_QR_SUCCESS)
                    triggerEvent(Dispatch.LoggedIn(localSiteId))
                },
                onFailure = { failure ->
                    _isAuthenticating.value = false
                    inFlight = false
                    val reason = failure.toReason()
                    trackScanFailure(
                        step = Step.EXCHANGE,
                        errorContext = failure.javaClass.simpleName,
                        errorType = reason.name,
                        errorDescription = failure.message
                    )
                    if (reason == ErrorReason.EndpointMissing) {
                        _endpointMissing.value = true
                    } else {
                        triggerEvent(Dispatch.RecoverableError(reason))
                    }
                }
            )
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
        errorDescription: String?
    ) {
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
            mapOf(AnalyticsTracker.KEY_STEP to step.name.lowercase()),
            errorContext = errorContext,
            errorType = errorType,
            errorDescription = errorDescription
        )
    }

    private fun Throwable.toReason(): ErrorReason = when (this) {
        QrLoginExchangeException.TokenRejected,
        QrLoginExchangeException.MalformedResponse -> ErrorReason.TokenRejected
        QrLoginExchangeException.EndpointMissing -> ErrorReason.EndpointMissing
        QrLoginExchangeException.RateLimited -> ErrorReason.RateLimited
        QrLoginExchangeException.Network -> ErrorReason.Network
        is QrLoginExchangeException.HttpError -> ErrorReason.Network
        is QrLoginExchangeException.Unknown -> ErrorReason.Unknown
        QrLoginAuthenticationException.NotAWooSite -> ErrorReason.NotAWooSite
        is QrLoginAuthenticationException.UserNotEligible -> ErrorReason.UserNotEligible
        is CookieNonceAuthenticationException -> ErrorReason.SiteAuthFailure
        is OnChangedException -> when ((error as? SiteError)?.type) {
            null -> ErrorReason.Network
            else -> ErrorReason.SiteAuthFailure
        }
        else -> ErrorReason.Unknown
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
        SiteAuthFailure,
        NotAWooSite,
        UserNotEligible,
        Unknown
    }

    enum class Step {
        SCANNER, PAYLOAD, EXCHANGE
    }
}
