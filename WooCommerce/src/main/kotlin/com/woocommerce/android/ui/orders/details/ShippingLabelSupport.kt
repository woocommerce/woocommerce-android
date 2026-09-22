package com.woocommerce.android.ui.orders.details

enum class ShippingLabelSupport {
    NOT_SUPPORTED,
    WC_SHIPPING_SUPPORTED,
    WCS_SUPPORTED;

    fun isSupported() = this == WCS_SUPPORTED || this == WC_SHIPPING_SUPPORTED
    fun isWooTaxLegacySupported() = this == WCS_SUPPORTED
    fun isWooShippingSupported() = this == WC_SHIPPING_SUPPORTED
}
