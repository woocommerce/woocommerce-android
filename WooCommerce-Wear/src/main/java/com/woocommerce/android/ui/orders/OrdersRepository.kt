package com.woocommerce.android.ui.orders

import javax.inject.Inject
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore

class OrdersRepository @Inject constructor(
    private val orderStore: WCOrderStore
) {
    fun observeOrdersChanges(selectedSite: SiteModel) =
        orderStore.observeOrdersForSite(selectedSite)
}
