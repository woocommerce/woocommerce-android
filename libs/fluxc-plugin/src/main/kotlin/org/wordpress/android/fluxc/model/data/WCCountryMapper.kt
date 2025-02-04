package org.wordpress.android.fluxc.model.data

import org.wordpress.android.fluxc.network.rest.wpcom.wc.data.WCDataRestClient.CountryApiResponse
import javax.inject.Inject

class WCCountryMapper
@Inject constructor() {
    fun map(country: CountryApiResponse): List<WCLocationModel> {
        return country.states.map { state ->
            WCLocationModel().apply {
                parentCode = country.code ?: ""
                name = state.name ?: ""
                code = state.code ?: ""
            }
        } + WCLocationModel().apply {
            name = country.name ?: ""
            code = country.code ?: ""
        }
    }
}
