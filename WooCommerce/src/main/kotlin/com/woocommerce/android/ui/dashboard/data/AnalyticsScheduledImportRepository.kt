package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class AnalyticsScheduledImportRepository @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) {
    suspend fun isEnabled(): WooResult<Boolean> =
        wooCommerceStore.fetchAnalyticsScheduledImportEnabled(selectedSite.get())

    suspend fun setEnabled(enabled: Boolean): WooResult<Boolean> =
        wooCommerceStore.updateAnalyticsScheduledImportEnabled(selectedSite.get(), enabled)
}
