package com.woocommerce.android.ui.orders

import android.support.v7.app.AppCompatActivity

// TODO: delete once UI test dependencies are removed
class AddOrderShipmentTrackingActivity : AppCompatActivity() {
    companion object {
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_TRACKING_NUMBER = "order-tracking-number"
        const val FIELD_ORDER_TRACKING_DATE_SHIPPED = "order-tracking-date-shipped"
        const val FIELD_ORDER_TRACKING_PROVIDER = "order-tracking-provider"
        const val FIELD_IS_CONFIRMING_DISCARD = "is-confirming-discard"
        const val FIELD_IS_CUSTOM_PROVIDER = "is-custom-provider"
        const val FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_NAME = "order-tracking-custom-provider-name"
        const val FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_URL = "order-tracking-custom-provider-url"
    }
}
