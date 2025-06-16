package com.woocommerce.android.ui.woopos

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_POS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosIsEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isScreenSizeAllowed: WooPosIsScreenSizeAllowed,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
    private val wooCommerceStore: WooCommerceStore,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled,
    private val isRemotelyEnabled: WooPOSIsRemotelyEnabled
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        val selectedSite = selectedSite.getOrNull() ?: return@withContext false

        if (!isRemoteFeatureFlagEnabled(WOO_POS)) return@withContext false
        if (!isScreenSizeAllowed()) return@withContext false
        if (!isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps()) return@withContext false
        if (isFeatureSwitchSupported() && isRemotelyEnabled() != true) return@withContext false

        val siteSettings = wooCommerceStore.getSiteSettings(selectedSite) ?: return@withContext false

        return@withContext isCountryAndCurrencySupported(
            countryCode = siteSettings.countryCode,
            currency = siteSettings.currencyCode
        )
    }

    private fun isCountryAndCurrencySupported(countryCode: String, currency: String) =
        SUPPORTED_COUNTRY_CURRENCY_PAIRS.any { it.first.equals(countryCode, true) && it.second.equals(currency, true) }

    private fun isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(): Boolean {
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_POS_PRODUCT_FILTERING) >= 0
    }

    private fun isFeatureSwitchSupported(): Boolean {
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_POS_FEATURE_SWITCH) >= 0
    }

    private companion object {
        const val WC_VERSION_SUPPORTS_POS_PRODUCT_FILTERING = "9.6.0"
        const val WC_VERSION_SUPPORTS_POS_FEATURE_SWITCH = "10.0.0"

        val SUPPORTED_COUNTRY_CURRENCY_PAIRS = listOf("us" to "usd", "gb" to "gbp")
    }
}
