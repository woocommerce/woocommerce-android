package com.woocommerce.android.ui.prefs.privacy.banner.domain

import com.woocommerce.android.ui.prefs.privacy.GeoRepository
import javax.inject.Inject

class IsUsersCountryGdprCompliant @Inject constructor(private val geoRepository: GeoRepository) {

    suspend operator fun invoke(): Boolean {
        return geoRepository.fetchCountryCode().fold(
            onSuccess = { countryCode ->
                countryCode.uppercase() in PRIVACY_BANNER_ELIGIBLE_COUNTRY_CODES
            },
            onFailure = {
                false
            }
        )
    }

    companion object {
        private val PRIVACY_BANNER_ELIGIBLE_COUNTRY_CODES = listOf(
            // European Member countries
            "AT", // Austria
            "BE", // Belgium
            "BG", // Bulgaria
            "CY", // Cyprus
            "CZ", // Czech Republic
            "DE", // Germany
            "DK", // Denmark
            "EE", // Estonia
            "ES", // Spain
            "FI", // Finland
            "FR", // France
            "GR", // Greece
            "HR", // Croatia
            "HU", // Hungary
            "IE", // Ireland
            "IT", // Italy
            "LT", // Lithuania
            "LU", // Luxembourg
            "LV", // Latvia
            "MT", // Malta
            "NL", // Netherlands
            "PL", // Poland
            "PT", // Portugal
            "RO", // Romania
            "SE", // Sweden
            "SI", // Slovenia
            "SK", // Slovakia
            // Single Market Countries that GDPR applies to
            "CH", // Switzerland
            "GB", // United Kingdom
            "IS", // Iceland
            "LI", // Liechtenstein
            "NO", // Norway
        )
    }
}
