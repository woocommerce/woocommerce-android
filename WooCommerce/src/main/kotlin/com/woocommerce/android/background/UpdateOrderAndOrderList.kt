package com.woocommerce.android.background

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class UpdateOrderAndOrderList @Inject constructor(
    private val updateOrdersList: UpdateOrdersList,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(remoteOrderId: Long): Boolean {
        val orderFetchedSuccess = selectedSite.getOrNull()?.let { site ->
            val result = orderStore.fetchSingleOrderSync(site, remoteOrderId)
            result.isError.not()
        } ?: false
        val listFetchedSuccess = updateOrdersList()
        return orderFetchedSuccess && listFetchedSuccess
    }
}
