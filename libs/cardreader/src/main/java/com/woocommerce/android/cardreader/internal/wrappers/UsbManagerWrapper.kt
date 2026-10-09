package com.woocommerce.android.cardreader.internal.wrappers

import android.app.Application
import android.hardware.usb.UsbManager
import com.stripe.stripeterminal.R
import org.xmlpull.v1.XmlPullParser

internal class UsbManagerWrapper(private val application: Application) {
    private val stripeCardReaderUsbIds: Set<CardReaderUsbId> by lazy { readStripeCardReaderUsbIds() }

    fun isCardReaderAttached(): Boolean {
        val usbManager = application.getSystemService(UsbManager::class.java)
        return usbManager.deviceList.values.any { device ->
            CardReaderUsbId(device.vendorId, device.productId) in stripeCardReaderUsbIds
        }
    }

    // The Stripe SDK has no API to tell if a reader is plugged in by USB. Its own USB filter lists the IDs of
    // every reader it supports over USB, so reading it keeps new readers working after an SDK update.
    private fun readStripeCardReaderUsbIds(): Set<CardReaderUsbId> =
        application.resources.getXml(R.xml.usb_device_filter).use { parser ->
            generateSequence { parser.next() }
                .takeWhile { it != XmlPullParser.END_DOCUMENT }
                .filter { it == XmlPullParser.START_TAG && parser.name == USB_DEVICE_TAG }
                .map {
                    CardReaderUsbId(
                        vendorId = checkNotNull(parser.getAttributeValue(null, VENDOR_ID_ATTRIBUTE)).toInt(),
                        productId = checkNotNull(parser.getAttributeValue(null, PRODUCT_ID_ATTRIBUTE)).toInt(),
                    )
                }
                .toSet()
        }

    private data class CardReaderUsbId(val vendorId: Int, val productId: Int)

    private companion object {
        const val USB_DEVICE_TAG = "usb-device"
        const val VENDOR_ID_ATTRIBUTE = "vendor-id"
        const val PRODUCT_ID_ATTRIBUTE = "product-id"
    }
}
