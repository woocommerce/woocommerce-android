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
import javax.inject.Inject

class WooPosTabShouldBeVisible @Inject constructor(
    private val appPrefs: AppPrefs,
    private val selectedSite: SelectedSite,
    private val isScreenSizeAllowed: WooPosIsScreenSizeAllowed,
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

        appPrefs.setPOSTabVisibilityForSite(site.id)
        return@withContext Result.success(true)
    }
}
