package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.remote.CardReaderRemoteCertificateKeyType
import com.woocommerce.android.cardreader.remote.CardReaderRemoteError
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayError
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocalNetworkPermissionDenied
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocalNetworkPermissionExplainer
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionDenied
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionExplainer
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.util.siteIdHash
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.util.UrlUtils
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CardReaderModeViewModel @Inject constructor(
    private val session: CardReaderRemoteSession,
    private val cardReaderManager: CardReaderManager,
    private val developerOptionsRepository: DeveloperOptionsRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    private val _viewState = MutableStateFlow<ViewState?>(null)
    val viewState: StateFlow<ViewState?> = _viewState.asStateFlow()

    private val _events = Channel<CardReaderModeEvent>(capacity = Channel.BUFFERED)
    val events: Flow<CardReaderModeEvent> = _events.receiveAsFlow()

    private var sessionStarted = false
    private var isSimulated = false
    private var tracking = SessionTracking()

    fun onPermissionsGranted() {
        startSessionIfNeeded()
    }

    fun onLocationPermissionMissing() {
        if (sessionStarted) return
        _viewState.value = RemoteTapToPayLocationPermissionExplainer(
            onPrimaryActionClicked = { _events.trySend(CardReaderModeEvent.RequestLocationPermission) },
        )
    }

    fun onLocationPermissionDenied() {
        if (sessionStarted) return
        _viewState.value = RemoteTapToPayLocationPermissionDenied(
            onPrimaryActionClicked = { _events.trySend(CardReaderModeEvent.OpenAppSettings) },
        )
    }

    fun onLocalNetworkPermissionMissing() {
        if (sessionStarted) return
        _viewState.value = RemoteTapToPayLocalNetworkPermissionExplainer(
            onPrimaryActionClicked = { _events.trySend(CardReaderModeEvent.RequestLocalNetworkPermission) },
        )
    }

    fun onLocalNetworkPermissionDenied() {
        if (sessionStarted) return
        _viewState.value = RemoteTapToPayLocalNetworkPermissionDenied(
            onPrimaryActionClicked = { _events.trySend(CardReaderModeEvent.OpenAppSettings) },
        )
    }

    private fun startSessionIfNeeded() {
        if (sessionStarted) return
        val siteHash = selectedSite.getOrNull()?.remoteId()?.value?.let(::siteIdHash) ?: return
        sessionStarted = true

        if (!cardReaderManager.initialized) {
            cardReaderManager.initialize(
                updateFrequency = developerOptionsRepository.getUpdateSimulatedReaderOption(),
                useInterac = developerOptionsRepository.isInteracPaymentEnabled(),
                useEftpos = developerOptionsRepository.isEftposPaymentEnabled(),
                isDebug = BuildConfig.DEBUG,
            )
        }
        isSimulated = developerOptionsRepository.isSimulatedCardReaderEnabled()
        viewModelScope.launch {
            session.state.collect { sessionState ->
                trackSessionState(sessionState)
                _viewState.value = mapToViewState(sessionState)
            }
        }
        session.start(
            parentScope = viewModelScope,
            siteHash = siteHash,
            deviceId = getOrCreateDeviceId(),
            isSimulated = isSimulated,
        )
    }

    private fun getOrCreateDeviceId(): String =
        appPrefsWrapper.wooPosRemoteReaderDeviceUUID.ifEmpty {
            UUID.randomUUID().toString().also { appPrefsWrapper.wooPosRemoteReaderDeviceUUID = it }
        }

    private fun trackSessionState(state: CardReaderRemoteSessionState) {
        tracking = tracking.observing(state)
        when (state) {
            CardReaderRemoteSessionState.Idle,
            CardReaderRemoteSessionState.Starting -> Unit
            is CardReaderRemoteSessionState.WaitingForPayment,
            is CardReaderRemoteSessionState.ReadyToPair -> trackSessionStartedOnce()
            is CardReaderRemoteSessionState.Error -> {
                val errorDescription = state.errorDescription ?: state.message.orEmpty()
                if (errorDescription != tracking.lastErrorDescription) {
                    tracking = tracking.copy(lastErrorDescription = errorDescription)
                    analyticsTrackerWrapper.track(
                        AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_ERROR,
                        mapOf("error_description" to errorDescription),
                    )
                }
            }
        }
    }

    private fun trackSessionStartedOnce() {
        if (tracking.startTracked) return
        tracking = tracking.copy(startTracked = true)
        analyticsTrackerWrapper.track(
            AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_STARTED,
            mapOf(
                "is_simulated" to isSimulated,
                "certificate_key_type" to certificateKeyTypeTrackingValue(),
            ),
        )
    }

    private fun certificateKeyTypeTrackingValue(): String = when (session.certificateKeyType) {
        CardReaderRemoteCertificateKeyType.ECDSA_256 -> "ecdsa_256"
        CardReaderRemoteCertificateKeyType.RSA_2048 -> "rsa_2048"
        null -> "unknown"
    }

    override fun onCleared() {
        if (tracking.startTracked && !tracking.endTracked) {
            tracking = tracking.copy(endTracked = true)
            analyticsTrackerWrapper.track(
                AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_ENDED,
                mapOf(
                    "reason" to tracking.endReason,
                    "last_state" to tracking.lastState.toAnalyticsValue(),
                    "tablet_connected" to tracking.tabletConnected,
                ),
            )
        }
        session.stop()
        super.onCleared()
    }

    private fun CardReaderRemoteSessionState.toAnalyticsValue(): String = when (this) {
        CardReaderRemoteSessionState.Idle -> "idle"
        CardReaderRemoteSessionState.Starting -> "starting"
        is CardReaderRemoteSessionState.ReadyToPair -> "ready_to_pair"
        is CardReaderRemoteSessionState.WaitingForPayment -> "waiting_for_payment"
        is CardReaderRemoteSessionState.Error -> "error"
    }

    private fun mapToViewState(state: CardReaderRemoteSessionState): ViewState? = when (state) {
        CardReaderRemoteSessionState.Idle -> null
        CardReaderRemoteSessionState.Starting -> RemoteTapToPayStarting(onPrimaryActionClicked = ::exit)
        is CardReaderRemoteSessionState.ReadyToPair -> RemoteTapToPayReadyToPair(
            deviceName = state.deviceName,
            fingerprintSuffix = state.fingerprintSuffix,
            siteUrl = selectedSiteDisplayUrl(),
            onPrimaryActionClicked = ::exit,
        )
        is CardReaderRemoteSessionState.WaitingForPayment -> RemoteTapToPayWaitingForPayment(
            tabletName = state.tabletName,
            onPrimaryActionClicked = ::exit,
        )
        is CardReaderRemoteSessionState.Error -> RemoteTapToPayError(
            message = errorMessageFor(state.error),
            onPrimaryActionClicked = ::exit,
        )
    }

    private fun errorMessageFor(error: CardReaderRemoteError): String? = when (error) {
        CardReaderRemoteError.PhoneNotEligible ->
            resourceProvider.getString(R.string.card_reader_mode_error_phone_not_eligible)
        CardReaderRemoteError.NfcDisabled ->
            resourceProvider.getString(R.string.card_reader_payment_failed_nfc_disabled)
        CardReaderRemoteError.TokenInvalid,
        CardReaderRemoteError.ConnectFailed,
        CardReaderRemoteError.CollectFailed,
        CardReaderRemoteError.CreateIntentFailed,
        CardReaderRemoteError.UnexpectedReply,
        is CardReaderRemoteError.Unknown -> null
    }

    private fun exit() {
        tracking = tracking.copy(userRequestedExit = true)
        _events.trySend(CardReaderModeEvent.Exit)
    }

    private data class SessionTracking(
        val startTracked: Boolean = false,
        val endTracked: Boolean = false,
        val lastErrorDescription: String? = null,
        val lastState: CardReaderRemoteSessionState = CardReaderRemoteSessionState.Idle,
        val tabletConnected: Boolean = false,
        val userRequestedExit: Boolean = false,
    ) {
        val endReason: String
            get() = when {
                lastState is CardReaderRemoteSessionState.Error -> REASON_ERROR
                userRequestedExit -> REASON_USER_EXIT
                else -> REASON_DISMISSED
            }

        fun observing(state: CardReaderRemoteSessionState) = copy(
            lastState = state,
            tabletConnected = tabletConnected || state is CardReaderRemoteSessionState.WaitingForPayment,
        )
    }

    private fun selectedSiteDisplayUrl(): String? =
        selectedSite.getOrNull()?.url
            ?.takeIf { it.isNotBlank() }
            ?.let(UrlUtils::removeScheme)
            ?.trim('/')

    private companion object {
        const val REASON_USER_EXIT = "user_exit"
        const val REASON_ERROR = "error"
        const val REASON_DISMISSED = "dismissed"
    }
}
