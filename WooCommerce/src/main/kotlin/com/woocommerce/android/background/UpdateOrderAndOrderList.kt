package com.woocommerce.android.background

import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class UpdateOrderAndOrderList @Inject constructor(
    private val updateOrdersListByStoreId: UpdateOrdersListByStoreId,
    private val orderStore: WCOrderStore,
    private val siteStore: SiteStore
) {
    suspend operator fun invoke(siteId: Long, remoteOrderId: Long): Boolean {
        val orderFetchedSuccess = siteStore.getSiteBySiteId(siteId)?.let { site ->
            val result = orderStore.fetchSingleOrderSync(site, remoteOrderId)
            result.isError.not()
        } ?: false
        val listFetchedSuccess = updateOrdersListByStoreId(siteId)
        return orderFetchedSuccess && listFetchedSuccess
    }
}
