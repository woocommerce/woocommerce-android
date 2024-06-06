package com.woocommerce.android.ui.moremenu.domain

import com.woocommerce.android.extensions.isFreeTrial
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MoreMenuRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val getWooVersion: GetWooCorePluginCachedVersion
) {
    companion object {
        private const val INBOX_MINIMUM_SUPPORTED_VERSION = "6.4.0"
    }

    suspend fun isInboxEnabled(): Boolean =
        withContext(Dispatchers.IO) {
            if (!selectedSite.exists() || !FeatureFlag.INBOX.isEnabled()) return@withContext false

            val currentWooCoreVersion = getWooVersion() ?: return@withContext false
            currentWooCoreVersion.semverCompareTo(INBOX_MINIMUM_SUPPORTED_VERSION) >= 0
        }

    fun isUpgradesEnabled(): Boolean = selectedSite.getIfExists()?.let {
        it.isWpComStore && !it.isFreeTrial
    } ?: false
}
