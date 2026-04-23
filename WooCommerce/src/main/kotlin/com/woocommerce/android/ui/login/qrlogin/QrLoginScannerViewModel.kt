package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import javax.inject.Inject

/**
 * Drives the QR login flow once the camera has produced a scan result:
 *
 *   1. Parse the deep link.
 *   2. Exchange the ticket for an Application Password.
 *   3. Persist credentials and resolve the selected site.
 *   4. Tell the activity to land in the main app via [Dispatch.LoggedIn].
 *
 * Recoverable failures (camera misread, invalid payload, network) keep the scanner running.
 */
@HiltViewModel
class QrLoginScannerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val parser: QrLoginPayloadParser,
    private val authenticator: QrLoginAuthenticator
) : ScopedViewModel(savedState) {

    private val _isAuthenticating = MutableLiveData(false)
    val isAuthenticating: LiveData<Boolean> = _isAuthenticating

    private var inFlight = false
    private var loggedIn = false

    fun onScanResult(status: CodeScannerStatus) {
        if (loggedIn || inFlight) return

        when (status) {
            is CodeScannerStatus.Success -> handlePayload(parser.parse(status.code))
            is CodeScannerStatus.Failure -> triggerEvent(Dispatch.RecoverableError(ErrorReason.Scanner))
            CodeScannerStatus.NotFound -> Unit
        }
    }

    private fun handlePayload(payload: QrLoginPayload) {
        when (payload) {
            is QrLoginPayload.Ticket -> startExchange(payload)
            QrLoginPayload.Invalid -> triggerEvent(Dispatch.RecoverableError(ErrorReason.InvalidPayload))
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
                    triggerEvent(Dispatch.LoggedIn(localSiteId))
                },
                onFailure = { failure ->
                    _isAuthenticating.value = false
                    inFlight = false
                    triggerEvent(Dispatch.RecoverableError(failure.toReason()))
                }
            )
        }
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
}
