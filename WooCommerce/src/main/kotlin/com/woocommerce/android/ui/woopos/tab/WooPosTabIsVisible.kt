package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_POS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosTabIsVisible @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled
) {
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        val selectedSite = selectedSite.getOrNull() ?: return@withContext false

        if (!isRemoteFeatureFlagEnabled(WOO_POS)) return@withContext false

        val siteSettings = wooCommerceStore.getSiteSettings(selectedSite)
            ?: wooCommerceStore.fetchSiteGeneralSettings(selectedSite).model

        if (siteSettings == null) return@withContext false

        return@withContext isCountrySupported(
            countryCode = siteSettings.countryCode
        )
    }

    private fun isCountrySupported(countryCode: String) = SUPPORTED_COUNTRIES.contains(countryCode.lowercase())

    private companion object {
        val SUPPORTED_COUNTRIES = listOf("us", "gb")
    }
}
