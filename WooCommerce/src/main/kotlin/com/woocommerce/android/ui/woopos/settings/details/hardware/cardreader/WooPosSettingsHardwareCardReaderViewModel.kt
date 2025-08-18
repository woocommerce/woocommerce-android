package com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHardwareCardReaderViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<WooPosSettingsHardwareCardReaderUiState>(
        WooPosSettingsHardwareCardReaderUiState.Disconnected
    )
    val uiState: StateFlow<WooPosSettingsHardwareCardReaderUiState> = _uiState.asStateFlow()

    init {
        observeCardReaderStatus()
    }

    private fun observeCardReaderStatus() {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect { status ->
                _uiState.value = when (status) {
                    is CardReaderStatus.Connected -> WooPosSettingsHardwareCardReaderUiState.Connected(
                        readerName = status.cardReader.id ?: resourceProvider.getString(
                            R.string.woopos_settings_card_reader_unknown_reader
                        ),
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
