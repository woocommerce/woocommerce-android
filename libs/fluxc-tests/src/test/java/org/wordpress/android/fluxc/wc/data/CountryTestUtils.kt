package org.wordpress.android.fluxc.wc.data

import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.data.WCDataRestClient.CountryApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.data.WCDataRestClient.CountryApiResponse.State

object CountryTestUtils {
    private fun generateSampleCountry(
        code: String = "",
        name: String = "",
        parentCode: String = ""
    ): WCLocationModel {
        return WCLocationModel().apply {
            this.code = code
            this.name = name
            this.parentCode = parentCode
        }
    }

    fun generateCountries(): List<WCLocationModel> {
        return listOf(
            generateSampleCountry("CA", "Canada"),
            generateSampleCountry("ON", "Ontario", parentCode = "CA"),
            generateSampleCountry("BC", "British Columbia", parentCode = "CA"),
            generateSampleCountry("CZ", "Czech Republic")
        )
    }

    fun generateCountryApiResponse(): List<CountryApiResponse> {
        return listOf(
            CountryApiResponse("CA", "Canada", listOf(
                    State("ON", "Ontario"),
                    State("BC", "British Columbia")
                )
            ),
            CountryApiResponse("CZ", "Czech Republic", emptyList())
        )
    }
}
