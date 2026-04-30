package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosSupportedCountries @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
) {
    suspend fun supportedCountryCurrencyPairs(): List<Pair<String, String>> {
        featureFlagRepository.awaitRemoteFlagsLoaded()
        return buildList {
            addAll(BASE_PAIRS)
            if (featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION)) {
                addAll(PRIMARY_EXPANSION_PAIRS)
            }
            if (featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED)) {
                addAll(EU_EXTENDED_PAIRS)
            }
        }
    }

    suspend fun supportedCountries(): List<String> = supportedCountryCurrencyPairs().map { it.first }

    private companion object {
        val BASE_PAIRS = listOf("us" to "usd", "gb" to "gbp")
        val PRIMARY_EXPANSION_PAIRS = listOf(
            "fr" to "eur",
            "de" to "eur",
            "ie" to "eur",
            "nl" to "eur",
            "sg" to "sgd",
            "nz" to "nzd",
        )
        val EU_EXTENDED_PAIRS = listOf(
            "at" to "eur",
            "be" to "eur",
            "fi" to "eur",
            "it" to "eur",
            "lu" to "eur",
            "pt" to "eur",
            "es" to "eur",
        )
    }
}
