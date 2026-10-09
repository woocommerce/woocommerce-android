package com.woocommerce.android.cardreader.internal.wrappers

import android.app.Application
import android.hardware.usb.UsbManager

internal class UsbManagerWrapper(private val application: Application) {
    fun isCardReaderAttached(): Boolean {
        val usbManager = application.getSystemService(UsbManager::class.java)
        return usbManager.deviceList.values.any { device ->
            CardReaderUsbId(device.vendorId, device.productId) in STRIPE_CARD_READER_USB_IDS
        }
    }

    private data class CardReaderUsbId(val vendorId: Int, val productId: Int)

    private companion object {
        val STRIPE_CARD_READER_USB_IDS = setOf(
            CardReaderUsbId(vendorId = 11369, productId = 22352),
            CardReaderUsbId(vendorId = 5538, productId = 257),
        )
    }
}
