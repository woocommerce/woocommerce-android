package com.woocommerce.android.ui.orders.wooshippinglabels.address.origin

import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.address.GetAllCountries
import javax.inject.Inject

class GetAcceptedOriginCountries @Inject constructor(
    private val getCountries: GetAllCountries
) {
    suspend operator fun invoke(): Result<List<Location>> {
        val result = getCountries()
        return if (result.isSuccess) {
            val acceptedCountries = result.getOrThrow().filter { ACCEPTED_USPS_ORIGIN_COUNTRIES.contains(it.code) }
            Result.success(acceptedCountries)
        } else {
            result
        }
    }

    companion object {
        private val ACCEPTED_USPS_ORIGIN_COUNTRIES = arrayOf(
            "US", // United States
            "PR", // Puerto Rico
            "VI", // Virgin Islands
            "GU", // Guam
            "AS", // American Samoa
            "UM", // United States Minor Outlying Islands
            "MH", // Marshall Islands
            "FM", // Micronesia
            "MP" // Northern Mariana Islands
        )
    }
}
