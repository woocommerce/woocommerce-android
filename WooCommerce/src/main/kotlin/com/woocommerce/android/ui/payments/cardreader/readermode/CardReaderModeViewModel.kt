package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayError
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionDenied
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionExplainer
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.util.siteIdHash
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
) : ViewModel() {

    private val _viewState = MutableStateFlow<ViewState?>(null)
    val viewState: StateFlow<ViewState?> = _viewState.asStateFlow()

    private val _events = Channel<CardReaderModeEvent>(capacity = Channel.BUFFERED)
    val events: Flow<CardReaderModeEvent> = _events.receiveAsFlow()

    private var sessionStarted = false
    private var sessionStartedTracked = false
    private var sessionEndedTracked = false
    private var lastTrackedErrorDescription: String? = null
    private var isSimulated = false
    private var lastSessionState: CardReaderRemoteSessionState = CardReaderRemoteSessionState.Idle
    private var tabletEverConnected = false
    private var exitRequestedByUser = false

    fun onLocationPermissionMissing() {
        if (sessionStarted) return
        _viewState.value = RemoteTapToPayLocationPermissionExplainer(
            onPrimaryActionClicked = { _events.trySend(CardReaderModeEvent.RequestLocationPermission) },
        )
    }

    fun onLocationPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        when {
            granted -> startSessionIfNeeded()
            !shouldShowRationale -> _viewState.value = RemoteTapToPayLocationPermissionDenied(
                onPrimaryActionClicked = { _events.trySend(CardReaderModeEvent.OpenAppSettings) },
            )
            else -> onLocationPermissionMissing()
        }
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
        lastSessionState = state
        when (state) {
            CardReaderRemoteSessionState.Idle,
            CardReaderRemoteSessionState.Starting -> Unit
            is CardReaderRemoteSessionState.WaitingForPayment -> {
                tabletEverConnected = true
                trackSessionStartedOnce()
            }
            is CardReaderRemoteSessionState.ReadyToPair -> trackSessionStartedOnce()
            is CardReaderRemoteSessionState.Error -> {
                val errorDescription = state.errorDescription ?: state.message.orEmpty()
                if (errorDescription != lastTrackedErrorDescription) {
                    lastTrackedErrorDescription = errorDescription
                    analyticsTrackerWrapper.track(
                        AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_ERROR,
                        mapOf("error_description" to errorDescription),
                    )
                }
            }
        }
    }

    private fun trackSessionStartedOnce() {
        if (sessionStartedTracked) return
        sessionStartedTracked = true
        analyticsTrackerWrapper.track(
            AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_STARTED,
            mapOf("is_simulated" to isSimulated),
        )
    }

    override fun onCleared() {
        if (sessionStartedTracked && !sessionEndedTracked) {
            sessionEndedTracked = true
            analyticsTrackerWrapper.track(
                AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_ENDED,
                mapOf(
                    "reason" to sessionEndReason(),
                    "last_state" to lastSessionState.toAnalyticsValue(),
                    "tablet_connected" to tabletEverConnected,
                ),
            )
        }
        session.stop()
        super.onCleared()
    }

    // An error outranks the user closing the error screen: the error is why the session ended.
    private fun sessionEndReason(): String = when {
        lastSessionState is CardReaderRemoteSessionState.Error -> REASON_ERROR
        exitRequestedByUser -> REASON_USER_EXIT
        else -> REASON_DISMISSED
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
            message = state.message,
            onPrimaryActionClicked = ::exit,
        )
    }

    private fun exit() {
        exitRequestedByUser = true
        _events.trySend(CardReaderModeEvent.Exit)
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
