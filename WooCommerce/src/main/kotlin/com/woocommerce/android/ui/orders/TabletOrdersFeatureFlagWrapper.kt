package com.woocommerce.android.ui.orders

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class TabletOrdersFeatureFlagWrapper @Inject constructor() {
    operator fun invoke() = FeatureFlag.TABLET_ORDERS_M1.isEnabled()
}
