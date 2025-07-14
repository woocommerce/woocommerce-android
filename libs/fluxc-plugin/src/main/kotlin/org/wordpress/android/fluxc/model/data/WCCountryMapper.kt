package org.wordpress.android.fluxc.model.data

import org.wordpress.android.fluxc.network.rest.wpcom.wc.data.WCDataRestClient.CountryApiResponse

object WCCountryMapper {
    fun map(country: CountryApiResponse): List<WCLocationModel> {
        return country.states.map { state ->
            WCLocationModel(
                parentCode = country.code.orEmpty(),
                name = state.name.orEmpty(),
                code = state.code.orEmpty()
            )
        } + WCLocationModel(
            parentCode = "",
            name = country.name.orEmpty(),
            code = country.code.orEmpty()
        )
    }
}
