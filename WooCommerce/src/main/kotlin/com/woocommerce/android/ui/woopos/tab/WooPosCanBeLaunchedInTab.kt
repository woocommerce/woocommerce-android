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

    suspend operator fun invoke(
        refreshPolicy: WooPosLaunchabilityRefreshPolicy
    ): WooPosLaunchability = withContext(Dispatchers.IO) {
        return@withContext checkLaunchability(refreshPolicy).also {
            if (it is WooPosLaunchability.NotLaunchable) {
                wooPosLog.i("POS cannot be launched: $it")
            }
        }
    }

    private suspend fun checkLaunchability(
        refreshPolicy: WooPosLaunchabilityRefreshPolicy
    ): WooPosLaunchability {
        val site = selectedSite.getOrNull()
            ?: return WooPosLaunchability.NotLaunchable(
                reason = WooPosLaunchability.NonLaunchabilityReason.NoSiteSelected
            )

        val cachedPositive = appPrefs.isPOSLaunchableForSite(site.id)

        checkSiteSettings(site, refreshPolicy, cachedPositive)?.let { return it }
        checkPlugin(site, refreshPolicy, cachedPositive)?.let { return it }

        appPrefs.setPOSLaunchableForSite(site.id)
        return WooPosLaunchability.Launchable
    }

    @Suppress("ReturnCount")
    private suspend fun checkSiteSettings(
        site: SiteModel,
        refreshPolicy: WooPosLaunchabilityRefreshPolicy,
        cachedPositive: Boolean
    ): WooPosLaunchability.NotLaunchable? {
        val resolved = resolveSiteSettings(site, refreshPolicy)
            ?: return couldNotDetermine(site.id, cachedPositive)

        val siteSettings = resolved.settings

        if (siteSettings.countryCode.isBlank()) return couldNotDetermine(site.id, cachedPositive)

        val supportedCurrencies = WooPosSupportedCountries.currenciesFor(siteSettings.countryCode)

        if (supportedCurrencies.isEmpty()) {
            return if (featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)) {
                null
            } else {
                notLaunchable(site.id, WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable)
            }
        }

        if (siteSettings.currencyCode.isBlank()) return couldNotDetermine(site.id, cachedPositive)

        if (siteSettings.currencyCode.uppercase() in supportedCurrencies) return null

        // Stored settings can be stale, so only a mismatch confirmed by a fetch drops the cached positive.
        return notLaunchable(
            siteId = site.id,
            reason = WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency,
            isDefinite = resolved.isFresh
        )
    }

    private suspend fun checkPlugin(
        site: SiteModel,
        refreshPolicy: WooPosLaunchabilityRefreshPolicy,
        cachedPositive: Boolean
    ): WooPosLaunchability.NotLaunchable? =
        when (val status = getWooCorePluginStatus(refreshPolicy)) {
            is WooPosWooCorePluginStatus.NotInstalledOrInactive ->
                notLaunchable(site.id, WooPosLaunchability.NonLaunchabilityReason.WooCommercePluginNotFound)

            is WooPosWooCorePluginStatus.CouldNotDetermine ->
                couldNotDetermine(site.id, cachedPositive)

            is WooPosWooCorePluginStatus.Active ->
                checkWooCoreVersion(site.id, status.version)
                    ?: checkFeatureSwitch(
                        site = site,
                        wooCoreVersion = status.version,
                        report = status.report,
                        refreshPolicy = refreshPolicy,
                        cachedPositive = cachedPositive,
                    )
        }

    private fun checkWooCoreVersion(
        siteId: Int,
        wooCoreVersion: String
    ): WooPosLaunchability.NotLaunchable? =
        if (!isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(wooCoreVersion)) {
            notLaunchable(siteId, WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion)
        } else {
            null
        }

    private suspend fun checkFeatureSwitch(
        site: SiteModel,
        wooCoreVersion: String,
        report: WooPosSystemStatusReport?,
        refreshPolicy: WooPosLaunchabilityRefreshPolicy,
        cachedPositive: Boolean
    ): WooPosLaunchability.NotLaunchable? {
        if (!isFeatureSwitchSupported(wooCoreVersion)) return null

        return when (isFeatureSwitchEnabled(refreshPolicy, report).getOrNull()) {
            true -> null
            false -> notLaunchable(site.id, WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled)
            null -> couldNotDetermine(site.id, cachedPositive)
        }
    }

    /**
     * The check could not tell the store's state. A store that has launched POS before keeps launching it.
     */
    private fun couldNotDetermine(
        siteId: Int,
        cachedPositive: Boolean
    ): WooPosLaunchability.NotLaunchable? =
        if (cachedPositive) {
            null
        } else {
            notLaunchable(siteId, WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache)
        }

    private fun notLaunchable(
        siteId: Int,
        reason: WooPosLaunchability.NonLaunchabilityReason,
        isDefinite: Boolean = reason.isDefiniteIneligibility
    ): WooPosLaunchability.NotLaunchable {
        if (isDefinite) {
            appPrefs.clearPOSLaunchableForSite(siteId)
        }

        return WooPosLaunchability.NotLaunchable(reason)
    }

    private suspend fun resolveSiteSettings(
        site: SiteModel,
        refreshPolicy: WooPosLaunchabilityRefreshPolicy
    ): ResolvedSettings? {
        if (refreshPolicy != WooPosLaunchabilityRefreshPolicy.ForceRefresh) {
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

    private data class ResolvedSettings(
        val settings: Settings,
        val isFresh: Boolean,
    )

    companion object {
        const val MINIMUM_SUPPORTED_WC_VERSION = "9.6.0"

        private const val WC_VERSION_SUPPORTS_POS_FEATURE_SWITCH = "10.0.0"
    }
}

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
