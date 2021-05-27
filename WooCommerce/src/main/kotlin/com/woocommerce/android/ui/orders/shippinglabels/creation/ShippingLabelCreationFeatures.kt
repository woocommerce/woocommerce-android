package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.util.FeatureFlag

object ShippingLabelCreationFeatures {
    const val CAN_CREATE_PAYMENT_METHOD = false
    const val CAN_CREATE_PACKAGE = false
    val CAN_CREATE_CUSTOMS_FORM = FeatureFlag.SHIPPING_LABELS_M4.isEnabled()
}
