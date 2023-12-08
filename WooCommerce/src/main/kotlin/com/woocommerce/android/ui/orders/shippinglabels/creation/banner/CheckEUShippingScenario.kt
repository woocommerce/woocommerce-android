package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.WaitingForInput
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StateMachineData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Transition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CheckEUShippingScenario @Inject constructor() {
    operator fun invoke(shippingStateTransitions: Flow<Transition>) = flow {
        shippingStateTransitions.collect {
            when (it.state) {
                is WaitingForInput -> emit(it.state.data.isEUShippingConditionMet())
                else -> emit(false)
            }
        }
    }

    private fun StateMachineData.isEUShippingConditionMet(): Boolean {
        val originCountry = stepsState.originAddressStep.data.country
        val destinationCountry = stepsState.shippingAddressStep.data.country

        return originCountry.code == US_COUNTRY_CODE &&
            destinationCountry.code in EU_COUNTRIES_FOLLOWING_CUSTOMS_RULES
    }

    companion object {
        const val US_COUNTRY_CODE = "US"
        val EU_COUNTRIES_FOLLOWING_CUSTOMS_RULES = setOf(
            "AT", "AUT",
            "BE", "BEL",
            "BG", "BGR",
            "HR", "HRV",
            "CY", "CYP",
            "CZ", "CZE",
            "DK", "DNK",
            "EE", "EST",
            "FI", "FIN",
            "FR", "FRA",
            "DE", "DEU",
            "GR", "GRC",
            "HU", "HUN",
            "IE", "IRL",
            "IT", "ITA",
            "LV", "LVA",
            "LT", "LTU",
            "LU", "LUX",
            "MT", "MLT",
            "NL", "NLD",
            "NO", "NOR",
            "PL", "POL",
            "PT", "PRT",
            "RO", "ROU",
            "SK", "SVK",
            "SI", "SVN",
            "ES", "ESP",
            "SE", "SWE",
            "CH", "CHE"
        )
    }
}
