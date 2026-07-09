package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines if POS can be launched *from within the POS tab* based on launch conditions
 * (WooCommerce version, plan eligibility). POS is available worldwide — the country gate
 * and the WC POS feature switch have been removed in favour of per-country card-payment
 * gating inside the POS UI.
 */
@Singleton
class WooPosCanBeLaunchedInTab @Inject constructor(
    private val appPrefs: AppPrefs,
    private val selectedSite: SelectedSite,
    private val getWooCoreCachedVersion: GetWooCorePluginCachedVersion,
    private val fetchWooCoreVersion: FetchActiveWCPluginVersion,
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

        getNonLaunchabilityReasonFromVersion(forceRefresh, cachedPositive)?.let {
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

    private suspend fun getNonLaunchabilityReasonFromVersion(
        forceRefresh: Boolean,
        cachedPositive: Boolean
    ): WooPosLaunchability.NonLaunchabilityReason? {
        val wooCoreVersion = getWooCoreVersion(forceRefresh)
            ?: return if (cachedPositive) null else WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache

        return if (!isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(wooCoreVersion)) {
            WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion
        } else {
            null
        }
    }

    private suspend fun getWooCoreVersion(forceRefresh: Boolean): String? =
        if (forceRefresh) fetchWooCoreVersion() else getWooCoreCachedVersion()

    private fun isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(wooCoreVersion: String): Boolean {
        return wooCoreVersion.semverCompareTo(MINIMUM_SUPPORTED_WC_VERSION) >= 0
    }

    companion object {
        const val MINIMUM_SUPPORTED_WC_VERSION = "9.6.0"
    }
}

sealed class WooPosLaunchability {
    object Launchable : WooPosLaunchability()
    data class NotLaunchable(val reason: NonLaunchabilityReason) : WooPosLaunchability()

    enum class NonLaunchabilityReason {
        UnsupportedWooCommerceVersion,
        SiteSettingsUnavailable,
        NoSiteSelected,
        UnknownNoPositiveCache,
    }
}
