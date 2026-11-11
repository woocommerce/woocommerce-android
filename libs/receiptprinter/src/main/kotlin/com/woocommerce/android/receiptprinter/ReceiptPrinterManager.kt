package com.woocommerce.android.receiptprinter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ReceiptPrinterManager {
    val connectionStatus: StateFlow<ReceiptPrinterConnectionStatus>

    /**
     * Cold flow: collecting starts a discovery session and cancelling the collector stops it. Emits each printer
     * as it is found and completes when the session ends or after a [ReceiptPrinterDiscoveryEvent.Failed].
     */
    fun discover(): Flow<ReceiptPrinterDiscoveryEvent>

    /**
     * A newer call supersedes one still in flight: the superseded call never reports
     * [ReceiptPrinterConnectionStatus.Connected].
     */
    suspend fun connect(device: ReceiptPrinterDevice): ReceiptPrinterResult

    /**
     * Best effort: leaves the status at [ReceiptPrinterConnectionStatus.Disconnected] unless a newer [connect]
     * took over.
     */
    suspend fun disconnect()

    suspend fun printReceipt(text: String): ReceiptPrinterResult
}
