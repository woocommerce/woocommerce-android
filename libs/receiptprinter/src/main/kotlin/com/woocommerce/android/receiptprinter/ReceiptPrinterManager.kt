package com.woocommerce.android.receiptprinter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ReceiptPrinterManager {
    val connectionStatus: StateFlow<ReceiptPrinterConnectionStatus>

    fun discover(): Flow<ReceiptPrinterDevice>

    fun stopDiscovery()

    @Throws(ReceiptPrinterException::class)
    suspend fun connect(device: ReceiptPrinterDevice)

    @Throws(ReceiptPrinterException::class)
    suspend fun disconnect()

    @Throws(ReceiptPrinterException::class)
    suspend fun print(text: String)
}
