package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderattribution.ChannelSummaryApiResponse
import org.wordpress.android.fluxc.store.WCOrderAttributionStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class OrderAttributionRepository @Inject constructor(
    private val orderAttributionStore: WCOrderAttributionStore,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {
    suspend fun fetchChannelSummary(
        from: String,
        to: String,
        compareFrom: String,
        compareTo: String
    ): Result<List<ChannelSummaryApiResponse>> {
        return orderAttributionStore.fetchChannelSummary(
            site = selectedSite.get(),
            from = from,
            to = to,
            compareFrom = compareFrom,
            compareTo = compareTo
        )
    }

    suspend fun isOrderAttributionAvailable(): Boolean {
        val result = wooCommerceStore.isOrderAttributionAvailable(selectedSite.get())
        return result.model ?: false
    }
}
