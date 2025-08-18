package com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHardwareCardReaderViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow<WooPosSettingsHardwareCardReaderUiState>(WooPosSettingsHardwareCardReaderUiState.Disconnected)
    val uiState: StateFlow<WooPosSettingsHardwareCardReaderUiState> = _uiState.asStateFlow()

    init {
        observeCardReaderStatus()
    }

    private fun observeCardReaderStatus() {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect { status ->
                _uiState.value = when (status) {
                    is CardReaderStatus.Connected -> WooPosSettingsHardwareCardReaderUiState.Connected(
                        readerName = status.cardReader.id ?: "Unknown Reader",
                        batteryLevel = status.cardReader.currentBatteryLevel,
                        firmwareVersion = status.cardReader.firmwareVersion
                    )
                    is CardReaderStatus.Connecting -> WooPosSettingsHardwareCardReaderUiState.Connecting
                    is CardReaderStatus.NotConnected -> WooPosSettingsHardwareCardReaderUiState.Disconnected
                }
            }
        }
    }

    fun onConnectClicked() {
        cardReaderFacade.connectToReader()
    }

    fun onDisconnectClicked() {
        viewModelScope.launch {
            cardReaderFacade.disconnectFromReader()
        }
    }
}
