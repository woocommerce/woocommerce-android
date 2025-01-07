package com.woocommerce.android.ui.woopos

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_POS
import kotlinx.coroutines.coroutineScope
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
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(): Boolean = coroutineScope {
        val selectedSite = selectedSite.getOrNull() ?: return@coroutineScope false

        if (!isRemoteFeatureFlagEnabled(WOO_POS)) return@coroutineScope false
        if (!isScreenSizeAllowed()) return@coroutineScope false
        if (!isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps()) return@coroutineScope false

        val siteSettings = wooCommerceStore.getSiteSettings(selectedSite) ?: return@coroutineScope false
        if (siteSettings.countryCode.lowercase() !in SUPPORTED_COUNTRIES) return@coroutineScope false
        if (siteSettings.currencyCode.lowercase() !in SUPPORTED_CURRENCIES) return@coroutineScope false

        return@coroutineScope true
    }

    private fun isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(): Boolean {
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_POS_PRODUCT_FILTERING) >= 0
    }

    private companion object {
        const val WC_VERSION_SUPPORTS_POS_PRODUCT_FILTERING = "9.6.0"

        val SUPPORTED_COUNTRIES = listOf("us")
        val SUPPORTED_CURRENCIES = listOf("usd")
    }
}
