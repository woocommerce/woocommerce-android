package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import com.woocommerce.android.util.FeatureFlag

class CheckEUShippingScenario {
    operator fun invoke(): Boolean {
        return FeatureFlag.EU_SHIPPING_NOTIFICATION.isEnabled()
    }
}
