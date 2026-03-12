package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosTabShouldBeVisible @Inject constructor(
    private val appPrefs: AppPrefs,
    private val selectedSite: SelectedSite,
    private val isScreenSizeAllowed: WooPosIsScreenSizeAllowed,
    private val wooCommerceStore: WooCommerceStore,
    private val featureFlagRepository: FeatureFlagRepository,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper,
    private val wooPosLog: WooPosLogWrapper,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        val site = selectedSite.getOrNull()
            ?: return@withContext Result.failure(WooPosCouldNotDetermineValueException())

        if (!forceRefresh && appPrefs.isPOSTabVisibleForSite(site.id)) {
            return@withContext Result.success(true)
        }

        if (ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.POS)) {
            appPrefs.clearPOSTabVisibilityForSite(site.id)
            return@withContext Result.success(false).also {
                wooPosLog.i("POS Tab Not visible reason: Site is CIAB")
            }
        }

        featureFlagRepository.awaitRemoteFlagsLoaded()

        if (!featureFlagRepository.isEnabled(FeatureFlag.WOO_POS)) {
            appPrefs.clearPOSTabVisibilityForSite(site.id)
            return@withContext Result.success(false).also {
                wooPosLog.i("POS Tab Not visible reason: Remote feature flag is disabled")
            }
        }

        if (!isScreenSizeAllowed()) {
            appPrefs.clearPOSTabVisibilityForSite(site.id)
            return@withContext Result.success(false).also {
                wooPosLog.i("POS Tab Not visible reason: Screen size is not allowed")
            }
        }

        val siteSettings = if (forceRefresh) {
            wooCommerceStore.fetchSiteGeneralSettings(site).model
        } else {
            wooCommerceStore.getSiteSettings(site)
                ?: wooCommerceStore.fetchSiteGeneralSettings(site).model
        }
            ?: return@withContext Result.failure(WooPosCouldNotDetermineValueException())

        val isSupported = isCountrySupported(countryCode = siteSettings.countryCode)

        if (isSupported) {
            appPrefs.setPOSTabVisibilityForSite(site.id)
        } else {
            appPrefs.clearPOSTabVisibilityForSite(site.id)
            wooPosLog.i("POS Tab Not visible reason: Country ${siteSettings.countryCode} is not supported")
        }

        return@withContext Result.success(isSupported)
    }

    private fun isCountrySupported(countryCode: String) = SUPPORTED_COUNTRIES.contains(countryCode.lowercase())

    private companion object {
        private val SUPPORTED_COUNTRIES = listOf("us", "gb")
    }
}
