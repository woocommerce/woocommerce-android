package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines if POS can be launched *from within the POS tab*.
 *
 * The checks and their order match the iOS `POSTabEligibilityChecker`: store currency, WooCommerce
 * plugin presence, WooCommerce version, and finally the store's POS feature switch.
 */
@Singleton
class WooPosCanBeLaunchedInTab @Inject constructor(
    private val appPrefs: AppPrefs,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val getWooCorePluginStatus: WooPosGetWooCorePluginStatus,
    private val isFeatureSwitchEnabled: WooPosIsFeatureSwitchEnabled,
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
            ?: return WooPosLaunchability.NotLaunchable(
                reason = WooPosLaunchability.NonLaunchabilityReason.NoSiteSelected
            )

        val cachedPositive = appPrefs.isPOSLaunchableForSite(site.id)

        getNonLaunchabilityReasonFromCurrency(site, forceRefresh, cachedPositive)?.let {
            return prepareNotLaunchableStateWithCacheUpdate(site.id, it)
        }

        getNonLaunchabilityReasonFromPlugin(forceRefresh, cachedPositive)?.let {
            return prepareNotLaunchableStateWithCacheUpdate(site.id, it)
        }

        appPrefs.setPOSLaunchableForSite(site.id)
        return WooPosLaunchability.Launchable
    }

    private fun prepareNotLaunchableStateWithCacheUpdate(
        siteId: Int,
        reason: WooPosLaunchability.NonLaunchabilityReason
    ): WooPosLaunchability.NotLaunchable {
        if (reason != WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache) {
            appPrefs.clearPOSLaunchableForSite(siteId)
        }

        return WooPosLaunchability.NotLaunchable(reason)
    }

    private suspend fun getNonLaunchabilityReasonFromCurrency(
        site: SiteModel,
        forceRefresh: Boolean,
        cachedPositive: Boolean
    ): WooPosLaunchability.NonLaunchabilityReason? {
        val siteSettings = resolveSiteSettings(site, forceRefresh)
            ?: return reasonIfNoPositiveCache(cachedPositive)

        val supportedCurrencies = WooPosSupportedCountries.currenciesFor(siteSettings.countryCode)

        // Countries outside the POS table are only reachable with the all-countries flag on. There
        // is no currency to validate against, so the store is let through.
        if (supportedCurrencies.isEmpty()) return null

        return if (siteSettings.currencyCode.uppercase() in supportedCurrencies) {
            null
        } else {
            WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency
        }
    }

    private suspend fun getNonLaunchabilityReasonFromPlugin(
        forceRefresh: Boolean,
        cachedPositive: Boolean
    ): WooPosLaunchability.NonLaunchabilityReason? =
        when (val status = getWooCorePluginStatus(forceRefresh)) {
            is WooPosWooCorePluginStatus.NotInstalledOrInactive ->
                WooPosLaunchability.NonLaunchabilityReason.WooCommercePluginNotFound

            is WooPosWooCorePluginStatus.CouldNotDetermine ->
                reasonIfNoPositiveCache(cachedPositive)

            is WooPosWooCorePluginStatus.Active ->
                getNonLaunchabilityReasonFromWooCoreVersion(status.version)
                    ?: getNonLaunchabilityReasonFromFeatureSwitch(status.version, forceRefresh, cachedPositive)
        }

    private fun getNonLaunchabilityReasonFromWooCoreVersion(
        wooCoreVersion: String
    ): WooPosLaunchability.NonLaunchabilityReason? =
        if (!isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(wooCoreVersion)) {
            WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion
        } else {
            null
        }

    private suspend fun getNonLaunchabilityReasonFromFeatureSwitch(
        wooCoreVersion: String,
        forceRefresh: Boolean,
        cachedPositive: Boolean
    ): WooPosLaunchability.NonLaunchabilityReason? {
        // Below the version that introduced the switch the feature is always on.
        if (!isFeatureSwitchSupported(wooCoreVersion)) return null

        return when (isFeatureSwitchEnabled(forceRefresh).getOrNull()) {
            true -> null
            false -> WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled
            null -> reasonIfNoPositiveCache(cachedPositive)
        }
    }

    private fun reasonIfNoPositiveCache(
        hasCachedPositive: Boolean
    ): WooPosLaunchability.NonLaunchabilityReason? =
        if (hasCachedPositive) {
            null
        } else {
            WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache
        }

    private suspend fun resolveSiteSettings(site: SiteModel, forceRefresh: Boolean): Settings? =
        if (forceRefresh) {
            wooCommerceStore.fetchSiteGeneralSettings(site).model
        } else {
            wooCommerceStore.getSiteSettings(site) ?: wooCommerceStore.fetchSiteGeneralSettings(site).model
        }

    private fun isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(wooCoreVersion: String): Boolean {
        return wooCoreVersion.semverCompareTo(MINIMUM_SUPPORTED_WC_VERSION) >= 0
    }

    private fun isFeatureSwitchSupported(wooCoreVersion: String): Boolean {
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_POS_FEATURE_SWITCH) >= 0
    }

    companion object {
        const val MINIMUM_SUPPORTED_WC_VERSION = "9.6.0"

        private const val WC_VERSION_SUPPORTS_POS_FEATURE_SWITCH = "10.0.0"
    }
}

sealed class WooPosLaunchability {
    object Launchable : WooPosLaunchability()
    data class NotLaunchable(val reason: NonLaunchabilityReason) : WooPosLaunchability()

    enum class NonLaunchabilityReason {
        WooCommercePluginNotFound,
        UnsupportedWooCommerceVersion,
        UnsupportedCurrency,
        FeatureSwitchDisabled,
        SiteSettingsUnavailable,
        NoSiteSelected,
        UnknownNoPositiveCache,
    }
}
