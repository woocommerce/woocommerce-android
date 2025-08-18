package com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader

sealed class WooPosSettingsHardwareCardReaderUiState {
    object Disconnected : WooPosSettingsHardwareCardReaderUiState()
    data class Connected(
        val readerName: String,
        val batteryLevel: Float? = null,
        val firmwareVersion: String? = null,
        val isSoftwareUpdateAvailable: Boolean = false
    ) : WooPosSettingsHardwareCardReaderUiState()
}
