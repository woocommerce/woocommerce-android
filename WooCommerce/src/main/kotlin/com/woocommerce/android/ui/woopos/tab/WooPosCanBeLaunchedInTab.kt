package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
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
 * The checks and their order match the iOS `POSTabEligibilityChecker`: store country and currency,
 * WooCommerce plugin presence, WooCommerce version, and finally the store's POS feature switch.
 */
@Singleton
class WooPosCanBeLaunchedInTab @Inject constructor(
    private val appPrefs: AppPrefs,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val getWooCorePluginStatus: WooPosGetWooCorePluginStatus,
    private val isFeatureSwitchEnabled: WooPosIsFeatureSwitchEnabled,
    private val featureFlagRepository: FeatureFlagRepository,
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

        getNonLaunchabilityReasonFromSiteSettings(site, forceRefresh, cachedPositive)?.let {
            return prepareNotLaunchableStateWithCacheUpdate(site.id, it.reason, it.isDefinite)
        }

        getNonLaunchabilityReasonFromPlugin(forceRefresh, cachedPositive)?.let {
            return prepareNotLaunchableStateWithCacheUpdate(site.id, it)
        }

        appPrefs.setPOSLaunchableForSite(site.id)
        return WooPosLaunchability.Launchable
    }

    private fun prepareNotLaunchableStateWithCacheUpdate(
        siteId: Int,
        reason: WooPosLaunchability.NonLaunchabilityReason,
        isDefinite: Boolean = reason.isDefiniteIneligibility
    ): WooPosLaunchability.NotLaunchable {
        if (isDefinite) {
            appPrefs.clearPOSLaunchableForSite(siteId)
        }

        return WooPosLaunchability.NotLaunchable(reason)
    }

    @Suppress("ReturnCount")
    private suspend fun getNonLaunchabilityReasonFromSiteSettings(
        site: SiteModel,
        forceRefresh: Boolean,
        cachedPositive: Boolean
    ): Ineligibility? {
        val resolved = resolveSiteSettings(site, forceRefresh)
            ?: return reasonIfNoPositiveCache(cachedPositive)?.let { Ineligibility(it) }

        val siteSettings = resolved.settings

        // The settings response can omit the country, which the mapper stores as an empty string.
        // That is an unknown, not an unsupported country, so it is treated like the other unknowns.
        if (siteSettings.countryCode.isBlank()) {
            return reasonIfNoPositiveCache(cachedPositive)?.let { Ineligibility(it) }
        }

        val supportedCurrencies = WooPosSupportedCountries.currenciesFor(siteSettings.countryCode)

        if (supportedCurrencies.isEmpty()) {
            // The country gate that makes the tab visible caches its verdict, so a store that moves
            // to an unsupported country keeps reaching this check. iOS blocks that case and asks the
            // merchant to relaunch, and reports it as SiteSettingsUnavailable; this does the same.
            // With the all-countries flag on there is no country to enforce, and no currency to
            // validate against either, so the store is let through.
            return if (featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)) {
                null
            } else {
                Ineligibility(WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable)
            }
        }

        // The settings response can omit the currency, which the mapper stores as an empty string.
        // That is an unknown, not a mismatch, so it is treated like the other unknowns here.
        if (siteSettings.currencyCode.isBlank()) {
            return reasonIfNoPositiveCache(cachedPositive)?.let { Ineligibility(it) }
        }

        if (siteSettings.currencyCode.uppercase() in supportedCurrencies) return null

        // A mismatch read from local settings can be stale: the merchant may have already fixed the
        // currency remotely. It still blocks this launch, but only a mismatch confirmed by a fresh
        // fetch is definite enough to drop the positive cache. iOS decides this on settings it has
        // just refreshed, in `waitForSiteSettingsRefresh`.
        return Ineligibility(
            reason = WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency,
            isDefinite = resolved.isFresh
        )
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
                    ?: getNonLaunchabilityReasonFromFeatureSwitch(
                        wooCoreVersion = status.version,
                        report = status.report,
                        forceRefresh = forceRefresh,
                        cachedPositive = cachedPositive,
                    )
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
        report: WooPosSystemStatusReport?,
        forceRefresh: Boolean,
        cachedPositive: Boolean
    ): WooPosLaunchability.NonLaunchabilityReason? {
        // Below the version that introduced the switch the feature is always on.
        if (!isFeatureSwitchSupported(wooCoreVersion)) return null

        // The plugin check hands over the report it read the plugin from, so the switch is taken
        // from that instead of asking the same endpoint for it a second time.
        return when (isFeatureSwitchEnabled(forceRefresh, report).getOrNull()) {
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

    private suspend fun resolveSiteSettings(site: SiteModel, forceRefresh: Boolean): ResolvedSettings? {
        if (!forceRefresh) {
            wooCommerceStore.getSiteSettings(site)?.let {
                return ResolvedSettings(settings = it, isFresh = false)
            }
        }

        val fetched = wooCommerceStore.fetchSiteGeneralSettings(site).model ?: return null
        return ResolvedSettings(settings = fetched, isFresh = true)
    }

    private fun isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(wooCoreVersion: String): Boolean {
        return wooCoreVersion.semverCompareTo(MINIMUM_SUPPORTED_WC_VERSION) >= 0
    }

    private fun isFeatureSwitchSupported(wooCoreVersion: String): Boolean {
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_POS_FEATURE_SWITCH) >= 0
    }

    /**
     * @param isFresh whether the settings came from a request just made, rather than from what was
     * already stored for the site.
     */
    private data class ResolvedSettings(
        val settings: Settings,
        val isFresh: Boolean,
    )

    /**
     * @param isDefinite whether the reason reflects the store's actual state. Defaults to what the
     * reason itself implies, and is overridden where the same reason can be reached from data that
     * may be stale.
     */
    private data class Ineligibility(
        val reason: WooPosLaunchability.NonLaunchabilityReason,
        val isDefinite: Boolean = reason.isDefiniteIneligibility,
    )

    companion object {
        const val MINIMUM_SUPPORTED_WC_VERSION = "9.6.0"

        private const val WC_VERSION_SUPPORTS_POS_FEATURE_SWITCH = "10.0.0"
    }
}

/**
 * Whether the reason reflects the store's actual state rather than a failure to determine it.
 * Only a definite reason drops the positive cache, which is what iOS's `isDefiniteIneligibility`
 * decides.
 */
private val WooPosLaunchability.NonLaunchabilityReason.isDefiniteIneligibility: Boolean
    get() = when (this) {
        WooPosLaunchability.NonLaunchabilityReason.WooCommercePluginNotFound,
        WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion,
        WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency,
        WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled -> true

        WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable,
        WooPosLaunchability.NonLaunchabilityReason.NoSiteSelected,
        WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache -> false
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
