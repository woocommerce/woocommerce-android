package com.woocommerce.android.ui.prefs.privacy.banner.domain

import com.woocommerce.android.util.TelephonyManagerProvider
import com.woocommerce.android.util.locale.LocaleProvider
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class IsUsersCountryGdprCompliant @Inject constructor(
    private val accountStore: AccountStore,
    private val telephonyManagerProvider: TelephonyManagerProvider,
    private val localeProvider: LocaleProvider,
) {

    operator fun invoke(): Boolean {
        val countryCode = if (accountStore.hasAccessToken()) {
            accountStore.account.userIpCountryCode
        } else {
            val networkCarrierCountryCode = telephonyManagerProvider.getCountryCode()

            networkCarrierCountryCode.ifEmpty {
                localeProvider.provideLocale()?.country.orEmpty()
            }
        }

        return countryCode in PRIVACY_BANNER_ELIGIBLE_COUNTRY_CODES
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
            "GB", // United Kingdom
            // Single Market Countries that GDPR applies to
            "CH", // Switzerland
            "IS", // Iceland
            "LI", // Liechtenstein
            "NO", // Norway
        )
    }
}
