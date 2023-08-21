package com.woocommerce.android.ui.orders.creation.tax

sealed class TaxBasedOnSetting(open val key: String, open val label: String) {
    data class StoreAddress(override val key: String, override val label: String) :
        TaxBasedOnSetting(key, label)

    data class ShippingAddress(override val key: String, override val label: String) :
        TaxBasedOnSetting(key, label)

    data class BillingAddress(override val key: String, override val label: String) :
        TaxBasedOnSetting(key, label)
}
