package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class AnalyticsScheduledImportRepository @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) {
    fun observeIsEnabled(): Flow<Boolean> =
        wooCommerceStore.observeAnalyticsScheduledImportEnabled(selectedSite.get()).map { it ?: false }

    suspend fun refresh(): WooResult<Boolean> =
        wooCommerceStore.fetchAnalyticsScheduledImportEnabled(selectedSite.get())

    suspend fun setEnabled(enabled: Boolean): WooResult<Boolean> =
        wooCommerceStore.updateAnalyticsScheduledImportEnabled(selectedSite.get(), enabled)
}
