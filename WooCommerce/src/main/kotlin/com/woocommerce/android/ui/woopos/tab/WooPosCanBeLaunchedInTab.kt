package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPOSIsRemotelyEnabled
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines if POS can be launched *from within the POS tab* based on launch conditions,
 * e.g., currency support, WooCommerce version, feature flags, etc.
 * This is only checked once the POS tab is already visible.
 */
@Singleton
class WooPosCanBeLaunchedInTab @Inject constructor(
    private val selectedSite: SelectedSite,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
    private val wooCommerceStore: WooCommerceStore,
    private val isRemotelyEnabled: WooPOSIsRemotelyEnabled
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(): WooPosLaunchability = withContext(Dispatchers.IO) {
        val selectedSite = selectedSite.getOrNull()
            ?: return@withContext WooPosLaunchability.NotLaunchable(
                WooPosLaunchability.NonLaunchabilityReason.NoSiteSelected
            )

        if (!isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps()) {
            return@withContext WooPosLaunchability.NotLaunchable(
                WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion
            )
        }

        if (isFeatureSwitchSupported() && isRemotelyEnabled() != true) {
            return@withContext WooPosLaunchability.NotLaunchable(
                WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
            )
        }

        val siteSettings = wooCommerceStore.getSiteSettings(selectedSite)
            ?: wooCommerceStore.fetchSiteGeneralSettings(selectedSite).model

        if (siteSettings == null) {
            return@withContext WooPosLaunchability.NotLaunchable(
                WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable
            )
        }

        return@withContext if (isCurrencySupported(siteSettings.currencyCode)) {
            WooPosLaunchability.Launchable
        } else {
            WooPosLaunchability.NotLaunchable(
                WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency
            )
        }
    }

    private fun isCurrencySupported(currency: String) = SUPPORTED_CURRENCIES.contains(currency.lowercase())

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

        val SUPPORTED_CURRENCIES = listOf("usd", "gbp")
    }
}

sealed class WooPosLaunchability {
    object Launchable : WooPosLaunchability()
    data class NotLaunchable(val reason: NonLaunchabilityReason) : WooPosLaunchability()

    enum class NonLaunchabilityReason {
        UnsupportedWooCommerceVersion,
        SiteSettingsUnavailable,
        FeatureSwitchDisabled,
        UnsupportedCurrency,
        NoSiteSelected,
    }
}
