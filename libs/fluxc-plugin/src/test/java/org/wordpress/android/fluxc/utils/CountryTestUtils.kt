package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.data.WCDataRestClient

object CountryTestUtils {
    private fun generateSampleCountry(
        code: String = "",
        name: String = "",
        parentCode: String = ""
    ): WCLocationModel {
        return WCLocationModel(
            code = code,
            name = name,
            parentCode = parentCode,
        )
    }

    fun generateCountries(): List<WCLocationModel> {
        return listOf(
            generateSampleCountry("CA", "Canada"),
            generateSampleCountry("ON", "Ontario", parentCode = "CA"),
            generateSampleCountry("BC", "British Columbia", parentCode = "CA"),
            generateSampleCountry("CZ", "Czech Republic")
        )
    }

    fun generateCountryApiResponse(): List<WCDataRestClient.CountryApiResponse> {
        return listOf(
            WCDataRestClient.CountryApiResponse(
                "CA", "Canada", listOf(
                    WCDataRestClient.CountryApiResponse.State("ON", "Ontario"),
                    WCDataRestClient.CountryApiResponse.State("BC", "British Columbia")
                )
            ),
            WCDataRestClient.CountryApiResponse("CZ", "Czech Republic", emptyList())
        )
    }
}
