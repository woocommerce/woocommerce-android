package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject
import kotlinx.coroutines.flow.flow

class CheckEUShippingScenario @Inject constructor(
    private val stateMachine: ShippingLabelsStateMachine
) {
    operator fun invoke() = flow {
        emit(FeatureFlag.EU_SHIPPING_NOTIFICATION.isEnabled())
    }
}
