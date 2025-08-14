package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPOSIsRemotelyEnabled
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.Settings
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
    private val appPrefs: AppPrefs,
    private val selectedSite: SelectedSite,
    private val getWooCoreCachedVersion: GetWooCorePluginCachedVersion,
    private val fetchWooCoreVersion: FetchActiveWCPluginVersion,
    private val wooCommerceStore: WooCommerceStore,
    private val isRemotelyEnabled: WooPOSIsRemotelyEnabled,
    private val wooPosLog: WooPosLogWrapper,
) {

    suspend operator fun invoke(forceRefresh: Boolean = false): WooPosLaunchability = withContext(Dispatchers.IO) {
        return@withContext checkLaunchability(forceRefresh).also {
            if (it is WooPosLaunchability.NotLaunchable) {
                wooPosLog.i("POS cannot be launched: $it")
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun checkLaunchability(forceRefresh: Boolean = false): WooPosLaunchability {
        val site = selectedSite.getOrNull()
            ?: return WooPosLaunchability.NotLaunchable(WooPosLaunchability.NonLaunchabilityReason.NoSiteSelected)

        val cachedPositive = appPrefs.isPOSLaunchableForSite(site.id)

        val wooCoreVersion = resolveWooCoreVersion(forceRefresh)
            ?: return fallbackForMissingWooCore(cachedPositive)

        validateWooCoreVersion(wooCoreVersion)?.let { return it }
        evaluateFeatureSwitch(wooCoreVersion, forceRefresh, cachedPositive)?.let { return it }

        val siteSettings = resolveSiteSettings(site, forceRefresh)
            ?: return fallbackUnknownOrCache(cachedPositive)

        if (isCountryAndCurrencySupported(siteSettings.countryCode, siteSettings.currencyCode)) {
            appPrefs.setPOSLaunchableForSite(site.id)
            return WooPosLaunchability.Launchable
        } else {
            return WooPosLaunchability.NotLaunchable(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency)
        }
    }

    private suspend fun resolveWooCoreVersion(forceRefresh: Boolean): String? =
        if (forceRefresh) fetchWooCoreVersion() else getWooCoreCachedVersion()

    private fun fallbackForMissingWooCore(cachedPositive: Boolean): WooPosLaunchability =
        if (cachedPositive) {
            WooPosLaunchability.Launchable
        } else {
            WooPosLaunchability.NotLaunchable(WooPosLaunchability.NonLaunchabilityReason.WooCommercePluginNotFound)
        }

    private fun validateWooCoreVersion(wooCoreVersion: String): WooPosLaunchability? =
        if (!isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(wooCoreVersion)) {
            WooPosLaunchability.NotLaunchable(WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion)
        } else {
            null
        }

    private suspend fun evaluateFeatureSwitch(
        wooCoreVersion: String,
        forceRefresh: Boolean,
        cachedPositive: Boolean
    ): WooPosLaunchability? {
        if (!isFeatureSwitchSupported(wooCoreVersion)) return null
        val remote = isRemotelyEnabled(forceRefresh)

        return remote.fold(
            onSuccess = { enabled ->
                if (enabled) {
                    null
                } else {
                    WooPosLaunchability.NotLaunchable(
                        WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
                    )
                }
            },
            onFailure = {
                if (cachedPositive) {
                    WooPosLaunchability.Launchable
                } else {
                    WooPosLaunchability.NotLaunchable(
                        WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache
                    )
                }
            }
        )
    }

    private suspend fun resolveSiteSettings(site: SiteModel, forceRefresh: Boolean): Settings? =
        if (forceRefresh) {
            wooCommerceStore.fetchSiteGeneralSettings(site).model
        } else {
            wooCommerceStore.getSiteSettings(site) ?: wooCommerceStore.fetchSiteGeneralSettings(site).model
        }

    private fun fallbackUnknownOrCache(cachedPositive: Boolean): WooPosLaunchability =
        if (cachedPositive) {
            WooPosLaunchability.Launchable
        } else {
            WooPosLaunchability.NotLaunchable(WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache)
        }

    private fun isCountryAndCurrencySupported(countryCode: String, currency: String) =
        SUPPORTED_COUNTRY_CURRENCY_PAIRS.any {
            it.first.equals(countryCode, true) && it.second.equals(currency, true)
        }

    private fun isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(wooCoreVersion: String): Boolean {
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_POS_PRODUCT_FILTERING) >= 0
    }

    private fun isFeatureSwitchSupported(wooCoreVersion: String): Boolean {
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_POS_FEATURE_SWITCH) >= 0
    }

    companion object {
        const val MINIMUM_SUPPORTED_WC_VERSION = "9.6.0"
        val SUPPORTED_COUNTRY_CURRENCY_PAIRS = listOf("us" to "usd", "gb" to "gbp")

        private const val WC_VERSION_SUPPORTS_POS_PRODUCT_FILTERING = MINIMUM_SUPPORTED_WC_VERSION
        private const val WC_VERSION_SUPPORTS_POS_FEATURE_SWITCH = "10.0.0"
    }
}

sealed class WooPosLaunchability {
    object Launchable : WooPosLaunchability()
    data class NotLaunchable(val reason: NonLaunchabilityReason) : WooPosLaunchability()

    enum class NonLaunchabilityReason {
        WooCommercePluginNotFound,
        UnsupportedWooCommerceVersion,
        SiteSettingsUnavailable,
        FeatureSwitchDisabled,
        UnsupportedCurrency,
        NoSiteSelected,
        UnknownNoPositiveCache
    }
}
