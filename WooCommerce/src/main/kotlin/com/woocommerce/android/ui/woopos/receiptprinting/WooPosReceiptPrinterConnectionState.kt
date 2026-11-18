package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterError

sealed interface WooPosReceiptPrinterConnectionState {
    data object Idle : WooPosReceiptPrinterConnectionState
    data class MissingBluetoothPermission(val permanentlyDenied: Boolean) : WooPosReceiptPrinterConnectionState
    data object BluetoothOff : WooPosReceiptPrinterConnectionState
    data object Searching : WooPosReceiptPrinterConnectionState
    data class FoundOne(val device: ReceiptPrinterDevice) : WooPosReceiptPrinterConnectionState
    data class FoundMultiple(val devices: List<ReceiptPrinterDevice>) : WooPosReceiptPrinterConnectionState
    data class Connecting(val device: ReceiptPrinterDevice) : WooPosReceiptPrinterConnectionState
    data class Connected(val device: ReceiptPrinterDevice) : WooPosReceiptPrinterConnectionState
    data object NoPrintersFound : WooPosReceiptPrinterConnectionState
    data object DiscoveryFailed : WooPosReceiptPrinterConnectionState
    data class ConnectionFailed(
        val device: ReceiptPrinterDevice,
        val error: ReceiptPrinterError,
    ) : WooPosReceiptPrinterConnectionState
}
