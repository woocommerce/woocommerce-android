package com.woocommerce.android.ui.orders.wooshippinglabels.models

enum class WooShippingCarrier(val carrierIds: List<String>) {
    FEDEX(listOf("fedex")),
    USPS(listOf("usps")),
    UPS(listOf("upsdap")),
    DHL(listOf("dhlexpress", "dhlecommerce", "dhlecommerceasia")),
    UNKNOWN(emptyList());

    companion object {
        fun fromCarrierId(carrierId: String): WooShippingCarrier {
            return entries.firstOrNull { it.carrierIds.contains(carrierId) } ?: UNKNOWN
        }
    }
}
