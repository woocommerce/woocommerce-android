package com.woocommerce.android.ui.woopos.settings.details.hardware.receiptprinter

sealed interface WooPosSettingsHardwareReceiptPrinterUiState {
    data object Disconnected : WooPosSettingsHardwareReceiptPrinterUiState

    data class Connected(val printerName: String) : WooPosSettingsHardwareReceiptPrinterUiState
}
