package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingAddresses
import javax.inject.Inject

class ShouldRequireCustomsForm @Inject constructor() {
    operator fun invoke(addressData: WooShippingAddresses): Boolean {
        val isCountryEmpty = addressData.shipTo.address.country.code.isEmpty()
        val isDifferentCountry = addressData.isDifferentCountryShipment

        if (isCountryEmpty.not() && isDifferentCountry) return true

        val isOriginAddressMilitary = isAddressInMilitaryState(
            addressData.shipFrom.country,
            addressData.shipFrom.state.orEmpty()
        )
        val isShippingAddressMilitary = isAddressInMilitaryState(
            addressData.shipTo.address.country.code,
            addressData.shipTo.address.state.codeOrRaw
        )

        return isOriginAddressMilitary || isShippingAddressMilitary
    }

    private val WooShippingAddresses.isDifferentCountryShipment
        get() = shipFrom.country != shipTo.address.country.code

    private fun isAddressInMilitaryState(
        countryCode: String,
        stateCode: String
    ) = countryCode == US_COUNTRY_CODE && stateCode in US_MILITARY_STATES

    companion object {
        const val US_COUNTRY_CODE = "US"
        val US_MILITARY_STATES = arrayOf("AA", "AE", "AP")
    }
}
