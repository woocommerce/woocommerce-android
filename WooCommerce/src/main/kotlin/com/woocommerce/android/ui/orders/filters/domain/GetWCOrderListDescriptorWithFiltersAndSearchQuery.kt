package com.woocommerce.android.ui.orders.filters.domain

import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import javax.inject.Inject

class GetWCOrderListDescriptorWithFiltersAndSearchQuery @Inject constructor(
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters
) {
    operator fun invoke(searchQuery: String): WCOrderListDescriptor {
        val listDescriptorWithFilters = getWCOrderListDescriptorWithFilters.invoke()

        return WCOrderListDescriptor(
            site = listDescriptorWithFilters.site,
            statusFilter = listDescriptorWithFilters.statusFilter,
            beforeFilter = listDescriptorWithFilters.beforeFilter,
            afterFilter = listDescriptorWithFilters.afterFilter,
            searchQuery = searchQuery
        )
    }
}
