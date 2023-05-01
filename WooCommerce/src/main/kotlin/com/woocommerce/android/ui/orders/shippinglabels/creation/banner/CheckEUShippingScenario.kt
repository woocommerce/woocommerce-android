package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.WaitingForInput
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StateMachineData
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
                is WaitingForInput -> emit(it.state.data.isEUShippingConditionMet())
                else -> emit(false)

            }
        }
    }

    operator fun invoke(data: StateMachineData) = data.isEUShippingConditionMet()

    private fun StateMachineData.isEUShippingConditionMet(): Boolean {
        val originCountry = stepsState.originAddressStep.data.country
        val destinationCountry = stepsState.shippingAddressStep.data.country

        return originCountry.code == US_COUNTRY_CODE &&
            destinationCountry.isEUCountryFollowingNewCustomRules()
    }

    private fun Location.isEUCountryFollowingNewCustomRules() =
        EU_COUNTRIES_FOLLOWING_CUSTOMS_RULES.any { countryIsoCode ->
            countryIsoCode.first == code || countryIsoCode.second == code
        }


    companion object {
        val US_COUNTRY_CODE = "US"
        val EU_COUNTRIES_FOLLOWING_CUSTOMS_RULES = listOf(
            Pair("AT", "AUT"),
            Pair("BE", "BEL"),
            Pair("BG", "BGR"),
            Pair("HR", "HRV"),
            Pair("CY", "CYP"),
            Pair("CZ", "CZE"),
            Pair("DK", "DNK"),
            Pair("EE", "EST"),
            Pair("FI", "FIN"),
            Pair("FR", "FRA"),
            Pair("DE", "DEU"),
            Pair("GR", "GRC"),
            Pair("HU", "HUN"),
            Pair("IE", "IRL"),
            Pair("IT", "ITA"),
            Pair("LV", "LVA"),
            Pair("LT", "LTU"),
            Pair("LU", "LUX"),
            Pair("MT", "MLT"),
            Pair("NL", "NLD"),
            Pair("NO", "NOR"),
            Pair("PL", "POL"),
            Pair("PT", "PRT"),
            Pair("RO", "ROU"),
            Pair("SK", "SVK"),
            Pair("SI", "SVN"),
            Pair("ES", "ESP"),
            Pair("SE", "SWE"),
            Pair("CH", "CHE")
        )
    }
}
