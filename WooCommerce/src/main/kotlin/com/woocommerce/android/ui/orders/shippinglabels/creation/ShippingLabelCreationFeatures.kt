package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.util.FeatureFlag

object ShippingLabelCreationFeatures {
    val CAN_CREATE_PAYMENT_METHOD = FeatureFlag.SHIPPING_LABELS_M4.isEnabled()
    val CAN_CREATE_PACKAGE = FeatureFlag.SHIPPING_LABELS_M4.isEnabled()
    const val CAN_CREATE_CUSTOMS_FORM = true
}
