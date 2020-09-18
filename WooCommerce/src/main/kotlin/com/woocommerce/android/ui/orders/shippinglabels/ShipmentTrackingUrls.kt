package com.woocommerce.android.ui.orders.shippinglabels

/**
 * Maps the shipping carriers with the corresponding tracking urls since it is currently not possible
 * from the API to fetch tracking urls for a shipment without the Shipment Tracking plugin.
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
