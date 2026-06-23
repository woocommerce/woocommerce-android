package com.woocommerce.android.ui.orders.wooshippinglabels.models

/**
 * Maps shipping carriers to tracking URLs when the Shipment Tracking plugin is unavailable.
 */
enum class ShipmentTrackingUrls(val trackingUrl: String) {
    USPS("https://tools.usps.com/go/TrackConfirmAction.action?tLabels=%s"),
    FEDEX("https://www.fedex.com/apps/fedextrack/?action=track&tracknumbers=%s"),
    UPS("https://www.ups.com/track?loc=en_US&tracknum=%s"),
    DHL("https://www.dhl.com/en/express/tracking.html?AWB=%s&brand=DHL");

    companion object {
        fun fromCarrier(carrierId: String, trackingNumber: String): String? {
            val shippingCarrier = when (carrierId) {
                "usps" -> USPS
                "fedex" -> FEDEX
                "ups" -> UPS
                "dhl" -> DHL
                else -> null
            }
            return shippingCarrier?.trackingUrl?.format(trackingNumber)
        }
    }
}
