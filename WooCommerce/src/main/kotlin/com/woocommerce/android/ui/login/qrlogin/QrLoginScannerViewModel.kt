package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Drives the QR login flow once the camera has produced a scan result.
 */
@HiltViewModel
class QrLoginScannerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val parser: QrLoginPayloadParser,
    private val authenticator: QrLoginAuthenticator,
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
}
