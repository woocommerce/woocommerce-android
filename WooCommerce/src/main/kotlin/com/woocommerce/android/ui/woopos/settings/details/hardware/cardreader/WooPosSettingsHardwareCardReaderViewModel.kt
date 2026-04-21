package com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionController
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionControllerFactory
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_LEARN_MORE_ABOUT_PAYMENTS
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_STRIPE_LEARN_MORE_ABOUT_PAYMENTS
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHardwareCardReaderViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val resourceProvider: ResourceProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    controllerFactory: WooPosCardReaderConnectionControllerFactory,
) : ViewModel() {

    private val controller: WooPosCardReaderConnectionController by lazy {
        controllerFactory.create(viewModelScope)
    }

    private val _uiState = MutableStateFlow<WooPosSettingsHardwareCardReaderUiState>(
        WooPosSettingsHardwareCardReaderUiState.Disconnected
    )
    val uiState: StateFlow<WooPosSettingsHardwareCardReaderUiState> = _uiState.asStateFlow()

    private val _openUrl = MutableSharedFlow<String>()
    val openUrl = _openUrl.asSharedFlow()

    private lateinit var softwareUpdateAvailabilityJob: Job
    private var batteryStatusJob: Job? = null
    private var currentSoftwareUpdateAvailable = false

    init {
        listenForSoftwareUpdateAvailability()
        observeCardReaderStatus()
    }

    fun onConnectClicked() {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(ChildToParentEvent.SettingsEvent.ShowCardReaderConnectionDialog)
        }
    }

    fun onDisconnectClicked() {
        viewModelScope.launch {
            controller.disconnect()
        }
    }

    fun onDocumentationClicked() {
        viewModelScope.launch {
            val preferredPlugin = appPrefsWrapper.getCardReaderPreferredPlugin(
                selectedSite.get().id,
                selectedSite.get().siteId,
                selectedSite.get().selfHostedSiteId
            )
            val learnMoreUrl = when (preferredPlugin) {
                STRIPE_EXTENSION_GATEWAY -> WOO_POS_STRIPE_LEARN_MORE_ABOUT_PAYMENTS
                WOOCOMMERCE_PAYMENTS, null -> WOO_POS_LEARN_MORE_ABOUT_PAYMENTS
            }
            _openUrl.emit(learnMoreUrl)
        }
    }

    fun onUpdateClick() {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(ChildToParentEvent.SettingsEvent.ShowCardReaderUpdateDialog)
        }
    }

    private fun listenForSoftwareUpdateAvailability() {
        softwareUpdateAvailabilityJob = viewModelScope.launch {
            cardReaderFacade.softwareUpdateAvailability.collect { updateAvailability ->
                handleSoftwareUpdateAvailability(updateAvailability)
            }
        }
    }

    private fun handleSoftwareUpdateAvailability(updateStatus: SoftwareUpdateAvailability) {
        currentSoftwareUpdateAvailable = updateStatus is SoftwareUpdateAvailability.Available

        val currentState = _uiState.value
        if (currentState is WooPosSettingsHardwareCardReaderUiState.Connected) {
            _uiState.value = currentState.copy(
                isSoftwareUpdateAvailable = currentSoftwareUpdateAvailable
            )
        }
    }

    private fun listenForBatteryStatus() {
        if (batteryStatusJob?.isActive == true) return
        batteryStatusJob = viewModelScope.launch {
            cardReaderFacade.batteryStatus.collect { status ->
                if (status is CardReaderBatteryStatus.StatusChanged) {
                    val currentState = _uiState.value
                    if (currentState is WooPosSettingsHardwareCardReaderUiState.Connected) {
                        _uiState.value = currentState.copy(
                            batteryLevel = status.batteryLevel
                        )
                    }
                }
            }
        }
    }

    private fun observeCardReaderStatus() {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect { status ->
                _uiState.value = when (status) {
                    is CardReaderStatus.Connected -> {
                        listenForBatteryStatus()
                        WooPosSettingsHardwareCardReaderUiState.Connected(
                            readerName = status.cardReader.id ?: resourceProvider.getString(
                                R.string.woopos_settings_card_reader_unknown_reader
                            ),
                            batteryLevel = status.cardReader.currentBatteryLevel,
                            firmwareVersion = status.cardReader.firmwareVersion,
                            isSoftwareUpdateAvailable = currentSoftwareUpdateAvailable
                        )
                    }

                    is CardReaderStatus.Connecting,
                    is CardReaderStatus.NotConnected -> {
                        cancelBatteryStatusJob()
                        currentSoftwareUpdateAvailable = false
                        WooPosSettingsHardwareCardReaderUiState.Disconnected
                    }

                    CardReaderStatus.Reconnecting -> {
                        // Keep current state while SDK attempts to reconnect
                        _uiState.value
                    }
                }
            }
        }
    }

    private fun cancelBatteryStatusJob() {
        batteryStatusJob?.cancel()
        batteryStatusJob = null
    }
}
