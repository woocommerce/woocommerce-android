package com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader

sealed interface WooPosSettingsHardwareCardReaderUiState {
    data object Disconnected : WooPosSettingsHardwareCardReaderUiState

    sealed interface Connected : WooPosSettingsHardwareCardReaderUiState {
        val readerName: String

        data class Bluetooth(
            override val readerName: String,
            val batteryLevel: Float?,
            val firmwareVersion: String?,
            val isSoftwareUpdateAvailable: Boolean,
        ) : Connected

        data class Phone(
            override val readerName: String,
            val fingerprintSuffix: String?,
        ) : Connected
    }
}
