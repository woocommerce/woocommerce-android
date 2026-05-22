package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

/**
 * Returns true when the selected site's country is allowed to launch POS.
 *
 * - When [FeatureFlag.WOO_POS_ALL_COUNTRIES] is enabled, every country is allowed.
 * - Otherwise, POS is restricted to the IPP-supported card-payment countries listed below.
 *
 * Inside POS, per-country card-payment gating still applies (CA, JP, etc. fall back to
 * a Cash-only checkout); this gate only decides whether POS is reachable at all.
 */
class WooPosIsCountryAllowed @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val featureFlagRepository: FeatureFlagRepository,
) {
    operator fun invoke(): Boolean {
        if (featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)) return true

        val site = selectedSite.getOrNull() ?: return false
        val countryCode = wooCommerceStore.getStoreCountryCode(site)?.uppercase() ?: return false
        return countryCode in SUPPORTED_COUNTRIES
    }

    private companion object {
        val SUPPORTED_COUNTRIES = setOf(
            "US", "PR", "GB", "CA",
            "FR", "DE", "IE", "NL", "AT", "BE", "FI", "IT", "LU", "PT", "ES",
            "SG", "NZ",
            "AU",
        )
    }
}
