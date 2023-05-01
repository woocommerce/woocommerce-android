package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.WaitingForInput
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject
import kotlinx.coroutines.flow.flow

class CheckEUShippingScenario @Inject constructor(
    private val stateMachine: ShippingLabelsStateMachine
) {
    operator fun invoke() = flow {
        if (FeatureFlag.EU_SHIPPING_NOTIFICATION.isEnabled().not()) emit(false)

        stateMachine.transitions.collect {
            when (it.state) {
                is WaitingForInput -> emit(it.state.isEUShippingConditionMet())
                else -> emit(false)

            }
        }
    }

    private fun WaitingForInput.isEUShippingConditionMet(): Boolean {
        val originCountry = data.stepsState.originAddressStep.data.country
        val destinationCountry = data.stepsState.shippingAddressStep.data.country

        return originCountry.code == "US" && destinationCountry.code == "UK"
    }
}
