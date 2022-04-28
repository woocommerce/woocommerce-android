package com.woocommerce.android.ui.moremenu.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_CORE
import javax.inject.Inject

class MoreMenuRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    companion object {
        private const val INBOX_MINIMUM_SUPPORTED_VERSION = "6.4.0"
    }

    suspend fun isInboxEnabled(): Boolean =
        withContext(Dispatchers.IO) {
            if (!FeatureFlag.MORE_MENU_INBOX.isEnabled()) return@withContext false

            val currentWooCoreVersion =
                wooCommerceStore.getSitePlugin(selectedSite.get(), WOO_CORE)?.version ?: "0.0"
            currentWooCoreVersion.semverCompareTo(INBOX_MINIMUM_SUPPORTED_VERSION) >= 0
        }

    fun observeCouponBetaSwitch() = appPrefsWrapper.observePrefs()
        .onStart { emit(Unit) }
        .map { appPrefsWrapper.isCouponsEnabled }
        .distinctUntilChanged()
}
