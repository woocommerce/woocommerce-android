package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import javax.inject.Inject

class ShippingLabelAddressMapper @Inject constructor(private val getLocations: GetLocations) {
    fun toAppModel(dto: WCShippingLabelModel.ShippingLabelAddress): Address {
        val (countryLocation, stateLocation) = getLocations(dto.country.orEmpty(), dto.state.orEmpty())
        return Address(
            company = dto.company ?: "",
            firstName = dto.name ?: "",
            lastName = "",
            phone = dto.phone ?: "",
            country = countryLocation,
            state = stateLocation,
            address1 = dto.address ?: "",
            address2 = dto.address2 ?: "",
            city = dto.city ?: "",
            postcode = dto.postcode ?: "",
            email = ""
        )
    }
}
