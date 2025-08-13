package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_POS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosTabShouldBeVisible @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isScreenSizeAllowed: WooPosIsScreenSizeAllowed,
    private val wooCommerceStore: WooCommerceStore,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled,
    private val wooPosLog: WooPosLogWrapper,
) {
    suspend operator fun invoke(): Result<Boolean> = withContext(Dispatchers.IO) {
        val selectedSite = selectedSite.getOrNull()
            ?: return@withContext Result.failure(WooPosCouldNotDetermineValueException())

        if (!isRemoteFeatureFlagEnabled(WOO_POS)) {
            return@withContext Result.success(true).also {
                wooPosLog.i("POS Tab Not visible reason: Remote feature flag is disabled")
            }
        }

        if (!isScreenSizeAllowed()) {
            return@withContext Result.success(false).also {
                wooPosLog.i("POS Tab Not visible reason: Screen size is not allowed")
            }
        }

        val siteSettings = wooCommerceStore
            .fetchSiteGeneralSettings(selectedSite)
            .model
            ?: return@withContext Result.failure(WooPosCouldNotDetermineValueException())

        return@withContext Result.success(
            isCountrySupported(countryCode = siteSettings.countryCode).also { isSupported ->
                if (!isSupported) {
                    wooPosLog.i("POS Tab Not visible reason: Country ${siteSettings.countryCode} is not supported")
                }
            }
        )
    }

    private fun isCountrySupported(countryCode: String) = SUPPORTED_COUNTRIES.contains(countryCode.lowercase())

    private companion object {
        private val SUPPORTED_COUNTRIES = listOf("us", "gb")
    }
}
