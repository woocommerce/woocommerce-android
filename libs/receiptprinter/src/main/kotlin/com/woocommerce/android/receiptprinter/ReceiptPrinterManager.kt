package com.woocommerce.android.receiptprinter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ReceiptPrinterManager {
    val connectionStatus: StateFlow<ReceiptPrinterConnectionStatus>

    /**
     * Emits each printer as it is found and completes when the discovery session ends or [stopDiscovery]
     * is called. Printers found earlier in the same session are not re-emitted. Fails with a
     * [ReceiptPrinterException] carrying [ReceiptPrinterError.DiscoveryFailure] when the scan cannot run.
     */
    fun discover(): Flow<ReceiptPrinterDevice>

    /**
     * Ends the current discovery session, completing the flow returned by [discover].
     */
    fun stopDiscovery()

    /**
     * A newer call supersedes one still in flight: the superseded call never reports
     * [ReceiptPrinterConnectionStatus.Connected]. Fails with a [ReceiptPrinterException] when the
     * connection cannot be established.
     */
    @Throws(ReceiptPrinterException::class)
    suspend fun connect(device: ReceiptPrinterDevice)

    /**
     * Best effort: never throws, and leaves the status at [ReceiptPrinterConnectionStatus.Disconnected]
     * unless a newer [connect] took over.
     */
    suspend fun disconnect()

    /**
     * Fails with a [ReceiptPrinterException] when no printer is connected or the printer reports an error.
     */
    @Throws(ReceiptPrinterException::class)
    suspend fun printReceipt(text: String)
}
